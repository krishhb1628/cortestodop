package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.CustomHabitEntity
import com.example.domain.model.AlcoholLevel
import com.example.domain.model.DailyHabitInput
import com.example.domain.model.DailyRewardOutcome
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneProfile
import com.example.domain.model.WorkoutType
import com.example.ui.components.OvertrainingWarningBanner
import com.example.ui.components.pathwayColor
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalHormoneColors
import com.example.ui.viewmodel.EndoPulseUiState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    state: EndoPulseUiState,
    onUpdateDraft: ((DailyHabitInput) -> DailyHabitInput) -> Unit,
    onUpdateBedtime: (Int, Int) -> Unit,
    onUpdateWakeTime: (Int, Int) -> Unit,
    onUpdateLastCaffeineTime: (Int, Int) -> Unit,
    onSaveDraft: () -> Unit,
    onToggleCustomHabit: (Int, Boolean) -> Unit,
    onAddCustomHabit: (String, HormonePathway, Int, Boolean, Int, Int) -> Unit,
    onUpdateCustomHabit: (CustomHabitEntity) -> Unit,
    onDeleteCustomHabit: (Int) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakePicker by remember { mutableStateOf(false) }
    var showCaffeinePicker by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<CustomHabitEntity?>(null) }
    var showCreateHabitDialog by remember { mutableStateOf(false) }

    val draft = state.draftInput
    val projected = state.draftProjectedProfile
    val reward = state.draftProjectedReward
    val friendlyDate = DateKeyUtils.formatFriendly(state.selectedDateKey)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            LiveProjectionBar(
                friendlyDate = friendlyDate,
                projected = projected,
                reward = reward,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (projected.overtrainingFlag) {
                    item {
                        OvertrainingWarningBanner()
                    }
                }

                // 1. Sleep & Circadian Module
                item {
                    SectionCard(title = "Sleep, Wind-Down & Morning Light") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimePickerTile(
                                label = "Bedtime",
                                timeText = DateKeyUtils.formatMinutesOfDay(draft.bedtimeMinutes),
                                icon = Icons.Filled.Bedtime,
                                onClick = { showBedtimePicker = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bedtime_picker_button")
                            )
                            TimePickerTile(
                                label = "Wake-Up",
                                timeText = DateKeyUtils.formatMinutesOfDay(draft.wakeTimeMinutes),
                                icon = Icons.Filled.WbSunny,
                                onClick = { showWakePicker = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("waketime_picker_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (draft.sleepHours < 6.5f) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Calculated Sleep Duration",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (draft.sleepHours < 6.5f) {
                                        Text(
                                            text = "Below 6.5h triggers non-linear hormone penalty",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = DateKeyUtils.formatDurationHours(draft.sleepHours),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (draft.sleepHours < 6.5f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("calculated_sleep_duration")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sleep Quality Rating (1 Poor – 5 Deep)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { q ->
                                FilterChip(
                                    selected = draft.sleepQuality == q,
                                    onClick = { onUpdateDraft { it.copy(sleepQuality = q) } },
                                    label = { Text("$q") },
                                    modifier = Modifier.testTag("sleep_quality_chip_$q")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        ToggleHabitRow(
                            title = "Regular Wake Time (±45m)",
                            subtitle = "Irregular wake times disrupt morning CAR & testosterone peak",
                            checked = draft.sleepConsistent,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(sleepConsistent = checked) }
                            },
                            testTag = "sleep_consistent_switch"
                        )
                        ToggleHabitRow(
                            title = "Consistent Wind-Down Window",
                            subtitle = "45–60m dimmed light / low-stimulation before bed",
                            checked = draft.consistentWindDown,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(consistentWindDown = checked) }
                            },
                            testTag = "wind_down_switch"
                        )
                        ToggleHabitRow(
                            title = "Outdoor Light Within 60m of Waking",
                            subtitle = "Anchors SCN circadian clock & serotonin synthesis",
                            checked = draft.morningLightWithin60Mins,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(morningLightWithin60Mins = checked) }
                            },
                            testTag = "morning_light_60_switch"
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Morning Outdoor Sunlight Duration", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 10, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = draft.morningSunlightMins == mins,
                                    onClick = { onUpdateDraft { it.copy(morningSunlightMins = mins) } },
                                    label = { Text(if (mins == 30) "30m+" else "${mins}m") },
                                    modifier = Modifier.testTag("sunlight_chip_$mins")
                                )
                            }
                        }
                    }
                }

                // 2. Custom Daily Habits with Streaks, Progress Dots & Reminders
                item {
                    val hormoneColors = LocalHormoneColors.current
                    val progressMap = remember(state.habitProgress) {
                        state.habitProgress.associateBy { it.habitId }
                    }
                    SectionCard(title = "Custom Daily Habits & Reminders") {
                        if (state.customHabits.isEmpty()) {
                            Text(
                                text = "Define your own effortful habits (e.g., \"Zone-2 Walk\", \"Magnesium\", \"Reading 20m\") with custom names, hormone targets, streaks, and daily reminder notifications.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.customHabits.forEach { habit ->
                                val isChecked = habit.id in state.completedCustomHabitIds
                                val pathway = habit.toPathway()
                                val color = pathwayColor(pathway, hormoneColors)
                                val prog = progressMap[habit.id]

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    onToggleCustomHabit(habit.id, checked)
                                                },
                                                modifier = Modifier.testTag("custom_habit_check_${habit.id}")
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = habit.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${pathway.shortLabel} (${if (pathway == HormonePathway.CORTISOL) "-${habit.impactPoints} load" else "+${habit.impactPoints} pts"})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = color
                                                    )
                                                    if (habit.reminderEnabled) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.NotificationsActive,
                                                            contentDescription = "Reminder active",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = DateKeyUtils.formatMinutesOfDay(habit.reminderHour * 60 + habit.reminderMinute),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = { editingHabit = habit },
                                                modifier = Modifier.testTag("edit_custom_habit_${habit.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit habit ${habit.title}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteCustomHabit(habit.id) },
                                                modifier = Modifier.testTag("delete_custom_habit_${habit.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Delete habit ${habit.title}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (prog != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp, end = 8.dp, top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${prog.currentStreakDays}d streak • ${prog.totalCompletions} total",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    prog.last7DaysCompleted.forEach { done ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(9.dp)
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

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showCreateHabitDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_custom_habit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add custom habit",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Custom Habit")
                        }
                    }
                }

                // 3. Training & Recovery
                item {
                    SectionCard(title = "Training & Recovery (${projected.weeklyResistanceCount}/7d Heavy Sessions)") {
                        Text("Training Stimulus (3–4x/wk Resistance = Optimal Multiplier)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WorkoutType.entries.forEach { type ->
                                FilterChip(
                                    selected = draft.workoutType == type,
                                    onClick = { onUpdateDraft { it.copy(workoutType = type) } },
                                    label = { Text(type.label) },
                                    modifier = Modifier.testTag("workout_chip_${type.name.lowercase()}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ToggleHabitRow(
                            title = "Cold Exposure (2–5m)",
                            subtitle = "Effortful catecholamine & tonic dopamine boost",
                            checked = draft.coldExposure,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(coldExposure = checked) }
                            },
                            testTag = "cold_exposure_switch"
                        )
                        ToggleHabitRow(
                            title = "Breathwork / Meditation (10m+)",
                            subtitle = "Lowers HPA cortisol reactivity",
                            checked = draft.meditationOrBreathwork,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(meditationOrBreathwork = checked) }
                            },
                            testTag = "breathwork_switch"
                        )
                    }
                }

                // 4. Caffeine Cutoff (8-10h before bed), Diet & Alcohol
                item {
                    SectionCard(title = "Caffeine Cutoff, Diet & Substances") {
                        ToggleHabitRow(
                            title = "Consumed Caffeine Today",
                            subtitle = "Penalized if within 8–10 hrs of bedtime",
                            checked = draft.hasCaffeine,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(hasCaffeine = checked) }
                            },
                            testTag = "has_caffeine_switch"
                        )
                        if (draft.hasCaffeine) {
                            val gap = draft.hoursCaffeineBeforeBed()
                            val isPenalty = draft.isLateCaffeinePenalty()
                            Spacer(modifier = Modifier.height(6.dp))
                            TimePickerTile(
                                label = if (isPenalty) {
                                    "Last Caffeine (${String.format(Locale.US, "%.1fh", gap)} before bed — Cutoff Penalty!)"
                                } else {
                                    "Last Caffeine (${String.format(Locale.US, "%.1fh", gap)} before bed — Clean Cutoff)"
                                },
                                timeText = DateKeyUtils.formatMinutesOfDay(draft.lastCaffeineMinutes),
                                icon = Icons.Filled.LocalCafe,
                                onClick = { showCaffeinePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("caffeine_picker_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        ToggleHabitRow(
                            title = "Whole-Food Protein & Healthy Fats",
                            subtitle = "Supports steroidogenesis & neurotransmitter cofactors",
                            checked = draft.nutritionClean,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(nutritionClean = checked) }
                            },
                            testTag = "nutrition_clean_switch"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Alcohol Intake", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlcoholLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = draft.alcoholLevel == level,
                                    onClick = { onUpdateDraft { it.copy(alcoholLevel = level) } },
                                    label = { Text(level.label) },
                                    modifier = Modifier.testTag("alcohol_chip_${level.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // 5. Dopamine Friction Ratio & Stress
                item {
                    SectionCard(title = "Dopamine Friction & Stress Load") {
                        Text("Deep Effortful Focus (Increases Friction Ratio)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 30, 60, 120).forEach { mins ->
                                FilterChip(
                                    selected = draft.deepWorkMins == mins,
                                    onClick = { onUpdateDraft { it.copy(deepWorkMins = mins) } },
                                    label = { Text(if (mins == 120) "120m+" else "${mins}m") },
                                    modifier = Modifier.testTag("deep_work_chip_$mins")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Passive High-Dopamine Scrolling / Feeds", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 15, 45, 90).forEach { mins ->
                                FilterChip(
                                    selected = draft.doomscrollMins == mins,
                                    onClick = { onUpdateDraft { it.copy(doomscrollMins = mins) } },
                                    label = { Text(if (mins == 90) "90m+" else "${mins}m") },
                                    modifier = Modifier.testTag("doomscroll_chip_$mins")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Perceived Daily Stress (1 Calm – 5 High)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { lvl ->
                                FilterChip(
                                    selected = draft.stressLevel == lvl,
                                    onClick = { onUpdateDraft { it.copy(stressLevel = lvl) } },
                                    label = { Text("$lvl") },
                                    modifier = Modifier.testTag("stress_chip_$lvl")
                                )
                            }
                        }
                    }
                }

                // 6. Notes + Save
                item {
                    SectionCard(title = "Daily Notes (Optional)") {
                        OutlinedTextField(
                            value = draft.notes,
                            onValueChange = { text -> onUpdateDraft { it.copy(notes = text) } },
                            placeholder = { Text("Energy, sleep observations, workout PRs...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_notes_input"),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                item {
                    Button(
                        onClick = onSaveDraft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_checkin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save check-in"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Claim +${reward.totalCoins} Coins")
                    }
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showBedtimePicker) {
        TimePickerDialogModal(
            title = "Select Bedtime",
            initialHour = draft.bedtimeMinutes / 60,
            initialMinute = draft.bedtimeMinutes % 60,
            onDismiss = { showBedtimePicker = false },
            onConfirm = { h, m ->
                onUpdateBedtime(h, m)
                showBedtimePicker = false
            }
        )
    }

    if (showWakePicker) {
        TimePickerDialogModal(
            title = "Select Wake-Up Time",
            initialHour = draft.wakeTimeMinutes / 60,
            initialMinute = draft.wakeTimeMinutes % 60,
            onDismiss = { showWakePicker = false },
            onConfirm = { h, m ->
                onUpdateWakeTime(h, m)
                showWakePicker = false
            }
        )
    }

    if (showCaffeinePicker) {
        TimePickerDialogModal(
            title = "Last Caffeine Time",
            initialHour = draft.lastCaffeineMinutes / 60,
            initialMinute = draft.lastCaffeineMinutes % 60,
            onDismiss = { showCaffeinePicker = false },
            onConfirm = { h, m ->
                onUpdateLastCaffeineTime(h, m)
                showCaffeinePicker = false
            }
        )
    }

    if (showCreateHabitDialog) {
        HabitEditorDialog(
            existing = null,
            onDismiss = { showCreateHabitDialog = false },
            onPermissionDenied = {
                onShowMessage("Notification permission denied. Habit saved without daily alarm.")
            },
            onSave = { title, pathway, weight, reminderEnabled, hour, minute ->
                onAddCustomHabit(title, pathway, weight, reminderEnabled, hour, minute)
                showCreateHabitDialog = false
            }
        )
    }

    editingHabit?.let { target ->
        HabitEditorDialog(
            existing = target,
            onDismiss = { editingHabit = null },
            onPermissionDenied = {
                onShowMessage("Notification permission denied. Reminder remains disabled.")
            },
            onSave = { title, pathway, weight, reminderEnabled, hour, minute ->
                onUpdateCustomHabit(
                    target.copy(
                        title = title,
                        pathway = pathway.name,
                        impactPoints = weight,
                        reminderEnabled = reminderEnabled,
                        reminderHour = hour,
                        reminderMinute = minute
                    )
                )
                editingHabit = null
            }
        )
    }
}

@Composable
private fun TimePickerTile(
    label: String,
    timeText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogModal(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                modifier = Modifier.testTag("confirm_time_picker_button")
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LiveProjectionBar(
    friendlyDate: String,
    projected: HormoneProfile,
    reward: DailyRewardOutcome,
    modifier: Modifier = Modifier
) {
    val colors = LocalHormoneColors.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$friendlyDate • Friction ${projected.frictionMetrics.ratio}x",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Projected coins",
                        tint = colors.testosterone,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${reward.totalCoins} (${reward.multiplierLabel})",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.testosterone
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeltaBadge("T", projected.testosterone.score, colors.testosterone)
                DeltaBadge("Cort", projected.cortisol.score, colors.cortisol)
                DeltaBadge("Dopa", projected.dopamine.score, colors.dopamine)
                DeltaBadge("Sero", projected.serotonin.score, colors.serotonin)
            }
        }
    }
}

@Composable
private fun DeltaBadge(label: String, value: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$value",
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
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleHabitRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitEditorDialog(
    existing: CustomHabitEntity?,
    onDismiss: () -> Unit,
    onPermissionDenied: () -> Unit,
    onSave: (String, HormonePathway, Int, Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var selectedPathway by remember(existing) {
        mutableStateOf(existing?.toPathway() ?: HormonePathway.TESTOSTERONE)
    }
    var impact by remember(existing) { mutableIntStateOf(existing?.impactPoints ?: 5) }
    var reminderEnabled by remember(existing) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderHour by remember(existing) { mutableIntStateOf(existing?.reminderHour ?: 8) }
    var reminderMinute by remember(existing) { mutableIntStateOf(existing?.reminderMinute ?: 0) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
        } else {
            reminderEnabled = false
            onPermissionDenied()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Custom Habit" else "Edit Custom Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Name (e.g. Creatine 5g)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_habit_title_input")
                )
                Text("Target Hormone Pathway", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HormonePathway.entries.forEach { pathway ->
                        FilterChip(
                            selected = selectedPathway == pathway,
                            onClick = { selectedPathway = pathway },
                            leadingIcon = if (selectedPathway == pathway) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(pathway.shortLabel) }
                        )
                    }
                }
                Text(
                    text = "Weight Impact: +$impact pts",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = impact.toFloat(),
                    onValueChange = { impact = it.roundToInt() },
                    valueRange = 2f..10f,
                    steps = 7
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = "Daily reminder toggle",
                            tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Reminder", style = MaterialTheme.typography.titleSmall)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { wantEnabled ->
                            if (!wantEnabled) {
                                reminderEnabled = false
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    reminderEnabled = true
                                } else {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                reminderEnabled = true
                            }
                        },
                        modifier = Modifier.testTag("habit_reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = { showReminderTimePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_reminder_time_button")
                    ) {
                        Text(
                            text = "Reminder Time: ${DateKeyUtils.formatMinutesOfDay(reminderHour * 60 + reminderMinute)}"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, selectedPathway, impact, reminderEnabled, reminderHour, reminderMinute)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_habit_button")
            ) {
                Text(if (existing == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showReminderTimePicker) {
        TimePickerDialogModal(
            title = "Reminder Time",
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                showReminderTimePicker = false
            }
        )
    }
}
