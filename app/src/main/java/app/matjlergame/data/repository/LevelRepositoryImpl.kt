package app.matjlergame.data.repository

import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.repository.LevelRepository
import org.json.JSONArray
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class LevelRepositoryImpl : LevelRepository {

    private val jsonUrl = "https://raw.githubusercontent.com/oussamaessid/MatjlerData/refs/heads/main/MatjlerData.json"

    private val cache = ConcurrentHashMap<GameMode, List<Level>>()
    private var isDataLoaded = false

    init {
        Thread {
            loadDataFromApi()
        }.start()
    }

    private fun loadDataFromApi() {
        try {
            val jsonText = URL(jsonUrl).readText()
            val jsonArray = JSONArray(jsonText)

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
        } catch (e: Exception) {
            e.printStackTrace()
            loadDefaultData()
        }
    }

    private fun loadDefaultData() {
        cache[GameMode.EASY] = listOf(
            Level(1, 10, "5+5", 3, 6),
            Level(2, 15, "8+7", 3, 6),
            Level(3, 20, "9+11", 4, 6),
            Level(4, 25, "15+10", 5, 6),
            Level(5, 30, "20+10", 5, 6)
        )

        cache[GameMode.MEDIUM] = listOf(
            Level(1, 50, "40+12-2", 7, 6),
            Level(2, 75, "50+30-5", 7, 6),
            Level(3, 100, "80+25-5", 7, 6),
            Level(4, 120, "100+25-5", 8, 6),
            Level(5, 150, "120+35-5", 8, 6)
        )

        cache[GameMode.HARD] = listOf(
            Level(1, 200, "180+25-5", 8, 6),
            Level(2, 278, "250+30-2", 8, 6),
            Level(3, 350, "300+55-5", 8, 6),
            Level(4, 500, "450+60-10", 9, 6),
            Level(5, 1000, "900+120-20", 10, 6)
        )

        isDataLoaded = true
    }

    override fun getLevelsForMode(mode: GameMode): List<Level> {
        var attempts = 0
        while (!isDataLoaded && attempts < 50) {
            Thread.sleep(100)
            attempts++
        }

        if (!isDataLoaded) {
            loadDefaultData()
        }

        return cache[mode] ?: emptyList()
    }

    override fun getLevelByNumber(mode: GameMode, levelNumber: Int): Level? {
        return getLevelsForMode(mode).find { it.number == levelNumber }
    }

    fun refreshData() {
        isDataLoaded = false
        cache.clear()
        Thread {
            loadDataFromApi()
        }.start()
    }
}