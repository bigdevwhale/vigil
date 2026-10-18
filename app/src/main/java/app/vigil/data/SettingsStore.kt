package app.vigil.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vigil.domain.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val name: String = "",
    /** Picks the Russian word forms. */
    val gender: Gender = Gender.MALE,
    /** The morning rule: the vows read out at this time, minutes after midnight. */
    val morning: Boolean = true,
    val morningAt: Int = 8 * 60,
    /** The evening examen: how was the day. */
    val examen: Boolean = true,
    val examenAt: Int = 21 * 60 + 30,
    /** How long the candle burns when temptation comes. */
    val candleMin: Int = 10,
    /** No more than three vows at once. */
    val ruleOfThree: Boolean = true,
    /** "At dawn": when a vow sealed for tomorrow morning begins, minutes after midnight. */
    val dawnAt: Int = 6 * 60,
    /** Things already shown once: "tip:<name>". */
    val seen: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        gender = Gender.of(this[GENDER]),
        morning = this[MORNING] ?: true,
        morningAt = this[MORNING_AT] ?: (8 * 60),
        examen = this[EXAMEN] ?: true,
        examenAt = this[EXAMEN_AT] ?: (21 * 60 + 30),
        candleMin = this[CANDLE] ?: 10,
        ruleOfThree = this[RULE] ?: true,
        dawnAt = this[DAWN] ?: (6 * 60),
        seen = this[SEEN] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[GENDER] = s.gender.name
            it[MORNING] = s.morning
            it[MORNING_AT] = s.morningAt
            it[EXAMEN] = s.examen
            it[EXAMEN_AT] = s.examenAt
            it[CANDLE] = s.candleMin
            it[RULE] = s.ruleOfThree
            it[DAWN] = s.dawnAt
            it[SEEN] = s.seen
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")
        val MORNING = booleanPreferencesKey("morning")
        val MORNING_AT = intPreferencesKey("morning_at")
        val EXAMEN = booleanPreferencesKey("examen")
        val EXAMEN_AT = intPreferencesKey("examen_at")
        val CANDLE = intPreferencesKey("candle_min")
        val RULE = booleanPreferencesKey("rule_of_three")
        val DAWN = intPreferencesKey("dawn_at")
        val SEEN = stringSetPreferencesKey("seen")
    }
}
