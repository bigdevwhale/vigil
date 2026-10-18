package app.vigil.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.vigil.domain.Bead
import app.vigil.ui.theme.Ruslan
import app.vigil.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private fun Color.darker(f: Float) = lerp(this, Color.Black, f)
private fun Color.lighter(f: Float) = lerp(this, Color.White, f)

/**
 * A wax seal: an uneven blob with an embossed ring and initial, cracked once for every lapse that
 * was let stand. [ash] greys it out for a vow that burned.
 */
@Composable
fun Seal(wax: Color, letter: String, modifier: Modifier, cracks: Int = 0, seed: Int = 0, ash: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val base = if (ash) lerp(wax, Color(0xFF8C8178), 0.75f) else wax
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2f * 0.92f
        val c = Offset(size.width / 2, size.height / 2)
        val rnd = Random(seed)
        val phases = List(4) { rnd.nextFloat() * 2 * PI.toFloat() }
        val drips = List(3) { rnd.nextFloat() * 2 * PI.toFloat() }
        val blob = Path()
        val n = 72
        for (i in 0..n) {
            val a = i / n.toFloat() * 2 * PI.toFloat()
            var k = 1f + 0.035f * sin(3 * a + phases[0]) + 0.025f * sin(5 * a + phases[1]) + 0.015f * sin(9 * a + phases[2])
            drips.forEach { d ->
                val diff = kotlin.math.abs(((a - d + 3 * PI) % (2 * PI) - PI).toFloat())
                if (diff < 0.22f) k += 0.07f * cos(diff / 0.22f * PI.toFloat() / 2)
            }
            val p = Offset(c.x + cos(a) * r * k, c.y + sin(a) * r * k)
            if (i == 0) blob.moveTo(p.x, p.y) else blob.lineTo(p.x, p.y)
        }
        blob.close()
        // A soft shadow on the parchment, then the wax.
        drawPath(blob, Color.Black.copy(alpha = 0.18f), style = Stroke(r * 0.06f))
        drawPath(
            blob,
            Brush.radialGradient(listOf(base.lighter(0.22f), base, base.darker(0.35f)), center = c + Offset(-r * 0.3f, -r * 0.35f), radius = r * 1.5f),
        )
        // The pressed ring.
        drawCircle(base.darker(0.3f), r * 0.74f, c, style = Stroke(r * 0.05f))
        drawCircle(base.lighter(0.25f).copy(alpha = 0.6f), r * 0.74f, c + Offset(r * 0.02f, r * 0.03f), style = Stroke(r * 0.02f))
        drawCircle(base.darker(0.18f), r * 0.68f, c)
        // A ring of pearls.
        for (i in 0 until 20) {
            val a = i / 20f * 2 * PI.toFloat()
            val p = Offset(c.x + cos(a) * r * 0.84f, c.y + sin(a) * r * 0.84f)
            drawCircle(base.darker(0.32f), r * 0.032f, p)
            drawCircle(base.lighter(0.3f).copy(alpha = 0.7f), r * 0.016f, p - Offset(r * 0.01f, r * 0.01f))
        }
        // The initial, embossed: a light edge below-right, dark letter on top.
        val style = TextStyle(fontFamily = Ruslan, fontSize = (r * 0.95f).toSp())
        val layout = measurer.measure(letter.take(1).uppercase(), style)
        val tl = c - Offset(layout.size.width / 2f, layout.size.height / 2f)
        drawText(layout, color = base.lighter(0.35f).copy(alpha = 0.8f), topLeft = tl + Offset(r * 0.03f, r * 0.04f))
        drawText(layout, color = base.darker(0.45f), topLeft = tl)
        // Shine.
        drawArc(
            Color.White.copy(alpha = 0.22f), 200f, 60f, false,
            topLeft = c - Offset(r * 0.88f, r * 0.88f), size = Size(r * 1.76f, r * 1.76f), style = Stroke(r * 0.06f, cap = StrokeCap.Round),
        )
        // Cracks.
        val cr = Random(seed * 31 + 7)
        repeat(if (ash) maxOf(cracks, 3) else min(cracks, 6)) { i ->
            var a = cr.nextFloat() * 2 * PI.toFloat() + i
            var d = r * (0.05f + cr.nextFloat() * 0.25f)
            val crack = Path().apply { moveTo(c.x + cos(a) * d, c.y + sin(a) * d) }
            while (d < r * 1.02f) {
                d += r * (0.12f + cr.nextFloat() * 0.12f)
                a += (cr.nextFloat() - 0.5f) * 0.5f
                crack.lineTo(c.x + cos(a) * d, c.y + sin(a) * d)
            }
            drawPath(crack, base.lighter(0.4f).copy(alpha = 0.5f), style = Stroke(r * 0.035f, cap = StrokeCap.Round))
            drawPath(crack, Color(0xFF1A0E08).copy(alpha = 0.85f), style = Stroke(r * 0.022f, cap = StrokeCap.Round))
        }
    }
}

/**
 * A candle that burns down as [burnt] goes from 0 to 1. Lit, its flame flickers; [breath] (0..1)
 * swells it, to breathe with.
 */
@Composable
fun Candle(burnt: Float, modifier: Modifier, lit: Boolean = true, breath: Float? = null, wax: Color = Color(0xFFF3E7CC)) {
    val t = rememberInfiniteTransition(label = "flame")
    val flicker by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart), label = "flicker")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val bodyW = w * 0.22f
        val bottom = h * 0.95f
        val full = h * 0.62f
        val bodyH = (full * (1f - 0.85f * burnt.coerceIn(0f, 1f))).coerceAtLeast(h * 0.06f)
        val top = bottom - bodyH
        val f = flicker * 2 * PI.toFloat()
        val swell = breath?.let { 0.75f + 0.5f * it } ?: 1f
        val flameH = h * 0.2f * swell * (1f + 0.05f * sin(f * 3) + 0.03f * sin(f * 7))
        val sway = w * 0.012f * sin(f * 2) + w * 0.006f * sin(f * 5)
        if (lit) {
            // The glow fades out before the canvas edge, so it never shows as a box.
            val gc = Offset(cx, top - flameH * 0.5f)
            val gr = minOf(w * 0.5f, gc.y, h - gc.y).coerceAtLeast(1f) * swell.coerceAtMost(1f)
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFC458), Color(0x22FF9B3A), Color.Transparent), center = gc, radius = gr), gr, gc)
        }
        // The holder.
        drawOval(Color(0xFF6E4E26), Offset(cx - w * 0.36f, bottom - h * 0.03f), Size(w * 0.72f, h * 0.06f))
        drawOval(Color(0xFF9A7338), Offset(cx - w * 0.3f, bottom - h * 0.04f), Size(w * 0.6f, h * 0.045f))
        // The body.
        drawRect(
            Brush.horizontalGradient(listOf(wax.darker(0.12f), wax.lighter(0.4f), wax, wax.darker(0.2f)), startX = cx - bodyW / 2, endX = cx + bodyW / 2),
            Offset(cx - bodyW / 2, top), Size(bodyW, bodyH - h * 0.02f),
        )
        drawOval(wax.darker(0.06f), Offset(cx - bodyW / 2, top - h * 0.012f), Size(bodyW, h * 0.024f))
        // Drips down the side.
        drawRoundRect(wax.lighter(0.2f), Offset(cx + bodyW * 0.18f, top - h * 0.004f), Size(bodyW * 0.16f, bodyH * 0.32f), CornerRadius(bodyW * 0.08f))
        drawRoundRect(wax.lighter(0.15f), Offset(cx - bodyW * 0.42f, top - h * 0.004f), Size(bodyW * 0.12f, bodyH * 0.18f), CornerRadius(bodyW * 0.06f))
        // The wick.
        drawLine(Color(0xFF2A1C12), Offset(cx, top), Offset(cx + sway * 0.3f, top - h * 0.035f), w * 0.012f, StrokeCap.Round)
        if (lit) {
            val base = Offset(cx, top - h * 0.02f)
            val tip = Offset(cx + sway, base.y - flameH)
            val fw = w * 0.07f * swell
            val flame = Path().apply {
                moveTo(tip.x, tip.y)
                cubicTo(tip.x + fw * 0.4f, base.y - flameH * 0.55f, base.x + fw, base.y - flameH * 0.15f, base.x, base.y)
                cubicTo(base.x - fw, base.y - flameH * 0.15f, tip.x - fw * 0.4f, base.y - flameH * 0.55f, tip.x, tip.y)
            }
            drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFF6CF), Color(0xFFFFC14A), Color(0xFFE5632A)), startY = tip.y, endY = base.y))
            drawOval(Color(0xFF4F7BD6).copy(alpha = 0.55f), Offset(base.x - fw * 0.35f, base.y - flameH * 0.18f), Size(fw * 0.7f, flameH * 0.2f))
        } else {
            // A thread of smoke.
            val smoke = Path().apply {
                moveTo(cx, top - h * 0.035f)
                cubicTo(cx + w * 0.05f, top - h * 0.09f, cx - w * 0.05f, top - h * 0.14f, cx + w * 0.02f, top - h * 0.2f)
            }
            drawPath(smoke, Color(0xFF8C8178).copy(alpha = 0.35f), style = Stroke(w * 0.01f, cap = StrokeCap.Round))
        }
    }
}

/**
 * The prayer rope: one bead a day, wrapped onto rows strung on a cord. Every tenth bead is a little
 * larger, the way a rope marks its decades.
 */
@Composable
fun Beads(beads: List<Bead>, modifier: Modifier = Modifier, maxBead: Float = 22f) {
    val colors = BeadColors(
        ink = Color(0xFF5A3B26), gold = U.c.gold, crack = U.c.rubric, mended = Color(0xFFC9772E),
        faint = U.c.faint, line = U.c.line, cord = U.c.muted.copy(alpha = 0.5f), today = U.c.rubric,
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val n = beads.size.coerceAtLeast(1)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val max = with(density) { maxBead.dp.toPx() }
        val min = with(density) { 9.dp.toPx() }
        // Fit them on as few rows as reads well: a bead shrinks before another row is added.
        var cell = max
        var perRow = (widthPx / cell).toInt().coerceAtLeast(1)
        while (cell > min && (n + perRow - 1) / perRow > 4) {
            cell -= 1f
            perRow = (widthPx / cell).toInt().coerceAtLeast(1)
        }
        val rows = (n + perRow - 1) / perRow
        val heightDp = with(density) { (rows * cell * 1.15f).toDp() }
        Canvas(Modifier.fillMaxWidth().height(heightDp)) { drawBeads(beads, perRow, cell, colors) }
    }
}

private class BeadColors(val ink: Color, val gold: Color, val crack: Color, val mended: Color, val faint: Color, val line: Color, val cord: Color, val today: Color)

private fun DrawScope.drawBeads(beads: List<Bead>, perRow: Int, cell: Float, k: BeadColors) {
    val rowH = cell * 1.15f
    beads.chunked(perRow).forEachIndexed { row, list ->
        val y = row * rowH + rowH / 2
        val x0 = cell / 2
        drawLine(k.cord, Offset(x0, y), Offset(x0 + (list.size - 1) * cell, y), cell * 0.06f)
        list.forEachIndexed { i, b ->
            val index = row * perRow + i
            val big = (index + 1) % 10 == 0
            val r = cell * (if (big) 0.44f else 0.36f)
            val c = Offset(x0 + i * cell, y)
            when (b) {
                Bead.FUTURE -> {
                    drawCircle(k.line.copy(alpha = 0.6f), r, c)
                    drawCircle(k.faint.copy(alpha = 0.5f), r, c, style = Stroke(cell * 0.05f))
                }
                Bead.TODAY -> {
                    drawCircle(k.today.copy(alpha = 0.18f), r * 1.25f, c)
                    drawCircle(k.today, r, c, style = Stroke(cell * 0.09f))
                }
                Bead.OPEN -> drawCircle(k.ink.copy(alpha = 0.55f), r, c, style = Stroke(cell * 0.07f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.5f, r * 0.35f))))
                Bead.MISSED -> drawCircle(k.faint, r, c, style = Stroke(cell * 0.07f))
                else -> {
                    val col = when (b) {
                        Bead.GOLD -> k.gold
                        Bead.CRACK -> k.crack
                        Bead.MENDED -> k.mended
                        else -> k.ink
                    }
                    drawCircle(Brush.radialGradient(listOf(col.lighter(0.35f), col, col.darker(0.3f)), c - Offset(r * 0.35f, r * 0.35f), r * 1.6f), r, c)
                    if (b == Bead.CRACK) {
                        drawLine(Color(0xFF1A0E08), c + Offset(-r * 0.5f, -r * 0.6f), c + Offset(r * 0.1f, 0f), cell * 0.05f)
                        drawLine(Color(0xFF1A0E08), c + Offset(r * 0.1f, 0f), c + Offset(-r * 0.1f, r * 0.7f), cell * 0.05f)
                    }
                    if (b == Bead.GOLD) drawCircle(Color.White.copy(alpha = 0.6f), r * 0.22f, c - Offset(r * 0.35f, r * 0.35f))
                }
            }
        }
    }
}

/**
 * The scroll's illumination: a vine runs around the page as far as the vow has come, with a leaf
 * every few days and a gold berry for each temptation held. When the vow is kept, the corners bloom.
 */
@Composable
fun Illumination(progress: Float, golds: Int, seed: Int, modifier: Modifier, color: Color = U.c.verdigris, gold: Color = U.c.gold, bloom: Color = U.c.rubric) {
    Canvas(modifier) {
        val inset = 9.dp.toPx()
        val rect = Rect(inset, inset, size.width - inset, size.height - inset)
        val path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(10.dp.toPx()))) }
        val pm = PathMeasure().apply { setPath(path, false) }
        val len = pm.length
        val p = progress.coerceIn(0f, 1f)
        if (p <= 0f) return@Canvas
        val drawn = Path()
        pm.getSegment(0f, len * p, drawn, true)
        drawPath(drawn, color.copy(alpha = 0.85f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        val step = 22.dp.toPx()
        val leaf = 7.dp.toPx()
        var d = step / 2
        var side = 1
        val rnd = Random(seed)
        while (d < len * p) {
            val pos = pm.getPosition(d)
            val tan = pm.getTangent(d)
            val angle = (kotlin.math.atan2(tan.y, tan.x) * 180 / PI).toFloat() + 90f * side
            rotate(angle, pos) {
                val l = Path().apply {
                    moveTo(pos.x, pos.y)
                    quadraticTo(pos.x + leaf * 0.5f, pos.y - leaf * 0.45f, pos.x + leaf, pos.y)
                    quadraticTo(pos.x + leaf * 0.5f, pos.y + leaf * 0.45f, pos.x, pos.y)
                }
                drawPath(l, color.copy(alpha = 0.7f + rnd.nextFloat() * 0.3f))
            }
            side = -side
            d += step * (0.8f + rnd.nextFloat() * 0.4f)
        }
        // Gold berries along the drawn length.
        val g = golds.coerceAtMost(40)
        for (i in 0 until g) {
            val at = len * p * (i + 0.5f) / g
            val pos = pm.getPosition(at)
            drawCircle(gold.darker(0.2f), 3.2.dp.toPx(), pos)
            drawCircle(gold.lighter(0.3f), 1.6.dp.toPx(), pos - Offset(0.8f, 0.8f))
        }
        if (p >= 1f) {
            listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { c -> flower(c, 7.dp.toPx(), bloom, gold) }
        }
    }
}

private fun DrawScope.flower(c: Offset, r: Float, petal: Color, heart: Color) {
    for (i in 0 until 5) {
        val a = i / 5f * 2 * PI.toFloat() - PI.toFloat() / 2
        drawCircle(petal, r * 0.55f, c + Offset(cos(a) * r * 0.6f, sin(a) * r * 0.6f))
    }
    drawCircle(heart, r * 0.42f, c)
}

/** An illuminated initial: the letter in a gilded square with a field of colour and a little foliage. */
@Composable
fun Initial(letter: String, modifier: Modifier, field: Color = U.c.lapis, gilt: Color = U.c.gold, ink: Color = Color(0xFFFFF4DE)) {
    val measurer = rememberTextMeasurer()
    val l = remember(letter) { letter.take(1).uppercase() }
    Canvas(modifier) {
        val s = min(size.width, size.height)
        val o = Offset((size.width - s) / 2, (size.height - s) / 2)
        drawRect(gilt.darker(0.15f), o, Size(s, s))
        drawRect(Brush.linearGradient(listOf(gilt.lighter(0.35f), gilt, gilt.darker(0.2f)), o, o + Offset(s, s)), o + Offset(s * 0.03f, s * 0.03f), Size(s * 0.94f, s * 0.94f))
        drawRect(field, o + Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f))
        // Corner tendrils.
        val tend = gilt.lighter(0.15f)
        listOf(Offset(0.18f, 0.18f), Offset(0.82f, 0.18f), Offset(0.18f, 0.82f), Offset(0.82f, 0.82f)).forEach { (x, y) ->
            drawCircle(tend, s * 0.035f, o + Offset(s * x, s * y))
        }
        val style = TextStyle(fontFamily = Ruslan, fontSize = (s * 0.62f).toSp())
        val layout = measurer.measure(l, style)
        val tl = o + Offset((s - layout.size.width) / 2f, (s - layout.size.height) / 2f)
        drawText(layout, color = Color.Black.copy(alpha = 0.3f), topLeft = tl + Offset(s * 0.015f, s * 0.02f))
        drawText(layout, color = ink, topLeft = tl)
    }
}

/** A heat cell for the map of temptations: from parchment to rubric. */
fun heat(base: Color, hot: Color, f: Float): Color = lerp(base, hot, f.coerceIn(0f, 1f))

