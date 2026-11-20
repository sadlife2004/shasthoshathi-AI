package com.example.ui.components

import androidx.compose.ui.platform.LocalContext
import com.example.alarm.AlarmSoundPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MedicineReminder
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealPrimary

@Composable
fun SafetyDisclaimerBanner(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = stringsFor(language)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_disclaimer_banner"),
        shape = RoundedCornerShape(12.dp),
        color = AmberContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.HealthAndSafety,
                contentDescription = "Medical guidance disclaimer",
                tint = AmberOnContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = strings.safetyBanner,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = AmberOnContainer
            )
        }
    }
}

@Composable
fun LanguageTogglePill(
    language: AppLanguage,
    onToggle: () -> Unit,
    darkSurface: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerBg = if (darkSurface) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
    val borderCol = if (darkSurface) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(containerBg)
            .border(1.dp, borderCol, RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .padding(3.dp)
            .testTag("language_toggle_pill"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageChipPart(
            label = "EN",
            selected = language == AppLanguage.EN,
            darkSurface = darkSurface
        )
        LanguageChipPart(
            label = "বাংলা",
            selected = language == AppLanguage.BN,
            darkSurface = darkSurface
        )
    }
}

@Composable
private fun LanguageChipPart(
    label: String,
    selected: Boolean,
    darkSurface: Boolean
) {
    val bg by animateColorAsState(
        targetValue = when {
            selected && darkSurface -> Color.White
            selected -> TealDeep
            else -> Color.Transparent
        },
        label = "langBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected && darkSurface -> TealDeep
            selected -> Color.White
            darkSurface -> Color.White.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "langText"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun ErrorRetryBanner(
    message: String,
    onRetry: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = stringsFor(language)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.testTag("error_retry_button"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(strings.retryBtn, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun MedicineAlarmModal(
    reminder: MedicineReminder,
    language: AppLanguage,
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current

    // Play the reminder's selected alarm ringtone (or system default alarm) while modal is open
    DisposableEffect(reminder.id, reminder.ringtoneUri) {
        AlarmSoundPlayer.play(context, reminder.ringtoneUri)
        onDispose {
            AlarmSoundPlayer.stop()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "alarmPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("medicine_alarm_modal"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(TealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = "Medicine Reminder Alarm",
                        tint = TealDeep,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = strings.alarmModalTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.alarmModalSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Medication,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = reminder.medicineName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TealContainer
                            ) {
                                Text(
                                    text = "${reminder.dosage} • ${reminder.scheduledTime}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TealDeep,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = AmberContainer
                            ) {
                                Text(
                                    text = reminder.mealTiming,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberOnContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
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
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = reminder.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        AlarmSoundPlayer.stop()
                        onMarkTaken()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("alarm_mark_taken_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.markTaken, style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        AlarmSoundPlayer.stop()
                        onSnooze()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("alarm_snooze_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.snooze10m, style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = {
                        AlarmSoundPlayer.stop()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("alarm_dismiss_button")
                ) {
                    Text(strings.dismiss, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
