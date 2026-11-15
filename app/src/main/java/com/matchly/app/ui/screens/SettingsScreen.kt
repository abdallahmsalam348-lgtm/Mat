package com.matchly.app.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matchly.app.BuildConfig
import com.matchly.app.R
import com.matchly.app.core.LocaleHelper
import com.matchly.app.data.ErrorKind
import com.matchly.app.data.remote.BackendDataSource
import com.matchly.app.notifications.Notifications
import com.matchly.app.notifications.WorkScheduler
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.ScreenHeader
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.components.GlassChip
import com.matchly.app.ui.components.SectionTitle
import com.matchly.app.ui.errorText
import com.matchly.app.ui.theme.Matchly
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val COMMON_ZONES = listOf(
    "Africa/Cairo", "Asia/Riyadh", "Asia/Dubai", "Asia/Baghdad", "Asia/Amman", "Asia/Beirut",
    "Africa/Casablanca", "Africa/Algiers", "Africa/Tunis", "Africa/Khartoum",
    "Europe/London", "Europe/Paris", "Europe/Madrid", "Europe/Istanbul",
    "America/New_York", "America/Los_Angeles", "Asia/Kolkata", "Asia/Tokyo", "UTC",
)

@Composable
fun SettingsScreen(onFavorites: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val settings by container.settings.flow.collectAsStateWithLifecycle()
    val favs by container.favorites.flow.collectAsStateWithLifecycle()
    val f = LocalFormatters.current
    val c = Matchly.colors
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        container.settings.update { it.copy(notifyEnabled = granted) }
        WorkScheduler.syncNotifications(context.applicationContext, granted)
    }

    var urlText by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }
    var tokenText by remember(settings.appToken) { mutableStateOf(settings.appToken) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val unreachable = stringResource(R.string.test_failed)
    val okTemplate = stringResource(R.string.test_ok)
    val notConfigured = stringResource(R.string.err_not_configured)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.nav_settings))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- language
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_language))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip("العربية", settings.effectiveLanguage == "ar", {
                        changeLanguage(context, container, "ar")
                    })
                    GlassChip("English", settings.effectiveLanguage == "en", {
                        changeLanguage(context, container, "en")
                    })
                }
                SwitchRow(stringResource(R.string.settings_arabic_digits), settings.arabicDigits) { v ->
                    container.settings.update { it.copy(arabicDigits = v) }
                }
            }

            // ---- theme
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_theme))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(stringResource(R.string.theme_forest), settings.theme == "dark", { container.settings.update { it.copy(theme = "dark") } })
                    GlassChip(stringResource(R.string.theme_black), settings.theme == "black", { container.settings.update { it.copy(theme = "black") } })
                    GlassChip(stringResource(R.string.theme_light), settings.theme == "light", { container.settings.update { it.copy(theme = "light") } })
                }
            }

            // ---- time zone
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_timezone))
                Text(
                    stringResource(R.string.timezone_current, f.zone.id),
                    color = c.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    item(key = "device") {
                        GlassChip(stringResource(R.string.timezone_device), settings.timeZoneId.isBlank(), {
                            container.settings.update { it.copy(timeZoneId = "") }
                        })
                    }
                    items(COMMON_ZONES, key = { it }) { id ->
                        GlassChip("${id.substringAfter('/')} ${offsetLabel(id)}", settings.timeZoneId == id, {
                            container.settings.update { it.copy(timeZoneId = id) }
                        })
                    }
                }
            }

            // ---- notifications
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_notifications))
                SwitchRow(stringResource(R.string.notif_enable), settings.notifyEnabled) { v ->
                    if (v) {
                        if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            container.settings.update { it.copy(notifyEnabled = true) }
                            WorkScheduler.syncNotifications(context.applicationContext, true)
                        }
                    } else {
                        container.settings.update { it.copy(notifyEnabled = false) }
                        WorkScheduler.syncNotifications(context.applicationContext, false)
                    }
                }
                if (settings.notifyEnabled) {
                    SwitchRow(stringResource(R.string.notif_kickoff), settings.notifyKickoff) { v -> container.settings.update { it.copy(notifyKickoff = v) } }
                    SwitchRow(stringResource(R.string.notif_goals), settings.notifyGoals) { v -> container.settings.update { it.copy(notifyGoals = v) } }
                    SwitchRow(stringResource(R.string.notif_red_cards), settings.notifyRedCards) { v -> container.settings.update { it.copy(notifyRedCards = v) } }
                    SwitchRow(stringResource(R.string.notif_halftime), settings.notifyHalfTime) { v -> container.settings.update { it.copy(notifyHalfTime = v) } }
                    SwitchRow(stringResource(R.string.notif_fulltime), settings.notifyFullTime) { v -> container.settings.update { it.copy(notifyFullTime = v) } }
                }
                Text(
                    stringResource(R.string.notif_honest_note),
                    color = c.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }

            // ---- favorites
            GlassCard(Modifier.fillMaxWidth(), onClick = onFavorites) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_favorites),
                        Modifier.weight(1f),
                        color = c.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(f.number(favs.size), color = c.gold, fontWeight = FontWeight.Bold)
                }
            }

            // ---- data source
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_data_source))
                val active = container.repository.effectiveBackendUrl()
                Text(
                    if (active == null) stringResource(R.string.data_mode_demo) else stringResource(R.string.data_mode_live, active),
                    color = if (active == null) c.gold else c.emerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text(stringResource(R.string.backend_url)) },
                    placeholder = { Text("https://your-backend.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = searchFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    label = { Text(stringResource(R.string.backend_token)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = searchFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !testing,
                        onClick = {
                            val normalized = BackendDataSource.normalizeBaseUrl(urlText)
                            container.settings.update {
                                it.copy(backendUrl = if (urlText.isBlank()) "" else (normalized ?: urlText.trim()), appToken = tokenText.trim(), forceDemo = false)
                            }
                            testResult = null
                            if (container.repository.effectiveBackendUrl() == null) {
                                testResult = notConfigured
                            } else {
                                testing = true
                                scope.launch {
                                    testResult = try {
                                        val h = container.repository.health()
                                        if (h == null) {
                                            notConfigured
                                        } else if (!h.ok) {
                                            unreachable
                                        } else if (!h.configured) {
                                            errorText0(context, ErrorKind.NOT_CONFIGURED)
                                        } else {
                                            String.format(okTemplate, h.provider ?: "?", h.plan ?: "?")
                                        }
                                    } catch (e: Exception) {
                                        errorText0(context, ErrorKind.from(e))
                                    }
                                    testing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.emerald, contentColor = Color(0xFF00281B)),
                    ) { Text(stringResource(R.string.save_and_test)) }
                    OutlinedButton(onClick = {
                        urlText = ""
                        tokenText = ""
                        container.settings.update { it.copy(backendUrl = "", appToken = "") }
                        testResult = null
                    }) { Text(stringResource(R.string.clear), color = c.textPrimary) }
                }
                testResult?.let {
                    Text(it, color = c.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
                SwitchRow(stringResource(R.string.force_demo), settings.forceDemo) { v ->
                    container.settings.update { it.copy(forceDemo = v) }
                }
            }

            // ---- about
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_about))
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    color = c.textPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text(
                    stringResource(R.string.about_disclaimer),
                    color = c.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun errorText0(context: android.content.Context, kind: ErrorKind): String = context.getString(
    when (kind) {
        ErrorKind.OFFLINE -> R.string.err_offline
        ErrorKind.TIMEOUT -> R.string.err_timeout
        ErrorKind.RATE_LIMITED -> R.string.err_rate_limited
        ErrorKind.NOT_CONFIGURED -> R.string.err_not_configured
        ErrorKind.UNAUTHORIZED -> R.string.err_unauthorized
        ErrorKind.PLAN_LIMIT -> R.string.err_plan_limit
        ErrorKind.NOT_FOUND -> R.string.err_not_found
        ErrorKind.SERVER -> R.string.err_server
        ErrorKind.UNKNOWN -> R.string.err_unknown
    },
)

private fun offsetLabel(id: String): String = try {
    val offset = ZoneId.of(id).rules.getOffset(Instant.now())
    "(" + (if (offset.id == "Z") "UTC" else "UTC" + offset.id) + ")"
} catch (e: Exception) {
    ""
}

private fun changeLanguage(context: android.content.Context, container: com.matchly.app.AppContainer, code: String) {
    if (container.settings.value.effectiveLanguage == code && container.settings.value.language == code) return
    container.settings.update { it.copy(language = code) }
    Notifications.ensureChannel(LocaleHelper.wrap(context.applicationContext))
    (context as? Activity)?.recreate()
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Matchly.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), color = c.textPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00281B),
                checkedTrackColor = c.emerald,
                uncheckedThumbColor = c.textSecondary,
                uncheckedTrackColor = c.borderLo,
            ),
        )
    }
}
