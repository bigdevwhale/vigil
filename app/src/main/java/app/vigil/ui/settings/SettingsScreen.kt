package app.vigil.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vigil.BuildConfig
import app.vigil.R
import app.vigil.container
import app.vigil.data.Settings
import app.vigil.domain.Gender
import app.vigil.ui.Go
import app.vigil.ui.Language
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.ConfirmDialog
import app.vigil.ui.components.InfoRow
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.Segmented
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.Stepper
import app.vigil.ui.components.SwitchRow
import app.vigil.ui.components.TimeDialog
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.UCard
import app.vigil.ui.components.UTextField
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType

@Composable
fun SettingsScreen(data: VigilData, go: Go) {
    val c = ctx()
    val container = c.container
    val s = data.settings
    var name by remember { mutableStateOf(s.name) }
    var pick by remember { mutableStateOf<String?>(null) }
    var wipe by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(Language.current()) }
    fun set(change: (Settings) -> Settings) = container.write { settings.update(change) }

    pick?.let { which ->
        TimeDialog(
            when (which) {
                "morning" -> s.morningAt
                "examen" -> s.examenAt
                else -> s.dawnAt
            },
            { pick = null },
        ) { m ->
            set {
                when (which) {
                    "morning" -> it.copy(morningAt = m)
                    "examen" -> it.copy(examenAt = m)
                    else -> it.copy(dawnAt = m)
                }
            }
        }
    }
    if (wipe) ConfirmDialog(
        stringResource(R.string.settings_wipe_title), g(stringResource(R.string.settings_wipe_text)), stringResource(R.string.settings_wipe_ok),
        onConfirm = {
            container.write {
                db.dao().vows().forEach { voice.delete(it.voice) }
                db.dao().clear()
            }
        },
        onDismiss = { wipe = false }, danger = true,
    )

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 30.dp)) {
            SectionTitle(stringResource(R.string.settings_you))
            UTextField(name, { name = it.take(30); set { st -> st.copy(name = name.trim()) } }, placeholder = stringResource(R.string.onb_name_hint))
            Spacer(Modifier.height(10.dp))
            Segmented(listOf(Gender.MALE, Gender.FEMALE), s.gender, { stringResource(if (it == Gender.MALE) R.string.gender_m else R.string.gender_f) }, { gd -> set { it.copy(gender = gd) } })
            Text(stringResource(R.string.gender_hint), style = UType.Small, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(10.dp))
            Segmented(
                listOf(Language.SYSTEM, Language.EN, Language.RU), lang,
                { stringResource(when (it) { Language.EN -> R.string.lang_en; Language.RU -> R.string.lang_ru; else -> R.string.lang_system }) },
                { lang = it; Language.set(it) },
            )

            SectionTitle(stringResource(R.string.settings_rhythm))
            UCard {
                SwitchRow(stringResource(R.string.settings_morning), stringResource(R.string.settings_morning_hint, T.clock(s.morningAt)), s.morning, { on -> set { it.copy(morning = on) } })
                if (s.morning) SoftButton(stringResource(R.string.settings_change_time), { pick = "morning" }, Modifier.fillMaxWidth(), color = U.c.lapis, soft = U.c.lapisSoft)
                HorizontalDivider(color = U.c.line, modifier = Modifier.padding(vertical = 8.dp))
                SwitchRow(stringResource(R.string.settings_examen), stringResource(R.string.settings_examen_hint, T.clock(s.examenAt)), s.examen, { on -> set { it.copy(examen = on) } })
                if (s.examen) SoftButton(stringResource(R.string.settings_change_time), { pick = "examen" }, Modifier.fillMaxWidth(), color = U.c.lapis, soft = U.c.lapisSoft)
            }

            SectionTitle(stringResource(R.string.settings_rule))
            UCard {
                Stepper(stringResource(R.string.settings_candle), stringResource(R.string.minutes_short, s.candleMin), { set { it.copy(candleMin = (it.candleMin - 1).coerceAtLeast(2)) } }, { set { it.copy(candleMin = (it.candleMin + 1).coerceAtMost(30)) } })
                HorizontalDivider(color = U.c.line, modifier = Modifier.padding(vertical = 4.dp))
                InfoRow(Icons.Rounded.WbTwilight, stringResource(R.string.settings_dawn), T.clock(s.dawnAt)) { pick = "dawn" }
                HorizontalDivider(color = U.c.line, modifier = Modifier.padding(vertical = 4.dp))
                SwitchRow(stringResource(R.string.settings_three), g(stringResource(R.string.settings_three_hint)), s.ruleOfThree, { on -> set { it.copy(ruleOfThree = on) } })
            }

            SectionTitle(stringResource(R.string.settings_about))
            UCard {
                InfoRow(Icons.Rounded.Info, stringResource(R.string.settings_version), BuildConfig.VERSION_NAME + if (BuildConfig.DEMO) " · demo" else "") {
                    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bigdevwhale.github.io/stillcraft/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                Text(stringResource(R.string.settings_privacy), style = UType.Muted)
            }
            Spacer(Modifier.height(20.dp))
            SoftButton(stringResource(R.string.settings_wipe), { wipe = true }, Modifier.fillMaxWidth())
        }
    }
}
