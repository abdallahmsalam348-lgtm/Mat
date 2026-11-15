package com.matchly.app.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language switching that works on every supported Android version.
 * The chosen language is stored in [SettingsStore] and applied by wrapping the base context
 * (Activities, notifications and widgets all go through [wrap]). RTL layout follows the locale.
 */
object LocaleHelper {
    fun effectiveLanguage(code: String): String =
        if (code == "ar" || code == "en") code
        else if (Locale.getDefault().language == "ar") "ar"
        else "en"

    fun localeFor(code: String): Locale = Locale.forLanguageTag(effectiveLanguage(code))

    fun wrap(base: Context): Context {
        val settings = SettingsStore.peek(base)
        val locale = localeFor(settings.language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
