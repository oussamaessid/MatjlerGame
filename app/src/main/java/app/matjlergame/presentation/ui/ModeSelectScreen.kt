package app.matjlergame.presentation.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Vérifie la connexion internet de façon fiable avec NET_CAPABILITY_VALIDATED.
 * Sans VALIDATED, un Wi-Fi sans accès réel (captive portal) serait vu comme connecté.
 */
private fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun ModeSelectScreen(
    adManager: AdManager,
    isLoading: Boolean = false,
    onModeSelected: (GameMode) -> Unit,
    onHowToPlayClicked: () -> Unit = {}
) {
    val context = LocalContext.current

    // ✅ FIX : null = pas encore vérifié → le dialog n'est JAMAIS affiché sur null
    // Le flash "NoInternetDialog" au premier lancement venait du fait que
    // l'état initial était false, avant que la vérification réseau ne tourne.
    var isConnected by remember { mutableStateOf<Boolean?>(null) }
    var showNoInternetDialog by remember { mutableStateOf(false) }

    // Vérification lancée une seule fois, après le premier rendu du Composable
    LaunchedEffect(Unit) {
        isConnected = checkInternet(context)
        if (isConnected == false) {
            showNoInternetDialog = true
        }
    }

    // ✅ Dialog affiché UNIQUEMENT si isConnected est explicitement false
    if (showNoInternetDialog && isConnected == false) {
        NoInternetDialog(
            onDismiss = { showNoInternetDialog = false },
            onRetry = {
                isConnected = checkInternet(context)
                showNoInternetDialog = isConnected == false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Header ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .height(56.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = onHowToPlayClicked,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Comment jouer",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "title")
                    val titleScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "titleScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(titleScale)
                    ) {
                        Text(
                            text = "🎯 Equal To",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Math Puzzle Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Cartes cliquables seulement si connexion confirmée ET pas en chargement
                    // Si isConnected == null (vérif en cours), les cartes sont grises mais pas bloquantes :
                    // au clic, on re-vérifie en temps réel avant de naviguer.
                    val cardsEnabled = !isLoading && isConnected != null

                    ModeCard(
                        title = "FACILE",
                        emoji = "🌱",
                        description = "Pour débuter",
                        gradient = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
                        enabled = cardsEnabled,
                        onClick = {
                            val connected = checkInternet(context)
                            isConnected = connected
                            if (connected) onModeSelected(GameMode.EASY)
                            else showNoInternetDialog = true
                        }
                    )

                    ModeCard(
                        title = "MOYEN",
                        emoji = "⚡",
                        description = "Un peu plus dur",
                        gradient = listOf(Color(0xFFFF9800), Color(0xFFFFB74D)),
                        enabled = cardsEnabled,
                        onClick = {
                            val connected = checkInternet(context)
                            isConnected = connected
                            if (connected) onModeSelected(GameMode.MEDIUM)
                            else showNoInternetDialog = true
                        }
                    )

                    ModeCard(
                        title = "DIFFICILE",
                        emoji = "🔥",
                        description = "Expert seulement",
                        gradient = listOf(Color(0xFFF44336), Color(0xFFE91E63)),
                        enabled = cardsEnabled,
                        onClick = {
                            val connected = checkInternet(context)
                            isConnected = connected
                            if (connected) onModeSelected(GameMode.HARD)
                            else showNoInternetDialog = true
                        }
                    )
                }
            }

            // ── Bannière pub ─────────────────────────────────────────────────
            // APRÈS
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 8.dp)
                    .navigationBarsPadding(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdManager.BANNER_MODE_SELECT_AD_UNIT_ID
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = Color.White,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "Chargement...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    emoji: String,
    description: String,
    gradient: List<Color>,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(300.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(gradient)
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            gradient[0].copy(alpha = 0.5f),
                            gradient[1].copy(alpha = 0.5f)
                        )
                    )
                }
            )
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            }
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 40.sp,
                modifier = Modifier.scale(if (enabled) 1f else 0.8f)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.6f)
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = if (enabled) 0.9f else 0.5f)
                )
            }
        }
    }
}