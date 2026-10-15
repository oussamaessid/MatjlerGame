package app.matjlergame

import app.matjlergame.domain.model.*
import java.time.LocalDate
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class PlayerRankingTest {
    private val player = PlayerProfile("a", "Joueur", 0)
    private val day = LocalDate.of(2026, 10, 14)
    @Test fun duplicateModeAndDayCountsOnce() {
        val score = PlayerScore("2026-10-14", "EASY", true, 5000)
        val result = PlayerRanking.calculate(player, listOf(score, score), day, RankingPeriod.DAY, GameMode.EASY).single()
        assertEquals(1, result.correct)
        assertEquals(5000L, result.durationMs)
    }
    @Test fun periodsExcludeLossesOtherDaysAndOtherMonths() {
        val scores = listOf(
            PlayerScore("2026-10-14", "EASY", true, 5000),
            PlayerScore("2026-10-14", "HARD", false, 1000),
            PlayerScore("2026-10-01", "EASY", true, 2000),
            PlayerScore("2026-09-30", "EASY", true, 3000),
            PlayerScore("2025-10-14", "EASY", true, 1000))
        assertEquals(1, PlayerRanking.calculate(player, scores, day, RankingPeriod.DAY, GameMode.EASY).single().correct)
        val month = PlayerRanking.calculate(player, scores, day, RankingPeriod.MONTH, GameMode.EASY).single()
        assertEquals(2, month.correct)
        assertEquals(7000L, month.durationMs)
    }
    @Test fun correctCountThenSpeedThenStableIdentity() {
        val entries = listOf(RankingEntry(player, 1, 1),
            RankingEntry(player.copy(id = "c"), 2, 100),
            RankingEntry(player.copy(id = "b"), 2, 100),
            RankingEntry(player.copy(id = "d"), 2, 50))
        assertEquals(listOf("d", "b", "c", "a"), PlayerRanking.sort(entries).map { it.player.id })
    }
    @Test fun tunisMidnightIsExplicit() {
        assertEquals(LocalDate.of(2026, 11, 1), Instant.parse("2026-10-31T23:00:00Z").atZone(PlayerPeriods.zone).toLocalDate())
        assertTrue(PlayerRanking.calculate(player, emptyList(), day, RankingPeriod.MONTH, GameMode.EASY).isEmpty())
    }
    @Test fun eachDifficultyKeepsItsOwnDailyAndMonthlyTotals() {
        val scores = listOf(
            PlayerScore("2026-10-14", "EASY", true, 1000),
            PlayerScore("2026-10-01", "EASY", true, 2000),
            PlayerScore("2026-10-14", "MEDIUM", true, 5000),
            PlayerScore("2026-10-14", "HARD", true, 9000),
            PlayerScore("2026-10-01", "HARD", true, 7000))
        for ((mode, expectedTime) in listOf(GameMode.EASY to 1000L, GameMode.MEDIUM to 5000L, GameMode.HARD to 9000L)) {
            val result = PlayerRanking.calculate(player, scores, day, RankingPeriod.DAY, mode).single()
            assertEquals(1, result.correct)
            assertEquals(expectedTime, result.durationMs)
        }
        val easyMonth = PlayerRanking.calculate(player, scores, day, RankingPeriod.MONTH, GameMode.EASY).single()
        assertEquals(2, easyMonth.correct)
        assertEquals(3000L, easyMonth.durationMs)
        assertEquals(16000L, PlayerRanking.calculate(player, scores, day, RankingPeriod.MONTH, GameMode.HARD).single().durationMs)
        assertTrue(PlayerRanking.calculate(player, scores.filter { it.mode == "EASY" }, day, RankingPeriod.MONTH, GameMode.HARD).isEmpty())
    }

}
