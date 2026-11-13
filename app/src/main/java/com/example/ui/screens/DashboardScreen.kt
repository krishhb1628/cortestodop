package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.DailyLogEntity
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.HormoneProfile
import com.example.ui.components.BioVitalityRingCluster
import com.example.ui.components.DateStripSelector
import com.example.ui.components.DopamineFrictionCard
import com.example.ui.components.HormoneMetricCard
import com.example.ui.components.OvertrainingWarningBanner
import com.example.ui.components.PersonalCorrelationCard
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalHormoneColors
import com.example.ui.viewmodel.EndoPulseUiState

@Composable
fun DashboardScreen(
    state: EndoPulseUiState,
    onSelectDate: (String) -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    val friendlyDate = DateKeyUtils.formatFriendly(state.selectedDateKey)
    val selectedEntity = remember(state.allLogs, state.selectedDateKey) {
        state.allLogs.firstOrNull { it.dateKey == state.selectedDateKey }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EndoPulse",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$friendlyDate • Lv.${state.currentLevel.level} ${state.currentLevel.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hormoneColors.testosterone.copy(alpha = 0.16f),
                        onClick = onNavigateToChallenges,
                        modifier = Modifier.testTag("dashboard_coin_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Open Quests and Levels",
                                tint = hormoneColors.testosterone,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.totalCoins} Coins",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                color = hormoneColors.testosterone
                            )
                        }
                    }
                }
            }

            item {
                DateStripSelector(
                    items = state.dateStrip,
                    selectedDateKey = state.selectedDateKey,
                    loggedDates = state.loggedDateKeys,
                    onSelectDate = onSelectDate
                )
            }

            val profile = state.selectedLogProfile
            if (profile == null) {
                item {
                    EmptyDayCard(
                        friendlyDate = friendlyDate,
                        onLogDay = onNavigateToCheckIn,
                        onOpenSimulator = onNavigateToSimulator
                    )
                }
            } else {
                if (profile.overtrainingFlag) {
                    item {
                        OvertrainingWarningBanner()
                    }
                }

                item {
                    VitalityHeroCard(
                        profile = profile,
                        logEntity = selectedEntity,
                        onEditDay = onNavigateToCheckIn
                    )
                }

                item {
                    DopamineFrictionCard(metrics = profile.frictionMetrics)
                }

                item {
                    CouplingInsightBanner(insight = profile.couplingInsight)
                }

                state.personalCorrelations.firstOrNull { it.isDataBacked }?.let { topCorr ->
                    item {
                        PersonalCorrelationCard(correlation = topCorr)
                    }
                }

                item {
                    Text(
                        text = "Hormone Pathways & Attribution",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    HormoneMetricCard(
                        breakdown = profile.testosterone,
                        initiallyExpanded = true
                    )
                }
                item {
                    HormoneMetricCard(breakdown = profile.cortisol)
                }
                item {
                    HormoneMetricCard(breakdown = profile.dopamine)
                }
                item {
                    HormoneMetricCard(breakdown = profile.serotonin)
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun VitalityHeroCard(
    profile: HormoneProfile,
    logEntity: DailyLogEntity?,
    onEditDay: () -> Unit
) {
    val hormoneColors = LocalHormoneColors.current

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val isWide = maxWidth >= 460.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BioVitalityRingCluster(profile = profile)
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        LegendGrid(profile = profile, hormoneColors = hormoneColors)
                        logEntity?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            SleepSummaryPill(it)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onEditDay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit logged habits",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Check-In")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BioVitalityRingCluster(profile = profile)
                    Spacer(modifier = Modifier.height(16.dp))
                    LegendGrid(profile = profile, hormoneColors = hormoneColors)
                    logEntity?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        SleepSummaryPill(it)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onEditDay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit logged habits",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Check-In")
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepSummaryPill(log: DailyLogEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = "Logged sleep window",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${DateKeyUtils.formatMinutesOfDay(log.bedtimeMinutes)} → ${DateKeyUtils.formatMinutesOfDay(log.wakeTimeMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${DateKeyUtils.formatDurationHours(log.sleepHours)} • +${log.coinsEarned}c",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LegendGrid(
    profile: HormoneProfile,
    hormoneColors: com.example.ui.theme.HormoneColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniHormonePill(
                label = "Testosterone",
                score = profile.testosterone.score,
                color = hormoneColors.testosterone,
                modifier = Modifier.weight(1f)
            )
            MiniHormonePill(
                label = "Cortisol",
                score = profile.cortisol.score,
                color = hormoneColors.cortisol,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniHormonePill(
                label = "Dopamine",
                score = profile.dopamine.score,
                color = hormoneColors.dopamine,
                modifier = Modifier.weight(1f)
            )
            MiniHormonePill(
                label = "Serotonin",
                score = profile.serotonin.score,
                color = hormoneColors.serotonin,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniHormonePill(
    label: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(8.dp),
                    content = {}
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
private fun CouplingInsightBanner(insight: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Endocrine coupling insight",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyDayCard(
    friendlyDate: String,
    onLogDay: () -> Unit,
    onOpenSimulator: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.NightlightRound,
                        contentDescription = "No log recorded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Habits Logged for $friendlyDate",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Log bedtime, wake-up time, caffeine cutoff, training, and effortful habits to earn variable Bio-Coins, level up, and track your endocrine balance locally.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onLogDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_state_log_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Log check-in",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log $friendlyDate & Earn Coins")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenSimulator,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_state_simulator_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = "Open hormone simulator",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try What-If Hormone Simulator")
            }
        }
    }
}
