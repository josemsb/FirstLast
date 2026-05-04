package com.appgrouplab.firstlast.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.appgrouplab.firstlast.R

@Composable
fun DashboardTopSection() {
    val greenGradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E8E3E), Color(0xFF1E8E3E))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.25f),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(WavyShape(splitYPosition = 0.9f, amplitude = 60f))
                .background(greenGradientBrush)
        )
        GameHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
        Image(
            painter = painterResource(id = R.drawable.header),
            contentDescription = "Header",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(250.dp)
                .padding(top = 80.dp, end = 60.dp)
        )
    }
}
