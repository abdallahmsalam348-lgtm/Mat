package com.matchly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.matchly.app.R
import com.matchly.app.core.MatchLabels
import com.matchly.app.data.UiState
import com.matchly.app.data.model.LineupPlayer
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.MatchEvent
import com.matchly.app.data.model.TeamLineup
import com.matchly.app.data.model.TeamStats
import com.matchly.app.data.model.displayScore
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus
import com.matchly.app.ui.EmptyState
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.RefreshButton
import com.matchly.app.ui.ScreenHeader
import com.matchly.app.ui.StateView
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.components.GlassChip
import com.matchly.app.ui.components.MatchRow
import com.matchly.app.ui.components.SectionTitle
import com.matchly.app.ui.components.TeamLogo
import com.matchly.app.ui.components.statusColor
import com.matchly.app.ui.rememberLoad
import com.matchly.app.ui.theme.Matchly
import kotlinx.coroutines.delay

@Composable
fun MatchDetailScreen(
    matchId: Long,
    onBack: () -> Unit,
    onTeam: (Long) -> Unit,
    onMatch: (Long) -> Unit,
    onCompetition: (Long, Int, String) -> Unit,
    onPlayer: (Long) -> Unit,
) {
    val container = LocalContainer.current
    val vm = rememberLoad<MatchDetail>("match-$matchId") { force ->
        container.repository.matchDetail(matchId, live = true, force = force)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val live = when (val s = state) {
        is UiState.Content -> s.data.match.matchStatus.isLive
        else -> false
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(45_000L)
                vm.reload(false)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.match_center),
            onBack = onBack,
            actions = { RefreshButton { vm.reload(true) } },
        )
        StateView(vm, Modifier.weight(1f)) { detail, _ ->
            DetailContent(detail, onTeam, onMatch, onCompetition, onPlayer)
        }
    }
}

@Composable
private fun DetailContent(
    detail: MatchDetail,
    onTeam: (Long) -> Unit,
    onMatch: (Long) -> Unit,
    onCompetition: (Long, Int, String) -> Unit,
    onPlayer: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf(
        stringResource(R.string.tab_events),
        stringResource(R.string.tab_lineups),
        stringResource(R.string.tab_stats),
        stringResource(R.string.tab_h2h),
        stringResource(R.string.tab_injuries),
    )
    Column(Modifier.fillMaxSize()) {
        MatchHeader(detail.match, onTeam, onCompetition)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(labels.size) { i -> GlassChip(labels[i], tab == i, { tab = i }) }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> EventsTab(detail)
                1 -> LineupsTab(detail, onPlayer)
                2 -> StatsTab(detail)
                3 -> H2HTab(detail.match, onMatch)
                else -> InjuriesTab(detail.match)
            }
        }
    }
}

@Composable
private fun MatchHeader(match: Match, onTeam: (Long) -> Unit, onCompetition: (Long, Int, String) -> Unit) {
    val c = Matchly.colors
    val f = LocalFormatters.current
    val ctx = LocalContext.current
    val score = match.displayScore()
    val status = match.matchStatus
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        val sub = listOfNotNull(match.competition.name, match.competition.round).joinToString(" · ")
        Text(
            sub,
            Modifier.fillMaxWidth().clickable {
                val season = match.competition.season
                if (match.competition.id != 0L && season != null) onCompetition(match.competition.id, season, match.competition.name)
            },
            color = c.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TeamColumn(match.home.name, match.home.logo, Modifier.weight(1f)) { onTeam(match.home.id) }
            Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (score.hasScore) f.scoreLine(score) else (match.kickoffInstant?.let { f.time(it) } ?: "-"),
                    color = if (status.isLive) c.live else c.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (score.hasScore) 30.sp else 24.sp,
                    maxLines = 1,
                )
                f.penaltyLine(score)?.let {
                    Text(stringResource(R.string.penalties_label, it), color = c.gold, fontSize = 12.sp)
                }
                Text(
                    MatchLabels.statusText(ctx, match, f),
                    color = statusColor(status),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            TeamColumn(match.away.name, match.away.logo, Modifier.weight(1f)) { onTeam(match.away.id) }
        }
        Spacer(Modifier.height(8.dp))
        val kickoff = match.kickoffInstant
        val meta = listOfNotNull(kickoff?.let { f.dateTime(it) }, match.venue).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, Modifier.fillMaxWidth(), color = c.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TeamColumn(name: String, logo: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TeamLogo(logo, name, 48.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            color = Matchly.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ------------------------------------------------------------------ events

@Composable
private fun EventsTab(detail: MatchDetail) {
    if (detail.events.isEmpty()) {
        EmptyState(stringResource(R.string.empty_events))
        return
    }
    val events = detail.events.sortedWith(compareBy({ it.minute ?: 0 }, { it.extra ?: 0 }))
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(events.size) { i -> EventRow(events[i], detail.match) }
    }
}

@Composable
private fun EventRow(e: MatchEvent, match: Match) {
    val c = Matchly.colors
    val f = LocalFormatters.current
    val type = e.type.lowercase()
    val detailText = (e.detail ?: "")
    val icon = when {
        type == "goal" && detailText.contains("Missed", true) -> "✖"
        type == "goal" -> "⚽"
        type == "card" && detailText.contains("Red", true) -> "🟥"
        type == "card" && detailText.contains("Second", true) -> "🟥"
        type == "card" -> "🟨"
        type == "subst" -> "🔄"
        type == "var" -> "VAR"
        else -> "•"
    }
    val title = when (type) {
        "subst" -> "▲ ${e.player.orEmpty()}" + (e.assist?.let { "   ▼ $it" } ?: "")
        "goal" -> e.player.orEmpty()
        "var" -> stringResource(R.string.ev_var) + (e.detail?.let { ": $it" } ?: "")
        else -> e.player.orEmpty()
    }
    val subtitle = buildList {
        when {
            type == "goal" && detailText.contains("Own", true) -> add(stringResource(R.string.ev_own_goal))
            type == "goal" && detailText.contains("Missed", true) -> add(stringResource(R.string.ev_missed_pen))
            type == "goal" && detailText.contains("Penalty", true) -> add(stringResource(R.string.ev_penalty))
            type == "card" && detailText.contains("Red", true) -> add(stringResource(R.string.ev_red))
            type == "card" && detailText.contains("Second", true) -> add(stringResource(R.string.ev_second_yellow))
            type == "card" -> add(stringResource(R.string.ev_yellow))
        }
        if (type == "goal" && !e.assist.isNullOrBlank()) add(stringResource(R.string.ev_assist, e.assist))
        val team = when (e.teamId) {
            match.home.id -> match.home.name
            match.away.id -> match.away.name
            else -> null
        }
        if (team != null) add(team)
    }.joinToString(" · ")

    GlassCard(Modifier.fillMaxWidth(), corner = 14.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                f.eventMinute(e.minute, e.extra),
                Modifier.width(52.dp),
                color = c.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(icon, Modifier.width(40.dp), fontSize = if (icon == "VAR") 12.sp else 18.sp, color = c.textPrimary, textAlign = TextAlign.Center)
            Column(Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (subtitle.isNotBlank()) Text(subtitle, color = c.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ lineups

private fun parseGrid(grid: String?): Pair<Int, Int>? {
    val parts = grid?.split(":") ?: return null
    if (parts.size != 2) return null
    val r = parts[0].trim().toIntOrNull() ?: return null
    val col = parts[1].trim().toIntOrNull() ?: return null
    return r to col
}

@Composable
private fun LineupsTab(detail: MatchDetail, onPlayer: (Long) -> Unit) {
    if (detail.lineups.isEmpty()) {
        EmptyState(stringResource(R.string.empty_lineups))
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        detail.lineups.forEach { LineupBlock(it, onPlayer) }
    }
}

@Composable
private fun LineupBlock(lineup: TeamLineup, onPlayer: (Long) -> Unit) {
    val c = Matchly.colors
    val parsed = lineup.startXI.map { it to parseGrid(it.grid) }
    val canDrawPitch = lineup.startXI.isNotEmpty() && parsed.all { it.second != null }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(lineup.team.logo, lineup.team.name, 28.dp)
            Column(Modifier.padding(horizontal = 10.dp)) {
                Text(lineup.team.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val meta = listOfNotNull(
                    lineup.formation?.let { stringResource(R.string.formation_label, it) },
                    lineup.coach?.let { stringResource(R.string.coach_label, it) },
                ).joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = c.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (canDrawPitch) {
            Pitch(parsed.map { it.first to it.second!! })
        } else {
            lineup.startXI.forEach { PlayerLine(it, onPlayer) }
        }
        if (lineup.substitutes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionTitle(stringResource(R.string.substitutes))
            lineup.substitutes.forEach { PlayerLine(it, onPlayer) }
        }
    }
}

@Composable
private fun PlayerLine(p: LineupPlayer, onPlayer: (Long) -> Unit) {
    val c = Matchly.colors
    val f = LocalFormatters.current
    val id = p.id
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (id != null && id != 0L) Modifier.clickable { onPlayer(id) } else Modifier)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(p.number?.let { f.number(it) } ?: "", Modifier.width(32.dp), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(p.name, Modifier.weight(1f), color = c.textPrimary, fontSize = 14.sp)
        Text(p.pos.orEmpty(), color = c.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun Pitch(players: List<Pair<LineupPlayer, Pair<Int, Int>>>) {
    val f = LocalFormatters.current
    val rows = players.groupBy { it.second.first }.toSortedMap(compareByDescending<Int> { it })
    Box(
        Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0F6B43), Color(0xFF0A4A30))))
            .drawBehind {
                val line = Color.White.copy(alpha = 0.28f)
                val sw = 2.dp.toPx()
                drawRoundRect(line, Offset(8.dp.toPx(), 8.dp.toPx()), Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), CornerRadius(8.dp.toPx()), Stroke(sw))
                drawLine(line, Offset(8.dp.toPx(), size.height / 2), Offset(size.width - 8.dp.toPx(), size.height / 2), sw)
                drawCircle(line, 34.dp.toPx(), Offset(size.width / 2, size.height / 2), style = Stroke(sw))
                val boxW = size.width * 0.5f
                val boxH = 52.dp.toPx()
                drawRect(line, Offset((size.width - boxW) / 2, 8.dp.toPx()), Size(boxW, boxH), style = Stroke(sw))
                drawRect(line, Offset((size.width - boxW) / 2, size.height - 8.dp.toPx() - boxH), Size(boxW, boxH), style = Stroke(sw))
            },
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 14.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            rows.values.forEach { rowPlayers ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    rowPlayers.sortedBy { it.second.second }.forEach { (p, _) ->
                        Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF2ECC8F)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(p.number?.let { f.number(it) } ?: "", color = Color(0xFF00281B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                p.name.trim().substringAfterLast(' '),
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ statistics

@Composable
private fun statLabel(type: String): String = when (type) {
    "Ball Possession" -> stringResource(R.string.stat_possession)
    "Total Shots" -> stringResource(R.string.stat_shots)
    "Shots on Goal" -> stringResource(R.string.stat_shots_on)
    "Corner Kicks" -> stringResource(R.string.stat_corners)
    "Fouls" -> stringResource(R.string.stat_fouls)
    "Offsides" -> stringResource(R.string.stat_offsides)
    "Yellow Cards" -> stringResource(R.string.stat_yellow)
    "Red Cards" -> stringResource(R.string.stat_red)
    "Goalkeeper Saves" -> stringResource(R.string.stat_saves)
    "Total passes" -> stringResource(R.string.stat_passes)
    else -> type
}

private fun statNumber(v: String?): Float = v?.replace("%", "")?.trim()?.toFloatOrNull() ?: 0f

@Composable
private fun StatsTab(detail: MatchDetail) {
    val home: TeamStats? = detail.statistics.firstOrNull { it.team.id == detail.match.home.id } ?: detail.statistics.getOrNull(0)
    val away: TeamStats? = detail.statistics.firstOrNull { it.team.id == detail.match.away.id } ?: detail.statistics.getOrNull(1)
    if (home == null || away == null || home.stats.isEmpty()) {
        EmptyState(stringResource(R.string.empty_stats))
        return
    }
    val c = Matchly.colors
    val f = LocalFormatters.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            home.stats.forEach { stat ->
                val hv = stat.value
                val av = away.stats.firstOrNull { it.type == stat.type }?.value
                val hn = statNumber(hv)
                val an = statNumber(av)
                val total = hn + an
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(f.digits(hv ?: "-"), Modifier.width(56.dp), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(statLabel(stat.type), Modifier.weight(1f), color = c.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(f.digits(av ?: "-"), Modifier.width(56.dp), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.borderLo)) {
                        if (total > 0f) {
                            Box(Modifier.weight(maxOf(hn, 0.001f)).fillMaxSize().background(c.emerald))
                            Spacer(Modifier.width(2.dp))
                            Box(Modifier.weight(maxOf(an, 0.001f)).fillMaxSize().background(c.gold))
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ head to head + injuries

@Composable
private fun H2HTab(match: Match, onMatch: (Long) -> Unit) {
    val container = LocalContainer.current
    val vm = rememberLoad<List<Match>>("h2h-${match.home.id}-${match.away.id}") { force ->
        container.repository.headToHead(match.home.id, match.away.id, force)
    }
    StateView(vm) { list, _ ->
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.empty_h2h))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { m -> MatchRow(m, onClick = { onMatch(m.id) }) }
            }
        }
    }
}

@Composable
private fun InjuriesTab(match: Match) {
    val container = LocalContainer.current
    val c = Matchly.colors
    val vm = rememberLoad<List<com.matchly.app.data.model.Injury>>("injuries-${match.id}") { force ->
        container.repository.injuries(match.id, force)
    }
    StateView(vm) { list, _ ->
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.empty_injuries))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list.size) { i ->
                    val inj = list[i]
                    GlassCard(Modifier.fillMaxWidth(), corner = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamLogo(inj.photo ?: inj.team.logo, inj.player, 36.dp)
                            Column(Modifier.padding(horizontal = 10.dp)) {
                                Text(inj.player, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    listOfNotNull(inj.team.name, inj.type, inj.reason).joinToString(" · "),
                                    color = c.textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
