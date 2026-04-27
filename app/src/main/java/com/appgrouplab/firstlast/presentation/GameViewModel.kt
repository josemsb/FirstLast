package com.appgrouplab.firstlast.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appgrouplab.firstlast.BuildConfig
import com.appgrouplab.firstlast.data.GameState
import com.appgrouplab.firstlast.data.GeminiGameRepository
import com.appgrouplab.firstlast.model.Game
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class GameViewModel(
    private val repository: GeminiGameRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private companion object { const val TAG = "GameViewModel" }

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "▶ ViewModel creado")
        Log.d(TAG, "API Key presente: ${BuildConfig.GEMINI_API_KEY.isNotEmpty()}")
        loadTodayMatches()
    }

    fun retry() = loadTodayMatches()

    private fun loadTodayMatches() {
        Log.d(TAG, "loadTodayMatches() llamado")
        viewModelScope.launch(dispatcher) {
            repository.getTodayMatches().collect { state ->
                Log.d(TAG, "Estado recibido: $state")
                _uiState.value = when (state) {
                    is GameState.Loading -> GameUiState.Loading
                    is GameState.Success -> GameUiState.Success(sortAndFilter(state.games))
                    is GameState.Error   -> GameUiState.Error(state.message)
                }
            }
        }
    }

    private fun sortAndFilter(games: List<Game>): List<Game> {
        val now = LocalDateTime.now()

        Log.d(TAG, "sortAndFilter: recibidos ${games.size} partidos del agente")

        // Filter: one team in top 5, other in the last 5 of the table
        val filtered = games.filter { game ->
            val h = game.homePosition
            val v = game.visitingPosition
            val bottomThreshold = game.leagueSize - 4
            val passes = ((h in 1..5) && (v >= bottomThreshold)) ||
                         ((v in 1..5) && (h >= bottomThreshold))
            if (!passes) Log.d(TAG, "  Descartado: ${game.homeTeam}($h) vs ${game.visitingTeam}($v) liga=${game.season} size=${game.leagueSize} umbral=$bottomThreshold")
            passes
        }

        Log.d(TAG, "sortAndFilter: ${filtered.size} partidos tras filtro top5/bottom5")

        // Partition: upcoming matches (dateTime > now) vs past (dateTime <= now)
        val (upcoming, past) = filtered.partition { game ->
            try { LocalDateTime.parse(game.dateTimeIso).isAfter(now) }
            catch (_: Exception) { false }
        }

        Log.d(TAG, "sortAndFilter: ${upcoming.size} próximos + ${past.size} pasados")

        // Upcoming: soonest first; Past: most recently started first
        return upcoming.sortedBy { it.dateTimeIso } +
               past.sortedByDescending { it.dateTimeIso }
    }
}
