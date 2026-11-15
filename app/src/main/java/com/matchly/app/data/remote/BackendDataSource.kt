package com.matchly.app.data.remote

import com.matchly.app.data.FootballDataSource
import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.Health
import com.matchly.app.data.model.Injury
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TopScorer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class BackendDataSource(private val api: MatchlyApi) : FootballDataSource {
    override val isDemo: Boolean = false

    override suspend fun fixturesByDate(date: String, timeZone: String): List<Match> = api.fixtures(date, timeZone).data
    override suspend fun live(): List<Match> = api.live().data
    override suspend fun matchDetail(id: Long): MatchDetail = api.matchDetail(id).data
    override suspend fun headToHead(homeId: Long, awayId: Long): List<Match> = api.headToHead(homeId, awayId).data
    override suspend fun injuries(matchId: Long): List<Injury> = api.injuries(matchId).data
    override suspend fun competitions(): List<Competition> = api.competitions().data
    override suspend fun competitionFixtures(id: Long, season: Int): List<Match> = api.competitionFixtures(id, season).data
    override suspend fun standings(id: Long, season: Int): List<StandingGroup> = api.standings(id, season).data
    override suspend fun topScorers(id: Long, season: Int): List<TopScorer> = api.topScorers(id, season).data
    override suspend fun search(query: String): SearchResult = api.search(query).data
    override suspend fun team(id: Long): TeamProfile = api.team(id).data
    override suspend fun teamFixtures(id: Long): List<Match> = api.teamFixtures(id).data
    override suspend fun player(id: Long, season: Int): PlayerProfile = api.player(id, season).data

    suspend fun health(): Health = api.health()

    companion object {
        /** Returns a normalised base URL ending with "/", or null if the text is not a valid http(s) URL. */
        fun normalizeBaseUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val parsed = trimmed.toHttpUrlOrNull() ?: return null
            val text = parsed.toString()
            return if (text.endsWith("/")) text else "$text/"
        }

        fun create(baseUrl: String, token: String, json: Json): BackendDataSource {
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val builder = chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "MATCHLY-Android/0.1")
                    if (token.isNotBlank()) builder.header("x-matchly-token", token.trim())
                    chain.proceed(builder.build())
                }
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            return BackendDataSource(retrofit.create(MatchlyApi::class.java))
        }
    }
}
