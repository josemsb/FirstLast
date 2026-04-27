package com.appgrouplab.firstlast.presentation.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {
    private var interstitialAd: InterstitialAd? = null

    fun loadInterstitial(context: Context, showAdd: MutableState<Boolean>) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            "ca-app-pub-3918734194731544/3959526616", // Reemplaza con tu ID de AdMob
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    showAdd.value = true
                    Log.d("AdMob", "Anuncio intersticial cargado")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    showAdd.value = false
                    Log.e("AdMob", "Error al cargar el anuncio: ${adError.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, showAdd: MutableState<Boolean>) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMob", "El usuario cerró el anuncio")
                    interstitialAd = null // Elimina el anuncio después de mostrarlo
                    showAdd.value = false
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e("AdMob", "Error al mostrar el anuncio: ${error.message}")
                    interstitialAd = null
                    showAdd.value = false
                }
            }
            ad.show(activity)
        } ?: Log.d("AdMob", "El anuncio aún no está listo")
    }
}
