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
    private fun prefs() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("sync_test_${UUID.randomUUID()}", Context.MODE_PRIVATE)

    @Test fun refusedWriteIsRetriedInsteadOfBeingMarkedSent() = runBlocking {
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
    @Test fun winsFromPreviousMonthsAreNotLostAfterAnOfflinePeriod() = runBlocking {
        val prefs=prefs();val cloud=Cloud();val repo=PlayerRepository(prefs,cloud)
        val old=PlayerPeriods.today().minusMonths(2)
        repo.record(GameMode.HARD,old,true,3210);repo.syncScores()
        assertTrue(cloud.sent.any { it.contains("day_HARD_$old/") })
        assertTrue(cloud.sent.any { it.contains("month_HARD_${old.toString().take(7)}/") })
        prefs.edit().clear().commit()
    }
    @Test fun aReadFailureCannotMasqueradeAsAnEmptyLeaderboard() = runBlocking {
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
}
