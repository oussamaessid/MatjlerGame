package app.matjlergame.domain.model

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.IsoFields
import java.time.YearMonth
import java.time.ZoneId

object PlayerPeriods {
    val zone: ZoneId = ZoneId.of("Africa/Tunis")
    fun today(): LocalDate = LocalDate.now(zone)
}

data class PlayerProfile(val id: String, val name: String, val avatar: Int)
data class PlayerScore(val day: String, val mode: String, val won: Boolean, val durationMs: Long)
data class RankingEntry(val player: PlayerProfile, val correct: Int, val durationMs: Long)

/** Leaderboard periods (Tunis time). maxWins is per difficulty: one win per level and per day. */
enum class RankingPeriod(val key: String, val maxWins: Int) {
    DAY("day", 1), WEEK("week", 7), MONTH("month", 31);

    /** Period id: 2026-10-14, 2026-W42 (ISO week, Monday to Sunday) or 2026-10. */
    fun id(day: LocalDate): String = when (this) {
        DAY -> day.toString()
        WEEK -> "%d-W%02d".format(day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        MONTH -> YearMonth.from(day).toString()
    }

    companion object {
        /** Monday of an ISO week id such as 2026-W42. */
        fun weekStart(id: String): LocalDate {
            val (year, week) = id.split("-W").map { it.toInt() }
            return LocalDate.of(year, 1, 4).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week.toLong()).with(DayOfWeek.MONDAY)
        }
    }
}

object PlayerRanking {
    /** mode = null: general ranking, the three difficulties added together. */
    fun calculate(player: PlayerProfile, scores: List<PlayerScore>, day: LocalDate, period: RankingPeriod, mode: GameMode?): List<RankingEntry> {
        val wins = scores.distinctBy { "${it.day}/${it.mode}" }.filter {
            it.won && (mode == null || it.mode == mode.name) && period.id(LocalDate.parse(it.day)) == period.id(day)
        }
        return if (wins.isEmpty()) emptyList() else listOf(RankingEntry(player, wins.size, wins.sumOf { it.durationMs }))
    }
    fun sort(entries: List<RankingEntry>): List<RankingEntry> = entries.sortedWith(
        compareByDescending<RankingEntry> { it.correct }.thenBy { it.durationMs }.thenBy { it.player.id }
    )
}
