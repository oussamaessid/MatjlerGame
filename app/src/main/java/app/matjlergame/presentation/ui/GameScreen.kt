package app.matjlergame.presentation.ui

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.model.TileStatus
import app.matjlergame.presentation.viewmodel.GameViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@Composable
fun GameScreen(
    level: Level,
    mode: GameMode,
    viewModel: GameViewModel,
    totalLevels: Int,
    adManager: AdManager,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }

    val gameState       = viewModel.gameState
    val pendingExtraRow = viewModel.pendingExtraRow
    val context         = LocalContext.current

    val modeColor = when (mode) {
        GameMode.EASY   -> Color(0xFF4CAF50)
        GameMode.MEDIUM -> Color(0xFFFF9800)
        GameMode.HARD   -> Color(0xFFF44336)
    }

    val configuration = LocalConfiguration.current
    val screenHeight  = configuration.screenHeightDp.dp
    val screenWidth   = configuration.screenWidthDp.dp

    // ── Tuiles ──────────────────────────────────────────────────────
    val tileSpacing  = if (screenWidth < 360.dp) 3.dp else 4.dp
    val totalSpacing = tileSpacing * (level.slots - 1)
    var tileSize     = (screenWidth - 32.dp - totalSpacing) / level.slots
    tileSize = when {
        screenWidth < 360.dp && level.slots >= 6 -> minOf(tileSize, 34.dp)
        screenWidth < 380.dp && level.slots >= 6 -> minOf(tileSize, 38.dp)
        screenWidth < 400.dp                     -> minOf(tileSize, 42.dp)
        else                                     -> minOf(tileSize, 48.dp)
    }

    // ── Clavier ─────────────────────────────────────────────────────
    val keyButtonHeight = when {
        screenHeight < 580.dp -> 36.dp
        screenHeight < 680.dp -> 40.dp
        screenHeight < 780.dp -> 44.dp
        else                  -> 48.dp
    }
    val keyRowSpacing = when {
        screenHeight < 580.dp -> 3.dp
        screenHeight < 680.dp -> 4.dp
        else                  -> 5.dp
    }
    val keyColSpacing = if (screenWidth < 360.dp) 3.dp else 5.dp

    // ── Textes ──────────────────────────────────────────────────────
    val headerTextSize    = when { screenWidth < 360.dp -> 11.sp; screenWidth < 400.dp -> 12.sp; else -> 13.sp }
    val targetTextSize    = when { screenWidth < 360.dp -> 18.sp; screenWidth < 400.dp -> 20.sp; else -> 22.sp }
    val objectiveTextSize = when { screenWidth < 360.dp -> 12.sp; screenWidth < 400.dp -> 13.sp; else -> 14.sp }
    val legendTextSize    = when { screenWidth < 360.dp -> 9.sp;  screenWidth < 400.dp -> 10.sp; else -> 11.sp }
    val keyTextSize       = when { screenWidth < 360.dp -> 16.sp; screenWidth < 400.dp -> 17.sp; else -> 19.sp }
    val actionKeyTextSize = when { screenWidth < 360.dp -> 13.sp; screenWidth < 400.dp -> 14.sp; else -> 15.sp }
    val iconSize          = if (screenWidth < 360.dp) 36.dp else 40.dp
    val headerPadding     = if (screenWidth < 360.dp) 8.dp  else 12.dp

    val gameSectionGap = when {
        screenHeight < 580.dp -> 4.dp
        screenHeight < 680.dp -> 6.dp
        else                  -> 8.dp
    }

    val keyStatuses = remember(gameState.guesses, gameState.tileStatuses) {
        calculateKeyStatuses(gameState.guesses, gameState.tileStatuses, level.slots)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // ✅ Interstitiel périodique toutes les 4 minutes
    LaunchedEffect(Unit) {
        adManager.loadPeriodicInterstitial()
        while (true) {
            delay(60_000L)
            if (!gameState.gameOver) {
                adManager.showPeriodicInterstitialIfReady(context as Activity)
            }
        }
    }

    LaunchedEffect(gameState.message) {
        if (gameState.message.isNotEmpty() && !gameState.gameOver) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message  = gameState.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {

            // ════════════════
            // HEADER
            // ════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(modeColor)
                    .statusBarsPadding()
                    .padding(horizontal = headerPadding, vertical = headerPadding),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text          = "CIBLE:",
                        fontSize      = headerTextSize,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text       = level.target.toString(),
                        fontSize   = targetTextSize,
                        fontWeight = FontWeight.Black,
                        color      = Color.White
                    )
                }
                Text(
                    text       = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date()),
                    fontSize   = headerTextSize,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // ════════════════
            // ZONE DE JEU
            // ════════════════
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text       = "Objectif : atteindre ${level.target}",
                    fontSize   = objectiveTextSize,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF334155),
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(gameSectionGap))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (screenWidth < 360.dp) 6.dp else 8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    LegendItem(Color(0xFF6AAA64), "✓ Correct",  legendTextSize)
                    LegendItem(Color(0xFFC9B458), "⚠ Mauvais", legendTextSize)
                    LegendItem(Color(0xFF787C7E), "✗ Absent",  legendTextSize)
                }
                Spacer(Modifier.height(gameSectionGap))

                // Grille
                Column(
                    verticalArrangement = Arrangement.spacedBy(tileSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0 until gameState.guesses.size) {
                        val offsetX by animateFloatAsState(
                            targetValue   = if (row == gameState.currentGuess && gameState.isShaking) 10f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                            label = "shake"
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                            modifier              = Modifier.offset(x = offsetX.dp)
                        ) {
                            for (col in 0 until level.slots) {
                                GameTile(
                                    letter   = gameState.guesses[row][col],
                                    status   = gameState.tileStatuses[row][col],
                                    isActive = row == gameState.currentGuess && col == gameState.currentPos && !gameState.gameOver,
                                    size     = tileSize,
                                    isBonus  = row >= 4
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════
            // CLAVIER FIXE
            // ════════════════
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(keyRowSpacing)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(keyColSpacing)) {
                    listOf("7", "8", "9", "/").forEach { key ->
                        KeyButton(key = key, status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) }, height = keyButtonHeight,
                            textSize = keyTextSize, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(keyColSpacing)) {
                    listOf("4", "5", "6", "*").forEach { key ->
                        KeyButton(key = key, status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) }, height = keyButtonHeight,
                            textSize = keyTextSize, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(keyColSpacing)) {
                    listOf("1", "2", "3", "-").forEach { key ->
                        KeyButton(key = key, status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) }, height = keyButtonHeight,
                            textSize = keyTextSize, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(keyColSpacing)) {
                    ActionKeyButton(key = "DEL", backgroundColor = Color(0xFFFEE2E2), textColor = Color(0xFFB91C1C),
                        onClick = { viewModel.handleKeyPress("DELETE") }, height = keyButtonHeight,
                        textSize = actionKeyTextSize, modifier = Modifier.weight(1f))
                    KeyButton(key = "0", status = keyStatuses["0"] ?: TileStatus.EMPTY,
                        onClick = { viewModel.handleKeyPress("0") }, height = keyButtonHeight,
                        textSize = keyTextSize, modifier = Modifier.weight(1f))
                    KeyButton(key = "+", status = keyStatuses["+"] ?: TileStatus.EMPTY,
                        onClick = { viewModel.handleKeyPress("+") }, height = keyButtonHeight,
                        textSize = keyTextSize, modifier = Modifier.weight(1f))
                }
                // ✅ VALIDER — Box direct, toujours visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyButtonHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDCFCE7))
                        .clickable { viewModel.handleKeyPress("ENTER") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "VALIDER",
                        fontWeight = FontWeight.Bold,
                        fontSize   = actionKeyTextSize,
                        color      = Color(0xFF15803D),
                        textAlign  = TextAlign.Center
                    )
                }
            }

            // ════════════════
            // BANNER — fixe en bas, toujours visible
            // ════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        AdView(ctx).apply {
                            adUnitId = AdManager.BANNER_GAME_AD_UNIT_ID
                            setAdSize(AdSize.BANNER)
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }

        } // fin Column

        // ════════════════
        // SNACKBAR
        // ════════════════
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.Center)) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = when {
                    gameState.isWon    -> Color(0xFF4CAF50)
                    gameState.gameOver -> Color(0xFFF44336)
                    else               -> Color(0xFFFF9800)
                },
                contentColor = Color.White,
                shape        = RoundedCornerShape(12.dp),
                modifier     = Modifier.padding(16.dp)
            )
        }

        // ════════════════
        // DIALOG VIDÉO — ligne 5 (1ère aide)
        // ════════════════
        if (pendingExtraRow == 5) {
            val adAvailable = adManager.isRewardedAdExtraTryAvailable()
            ExtraRowAdDialog(
                rowNumber     = 5,
                isAdAvailable = adAvailable,
                onWatchAd     = {
                    // ✅ Ferme le dialog
                    viewModel.clearPendingExtraRow()
                    if (adAvailable) {
                        adManager.showRewardedAdExtraTry(
                            activity      = context as Activity,
                            onRewarded    = {
                                // Pub vue jusqu'au bout → ligne bonus débloquée
                                viewModel.addExtraTry()
                            },
                            onAdDismissed = {
                                // Pub fermée SANS récompense → perdu
                                // (si onRewarded a déjà été appelé, addExtraTry a déjà agi)
                            }
                        )
                    } else {
                        // Pas de pub disponible → perdu
                        viewModel.finishGameAsLost()
                    }
                },
                onDismiss     = {
                    // Utilisateur a cliqué "Non merci" → perdu
                    viewModel.finishGameAsLost()
                }
            )
        }

        // ════════════════
        // DIALOG VIDÉO — ligne 6 (2ème aide)
        // ════════════════
        if (pendingExtraRow == 6) {
            val adAvailable = adManager.isRewardedAdSolutionAvailable()
            ExtraRowAdDialog(
                rowNumber     = 6,
                isAdAvailable = adAvailable,
                onWatchAd     = {
                    // ✅ Ferme le dialog
                    viewModel.clearPendingExtraRow()
                    if (adAvailable) {
                        adManager.showRewardedAdSolution(
                            activity      = context as Activity,
                            onRewarded    = {
                                viewModel.addExtraTry()
                            },
                            onAdDismissed = {}
                        )
                    } else {
                        viewModel.finishGameAsLost()
                    }
                },
                onDismiss     = {
                    viewModel.finishGameAsLost()
                }
            )
        }

    } // fin Box
}

// ════════════════════════════════════════════════════════════════════
// DIALOG
// ════════════════════════════════════════════════════════════════════
@Composable
private fun ExtraRowAdDialog(
    rowNumber     : Int,
    isAdAvailable : Boolean,
    onWatchAd     : () -> Unit,
    onDismiss     : () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text     = if (isAdAvailable) "🎬" else "😔",
                    fontSize = 48.sp
                )

                Text(
                    text       = if (rowNumber == 5) "Débloquer la ligne 5" else "Dernière chance — Ligne 6",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF334155),
                    textAlign  = TextAlign.Center
                )

                Text(
                    text = when {
                        !isAdAvailable ->
                            "Aucune vidéo disponible pour le moment.\nVous perdez cette partie."
                        rowNumber == 5 ->
                            "Regardez une courte vidéo pour obtenir\nune tentative supplémentaire !"
                        else ->
                            "Regardez une dernière vidéo pour votre\nultime tentative. Bonne chance !"
                    },
                    fontSize  = 14.sp,
                    color     = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (isAdAvailable) {
                    // ✅ Pub disponible → bouton regarder
                    Button(
                        onClick  = onWatchAd,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text       = "▶  Regarder la vidéo",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text     = "Non merci, abandonner",
                            fontSize = 13.sp,
                            color    = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    // ✅ Pas de pub → bouton "Terminer" → perdu
                    Button(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text       = "Terminer la partie",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// UTILS
// ════════════════════════════════════════════════════════════════════
private fun calculateKeyStatuses(
    guesses      : List<List<String>>,
    tileStatuses : List<List<TileStatus>>,
    slots        : Int
): Map<String, TileStatus> {
    val map = mutableMapOf<String, TileStatus>()
    for (row in guesses.indices) {
        for (col in 0 until slots) {
            val char   = guesses[row][col]
            val status = tileStatuses[row][col]
            if (char.isNotEmpty() && status != TileStatus.EMPTY) {
                val current = map[char]
                when {
                    status == TileStatus.CORRECT                                  -> map[char] = TileStatus.CORRECT
                    status == TileStatus.PRESENT && current != TileStatus.CORRECT -> map[char] = TileStatus.PRESENT
                    status == TileStatus.ABSENT  && current == null               -> map[char] = TileStatus.ABSENT
                }
            }
        }
    }
    return map
}

@Composable
private fun GameTile(
    letter   : String,
    status   : TileStatus,
    isActive : Boolean,
    size     : Dp,
    isBonus  : Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue   = when (status) {
            TileStatus.CORRECT -> Color(0xFF6AAA64)
            TileStatus.PRESENT -> Color(0xFFC9B458)
            TileStatus.ABSENT  -> Color(0xFF787C7E)
            else               -> Color.White
        },
        animationSpec = tween(400),
        label = "tileBackground"
    )
    val borderColor = when {
        status == TileStatus.CORRECT -> Color(0xFF6AAA64)
        status == TileStatus.PRESENT -> Color(0xFFC9B458)
        status == TileStatus.ABSENT  -> Color(0xFF787C7E)
        isActive                     -> Color(0xFF667EEA)
        letter.isNotEmpty()          -> Color(0xFF1F2937)
        isBonus                      -> Color(0xFF7C3AED)
        else                         -> Color(0xFFD1D5DB)
    }
    val textColor = when (status) {
        TileStatus.CORRECT, TileStatus.PRESENT, TileStatus.ABSENT -> Color.White
        else -> Color.Black
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = letter, fontSize = (size.value * 0.5).sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun KeyButton(
    key      : String,
    status   : TileStatus,
    onClick  : () -> Unit,
    height   : Dp,
    textSize : androidx.compose.ui.unit.TextUnit,
    modifier : Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue   = when (status) {
            TileStatus.CORRECT -> Color(0xFF6AAA64)
            TileStatus.PRESENT -> Color(0xFFC9B458)
            TileStatus.ABSENT  -> Color(0xFF787C7E)
            else               -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(300),
        label = "keyBackground"
    )
    val textColor = when (status) {
        TileStatus.CORRECT, TileStatus.PRESENT, TileStatus.ABSENT -> Color.White
        else -> Color.Black
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = key, fontWeight = FontWeight.Bold, fontSize = textSize, color = textColor)
    }
}

@Composable
private fun ActionKeyButton(
    key             : String,
    backgroundColor : Color,
    textColor       : Color,
    onClick         : () -> Unit,
    height          : Dp,
    textSize        : androidx.compose.ui.unit.TextUnit,
    modifier        : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = key, fontWeight = FontWeight.Bold, fontSize = textSize, color = textColor, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LegendItem(color: Color, text: String, textSize: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(text = text, fontSize = textSize, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
    }
}