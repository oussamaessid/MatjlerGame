package app.matjlergame.presentation.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.matjlergame.sharing.ResultShare
import app.matjlergame.sharing.ShareResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareResultButtons(result: ShareResult) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    fun share(image: Boolean) {
        if(busy) return
        busy=true
        scope.launch {
            try {
                val intent=withContext(Dispatchers.IO) { if(image) ResultShare.imageIntent(context,result) else ResultShare.textIntent(result) }
                context.startActivity(Intent.createChooser(intent,if(result.english) "Share result" else "Partager le résultat"))
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { Toast.makeText(context,if(result.english) "Sharing unavailable. Please try again." else "Partage indisponible. Réessayez.",Toast.LENGTH_LONG).show() }
            finally { busy=false }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        // Text sharing keeps the Play Store URL visible and clickable in messaging apps.
        // A bitmap attachment itself cannot carry a hyperlink, so image sharing is optional.
        Button(onClick={share(false)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Share,contentDescription=null)
            Spacer(Modifier.width(8.dp))
            Text(if(busy) {if(result.english) "Preparing…" else "Préparation…"} else {if(result.english) "Share my result" else "Partager mon résultat"})
        }
        TextButton(onClick={share(true)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
            Text(if(result.english) "Share as image" else "Partager comme image")
        }
    }
}
