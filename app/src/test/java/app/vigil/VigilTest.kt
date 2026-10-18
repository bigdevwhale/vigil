package app.vigil

import app.vigil.content.Content
import app.vigil.data.Event
import app.vigil.data.Urge
import app.vigil.data.Vow
import app.vigil.domain.Bead
import app.vigil.domain.Catalog
import app.vigil.domain.DAY_MS
import app.vigil.domain.Easter
import app.vigil.domain.EventKind
import app.vigil.domain.Gender
import app.vigil.domain.Outcome
import app.vigil.domain.Rigor
import app.vigil.domain.Season
import app.vigil.domain.Step
import app.vigil.domain.UrgeMap
import app.vigil.domain.VowType
import app.vigil.domain.Vows
import app.vigil.domain.gendered
import app.vigil.domain.steps
import app.vigil.domain.stepsCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class VigilTest {
    private val start = LocalDateTime.of(2026, 10, 1, 6, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun vow(
        days: Int = 21, rigor: Rigor = Rigor.MONK, type: VowType = VowType.ABSTAIN, extra: Int = 0, indulgences: Int = 0,
        from: Int = -1, to: Int = -1,
    ) = Vow(
        id = 1, title = "t", category = "BODY", type = type.name, days = days, createdAt = start, startAt = start,
        rigor = rigor.name, ladder = "", wax = "VERMILION", initial = "T", extraDays = extra, indulgences = indulgences,
        windowFrom = from, windowTo = to, seed = 1,
    )

    private fun at(day: Int, hours: Int = 2) = start + (day - 1) * DAY_MS + hours * 3_600_000L

    private fun ev(day: Int, kind: EventKind) = Event(vowId = 1, at = at(day), day = day, kind = kind.name)

    @Test
    fun daysAreCountedFromTheSealingHour() {
        val v = vow()
        assertEquals(0, Vows.day(v, start - 1))
        assertEquals(1, Vows.day(v, start))
        assertEquals(1, Vows.day(v, start + DAY_MS - 1))
        assertEquals(2, Vows.day(v, start + DAY_MS))
        assertEquals(start + 21 * DAY_MS, Vows.endAt(v))
        assertFalse(Vows.fulfilled(v, start + 21 * DAY_MS - 1))
        assertTrue(Vows.fulfilled(v, start + 21 * DAY_MS))
    }

    @Test
    fun penanceMovesTheEnd() {
        val v = vow(extra = 3)
        assertEquals(24, Vows.total(v))
        assertFalse(Vows.fulfilled(v, start + 22 * DAY_MS))
        assertEquals(3, Vows.penance(Rigor.MONK))
        assertEquals(1, Vows.penance(Rigor.NOVICE))
    }

    @Test
    fun openVowsHaveNoEnd() {
        val v = vow(days = 0)
        assertNull(Vows.total(v))
        assertNull(Vows.endAt(v))
        assertFalse(Vows.fulfilled(v, start + 1000 * DAY_MS))
        // No end to add penance to.
        assertEquals(listOf(Outcome.CRACK, Outcome.BURN), Vows.outcomes(v, at(5)))
    }

    @Test
    fun rigorDecidesWhatALapseCanBecome() {
        assertEquals(listOf(Outcome.INDULGENCE, Outcome.PENANCE, Outcome.CRACK, Outcome.BURN), Vows.outcomes(vow(rigor = Rigor.NOVICE), at(2)))
        assertEquals(listOf(Outcome.PENANCE, Outcome.CRACK, Outcome.BURN), Vows.outcomes(vow(rigor = Rigor.MONK), at(2)))
        assertEquals(listOf(Outcome.BURN), Vows.outcomes(vow(rigor = Rigor.HERMIT), at(2)))
    }

    @Test
    fun oneIndulgenceInTenDays() {
        assertEquals(1, Vows.indulgencesLeft(vow(rigor = Rigor.NOVICE), at(1)))
        assertEquals(0, Vows.indulgencesLeft(vow(rigor = Rigor.NOVICE, indulgences = 1), at(10)))
        assertEquals(1, Vows.indulgencesLeft(vow(rigor = Rigor.NOVICE, indulgences = 1), at(11)))
        assertEquals(0, Vows.indulgencesLeft(vow(rigor = Rigor.MONK), at(30)))
        // Spent: the novice is left with penance and the rest.
        assertEquals(listOf(Outcome.PENANCE, Outcome.CRACK, Outcome.BURN), Vows.outcomes(vow(rigor = Rigor.NOVICE, indulgences = 1), at(3)))
    }

    @Test
    fun beadsTellTheDays() {
        val v = vow(days = 7)
        val events = listOf(ev(2, EventKind.CRACK), ev(3, EventKind.PENANCE))
        val urges = listOf(Urge(vowId = 1, at = at(4), held = true), Urge(vowId = 1, at = at(1), held = false))
        val beads = Vows.beads(v, events, urges, at(5))
        assertEquals(listOf(Bead.KEPT, Bead.CRACK, Bead.MENDED, Bead.GOLD, Bead.TODAY, Bead.FUTURE, Bead.FUTURE), beads)
        assertEquals(3, Vows.keptDays(v, events, urges, at(5)))
    }

    @Test
    fun practiceDaysWaitForATick() {
        val v = vow(days = 7, type = VowType.PRACTICE)
        val events = listOf(ev(1, EventKind.DONE), ev(3, EventKind.MISSED), ev(3, EventKind.INDULGENCE))
        val beads = Vows.beads(v, events, emptyList(), at(4))
        assertEquals(listOf(Bead.KEPT, Bead.OPEN, Bead.MENDED, Bead.TODAY), beads.take(4))
        // Day 2 went by unanswered; day 3 was confessed.
        assertEquals(listOf(2), Vows.unconfessed(v, events, at(4)))
        assertTrue(Vows.doneOn(v, events, 1))
        assertEquals(Bead.KEPT, Vows.beads(v, events + ev(4, EventKind.DONE), emptyList(), at(4))[3])
    }

    @Test
    fun examenIsOncePerDay() {
        val v = vow()
        assertFalse(Vows.examined(v, emptyList(), at(3)))
        assertTrue(Vows.examined(v, listOf(ev(3, EventKind.HARD)), at(3)))
        assertFalse(Vows.examined(v, listOf(ev(2, EventKind.KEPT)), at(3)))
    }

    @Test
    fun windowsMayCrossMidnight() {
        val night = vow(type = VowType.WINDOW, from = 22 * 60, to = 7 * 60)
        assertTrue(Vows.inWindow(night, 23 * 60))
        assertTrue(Vows.inWindow(night, 3 * 60))
        assertFalse(Vows.inWindow(night, 12 * 60))
        val morning = vow(type = VowType.WINDOW, from = 6 * 60, to = 9 * 60)
        assertTrue(Vows.inWindow(morning, 7 * 60))
        assertFalse(Vows.inWindow(morning, 9 * 60))
    }

    @Test
    fun theHardDaysAreMarked() {
        assertEquals(3, Vows.demon(vow(), at(3)))
        assertNull(Vows.demon(vow(), at(4)))
        assertNull(Vows.demon(vow(days = 7), at(14)))
    }

    @Test
    fun laddersKeepTheirOrder() {
        assertEquals(listOf(Step.WAIT, Step.LETTER, Step.TOMORROW), steps("TOMORROW,WAIT,LETTER,NONSENSE"))
        assertEquals("WAIT,VOICE", stepsCsv(listOf(Step.VOICE, Step.WAIT, Step.VOICE)))
        assertTrue(Vows.defaultLadder(Rigor.HERMIT).contains(Step.TOMORROW))
    }

    @Test
    fun easterByBothReckonings() {
        assertEquals(LocalDate.of(2026, 4, 5), Easter.western(2026))
        assertEquals(LocalDate.of(2026, 4, 12), Easter.orthodox(2026))
        assertEquals(LocalDate.of(2025, 4, 20), Easter.western(2025))
        assertEquals(LocalDate.of(2025, 4, 20), Easter.orthodox(2025))
        assertEquals(LocalDate.of(2027, 5, 2), Easter.orthodox(2027))
    }

    @Test
    fun seasonsShowUpTwoWeeksAhead() {
        val ru = Season.near(LocalDate.of(2026, 10, 18), russian = true).map { it.first }
        assertEquals(listOf(Season.SOBER_OCTOBER, Season.NO_SPEND_NOVEMBER), ru)
        val dec = Season.near(LocalDate.of(2026, 12, 20), russian = true).map { it.first }
        assertEquals(listOf(Season.NATIVITY_FAST, Season.DRY_JANUARY), dec)
        assertFalse(Season.NATIVITY_FAST in Season.near(LocalDate.of(2026, 12, 20), russian = false).map { it.first })
        val lent = Season.near(LocalDate.of(2026, 2, 20), russian = true).first { it.first == Season.GREAT_LENT }
        assertEquals(LocalDate.of(2026, 2, 23), lent.second)
    }

    @Test
    fun theCatalogIsWholeInBothLanguages() {
        assertTrue(Content.missing().toString(), Content.missing().isEmpty())
        assertEquals(Catalog.all.size, Catalog.all.map { it.id }.toSet().size)
        Catalog.all.mapNotNull { it.next }.forEach { assertTrue(it, Catalog.get(it) != null) }
        Season.entries.forEach { assertTrue(it.name, Catalog.get(it.preset) != null) }
        assertTrue(Catalog.all.size >= 60)
        Catalog.all.filter { it.type == VowType.WINDOW }.forEach { assertTrue(it.id, it.windowFrom >= 0 && it.windowTo >= 0) }
    }

    @Test
    fun genderMarksResolve() {
        assertEquals("ты выстоял", gendered("ты выстоял{|а}", Gender.MALE))
        assertEquals("ты выстояла", gendered("ты выстоял{|а}", Gender.FEMALE))
        assertEquals("сам", gendered("{сам|сама}", Gender.MALE))
    }

    @Test
    fun theMapFindsThePeak() {
        val fri = LocalDateTime.of(2026, 10, 16, 21, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val mon = LocalDateTime.of(2026, 10, 12, 9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val urges = listOf(Urge(vowId = 1, at = fri), Urge(vowId = 1, at = fri + 60_000), Urge(vowId = 1, at = mon, held = false))
        val map = UrgeMap(urges, ZoneOffset.UTC)
        assertEquals(DayOfWeek.FRIDAY to 5, map.peak)
        assertEquals(66, map.peakShare())
        assertEquals(2, map.held)
    }
}
