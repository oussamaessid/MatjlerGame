package app.matjlergame.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import app.matjlergame.BuildConfig
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
    private var appOpenLoadFailed = false

    companion object {
        private const val TAG = "AdManager"

        // Les builds debug utilisent TOUJOURS les annonces de test Google : afficher/cliquer
        // de vraies annonces depuis votre propre appareil = trafic invalide (risque AdMob).
        private val USE_TEST_ADS = BuildConfig.DEBUG

        // Si l'App Open arrive plus tard que ça après le lancement, on ne l'affiche pas :
        // l'utilisateur est déjà en train de toucher l'écran → clic accidentel.
        private const val APP_OPEN_MAX_WAIT_MS = 4_000L

        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_MODE_SELECT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_BANNER_GAME_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-3940256099942544/5224354917"

        private const val PROD_APP_OPEN_ID = "ca-app-pub-9651830078758870/2364043726"
        private const val PROD_BANNER_MODE_SELECT_ID = "ca-app-pub-9651830078758870/8737880386"
        private const val PROD_BANNER_GAME_ID = "ca-app-pub-9651830078758870/1194432283"
        private const val PROD_REWARDED_VIDEO_EXTRA_TRY_ID = "ca-app-pub-9651830078758870/1243593238"
        private const val PROD_REWARDED_VIDEO_SOLUTION_ID = "ca-app-pub-9651830078758870/7041655337"

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
    }

    fun initialize() {
        try {
            // Ajoutez l'ID de votre vrai téléphone ici (trouvez-le dans Logcat :
            // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("VOTRE_ID"))")
            val testDeviceIds = listOf(
                AdRequest.DEVICE_ID_EMULATOR
                // "VOTRE_ID_APPAREIL_LOGCAT"
            )
            val requestConfig = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

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
        val startMs = System.currentTimeMillis()
        try {
            AppOpenAd.load(
                context,
                APP_OPEN_AD_UNIT_ID,
                AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        isLoadingAppOpen = false
                        if (System.currentTimeMillis() - startMs > APP_OPEN_MAX_WAIT_MS) {
                            Log.d(TAG, "⏱️ Annonce à l'OUVERTURE chargée trop tard → ignorée")
                            return
                        }
                        Log.d(TAG, "✅ Annonce à l'OUVERTURE chargée")
                        appOpenAd = ad
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
        // Jamais par-dessus une activité fermée ou en arrière-plan.
        val resumed = (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
            ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ?: true
        if (activity.isFinishing || activity.isDestroyed || !resumed) {
            appOpenAd = null
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
        if (isLoadingRewardedExtraTry || rewardedAdExtraTry != null) return
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
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Rewarded EXTRA TRY: ${adError.message}")
                        rewardedAdExtraTry = null
                        isLoadingRewardedExtraTry = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded EXTRA TRY", e)
            isLoadingRewardedExtraTry = false
        }
    }

    fun showRewardedAdExtraTry(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        when {
            rewardedAdExtraTry != null -> showRewardedExtraTry(activity, onRewarded, onAdDismissed)
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
    // REWARDED — SOLUTION
    // ─────────────────────────────────────────────────────────────

    fun loadRewardedAdSolution(onAdLoaded: () -> Unit = {}) {
        if (isLoadingRewardedSolution || rewardedAdSolution != null) return
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
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "❌ Échec Rewarded SOLUTION: ${adError.message}")
                        rewardedAdSolution = null
                        isLoadingRewardedSolution = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Rewarded SOLUTION", e)
            isLoadingRewardedSolution = false
        }
    }

    fun showRewardedAdSolution(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        when {
            rewardedAdSolution != null -> showRewardedSolution(activity, onRewarded, onAdDismissed)
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
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    fun isRewardedAdExtraTryAvailable(): Boolean {
        return rewardedAdExtraTry != null
    }

    fun isRewardedAdSolutionAvailable(): Boolean {
        return rewardedAdSolution != null
    }

    fun isTestMode(): Boolean = USE_TEST_ADS
}