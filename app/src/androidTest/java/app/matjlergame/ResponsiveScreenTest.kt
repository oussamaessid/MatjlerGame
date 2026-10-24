package app.matjlergame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.usecase.CalculateTileStatusesUseCase
import app.matjlergame.domain.usecase.ValidateExpressionUseCase
import app.matjlergame.presentation.ui.GameScreen
import app.matjlergame.presentation.ui.ModeSelectScreen
import app.matjlergame.presentation.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResponsiveScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeKeepsAllModesVisibleWithoutScrollingOnSmallPhonesAndLargeText() {
        var width by mutableStateOf(280)
        var height by mutableStateOf(480)
        var scale by mutableStateOf(1f)
        var selected: GameMode? = null
        compose.setContent {
            val density = LocalDensity.current
            val context = LocalContext.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.dp, height.dp)) {
                        ModeSelectScreen(remember { AdManager(context) }, onModeSelected = { selected = it },
                            playerName = "Un nom de joueur particulièrement long", totalPlayed = 45, totalWon = 32)
                    }
                }
            }
        }
        for ((w, h, font) in listOf(Triple(280,480,1f), Triple(320,568,1f), Triple(360,640,1.5f), Triple(320,480,2f))) {
            compose.runOnIdle { width = w; height = h; scale = font }
            listOf("Facile", "Moyen", "Difficile").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
            compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
            compose.onNodeWithText("Difficile").performClick()
            compose.runOnIdle { assertEquals(GameMode.HARD, selected) }
        }
    }

    @Test fun landscapeKeepsHomeAndGameActionsVisible() {
        var showGame by mutableStateOf(false)
        compose.setContent {
            val context = LocalContext.current
            val level = remember { Level(1, 15, "12+3", 4, 4) }
            val viewModel = remember { GameViewModel(level, ValidateExpressionUseCase(), CalculateTileStatusesUseCase(), { _, _, _ -> }, 1) }
            DisposableEffect(viewModel) { onDispose { viewModel.close() } }
            CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(640.dp, 320.dp)) {
                        if (showGame) GameScreen(level, GameMode.EASY, viewModel, 1, remember { AdManager(context) }, {})
                        else ModeSelectScreen(remember { AdManager(context) }, onModeSelected = { showGame = true })
                    }
                }
            }
        }
        listOf("Facile", "Moyen", "Difficile").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Facile").performClick()
        compose.onNodeWithText("VALIDER").assertIsDisplayed()
        compose.onNodeWithText("7").assertIsDisplayed()
    }

    @Test fun smallGameKeepsKeyboardAndValidationAvailable() {
        var model: GameViewModel? = null
        compose.setContent {
            val context = LocalContext.current
            val level = remember { Level(1, 15, "12+3", 4, 4) }
            val viewModel = remember { GameViewModel(level, ValidateExpressionUseCase(), CalculateTileStatusesUseCase(), { _, _, _ -> }, 1) }
            DisposableEffect(viewModel) { model = viewModel; onDispose { viewModel.close() } }
            MaterialTheme {
                Box(Modifier.requiredSize(280.dp, 480.dp)) {
                    GameScreen(level, GameMode.EASY, viewModel, 1, remember { AdManager(context) }, {})
                }
            }
        }
        compose.onNodeWithText("VALIDER").assertIsDisplayed()
        compose.onNodeWithText("7").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("7", model!!.gameState.guesses[0][0]) }
    }
}
