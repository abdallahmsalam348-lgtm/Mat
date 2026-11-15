package com.matchly.app

import com.matchly.app.data.model.GoalPair
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.ScoreBreakdown
import com.matchly.app.data.model.StatusInfo
import com.matchly.app.data.model.displayScore
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus
import com.matchly.app.data.model.penaltyWinner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MatchStatusTest {

    private fun match(code: String, h: Int?, a: Int?, penH: Int? = null, penA: Int? = null, kickoff: String = "2026-11-13T19:00:00Z") =
        Match(
            id = 1,
            kickoff = kickoff,
            status = StatusInfo(code, null, null),
            goals = GoalPair(h, a),
            score = ScoreBreakdown(penalty = GoalPair(penH, penA)),
        )

    @Test
    fun codesMapToStatuses() {
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("NS"))
        assertEquals(MatchStatus.FIRST_HALF, MatchStatus.fromCode("1H"))
        assertEquals(MatchStatus.HALF_TIME, MatchStatus.fromCode("HT"))
        assertEquals(MatchStatus.SECOND_HALF, MatchStatus.fromCode("2H"))
        assertEquals(MatchStatus.FINISHED, MatchStatus.fromCode("FT"))
        assertEquals(MatchStatus.FINISHED_EXTRA_TIME, MatchStatus.fromCode("AET"))
        assertEquals(MatchStatus.FINISHED_PENALTIES, MatchStatus.fromCode("PEN"))
        assertEquals(MatchStatus.POSTPONED, MatchStatus.fromCode("PST"))
        assertEquals(MatchStatus.CANCELLED, MatchStatus.fromCode("CANC"))
        assertEquals(MatchStatus.ABANDONED, MatchStatus.fromCode("ABD"))
        assertEquals(MatchStatus.UNKNOWN, MatchStatus.fromCode("???"))
        assertEquals(MatchStatus.UNKNOWN, MatchStatus.fromCode(null))
        assertEquals(MatchStatus.FINISHED, MatchStatus.fromCode(" ft "))
    }

    @Test
    fun liveFinishedUpcomingClassification() {
        listOf("1H", "HT", "2H", "ET", "BT", "P", "LIVE").forEach {
            assertTrue("$it should be live", MatchStatus.fromCode(it).isLive)
        }
        listOf("FT", "AET", "PEN", "AWD", "WO").forEach {
            assertTrue("$it should be finished", MatchStatus.fromCode(it).isFinished)
            assertFalse("$it should not be live", MatchStatus.fromCode(it).isLive)
        }
        assertTrue(MatchStatus.SCHEDULED.isUpcoming)
        assertTrue(MatchStatus.TO_BE_DEFINED.isUpcoming)
        listOf("PST", "CANC", "ABD", "SUSP", "INT").forEach {
            assertTrue("$it should be disrupted", MatchStatus.fromCode(it).isDisrupted)
        }
    }

    @Test
    fun upcomingMatchHasNoScore() {
        val s = match("NS", null, null).displayScore()
        assertFalse(s.hasScore)
    }

    @Test
    fun postponedAndCancelledNeverShowScores() {
        assertFalse(match("PST", 0, 0).displayScore().hasScore)
        assertFalse(match("CANC", 0, 0).displayScore().hasScore)
    }

    @Test
    fun liveMatchWithMissingGoalsShowsZeroZero() {
        val s = match("1H", null, null).displayScore()
        assertTrue(s.hasScore)
        assertEquals(0, s.home)
        assertEquals(0, s.away)
    }

    @Test
    fun finishedScoreIsParsed() {
        val s = match("FT", 2, 1).displayScore()
        assertEquals(2, s.home)
        assertEquals(1, s.away)
        assertFalse(s.hasPenalties)
    }

    @Test
    fun penaltyShootoutIsReportedSeparately() {
        val s = match("PEN", 1, 1, 4, 3).displayScore()
        assertEquals(1, s.home)
        assertEquals(1, s.away)
        assertTrue(s.hasPenalties)
        assertEquals(4, s.penaltyHome)
        assertEquals(3, s.penaltyAway)
        assertEquals(1, s.penaltyWinner())
        assertEquals(-1, match("PEN", 0, 0, 2, 5).displayScore().penaltyWinner())
    }

    @Test
    fun zeroZeroShootoutKeepsRegularScore() {
        val s = match("PEN", 0, 0, 3, 4).displayScore()
        assertEquals(0, s.home)
        assertTrue(s.hasPenalties)
        assertEquals(-1, s.penaltyWinner())
    }

    @Test
    fun abandonedMatchKeepsItsPartialScore() {
        val s = match("ABD", 1, 0).displayScore()
        assertTrue(s.hasScore)
        assertEquals(1, s.home)
    }

    @Test
    fun unknownStatusWithoutGoalsHasNoScore() {
        assertFalse(match("XYZ", null, null).displayScore().hasScore)
    }

    @Test
    fun noPenaltyNoWinner() {
        assertEquals(0, match("FT", 1, 0).displayScore().penaltyWinner())
    }

    @Test
    fun kickoffParsing() {
        assertEquals(Instant.parse("2026-11-13T19:00:00Z"), match("NS", null, null).kickoffInstant)
        assertNotNull(match("NS", null, null, kickoff = "2026-11-13T21:00:00+02:00").kickoffInstant)
        assertEquals(
            Instant.parse("2026-11-13T19:00:00Z"),
            match("NS", null, null, kickoff = "2026-11-13T21:00:00+02:00").kickoffInstant,
        )
        assertNull(match("NS", null, null, kickoff = "").kickoffInstant)
        assertNull(match("NS", null, null, kickoff = "garbage").kickoffInstant)
    }

    @Test
    fun matchStatusExtension() {
        assertEquals(MatchStatus.HALF_TIME, match("HT", 1, 1).matchStatus)
    }
}
