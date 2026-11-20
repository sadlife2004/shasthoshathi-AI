package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MoodEntry
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.ErrorRetryBanner
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealLight
import kotlinx.coroutines.delay

private data class MoodOption(
    val key: String,
    val labelEn: String,
    val labelBn: String,
    val icon: ImageVector,
    val bg: Color,
    val tint: Color
)

private val MoodOptions = listOf(
    MoodOption("HAPPY", "Happy", "খুশি", Icons.Filled.SentimentVerySatisfied, EmeraldContainer, EmeraldSuccess),
    MoodOption("CALM", "Calm", "শান্ত", Icons.Filled.Mood, TealContainer, TealDeep),
    MoodOption("TIRED", "Tired", "ক্লান্ত", Icons.Filled.SentimentNeutral, AmberContainer, AmberOnContainer),
    MoodOption("STRESSED", "Stressed", "চাপ", Icons.Filled.SentimentDissatisfied, Color(0xFFFFEDD5), AmberAccent),
    MoodOption("SAD", "Sad", "মন খারাপ", Icons.Filled.MoodBad, CoralContainer, CoralHeart)
)

@Composable
fun MoodJournalSection(
    language: AppLanguage,
    moodGenState: UiState<MoodEntry>,
    moodEntries: List<MoodEntry>,
    onAnalyzeAndSave: (selectedMood: String, journalText: String) -> Unit
) {
    val strings = stringsFor(language)
    var selectedMood by remember { mutableStateOf("CALM") }
    var diaryText by remember { mutableStateOf("") }

    val latestReflection = (moodGenState as? UiState.Success)?.data ?: moodEntries.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mood_journal_section"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SafetyDisclaimerBanner(language = language) }

        // 1. Mood Meter + Diary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.moodMeterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MoodOptions.forEach { option ->
                            val isSelected = selectedMood == option.key
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.12f else 0.96f,
                                animationSpec = tween(200),
                                label = "moodScale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedMood = option.key }
                                    .padding(4.dp)
                                    .testTag("mood_option_${option.key.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(option.bg)
                                        .then(
                                            if (isSelected) Modifier.border(2.5.dp, option.tint, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = option.labelEn,
                                        tint = option.tint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) option.labelBn else option.labelEn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) option.tint else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diaryText,
                        onValueChange = { diaryText = it },
                        placeholder = {
                            Text(strings.diaryPromptHint, style = MaterialTheme.typography.bodyMedium)
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mood_diary_input")
                    )

                    Button(
                        onClick = {
                            val textToSubmit = diaryText
                            onAnalyzeAndSave(selectedMood, textToSubmit)
                            diaryText = ""
                        },
                        enabled = diaryText.isNotBlank() && moodGenState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_mood_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        if (moodGenState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(strings.analyzingMood, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.analyzeSaveBtn, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (moodGenState is UiState.Error) {
            item {
                ErrorRetryBanner(
                    message = moodGenState.message,
                    onRetry = {
                        if (diaryText.isNotBlank()) onAnalyzeAndSave(selectedMood, diaryText)
                    },
                    language = language
                )
            }
        }

        // 2. AI Reflection Card
        if (latestReflection != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_mood_reflection_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, IndigoCalm.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Spa, contentDescription = null, tint = IndigoCalm)
                                Text(
                                    text = strings.aiReflectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IndigoCalm
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = IndigoContainer
                            ) {
                                Text(
                                    text = "${latestReflection.detectedMoodTag} • ${latestReflection.moodScore}/5",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoCalm,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = latestReflection.aiReflection,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TealContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = TealDeep, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = strings.microActionTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TealDeep
                                    )
                                    Text(
                                        text = latestReflection.microAction,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (latestReflection.supportNote.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberContainer
                            ) {
                                Text(
                                    text = latestReflection.supportNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberOnContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Guided 4-7-8 Breathing Interactive Timer Card
        item {
            BreathingExerciseCard(language = language)
        }

        // 4. Weekly Mood Trend Area/Line Chart
        item {
            MoodTrendChartCard(
                entries = moodEntries.take(7).reversed(),
                language = language
            )
        }

        // 5. Journal History List
        if (moodEntries.isNotEmpty()) {
            item {
                Text(
                    text = strings.journalHistoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(moodEntries, key = { it.id.ifBlank { "${it.dateLabel}_${it.journalText.hashCode()}" } }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.dateLabel} • ${entry.detectedMoodTag}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TealDeep
                            )
                            Surface(shape = RoundedCornerShape(50), color = TealContainer) {
                                Text(
                                    text = "${entry.moodScore}/5",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealDeep,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = entry.journalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = entry.aiReflection,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun BreathingExerciseCard(language: AppLanguage) {
    val strings = stringsFor(language)
    var running by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) } // 0=Inhale 4s, 1=Hold 7s, 2=Exhale 8s
    var secondsLeft by remember { mutableIntStateOf(4) }

    LaunchedEffect(running) {
        if (!running) {
            phaseIndex = 0
            secondsLeft = 4
            return@LaunchedEffect
        }
        val durations = listOf(4, 7, 8)
        while (running) {
            val dur = durations[phaseIndex]
            for (sec in dur downTo 1) {
                secondsLeft = sec
                delay(1000L)
            }
            phaseIndex = (phaseIndex + 1) % 3
        }
    }

    val phaseText = when (phaseIndex) {
        0 -> strings.inhaleText
        1 -> strings.holdText
        else -> strings.exhaleText
    }
    val targetScale = when {
        !running -> 1f
        phaseIndex == 0 -> 1.28f
        phaseIndex == 1 -> 1.28f
        else -> 0.86f
    }
    val animScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = if (phaseIndex == 2) 1500 else 1000),
        label = "breathCircle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("breathing_exercise_card"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_calm_breathing_1795106965753),
                contentDescription = "Calming breathing exercise backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B4F4A).copy(alpha = 0.45f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Air, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = strings.breathingExerciseTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(animScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (running) "${secondsLeft}s" else "4-7-8",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (running) {
                            Text(
                                text = phaseText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Button(
                    onClick = { running = !running },
                    modifier = Modifier.testTag("breathing_toggle_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TealDeep
                    )
                ) {
                    Icon(
                        imageVector = if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (running) strings.stopBreathing else strings.startBreathing,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodTrendChartCard(
    entries: List<MoodEntry>,
    language: AppLanguage
) {
    val strings = stringsFor(language)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mood_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = TealDeep, modifier = Modifier.size(20.dp))
                Text(
                    text = strings.weeklyMoodTrend,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(14.dp))

            if (entries.isEmpty()) {
                Text(
                    text = strings.noMoodYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scores = entries.map { it.moodScore.toFloat().coerceIn(1f, 5f) }
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Horizontal guide lines (1 to 5)
                    for (level in 0..4) {
                        val y = h - (level / 4f) * h
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5f
                        )
                    }

                    val stepX = if (scores.size > 1) w / (scores.size - 1) else w / 2f
                    val points = scores.mapIndexed { idx, score ->
                        val x = if (scores.size > 1) idx * stepX else w / 2f
                        val y = h - ((score - 1f) / 4f) * h
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        val areaPath = Path().apply {
                            moveTo(points.first().x, h)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, h)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(TealLight.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = TealDeep,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }

                    points.forEach { pt ->
                        drawCircle(color = Color.White, radius = 10f, center = pt)
                        drawCircle(color = TealDeep, radius = 7f, center = pt)
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    entries.forEach { entry ->
                        Text(
                            text = entry.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
