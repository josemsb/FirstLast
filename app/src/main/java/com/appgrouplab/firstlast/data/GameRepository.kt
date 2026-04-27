package com.appgrouplab.firstlast.data

import android.content.Context
import com.appgrouplab.firstlast.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GeminiGameRepository(
    context: Context,
    private val agentService: FootballAgentService = FootballAgentService()
) {
    private val cache = MatchCacheStorage(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun getTodayMatches(): Flow<GameState> = flow {
        emit(GameState.Loading)

        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Serve from cache if already consulted today
        if (cache.getCachedDate() == todayStr) {
            val cachedJson = cache.getCachedJson()
            if (cachedJson != null) {
                try {
                    val games = json.decodeFromString<List<Game>>(cachedJson)
                    emit(GameState.Success(games))
                    return@flow
                } catch (_: Exception) {
                    cache.clearCache()
                }
            }
        }

        // Query AI agent and cache result for the day
        try {
            val games = agentService.fetchTodayMatches(today)
            cache.saveCache(todayStr, json.encodeToString(games))
            emit(GameState.Success(games))
        } catch (e: Exception) {
            emit(GameState.Error(e.message ?: "Error al consultar partidos"))
        }
    }
}

sealed class GameState {
    object Loading : GameState()
    data class Success(val games: List<Game>) : GameState()
    data class Error(val message: String) : GameState()
}
