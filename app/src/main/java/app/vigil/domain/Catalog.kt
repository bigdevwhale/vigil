package app.vigil.domain

import java.time.LocalDate

/** A vow from the catalog: its shape. Its words are in [app.vigil.content.CatalogText], per language. */
data class Preset(
    val id: String,
    val category: Category,
    val type: VowType,
    val days: Int,
    val limit: Int = 0,
    val windowFrom: Int = -1,
    val windowTo: Int = -1,
    /** The next step once this one is kept. */
    val next: String? = null,
)

private fun h(hour: Int, minute: Int = 0) = hour * 60 + minute

object Catalog {
    val all: List<Preset> = listOf(
        // Body
        Preset("no_sugar", Category.BODY, VowType.ABSTAIN, 21, next = "no_snacks"),
        Preset("no_alcohol", Category.BODY, VowType.ABSTAIN, 30, next = "no_alcohol_100"),
        Preset("no_alcohol_100", Category.BODY, VowType.ABSTAIN, 100),
        Preset("no_fastfood", Category.BODY, VowType.ABSTAIN, 30),
        Preset("no_meat", Category.BODY, VowType.ABSTAIN, 30, next = "plant_only"),
        Preset("plant_only", Category.BODY, VowType.ABSTAIN, 21),
        Preset("no_caffeine", Category.BODY, VowType.ABSTAIN, 14),
        Preset("coffee_one", Category.BODY, VowType.LIMIT, 21, limit = 1, next = "no_caffeine"),
        Preset("no_snacks", Category.BODY, VowType.ABSTAIN, 21),
        Preset("no_late_food", Category.BODY, VowType.WINDOW, 21, windowFrom = h(19), windowTo = h(7), next = "fast_16_8"),
        Preset("fast_16_8", Category.BODY, VowType.WINDOW, 21, windowFrom = h(20), windowTo = h(12)),
        Preset("cold_shower", Category.BODY, VowType.PRACTICE, 21, next = "cold_shower_40"),
        Preset("cold_shower_40", Category.BODY, VowType.PRACTICE, 40),
        Preset("steps", Category.BODY, VowType.PRACTICE, 30),
        Preset("early_rise", Category.BODY, VowType.PRACTICE, 21),
        Preset("lights_out", Category.BODY, VowType.PRACTICE, 21),
        Preset("no_lift", Category.BODY, VowType.ABSTAIN, 30),
        Preset("stretch", Category.BODY, VowType.PRACTICE, 21),
        Preset("water", Category.BODY, VowType.PRACTICE, 21),
        Preset("no_smoking", Category.BODY, VowType.ABSTAIN, 40, next = "no_alcohol"),
        // Digital
        Preset("no_social", Category.DIGITAL, VowType.ABSTAIN, 30, next = "no_shorts"),
        Preset("no_shorts", Category.DIGITAL, VowType.ABSTAIN, 30),
        Preset("no_youtube", Category.DIGITAL, VowType.ABSTAIN, 14),
        Preset("no_news", Category.DIGITAL, VowType.ABSTAIN, 21),
        Preset("no_games", Category.DIGITAL, VowType.ABSTAIN, 30),
        Preset("no_series", Category.DIGITAL, VowType.ABSTAIN, 30),
        Preset("no_porn", Category.DIGITAL, VowType.ABSTAIN, 90),
        Preset("phone_bedroom", Category.DIGITAL, VowType.WINDOW, 21, windowFrom = h(22), windowTo = h(7)),
        Preset("phone_morning", Category.DIGITAL, VowType.WINDOW, 21, windowFrom = h(6), windowTo = h(8)),
        Preset("grayscale", Category.DIGITAL, VowType.ABSTAIN, 14),
        Preset("screen_hour", Category.DIGITAL, VowType.LIMIT, 30, limit = 60),
        // Speech
        Preset("no_swearing", Category.SPEECH, VowType.ABSTAIN, 21),
        Preset("no_complaints", Category.SPEECH, VowType.ABSTAIN, 21),
        Preset("no_gossip", Category.SPEECH, VowType.ABSTAIN, 30),
        Preset("no_criticism", Category.SPEECH, VowType.ABSTAIN, 21),
        Preset("no_must", Category.SPEECH, VowType.ABSTAIN, 7),
        Preset("silent_morning", Category.SPEECH, VowType.WINDOW, 7, windowFrom = h(6), windowTo = h(9)),
        Preset("no_sarcasm", Category.SPEECH, VowType.ABSTAIN, 14),
        // Money
        Preset("no_spend", Category.MONEY, VowType.ABSTAIN, 30),
        Preset("no_delivery", Category.MONEY, VowType.ABSTAIN, 30),
        Preset("no_marketplace", Category.MONEY, VowType.ABSTAIN, 30),
        Preset("no_taxi", Category.MONEY, VowType.ABSTAIN, 21),
        Preset("cash_only", Category.MONEY, VowType.ABSTAIN, 30),
        Preset("no_coffee_out", Category.MONEY, VowType.ABSTAIN, 21),
        // Mind
        Preset("read", Category.MIND, VowType.PRACTICE, 30, next = "read_100"),
        Preset("read_100", Category.MIND, VowType.PRACTICE, 100),
        Preset("no_headphones", Category.MIND, VowType.ABSTAIN, 7),
        Preset("one_thing", Category.MIND, VowType.PRACTICE, 14),
        Preset("journal", Category.MIND, VowType.PRACTICE, 30),
        Preset("meditate", Category.MIND, VowType.PRACTICE, 21, next = "meditate_108"),
        Preset("meditate_108", Category.MIND, VowType.PRACTICE, 108),
        Preset("walk_bare", Category.MIND, VowType.PRACTICE, 14),
        Preset("words", Category.MIND, VowType.PRACTICE, 30),
        // Heart
        Preset("good_deed", Category.HEART, VowType.PRACTICE, 21),
        Preset("call_kin", Category.HEART, VowType.PRACTICE, 30),
        Preset("gratitude", Category.HEART, VowType.PRACTICE, 21),
        Preset("no_arguing", Category.HEART, VowType.ABSTAIN, 30),
        Preset("no_interrupting", Category.HEART, VowType.ABSTAIN, 14),
        Preset("compliment", Category.HEART, VowType.PRACTICE, 14),
        // Spirit and the stern ones
        Preset("lent", Category.SPIRIT, VowType.ABSTAIN, 40),
        Preset("nativity_fast", Category.SPIRIT, VowType.ABSTAIN, 40),
        Preset("silence_day", Category.SPIRIT, VowType.ABSTAIN, 1),
        Preset("no_soft_bed", Category.SPIRIT, VowType.ABSTAIN, 7),
        Preset("no_hot_water", Category.SPIRIT, VowType.ABSTAIN, 14),
        Preset("one_meal", Category.SPIRIT, VowType.LIMIT, 7, limit = 1),
        Preset("no_salt_sugar", Category.SPIRIT, VowType.ABSTAIN, 14),
        Preset("vigil", Category.SPIRIT, VowType.PRACTICE, 40),
        Preset("dawn", Category.SPIRIT, VowType.PRACTICE, 21),
        Preset("prayer_rope", Category.SPIRIT, VowType.PRACTICE, 108),
    )

    fun get(id: String): Preset? = all.firstOrNull { it.id == id }

    fun of(c: Category): List<Preset> = all.filter { it.category == c }

    /** Lengths with a tradition behind them. */
    val lengths = listOf(3, 7, 21, 40, 49, 108)
}

/**
 * A season some vows belong to: offered in the catalog from two weeks before it starts until it ends.
 * The Nativity Fast and the Orthodox Lent are offered when the app speaks Russian, the Western Lent otherwise.
 */
enum class Season(val preset: String) {
    DRY_JANUARY("no_alcohol"),
    GREAT_LENT("lent"),
    SOBER_OCTOBER("no_alcohol"),
    NO_SPEND_NOVEMBER("no_spend"),
    NATIVITY_FAST("nativity_fast");

    /** The season's start and end (exclusive) in [year]. */
    fun span(year: Int, orthodox: Boolean): Pair<LocalDate, LocalDate> = when (this) {
        DRY_JANUARY -> LocalDate.of(year, 1, 1) to LocalDate.of(year, 2, 1)
        SOBER_OCTOBER -> LocalDate.of(year, 10, 1) to LocalDate.of(year, 11, 1)
        NO_SPEND_NOVEMBER -> LocalDate.of(year, 11, 1) to LocalDate.of(year, 12, 1)
        // Clean Monday to Easter (48 days) in the East; Ash Wednesday to Easter (46 days) in the West.
        GREAT_LENT -> if (orthodox) Easter.orthodox(year).let { it.minusDays(48) to it } else Easter.western(year).let { it.minusDays(46) to it }
        // 28 November to Christmas on 7 January.
        NATIVITY_FAST -> LocalDate.of(year, 11, 28) to LocalDate.of(year + 1, 1, 7)
    }

    companion object {
        /** Seasons near [today]: their start and end, soonest first. */
        fun near(today: LocalDate, russian: Boolean): List<Triple<Season, LocalDate, LocalDate>> =
            entries.filter { russian || it != NATIVITY_FAST }.flatMap { s ->
                listOf(today.year - 1, today.year, today.year + 1).map { y -> s.span(y, russian).let { (a, b) -> Triple(s, a, b) } }
            }.filter { (_, a, b) -> !today.isBefore(a.minusDays(14)) && today.isBefore(b) }
                .sortedBy { it.second }
    }
}

/** Easter Sunday by the old reckonings. */
object Easter {
    /** Western (Gregorian) Easter, the anonymous algorithm. */
    fun western(y: Int): LocalDate {
        val a = y % 19
        val b = y / 100
        val c = y % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(y, month, day)
    }

    /** Orthodox Easter: Meeus' Julian algorithm, moved to the Gregorian calendar (+13 days, 1900–2099). */
    fun orthodox(y: Int): LocalDate {
        val a = y % 4
        val b = y % 7
        val c = y % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        return LocalDate.of(y, month, day).plusDays(13)
    }
}
