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

    private fun buildPrompt(today: LocalDate): String {
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        return """
            Eres un experto en fútbol con conocimiento actualizado de las tablas de posiciones y los fixtures de las principales ligas del mundo.

            Necesito los partidos de HOY: $dayOfWeek $dateStr
            Ligas a consultar: La Liga Española, Premier League, Ligue 1, Serie A, Bundesliga, Liga Peruana, Liga Argentina y Brasileirão.

            REGLA OBLIGATORIA: Solo incluye partidos donde se cumpla esta condición:
            - Un equipo ocupa una posición del TOP 5 en la tabla (posición 1 al 5) Y
            - El otro equipo ocupa una de las ÚLTIMAS 5 posiciones de la tabla (zona de descenso)

            Devuelve ÚNICAMENTE un JSON válido sin markdown, sin explicaciones adicionales:
            {"matches":[{"league":"liga_espanola","homeTeam":"real_madrid","homePosition":1,"visitingTeam":"valencia","visitingPosition":18,"dateTimeIso":"${dateStr}T20:00:00","leagueSize":20}]}

            Valores exactos para el campo "league":
            liga_espanola, premier_league, ligue_1, serie_a, bundesliga, liga_peruana, liga_argentina, liga_brasilera

            Claves exactas para "homeTeam" y "visitingTeam":
            España (20 equipos): real_madrid, barcelona, atletico_madrid, girona, athletic_club, real_sociedad, betis, villarreal, osasuna, sevilla, rayo_vallecano, celta_de_vigo, getafe, alaves, rcd_mallorca, rcd_espanol, levante, real_oviedo, valencia, elche
            Inglaterra (20 equipos): arsenal, manchester_city, liverpool, chelsea, aston_villa, tottenham_hotspur, newcastle_united, manchester_united, west_ham_united, brighton, wolves, crystal_palace, fulham, brentford, everton, bournemouth, nottingham_forest, burnley, leeds_united, sunderland
            Francia (18 equipos): paris_saint_germain, marseille, monaco, nice, lens, rennes, lille, strasbourg, reims, brest, toulouse, nantes, lyon, le_havre, angers, auxerre, paris_fc, stade_brestois
            Italia (20 equipos): inter, napoli, atalanta, juventus, lazio, fiorentina, bologna, roma, torino, milano, udinese, como, genoa, lecce, cagliari, parma, empoli, hellas_verona, monza, sassuolo
            Alemania (18 equipos): bayer_leverkusen, bayern_munich, rb_leipzig, eintracht_frankfurt, borussia_dortmund, stuttgart, sc_freiburg, wolfsburg, mainz_05, werder_bremen, tsg_hoffenheim, borussia_monchengladbach, union_berlin, fc_augsburg, heidenheim, st_pauli, fc_cologne, hamburg_sv
            Perú (19 equipos): alianza_lima, universitario, sporting_cristal, melgar, cusco_fc, cienciano, sport_huancayo, deportivo_garcilaso, comerciantes_unidos, los_chankas, alianza_atletico, atletico_grau, adt, utc, alianza_universidad, binacional, sport_boys, ayacucho_fc
            Argentina (28 equipos): river_plate, boca_juniors, racing_club, independiente_avellaneda, san_lorenzo, huracan, rosario_central, newells_old_boys, estudiantes, gimnasia, argentinos_juniors, velez_sarsfield, lanus, banfield, tigre, defensa_y_justicia, godoy_cruz, belgrano, talleres, atletico_tucuman, barracas_central, deportivo_riestra, sarmiento_junin, platense
            Brasil (20 equipos): flamengo, palmeiras, atletico_mineiro, fluminense, botafogo, internacional, gremio, sao_paulo, corinthians, atletico_paranaense, cruzeiro, bahia, fortaleza, bragantino, vasco_da_gama, cuiaba, santos, goias, coritiba, juventude

            El campo "dateTimeIso" debe estar en formato "YYYY-MM-DDTHH:MM:SS" en la hora local del país donde se juega.
            El campo "leagueSize" debe ser el número total de equipos en esa liga esta temporada.

            Si no hay partidos que cumplan la condición hoy, devuelve exactamente: {"matches":[]}
        """.trimIndent()
    }

    private fun parseMatches(text: String): List<Game>? {
        return try {
            val clean = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val root = Json.parseToJsonElement(clean).jsonObject
            val array = root["matches"]?.jsonArray ?: return emptyList()
            array.mapNotNull { el ->
                try {
                    val obj = el.jsonObject
                    val homePos = obj["homePosition"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val visitPos = obj["visitingPosition"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val size = obj["leagueSize"]?.jsonPrimitive?.intOrNull ?: 20
                    Game(
                        dateTimeIso = obj["dateTimeIso"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        homePosition = homePos,
                        homeTeam = obj["homeTeam"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        season = obj["league"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        visitingPosition = visitPos,
                        visitingTeam = obj["visitingTeam"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        leagueSize = size
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { null }
    }
}
