package com.matchly.app

import com.matchly.app.core.Formatters
import com.matchly.app.core.Seasons
import com.matchly.app.data.model.DisplayScore
import com.matchly.app.data.model.StatusInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FormattersTest {
    private val utc = ZoneId.of("UTC")
    private val en = Formatters("en", true, utc)
    private val ar = Formatters("ar", true, utc)
    private val arLatin = Formatters("ar", false, utc)

    // ---- localization of numbers
    @Test
    fun arabicDigitsAreUsedInArabic() {
        assertEquals("١٢:٣٠", ar.digits("12:30"))
        assertEquals("٠٩٨٧", ar.digits("0987"))
    }

    @Test
    fun latinDigitsWhenDisabledOrEnglish() {
        assertEquals("12:30", arLatin.digits("12:30"))
        assertEquals("12:30", en.digits("12:30"))
    }

    @Test
    fun nonDigitsAreKept() {
        assertEquals("٤٥+٢'", ar.digits("45+2'"))
    }

    @Test
    fun numberHandlesNull() {
        assertEquals("-", en.number(null))
        assertEquals("٧", ar.number(7))
    }

    // ---- time zone conversion
    @Test
    fun timeConvertsToZone() {
        val kickoff = Instant.parse("2026-01-15T19:00:00Z")
        assertEquals("19:00", Formatters("en", false, ZoneId.of("UTC")).time(kickoff))
        assertEquals("22:00", Formatters("en", false, ZoneId.of("Asia/Riyadh")).time(kickoff))
        assertEquals("14:00", Formatters("en", false, ZoneId.of("America/New_York")).time(kickoff))
    }

    @Test
    fun timeIsLocalizedForArabic() {
        val kickoff = Instant.parse("2026-01-15T19:00:00Z")
        assertEquals("٢٢:٠٠", Formatters("ar", true, ZoneId.of("Asia/Riyadh")).time(kickoff))
    }

    @Test
    fun localDateCrossesMidnight() {
        val late = Instant.parse("2026-01-15T23:30:00Z")
        assertEquals(LocalDate.of(2026, 1, 15), Formatters("en", false, ZoneId.of("UTC")).localDate(late))
        assertEquals(LocalDate.of(2026, 1, 16), Formatters("en", false, ZoneId.of("Asia/Riyadh")).localDate(late))
    }

    @Test
    fun dayLabels() {
        val friday = LocalDate.of(2026, 11, 13)
        assertEquals("Fri", en.weekday(friday))
        assertNotEquals("Fri", ar.weekday(friday))
        assertEquals("13", en.dayOfMonth(friday))
        assertEquals("١٣", ar.dayOfMonth(friday))
    }

    // ---- match clock
    @Test
    fun minuteDuringPlay() {
        assertEquals("67'", en.minute(StatusInfo("2H", 67, null)))
        assertEquals("45+2'", en.minute(StatusInfo("1H", 45, 2)))
        assertEquals("٦٧'", ar.minute(StatusInfo("2H", 67, null)))
    }

    @Test
    fun noMinuteWhenClockNotRunning() {
        assertNull(en.minute(StatusInfo("HT", 45, null)))
        assertNull(en.minute(StatusInfo("NS", null, null)))
        assertNull(en.minute(StatusInfo("FT", 90, null)))
        assertNull(en.minute(StatusInfo("1H", null, null)))
    }

    @Test
    fun eventMinute() {
        assertEquals("90+3'", en.eventMinute(90, 3))
        assertEquals("12'", en.eventMinute(12, null))
        assertEquals("", en.eventMinute(null, null))
        assertEquals("١٢'", ar.eventMinute(12, 0))
    }

    // ---- score text
    @Test
    fun scoreLines() {
        assertEquals("2 - 1", en.scoreLine(DisplayScore(2, 1, null, null)))
        assertEquals("٢ - ١", ar.scoreLine(DisplayScore(2, 1, null, null)))
        assertEquals("-", en.scoreLine(DisplayScore(null, null, null, null)))
        assertEquals("(4 - 3)", en.penaltyLine(DisplayScore(1, 1, 4, 3)))
        assertNull(en.penaltyLine(DisplayScore(1, 1, null, null)))
    }

    // ---- seasons
    @Test
    fun seasonRolloverInJuly() {
        assertEquals(2025, Seasons.current(LocalDate.of(2026, 6, 30)))
        assertEquals(2026, Seasons.current(LocalDate.of(2026, 7, 1)))
        assertEquals(2026, Seasons.current(LocalDate.of(2026, 11, 13)))
        assertEquals(2025, Seasons.current(LocalDate.of(2026, 1, 15)))
    }
}
