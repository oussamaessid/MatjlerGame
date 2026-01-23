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

class AdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var timedInterstitialAd: InterstitialAd? = null
    private var isLoadingAppOpen = false
    private var isLoadingTimed = false

    // Timer pour la publicité après 4 minutes
    private var appStartTime: Long = 0
    private var hasShownTimedAd = false
    private var hasShownAppOpenAd = false

    companion object {
        private const val TAG = "AdManager"

        // ⚠️ CHANGEZ CETTE VALEUR À false POUR LA PRODUCTION ⚠️
        private const val USE_TEST_ADS = true

        // Temps d'attente avant la pub (4 minutes en millisecondes)
        private const val AD_DELAY_MILLIS = 4 * 60 * 1000L // 4 minutes

        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_TIMED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_BANNER_MODE_SELECT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_BANNER_GAME_ID = "ca-app-pub-3940256099942544/6300978111"

        // IDs réels (pour la production)
        private const val PROD_APP_OPEN_ID = "ca-app-pub-4161995857939030/6856571786"
        private const val PROD_TIMED_INTERSTITIAL_ID = "ca-app-pub-4161995857939030/9535717778"
        private const val PROD_BANNER_MODE_SELECT_ID = "ca-app-pub-4161995857939030/7131903958"
        private const val PROD_BANNER_GAME_ID = "ca-app-pub-4161995857939030/7494099094"

        // ID pour Annonce à l'ouverture
        val APP_OPEN_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_APP_OPEN_ID else PROD_APP_OPEN_ID

        // ID pour publicité interstitielle après 4 minutes
        val TIMED_INTERSTITIAL_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_TIMED_INTERSTITIAL_ID else PROD_TIMED_INTERSTITIAL_ID

        // ID pour bannière écran de sélection de mode
        val BANNER_MODE_SELECT_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_MODE_SELECT_ID else PROD_BANNER_MODE_SELECT_ID

        // ID pour bannière écran de jeu
        val BANNER_GAME_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_GAME_ID else PROD_BANNER_GAME_ID
    }

    fun initialize() {
        MobileAds.initialize(context) {
            val mode = if (USE_TEST_ADS) "TEST" else "PRODUCTION"
            Log.d(TAG, "AdMob initialisé en mode: $mode")
            Log.d(TAG, "App Open ID: $APP_OPEN_AD_UNIT_ID")
            Log.d(TAG, "Timed Interstitial ID: $TIMED_INTERSTITIAL_AD_UNIT_ID")
            Log.d(TAG, "Banner Mode Select ID: $BANNER_MODE_SELECT_AD_UNIT_ID")
            Log.d(TAG, "Banner Game ID: $BANNER_GAME_AD_UNIT_ID")
        }

        // Enregistrer l'heure de démarrage
        appStartTime = System.currentTimeMillis()
        Log.d(TAG, "⏱️ Timer démarré - Publicité dans 4 minutes")
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
     * Charge la publicité temporisée (affichée après 4 minutes)
     */
    fun loadTimedInterstitialAd(onAdLoaded: () -> Unit = {}) {
        if (isLoadingTimed) {
            Log.d(TAG, "Publicité temporisée déjà en cours de chargement")
            return
        }

        isLoadingTimed = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TIMED_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Publicité TEMPORISÉE chargée (4 min)")
                    timedInterstitialAd = ad
                    isLoadingTimed = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "❌ Échec chargement TEMPORISÉE: ${adError.message}")
                    timedInterstitialAd = null
                    isLoadingTimed = false
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

    /**
     * Vérifie si 4 minutes se sont écoulées et affiche la pub si nécessaire
     */
    fun checkAndShowTimedAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (hasShownTimedAd) {
            Log.d(TAG, "⏭️ Publicité de 4 min déjà affichée")
            return
        }

        val elapsedTime = System.currentTimeMillis() - appStartTime
        val remainingTime = AD_DELAY_MILLIS - elapsedTime

        if (elapsedTime >= AD_DELAY_MILLIS) {
            Log.d(TAG, "⏰ 4 minutes écoulées ! Affichage de la publicité")
            showTimedInterstitialAd(activity, onAdDismissed)
        } else {
            val remainingMinutes = remainingTime / 60000
            val remainingSeconds = (remainingTime % 60000) / 1000
            Log.d(TAG, "⏱️ Temps restant: ${remainingMinutes}m ${remainingSeconds}s")
        }
    }

    /**
     * Affiche la publicité temporisée (après 4 minutes)
     */
    private fun showTimedInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (timedInterstitialAd != null) {
            timedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Publicité de 4 MIN fermée")
                    timedInterstitialAd = null
                    hasShownTimedAd = true
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Échec affichage 4 MIN: ${adError.message}")
                    timedInterstitialAd = null
                    hasShownTimedAd = true
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Publicité de 4 MIN affichée")
                }
            }
            timedInterstitialAd?.show(activity)
        } else {
            Log.d(TAG, "⏳ Publicité de 4 MIN pas encore chargée")
            // Marquer comme affichée même si échec pour éviter les tentatives répétées
            hasShownTimedAd = true
            onAdDismissed()
        }
    }

    /**
     * Obtenir le temps écoulé depuis le démarrage (en secondes)
     */
    fun getElapsedTimeSeconds(): Long {
        return (System.currentTimeMillis() - appStartTime) / 1000
    }

    /**
     * Réinitialiser le timer (si besoin)
     */
    fun resetTimer() {
        appStartTime = System.currentTimeMillis()
        hasShownTimedAd = false
        Log.d(TAG, "🔄 Timer réinitialisé")
    }

    fun isTestMode(): Boolean = USE_TEST_ADS
}