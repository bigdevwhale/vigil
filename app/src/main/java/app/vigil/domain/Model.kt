package app.vigil.domain

/** Where a vow belongs; fixes its wax colour by default and its place in the catalog. */
enum class Category { BODY, DIGITAL, SPEECH, MONEY, MIND, HEART, SPIRIT }

/**
 * What a vow asks of you.
 * - [ABSTAIN]: don't do it at all.
 * - [PRACTICE]: do it every day; each day is ticked off.
 * - [LIMIT]: no more than so much a day.
 * - [WINDOW]: not between two hours of the day.
 */
enum class VowType { ABSTAIN, PRACTICE, LIMIT, WINDOW }

/** How hard the vow is to break, and what is left of it when it is broken. */
enum class Rigor { NOVICE, MONK, HERMIT }

/**
 * The ladder of release: what stands between you and breaking a vow, in this order.
 * Every step is optional; a vow keeps its own ladder.
 */
enum class Step { WAIT, VOICE, LETTER, QUESTIONS, RANSOM, WITNESS, TITHE, TOMORROW }

/** What becomes of a vow once it is broken. Which ones are open depends on its [Rigor]. */
enum class Outcome { INDULGENCE, PENANCE, CRACK, BURN }

/** What set a temptation off. */
enum class Trigger { BOREDOM, FATIGUE, STRESS, COMPANY, EVENING, HABIT, SADNESS, JOY }

/**
 * Everything that happens to a vow, stored as [app.vigil.data.Event].
 * DONE: a practice day ticked off. KEPT / HARD: the evening examen. OVERCOME: a temptation held.
 * RANSOM, LETTER, TITHE: from the ladder. INDULGENCE, PENANCE, CRACK, BURN: how a lapse was answered.
 * MISSED: a practice day confessed as not done. END: the vow was fulfilled. RELEASED: an open vow laid down.
 */
enum class EventKind { DONE, KEPT, HARD, OVERCOME, RANSOM, LETTER, TITHE, INDULGENCE, PENANCE, CRACK, BURN, MISSED, END, RELEASED }

enum class VowState { ACTIVE, KEPT, BROKEN }

/** Sealing wax. */
enum class Wax(val argb: Long) {
    VERMILION(0xFFB8322A),
    OXBLOOD(0xFF6E1E1E),
    LAPIS(0xFF2C4A86),
    VERDIGRIS(0xFF3E7A68),
    GOLD(0xFFC0912F),
    UMBER(0xFF6B4A2E),
    VIOLET(0xFF5B3A78),
    SABLE(0xFF2A2522);

    companion object {
        fun of(c: Category): Wax = when (c) {
            Category.BODY -> VERMILION
            Category.DIGITAL -> LAPIS
            Category.SPEECH -> VIOLET
            Category.MONEY -> GOLD
            Category.MIND -> VERDIGRIS
            Category.HEART -> OXBLOOD
            Category.SPIRIT -> SABLE
        }
    }
}

enum class Gender { MALE, FEMALE;
    companion object {
        fun of(id: String?): Gender = entries.firstOrNull { it.name == id } ?: MALE
    }
}

private val genderMark = Regex("\\{([^{}|]*)\\|([^{}|]*)\\}")

/** Resolves "{выстоял|выстояла}" marks: the first form for [Gender.MALE], the second for [Gender.FEMALE]. */
fun gendered(text: String, g: Gender): String =
    if ('{' !in text) text else genderMark.replace(text) { if (g == Gender.MALE) it.groupValues[1] else it.groupValues[2] }

inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
    if (name == null) fallback else runCatching { enumValueOf<E>(name) }.getOrDefault(fallback)

fun steps(csv: String): List<Step> = csv.split(',').mapNotNull { s -> Step.entries.firstOrNull { it.name == s.trim() } }.distinct().sortedBy { it.ordinal }

fun stepsCsv(l: Collection<Step>): String = l.distinct().sortedBy { it.ordinal }.joinToString(",") { it.name }
