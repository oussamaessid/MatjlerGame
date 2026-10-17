package app.matjlergame.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.SharedPreferences
import app.matjlergame.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Device-local guest store. Never represents these scores as server-verified. */
class PlayerRepository(private val prefs: SharedPreferences, val firebase: PlayerCloud? = null) {
    init {
        if (!prefs.contains("player_id")) {
            val id = UUID.randomUUID().toString()
            check(prefs.edit().putString("player_id", id)
                .putString("player_name", defaultName(id)).putInt("player_avatar", PlayerAvatars.DEFAULT).commit())
        } else if ((!prefs.getBoolean("profile_setup_complete", false) ||
            prefs.getString("player_name", "") == defaultName(prefs.getString("player_id", "")!!)) &&
            !prefs.getBoolean("avatar_customized", false) && prefs.getInt("player_avatar", 8) == 8) {
            // Only migrate the old automatic avatar; preserve personalized profiles.
            check(prefs.edit().putInt("player_avatar", PlayerAvatars.DEFAULT).commit())
        }
        // Earlier versions marked scores as sent when the API key was blocked; send them all again once.
        if (!prefs.getBoolean("sync_reset_v2", false)) {
            val edit = prefs.edit()
            prefs.all.keys.filter { it.startsWith("synced_") }.forEach { edit.remove(it) }
            edit.putBoolean("sync_reset_v2", true).apply()
        }
    }
    var currentProfile by androidx.compose.runtime.mutableStateOf(profile())
        private set
    var syncError by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    val needsSetup get() = !prefs.getBoolean("profile_setup_complete", false)
    fun completeSetup() { prefs.edit().putBoolean("profile_setup_complete", true).apply() }
    fun profile() = PlayerProfile(prefs.getString("player_id", "")!!,
        prefs.getString("player_name", "Joueur")!!, prefs.getInt("player_avatar", PlayerAvatars.DEFAULT).takeIf { PlayerAvatars.isValid(it) } ?: PlayerAvatars.DEFAULT)

    fun update(name: String, avatar: Int): Boolean {
        val clean = name.trim()
        require(clean.length in 2..24 && clean.none { it.isISOControl() })
        require(PlayerAvatars.isValid(avatar))
        val saved = prefs.edit().putString("player_name", clean).putInt("player_avatar", avatar).putBoolean("avatar_customized", true).commit()
        if (saved) currentProfile = profile()
        return saved
    }
    /** Time already played on today's game; time spent away from the game screen is not included. */
    fun playedTime(mode: GameMode, day: LocalDate): Long = prefs.getLong("played_${day}_${mode.name}", 0L)
    fun savePlayedTime(mode: GameMode, day: LocalDate, durationMs: Long) {
        prefs.edit().putLong("played_${day}_${mode.name}", durationMs.coerceAtLeast(0)).apply()
    }
    @Synchronized
    fun record(mode: GameMode, day: LocalDate, won: Boolean, durationMs: Long): Boolean {
        val key = "score_${day}_${mode.name}"
        if (prefs.contains(key)) return false
        return prefs.edit().putString(key, JSONObject().put("day", day.toString())
            .put("mode", mode.name).put("won", won).put("duration", durationMs.coerceAtLeast(1)).toString()).commit()
    }
    fun ranking(day: LocalDate, period: RankingPeriod, mode: GameMode?): List<RankingEntry> {
        val scores = prefs.all.filterKeys { it.startsWith("score_") }.values.mapNotNull { value ->
            runCatching {
                val obj = JSONObject(value as String)
                PlayerScore(obj.getString("day"), obj.getString("mode"), obj.getBoolean("won"), obj.getLong("duration"))
            }.getOrNull()
        }
        return PlayerRanking.sort(PlayerRanking.calculate(profile(), scores, day, period, mode))
    }
    val globalEnabled get() = firebase?.configured == true
    private val syncLock = Mutex()

    /**
     * Pushes local wins to the global boards (Firestore, no account needed). Silent by design:
     * failures are retried at the next launch, game end or leaderboard visit.
     */
    suspend fun syncScores() {
        if (!globalEnabled) {
            syncError = "Configuration du classement absente de cette version. Mettez l’application à jour."
            return
        }
        syncLock.withLock {
            syncError = null
            // Players from earlier versions claim their name and avatar once; a pair already taken is left as is.
            val current = profile()
            if (!needsSetup && current.name != defaultName(current.id)) reserveName(current.name, current.avatar)
            val today = PlayerPeriods.today()
            // Retry every saved period, even after several days offline.
            val days = (listOf(today) + prefs.all.filterKeys { it.startsWith("score_") }.values.mapNotNull {
                runCatching { LocalDate.parse(JSONObject(it as String).getString("day")) }.getOrNull()
            }).distinct()
            val boards = linkedMapOf<String, RankingEntry>()
            // null = general board, all three difficulties together.
            for (mode in listOf<GameMode?>(null) + GameMode.entries) for (period in RankingPeriod.entries) {
                days.distinctBy { period.id(it) }.forEach { day ->
                    ranking(day, period, mode).firstOrNull()?.let { boards[boardId(period, mode, period.id(day))] = it }
                }
            }
            val profile = profile()
            for ((board, entry) in boards) {
                val signature = "${entry.correct}:${entry.durationMs}:${profile.name}:${profile.avatar}"
                if (prefs.getString("synced_$board", null) == signature) continue
                try {
                    firebase!!.write(mapOf("leaderboards/$board/entries/${profile.id}" to JSONObject()
                        .put("name", JSONObject().put("stringValue", profile.name))
                        .put("avatar", JSONObject().put("integerValue", profile.avatar.toString()))
                        .put("correctCount", JSONObject().put("integerValue", entry.correct.toString()))
                        .put("totalMs", JSONObject().put("integerValue", entry.durationMs.toString()))))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    // A rejected request is never acknowledged as a successful synchronization.
                    syncError = app.matjlergame.presentation.model.playerServiceMessage(e)
                    return
                }
                prefs.edit().putString("synced_$board", signature).apply()
            }
        }
    }
    /** Returns null once saved, otherwise the message to show (name taken, offline…). */
    suspend fun saveGlobalProfile(name: String, avatar: Int): String? {
        if (globalEnabled) reserveName(name, avatar)?.let { return it }
        if (!update(name, avatar)) return "Enregistrement impossible."
        syncScores()
        return null
    }

    /**
     * Claims the name and avatar pair in names/{nameKey} so that no other player can use both together;
     * the same name stays free with another avatar.
     * firestore.rules refuse to hand a reserved pair to another player id, even on simultaneous requests.
     */
    suspend fun reserveName(name: String, avatar: Int): String? {
        val key = nameKey(name, avatar)
        if (prefs.getString("reserved_name", null) == key) return null
        val cloud = firebase!!
        val path = "names/$key"
        val me = profile().id
        fun takenByOther(document: JSONObject?) = document?.optJSONObject("fields")?.optJSONObject("playerId")
            ?.optString("stringValue")?.let { it != me } == true
        try {
            if (takenByOther(cloud.read(path))) return NAME_TAKEN
            cloud.write(mapOf(path to JSONObject()
                .put("name", JSONObject().put("stringValue", name.trim()))
                .put("avatar", JSONObject().put("integerValue", avatar.toString()))
                .put("playerId", JSONObject().put("stringValue", me))))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // Lost the race to another player between the read and the write.
            if (e is FirebaseRequestException && e.status == "PERMISSION_DENIED" &&
                runCatching { takenByOther(cloud.read(path)) }.getOrDefault(false)) return NAME_TAKEN
            return if (e is java.io.IOException) "Connexion impossible : vérifiez Internet pour valider ce pseudo."
                else "Le pseudo n’a pas pu être vérifié. Réessayez dans un instant."
        }
        prefs.edit().putString("reserved_name", key).apply()
        return null
    }
    /**
     * The player's neighbourhood in the current period: up to 2 players ahead, the player, up to 2 behind,
     * each with its global rank ("rank"). A player not ranked yet gets the top of the board instead.
     * Falls back to the last cached result when offline.
     */
    suspend fun globalRanking(range: RankingPeriod, mode: GameMode?): JSONObject {
        val period = range.id(PlayerPeriods.today())
        val board = boardId(range, mode, period)
        val empty = JSONObject().put("period", period).put("mode", mode?.name ?: ALL).put("entries", JSONArray())
        if (!globalEnabled) throw FirebaseRequestException("MISSING_APP_CONFIGURATION", "FAILED_PRECONDITION")
        val cloud = firebase!!
        val parent = "leaderboards/$board"
        return try {
            val me = cloud.documentName("$parent/entries/${profile().id}")
            val own = cloud.query(parent, entriesQuery().put("where", JSONObject().put("fieldFilter", JSONObject()
                .put("field", JSONObject().put("fieldPath", "__name__")).put("op", "EQUAL")
                .put("value", JSONObject().put("referenceValue", me)))).put("limit", 1)).firstOrNull()
            val rows: List<Pair<Int, JSONObject>> = if (own == null) {
                cloud.query(parent, ranked().put("limit", NEIGHBOURS * 2 + 1)).mapIndexed { i, doc -> i + 1 to doc }
            } else {
                val cursor = cursorOf(own)
                val ahead = cloud.count(parent, ranked().put("endAt", JSONObject().put("before", true).put("values", cursor))).toInt()
                val above = playersAbove(parent, cursor, ahead)
                val below = cloud.query(parent, ranked().put("limit", NEIGHBOURS)
                    .put("startAt", JSONObject().put("before", false).put("values", cursor)))
                val rank = ahead + 1
                above.mapIndexed { i, doc -> rank - above.size + i to doc } + (rank to own) +
                    below.mapIndexed { i, doc -> rank + 1 + i to doc }
            }
            val entries = JSONArray()
            rows.forEach { (rank, document) ->
                val fields = document.getJSONObject("fields")
                entries.put(JSONObject().put("uid", document.getString("name").substringAfterLast('/')).put("rank", rank)
                    .put("name", fields.getJSONObject("name").getString("stringValue"))
                    .put("avatar", fields.optJSONObject("avatar")?.optString("integerValue")?.toIntOrNull() ?: PlayerAvatars.DEFAULT)
                    .put("correctCount", fields.getJSONObject("correctCount").getString("integerValue").toInt())
                    .put("totalMs", fields.getJSONObject("totalMs").getString("integerValue").toLong()))
            }
            val page = empty.put("entries", entries).put("ranked", own != null)
            prefs.edit().putString("cache_$board", page.toString()).apply()
            page
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            prefs.getString("cache_$board", null)?.let { saved ->
                runCatching { JSONObject(saved).put("cached", true).put("problem", app.matjlergame.presentation.model.playerServiceMessage(e)) }.getOrNull()
            } ?: throw e
        }
    }

    /** The [NEIGHBOURS] players just ahead, best first. */
    private suspend fun playersAbove(parent: String, cursor: JSONArray, ahead: Int): List<JSONObject> {
        if (ahead == 0) return emptyList()
        val cloud = firebase!!
        return try {
            // Reverse order needs the (correctCount ASC, totalMs DESC) index from firebase/firestore.indexes.json.
            cloud.query(parent, entriesQuery().put("orderBy", JSONArray().put(order("correctCount", "ASCENDING"))
                .put(order("totalMs", "DESCENDING")).put(order("__name__", "DESCENDING")))
                .put("startAt", JSONObject().put("before", false).put("values", cursor)).put("limit", NEIGHBOURS)).reversed()
        } catch (e: FirebaseRequestException) {
            if (e.status != "FAILED_PRECONDITION") throw e
            // Index not built yet: skip to the same rows in the normal order (costs the skipped reads).
            cloud.query(parent, ranked().put("offset", (ahead - NEIGHBOURS).coerceAtLeast(0))
                .put("limit", minOf(ahead, NEIGHBOURS)))
        }
    }
    private fun entriesQuery() = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "entries")))
    /** Board order: most wins, then shortest total time, then player id for ties. */
    private fun ranked() = entriesQuery().put("orderBy", JSONArray().put(order("correctCount", "DESCENDING"))
        .put(order("totalMs", "ASCENDING")).put(order("__name__", "ASCENDING")))
    private fun cursorOf(document: JSONObject): JSONArray {
        val fields = document.getJSONObject("fields")
        return JSONArray().put(fields.getJSONObject("correctCount")).put(fields.getJSONObject("totalMs"))
            .put(JSONObject().put("referenceValue", document.getString("name")))
    }
    private fun order(field: String, direction: String) =
        JSONObject().put("field", JSONObject().put("fieldPath", field)).put("direction", direction)

    companion object {
        /** Players shown on each side of the current player. */
        private const val NEIGHBOURS = 2
        const val ALL = "ALL"
        const val NAME_TAKEN = "Un autre joueur a déjà ce pseudo avec cet avatar. Changez de pseudo ou d’avatar."
        fun defaultName(id: String) = "Joueur-${id.take(8)}"
        /**
         * Firestore id shared by every spelling of a name with the same avatar: "Alice", " ALICE " and "alice"
         * collide, "Alice" with another avatar does not. Hex keeps any character (emoji, accents, "/") valid in a document id.
         */
        fun nameKey(name: String, avatar: Int): String = java.text.Normalizer.normalize(name.trim().replace(Regex("\\s+"), " "),
            java.text.Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
            .toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) } + "_$avatar"
        /** Firestore board id, e.g. day_HARD_2026-10-14, week_ALL_2026-W42 or month_EASY_2026-10. */
        fun boardId(period: RankingPeriod, mode: GameMode?, id: String) = "${period.key}_${mode?.name ?: ALL}_$id"
    }
}
