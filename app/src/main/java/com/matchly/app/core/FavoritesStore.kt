package com.matchly.app.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Favorite(
    /** "team" | "competition" | "player" */
    val type: String,
    val id: Long,
    val name: String,
    val logo: String? = null,
    val subtitle: String? = null,
    val season: Int? = null,
)

class FavoritesStore(context: Context, private val json: Json) {
    private val prefs = context.getSharedPreferences("matchly_favorites", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(Favorite.serializer())
    private val state = MutableStateFlow(load())

    val flow: StateFlow<List<Favorite>> = state.asStateFlow()
    val value: List<Favorite> get() = state.value

    private fun load(): List<Favorite> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<Favorite>) {
        prefs.edit().putString("items", json.encodeToString(serializer, list)).apply()
        state.value = list
    }

    fun isFavorite(type: String, id: Long): Boolean =
        state.value.any { it.type == type && it.id == id }

    fun toggle(fav: Favorite) {
        val current = state.value
        if (current.any { it.type == fav.type && it.id == fav.id }) {
            save(current.filterNot { it.type == fav.type && it.id == fav.id })
        } else {
            save(current + fav)
        }
    }

    fun ids(type: String): Set<Long> = state.value.filter { it.type == type }.map { it.id }.toSet()
}
