package com.appgrouplab.firstlast.presentation

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.appgrouplab.firstlast.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val AD_UNIT_ID = "ca-app-pub-3918734194731544/5581758069"

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AD_UNIT_ID)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose { nativeAd?.destroy() }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory  = { ctx ->
                val view = LayoutInflater.from(ctx)
                    .inflate(R.layout.native_ad, null) as NativeAdView
                populateNativeAdView(ad, view)
                view
            },
            update = { view -> populateNativeAdView(ad, view) }
        )
    }
}

private fun populateNativeAdView(ad: NativeAd, view: NativeAdView) {
    val headline  = view.findViewById<android.widget.TextView>(R.id.tv_headline)
    val body      = view.findViewById<android.widget.TextView>(R.id.tv_body)
    val cta       = view.findViewById<android.widget.Button>(R.id.btn_cta)
    val mediaView = view.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.media_view)

    view.mediaView = mediaView
    mediaView.mediaContent = ad.mediaContent

    headline.text = ad.headline
    body.text     = ad.body ?: ""
    cta.text      = ad.callToAction ?: "Ver más"

    view.headlineView    = headline
    view.bodyView        = body
    view.callToActionView = cta

    view.setNativeAd(ad)
}
