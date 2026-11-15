package com.matchly.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matchly.app.core.Formatters
import com.matchly.app.core.LocaleHelper
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.MatchlyRoot
import com.matchly.app.ui.components.GlassBackground
import com.matchly.app.ui.screens.OnboardingScreen
import com.matchly.app.ui.theme.MatchlyTheme

class MainActivity : ComponentActivity() {
    private val pendingMatch = mutableStateOf<Long?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readIntent(intent)
        val container = (application as MatchlyApp).container

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle()
            val formatters = remember(settings.effectiveLanguage, settings.arabicDigits, settings.timeZoneId) {
                Formatters.from(settings)
            }
            val dark = settings.theme != "light"
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            MatchlyTheme(settings.theme) {
                CompositionLocalProvider(
                    LocalContainer provides container,
                    LocalFormatters provides formatters,
                ) {
                    GlassBackground {
                        if (!settings.onboardingDone) {
                            OnboardingScreen(onLanguageChanged = { recreate() })
                        } else {
                            MatchlyRoot(
                                pendingMatchId = pendingMatch.value,
                                onPendingConsumed = { pendingMatch.value = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_MATCH_ID, 0L) ?: 0L
        if (id != 0L) pendingMatch.value = id
    }

    companion object {
        const val EXTRA_MATCH_ID = "matchly.matchId"
    }
}
