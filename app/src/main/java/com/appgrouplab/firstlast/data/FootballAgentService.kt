package com.appgrouplab.firstlast.data

import android.util.Log
import com.appgrouplab.firstlast.BuildConfig
import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.model.League
import com.appgrouplab.firstlast.model.TeamEntry
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GoogleSearch
import com.google.genai.types.ThinkingConfig
import com.google.genai.types.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val MODELS = listOf("gemini-2.5-flash", "gemini-2.0-flash")
    }

    private val validLeagueKeys = TOURNAMENT_DICTIONARY.keys.toSet()
    private val validTeamKeys   = TEAM_DICTIONARY.keys.toSet()

    // New unified SDK client
    private val client = Client.builder()
        .apiKey(BuildConfig.GEMINI_API_KEY)
        .build()

    // Google Search tool for real-time grounding
    private val searchTool = Tool.builder()
        .googleSearch(GoogleSearch.builder().build())
        .build()

    private val config = GenerateContentConfig.builder()
        .tools(listOf(searchTool))
        .temperature(0.3f)
        .maxOutputTokens(2048)
        .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
        .build()

    // ── Fetch ──────────────────────────────────────────────────────────────────

    suspend fun fetchTodayMatches(today: LocalDate): List<Game> {
        val prompt = buildPrompt(today)
        var lastError: Exception? = null

        Log.d(TAG, "──── PROMPT ────\n$prompt\n────────────────")

        for (model in MODELS) {
            try {
                Log.d(TAG, "Consultando con modelo: $model")

                val text = withContext(Dispatchers.IO) {
                    val response = client.models.generateContent(model, prompt, config)
                    val usage = response.usageMetadata().orElse(null)
                    val promptTokens = usage?.promptTokenCount()?.orElse(null)
                    val totalTokens  = usage?.totalTokenCount()?.orElse(null)
                    Log.d(TAG, "Tokens — prompt: $promptTokens | total: $totalTokens")
                    val rawText = response.text() ?: throw Exception("Respuesta sin texto del modelo")
                    Log.d(TAG, "──── RESPONSE (${rawText.length} chars) ────")
                    rawText.chunked(3000).forEachIndexed { i, chunk ->
                        Log.d(TAG, "[parte ${i + 1}]: $chunk")
                    }
                    Log.d(TAG, "────────────────────────────────────────────")
                    rawText
                }

                val games = parseMatches(text)
                if (games != null) {
                    Log.d(TAG, "Partidos parseados correctamente: ${games.size}")
                    return games
                }
                Log.w(TAG, "parseMatches retornó null, probando fallback")
            } catch (e: Exception) {
                Log.e(TAG, "Error con modelo $model: ${e.message}")
                lastError = e
                continue
            }
        }

        Log.e(TAG, "Todos los modelos fallaron.")
        throw classifyError(lastError)
    }

    // ── Prompt ─────────────────────────────────────────────────────────────────

    private fun buildPrompt(today: LocalDate): String {
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val leagueSection = TOURNAMENT_DICTIONARY.keys.joinToString(",")

        return """
            Actúa como especialista en extracción de datos deportivos. Fecha: $dateStr.
            Ligas: $leagueSection.
            Filtro: Solo partidos donde un equipo sea TOP 5 y el rival sea ÚLTIMOS 5 de su tabla o grupo.

            Reglas Técnicas:
            1. Key: Nombre geográfico completo en snake_case (ej: manchester_city, atletico_madrid).
            2. Name: Nombre público corto para UI (ej: Man City, Atleti).
            3. League: Usa la key exacta de la lista: $leagueSection.

            Salida: JSON puro, sin markdown.
            Formato: {"matches":[{"league":{"name":"Liga","key":"KEY"},"home":{"name":"Público","key":"KEY","pos":1},"away":{"name":"Público","key":"KEY","pos":18},"dateTimeIso":"${dateStr}T00:00:00","leagueSize":20}]}
            Si no hay partidos: {"matches":[]}
        """.trimIndent()
    }

    // ── Parse ──────────────────────────────────────────────────────────────────

    private fun parseMatches(text: String): List<Game>? {
        return try {
            Log.d(TAG, "parseMatches RAW (primeros 800 chars): ${text.take(800)}")

            val clean = text.trim().let { raw ->
                val stripped = if (raw.startsWith("```")) {
                    val nl = raw.indexOf('\n')
                    if (nl != -1) raw.substring(nl + 1) else raw
                } else raw
                if (stripped.trimEnd().endsWith("```")) stripped.trimEnd().dropLast(3).trimEnd()
                else stripped
            }.trim()

            Log.d(TAG, "parseMatches CLEAN (primeros 800 chars): ${clean.take(800)}")

            val root = try {
                Json.parseToJsonElement(clean).jsonObject
            } catch (e: Exception) {
                Log.e(TAG, "Error parseToJsonElement: ${e.message}")
                return null
            }

            val array = root["matches"]?.jsonArray
            if (array == null) {
                Log.e(TAG, "Campo 'matches' no encontrado. Keys: ${root.keys}")
                return emptyList()
            }
            Log.d(TAG, "Número de elementos en 'matches': ${array.size}")

            array.mapNotNull { el ->
                try {
                    val obj = el.jsonObject

                    // league
                    val leagueObj  = obj["league"]?.jsonObject
                    val leagueName = leagueObj?.get("name")?.jsonPrimitive?.content
                    val leagueKey  = leagueObj?.get("key")?.jsonPrimitive?.content

                    // home
                    val homeObj  = obj["home"]?.jsonObject
                    val homeName = homeObj?.get("name")?.jsonPrimitive?.content
                    val homeKey  = homeObj?.get("key")?.jsonPrimitive?.content
                    val homePos  = homeObj?.get("pos")?.jsonPrimitive?.intOrNull

                    // away
                    val awayObj  = obj["away"]?.jsonObject
                    val awayName = awayObj?.get("name")?.jsonPrimitive?.content
                    val awayKey  = awayObj?.get("key")?.jsonPrimitive?.content
                    val awayPos  = awayObj?.get("pos")?.jsonPrimitive?.intOrNull

                    val rawDate = obj["dateTimeIso"]?.jsonPrimitive?.content
                    val size    = obj["leagueSize"]?.jsonPrimitive?.intOrNull ?: 20

                    Log.d(TAG, "Item: league=$leagueKey($leagueName) home=$homeKey($homeName,$homePos) away=$awayKey($awayName,$awayPos) date=$rawDate")

                    if (leagueKey  == null) { Log.w(TAG, "league.key nulo, skip");  return@mapNotNull null }
                    if (leagueName == null) { Log.w(TAG, "league.name nulo, skip"); return@mapNotNull null }
                    if (homeKey    == null) { Log.w(TAG, "home.key nulo, skip");    return@mapNotNull null }
                    if (homeName   == null) { Log.w(TAG, "home.name nulo, skip");   return@mapNotNull null }
                    if (homePos    == null) { Log.w(TAG, "home.pos nulo, skip");    return@mapNotNull null }
                    if (awayKey    == null) { Log.w(TAG, "away.key nulo, skip");    return@mapNotNull null }
                    if (awayName   == null) { Log.w(TAG, "away.name nulo, skip");   return@mapNotNull null }
                    if (awayPos    == null) { Log.w(TAG, "away.pos nulo, skip");    return@mapNotNull null }
                    if (rawDate    == null) { Log.w(TAG, "dateTimeIso nulo, skip"); return@mapNotNull null }

                    // Match keys against dictionaries for logo resolution
                    val resolvedLeagueKey = KeyMatcher.match(leagueKey, validLeagueKeys)
                    val resolvedHomeKey   = KeyMatcher.match(homeKey,   validTeamKeys)
                    val resolvedAwayKey   = KeyMatcher.match(awayKey,   validTeamKeys)

                    Log.d(TAG, "Keys resueltos: league=$resolvedLeagueKey home=$resolvedHomeKey away=$resolvedAwayKey")

                    Game(
                        league      = League(name = leagueName, key = resolvedLeagueKey),
                        home        = TeamEntry(name = homeName, key = resolvedHomeKey, pos = homePos),
                        away        = TeamEntry(name = awayName, key = resolvedAwayKey, pos = awayPos),
                        dateTimeIso = rawDate,
                        leagueSize  = size
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

    // ── Error classification ───────────────────────────────────────────────────

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
}
