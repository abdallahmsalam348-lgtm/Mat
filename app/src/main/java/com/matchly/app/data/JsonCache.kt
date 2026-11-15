package com.matchly.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
internal data class CacheEntry(val savedAt: Long, val payload: String)

data class Cached<T>(val data: T, val savedAt: Long)

/** Tiny file-based JSON cache (offline viewing). One file per key inside the app cache directory. */
class JsonCache(private val dir: File, private val json: Json) {
    init {
        dir.mkdirs()
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it) }.take(40)
        return File(dir, "$name.json")
    }

    fun <T> read(key: String, serializer: KSerializer<T>): Cached<T>? {
        val f = fileFor(key)
        if (!f.exists()) return null
        return try {
            val entry = json.decodeFromString(CacheEntry.serializer(), f.readText())
            Cached(json.decodeFromString(serializer, entry.payload), entry.savedAt)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        try {
            val payload = json.encodeToString(serializer, value)
            val entry = CacheEntry(System.currentTimeMillis(), payload)
            fileFor(key).writeText(json.encodeToString(CacheEntry.serializer(), entry))
        } catch (e: Exception) {
            // Cache failures must never break the app.
        }
    }

    fun prune(maxAgeMs: Long) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { f ->
            if (now - f.lastModified() > maxAgeMs) f.delete()
        }
    }
}
