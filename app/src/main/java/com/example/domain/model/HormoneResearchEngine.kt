package com.example.domain.model

import com.example.data.local.CustomHabitCompletionEntity
import com.example.data.local.CustomHabitEntity
import com.example.data.local.DailyLogEntity
import com.example.data.local.MilestoneRewardEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure Kotlin physiological lifestyle & behavioral gamification engine.
 * Models sleep architecture, late caffeine (8-10h cutoff), irregular wake times,
 * morning outdoor light (<60m), wind-down consistency, 3-4x/wk resistance multiplier,
 * overtraining detection, Dopamine Friction Ratio, and variable/milestone coin rewards.
 */
object HormoneResearchEngine {

    val bioLevels: List<BioLevel> = listOf(
        BioLevel(1, "Circadian Novice", 0, 200, "Building baseline sleep & morning light anchors."),
        BioLevel(2, "Dopamine Apprentice", 200, 500, "Replacing passive feeds with high-friction wins."),
        BioLevel(3, "HPA Balancer", 500, 1000, "Taming cortisol spikes & caffeine cutoffs."),
        BioLevel(4, "Androgen Architect", 1000, 1800, "Compounding 3–4x/wk resistance & 7.5h+ sleep."),
        BioLevel(5, "Endocrine Master", 1800, 3000, "Sustaining HPA–HPG equilibrium across weeks."),
        BioLevel(6, "Apex Bio-Vitality", 3000, null, "Peak multi-week circadian & dopamine resilience.")
    )

    fun computeBioLevel(totalCoins: Int): BioLevel {
        return bioLevels.lastOrNull { totalCoins >= it.minCoins } ?: bioLevels.first()
    }

    fun evaluate(
        input: DailyHabitInput,
        customBonuses: List<CustomHabitBonus> = emptyList(),
        recentHistory: List<DailyHabitInput> = emptyList()
    ): HormoneProfile {
        val irregularWake = detectIrregularWake(input, recentHistory)
        val weeklyResistanceCount = countWeeklyResistance(input, recentHistory)
        val poorSleep = input.sleepHours < 6.5f || input.sleepQuality <= 2
        val highTrainingLoad = input.workoutType.isHighLoad || weeklyResistanceCount >= 5
        val overtrainingFlag = highTrainingLoad && poorSleep
        val frictionMetrics = computeDopamineFriction(input, customBonuses.size)

        val cortisol = calculateCortisol(
            input = input,
            irregularWake = irregularWake,
            overtrainingFlag = overtrainingFlag,
            customBonuses = customBonuses,
            recentHistory = recentHistory
        )
        val testosterone = calculateTestosterone(
            input = input,
            cortisolScore = cortisol.score,
            irregularWake = irregularWake,
            weeklyResistanceCount = weeklyResistanceCount,
            overtrainingFlag = overtrainingFlag,
            customBonuses = customBonuses,
            recentHistory = recentHistory
        )
        val dopamine = calculateDopamine(
            input = input,
            cortisolScore = cortisol.score,
            frictionMetrics = frictionMetrics,
            weeklyResistanceCount = weeklyResistanceCount,
            overtrainingFlag = overtrainingFlag,
            customBonuses = customBonuses
        )
        val serotonin = calculateSerotonin(
            input = input,
            cortisolScore = cortisol.score,
            irregularWake = irregularWake,
            customBonuses = customBonuses
        )

        val cortisolCalmness = when {
            cortisol.score <= 45 -> 90 - abs(35 - cortisol.score)
            else -> (100 - ((cortisol.score - 45) * 1.45)).roundToInt()
        }.coerceIn(0, 100)

        val vitality = (
            testosterone.score * 0.30 +
                dopamine.score * 0.25 +
                serotonin.score * 0.25 +
                cortisolCalmness * 0.20
            ).roundToInt().coerceIn(0, 100)

        val insight = buildCouplingInsight(
            input = input,
            testosterone = testosterone.score,
            cortisol = cortisol.score,
            dopamine = dopamine.score,
            overtrainingFlag = overtrainingFlag,
            irregularWake = irregularWake,
            friction = frictionMetrics
        )

        return HormoneProfile(
            dateKey = input.dateKey,
            vitalityScore = vitality,
            testosterone = testosterone,
            cortisol = cortisol,
            dopamine = dopamine,
            serotonin = serotonin,
            frictionMetrics = frictionMetrics,
            overtrainingFlag = overtrainingFlag,
            irregularWakeDetected = irregularWake,
            weeklyResistanceCount = weeklyResistanceCount,
            couplingInsight = insight
        )
    }

    private fun detectIrregularWake(
        input: DailyHabitInput,
        recentHistory: List<DailyHabitInput>
    ): Boolean {
        if (!input.sleepConsistent) return true
        if (recentHistory.size < 2) return false
        val avgWake = recentHistory.map { it.wakeTimeMinutes }.average().roundToInt()
        return DateKeyUtils.circularTimeDiffMinutes(input.wakeTimeMinutes, avgWake) > 45
    }

    private fun countWeeklyResistance(
        input: DailyHabitInput,
        recentHistory: List<DailyHabitInput>
    ): Int {
        val pastCount = recentHistory.take(6).count { it.workoutType.isHighLoad }
        return pastCount + if (input.workoutType.isHighLoad) 1 else 0
    }

    fun computeDopamineFriction(
        input: DailyHabitInput,
        completedCustomCount: Int = 0
    ): DopamineFrictionMetrics {
        val lightEffort = if (input.morningLightWithin60Mins) input.morningSunlightMins else input.morningSunlightMins / 2
        val effortful = input.deepWorkMins +
            input.workoutType.effortPoints +
            (if (input.coldExposure) 20 else 0) +
            (if (input.meditationOrBreathwork) 20 else 0) +
            lightEffort +
            (completedCustomCount * 15)

        val passive = input.doomscrollMins +
            (if (!input.nutritionClean) 20 else 0) +
            input.alcoholLevel.passivePenalty

        val effectivePassive = maxOf(passive, 15)
        val ratio = ((effortful.toFloat() / effectivePassive.toFloat()) * 10f).roundToInt() / 10f

        val (status, summary) = when {
            ratio >= 2.5f -> "High-Friction Mastery" to "Effortful focus & physical challenge far outweigh passive stimulation."
            ratio >= 1.2f -> "Balanced Ratio" to "Healthy effort-to-consumption ratio sustaining steady D2 sensitivity."
            else -> "Passive Overload" to "Passive consumption exceeds effortful output, risking a dopamine trough."
        }
        return DopamineFrictionMetrics(
            effortfulPoints = effortful,
            passivePoints = passive,
            ratio = ratio,
            statusLabel = status,
            summary = summary
        )
    }

    fun evaluateDailyReward(
        profile: HormoneProfile,
        input: DailyHabitInput,
        completedCustomCount: Int
    ): DailyRewardOutcome {
        var base = 15 // Baseline check-in commitment
        val reasons = mutableListOf<String>()

        if (input.sleepHours >= 7.5f) {
            base += 15
            reasons += "7.5h+ Sleep"
        }
        if (input.morningLightWithin60Mins && input.morningSunlightMins >= 10) {
            base += 10
            reasons += "Morning Light"
        }
        if (input.consistentWindDown) {
            base += 8
            reasons += "Wind-Down"
        }
        if (!input.isLateCaffeinePenalty()) {
            base += 8
            reasons += "Caffeine Cutoff"
        }
        if (input.workoutType != WorkoutType.NONE && !profile.overtrainingFlag) {
            base += 12
            reasons += input.workoutType.label
        }
        if (profile.frictionMetrics.ratio >= 2.0f) {
            base += 12
            reasons += "High Friction (${profile.frictionMetrics.ratio}x)"
        }
        base += (completedCustomCount * 4)

        // Variable / intermittent reinforcement multiplier (deterministic per dateKey + score signature
        // so UI remains stable while delivering unpredictable synergy surges!)
        val seed = abs((input.dateKey + "_" + profile.vitalityScore + "_" + base).hashCode()) % 100
        val (multiplier, label) = when {
            profile.vitalityScore >= 76 && seed < 35 -> 2.0f to "2.0x Mystery Jackpot!"
            profile.frictionMetrics.ratio >= 2.2f && seed < 65 -> 1.5f to "1.5x Dopamine Surge"
            input.morningLightWithin60Mins && input.consistentWindDown && seed < 50 -> 1.35f to "1.35x Circadian Combo"
            else -> 1.0f to "1.0x Base"
        }

        val total = (base * multiplier).roundToInt()
        val bonus = (total - base).coerceAtLeast(0)
        val summary = if (reasons.isEmpty()) {
            "Logged check-in baseline."
        } else {
            reasons.take(4).joinToString(" • ")
        }
        return DailyRewardOutcome(
            baseCoins = base,
            bonusCoins = bonus,
            totalCoins = total,
            multiplierLabel = label,
            reasonSummary = summary
        )
    }

    fun evaluateChallenges(
        todayInput: DailyHabitInput,
        todayProfile: HormoneProfile,
        allLogs: List<DailyLogEntity>,
        unlockedMilestoneIds: Set<String>
    ): List<ChallengeItem> {
        val recent7 = allLogs.take(7)
        val resistanceCount = todayProfile.weeklyResistanceCount
        val goodSleepStreak = recent7.takeWhile { it.sleepHours >= 7.5f }.size
        val highFrictionStreak = recent7.takeWhile { it.dopamineFrictionRatio >= 2.0f }.size
        val circadianAnchorStreak = recent7.takeWhile { it.morningLightWithin60Mins && it.sleepConsistent }.size

        return listOf(
            ChallengeItem(
                id = "quest_morning_light",
                title = "60-Min Morning Light Anchor",
                description = "Get ≥10m outdoor sunlight within 60 mins of waking + consistent wake time.",
                rewardCoins = 25,
                progressLabel = if (todayInput.morningLightWithin60Mins && !todayProfile.irregularWakeDetected) "Completed" else "Pending",
                progressRatio = if (todayInput.morningLightWithin60Mins && !todayProfile.irregularWakeDetected) 1f else 0.25f,
                isCompleted = todayInput.morningLightWithin60Mins && !todayProfile.irregularWakeDetected
            ),
            ChallengeItem(
                id = "quest_caffeine_winddown",
                title = "Caffeine Curfew & Wind-Down",
                description = "Keep caffeine ≥10h before bed and complete your wind-down window.",
                rewardCoins = 25,
                progressLabel = if (!todayInput.isLateCaffeinePenalty() && todayInput.consistentWindDown) "Completed" else "In Progress",
                progressRatio = ((if (!todayInput.isLateCaffeinePenalty()) 0.5f else 0f) + (if (todayInput.consistentWindDown) 0.5f else 0f)),
                isCompleted = !todayInput.isLateCaffeinePenalty() && todayInput.consistentWindDown
            ),
            ChallengeItem(
                id = "quest_friction_mystery",
                title = "Dopamine Friction Mastery (Variable Chest)",
                description = "Reach ≥ 2.5x Effort-to-Passive Dopamine Friction Ratio for a chance at 1.5x–2.0x coin surges.",
                rewardCoins = 60,
                progressLabel = "${todayProfile.frictionMetrics.ratio}x / 2.5x",
                progressRatio = (todayProfile.frictionMetrics.ratio / 2.5f).coerceIn(0f, 1f),
                isCompleted = todayProfile.frictionMetrics.ratio >= 2.5f,
                isVariableMystery = true
            ),
            ChallengeItem(
                id = "milestone_resistance_3x",
                title = "3–4x/Wk Resistance Microcycle",
                description = "Hit 3–4 Strength/HIIT sessions in 7 days without triggering Overtraining.",
                rewardCoins = 120,
                progressLabel = "${resistanceCount.coerceAtMost(4)} / 3 sessions",
                progressRatio = (resistanceCount / 3f).coerceIn(0f, 1f),
                isCompleted = (resistanceCount in 3..4) && !todayProfile.overtrainingFlag || "milestone_resistance_3x" in unlockedMilestoneIds
            ),
            ChallengeItem(
                id = "milestone_sleep_3d",
                title = "3-Night 7.5h+ Sleep Streak",
                description = "Chain 3 restorative nights ≥ 7.5h to maximize nocturnal LH & testosterone pulses.",
                rewardCoins = 150,
                progressLabel = "${goodSleepStreak.coerceAtMost(3)} / 3 nights",
                progressRatio = (goodSleepStreak / 3f).coerceIn(0f, 1f),
                isCompleted = goodSleepStreak >= 3 || "milestone_sleep_3d" in unlockedMilestoneIds
            ),
            ChallengeItem(
                id = "milestone_circadian_3d",
                title = "Circadian Anchor Streak",
                description = "3 consecutive days of morning light <60m + consistent wake timing (and ${highFrictionStreak}d friction streak).",
                rewardCoins = 110,
                progressLabel = "${circadianAnchorStreak.coerceAtMost(3)} / 3 days",
                progressRatio = (circadianAnchorStreak / 3f).coerceIn(0f, 1f),
                isCompleted = circadianAnchorStreak >= 3 || "milestone_circadian_3d" in unlockedMilestoneIds
            )
        )
    }

    fun detectNewlyUnlockedMilestones(
        allLogs: List<DailyLogEntity>,
        alreadyUnlockedIds: Set<String>
    ): List<MilestoneRewardEntity> {
        val newlyUnlocked = mutableListOf<MilestoneRewardEntity>()
        val recent7 = allLogs.take(7)

        if ("milestone_first_checkin" !in alreadyUnlockedIds && allLogs.isNotEmpty()) {
            newlyUnlocked += MilestoneRewardEntity(
                milestoneId = "milestone_first_checkin",
                title = "First Endocrine Check-In",
                description = "Initialized your local bio-circadian baseline.",
                coinsAwarded = 50
            )
        }

        val resistanceCount = recent7.count {
            it.workoutType == WorkoutType.RESISTANCE.name || it.workoutType == WorkoutType.HIIT.name
        }
        val anyOvertraining = recent7.any { it.overtrainingFlag }
        if ("milestone_resistance_3x" !in alreadyUnlockedIds && resistanceCount in 3..4 && !anyOvertraining) {
            newlyUnlocked += MilestoneRewardEntity(
                milestoneId = "milestone_resistance_3x",
                title = "3–4x/Wk Resistance Multiplier",
                description = "Completed optimal weekly strength frequency with recovered sleep.",
                coinsAwarded = 120
            )
        }

        val sleepStreak = recent7.takeWhile { it.sleepHours >= 7.5f }.size
        if ("milestone_sleep_3d" !in alreadyUnlockedIds && sleepStreak >= 3) {
            newlyUnlocked += MilestoneRewardEntity(
                milestoneId = "milestone_sleep_3d",
                title = "3-Night Sleep Architecture Streak",
                description = "3 straight nights ≥ 7.5h protecting nocturnal testosterone synthesis.",
                coinsAwarded = 150
            )
        }

        val frictionStreak = recent7.takeWhile { it.dopamineFrictionRatio >= 2.5f }.size
        if ("milestone_friction_3d" !in alreadyUnlockedIds && frictionStreak >= 3) {
            newlyUnlocked += MilestoneRewardEntity(
                milestoneId = "milestone_friction_3d",
                title = "High-Friction Dopamine Streak",
                description = "Maintained ≥ 2.5x Effort-to-Passive ratio for 3 consecutive days.",
                coinsAwarded = 140
            )
        }

        val circadianStreak = recent7.takeWhile { it.morningLightWithin60Mins && it.sleepConsistent }.size
        if ("milestone_circadian_3d" !in alreadyUnlockedIds && circadianStreak >= 3) {
            newlyUnlocked += MilestoneRewardEntity(
                milestoneId = "milestone_circadian_3d",
                title = "Circadian Anchor Master",
                description = "3 days of morning outdoor light within 60m & consistent wake schedule.",
                coinsAwarded = 110
            )
        }

        return newlyUnlocked
    }

    private fun calculateCortisol(
        input: DailyHabitInput,
        irregularWake: Boolean,
        overtrainingFlag: Boolean,
        customBonuses: List<CustomHabitBonus>,
        recentHistory: List<DailyHabitInput>
    ): HormoneBreakdown {
        val factors = mutableListOf<FactorContribution>()
        var score = 36

        // Non-linear sleep deprivation penalty below 6.5h
        val sleepDelta = when {
            input.sleepHours >= 7.5f -> -7
            input.sleepHours >= 6.5f -> 4
            else -> {
                val deficit = 6.5f - input.sleepHours
                (10f + deficit * deficit * 11f).roundToInt()
            }
        }
        factors += FactorContribution(
            label = "Sleep Duration (${DateKeyUtils.formatDurationHours(input.sleepHours)})",
            delta = sleepDelta,
            detail = if (input.sleepHours < 6.5f) {
                "Non-linear HPA stress surge below 6.5h sleep"
            } else {
                "Restorative sleep buffers HPA reactivity"
            }
        )
        score += sleepDelta

        // Late caffeine within 8-10h of bedtime
        if (input.isLateCaffeinePenalty()) {
            val gap = input.hoursCaffeineBeforeBed()
            val cafDelta = if (gap < 8.0f) 14 else 9
            factors += FactorContribution(
                label = "Late Caffeine (${String.format(Locale.US, "%.1fh", gap)} before bed)",
                delta = cafDelta,
                detail = "Caffeine within 8–10h of sleep prolongs evening cortisol & blocks adenosine"
            )
            score += cafDelta
        } else if (input.hasCaffeine) {
            factors += FactorContribution(
                label = "Caffeine Cutoff Respected",
                delta = -2,
                detail = "Last caffeine ≥ 10h before bedtime protects evening cortisol drop"
            )
            score -= 2
        }

        // Irregular wake time penalty
        if (irregularWake) {
            factors += FactorContribution(
                label = "Irregular Wake Timing",
                delta = 7,
                detail = "Wake-up shift >45m desynchronizes the morning Cortisol Awakening Response"
            )
            score += 7
        }

        // Consistent wind-down window reward
        if (input.consistentWindDown) {
            factors += FactorContribution(
                label = "Consistent Wind-Down Window",
                delta = -7,
                detail = "Pre-bed screen/light taper lowers evening sympathetic arousal"
            )
            score -= 7
        }

        // Morning light within 60m anchors CAR
        if (input.morningLightWithin60Mins && input.morningSunlightMins >= 10) {
            factors += FactorContribution(
                label = "Morning Light <60m of Waking",
                delta = -4,
                detail = "Anchors healthy morning CAR peak and faster evening decline"
            )
            score -= 4
        }

        val stressDelta = (input.stressLevel - 2) * 8
        if (stressDelta != 0) {
            factors += FactorContribution(
                label = "Perceived Stress (${input.stressLevel}/5)",
                delta = stressDelta,
                detail = if (stressDelta > 0) "Sympathetic arousal stimulates CRH/ACTH release" else "Low psychological strain keeps baseline calm"
            )
            score += stressDelta
        }

        if (input.meditationOrBreathwork) {
            factors += FactorContribution(
                label = "Breathwork / Mindfulness",
                delta = -10,
                detail = "Vagal parasympathetic activation downregulates HPA load"
            )
            score -= 10
        }

        if (input.workoutType.cortisolDelta != 0) {
            factors += FactorContribution(
                label = input.workoutType.label,
                delta = input.workoutType.cortisolDelta,
                detail = if (input.workoutType.cortisolDelta > 0) "Acute training stress spike" else "Aerobic/parasympathetic cortisol clearance"
            )
            score += input.workoutType.cortisolDelta
        }

        if (overtrainingFlag) {
            factors += FactorContribution(
                label = "Overtraining Flag (Load + Poor Sleep)",
                delta = 15,
                detail = "High training load paired with <6.5h or poor sleep spikes catabolic cortisol"
            )
            score += 15
        }

        if (input.alcoholLevel != AlcoholLevel.NONE) {
            factors += FactorContribution(
                label = "Alcohol (${input.alcoholLevel.label})",
                delta = input.alcoholLevel.cortisolDelta,
                detail = "Ethanol metabolism triggers rebound cortisol surges"
            )
            score += input.alcoholLevel.cortisolDelta
        }

        if (recentHistory.size >= 2) {
            val avgSleep = recentHistory.map { it.sleepHours }.average()
            if (avgSleep < 6.3) {
                factors += FactorContribution(
                    label = "Multi-Day Sleep Debt",
                    delta = 6,
                    detail = "Rolling avg ${String.format(Locale.US, "%.1fh", avgSleep)} sustains higher HPA tone"
                )
                score += 6
            }
        }

        customBonuses.filter { it.pathway == HormonePathway.CORTISOL }.forEach { bonus ->
            val delta = -bonus.impactPoints
            factors += FactorContribution(
                label = bonus.title,
                delta = delta,
                detail = "Custom stress-buffering habit"
            )
            score += delta
        }

        val finalScore = score.coerceIn(10, 99)
        val status = when {
            finalScore < 30 -> "Very Low"
            finalScore <= 48 -> "Balanced Diurnal"
            finalScore <= 65 -> "Elevated Load"
            else -> "High HPA Strain"
        }
        val summary = when {
            overtrainingFlag -> "Overtraining strain detected—prioritize rest & 8h sleep tonight."
            finalScore <= 48 -> "Healthy morning curve with low chronic HPA strain."
            finalScore <= 65 -> "Mild stress elevation; cut caffeine >10h before bed and wind down."
            else -> "Elevated cortisol load is actively suppressing anabolic & dopamine tone."
        }
        return HormoneBreakdown(
            pathway = HormonePathway.CORTISOL,
            score = finalScore,
            statusLabel = status,
            summary = summary,
            contributions = factors
        )
    }

    private fun calculateTestosterone(
        input: DailyHabitInput,
        cortisolScore: Int,
        irregularWake: Boolean,
        weeklyResistanceCount: Int,
        overtrainingFlag: Boolean,
        customBonuses: List<CustomHabitBonus>,
        recentHistory: List<DailyHabitInput>
    ): HormoneBreakdown {
        val factors = mutableListOf<FactorContribution>()
        var score = 42

        // Heavy non-linear penalty below 6.5 hrs
        val sleepDelta = when {
            input.sleepHours >= 8.0f -> 26
            input.sleepHours >= 7.5f -> 22
            input.sleepHours >= 7.0f -> 12
            input.sleepHours >= 6.5f -> 4
            else -> {
                val deficit = 6.5f - input.sleepHours
                -((8f + deficit * deficit * 14f).roundToInt())
            }
        }
        factors += FactorContribution(
            label = "Sleep Duration (${DateKeyUtils.formatDurationHours(input.sleepHours)})",
            delta = sleepDelta,
            detail = if (input.sleepHours < 6.5f) {
                "Non-linear suppression below 6.5h cuts nocturnal LH & testosterone pulses"
            } else {
                "Optimal nocturnal sleep window maximizes androgen synthesis"
            }
        )
        score += sleepDelta

        if (irregularWake) {
            factors += FactorContribution(
                label = "Irregular Wake Time",
                delta = -8,
                detail = "Circadian wake drift dampens morning GnRH/testosterone peak"
            )
            score -= 8
        }

        if (input.consistentWindDown) {
            factors += FactorContribution(
                label = "Consistent Wind-Down Window",
                delta = 5,
                detail = "Supports deeper Stage 3 slow-wave sleep onset"
            )
            score += 5
        }

        if (input.isLateCaffeinePenalty()) {
            factors += FactorContribution(
                label = "Late Caffeine Sleep Disruption",
                delta = -5,
                detail = "Caffeine within 8–10h of bed reduces restorative slow-wave sleep"
            )
            score -= 5
        }

        if (input.workoutType != WorkoutType.NONE) {
            factors += FactorContribution(
                label = input.workoutType.label,
                delta = input.workoutType.testosteroneDelta,
                detail = "Compound mechanical tension boosts acute T & receptor density"
            )
            score += input.workoutType.testosteroneDelta
        }

        // Positive multiplier for 3-4x/week resistance sessions (when recovered)
        if (!overtrainingFlag && weeklyResistanceCount in 3..4 && input.workoutType.isHighLoad) {
            factors += FactorContribution(
                label = "3–4x/Wk Resistance Multiplier",
                delta = 7,
                detail = "Sweet-spot weekly frequency ($weeklyResistanceCount/7d) amplifies androgen receptor sensitivity"
            )
            score += 7
        }

        // Overtraining negative modifier when high load pairs with poor sleep
        if (overtrainingFlag) {
            factors += FactorContribution(
                label = "Overtraining Flag (Load + Poor Sleep)",
                delta = -16,
                detail = "Insufficient sleep recovery turns heavy training into catabolic T suppression"
            )
            score -= 16
        }

        // Morning sunlight within 60m of waking
        val sunDelta = when {
            input.morningLightWithin60Mins && input.morningSunlightMins >= 20 -> 9
            input.morningLightWithin60Mins && input.morningSunlightMins >= 10 -> 6
            input.morningSunlightMins >= 20 -> 4
            else -> 0
        }
        if (sunDelta > 0) {
            factors += FactorContribution(
                label = if (input.morningLightWithin60Mins) "Morning Light <60m (${input.morningSunlightMins}m)" else "Daylight (${input.morningSunlightMins}m)",
                delta = sunDelta,
                detail = "Early outdoor light + vitamin D pathway support Leydig output"
            )
            score += sunDelta
        }

        val nutritionDelta = if (input.nutritionClean) 9 else -5
        factors += FactorContribution(
            label = if (input.nutritionClean) "Whole Protein & Fats" else "Processed / Low Micronutrients",
            delta = nutritionDelta,
            detail = "Cholesterol, zinc & magnesium serve as steroidogenic precursors"
        )
        score += nutritionDelta

        if (input.alcoholLevel != AlcoholLevel.NONE) {
            factors += FactorContribution(
                label = "Alcohol (${input.alcoholLevel.label})",
                delta = input.alcoholLevel.testosteroneDelta,
                detail = "Increases aromatase & suppresses testicular steroidogenesis"
            )
            score += input.alcoholLevel.testosteroneDelta
        }

        val hpaDelta = when {
            cortisolScore > 58 -> -((cortisolScore - 58) * 0.48).roundToInt()
            cortisolScore <= 42 -> 4
            else -> 0
        }
        if (hpaDelta != 0) {
            factors += FactorContribution(
                label = if (hpaDelta < 0) "Cortisol HPA Suppression" else "Calm HPA Synergy",
                delta = hpaDelta,
                detail = if (hpaDelta < 0) {
                    "Elevated cortisol ($cortisolScore) blunts GnRH/LH signaling"
                } else {
                    "Low stress tone supports unobstructed HPG signaling"
                }
            )
            score += hpaDelta
        }

        if (recentHistory.size >= 3 && !overtrainingFlag) {
            val goodSleepDays = recentHistory.count { it.sleepHours >= 7.5f }
            if (goodSleepDays >= 3) {
                factors += FactorContribution(
                    label = "Sleep Consistency Momentum",
                    delta = 4,
                    detail = "Multi-day restorative sleep compounds HPG tone"
                )
                score += 4
            }
        }

        customBonuses.filter { it.pathway == HormonePathway.TESTOSTERONE }.forEach { bonus ->
            factors += FactorContribution(
                label = bonus.title,
                delta = bonus.impactPoints,
                detail = "Custom vitality habit"
            )
            score += bonus.impactPoints
        }

        val finalScore = score.coerceIn(5, 99)
        val status = when {
            overtrainingFlag -> "Overtraining Risk"
            finalScore >= 78 -> "Peak Anabolic"
            finalScore >= 60 -> "Optimal Range"
            finalScore >= 42 -> "Moderate"
            else -> "Suppressed"
        }
        val summary = when {
            overtrainingFlag -> "High training load + poor sleep triggered an Overtraining Flag."
            finalScore >= 75 -> "Strong sleep, optimal 3–4x training rhythm, and calm HPA balance."
            finalScore >= 55 -> "Solid foundation; hit 7.5h+ sleep and morning light <60m to peak."
            else -> "Recover sleep debt and eliminate late caffeine to restore HPG drive."
        }
        return HormoneBreakdown(
            pathway = HormonePathway.TESTOSTERONE,
            score = finalScore,
            statusLabel = status,
            summary = summary,
            contributions = factors
        )
    }

    private fun calculateDopamine(
        input: DailyHabitInput,
        cortisolScore: Int,
        frictionMetrics: DopamineFrictionMetrics,
        weeklyResistanceCount: Int,
        overtrainingFlag: Boolean,
        customBonuses: List<CustomHabitBonus>
    ): HormoneBreakdown {
        val factors = mutableListOf<FactorContribution>()
        var score = 45

        val sleepQDelta = (input.sleepQuality - 3) * 5 + when {
            input.sleepHours >= 7.5f -> 6
            input.sleepHours < 6.5f -> -10
            else -> 0
        }
        factors += FactorContribution(
            label = "Sleep Restoration (${input.sleepQuality}/5)",
            delta = sleepQDelta,
            detail = "Deep sleep replenishes striatal dopamine & D2 sensitivity"
        )
        score += sleepQDelta

        // Dopamine Friction Ratio adjustment
        val frictionDelta = when {
            frictionMetrics.ratio >= 2.5f -> 14
            frictionMetrics.ratio >= 1.5f -> 7
            frictionMetrics.ratio >= 1.0f -> 0
            else -> -15
        }
        factors += FactorContribution(
            label = "Dopamine Friction Ratio (${frictionMetrics.ratio}x)",
            delta = frictionDelta,
            detail = "${frictionMetrics.effortfulPoints} effortful vs. ${frictionMetrics.passivePoints} passive (${frictionMetrics.statusLabel})"
        )
        score += frictionDelta

        if (input.coldExposure) {
            factors += FactorContribution(
                label = "Cold Exposure",
                delta = 9,
                detail = "Sustained norepinephrine & tonic dopamine release without crash"
            )
            score += 9
        }

        if (input.workoutType.dopamineDelta != 0) {
            factors += FactorContribution(
                label = input.workoutType.label,
                delta = input.workoutType.dopamineDelta,
                detail = "Movement upregulates tyrosine hydroxylase & D2 receptors"
            )
            score += input.workoutType.dopamineDelta
        }

        if (!overtrainingFlag && weeklyResistanceCount in 3..4 && input.workoutType.isHighLoad) {
            factors += FactorContribution(
                label = "3–4x/Wk Training Rhythm",
                delta = 5,
                detail = "Consistent resistance cadence supports baseline drive"
            )
            score += 5
        }

        if (overtrainingFlag) {
            factors += FactorContribution(
                label = "Overtraining CNS Fatigue",
                delta = -10,
                detail = "High load + poor sleep depletes catecholamine drive"
            )
            score -= 10
        }

        if (input.morningLightWithin60Mins && input.morningSunlightMins >= 10) {
            factors += FactorContribution(
                label = "Morning Outdoor Light <60m",
                delta = 7,
                detail = "Retinal-SCN pathway primes daytime alertness & catecholamines"
            )
            score += 7
        }

        if (input.alcoholLevel != AlcoholLevel.NONE) {
            factors += FactorContribution(
                label = "Alcohol (${input.alcoholLevel.label})",
                delta = input.alcoholLevel.dopamineDelta,
                detail = "Acute GABA/dopamine spike followed by next-day depletion"
            )
            score += input.alcoholLevel.dopamineDelta
        }

        if (cortisolScore > 70) {
            factors += FactorContribution(
                label = "High Cortisol Blunting",
                delta = -6,
                detail = "Chronic stress impairs reward-circuit motivation"
            )
            score -= 6
        }

        customBonuses.filter { it.pathway == HormonePathway.DOPAMINE }.forEach { bonus ->
            factors += FactorContribution(
                label = bonus.title,
                delta = bonus.impactPoints,
                detail = "Custom focus/motivation habit"
            )
            score += bonus.impactPoints
        }

        val finalScore = score.coerceIn(5, 99)
        val status = when {
            finalScore >= 76 -> "High Tonic Drive"
            finalScore >= 58 -> "Steady Focus"
            finalScore >= 40 -> "Fluctuating"
            else -> "Depleted Trough"
        }
        val summary = when {
            finalScore >= 72 -> "Strong tonic motivation backed by a ${frictionMetrics.ratio}x Friction Ratio."
            finalScore >= 52 -> "Balanced baseline; raise effortful focus to push Friction Ratio ≥ 2.5x."
            else -> "Passive stimulation or poor sleep is blunting D2 sensitivity."
        }
        return HormoneBreakdown(
            pathway = HormonePathway.DOPAMINE,
            score = finalScore,
            statusLabel = status,
            summary = summary,
            contributions = factors
        )
    }

    private fun calculateSerotonin(
        input: DailyHabitInput,
        cortisolScore: Int,
        irregularWake: Boolean,
        customBonuses: List<CustomHabitBonus>
    ): HormoneBreakdown {
        val factors = mutableListOf<FactorContribution>()
        var score = 44

        val sunDelta = when {
            input.morningLightWithin60Mins && input.morningSunlightMins >= 20 -> 16
            input.morningLightWithin60Mins && input.morningSunlightMins >= 10 -> 12
            input.morningSunlightMins >= 10 -> 7
            else -> -4
        }
        factors += FactorContribution(
            label = if (input.morningLightWithin60Mins) "Morning Light <60m (${input.morningSunlightMins}m)" else "Daylight (${input.morningSunlightMins}m)",
            delta = sunDelta,
            detail = "Early bright outdoor light stimulates raphe nuclei serotonin synthesis"
        )
        score += sunDelta

        val sleepDelta = (input.sleepQuality - 3) * 5 + if (input.sleepHours >= 7.0f) 6 else -8
        factors += FactorContribution(
            label = "Sleep Quality (${input.sleepQuality}/5)",
            delta = sleepDelta,
            detail = "Restorative sleep stabilizes mood & melatonin conversion"
        )
        score += sleepDelta

        if (input.consistentWindDown) {
            factors += FactorContribution(
                label = "Consistent Wind-Down Window",
                delta = 7,
                detail = "Evening light dimming supports serotonin-to-melatonin conversion"
            )
            score += 7
        }

        if (irregularWake) {
            factors += FactorContribution(
                label = "Irregular Wake Drift",
                delta = -5,
                detail = "Shifting wake times blunts circadian mood stability"
            )
            score -= 5
        }

        if (input.meditationOrBreathwork) {
            factors += FactorContribution(
                label = "Mindfulness / Breathwork",
                delta = 9,
                detail = "Promotes parasympathetic calm & emotional regulation"
            )
            score += 9
        }

        if (input.workoutType.serotoninDelta != 0) {
            factors += FactorContribution(
                label = input.workoutType.label,
                delta = input.workoutType.serotoninDelta,
                detail = "Rhythmic aerobic & resistance movement boosts tryptophan uptake"
            )
            score += input.workoutType.serotoninDelta
        }

        if (input.nutritionClean) {
            factors += FactorContribution(
                label = "Whole-Food Nutrition",
                delta = 6,
                detail = "Tryptophan + B-vitamin cofactors support neurotransmitter synthesis"
            )
            score += 6
        }

        if (input.alcoholLevel != AlcoholLevel.NONE) {
            factors += FactorContribution(
                label = "Alcohol (${input.alcoholLevel.label})",
                delta = input.alcoholLevel.serotoninDelta,
                detail = "Disrupts REM & lowers next-day serotonergic stability"
            )
            score += input.alcoholLevel.serotoninDelta
        }

        if (cortisolScore > 65) {
            factors += FactorContribution(
                label = "Elevated Cortisol",
                delta = -7,
                detail = "HPA overactivation diverts tryptophan down kynurenine pathway"
            )
            score -= 7
        }

        customBonuses.filter { it.pathway == HormonePathway.SEROTONIN }.forEach { bonus ->
            factors += FactorContribution(
                label = bonus.title,
                delta = bonus.impactPoints,
                detail = "Custom recovery habit"
            )
            score += bonus.impactPoints
        }

        val finalScore = score.coerceIn(5, 99)
        val status = when {
            finalScore >= 76 -> "Calm & Resilient"
            finalScore >= 58 -> "Balanced Mood"
            finalScore >= 40 -> "Needs Recovery"
            else -> "Low Reserve"
        }
        val summary = when {
            finalScore >= 70 -> "Well-entrained circadian mood and parasympathetic recovery."
            else -> "Combine morning outdoor light <60m and a wind-down window to lift recovery."
        }
        return HormoneBreakdown(
            pathway = HormonePathway.SEROTONIN,
            score = finalScore,
            statusLabel = status,
            summary = summary,
            contributions = factors
        )
    }

    private fun buildCouplingInsight(
        input: DailyHabitInput,
        testosterone: Int,
        cortisol: Int,
        dopamine: Int,
        overtrainingFlag: Boolean,
        irregularWake: Boolean,
        friction: DopamineFrictionMetrics
    ): String {
        val dur = DateKeyUtils.formatDurationHours(input.sleepHours)
        return when {
            overtrainingFlag ->
                "OVERTRAINING FLAG: High training load paired with poor sleep ($dur, Q${input.sleepQuality}/5) is driving Cortisol ($cortisol) up and suppressing Testosterone ($testosterone). Swap heavy lifting for Zone-2/Mobility until sleep recovers."
            input.isLateCaffeinePenalty() && cortisol > 55 ->
                "Late Caffeine Cutoff Alert: Caffeine ${String.format(Locale.US, "%.1fh", input.hoursCaffeineBeforeBed())} before bed (within 8–10h window) is elevating Cortisol ($cortisol) and fragmenting deep sleep."
            irregularWake ->
                "Circadian Drift Detected: Irregular wake timing shifts your morning cortisol curve and dampens Testosterone ($testosterone). Anchor wake-up within ±45m and get outdoor light within 60m."
            friction.ratio < 1.0f ->
                "Dopamine Friction Warning (${friction.ratio}x): Passive consumption (${friction.passivePoints}) exceeds effortful habits (${friction.effortfulPoints}), pulling Dopamine ($dopamine) into a trough."
            testosterone >= 75 && cortisol <= 48 ->
                "Anabolic Equilibrium: $dur sleep, calm cortisol ($cortisol), and ${friction.ratio}x Dopamine Friction Ratio are reinforcing peak Testosterone ($testosterone)."
            else ->
                "Steady Rhythm: Aim for ≥ 2.5x Dopamine Friction Ratio and morning light within 60m of waking to unlock bonus surge coins."
        }
    }

    fun summarizeSleepPatterns(logs: List<DailyLogEntity>): SleepPatternSummary? {
        if (logs.isEmpty()) return null
        val avgDuration = logs.map { it.sleepHours }.average().toFloat()
        val avgQuality = logs.map { it.sleepQuality }.average().toFloat()
        val optimalCount = logs.count { it.sleepHours >= 7.5f }

        val avgBedWrapped = logs.map { log ->
            if (log.bedtimeMinutes < 12 * 60) log.bedtimeMinutes + 1440 else log.bedtimeMinutes
        }.average().roundToInt() % 1440

        val avgWake = logs.map { it.wakeTimeMinutes }.average().roundToInt() % 1440

        return SleepPatternSummary(
            loggedNights = logs.size,
            avgDurationHours = avgDuration,
            avgQuality = avgQuality,
            avgBedtimeLabel = DateKeyUtils.formatMinutesOfDay(avgBedWrapped),
            avgWakeTimeLabel = DateKeyUtils.formatMinutesOfDay(avgWake),
            optimalNightsCount = optimalCount
        )
    }

    fun analyzePersonalCorrelations(
        logs: List<DailyLogEntity>,
        customHabits: List<CustomHabitEntity> = emptyList(),
        completions: List<CustomHabitCompletionEntity> = emptyList()
    ): List<PersonalCorrelation> {
        val correlations = mutableListOf<PersonalCorrelation>()

        val goodSleep = logs.filter { it.sleepHours >= 7.5f }
        val shortSleep = logs.filter { it.sleepHours < 6.5f }
        if (goodSleep.isNotEmpty() && shortSleep.isNotEmpty()) {
            val avgTGood = goodSleep.map { it.testosteroneScore }.average().roundToInt()
            val avgTShort = shortSleep.map { it.testosteroneScore }.average().roundToInt()
            val avgCGood = goodSleep.map { it.cortisolScore }.average().roundToInt()
            val avgCShort = shortSleep.map { it.cortisolScore }.average().roundToInt()
            val diffT = avgTGood - avgTShort
            correlations += PersonalCorrelation(
                id = "corr_sleep",
                title = "Sleep Duration (<6.5h Penalty vs ≥7.5h)",
                targetPathway = HormonePathway.TESTOSTERONE,
                findingSummary = "On ≥7.5h nights (${goodSleep.size}d), Testosterone averaged $avgTGood vs. $avgTShort on <6.5h nights, while Cortisol dropped from $avgCShort to $avgCGood.",
                researchContext = "Leproult & Van Cauter (JAMA 2011): Non-linear suppression below 6.5h cuts nocturnal LH pulses.",
                deltaLabel = if (diffT >= 0) "+$diffT T" else "$diffT T",
                isDataBacked = true
            )
        } else {
            correlations += PersonalCorrelation(
                id = "corr_sleep_base",
                title = "Sleep Duration (<6.5h Penalty vs ≥7.5h)",
                targetPathway = HormonePathway.TESTOSTERONE,
                findingSummary = "Log both ≥7.5h restorative nights and <6.5h nights to measure your personal non-linear sleep delta.",
                researchContext = "Research baseline: Sleep <6.5h triggers steep HPA cortisol spikes and sharp testosterone suppression.",
                deltaLabel = "Research",
                isDataBacked = false
            )
        }

        val resistanceDays = logs.filter {
            it.workoutType == WorkoutType.RESISTANCE.name || it.workoutType == WorkoutType.HIIT.name
        }
        val restDays = logs.filter { it.workoutType == WorkoutType.NONE.name }
        if (resistanceDays.isNotEmpty() && restDays.isNotEmpty()) {
            val avgTTrain = resistanceDays.map { it.testosteroneScore }.average().roundToInt()
            val avgTRest = restDays.map { it.testosteroneScore }.average().roundToInt()
            val diff = avgTTrain - avgTRest
            correlations += PersonalCorrelation(
                id = "corr_exercise",
                title = "Strength / HIIT vs Rest Days",
                targetPathway = HormonePathway.TESTOSTERONE,
                findingSummary = "Training days (${resistanceDays.size}d) averaged $avgTTrain Testosterone Vitality vs. $avgTRest on rest days.",
                researchContext = "Kraemer & Ratamess (2005): 3–4x/wk resistance sessions amplify androgen receptor density when sleep is recovered.",
                deltaLabel = if (diff >= 0) "+$diff T" else "$diff T",
                isDataBacked = true
            )
        }

        val highFrictionDays = logs.filter { it.dopamineFrictionRatio >= 2.0f }
        val lowFrictionDays = logs.filter { it.dopamineFrictionRatio < 1.2f }
        if (highFrictionDays.isNotEmpty() && lowFrictionDays.isNotEmpty()) {
            val avgDHigh = highFrictionDays.map { it.dopamineScore }.average().roundToInt()
            val avgDLow = lowFrictionDays.map { it.dopamineScore }.average().roundToInt()
            val diff = avgDHigh - avgDLow
            correlations += PersonalCorrelation(
                id = "corr_friction",
                title = "Dopamine Friction Ratio -> Baseline Drive",
                targetPathway = HormonePathway.DOPAMINE,
                findingSummary = "High-friction days ≥2.0x (${highFrictionDays.size}d) averaged $avgDHigh Dopamine vs. $avgDLow on passive-overload days.",
                researchContext = "Schultz & Volkow: Effortful effort protects D2 receptor sensitivity while passive feeds drive post-spike troughs.",
                deltaLabel = if (diff >= 0) "+$diff Dopa" else "$diff Dopa",
                isDataBacked = true
            )
        }

        val highStressDays = logs.filter { it.stressLevel >= 4 }
        val calmDays = logs.filter { it.stressLevel <= 2 }
        if (highStressDays.isNotEmpty() && calmDays.isNotEmpty()) {
            val avgCHigh = highStressDays.map { it.cortisolScore }.average().roundToInt()
            val avgCCalm = calmDays.map { it.cortisolScore }.average().roundToInt()
            val diff = avgCHigh - avgCCalm
            correlations += PersonalCorrelation(
                id = "corr_stress",
                title = "Daily Stress -> Cortisol & HPG Suppression",
                targetPathway = HormonePathway.CORTISOL,
                findingSummary = "High-stress days (${highStressDays.size}d) elevated Cortisol Load to $avgCHigh vs. $avgCCalm on calm days.",
                researchContext = "Toufexis et al. (2014): Sustained cortisol >58 suppresses GnRH/testosterone via HPA–HPG cross-talk.",
                deltaLabel = if (diff >= 0) "+$diff Cort" else "$diff Cort",
                isDataBacked = true
            )
        }

        if (logs.size >= 2 && customHabits.isNotEmpty()) {
            val completedMap = completions.filter { it.completed }.groupBy { it.habitId }
            customHabits.take(2).forEach { habit ->
                val doneDates = completedMap[habit.id]?.map { it.dateKey }?.toSet() ?: emptySet()
                val doneLogs = logs.filter { it.dateKey in doneDates }
                val missedLogs = logs.filter { it.dateKey !in doneDates }
                if (doneLogs.isNotEmpty() && missedLogs.isNotEmpty()) {
                    val pathway = habit.toPathway()
                    val extractor: (DailyLogEntity) -> Int = when (pathway) {
                        HormonePathway.TESTOSTERONE -> { l -> l.testosteroneScore }
                        HormonePathway.CORTISOL -> { l -> l.cortisolScore }
                        HormonePathway.DOPAMINE -> { l -> l.dopamineScore }
                        HormonePathway.SEROTONIN -> { l -> l.serotoninScore }
                    }
                    val avgDone = doneLogs.map(extractor).average().roundToInt()
                    val avgMissed = missedLogs.map(extractor).average().roundToInt()
                    val diff = avgDone - avgMissed
                    correlations += PersonalCorrelation(
                        id = "corr_habit_${habit.id}",
                        title = "Habit: ${habit.title}",
                        targetPathway = pathway,
                        findingSummary = "On days you completed \"${habit.title}\" (${doneLogs.size}d), ${pathway.shortLabel} averaged $avgDone vs. $avgMissed when missed.",
                        researchContext = "Personal Room history correlation across your logged check-ins.",
                        deltaLabel = "${if (diff >= 0) "+" else ""}$diff ${pathway.shortLabel.take(4)}",
                        isDataBacked = true
                    )
                }
            }
        }

        return correlations
    }

    fun computeHabitProgress(
        habits: List<CustomHabitEntity>,
        completions: List<CustomHabitCompletionEntity>
    ): List<HabitProgressItem> {
        val last7Keys = DateKeyUtils.recentDaysKeys(7)
        val completedByHabit = completions
            .filter { it.completed }
            .groupBy { it.habitId }
            .mapValues { (_, list) -> list.map { it.dateKey }.toSet() }

        return habits.map { habit ->
            val dates = completedByHabit[habit.id] ?: emptySet()
            val last7Bool = last7Keys.map { it in dates }
            var streak = 0
            val last30Keys = DateKeyUtils.recentDaysKeys(30).reversed()
            for ((index, key) in last30Keys.withIndex()) {
                if (key in dates) {
                    streak++
                } else if (index == 0) {
                    continue
                } else {
                    break
                }
            }
            HabitProgressItem(
                habitId = habit.id,
                title = habit.title,
                pathway = habit.toPathway(),
                impactPoints = habit.impactPoints,
                reminderEnabled = habit.reminderEnabled,
                reminderHour = habit.reminderHour,
                reminderMinute = habit.reminderMinute,
                currentStreakDays = streak,
                last7DaysCompleted = last7Bool,
                totalCompletions = dates.size
            )
        }
    }

    val researchPapers: List<ResearchPaper> = listOf(
        ResearchPaper(
            id = "sleep_nonlinear",
            pathway = HormonePathway.TESTOSTERONE,
            title = "Non-Linear Sleep Loss & Testosterone Drop",
            citation = "Leproult & Van Cauter, JAMA (2011)",
            mechanism = "Restricting sleep below 6.5 hours rapidly truncates REM and slow-wave windows where nocturnal LH and testosterone pulses occur, while elevating next-day evening cortisol.",
            modelImplementation = "Non-linear quadratic penalty below 6.5h (e.g., 6.0h = -12 T, 5.0h = -40 T) paired with +13 to +35 Cortisol Load."
        ),
        ResearchPaper(
            id = "caffeine_cutoff",
            pathway = HormonePathway.CORTISOL,
            title = "8–10 Hour Caffeine Cutoff & Circadian Sleep Architecture",
            citation = "Drake et al. (J Clin Sleep Med 2013) & Lovallo et al. (2005)",
            mechanism = "Caffeine consumed within 8–10 hours of bedtime sustains adenosine receptor blockade, elevates diurnal cortisol, and reduces deep slow-wave sleep even when total hours appear adequate.",
            modelImplementation = "Tracks exact hours between Last Caffeine Time and Bedtime; <10h adds +9 to +14 Cortisol Load and -5 Testosterone."
        ),
        ResearchPaper(
            id = "morning_light_winddown",
            pathway = HormonePathway.SEROTONIN,
            title = "Morning Light <60m, Wake Regularity & Wind-Down",
            citation = "Pilz et al. (2011) & SCN Circadian Entrainment Biology",
            mechanism = "Viewing bright outdoor light within 60 minutes of waking anchors the suprachiasmatic nucleus (SCN), timing the cortisol awakening peak and evening melatonin release. Irregular wake times >45m and missed wind-downs blur this rhythm.",
            modelImplementation = "Outdoor light <60m rewards +9 T / +16 Serotonin / +7 Dopamine; irregular wake drift penalizes -8 T / +7 Cortisol; wind-down rewards -7 Cortisol."
        ),
        ResearchPaper(
            id = "resistance_overtraining",
            pathway = HormonePathway.TESTOSTERONE,
            title = "3–4x/Wk Resistance Multiplier vs. Overtraining Flag",
            citation = "Kraemer & Ratamess (2005) & Meeusen et al. (Overtraining Syndrome)",
            mechanism = "3–4 weekly resistance sessions maximize androgen receptor upregulation when sleep is sufficient. Pairing heavy training load with <6.5h or poor sleep triggers catabolic HPA overreaching.",
            modelImplementation = "3–4x/wk resistance adds +7 T / +5 Dopamine multiplier; heavy training + poor sleep triggers Overtraining Flag (-16 T, +15 Cortisol, -10 Dopamine)."
        ),
        ResearchPaper(
            id = "dopamine_friction_variable",
            pathway = HormonePathway.DOPAMINE,
            title = "Dopamine Friction Ratio & Variable Reward Prediction",
            citation = "Schultz (J Neurophysiol) & Volkow et al. (Nat Rev Neurosci)",
            mechanism = "Comparing effortful friction (deep focus, exercise, cold) against passive supernormal consumption prevents D2 receptor downregulation. Variable milestone rewards sustain intrinsic motivation far longer than predictable flat points.",
            modelImplementation = "Computes Effortful/Passive Friction Ratio (≥2.5x rewards +14 Dopamine) and awards variable 1.35x–2.0x surge coins & milestone chests."
        )
    )

    val simulationPresets: List<SimulationPreset> = listOf(
        SimulationPreset(
            name = "Peak Vitality Protocol",
            subtitle = "8.2h sleep, 10 AM caffeine, morning light <60m, lifting",
            input = DailyHabitInput(
                dateKey = "sim",
                bedtimeMinutes = 22 * 60 + 30,
                wakeTimeMinutes = 6 * 60 + 45,
                sleepHours = 8.25f,
                sleepQuality = 5,
                sleepConsistent = true,
                consistentWindDown = true,
                morningSunlightMins = 30,
                morningLightWithin60Mins = true,
                workoutType = WorkoutType.RESISTANCE,
                coldExposure = true,
                meditationOrBreathwork = true,
                nutritionClean = true,
                alcoholLevel = AlcoholLevel.NONE,
                hasCaffeine = true,
                lastCaffeineMinutes = 9 * 60 + 30,
                caffeineAfter2Pm = false,
                doomscrollMins = 0,
                deepWorkMins = 120,
                stressLevel = 2
            )
        ),
        SimulationPreset(
            name = "Overtraining + 5h Sleep",
            subtitle = "Heavy lifting on 5h sleep triggers Overtraining Flag",
            input = DailyHabitInput(
                dateKey = "sim",
                bedtimeMinutes = 1 * 60 + 30,
                wakeTimeMinutes = 6 * 60 + 30,
                sleepHours = 5.0f,
                sleepQuality = 2,
                sleepConsistent = false,
                consistentWindDown = false,
                morningSunlightMins = 0,
                morningLightWithin60Mins = false,
                workoutType = WorkoutType.RESISTANCE,
                coldExposure = false,
                meditationOrBreathwork = false,
                nutritionClean = false,
                alcoholLevel = AlcoholLevel.NONE,
                hasCaffeine = true,
                lastCaffeineMinutes = 17 * 60,
                caffeineAfter2Pm = true,
                doomscrollMins = 60,
                deepWorkMins = 30,
                stressLevel = 5
            )
        ),
        SimulationPreset(
            name = "Dopamine Passive Overload",
            subtitle = "90m scrolling, late caffeine, zero effortful focus",
            input = DailyHabitInput(
                dateKey = "sim",
                bedtimeMinutes = 23 * 60 + 45,
                wakeTimeMinutes = 6 * 60 + 30,
                sleepHours = 6.75f,
                sleepQuality = 3,
                sleepConsistent = true,
                consistentWindDown = false,
                morningSunlightMins = 10,
                morningLightWithin60Mins = false,
                workoutType = WorkoutType.NONE,
                coldExposure = false,
                meditationOrBreathwork = false,
                nutritionClean = false,
                alcoholLevel = AlcoholLevel.NONE,
                hasCaffeine = true,
                lastCaffeineMinutes = 16 * 60,
                caffeineAfter2Pm = true,
                doomscrollMins = 90,
                deepWorkMins = 0,
                stressLevel = 3
            )
        )
    )
}
