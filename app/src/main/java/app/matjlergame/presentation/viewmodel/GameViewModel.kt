package app.matjlergame.presentation.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.GameState
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.model.TileStatus
import app.matjlergame.domain.usecase.CalculateTileStatusesUseCase
import app.matjlergame.domain.usecase.ValidateExpressionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GameViewModel(
    private val level: Level,
    private val validateExpressionUseCase: ValidateExpressionUseCase,
    private val calculateTileStatusesUseCase: CalculateTileStatusesUseCase,
    private val onLevelCompleted: (won: Boolean, attempts: Int) -> Unit,
    private val totalLevels: Int,
    private val mode: GameMode? = null,
    private val sharedPreferences: SharedPreferences? = null
) : ViewModel() {

    private val gameStateKey = if (mode != null) {
        "game_state_${mode.name}_level_${level.number}"
    } else {
        "game_state_level_${level.number}"
    }

    var gameState by mutableStateOf(loadGameState())
        private set

    private fun loadGameState(): GameState {
        if (sharedPreferences == null) {
            return createInitialGameState()
        }

        val savedJson = sharedPreferences.getString(gameStateKey, null)

        return if (savedJson != null) {
            try {
                parseGameStateFromJson(savedJson)
            } catch (e: Exception) {
                createInitialGameState()
            }
        } else {
            createInitialGameState()
        }
    }

    private fun createInitialGameState(): GameState {
        return GameState(
            guesses = List(level.maxGuesses) { List(level.slots) { "" } },
            tileStatuses = List(level.maxGuesses) { List(level.slots) { TileStatus.EMPTY } }
        )
    }

    private fun parseGameStateFromJson(json: String): GameState {
        val jsonObject = JSONObject(json)

        val guessesArray = jsonObject.getJSONArray("guesses")
        val guesses = mutableListOf<List<String>>()
        for (i in 0 until guessesArray.length()) {
            val rowArray = guessesArray.getJSONArray(i)
            val row = mutableListOf<String>()
            for (j in 0 until rowArray.length()) {
                row.add(rowArray.getString(j))
            }
            guesses.add(row)
        }

        val statusesArray = jsonObject.getJSONArray("tileStatuses")
        val tileStatuses = mutableListOf<List<TileStatus>>()
        for (i in 0 until statusesArray.length()) {
            val rowArray = statusesArray.getJSONArray(i)
            val row = mutableListOf<TileStatus>()
            for (j in 0 until rowArray.length()) {
                row.add(TileStatus.valueOf(rowArray.getString(j)))
            }
            tileStatuses.add(row)
        }

        return GameState(
            guesses = guesses,
            tileStatuses = tileStatuses,
            currentGuess = jsonObject.getInt("currentGuess"),
            currentPos = jsonObject.getInt("currentPos"),
            gameOver = jsonObject.getBoolean("gameOver"),
            isWon = jsonObject.getBoolean("isWon"),
            message = jsonObject.getString("message"),
            isShaking = false
        )
    }

    private fun saveGameState() {
        if (sharedPreferences == null) return

        val jsonObject = JSONObject()

        val guessesArray = JSONArray()
        for (row in gameState.guesses) {
            val rowArray = JSONArray()
            for (cell in row) {
                rowArray.put(cell)
            }
            guessesArray.put(rowArray)
        }
        jsonObject.put("guesses", guessesArray)

        val statusesArray = JSONArray()
        for (row in gameState.tileStatuses) {
            val rowArray = JSONArray()
            for (status in row) {
                rowArray.put(status.name)
            }
            statusesArray.put(rowArray)
        }
        jsonObject.put("tileStatuses", statusesArray)

        jsonObject.put("currentGuess", gameState.currentGuess)
        jsonObject.put("currentPos", gameState.currentPos)
        jsonObject.put("gameOver", gameState.gameOver)
        jsonObject.put("isWon", gameState.isWon)
        jsonObject.put("message", gameState.message)

        sharedPreferences.edit().putString(gameStateKey, jsonObject.toString()).apply()
    }

    private fun clearSavedState() {
        sharedPreferences?.edit()?.remove(gameStateKey)?.apply()
    }

    fun handleKeyPress(key: String) {
        if (gameState.gameOver) return

        when (key) {
            "ENTER" -> submitGuess()
            "DELETE" -> deleteLastChar()
            else -> addChar(key)
        }
    }

    private fun addChar(char: String) {
        if (gameState.currentPos < level.slots && "0123456789+-*/()".contains(char)) {
            val newGuesses = gameState.guesses.toMutableList()
            val currentRow = newGuesses[gameState.currentGuess].toMutableList()
            currentRow[gameState.currentPos] = char
            newGuesses[gameState.currentGuess] = currentRow

            gameState = gameState.copy(
                guesses = newGuesses,
                currentPos = gameState.currentPos + 1
            )
            saveGameState()
        }
    }

    private fun deleteLastChar() {
        if (gameState.currentPos > 0) {
            val newGuesses = gameState.guesses.toMutableList()
            val currentRow = newGuesses[gameState.currentGuess].toMutableList()
            currentRow[gameState.currentPos - 1] = ""
            newGuesses[gameState.currentGuess] = currentRow

            gameState = gameState.copy(
                guesses = newGuesses,
                currentPos = gameState.currentPos - 1
            )
            saveGameState()
        }
    }

    private fun submitGuess() {
        if (gameState.currentPos < level.slots) {
            showMessage("Trop court! ${level.slots} caractères requis.")
            shakeRow()
            return
        }

        val guessStr = gameState.guesses[gameState.currentGuess].joinToString("")

        if (!validateExpressionUseCase(guessStr, level.target)) {
            showMessage("Calcul invalide ou ≠ ${level.target}")
            shakeRow()
            return
        }

        revealColors(guessStr)
    }

    private fun revealColors(guessStr: String) {
        val statuses = calculateTileStatusesUseCase(guessStr, level.solution, level.slots)

        viewModelScope.launch {
            val currentRow = gameState.currentGuess

            for (i in 0 until level.slots) {
                delay(150)
                val newTileStatuses = gameState.tileStatuses.toMutableList()
                val rowStatuses = newTileStatuses[currentRow].toMutableList()
                rowStatuses[i] = statuses[i]
                newTileStatuses[currentRow] = rowStatuses
                gameState = gameState.copy(tileStatuses = newTileStatuses)
                saveGameState()
            }

            delay(300)

            val attempts = gameState.currentGuess + 1

            when {
                guessStr == level.solution -> {
                    val message = "🎉 Bravo! Niveau réussi en $attempts essais!\n\nRevenez demain pour un nouveau défi 🌟"
                    showMessage(message, permanent = true)
                    gameState = gameState.copy(gameOver = true, isWon = true)
                    saveGameState()

                    delay(2000)
                    clearSavedState() // Effacer après victoire
                    onLevelCompleted(true, attempts)
                }

                gameState.currentGuess == level.maxGuesses - 1 -> {
                    val message = "😔 Perdu! Solution: ${level.solution}\n\nRevenez demain pour un nouveau défi 🌟"
                    showMessage(message, permanent = true)
                    gameState = gameState.copy(gameOver = true, isWon = false)
                    saveGameState()

                    delay(2000)
                    clearSavedState() // Effacer après défaite
                    onLevelCompleted(false, attempts)
                }

                else -> {
                    gameState = gameState.copy(
                        currentGuess = gameState.currentGuess + 1,
                        currentPos = 0
                    )
                    saveGameState()
                }
            }
        }
    }

    private fun showMessage(text: String, permanent: Boolean = false) {
        gameState = gameState.copy(message = text)
        if (!permanent) {
            viewModelScope.launch {
                delay(2500)
                if (gameState.message == text) {
                    gameState = gameState.copy(message = "")
                    saveGameState()
                }
            }
        }
    }

    private fun shakeRow() {
        gameState = gameState.copy(isShaking = true)
        viewModelScope.launch {
            delay(500)
            gameState = gameState.copy(isShaking = false)
        }
    }

    fun resetLevel() {
        clearSavedState()
        gameState = createInitialGameState()
    }
}