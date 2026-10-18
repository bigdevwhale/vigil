package app.vigil.ui.vow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.window.Dialog
import app.vigil.R
import app.vigil.container
import app.vigil.content.Content
import app.vigil.data.Event
import app.vigil.data.Vow
import app.vigil.domain.Catalog
import app.vigil.domain.EventKind
import app.vigil.domain.Rigor
import app.vigil.domain.VowState
import app.vigil.domain.VowType
import app.vigil.domain.Vows
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Bar
import app.vigil.ui.components.Beads
import app.vigil.ui.components.Candle
import app.vigil.ui.components.ConfirmDialog
import app.vigil.ui.components.Illumination
import app.vigil.ui.components.Initial
import app.vigil.ui.components.Ornament
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.Stat
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.UCard
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.locale
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType

/** One vow as a page: its words, its rope of days, what stands between you and breaking it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VowScreen(id: Long, data: VigilData, go: Go) {
    val v = data.vow(id) ?: return
    val c = ctx()
    val container = c.container
    val now = data.now
    val events = data.eventsOf(v)
    val urges = data.urgesOf(v)
    var delete by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf(false) }
    val started = Vows.started(v, now)

    if (delete) ConfirmDialog(
        stringResource(R.string.vow_delete_title), g(stringResource(R.string.vow_delete_text)), stringResource(R.string.delete),
        onConfirm = {
            container.write {
                if (db.dao().vows().none { it.id != v.id && it.voice == v.voice }) voice.delete(v.voice)
                keeper.delete(v)
            }
            go.back()
        },
        onDismiss = { delete = false }, danger = true,
    )
    if (release) ConfirmDialog(
        gs(R.string.vow_release_title), g(stringResource(R.string.vow_release_text)), stringResource(R.string.vow_release_ok),
        onConfirm = { container.write { keeper.release(v) } },
        onDismiss = { release = false },
    )
    // Just sealed: the seal comes down onto the page.
    if (now - v.createdAt < 20_000 && v.active) SealedStamp(v)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(T.category(c, v.categoryOf), go::back) {
            IconButton(onClick = { delete = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 32.dp)) {
            Page(v, data)
            Spacer(Modifier.height(14.dp))

            when {
                !v.active -> {
                    PrimaryButton(stringResource(R.string.vow_open_page), { go.page(v.id) }, Modifier.fillMaxWidth(), color = U.c.lapis)
                }
                !started -> {
                    UCard {
                        Text(stringResource(R.string.waiting_starts, T.dateTime(c, v.startAt)), style = UType.Strong)
                        Text(T.until(c, v.startAt - now), style = UType.Muted)
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.vow_start_now), { container.write { keeper.update(v.copy(startAt = System.currentTimeMillis())) } }, Modifier.fillMaxWidth())
                    }
                }
                else -> Actions(v, data, go) { release = true }
            }

            SectionTitle(stringResource(R.string.vow_rope))
            Beads(Vows.beads(v, events, urges, now), maxBead = 26f)
            Spacer(Modifier.height(10.dp))
            Legend()
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(Vows.keptDays(v, events, urges, now).toString(), stringResource(R.string.stat_kept), Modifier.weight(1f))
                Stat((urges.count { it.held } + events.count { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM }).toString(), stringResource(R.string.stat_held), Modifier.weight(1f), U.c.gold)
                Stat(v.cracks.toString(), stringResource(R.string.stat_cracks), Modifier.weight(1f), U.c.rubric)
            }
            if (v.extraDays > 0 || (v.rigorOf == Rigor.NOVICE && v.active)) {
                Spacer(Modifier.height(10.dp))
                val parts = listOfNotNull(
                    if (v.extraDays > 0) stringResource(R.string.vow_penance_added, T.days(c, v.extraDays)) else null,
                    if (v.rigorOf == Rigor.NOVICE && v.active) c.resources.getQuantityString(R.plurals.outcome_indulgence_hint, Vows.indulgencesLeft(v, now), Vows.indulgencesLeft(v, now)) else null,
                )
                Text(parts.joinToString(" · "), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
            }

            SectionTitle(stringResource(R.string.vow_ladder))
            Text(T.rigor(c, v.rigorOf) + " · " + g(T.rigorHint(c, v.rigorOf)), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            if (v.ladderOf.isEmpty()) Text(g(stringResource(R.string.vow_ladder_empty)), style = UType.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                v.ladderOf.forEachIndexed { i, s ->
                    Text(
                        "${i + 1}. " + T.step(c, s),
                        style = UType.Small.copy(color = U.c.ink),
                        modifier = Modifier.clip(U.RSm).background(U.c.surface2).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (v.testament.isNotBlank() && v.active) {
                SectionTitle(stringResource(R.string.vow_testament))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = U.c.gold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        Vows.endAt(v)?.let { g(stringResource(R.string.vow_testament_locked, T.date(c, it))) } ?: g(stringResource(R.string.vow_testament_locked_open)),
                        style = UType.Muted,
                    )
                }
            }

            val shown = events.filter { it.kindOf in setOf(EventKind.LETTER, EventKind.TITHE, EventKind.HARD, EventKind.KEPT, EventKind.RANSOM, EventKind.CRACK, EventKind.PENANCE, EventKind.INDULGENCE) }.take(12)
            if (shown.isNotEmpty()) {
                SectionTitle(stringResource(R.string.vow_chronicle))
                shown.forEach { EventLine(it) }
            }
        }
    }
}

/** The parchment: illuminated initial, title, rule, reason, and how far along. */
@Composable
private fun Page(v: Vow, data: VigilData) {
    val c = ctx()
    val now = data.now
    val voice = c.container.voice
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { voice.stopPlaying() } }
    val urges = data.urgesOf(v)
    val events = data.eventsOf(v)
    val golds = urges.count { it.held } + events.count { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM }
    val total = Vows.total(v)
    val progress = when {
        v.stateOf == VowState.KEPT -> 1f
        total != null -> Vows.progress(v, now)
        else -> (Vows.day(v, now) / 40f).coerceAtMost(1f)
    }
    Box(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg)) {
        Illumination(progress, golds, v.seed, Modifier.matchParentSize())
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Initial(v.initial, Modifier.size(64.dp), field = if (v.stateOf == VowState.BROKEN) U.c.faint else Color(v.waxOf.argb))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (v.stateOf) {
                            VowState.ACTIVE -> if (Vows.started(v, now)) T.dayOf(c, v, now) else stringResource(R.string.vow_waiting)
                            VowState.KEPT -> gs(R.string.vow_kept)
                            VowState.BROKEN -> stringResource(R.string.vow_burned)
                        }.uppercase(),
                        style = UType.Eyebrow,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(v.title, style = UType.Title)
                    Text(g(T.rule(c, v)), style = UType.Muted)
                }
                Seal(Color(v.waxOf.argb), v.initial, Modifier.size(58.dp), cracks = v.cracks, seed = v.seed, ash = v.stateOf == VowState.BROKEN)
            }
            if (v.why.isNotBlank() || voice.exists(v.voice)) {
                Ornament(Modifier.padding(vertical = 14.dp))
                if (v.why.isNotBlank()) Text("«" + v.why + "»", style = UType.Script.copy(color = U.c.ink))
                if (voice.exists(v.voice)) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.clip(U.RSm).clickable {
                            if (playing) { voice.stopPlaying(); playing = false } else { playing = true; voice.play(v.voice) { playing = false } }
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = U.c.lapis)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vow_voice), style = UType.Strong.copy(color = U.c.lapis))
                    }
                }
            }
            if (total != null) {
                Spacer(Modifier.height(16.dp))
                Bar(progress)
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(T.date(c, v.startAt), style = UType.Small, modifier = Modifier.weight(1f))
                    Text(Vows.endAt(v)?.let { T.date(c, it) }.orEmpty(), style = UType.Small)
                }
            }
        }
    }
}

@Composable
private fun Actions(v: Vow, data: VigilData, go: Go, onRelease: () -> Unit) {
    val c = ctx()
    val now = data.now
    val day = Vows.day(v, now)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (v.typeOf == VowType.PRACTICE) {
            val done = Vows.doneOn(v, data.events, day)
            SoftButton(
                gs(if (done) R.string.practice_done_today else R.string.practice_mark),
                { c.container.write { keeper.tick(v, day, !done) } },
                Modifier.fillMaxWidth(), color = U.c.verdigris, soft = U.c.verdigrisSoft, icon = Icons.Rounded.Check,
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(U.RMd).background(U.c.night).clickable { go.tempt(v.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Candle(0.3f, Modifier.size(40.dp, 52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tempt_button), style = UType.Strong.copy(color = U.c.nightInk))
                Text(stringResource(R.string.vow_tempt_hint, data.settings.candleMin), style = UType.Muted.copy(color = U.c.nightMuted))
            }
        }
        if (Vows.delayed(v, now)) {
            Text(stringResource(R.string.vow_delayed, T.dateTime(c, v.breakAt ?: now)), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        } else {
            SoftButton(gs(R.string.vow_want_break), { go.release(v.id) }, Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                gs(R.string.vow_confess),
                style = UType.Muted.copy(color = U.c.rubric, textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f).clip(U.RSm).clickable { go.release(v.id, confess = day) }.padding(8.dp),
            )
            if (Vows.total(v) == null) {
                Text(
                    stringResource(R.string.vow_release),
                    style = UType.Muted.copy(color = U.c.lapis, textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f).clip(U.RSm).clickable(onClick = onRelease).padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LegendDot(Color(0xFF5A3B26), stringResource(R.string.legend_kept))
        LegendDot(U.c.gold, stringResource(R.string.legend_gold))
        LegendDot(Color(0xFFC9772E), stringResource(R.string.legend_mended))
        LegendDot(U.c.rubric, stringResource(R.string.legend_crack))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = UType.Small)
    }
}

@Composable
fun EventLine(e: Event) {
    val (mark, color) = when (e.kindOf) {
        EventKind.LETTER -> "✉" to U.c.ink
        EventKind.TITHE -> "⚖" to U.c.gold
        EventKind.HARD -> "☁" to U.c.lapis
        EventKind.KEPT -> "✓" to U.c.verdigris
        EventKind.RANSOM -> "⚜" to U.c.gold
        EventKind.CRACK -> "✗" to U.c.rubric
        EventKind.PENANCE -> "+" to Color(0xFFC9772E)
        EventKind.INDULGENCE -> "☙" to Color(0xFFC9772E)
        else -> "·" to U.c.faint
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(mark, style = UType.Strong.copy(color = color), modifier = Modifier.width(26.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.event_day, e.day) + " · " + eventName(e), style = UType.Small)
            if (e.text.isNotBlank()) Text(e.text, style = UType.Body)
        }
    }
}

@Composable
fun eventName(e: Event): String = g(
    stringResource(
        when (e.kindOf) {
            EventKind.DONE -> R.string.ev_done
            EventKind.KEPT -> R.string.ev_kept
            EventKind.HARD -> R.string.ev_hard
            EventKind.OVERCOME -> R.string.ev_overcome
            EventKind.RANSOM -> R.string.ev_ransom
            EventKind.LETTER -> R.string.ev_letter
            EventKind.TITHE -> R.string.ev_tithe
            EventKind.INDULGENCE -> R.string.ev_indulgence
            EventKind.PENANCE -> R.string.ev_penance
            EventKind.CRACK -> R.string.ev_crack
            EventKind.BURN -> R.string.ev_burn
            EventKind.MISSED -> R.string.ev_missed
            EventKind.END -> R.string.ev_end
            EventKind.RELEASED -> R.string.ev_released
        }
    )
)

/** The seal coming down onto a fresh vow. */
@Composable
private fun SealedStamp(v: Vow) {
    var open by remember { mutableStateOf(true) }
    if (!open) return
    val scale = remember { Animatable(2.4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Dialog(onDismissRequest = { open = false }) {
        Column(
            Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Seal(Color(v.waxOf.argb), v.initial, Modifier.size(150.dp).scale(scale.value).alpha(alpha.value), seed = v.seed)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.sealed_eyebrow).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(4.dp))
            Text(v.title, style = UType.Title.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            val c = ctx()
            Text(
                g(if (Vows.started(v, System.currentTimeMillis())) stringResource(R.string.sealed_text_now) else stringResource(R.string.sealed_text_later, T.dateTime(c, v.startAt))),
                style = UType.Sub.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(gs(R.string.sealed_ok), { open = false }, Modifier.fillMaxWidth())
        }
    }
}

/** The catalog's next step after a kept vow, if there is one. */
fun nextStep(v: Vow): String? = Catalog.get(v.preset)?.next

@Composable
fun nextName(id: String): String = g(Content.text(id, locale())?.name ?: id)
