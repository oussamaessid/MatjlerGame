package app.matjlergame.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun NoInternetDialog(
    title: String = "Pas de connexion",
    message: String = "Impossible de charger les données.\n\nVérifiez votre connexion internet et réessayez.",
    showRetry: Boolean = true,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icône
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF9800)
                )

                // Titre
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                // Message
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Boutons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showRetry) {
                        Button(
                            onClick = {
                                onDismiss()
                                onRetry()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF667EEA)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Réessayer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF667EEA)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (showRetry) "Annuler" else "Fermer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialogue spécifique pour les timeouts de chargement
 */
@Composable
fun LoadingTimeoutDialog(
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    NoInternetDialog(
        title = "Temps de chargement dépassé",
        message = "Le chargement prend trop de temps.\n\nVérifiez votre connexion internet.",
        showRetry = true,
        onDismiss = onBack,
        onRetry = onRetry
    )
}

/**
 * Dialogue d'erreur générique pour le chargement
 */
@Composable
fun LoadingErrorDialog(
    errorMessage: String = "Une erreur est survenue lors du chargement des données.",
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    NoInternetDialog(
        title = "Erreur de chargement",
        message = errorMessage,
        showRetry = true,
        onDismiss = onBack,
        onRetry = onRetry
    )
}