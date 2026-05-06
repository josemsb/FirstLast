package com.appgrouplab.firstlast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary   = GreenFistLast,
    secondary = GreenFistLast,
    tertiary  = GreenFistLast
)

@Composable
fun FirstLastTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
