package com.appgrouplab.firstlast.data

import android.util.Log
import com.appgrouplab.firstlast.BuildConfig
import com.appgrouplab.firstlast.model.Game
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FootballAgentService {

    private companion object {
        const val TAG = "FootballAgent"
        const val API_KEY = BuildConfig.GEMINI_API_KEY
        const val GEMINI_2_5_FLASH = "gemini-2.5-flash"
        const val GEMINI_2_5_FLASH_LITE = "gemini-2.0-flash"
    }

    private val generationConfig = generationConfig {
        temperature = 0.3f
        topK = 40
        topP = 0.95f
        maxOutputTokens = 512
        stopSequences = listOf("STOP")
    }

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
    )

    private val models = listOf(
        GenerativeModel(
            modelName = GEMINI_2_5_FLASH,
            apiKey = API_KEY,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        ),
        GenerativeModel(
            modelName = GEMINI_2_5_FLASH_LITE,
            apiKey = API_KEY,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )
    )

    // Precomputed key sets for matching (built once at service creation)
    private val validLeagueKeys = TOURNAMENT_DICTIONARY.keys.toSet()
    private val validTeamKeys   = TEAM_DICTIONARY.keys.toSet()

    // ── Fetch ──────────────────────────────────────────────────────────────────

    suspend fun fetchTodayMatches(today: LocalDate): List<Game> {
        val prompt = buildPrompt(today)
        var lastError: Exception? = null

        for (model in models) {
            try {
                Log.d(TAG, "Consultando con modelo: ${model.modelName}")
                val response = model.generateContent(prompt)
                val usage = response.usageMetadata
                Log.d(TAG, "Tokens — prompt: ${usage?.promptTokenCount} | respuesta: ${usage?.candidatesTokenCount} | total: ${usage?.totalTokenCount}")
                val text = response.text
                Log.d(TAG, "Respuesta Gemini longitud: ${text?.length} chars")
                text?.chunked(3000)?.forEachIndexed { i, chunk ->
                    Log.d(TAG, "Respuesta Gemini [parte ${i + 1}]: $chunk")
                }
                if (text == null) { Log.w(TAG, "Respuesta vacía, probando fallback"); continue }
                val games = parseMatches(text)
                if (games != null) {
                    Log.d(TAG, "Partidos parseados correctamente: ${games.size}")
                    return games
                }
                Log.w(TAG, "parseMatches retornó null, probando fallback")
            } catch (e: Exception) {
                Log.e(TAG, "Error con modelo ${model.modelName}: ${e.message}")
                lastError = e
                continue
            }
        }

        Log.e(TAG, "Todos los modelos fallaron.")
        throw classifyError(lastError)
    }

    private fun classifyError(e: Exception?): Exception {
        val msg = e?.message?.lowercase() ?: ""
        return when {
            msg.contains("quota") || msg.contains("429") ->
                Exception("Límite de consultas alcanzado. Intenta en unos minutos.")
            msg.contains("not_found") || msg.contains("404") ->
                Exception("Modelo no disponible temporalmente.")
            msg.contains("unable to resolve") || msg.contains("failed to connect")
                    || msg.contains("network") || msg.contains("socket") ->
                Exception("Sin conexión a internet. Verifica tu red.")
            msg.contains("api_key") || msg.contains("invalid") || msg.contains("401") ->
                Exception("Error de autenticación con el servicio.")
            else ->
                Exception("No se pudo consultar el servicio. ${e?.message ?: ""}")
        }
    }

    // ── Prompt (generated dynamically from the dictionaries) ──────────────────

    private fun buildPrompt(today: LocalDate): String {
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Leagues as CSV (auto-updated when dict changes)
        val leagues = TOURNAMENT_DICTIONARY.keys.joinToString(",")

        // Teams as CSV (auto-updated when dict changes)
        val teams = TEAM_DICTIONARY.keys.joinToString(",")

        return """
            Fixtures $dateStr. JSON only, no markdown.
            LIGAS(key exacto): $leagues
            REGLA: partido donde un equipo es posición 1-5 Y el otro en las últimas 5 del total de equipos de esa liga.
            EQUIPOS(key exacto): $teams
            Si el equipo no está en la lista usa el snake_case más parecido.
            Responde SOLO: {"matches":[{"league":"KEY","homeTeam":"KEY","homePosition":1,"visitingTeam":"KEY","visitingPosition":18,"dateTimeIso":"${dateStr}T20:00:00","leagueSize":20}]}
            Sin partidos: {"matches":[]}
        """.trimIndent()
    }

    // ── Parse + match against dictionaries ────────────────────────────────────

    private fun parseMatches(text: String): List<Game>? {
        return try {
            Log.d(TAG, "parseMatches RAW (primeros 800 chars): ${text.take(800)}")

            val clean = text.trim()
                .let { raw ->
                    // Strip optional leading ```json or ``` fence
                    val stripped = if (raw.startsWith("```")) {
                        val firstNewline = raw.indexOf('\n')
                        if (firstNewline != -1) raw.substring(firstNewline + 1) else raw
                    } else raw
                    // Strip trailing ```
                    if (stripped.trimEnd().endsWith("```"))
                        stripped.trimEnd().dropLast(3).trimEnd()
                    else stripped
                }
                .trim()

            Log.d(TAG, "parseMatches CLEAN (primeros 800 chars): ${clean.take(800)}")

            val root = try {
                Json.parseToJsonElement(clean).jsonObject
            } catch (e: Exception) {
                Log.e(TAG, "Error parseToJsonElement: ${e.message}")
                return null
            }

            val array = root["matches"]?.jsonArray
            if (array == null) {
                Log.e(TAG, "Campo 'matches' no encontrado. Keys presentes: ${root.keys}")
                return emptyList()
            }
            Log.d(TAG, "Número de elementos en 'matches': ${array.size}")

            array.mapNotNull { el ->
                try {
                    val obj      = el.jsonObject
                    val homePos  = obj["homePosition"]?.jsonPrimitive?.intOrNull
                    val visitPos = obj["visitingPosition"]?.jsonPrimitive?.intOrNull
                    val size     = obj["leagueSize"]?.jsonPrimitive?.intOrNull ?: 20

                    val rawLeague = obj["league"]?.jsonPrimitive?.content
                    val rawHome   = obj["homeTeam"]?.jsonPrimitive?.content
                    val rawVisit  = obj["visitingTeam"]?.jsonPrimitive?.content
                    val rawDate   = obj["dateTimeIso"]?.jsonPrimitive?.content

                    Log.d(TAG, "Item: league=$rawLeague home=$rawHome($homePos) visit=$rawVisit($visitPos) date=$rawDate size=$size")

                    if (homePos == null)  { Log.w(TAG, "homePosition nulo, skip"); return@mapNotNull null }
                    if (visitPos == null) { Log.w(TAG, "visitingPosition nulo, skip"); return@mapNotNull null }
                    if (rawLeague == null){ Log.w(TAG, "league nulo, skip"); return@mapNotNull null }
                    if (rawHome == null)  { Log.w(TAG, "homeTeam nulo, skip"); return@mapNotNull null }
                    if (rawVisit == null) { Log.w(TAG, "visitingTeam nulo, skip"); return@mapNotNull null }
                    if (rawDate == null)  { Log.w(TAG, "dateTimeIso nulo, skip"); return@mapNotNull null }

                    val leagueKey = KeyMatcher.match(rawLeague, validLeagueKeys)
                    val homeKey   = KeyMatcher.match(rawHome,   validTeamKeys)
                    val visitKey  = KeyMatcher.match(rawVisit,  validTeamKeys)

                    Log.d(TAG, "Keys resueltos: league=$leagueKey home=$homeKey visit=$visitKey")

                    Game(
                        dateTimeIso      = rawDate,
                        homePosition     = homePos,
                        homeTeam         = homeKey,
                        season           = leagueKey,
                        visitingPosition = visitPos,
                        visitingTeam     = visitKey,
                        leagueSize       = size
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción parseando item: ${e.message} — item=$el")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción general en parseMatches: ${e.message}")
            null
        }
    }
}
