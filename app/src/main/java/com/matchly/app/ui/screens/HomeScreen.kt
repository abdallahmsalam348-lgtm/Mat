package com.matchly.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.matchly.app.R
import com.matchly.app.core.Seasons
import com.matchly.app.data.UiState
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus
import com.matchly.app.ui.EmptyState
import com.matchly.app.ui.LocalContainer
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.RefreshButton
import com.matchly.app.ui.ScreenHeader
import com.matchly.app.ui.StateView
import com.matchly.app.ui.components.GlassChip
import com.matchly.app.ui.components.MatchRow
import com.matchly.app.ui.components.TeamLogo
import com.matchly.app.ui.rememberLoad
import com.matchly.app.ui.theme.Matchly
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun HomeScreen(onMatch: (Long) -> Unit, onCompetition: (Long, Int, String) -> Unit) {
    val container = LocalContainer.current
    val f = LocalFormatters.current
    val zone = f.zone
    val today = LocalDate.now(zone)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val date = LocalDate.parse(selected)

    val vm = rememberLoad<List<Match>>("fixtures-$selected-${zone.id}") { force ->
        container.repository.fixtures(date, zone, force)
    }

    // Light auto-refresh: only while this screen is visible, only for today's list.
    val state by vm.state.collectAsStateWithLifecycle()
    val anyLive = when (val s = state) {
        is UiState.Content -> s.data.any { it.matchStatus.isLive }
        else -> false
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(selected, anyLive) {
        if (date != LocalDate.now(zone)) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(if (anyLive) 60_000L else 300_000L)
                vm.reload(false)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        ScreenHeader(
            title = stringResource(R.string.app_name),
            actions = { RefreshButton { vm.reload(true) } },
        )
        val days = remember(today) { (-3L..10L).map { today.plusDays(it) } }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(days, key = { it.toEpochDay() }) { d ->
                DayChip(
                    weekday = if (d == today) stringResource(R.string.today) else f.weekday(d),
                    day = f.dayOfMonth(d),
                    selected = d.toString() == selected,
                    onClick = { selected = d.toString() },
                )
            }
        }
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassChip(stringResource(R.string.filter_all), filter == 0, { filter = 0 })
            GlassChip(stringResource(R.string.filter_live), filter == 1, { filter = 1 })
            GlassChip(stringResource(R.string.filter_favorites), filter == 2, { filter = 2 })
        }
        StateView(vm, Modifier.weight(1f)) { matches, _ ->
            MatchList(matches, filter, onMatch, onCompetition)
        }
    }
}

@Composable
private fun DayChip(weekday: String, day: String, selected: Boolean, onClick: () -> Unit) {
    val c = Matchly.colors
    val shape = RoundedCornerShape(16.dp)
    val bg = if (selected) Brush.linearGradient(listOf(c.emerald, c.emeraldDeep))
    else Brush.verticalGradient(listOf(c.glassTop, c.glassBottom))
    Column(
        Modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, if (selected) c.emerald else c.borderLo), shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .width(58.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekday, fontSize = 12.sp, color = if (selected) androidx.compose.ui.graphics.Color(0xFF00281B) else c.textSecondary, maxLines = 1)
        Text(
            day,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) androidx.compose.ui.graphics.Color(0xFF00281B) else c.textPrimary,
        )
    }
}

@Composable
private fun MatchList(
    matches: List<Match>,
    filter: Int,
    onMatch: (Long) -> Unit,
    onCompetition: (Long, Int, String) -> Unit,
) {
    val container = LocalContainer.current
    val favs by container.favorites.flow.collectAsStateWithLifecycle()
    val favTeams = favs.filter { it.type == "team" }.map { it.id }.toSet()
    val favComps = favs.filter { it.type == "competition" }.map { it.id }.toSet()
    val zone = LocalFormatters.current.zone

    val filtered = when (filter) {
        1 -> matches.filter { it.matchStatus.isLive }
        2 -> matches.filter {
            favTeams.contains(it.home.id) || favTeams.contains(it.away.id) || favComps.contains(it.competition.id)
        }
        else -> matches
    }
    if (filtered.isEmpty()) {
        val text = when (filter) {
            1 -> stringResource(R.string.empty_live)
            2 -> stringResource(R.string.empty_favorite_matches)
            else -> stringResource(R.string.empty_matches)
        }
        EmptyState(text)
        return
    }
    val groups = filtered
        .groupBy { it.competition.id }
        .values
        .map { list -> list.sortedBy { it.kickoffInstant } }
        .sortedWith(
            compareBy<List<Match>>({ if (favComps.contains(it.first().competition.id)) 0 else 1 })
                .thenBy { it.first().kickoffInstant },
        )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { list ->
            val comp = list.first().competition
            item(key = "h${comp.id}") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCompetition(comp.id, comp.season ?: Seasons.current(zone), comp.name) }
                        .padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamLogo(comp.logo ?: comp.flag, comp.name, 22.dp)
                    Column(Modifier.padding(horizontal = 8.dp)) {
                        Text(comp.name, color = Matchly.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val sub = listOfNotNull(comp.country, comp.round).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(sub, color = Matchly.colors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Start)
                        }
                    }
                }
            }
            items(list, key = { it.id }) { m -> MatchRow(m, onClick = { onMatch(m.id) }) }
        }
    }
}
