package app.matjlergame

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.matjlergame.data.repository.PlayerRepository
import app.matjlergame.presentation.ui.PlayerScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class PlayerProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playerChoosesNameAndIconThenCreatesProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("profile_test_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        val repository = PlayerRepository(prefs)
        var completed = false
        compose.setContent {
            MaterialTheme { PlayerScreen(repository, ranking = false, setup = true) { completed = true } }
        }
        compose.onNodeWithText("Bienvenue dans Equal To").assertIsDisplayed()
        compose.onNodeWithText("Pseudo").performTextReplacement("Mina")
        compose.onNodeWithText("Ludo 01", useUnmergedTree = false).performClick()
        compose.onNodeWithText("Créer mon profil").performScrollTo().assertIsEnabled()
        val screenshot = File(context.getExternalFilesDir(null), "profile-setup.png")
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            screenshot.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithText("Créer mon profil").performClick()
        compose.waitUntil(5_000) { completed }
        assertEquals("Mina", repository.currentProfile.name)
        assertEquals(8, repository.currentProfile.avatar)
        assertEquals(repository.currentProfile, PlayerRepository(prefs).currentProfile)
    }

    @Test fun shortNameCannotBeSaved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = PlayerRepository(context.getSharedPreferences("profile_test_${UUID.randomUUID()}", Context.MODE_PRIVATE))
        compose.setContent { MaterialTheme { PlayerScreen(repository, ranking = false, setup = true) {} } }
        compose.onNodeWithText("Pseudo").performTextReplacement("x")
        compose.onNodeWithText("Créer mon profil").performScrollTo().assertIsNotEnabled()
    }
}
