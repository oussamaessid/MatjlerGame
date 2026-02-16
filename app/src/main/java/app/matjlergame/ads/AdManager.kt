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

    // Annonces interstitielles en fallback
    private var interstitialExtraTry: InterstitialAd? = null
    private var interstitialSolution: InterstitialAd? = null

    private var isLoadingAppOpen = false
    private var isLoadingRewardedExtraTry = false
    private var isLoadingRewardedSolution = false
    private var isLoadingInterstitialExtraTry = false
    private var isLoadingInterstitialSolution = false

    private var hasShownAppOpenAd = false

    // Flags pour éviter les tentatives multiples en cas d'échec
    private var appOpenLoadFailed = false
    private var extraTryLoadFailed = false
    private var solutionLoadFailed = false

    companion object {
        private const val TAG = "AdManager"

        // IMPORTANT: Passer à true pour les tests, false pour la production
        private const val USE_TEST_ADS = false  // CHANGER À false EN PRODUCTION

        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_MODE_SELECT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_BANNER_GAME_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

        // IDs réels - Rewarded Ads (priorité)
        private const val PROD_APP_OPEN_ID = "ca-app-pub-4161995857939030/6856571786"
        private const val PROD_BANNER_MODE_SELECT_ID = "ca-app-pub-4161995857939030/7131903958"
        private const val PROD_BANNER_GAME_ID = "ca-app-pub-4161995857939030/7494099094"
        private const val PROD_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-4161995857939030/7582944991"
        private const val PROD_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-4161995857939030/2664816346"

        // IDs réels - Interstitial Ads (fallback)
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
            val adRequest = AdRequest.Builder().build()

            AppOpenAd.load(
                context,
                APP_OPEN_AD_UNIT_ID,
                adRequest,
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

    /**
     * Charge les annonces pour Extra Try : Rewarded en priorité, puis Interstitial en fallback
     */
    fun loadRewardedAdExtraTry(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedExtraTry || extraTryLoadFailed) return

        isLoadingRewardedExtraTry = true

        try {
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                context,
                REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID,
                adRequest,
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

                        // Charger l'interstitiel en fallback
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

    /**
     * Charge l'interstitiel Extra Try en fallback
     */
    private fun loadInterstitialExtraTry() {
        if (isLoadingInterstitialExtraTry) return

        isLoadingInterstitialExtraTry = true

        try {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                INTERSTITIAL_EXTRA_TRY_AD_UNIT_ID,
                adRequest,
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

    fun loadRewardedAdSolution(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedSolution || solutionLoadFailed) return

        isLoadingRewardedSolution = true

        try {
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                context,
                REWARDED_VIDEO_SOLUTION_AD_UNIT_ID,
                adRequest,
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

                        // Charger l'interstitiel en fallback
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

    /**
     * Charge l'interstitiel Solution en fallback
     */
    private fun loadInterstitialSolution() {
        if (isLoadingInterstitialSolution) return

        isLoadingInterstitialSolution = true

        try {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                INTERSTITIAL_SOLUTION_AD_UNIT_ID,
                adRequest,
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

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (hasShownAppOpenAd) {
            onAdDismissed()
            return
        }

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

    /**
     * Affiche Extra Try : Rewarded en priorité, sinon Interstitial
     */
    fun showRewardedAdExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        // Priorité 1: Rewarded Ad
        if (rewardedAdExtraTry != null) {
            showRewardedExtraTry(activity, onRewarded, onAdDismissed)
        }
        // Priorité 2: Interstitial Ad (fallback)
        else if (interstitialExtraTry != null) {
            showInterstitialExtraTry(activity, onRewarded, onAdDismissed)
        }
        // Aucune annonce disponible
        else {
            Log.d(TAG, "⏳ Aucune annonce EXTRA TRY disponible")
            onAdDismissed()
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

            rewardedAdExtraTry?.show(activity) { rewardItem ->
                Log.d(TAG, "🎁 Récompense EXTRA TRY gagnée")
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded EXTRA TRY", e)
            onAdDismissed()
        }
    }

    private fun showInterstitialExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            interstitialExtraTry?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitiel EXTRA TRY fermé")
                    interstitialExtraTry = null
                    onRewarded() // On donne quand même la récompense
                    onAdDismissed()
                    loadInterstitialExtraTry()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec Interstitiel EXTRA TRY: ${adError.message}")
                    interstitialExtraTry = null
                    onAdDismissed()
                }
            }

            interstitialExtraTry?.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Interstitiel EXTRA TRY", e)
            onAdDismissed()
        }
    }

    fun showRewardedAdSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        // Priorité 1: Rewarded Ad
        if (rewardedAdSolution != null) {
            showRewardedSolution(activity, onRewarded, onAdDismissed)
        }
        // Priorité 2: Interstitial Ad (fallback)
        else if (interstitialSolution != null) {
            showInterstitialSolution(activity, onRewarded, onAdDismissed)
        }
        // Aucune annonce disponible
        else {
            Log.d(TAG, "⏳ Aucune annonce SOLUTION disponible")
            onAdDismissed()
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
                    Log.d(TAG, "✅ Rewarded SOLUTION fermée")
                    rewardedAdSolution = null
                    onAdDismissed()
                    loadRewardedAdSolution()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec Rewarded SOLUTION: ${adError.message}")
                    rewardedAdSolution = null
                    onAdDismissed()
                }
            }

            rewardedAdSolution?.show(activity) { rewardItem ->
                Log.d(TAG, "🎁 Récompense SOLUTION gagnée")
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded SOLUTION", e)
            onAdDismissed()
        }
    }

    private fun showInterstitialSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        try {
            interstitialSolution?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitiel SOLUTION fermé")
                    interstitialSolution = null
                    onRewarded() // On donne quand même la récompense
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

    fun isRewardedAdExtraTryAvailable(): Boolean {
        return rewardedAdExtraTry != null || interstitialExtraTry != null
    }

    fun isRewardedAdSolutionAvailable(): Boolean {
        return rewardedAdSolution != null || interstitialSolution != null
    }

    fun isTestMode(): Boolean = USE_TEST_ADS
}