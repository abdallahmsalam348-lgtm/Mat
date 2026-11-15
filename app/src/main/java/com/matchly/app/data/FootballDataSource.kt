package com.matchly.app.data

import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.Injury
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TopScorer

/**
 * Provider abstraction used by the app. Today there are two implementations:
 *  - BackendDataSource: talks to the MATCHLY backend proxy (which holds the sports-API key)
 *  - DemoDataSource:    fictional teams, clearly labelled DEMO in the UI
 * A new sports provider is added on the BACKEND (see backend/src/providers); the app does not change.
 */
interface FootballDataSource {
    val isDemo: Boolean

    suspend fun fixturesByDate(date: String, timeZone: String): List<Match>
    suspend fun live(): List<Match>
    suspend fun matchDetail(id: Long): MatchDetail
    suspend fun headToHead(homeId: Long, awayId: Long): List<Match>
    suspend fun injuries(matchId: Long): List<Injury>
    suspend fun competitions(): List<Competition>
    suspend fun competitionFixtures(id: Long, season: Int): List<Match>
    suspend fun standings(id: Long, season: Int): List<StandingGroup>
    suspend fun topScorers(id: Long, season: Int): List<TopScorer>
    suspend fun search(query: String): SearchResult
    suspend fun team(id: Long): TeamProfile
    suspend fun teamFixtures(id: Long): List<Match>
    suspend fun player(id: Long, season: Int): PlayerProfile
}
