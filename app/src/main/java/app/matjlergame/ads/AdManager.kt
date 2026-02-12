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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var rewardedAdExtraTry: RewardedAd? = null
    private var rewardedAdSolution: RewardedAd? = null
    private var isLoadingAppOpen = false
    private var isLoadingRewardedExtraTry = false
    private var isLoadingRewardedSolution = false

    private var hasShownAppOpenAd = false

    companion object {
        private const val TAG = "AdManager"

        private const val USE_TEST_ADS = false

        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_MODE_SELECT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_BANNER_GAME_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-3940256099942544/5224354917"

        // IDs réels (pour la production)
        private const val PROD_APP_OPEN_ID = "ca-app-pub-4161995857939030/6856571786"
        private const val PROD_BANNER_MODE_SELECT_ID = "ca-app-pub-4161995857939030/7131903958"
        private const val PROD_BANNER_GAME_ID = "ca-app-pub-4161995857939030/7494099094"
        private const val PROD_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-4161995857939030/7582944991"
        private const val PROD_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-4161995857939030/2664816346"

        // ID pour Annonce à l'ouverture
        val APP_OPEN_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_APP_OPEN_ID else PROD_APP_OPEN_ID

        // ID pour bannière écran de sélection de mode
        val BANNER_MODE_SELECT_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_MODE_SELECT_ID else PROD_BANNER_MODE_SELECT_ID

        // ID pour bannière écran de jeu
        val BANNER_GAME_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_GAME_ID else PROD_BANNER_GAME_ID

        // ID pour vidéo avec récompense - Essai supplémentaire
        val REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_VIDEO_EXTRA_TRY_ID else PROD_REWARDED_VIDEO_EXTRA_TRY_ID

        // ID pour vidéo avec récompense - Révéler la solution
        val REWARDED_VIDEO_SOLUTION_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_VIDEO_SOLUTION_ID else PROD_REWARDED_VIDEO_SOLUTION_ID
    }

    fun initialize() {
        MobileAds.initialize(context) {
            val mode = if (USE_TEST_ADS) "TEST" else "PRODUCTION"
            Log.d(TAG, "AdMob initialisé en mode: $mode")
            Log.d(TAG, "App Open ID: $APP_OPEN_AD_UNIT_ID")
            Log.d(TAG, "Banner Mode Select ID: $BANNER_MODE_SELECT_AD_UNIT_ID")
            Log.d(TAG, "Banner Game ID: $BANNER_GAME_AD_UNIT_ID")
            Log.d(TAG, "Rewarded Video Extra Try ID: $REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID")
            Log.d(TAG, "Rewarded Video Solution ID: $REWARDED_VIDEO_SOLUTION_AD_UNIT_ID")
        }
    }

    /**
     * Charge la publicité à l'ouverture de l'app (App Open Ad)
     */
    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        if (isLoadingAppOpen) {
            Log.d(TAG, "Annonce à l'ouverture déjà en cours de chargement")
            return
        }

        isLoadingAppOpen = true
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
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "❌ Échec chargement OUVERTURE: ${adError.message}")
                    appOpenAd = null
                    isLoadingAppOpen = false
                }
            }
        )
    }

    /**
     * Charge la vidéo avec récompense pour essai supplémentaire
     */
    fun loadRewardedAdExtraTry(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedExtraTry) {
            Log.d(TAG, "Vidéo Extra Try déjà en cours de chargement")
            return
        }

        isLoadingRewardedExtraTry = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_VIDEO_EXTRA_TRY_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Vidéo EXTRA TRY chargée")
                    rewardedAdExtraTry = ad
                    isLoadingRewardedExtraTry = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "❌ Échec chargement EXTRA TRY: ${adError.message}")
                    rewardedAdExtraTry = null
                    isLoadingRewardedExtraTry = false
                }
            }
        )
    }

    fun loadRewardedAdSolution(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedSolution) {
            Log.d(TAG, "Vidéo Solution déjà en cours de chargement")
            return
        }

        isLoadingRewardedSolution = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_VIDEO_SOLUTION_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Vidéo SOLUTION chargée")
                    rewardedAdSolution = ad
                    isLoadingRewardedSolution = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "❌ Échec chargement SOLUTION: ${adError.message}")
                    rewardedAdSolution = null
                    isLoadingRewardedSolution = false
                }
            }
        )
    }

    /**
     * Affiche l'annonce à l'ouverture
     */
    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (hasShownAppOpenAd) {
            Log.d(TAG, "⏭️ Annonce à l'ouverture déjà affichée")
            onAdDismissed()
            return
        }

        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Annonce à l'OUVERTURE fermée")
                    appOpenAd = null
                    hasShownAppOpenAd = true
                    onAdDismissed()
                    // Recharger pour une prochaine utilisation
                    loadAppOpenAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage OUVERTURE: ${adError.message}")
                    appOpenAd = null
                    hasShownAppOpenAd = true
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Annonce à l'OUVERTURE affichée")
                }
            }
            appOpenAd?.show(activity)
        } else {
            Log.d(TAG, "⏳ Annonce à l'OUVERTURE pas encore chargée")
            onAdDismissed()
        }
    }

    fun showRewardedAdExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        if (rewardedAdExtraTry != null) {
            rewardedAdExtraTry?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Vidéo EXTRA TRY fermée")
                    rewardedAdExtraTry = null
                    onAdDismissed()
                    // Recharger pour une prochaine utilisation
                    loadRewardedAdExtraTry()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage EXTRA TRY: ${adError.message}")
                    rewardedAdExtraTry = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Vidéo EXTRA TRY affichée")
                }
            }

            rewardedAdExtraTry?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "🎁 Récompense EXTRA TRY gagnée: $rewardAmount $rewardType")
                onRewarded()
            }
        } else {
            Log.d(TAG, "⏳ Vidéo EXTRA TRY pas encore chargée")
            onAdDismissed()
        }
    }

    fun showRewardedAdSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        if (rewardedAdSolution != null) {
            rewardedAdSolution?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Vidéo SOLUTION fermée")
                    rewardedAdSolution = null
                    onAdDismissed()
                    // Recharger pour une prochaine utilisation
                    loadRewardedAdSolution()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage SOLUTION: ${adError.message}")
                    rewardedAdSolution = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Vidéo SOLUTION affichée")
                }
            }

            rewardedAdSolution?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "🎁 Récompense SOLUTION gagnée: $rewardAmount $rewardType")
                onRewarded()
            }
        } else {
            Log.d(TAG, "⏳ Vidéo SOLUTION pas encore chargée")
            onAdDismissed()
        }
    }

    fun isRewardedAdExtraTryAvailable(): Boolean {
        return rewardedAdExtraTry != null
    }

    fun isRewardedAdSolutionAvailable(): Boolean {
        return rewardedAdSolution != null
    }

    fun isTestMode(): Boolean = USE_TEST_ADS
}