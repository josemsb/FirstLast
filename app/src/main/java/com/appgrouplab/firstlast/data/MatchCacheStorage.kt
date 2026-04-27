package com.appgrouplab.firstlast.data

import android.content.Context

class MatchCacheStorage(context: Context) {
    private val prefs = context.getSharedPreferences("match_cache", Context.MODE_PRIVATE)

    fun getCachedDate(): String? = prefs.getString("cache_date", null)
    fun getCachedJson(): String? = prefs.getString("cache_json", null)

    fun saveCache(date: String, json: String) {
        prefs.edit()
            .putString("cache_date", date)
            .putString("cache_json", json)
            .apply()
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }
}
