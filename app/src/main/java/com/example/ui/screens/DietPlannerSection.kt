package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DietPlanRecord
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
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep

private val ConditionOptions = listOf(
    "Diabetes" to "ডায়াবেটিস",
    "High BP" to "উচ্চ রক্তচাপ",
    "Gastric" to "গ্যাস্ট্রিক",
    "General Wellness" to "সাধারণ সুস্থতা"
)

private val PreferenceOptions = listOf(
    "Regular" to "স্বাভাবিক",
    "Low-Carb" to "কম শর্করা",
    "Vegetarian" to "নিরামিষ",
    "Budget-Friendly" to "সাশ্রয়ী"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietPlannerSection(
    language: AppLanguage,
    initialAge: Int,
    dietGenState: UiState<DietPlanRecord>,
    latestSavedPlan: DietPlanRecord?,
    onGeneratePlan: (age: Int, conditions: List<String>, preference: String) -> Unit
) {
    val strings = stringsFor(language)
    var age by remember(initialAge) { mutableIntStateOf(initialAge.coerceIn(10, 100)) }
    var selectedConditions by remember { mutableStateOf(setOf("General Wellness")) }
    var selectedPreference by remember { mutableStateOf("Regular") }

    val displayedPlan = (dietGenState as? UiState.Success)?.data ?: latestSavedPlan

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("diet_planner_section"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SafetyDisclaimerBanner(language = language) }

        // Profile Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Age Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.ageLabel}: $age",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                IconButton(
                                    onClick = { if (age > 10) age-- },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease age", modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "$age",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = TealContainer
                            ) {
                                IconButton(
                                    onClick = { if (age < 100) age++ },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase age", tint = TealDeep, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Health Condition Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.healthConditionsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ConditionOptions.forEach { (en, bn) ->
                                val isSelected = en in selectedConditions
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedConditions = if (isSelected && selectedConditions.size > 1) {
                                            selectedConditions - en
                                        } else {
                                            selectedConditions + en
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (language == AppLanguage.BN) bn else en,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealDeep,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Dietary Preference Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.dietPreferenceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreferenceOptions.forEach { (en, bn) ->
                                val isSelected = selectedPreference == en
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPreference = en },
                                    label = {
                                        Text(
                                            text = if (language == AppLanguage.BN) bn else en,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberAccent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onGeneratePlan(age, selectedConditions.toList(), selectedPreference)
                        },
                        enabled = dietGenState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_diet_plan_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        if (dietGenState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(strings.generatingDiet, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.generateDietBtn, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (dietGenState is UiState.Error) {
            item {
                ErrorRetryBanner(
                    message = dietGenState.message,
                    onRetry = {
                        onGeneratePlan(age, selectedConditions.toList(), selectedPreference)
                    },
                    language = language
                )
            }
        }

        if (displayedPlan != null) {
            item {
                MealCard(
                    slotLabel = strings.breakfastLabel,
                    title = displayedPlan.breakfastTitle,
                    portion = displayedPlan.breakfastPortion,
                    rationale = displayedPlan.breakfastRationale,
                    imageResId = R.drawable.img_meal_breakfast_1795106998450
                )
            }
            item {
                MealCard(
                    slotLabel = strings.lunchLabel,
                    title = displayedPlan.lunchTitle,
                    portion = displayedPlan.lunchPortion,
                    rationale = displayedPlan.lunchRationale,
                    imageResId = R.drawable.img_meal_lunch_1795107089376
                )
            }
            item {
                MealCard(
                    slotLabel = strings.snackLabel,
                    title = displayedPlan.snackTitle,
                    portion = displayedPlan.snackPortion,
                    rationale = displayedPlan.snackRationale,
                    imageResId = null
                )
            }
            item {
                MealCard(
                    slotLabel = strings.dinnerLabel,
                    title = displayedPlan.dinnerTitle,
                    portion = displayedPlan.dinnerPortion,
                    rationale = displayedPlan.dinnerRationale,
                    imageResId = null
                )
            }

            // Side-by-side Prioritize vs Limit cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FoodListCard(
                        title = strings.foodsToPrioritize,
                        items = displayedPlan.foodsToPrioritize,
                        positive = true,
                        modifier = Modifier.weight(1f)
                    )
                    FoodListCard(
                        title = strings.foodsToLimit,
                        items = displayedPlan.foodsToLimit,
                        positive = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Hydration Tip Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = TealContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TealDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = strings.hydrationTipTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = TealDeep
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = displayedPlan.hydrationTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        onGeneratePlan(age, selectedConditions.toList(), selectedPreference)
                    },
                    enabled = dietGenState !is UiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("regenerate_diet_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.regeneratePlanBtn, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MealCard(
    slotLabel: String,
    title: String,
    portion: String,
    rationale: String,
    imageResId: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageResId != null) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = AmberOnContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TealContainer
                ) {
                    Text(
                        text = slotLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealDeep,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = portion,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TealDeep
                )
                Text(
                    text = rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FoodListCard(
    title: String,
    items: List<String>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (positive) EmeraldContainer.copy(alpha = 0.45f) else CoralContainer.copy(alpha = 0.45f)
    val accent = if (positive) EmeraldSuccess else CoralHeart
    val icon = if (positive) Icons.Filled.CheckCircle else Icons.Filled.Cancel

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
            items.forEach { food ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp)
                    )
                    Text(
                        text = food,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
