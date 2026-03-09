package app.matjlergame.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var rewardedAdExtraTry: RewardedAd? = null
    private var rewardedAdSolution: RewardedAd? = null

    private var interstitialExtraTry: InterstitialAd? = null
    private var interstitialSolution: InterstitialAd? = null

    private var interstitialPeriodic: InterstitialAd? = null
    private var isLoadingInterstitialPeriodic = false
    private var lastPeriodicAdShownTime: Long = 0L
    private val periodicAdIntervalMs = 4 * 60 * 1000L

    private var isLoadingAppOpen = false
    private var isLoadingRewardedExtraTry = false
    private var isLoadingRewardedSolution = false
    private var isLoadingInterstitialExtraTry = false
    private var isLoadingInterstitialSolution = false

    private var hasShownAppOpenAd = false

    private var appOpenLoadFailed = false
    private var extraTryLoadFailed = false
    private var solutionLoadFailed = false

    companion object {
        private const val TAG = "AdManager"

        private const val USE_TEST_ADS = true

        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_MODE_SELECT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_BANNER_GAME_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

        private const val PROD_APP_OPEN_ID = "ca-app-pub-4161995857939030/6856571786"
        private const val PROD_BANNER_MODE_SELECT_ID = "ca-app-pub-4161995857939030/7131903958"
        private const val PROD_BANNER_GAME_ID = "ca-app-pub-4161995857939030/7494099094"
        private const val PROD_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-4161995857939030/7582944991"
        private const val PROD_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-4161995857939030/2664816346"

        private const val PROD_INTERSTITIAL_EXTRA_TRY_ID = "ca-app-pub-2498267529185476/6361352920"
        private const val PROD_INTERSTITIAL_SOLUTION_ID = "ca-app-pub-2498267529185476/4880859723"

        val APP_OPEN_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_APP_OPEN_ID else PROD_APP_OPEN_ID

        val BANNER_MODE_SELECT_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_MODE_SELECT_ID else PROD_BANNER_MODE_SELECT_ID

        val BANNER_GAME_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_GAME_ID else PROD_BANNER_GAME_ID

        val REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_VIDEO_EXTRA_TRY_ID else PROD_REWARDED_VIDEO_EXTRA_TRY_ID

        val REWARDED_VIDEO_SOLUTION_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_VIDEO_SOLUTION_ID else PROD_REWARDED_VIDEO_SOLUTION_ID

        val INTERSTITIAL_EXTRA_TRY_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_EXTRA_TRY_ID

        val INTERSTITIAL_SOLUTION_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_SOLUTION_ID
    }

    fun initialize() {
        try {
            MobileAds.initialize(context) {
                val mode = if (USE_TEST_ADS) "TEST" else "PRODUCTION"
                Log.d(TAG, "✅ AdMob initialisé en mode: $mode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur initialisation AdMob", e)
        }
    }

    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        if (isLoadingAppOpen || appOpenLoadFailed) return
        isLoadingAppOpen = true
        try {
            AppOpenAd.load(
                context,
                APP_OPEN_AD_UNIT_ID,
                AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "✅ Annonce à l'OUVERTURE chargée")
                        appOpenAd = ad
                        isLoadingAppOpen = false
                        appOpenLoadFailed = false
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec chargement OUVERTURE: ${adError.message}")
                        appOpenAd = null
                        isLoadingAppOpen = false
                        appOpenLoadFailed = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception chargement OUVERTURE", e)
            isLoadingAppOpen = false
            appOpenLoadFailed = true
        }
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (hasShownAppOpenAd) { onAdDismissed(); return }
        if (appOpenAd != null) {
            try {
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        hasShownAppOpenAd = true
                        onAdDismissed()
                        loadAppOpenAd()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        hasShownAppOpenAd = true
                        onAdDismissed()
                    }
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "✅ Annonce à l'OUVERTURE affichée")
                    }
                }
                appOpenAd?.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception affichage OUVERTURE", e)
                onAdDismissed()
            }
        } else {
            onAdDismissed()
        }
    }

    fun loadRewardedAdExtraTry(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedExtraTry || extraTryLoadFailed) return
        isLoadingRewardedExtraTry = true
        try {
            RewardedAd.load(
                context,
                REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "✅ Vidéo EXTRA TRY (Rewarded) chargée")
                        rewardedAdExtraTry = ad
                        isLoadingRewardedExtraTry = false
                        extraTryLoadFailed = false
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Rewarded EXTRA TRY: ${adError.message}")
                        rewardedAdExtraTry = null
                        isLoadingRewardedExtraTry = false
                        Log.d(TAG, "🔄 Chargement Interstitiel EXTRA TRY en fallback...")
                        loadInterstitialExtraTry()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded EXTRA TRY", e)
            isLoadingRewardedExtraTry = false
            loadInterstitialExtraTry()
        }
    }

    fun showRewardedAdExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        when {
            rewardedAdExtraTry != null -> showRewardedExtraTry(activity, onRewarded, onAdDismissed)
            interstitialExtraTry != null -> showInterstitialExtraTryInternal(activity, onRewarded, onAdDismissed)
            else -> {
                Log.d(TAG, "⏳ Aucune annonce EXTRA TRY disponible")
                onAdDismissed()
            }
        }
    }

    private fun showRewardedExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            rewardedAdExtraTry?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Rewarded EXTRA TRY fermée")
                    rewardedAdExtraTry = null
                    onAdDismissed()
                    loadRewardedAdExtraTry()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage Rewarded EXTRA TRY: ${adError.message}")
                    rewardedAdExtraTry = null
                    onAdDismissed()
                }
            }
            rewardedAdExtraTry?.show(activity) {
                Log.d(TAG, "🎁 Récompense EXTRA TRY gagnée")
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded EXTRA TRY", e)
            onAdDismissed()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INTERSTITIEL — EXTRA TRY (fallback rewarded)
    // ─────────────────────────────────────────────────────────────

    private fun loadInterstitialExtraTry() {
        if (isLoadingInterstitialExtraTry) return
        isLoadingInterstitialExtraTry = true
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_EXTRA_TRY_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "✅ Interstitiel EXTRA TRY chargé (fallback)")
                        interstitialExtraTry = ad
                        isLoadingInterstitialExtraTry = false
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Interstitiel EXTRA TRY: ${adError.message}")
                        interstitialExtraTry = null
                        isLoadingInterstitialExtraTry = false
                        extraTryLoadFailed = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Interstitiel EXTRA TRY", e)
            isLoadingInterstitialExtraTry = false
            extraTryLoadFailed = true
        }
    }

    private fun showInterstitialExtraTryInternal(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            interstitialExtraTry?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialExtraTry = null
                    onRewarded()
                    onAdDismissed()
                    loadInterstitialExtraTry()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialExtraTry = null
                    onAdDismissed()
                }
            }
            interstitialExtraTry?.show(activity)
        } catch (e: Exception) {
            onAdDismissed()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REWARDED — SOLUTION
    // ─────────────────────────────────────────────────────────────

    fun loadRewardedAdSolution(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedSolution || solutionLoadFailed) return
        isLoadingRewardedSolution = true
        try {
            RewardedAd.load(
                context,
                REWARDED_VIDEO_SOLUTION_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "✅ Vidéo SOLUTION (Rewarded) chargée")
                        rewardedAdSolution = ad
                        isLoadingRewardedSolution = false
                        solutionLoadFailed = false
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Rewarded SOLUTION: ${adError.message}")
                        rewardedAdSolution = null
                        isLoadingRewardedSolution = false
                        Log.d(TAG, "🔄 Chargement Interstitiel SOLUTION en fallback...")
                        loadInterstitialSolution()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded SOLUTION", e)
            isLoadingRewardedSolution = false
            loadInterstitialSolution()
        }
    }

    fun showRewardedAdSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        when {
            rewardedAdSolution != null -> showRewardedSolution(activity, onRewarded, onAdDismissed)
            interstitialSolution != null -> showInterstitialSolutionInternal(activity, onRewarded, onAdDismissed)
            else -> {
                Log.d(TAG, "⏳ Aucune annonce SOLUTION disponible")
                onAdDismissed()
            }
        }
    }

    private fun showRewardedSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            rewardedAdSolution?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAdSolution = null
                    onAdDismissed()
                    loadRewardedAdSolution()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAdSolution = null
                    onAdDismissed()
                }
            }
            rewardedAdSolution?.show(activity) {
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded SOLUTION", e)
            onAdDismissed()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INTERSTITIEL — SOLUTION (fallback rewarded)
    // ─────────────────────────────────────────────────────────────

    private fun loadInterstitialSolution() {
        if (isLoadingInterstitialSolution) return
        isLoadingInterstitialSolution = true
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_SOLUTION_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "✅ Interstitiel SOLUTION chargé (fallback)")
                        interstitialSolution = ad
                        isLoadingInterstitialSolution = false
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Interstitiel SOLUTION: ${adError.message}")
                        interstitialSolution = null
                        isLoadingInterstitialSolution = false
                        solutionLoadFailed = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Interstitiel SOLUTION", e)
            isLoadingInterstitialSolution = false
            solutionLoadFailed = true
        }
    }

    private fun showInterstitialSolutionInternal(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            interstitialSolution?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialSolution = null
                    onRewarded()
                    onAdDismissed()
                    loadInterstitialSolution()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec Interstitiel SOLUTION: ${adError.message}")
                    interstitialSolution = null
                    onAdDismissed()
                }
            }
            interstitialSolution?.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Interstitiel SOLUTION", e)
            onAdDismissed()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✅ INTERSTITIEL PÉRIODIQUE — toutes les 4 minutes (pendant le jeu)
    // Utilise PROD_INTERSTITIAL_EXTRA_TRY_ID
    // ─────────────────────────────────────────────────────────────

    fun loadPeriodicInterstitial() {
        if (isLoadingInterstitialPeriodic || interstitialPeriodic != null) return
        isLoadingInterstitialPeriodic = true
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_EXTRA_TRY_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "✅ Interstitiel PÉRIODIQUE chargé")
                        interstitialPeriodic = ad
                        isLoadingInterstitialPeriodic = false
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Interstitiel PÉRIODIQUE: ${adError.message}")
                        interstitialPeriodic = null
                        isLoadingInterstitialPeriodic = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Interstitiel PÉRIODIQUE", e)
            isLoadingInterstitialPeriodic = false
        }
    }

    /**
     * ✅ Appelle cette fonction depuis GameScreen via LaunchedEffect toutes les 4 minutes.
     * Elle vérifie elle-même si l'intervalle est écoulé et si une pub est prête.
     * Ne fait rien si le jeu est terminé (gameOver doit être vérifié côté appelant).
     */
    fun showPeriodicInterstitialIfReady(
        activity: Activity,
        onAdDismissed: () -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastPeriodicAdShownTime

        if (elapsed < periodicAdIntervalMs) {
            val remaining = (periodicAdIntervalMs - elapsed) / 1000
            Log.d(TAG, "⏳ Interstitiel périodique : encore ${remaining}s à attendre")
            return
        }

        if (interstitialPeriodic == null) {
            Log.d(TAG, "⏳ Interstitiel périodique non prêt, rechargement...")
            loadPeriodicInterstitial()
            return
        }

        try {
            interstitialPeriodic?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitiel PÉRIODIQUE fermé")
                    interstitialPeriodic = null
                    lastPeriodicAdShownTime = System.currentTimeMillis()
                    onAdDismissed()
                    loadPeriodicInterstitial() // Précharge le suivant
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage Interstitiel PÉRIODIQUE: ${adError.message}")
                    interstitialPeriodic = null
                    loadPeriodicInterstitial()
                }
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitiel PÉRIODIQUE affiché")
                }
            }
            interstitialPeriodic?.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception affichage Interstitiel PÉRIODIQUE", e)
            interstitialPeriodic = null
            loadPeriodicInterstitial()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    fun isRewardedAdExtraTryAvailable(): Boolean {
        return rewardedAdExtraTry != null || interstitialExtraTry != null
    }

    fun isRewardedAdSolutionAvailable(): Boolean {
        return rewardedAdSolution != null || interstitialSolution != null
    }

    fun isTestMode(): Boolean = USE_TEST_ADS
}