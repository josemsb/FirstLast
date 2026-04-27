package com.appgrouplab.firstlast.presentation

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.appgrouplab.firstlast.presentation.util.AdMobManager
import com.google.firebase.FirebaseApp
import kotlin.random.Random

class GameActivity : ComponentActivity() {

    private val gameViewModel by lazy {
        ViewModelProvider(
            this,
            GameModelFactory(this)
        )[GameViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            val shouldAttemptAd = remember { Random.nextFloat() < 0.7f }
            if (shouldAttemptAd) {
                val showAdd = rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    AdMobManager.loadInterstitial(this@GameActivity, showAdd)
                }

                if (showAdd.value) {
                    AdMobManager.showInterstitial(this@GameActivity, showAdd)
                    showAdd.value = false
                }
            }
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