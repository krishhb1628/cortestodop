package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DailyHabitInput
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneResearchEngine
import com.example.domain.model.SimulationPreset
import com.example.domain.model.WorkoutType
import com.example.ui.components.MultiHormoneTrendChart
import com.example.ui.components.PersonalCorrelationCard
import com.example.ui.components.SleepPatternChart
import com.example.ui.components.pathwayColor
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalHormoneColors
import com.example.ui.viewmodel.EndoPulseUiState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendsSimulatorScreen(
    state: EndoPulseUiState,
    onSetTrendRange: (Int) -> Unit,
    onUpdateSimulation: ((DailyHabitInput) -> DailyHabitInput) -> Unit,
    onApplyPreset: (SimulationPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    var visiblePathways by remember {
        mutableStateOf(HormonePathway.entries.toSet())
    }
    val filteredLogs = remember(state.allLogs, state.trendRangeDays) {
        state.allLogs.take(state.trendRangeDays)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Trends, Sleep & Correlations",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Historical sleep patterns, habit streaks, and personal hormone correlations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Historical Sleep Patterns Overview
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = "Sleep history",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sleep Patterns History",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val sleepSummary = state.sleepSummary
                        if (sleepSummary == null) {
                            Text(
                                text = "Log your bedtime and wake-up time in Check-In to build your sleep duration and circadian consistency history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SleepStatChip(
                                    label = "Avg Sleep",
                                    value = DateKeyUtils.formatDurationHours(sleepSummary.avgDurationHours),
                                    modifier = Modifier.weight(1f)
                                )
                                SleepStatChip(
                                    label = "Avg Bed",
                                    value = sleepSummary.avgBedtimeLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                SleepStatChip(
                                    label = "Avg Wake",
                                    value = sleepSummary.avgWakeTimeLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                SleepStatChip(
                                    label = "Quality",
                                    value = "${String.format(Locale.US, "%.1f", sleepSummary.avgQuality)}/5",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            SleepPatternChart(logs = filteredLogs)
                        }
                    }
                }
            }

            // 2. Multi-Hormone Trajectory Chart
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ShowChart,
                                    contentDescription = "Hormone trends",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Endocrine Trajectory",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(7, 14, 30).forEach { days ->
                                    FilterChip(
                                        selected = state.trendRangeDays == days,
                                        onClick = { onSetTrendRange(days) },
                                        label = { Text("${days}D") },
                                        modifier = Modifier.testTag("trend_range_$days")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HormonePathway.entries.forEach { pathway ->
                                val active = pathway in visiblePathways
                                val color = pathwayColor(pathway, hormoneColors)
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        visiblePathways = if (active && visiblePathways.size > 1) {
                                            visiblePathways - pathway
                                        } else {
                                            visiblePathways + pathway
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = pathway.shortLabel,
                                            color = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier.testTag("trend_toggle_${pathway.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredLogs.size < 2) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (filteredLogs.isEmpty()) {
                                            "No check-ins logged yet"
                                        } else {
                                            "1 day logged — log 1 more day to unlock trend curves"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Use the date strip on Pulse to log today or past days, or experiment with the What-If Simulator below right now.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            MultiHormoneTrendChart(
                                logs = filteredLogs,
                                visiblePathways = visiblePathways
                            )
                        }
                    }
                }
            }

            // 3. Habit Progress Over Time
            if (state.habitProgress.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Habit progress",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Custom Habit Consistency (Last 7 Days)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            state.habitProgress.forEach { prog ->
                                val color = pathwayColor(prog.pathway, hormoneColors)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prog.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${prog.currentStreakDays}d streak • ${prog.totalCompletions} completions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        prog.last7DaysCompleted.forEach { done ->
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (done) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Personal Data <-> Hormone Correlations
            item {
                Text(
                    text = "Your Data ↔ Hormone Correlations",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            state.personalCorrelations.forEach { correlation ->
                item(key = correlation.id) {
                    PersonalCorrelationCard(correlation = correlation)
                }
            }

            // 5. Interactive What-If Hormone Simulator
            item {
                val simInput = state.simulationInput
                val simProfile = state.simulationProfile

                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "What-If Simulator",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "What-If Hormone Simulator",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Test how sleep, lifting, and stress couple in real time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HormoneResearchEngine.simulationPresets.forEachIndexed { idx, preset ->
                                FilterChip(
                                    selected = simInput == preset.input,
                                    onClick = { onApplyPreset(preset) },
                                    label = { Text(preset.name) },
                                    modifier = Modifier.testTag("sim_preset_$idx")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SimScoreBox("Testosterone", simProfile.testosterone.score, hormoneColors.testosterone, Modifier.weight(1f))
                            SimScoreBox("Cortisol", simProfile.cortisol.score, hormoneColors.cortisol, Modifier.weight(1f))
                            SimScoreBox("Dopamine", simProfile.dopamine.score, hormoneColors.dopamine, Modifier.weight(1f))
                            SimScoreBox("Serotonin", simProfile.serotonin.score, hormoneColors.serotonin, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = simProfile.couplingInsight,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulated Sleep", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = DateKeyUtils.formatDurationHours(simInput.sleepHours),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = simInput.sleepHours,
                            onValueChange = { raw ->
                                val stepped = (raw * 2f).roundToInt() / 2f
                                onUpdateSimulation { it.copy(sleepHours = stepped) }
                            },
                            valueRange = 4.0f..10.0f,
                            steps = 11,
                            modifier = Modifier.testTag("sim_sleep_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Perceived Stress", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${simInput.stressLevel} / 5",
                                style = MaterialTheme.typography.labelMedium,
                                color = hormoneColors.cortisol
                            )
                        }
                        Slider(
                            value = simInput.stressLevel.toFloat(),
                            onValueChange = { raw ->
                                onUpdateSimulation { it.copy(stressLevel = raw.roundToInt()) }
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.testTag("sim_stress_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Workout Type", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WorkoutType.entries.forEach { type ->
                                FilterChip(
                                    selected = simInput.workoutType == type,
                                    onClick = { onUpdateSimulation { it.copy(workoutType = type) } },
                                    label = { Text(type.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cold Exposure", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = simInput.coldExposure,
                                onCheckedChange = { checked ->
                                    onUpdateSimulation { it.copy(coldExposure = checked) }
                                },
                                modifier = Modifier.testTag("sim_cold_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Breathwork / Meditation", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = simInput.meditationOrBreathwork,
                                onCheckedChange = { checked ->
                                    onUpdateSimulation { it.copy(meditationOrBreathwork = checked) }
                                },
                                modifier = Modifier.testTag("sim_breath_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Caffeine After 2 PM", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = simInput.caffeineAfter2Pm,
                                onCheckedChange = { checked ->
                                    onUpdateSimulation { it.copy(caffeineAfter2Pm = checked) }
                                },
                                modifier = Modifier.testTag("sim_caffeine_switch")
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun SleepStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimScoreBox(
    label: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Text(
                text = label.take(5),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
