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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GameViewModel(
    private val level: Level,
    private val validateExpressionUseCase: ValidateExpressionUseCase,
    private val calculateTileStatusesUseCase: CalculateTileStatusesUseCase,
    private val onLevelCompleted: (won: Boolean, attempts: Int, day: java.time.LocalDate) -> Unit,
    private val totalLevels: Int,
    private val mode: GameMode? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val playerRepository: app.matjlergame.data.repository.PlayerRepository? = null
) : ViewModel() {

    private val playDay = app.matjlergame.domain.model.PlayerPeriods.today()
    private val startedAt = mode?.let { playerRepository?.start(it, playDay) } ?: System.currentTimeMillis()
    private var submittedDuration = 0L
    private val responseTimer = app.matjlergame.domain.model.GameTimer(startedAt,
        System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime())
    private val gameStateKey = if (mode != null) {
        "game_state_${playDay}_${mode.name}_level_${level.number}"
    } else {
        "game_state_level_${level.number}"
    }

    var gameState by mutableStateOf(loadGameState())
        private set

    private var timerStopped = gameState.isWon || (gameState.gameOver && gameState.currentGuess == 5)
    var elapsedTimeMs by mutableStateOf(responseTimer.elapsed(android.os.SystemClock.elapsedRealtime()))
        private set
    private fun freezeTimer() {
        elapsedTimeMs = responseTimer.stop(android.os.SystemClock.elapsedRealtime())
        timerStopped = true
    }

    // null = pas de dialog
    // 5   = proposer vidéo pour ligne 5
    // 6   = proposer vidéo pour ligne 6
    var pendingExtraRow by mutableStateOf<Int?>(null)
        private set

    private var isRevealing = false
    fun close() { viewModelScope.cancel() }

    /** Stores the result locally, then pushes it to the global leaderboard in the background, silently. */
    private fun recordResult(won: Boolean, durationMs: Long) {
        val mode = mode ?: return
        playerRepository?.record(mode, playDay, won, durationMs)
        val resultKey = "result_${mode.name}_$playDay"
        // Frozen alongside the score; reopening or sharing never restarts the timer.
        if (sharedPreferences?.contains("${resultKey}_duration") == false) {
            sharedPreferences.edit().putLong("${resultKey}_duration", durationMs.coerceAtLeast(1))
                .putInt("${resultKey}_level", level.number).commit()
        }
        playerRepository?.let { repository ->
            // Not tied to viewModelScope: the game screen closes right after the result.
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { repository.syncScores() }
            }
        }
    }

    init {
        if (timerStopped) {
            responseTimer.restoreFinished(sharedPreferences?.getLong("${gameStateKey}_duration", 0L) ?: 0L)
            elapsedTimeMs = responseTimer.elapsed(android.os.SystemClock.elapsedRealtime())
        }
        viewModelScope.launch {
            while (!timerStopped) {
                elapsedTimeMs = responseTimer.elapsed(android.os.SystemClock.elapsedRealtime())
                delay(250)
            }
        }
        // Recover a completed win if the process stopped during the result animation.
        if (gameState.isWon) {
            recordResult(true, sharedPreferences?.getLong("${gameStateKey}_duration", 1L) ?: 1L)
            viewModelScope.launch { onLevelCompleted(true, gameState.currentGuess + 1, playDay) }
        }
        // Restaurer pendingExtraRow si le jeu était bloqué (gameOver sauvegardé, pendingExtraRow non sauvegardé)
        if (gameState.gameOver && !gameState.isWon) {
            when {
                gameState.guesses.size == 4 && gameState.currentGuess == 3 -> pendingExtraRow = 5
                gameState.guesses.size == 5 && gameState.currentGuess == 4 -> pendingExtraRow = 6
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────

    private fun loadGameState(): GameState {
        if (sharedPreferences == null) return createInitialGameState()
        val savedJson = sharedPreferences.getString(gameStateKey, null)
        return if (savedJson != null) {
            try { parseGameStateFromJson(savedJson) } catch (e: Exception) { createInitialGameState() }
        } else {
            createInitialGameState()
        }
    }

    // ✅ 4 lignes au départ
    private fun createInitialGameState(): GameState {
        return GameState(
            guesses      = List(4) { List(level.slots) { "" } },
            tileStatuses = List(4) { List(level.slots) { TileStatus.EMPTY } }
        )
    }

    private fun parseGameStateFromJson(json: String): GameState {
        val obj          = JSONObject(json)
        val guessesArray = obj.getJSONArray("guesses")
        val guesses      = mutableListOf<List<String>>()
        for (i in 0 until guessesArray.length()) {
            val rowArr = guessesArray.getJSONArray(i)
            val row    = mutableListOf<String>()
            for (j in 0 until rowArr.length()) row.add(rowArr.getString(j))
            guesses.add(row)
        }
        val statusesArray = obj.getJSONArray("tileStatuses")
        val tileStatuses  = mutableListOf<List<TileStatus>>()
        for (i in 0 until statusesArray.length()) {
            val rowArr = statusesArray.getJSONArray(i)
            val row    = mutableListOf<TileStatus>()
            for (j in 0 until rowArr.length()) row.add(TileStatus.valueOf(rowArr.getString(j)))
            tileStatuses.add(row)
        }
        return GameState(
            guesses      = guesses,
            tileStatuses = tileStatuses,
            currentGuess = obj.getInt("currentGuess"),
            currentPos   = obj.getInt("currentPos"),
            gameOver     = obj.getBoolean("gameOver"),
            isWon        = obj.getBoolean("isWon"),
            message      = obj.getString("message"),
            isShaking    = false
        )
    }

    private fun saveGameState() {
        if (sharedPreferences == null) return
        val obj          = JSONObject()
        val guessesArray = JSONArray()
        for (row in gameState.guesses) {
            val rowArr = JSONArray(); for (cell in row) rowArr.put(cell); guessesArray.put(rowArr)
        }
        obj.put("guesses", guessesArray)
        val statusesArray = JSONArray()
        for (row in gameState.tileStatuses) {
            val rowArr = JSONArray(); for (s in row) rowArr.put(s.name); statusesArray.put(rowArr)
        }
        obj.put("tileStatuses", statusesArray)
        obj.put("currentGuess", gameState.currentGuess)
        obj.put("currentPos",   gameState.currentPos)
        obj.put("gameOver",     gameState.gameOver)
        obj.put("isWon",        gameState.isWon)
        obj.put("message",      gameState.message)
        val edit = sharedPreferences.edit().putString(gameStateKey, obj.toString())
            .putLong("${gameStateKey}_duration", submittedDuration.coerceAtLeast(1))
        if (gameState.isWon) edit.commit() else edit.apply()
    }

    private fun clearSavedState() {
        sharedPreferences?.edit()?.remove(gameStateKey)?.apply()
    }

    // ─────────────────────────────────────────────────────────────
    // CLAVIER
    // ─────────────────────────────────────────────────────────────

    fun handleKeyPress(key: String) {
        if (gameState.gameOver || isRevealing) return
        when (key) {
            "ENTER"  -> submitGuess()
            "DELETE" -> deleteLastChar()
            else     -> addChar(key)
        }
    }

    private fun addChar(char: String) {
        if (gameState.currentPos < level.slots && "0123456789+-*/()".contains(char)) {
            val newGuesses  = gameState.guesses.toMutableList()
            val currentRow  = newGuesses[gameState.currentGuess].toMutableList()
            currentRow[gameState.currentPos] = char
            newGuesses[gameState.currentGuess] = currentRow
            gameState = gameState.copy(guesses = newGuesses, currentPos = gameState.currentPos + 1)
            saveGameState()
        }
    }

    private fun deleteLastChar() {
        if (gameState.currentPos > 0) {
            val newGuesses = gameState.guesses.toMutableList()
            val currentRow = newGuesses[gameState.currentGuess].toMutableList()
            currentRow[gameState.currentPos - 1] = ""
            newGuesses[gameState.currentGuess] = currentRow
            gameState = gameState.copy(guesses = newGuesses, currentPos = gameState.currentPos - 1)
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
        if (guessStr == level.solution) freezeTimer()
        else if (gameState.currentGuess == 5) freezeTimer()
        submittedDuration = responseTimer.elapsed(android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1)
        revealColors(guessStr)
    }


    private fun revealColors(guessStr: String) {
        isRevealing = true
        val statuses   = calculateTileStatusesUseCase(guessStr, level.solution, level.slots)
        val currentRow = gameState.currentGuess

        viewModelScope.launch {
            for (i in 0 until level.slots) {
                delay(150)
                val newStatuses = gameState.tileStatuses.toMutableList()
                val rowStatuses = newStatuses[currentRow].toMutableList()
                rowStatuses[i]  = statuses[i]
                newStatuses[currentRow] = rowStatuses
                gameState = gameState.copy(tileStatuses = newStatuses)
                saveGameState()
            }

            delay(300)

            val attempts = currentRow + 1

            when {
                guessStr == level.solution -> {
                    val msg = when (attempts) {
                        in 1..4 -> "🎉 Bravo! Résolu en $attempts essais!\n\nRevenez demain pour un nouveau défi 🌟"
                        5       -> "🎉 Bravo! Résolu avec 1 aide vidéo!\n\nRevenez demain pour un nouveau défi 🌟"
                        6       -> "🎉 Bravo! Résolu avec 2 aides vidéo!\n\nRevenez demain pour un nouveau défi 🌟"
                        else    -> "🎉 Bravo! Résolu en $attempts essais!"
                    }
                    showMessage(msg, permanent = true)
                    gameState = gameState.copy(gameOver = true, isWon = true)
                    saveGameState()
                    recordResult(true, submittedDuration)
                    delay(2000)
                    onLevelCompleted(true, attempts, playDay)
                }

                //  Échec ligne 4 (index 3) → proposer vidéo pour ligne 5
                currentRow == 3 -> {
                    gameState = gameState.copy(gameOver = true, isWon = false)
                    saveGameState()
                    pendingExtraRow = 5
                }

                //  Échec ligne 5 (index 4) → proposer vidéo pour ligne 6
                currentRow == 4 -> {
                    gameState = gameState.copy(gameOver = true, isWon = false)
                    saveGameState()
                    pendingExtraRow = 6
                }

                //  Échec ligne 6 (index 5) → perdu définitivement
                currentRow == 5 -> {
                    showMessage("😔 Perdu ! La solution était : ${level.solution}", permanent = true)
                    gameState = gameState.copy(gameOver = true, isWon = false)
                    saveGameState()
                    delay(2000)
                    clearSavedState()
                    freezeTimer()
                    recordResult(false, submittedDuration)
                    onLevelCompleted(false, 6, playDay)
                }

                else -> {
                    isRevealing = false
                    gameState = gameState.copy(
                        currentGuess = gameState.currentGuess + 1,
                        currentPos   = 0
                    )
                    saveGameState()
                }
            }
        }
    }

    fun clearPendingExtraRow() {
        pendingExtraRow = null
    }

    fun addExtraTry() {
        val newGuesses      = gameState.guesses.toMutableList()
        val newTileStatuses = gameState.tileStatuses.toMutableList()
        newGuesses.add(List(level.slots) { "" })
        newTileStatuses.add(List(level.slots) { TileStatus.EMPTY })
        val newRow = newGuesses.size - 1
        isRevealing = false
        gameState = gameState.copy(
            guesses      = newGuesses,
            tileStatuses = newTileStatuses,
            currentGuess = newRow,
            currentPos   = 0,
            gameOver     = false,
            message      = ""
        )
        pendingExtraRow = null
        saveGameState()
    }

    fun finishGameAsLost() {
        pendingExtraRow = null
        val attempts = gameState.currentGuess + 1
        viewModelScope.launch {
            freezeTimer()
            clearSavedState()
            recordResult(false, elapsedTimeMs.coerceAtLeast(1))
            onLevelCompleted(false, attempts, playDay)
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
        isRevealing = false
        gameState = createInitialGameState()
    }
}