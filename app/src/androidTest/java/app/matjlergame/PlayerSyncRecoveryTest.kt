package app.matjlergame

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import app.matjlergame.data.repository.*
import app.matjlergame.domain.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class PlayerSyncRecoveryTest {
    private class Cloud : PlayerCloud {
        override var configured = true
        var failure: Exception? = null
        var queryFailure: Exception? = null
        val sent = mutableListOf<String>()
        override suspend fun write(documents: Map<String, JSONObject>) {
            failure?.let { throw it }; sent.addAll(documents.keys)
        }
        override suspend fun query(parentPath: String, structuredQuery: JSONObject): List<JSONObject> {
            queryFailure?.let { throw it }; return emptyList()
        }
    }
    /** Applies the names/ rule of firestore.rules: a reserved name never changes owner. */
    private class NamesCloud : PlayerCloud {
        override val configured = true
        var offline = false
        val documents = mutableMapOf<String, JSONObject>()
        override suspend fun write(documents: Map<String, JSONObject>) {
            if (offline) throw IOException("offline")
            documents.forEach { (path, fields) ->
                val owner = this.documents[path]?.getJSONObject("fields")?.getJSONObject("playerId")?.getString("stringValue")
                if (path.startsWith("names/") && owner != null && owner != fields.getJSONObject("playerId").getString("stringValue"))
                    throw FirebaseRequestException("denied", "PERMISSION_DENIED")
                this.documents[path] = JSONObject().put("fields", fields)
            }
        }
        override suspend fun query(parentPath: String, structuredQuery: JSONObject) = emptyList<JSONObject>()
        override suspend fun read(path: String): JSONObject? {
            if (offline) throw IOException("offline")
            return documents[path]
        }
    }
    private fun prefs() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("sync_test_${UUID.randomUUID()}", Context.MODE_PRIVATE)

    @Test fun refusedWriteIsRetriedInsteadOfBeingMarkedSent() = runBlocking<Unit> {
        val prefs=prefs();val cloud=Cloud();val repo=PlayerRepository(prefs,cloud)
        repo.record(GameMode.EASY,PlayerPeriods.today(),true,1234)
        cloud.failure=FirebaseRequestException("Missing or insufficient permissions.","PERMISSION_DENIED")
        repo.syncScores()
        assertNotNull(repo.syncError)
        assertFalse(prefs.all.keys.any { it.startsWith("synced_") })
        cloud.failure=null;repo.syncScores()
        assertNull(repo.syncError);assertEquals(6,cloud.sent.size)
        repo.syncScores();assertEquals(6,cloud.sent.size)
        prefs.edit().clear().commit()
    }
    @Test fun winsFromPreviousMonthsAreNotLostAfterAnOfflinePeriod() = runBlocking<Unit> {
        val prefs=prefs();val cloud=Cloud();val repo=PlayerRepository(prefs,cloud)
        val old=PlayerPeriods.today().minusMonths(2)
        repo.record(GameMode.HARD,old,true,3210);repo.syncScores()
        assertTrue(cloud.sent.any { it.contains("day_HARD_$old/") })
        assertTrue(cloud.sent.any { it.contains("month_HARD_${old.toString().take(7)}/") })
        prefs.edit().clear().commit()
    }
    @Test fun aReadFailureCannotMasqueradeAsAnEmptyLeaderboard() = runBlocking<Unit> {
        val prefs=prefs();val cloud=Cloud();val repo=PlayerRepository(prefs,cloud)
        cloud.queryFailure=IOException("offline")
        try { repo.globalRanking(RankingPeriod.DAY,null);fail("An unavailable service must not appear empty") }
        catch (_: IOException) { }
        cloud.queryFailure=null
        assertEquals(0,repo.globalRanking(RankingPeriod.DAY,null).getJSONArray("entries").length())
        cloud.queryFailure=IOException("offline")
        val cached=repo.globalRanking(RankingPeriod.DAY,null)
        assertTrue(cached.getBoolean("cached"));assertTrue(cached.has("problem"))
        prefs.edit().clear().commit()
    }
    @Test fun aNameTakenByAnotherPlayerIsRefusedEvenWithAnotherAvatar() = runBlocking<Unit> {
        val cloud = NamesCloud()
        val alice = PlayerRepository(prefs(), cloud)
        assertNull(alice.saveGlobalProfile("Alice", 3))
        val other = PlayerRepository(prefs(), cloud)
        val before = other.currentProfile
        assertEquals(PlayerRepository.NAME_TAKEN, other.saveGlobalProfile(" alice ", 9))
        assertEquals(before, other.currentProfile)
        // The owner keeps the name when changing only the avatar.
        assertNull(alice.saveGlobalProfile("Alice", 7))
        assertNull(other.saveGlobalProfile("Bob", 3))
    }
    @Test fun offlineNameCheckDoesNotSaveTheProfile() = runBlocking<Unit> {
        val cloud = NamesCloud().apply { offline = true }
        val repository = PlayerRepository(prefs(), cloud)
        val before = repository.currentProfile
        assertNotNull(repository.saveGlobalProfile("Alice", 3))
        assertEquals(before, repository.currentProfile)
    }
}
