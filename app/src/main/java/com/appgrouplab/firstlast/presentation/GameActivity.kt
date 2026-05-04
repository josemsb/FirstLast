package com.appgrouplab.firstlast.presentation

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    // true desde el inicio para usuarios que ya hicieron el onboarding
    private var adsUnlocked = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // permiso respondido (aceptado o denegado) — esperar 2s y habilitar intersticial
        lifecycleScope.launch {
            delay(2_000)
            adsUnlocked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        AdMobManager.preload(this)

        val onboardingPrefs = OnboardingPreferences(this)

        // usuarios existentes: los anuncios están habilitados desde el inicio
        adsUnlocked = onboardingPrefs.onboardingShown

        lifecycleScope.launch {
            gameViewModel.showAd.collect {
                if (adsUnlocked) {
                    AdMobManager.showIfReady(this@GameActivity)
                }
            }
        }

        setContent {
            val showOnboarding = remember { mutableStateOf(!onboardingPrefs.onboardingShown) }

            MaterialTheme {
                ConfigurationStyleIconStatusBar()
                if (showOnboarding.value) {
                    OnboardingScreen(
                        onFinish = {
                            onboardingPrefs.onboardingShown = true
                            showOnboarding.value = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // el launcher callback habilitará adsUnlocked tras 2s
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Android < 13 no necesita permiso: habilitar tras breve pausa
                                lifecycleScope.launch {
                                    delay(2_000)
                                    adsUnlocked = true
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

@Composable
fun ConfigurationStyleIconStatusBar(usaTemaOscuroApp: Boolean = isSystemInDarkTheme()) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            val insetsController = WindowInsetsControllerCompat(window, view)
            insetsController.isAppearanceLightStatusBars = !usaTemaOscuroApp
        }
    }
}
