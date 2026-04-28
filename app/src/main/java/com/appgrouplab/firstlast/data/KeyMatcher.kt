package com.appgrouplab.firstlast.data

import java.util.Locale

/**
 * Normaliza y hace match entre los valores que devuelve el agente Gemini
 * y los keys exactos de TOURNAMENT_DICTIONARY / TEAM_DICTIONARY.
 *
 * Estrategia:
 *  1. Exacto       → devuelve el key tal cual
 *  2. Normalizado  → lowercase, sin acentos, espacios→"_", sin caracteres especiales
 *  3. Sin match    → devuelve el input original (la UI mostrará icono genérico)
 */
object KeyMatcher {

    fun normalize(input: String): String =
        input
            .lowercase(Locale.ROOT)
            .trim()
            .replace("á", "a").replace("à", "a").replace("ä", "a").replace("â", "a")
            .replace("é", "e").replace("è", "e").replace("ë", "e").replace("ê", "e")
            .replace("í", "i").replace("ì", "i").replace("ï", "i").replace("î", "i")
            .replace("ó", "o").replace("ò", "o").replace("ö", "o").replace("ô", "o")
            .replace("ú", "u").replace("ù", "u").replace("ü", "u").replace("û", "u")
            .replace("ñ", "n")
            .replace("ç", "c")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^a-z0-9_]"), "")

    /**
     * Busca el key correcto en [validKeys] para el [input] devuelto por Gemini.
     *
     * Estrategia en orden de precisión:
     *  1. Exacto
     *  2. Normalizado exacto
     *  3. Normalizado de todos los keys
     *  4. Contains por palabras — cada palabra del AI key debe aparecer en el DB key
     *     Ej: "man_city" → ["man","city"] → encuentra "manchester_city"
     *     Ej: "atletico" → ["atletico"]   → encuentra "atletico_de_madrid"
     *  5. Sin match → input original (la UI mostrará icono genérico)
     */
    fun match(input: String, validKeys: Set<String>): String {
        // 1. Match exacto
        if (input in validKeys) return input

        // 2. Normalizar el input y buscar exacto
        val normalizedInput = normalize(input)
        if (normalizedInput in validKeys) return normalizedInput

        // 3. Normalizar TODOS los keys y comparar exacto
        val exactNorm = validKeys.firstOrNull { normalize(it) == normalizedInput }
        if (exactNorm != null) return exactNorm

        // 4. Contains por palabras
        val aiWords = normalizedInput.split("_").filter { it.length >= 3 }
        if (aiWords.isNotEmpty()) {
            val candidates = validKeys.filter { dbKey ->
                val normalizedDb = normalize(dbKey)
                // Todas las palabras del AI key deben estar contenidas en el DB key
                aiWords.all { word -> normalizedDb.contains(word) }
            }
            if (candidates.size == 1) return candidates.first()
            if (candidates.size > 1) {
                // Si hay varios candidatos, devolver el más corto (más específico)
                return candidates.minByOrNull { it.length } ?: candidates.first()
            }

            // Fallback inverso: buscar si las palabras del DB key están en el AI input
            val reverseCandidates = validKeys.filter { dbKey ->
                val dbWords = normalize(dbKey).split("_").filter { it.length >= 3 }
                dbWords.isNotEmpty() && dbWords.all { word -> normalizedInput.contains(word) }
            }
            if (reverseCandidates.size == 1) return reverseCandidates.first()
            if (reverseCandidates.size > 1) {
                return reverseCandidates.minByOrNull { it.length } ?: reverseCandidates.first()
            }
        }

        // 5. Sin match → input original (la UI mostrará icono genérico/placeholder)
        return input
    }
}
