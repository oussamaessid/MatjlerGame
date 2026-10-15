package app.matjlergame

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.LeaderboardPlayer
import app.matjlergame.domain.model.RankingPeriod
import app.matjlergame.presentation.ui.LeaderboardLayout
import app.matjlergame.presentation.ui.LeaderboardViewState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LeaderboardScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyBoardShowsNoErrorAndOffersRefresh() {
        var retries = 0
        compose.setContent {
            MaterialTheme { LeaderboardLayout(LeaderboardViewState(period = "2026-10-14"),
                {}, {}, {}, { retries++ }, {}) }
        }
        compose.onNodeWithText("14 octobre 2026", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Classement indisponible").assertDoesNotExist()
        compose.onNodeWithText("Réessayer").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test fun currentPlayerRowIsHighlighted() {
        val players = listOf(LeaderboardPlayer("a", "Mina", 8, 2, 155400), LeaderboardPlayer("me", "Moi", 15, 1, 170100))
        compose.setContent {
            MaterialTheme { LeaderboardLayout(LeaderboardViewState(GameMode.EASY, RankingPeriod.DAY, "2026-10-14", players, me = "me"),
                {}, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("C’est vous").performScrollTo().assertIsDisplayed()
    }

    @Test fun allPeriodAndDifficultySelectorsWorkAndAreAccessible() {
        compose.setContent {
            var state by remember { mutableStateOf(LeaderboardViewState(period = "2026-10-14")) }
            MaterialTheme {
                LeaderboardLayout(state, {}, { state = state.copy(range = it, period = it.id(java.time.LocalDate.of(2026, 10, 14))) },
                    { state = state.copy(mode = it) }, {}, {})
            }
        }
        compose.onNodeWithText("Mois").performClick().assertIsSelected()
        compose.onNodeWithText("Semaine").performClick().assertIsSelected()
        compose.onNodeWithText("Du 12 octobre au 18 octobre 2026").assertExists()
        for (mode in listOf("Difficile", "Moyen", "Facile", "Général")) {
            compose.onNodeWithText(mode).performClick().assertIsSelected()
            compose.onNodeWithText("Tous les joueurs · $mode").assertExists()
        }
        compose.onNodeWithText("Aujourd’hui").performClick().assertIsSelected()
    }

    @Test fun actualResultRowsShowWinsAndTimesAndRenderLongNames() {
        // Test-only fixture: never inserted into the real Firebase project or production UI.
        val players = listOf(LeaderboardPlayer("a", "Mina", 8, 8, 155400),
            LeaderboardPlayer("b", "Sami", 15, 7, 170100),
            LeaderboardPlayer("c", "Lina", 29, 7, 181500),
            LeaderboardPlayer("d", "Un pseudo très long à vérifier", 42, 6, 151100))
        compose.setContent {
            MaterialTheme { LeaderboardLayout(LeaderboardViewState(GameMode.MEDIUM, RankingPeriod.MONTH, "2026-10", players),
                {}, {}, {}, {}, {}) }
        }
        compose.onAllNodesWithText("8 victoires")[0].assertIsDisplayed()
        compose.onAllNodesWithText("02:35.4")[0].assertIsDisplayed()
        screenshot("leaderboard-results-test-data.png")
        compose.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("RANG / JOUEUR"))
        screenshot("leaderboard-podium-test-data.png")
        compose.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("Un pseudo très long à vérifier"))
        compose.onNodeWithText("Un pseudo très long à vérifier").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), name)
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
