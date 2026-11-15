package com.matchly.app

import com.matchly.app.core.AppSettings
import com.matchly.app.core.LocaleHelper
import com.matchly.app.data.ErrorKind
import com.matchly.app.data.model.Envelope
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.displayScore
import com.matchly.app.data.model.matchStatus
import com.matchly.app.data.remote.BackendDataSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class ParsingAndConfigTest {
    // Same configuration as AppContainer.json
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun parsesBackendEnvelope() {
        val text = """
            {"data":[{"id":42,"kickoff":"2026-11-13T19:00:00Z",
              "status":{"code":"2H","elapsed":67},
              "competition":{"id":39,"name":"Premier League","round":"Regular Season - 12","season":2026},
              "home":{"id":1,"name":"A"},"away":{"id":2,"name":"B"},
              "goals":{"home":2,"away":1},
              "unknownField":"ignored"}],
             "meta":{"provider":"api-football","cached":true,"stale":false}}
        """.trimIndent()
        val env = json.decodeFromString<Envelope<List<Match>>>(text)
        assertEquals(1, env.data.size)
        val m = env.data[0]
        assertEquals(42L, m.id)
        assertEquals(MatchStatus.SECOND_HALF, m.matchStatus)
        assertEquals(67, m.status.elapsed)
        assertEquals(2, m.displayScore().home)
        assertEquals("api-football", env.meta.provider)
        assertTrue(env.meta.cached)
    }

    @Test
    fun missingAndNullFieldsDoNotCrash() {
        val text = """{"data":[{"id":7,"status":{"code":"NS"},"goals":{"home":null,"away":null},"home":{"id":1,"name":"A"}}]}"""
        val env = json.decodeFromString<Envelope<List<Match>>>(text)
        val m = env.data[0]
        assertEquals("", m.away.name)
        assertEquals("", m.kickoff)
        assertFalse(m.displayScore().hasScore)
    }

    @Test
    fun parsesPenaltyShootout() {
        val text = """{"data":[{"id":9,"status":{"code":"PEN","elapsed":120},
            "goals":{"home":1,"away":1},
            "score":{"penalty":{"home":5,"away":4}}}]}"""
        val m = json.decodeFromString<Envelope<List<Match>>>(text).data[0]
        val s = m.displayScore()
        assertEquals(1, s.home)
        assertEquals(5, s.penaltyHome)
        assertEquals(4, s.penaltyAway)
    }

    // ---- backend URL handling
    @Test
    fun normalizesBackendUrl() {
        assertEquals("https://api.example.com/", BackendDataSource.normalizeBaseUrl("https://api.example.com"))
        assertEquals("https://api.example.com/", BackendDataSource.normalizeBaseUrl("  https://api.example.com/  "))
        assertEquals("https://api.example.com/v1/", BackendDataSource.normalizeBaseUrl("https://api.example.com/v1"))
        assertNull(BackendDataSource.normalizeBaseUrl(""))
        assertNull(BackendDataSource.normalizeBaseUrl("not a url"))
        assertNull(BackendDataSource.normalizeBaseUrl("ftp://example.com"))
    }

    // ---- API error mapping (rate limits, missing key, plan limits)
    @Test
    fun mapsHttpErrors() {
        assertEquals(ErrorKind.RATE_LIMITED, ErrorKind.fromHttp(429, ""))
        assertEquals(ErrorKind.UNAUTHORIZED, ErrorKind.fromHttp(401, ""))
        assertEquals(ErrorKind.UNAUTHORIZED, ErrorKind.fromHttp(403, ""))
        assertEquals(ErrorKind.NOT_FOUND, ErrorKind.fromHttp(404, ""))
        assertEquals(ErrorKind.SERVER, ErrorKind.fromHttp(502, ""))
        assertEquals(ErrorKind.NOT_CONFIGURED, ErrorKind.fromHttp(503, """{"error":"provider_not_configured"}"""))
        assertEquals(ErrorKind.PLAN_LIMIT, ErrorKind.fromHttp(403, """{"error":"plan_restricted"}"""))
        assertEquals(ErrorKind.UNKNOWN, ErrorKind.fromHttp(418, ""))
    }

    // ---- language + time zone settings
    @Test
    fun explicitLanguageIsRespected() {
        assertEquals("ar", LocaleHelper.effectiveLanguage("ar"))
        assertEquals("en", LocaleHelper.effectiveLanguage("en"))
    }

    @Test
    fun unsetLanguageFollowsDeviceArabicElseEnglish() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("ar", LocaleHelper.effectiveLanguage(""))
            Locale.setDefault(Locale.FRANCE)
            assertEquals("en", LocaleHelper.effectiveLanguage(""))
            assertEquals("en", LocaleHelper.effectiveLanguage("fr"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun invalidTimeZoneFallsBackToDevice() {
        assertEquals(ZoneId.systemDefault(), AppSettings(timeZoneId = "Not/AZone").zone)
        assertEquals(ZoneId.systemDefault(), AppSettings(timeZoneId = "").zone)
        assertEquals("Asia/Riyadh", AppSettings(timeZoneId = "Asia/Riyadh").zone.id)
    }
}
