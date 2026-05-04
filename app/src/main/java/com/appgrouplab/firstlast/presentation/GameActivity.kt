package com.appgrouplab.firstlast.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.appgrouplab.firstlast.ui.theme.FirstLastTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.appgrouplab.firstlast.data.OnboardingPreferences
import com.appgrouplab.firstlast.presentation.util.AdMobManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {

    private val gameViewModel by lazy {
        ViewModelProvider(
            this,
            GameModelFactory(application)
        )[GameViewModel::class.java]
    }

    private var adsUnlocked = false
    private var pendingAd   = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        lifecycleScope.launch {
            delay(2_000)
            unlockAds(showPending = false)
        }
    }

    private fun unlockAds(showPending: Boolean = true) {
        adsUnlocked = true
        if (showPending && pendingAd) {
            pendingAd = false
            AdMobManager.showIfReady(this)
        } else {
            pendingAd = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        AdMobManager.preload(this)

        val onboardingPrefs = OnboardingPreferences(this)

        // usuarios existentes: los anuncios están habilitados desde el inicio
        adsUnlocked = onboardingPrefs.onboardingShown

        lifecycleScope.launch {
            gameViewModel.showAd.collect {
                if (adsUnlocked) {
                    AdMobManager.showIfReady(this@GameActivity)
                } else {
                    pendingAd = true
                }
            }
        }

        setContent {
            val showOnboarding = remember { mutableStateOf(!onboardingPrefs.onboardingShown) }

            FirstLastTheme {
                if (showOnboarding.value) {
                    OnboardingScreen(
                        onFinish = {
                            onboardingPrefs.onboardingShown = true
                            showOnboarding.value = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // el launcher callback habilitará adsUnlocked tras 2s
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                lifecycleScope.launch {
                                    delay(2_000)
                                    unlockAds(showPending = false)
                                }
                            }
                        }
                    )
                } else {
                    GameScreen(gameViewModel)
                }
            }
        }
    }
}

