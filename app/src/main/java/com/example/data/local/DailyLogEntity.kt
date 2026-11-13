package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlcoholLevel
import com.example.domain.model.DailyHabitInput
import com.example.domain.model.WorkoutType

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val dateKey: String,
    val bedtimeMinutes: Int = 23 * 60,
    val wakeTimeMinutes: Int = 7 * 60,
    val sleepHours: Float,
    val sleepQuality: Int,
    val sleepConsistent: Boolean,
    val consistentWindDown: Boolean = true,
    val morningSunlightMins: Int,
    val morningLightWithin60Mins: Boolean = true,
    val workoutType: String,
    val coldExposure: Boolean,
    val meditationOrBreathwork: Boolean,
    val nutritionClean: Boolean,
    val alcoholLevel: String,
    val hasCaffeine: Boolean = true,
    val lastCaffeineMinutes: Int = 10 * 60,
    val caffeineAfter2Pm: Boolean,
    val doomscrollMins: Int,
    val deepWorkMins: Int,
    val stressLevel: Int,
    val notes: String,
    val testosteroneScore: Int,
    val cortisolScore: Int,
    val dopamineScore: Int,
    val serotoninScore: Int,
    val vitalityScore: Int,
    val dopamineFrictionRatio: Float = 2.0f,
    val overtrainingFlag: Boolean = false,
    val coinsEarned: Int = 0,
    val rewardMultiplierLabel: String = "1.0x Base",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainInput(): DailyHabitInput = DailyHabitInput(
        dateKey = dateKey,
        bedtimeMinutes = bedtimeMinutes,
        wakeTimeMinutes = wakeTimeMinutes,
        sleepHours = sleepHours,
        sleepQuality = sleepQuality,
        sleepConsistent = sleepConsistent,
        consistentWindDown = consistentWindDown,
        morningSunlightMins = morningSunlightMins,
        morningLightWithin60Mins = morningLightWithin60Mins,
        workoutType = runCatching { WorkoutType.valueOf(workoutType) }.getOrDefault(WorkoutType.NONE),
        coldExposure = coldExposure,
        meditationOrBreathwork = meditationOrBreathwork,
        nutritionClean = nutritionClean,
        alcoholLevel = runCatching { AlcoholLevel.valueOf(alcoholLevel) }.getOrDefault(AlcoholLevel.NONE),
        hasCaffeine = hasCaffeine,
        lastCaffeineMinutes = lastCaffeineMinutes,
        caffeineAfter2Pm = caffeineAfter2Pm,
        doomscrollMins = doomscrollMins,
        deepWorkMins = deepWorkMins,
        stressLevel = stressLevel,
        notes = notes
    )

    companion object {
        fun fromDomain(
            input: DailyHabitInput,
            testosteroneScore: Int,
            cortisolScore: Int,
            dopamineScore: Int,
            serotoninScore: Int,
            vitalityScore: Int,
            dopamineFrictionRatio: Float,
            overtrainingFlag: Boolean,
            coinsEarned: Int,
            rewardMultiplierLabel: String
        ): DailyLogEntity = DailyLogEntity(
            dateKey = input.dateKey,
            bedtimeMinutes = input.bedtimeMinutes,
            wakeTimeMinutes = input.wakeTimeMinutes,
            sleepHours = input.sleepHours,
            sleepQuality = input.sleepQuality,
            sleepConsistent = input.sleepConsistent,
            consistentWindDown = input.consistentWindDown,
            morningSunlightMins = input.morningSunlightMins,
            morningLightWithin60Mins = input.morningLightWithin60Mins,
            workoutType = input.workoutType.name,
            coldExposure = input.coldExposure,
            meditationOrBreathwork = input.meditationOrBreathwork,
            nutritionClean = input.nutritionClean,
            alcoholLevel = input.alcoholLevel.name,
            hasCaffeine = input.hasCaffeine,
            lastCaffeineMinutes = input.lastCaffeineMinutes,
            caffeineAfter2Pm = input.caffeineAfter2Pm,
            doomscrollMins = input.doomscrollMins,
            deepWorkMins = input.deepWorkMins,
            stressLevel = input.stressLevel,
            notes = input.notes,
            testosteroneScore = testosteroneScore,
            cortisolScore = cortisolScore,
            dopamineScore = dopamineScore,
            serotoninScore = serotoninScore,
            vitalityScore = vitalityScore,
            dopamineFrictionRatio = dopamineFrictionRatio,
            overtrainingFlag = overtrainingFlag,
            coinsEarned = coinsEarned,
            rewardMultiplierLabel = rewardMultiplierLabel
        )
    }
}
