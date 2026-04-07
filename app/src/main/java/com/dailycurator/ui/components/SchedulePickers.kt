package com.dailycurator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

private val systemZone: ZoneId get() = ZoneId.systemDefault()

fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(systemZone).toInstant().toEpochMilli()

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(systemZone).toLocalDate()

/** Rounded to 5 minutes, e.g. "1h 30m", "45m". */
fun formatDurationMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return buildString {
        if (h > 0) append("${h}h")
        if (m > 0) {
            if (h > 0) append(" ")
            append("${m}m")
        }
        if (isEmpty()) append("0m")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(
    visible: Boolean,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    if (!visible) return
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localDateToMillis(initialDate),
        initialDisplayMode = DisplayMode.Input,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: return@TextButton
                onConfirm(millisToLocalDate(millis))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTimeDialog(
    visible: Boolean,
    initialTime: LocalTime,
    title: String = "Start time",
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    if (!visible) return
    val timeState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 400.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    String.format("%02d:%02d", timeState.hour, timeState.minute),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Scroll hours and minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TimePicker(
                    state = timeState,
                    layoutType = TimePickerLayoutType.Vertical,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(LocalTime.of(timeState.hour, timeState.minute))
                            onDismiss()
                        },
                    ) { Text("OK") }
                }
            }
        }
    }
}

private val CUSTOM_DURATION_SUGGESTION_MINUTES = listOf(15, 25, 30, 40, 45, 60, 75, 90, 120, 180, 240)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickCustomDurationDialog(
    visible: Boolean,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    if (!visible) return
    var value by remember(initialMinutes) { mutableFloatStateOf(initialMinutes.coerceIn(15, 480).toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom duration", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(
                    formatDurationMinutes(value.roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Suggestions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CUSTOM_DURATION_SUGGESTION_MINUTES.forEach { m ->
                        FilterChip(
                            selected = value.roundToInt() == m,
                            onClick = { value = m.toFloat().coerceIn(15f, 480f) },
                            label = { Text(formatDurationMinutes(m)) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 15f..480f,
                )
                Text(
                    "15 min — 8 h (snap to 5 min on save)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val snapped = ((value / 5f).roundToInt() * 5).coerceIn(15, 480)
                onConfirm(snapped)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class DurationPreset(val label: String, val minutes: Int)

val DEFAULT_DURATION_PRESETS: List<DurationPreset> = listOf(
    DurationPreset("15m", 15),
    DurationPreset("30m", 30),
    DurationPreset("45m", 45),
    DurationPreset("1h", 60),
    DurationPreset("1h 30m", 90),
    DurationPreset("2h", 120),
    DurationPreset("3h", 180),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPresetSelector(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<DurationPreset> = DEFAULT_DURATION_PRESETS,
    /** Single row 10m / 20m / Custom for task editor; habits/goals use full [presets]. */
    useCompactTaskRow: Boolean = false,
) {
    var showCustom by remember { mutableStateOf(false) }
    val compactPresets = remember {
        listOf(DurationPreset("10m", 10), DurationPreset("20m", 20))
    }
    Column(modifier) {
        Text("Duration", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        if (useCompactTaskRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                compactPresets.forEach { p ->
                    FilterChip(
                        selected = selectedMinutes == p.minutes && !showCustom,
                        onClick = {
                            showCustom = false
                            onMinutesSelected(p.minutes)
                        },
                        label = { Text(p.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                FilterChip(
                    selected = showCustom || compactPresets.none { it.minutes == selectedMinutes },
                    onClick = { showCustom = true },
                    label = { Text("Custom") },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presets.forEach { p ->
                    FilterChip(
                        selected = selectedMinutes == p.minutes,
                        onClick = { onMinutesSelected(p.minutes) },
                        label = { Text(p.label) },
                    )
                }
                FilterChip(
                    selected = showCustom || presets.none { it.minutes == selectedMinutes },
                    onClick = { showCustom = true },
                    label = { Text("Custom") },
                )
            }
        }
        PickCustomDurationDialog(
            visible = showCustom,
            initialMinutes = selectedMinutes,
            onDismiss = { showCustom = false },
            onConfirm = {
                onMinutesSelected(it)
                showCustom = false
            },
        )
    }
}

@Composable
fun NumericTargetStepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) }
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(
                if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) }
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
fun OutlinedPickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text)
    }
}
