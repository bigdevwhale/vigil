package app.vigil.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.vigil.R
import app.vigil.ui.theme.Alegreya
import app.vigil.ui.theme.U
import app.vigil.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** A leaf of vellum. */
@Composable
fun UCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), border: Color = U.c.line, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(U.c.surface)
            .border(1.dp, border, U.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** A rubric heading between two rules, the way a manuscript marks a new part. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("❦", color = U.c.rubric, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(U.c.line))
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = U.c.rubric, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = U.RMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = U.c.onRubric,
            disabledContainerColor = U.c.surface2,
            disabledContentColor = U.c.faint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Quiet, tinted button for secondary actions. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.rubric, soft: Color = U.c.rubricSoft, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(50.dp)
            .clip(U.RMd)
            .background(if (enabled) soft else U.c.surface2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = if (enabled) color else U.c.faint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, color = if (enabled) color else U.c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Hold to act: a ring of wax fills while the finger stays down, and the action fires when it closes.
 * Used for sealing a vow, so that it is never done by a slip of the thumb.
 */
@Composable
fun HoldButton(text: String, onDone: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.rubric, holdMs: Int = 1400, enabled: Boolean = true) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .height(58.dp)
            .clip(U.RMd)
            .background(if (enabled) color.copy(alpha = 0.18f) else U.c.surface2)
            .border(1.5.dp, if (enabled) color else U.c.line, U.RMd)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        progress.animateTo(1f, tween(((1f - progress.value) * holdMs).toInt(), easing = LinearEasing))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDone()
                        progress.snapTo(0f)
                    }
                    waitForUpOrCancellation()
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).align(Alignment.CenterStart).background(color))
        Text(text, style = UType.Button, color = if (progress.value > 0.5f) U.c.onRubric else if (enabled) color else U.c.faint)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = U.c.rubric) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Alegreya, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        shape = U.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = U.c.surface,
            labelColor = U.c.muted,
            selectedContainerColor = color.copy(alpha = 0.14f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = U.c.line,
            selectedBorderColor = color.copy(alpha = 0.55f),
        ),
    )
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(U.RSm)
            .background(U.c.surface2)
            .border(1.dp, U.c.line, U.RSm)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedSm)
                    .background(if (on) U.c.surface else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, U.c.line, RoundedSm) else Modifier)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = UType.Button.copy(fontSize = 14.5.sp), color = if (on) U.c.rubric else U.c.muted, maxLines = 1)
            }
        }
    }
}

private val RoundedSm = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

/** A glyph on a soft disc. */
@Composable
fun IconDisc(icon: ImageVector, size: Dp = 44.dp, color: Color = U.c.rubric, soft: Color = U.c.rubricSoft) {
    Box(Modifier.size(size).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, color: Color = U.c.ink, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = color)
            }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = UType.Name.copy(color = color), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** A tappable row inside a card: icon, label, value and a chevron. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, tint: Color = U.c.rubric, soft: Color = U.c.rubricSoft, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp, tint, soft)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = UType.Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = UType.Strong)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = U.c.rubric, track: Color = U.c.surface2) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        color = color,
        trackColor = track,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** A thin ornamental rule with a lozenge in the middle. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = U.c.gold) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val y = size.height / 2
        val cx = size.width / 2
        drawLine(color.copy(alpha = 0.6f), Offset(size.width * 0.12f, y), Offset(cx - 14f, y), 2f)
        drawLine(color.copy(alpha = 0.6f), Offset(cx + 14f, y), Offset(size.width * 0.88f, y), 2f)
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, y - 9f); lineTo(cx + 9f, y); lineTo(cx, y + 9f); lineTo(cx - 9f, y); close()
        }
        drawPath(p, color)
        drawCircle(color, 3f, Offset(cx - 20f, y))
        drawCircle(color, 3f, Offset(cx + 20f, y))
    }
}

/** Wall clock that ticks every [stepMs] while the screen is visible. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { visible = true; now = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    LaunchedEffect(visible, stepMs) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}

@Composable
fun UTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    number: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (minLines == 1) 1 else 12,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = U.c.faint) },
        minLines = minLines,
        maxLines = maxLines,
        singleLine = minLines == 1 && maxLines == 1,
        shape = U.RSm,
        textStyle = UType.Body,
        suffix = suffix?.let { { Text(it, color = U.c.muted) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = U.c.surface,
            unfocusedContainerColor = U.c.surface,
            unfocusedBorderColor = U.c.line,
            focusedBorderColor = U.c.rubric,
            cursorColor = U.c.rubric,
        ),
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        title = { Text(title, style = UType.Name) },
        text = { Text(text, style = UType.Sub) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) U.c.rubric else U.c.lapis, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

/** Clock-face picker; [minutes] after midnight in and out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minutes: Int, onDismiss: () -> Unit, onPicked: (Int) -> Unit) {
    val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.bg,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.save), color = U.c.rubric) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
    )
}

/** A day from today on, then a time. [initial] and the result are epoch millis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureDateTimeDialog(initial: Long, onDismiss: () -> Unit, withTime: Boolean = true, onPicked: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(initial).atZone(zone)
    var date by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now(zone)
    val d = date
    if (d == null) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: start.toLocalDate()
                    if (withTime) date = picked
                    else {
                        onPicked(picked.atTime(start.hour, start.minute).atZone(zone).toInstant().toEpochMilli())
                        onDismiss()
                    }
                }) { Text(stringResource(if (withTime) R.string.next else R.string.save), color = U.c.rubric) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
            colors = DatePickerDefaults.colors(containerColor = U.c.bg),
        ) { DatePicker(state, showModeToggle = false) }
    } else {
        TimeDialog(start.hour * 60 + start.minute, onDismiss) { m ->
            onPicked(d.atTime(m / 60, m % 60).atZone(zone).toInstant().toEpochMilli())
        }
    }
}

/** − value + */
@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.rubric) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        RoundIcon(Icons.Rounded.Remove, color, onMinus)
        Text(value, style = UType.Number.copy(fontSize = 22.sp, textAlign = TextAlign.Center), modifier = Modifier.width(92.dp), maxLines = 1)
        RoundIcon(Icons.Rounded.Add, color, onPlus)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(20.dp), tint = color) }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit, color: Color = U.c.rubric) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Body)
            if (sub != null) Text(sub, style = UType.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = color, checkedThumbColor = U.c.surface, uncheckedTrackColor = U.c.surface2, uncheckedBorderColor = U.c.line, uncheckedThumbColor = U.c.faint),
        )
    }
}

/** A choice among a few, drawn as a card with a tick. */
@Composable
fun SelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(if (selected) color.copy(alpha = 0.10f) else U.c.surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) color.copy(alpha = 0.7f) else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) { content() }
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = color, modifier = Modifier.size(22.dp))
    }
}

/** A small number with a caption, for stat rows. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = U.c.ink) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = UType.Number.copy(color = color), maxLines = 1)
        Text(label, style = UType.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}

/** Five steps of strength, 1..5. */
@Composable
fun ScalePicker(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, color: Color = U.c.rubric, ink: Color = U.c.ink, track: Color = U.c.surface) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { v ->
            val on = v <= value
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(U.RSm)
                    .background(if (on) color.copy(alpha = 0.12f + 0.12f * v) else track)
                    .border(1.dp, if (v == value) color else color.copy(alpha = 0.25f), U.RSm)
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center,
            ) {
                Text(v.toString(), style = UType.Button, color = if (on) ink else ink.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
}
