package app.matjlergame

import app.matjlergame.data.repository.FirebaseRequestException
import app.matjlergame.presentation.model.playerServiceMessage
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PlayerServiceErrorTest {
    @Test fun technicalMessagesAreNotExposedToPlayers() {
        val message=playerServiceMessage(FirebaseRequestException("API_KEY_ANDROID_APP_BLOCKED private-detail", "PERMISSION_DENIED", "API_KEY_ANDROID_APP_BLOCKED"))
        assertFalse(message.contains("private-detail"));assertFalse(message.contains("API_KEY"));assertTrue(message.contains("conservé"))
    }
    @Test fun missingAssetAndOfflineHaveDifferentMessages() {
        val missing=playerServiceMessage(FirebaseRequestException("MISSING_APP_CONFIGURATION","FAILED_PRECONDITION"))
        assertTrue(missing.contains("Mettez"));assertTrue(playerServiceMessage(IOException()).contains("Internet"))
    }
}
