package app.matjlergame.presentation.ui

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.model.TileStatus
import app.matjlergame.presentation.viewmodel.GameViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
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

    var gameBannerLoaded by remember { mutableStateOf(false) }
    val gameBannerAdView = remember {
        AdView(context).also { view ->
            view.adUnitId = AdManager.BANNER_GAME_AD_UNIT_ID
            view.setAdSize(AdSize.BANNER)
            view.adListener = object : AdListener() {
                override fun onAdLoaded() { gameBannerLoaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { gameBannerLoaded = false }
            }
            view.loadAd(AdRequest.Builder().build())
        }
    }

    val tileSpacing = 3.dp

    // ── Clavier ─────────────────────────────────────────────────────
    val keyRowSpacing = when {
        screenHeight < 580.dp -> 2.5.dp
        screenHeight < 680.dp -> 3.5.dp
        else                  -> 4.dp
    }
    val keyColSpacing = if (screenWidth < 360.dp) 2.5.dp else 4.dp

    // ── Textes ──────────────────────────────────────────────────────
    val headerTextSize    = when { screenWidth < 360.dp -> 10.sp; screenWidth < 400.dp -> 11.sp; else -> 12.sp }
    val targetTextSize    = when { screenWidth < 360.dp -> 16.sp; screenWidth < 400.dp -> 18.sp; else -> 20.sp }
    val objectiveTextSize = when { screenWidth < 360.dp -> 11.sp; screenWidth < 400.dp -> 12.sp; else -> 13.sp }
    val legendTextSize    = when { screenWidth < 360.dp -> 8.sp;  screenWidth < 400.dp -> 9.sp; else -> 10.sp }
    val keyTextSize       = when { screenWidth < 360.dp -> 15.sp; screenWidth < 400.dp -> 16.sp; else -> 18.sp }
    val actionKeyTextSize = when { screenWidth < 360.dp -> 12.sp; screenWidth < 400.dp -> 13.sp; else -> 14.sp }
    val iconSize          = if (screenWidth < 360.dp) 34.dp else 38.dp
    val headerPadding     = if (screenWidth < 360.dp) 6.dp  else 10.dp

    val gameSectionGap = 2.dp

    val keyStatuses = remember(gameState.guesses, gameState.tileStatuses) {
        calculateKeyStatuses(gameState.guesses, gameState.tileStatuses, level.slots)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // null = pas affiché, 5 = retry pour ligne 5, 6 = retry pour ligne 6
    var showRetryVideoDialog by remember { mutableStateOf<Int?>(null) }

    // Pas d'interstitiel pendant la partie : une pub qui surgit pendant qu'on tape
    // sur le clavier = clics accidentels (trafic invalide AdMob).
    // On précharge seulement les vidéos récompensées (retente si un chargement a échoué).
    LaunchedEffect(Unit) {
        adManager.loadRewardedAdExtraTry()
        adManager.loadRewardedAdSolution()
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
                .safeDrawingPadding()
        ) {

            // ════════════════
            // HEADER
            // ════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(modeColor)
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
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text          = "CIBLE:",
                        fontSize      = headerTextSize,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text       = level.target.toString(),
                        fontSize   = targetTextSize,
                        fontWeight = FontWeight.Black,
                        color      = Color.White
                    )
                }
                Text(
                    text       = "⏱ " + app.matjlergame.domain.model.GameTimer.format(viewModel.elapsedTimeMs),
                    fontSize   = headerTextSize,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier   = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val contentHeight = maxHeight
                val sideBySide = maxWidth > maxHeight * 1.25f
                val grid: @Composable (Modifier) -> Unit = { pane ->
                    BoxWithConstraints(pane) {
                        val tileSize = responsiveTileSize(maxWidth.value, maxHeight.value,
                            level.slots, gameState.guesses.size, tileSpacing.value).dp
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text       = "Objectif : atteindre ${level.target}",
                                fontSize   = objectiveTextSize,
                                fontWeight = FontWeight.Medium,
                                color      = Color(0xFF334155),
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(gameSectionGap))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(if (screenWidth < 360.dp) 4.dp else 6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .align(Alignment.CenterHorizontally),
                                content = {
                                    LegendItem(Color(0xFF6AAA64), "✓", legendTextSize, modifier = Modifier)
                                    LegendItem(Color(0xFFC9B458), "⚠", legendTextSize, modifier = Modifier)
                                    LegendItem(Color(0xFF787C7E), "✗", legendTextSize, modifier = Modifier)
                                }
                            )
                            Spacer(Modifier.height(gameSectionGap))

                            // Grille — taille adaptée
                            Column(
                                verticalArrangement = Arrangement.spacedBy(tileSpacing),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (row in 0 until gameState.guesses.size) {
                                    val offsetX by animateFloatAsState(
                                        targetValue   = if (row == gameState.currentGuess && gameState.isShaking) 8f else 0f,
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

                    }
                }
                val keyboard: @Composable (Modifier) -> Unit = { pane ->
                    BoxWithConstraints(pane) {
                        val keyButtonHeight = ((maxHeight - 28.dp) / (if (sideBySide) 3 else 5)).coerceIn(24.dp, 54.dp)
                        if (sideBySide) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "DEL", "0", "+", "VALIDER")
                                    .chunked(6).forEach { keys ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            keys.forEach { key ->
                                                if (key == "DEL" || key == "VALIDER") {
                                                    ActionKeyButton(key, if (key == "DEL") Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                                        if (key == "DEL") Color(0xFFB91C1C) else Color(0xFF15803D),
                                                        { viewModel.handleKeyPress(if (key == "DEL") "DELETE" else "ENTER") },
                                                        keyButtonHeight, actionKeyTextSize, Modifier.weight(if (key == "VALIDER") 2f else 1f))
                                                } else KeyButton(key, keyStatuses[key] ?: TileStatus.EMPTY,
                                                    { viewModel.handleKeyPress(key) }, keyButtonHeight, keyTextSize, Modifier.weight(1f))
                                            }
                                        }
                                    }
                            }
                        } else {
                        Column(
                            modifier            = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            // ✅ VALIDER
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(keyButtonHeight)
                                    .clip(RoundedCornerShape(6.dp))
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

                        }
                    }
                }
                if (sideBySide) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        grid(Modifier.weight(1f).fillMaxHeight())
                        keyboard(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        grid(Modifier.weight(1f).fillMaxWidth())
                        keyboard(Modifier.fillMaxWidth().height((contentHeight * 0.47f).coerceIn(188.dp, 290.dp)))
                    }
                }
            }

            // ════════════════
            // BANNER — espace TOUJOURS réservé : sinon, quand la pub arrive, le clavier
            // remonte et le doigt qui visait "VALIDER" tombe sur la bannière (clic accidentel).
            // ════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(top = 20.dp, bottom = 4.dp)
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (gameBannerLoaded) {
                    AndroidView(
                        factory  = { gameBannerAdView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
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
                shape        = RoundedCornerShape(10.dp),
                modifier     = Modifier.padding(16.dp)
            )
        }

        // ════════════════
        // DIALOGS VIDÉO
        // ════════════════
        if (pendingExtraRow == 5) {
            val adAvailable = adManager.isRewardedAdExtraTryAvailable()
            ExtraRowAdDialog(
                rowNumber     = 5,
                isAdAvailable = adAvailable,
                onWatchAd     = {
                    viewModel.clearPendingExtraRow()
                    if (adAvailable) {
                        var rewarded = false
                        adManager.showRewardedAdExtraTry(
                            activity      = context as Activity,
                            onRewarded    = { rewarded = true; viewModel.addExtraTry() },
                            onAdDismissed = {
                                if (!rewarded) {
                                    if (adManager.isRewardedAdExtraTryAvailable()) {
                                        showRetryVideoDialog = 5
                                    } else {
                                        viewModel.finishGameAsLost()
                                    }
                                }
                            }
                        )
                    } else {
                        viewModel.finishGameAsLost()
                    }
                },
                onDismiss     = { viewModel.finishGameAsLost() }
            )
        }

        if (pendingExtraRow == 6) {
            val adAvailable = adManager.isRewardedAdSolutionAvailable()
            ExtraRowAdDialog(
                rowNumber     = 6,
                isAdAvailable = adAvailable,
                onWatchAd     = {
                    viewModel.clearPendingExtraRow()
                    if (adAvailable) {
                        var rewarded = false
                        adManager.showRewardedAdSolution(
                            activity      = context as Activity,
                            onRewarded    = { rewarded = true; viewModel.addExtraTry() },
                            onAdDismissed = {
                                if (!rewarded) {
                                    if (adManager.isRewardedAdSolutionAvailable()) {
                                        showRetryVideoDialog = 6
                                    } else {
                                        viewModel.finishGameAsLost()
                                    }
                                }
                            }
                        )
                    } else {
                        viewModel.finishGameAsLost()
                    }
                },
                onDismiss     = { viewModel.finishGameAsLost() }
            )
        }

        if (showRetryVideoDialog == 5) {
            RetryVideoDialog(
                onWatchAgain = {
                    showRetryVideoDialog = null
                    var rewarded2 = false
                    adManager.showRewardedAdExtraTry(
                        activity      = context as Activity,
                        onRewarded    = { rewarded2 = true; viewModel.addExtraTry() },
                        onAdDismissed = { if (!rewarded2) viewModel.finishGameAsLost() }
                    )
                },
                onGiveUp = {
                    showRetryVideoDialog = null
                    viewModel.finishGameAsLost()
                }
            )
        }

        if (showRetryVideoDialog == 6) {
            RetryVideoDialog(
                onWatchAgain = {
                    showRetryVideoDialog = null
                    var rewarded2 = false
                    adManager.showRewardedAdSolution(
                        activity      = context as Activity,
                        onRewarded    = { rewarded2 = true; viewModel.addExtraTry() },
                        onAdDismissed = { if (!rewarded2) viewModel.finishGameAsLost() }
                    )
                },
                onGiveUp = {
                    showRetryVideoDialog = null
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
            shape    = RoundedCornerShape(18.dp),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = if (isAdAvailable) "🎬" else "😔", fontSize = 44.sp)

                Text(
                    text       = if (rowNumber == 5) "Débloquer la ligne 5" else "Dernière chance — Ligne 6",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF334155),
                    textAlign  = TextAlign.Center
                )

                Text(
                    text = when {
                        !isAdAvailable -> "Aucune vidéo disponible.\nVous perdez cette partie."
                        rowNumber == 5 -> "Regardez une courte vidéo pour\nobtenir une tentative supplémentaire !"
                        else           -> "Regardez une dernière vidéo pour\nvotre ultime tentative."
                    },
                    fontSize  = 13.sp,
                    color     = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (isAdAvailable) {
                    Button(
                        onClick  = onWatchAd,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text("▶  Regarder la vidéo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Non merci, abandonner", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    Button(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text("Terminer la partie", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RetryVideoDialog(
    onWatchAgain : () -> Unit,
    onGiveUp     : () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onGiveUp) {
        Card(
            shape    = RoundedCornerShape(18.dp),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⏸️", fontSize = 44.sp)

                Text(
                    text       = "Vidéo non terminée",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF334155),
                    textAlign  = TextAlign.Center
                )

                Text(
                    text      = "Vous n'avez pas terminé la vidéo.\nUne autre vidéo est disponible.",
                    fontSize  = 13.sp,
                    color     = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick  = onWatchAgain,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text("▶  Regarder une autre vidéo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                TextButton(onClick = onGiveUp) {
                    Text("Non merci, abandonner", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            fontSize = (size.value * 0.45).sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
            .clip(RoundedCornerShape(6.dp))
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
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text = text, fontSize = textSize, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
    }
}