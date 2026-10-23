package app.matjlergame

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.matjlergame.presentation.ui.QuickStartDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickStartDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun practiceExplainsFeedbackBeforeCompleting() {
        var finished = false
        compose.setContent { MaterialTheme { QuickStartDialog { finished = true } } }
        compose.onNodeWithText("Tester 10 + 5").performClick()
        compose.onNodeWithText("Vert : le 1 et le + sont bien placés. Gris : le 0 et le 5 ne sont pas dans la réponse.").assertIsDisplayed()
        compose.onNodeWithText("Essayer 12 + 3").performClick()
        compose.onNodeWithText("1 2 + 3 = 15").assertIsDisplayed()
        compose.onNodeWithText("Jouer le défi du jour").performClick()
        compose.runOnIdle { assertTrue(finished) }
    }

    @Test fun practiceCanBeSkipped() {
        var dismissed = false
        compose.setContent { MaterialTheme { QuickStartDialog { dismissed = true } } }
        compose.onNodeWithText("Plus tard").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
