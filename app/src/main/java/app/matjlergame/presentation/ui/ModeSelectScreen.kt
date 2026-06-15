package app.matjlergame.presentation.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// ✅ AUCUNE vérification internet ici.
// AUCUN dialog internet ici.
// La vérification se fait UNIQUEMENT dans MainActivity → onModeSelected.

@Composable
fun ModeSelectScreen(
    adManager: AdManager,
    isLoading: Boolean = false,
    onModeSelected: (GameMode) -> Unit,
    onHowToPlayClicked: () -> Unit = {}
) {
    // ✅ SUPPRIMÉ : showNoInternetDialog local — c'était la cause du bug
    // Le dialog internet est géré uniquement dans MainActivity

    val configuration = LocalConfiguration.current
    val screenHeight  = configuration.screenHeightDp.dp
    val screenWidth   = configuration.screenWidthDp.dp
    val ctx = LocalContext.current

    val cardHeight  = when {
        screenHeight < 600.dp -> 68.dp
        screenHeight < 700.dp -> 78.dp
        screenHeight < 800.dp -> 88.dp
        else                  -> 100.dp
    }
    val cardSpacing = when {
        screenHeight < 600.dp -> 8.dp
        screenHeight < 700.dp -> 14.dp
        screenHeight < 800.dp -> 22.dp
        else                  -> 30.dp
    }
    val cardWidth   = minOf(300.dp, screenWidth - 48.dp)
    val cardEmojiSz = if (screenHeight < 700.dp) 28.sp else 40.sp
    val cardTitleSz = if (screenHeight < 700.dp) 18.sp else 24.sp
    val cardDescSz  = if (screenHeight < 700.dp) 11.sp else 14.sp
    val cardPadding = if (screenHeight < 700.dp) 10.dp else 20.dp

    var bannerLoaded by remember { mutableStateOf(false) }
    val bannerAdView = remember {
        AdView(ctx).also { view ->
            view.adUnitId = AdManager.BANNER_MODE_SELECT_AD_UNIT_ID
            view.setAdSize(AdSize.BANNER)
            view.adListener = object : AdListener() {
                override fun onAdLoaded() { bannerLoaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { bannerLoaded = false }
            }
            view.loadAd(AdRequest.Builder().build())
        }
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

            // ── Header ──────────────────────────────────────────────
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
                        imageVector        = Icons.Filled.Info,
                        contentDescription = "Comment jouer",
                        tint               = Color.White,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }

            // ── Contenu central ─────────────────────────────────────
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(cardSpacing),
                    modifier            = Modifier.padding(24.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "title")
                    val titleScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue  = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "titleScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.scale(titleScale)
                    ) {
                        Text(
                            text          = "🎯 Equal To",
                            fontSize      = 48.sp,
                            fontWeight    = FontWeight.Black,
                            color         = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text       = "Math Puzzle Game",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    val cardsEnabled = !isLoading

                    // ✅ Les cards appellent directement onModeSelected
                    // La vérification internet est dans MainActivity, pas ici
                    ModeCard(
                        title       = "FACILE",
                        emoji       = "🌱",
                        description = "Pour débuter",
                        gradient    = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
                        enabled     = cardsEnabled,
                        cardHeight  = cardHeight,
                        cardWidth   = cardWidth,
                        emojiSize   = cardEmojiSz,
                        titleSize   = cardTitleSz,
                        descSize    = cardDescSz,
                        cardPadding = cardPadding,
                        onClick     = { onModeSelected(GameMode.EASY) }
                    )

                    ModeCard(
                        title       = "MOYEN",
                        emoji       = "⚡",
                        description = "Un peu plus dur",
                        gradient    = listOf(Color(0xFFFF9800), Color(0xFFFFB74D)),
                        enabled     = cardsEnabled,
                        cardHeight  = cardHeight,
                        cardWidth   = cardWidth,
                        emojiSize   = cardEmojiSz,
                        titleSize   = cardTitleSz,
                        descSize    = cardDescSz,
                        cardPadding = cardPadding,
                        onClick     = { onModeSelected(GameMode.MEDIUM) }
                    )

                    ModeCard(
                        title       = "DIFFICILE",
                        emoji       = "🔥",
                        description = "Expert seulement",
                        gradient    = listOf(Color(0xFFF44336), Color(0xFFE91E63)),
                        enabled     = cardsEnabled,
                        cardHeight  = cardHeight,
                        cardWidth   = cardWidth,
                        emojiSize   = cardEmojiSz,
                        titleSize   = cardTitleSz,
                        descSize    = cardDescSz,
                        cardPadding = cardPadding,
                        onClick     = { onModeSelected(GameMode.HARD) }
                    )
                }
            }

            // ── Bannière pub ─────────────────────────────────────────
            if (bannerLoaded) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .navigationBarsPadding()
                        .padding(top = 4.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory  = { bannerAdView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }

        // ── Overlay chargement ───────────────────────────────────────
        if (isLoading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(64.dp),
                        color       = Color.White,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text       = "Chargement...",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title       : String,
    emoji       : String,
    description : String,
    gradient    : List<Color>,
    enabled     : Boolean = true,
    cardHeight  : Dp = 100.dp,
    cardWidth   : Dp = 300.dp,
    emojiSize   : TextUnit = 40.sp,
    titleSize   : TextUnit = 24.sp,
    descSize    : TextUnit = 14.sp,
    cardPadding : Dp = 20.dp,
    onClick     : () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "cardScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(gradient)
                } else {
                    Brush.horizontalGradient(
                        listOf(gradient[0].copy(alpha = 0.5f), gradient[1].copy(alpha = 0.5f))
                    )
                }
            )
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            }
            .padding(cardPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text     = emoji,
                fontSize = emojiSize,
                modifier = Modifier.scale(if (enabled) 1f else 0.8f)
            )
            Column {
                Text(
                    text       = title,
                    fontSize   = titleSize,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = if (enabled) 1f else 0.6f)
                )
                Text(
                    text     = description,
                    fontSize = descSize,
                    color    = Color.White.copy(alpha = if (enabled) 0.9f else 0.5f)
                )
            }
        }
    }
}