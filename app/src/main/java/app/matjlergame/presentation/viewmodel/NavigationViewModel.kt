package app.matjlergame.presentation.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Screen

class NavigationViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    companion object {
        private const val KEY_CURRENT_SCREEN = "current_screen"
        private const val KEY_SELECTED_MODE = "selected_mode"
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }

    var currentScreen by mutableStateOf(getInitialScreen())
        private set

    var selectedMode by mutableStateOf<GameMode?>(loadMode())
        private set

    var currentLevelNumber by mutableStateOf(loadLevelNumber())
        private set

    // Détermine l'écran initial : HOW_TO_PLAY si première fois, sinon MODE_SELECT
    private fun getInitialScreen(): Screen {
        val isFirstLaunch = sharedPreferences.getBoolean(KEY_IS_FIRST_LAUNCH, true)

        if (isFirstLaunch) {
            // Marquer comme déjà lancé
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply()
            return Screen.HOW_TO_PLAY
        }

        // Si ce n'est pas la première fois, charger l'écran sauvegardé
        return loadScreen()
    }

    private fun loadScreen(): Screen {
        val screenName = sharedPreferences.getString(KEY_CURRENT_SCREEN, Screen.MODE_SELECT.name)
        return try {
            Screen.valueOf(screenName ?: Screen.MODE_SELECT.name)
        } catch (e: Exception) {
            Screen.MODE_SELECT
        }
    }

    private fun loadMode(): GameMode? {
        val modeName = sharedPreferences.getString(KEY_SELECTED_MODE, null)
        return try {
            modeName?.let { GameMode.valueOf(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadLevelNumber(): Int {
        return sharedPreferences.getInt(KEY_CURRENT_LEVEL, 1)
    }

    private fun saveState() {
        sharedPreferences.edit().apply {
            putString(KEY_CURRENT_SCREEN, currentScreen.name)
            putString(KEY_SELECTED_MODE, selectedMode?.name)
            putInt(KEY_CURRENT_LEVEL, currentLevelNumber)
            apply()
        }
    }

    fun navigateToGame(mode: GameMode) {
        selectedMode = mode
        currentLevelNumber = 1
        currentScreen = Screen.GAME
        saveState()
    }

    fun navigateToHowToPlay() {
        currentScreen = Screen.HOW_TO_PLAY
        saveState()
    }

    fun navigateToNextLevel() {
        currentLevelNumber++
        currentScreen = Screen.GAME
        saveState()
    }

    fun navigateBack() {
        currentScreen = when(currentScreen) {
            Screen.HOW_TO_PLAY -> Screen.MODE_SELECT
            Screen.GAME -> {
                currentLevelNumber = 1
                Screen.MODE_SELECT
            }
            Screen.MODE_SELECT -> Screen.MODE_SELECT
            Screen.LEVEL_SELECT -> Screen.MODE_SELECT
        }
        saveState()
    }

    fun resetToFirstLevel() {
        currentLevelNumber = 1
        saveState()
    }

    fun clearState() {
        currentScreen = Screen.MODE_SELECT
        selectedMode = null
        currentLevelNumber = 1
        sharedPreferences.edit().clear().apply()
        // Remettre la clé first launch à true si on veut revoir le tutorial
        // sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, true).apply()
    }
}