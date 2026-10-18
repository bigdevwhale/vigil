package app.vigil.ui.release

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vigil.R
import app.vigil.container
import app.vigil.data.Vow
import app.vigil.domain.Outcome
import app.vigil.domain.Rigor
import app.vigil.domain.Step
import app.vigil.domain.VowType
import app.vigil.domain.Vows
import app.vigil.domain.gendered
import app.vigil.ui.Go
import app.vigil.ui.LocalGender
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Candle
import app.vigil.ui.components.ConfirmDialog
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SelectCard
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.UCard
import app.vigil.ui.components.UTextField
import app.vigil.ui.components.rememberNow
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gs
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Where the release has got to. */
private enum class Stage { LADDER, OUTCOME, HELD, RANSOMED, DELAYED, DONE }

/**
 * The ladder of release: every step the vow was sealed with stands between you and breaking it. At
 * any step you may still hold. With [confess] (a day), the lapse has already happened: straight to
 * what becomes of the vow.
 */
@Composable
fun ReleaseScreen(id: Long, confess: Int, data: VigilData, go: Go) {
    val v = data.vow(id) ?: return
    val c = ctx()
    val container = c.container
    val now = data.now
    val voiceOk = container.voice.exists(v.voice)
    val ladder = remember(v.ladder, voiceOk) { v.ladderOf.filter { it != Step.VOICE || voiceOk } }
    // After a night of "sleep on it", go on from the step after it.
    val from = if (Vows.delayOver(v, now)) ladder.indexOf(Step.TOMORROW) + 1 else 0
    var stage by rememberSaveable {
        mutableStateOf(
            when {
                confess > 0 -> Stage.OUTCOME
                Vows.delayed(v, now) -> Stage.DELAYED
                from >= ladder.size -> Stage.OUTCOME
                else -> Stage.LADDER
            }
        )
    }
    var index by rememberSaveable { mutableIntStateOf(from) }
    var outcome by rememberSaveable { mutableStateOf<Outcome?>(null) }

    fun next() {
        if (index + 1 >= ladder.size) stage = Stage.OUTCOME else index++
    }
    fun hold() {
        container.write { keeper.overcome(v) }
        stage = Stage.HELD
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(if (confess > 0) gs(R.string.release_confess_title) else stringResource(R.string.release_title), go::back)
        Box(Modifier.weight(1f)) {
            when (stage) {
                Stage.LADDER -> {
                    val step = ladder[index]
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
                        Progress(ladder, index)
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.release_step, index + 1, ladder.size).uppercase(), style = UType.Eyebrow)
                        Spacer(Modifier.height(4.dp))
                        Text(T.step(c, step), style = UType.Title)
                        Spacer(Modifier.height(14.dp))
                        when (step) {
                            Step.WAIT -> WaitStep(v, ::next)
                            Step.VOICE -> VoiceStep(v, ::next)
                            Step.LETTER -> LetterStep(v, ::next)
                            Step.QUESTIONS -> QuestionsStep(::next)
                            Step.RANSOM -> RansomStep(v, onRansom = {
                                container.write { keeper.ransom(v) }
                                stage = Stage.RANSOMED
                            }, onNext = ::next)
                            Step.WITNESS -> WitnessStep(v, ::next)
                            Step.TITHE -> TitheStep(v) {
                                container.write { keeper.tithe(v) }
                                next()
                            }
                            Step.TOMORROW -> TomorrowStep(v) {
                                container.write { keeper.delay(v, Vows.tomorrowAt(System.currentTimeMillis(), v.delayAt, ZoneId.systemDefault())) }
                                stage = Stage.DELAYED
                            }
                        }
                    }
                }
                Stage.OUTCOME -> OutcomeStep(v, data, confess, outcome, { outcome = it }) { o ->
                    val day = if (confess > 0) confess else null
                    val missed = v.typeOf == VowType.PRACTICE && confess > 0
                    container.write { keeper.lapse(v, o, day, missed) }
                    outcome = o
                    stage = Stage.DONE
                }
                Stage.HELD -> Ending(v, gs(R.string.release_held_title), gs(R.string.release_held_text), U.c.gold) { go.back() }
                Stage.RANSOMED -> Ending(v, gs(R.string.release_ransomed_title), g(stringResource(R.string.release_ransomed_text, v.ransom)), U.c.gold) { go.back() }
                Stage.DELAYED -> Ending(
                    v, gs(R.string.release_delayed_title),
                    g(stringResource(R.string.release_delayed_text, T.dateTime(c, data.vow(id)?.breakAt ?: Vows.tomorrowAt(now, v.delayAt, ZoneId.systemDefault())))),
                    U.c.lapis,
                ) { go.back() }
                Stage.DONE -> Done(v, outcome ?: Outcome.CRACK, go)
            }
        }
        if (stage == Stage.LADDER) {
            HoldStrip(::hold)
        }
    }
}

/** Steps as a row of dots: done, here, to come. */
@Composable
private fun Progress(ladder: List<Step>, index: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ladder.indices.forEach { i ->
            Box(
                Modifier.weight(1f).height(5.dp).clip(CircleShape)
                    .background(if (i < index) U.c.rubric else if (i == index) U.c.rubric.copy(alpha = 0.5f) else U.c.surface2),
            )
        }
    }
}

@Composable
private fun HoldStrip(onHold: () -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
        PrimaryButton(gs(R.string.release_hold), onHold, Modifier.fillMaxWidth(), color = U.c.verdigris)
    }
}

@Composable
private fun WaitStep(v: Vow, onNext: () -> Unit) {
    val total = v.waitMin * 60_000L
    val begun = rememberSaveable { System.currentTimeMillis() }
    val now = rememberNow(500)
    val elapsed = (now - begun).coerceAtLeast(0)
    val done = elapsed >= total
    Text(g(T.stepHint(ctx(), Step.WAIT)), style = UType.Sub)
    Box(Modifier.fillMaxWidth().height(250.dp).padding(vertical = 10.dp).clip(U.RLg).background(U.c.night), contentAlignment = Alignment.Center) {
        Candle(elapsed.toFloat() / total, Modifier.size(150.dp, 230.dp), lit = !done)
    }
    val left = ((total - elapsed) / 1000).coerceAtLeast(0)
    Text(
        if (done) gs(R.string.release_wait_done) else stringResource(R.string.release_wait_left, "%d:%02d".format(left / 60, left % 60)),
        style = UType.Name.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    SoftButton(stringResource(R.string.release_next), onNext, Modifier.fillMaxWidth(), enabled = done)
}

@Composable
private fun VoiceStep(v: Vow, onNext: () -> Unit) {
    val voice = ctx().container.voice
    var heard by rememberSaveable { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { voice.stopPlaying() } }
    LaunchedEffect(Unit) {
        if (!heard) {
            playing = true
            voice.play(v.voice) { playing = false; heard = true }
        }
    }
    Text(g(T.stepHint(ctx(), Step.VOICE)), style = UType.Sub)
    Spacer(Modifier.height(14.dp))
    UCard {
        if (v.why.isNotBlank()) Text("«" + v.why + "»", style = UType.Script.copy(color = U.c.ink))
        Spacer(Modifier.height(8.dp))
        if (playing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlayArrow, null, tint = U.c.lapis)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.release_voice_playing), style = UType.Strong.copy(color = U.c.lapis))
            }
        }
        if (!playing) {
            Spacer(Modifier.height(6.dp))
            SoftButton(stringResource(R.string.release_voice_again), { playing = true; voice.play(v.voice) { playing = false; heard = true } }, Modifier.fillMaxWidth(), color = U.c.lapis, soft = U.c.lapisSoft)
        }
    }
    Spacer(Modifier.height(14.dp))
    SoftButton(stringResource(R.string.release_next), onNext, Modifier.fillMaxWidth(), enabled = heard && !playing)
}

@Composable
private fun LetterStep(v: Vow, onNext: () -> Unit) {
    val c = ctx()
    var text by rememberSaveable { mutableStateOf("") }
    Text(g(T.stepHint(c, Step.LETTER)), style = UType.Sub)
    Spacer(Modifier.height(10.dp))
    UTextField(text, { text = it.take(3000) }, placeholder = g(stringResource(R.string.release_letter_hint)), minLines = 6)
    Spacer(Modifier.height(6.dp))
    val n = text.trim().length
    Text(
        stringResource(R.string.release_letter_count, n, v.letterMin),
        style = UType.Small.copy(color = if (n >= v.letterMin) U.c.verdigris else U.c.faint),
    )
    Spacer(Modifier.height(14.dp))
    SoftButton(stringResource(R.string.release_next), {
        c.container.write { keeper.letter(v, text) }
        onNext()
    }, Modifier.fillMaxWidth(), enabled = n >= v.letterMin)
}

@Composable
private fun QuestionsStep(onNext: () -> Unit) {
    val qs = stringArrayResource(R.array.questions).map { g(it) }
    var answers by rememberSaveable { mutableStateOf(List(qs.size) { "" }) }
    Text(g(T.stepHint(ctx(), Step.QUESTIONS)), style = UType.Sub)
    qs.forEachIndexed { i, q ->
        Spacer(Modifier.height(14.dp))
        Text(q, style = UType.Name)
        Spacer(Modifier.height(6.dp))
        UTextField(answers[i], { a -> answers = answers.toMutableList().also { it[i] = a.take(500) } }, minLines = 2)
    }
    Spacer(Modifier.height(16.dp))
    SoftButton(stringResource(R.string.release_next), onNext, Modifier.fillMaxWidth(), enabled = answers.all { it.trim().length >= 3 })
}

@Composable
private fun RansomStep(v: Vow, onRansom: () -> Unit, onNext: () -> Unit) {
    Text(g(T.stepHint(ctx(), Step.RANSOM)), style = UType.Sub)
    Spacer(Modifier.height(14.dp))
    UCard(border = U.c.gold) {
        Text(stringResource(R.string.release_ransom_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
        Spacer(Modifier.height(4.dp))
        Text(v.ransom.ifBlank { g(stringResource(R.string.release_ransom_default)) }, style = UType.Title)
    }
    Spacer(Modifier.height(14.dp))
    PrimaryButton(gs(R.string.release_ransom_done), onRansom, Modifier.fillMaxWidth(), color = U.c.gold)
    Spacer(Modifier.height(10.dp))
    SoftButton(gs(R.string.release_ransom_skip), onNext, Modifier.fillMaxWidth())
}

@Composable
private fun WitnessStep(v: Vow, onNext: () -> Unit) {
    val c = ctx()
    var sent by rememberSaveable { mutableStateOf(false) }
    val who = v.witness.ifBlank { stringResource(R.string.release_witness_someone) }
    val message = g(stringResource(R.string.release_witness_message, v.title))
    Text(g(T.stepHint(c, Step.WITNESS)), style = UType.Sub)
    Spacer(Modifier.height(14.dp))
    UCard {
        Text(stringResource(R.string.release_witness_to, who), style = UType.Strong)
        Spacer(Modifier.height(6.dp))
        Text("«$message»", style = UType.Script.copy(color = U.c.ink))
    }
    Spacer(Modifier.height(14.dp))
    PrimaryButton(stringResource(R.string.release_witness_send), {
        val intent = if (v.witnessPhone.isNotBlank()) Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + v.witnessPhone)).putExtra("sms_body", message)
        else Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message), null)
        runCatching { c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        sent = true
    }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    SoftButton(stringResource(R.string.release_next), onNext, Modifier.fillMaxWidth(), enabled = sent)
}

@Composable
private fun TitheStep(v: Vow, onAccept: () -> Unit) {
    Text(g(T.stepHint(ctx(), Step.TITHE)), style = UType.Sub)
    Spacer(Modifier.height(14.dp))
    UCard(border = U.c.gold) {
        Text(gs(R.string.release_tithe_pledged).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
        Spacer(Modifier.height(4.dp))
        Text(v.tithe.ifBlank { stringResource(R.string.release_tithe_default) }, style = UType.Title)
    }
    Spacer(Modifier.height(14.dp))
    SoftButton(gs(R.string.release_tithe_accept), onAccept, Modifier.fillMaxWidth())
}

@Composable
private fun TomorrowStep(v: Vow, onDelay: () -> Unit) {
    val c = ctx()
    Text(g(T.stepHint(c, Step.TOMORROW)), style = UType.Sub)
    Spacer(Modifier.height(14.dp))
    UCard {
        Text(stringResource(R.string.release_tomorrow_at, T.clock(v.delayAt)), style = UType.Name)
    }
    Spacer(Modifier.height(14.dp))
    PrimaryButton(stringResource(R.string.release_tomorrow_ok), onDelay, Modifier.fillMaxWidth(), color = U.c.lapis)
}

/** What becomes of the vow, from what its rigor allows. */
@Composable
private fun OutcomeStep(v: Vow, data: VigilData, confess: Int, picked: Outcome?, onPick: (Outcome) -> Unit, onApply: (Outcome) -> Unit) {
    val c = ctx()
    val now = data.now
    val options = Vows.outcomes(v, now)
    var choice by remember { mutableStateOf(picked ?: options.first()) }
    var burn by remember { mutableStateOf(false) }
    if (burn) ConfirmDialog(
        gs(R.string.outcome_burn_confirm_title), g(stringResource(R.string.outcome_burn_confirm_text, v.title)), stringResource(R.string.outcome_burn),
        onConfirm = { onApply(Outcome.BURN) }, onDismiss = { burn = false }, danger = true,
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Seal(Color(v.waxOf.argb), v.initial, Modifier.size(64.dp), cracks = v.cracks, seed = v.seed)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    (if (confess > 0) stringResource(R.string.event_day, confess) else T.dayOf(c, v, now)).uppercase(),
                    style = UType.Eyebrow,
                )
                Text(v.title, style = UType.Name)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(gs(R.string.outcome_title), style = UType.Title)
        Spacer(Modifier.height(6.dp))
        Text(g(stringResource(if (v.rigorOf == Rigor.HERMIT) R.string.outcome_hermit_text else R.string.outcome_text)), style = UType.Sub)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                SelectCard(choice == o, { choice = o; onPick(o) }, if (o == Outcome.BURN) U.c.rubric else U.c.lapis) {
                    Column {
                        Text(T.outcome(c, o), style = UType.Name)
                        Text(g(T.outcomeHint(c, o, v, now)), style = UType.Muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.outcome_apply), { if (choice == Outcome.BURN) burn = true else onApply(choice) }, Modifier.fillMaxWidth())
    }
}

/** The vow is still standing (held, ransomed or put off till morning). */
@Composable
private fun Ending(v: Vow, title: String, text: String, color: Color, onOk: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Seal(Color(v.waxOf.argb), v.initial, Modifier.size(130.dp), cracks = v.cracks, seed = v.seed)
        Spacer(Modifier.height(18.dp))
        Text(title, style = UType.Title.copy(textAlign = TextAlign.Center, color = color))
        Spacer(Modifier.height(8.dp))
        Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.release_back), onOk, Modifier.fillMaxWidth(), color = color)
    }
}

/** After the lapse: what it did to the vow, and for a burned one, whether to begin again. */
@Composable
private fun Done(v: Vow, o: Outcome, go: Go) {
    val c = ctx()
    val gender = LocalGender.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val burned = o == Outcome.BURN
        Seal(Color(v.waxOf.argb), v.initial, Modifier.size(130.dp), cracks = v.cracks + if (o == Outcome.CRACK) 1 else 0, seed = v.seed, ash = burned)
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(
                when (o) {
                    Outcome.INDULGENCE -> R.string.done_indulgence
                    Outcome.PENANCE -> R.string.done_penance
                    Outcome.CRACK -> R.string.done_crack
                    Outcome.BURN -> R.string.done_burn
                }
            ),
            style = UType.Title.copy(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            gendered(
                when (o) {
                    Outcome.PENANCE -> c.resources.getQuantityString(R.plurals.done_penance_text, Vows.penance(v.rigorOf), Vows.penance(v.rigorOf))
                    Outcome.INDULGENCE -> c.getString(R.string.done_indulgence_text)
                    Outcome.CRACK -> c.getString(R.string.done_crack_text)
                    Outcome.BURN -> c.getString(R.string.done_burn_text)
                },
                gender,
            ),
            style = UType.Sub.copy(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(24.dp))
        if (burned) {
            PrimaryButton(stringResource(R.string.done_again), {
                c.container.write {
                    val id = keeper.again(v)
                    scope.launch(Dispatchers.Main) { go.home(); go.vow(id) }
                }
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.done_chronicle), { go.home() }, Modifier.fillMaxWidth())
        } else {
            PrimaryButton(stringResource(R.string.release_back), { go.home() }, Modifier.fillMaxWidth())
        }
    }
}
