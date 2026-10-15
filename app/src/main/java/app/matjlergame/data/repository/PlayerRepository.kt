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
class PlayerRepository(private val prefs: SharedPreferences, val firebase: FirebasePlayers? = null) {
    init {
        if (!prefs.contains("player_id")) {
            val id = UUID.randomUUID().toString()
            check(prefs.edit().putString("player_id", id)
                .putString("player_name", "Joueur-${id.take(8)}").putInt("player_avatar", PlayerAvatars.DEFAULT).commit())
        } else if ((!prefs.getBoolean("profile_setup_complete", false) ||
            prefs.getString("player_name", "") == "Joueur-${prefs.getString("player_id", "")!!.take(8)}") &&
            !prefs.getBoolean("avatar_customized", false) && prefs.getInt("player_avatar", 8) == 8) {
            // Only migrate the old automatic avatar; preserve personalized profiles.
            check(prefs.edit().putInt("player_avatar", PlayerAvatars.DEFAULT).commit())
        }
    }
    var currentProfile by androidx.compose.runtime.mutableStateOf(profile())
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
    @Synchronized
    fun start(mode: GameMode, day: LocalDate): Long {
        val key = "start_${day}_${mode.name}"
        if (!prefs.contains(key)) check(prefs.edit().putLong(key, System.currentTimeMillis()).commit())
        return prefs.getLong(key, System.currentTimeMillis())
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
        if (!globalEnabled) return
        syncLock.withLock {
            val today = PlayerPeriods.today()
            // Yesterday too, so a win sent after midnight (or offline) still reaches its period.
            val days = listOf(today, today.minusDays(1))
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
                catch (e: FirebaseRequestException) {
                    // Rejected by the rules (e.g. server already holds a better score): nothing to retry.
                    if (e.status != "PERMISSION_DENIED" && e.status != "INVALID_ARGUMENT") return
                } catch (_: Exception) { return }
                prefs.edit().putString("synced_$board", signature).apply()
            }
        }
    }
    suspend fun saveGlobalProfile(name: String, avatar: Int): Boolean {
        if (!update(name, avatar)) return false
        syncScores()
        return true
    }
    /** Top players of the current period. Never throws: falls back to the last cached page, then to an empty one. */
    suspend fun globalRanking(range: RankingPeriod, mode: GameMode?, cursor: JSONObject? = null): JSONObject {
        val period = range.id(PlayerPeriods.today())
        val board = boardId(range, mode, period)
        val empty = JSONObject().put("period", period).put("mode", mode?.name ?: ALL).put("entries", JSONArray())
        if (!globalEnabled) return empty
        return try {
            val query = JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "entries")))
                .put("orderBy", JSONArray().put(order("correctCount", "DESCENDING"))
                    .put(order("totalMs", "ASCENDING")).put(order("__name__", "ASCENDING")))
                .put("limit", PAGE_SIZE)
            if (cursor != null) query.put("startAt", JSONObject().put("before", false).put("values", JSONArray()
                .put(JSONObject().put("integerValue", cursor.getLong("correctCount").toString()))
                .put(JSONObject().put("integerValue", cursor.getLong("totalMs").toString()))
                .put(JSONObject().put("referenceValue", cursor.getString("document")))))
            val documents = firebase!!.query("leaderboards/$board", query)
            val entries = JSONArray()
            documents.forEach { document ->
                val fields = document.getJSONObject("fields")
                entries.put(JSONObject().put("uid", document.getString("name").substringAfterLast('/'))
                    .put("name", fields.getJSONObject("name").getString("stringValue"))
                    .put("avatar", fields.optJSONObject("avatar")?.optString("integerValue")?.toIntOrNull() ?: PlayerAvatars.DEFAULT)
                    .put("correctCount", fields.getJSONObject("correctCount").getString("integerValue").toInt())
                    .put("totalMs", fields.getJSONObject("totalMs").getString("integerValue").toLong()))
            }
            val last = entries.length() - 1
            val page = empty.put("entries", entries).put("cursor", if (documents.size < PAGE_SIZE) JSONObject.NULL else
                JSONObject().put("correctCount", entries.getJSONObject(last).getInt("correctCount"))
                    .put("totalMs", entries.getJSONObject(last).getLong("totalMs"))
                    .put("document", documents[last].getString("name")))
            if (cursor == null) prefs.edit().putString("cache_$board", page.toString()).apply()
            page
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (cursor != null) empty
            else prefs.getString("cache_$board", null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: empty
        }
    }
    private fun order(field: String, direction: String) =
        JSONObject().put("field", JSONObject().put("fieldPath", field)).put("direction", direction)

    companion object {
        private const val PAGE_SIZE = 50
        const val ALL = "ALL"
        /** Firestore board id, e.g. day_HARD_2026-10-14, week_ALL_2026-W42 or month_EASY_2026-10. */
        fun boardId(period: RankingPeriod, mode: GameMode?, id: String) = "${period.key}_${mode?.name ?: ALL}_$id"
    }
}
