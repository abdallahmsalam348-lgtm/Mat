package com.matchly.app.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matchly.app.R
import com.matchly.app.ui.screens.CompetitionScreen
import com.matchly.app.ui.screens.CompetitionsScreen
import com.matchly.app.ui.screens.FavoritesScreen
import com.matchly.app.ui.screens.HomeScreen
import com.matchly.app.ui.screens.MatchDetailScreen
import com.matchly.app.ui.screens.PlayerScreen
import com.matchly.app.ui.screens.SearchScreen
import com.matchly.app.ui.screens.SettingsScreen
import com.matchly.app.ui.screens.TeamScreen
import com.matchly.app.ui.theme.Matchly

private enum class TopLevel(val route: String, val label: Int, val icon: ImageVector) {
    Home("home", R.string.nav_scores, Icons.Filled.Home),
    Competitions("competitions", R.string.nav_competitions, Icons.AutoMirrored.Filled.List),
    Search("search", R.string.nav_search, Icons.Filled.Search),
    Favorites("favorites", R.string.nav_favorites, Icons.Filled.Favorite),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}

private fun NavHostController.openMatch(id: Long) = navigate("match/$id")
private fun NavHostController.openTeam(id: Long) = navigate("team/$id")
private fun NavHostController.openPlayer(id: Long) = navigate("player/$id")
private fun NavHostController.openCompetition(id: Long, season: Int, name: String) =
    navigate("competition/$id/$season/${Uri.encode(name.ifBlank { "-" })}")

@Composable
fun MatchlyRoot(pendingMatchId: Long?, onPendingConsumed: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val c = Matchly.colors

    LaunchedEffect(pendingMatchId) {
        if (pendingMatchId != null) {
            nav.openMatch(pendingMatchId)
            onPendingConsumed()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (TopLevel.entries.any { it.route == route }) {
                NavigationBar(containerColor = c.barBackground, contentColor = c.textPrimary) {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = {
                                if (route != item.route) {
                                    nav.navigate(item.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.emerald,
                                selectedTextColor = c.emerald,
                                indicatorColor = c.emerald.copy(alpha = 0.18f),
                                unselectedIconColor = c.textSecondary,
                                unselectedTextColor = c.textSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad),
            enterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(160)) + androidx.compose.animation.scaleOut(tween(200), targetScale = 0.97f) },
        ) {
            matchlyGraph(nav)
        }
    }
}

private fun NavGraphBuilder.matchlyGraph(nav: NavHostController) {
    composable("home") {
        HomeScreen(onMatch = nav::openMatch, onCompetition = nav::openCompetition)
    }
    composable("competitions") {
        CompetitionsScreen(onCompetition = nav::openCompetition)
    }
    composable("search") {
        SearchScreen(onTeam = nav::openTeam, onCompetition = nav::openCompetition)
    }
    composable("favorites") {
        FavoritesScreen(onTeam = nav::openTeam, onCompetition = nav::openCompetition, onPlayer = nav::openPlayer)
    }
    composable("settings") {
        SettingsScreen(onFavorites = {
            nav.navigate("favorites") {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        })
    }
    composable("match/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
        MatchDetailScreen(
            matchId = e.arguments?.getLong("id") ?: 0L,
            onBack = { nav.popBackStack() },
            onTeam = nav::openTeam,
            onMatch = nav::openMatch,
            onCompetition = nav::openCompetition,
            onPlayer = nav::openPlayer,
        )
    }
    composable(
        "competition/{id}/{season}/{name}",
        arguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("season") { type = NavType.IntType },
            navArgument("name") { type = NavType.StringType },
        ),
    ) { e ->
        CompetitionScreen(
            id = e.arguments?.getLong("id") ?: 0L,
            season = e.arguments?.getInt("season") ?: 0,
            name = e.arguments?.getString("name") ?: "",
            onBack = { nav.popBackStack() },
            onTeam = nav::openTeam,
            onMatch = nav::openMatch,
            onPlayer = nav::openPlayer,
        )
    }
    composable("team/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
        TeamScreen(
            teamId = e.arguments?.getLong("id") ?: 0L,
            onBack = { nav.popBackStack() },
            onMatch = nav::openMatch,
            onPlayer = nav::openPlayer,
        )
    }
    composable("player/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
        PlayerScreen(playerId = e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
    }
}
