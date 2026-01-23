package app.matjlergame.domain.usecase

import android.content.SharedPreferences
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.repository.LevelRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyLevelManager(
    private val repository: LevelRepository
) {
    private val startDate = LocalDate.of(2026, 1, 1)

    /**
     * Récupère le niveau du jour pour un mode donné
     * Les niveaux s'affichent dans l'ordre du JSON, un par jour
     */
    fun getDailyLevel(mode: GameMode): Level? {
        val allLevels = repository.getLevelsForMode(mode)
        if (allLevels.isEmpty()) return null

        val today = LocalDate.now()
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)

        val index = if (daysSinceStart >= 0) {
            (daysSinceStart % allLevels.size).toInt()
        } else {
            0
        }

        return allLevels.getOrNull(index)
    }

    /**
     * Vérifie si le joueur a déjà joué aujourd'hui
     */
    fun hasPlayedToday(mode: GameMode, sharedPreferences: SharedPreferences): Boolean {
        val today = LocalDate.now().toString()
        val lastPlayedKey = "last_played_${mode.name}"
        val lastPlayed = sharedPreferences.getString(lastPlayedKey, "")
        return lastPlayed == today
    }

    /**
     * Marque le niveau du jour comme joué
     */
    fun markAsPlayed(mode: GameMode, sharedPreferences: SharedPreferences) {
        val today = LocalDate.now().toString()
        val lastPlayedKey = "last_played_${mode.name}"
        sharedPreferences.edit().putString(lastPlayedKey, today).apply()
    }

    /**
     * Récupère le résultat du jour
     */
    fun getTodayResult(mode: GameMode, sharedPreferences: SharedPreferences): DailyResult? {
        val today = LocalDate.now().toString()
        val resultKey = "result_${mode.name}_$today"
        val won = sharedPreferences.getBoolean("${resultKey}_won", false)
        val attempts = sharedPreferences.getInt("${resultKey}_attempts", 0)

        if (attempts == 0) return null
        return DailyResult(won, attempts)
    }

    /**
     * Sauvegarde le résultat du jour
     */
    fun saveTodayResult(
        mode: GameMode,
        won: Boolean,
        attempts: Int,
        sharedPreferences: SharedPreferences
    ) {
        val today = LocalDate.now().toString()
        val resultKey = "result_${mode.name}_$today"

        sharedPreferences.edit().apply {
            putBoolean("${resultKey}_won", won)
            putInt("${resultKey}_attempts", attempts)
            apply()
        }

        markAsPlayed(mode, sharedPreferences)
    }

    /**
     * Calcule les statistiques globales
     */
    fun getStatistics(mode: GameMode, sharedPreferences: SharedPreferences): Statistics {
        val totalPlayed = sharedPreferences.getInt("stats_${mode.name}_played", 0)
        val totalWon = sharedPreferences.getInt("stats_${mode.name}_won", 0)
        val currentStreak = sharedPreferences.getInt("stats_${mode.name}_streak", 0)
        val maxStreak = sharedPreferences.getInt("stats_${mode.name}_max_streak", 0)

        return Statistics(totalPlayed, totalWon, currentStreak, maxStreak)
    }

    /**
     * Met à jour les statistiques après une partie
     */
    fun updateStatistics(
        mode: GameMode,
        won: Boolean,
        sharedPreferences: SharedPreferences
    ) {
        val stats = getStatistics(mode, sharedPreferences)

        val newPlayed = stats.totalPlayed + 1
        val newWon = if (won) stats.totalWon + 1 else stats.totalWon
        val newStreak = if (won) stats.currentStreak + 1 else 0
        val newMaxStreak = maxOf(stats.maxStreak, newStreak)

        sharedPreferences.edit().apply {
            putInt("stats_${mode.name}_played", newPlayed)
            putInt("stats_${mode.name}_won", newWon)
            putInt("stats_${mode.name}_streak", newStreak)
            putInt("stats_${mode.name}_max_streak", newMaxStreak)
            apply()
        }
    }
}

data class DailyResult(
    val won: Boolean,
    val attempts: Int
)

data class Statistics(
    val totalPlayed: Int,
    val totalWon: Int,
    val currentStreak: Int,
    val maxStreak: Int
) {
    val winPercentage: Int
        get() = if (totalPlayed > 0) (totalWon * 100 / totalPlayed) else 0
}