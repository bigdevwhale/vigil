package app.vigil.ui.chronicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.container
import app.vigil.data.Vow
import app.vigil.domain.EventKind
import app.vigil.domain.UrgeMap
import app.vigil.domain.VowState
import app.vigil.domain.Vows
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Bar
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.Stat
import app.vigil.ui.components.UCard
import app.vigil.ui.components.heat
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import java.time.ZoneId
import java.time.format.TextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChronicleTab(data: VigilData, go: Go) {
    val c = ctx()
    val now = data.now
    val kept = data.ended.filter { it.stateOf == VowState.KEPT }
    val burned = data.ended.filter { it.stateOf == VowState.BROKEN }
    val days = data.vows.sumOf { Vows.keptDays(it, data.eventsOf(it), data.urgesOf(it), now) }
    val held = data.urges.count { it.held } + data.events.count { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM }
    val debts = data.events.filter { it.kindOf == EventKind.TITHE }
    val letters = data.events.filter { it.kindOf == EventKind.LETTER && it.text.isNotBlank() }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.chronicle_eyebrow).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.chronicle_title), style = UType.Display)
            }
        }
        item {
            UCard {
                Row(Modifier.fillMaxWidth()) {
                    Stat(kept.size.toString(), stringResource(R.string.stat_vows_kept), Modifier.weight(1f), U.c.verdigris)
                    Stat(days.toString(), stringResource(R.string.stat_days), Modifier.weight(1f))
                    Stat(held.toString(), stringResource(R.string.stat_held), Modifier.weight(1f), U.c.gold)
                }
            }
        }

        item { SectionTitle(stringResource(R.string.reliquary)) }
        item {
            if (kept.isEmpty()) Text(gs(R.string.reliquary_empty), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                kept.forEach { v -> Relic(v) { go.page(v.id) } }
            }
        }

        if (data.urges.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.map_title)) }
            item { TemptationMap(data) }
        }

        if (debts.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.debts_title)) }
            items(debts, key = { "d" + it.id }) { e ->
                val v = data.vow(e.vowId)
                Row(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.surface).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        e.settled, { s -> c.container.write { keeper.settle(e, s) } },
                        colors = CheckboxDefaults.colors(checkedColor = U.c.verdigris, uncheckedColor = U.c.gold),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(e.text, style = UType.Strong.copy(textDecoration = if (e.settled) TextDecoration.LineThrough else null))
                        Text((v?.title ?: "") + " · " + T.date(c, e.at), style = UType.Small)
                    }
                }
            }
        }

        if (letters.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.letters_title)) }
            items(letters.take(10), key = { "l" + it.id }) { e ->
                UCard {
                    Text((data.vow(e.vowId)?.title ?: "") + " · " + T.date(c, e.at), style = UType.Small)
                    Spacer(Modifier.height(4.dp))
                    Text(e.text, style = UType.Script.copy(color = U.c.ink), maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (data.ended.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.hours_title)) }
            items(data.ended, key = { "h" + it.id }) { v -> HoursRow(v, data) { go.page(v.id) } }
        }
        if (burned.isEmpty() && kept.isEmpty() && data.urges.isEmpty()) {
            item { Text(gs(R.string.chronicle_empty), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(20.dp)) }
        }
    }
}

@Composable
private fun Relic(v: Vow, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).clip(U.RSm).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Seal(Color(v.waxOf.argb), v.initial, Modifier.size(64.dp), cracks = v.cracks, seed = v.seed)
        Spacer(Modifier.height(4.dp))
        Text(v.title, style = UType.Small.copy(color = U.c.ink, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HoursRow(v: Vow, data: VigilData, onClick: () -> Unit) {
    val c = ctx()
    val kept = v.stateOf == VowState.KEPT
    UCard(onClick = onClick, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Seal(Color(v.waxOf.argb), v.initial, Modifier.size(44.dp), cracks = v.cracks, seed = v.seed, ash = !kept)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(v.title, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (kept) gs(R.string.vow_kept) else stringResource(R.string.vow_burned)) + " · " + T.date(c, v.endedAt ?: v.startAt) +
                        " · " + T.days(c, Vows.keptDays(v, data.eventsOf(v), data.urgesOf(v), data.now)),
                    style = UType.Small.copy(color = if (kept) U.c.verdigris else U.c.rubric),
                )
            }
        }
    }
}

/** When temptation comes: a heat grid of weekdays by four-hour slots, the usual triggers and how many were held. */
@Composable
private fun TemptationMap(data: VigilData) {
    val c = ctx()
    val map = UrgeMap(data.urges, ZoneId.systemDefault())
    val loc = c.resources.configuration.locales[0]
    val slots = stringArrayResource(R.array.slots)
    UCard {
        map.peak?.let { (day, slot) ->
            Text(
                stringResource(R.string.map_peak, map.peakShare(), day.getDisplayName(TextStyle.FULL_STANDALONE, loc).let { if (loc.language == "ru") it.lowercase(loc) else it }, slots[slot]),
                style = UType.Body,
            )
            Spacer(Modifier.height(12.dp))
        }
        val max = map.max.coerceAtLeast(1)
        val base = U.c.surface2
        val hot = U.c.rubric
        val labels = (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, loc) }
        Row {
            Column(Modifier.padding(end = 6.dp)) {
                labels.forEach { Box(Modifier.height(22.dp), contentAlignment = Alignment.CenterStart) { Text(it, style = UType.Small) } }
            }
            Canvas(Modifier.weight(1f).height(22.dp * 7)) {
                val cw = size.width / UrgeMap.SLOTS
                val ch = size.height / 7
                for (d in 0 until 7) for (s in 0 until UrgeMap.SLOTS) {
                    val n = map.grid[d][s]
                    drawRoundRect(
                        heat(base, hot, if (n == 0) 0f else 0.2f + 0.8f * n / max),
                        Offset(s * cw + 2f, d * ch + 2f), Size(cw - 4f, ch - 4f), CornerRadius(6f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 34.dp, top = 4.dp)) {
            listOf("0", "4", "8", "12", "16", "20").forEach { Text(it, style = UType.Small, modifier = Modifier.weight(1f)) }
        }
        if (map.triggers.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            val top = map.triggers.first().second.coerceAtLeast(1)
            map.triggers.take(4).forEach { (t, n) ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(T.trigger(c, t), style = UType.Body, modifier = Modifier.width(110.dp))
                    Bar(n.toFloat() / top, Modifier.weight(1f), color = U.c.lapis)
                    Text(n.toString(), style = UType.Small, modifier = Modifier.padding(start = 8.dp).width(24.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(g(stringResource(R.string.map_held, map.held, map.total)), style = UType.Muted)
    }
}
