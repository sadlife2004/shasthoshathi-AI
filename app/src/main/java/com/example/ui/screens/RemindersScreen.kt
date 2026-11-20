package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmSoundPlayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MedicineReminder
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.ReminderClockPickerDialog
import com.example.ui.components.clockSlotLabel
import com.example.ui.components.formatClockTime
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    language: AppLanguage,
    reminders: List<MedicineReminder>,
    onToggleActive: (MedicineReminder, Boolean) -> Unit,
    onMarkTaken: (MedicineReminder) -> Unit,
    onSnooze: (MedicineReminder) -> Unit,
    onSkip: (MedicineReminder) -> Unit,
    onDelete: (MedicineReminder) -> Unit,
    onTestAlarmNow: () -> Unit,
    onAddManual: (
        name: String,
        dosage: String,
        scheduledTimes: List<String>,
        mealTiming: String,
        durationDays: Int,
        notes: String,
        ringtoneUri: String,
        ringtoneName: String
    ) -> Unit,
    onEditReminder: (
        existing: MedicineReminder,
        name: String,
        dosage: String,
        scheduledTime: String,
        mealTiming: String,
        durationDays: Int,
        notes: String,
        ringtoneUri: String,
        ringtoneName: String
    ) -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedSlot by remember { mutableStateOf("ALL") } // ALL, MORNING, NOON, NIGHT
    var showAddSheet by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<MedicineReminder?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val weekDays = remember {
        val cal = Calendar.getInstance()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val numFmt = SimpleDateFormat("dd", Locale.US)
        (0..6).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            dayFmt.format(c.time) to numFmt.format(c.time)
        }
    }

    val filtered = remember(reminders, selectedSlot) {
        if (selectedSlot == "ALL") reminders
        else reminders.filter { it.timeSlot == selectedSlot }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reminders_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Test Alarm Now Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.remindersHeader,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onTestAlarmNow)
                            .testTag("test_alarm_now_button"),
                        color = AmberContainer,
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = strings.testAlarmNow,
                                tint = AmberOnContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.testAlarmNow,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberOnContainer
                            )
                        }
                    }
                }
            }

            // Background Alarm & Notification Status Banner
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_notification_permission_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = AmberContainer,
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = AmberOnContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.enableAlarmNotificationsTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberOnContainer
                                )
                                Text(
                                    text = strings.enableAlarmNotificationsSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberOnContainer
                                )
                            }
                            Button(
                                onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                            ) {
                                Text(strings.allowNotificationsBtn, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldContainer.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.backgroundAlarmActiveNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }

            // 7-Day Horizontal Calendar Strip
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(weekDays) { index, (dayName, dateNum) ->
                        val isSelected = selectedDayIndex == index
                        Surface(
                            modifier = Modifier
                                .width(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedDayIndex = index },
                            color = if (isSelected) TealDeep else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) TealDeep else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = dateNum,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Morning / Noon / Night Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to strings.filterAll,
                        "MORNING" to strings.filterMorning,
                        "NOON" to strings.filterNoon,
                        "NIGHT" to strings.filterNight
                    ).forEach { (slotKey, label) ->
                        FilterChip(
                            selected = selectedSlot == slotKey,
                            onClick = { selectedSlot = slotKey },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealDeep,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Medication,
                                contentDescription = null,
                                tint = TealDeep,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = strings.addFirstMedicineHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { showAddSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealDeep),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("empty_add_manual_medicine_button")
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.addManualMedicine)
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        language = language,
                        onToggleActive = { onToggleActive(reminder, it) },
                        onMarkTaken = { onMarkTaken(reminder) },
                        onSnooze = { onSnooze(reminder) },
                        onSkip = { onSkip(reminder) },
                        onEdit = { editingReminder = reminder },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }
        }

        // Floating Action Button: "+ Add Manual Medicine"
        FloatingActionButton(
            onClick = { showAddSheet = true },
            containerColor = TealDeep,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_manual_medicine_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.addManualMedicine)
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                AlarmSoundPlayer.stop()
                showAddSheet = false
            },
            sheetState = sheetState
        ) {
            AddManualMedicineForm(
                language = language,
                onSave = { name, dosage, times, meal, days, notes, ringtoneUri, ringtoneName ->
                    AlarmSoundPlayer.stop()
                    onAddManual(name, dosage, times, meal, days, notes, ringtoneUri, ringtoneName)
                    showAddSheet = false
                }
            )
        }
    }

    editingReminder?.let { existing ->
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                AlarmSoundPlayer.stop()
                editingReminder = null
            },
            sheetState = editSheetState
        ) {
            EditManualReminderForm(
                existing = existing,
                language = language,
                onSave = { name, dosage, time, meal, days, notes, ringtoneUri, ringtoneName ->
                    AlarmSoundPlayer.stop()
                    onEditReminder(existing, name, dosage, time, meal, days, notes, ringtoneUri, ringtoneName)
                    editingReminder = null
                }
            )
        }
    }
}

@Composable
private fun ReminderItemCard(
    reminder: MedicineReminder,
    language: AppLanguage,
    onToggleActive: (Boolean) -> Unit,
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = stringsFor(language)
    val isAutoRx = reminder.sourceType == "AUTO_RX"

    val statusText = when (reminder.todayStatus) {
        "TAKEN" -> strings.statusTaken
        "SNOOZED" -> strings.statusSnoozed
        "SKIPPED" -> strings.statusSkipped
        else -> strings.statusPending
    }
    val statusBg = when (reminder.todayStatus) {
        "TAKEN" -> EmeraldContainer
        "SNOOZED" -> AmberContainer
        "SKIPPED" -> MaterialTheme.colorScheme.surfaceVariant
        else -> TealContainer
    }
    val statusFg = when (reminder.todayStatus) {
        "TAKEN" -> EmeraldSuccess
        "SNOOZED" -> AmberOnContainer
        "SKIPPED" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> TealDeep
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAutoRx) IndigoContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isAutoRx) strings.autoRxTag else strings.manualTag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAutoRx) IndigoCalm else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    // Tappable Clock Badge + Dosage & Meal Timing
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onEdit),
                            color = TealContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = strings.setClockTimeTitle,
                                    tint = TealDeep,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = reminder.scheduledTime,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TealDeep
                                )
                            }
                        }
                        Text(
                            text = "${reminder.dosage} • ${reminder.mealTiming}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = reminder.active,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TealDeep
                        )
                    )
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_reminder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = strings.editReminderTitle,
                                tint = TealDeep
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete reminder",
                                tint = CoralHeart.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (reminder.ringtoneName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = reminder.ringtoneName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TealDeep
                    )
                }
            }

            if (reminder.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkTaken,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.markTaken, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Filled.Snooze,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.snooze10m, style = MaterialTheme.typography.labelMedium, color = AmberAccent)
                }

                OutlinedButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(strings.skip, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun AddManualMedicineForm(
    language: AppLanguage,
    onSave: (String, String, List<String>, String, Int, String, String, String) -> Unit
) {
    val strings = stringsFor(language)
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("1 Tablet") }
    var clockTimes by remember { mutableStateOf(listOf("08:30 AM", "09:30 PM")) }
    var mealTiming by remember { mutableStateOf("After Meal") }
    var durationStr by remember { mutableStateOf("7") }
    var notes by remember { mutableStateOf("") }
    var ringtoneUri by remember { mutableStateOf("") }
    var ringtoneName by remember { mutableStateOf(strings.defaultRingtoneName) }

    // Clock picker target: null = closed, -1 = adding new time, >= 0 = editing existing index
    var clockPickerTargetIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.addManualMedicine,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(strings.medicineNameLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_med_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(strings.dosageLabel) },
                singleLine = true,
                modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
                value = durationStr,
                onValueChange = { durationStr = it.filter { c -> c.isDigit() } },
                label = { Text(strings.durationDaysLabel) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Dosage Presets
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("1 Tablet", "1 Capsule", "1/2 Tablet", "5 ml", "10 ml")) { preset ->
                FilterChip(
                    selected = dosage.equals(preset, ignoreCase = true),
                    onClick = { dosage = preset },
                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealContainer,
                        selectedLabelColor = TealDeep
                    )
                )
            }
        }

        // Full Manual Clock Schedule Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.reminderClockTimesLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = { clockPickerTargetIndex = -1 },
                    enabled = clockTimes.size < 8,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_clock_time_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealDeep)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addDoseTimeBtn, style = MaterialTheme.typography.labelMedium, color = TealDeep)
                }
            }

            Text(
                text = strings.tapTimeToChangeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick Schedule Presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = clockTimes.size == 1,
                    onClick = { clockTimes = listOf("08:30 AM") },
                    label = { Text(strings.presetOnceDaily, style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = clockTimes == listOf("08:30 AM", "09:30 PM"),
                    onClick = { clockTimes = listOf("08:30 AM", "09:30 PM") },
                    label = { Text(strings.presetTwiceDaily, style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = clockTimes == listOf("08:30 AM", "01:30 PM", "09:30 PM"),
                    onClick = { clockTimes = listOf("08:30 AM", "01:30 PM", "09:30 PM") },
                    label = { Text(strings.presetThriceDaily, style = MaterialTheme.typography.labelSmall) }
                )
            }

            // Editable Clock Time Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                clockTimes.forEachIndexed { index, timeLabel ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { clockPickerTargetIndex = index }
                            .testTag("clock_time_row_$index"),
                        color = TealContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TealDeep),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TealDeep
                                    )
                                    Text(
                                        text = clockSlotLabel(timeLabel, language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = strings.setClockTimeTitle,
                                    tint = TealDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (clockTimes.size > 1) {
                                    IconButton(
                                        onClick = {
                                            clockTimes = clockTimes.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.testTag("remove_clock_time_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove dose time",
                                            tint = CoralHeart
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.mealTimingLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Before Meal", "After Meal", "Anytime").forEach { option ->
                    FilterChip(
                        selected = mealTiming == option,
                        onClick = { mealTiming = option },
                        label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealDeep,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        RingtoneSelectorSection(
            language = language,
            selectedUri = ringtoneUri,
            selectedName = ringtoneName,
            onRingtonePicked = { uri, title ->
                ringtoneUri = uri
                ringtoneName = title
            }
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(strings.notesLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (name.isNotBlank() && clockTimes.isNotEmpty()) {
                    onSave(
                        name,
                        dosage,
                        clockTimes,
                        mealTiming,
                        durationStr.toIntOrNull()?.coerceIn(1, 365) ?: 7,
                        notes,
                        ringtoneUri,
                        ringtoneName
                    )
                }
            },
            enabled = name.isNotBlank() && clockTimes.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_manual_medicine_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
        ) {
            Text(strings.saveMedicineBtn, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
    }

    clockPickerTargetIndex?.let { targetIdx ->
        val initialTime = if (targetIdx >= 0 && targetIdx < clockTimes.size) {
            clockTimes[targetIdx]
        } else {
            val now = Calendar.getInstance()
            formatClockTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }
        ReminderClockPickerDialog(
            initialTimeLabel = initialTime,
            language = language,
            onDismiss = { clockPickerTargetIndex = null },
            onTimeSelected = { chosenTime ->
                clockTimes = if (targetIdx >= 0 && targetIdx < clockTimes.size) {
                    clockTimes.mapIndexed { idx, old -> if (idx == targetIdx) chosenTime else old }
                        .distinct()
                } else {
                    (clockTimes + chosenTime).distinct()
                }
                clockPickerTargetIndex = null
            }
        )
    }
}

@Composable
private fun EditManualReminderForm(
    existing: MedicineReminder,
    language: AppLanguage,
    onSave: (String, String, String, String, Int, String, String, String) -> Unit
) {
    val strings = stringsFor(language)
    var name by remember(existing.id) { mutableStateOf(existing.medicineName) }
    var dosage by remember(existing.id) { mutableStateOf(existing.dosage) }
    var scheduledTime by remember(existing.id) { mutableStateOf(existing.scheduledTime) }
    var mealTiming by remember(existing.id) { mutableStateOf(existing.mealTiming) }
    var durationStr by remember(existing.id) { mutableStateOf(existing.durationDays.toString()) }
    var notes by remember(existing.id) { mutableStateOf(existing.notes) }
    var ringtoneUri by remember(existing.id) { mutableStateOf(existing.ringtoneUri) }
    var ringtoneName by remember(existing.id) {
        mutableStateOf(existing.ringtoneName.ifBlank { strings.defaultRingtoneName })
    }
    var showClockPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.editReminderTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(strings.medicineNameLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_reminder_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(strings.dosageLabel) },
                singleLine = true,
                modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
                value = durationStr,
                onValueChange = { durationStr = it.filter { c -> c.isDigit() } },
                label = { Text(strings.durationDaysLabel) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Single Clock Time Selector Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = strings.reminderClockTimesLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showClockPicker = true }
                    .testTag("edit_reminder_clock_button"),
                color = TealContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TealDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = scheduledTime,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TealDeep
                            )
                            Text(
                                text = clockSlotLabel(scheduledTime, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = strings.setClockTimeTitle,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealDeep
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.mealTimingLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Before Meal", "After Meal", "Anytime").forEach { option ->
                    FilterChip(
                        selected = mealTiming == option,
                        onClick = { mealTiming = option },
                        label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealDeep,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        RingtoneSelectorSection(
            language = language,
            selectedUri = ringtoneUri,
            selectedName = ringtoneName,
            onRingtonePicked = { uri, title ->
                ringtoneUri = uri
                ringtoneName = title
            }
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(strings.notesLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        name,
                        dosage,
                        scheduledTime,
                        mealTiming,
                        durationStr.toIntOrNull()?.coerceIn(1, 365) ?: existing.durationDays.toInt(),
                        notes,
                        ringtoneUri,
                        ringtoneName
                    )
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_edited_reminder_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
        ) {
            Text(strings.saveChanges, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showClockPicker) {
        ReminderClockPickerDialog(
            initialTimeLabel = scheduledTime,
            language = language,
            onDismiss = { showClockPicker = false },
            onTimeSelected = { picked ->
                scheduledTime = picked
                showClockPicker = false
            }
        )
    }
}

@Composable
private fun RingtoneSelectorSection(
    language: AppLanguage,
    selectedUri: String,
    selectedName: String,
    onRingtonePicked: (uri: String, title: String) -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    var isPreviewing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            AlarmSoundPlayer.stop()
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val uriStr = pickedUri?.toString().orEmpty()
            val title = if (uriStr.isNotBlank()) {
                AlarmSoundPlayer.resolveRingtoneTitle(context, uriStr)
            } else {
                strings.defaultRingtoneName
            }
            onRingtonePicked(uriStr, title)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = strings.alarmRingtoneLabel,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = TealDeep,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = selectedName.ifBlank { strings.defaultRingtoneName },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (isPreviewing) {
                            AlarmSoundPlayer.stop()
                            isPreviewing = false
                        } else {
                            AlarmSoundPlayer.play(context, selectedUri, autoStopMs = 6_000L)
                            isPreviewing = true
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("preview_ringtone_button")
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPreviewing) strings.stopPreviewBtn else strings.previewRingtoneBtn,
                        style = MaterialTheme.typography.labelSmall,
                        color = TealDeep
                    )
                }
                Button(
                    onClick = {
                        AlarmSoundPlayer.stop()
                        isPreviewing = false
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, strings.alarmRingtoneLabel)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (selectedUri.isNotBlank()) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedUri))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep),
                    modifier = Modifier.testTag("choose_ringtone_button")
                ) {
                    Text(strings.chooseRingtoneBtn, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
