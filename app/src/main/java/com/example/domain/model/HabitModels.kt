package com.example.domain.model

enum class HormonePathway(val displayName: String, val shortLabel: String) {
    TESTOSTERONE("Testosterone Vitality", "Testosterone"),
    CORTISOL("Cortisol Load (HPA)", "Cortisol"),
    DOPAMINE("Dopamine Baseline", "Dopamine"),
    SEROTONIN("Serotonin & Recovery", "Serotonin")
}

enum class WorkoutType(
    val label: String,
    val testosteroneDelta: Int,
    val dopamineDelta: Int,
    val cortisolDelta: Int,
    val serotoninDelta: Int,
    val effortPoints: Int,
    val isHighLoad: Boolean
) {
    NONE("Rest / None", 0, 0, 0, 0, 0, false),
    RESISTANCE("Strength / Weights", 18, 12, 2, 6, 45, true),
    HIIT("HIIT / Sprints", 14, 14, 5, 5, 40, true),
    CARDIO("Zone 2 Cardio", 7, 10, -8, 12, 35, false),
    MOBILITY("Yoga / Walk", 3, 6, -12, 10, 25, false)
}

enum class AlcoholLevel(
    val label: String,
    val testosteroneDelta: Int,
    val cortisolDelta: Int,
    val dopamineDelta: Int,
    val serotoninDelta: Int,
    val passivePenalty: Int
) {
    NONE("None", 2, -2, 2, 2, 0),
    MODERATE("1–2 Drinks", -7, 6, -5, -4, 20),
    HEAVY("3+ Drinks", -16, 15, -14, -12, 40)
}

data class CustomHabitBonus(
    val title: String,
    val pathway: HormonePathway,
    val impactPoints: Int
)

data class DailyHabitInput(
    val dateKey: String,
    val bedtimeMinutes: Int = 23 * 60, // 11:00 PM
    val wakeTimeMinutes: Int = 7 * 60, // 7:00 AM
    val sleepHours: Float = DateKeyUtils.calculateSleepDurationHours(23 * 60, 7 * 60),
    val sleepQuality: Int = 4, // 1..5
    val sleepConsistent: Boolean = true, // Irregular wake / schedule check
    val consistentWindDown: Boolean = true, // 45-60m wind-down window before bed
    val morningSunlightMins: Int = 20, // 0, 10, 20, 30
    val morningLightWithin60Mins: Boolean = true,
    val workoutType: WorkoutType = WorkoutType.NONE,
    val coldExposure: Boolean = false,
    val meditationOrBreathwork: Boolean = false,
    val nutritionClean: Boolean = true,
    val alcoholLevel: AlcoholLevel = AlcoholLevel.NONE,
    val hasCaffeine: Boolean = true,
    val lastCaffeineMinutes: Int = 10 * 60, // 10:00 AM (>10h before 11 PM bed)
    val caffeineAfter2Pm: Boolean = false,
    val doomscrollMins: Int = 15, // 0, 15, 45, 90
    val deepWorkMins: Int = 60, // 0, 30, 60, 120
    val stressLevel: Int = 2, // 1..5
    val notes: String = ""
) {
    fun hoursCaffeineBeforeBed(): Float {
        if (!hasCaffeine) return 24f
        return DateKeyUtils.calculateHoursBeforeBedtime(lastCaffeineMinutes, bedtimeMinutes)
    }

    fun isLateCaffeinePenalty(): Boolean {
        if (!hasCaffeine) return false
        return hoursCaffeineBeforeBed() < 10.0f || caffeineAfter2Pm
    }
}

data class FactorContribution(
    val label: String,
    val delta: Int,
    val detail: String
)

data class HormoneBreakdown(
    val pathway: HormonePathway,
    val score: Int,
    val statusLabel: String,
    val summary: String,
    val contributions: List<FactorContribution>
)

data class DopamineFrictionMetrics(
    val effortfulPoints: Int,
    val passivePoints: Int,
    val ratio: Float,
    val statusLabel: String,
    val summary: String
)

data class HormoneProfile(
    val dateKey: String,
    val vitalityScore: Int,
    val testosterone: HormoneBreakdown,
    val cortisol: HormoneBreakdown,
    val dopamine: HormoneBreakdown,
    val serotonin: HormoneBreakdown,
    val frictionMetrics: DopamineFrictionMetrics,
    val overtrainingFlag: Boolean,
    val irregularWakeDetected: Boolean,
    val weeklyResistanceCount: Int,
    val couplingInsight: String
)

data class ResearchPaper(
    val id: String,
    val pathway: HormonePathway,
    val title: String,
    val citation: String,
    val mechanism: String,
    val modelImplementation: String
)

data class SimulationPreset(
    val name: String,
    val subtitle: String,
    val input: DailyHabitInput
)

data class PersonalCorrelation(
    val id: String,
    val title: String,
    val targetPathway: HormonePathway,
    val findingSummary: String,
    val researchContext: String,
    val deltaLabel: String,
    val isDataBacked: Boolean
)

data class SleepPatternSummary(
    val loggedNights: Int,
    val avgDurationHours: Float,
    val avgQuality: Float,
    val avgBedtimeLabel: String,
    val avgWakeTimeLabel: String,
    val optimalNightsCount: Int
)

data class HabitProgressItem(
    val habitId: Int,
    val title: String,
    val pathway: HormonePathway,
    val impactPoints: Int,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val currentStreakDays: Int,
    val last7DaysCompleted: List<Boolean>,
    val totalCompletions: Int
)

data class BioLevel(
    val level: Int,
    val title: String,
    val minCoins: Int,
    val nextLevelCoins: Int?,
    val perkSummary: String
)

data class DailyRewardOutcome(
    val baseCoins: Int,
    val bonusCoins: Int,
    val totalCoins: Int,
    val multiplierLabel: String,
    val reasonSummary: String
)

data class ChallengeItem(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val progressLabel: String,
    val progressRatio: Float,
    val isCompleted: Boolean,
    val isVariableMystery: Boolean = false
)
