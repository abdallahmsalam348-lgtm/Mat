package com.matchly.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

data class AppSettings(
    /** "" = not chosen yet (follows device), otherwise "ar" or "en". */
    val language: String = "",
    val onboardingDone: Boolean = false,
    /** "dark" | "black" | "light" */
    val theme: String = "dark",
    /** "" = device time zone */
    val timeZoneId: String = "",
    val arabicDigits: Boolean = true,
    val notifyEnabled: Boolean = false,
    val notifyKickoff: Boolean = true,
    val notifyGoals: Boolean = true,
    val notifyRedCards: Boolean = true,
    val notifyHalfTime: Boolean = true,
    val notifyFullTime: Boolean = true,
    val backendUrl: String = "",
    val appToken: String = "",
    val forceDemo: Boolean = false,
) {
    val effectiveLanguage: String get() = LocaleHelper.effectiveLanguage(language)

    val zone: ZoneId
        get() = try {
            if (timeZoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(timeZoneId)
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
}

/** Small synchronous settings store (SharedPreferences) exposed as a StateFlow. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read(prefs))

    val flow: StateFlow<AppSettings> = state.asStateFlow()
    val value: AppSettings get() = state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(state.value)
        write(prefs, next)
        state.value = next
    }

    companion object {
        const val NAME = "matchly_settings"

        /** Read settings without creating a store (used before the Application exists, e.g. attachBaseContext). */
        fun peek(context: Context): AppSettings = read(context.getSharedPreferences(NAME, Context.MODE_PRIVATE))

        private fun read(p: SharedPreferences): AppSettings = AppSettings(
            language = p.getString("language", "") ?: "",
            onboardingDone = p.getBoolean("onboardingDone", false),
            theme = p.getString("theme", "dark") ?: "dark",
            timeZoneId = p.getString("timeZoneId", "") ?: "",
            arabicDigits = p.getBoolean("arabicDigits", true),
            notifyEnabled = p.getBoolean("notifyEnabled", false),
            notifyKickoff = p.getBoolean("notifyKickoff", true),
            notifyGoals = p.getBoolean("notifyGoals", true),
            notifyRedCards = p.getBoolean("notifyRedCards", true),
            notifyHalfTime = p.getBoolean("notifyHalfTime", true),
            notifyFullTime = p.getBoolean("notifyFullTime", true),
            backendUrl = p.getString("backendUrl", "") ?: "",
            appToken = p.getString("appToken", "") ?: "",
            forceDemo = p.getBoolean("forceDemo", false),
        )

        private fun write(p: SharedPreferences, s: AppSettings) {
            p.edit()
                .putString("language", s.language)
                .putBoolean("onboardingDone", s.onboardingDone)
                .putString("theme", s.theme)
                .putString("timeZoneId", s.timeZoneId)
                .putBoolean("arabicDigits", s.arabicDigits)
                .putBoolean("notifyEnabled", s.notifyEnabled)
                .putBoolean("notifyKickoff", s.notifyKickoff)
                .putBoolean("notifyGoals", s.notifyGoals)
                .putBoolean("notifyRedCards", s.notifyRedCards)
                .putBoolean("notifyHalfTime", s.notifyHalfTime)
                .putBoolean("notifyFullTime", s.notifyFullTime)
                .putString("backendUrl", s.backendUrl)
                .putString("appToken", s.appToken)
                .putBoolean("forceDemo", s.forceDemo)
                .apply()
        }
    }
}
