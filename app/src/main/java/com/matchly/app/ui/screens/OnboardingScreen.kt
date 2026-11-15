package com.matchly.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matchly.app.R
import com.matchly.app.notifications.Notifications
import com.matchly.app.notifications.WorkScheduler
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.theme.Matchly

@Composable
fun OnboardingScreen(onLanguageChanged: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val settings by container.settings.flow.collectAsStateWithLifecycle()
    val c = Matchly.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step -= 1 }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        container.settings.update { it.copy(notifyEnabled = granted, onboardingDone = true) }
        WorkScheduler.syncNotifications(context.applicationContext, granted)
    }

    fun finish(enableNotifications: Boolean) {
        if (enableNotifications) {
            if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            container.settings.update { it.copy(notifyEnabled = true, onboardingDone = true) }
            WorkScheduler.syncNotifications(context.applicationContext, true)
        } else {
            container.settings.update { it.copy(onboardingDone = true) }
        }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("MATCHLY", color = c.emerald, fontWeight = FontWeight.Black, fontSize = 38.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(width = 48.dp, height = 3.dp).clip(CircleShape).background(c.gold))
            Spacer(Modifier.height(28.dp))
            when (step) {
                0 -> {
                    Text(stringResource(R.string.onb_choose_language), color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    LanguageCard("العربية", settings.effectiveLanguage == "ar") { pick("ar", container, onLanguageChanged) }
                    Spacer(Modifier.height(10.dp))
                    LanguageCard("English", settings.effectiveLanguage == "en") { pick("en", container, onLanguageChanged) }
                }
                1 -> {
                    Text(stringResource(R.string.onb_welcome_title), color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    listOf(R.string.onb_feature_live, R.string.onb_feature_center, R.string.onb_feature_fav, R.string.onb_feature_widgets).forEach {
                        GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), corner = 16.dp) {
                            Text(stringResource(it), color = c.textPrimary, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassCard(Modifier.fillMaxWidth(), corner = 16.dp) {
                        Text(stringResource(R.string.onb_demo_notice), color = c.gold, fontSize = 13.sp)
                    }
                }
                else -> {
                    Text(stringResource(R.string.onb_notifications_title), color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.onb_notifications_body), color = c.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(if (i == step) 10.dp else 8.dp).clip(CircleShape).background(if (i == step) c.emerald else c.borderLo))
            }
        }
        if (step < 2) {
            Button(
                onClick = {
                    if (step == 0) container.settings.update { it.copy(language = it.effectiveLanguage) }
                    step += 1
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.emerald, contentColor = Color(0xFF00281B)),
            ) { Text(stringResource(R.string.onb_continue), fontWeight = FontWeight.Bold) }
        } else {
            Button(
                onClick = { finish(true) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.emerald, contentColor = Color(0xFF00281B)),
            ) { Text(stringResource(R.string.onb_enable_notifications), fontWeight = FontWeight.Bold) }
            TextButton(onClick = { finish(false) }) {
                Text(stringResource(R.string.onb_skip), color = c.textSecondary)
            }
        }
    }
}

private fun pick(code: String, container: com.matchly.app.AppContainer, onChanged: () -> Unit) {
    if (container.settings.value.language == code) return
    container.settings.update { it.copy(language = code) }
    onChanged()
}

@Composable
private fun LanguageCard(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Matchly.colors
    GlassCard(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        corner = 18.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (selected) Text("✓", color = c.emerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
