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
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp

class GameActivity : ComponentActivity() {

    private val gameViewModel by lazy {
        ViewModelProvider(
            this,
            GameModelFactory(application)
        )[GameViewModel::class.java]
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permiso otorgado o denegado — las notificaciones se programan igual */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
//            val shouldAttemptAd = remember { Random.nextFloat() < 0.7f }
//            if (shouldAttemptAd) {
//                val showAdd = rememberSaveable { mutableStateOf(false) }
//                LaunchedEffect(Unit) {
//                    AdMobManager.loadInterstitial(this@GameActivity, showAdd)
//                }
//
//                if (showAdd.value) {
//                    AdMobManager.showInterstitial(this@GameActivity, showAdd)
//                    showAdd.value = false
//                }
//            }
            MaterialTheme {
                ConfigurationStyleIconStatusBar()
                GameScreen(gameViewModel)
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
