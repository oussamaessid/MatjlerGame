package app.matjlergame.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A separate practice example: it never reveals or records a daily answer. */
@Composable
fun QuickStartDialog(onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 2) "Bravo, vous avez compris !" else "Votre première énigme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EXEMPLE GUIDÉ · SANS CLASSEMENT", fontSize = 11.sp)
                Text("Trouvez l’expression cachée qui donne 15. Chaque couleur est un indice.")
                if (step > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        "10+5".forEachIndexed { index, character ->
                            val correct = index == 0 || index == 2
                            Text(character.toString(), color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(if (correct) Color(0xFF397E43) else Color(0xFF666C76),
                                    RoundedCornerShape(6.dp)).padding(12.dp))
                        }
                    }
                    Text("Vert : le 1 et le + sont bien placés. Gris : le 0 et le 5 ne sont pas dans la réponse.")
                    Text("Jaune : un caractère présent, mais placé ailleurs.")
                }
                if (step == 2) {
                    Text("1 2 + 3 = 15", color = Color(0xFF397E43), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Vous avez trouvé ! Dans le défi du jour, utilisez les indices pour découvrir la réponse. Les essais de base et les aides disponibles sont affichés pendant la partie.")
                } else if (step == 1) {
                    Text("Gardez le 1 et le +, puis remplacez les deux chiffres gris.")
                } else {
                    Text("Essayons 10 + 5 : le calcul est juste, mais est-ce l’expression cachée ?")
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (step < 2) step++ else onDismiss() }) {
                Text(when (step) { 0 -> "Tester 10 + 5"; 1 -> "Essayer 12 + 3"; else -> "Jouer le défi du jour" })
            }
        },
        dismissButton = { if (step < 2) TextButton(onClick = onDismiss) { Text("Plus tard") } }
    )
}
