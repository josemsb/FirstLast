package com.appgrouplab.firstlast.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appgrouplab.firstlast.model.League
import com.appgrouplab.firstlast.ui.theme.GreenFistLast
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState        by viewModel.uiState.collectAsState()
    val selectedLeague by viewModel.selectedLeague.collectAsState()
    val isRefreshing   by viewModel.isRefreshing.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardTopSection()
            when (val state = uiState) {
                is GameUiState.Loading -> LoadingScreen()

                is GameUiState.Success -> {
                    LeagueFilterRow(
                        leagues           = state.leagues,
                        selectedLeagueKey = selectedLeague,
                        onSelect          = { viewModel.setLeagueFilter(it) }
                    )
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh    = { viewModel.refresh() },
                        modifier     = Modifier.fillMaxSize()
                    ) {
                        if (state.games.isEmpty()) {
                            EmptyMatchesScreen()
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 60.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.games) { game ->
                                    GameCard(game = game)
                                }
                            }
                        }
                    }
                }

                is GameUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No se pudieron cargar los partidos",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueFilterRow(
    leagues: List<League>,
    selectedLeagueKey: String?,
    onSelect: (String?) -> Unit
) {
    if (leagues.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedLeagueKey == null,
            onClick  = { onSelect(null) },
            label    = { Text("Todas") },
            colors   = FilterChipDefaults.filterChipColors(
                selectedContainerColor     = GreenFistLast,
                selectedLabelColor         = Color.White
            )
        )
        leagues.forEach { league ->
            FilterChip(
                selected = selectedLeagueKey == league.key,
                onClick  = { onSelect(if (selectedLeagueKey == league.key) null else league.key) },
                label    = { Text(league.name) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenFistLast,
                    selectedLabelColor     = Color.White
                )
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    val leagues = listOf(
        "LaLiga", "Premier League", "Ligue 1", "Serie A", "Bundesliga",
        "Liga 1", "Liga Profesional", "Brasileirão Serie A",
        "Liga MX", "MLS", "Eredivisie", "Primeira Liga",
        "Jupiler Pro League", "Superliga de Turquía",
        "Campeonato Nacional", "Primera División",
        "Categoría Primera A", "Serie A Ecuador"
    )

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            index = (index + 1) % leagues.size
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Text(text = "⚽", fontSize = 56.sp, modifier = Modifier.scale(scale))

            Text(
                text = "Tu agente de IA en deportes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GreenFistLast,
                textAlign = TextAlign.Center
            )

            Text(
                text = "está buscando los partidos de las ligas...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            AnimatedContent(
                targetState = leagues[index],
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "leagueAnim"
            ) { league ->
                Text(
                    text = "🏆 $league",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenFistLast,
                    textAlign = TextAlign.Center
                )
            }

            CircularProgressIndicator(
                color = GreenFistLast,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun EmptyMatchesScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Text(text = "😴", fontSize = 56.sp)
            Text(
                text = "Sin partidos hoy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GreenFistLast,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Te hemos buscado los partidos del top 5 vs los últimos 5 de cada tabla y hoy no hay.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "↓ Desliza hacia abajo para volver a buscar",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
