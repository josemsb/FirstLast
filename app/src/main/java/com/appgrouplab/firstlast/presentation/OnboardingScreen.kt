package com.appgrouplab.firstlast.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appgrouplab.firstlast.ui.theme.GreenFistLast
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        emoji       = "🏆 vs 🔻",
        title       = "Top 5 contra Bottom 5",
        description = "Solo te mostramos los partidos donde los líderes de la tabla se enfrentan a los últimos.\nLos duelos con más tensión, los que más importan."
    ),
    OnboardingPage(
        emoji       = "🔔",
        title       = "Nunca te pierdas un partido",
        description = "Activa las notificaciones y te avisamos 1 hora antes de que empiece el partido para que no te pilles de sorpresa."
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState  = rememberPagerState(pageCount = { pages.size })
    val scope       = rememberCoroutineScope()
    val isLastPage  = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }

        // Botón Saltar
        if (!isLastPage) {
            TextButton(
                onClick  = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                Text("Saltar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }

        // Indicadores + botón
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DotsIndicator(
                count   = pages.size,
                current = pagerState.currentPage
            )
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenFistLast)
            ) {
                Text(
                    text       = if (isLastPage) "¡Empezar!" else "Siguiente",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = page.emoji, fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text       = page.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = GreenFistLast,
            textAlign  = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = page.description,
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}

@Composable
private fun DotsIndicator(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 24.dp else 8.dp,
                label       = "dot_width"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (index == current) GreenFistLast else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
