package com.appgrouplab.firstlast.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appgrouplab.firstlast.data.GameRepository
import com.appgrouplab.firstlast.data.GameState
import com.appgrouplab.firstlast.data.NotificationPreferences
import com.appgrouplab.firstlast.data.NotificationScheduler
import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.model.League
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class GameViewModel(
    application: Application,
    private val repository: GameRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private companion object { const val TAG = "GameViewModel" }

    private val notificationScheduler  = NotificationScheduler(application)
    private val notificationPreferences = NotificationPreferences(application)

    private val _notificationsEnabled = MutableStateFlow(notificationPreferences.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedLeague = MutableStateFlow<String?>(null)
    val selectedLeague: StateFlow<String?> = _selectedLeague.asStateFlow()

    private var allGames: List<Game> = emptyList()
    private var allLeagues: List<League> = emptyList()

    init { loadTodayMatches() }

    fun retry() {
        _selectedLeague.value = null
        loadTodayMatches()
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationPreferences.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
        if (enabled) {
            notificationScheduler.scheduleAll(allGames)
        } else {
            notificationScheduler.cancelAll()
        }
    }

    fun refresh() {
        viewModelScope.launch(dispatcher) {
            _isRefreshing.value = true
            repository.getTodayMatches().collect { state ->
                if (state !is GameState.Loading) {
                    when (state) {
                        is GameState.Success -> {
                            allGames   = sortAndFilter(state.games)
                            allLeagues = state.leagues
                            if (notificationPreferences.notificationsEnabled)
                                notificationScheduler.scheduleAll(allGames)
                            _uiState.value = GameUiState.Success(allGames, allLeagues)
                        }
                        is GameState.Error -> _uiState.value = GameUiState.Error(state.message)
                        else -> {}
                    }
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun setLeagueFilter(leagueKey: String?) {
        _selectedLeague.value = leagueKey
        applyFilter()
    }

    private fun loadTodayMatches() {
        viewModelScope.launch(dispatcher) {
            _uiState.value = GameUiState.Loading
            val timerJob = launch { delay(3_000) }

            var result: GameUiState = GameUiState.Loading
            repository.getTodayMatches().collect { state ->
                if (state !is GameState.Loading) {
                    result = when (state) {
                        is GameState.Success -> {
                            allGames   = sortAndFilter(state.games)
                            allLeagues = state.leagues
                            if (notificationPreferences.notificationsEnabled)
                                notificationScheduler.scheduleAll(allGames)
                            GameUiState.Success(allGames, allLeagues)
                        }
                        is GameState.Error -> GameUiState.Error(state.message)
                        else -> GameUiState.Loading
                    }
                }
            }

            timerJob.join()
            _uiState.value = result
        }
    }

    private fun applyFilter() {
        val key = _selectedLeague.value
        val filtered = if (key == null) allGames else allGames.filter { it.league.key == key }
        _uiState.value = GameUiState.Success(filtered, allLeagues)
    }

    private fun sortAndFilter(games: List<Game>): List<Game> {
        val nowInstant = Instant.now()

        // TODO: restaurar filtro top5/bottom5 cuando termines de probar la UI
//        val filtered = games.filter { game ->
//            val h = game.home.pos
//            val v = game.away.pos
//            val bottomThreshold = game.leagueSize - 4
//            ((h in 1..5) && (v >= bottomThreshold)) || ((v in 1..5) && (h >= bottomThreshold))
//        }
        val filtered = games

        val (upcoming, past) = filtered.partition { game ->
            try {
                val instant = try {
                    Instant.parse(game.dateTimeIso)
                } catch (_: Exception) {
                    LocalDateTime.parse(game.dateTimeIso).toInstant(ZoneOffset.UTC)
                }
                instant.isAfter(nowInstant)
            } catch (_: Exception) { false }
        }

        Log.d(TAG, "sortAndFilter: ${upcoming.size} próximos + ${past.size} pasados")

        return upcoming.sortedBy { it.dateTimeIso } +
               past.sortedByDescending { it.dateTimeIso }
    }
}
