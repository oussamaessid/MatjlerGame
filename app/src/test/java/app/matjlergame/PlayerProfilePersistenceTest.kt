package app.matjlergame

import android.content.SharedPreferences
import app.matjlergame.data.repository.PlayerRepository
import app.matjlergame.domain.model.PlayerRanking
import app.matjlergame.domain.model.PlayerScore
import app.matjlergame.domain.model.RankingPeriod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlayerProfilePersistenceTest {
    @Test fun guestIdentityAndAvatarSurviveReopeningRepository() {
        val storage = MemoryPreferences()
        val original = PlayerRepository(storage).currentProfile
        val reopened = PlayerRepository(storage).currentProfile
        assertEquals(original, reopened)
        assertTrue(original.id.isNotBlank())
        assertTrue(original.name.startsWith("Joueur-"))
        assertEquals(app.matjlergame.domain.model.PlayerAvatars.DEFAULT, original.avatar)
    }
    @Test fun profileEditsSurviveWithoutFirebase() = runBlocking {
        val storage = MemoryPreferences()
        val repository = PlayerRepository(storage)
        assertNull(repository.saveGlobalProfile("  Alice  ", 5))
        repository.completeSetup()
        repository.syncScores()
        val reopened = PlayerRepository(storage)
        assertEquals("Alice", reopened.currentProfile.name)
        assertEquals(5, reopened.currentProfile.avatar)
        assertFalse(reopened.needsSetup)
    }
    @Test fun boardIdsMatchFirestoreRules() {
        val day = java.time.LocalDate.of(2026, 10, 14)
        val hard = app.matjlergame.domain.model.GameMode.HARD
        assertEquals("day_HARD_2026-10-14", PlayerRepository.boardId(RankingPeriod.DAY, hard, RankingPeriod.DAY.id(day)))
        assertEquals("week_ALL_2026-W42", PlayerRepository.boardId(RankingPeriod.WEEK, null, RankingPeriod.WEEK.id(day)))
        assertEquals("month_ALL_2026-10", PlayerRepository.boardId(RankingPeriod.MONTH, null, RankingPeriod.MONTH.id(day)))
        assertEquals(java.time.LocalDate.of(2026, 10, 12), RankingPeriod.weekStart("2026-W42"))
    }
    @Test fun generalRankingAddsTheThreeDifficultiesAndWeekGroupsMondayToSunday() {
        val me = app.matjlergame.domain.model.PlayerProfile("id", "Moi", 0)
        val scores = listOf(PlayerScore("2026-10-12", "EASY", true, 1000), PlayerScore("2026-10-12", "HARD", true, 3000),
            PlayerScore("2026-10-13", "MEDIUM", true, 2000), PlayerScore("2026-10-13", "HARD", false, 500),
            PlayerScore("2026-10-11", "EASY", true, 9000))
        val day = java.time.LocalDate.of(2026, 10, 14)
        val week = PlayerRanking.calculate(me, scores, day, RankingPeriod.WEEK, null).single()
        assertEquals(3, week.correct)
        assertEquals(6000, week.durationMs)
        assertEquals(1, PlayerRanking.calculate(me, scores, day, RankingPeriod.WEEK,
            app.matjlergame.domain.model.GameMode.HARD).single().correct)
        assertEquals(4, PlayerRanking.calculate(me, scores, day, RankingPeriod.MONTH, null).single().correct)
    }
    @Test fun ludoSelectionAndExistingClassicIdsSurviveRestart() = runBlocking {
        val storage = MemoryPreferences()
        val repository = PlayerRepository(storage)
        assertNull(repository.saveGlobalProfile("Joueur Ludo", 55))
        assertEquals(55, PlayerRepository(storage).currentProfile.avatar)
        assertNull(repository.saveGlobalProfile("Joueur classique", 0))
        assertEquals(0, PlayerRepository(storage).currentProfile.avatar)
    }

    @Test fun nameKeyIgnoresCaseAndSpacesButNotTheAvatar() {
        assertEquals(PlayerRepository.nameKey("Alice", 3), PlayerRepository.nameKey("  ALICE ", 3))
        assertEquals(PlayerRepository.nameKey("Ali  ce", 3), PlayerRepository.nameKey("ali ce", 3))
        assertNotEquals(PlayerRepository.nameKey("Alice", 3), PlayerRepository.nameKey("Alicia", 3))
        assertNotEquals(PlayerRepository.nameKey("Alice", 3), PlayerRepository.nameKey("Alice", 9))
        assertTrue(PlayerRepository.nameKey("Été ⭐/x", 55).matches(Regex("[0-9a-f]+_55")))
    }

    @Test fun invalidProfileCannotReplaceExistingProfile() {
        val repository = PlayerRepository(MemoryPreferences())
        val original = repository.currentProfile
        for ((name, avatar) in listOf("x" to 0, "Alice" to app.matjlergame.domain.model.PlayerAvatars.COUNT, "Al\nce" to 1)) {
            try { repository.update(name, avatar); fail("Invalid profile accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals(original, repository.currentProfile)
    }
}

private class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun contains(key: String?) = values.containsKey(key)
    override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) = values[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clearing = false
        private fun put(key: String?, value: Any?): SharedPreferences.Editor { changes[key!!] = value; return this }
        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values)
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)
        override fun remove(key: String?) = put(key, null)
        override fun clear(): SharedPreferences.Editor { clearing = true; return this }
        override fun apply() { commit() }
        override fun commit(): Boolean {
            if (clearing) values.clear()
            changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            return true
        }
    }
}
