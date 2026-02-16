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

class MainActivity : ComponentActivity() {
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("mathler_prefs", MODE_PRIVATE)

        adManager = AdManager(this)

        // Initialiser AdMob de manière asynchrone pour ne pas bloquer l'UI
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

        // Charger les annonces après un délai pour ne pas bloquer le démarrage
        window.decorView.postDelayed({
            try {
                adManager.loadAppOpenAd {
                    adManager.showAppOpenAd(this)
                }
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

    // Compteur pour le temps de chargement
    var loadingTimeSeconds by remember { mutableStateOf(0) }

    // Cache pour éviter de charger le niveau deux fois
    var cachedLevel by remember { mutableStateOf<Pair<GameMode, Level>?>(null) }

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

        if (isFirstLaunch) {
            navigationViewModel.navigateToHowToPlay()
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
            // Nettoyer le cache quand on retourne à l'écran de sélection
            LaunchedEffect(Unit) {
                cachedLevel = null
                isLoading = false
            }

            ModeSelectScreen(
                adManager = adManager,
                isLoading = false,  // On gère le loading séparément avec ApiLoadingDialog
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
                            // Charger le niveau quotidien de manière asynchrone avec timeout
                            isLoading = true
                            loadingTimeSeconds = 0
                            val startTime = System.currentTimeMillis()
                            val timeoutMillis = 120000L // 2 minutes
                            var loadingCompleted = false

                            // Thread pour mettre à jour le compteur et vérifier le timeout
                            Thread {
                                while (!loadingCompleted &&
                                    (System.currentTimeMillis() - startTime) < timeoutMillis) {
                                    Thread.sleep(1000)
                                    context.runOnUiThread {
                                        loadingTimeSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                                    }
                                }

                                if (!loadingCompleted) {
                                    // Timeout dépassé
                                    context.runOnUiThread {
                                        isLoading = false
                                        loadingTimeSeconds = 0
                                        showNoInternetDialog = true
                                        Log.e("MathlerGame", "⏱️ Timeout dépassé après ${(System.currentTimeMillis() - startTime) / 1000} secondes")
                                    }
                                }
                            }.start()

                            // Charger le niveau
                            dailyLevelManager.loadDailyLevelAsync(mode) { dailyLevel ->
                                loadingCompleted = true
                                context.runOnUiThread {
                                    loadingTimeSeconds = 0
                                    if (dailyLevel == null) {
                                        showNoInternetDialog = true
                                        isLoading = false
                                    } else {
                                        // Mettre en cache le niveau chargé
                                        cachedLevel = Pair(mode, dailyLevel)
                                        navigationViewModel.navigateToGame(mode)
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                },
                onHowToPlayClicked = {
                    navigationViewModel.navigateToHowToPlay()
                }
            )

            // Afficher le dialogue de chargement API
            if (isLoading) {
                ApiLoadingDialog(
                    loadingTimeSeconds = loadingTimeSeconds,
                    maxTimeSeconds = 120
                )
            }

            // Afficher le dialogue de résultats si disponible
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
            // Utiliser le niveau en cache au lieu de le recharger
            val mode = navigationViewModel.selectedMode

            if (mode == null) {
                // Si pas de mode sélectionné, retourner à l'écran de sélection
                LaunchedEffect(Unit) {
                    Log.e("MathlerGame", "Mode null dans GAME screen")
                    navigationViewModel.navigateBack()
                }
                return@MathlerGameApp
            }

            // Récupérer le niveau du cache
            val level = if (cachedLevel != null && cachedLevel!!.first == mode) {
                cachedLevel!!.second
            } else {
                // Si pas en cache, essayer de charger (fallback)
                try {
                    dailyLevelManager.getDailyLevel(mode)
                } catch (e: Exception) {
                    Log.e("MathlerGame", "Erreur rechargement niveau", e)
                    null
                }
            }

            if (level == null) {
                LaunchedEffect(Unit) {
                    Log.e("MathlerGame", "Niveau null pour mode $mode")
                    showNoInternetDialog = true
                    navigationViewModel.navigateBack()
                }
                return@MathlerGameApp
            }

            val gameViewModel = remember(level) {
                GameViewModel(
                    level = level,
                    validateExpressionUseCase = validateExpressionUseCase,
                    calculateTileStatusesUseCase = calculateTileStatusesUseCase,
                    onLevelCompleted = { won, attempts ->
                        // Sauvegarder le résultat
                        dailyLevelManager.saveTodayResult(mode, won, attempts, sharedPreferences)
                        dailyLevelManager.updateStatistics(mode, won, sharedPreferences)

                        // Préparer les données du dialogue
                        val result = dailyLevelManager.getTodayResult(mode, sharedPreferences)
                        val stats = dailyLevelManager.getStatistics(mode, sharedPreferences)

                        if (result != null) {
                            dialogMode = mode
                            dialogResult = result
                            dialogStats = stats
                            showResultDialog = true
                        }

                        // Nettoyer le cache
                        cachedLevel = null
                        navigationViewModel.navigateBack()
                    },
                    totalLevels = 1,
                    mode = mode,
                    sharedPreferences = sharedPreferences
                )
            }

            GameScreen(
                level = level,
                mode = mode,
                viewModel = gameViewModel,
                totalLevels = 1,
                adManager = adManager,
                onBack = {
                    cachedLevel = null
                    navigationViewModel.navigateBack()
                }
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