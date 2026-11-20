package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.HealthMetricsRecord
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
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
import java.util.Locale
import kotlin.math.round

@Composable
fun BmiHydrationSection(
    language: AppLanguage,
    todayMetrics: HealthMetricsRecord?,
    latestBmiRecord: HealthMetricsRecord?,
    isGeneratingSummary: Boolean,
    onSaveBmi: (heightCm: Double, weightKg: Double) -> Unit,
    onUpdateWater: (deltaMl: Int, newGoalMl: Int?) -> Unit,
    onGenerateLifeSummary: () -> Unit
) {
    val strings = stringsFor(language)
    val initialHeight = (todayMetrics?.heightCm?.takeIf { it > 0 } ?: latestBmiRecord?.heightCm?.takeIf { it > 0 } ?: 168.0)
    val initialWeight = (todayMetrics?.weightKg?.takeIf { it > 0 } ?: latestBmiRecord?.weightKg?.takeIf { it > 0 } ?: 65.0)

    var useFeetInches by remember { mutableStateOf(false) }
    var heightCmText by remember(initialHeight) { mutableStateOf(String.format(Locale.US, "%.0f", initialHeight)) }
    var feetText by remember(initialHeight) {
        val totalInches = initialHeight / 2.54
        mutableStateOf((totalInches.toInt() / 12).toString())
    }
    var inchesText by remember(initialHeight) {
        val totalInches = initialHeight / 2.54
        mutableStateOf((totalInches.toInt() % 12).toString())
    }
    var weightKgText by remember(initialWeight) { mutableStateOf(String.format(Locale.US, "%.1f", initialWeight)) }

    val resolvedHeightCm = if (useFeetInches) {
        val ft = feetText.toDoubleOrNull() ?: 0.0
        val inch = inchesText.toDoubleOrNull() ?: 0.0
        (ft * 30.48) + (inch * 2.54)
    } else {
        heightCmText.toDoubleOrNull() ?: 0.0
    }
    val resolvedWeightKg = weightKgText.toDoubleOrNull() ?: 0.0
    val liveBmi = if (resolvedHeightCm > 30.0 && resolvedWeightKg > 2.0) {
        val m = resolvedHeightCm / 100.0
        round((resolvedWeightKg / (m * m)) * 10.0) / 10.0
    } else {
        0.0
    }

    val waterIntakeMl = todayMetrics?.waterIntakeMl ?: 0L
    val waterGoalMl = (todayMetrics?.waterGoalMl ?: latestBmiRecord?.waterGoalMl ?: 2500L).coerceAtLeast(500L)
    val hydrationFraction by animateFloatAsState(
        targetValue = (waterIntakeMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f),
        label = "hydration_progress"
    )
    val glassesCount = (waterIntakeMl / 250L).toInt()
    val summaryRecord = todayMetrics?.takeIf { it.lifeConditionSummary.isNotBlank() }
        ?: latestBmiRecord?.takeIf { it.lifeConditionSummary.isNotBlank() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bmi_hydration_section"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SafetyDisclaimerBanner(language = language) }

        // 1. BMI Calculator Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bmi_calculator_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = TealDeep)
                            Text(
                                text = strings.bmiCardTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !useFeetInches,
                                onClick = { useFeetInches = false },
                                label = { Text("cm", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealDeep,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = useFeetInches,
                                onClick = { useFeetInches = true },
                                label = { Text("ft/in", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealDeep,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!useFeetInches) {
                            OutlinedTextField(
                                value = heightCmText,
                                onValueChange = { heightCmText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                                label = { Text(strings.heightCmLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bmi_height_cm_input")
                            )
                        } else {
                            OutlinedTextField(
                                value = feetText,
                                onValueChange = { feetText = it.filter { ch -> ch.isDigit() }.take(1) },
                                label = { Text(strings.heightFeetLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.55f)
                                    .testTag("bmi_height_ft_input")
                            )
                            OutlinedTextField(
                                value = inchesText,
                                onValueChange = { inchesText = it.filter { ch -> ch.isDigit() }.take(2) },
                                label = { Text(strings.heightInchesLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.55f)
                                    .testTag("bmi_height_in_input")
                            )
                        }
                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                            label = { Text(strings.weightKgLabel) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bmi_weight_kg_input")
                        )
                    }

                    if (liveBmi > 0.0) {
                        val (catLabel, badgeBg, badgeFg) = bmiVisualStyle(liveBmi, language)
                        val bmiGaugeProgress = ((liveBmi.toFloat() - 12f) / 28f).coerceIn(0.05f, 1f)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = badgeBg.copy(alpha = 0.65f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BMI ${String.format(Locale.US, "%.1f", liveBmi)}",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = badgeFg
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = badgeFg
                                    ) {
                                        Text(
                                            text = catLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { bmiGaugeProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = badgeFg,
                                    trackColor = Color.White.copy(alpha = 0.7f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = strings.healthyRangeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeFg
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onSaveBmi(resolvedHeightCm, resolvedWeightKg) },
                        enabled = liveBmi > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_bmi_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Text(strings.calculateSaveBmiBtn, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 2. Daily Hydration Tracker Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hydration_tracker_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = IndigoCalm)
                            Text(
                                text = strings.hydrationCardTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = IndigoContainer
                        ) {
                            Text(
                                text = "${(hydrationFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = IndigoCalm,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "$waterIntakeMl ml",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = IndigoCalm
                            )
                            Text(
                                text = "$glassesCount ${strings.glassesLabel} • ${strings.dailyGoalLabel}: $waterGoalMl ml",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { hydrationFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(50)),
                        color = IndigoCalm,
                        trackColor = IndigoContainer,
                        strokeCap = StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateWater(-250, null) },
                            enabled = waterIntakeMl > 0L,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("water_minus_250_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Remove 250ml", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("250ml", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onUpdateWater(250, null) },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("water_plus_250_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoCalm)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add 250ml", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+250ml", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onUpdateWater(500, null) },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("water_plus_500_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = "Add 500ml", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+500ml", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Daily Water Goal Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.dailyGoalLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2000, 2500, 3000, 3500).forEach { goalOption ->
                                FilterChip(
                                    selected = waterGoalMl == goalOption.toLong(),
                                    onClick = { onUpdateWater(0, goalOption) },
                                    label = { Text("${goalOption / 1000.0}L", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoCalm,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. AI "My Life Condition Summary" Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("life_condition_summary_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, TealDeep.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = TealDeep, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = strings.lifeConditionCardTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.lifeConditionCardSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (summaryRecord != null && summaryRecord.lifeConditionScore > 0L) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "${summaryRecord.lifeConditionScore}/100",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (summaryRecord != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = TealContainer.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = summaryRecord.lifeConditionTitle.ifBlank { strings.lifeConditionCardTitle },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealDeep
                                )
                                Text(
                                    text = summaryRecord.lifeConditionSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (summaryRecord.lifeConditionTips.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = strings.dailyRecommendationsLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                summaryRecord.lifeConditionTips.forEach { tip ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                        )
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isGeneratingSummary) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = TealDeep
                            )
                            Text(
                                text = strings.generatingLifeSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TealDeep
                            )
                        }
                    }

                    Button(
                        onClick = onGenerateLifeSummary,
                        enabled = !isGeneratingSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_life_condition_summary_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (summaryRecord != null) strings.refreshLifeSummaryBtn else strings.generateLifeSummaryBtn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun bmiVisualStyle(bmi: Double, language: AppLanguage): Triple<String, Color, Color> {
    val strings = stringsFor(language)
    return when {
        bmi < 18.5 -> Triple(strings.bmiUnderweight, IndigoContainer, IndigoCalm)
        bmi < 25.0 -> Triple(strings.bmiNormal, EmeraldContainer, EmeraldSuccess)
        bmi < 30.0 -> Triple(strings.bmiOverweight, AmberContainer, AmberOnContainer)
        else -> Triple(strings.bmiObese, CoralContainer, CoralHeart)
    }
}
