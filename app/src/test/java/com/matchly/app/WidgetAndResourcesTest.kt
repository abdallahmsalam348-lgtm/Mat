package com.matchly.app

import com.matchly.app.data.model.CompetitionRef
import com.matchly.app.data.model.GoalPair
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.StatusInfo
import com.matchly.app.data.model.TeamRef
import com.matchly.app.widget.WidgetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetAndResourcesTest {
    private fun m(id: Long, code: String, home: Long = id * 10, away: Long = id * 10 + 1, comp: Long = 1, kickoff: String = "2026-11-13T19:00:00Z") =
        Match(
            id = id,
            kickoff = kickoff,
            status = StatusInfo(code),
            competition = CompetitionRef(id = comp, name = "C$comp"),
            home = TeamRef(home, "H$id"),
            away = TeamRef(away, "A$id"),
            goals = GoalPair(0, 0),
        )

    @Test
    fun liveBeatsUpcomingBeatsFinished() {
        val list = listOf(m(1, "FT"), m(2, "NS"), m(3, "1H"))
        val picked = WidgetSelection.select(list, emptySet(), emptySet(), 3).map { it.id }
        assertEquals(listOf(3L, 2L, 1L), picked)
    }

    @Test
    fun favoritesWinWithinTheSameGroup() {
        val list = listOf(
            m(1, "NS", kickoff = "2026-11-13T15:00:00Z"),
            m(2, "NS", home = 99, kickoff = "2026-11-13T20:00:00Z"),
        )
        val picked = WidgetSelection.select(list, setOf(99L), emptySet(), 1).map { it.id }
        assertEquals(listOf(2L), picked)
    }

    @Test
    fun favoriteCompetitionCounts() {
        val list = listOf(m(1, "NS", comp = 1), m(2, "NS", comp = 7, kickoff = "2026-11-13T22:00:00Z"))
        assertEquals(2L, WidgetSelection.select(list, emptySet(), setOf(7L), 1).first().id)
    }

    @Test
    fun limitIsRespectedAndKickoffOrders() {
        val list = listOf(
            m(1, "NS", kickoff = "2026-11-13T21:00:00Z"),
            m(2, "NS", kickoff = "2026-11-13T15:00:00Z"),
            m(3, "NS", kickoff = "2026-11-13T18:00:00Z"),
        )
        assertEquals(listOf(2L, 3L), WidgetSelection.select(list, emptySet(), emptySet(), 2).map { it.id })
        assertTrue(WidgetSelection.select(emptyList(), emptySet(), emptySet(), 3).isEmpty())
    }

    // ---- localization completeness: every English string exists in Arabic with the same placeholders
    @Test
    fun arabicAndEnglishStringsMatch() {
        val en = readStrings("src/main/res/values/strings.xml")
        val ar = readStrings("src/main/res/values-ar/strings.xml")
        assertTrue("English strings file should not be empty", en.isNotEmpty())
        assertEquals("Missing Arabic translations", emptySet<String>(), en.keys - ar.keys)
        assertEquals("Arabic strings without English source", emptySet<String>(), ar.keys - en.keys)
        for ((k, v) in en) {
            assertEquals("Placeholders differ for $k", placeholders(v), placeholders(ar.getValue(k)))
        }
    }

    private fun placeholders(s: String): List<String> =
        Regex("%\\d\\$[sd]").findAll(s).map { it.value }.sorted().toList()

    private fun readStrings(path: String): Map<String, String> {
        val file = listOf(File(path), File("app/$path")).first { it.exists() }
        val text = file.readText()
        return Regex("<string name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }
}
