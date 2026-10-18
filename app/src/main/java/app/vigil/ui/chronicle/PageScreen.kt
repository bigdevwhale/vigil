package app.vigil.ui.chronicle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.container
import app.vigil.domain.EventKind
import app.vigil.domain.VowState
import app.vigil.domain.Vows
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Beads
import app.vigil.ui.components.Illumination
import app.vigil.ui.components.Initial
import app.vigil.ui.components.Ornament
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.Stat
import app.vigil.ui.components.TopBar
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import app.vigil.ui.vow.EventLine
import app.vigil.ui.vow.nextName
import app.vigil.ui.vow.nextStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A page of the book of hours: a vow that has ended, fully illuminated if it was kept, in ash if it
 * burned. The first time it opens after a vow is kept, the testament is unsealed.
 */
@Composable
fun PageScreen(id: Long, data: VigilData, go: Go) {
    val v = data.vow(id) ?: return
    val c = ctx()
    val container = c.container
    val now = data.now
    val kept = v.stateOf == VowState.KEPT
    val first = remember { !v.endSeen }
    LaunchedEffect(v.id) { if (!v.endSeen) container.write { keeper.endSeen(v) } }
    var unsealed by remember { mutableStateOf(!first || v.testament.isBlank()) }
    val drop = remember { Animatable(if (first) 2f else 1f) }
    val fade = remember { Animatable(if (first) 0f else 1f) }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(500))
        drop.animateTo(1f, tween(700))
    }
    val events = data.eventsOf(v)
    val urges = data.urgesOf(v)
    val golds = urges.count { it.held } + events.count { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM }
    val days = Vows.keptDays(v, events, urges, now)

    Column(Modifier.fillMaxSize().background(if (kept) U.c.bg else Color(0xFFE4DCCD)).statusBarsPadding()) {
        TopBar(stringResource(R.string.hours_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 30.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Seal(Color(v.waxOf.argb), v.initial, Modifier.size(130.dp).scale(drop.value).alpha(fade.value), cracks = v.cracks, seed = v.seed, ash = !kept)
                Spacer(Modifier.height(10.dp))
                Text(
                    (if (kept) gs(if (v.days <= 0) R.string.page_released else R.string.page_kept) else stringResource(R.string.page_burned)).uppercase(),
                    style = UType.Eyebrow.copy(color = if (kept) U.c.verdigris else U.c.rubric),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg)) {
                if (kept) Illumination(1f, golds, v.seed, Modifier.matchParentSize())
                Column(Modifier.padding(26.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Initial(v.initial, Modifier.size(72.dp), field = if (kept) Color(v.waxOf.argb) else U.c.faint)
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(v.title, style = UType.Title)
                            Text(T.date(c, v.startAt) + " — " + T.date(c, v.endedAt ?: now), style = UType.Small)
                        }
                    }
                    if (v.why.isNotBlank()) {
                        Ornament(Modifier.padding(vertical = 14.dp))
                        Text("«" + v.why + "»", style = UType.Script.copy(color = U.c.ink))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Stat(days.toString(), stringResource(R.string.stat_kept), Modifier.weight(1f))
                        Stat(golds.toString(), stringResource(R.string.stat_held), Modifier.weight(1f), U.c.gold)
                        Stat(v.cracks.toString(), stringResource(R.string.stat_cracks), Modifier.weight(1f), U.c.rubric)
                    }
                    Spacer(Modifier.height(14.dp))
                    Beads(Vows.beads(v, events, urges, now))
                }
            }

            if (kept && v.testament.isNotBlank()) {
                SectionTitle(stringResource(R.string.page_testament))
                if (unsealed) {
                    Box(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.goldSoft).padding(20.dp)) {
                        Text(v.testament, style = UType.Script.copy(color = U.c.ink))
                    }
                } else {
                    Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.gold, U.RMd).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gs(R.string.page_testament_sealed), style = UType.Sub.copy(textAlign = TextAlign.Center))
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(stringResource(R.string.page_testament_open), { unsealed = true }, Modifier.fillMaxWidth(), color = U.c.gold)
                    }
                }
            }

            val letters = events.filter { it.kindOf == EventKind.LETTER || it.kindOf == EventKind.TITHE || it.kindOf == EventKind.CRACK }
            if (letters.isNotEmpty()) {
                SectionTitle(stringResource(R.string.vow_chronicle))
                letters.forEach { EventLine(it) }
            }

            Spacer(Modifier.height(18.dp))
            if (kept) {
                nextStep(v)?.let { n ->
                    PrimaryButton(stringResource(R.string.page_next, nextName(n)), { go.preset(n) }, Modifier.fillMaxWidth(), color = U.c.lapis)
                    Spacer(Modifier.height(10.dp))
                }
                SoftButton(stringResource(R.string.page_again), { go.make(v.preset) }, Modifier.fillMaxWidth())
            } else {
                Text(g(stringResource(R.string.page_burned_text)), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                SoftButton(stringResource(R.string.done_again), {
                    container.write {
                        val nid = keeper.again(v)
                        scope.launch(Dispatchers.Main) { go.home(); go.vow(nid) }
                    }
                }, Modifier.fillMaxWidth())
            }
        }
    }
}
