package app.vigil.domain

import app.vigil.data.Event
import app.vigil.data.Urge
import app.vigil.data.Vow
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

const val DAY_MS = 86_400_000L

/** One bead of a vow's prayer rope: one day. */
enum class Bead {
    /** Not yet come. */
    FUTURE,
    /** Today, still going. */
    TODAY,
    /** A day kept. */
    KEPT,
    /** A day a temptation was held: a gold bead. */
    GOLD,
    /** Broken but forgiven or atoned for. */
    MENDED,
    /** Broken, and the crack stays. */
    CRACK,
    /** A practice day not done. */
    MISSED,
    /** A practice day gone by without a tick, not yet confessed. */
    OPEN,
}

object Vows {
    /** Days on which a vow is known to bite, with a word of warning. */
    val DEMON_DAYS = listOf(3, 7, 14, 21, 30, 40)

    private val lapses = setOf(EventKind.CRACK, EventKind.MISSED, EventKind.BURN)
    private val mends = setOf(EventKind.INDULGENCE, EventKind.PENANCE)

    fun started(v: Vow, now: Long) = now >= v.startAt

    /** The vow's own day, 1-based; 0 before it starts. */
    fun day(v: Vow, now: Long): Int = if (now < v.startAt) 0 else ((now - v.startAt) / DAY_MS).toInt() + 1

    /** Its length with the penance added; null for a vow with no end. */
    fun total(v: Vow): Int? = if (v.days <= 0) null else v.days + v.extraDays

    fun endAt(v: Vow): Long? = total(v)?.let { v.startAt + it * DAY_MS }

    /** The last day has passed with the vow unbroken. */
    fun fulfilled(v: Vow, now: Long) = v.active && endAt(v)?.let { now >= it } == true

    /** Today's day, never past the end. */
    fun current(v: Vow, now: Long): Int = total(v)?.let { day(v, now).coerceAtMost(it) } ?: day(v, now)

    fun progress(v: Vow, now: Long): Float {
        val t = total(v) ?: return 0f
        if (now <= v.startAt) return 0f
        return ((now - v.startAt).toFloat() / (t * DAY_MS)).coerceIn(0f, 1f)
    }

    /** Novices are forgiven one lapse in every ten days. */
    fun indulgencesLeft(v: Vow, now: Long): Int =
        if (v.rigorOf != Rigor.NOVICE) 0 else ((day(v, now).coerceAtLeast(1) - 1) / 10 + 1 - v.indulgences).coerceAtLeast(0)

    /** Days of penance for one lapse. */
    fun penance(r: Rigor): Int = when (r) {
        Rigor.NOVICE -> 1
        Rigor.MONK -> 3
        Rigor.HERMIT -> 0
    }

    /** What may become of the vow when it's broken today. A vow with no end has no end to add penance to. */
    fun outcomes(v: Vow, now: Long): List<Outcome> = when (v.rigorOf) {
        Rigor.NOVICE -> listOfNotNull(
            Outcome.INDULGENCE.takeIf { indulgencesLeft(v, now) > 0 },
            Outcome.PENANCE.takeIf { total(v) != null },
            Outcome.CRACK,
            Outcome.BURN,
        )
        Rigor.MONK -> listOfNotNull(Outcome.PENANCE.takeIf { total(v) != null }, Outcome.CRACK, Outcome.BURN)
        Rigor.HERMIT -> listOf(Outcome.BURN)
    }

    /** A hard day ahead, if today is one. */
    fun demon(v: Vow, now: Long): Int? = day(v, now).takeIf { v.active && it in DEMON_DAYS && (total(v)?.let { t -> it <= t } ?: true) }

    /** The ladder a vow starts with, by its rigor. */
    fun defaultLadder(r: Rigor): List<Step> = when (r) {
        Rigor.NOVICE -> listOf(Step.WAIT, Step.QUESTIONS, Step.RANSOM)
        Rigor.MONK -> listOf(Step.WAIT, Step.VOICE, Step.LETTER, Step.QUESTIONS)
        Rigor.HERMIT -> listOf(Step.WAIT, Step.VOICE, Step.LETTER, Step.QUESTIONS, Step.TOMORROW)
    }

    fun defaultWait(r: Rigor): Int = when (r) {
        Rigor.NOVICE -> 5
        Rigor.MONK -> 10
        Rigor.HERMIT -> 30
    }

    fun defaultLetter(r: Rigor): Int = when (r) {
        Rigor.NOVICE -> 60
        Rigor.MONK -> 140
        Rigor.HERMIT -> 300
    }

    /** Practice days gone by without a tick or an answer, newest first, at most a week back. */
    fun unconfessed(v: Vow, events: List<Event>, now: Long): List<Int> {
        if (v.typeOf != VowType.PRACTICE || !v.active) return emptyList()
        val today = current(v, now)
        val answered = events.filter { it.vowId == v.id && (it.kindOf == EventKind.DONE || it.kindOf in lapses || it.kindOf in mends) }.map { it.day }.toSet()
        return (today - 1 downTo maxOf(1, today - 7)).filter { it !in answered }
    }

    fun doneOn(v: Vow, events: List<Event>, day: Int) = events.any { it.vowId == v.id && it.day == day && it.kindOf == EventKind.DONE }

    /** The evening examen was answered today. */
    fun examined(v: Vow, events: List<Event>, now: Long): Boolean {
        val d = day(v, now)
        return events.any { it.vowId == v.id && it.day == d && it.kindOf in setOf(EventKind.KEPT, EventKind.HARD, EventKind.CRACK, EventKind.INDULGENCE, EventKind.PENANCE, EventKind.MISSED) }
    }

    /** One bead per day: the whole length, or the days so far for a vow with no end. */
    fun beads(v: Vow, events: List<Event>, urges: List<Urge>, now: Long): List<Bead> {
        val mine = events.filter { it.vowId == v.id }.groupBy { it.day }
        val held = urges.filter { it.vowId == v.id && it.held }.map { dayAt(v, it.at) }.toSet() +
            mine.values.flatten().filter { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM }.map { it.day }
        val ended = !v.active
        val last = if (ended) v.endedAt?.let { day(v, it - 1) } ?: current(v, now) else current(v, now)
        val n = total(v) ?: last.coerceAtLeast(1)
        val practice = v.typeOf == VowType.PRACTICE
        return (1..n).map { d ->
            val kinds = mine[d].orEmpty().map { it.kindOf }
            when {
                EventKind.CRACK in kinds || EventKind.BURN in kinds -> Bead.CRACK
                EventKind.MISSED in kinds && kinds.none { it in mends } -> Bead.MISSED
                kinds.any { it in mends } -> Bead.MENDED
                d > last -> Bead.FUTURE
                d == last && !ended && !fulfilled(v, now) -> when {
                    d in held -> Bead.GOLD
                    practice && EventKind.DONE in kinds -> Bead.KEPT
                    else -> Bead.TODAY
                }
                d in held -> Bead.GOLD
                practice && EventKind.DONE !in kinds -> Bead.OPEN
                else -> Bead.KEPT
            }
        }
    }

    /** Days kept so far: gone by (or today, once ticked off) and not lapsed. */
    fun keptDays(v: Vow, events: List<Event>, urges: List<Urge>, now: Long): Int =
        beads(v, events, urges, now).count { it == Bead.KEPT || it == Bead.GOLD || it == Bead.MENDED }

    fun dayAt(v: Vow, at: Long): Int = day(v, at).coerceAtLeast(1)

    /** Now inside a [VowType.WINDOW] vow's quiet hours. */
    fun inWindow(v: Vow, minuteOfDay: Int): Boolean {
        if (v.typeOf != VowType.WINDOW || v.windowFrom < 0 || v.windowTo < 0) return false
        return if (v.windowFrom <= v.windowTo) minuteOfDay in v.windowFrom until v.windowTo
        else minuteOfDay >= v.windowFrom || minuteOfDay < v.windowTo
    }

    /** Breaking was put off and the hour has not come yet. */
    fun delayed(v: Vow, now: Long) = v.breakAt != null && now < v.breakAt

    /** Breaking was put off and may now go on (for a day; after that the wish has passed). */
    fun delayOver(v: Vow, now: Long) = v.breakAt != null && now >= v.breakAt && now < v.breakAt + DAY_MS

    /** The next time [minutes] after midnight comes, at least a few hours from now. */
    fun tomorrowAt(now: Long, minutes: Int, zone: ZoneId): Long {
        val d = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
        return d.atTime(minutes / 60, minutes % 60).atZone(zone).toInstant().toEpochMilli()
    }
}

/** Where and when temptations come: a weekday × time-of-day grid, the triggers and how many were held. */
class UrgeMap(urges: List<Urge>, zone: ZoneId) {
    /** [weekday 0..6][slot 0..5], slots of four hours from midnight. */
    val grid: Array<IntArray> = Array(7) { IntArray(SLOTS) }
    val triggers: List<Pair<Trigger, Int>>
    val total = urges.size
    val held = urges.count { it.held }
    val peak: Pair<DayOfWeek, Int>?

    init {
        urges.forEach {
            val z = Instant.ofEpochMilli(it.at).atZone(zone)
            grid[z.dayOfWeek.value - 1][z.hour / 4]++
        }
        triggers = urges.mapNotNull { it.triggerOf }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
        var best = 0
        var at: Pair<DayOfWeek, Int>? = null
        for (d in 0 until 7) for (s in 0 until SLOTS) if (grid[d][s] > best) { best = grid[d][s]; at = DayOfWeek.of(d + 1) to s }
        peak = at
    }

    val max: Int get() = grid.maxOf { it.max() }

    /** Share of temptations falling in the peak cell's day and slot. */
    fun peakShare(): Int = peak?.let { (d, s) -> if (total == 0) 0 else grid[d.value - 1][s] * 100 / total } ?: 0

    companion object {
        const val SLOTS = 6
    }
}
