package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomHabitEntity
import com.example.data.local.DailyLogEntity
import com.example.data.local.EndoPulseRepository
import com.example.data.local.MilestoneRewardEntity
import com.example.domain.model.BioLevel
import com.example.domain.model.ChallengeItem
import com.example.domain.model.DailyHabitInput
import com.example.domain.model.DailyRewardOutcome
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.DateStripItem
import com.example.domain.model.HabitProgressItem
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneProfile
import com.example.domain.model.HormoneResearchEngine
import com.example.domain.model.PersonalCorrelation
import com.example.domain.model.SimulationPreset
import com.example.domain.model.SleepPatternSummary
import com.example.notifications.HabitReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EndoPulseUiState(
    val selectedDateKey: String = DateKeyUtils.todayKey(),
    val dateStrip: List<DateStripItem> = DateKeyUtils.recentSevenDays(),
    val allLogs: List<DailyLogEntity> = emptyList(),
    val loggedDateKeys: Set<String> = emptySet(),
    val selectedLogProfile: HormoneProfile? = null,
    val draftInput: DailyHabitInput = DailyHabitInput(dateKey = DateKeyUtils.todayKey()),
    val draftProjectedProfile: HormoneProfile = HormoneResearchEngine.evaluate(
        DailyHabitInput(dateKey = DateKeyUtils.todayKey())
    ),
    val draftProjectedReward: DailyRewardOutcome = HormoneResearchEngine.evaluateDailyReward(
        HormoneResearchEngine.evaluate(DailyHabitInput(dateKey = DateKeyUtils.todayKey())),
        DailyHabitInput(dateKey = DateKeyUtils.todayKey()),
        0
    ),
    val customHabits: List<CustomHabitEntity> = emptyList(),
    val completedCustomHabitIds: Set<Int> = emptySet(),
    val habitProgress: List<HabitProgressItem> = emptyList(),
    val sleepSummary: SleepPatternSummary? = null,
    val personalCorrelations: List<PersonalCorrelation> = emptyList(),
    val totalCoins: Int = 0,
    val currentLevel: BioLevel = HormoneResearchEngine.bioLevels.first(),
    val activeChallenges: List<ChallengeItem> = emptyList(),
    val unlockedMilestones: List<MilestoneRewardEntity> = emptyList(),
    val simulationInput: DailyHabitInput = HormoneResearchEngine.simulationPresets.first().input,
    val simulationProfile: HormoneProfile = HormoneResearchEngine.evaluate(
        HormoneResearchEngine.simulationPresets.first().input
    ),
    val trendRangeDays: Int = 7,
    val snackbarMessage: String? = null,
    val exportedJson: String? = null
)

private data class LocalDraftState(
    val selectedDateKey: String = DateKeyUtils.todayKey(),
    val draftInput: DailyHabitInput = DailyHabitInput(dateKey = DateKeyUtils.todayKey()),
    val draftInitializedForDate: String? = null,
    val simulationInput: DailyHabitInput = HormoneResearchEngine.simulationPresets.first().input,
    val trendRangeDays: Int = 7,
    val snackbarMessage: String? = null,
    val exportedJson: String? = null
)

class EndoPulseViewModel(
    private val repository: EndoPulseRepository
) : ViewModel() {

    private val localState = MutableStateFlow(LocalDraftState())

    val uiState: StateFlow<EndoPulseUiState> = combine(
        repository.allLogs,
        repository.customHabits,
        repository.allCompletions,
        repository.milestones,
        localState
    ) { logs, habits, completions, milestones, local ->
        val selectedDate = local.selectedDateKey
        val logsByDate = logs.associateBy { it.dateKey }
        val selectedEntity = logsByDate[selectedDate]

        val completedIdsForDate = completions
            .filter { it.dateKey == selectedDate && it.completed }
            .map { it.habitId }
            .toSet()

        val bonusesForDate = habits
            .filter { it.id in completedIdsForDate }
            .map { it.toBonus() }

        val recentBeforeSelected = logs
            .filter { it.dateKey < selectedDate }
            .take(6)
            .map { it.toDomainInput() }

        val effectiveDraft = if (local.draftInitializedForDate == selectedDate) {
            local.draftInput
        } else {
            selectedEntity?.toDomainInput() ?: DailyHabitInput(dateKey = selectedDate)
        }

        val selectedProfile = selectedEntity?.let { entity ->
            HormoneResearchEngine.evaluate(
                input = entity.toDomainInput(),
                customBonuses = bonusesForDate,
                recentHistory = recentBeforeSelected
            )
        }

        val projectedDraftProfile = HormoneResearchEngine.evaluate(
            input = effectiveDraft,
            customBonuses = bonusesForDate,
            recentHistory = recentBeforeSelected
        )
        val projectedReward = HormoneResearchEngine.evaluateDailyReward(
            profile = projectedDraftProfile,
            input = effectiveDraft,
            completedCustomCount = bonusesForDate.size
        )

        val totalCoins = logs.sumOf { it.coinsEarned } + milestones.sumOf { it.coinsAwarded }
        val currentLevel = HormoneResearchEngine.computeBioLevel(totalCoins)
        val unlockedIds = milestones.map { it.milestoneId }.toSet()

        val referenceInput = selectedEntity?.toDomainInput() ?: effectiveDraft
        val referenceProfile = selectedProfile ?: projectedDraftProfile
        val challenges = HormoneResearchEngine.evaluateChallenges(
            todayInput = referenceInput,
            todayProfile = referenceProfile,
            allLogs = logs,
            unlockedMilestoneIds = unlockedIds
        )

        val habitProgress = HormoneResearchEngine.computeHabitProgress(habits, completions)
        val sleepSummary = HormoneResearchEngine.summarizeSleepPatterns(logs)
        val personalCorrelations = HormoneResearchEngine.analyzePersonalCorrelations(
            logs = logs,
            customHabits = habits,
            completions = completions
        )
        val simProfile = HormoneResearchEngine.evaluate(local.simulationInput)

        EndoPulseUiState(
            selectedDateKey = selectedDate,
            dateStrip = DateKeyUtils.recentSevenDays(),
            allLogs = logs,
            loggedDateKeys = logsByDate.keys,
            selectedLogProfile = selectedProfile,
            draftInput = effectiveDraft,
            draftProjectedProfile = projectedDraftProfile,
            draftProjectedReward = projectedReward,
            customHabits = habits,
            completedCustomHabitIds = completedIdsForDate,
            habitProgress = habitProgress,
            sleepSummary = sleepSummary,
            personalCorrelations = personalCorrelations,
            totalCoins = totalCoins,
            currentLevel = currentLevel,
            activeChallenges = challenges,
            unlockedMilestones = milestones,
            simulationInput = local.simulationInput,
            simulationProfile = simProfile,
            trendRangeDays = local.trendRangeDays,
            snackbarMessage = local.snackbarMessage,
            exportedJson = local.exportedJson
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EndoPulseUiState()
    )

    fun selectDate(dateKey: String) {
        val currentLogs = uiState.value.allLogs.associateBy { it.dateKey }
        val existing = currentLogs[dateKey]?.toDomainInput() ?: DailyHabitInput(dateKey = dateKey)
        localState.update {
            it.copy(
                selectedDateKey = dateKey,
                draftInput = existing,
                draftInitializedForDate = dateKey
            )
        }
    }

    fun updateDraft(transform: (DailyHabitInput) -> DailyHabitInput) {
        val currentState = uiState.value
        val updated = transform(currentState.draftInput).copy(dateKey = currentState.selectedDateKey)
        localState.update {
            it.copy(
                draftInput = updated,
                draftInitializedForDate = currentState.selectedDateKey
            )
        }
    }

    fun updateDraftBedtime(hour24: Int, minute: Int) {
        val bedMinutes = (hour24.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
        updateDraft { current ->
            val duration = DateKeyUtils.calculateSleepDurationHours(bedMinutes, current.wakeTimeMinutes)
            current.copy(bedtimeMinutes = bedMinutes, sleepHours = duration)
        }
    }

    fun updateDraftWakeTime(hour24: Int, minute: Int) {
        val wakeMinutes = (hour24.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
        updateDraft { current ->
            val duration = DateKeyUtils.calculateSleepDurationHours(current.bedtimeMinutes, wakeMinutes)
            current.copy(wakeTimeMinutes = wakeMinutes, sleepHours = duration)
        }
    }

    fun updateDraftLastCaffeineTime(hour24: Int, minute: Int) {
        val cafMinutes = (hour24.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
        updateDraft { current ->
            val gap = DateKeyUtils.calculateHoursBeforeBedtime(cafMinutes, current.bedtimeMinutes)
            current.copy(
                hasCaffeine = true,
                lastCaffeineMinutes = cafMinutes,
                caffeineAfter2Pm = gap < 10.0f
            )
        }
    }

    fun saveCurrentDraft() {
        val draft = uiState.value.draftInput
        viewModelScope.launch {
            val result = repository.saveDailyCheckIn(draft)
            val milestoneText = if (result.newlyUnlockedMilestones.isNotEmpty()) {
                val m = result.newlyUnlockedMilestones.first()
                " • Milestone: ${m.title} (+${m.coinsAwarded} coins!)"
            } else ""
            val reward = result.dailyReward
            localState.update {
                it.copy(
                    draftInput = draft,
                    draftInitializedForDate = draft.dateKey,
                    snackbarMessage = "Saved! +${reward.totalCoins} coins (${reward.multiplierLabel})$milestoneText"
                )
            }
        }
    }

    fun toggleCustomHabit(habitId: Int, completed: Boolean) {
        val dateKey = uiState.value.selectedDateKey
        viewModelScope.launch {
            repository.toggleCustomHabitCompletion(dateKey, habitId, completed)
        }
    }

    fun addCustomHabit(
        context: Context,
        title: String,
        pathway: HormonePathway,
        impactPoints: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        viewModelScope.launch {
            val created = repository.addCustomHabit(
                title = title,
                pathway = pathway,
                impactPoints = impactPoints,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            if (created != null) {
                HabitReminderScheduler.scheduleOrCancel(context.applicationContext, created)
                localState.update { it.copy(snackbarMessage = "Added habit \"${created.title}\"") }
            }
        }
    }

    fun updateCustomHabit(context: Context, habit: CustomHabitEntity) {
        viewModelScope.launch {
            val updated = repository.updateCustomHabit(habit)
            HabitReminderScheduler.scheduleOrCancel(context.applicationContext, updated)
            localState.update { it.copy(snackbarMessage = "Updated \"${updated.title}\"") }
        }
    }

    fun deleteCustomHabit(context: Context, habitId: Int) {
        viewModelScope.launch {
            HabitReminderScheduler.cancelReminder(context.applicationContext, habitId)
            repository.deleteCustomHabit(habitId)
        }
    }

    fun setTrendRange(days: Int) {
        localState.update { it.copy(trendRangeDays = days) }
    }

    fun updateSimulation(transform: (DailyHabitInput) -> DailyHabitInput) {
        localState.update { it.copy(simulationInput = transform(it.simulationInput)) }
    }

    fun applySimulationPreset(preset: SimulationPreset) {
        localState.update { it.copy(simulationInput = preset.input) }
    }

    fun requestJsonExport() {
        viewModelScope.launch {
            val json = repository.exportLogsToJson()
            localState.update { it.copy(exportedJson = json) }
        }
    }

    fun consumeExportedJson() {
        localState.update { it.copy(exportedJson = null) }
    }

    fun clearAllLocalData(context: Context) {
        val existingHabits = uiState.value.customHabits
        viewModelScope.launch {
            existingHabits.forEach { HabitReminderScheduler.cancelReminder(context.applicationContext, it.id) }
            repository.clearAllLocalData()
            val today = DateKeyUtils.todayKey()
            localState.update {
                it.copy(
                    selectedDateKey = today,
                    draftInput = DailyHabitInput(dateKey = today),
                    draftInitializedForDate = today,
                    snackbarMessage = "Local database cleared"
                )
            }
        }
    }

    fun showMessage(message: String) {
        localState.update { it.copy(snackbarMessage = message) }
    }

    fun consumeSnackbar() {
        localState.update { it.copy(snackbarMessage = null) }
    }

    class Factory(private val repository: EndoPulseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EndoPulseViewModel(repository) as T
        }
    }
}
