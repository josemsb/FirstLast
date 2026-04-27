package com.appgrouplab.firstlast.data

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
import java.time.format.TextStyle
import java.util.Locale

class FootballAgentService {

    private companion object {
        const val API_KEY = BuildConfig.GEMINI_API_KEY
        const val GEMINI_2_5_FLASH_LITE = "gemini-2.5-flash-lite"
        const val GEMINI_2_5_FLASH = "gemini-2.5-flash"
    }

    private val generationConfig = generationConfig {
        temperature = 0.3f
        topK = 40
        topP = 0.95f
        maxOutputTokens = 2048
        stopSequences = listOf("STOP")
    }

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
    )

    private val models = listOf(
        GenerativeModel(
            modelName = GEMINI_2_5_FLASH_LITE,
            apiKey = API_KEY,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        ),
        GenerativeModel(
            modelName = GEMINI_2_5_FLASH,
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
        for (model in models) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text ?: continue
                val games = parseMatches(text)
                if (games != null) return games
            } catch (_: Exception) {
                continue
            }
        }
        return emptyList()
    }

    // ── Prompt (generated dynamically from the dictionaries) ──────────────────

    private fun buildPrompt(today: LocalDate): String {
        val dateStr   = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))

        // League section: "key" → Display name  (auto-updated when dict changes)
        val leagueSection = TOURNAMENT_DICTIONARY.entries.joinToString("\n") { (key, name) ->
            "  \"$key\" → $name"
        }

        // Team section: all valid team keys listed in blocks of 15 (auto-updated when dict changes)
        val teamSection = TEAM_DICTIONARY.keys
            .chunked(15)
            .joinToString("\n") { chunk -> "  " + chunk.joinToString(", ") }

        return """
            Eres un experto en fútbol con conocimiento actualizado de las tablas de posiciones y los fixtures de las principales ligas del mundo.

            Necesito los partidos de HOY: $dayOfWeek $dateStr.

            LIGAS A CONSULTAR (usa EXACTAMENTE el key indicado en el campo "league"):
$leagueSection

            REGLA OBLIGATORIA: Solo incluye partidos donde:
            - Un equipo está en el TOP 5 de la tabla (posición 1–5) Y
            - El otro está en las ÚLTIMAS 5 posiciones (zona de descenso)

            EQUIPOS VÁLIDOS para "homeTeam" y "visitingTeam" (usa EXACTAMENTE uno de estos keys):
$teamSection

            Si el equipo no aparece en la lista de claves, usa el nombre en snake_case más aproximado de la lista.

            Devuelve ÚNICAMENTE un JSON válido, sin markdown ni explicaciones:
            {"matches":[{"league":"liga_espanola","homeTeam":"real_madrid","homePosition":1,"visitingTeam":"valencia","visitingPosition":18,"dateTimeIso":"${dateStr}T20:00:00","leagueSize":20}]}

            Reglas del JSON:
            - "league"       → key exacto de la lista de ligas
            - "homeTeam"     → key exacto de la lista de equipos
            - "visitingTeam" → key exacto de la lista de equipos
            - "dateTimeIso"  → formato "YYYY-MM-DDTHH:MM:SS" en hora local del país
            - "leagueSize"   → número total de equipos en esa liga esta temporada

            Si no hay partidos que cumplan la condición hoy: {"matches":[]}
        """.trimIndent()
    }

    // ── Parse + match against dictionaries ────────────────────────────────────

    private fun parseMatches(text: String): List<Game>? {
        return try {
            val clean = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val root  = Json.parseToJsonElement(clean).jsonObject
            val array = root["matches"]?.jsonArray ?: return emptyList()

            array.mapNotNull { el ->
                try {
                    val obj      = el.jsonObject
                    val homePos  = obj["homePosition"]?.jsonPrimitive?.intOrNull  ?: return@mapNotNull null
                    val visitPos = obj["visitingPosition"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val size     = obj["leagueSize"]?.jsonPrimitive?.intOrNull ?: 20

                    val rawLeague = obj["league"]?.jsonPrimitive?.content       ?: return@mapNotNull null
                    val rawHome   = obj["homeTeam"]?.jsonPrimitive?.content      ?: return@mapNotNull null
                    val rawVisit  = obj["visitingTeam"]?.jsonPrimitive?.content  ?: return@mapNotNull null
                    val rawDate   = obj["dateTimeIso"]?.jsonPrimitive?.content   ?: return@mapNotNull null

                    // Match against dictionaries — fallback to original if unknown
                    // (unknown team → imageResId = 0 → generic icon in UI)
                    val leagueKey = KeyMatcher.match(rawLeague, validLeagueKeys)
                    val homeKey   = KeyMatcher.match(rawHome,   validTeamKeys)
                    val visitKey  = KeyMatcher.match(rawVisit,  validTeamKeys)

                    Game(
                        dateTimeIso    = rawDate,
                        homePosition   = homePos,
                        homeTeam       = homeKey,
                        season         = leagueKey,
                        visitingPosition = visitPos,
                        visitingTeam   = visitKey,
                        leagueSize     = size
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { null }
    }
}
