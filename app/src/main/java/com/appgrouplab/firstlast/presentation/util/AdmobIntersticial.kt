package com.appgrouplab.firstlast.presentation.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val AD_UNIT_ID = "ca-app-pub-3918734194731544/3959526616"

object AdMobManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d("AdMob", "Intersticial listo")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e("AdMob", "Error al cargar: ${error.message}")
                }
            }
        )
    }

    fun showIfReady(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            Log.d("AdMob", "Anuncio no listo todavía")
            onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)   // precarga el siguiente
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                preload(activity)
                onDismiss()
            }
        }
        ad.show(activity)
    }
}
