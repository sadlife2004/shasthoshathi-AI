package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DietPlanRecord
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MoodEntry
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.theme.TealDeep

enum class WellnessSubTab {
    DIET,
    MOOD,
    BMI_HYDRATION
}

@Composable
fun WellnessScreen(
    language: AppLanguage,
    activeSubTab: WellnessSubTab,
    onSelectSubTab: (WellnessSubTab) -> Unit,
    userAge: Int,
    dietGenState: UiState<DietPlanRecord>,
    latestSavedDiet: DietPlanRecord?,
    onGenerateDiet: (age: Int, conditions: List<String>, preference: String) -> Unit,
    moodGenState: UiState<MoodEntry>,
    moodEntries: List<MoodEntry>,
    onAnalyzeMood: (selectedMood: String, journalText: String) -> Unit,
    todayMetrics: HealthMetricsRecord?,
    latestBmiRecord: HealthMetricsRecord?,
    isGeneratingLifeSummary: Boolean,
    onSaveBmi: (heightCm: Double, weightKg: Double) -> Unit,
    onUpdateWater: (deltaMl: Int, newGoalMl: Int?) -> Unit,
    onGenerateLifeSummary: () -> Unit
) {
    val strings = stringsFor(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wellness_screen")
    ) {
        // Segmented Sub-Tab Bar (Local Diet Plan | Mood Journal | BMI & Water)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WellnessTabPill(
                    label = strings.dietTabLabel,
                    icon = Icons.Filled.Restaurant,
                    selected = activeSubTab == WellnessSubTab.DIET,
                    testTag = "wellness_subtab_diet",
                    onClick = { onSelectSubTab(WellnessSubTab.DIET) },
                    modifier = Modifier.weight(1f)
                )
                WellnessTabPill(
                    label = strings.moodTabLabel,
                    icon = Icons.Filled.SelfImprovement,
                    selected = activeSubTab == WellnessSubTab.MOOD,
                    testTag = "wellness_subtab_mood",
                    onClick = { onSelectSubTab(WellnessSubTab.MOOD) },
                    modifier = Modifier.weight(1f)
                )
                WellnessTabPill(
                    label = strings.bmiHydrationTabLabel,
                    icon = Icons.Filled.MonitorWeight,
                    selected = activeSubTab == WellnessSubTab.BMI_HYDRATION,
                    testTag = "wellness_subtab_bmi_hydration",
                    onClick = { onSelectSubTab(WellnessSubTab.BMI_HYDRATION) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                WellnessSubTab.DIET -> DietPlannerSection(
                    language = language,
                    initialAge = userAge,
                    dietGenState = dietGenState,
                    latestSavedPlan = latestSavedDiet,
                    onGeneratePlan = onGenerateDiet
                )
                WellnessSubTab.MOOD -> MoodJournalSection(
                    language = language,
                    moodGenState = moodGenState,
                    moodEntries = moodEntries,
                    onAnalyzeAndSave = onAnalyzeMood
                )
                WellnessSubTab.BMI_HYDRATION -> BmiHydrationSection(
                    language = language,
                    todayMetrics = todayMetrics,
                    latestBmiRecord = latestBmiRecord,
                    isGeneratingSummary = isGeneratingLifeSummary,
                    onSaveBmi = onSaveBmi,
                    onUpdateWater = onUpdateWater,
                    onGenerateLifeSummary = onGenerateLifeSummary
                )
            }
        }
    }
}

@Composable
private fun WellnessTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) TealDeep else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = " $label",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
