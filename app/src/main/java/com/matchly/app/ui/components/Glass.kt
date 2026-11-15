package com.matchly.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.matchly.app.ui.theme.Matchly

/**
 * Glass-inspired surfaces. Compose has no built-in backdrop blur, so "glass" here means:
 * translucent gradient fill + thin luminous border + soft top reflection + glowing background orbs.
 * Contrast of text on glass is kept high on purpose.
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Matchly.colors
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            .drawBehind {
                val r = size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.emerald.copy(alpha = if (c.isDark) 0.24f else 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.04f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(size.width * 0.1f, size.height * 0.04f),
                )
                val r2 = size.width * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.gold.copy(alpha = if (c.isDark) 0.10f else 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.92f),
                        radius = r2,
                    ),
                    radius = r2,
                    center = Offset(size.width * 0.95f, size.height * 0.92f),
                )
            },
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    corner: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Matchly.colors
    val shape = RoundedCornerShape(corner)
    var m = modifier
        .clip(shape)
        .background(Brush.verticalGradient(listOf(c.glassTop, c.glassBottom)))
        .drawBehind {
            if (c.isDark) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.4f,
                    ),
                    size = Size(size.width, size.height * 0.4f),
                )
            }
        }
        .border(1.dp, Brush.linearGradient(listOf(c.borderHi, c.borderLo)), shape)
    if (onClick != null) {
        m = m.clickable(role = Role.Button, onClick = onClick)
    }
    Column(m.padding(contentPadding), content = content)
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Matchly.colors
    val shape = RoundedCornerShape(50)
    val bg = if (selected) Brush.linearGradient(listOf(c.emerald, c.emeraldDeep))
    else Brush.verticalGradient(listOf(c.glassTop, c.glassBottom))
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, if (selected) c.emerald else c.borderLo), shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF00281B) else c.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val c = Matchly.colors
    Row(modifier.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.gold),
        )
        Text(
            text,
            modifier = Modifier.padding(start = 8.dp),
            color = c.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun TeamLogo(url: String?, name: String, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val c = Matchly.colors
    var failed by remember(url) { mutableStateOf(false) }
    if (url.isNullOrBlank() || failed) {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(c.emerald.copy(alpha = 0.18f))
                .border(1.dp, c.borderLo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().take(1).uppercase(),
                color = c.emerald,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            onError = { failed = true },
        )
    }
}

@Composable
fun SpacedColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}
