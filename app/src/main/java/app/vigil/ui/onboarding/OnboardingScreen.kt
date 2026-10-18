package app.vigil.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.container
import app.vigil.domain.Bead
import app.vigil.domain.Gender
import app.vigil.domain.gendered
import app.vigil.ui.components.Beads
import app.vigil.ui.components.Candle
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.Seal
import app.vigil.ui.components.Segmented
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.UTextField
import app.vigil.ui.ctx
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType

/** Three leaves on what a vow is here, then a name and how to be spoken to. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = ctx()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(Gender.MALE) }
    val pages = 4
    Column(Modifier.fillMaxSize().background(if (page == 2) U.c.night else U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val night = page == 2
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (page) {
                    0 -> Seal(U.c.rubric, "V", Modifier.size(170.dp), seed = 11)
                    1 -> Seal(U.c.lapis, "V", Modifier.size(170.dp), cracks = 2, seed = 5)
                    2 -> Candle(0.25f, Modifier.size(150.dp, 220.dp))
                    else -> Beads(List(21) { i -> when { i < 9 -> if (i % 4 == 2) Bead.GOLD else Bead.KEPT; i == 9 -> Bead.TODAY; else -> Bead.FUTURE } }, Modifier.padding(horizontal = 12.dp))
                }
                Spacer(Modifier.height(28.dp))
                val title = when (page) { 0 -> R.string.onb_1_title; 1 -> R.string.onb_2_title; 2 -> R.string.onb_3_title; else -> R.string.onb_4_title }
                val text = when (page) { 0 -> R.string.onb_1_text; 1 -> R.string.onb_2_text; 2 -> R.string.onb_3_text; else -> R.string.onb_4_text }
                Text(gendered(stringResource(title), gender), style = UType.Display.copy(textAlign = TextAlign.Center, color = if (night) U.c.nightInk else U.c.ink))
                Spacer(Modifier.height(12.dp))
                Text(gendered(stringResource(text), gender), style = UType.Sub.copy(textAlign = TextAlign.Center, color = if (night) U.c.nightMuted else U.c.muted))
                if (page == 3) {
                    Spacer(Modifier.height(20.dp))
                    UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.onb_name_hint))
                    Spacer(Modifier.height(10.dp))
                    Segmented(listOf(Gender.MALE, Gender.FEMALE), gender, { stringResource(if (it == Gender.MALE) R.string.gender_m else R.string.gender_f) }, { gender = it })
                    Text(stringResource(R.string.gender_hint), style = UType.Small, modifier = Modifier.padding(4.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages) { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (i == page) 9.dp else 6.dp).clip(CircleShape).background(if (i == page) U.c.rubric else U.c.faint))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) {
                SoftButton(stringResource(R.string.back), { page-- })
                Spacer(Modifier.width(10.dp))
            }
            PrimaryButton(
                stringResource(if (page == pages - 1) R.string.onb_start else R.string.next),
                {
                    if (page < pages - 1) page++
                    else {
                        c.container.write { settings.update { it.copy(onboarded = true, name = name.trim(), gender = gender) } }
                        onDone()
                    }
                },
                Modifier.weight(1f),
                color = if (page == 2) U.c.gold else U.c.rubric,
            )
        }
    }
}
