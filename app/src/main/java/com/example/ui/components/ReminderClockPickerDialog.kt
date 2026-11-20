package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderClockPickerDialog(
    initialTimeLabel: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val strings = stringsFor(language)
    val (initHour, initMinute) = remember(initialTimeLabel) { parseClockTime(initialTimeLabel) }
    val timePickerState = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMinute,
        is24Hour = false
    )
    var useKeyboardInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("reminder_clock_picker_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.setClockTimeTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { useKeyboardInput = !useKeyboardInput },
                        modifier = Modifier.testTag("clock_picker_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (useKeyboardInput) Icons.Filled.AccessTime else Icons.Filled.Keyboard,
                            contentDescription = if (useKeyboardInput) {
                                strings.switchToClockDial
                            } else {
                                strings.switchToKeyboardInput
                            },
                            tint = TealDeep
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                val pickerColors = TimePickerDefaults.colors(
                    clockDialSelectedContentColor = Color.White,
                    selectorColor = TealDeep,
                    periodSelectorSelectedContainerColor = TealContainer,
                    periodSelectorSelectedContentColor = TealDeep,
                    timeSelectorSelectedContainerColor = TealContainer,
                    timeSelectorSelectedContentColor = TealDeep
                )

                if (useKeyboardInput) {
                    TimeInput(
                        state = timePickerState,
                        colors = pickerColors
                    )
                } else {
                    TimePicker(
                        state = timePickerState,
                        colors = pickerColors
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("clock_picker_cancel_button")
                    ) {
                        Text(strings.cancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTimeSelected(formatClockTime(timePickerState.hour, timePickerState.minute))
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep),
                        modifier = Modifier.testTag("clock_picker_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.confirmTimeBtn)
                    }
                }
            }
        }
    }
}

fun formatClockTime(hour24: Int, minute: Int): String {
    val safeHour24 = hour24.coerceIn(0, 23)
    val safeMin = minute.coerceIn(0, 59)
    val amPm = if (safeHour24 < 12) "AM" else "PM"
    val hour12 = when {
        safeHour24 == 0 -> 12
        safeHour24 > 12 -> safeHour24 - 12
        else -> safeHour24
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, safeMin, amPm)
}

fun parseClockTime(timeLabel: String): Pair<Int, Int> {
    val cleaned = timeLabel.trim().uppercase(Locale.US)
    val match = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""").matchEntire(cleaned) ?: return 8 to 30
    val rawHour = match.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: return 8 to 30
    val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
    val amPm = match.groupValues[3]
    val hour24 = when {
        amPm == "AM" && rawHour == 12 -> 0
        amPm == "PM" && rawHour != 12 -> rawHour + 12
        else -> rawHour
    }
    return hour24 to minute
}

fun clockSlotLabel(timeLabel: String, language: AppLanguage): String {
    val (hour24, _) = parseClockTime(timeLabel)
    val strings = stringsFor(language)
    return when (hour24) {
        in 4..11 -> strings.filterMorning
        in 12..17 -> strings.filterNoon
        else -> strings.filterNight
    }
}
