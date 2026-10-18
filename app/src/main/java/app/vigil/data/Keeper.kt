package app.vigil.data

import app.vigil.domain.DAY_MS
import app.vigil.domain.EventKind
import app.vigil.domain.Outcome
import app.vigil.domain.Trigger
import app.vigil.domain.VowState
import app.vigil.domain.Vows

/** Everything that changes a vow, in one place. */
class Keeper(private val db: VigilDatabase) {
    private val dao get() = db.dao()

    suspend fun seal(v: Vow): Long = dao.insert(v)

    private suspend fun log(v: Vow, kind: EventKind, text: String = "", day: Int? = null, at: Long = System.currentTimeMillis()) {
        dao.insert(Event(vowId = v.id, at = at, day = day ?: Vows.dayAt(v, at), kind = kind.name, text = text))
    }

    /** A practice day ticked off, or the tick taken back. */
    suspend fun tick(v: Vow, day: Int, done: Boolean) {
        dao.clearDay(v.id, day, listOf(EventKind.DONE.name))
        if (done) log(v, EventKind.DONE, day = day)
    }

    /** The evening examen: a kept day, or a hard one. */
    suspend fun examen(v: Vow, hard: Boolean, note: String) {
        val now = System.currentTimeMillis()
        val day = Vows.dayAt(v, now)
        dao.clearDay(v.id, day, listOf(EventKind.KEPT.name, EventKind.HARD.name))
        log(v, if (hard) EventKind.HARD else EventKind.KEPT, note.trim(), day)
    }

    /** A temptation, held or not. */
    suspend fun urge(v: Vow, strength: Int, trigger: Trigger?, held: Boolean): Long =
        dao.insert(Urge(vowId = v.id, at = System.currentTimeMillis(), strength = strength, trigger = trigger?.name.orEmpty(), held = held))

    /** Held on the ladder of release, short of a full temptation record. */
    suspend fun overcome(v: Vow) {
        log(v, EventKind.OVERCOME)
        dao.update(v.copy(breakAt = null))
    }

    suspend fun ransom(v: Vow) {
        log(v, EventKind.RANSOM, v.ransom)
        dao.update(v.copy(breakAt = null))
    }

    suspend fun letter(v: Vow, text: String) = log(v, EventKind.LETTER, text.trim())

    suspend fun tithe(v: Vow) = log(v, EventKind.TITHE, v.tithe)

    suspend fun settle(e: Event, settled: Boolean) = dao.update(e.copy(settled = settled))

    /** "Sleep on it": breaking is allowed only from tomorrow at the vow's hour. */
    suspend fun delay(v: Vow, until: Long) = dao.update(v.copy(breakAt = until))

    /** The vow is broken (today, or on an earlier [day] confessed now) and [o] is what becomes of it. */
    suspend fun lapse(v: Vow, o: Outcome, day: Int? = null, missed: Boolean = false) {
        val now = System.currentTimeMillis()
        val d = day ?: Vows.dayAt(v, now)
        if (missed) log(v, EventKind.MISSED, day = d)
        val fresh = dao.vow(v.id) ?: v
        val next = when (o) {
            Outcome.INDULGENCE -> fresh.copy(indulgences = fresh.indulgences + 1)
            Outcome.PENANCE -> fresh.copy(extraDays = fresh.extraDays + Vows.penance(fresh.rigorOf))
            Outcome.CRACK -> fresh.copy(cracks = fresh.cracks + 1)
            Outcome.BURN -> fresh.copy(state = VowState.BROKEN.name, endedAt = now, endSeen = false)
        }.copy(breakAt = null)
        dao.update(next)
        log(
            v,
            when (o) {
                Outcome.INDULGENCE -> EventKind.INDULGENCE
                Outcome.PENANCE -> EventKind.PENANCE
                Outcome.CRACK -> EventKind.CRACK
                Outcome.BURN -> EventKind.BURN
            },
            day = d,
        )
    }

    /** An open vow laid down with honour. */
    suspend fun release(v: Vow) {
        val now = System.currentTimeMillis()
        dao.update(v.copy(state = VowState.KEPT.name, endedAt = now, endSeen = false, breakAt = null))
        log(v, EventKind.RELEASED)
    }

    /** Vows whose last day has passed become fulfilled; a stale "sleep on it" is forgotten. */
    suspend fun sweep(now: Long = System.currentTimeMillis()) {
        dao.vows().filter { it.active }.forEach { v ->
            val end = Vows.endAt(v)
            when {
                end != null && now >= end -> {
                    dao.update(v.copy(state = VowState.KEPT.name, endedAt = end, endSeen = false, breakAt = null))
                    log(v, EventKind.END, at = end - 1)
                }
                v.breakAt != null && now >= v.breakAt + DAY_MS -> {
                    dao.update(v.copy(breakAt = null))
                    log(v, EventKind.OVERCOME)
                }
            }
        }
    }

    suspend fun endSeen(v: Vow) = dao.update(v.copy(endSeen = true))

    /** The same vow again, from now: for a hermit after the fire. */
    suspend fun again(v: Vow): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            v.copy(
                id = 0, createdAt = now, startAt = now, extraDays = 0, indulgences = 0, cracks = 0,
                state = VowState.ACTIVE.name, endedAt = null, breakAt = null, endSeen = false, seed = (now % 100_000).toInt(),
            )
        )
    }

    suspend fun update(v: Vow) = dao.update(v)

    suspend fun delete(v: Vow) = dao.delete(v)
}
