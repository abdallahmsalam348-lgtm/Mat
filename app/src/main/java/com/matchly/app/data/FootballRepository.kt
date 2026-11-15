package com.matchly.app.data

import com.matchly.app.BuildConfig
import com.matchly.app.core.SettingsStore
import com.matchly.app.data.demo.DemoDataSource
import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.Injury
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TopScorer
import com.matchly.app.data.remote.BackendDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single entry point for data. Strategy per request:
 *  1. DEMO source (no backend configured, or demo forced): fictional data, never cached, flagged demo.
 *  2. fresh cache -> return it; stale cache -> emit it immediately, then try the network;
 *  3. network failure with cache available -> keep showing cache and report the error kind;
 *  4. network failure without cache -> throw so the UI shows an error/retry state.
 */
class FootballRepository(
    private val settings: SettingsStore,
    private val cache: JsonCache,
    private val json: Json,
) {
    private var backendCache: Pair<String, BackendDataSource>? = null
    private val demo = DemoDataSource()

    /** The backend URL actually in use (settings override first, then build-time value). Null = demo mode. */
    fun effectiveBackendUrl(): String? {
        val s = settings.value
        if (s.forceDemo) return null
        val fromSettings = BackendDataSource.normalizeBaseUrl(s.backendUrl)
        if (fromSettings != null) return fromSettings
        return BackendDataSource.normalizeBaseUrl(BuildConfig.BACKEND_URL)
    }

    val isDemo: Boolean get() = effectiveBackendUrl() == null

    private fun backend(): BackendDataSource? {
        val url = effectiveBackendUrl() ?: return null
        val s = settings.value
        val token = s.appToken.ifBlank { BuildConfig.APP_TOKEN }
        val key = url + "|" + token
        val existing = backendCache
        if (existing != null && existing.first == key) return existing.second
        val created = BackendDataSource.create(url, token, json)
        backendCache = key to created
        return created
    }

    suspend fun health() = backend()?.health()

    private fun cacheScope(): String = effectiveBackendUrl() ?: "demo"

    private fun <T> load(
        key: String,
        ttlMs: Long,
        serializer: KSerializer<T>,
        force: Boolean,
        fetch: suspend (FootballDataSource) -> T,
    ): Flow<Loaded<T>> = flow {
        val be = backend()
        if (be == null) {
            emit(Loaded(fetch(demo), SourceInfo(demo = true)))
            return@flow
        }
        val fullKey = cacheScope() + "|" + key
        val cached = cache.read(fullKey, serializer)
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.savedAt
            val fresh = age <= ttlMs && !force
            emit(Loaded(cached.data, SourceInfo(fromCache = true, savedAt = cached.savedAt, refreshing = !fresh)))
            if (fresh) return@flow
        }
        try {
            val data = fetch(be)
            cache.write(fullKey, serializer, data)
            emit(Loaded(data, SourceInfo(savedAt = System.currentTimeMillis())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val kind = ErrorKind.from(e)
            if (cached != null) {
                emit(Loaded(cached.data, SourceInfo(fromCache = true, savedAt = cached.savedAt, error = kind)))
            } else {
                throw ApiException(kind, e.message, e)
            }
        }
    }

    private val matchList get() = ListSerializer(Match.serializer())

    fun fixtures(date: LocalDate, zone: ZoneId, force: Boolean = false): Flow<Loaded<List<Match>>> {
        val today = LocalDate.now(zone)
        val ttl = when {
            date == today -> 45_000L
            date.isAfter(today) -> 30 * 60_000L
            else -> 60 * 60_000L
        }
        return load("fixtures:${zone.id}:$date", ttl, matchList, force) { it.fixturesByDate(date.toString(), zone.id) }
    }

    fun matchDetail(id: Long, live: Boolean, force: Boolean = false): Flow<Loaded<MatchDetail>> =
        load("match:$id", if (live) 30_000L else 10 * 60_000L, MatchDetail.serializer(), force) { it.matchDetail(id) }

    fun headToHead(homeId: Long, awayId: Long, force: Boolean = false): Flow<Loaded<List<Match>>> =
        load("h2h:$homeId:$awayId", 6 * 3_600_000L, matchList, force) { it.headToHead(homeId, awayId) }

    fun injuries(matchId: Long, force: Boolean = false): Flow<Loaded<List<Injury>>> =
        load("injuries:$matchId", 3_600_000L, ListSerializer(Injury.serializer()), force) { it.injuries(matchId) }

    fun competitions(force: Boolean = false): Flow<Loaded<List<Competition>>> =
        load("competitions", 12 * 3_600_000L, ListSerializer(Competition.serializer()), force) { it.competitions() }

    fun competitionFixtures(id: Long, season: Int, force: Boolean = false): Flow<Loaded<List<Match>>> =
        load("compfix:$id:$season", 15 * 60_000L, matchList, force) { it.competitionFixtures(id, season) }

    fun standings(id: Long, season: Int, force: Boolean = false): Flow<Loaded<List<StandingGroup>>> =
        load("standings:$id:$season", 30 * 60_000L, ListSerializer(StandingGroup.serializer()), force) { it.standings(id, season) }

    fun topScorers(id: Long, season: Int, force: Boolean = false): Flow<Loaded<List<TopScorer>>> =
        load("scorers:$id:$season", 60 * 60_000L, ListSerializer(TopScorer.serializer()), force) { it.topScorers(id, season) }

    fun search(query: String, force: Boolean = false): Flow<Loaded<SearchResult>> =
        load("search:${query.lowercase()}", 6 * 3_600_000L, SearchResult.serializer(), force) { it.search(query) }

    fun team(id: Long, force: Boolean = false): Flow<Loaded<TeamProfile>> =
        load("team:$id", 12 * 3_600_000L, TeamProfile.serializer(), force) { it.team(id) }

    fun teamFixtures(id: Long, force: Boolean = false): Flow<Loaded<List<Match>>> =
        load("teamfix:$id", 15 * 60_000L, matchList, force) { it.teamFixtures(id) }

    fun player(id: Long, season: Int, force: Boolean = false): Flow<Loaded<PlayerProfile>> =
        load("player:$id:$season", 12 * 3_600_000L, PlayerProfile.serializer(), force) { it.player(id, season) }
}
