package app.matjlergame
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.matjlergame.presentation.ui.ShareResultButtons
import app.matjlergame.sharing.ShareResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ShareResultButtonsTest {
    @get:Rule val compose=createComposeRule()
    @Test fun bothButtonsOpenTheSystemChooserWithoutSendingAnything() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val monitor=Instrumentation.ActivityMonitor(IntentFilter(Intent.ACTION_CHOOSER),
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true)
        instrumentation.addMonitor(monitor)
        try {
            compose.setContent {
                MaterialTheme { ShareResultButtons(ShareResult("Equal To","Joueur Test","2026-10-15","Mode Facile",true,2,5,42000)) }
            }
            compose.onNodeWithText("Partager mon résultat").performClick()
            compose.waitUntil(10000) { monitor.hits>=1 }
            compose.onNodeWithText("Partager comme image").performClick()
            compose.waitUntil(15000) { monitor.hits>=2 }
            assertEquals(2,monitor.hits)
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
