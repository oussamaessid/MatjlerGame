package app.matjlergame.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ApiLoadingDialog(
    loadingTimeSeconds: Int = 0,
    maxTimeSeconds: Int = 120
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val progress = (loadingTimeSeconds.toFloat() / maxTimeSeconds.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Emoji animé
                Text(
                    text = "🎯",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(scale)
                )

                // Titre
                Text(
                    text = "Chargement du défi quotidien",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                // Message
                Text(
                    text = "Récupération des données depuis le serveur...",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                // Progress indicator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF667EEA),
                        strokeWidth = 4.dp
                    )

                    if (loadingTimeSeconds > 5) {
                        Text(
                            text = "$loadingTimeSeconds s",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Barre de progression si le chargement prend du temps
                if (loadingTimeSeconds > 10) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.8f) Color(0xFFFF9800) else Color(0xFF667EEA),
                            trackColor = Color(0xFFE5E7EB)
                        )

                        Text(
                            text = when {
                                loadingTimeSeconds > 90 -> "Encore quelques secondes..."
                                loadingTimeSeconds > 60 -> "Connexion lente détectée..."
                                else -> "Chargement en cours..."
                            },
                            fontSize = 12.sp,
                            color = when {
                                loadingTimeSeconds > 90 -> Color(0xFFFF9800)
                                else -> Color(0xFF9CA3AF)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Conseil
                if (loadingTimeSeconds > 30) {
                    Text(
                        text = "💡 Assurez-vous d'avoir une connexion stable",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Version simple sans compteur de temps
 */
@Composable
fun SimpleLoadingOverlay(
    message: String = "Chargement..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF667EEA),
                strokeWidth = 6.dp
            )
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        }
    }
}