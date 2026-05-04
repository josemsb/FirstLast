package com.appgrouplab.firstlast.data

import android.util.Log
import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.model.League
import com.appgrouplab.firstlast.model.TeamEntry
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GameRepository {

    private companion object { const val TAG = "GameRepository" }

    private val db = Firebase.firestore

    fun getTodayMatches(): Flow<GameState> = flow {
        emit(GameState.Loading)
        try {
            // Cargar equipos y torneos en paralelo
            val teamsSnap       = db.collection("team").get().await()
            val tournamentsSnap = db.collection("tournament").get().await()

            val teams       = teamsSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }
            val tournaments = tournamentsSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }

            // TODO: restaurar filtro por fecha cuando termines de probar
//            val today = LocalDate.now(ZoneOffset.UTC)
//            val startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant()
//            val endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

            val gamesSnap = db.collection("game")
//                .whereGreaterThanOrEqualTo("date", Timestamp(startOfDay.epochSecond, 0))
//                .whereLessThan("date", Timestamp(endOfDay.epochSecond, 0))
                .get()
                .await()

            val allLeagues = tournamentsSnap.documents.map { doc ->
                League(name = doc.getString("name") ?: doc.id, key = doc.id)
            }

            val games = gamesSnap.documents.mapNotNull { doc ->
                try {
                    val season       = doc.getString("season")       ?: return@mapNotNull null
                    val homeTeam     = doc.getString("homeTeam")     ?: return@mapNotNull null
                    val visitingTeam = doc.getString("visitingTeam") ?: return@mapNotNull null
                    val homePos      = doc.getLong("homePosition")?.toInt()     ?: return@mapNotNull null
                    val awayPos      = doc.getLong("visitingPosition")?.toInt() ?: return@mapNotNull null
                    val timestamp    = doc.getTimestamp("date")      ?: return@mapNotNull null

                    val leagueName = tournaments[season]  ?: season
                    val homeName   = teams[homeTeam]      ?: homeTeam
                    val awayName   = teams[visitingTeam]  ?: visitingTeam

                    val dateIso = timestamp.toDate().toInstant()
                        .atZone(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                    Game(
                        league      = League(name = leagueName, key = season),
                        home        = TeamEntry(name = homeName, key = homeTeam, pos = homePos),
                        away        = TeamEntry(name = awayName, key = visitingTeam, pos = awayPos),
                        dateTimeIso = dateIso,
                        leagueSize  = 20
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al mapear documento ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Partidos cargados de Firestore: ${games.size}")
            emit(GameState.Success(games, allLeagues))
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar Firestore: ${e.message}")
            emit(GameState.Error(e.message ?: "Error al cargar partidos"))
        }
    }
}

sealed class GameState {
    object Loading : GameState()
    data class Success(val games: List<Game>, val leagues: List<com.appgrouplab.firstlast.model.League>) : GameState()
    data class Error(val message: String) : GameState()
}
