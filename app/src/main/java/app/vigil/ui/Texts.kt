package app.vigil.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.vigil.R
import app.vigil.content.CatalogText
import app.vigil.content.Content
import app.vigil.data.Vow
import app.vigil.domain.Category
import app.vigil.domain.Gender
import app.vigil.domain.Outcome
import app.vigil.domain.Preset
import app.vigil.domain.Rigor
import app.vigil.domain.Season
import app.vigil.domain.Step
import app.vigil.domain.Trigger
import app.vigil.domain.VowType
import app.vigil.domain.Vows
import app.vigil.domain.gendered
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Who the app speaks to: the gender picks the Russian word forms. */
val LocalGender = staticCompositionLocalOf { Gender.MALE }

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun ctx(): Context = LocalContext.current

/** A string resource with its "{male|female}" forms resolved. */
@Composable
fun gs(@StringRes id: Int, vararg args: Any): String = gendered(stringResource(id, *args), LocalGender.current)

@Composable
fun gp(@PluralsRes id: Int, count: Int, vararg args: Any): String = gendered(pluralStringResource(id, count, *args), LocalGender.current)

@Composable
fun g(text: String): String = gendered(text, LocalGender.current)

@Composable
fun presetText(p: Preset): CatalogText = Content.text(p, locale()).let { it.copy(name = g(it.name), line = g(it.line), life = g(it.life), hard = g(it.hard)) }

/** Names and words, in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    fun category(c: Context, k: Category): String = c.getString(
        when (k) {
            Category.BODY -> R.string.cat_body
            Category.DIGITAL -> R.string.cat_digital
            Category.SPEECH -> R.string.cat_speech
            Category.MONEY -> R.string.cat_money
            Category.MIND -> R.string.cat_mind
            Category.HEART -> R.string.cat_heart
            Category.SPIRIT -> R.string.cat_spirit
        }
    )

    fun type(c: Context, t: VowType): String = c.getString(
        when (t) {
            VowType.ABSTAIN -> R.string.type_abstain
            VowType.PRACTICE -> R.string.type_practice
            VowType.LIMIT -> R.string.type_limit
            VowType.WINDOW -> R.string.type_window
        }
    )

    fun typeHint(c: Context, t: VowType): String = c.getString(
        when (t) {
            VowType.ABSTAIN -> R.string.type_abstain_hint
            VowType.PRACTICE -> R.string.type_practice_hint
            VowType.LIMIT -> R.string.type_limit_hint
            VowType.WINDOW -> R.string.type_window_hint
        }
    )

    fun rigor(c: Context, r: Rigor): String = c.getString(
        when (r) {
            Rigor.NOVICE -> R.string.rigor_novice
            Rigor.MONK -> R.string.rigor_monk
            Rigor.HERMIT -> R.string.rigor_hermit
        }
    )

    fun rigorHint(c: Context, r: Rigor): String = c.getString(
        when (r) {
            Rigor.NOVICE -> R.string.rigor_novice_hint
            Rigor.MONK -> R.string.rigor_monk_hint
            Rigor.HERMIT -> R.string.rigor_hermit_hint
        }
    )

    fun step(c: Context, s: Step): String = c.getString(
        when (s) {
            Step.WAIT -> R.string.step_wait
            Step.VOICE -> R.string.step_voice
            Step.LETTER -> R.string.step_letter
            Step.QUESTIONS -> R.string.step_questions
            Step.RANSOM -> R.string.step_ransom
            Step.WITNESS -> R.string.step_witness
            Step.TITHE -> R.string.step_tithe
            Step.TOMORROW -> R.string.step_tomorrow
        }
    )

    fun stepHint(c: Context, s: Step): String = c.getString(
        when (s) {
            Step.WAIT -> R.string.step_wait_hint
            Step.VOICE -> R.string.step_voice_hint
            Step.LETTER -> R.string.step_letter_hint
            Step.QUESTIONS -> R.string.step_questions_hint
            Step.RANSOM -> R.string.step_ransom_hint
            Step.WITNESS -> R.string.step_witness_hint
            Step.TITHE -> R.string.step_tithe_hint
            Step.TOMORROW -> R.string.step_tomorrow_hint
        }
    )

    fun outcome(c: Context, o: Outcome): String = c.getString(
        when (o) {
            Outcome.INDULGENCE -> R.string.outcome_indulgence
            Outcome.PENANCE -> R.string.outcome_penance
            Outcome.CRACK -> R.string.outcome_crack
            Outcome.BURN -> R.string.outcome_burn
        }
    )

    fun outcomeHint(c: Context, o: Outcome, v: Vow, now: Long): String = when (o) {
        Outcome.INDULGENCE -> c.resources.getQuantityString(R.plurals.outcome_indulgence_hint, Vows.indulgencesLeft(v, now), Vows.indulgencesLeft(v, now))
        Outcome.PENANCE -> Vows.penance(v.rigorOf).let { c.resources.getQuantityString(R.plurals.outcome_penance_hint, it, it) }
        Outcome.CRACK -> c.getString(R.string.outcome_crack_hint)
        Outcome.BURN -> c.getString(if (v.rigorOf == Rigor.HERMIT) R.string.outcome_burn_hermit_hint else R.string.outcome_burn_hint)
    }

    fun trigger(c: Context, t: Trigger): String = c.getString(
        when (t) {
            Trigger.BOREDOM -> R.string.trigger_boredom
            Trigger.FATIGUE -> R.string.trigger_fatigue
            Trigger.STRESS -> R.string.trigger_stress
            Trigger.COMPANY -> R.string.trigger_company
            Trigger.EVENING -> R.string.trigger_evening
            Trigger.HABIT -> R.string.trigger_habit
            Trigger.SADNESS -> R.string.trigger_sadness
            Trigger.JOY -> R.string.trigger_joy
        }
    )

    fun season(c: Context, s: Season): String = c.getString(
        when (s) {
            Season.DRY_JANUARY -> R.string.season_dry_january
            Season.GREAT_LENT -> R.string.season_lent
            Season.SOBER_OCTOBER -> R.string.season_sober_october
            Season.NO_SPEND_NOVEMBER -> R.string.season_no_spend
            Season.NATIVITY_FAST -> R.string.season_nativity
        }
    )

    /** "day 12 of 40", or "day 12" for a vow with no end. */
    fun dayOf(c: Context, v: Vow, now: Long): String {
        val d = Vows.current(v, now).coerceAtLeast(1)
        return Vows.total(v)?.let { c.getString(R.string.day_of, d, it) } ?: c.getString(R.string.day_n, d)
    }

    fun days(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.days, n, n)

    /** "21 days", or "no end". */
    fun length(c: Context, days: Int): String = if (days <= 0) c.getString(R.string.length_open) else days(c, days)

    /** A warning for one of the hard days. */
    fun demon(c: Context, day: Int): String = c.getString(
        when (day) {
            3 -> R.string.demon_3
            7 -> R.string.demon_7
            14 -> R.string.demon_14
            21 -> R.string.demon_21
            30 -> R.string.demon_30
            else -> R.string.demon_40
        }
    )

    fun clock(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

    fun window(v: Vow): String = clock(v.windowFrom) + "–" + clock(v.windowTo)

    /** What the vow asks, in a line: "not between 22:00 and 7:00", "no more than 1 cup a day". */
    fun rule(c: Context, v: Vow): String = when (v.typeOf) {
        VowType.ABSTAIN -> c.getString(R.string.rule_abstain)
        VowType.PRACTICE -> c.getString(R.string.rule_practice)
        VowType.LIMIT -> c.getString(R.string.rule_limit, v.limit, v.unit.ifBlank { c.getString(R.string.unit_default) })
        VowType.WINDOW -> c.getString(R.string.rule_window, clock(v.windowFrom), clock(v.windowTo))
    }

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    fun date(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(c.resources.configuration.locales[0])).replace(Regex("\\s*г\\.$"), "")

    fun date(c: Context, ms: Long): String = date(c, local(ms).toLocalDate())

    fun dateTime(c: Context, ms: Long): String {
        val z = local(ms)
        return date(c, z.toLocalDate()) + ", " + clock(z.hour * 60 + z.minute)
    }

    /** "in 3 h 20 min" / "in 2 days". */
    fun until(c: Context, ms: Long): String {
        val m = (ms / 60_000).coerceAtLeast(1)
        return when {
            m < 60 -> c.getString(R.string.in_min, m)
            m < 48 * 60 -> c.getString(R.string.in_hours, m / 60, m % 60)
            else -> c.resources.getQuantityString(R.plurals.in_days, (m / 1440).toInt(), (m / 1440).toInt())
        }
    }
}
