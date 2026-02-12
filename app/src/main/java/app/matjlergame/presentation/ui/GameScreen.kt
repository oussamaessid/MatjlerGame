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
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    level: Level,
    mode: GameMode,
    viewModel: GameViewModel,
    totalLevels: Int,
    adManager: AdManager,
    onBack: () -> Unit
) {
    val gameState = viewModel.gameState
    val modeColor = when(mode) {
        GameMode.EASY -> Color(0xFF4CAF50)
        GameMode.MEDIUM -> Color(0xFFFF9800)
        GameMode.HARD -> Color(0xFFF44336)
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val availableWidth = screenWidth - 32.dp
    val tileSpacing = when {
        screenWidth < 360.dp -> 3.dp
        else -> 4.dp
    }
    val totalSpacing = tileSpacing * (level.slots - 1)
    var tileSize = (availableWidth - totalSpacing) / level.slots
    tileSize = when {
        screenWidth < 360.dp && level.slots >= 6 -> minOf(tileSize, 35.dp)
        screenWidth < 380.dp && level.slots >= 6 -> minOf(tileSize, 38.dp)
        screenWidth < 400.dp -> minOf(tileSize, 42.dp)
        else -> minOf(tileSize, 48.dp)
    }

    val keyButtonHeight = when {
        screenHeight < 600.dp -> 40.dp
        screenHeight < 700.dp -> 44.dp
        screenHeight < 800.dp -> 48.dp
        else -> 52.dp
    }

    // Tailles de texte responsives
    val headerTextSize = when {
        screenWidth < 360.dp -> 11.sp
        screenWidth < 400.dp -> 12.sp
        else -> 13.sp
    }

    val targetTextSize = when {
        screenWidth < 360.dp -> 18.sp
        screenWidth < 400.dp -> 20.sp
        else -> 22.sp
    }

    val objectiveTextSize = when {
        screenWidth < 360.dp -> 13.sp
        screenWidth < 400.dp -> 14.sp
        else -> 15.sp
    }

    val legendTextSize = when {
        screenWidth < 360.dp -> 10.sp
        screenWidth < 400.dp -> 11.sp
        else -> 12.sp
    }

    val keyTextSize = when {
        screenWidth < 360.dp -> 16.sp
        screenWidth < 400.dp -> 18.sp
        else -> 20.sp
    }

    val actionKeyTextSize = when {
        screenWidth < 360.dp -> 13.sp
        screenWidth < 400.dp -> 14.sp
        else -> 16.sp
    }

    val iconSize = when {
        screenWidth < 360.dp -> 36.dp
        else -> 40.dp
    }

    val headerPadding = when {
        screenWidth < 360.dp -> 10.dp
        else -> 12.dp
    }

    val keyStatuses = remember(gameState.guesses, gameState.tileStatuses) {
        calculateKeyStatuses(gameState.guesses, gameState.tileStatuses, level.slots)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var hasUsedRewardedAd by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.message) {
        if (gameState.message.isNotEmpty() && !gameState.gameOver) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = gameState.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(gameState.gameOver, gameState.isWon) {
        if (gameState.gameOver && !gameState.isWon && !hasUsedRewardedAd) {
            if (adManager.isRewardedAdExtraTryAvailable()) {
                showRewardedAdDialog = true
            } else {
                hasUsedRewardedAd = true
                viewModel.finishGameAsLost()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {
            // Header avec cible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(modeColor)
                    .statusBarsPadding()
                    .padding(horizontal = headerPadding, vertical = headerPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CIBLE:",
                        fontSize = headerTextSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = level.target.toString(),
                        fontSize = targetTextSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Text(
                    text = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date()),
                    fontSize = headerTextSize,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Zone de jeu
            Column(
                modifier = Modifier
                    .padding(top = if (screenHeight < 600.dp) 12.dp else 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Objectif
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Objectif : atteindre ${level.target}",
                            fontSize = objectiveTextSize,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Légende
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            if (screenWidth < 360.dp) 6.dp else 8.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        LegendItem(Color(0xFF6AAA64), "✓ Correct", legendTextSize)
                        LegendItem(Color(0xFFC9B458), "⚠ Mauvais", legendTextSize)
                        LegendItem(Color(0xFF787C7E), "✗ Absent", legendTextSize)
                    }

                    // Grille de jeu
                    Column(
                        verticalArrangement = Arrangement.spacedBy(tileSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (row in 0 until gameState.guesses.size) {
                            val offsetX by animateFloatAsState(
                                targetValue = if (row == gameState.currentGuess && gameState.isShaking) 10f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessHigh
                                ),
                                label = "shake"
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                                modifier = Modifier.offset(x = offsetX.dp)
                            ) {
                                for (col in 0 until level.slots) {
                                    GameTile(
                                        letter = gameState.guesses[row][col],
                                        status = gameState.tileStatuses[row][col],
                                        isActive = row == gameState.currentGuess &&
                                                col == gameState.currentPos &&
                                                !gameState.gameOver,
                                        size = tileSize
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (screenHeight < 600.dp) 8.dp else 16.dp))

            // Clavier
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    if (screenHeight < 600.dp) 4.dp else 5.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = if (screenHeight < 600.dp) 8.dp else 16.dp
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (screenWidth < 360.dp) 4.dp else 6.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("7", "8", "9", "/").forEach { key ->
                        KeyButton(
                            key = key,
                            status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) },
                            height = keyButtonHeight,
                            textSize = keyTextSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (screenWidth < 360.dp) 4.dp else 6.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("4", "5", "6", "*").forEach { key ->
                        KeyButton(
                            key = key,
                            status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) },
                            height = keyButtonHeight,
                            textSize = keyTextSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (screenWidth < 360.dp) 4.dp else 6.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("1", "2", "3", "-").forEach { key ->
                        KeyButton(
                            key = key,
                            status = keyStatuses[key] ?: TileStatus.EMPTY,
                            onClick = { viewModel.handleKeyPress(key) },
                            height = keyButtonHeight,
                            textSize = keyTextSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (screenWidth < 360.dp) 4.dp else 6.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionKeyButton(
                        key = "DEL",
                        backgroundColor = Color(0xFFFEE2E2),
                        textColor = Color(0xFFB91C1C),
                        onClick = { viewModel.handleKeyPress("DELETE") },
                        height = keyButtonHeight,
                        textSize = actionKeyTextSize,
                        modifier = Modifier.weight(1f)
                    )
                    KeyButton(
                        key = "0",
                        status = keyStatuses["0"] ?: TileStatus.EMPTY,
                        onClick = { viewModel.handleKeyPress("0") },
                        height = keyButtonHeight,
                        textSize = keyTextSize,
                        modifier = Modifier.weight(1f)
                    )
                    KeyButton(
                        key = "+",
                        status = keyStatuses["+"] ?: TileStatus.EMPTY,
                        onClick = { viewModel.handleKeyPress("+") },
                        height = keyButtonHeight,
                        textSize = keyTextSize,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    ActionKeyButton(
                        key = "VALIDER",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF15803D),
                        onClick = { viewModel.handleKeyPress("ENTER") },
                        height = keyButtonHeight,
                        textSize = actionKeyTextSize,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bannière publicitaire
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (screenHeight < 600.dp) 50.dp else 60.dp),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdManager.BANNER_GAME_AD_UNIT_ID
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.Center)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = when {
                    gameState.isWon -> Color(0xFF4CAF50)
                    gameState.gameOver -> Color(0xFFF44336)
                    else -> Color(0xFFFF9800)
                },
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            )
        }

        // Dialogue de récompense - N'afficher que si la vidéo est disponible
        if (showRewardedAdDialog && adManager.isRewardedAdExtraTryAvailable()) {
            RewardedAdChoiceDialog(
                onWatchExtraTryAd = {
                    showRewardedAdDialog = false
                    hasUsedRewardedAd = true

                    adManager.showRewardedAdExtraTry(
                        activity = context as Activity,
                        onRewarded = {
                            viewModel.addExtraTry()
                        },
                        onAdDismissed = {
                        }
                    )
                },
                onWatchSolutionAd = {
                },
                onDismiss = {
                    showRewardedAdDialog = false
                    hasUsedRewardedAd = true
                    viewModel.finishGameAsLost()
                }
            )
        }
    }
}

private fun calculateKeyStatuses(
    guesses: List<List<String>>,
    tileStatuses: List<List<TileStatus>>,
    slots: Int
): Map<String, TileStatus> {
    val keyStatusMap = mutableMapOf<String, TileStatus>()

    for (row in guesses.indices) {
        for (col in 0 until slots) {
            val char = guesses[row][col]
            val status = tileStatuses[row][col]

            if (char.isNotEmpty() && status != TileStatus.EMPTY) {
                val currentStatus = keyStatusMap[char]

                when {
                    status == TileStatus.CORRECT -> keyStatusMap[char] = TileStatus.CORRECT
                    status == TileStatus.PRESENT && currentStatus != TileStatus.CORRECT -> {
                        keyStatusMap[char] = TileStatus.PRESENT
                    }
                    status == TileStatus.ABSENT && currentStatus == null -> {
                        keyStatusMap[char] = TileStatus.ABSENT
                    }
                }
            }
        }
    }

    return keyStatusMap
}

@Composable
private fun GameTile(
    letter: String,
    status: TileStatus,
    isActive: Boolean,
    size: Dp
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            TileStatus.CORRECT -> Color(0xFF6AAA64)
            TileStatus.PRESENT -> Color(0xFFC9B458)
            TileStatus.ABSENT -> Color(0xFF787C7E)
            else -> Color.White
        },
        animationSpec = tween(400),
        label = "tileBackground"
    )

    val borderColor = when {
        status == TileStatus.CORRECT -> Color(0xFF6AAA64)
        status == TileStatus.PRESENT -> Color(0xFFC9B458)
        status == TileStatus.ABSENT -> Color(0xFF787C7E)
        isActive -> Color(0xFF667EEA)
        letter.isNotEmpty() -> Color(0xFF1F2937)
        else -> Color(0xFFD1D5DB)
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
        Text(
            text = letter,
            fontSize = (size.value * 0.5).sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun KeyButton(
    key: String,
    status: TileStatus,
    onClick: () -> Unit,
    height: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            TileStatus.CORRECT -> Color(0xFF6AAA64)
            TileStatus.PRESENT -> Color(0xFFC9B458)
            TileStatus.ABSENT -> Color(0xFF787C7E)
            else -> Color(0xFFE0E0E0)
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
        Text(
            text = key,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            color = textColor
        )
    }
}

@Composable
private fun ActionKeyButton(
    key: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    height: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            color = textColor
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = text,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
    }
}