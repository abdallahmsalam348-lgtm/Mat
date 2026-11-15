package com.matchly.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matchly.app.R
import com.matchly.app.data.UiState
import com.matchly.app.ui.errorText
import com.matchly.app.core.Favorite
import com.matchly.app.core.Seasons
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TeamSummary
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.ui.EmptyState
import com.matchly.app.ui.FavoriteButton
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.ScreenHeader
import com.matchly.app.ui.StateView
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.components.MatchRow
import com.matchly.app.ui.components.SectionTitle
import com.matchly.app.ui.components.TeamLogo
import com.matchly.app.ui.rememberLoad
import com.matchly.app.ui.theme.Matchly
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ search

@Composable
fun SearchScreen(onTeam: (Long) -> Unit, onCompetition: (Long, Int, String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(500)
        debounced = query.trim()
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.nav_search))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Matchly.colors.textSecondary) },
            colors = searchFieldColors(),
        )
        if (debounced.length < 2) {
            EmptyState(stringResource(R.string.search_prompt))
        } else {
            SearchResults(debounced, onTeam, onCompetition)
        }
    }
}

@Composable
private fun SearchResults(query: String, onTeam: (Long) -> Unit, onCompetition: (Long, Int, String) -> Unit) {
    val container = LocalContainer.current
    val zone = LocalFormatters.current.zone
    val vm = rememberLoad<SearchResult>("search-$query") { force -> container.repository.search(query, force) }
    StateView(vm) { result, _ ->
        if (result.teams.isEmpty() && result.competitions.isEmpty()) {
            EmptyState(stringResource(R.string.empty_search))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.competitions.isNotEmpty()) {
                    item(key = "hc") { SectionTitle(stringResource(R.string.nav_competitions)) }
                    items(result.competitions, key = { "c${it.id}" }) { comp ->
                        CompetitionRow(comp) { onCompetition(comp.id, comp.season ?: Seasons.current(zone), comp.name) }
                    }
                }
                if (result.teams.isNotEmpty()) {
                    item(key = "ht") { SectionTitle(stringResource(R.string.teams)) }
                    items(result.teams, key = { "t${it.id}" }) { team -> TeamRow(team) { onTeam(team.id) } }
                }
            }
        }
    }
}

@Composable
private fun TeamRow(team: TeamSummary, onClick: () -> Unit) {
    val c = Matchly.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, corner = 16.dp, contentPadding = PaddingValues(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(team.logo, team.name, 36.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(team.name, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val sub = listOfNotNull(
                    team.country,
                    if (team.national) stringResource(R.string.national_team) else null,
                ).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = c.textSecondary, fontSize = 12.sp)
            }
            FavoriteButton(Favorite("team", team.id, team.name, team.logo, team.country, null))
        }
    }
}

// ------------------------------------------------------------------ team

@Composable
fun TeamScreen(teamId: Long, onBack: () -> Unit, onMatch: (Long) -> Unit, onPlayer: (Long) -> Unit) {
    val container = LocalContainer.current
    val vm = rememberLoad<TeamProfile>("team-$teamId") { force -> container.repository.team(teamId, force) }
    val fixturesVm = rememberLoad<List<Match>>("teamfix-$teamId") { force -> container.repository.teamFixtures(teamId, force) }
    val fixState by fixturesVm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        StateView(vm, Modifier.weight(1f)) { profile, _ ->
            val c = Matchly.colors
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = profile.team.name,
                    onBack = onBack,
                    actions = {
                        FavoriteButton(Favorite("team", profile.team.id, profile.team.name, profile.team.logo, profile.team.country, null))
                    },
                )
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogo(profile.team.logo, profile.team.name, 56.dp)
                                Column(Modifier.padding(horizontal = 14.dp)) {
                                    Text(profile.team.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    val f = LocalFormatters.current
                                    val lines = listOfNotNull(
                                        profile.team.country,
                                        profile.team.city,
                                        profile.team.venue,
                                        profile.team.founded?.let { stringResource(R.string.founded_label, f.digits(it.toString())) },
                                    )
                                    lines.forEach { Text(it, color = c.textSecondary, fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                    item { SectionTitle(stringResource(R.string.matches)) }
                    item {
                        when (val s = fixState) {
                            is UiState.Loading -> Text(stringResource(R.string.loading), color = c.textSecondary, modifier = Modifier.padding(8.dp))
                            is UiState.Failure -> Text(errorText(s.kind), color = c.textSecondary, modifier = Modifier.padding(8.dp))
                            is UiState.Content -> {
                                if (s.data.isEmpty()) {
                                    Text(stringResource(R.string.empty_matches), color = c.textSecondary, modifier = Modifier.padding(8.dp))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        s.data.sortedBy { it.kickoffInstant }.takeLast(8).forEach { m ->
                                            MatchRow(m, onClick = { onMatch(m.id) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { SectionTitle(stringResource(R.string.squad)) }
                    if (profile.squad.isEmpty()) {
                        item { Text(stringResource(R.string.empty_squad), color = c.textSecondary, modifier = Modifier.padding(8.dp)) }
                    }
                    items(profile.squad, key = { it.id }) { p ->
                        val f = LocalFormatters.current
                        GlassCard(Modifier.fillMaxWidth(), onClick = { onPlayer(p.id) }, corner = 14.dp, contentPadding = PaddingValues(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogo(p.photo, p.name, 34.dp)
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(p.name, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        listOfNotNull(p.position, p.age?.let { stringResource(R.string.age_label, f.number(it)) }).joinToString(" · "),
                                        color = c.textSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                                if (p.number != null) {
                                    Text(f.number(p.number), color = c.gold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ player

@Composable
fun PlayerScreen(playerId: Long, onBack: () -> Unit) {
    val container = LocalContainer.current
    val zone = LocalFormatters.current.zone
    val season = Seasons.current(zone)
    val vm = rememberLoad<PlayerProfile>("player-$playerId") { force -> container.repository.player(playerId, season, force) }
    Column(Modifier.fillMaxSize()) {
        StateView(vm, Modifier.weight(1f)) { p, _ ->
            val c = Matchly.colors
            val f = LocalFormatters.current
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(title = p.name, onBack = onBack)
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogo(p.photo, p.name, 64.dp)
                                Column(Modifier.padding(horizontal = 14.dp)) {
                                    Text(p.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    p.team?.let { Text(it.name, color = c.textSecondary, fontSize = 13.sp) }
                                    val bio = listOfNotNull(
                                        p.nationality,
                                        p.age?.let { stringResource(R.string.age_label, f.number(it)) },
                                        p.height?.let { f.digits(it) },
                                        p.weight?.let { f.digits(it) },
                                    ).joinToString(" · ")
                                    if (bio.isNotBlank()) Text(bio, color = c.textSecondary, fontSize = 12.sp)
                                    if (p.injured == true) Text(stringResource(R.string.injured), color = c.live, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item { SectionTitle(stringResource(R.string.season_stats)) }
                    if (p.stats.isEmpty()) {
                        item { Text(stringResource(R.string.empty_player_stats), color = c.textSecondary, modifier = Modifier.padding(8.dp)) }
                    }
                    items(p.stats.size) { i ->
                        val s = p.stats[i]
                        GlassCard(Modifier.fillMaxWidth(), corner = 14.dp) {
                            Text(s.competition.name + (s.team.name.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatCell(stringResource(R.string.stat_apps), f.number(s.appearances))
                                StatCell(stringResource(R.string.stat_goals), f.number(s.goals))
                                StatCell(stringResource(R.string.stat_assists), f.number(s.assists))
                                StatCell(stringResource(R.string.stat_yellow), f.number(s.yellow))
                                StatCell(stringResource(R.string.stat_red), f.number(s.red))
                                if (!s.rating.isNullOrBlank()) StatCell(stringResource(R.string.stat_rating), f.digits(s.rating.take(4)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Matchly.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Matchly.colors.textSecondary, fontSize = 11.sp)
    }
}

// ------------------------------------------------------------------ favorites

@Composable
fun FavoritesScreen(
    onTeam: (Long) -> Unit,
    onCompetition: (Long, Int, String) -> Unit,
    onPlayer: (Long) -> Unit,
) {
    val container = LocalContainer.current
    val favs by container.favorites.flow.collectAsStateWithLifecycle()
    val zone = LocalFormatters.current.zone
    val c = Matchly.colors
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.nav_favorites))
        if (favs.isEmpty()) {
            EmptyState(stringResource(R.string.empty_favorites))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favs, key = { it.type + it.id }) { fav ->
                    GlassCard(
                        Modifier.fillMaxWidth(),
                        onClick = {
                            when (fav.type) {
                                "team" -> onTeam(fav.id)
                                "competition" -> onCompetition(fav.id, fav.season ?: Seasons.current(zone), fav.name)
                                "player" -> onPlayer(fav.id)
                            }
                        },
                        corner = 16.dp,
                        contentPadding = PaddingValues(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamLogo(fav.logo, fav.name, 36.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(fav.name, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                val kind = when (fav.type) {
                                    "team" -> stringResource(R.string.teams)
                                    "competition" -> stringResource(R.string.nav_competitions)
                                    else -> stringResource(R.string.players)
                                }
                                Text(listOfNotNull(kind, fav.subtitle).joinToString(" · "), color = c.textSecondary, fontSize = 12.sp)
                            }
                            FavoriteButton(fav)
                        }
                    }
                }
            }
        }
    }
}
