package app.matjlergame.data.repository

import android.util.Log
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.repository.LevelRepository
import org.json.JSONArray
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LevelRepositoryImpl : LevelRepository {

    private val jsonUrl = "https://raw.githubusercontent.com/oussamaessid/MatjlerData/refs/heads/main/MatjlerData.json"
    private val cache = ConcurrentHashMap<GameMode, List<Level>>()
    private var isDataLoaded = false
    private var loadError: Exception? = null

    companion object {
        private const val TAG = "LevelRepository"
        private const val TIMEOUT_SECONDS = 30L  // Timeout de 30 secondes pour charger
    }

    /**
     * Charge les données depuis l'API de manière synchrone
     * Retourne true si succès, false si échec
     */
    fun loadDataFromApiSync(): Boolean {
        return try {
            Log.d(TAG, "🔄 Chargement des données depuis l'API...")

            val jsonText = URL(jsonUrl).readText()
            val jsonArray = JSONArray(jsonText)

            // Vider le cache avant de recharger
            cache.clear()

            for (i in 0 until jsonArray.length()) {
                val modeObject = jsonArray.getJSONObject(i)
                val modeName = modeObject.getString("mode")
                val questionsArray = modeObject.getJSONArray("questions")

                val levels = mutableListOf<Level>()
                for (j in 0 until questionsArray.length()) {
                    val question = questionsArray.getJSONObject(j)
                    levels.add(
                        Level(
                            number = question.getInt("level"),
                            target = question.getInt("target"),
                            solution = question.getString("solution"),
                            slots = question.getInt("slots"),
                            maxGuesses = question.getInt("max_guesses")
                        )
                    )
                }

                when (modeName) {
                    "EASY" -> cache[GameMode.EASY] = levels
                    "MEDIUM" -> cache[GameMode.MEDIUM] = levels
                    "HARD" -> cache[GameMode.HARD] = levels
                }
            }

            isDataLoaded = true
            loadError = null
            Log.d(TAG, "✅ Données chargées avec succès depuis l'API")
            Log.d(TAG, "EASY: ${cache[GameMode.EASY]?.size} niveaux")
            Log.d(TAG, "MEDIUM: ${cache[GameMode.MEDIUM]?.size} niveaux")
            Log.d(TAG, "HARD: ${cache[GameMode.HARD]?.size} niveaux")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du chargement depuis l'API", e)
            loadError = e
            isDataLoaded = false
            false
        }
    }

    /**
     * Charge les données de manière asynchrone avec timeout
     * Appelle callback(true) si succès, callback(false) si échec
     */
    fun loadDataAsync(callback: (Boolean) -> Unit) {
        Thread {
            val startTime = System.currentTimeMillis()
            val success = loadDataFromApiSync()
            val duration = System.currentTimeMillis() - startTime

            Log.d(TAG, "Chargement terminé en ${duration}ms avec succès=$success")
            callback(success)
        }.start()
    }

    override fun getLevelsForMode(mode: GameMode): List<Level> {
        if (!isDataLoaded) {
            Log.w(TAG, "⚠️ Tentative d'accès aux niveaux alors que les données ne sont pas chargées")
            return emptyList()
        }

        return cache[mode] ?: emptyList()
    }

    override fun getLevelByNumber(mode: GameMode, levelNumber: Int): Level? {
        return getLevelsForMode(mode).find { it.number == levelNumber }
    }

    /**
     * Force le rechargement des données depuis l'API
     */
    fun refreshData(callback: (Boolean) -> Unit) {
        isDataLoaded = false
        cache.clear()
        loadError = null
        loadDataAsync(callback)
    }

    /**
     * Retourne l'erreur de chargement s'il y en a une
     */
    fun getLoadError(): Exception? = loadError

    /**
     * Vérifie si les données sont chargées
     */
    fun isLoaded(): Boolean = isDataLoaded
}