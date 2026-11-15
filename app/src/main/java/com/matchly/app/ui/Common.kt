package com.matchly.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchly.app.AppContainer
import com.matchly.app.R
import com.matchly.app.core.Favorite
import com.matchly.app.core.Formatters
import com.matchly.app.data.ErrorKind
import com.matchly.app.data.Loaded
import com.matchly.app.data.Loader
import com.matchly.app.data.SourceInfo
import com.matchly.app.data.UiState
import com.matchly.app.ui.components.GlassCard
import com.matchly.app.ui.theme.Matchly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

val LocalFormatters = staticCompositionLocalOf<Formatters> { error("Formatters not provided") }
val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

fun <T : ViewModel> vmFactory(build: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = build() as V
    }

/** One generic ViewModel for every "load something through the repository" screen. */
class LoadVm<T>(source: (Boolean) -> Flow<Loaded<T>>) : ViewModel() {
    private val loader = Loader(viewModelScope, source)
    val state: StateFlow<UiState<T>> = loader.state

    init {
        loader.load(false)
    }

    fun reload(force: Boolean = true) = loader.load(force)
}

@Composable
fun <T> rememberLoad(key: String, source: (Boolean) -> Flow<Loaded<T>>): LoadVm<T> =
    viewModel<LoadVm<T>>(key = key, factory = vmFactory { LoadVm(source) })

@Composable
fun errorText(kind: ErrorKind): String = stringResource(
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

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Matchly.colors.emerald)
    }
}

@Composable
fun ErrorView(kind: ErrorKind, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = Matchly.colors
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.gold, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(errorText(kind), color = c.textPrimary, textAlign = TextAlign.Center, fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = c.emerald, contentColor = Color(0xFF00281B)),
        ) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Matchly.colors.textSecondary, textAlign = TextAlign.Center, fontSize = 15.sp)
    }
}

@Composable
fun SourceBanner(info: SourceInfo, onRetry: () -> Unit) {
    val c = Matchly.colors
    val f = LocalFormatters.current
    val message: String? = when {
        info.demo -> stringResource(R.string.banner_demo)
        info.error != null -> {
            val savedAt = info.savedAt
            if (savedAt != null) {
                stringResource(R.string.banner_cached_error, errorText(info.error), f.dateTime(Instant.ofEpochMilli(savedAt)))
            } else {
                errorText(info.error)
            }
        }
        else -> null
    }
    if (message != null) {
        GlassCard(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
            corner = 14.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = if (info.demo) c.gold else c.live,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    message,
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    color = c.textPrimary,
                    fontSize = 12.sp,
                )
                if (!info.demo) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.retry), tint = c.emerald)
                    }
                }
            }
        }
    }
    if (info.refreshing && info.error == null) {
        LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(2.dp),
            color = c.emerald,
            trackColor = Color.Transparent,
        )
    }
}

/** Renders loading / error / content (with offline + demo banners) for a [LoadVm]. */
@Composable
fun <T> StateView(
    vm: LoadVm<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T, SourceInfo) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> LoadingView(modifier)
        is UiState.Failure -> ErrorView(s.kind, { vm.reload() }, modifier)
        is UiState.Content -> Column(modifier) {
            SourceBanner(s.info) { vm.reload() }
            Box(Modifier.weight(1f)) { content(s.data, s.info) }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Matchly.colors
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.textPrimary)
            }
        }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 4.dp),
            color = c.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun FavoriteButton(fav: Favorite) {
    val store = LocalContainer.current.favorites
    val list by store.flow.collectAsStateWithLifecycle()
    val isFav = list.any { it.type == fav.type && it.id == fav.id }
    val c = Matchly.colors
    IconButton(onClick = { store.toggle(fav) }) {
        Icon(
            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(if (isFav) R.string.remove_favorite else R.string.add_favorite),
            tint = if (isFav) c.gold else c.textSecondary,
        )
    }
}

@Composable
fun RefreshButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh), tint = Matchly.colors.textPrimary)
    }
}
