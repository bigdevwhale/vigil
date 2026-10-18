package app.vigil.ui.vows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.container
import app.vigil.data.Vow
import app.vigil.domain.Vows
import app.vigil.domain.VowType
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Beads
import app.vigil.ui.components.Illumination
import app.vigil.ui.components.Ornament
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.UCard
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import java.time.LocalTime

@Composable
fun VowsTab(data: VigilData, go: Go) {
    val c = ctx()
    val now = data.now
    val minute = LocalTime.now().let { it.hour * 60 + it.minute }
    // The examen opens in the evening, a little before its notice.
    val evening = minute >= minOf(data.settings.examenAt - 60, 20 * 60) || minute < 3 * 60
    val toExamine = if (evening) data.running.filter { !Vows.examined(it, data.events, now) } else emptyList()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text(T.date(c, now).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(4.dp))
                Text(
                    data.settings.name.takeIf { it.isNotBlank() }?.let { gs(R.string.vows_title_named, it) } ?: stringResource(R.string.vows_title),
                    style = UType.Display,
                )
            }
        }
        if (data.active.isEmpty()) item { Empty(data, go) }

        // A practice day that went by unanswered.
        data.running.forEach { v ->
            Vows.unconfessed(v, data.events, now).firstOrNull()?.let { d -> item(key = "confess${v.id}") { Confess(v, d, now, go) } }
        }
        // The night of "sleep on it" is over.
        data.running.filter { Vows.delayOver(it, now) }.forEach { v -> item(key = "delay${v.id}") { DelayOver(v, go) } }
        // A hard day.
        data.running.forEach { v -> Vows.demon(v, now)?.let { d -> item(key = "demon${v.id}") { Demon(v, d) } } }

        items(data.running, key = { it.id }) { v -> VowCard(v, data, go) }

        if (toExamine.isNotEmpty()) item { Examen(toExamine, go) }

        if (data.waiting.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.vows_waiting)) }
            items(data.waiting, key = { "w" + it.id }) { v -> Waiting(v, now, go) }
        }
        if (data.active.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                if (data.room) SoftButton(stringResource(R.string.vows_another), { go.make() }, Modifier.fillMaxWidth(), color = U.c.lapis, soft = U.c.lapisSoft)
                else Text(g(stringResource(R.string.vows_rule_of_three)), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
        }
    }
}

@Composable
private fun Empty(data: VigilData, go: Go) {
    UCard(padding = PaddingValues(22.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Seal(U.c.rubric, "V", Modifier.size(120.dp), seed = 3)
        }
        Spacer(Modifier.height(14.dp))
        Text(gs(R.string.empty_title), style = UType.Title.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(gs(R.string.empty_text), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(R.string.empty_catalog), { go.make() }, Modifier.fillMaxWidth())
        if (data.ended.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(gs(R.string.empty_kept, data.ended.count { it.stateOf.name == "KEPT" }), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        }
    }
}

/** One vow on the home page: its seal, its day, its rope of beads and the vine round the edge. */
@Composable
fun VowCard(v: Vow, data: VigilData, go: Go) {
    val c = ctx()
    val now = data.now
    val events = data.eventsOf(v)
    val urges = data.urgesOf(v)
    val beads = Vows.beads(v, events, urges, now)
    val golds = urges.count { it.held } + events.count { it.kindOf.name == "OVERCOME" || it.kindOf.name == "RANSOM" }
    val total = Vows.total(v)
    val progress = if (total != null) Vows.progress(v, now) else (Vows.day(v, now) / 40f).coerceAtMost(1f)
    Box(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { go.vow(v.id) }) {
        Illumination(progress, golds, v.seed, Modifier.matchParentSize())
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Seal(Color(v.waxOf.argb), v.initial, Modifier.size(62.dp), cracks = v.cracks, seed = v.seed)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(T.dayOf(c, v, now).uppercase(), style = UType.Eyebrow)
                    Spacer(Modifier.height(2.dp))
                    Text(v.title, style = UType.Name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(g(T.rule(c, v)), style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            Beads(beads)
            if (v.typeOf == VowType.PRACTICE) {
                val today = Vows.day(v, now)
                val done = Vows.doneOn(v, events, today)
                Spacer(Modifier.height(12.dp))
                if (done) {
                    Row(Modifier.fillMaxWidth().clip(U.RSm).clickable { c.container.write { keeper.tick(v, today, false) } }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = U.c.verdigris, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(gs(R.string.practice_done_today), style = UType.Strong.copy(color = U.c.verdigris))
                    }
                } else {
                    SoftButton(gs(R.string.practice_mark), { c.container.write { keeper.tick(v, today, true) } }, Modifier.fillMaxWidth(), color = U.c.verdigris, soft = U.c.verdigrisSoft, icon = Icons.Rounded.Check)
                }
            }
        }
    }
}

@Composable
private fun Confess(v: Vow, day: Int, now: Long, go: Go) {
    val c = ctx()
    val yesterday = Vows.day(v, now) - day == 1
    UCard(border = U.c.verdigris.copy(alpha = 0.5f)) {
        Text(stringResource(R.string.confess_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.verdigris))
        Spacer(Modifier.height(4.dp))
        Text(
            gs(if (yesterday) R.string.confess_yesterday else R.string.confess_day, v.title, day),
            style = UType.Name,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(gs(R.string.confess_yes), { c.container.write { keeper.tick(v, day, true) } }, Modifier.weight(1f), color = U.c.verdigris, soft = U.c.verdigrisSoft)
            SoftButton(gs(R.string.confess_no), { go.release(v.id, confess = day) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DelayOver(v: Vow, go: Go) {
    val c = ctx()
    UCard(border = U.c.rubric.copy(alpha = 0.5f)) {
        Text(stringResource(R.string.delay_eyebrow).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(4.dp))
        Text(gs(R.string.delay_title, v.title), style = UType.Name)
        Spacer(Modifier.height(6.dp))
        Text(gs(R.string.delay_text), style = UType.Sub)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(gs(R.string.delay_hold), { c.container.write { keeper.overcome(v) } }, Modifier.weight(1f), color = U.c.verdigris, soft = U.c.verdigrisSoft)
            SoftButton(gs(R.string.delay_go), { go.release(v.id) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Demon(v: Vow, day: Int) {
    val c = ctx()
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.night).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("☩", color = U.c.flame, style = UType.Title)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.demon_eyebrow, day, v.title).uppercase(), style = UType.Eyebrow.copy(color = U.c.flame), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(g(T.demon(c, day)), style = UType.Body.copy(color = U.c.nightInk))
        }
    }
}

/** The evening examen: how was the day, for each vow not yet answered. */
@Composable
private fun Examen(vows: List<Vow>, go: Go) {
    val c = ctx()
    UCard(border = U.c.lapis.copy(alpha = 0.4f)) {
        Text(stringResource(R.string.examen_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.lapis))
        Spacer(Modifier.height(4.dp))
        Text(gs(R.string.examen_title), style = UType.Name)
        vows.forEach { v ->
            Ornament(Modifier.padding(vertical = 10.dp), color = U.c.line)
            Text(v.title, style = UType.Strong)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton(gs(R.string.examen_kept), { c.container.write { keeper.examen(v, false, "") } }, Modifier.weight(1f), color = U.c.verdigris, soft = U.c.verdigrisSoft)
                SoftButton(stringResource(R.string.examen_hard), { c.container.write { keeper.examen(v, true, "") } }, Modifier.weight(1f), color = U.c.lapis, soft = U.c.lapisSoft)
            }
            Text(
                gs(R.string.examen_broke),
                style = UType.Muted.copy(color = U.c.rubric, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().clip(U.RSm).clickable { go.release(v.id, confess = Vows.day(v, System.currentTimeMillis())) }.padding(10.dp),
            )
        }
    }
}

@Composable
private fun Waiting(v: Vow, now: Long, go: Go) {
    val c = ctx()
    UCard(onClick = { go.vow(v.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Seal(Color(v.waxOf.argb), v.initial, Modifier.size(48.dp), seed = v.seed)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(v.title, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.waiting_starts, T.dateTime(c, v.startAt)), style = UType.Muted)
            }
            Icon(Icons.Rounded.HourglassTop, null, tint = U.c.faint)
            Spacer(Modifier.width(4.dp))
            Text(T.until(c, v.startAt - now), style = UType.Small)
        }
    }
}
