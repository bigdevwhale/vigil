package app.vigil.ui.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.domain.Catalog
import app.vigil.domain.Category
import app.vigil.domain.DAY_MS
import app.vigil.domain.Preset
import app.vigil.domain.Season
import app.vigil.domain.Wax
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Chip
import app.vigil.ui.components.Initial
import app.vigil.ui.components.Ornament
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.UCard
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.locale
import app.vigil.ui.presetText
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import app.vigil.ui.vow.nextName
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun CatalogTab(data: VigilData, go: Go) {
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    val russian = locale().language == "ru"
    val today = LocalDate.now(ZoneId.systemDefault())
    val seasons = Season.near(today, russian)
    val list = category?.let { Catalog.of(it) } ?: Catalog.all
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.catalog_eyebrow).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.catalog_title), style = UType.Display)
            }
        }
        item { SoftButton(stringResource(R.string.catalog_own), { go.make() }, Modifier.fillMaxWidth(), color = U.c.lapis, soft = U.c.lapisSoft, icon = Icons.Rounded.Edit) }
        seasons.forEach { (s, start, end) -> item(key = s.name + start) { SeasonCard(s, start, end, today, go) } }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(R.string.catalog_all), category == null, { category = null }) }
                items(Category.entries) { k -> Chip(T.category(ctx(), k), category == k, { category = k }) }
            }
        }
        items(list, key = { it.id }) { p -> PresetRow(p, data) { go.preset(p.id) } }
    }
}

@Composable
private fun SeasonCard(s: Season, start: LocalDate, end: LocalDate, today: LocalDate, go: Go) {
    val c = ctx()
    val begun = !today.isBefore(start)
    val from = if (begun) today else start
    val days = ChronoUnit.DAYS.between(from, end).toInt()
    Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.night).padding(18.dp)) {
        Text(
            stringResource(if (begun) R.string.season_now else R.string.season_soon, T.date(c, start), T.date(c, end.minusDays(1))).uppercase(),
            style = UType.Eyebrow.copy(color = U.c.flame),
        )
        Spacer(Modifier.height(4.dp))
        Text(T.season(c, s), style = UType.Title.copy(color = U.c.nightInk))
        Spacer(Modifier.height(4.dp))
        Text(
            g(stringResource(if (begun) R.string.season_join else R.string.season_prepare, T.days(c, days))),
            style = UType.Muted.copy(color = U.c.nightMuted),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            stringResource(R.string.season_take),
            { go.make(s.preset, start = if (begun) 0L else start.toEpochDay() * DAY_MS, days = days, title = T.season(c, s)) },
            Modifier.fillMaxWidth(), color = U.c.gold,
        )
    }
}

@Composable
private fun PresetRow(p: Preset, data: VigilData, onClick: () -> Unit) {
    val c = ctx()
    val t = presetText(p)
    val taken = data.active.any { it.preset == p.id }
    UCard(onClick = onClick, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Initial(t.name, Modifier.size(46.dp), field = Color(Wax.of(p.category).argb))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.line, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    (if (taken) stringResource(R.string.catalog_taken) + " · " else "") + T.days(c, p.days) + " · " + T.type(c, p.type),
                    style = UType.Small.copy(color = if (taken) U.c.rubric else U.c.faint),
                )
            }
        }
    }
}

/** A catalog vow's page: its life, its hard days, and the next step. */
@Composable
fun PresetScreen(id: String, data: VigilData, go: Go) {
    val p = Catalog.get(id) ?: return
    val c = ctx()
    val t = presetText(p)
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(T.category(c, p.category), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Initial(t.name, Modifier.size(84.dp), field = Color(Wax.of(p.category).argb))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(T.days(c, p.days).uppercase() + " · " + T.type(c, p.type).uppercase(), style = UType.Eyebrow)
                    Text(t.name, style = UType.Title)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(t.line, style = UType.Script)
            Ornament(Modifier.padding(vertical = 16.dp))
            SectionTitle(stringResource(R.string.preset_life))
            Text(t.life, style = UType.Body)
            SectionTitle(stringResource(R.string.preset_hard))
            Text(t.hard, style = UType.Body)
            p.next?.let { n ->
                SectionTitle(stringResource(R.string.preset_next))
                UCard(onClick = { go.preset(n) }) {
                    Text(nextName(n), style = UType.Name)
                    Text(g(stringResource(R.string.preset_next_hint)), style = UType.Muted)
                }
            }
            if (data.active.any { it.preset == p.id }) {
                Spacer(Modifier.height(12.dp))
                Text(gs(R.string.preset_taken), style = UType.Muted.copy(color = U.c.rubric))
            }
        }
        Box(Modifier.background(U.c.surface).navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(stringResource(R.string.preset_take), { go.make(p.id) }, Modifier.fillMaxWidth(), enabled = data.room)
        }
    }
}
