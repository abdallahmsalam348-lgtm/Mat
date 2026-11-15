package com.matchly.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.matchly.app.MainActivity
import com.matchly.app.R
import com.matchly.app.core.LocaleHelper
import com.matchly.app.notifications.WorkScheduler

private val TextPrimary = Color(0xFFF2F7F4)
private val TextSecondary = Color(0xFFA3BBAF)
private val Live = Color(0xFFFF5468)
private val Gold = Color(0xFFD9B445)
private val Emerald = Color(0xFF2ECC8F)

private fun style(color: Color, size: Int, bold: Boolean = false, align: TextAlign? = null) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    textAlign = align,
)

private fun openApp(context: Context) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply { data = Uri.parse("matchly://home") },
)

private fun openMatch(context: Context, id: Long) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        // A unique data URI keeps each row's PendingIntent distinct (extras alone are ignored by Intent matching).
        data = Uri.parse("matchly://match/$id")
        putExtra(MainActivity.EXTRA_MATCH_ID, id)
    },
)

@Composable
private fun Header(model: WidgetModel, title: String) {
    Row(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = style(Emerald, 13, bold = true), maxLines = 1)
        if (model.demo) {
            Text("  DEMO", style = style(Gold, 11, bold = true), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (model.updated.isNotBlank()) {
            Text(model.updated, style = style(TextSecondary, 10), maxLines = 1)
        }
    }
}

@Composable
private fun MatchLine(context: Context, row: WidgetRow) {
    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 3.dp).clickable(openMatch(context, row.matchId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.status,
            modifier = GlanceModifier.width(46.dp),
            style = style(if (row.live) Live else TextSecondary, 12, bold = row.live),
            maxLines = 1,
        )
        androidx.glance.layout.Column(GlanceModifier.defaultWeight()) {
            Text(row.home, style = style(TextPrimary, 13, bold = true), maxLines = 1)
            Text(row.away, style = style(TextPrimary, 13), maxLines = 1)
        }
        androidx.glance.layout.Column(GlanceModifier.width(24.dp)) {
            Text(row.homeScore, style = style(if (row.live) Live else TextPrimary, 13, bold = true, align = TextAlign.End), maxLines = 1, modifier = GlanceModifier.fillMaxWidth())
            Text(row.awayScore, style = style(if (row.live) Live else TextPrimary, 13, bold = true, align = TextAlign.End), maxLines = 1, modifier = GlanceModifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Shell(context: Context, content: @Composable () -> Unit) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_glass_bg))
            .padding(12.dp)
            .clickable(openApp(context)),
    ) { content() }
}

@Composable
private fun Message(text: String) {
    Text(text, style = style(TextSecondary, 12), modifier = GlanceModifier.padding(top = 6.dp))
}

// ---------------------------------------------------------------------------------- compact

class CompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetData.load(context, 1)
        provideContent {
            Shell(context) {
                Header(model, model.title)
                val row = model.rows.firstOrNull()
                if (row == null) {
                    Message(model.message ?: "")
                } else {
                    Column(GlanceModifier.fillMaxWidth().clickable(openMatch(context, row.matchId))) {
                        Text(
                            row.status,
                            modifier = GlanceModifier.fillMaxWidth(),
                            style = style(if (row.live) Live else TextSecondary, 13, bold = true, align = TextAlign.Center),
                            maxLines = 1,
                        )
                        Text(row.home, modifier = GlanceModifier.fillMaxWidth(), style = style(TextPrimary, 13, bold = true, align = TextAlign.Center), maxLines = 1)
                        val score = if (row.homeScore.isEmpty()) "-" else "${row.homeScore} - ${row.awayScore}"
                        Text(score, modifier = GlanceModifier.fillMaxWidth(), style = style(if (row.live) Live else TextPrimary, 22, bold = true, align = TextAlign.Center), maxLines = 1)
                        Text(row.away, modifier = GlanceModifier.fillMaxWidth(), style = style(TextPrimary, 13, bold = true, align = TextAlign.Center), maxLines = 1)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- medium

class MediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetData.load(context, 3)
        provideContent {
            Shell(context) {
                Header(model, model.title)
                if (model.rows.isEmpty()) {
                    Message(model.message ?: "")
                } else {
                    model.rows.forEach { MatchLine(context, it) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- large

class LargeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetData.load(context, 7)
        val localized = LocaleHelper.wrap(context.applicationContext)
        val title = localized.getString(R.string.widget_large_title)
        provideContent {
            Shell(context) {
                Header(model, title)
                if (model.rows.isEmpty()) {
                    Message(model.message ?: "")
                } else {
                    model.rows.forEach {
                        MatchLine(context, it)
                        Spacer(GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- receivers + helpers

suspend fun updateAllWidgets(context: Context) {
    CompactWidget().updateAll(context)
    MediumWidget().updateAll(context)
    LargeWidget().updateAll(context)
}

private fun cancelWorkIfNoWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CompactWidgetReceiver::class.java,
        MediumWidgetReceiver::class.java,
        LargeWidgetReceiver::class.java,
    )
    val remaining = receivers.sumOf { manager.getAppWidgetIds(ComponentName(context, it)).size }
    if (remaining == 0) WorkScheduler.cancelWidgets(context)
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkScheduler.scheduleWidgets(context)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWorkIfNoWidgets(context)
    }
}

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkScheduler.scheduleWidgets(context)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWorkIfNoWidgets(context)
    }
}

class LargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkScheduler.scheduleWidgets(context)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWorkIfNoWidgets(context)
    }
}
