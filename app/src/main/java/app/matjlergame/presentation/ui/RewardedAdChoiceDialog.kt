package app.matjlergame.presentation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun RewardedAdChoiceDialog(
    onWatchExtraTryAd: () -> Unit,
    onWatchSolutionAd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "Dernier essai disponible ! 🎁",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Regardez une courte vidéo pour obtenir un essai supplémentaire. Voulez-vous continuer ?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onWatchExtraTryAd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Watch Video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}