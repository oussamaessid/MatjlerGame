package app.matjlergame.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.usecase.DailyResult
import app.matjlergame.domain.usecase.Statistics

@Composable
fun DailyResultDialog(
    mode: GameMode,
    result: DailyResult,
    statistics: Statistics,
    onDismiss: () -> Unit,
    playerName: String = "Joueur",
    playerAvatar: Int = app.matjlergame.domain.model.PlayerAvatars.DEFAULT
) {
    val modeColor = when(mode) {
        GameMode.EASY -> Color(0xFF4CAF50)
        GameMode.MEDIUM -> Color(0xFFFF9800)
        GameMode.HARD -> Color(0xFFF44336)
    }

    val modeName = when(mode) {
        GameMode.EASY -> "FACILE"
        GameMode.MEDIUM -> "MOYEN"
        GameMode.HARD -> "DIFFICILE"
    }

    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(modeColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mode $modeName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (result.won) "🎉" else "😔",
                        fontSize = 56.sp
                    )

                    Text(
                        text = if (result.won) "BRAVO !" else "PERDU !",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (result.won) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    Text(
                        text = if (result.won) {
                            when (result.attempts) {
                                in 1..5 -> {
                                    // Victoire normale (lignes 1-5)
                                    "Résolu en ${result.attempts}/5 essais"
                                }
                                6 -> {
                                    // Victoire avec aide vidéo (ligne 6)
                                    "Résolu avec aide vidéo\n(essai bonus)"
                                }
                                else -> {
                                    // Cas imprévu
                                    "Résolu en ${result.attempts} essais"
                                }
                            }
                        } else {
                            "Vous avez utilisé tous vos 5 essais"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }

                if (result.durationMs != null) Text("⏱ " + app.matjlergame.domain.model.GameTimer.format(result.durationMs),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = modeColor)
                ShareResultButtons(app.matjlergame.sharing.ShareResult(
                    game = "Equal To", player = playerName, day = result.day,
                    // No level number: the game's date is already shown under the title.
                    category = "Mode ${modeName.lowercase().replaceFirstChar { it.uppercaseChar() }}",
                    won = result.won, attempts = result.attempts, maxAttempts = if (result.attempts > 5) result.attempts else 5,
                    durationMs = result.durationMs,
                    avatarResource = AvatarGallery.icons.getOrElse(playerAvatar) { AvatarGallery.icons[11] }))

                Divider(color = Color(0xFFE2E8F0))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📊 Vos Statistiques",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(
                            value = statistics.totalPlayed.toString(),
                            label = "Parties"
                        )

                        StatColumn(
                            value = "${statistics.winPercentage}%",
                            label = "Victoires"
                        )

                        StatColumn(
                            value = statistics.currentStreak.toString(),
                            label = "Série"
                        )

                        StatColumn(
                            value = statistics.maxStreak.toString(),
                            label = "Record"
                        )
                    }
                }

                Divider(color = Color(0xFFE2E8F0))

                Text(
                    text = "Revenez demain pour un nouveau défi ! 🌟",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = modeColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Fermer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}