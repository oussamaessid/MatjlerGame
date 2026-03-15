package app.matjlergame

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.matjlergame.ads.AdManager
import app.matjlergame.data.repository.LevelRepositoryImpl
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level
import app.matjlergame.domain.model.Screen
import app.matjlergame.domain.usecase.CalculateTileStatusesUseCase
import app.matjlergame.domain.usecase.DailyLevelManager
import app.matjlergame.domain.usecase.DailyResult
import app.matjlergame.domain.usecase.Statistics
import app.matjlergame.domain.usecase.ValidateExpressionUseCase
import app.matjlergame.presentation.ui.DailyResultDialog
import app.matjlergame.presentation.ui.GameScreen
import app.matjlergame.presentation.ui.HowToPlayScreen
import app.matjlergame.presentation.ui.ModeSelectScreen
import app.matjlergame.presentation.ui.NoInternetDialog
import app.matjlergame.presentation.ui.ApiLoadingDialog
import app.matjlergame.presentation.viewmodel.GameViewModel
import app.matjlergame.presentation.viewmodel.NavigationViewModel
import app.matjlergame.utils.NetworkChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("mathler_prefs", MODE_PRIVATE)
        adManager = AdManager(this)

        try {
            adManager.initialize()
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur initialisation AdMob", e)
        }

        setContent {
            MaterialTheme {
                MathlerGameApp(
                    sharedPreferences = sharedPreferences,
                    adManager = adManager,
                    context = this
                )
            }
        }

        window.decorView.postDelayed({
            try {
                adManager.loadAppOpenAd { adManager.showAppOpenAd(this) }
                adManager.loadRewardedAdExtraTry()
                adManager.loadRewardedAdSolution()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur chargement annonces", e)
            }
        }, 1000)
    }
}

@Composable
fun MathlerGameApp(
    sharedPreferences: SharedPreferences,
    adManager: AdManager,
    context: MainActivity
) {
    val repository                   = remember { LevelRepositoryImpl() }
    val validateExpressionUseCase    = remember { ValidateExpressionUseCase() }
    val calculateTileStatusesUseCase = remember { CalculateTileStatusesUseCase() }
    val dailyLevelManager            = remember { DailyLevelManager(repository) }
    val navigationViewModel          = remember { NavigationViewModel(sharedPreferences) }
    val coroutineScope               = rememberCoroutineScope()

    var showNoInternetDialog by remember { mutableStateOf(false) }
    var showResultDialog     by remember { mutableStateOf(false) }
    var dialogMode           by remember { mutableStateOf<GameMode?>(null) }
    var dialogResult         by remember { mutableStateOf<DailyResult?>(null) }
    var dialogStats          by remember { mutableStateOf<Statistics?>(null) }

    var isLoading          by remember { mutableStateOf(false) }
    var loadingTimeSeconds by remember { mutableStateOf(0) }
    var loadingJob         by remember { mutableStateOf<Job?>(null) }

    var cachedLevel by remember { mutableStateOf<Pair<GameMode, Level>?>(null) }

    val navigateToHowToPlay = remember {
        val isFirst = sharedPreferences.getBoolean("is_first_launch", true)
        if (isFirst) {
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        }
        isFirst
    }
    if (navigateToHowToPlay) {
        LaunchedEffect("howtoplay_once") {
            navigationViewModel.navigateToHowToPlay()
        }
    }

    fun cancelLoading() {
        loadingJob?.cancel()
        loadingJob         = null
        isLoading          = false
        loadingTimeSeconds = 0
    }

    // ── Dialog "pas de connexion" ─────────────────────────────────────────────
    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = { showNoInternetDialog = false },
            onRetry   = { showNoInternetDialog = false }
        )
    }

    // ── Écrans ───────────────────────────────────────────────────────────────
    when (navigationViewModel.currentScreen) {

        // ── HowToPlay ──────────────────────────────────────────────────────
        Screen.HOW_TO_PLAY -> {
            HowToPlayScreen(onBack = { navigationViewModel.navigateBack() })
        }

        // ── Sélection du mode ──────────────────────────────────────────────
        Screen.MODE_SELECT -> {

            // Nettoyage à chaque arrivée sur cet écran
            LaunchedEffect(Unit) {
                cancelLoading()
                cachedLevel = null
            }

            ModeSelectScreen(
                adManager          = adManager,
                isLoading          = isLoading,
                onHowToPlayClicked = { navigationViewModel.navigateToHowToPlay() },
                onModeSelected     = { mode ->

                    if (isLoading) return@ModeSelectScreen   // déjà en cours, ignorer

                    // ── Étape 1 : vérifier la connexion (UNIQUEMENT ici, déclenché par clic)
                    if (!NetworkChecker.isInternetAvailable(context)) {
                        showNoInternetDialog = true
                        return@ModeSelectScreen
                    }

                    // ── Étape 2 : déjà joué aujourd'hui ?
                    if (dailyLevelManager.hasPlayedToday(mode, sharedPreferences)) {
                        val result = dailyLevelManager.getTodayResult(mode, sharedPreferences)
                        val stats  = dailyLevelManager.getStatistics(mode, sharedPreferences)
                        if (result != null) {
                            dialogMode       = mode
                            dialogResult     = result
                            dialogStats      = stats
                            showResultDialog = true
                        }
                        return@ModeSelectScreen
                    }

                    // ── Étape 3 : lancer le chargement du niveau
                    isLoading          = true
                    loadingTimeSeconds = 0

                    loadingJob = coroutineScope.launch {

                        // Sous-job : timer de 30 secondes
                        val timerJob = launch {
                            for (s in 1..30) {
                                delay(1000)
                                loadingTimeSeconds = s
                            }
                            // Timeout atteint → afficher le dialog (connexion trop lente)
                            Log.w("MathlerGame", "⏱️ Timeout chargement après 30s")
                            isLoading          = false
                            loadingTimeSeconds = 0
                            loadingJob         = null
                            showNoInternetDialog = true
                        }

                        // Fetch du niveau quotidien
                        dailyLevelManager.loadDailyLevelAsync(mode) { dailyLevel ->
                            timerJob.cancel()   // annuler le timer dès qu'on a la réponse
                            context.runOnUiThread {
                                isLoading          = false
                                loadingTimeSeconds = 0
                                loadingJob         = null
                                if (dailyLevel == null) {
                                    showNoInternetDialog = true
                                } else {
                                    cachedLevel = Pair(mode, dailyLevel)
                                    navigationViewModel.navigateToGame(mode)
                                }
                            }
                        }
                    }
                }
            )

            // Overlay de chargement
            if (isLoading) {
                ApiLoadingDialog(
                    loadingTimeSeconds = loadingTimeSeconds,
                    maxTimeSeconds     = 30
                )
            }

            // Dialog "déjà joué aujourd'hui"
            if (showResultDialog && dialogMode != null && dialogResult != null && dialogStats != null) {
                DailyResultDialog(
                    mode       = dialogMode!!,
                    result     = dialogResult!!,
                    statistics = dialogStats!!,
                    onDismiss  = {
                        showResultDialog = false
                        dialogMode       = null
                        dialogResult     = null
                        dialogStats      = null
                    }
                )
            }
        }

        // ── Jeu ────────────────────────────────────────────────────────────
        Screen.GAME -> {
            val mode = navigationViewModel.selectedMode

            if (mode == null) {
                LaunchedEffect(Unit) {
                    Log.e("MathlerGame", "Mode null dans GAME screen")
                    navigationViewModel.navigateBack()
                }
                return@MathlerGameApp
            }

            val level = if (cachedLevel?.first == mode) {
                cachedLevel!!.second
            } else {
                try { dailyLevelManager.getDailyLevel(mode) }
                catch (e: Exception) {
                    Log.e("MathlerGame", "Erreur rechargement niveau", e)
                    null
                }
            }

            if (level == null) {
                LaunchedEffect(Unit) {
                    Log.e("MathlerGame", "Niveau null pour mode $mode")
                    navigationViewModel.navigateBack()
                }
                return@MathlerGameApp
            }

            val gameViewModel = remember(level) {
                GameViewModel(
                    level                    = level,
                    validateExpressionUseCase    = validateExpressionUseCase,
                    calculateTileStatusesUseCase = calculateTileStatusesUseCase,
                    onLevelCompleted         = { won, attempts ->
                        dailyLevelManager.saveTodayResult(mode, won, attempts, sharedPreferences)
                        dailyLevelManager.updateStatistics(mode, won, sharedPreferences)
                        val result = dailyLevelManager.getTodayResult(mode, sharedPreferences)
                        val stats  = dailyLevelManager.getStatistics(mode, sharedPreferences)
                        if (result != null) {
                            dialogMode       = mode
                            dialogResult     = result
                            dialogStats      = stats
                            showResultDialog = true
                        }
                        cachedLevel = null
                        navigationViewModel.navigateBack()
                    },
                    totalLevels       = 1,
                    mode              = mode,
                    sharedPreferences = sharedPreferences
                )
            }

            GameScreen(
                level       = level,
                mode        = mode,
                viewModel   = gameViewModel,
                totalLevels = 1,
                adManager   = adManager,
                onBack      = {
                    cachedLevel = null
                    navigationViewModel.navigateBack()
                }
            )
        }

        else -> {
            LaunchedEffect(Unit) { navigationViewModel.navigateBack() }
        }
    }
}