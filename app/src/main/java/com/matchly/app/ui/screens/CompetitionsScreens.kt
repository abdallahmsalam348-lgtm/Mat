package com.matchly.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchly.app.R
import com.matchly.app.core.Favorite
import com.matchly.app.core.Seasons
import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.StandingRow
import com.matchly.app.data.model.TopScorer
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus
import com.matchly.app.ui.EmptyState
import com.matchly.app.ui.FavoriteButton
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.ScreenHeader
import com.matchly.app.ui.StateView
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.components.GlassChip
import com.matchly.app.ui.components.MatchRow
import com.matchly.app.ui.components.SectionTitle
import com.matchly.app.ui.components.TeamLogo
import com.matchly.app.ui.rememberLoad
import com.matchly.app.ui.theme.Matchly

@Composable
fun searchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Matchly.colors.textPrimary,
    unfocusedTextColor = Matchly.colors.textPrimary,
    focusedBorderColor = Matchly.colors.emerald,
    unfocusedBorderColor = Matchly.colors.borderLo,
    cursorColor = Matchly.colors.emerald,
    focusedLabelColor = Matchly.colors.emerald,
    unfocusedLabelColor = Matchly.colors.textSecondary,
    focusedPlaceholderColor = Matchly.colors.textSecondary,
    unfocusedPlaceholderColor = Matchly.colors.textSecondary,
)

// ------------------------------------------------------------------ competitions list

@Composable
fun CompetitionsScreen(onCompetition: (Long, Int, String) -> Unit) {
    val container = LocalContainer.current
    val zone = LocalFormatters.current.zone
    val vm = rememberLoad<List<Competition>>("competitions") { force -> container.repository.competitions(force) }
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.nav_competitions))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_competitions_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Matchly.colors.textSecondary) },
            colors = searchFieldColors(),
        )
        StateView(vm, Modifier.weight(1f)) { list, _ ->
            val q = query.trim().lowercase()
            val filtered = if (q.isEmpty()) list else list.filter {
                it.name.lowercase().contains(q) || (it.country ?: "").lowercase().contains(q)
            }
            if (filtered.isEmpty()) {
                EmptyState(stringResource(R.string.empty_search))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { comp ->
                        CompetitionRow(comp) { onCompetition(comp.id, comp.season ?: Seasons.current(zone), comp.name) }
                    }
                }
            }
        }
    }
}

@Composable
fun CompetitionRow(comp: Competition, onClick: () -> Unit) {
    val c = Matchly.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, corner = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(comp.logo ?: comp.flag, comp.name, 36.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(comp.name, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val sub = listOfNotNull(comp.country, comp.type).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = c.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ one competition

@Composable
fun CompetitionScreen(
    id: Long,
    season: Int,
    name: String,
    onBack: () -> Unit,
    onTeam: (Long) -> Unit,
    onMatch: (Long) -> Unit,
    onPlayer: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf(
        stringResource(R.string.tab_table),
        stringResource(R.string.tab_fixtures),
        stringResource(R.string.tab_scorers),
    )
    val seasonText = stringResource(R.string.season_label, LocalFormatters.current.digits(season.toString()))
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = name,
            onBack = onBack,
            actions = { FavoriteButton(Favorite("competition", id, name, null, seasonText, season)) },
        )
        Text(
            seasonText,
            Modifier.padding(horizontal = 16.dp),
            color = Matchly.colors.textSecondary,
            fontSize = 12.sp,
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, l -> GlassChip(l, tab == i, { tab = i }) }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> StandingsTab(id, season, onTeam)
                1 -> CompetitionFixturesTab(id, season, onMatch)
                else -> ScorersTab(id, season, onPlayer)
            }
        }
    }
}

@Composable
private fun StandingsTab(id: Long, season: Int, onTeam: (Long) -> Unit) {
    val container = LocalContainer.current
    val vm = rememberLoad<List<StandingGroup>>("standings-$id-$season") { force ->
        container.repository.standings(id, season, force)
    }
    StateView(vm) { groups, _ ->
        if (groups.isEmpty() || groups.all { it.rows.isEmpty() }) {
            EmptyState(stringResource(R.string.empty_standings))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                groups.forEachIndexed { gi, g ->
                    item(key = "g$gi") {
                        Column {
                            if (!g.name.isNullOrBlank()) SectionTitle(g.name)
                            StandingsHeader()
                        }
                    }
                    items(g.rows.size, key = { "g$gi-r$it" }) { i -> StandingRowView(g.rows[i], onTeam) }
                }
            }
        }
    }
}

@Composable
private fun StandingsHeader() {
    val c = Matchly.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#", Modifier.width(28.dp), color = c.textSecondary, fontSize = 11.sp)
        Text(stringResource(R.string.col_team), Modifier.weight(1f), color = c.textSecondary, fontSize = 11.sp)
        listOf(R.string.col_played, R.string.col_win, R.string.col_draw, R.string.col_lose, R.string.col_gd, R.string.col_points).forEach {
            Text(stringResource(it), Modifier.width(if (it == R.string.col_points) 32.dp else 28.dp), color = c.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StandingRowView(row: StandingRow, onTeam: (Long) -> Unit) {
    val c = Matchly.colors
    val f = LocalFormatters.current
    val desc = (row.description ?: "").lowercase()
    val marker = when {
        desc.contains("champions") || desc.contains("promotion") || desc.contains("final") -> c.emerald
        desc.contains("europa") || desc.contains("conference") || desc.contains("qualif") -> c.gold
        desc.contains("relegation") -> c.live
        else -> Color.Transparent
    }
    GlassCard(
        Modifier.fillMaxWidth(),
        onClick = { onTeam(row.team.id) },
        corner = 12.dp,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.number(row.rank), Modifier.width(28.dp), color = marker.takeIf { it != Color.Transparent } ?: c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            TeamLogo(row.team.logo, row.team.name, 20.dp)
            Text(
                row.team.name,
                Modifier.weight(1f).padding(horizontal = 8.dp),
                color = c.textPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            listOf(row.played, row.win, row.draw, row.lose, row.goalsDiff).forEach {
                Text(f.number(it), Modifier.width(28.dp), color = c.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Text(f.number(row.points), Modifier.width(32.dp), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CompetitionFixturesTab(id: Long, season: Int, onMatch: (Long) -> Unit) {
    val container = LocalContainer.current
    val f = LocalFormatters.current
    val vm = rememberLoad<List<Match>>("compfix-$id-$season") { force ->
        container.repository.competitionFixtures(id, season, force)
    }
    StateView(vm) { matches, _ ->
        if (matches.isEmpty()) {
            EmptyState(stringResource(R.string.empty_matches))
        } else {
            val sorted = matches.sortedBy { it.kickoffInstant }
            val firstUpcoming = sorted.indexOfFirst { !it.matchStatus.isFinished }.let { if (it < 0) 0 else it }
            val state = rememberLazyListState()
            LaunchedEffect(sorted.size) { state.scrollToItem(maxOf(firstUpcoming - 1, 0)) }
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorted, key = { it.id }) { m ->
                    Column {
                        m.kickoffInstant?.let {
                            Text(
                                f.dateTime(it),
                                color = Matchly.colors.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                            )
                        }
                        MatchRow(m, onClick = { onMatch(m.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorersTab(id: Long, season: Int, onPlayer: (Long) -> Unit) {
    val container = LocalContainer.current
    val c = Matchly.colors
    val f = LocalFormatters.current
    val vm = rememberLoad<List<TopScorer>>("scorers-$id-$season") { force ->
        container.repository.topScorers(id, season, force)
    }
    StateView(vm) { list, _ ->
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.empty_scorers))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list.size) { i ->
                    val s = list[i]
                    GlassCard(
                        Modifier.fillMaxWidth(),
                        onClick = { if (s.player.id != 0L) onPlayer(s.player.id) },
                        corner = 14.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.number(i + 1), Modifier.width(28.dp), color = c.gold, fontWeight = FontWeight.Bold)
                            TeamLogo(s.player.photo, s.player.name, 36.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(s.player.name, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(s.team.name, color = c.textSecondary, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.goals_count, f.number(s.goals)), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (s.assists != null) {
                                    Text(stringResource(R.string.assists_count, f.number(s.assists)), color = c.textSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
