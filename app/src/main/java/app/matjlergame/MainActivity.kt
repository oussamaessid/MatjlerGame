package app.matjlergame

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.matjlergame.ads.AdManager
import app.matjlergame.data.repository.LevelRepositoryImpl
import app.matjlergame.domain.model.GameMode
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
import app.matjlergame.presentation.viewmodel.GameViewModel
import app.matjlergame.presentation.viewmodel.NavigationViewModel
import app.matjlergame.utils.NetworkChecker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("mathler_prefs", MODE_PRIVATE)

        adManager = AdManager(this)
        adManager.initialize()

        // Charger et afficher l'annonce à l'ouverture
        adManager.loadAppOpenAd {
            adManager.showAppOpenAd(this)
        }

        // Charger la pub interstitielle pour après 4 minutes
        adManager.loadTimedInterstitialAd()

        // Charger les deux vidéos avec récompense
        adManager.loadRewardedAdExtraTry()
        adManager.loadRewardedAdSolution()

        // Vérifier toutes les 30 secondes si 4 minutes se sont écoulées
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(30_000) // 30 secondes
                    adManager.checkAndShowTimedAd(this@MainActivity)
                }
            }
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
    }

    override fun onResume() {
        super.onResume()
        adManager.checkAndShowTimedAd(this)
    }
}

@Composable
fun MathlerGameApp(
    sharedPreferences: SharedPreferences,
    adManager: AdManager,
    context: MainActivity
) {
    val repository = remember { LevelRepositoryImpl() }
    val validateExpressionUseCase = remember { ValidateExpressionUseCase() }
    val calculateTileStatusesUseCase = remember { CalculateTileStatusesUseCase() }
    val dailyLevelManager = remember { DailyLevelManager(repository) }

    val navigationViewModel = remember { NavigationViewModel(sharedPreferences) }

    var showResultDialog by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf<GameMode?>(null) }
    var dialogResult by remember { mutableStateOf<DailyResult?>(null) }
    var dialogStats by remember { mutableStateOf<Statistics?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var hasCheckedInternet by remember { mutableStateOf(false) }

    // Vérifier si c'est la première fois que l'app est lancée
    var isFirstLaunch by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_first_launch", true))
    }

    LaunchedEffect(Unit) {
        if (!hasCheckedInternet) {
            hasCheckedInternet = true
            if (!NetworkChecker.isInternetAvailable(context)) {
                showNoInternetDialog = true
            }
        }

        // Si c'est le premier lancement, naviguer vers HowToPlay
        if (isFirstLaunch) {
            navigationViewModel.navigateToHowToPlay()
            // Marquer comme non première fois
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
            isFirstLaunch = false
        }
    }

    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = { showNoInternetDialog = false },
            onRetry = {
                if (NetworkChecker.isInternetAvailable(context)) {
                    showNoInternetDialog = false
                }
            }
        )
    }

    when (navigationViewModel.currentScreen) {
        Screen.HOW_TO_PLAY -> {
            HowToPlayScreen(
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        Screen.MODE_SELECT -> {
            ModeSelectScreen(
                adManager = adManager,
                isLoading = isLoading,
                onModeSelected = { mode ->
                    if (!isLoading) {
                        if (!NetworkChecker.isInternetAvailable(context)) {
                            showNoInternetDialog = true
                            return@ModeSelectScreen
                        }

                        isLoading = true

                        val hasPlayed = dailyLevelManager.hasPlayedToday(mode, sharedPreferences)

                        if (hasPlayed) {
                            val result = dailyLevelManager.getTodayResult(mode, sharedPreferences)
                            val stats = dailyLevelManager.getStatistics(mode, sharedPreferences)

                            if (result != null) {
                                dialogMode = mode
                                dialogResult = result
                                dialogStats = stats
                                showResultDialog = true
                            }
                            isLoading = false
                        } else {
                            val dailyLevel = dailyLevelManager.getDailyLevel(mode)
                            if (dailyLevel == null) {
                                showNoInternetDialog = true
                                isLoading = false
                            } else {
                                navigationViewModel.navigateToGame(mode)
                            }
                        }
                    }
                },
                onHowToPlayClicked = {
                    navigationViewModel.navigateToHowToPlay()
                }
            )

            if (showResultDialog && dialogMode != null && dialogResult != null && dialogStats != null) {
                DailyResultDialog(
                    mode = dialogMode!!,
                    result = dialogResult!!,
                    statistics = dialogStats!!,
                    onDismiss = {
                        showResultDialog = false
                        dialogMode = null
                        dialogResult = null
                        dialogStats = null
                    }
                )
            }
        }

        Screen.GAME -> {
            LaunchedEffect(Unit) {
                isLoading = false
            }

            val mode = navigationViewModel.selectedMode!!
            val level = dailyLevelManager.getDailyLevel(mode)

            if (level == null) {
                LaunchedEffect(Unit) {
                    showNoInternetDialog = true
                    navigationViewModel.navigateBack()
                }
                return@MathlerGameApp
            }

            var shouldShowDialogAfterGame by remember { mutableStateOf(false) }

            val gameViewModel = remember(level) {
                GameViewModel(
                    level = level,
                    validateExpressionUseCase = validateExpressionUseCase,
                    calculateTileStatusesUseCase = calculateTileStatusesUseCase,
                    onLevelCompleted = { won, attempts ->
                        dailyLevelManager.saveTodayResult(mode, won, attempts, sharedPreferences)
                        dailyLevelManager.updateStatistics(mode, won, sharedPreferences)

                        shouldShowDialogAfterGame = true

                        GlobalScope.launch {
                            delay(2000)
                            navigationViewModel.navigateBack()
                        }
                    },
                    totalLevels = 1,
                    mode = mode,
                    sharedPreferences = sharedPreferences
                )
            }

            LaunchedEffect(shouldShowDialogAfterGame) {
                if (shouldShowDialogAfterGame) {
                    delay(2500)

                    val result = dailyLevelManager.getTodayResult(mode, sharedPreferences)
                    val stats = dailyLevelManager.getStatistics(mode, sharedPreferences)

                    if (result != null) {
                        dialogMode = mode
                        dialogResult = result
                        dialogStats = stats
                        showResultDialog = true
                    }
                }
            }

            GameScreen(
                level = level,
                mode = mode,
                viewModel = gameViewModel,
                totalLevels = 1,
                adManager = adManager,
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        Screen.LEVEL_SELECT -> {
            LaunchedEffect(Unit) {
                navigationViewModel.navigateBack()
            }
        }

        else -> {
            LaunchedEffect(Unit) {
                navigationViewModel.navigateBack()
            }
        }
    }
}