package app.vigil.data

import android.content.Context
import app.vigil.AppContainer
import app.vigil.content.Content
import app.vigil.domain.Catalog
import app.vigil.domain.DAY_MS
import app.vigil.domain.EventKind
import app.vigil.domain.Gender
import app.vigil.domain.Rigor
import app.vigil.domain.Trigger
import app.vigil.domain.VowState
import app.vigil.domain.Vows
import app.vigil.domain.Wax
import app.vigil.domain.gendered
import app.vigil.domain.stepsCsv
import app.vigil.domain.Step
import app.vigil.localized
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/** A made-up summer and autumn of vows for the demo build (`-Pdemo`), in the app's language. */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.vows().isNotEmpty()) return
        val ru = context.localized().resources.configuration.locales[0].language == "ru"
        val loc = context.localized().resources.configuration.locales[0]
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        fun daysAgo(d: Double) = now - (d * DAY_MS).toLong()
        fun at(daysBack: Int, hour: Int, minute: Int = 0) =
            LocalDateTime.now(zone).toLocalDate().minusDays(daysBack.toLong()).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        fun name(id: String) = gendered(Content.text(id, loc)?.name ?: id, Gender.MALE)

        fun vow(
            id: String, start: Long, rigor: Rigor, why: String, days: Int = Catalog.get(id)?.days ?: 21, seed: Int,
            state: VowState = VowState.ACTIVE, ended: Long? = null, testament: String = "", extra: Int = 0, cracks: Int = 0, indulgences: Int = 0,
            ladder: List<Step> = Vows.defaultLadder(rigor), ransom: String = "", tithe: String = "",
        ): Vow {
            val p = Catalog.get(id)!!
            val n = name(id)
            return Vow(
                preset = id, title = n, why = why, category = p.category.name, type = p.type.name, days = days,
                limit = p.limit, unit = Content.text(id, loc)?.unit.orEmpty(), windowFrom = p.windowFrom, windowTo = p.windowTo,
                createdAt = start - 3_600_000, startAt = start, rigor = rigor.name, ladder = stepsCsv(ladder),
                waitMin = Vows.defaultWait(rigor), letterMin = Vows.defaultLetter(rigor), ransom = ransom, tithe = tithe,
                testament = testament, wax = Wax.of(p.category).name, initial = n.take(1).uppercase(),
                extraDays = extra, cracks = cracks, indulgences = indulgences, state = state.name, endedAt = ended, endSeen = true, seed = seed,
            )
        }

        val sugar = dao.insert(
            vow(
                "no_sugar", daysAgo(11.4), Rigor.MONK,
                if (ru) "Хочу снова чувствовать вкус еды и не тянуться к сладкому после каждого звонка." else "I want to taste food again, not reach for sweets after every call.",
                days = 40, seed = 4107, extra = 3, ransom = if (ru) "30 приседаний" else "30 squats",
                ladder = listOf(Step.WAIT, Step.LETTER, Step.QUESTIONS, Step.RANSOM, Step.TITHE), tithe = if (ru) "500 ₽ в приют для животных" else "€10 to the animal shelter",
            )
        )
        val shower = dao.insert(
            vow(
                "cold_shower", daysAgo(4.6), Rigor.NOVICE,
                if (ru) "Начинать день с маленькой победы." else "Start each day with a small victory.", seed = 2231, indulgences = 1,
            )
        )
        val social = dao.insert(
            vow(
                "no_social", daysAgo(2.3), Rigor.HERMIT,
                if (ru) "Вернуть себе вечера и голову." else "Win back my evenings, and my head.", seed = 9120,
                testament = if (ru) "Ты справился. Помнишь, как было пусто в первые дни? Это была свобода." else "You made it. Remember how empty the first days felt? That was freedom.",
            )
        )
        val alcohol = dao.insert(
            vow(
                "no_alcohol", at(58, 7), Rigor.MONK, if (ru) "Посмотреть, каким я бываю без бокала." else "To see who I am without the glass.",
                days = 30, seed = 771, state = VowState.KEPT, ended = at(28, 7),
                testament = if (ru) "Тридцать дней. Ты спал лучше, чем за весь год. Не забывай этого." else "Thirty days. You slept better than all year. Don't forget it.",
            )
        )
        val silence = dao.insert(
            vow(
                "silent_morning", at(96, 6), Rigor.NOVICE, if (ru) "Слышать утро." else "To hear the morning.",
                days = 7, seed = 3302, state = VowState.KEPT, ended = at(89, 6),
            )
        )
        val read = dao.insert(
            vow(
                "read", at(41, 21), Rigor.MONK, if (ru) "Дочитать наконец «Братьев Карамазовых»." else "Finally finish The Brothers Karamazov.",
                days = 30, seed = 5519, state = VowState.KEPT, ended = at(11, 21), cracks = 1,
            )
        )
        val coffee = dao.insert(
            vow(
                "no_caffeine", at(130, 8), Rigor.HERMIT, if (ru) "Проверить, кто я без кофе." else "To find out who I am without coffee.",
                days = 14, seed = 1888, state = VowState.BROKEN, ended = at(126, 15),
            )
        )

        val all = dao.vows().associateBy { it.id }
        suspend fun ev(id: Long, day: Int, kind: EventKind, text: String = "", hour: Int = 21) {
            val v = all.getValue(id)
            dao.insert(Event(vowId = id, at = v.startAt + (day - 1) * DAY_MS + hour * 1_800_000L, day = day, kind = kind.name, text = text))
        }
        // The sugar vow: kept with one lapse atoned for, a letter and a pledge.
        for (d in 1..11) if (d != 6) ev(sugar, d, if (d % 4 == 0) EventKind.HARD else EventKind.KEPT)
        ev(
            sugar, 6, EventKind.LETTER,
            if (ru) "Был день рождения Лены, и я сказал себе, что кусок торта ничего не значит. Значит. Я знаю, что завтра буду злиться на себя."
            else "It was Lena's birthday and I told myself a slice of cake meant nothing. It does. I know I'll be cross with myself tomorrow.",
        )
        ev(sugar, 6, EventKind.TITHE, if (ru) "500 ₽ в приют для животных" else "€10 to the animal shelter")
        ev(sugar, 6, EventKind.PENANCE)
        ev(sugar, 9, EventKind.RANSOM, if (ru) "30 приседаний" else "30 squats")
        // The cold shower: done, but for day three, forgiven.
        for (d in listOf(1, 2, 4)) ev(shower, d, EventKind.DONE, hour = 7)
        ev(shower, 3, EventKind.MISSED, hour = 7)
        ev(shower, 3, EventKind.INDULGENCE, hour = 7)
        // Social media: two days held so far.
        ev(social, 1, EventKind.KEPT)
        ev(social, 2, EventKind.HARD)
        ev(alcohol, 30, EventKind.END)
        ev(silence, 7, EventKind.END)
        for (d in 1..30) if (d != 17) ev(read, d, EventKind.DONE)
        ev(read, 17, EventKind.MISSED)
        ev(read, 17, EventKind.CRACK)
        ev(read, 30, EventKind.END)
        ev(coffee, 5, EventKind.LETTER, if (ru) "Дедлайн, ночь, глаза закрываются. Сдаюсь." else "A deadline, the night, my eyes closing. I give in.")
        ev(coffee, 5, EventKind.BURN)

        // Temptations: mostly evenings at the end of the week, mostly held.
        val rnd = Random(42)
        val triggers = listOf(Trigger.EVENING, Trigger.EVENING, Trigger.BOREDOM, Trigger.FATIGUE, Trigger.COMPANY, Trigger.STRESS, Trigger.HABIT)
        listOf(sugar to 11, social to 2, alcohol to 30, read to 30).forEach { (id, span) ->
            val v = all.getValue(id)
            repeat(if (id == social) 3 else 10) {
                val day = rnd.nextInt(span)
                val evening = rnd.nextFloat() < 0.65f
                val hour = if (evening) 19 + rnd.nextInt(4) else 8 + rnd.nextInt(10)
                var t = v.startAt + day * DAY_MS
                t = t - (t % DAY_MS) + hour * 3_600_000L
                if (t < now - 60_000) dao.insert(Urge(vowId = id, at = t, strength = 2 + rnd.nextInt(4), trigger = triggers[rnd.nextInt(triggers.size)].name, held = rnd.nextFloat() < 0.85f))
            }
        }

        c.settings.update {
            it.copy(
                onboarded = true,
                name = if (ru) "Саша" else "Alex",
                // Three vows are running; leave room to try sealing a fourth.
                ruleOfThree = false,
                seen = it.seen + listOf("end:$alcohol", "end:$silence", "end:$read"),
            )
        }
    }
}
