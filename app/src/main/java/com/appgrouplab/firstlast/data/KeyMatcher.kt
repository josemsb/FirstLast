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
     * Si no encuentra ningún match, retorna [input] tal cual
     * (imageResId = 0 → icono genérico en TeamInfoLocal/Visitante).
     */
    fun match(input: String, validKeys: Set<String>): String {
        // 1. Match exacto
        if (input in validKeys) return input

        // 2. Normalizar el input y buscar exacto
        val normalizedInput = normalize(input)
        if (normalizedInput in validKeys) return normalizedInput

        // 3. Normalizar TODOS los keys y comparar
        val match = validKeys.firstOrNull { normalize(it) == normalizedInput }
        if (match != null) return match

        // 4. Sin match → input original (la UI mostrará icono genérico/placeholder)
        return input
    }
}
