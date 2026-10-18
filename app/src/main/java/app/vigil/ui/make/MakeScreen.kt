package app.vigil.ui.make

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.vigil.R
import app.vigil.container
import app.vigil.content.Content
import app.vigil.data.Vow
import app.vigil.domain.Catalog
import app.vigil.domain.Category
import app.vigil.domain.Rigor
import app.vigil.domain.Step
import app.vigil.domain.VowType
import app.vigil.domain.Vows
import app.vigil.domain.Wax
import app.vigil.domain.gendered
import app.vigil.domain.stepsCsv
import app.vigil.ui.Go
import app.vigil.ui.LocalGender
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Chip
import app.vigil.ui.components.FutureDateTimeDialog
import app.vigil.ui.components.HoldButton
import app.vigil.ui.components.Initial
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SectionTitle
import app.vigil.ui.components.Segmented
import app.vigil.ui.components.SelectCard
import app.vigil.ui.components.Stepper
import app.vigil.ui.components.SwitchRow
import app.vigil.ui.components.TimeDialog
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.UCard
import app.vigil.ui.components.UTextField
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.locale
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** When a vow begins. */
private enum class StartMode { NOW, DAWN, DATE, SUPPER }

/** The rule of a new vow, one section after another, sealed by holding the wax. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MakeScreen(presetId: String, seasonStart: Long, seasonDays: Int, givenTitle: String, data: VigilData, go: Go) {
    val c = ctx()
    val container = c.container
    val gender = LocalGender.current
    val loc = locale()
    val preset = remember(presetId) { Catalog.get(presetId) }
    val text = remember(presetId, loc) { preset?.let { Content.text(it, loc) } }
    val zone = ZoneId.systemDefault()

    var title by rememberSaveable { mutableStateOf(givenTitle.ifBlank { text?.name?.let { gendered(it, gender) }.orEmpty() }) }
    var category by rememberSaveable { mutableStateOf(preset?.category ?: Category.SPIRIT) }
    var type by rememberSaveable { mutableStateOf(preset?.type ?: VowType.ABSTAIN) }
    var days by rememberSaveable { mutableIntStateOf(if (seasonDays > 0) seasonDays else preset?.days ?: 21) }
    var custom by rememberSaveable { mutableStateOf(days !in Catalog.lengths && days > 0) }
    var limit by rememberSaveable { mutableIntStateOf(preset?.limit?.takeIf { it > 0 } ?: 1) }
    var unit by rememberSaveable { mutableStateOf(text?.unit.orEmpty()) }
    var windowFrom by rememberSaveable { mutableIntStateOf(preset?.windowFrom?.takeIf { it >= 0 } ?: (22 * 60)) }
    var windowTo by rememberSaveable { mutableIntStateOf(preset?.windowTo?.takeIf { it >= 0 } ?: (7 * 60)) }
    var startMode by rememberSaveable { mutableStateOf(if (seasonStart > System.currentTimeMillis()) StartMode.DATE else StartMode.NOW) }
    var startAt by rememberSaveable {
        mutableLongStateOf(
            if (seasonStart > 0) LocalDate.ofEpochDay(seasonStart / 86_400_000L).atTime(data.settings.dawnAt / 60, data.settings.dawnAt % 60).atZone(zone).toInstant().toEpochMilli()
            else System.currentTimeMillis() + 86_400_000L
        )
    }
    var supperAt by rememberSaveable { mutableIntStateOf(21 * 60) }
    var why by rememberSaveable { mutableStateOf("") }
    var voice by rememberSaveable { mutableStateOf("") }
    var rigor by rememberSaveable { mutableStateOf(Rigor.MONK) }
    var ladder by rememberSaveable { mutableStateOf(stepsCsv(Vows.defaultLadder(Rigor.MONK))) }
    var waitMin by rememberSaveable { mutableIntStateOf(Vows.defaultWait(Rigor.MONK)) }
    var letterMin by rememberSaveable { mutableIntStateOf(Vows.defaultLetter(Rigor.MONK)) }
    var ransom by rememberSaveable { mutableStateOf("") }
    var witness by rememberSaveable { mutableStateOf("") }
    var witnessPhone by rememberSaveable { mutableStateOf("") }
    var tithe by rememberSaveable { mutableStateOf("") }
    var delayAt by rememberSaveable { mutableIntStateOf(9 * 60) }
    var testament by rememberSaveable { mutableStateOf("") }
    var wax by rememberSaveable { mutableStateOf(Wax.of(preset?.category ?: Category.SPIRIT)) }
    var initial by rememberSaveable { mutableStateOf("") }
    var sealed by remember { mutableStateOf(false) }

    var pickTime by remember { mutableStateOf<String?>(null) }
    var pickDate by remember { mutableStateOf(false) }

    val steps = app.vigil.domain.steps(ladder)
    fun toggle(s: Step, on: Boolean) {
        ladder = stepsCsv(if (on) steps + s else steps - s)
    }

    // A recording that was never sealed into a vow is thrown away.
    DisposableEffect(Unit) {
        onDispose {
            container.voice.stopPlaying()
            container.voice.stop()
            if (!sealed && voice.isNotBlank()) container.voice.delete(voice)
        }
    }

    pickTime?.let { which ->
        val m = when (which) {
            "from" -> windowFrom
            "to" -> windowTo
            "supper" -> supperAt
            else -> delayAt
        }
        TimeDialog(m, { pickTime = null }) { v ->
            when (which) {
                "from" -> windowFrom = v
                "to" -> windowTo = v
                "supper" -> supperAt = v
                else -> delayAt = v
            }
        }
    }
    if (pickDate) FutureDateTimeDialog(startAt, { pickDate = false }) { startAt = it }

    fun start(): Long {
        val now = LocalDateTime.now(zone)
        return when (startMode) {
            StartMode.NOW -> System.currentTimeMillis()
            StartMode.DAWN -> now.toLocalDate().plusDays(1).atTime(data.settings.dawnAt / 60, data.settings.dawnAt % 60).atZone(zone).toInstant().toEpochMilli()
            StartMode.DATE -> maxOf(startAt, System.currentTimeMillis())
            StartMode.SUPPER -> {
                var t = now.toLocalDate().atTime(supperAt / 60, supperAt % 60)
                if (!t.isAfter(now)) t = t.plusDays(1)
                t.atZone(zone).toInstant().toEpochMilli()
            }
        }
    }

    val letter = initial.ifBlank { title.trim().take(1) }.ifBlank { "V" }
    val canSeal = title.isNotBlank() && data.room

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (preset == null) R.string.make_title_own else R.string.make_title), go::back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            // The vow itself.
            SectionTitle(stringResource(R.string.make_what))
            UTextField(title, { title = it.take(60) }, placeholder = stringResource(R.string.make_title_hint))
            text?.let {
                Spacer(Modifier.height(8.dp))
                Text(g(it.line), style = UType.Script)
            }
            Spacer(Modifier.height(12.dp))
            Segmented(VowType.entries, type, { T.type(c, it) }, { type = it })
            Spacer(Modifier.height(6.dp))
            Text(T.typeHint(c, type), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            when (type) {
                VowType.LIMIT -> {
                    Spacer(Modifier.height(8.dp))
                    Stepper(stringResource(R.string.make_limit), limit.toString(), { limit = (limit - 1).coerceAtLeast(1) }, { limit = (limit + if (limit >= 20) 5 else 1).coerceAtMost(600) })
                    UTextField(unit, { unit = it.take(20) }, placeholder = stringResource(R.string.make_unit_hint))
                }
                VowType.WINDOW -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeBox(stringResource(R.string.make_window_from), T.clock(windowFrom), Modifier.weight(1f)) { pickTime = "from" }
                        TimeBox(stringResource(R.string.make_window_to), T.clock(windowTo), Modifier.weight(1f)) { pickTime = "to" }
                    }
                }
                else -> {}
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { k -> Chip(T.category(c, k), category == k, { category = k; wax = Wax.of(k) }) }
            }

            // How long.
            SectionTitle(stringResource(R.string.make_length))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Catalog.lengths.forEach { n -> Chip(T.days(c, n), !custom && days == n, { days = n; custom = false }) }
                Chip(stringResource(R.string.make_length_custom), custom, { custom = true; if (days <= 0) days = 30 })
                Chip(stringResource(R.string.length_open), !custom && days == 0, { days = 0; custom = false })
            }
            if (custom) Stepper(stringResource(R.string.make_days), days.toString(), { days = (days - if (days > 30) 5 else 1).coerceAtLeast(1) }, { days = (days + if (days >= 30) 5 else 1).coerceAtMost(365) })
            lengthNote(days)?.let {
                Spacer(Modifier.height(6.dp))
                Text(g(stringResource(it)), style = UType.Script, modifier = Modifier.padding(horizontal = 4.dp))
            }

            // When it begins.
            SectionTitle(stringResource(R.string.make_start))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectCard(startMode == StartMode.NOW, { startMode = StartMode.NOW }, U.c.rubric) {
                    StartLabel(stringResource(R.string.start_now), stringResource(R.string.start_now_hint))
                }
                SelectCard(startMode == StartMode.DAWN, { startMode = StartMode.DAWN }, U.c.rubric) {
                    StartLabel(stringResource(R.string.start_dawn), stringResource(R.string.start_dawn_hint, T.clock(data.settings.dawnAt)))
                }
                SelectCard(startMode == StartMode.SUPPER, { startMode = StartMode.SUPPER }, U.c.rubric) {
                    StartLabel(stringResource(R.string.start_supper), stringResource(R.string.start_supper_hint, T.clock(supperAt)))
                }
                SelectCard(startMode == StartMode.DATE, { startMode = StartMode.DATE }, U.c.rubric) {
                    StartLabel(stringResource(R.string.start_date), T.dateTime(c, startAt))
                }
            }
            if (startMode == StartMode.SUPPER) TimeLink(stringResource(R.string.start_supper_change)) { pickTime = "supper" }
            if (startMode == StartMode.DATE) TimeLink(stringResource(R.string.start_date_change)) { pickDate = true }

            // Why.
            SectionTitle(stringResource(R.string.make_why))
            UTextField(why, { why = it.take(600) }, placeholder = g(stringResource(R.string.make_why_hint)), minLines = 3)
            Spacer(Modifier.height(10.dp))
            VoiceRow(voice) { voice = it }

            // Rigor.
            SectionTitle(stringResource(R.string.make_rigor))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Rigor.entries.forEach { r ->
                    SelectCard(rigor == r, {
                        rigor = r
                        ladder = stepsCsv(Vows.defaultLadder(r).filter { it != Step.VOICE || voice.isNotBlank() || r != Rigor.NOVICE })
                        waitMin = Vows.defaultWait(r)
                        letterMin = Vows.defaultLetter(r)
                    }, U.c.rubric) {
                        Column {
                            Text(T.rigor(c, r), style = UType.Name)
                            Text(g(T.rigorHint(c, r)), style = UType.Muted)
                        }
                    }
                }
            }

            // The ladder of release.
            SectionTitle(stringResource(R.string.make_ladder))
            Text(g(stringResource(R.string.make_ladder_hint)), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(8.dp))
            UCard {
                Step.entries.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = U.c.line)
                    SwitchRow(T.step(c, s), g(T.stepHint(c, s)), s in steps, { toggle(s, it) })
                    if (s in steps) {
                        when (s) {
                            Step.WAIT -> Stepper(stringResource(R.string.make_wait), stringResource(R.string.minutes_short, waitMin), { waitMin = (waitMin - 5).coerceAtLeast(5) }, { waitMin = (waitMin + 5).coerceAtMost(120) })
                            Step.LETTER -> Stepper(stringResource(R.string.make_letter), letterMin.toString(), { letterMin = (letterMin - 20).coerceAtLeast(40) }, { letterMin = (letterMin + 20).coerceAtMost(1000) })
                            Step.VOICE -> if (voice.isBlank()) Text(g(stringResource(R.string.make_voice_needed)), style = UType.Small.copy(color = U.c.rubric), modifier = Modifier.padding(bottom = 8.dp))
                            Step.RANSOM -> Field(ransom, { ransom = it.take(80) }, g(stringResource(R.string.make_ransom_hint)))
                            Step.WITNESS -> {
                                Field(witness, { witness = it.take(40) }, stringResource(R.string.make_witness_hint))
                                UTextField(witnessPhone, { witnessPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(20) }, placeholder = stringResource(R.string.make_witness_phone), number = true)
                                Spacer(Modifier.height(8.dp))
                            }
                            Step.TITHE -> Field(tithe, { tithe = it.take(80) }, stringResource(R.string.make_tithe_hint))
                            Step.TOMORROW -> TimeLink(stringResource(R.string.make_tomorrow_at, T.clock(delayAt))) { pickTime = "delay" }
                            Step.QUESTIONS -> {}
                        }
                    }
                }
            }

            // The testament.
            SectionTitle(stringResource(R.string.make_testament))
            Text(g(stringResource(R.string.make_testament_hint)), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(8.dp))
            UTextField(testament, { testament = it.take(2000) }, placeholder = g(stringResource(R.string.make_testament_placeholder)), minLines = 3)

            // The seal.
            SectionTitle(stringResource(R.string.make_seal))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Seal(Color(wax.argb), letter, Modifier.size(96.dp), seed = title.hashCode())
                Spacer(Modifier.width(14.dp))
                Initial(letter, Modifier.size(64.dp))
                Spacer(Modifier.width(14.dp))
                UTextField(initial, { initial = it.trim().take(1) }, Modifier.width(90.dp), placeholder = letter)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Wax.entries.forEach { w ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(w.argb))
                            .border(if (w == wax) 3.dp else 1.dp, if (w == wax) U.c.ink else U.c.line, CircleShape)
                            .clickable { wax = w },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (!data.room) {
                Text(g(stringResource(R.string.vows_rule_of_three)), style = UType.Muted.copy(color = U.c.rubric, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
        }
        Column(Modifier.background(U.c.surface).navigationBarsPadding().padding(16.dp)) {
            HoldButton(stringResource(R.string.make_hold), enabled = canSeal, modifier = Modifier.fillMaxWidth(), onDone = {
                sealed = true
                val now = System.currentTimeMillis()
                val v = Vow(
                    preset = preset?.id.orEmpty(),
                    title = title.trim(),
                    why = why.trim(),
                    category = category.name,
                    type = type.name,
                    days = days,
                    limit = if (type == VowType.LIMIT) limit else 0,
                    unit = if (type == VowType.LIMIT) unit.trim() else "",
                    windowFrom = if (type == VowType.WINDOW) windowFrom else -1,
                    windowTo = if (type == VowType.WINDOW) windowTo else -1,
                    createdAt = now,
                    startAt = start(),
                    rigor = rigor.name,
                    ladder = stepsCsv(steps.filter { it != Step.VOICE || voice.isNotBlank() }),
                    waitMin = waitMin,
                    letterMin = letterMin,
                    ransom = ransom.trim(),
                    witness = witness.trim(),
                    witnessPhone = witnessPhone.trim(),
                    tithe = tithe.trim(),
                    delayAt = delayAt,
                    voice = voice,
                    testament = testament.trim(),
                    wax = wax.name,
                    initial = letter.uppercase(),
                    seed = (now % 100_000).toInt(),
                )
                container.write {
                    val id = keeper.seal(v)
                    scope.launch(Dispatchers.Main) { go.home(); go.vow(id) }
                }
            })
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.make_hold_hint), style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun lengthNote(days: Int): Int? = when (days) {
    0 -> R.string.length_note_open
    3 -> R.string.length_note_3
    7 -> R.string.length_note_7
    21 -> R.string.length_note_21
    40 -> R.string.length_note_40
    49 -> R.string.length_note_49
    108 -> R.string.length_note_108
    else -> null
}

@Composable
private fun StartLabel(title: String, sub: String) {
    Column {
        Text(title, style = UType.Strong)
        Text(sub, style = UType.Muted)
    }
}

@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(U.RSm).background(U.c.surface).border(1.dp, U.c.line, U.RSm).clickable(onClick = onClick).padding(12.dp)) {
        Text(label, style = UType.Muted)
        Text(value, style = UType.Number.copy(fontSize = 24.sp))
    }
}

@Composable
private fun TimeLink(text: String, onClick: () -> Unit) {
    Row(Modifier.clip(U.RSm).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Schedule, null, tint = U.c.lapis, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = UType.Strong.copy(color = U.c.lapis))
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String) {
    UTextField(value, onChange, placeholder = hint)
    Spacer(Modifier.height(8.dp))
}

/** Record the reason in your own voice: tap to start, tap to stop; then listen, redo or remove. */
@Composable
private fun VoiceRow(voice: String, onVoice: (String) -> Unit) {
    val c = ctx()
    val v = c.container.voice
    var recording by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var denied by remember { mutableStateOf(false) }
    LaunchedEffect(recording) {
        seconds = 0
        while (recording != null) {
            kotlinx.coroutines.delay(1000)
            seconds++
            if (seconds >= 90) {
                val name = recording
                recording = null
                if (v.stop() && name != null) onVoice(name)
            }
        }
    }
    fun begin() {
        if (voice.isNotBlank()) v.delete(voice)
        onVoice("")
        recording = v.start()
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) begin() else denied = true }
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(if (recording != null) U.c.rubricSoft else U.c.surface).border(1.dp, U.c.line, U.RMd).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {
            val name = recording
            if (name != null) {
                recording = null
                if (v.stop()) onVoice(name) else v.delete(name)
            } else if (ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) begin()
            else ask.launch(Manifest.permission.RECORD_AUDIO)
        }) {
            Icon(if (recording != null) Icons.Rounded.Stop else Icons.Rounded.Mic, stringResource(R.string.voice_record), tint = U.c.rubric)
        }
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    recording != null -> stringResource(R.string.voice_recording, seconds)
                    voice.isNotBlank() -> stringResource(R.string.voice_ready)
                    else -> stringResource(R.string.voice_record)
                },
                style = UType.Strong,
            )
            Text(
                g(stringResource(if (denied) R.string.voice_denied else R.string.voice_hint)),
                style = UType.Muted,
            )
        }
        if (voice.isNotBlank() && recording == null) {
            IconButton(onClick = {
                if (playing) { v.stopPlaying(); playing = false } else { playing = true; v.play(voice) { playing = false } }
            }) { Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(R.string.voice_play), tint = U.c.lapis) }
            IconButton(onClick = { v.stopPlaying(); v.delete(voice); onVoice("") }) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.faint)
            }
        }
    }
}
