package com.example.data.local

import com.example.domain.model.DailyHabitInput
import com.example.domain.model.DailyRewardOutcome
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneProfile
import com.example.domain.model.HormoneResearchEngine
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class SaveCheckInResult(
    val profile: HormoneProfile,
    val dailyReward: DailyRewardOutcome,
    val newlyUnlockedMilestones: List<MilestoneRewardEntity>
)

class EndoPulseRepository(private val dao: EndoPulseDao) {

    val allLogs: Flow<List<DailyLogEntity>> = dao.observeAllLogs()
    val customHabits: Flow<List<CustomHabitEntity>> = dao.observeCustomHabits()
    val allCompletions: Flow<List<CustomHabitCompletionEntity>> = dao.observeAllCompletions()
    val milestones: Flow<List<MilestoneRewardEntity>> = dao.observeMilestones()

    suspend fun saveDailyCheckIn(input: DailyHabitInput): SaveCheckInResult {
        val computedSleep = DateKeyUtils.calculateSleepDurationHours(
            bedtimeMinutes = input.bedtimeMinutes,
            wakeTimeMinutes = input.wakeTimeMinutes
        )
        val normalizedInput = input.copy(
            sleepHours = computedSleep,
            caffeineAfter2Pm = input.hasCaffeine && DateKeyUtils.calculateHoursBeforeBedtime(
                input.lastCaffeineMinutes,
                input.bedtimeMinutes
            ) < 10.0f
        )

        val recent = dao.getRecentLogs(7)
            .filter { it.dateKey < normalizedInput.dateKey }
            .take(6)
            .map { it.toDomainInput() }

        val habits = dao.getAllCustomHabits()
        val completedIds = dao.getCompletionsForDate(normalizedInput.dateKey)
            .filter { it.completed }
            .map { it.habitId }
            .toSet()

        val bonuses = habits.filter { it.id in completedIds }.map { it.toBonus() }
        val profile = HormoneResearchEngine.evaluate(
            input = normalizedInput,
            customBonuses = bonuses,
            recentHistory = recent
        )
        val reward = HormoneResearchEngine.evaluateDailyReward(
            profile = profile,
            input = normalizedInput,
            completedCustomCount = bonuses.size
        )

        dao.upsertLog(
            DailyLogEntity.fromDomain(
                input = normalizedInput,
                testosteroneScore = profile.testosterone.score,
                cortisolScore = profile.cortisol.score,
                dopamineScore = profile.dopamine.score,
                serotoninScore = profile.serotonin.score,
                vitalityScore = profile.vitalityScore,
                dopamineFrictionRatio = profile.frictionMetrics.ratio,
                overtrainingFlag = profile.overtrainingFlag,
                coinsEarned = reward.totalCoins,
                rewardMultiplierLabel = reward.multiplierLabel
            )
        )

        val allAfterSave = dao.getRecentLogs(30)
        val alreadyUnlocked = dao.getAllMilestones().map { it.milestoneId }.toSet()
        val newMilestones = HormoneResearchEngine.detectNewlyUnlockedMilestones(
            allLogs = allAfterSave,
            alreadyUnlockedIds = alreadyUnlocked
        )
        newMilestones.forEach { dao.insertMilestoneIfNew(it) }

        return SaveCheckInResult(
            profile = profile,
            dailyReward = reward,
            newlyUnlockedMilestones = newMilestones
        )
    }

    suspend fun toggleCustomHabitCompletion(dateKey: String, habitId: Int, completed: Boolean) {
        if (completed) {
            dao.upsertCompletion(
                CustomHabitCompletionEntity(
                    dateKey = dateKey,
                    habitId = habitId,
                    completed = true
                )
            )
        } else {
            dao.deleteCompletion(dateKey = dateKey, habitId = habitId)
        }
        dao.getLogByDate(dateKey)?.let { existing ->
            saveDailyCheckIn(existing.toDomainInput())
        }
    }

    suspend fun addCustomHabit(
        title: String,
        pathway: HormonePathway,
        impactPoints: Int,
        reminderEnabled: Boolean = false,
        reminderHour: Int = 8,
        reminderMinute: Int = 0
    ): CustomHabitEntity? {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return null
        val entity = CustomHabitEntity(
            title = sanitized,
            pathway = pathway.name,
            impactPoints = impactPoints.coerceIn(2, 10),
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59)
        )
        val newId = dao.insertCustomHabit(entity).toInt()
        return entity.copy(id = newId)
    }

    suspend fun updateCustomHabit(habit: CustomHabitEntity): CustomHabitEntity {
        val sanitized = habit.copy(
            title = habit.title.trim().ifEmpty { habit.title },
            impactPoints = habit.impactPoints.coerceIn(2, 10),
            reminderHour = habit.reminderHour.coerceIn(0, 23),
            reminderMinute = habit.reminderMinute.coerceIn(0, 59)
        )
        dao.updateCustomHabit(sanitized)
        return sanitized
    }

    suspend fun deleteCustomHabit(habitId: Int) {
        dao.deleteCompletionsForHabit(habitId)
        dao.deleteCustomHabit(habitId)
    }

    suspend fun clearAllLocalData() {
        dao.clearAllCompletions()
        dao.clearAllLogs()
        dao.clearCustomHabits()
        dao.clearMilestones()
    }

    suspend fun exportLogsToJson(): String {
        val logs = dao.getRecentLogs(365)
        val habits = dao.getAllCustomHabits()
        val milestoneList = dao.getAllMilestones()
        val root = JSONObject()
        root.put("app", "EndoPulse")
        root.put("exportedAt", System.currentTimeMillis())

        val logsArray = JSONArray()
        logs.forEach { log ->
            logsArray.put(
                JSONObject().apply {
                    put("dateKey", log.dateKey)
                    put("bedtimeMinutes", log.bedtimeMinutes)
                    put("wakeTimeMinutes", log.wakeTimeMinutes)
                    put("sleepHours", log.sleepHours.toDouble())
                    put("sleepQuality", log.sleepQuality)
                    put("sleepConsistent", log.sleepConsistent)
                    put("consistentWindDown", log.consistentWindDown)
                    put("morningSunlightMins", log.morningSunlightMins)
                    put("morningLightWithin60Mins", log.morningLightWithin60Mins)
                    put("workoutType", log.workoutType)
                    put("coldExposure", log.coldExposure)
                    put("meditationOrBreathwork", log.meditationOrBreathwork)
                    put("nutritionClean", log.nutritionClean)
                    put("alcoholLevel", log.alcoholLevel)
                    put("hasCaffeine", log.hasCaffeine)
                    put("lastCaffeineMinutes", log.lastCaffeineMinutes)
                    put("doomscrollMins", log.doomscrollMins)
                    put("deepWorkMins", log.deepWorkMins)
                    put("stressLevel", log.stressLevel)
                    put("testosteroneScore", log.testosteroneScore)
                    put("cortisolScore", log.cortisolScore)
                    put("dopamineScore", log.dopamineScore)
                    put("serotoninScore", log.serotoninScore)
                    put("vitalityScore", log.vitalityScore)
                    put("dopamineFrictionRatio", log.dopamineFrictionRatio.toDouble())
                    put("overtrainingFlag", log.overtrainingFlag)
                    put("coinsEarned", log.coinsEarned)
                    put("rewardMultiplierLabel", log.rewardMultiplierLabel)
                    put("notes", log.notes)
                }
            )
        }
        root.put("dailyLogs", logsArray)

        val habitsArray = JSONArray()
        habits.forEach { h ->
            habitsArray.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("pathway", h.pathway)
                    put("impactPoints", h.impactPoints)
                    put("reminderEnabled", h.reminderEnabled)
                    put("reminderHour", h.reminderHour)
                    put("reminderMinute", h.reminderMinute)
                }
            )
        }
        root.put("customHabits", habitsArray)

        val milestoneArray = JSONArray()
        milestoneList.forEach { m ->
            milestoneArray.put(
                JSONObject().apply {
                    put("milestoneId", m.milestoneId)
                    put("title", m.title)
                    put("coinsAwarded", m.coinsAwarded)
                }
            )
        }
        root.put("milestoneRewards", milestoneArray)
        return root.toString(2)
    }
}
