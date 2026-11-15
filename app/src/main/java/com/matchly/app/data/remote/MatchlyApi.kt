package com.matchly.app.data.remote

import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.Envelope
import com.matchly.app.data.model.Health
import com.matchly.app.data.model.Injury
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TopScorer
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract of the MATCHLY backend proxy (see backend/README.md). */
interface MatchlyApi {
    @GET("v1/health")
    suspend fun health(): Health

    @GET("v1/fixtures")
    suspend fun fixtures(@Query("date") date: String, @Query("tz") tz: String): Envelope<List<Match>>

    @GET("v1/live")
    suspend fun live(): Envelope<List<Match>>

    @GET("v1/matches/{id}")
    suspend fun matchDetail(@Path("id") id: Long): Envelope<MatchDetail>

    @GET("v1/matches/{id}/injuries")
    suspend fun injuries(@Path("id") id: Long): Envelope<List<Injury>>

    @GET("v1/h2h")
    suspend fun headToHead(@Query("home") home: Long, @Query("away") away: Long): Envelope<List<Match>>

    @GET("v1/competitions")
    suspend fun competitions(): Envelope<List<Competition>>

    @GET("v1/competitions/{id}/fixtures")
    suspend fun competitionFixtures(@Path("id") id: Long, @Query("season") season: Int): Envelope<List<Match>>

    @GET("v1/competitions/{id}/standings")
    suspend fun standings(@Path("id") id: Long, @Query("season") season: Int): Envelope<List<StandingGroup>>

    @GET("v1/competitions/{id}/scorers")
    suspend fun topScorers(@Path("id") id: Long, @Query("season") season: Int): Envelope<List<TopScorer>>

    @GET("v1/search")
    suspend fun search(@Query("q") query: String): Envelope<SearchResult>

    @GET("v1/teams/{id}")
    suspend fun team(@Path("id") id: Long): Envelope<TeamProfile>

    @GET("v1/teams/{id}/fixtures")
    suspend fun teamFixtures(@Path("id") id: Long): Envelope<List<Match>>

    @GET("v1/players/{id}")
    suspend fun player(@Path("id") id: Long, @Query("season") season: Int): Envelope<PlayerProfile>
}
