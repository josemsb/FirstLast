package com.appgrouplab.firstlast.data

import android.content.Context
import android.util.Log
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
    private companion object { const val TAG = "GameRepository" }

    private val cache = MatchCacheStorage(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun getTodayMatches(): Flow<GameState> = flow {
        Log.d(TAG, "▶ getTodayMatches iniciado")
        emit(GameState.Loading)

        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Log.d(TAG, "Fecha de hoy: $todayStr")

        // Serve from cache if already consulted today
        val cachedDate = cache.getCachedDate()
        Log.d(TAG, "Cache fecha guardada: $cachedDate")
        if (cachedDate == todayStr) {
            val cachedJson = cache.getCachedJson()
            if (cachedJson != null) {
                try {
                    val games = json.decodeFromString<List<Game>>(cachedJson)
                    Log.d(TAG, "✅ Sirviendo desde cache: ${games.size} partidos")
                    emit(GameState.Success(games))
                    return@flow
                } catch (e: Exception) {
                    Log.e(TAG, "Error al leer cache, limpiando: ${e.message}")
                    cache.clearCache()
                }
            }
        }

        // Query AI agent and cache result for the day
        Log.d(TAG, "Llamando al agente Gemini...")
        try {
            val games = agentService.fetchTodayMatches(today)
            Log.d(TAG, "Agente devolvió ${games.size} partidos")
            cache.saveCache(todayStr, json.encodeToString(games))
            emit(GameState.Success(games))
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar agente: ${e.message}")
            emit(GameState.Error(e.message ?: "Error al consultar partidos"))
        }
    }
}

sealed class GameState {
    object Loading : GameState()
    data class Success(val games: List<Game>) : GameState()
    data class Error(val message: String) : GameState()
}
