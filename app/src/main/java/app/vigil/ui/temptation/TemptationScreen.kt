package app.vigil.ui.temptation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vigil.R
import app.vigil.container
import app.vigil.data.Vow
import app.vigil.domain.EventKind
import app.vigil.domain.Trigger
import app.vigil.domain.Vows
import app.vigil.ui.Go
import app.vigil.ui.T
import app.vigil.ui.VigilData
import app.vigil.ui.components.Candle
import app.vigil.ui.components.PrimaryButton
import app.vigil.ui.components.ScalePicker
import app.vigil.ui.components.Seal
import app.vigil.ui.components.SoftButton
import app.vigil.ui.components.TopBar
import app.vigil.ui.components.rememberNow
import app.vigil.ui.ctx
import app.vigil.ui.g
import app.vigil.ui.gp
import app.vigil.ui.gs
import app.vigil.ui.theme.Alegreya
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType

/**
 * When temptation comes: the night, a candle, your own reason. Breathe with the flame until it has
 * burnt down; most cravings pass in those minutes. Then say whether you held.
 */
@Composable
fun TemptationScreen(id: Long, data: VigilData, go: Go) {
    var chosen by rememberSaveable { mutableStateOf(id.takeIf { it > 0 } ?: data.running.singleOrNull()?.id ?: 0L) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        val bars = (view.context as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        onDispose {
            view.keepScreenOn = false
            bars?.isAppearanceLightStatusBars = true
            bars?.isAppearanceLightNavigationBars = true
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(U.c.night, U.c.night2, U.c.night)))) {
        val v = data.vow(chosen)
        if (v == null) Choose(data, go) { chosen = it } else Vigil(v, data, go)
    }
}

@Composable
private fun Choose(data: VigilData, go: Go, onPick: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar("", go::back, color = U.c.nightInk)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.tempt_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.flame))
            Spacer(Modifier.height(6.dp))
            Text(gs(R.string.tempt_which), style = UType.Title.copy(color = U.c.nightInk))
            Spacer(Modifier.height(18.dp))
            data.running.forEach { v ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(U.RMd).background(U.c.night2).border(1.dp, U.c.nightMuted.copy(alpha = 0.3f), U.RMd)
                        .clickable { onPick(v.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Seal(Color(v.waxOf.argb), v.initial, Modifier.size(44.dp), cracks = v.cracks, seed = v.seed)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(v.title, style = UType.Name.copy(color = U.c.nightInk))
                        Text(T.dayOf(ctx(), v, data.now), style = UType.Muted.copy(color = U.c.nightMuted))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Vigil(v: Vow, data: VigilData, go: Go) {
    val c = ctx()
    val container = c.container
    val total = data.settings.candleMin * 60_000L
    val begun = rememberSaveable { System.currentTimeMillis() }
    val now = rememberNow(250)
    val elapsed = (now - begun).coerceAtLeast(0)
    val burnt = (elapsed.toFloat() / total).coerceIn(0f, 1f)
    val out = elapsed >= total
    var strength by rememberSaveable { mutableIntStateOf(3) }
    var trigger by rememberSaveable { mutableStateOf<Trigger?>(null) }
    var playing by remember { mutableStateOf(false) }
    var held by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { container.voice.stopPlaying() } }

    // Breathe in for four seconds, out for six.
    val t = rememberInfiniteTransition(label = "breath")
    val breath by t.animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes { durationMillis = 10_000; 0f at 0 using LinearEasing; 1f at 4000 using LinearEasing; 0f at 10_000 }, RepeatMode.Restart),
        label = "b",
    )
    val inhaling = (now - begun) % 10_000 < 4_000

    if (held > 0) {
        Held(v, held, go)
        return
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar("", go::back, color = U.c.nightInk)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.tempt_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.flame))
            Spacer(Modifier.height(4.dp))
            Text(v.title, style = UType.Title.copy(color = U.c.nightInk, textAlign = TextAlign.Center))
            val kept = Vows.keptDays(v, data.eventsOf(v), data.urgesOf(v), data.now)
            if (kept > 0) Text(gp(R.plurals.tempt_kept, kept, kept), style = UType.Muted.copy(color = U.c.nightMuted))
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Candle(burnt, Modifier.size(220.dp, 300.dp), lit = !out, breath = if (out) null else breath)
            }
            Text(
                if (out) gs(R.string.tempt_out) else stringResource(if (inhaling) R.string.tempt_in else R.string.tempt_outbreath),
                style = UType.Name.copy(color = U.c.nightInk, textAlign = TextAlign.Center),
            )
            if (!out) {
                val left = (total - elapsed) / 1000
                Text("%d:%02d".format(left / 60, left % 60), fontFamily = Alegreya, fontSize = 15.sp, color = U.c.nightMuted)
            }
            if (v.why.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text("«" + v.why + "»", style = UType.Script.copy(color = U.c.nightInk, textAlign = TextAlign.Center))
            }
            if (container.voice.exists(v.voice)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.clip(U.RSm).clickable {
                        if (playing) { container.voice.stopPlaying(); playing = false } else { playing = true; container.voice.play(v.voice) { playing = false } }
                    }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = U.c.flame)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.vow_voice), style = UType.Strong.copy(color = U.c.flame))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.tempt_strength), style = UType.Small.copy(color = U.c.nightMuted))
            Spacer(Modifier.height(6.dp))
            ScalePicker(strength, { strength = it }, color = U.c.flame, ink = U.c.nightInk, track = U.c.night2)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.tempt_trigger), style = UType.Small.copy(color = U.c.nightMuted))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Trigger.entries.forEach { tr ->
                    val on = trigger == tr
                    Text(
                        T.trigger(c, tr),
                        style = UType.Body.copy(color = if (on) U.c.night else U.c.nightInk),
                        modifier = Modifier.clip(U.RSm).background(if (on) U.c.flame else U.c.night2).border(1.dp, U.c.nightMuted.copy(alpha = 0.3f), U.RSm)
                            .clickable { trigger = if (on) null else tr }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(gs(R.string.tempt_held), {
                held = data.urgesOf(v).count { it.held } + data.eventsOf(v).count { it.kindOf == EventKind.OVERCOME || it.kindOf == EventKind.RANSOM } + 1
                container.write { keeper.urge(v, strength, trigger, held = true) }
            }, Modifier.fillMaxWidth(), color = U.c.gold)
            SoftButton(
                g(stringResource(R.string.tempt_stronger)),
                {
                    container.write { keeper.urge(v, strength, trigger, held = false) }
                    go.back()
                    go.release(v.id)
                },
                Modifier.fillMaxWidth(), color = U.c.nightMuted, soft = U.c.night2,
            )
        }
    }
}

/** A temptation held: a gold bead on the rope. */
@Composable
private fun Held(v: Vow, held: Int, go: Go) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(120.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFE7A3), U.c.gold, Color(0xFF7A5A1C)))))
        Spacer(Modifier.height(24.dp))
        Text(gs(R.string.held_title).uppercase(), style = UType.Eyebrow.copy(color = U.c.flame))
        Spacer(Modifier.height(6.dp))
        Text(v.title, style = UType.Title.copy(color = U.c.nightInk, textAlign = TextAlign.Center))
        Spacer(Modifier.height(10.dp))
        Text(gp(R.plurals.held_text, held, held), style = UType.Sub.copy(color = U.c.nightMuted, textAlign = TextAlign.Center))
        Spacer(Modifier.height(28.dp))
        PrimaryButton(stringResource(R.string.held_ok), { go.back() }, Modifier.fillMaxWidth(), color = U.c.gold)
    }
}
