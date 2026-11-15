package com.matchly.app

import android.content.Context
import com.matchly.app.core.FavoritesStore
import com.matchly.app.core.SettingsStore
import com.matchly.app.data.FootballRepository
import com.matchly.app.data.JsonCache
import kotlinx.serialization.json.Json
import java.io.File

/** Hand-rolled service locator (keeps the project free of annotation processors / code generation). */
class AppContainer(context: Context) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }
    val settings = SettingsStore(context)
    val favorites = FavoritesStore(context, json)
    val cache = JsonCache(File(context.cacheDir, "api"), json)
    val repository = FootballRepository(settings, cache, json)
}
