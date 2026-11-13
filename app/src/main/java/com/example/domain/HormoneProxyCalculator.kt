package com.example.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure, dependency-free Kotlin domain engine (no Android imports).
 *
 * Think of Kotlin `data class` like a Python `@dataclass` or a C `struct`
 * with built-in equality, copying, and readable `toString()`.
 */
data class DailyInput(
    val dateKey: String,
    val sleepHours: Float,
    val wakeTimeMinutes: Int, // Minutes since midnight (e.g., 7:30 AM = 7 * 60 + 30 = 450)
    val stressRating: Int, // 1 (calm) .. 5 (overwhelmed)
    val resistanceTraining: Boolean = false,
    val deepWorkBlock: Boolean = false,
    val morningSunlight: Boolean = false,
    val noLateCaffeine: Boolean = true, // true = avoided caffeine within 8-10h of bed
    val lowDoomscroll: Boolean = true, // true = kept passive social feeds low
    val coldExposure: Boolean = false
)

data class ProxyScoreReport(
    val recoveryIndex: Int, // Testosterone Behavioral Proxy (0..100)
    val stressLoadIndex: Int, // Cortisol Behavioral Proxy (0..100, lower is calmer)
    val driveIndex: Int, // Dopamine Behavioral Proxy (0..100)
    val dopamineFrictionRatio: Float, // Effortful / Passive ratio
    val avgSleepHours: Float,
    val avgWakeDriftMinutes: Int,
    val weeklyResistanceSessions: Int,
    val overtrainingFlag: Boolean,
    val primaryInsight: String
)

/**
 * Kotlin `object` is a thread-safe singleton (like a Python module of pure functions).
 */
object HormoneProxyCalculator {

    // Recency weights for up to 7 days (oldest -> newest) so today matters more than 6 days ago.
    private val recencyWeights = listOf(0.65f, 0.75f, 0.85f, 1.00f, 1.15f, 1.30f, 1.50f)

    fun calculateRollingProfile(history: List<DailyInput>): ProxyScoreReport {
        require(history.isNotEmpty()) { "At least 1 DailyInput is required to compute rolling indices." }

        val window = history.takeLast(7)
        val weights = recencyWeights.takeLast(window.size)
        val weightSum = weights.sum()

        // 1. Weighted 7-day averages
        val avgSleep = weightedAverage(window.map { it.sleepHours }, weights, weightSum)
        val avgStress = weightedAverage(window.map { it.stressRating.toFloat() }, weights, weightSum)
        val sunlightRate = weightedAverage(window.map { if (it.morningSunlight) 1f else 0f }, weights, weightSum)
        val lateCaffeineRate = weightedAverage(window.map { if (!it.noLateCaffeine) 1f else 0f }, weights, weightSum)

        // 2. Wake-time regularity (circular distance around midnight)
        val avgWakeMinutes = circularMeanMinutes(window.map { it.wakeTimeMinutes })
        val avgWakeDrift = window.map { circularDiffMinutes(it.wakeTimeMinutes, avgWakeMinutes) }
            .average()
            .roundToInt()

        // 3. Weekly resistance count & overtraining detection
        val weeklyResistanceCount = window.count { it.resistanceTraining }
        val latest = window.last()
        val highTrainingLoad = weeklyResistanceCount >= 4 || (latest.resistanceTraining && weeklyResistanceCount >= 3)
        val poorRecoveryState = avgSleep < 6.5f || latest.sleepHours < 6.0f || avgStress >= 3.8f
        val overtrainingFlag = highTrainingLoad && poorRecoveryState

        // 4. Stress Load Index (Cortisol Proxy: 0..100, lower is calmer)
        val stressLoad = computeStressLoad(
            avgSleep = avgSleep,
            avgStress = avgStress,
            avgWakeDrift = avgWakeDrift,
            sunlightRate = sunlightRate,
            lateCaffeineRate = lateCaffeineRate,
            overtrainingFlag = overtrainingFlag
        )

        // 5. Recovery Index (Testosterone Proxy: 0..100)
        val recovery = computeRecoveryIndex(
            avgSleep = avgSleep,
            latestSleep = latest.sleepHours,
            weeklyResistanceCount = weeklyResistanceCount,
            stressLoad = stressLoad,
            overtrainingFlag = overtrainingFlag,
            sunlightRate = sunlightRate
        )

        // 6. Drive & Baseline Index (Dopamine Proxy) + Dopamine Friction Ratio
        val (drive, frictionRatio) = computeDriveAndFriction(
            window = window,
            weights = weights,
            weightSum = weightSum,
            avgSleep = avgSleep,
            stressLoad = stressLoad
        )

        val insight = buildPlainEnglishInsight(
            avgSleep = avgSleep,
            latestSleep = latest.sleepHours,
            avgWakeDrift = avgWakeDrift,
            lateCaffeineRate = lateCaffeineRate,
            overtrainingFlag = overtrainingFlag,
            frictionRatio = frictionRatio,
            weeklyResistanceCount = weeklyResistanceCount,
            stressLoad = stressLoad
        )

        return ProxyScoreReport(
            recoveryIndex = recovery,
            stressLoadIndex = stressLoad,
            driveIndex = drive,
            dopamineFrictionRatio = frictionRatio,
            avgSleepHours = ((avgSleep * 10f).roundToInt() / 10f),
            avgWakeDriftMinutes = avgWakeDrift,
            weeklyResistanceSessions = weeklyResistanceCount,
            overtrainingFlag = overtrainingFlag,
            primaryInsight = insight
        )
    }

    private fun computeStressLoad(
        avgSleep: Float,
        avgStress: Float,
        avgWakeDrift: Int,
        sunlightRate: Float,
        lateCaffeineRate: Float,
        overtrainingFlag: Boolean
    ): Int {
        var score = 34f

        // Self-reported stress (1..5 -> -10 to +30)
        score += (avgStress - 2.0f) * 10f

        // Non-linear sleep deprivation HPA surge below 6.5h
        if (avgSleep < 6.5f) {
            val deficit = 6.5f - avgSleep
            score += 8f + (deficit.pow(2) * 11f)
        } else if (avgSleep >= 7.5f) {
            score -= 6f
        }

        // Wake-time regularity vs irregular schedule penalty
        score += when {
            avgWakeDrift <= 25 -> -6f
            avgWakeDrift <= 45 -> 0f
            else -> ((avgWakeDrift - 45) * 0.28f).coerceAtMost(16f)
        }

        // Morning sunlight within 60m buffers cortisol curve
        score -= sunlightRate * 9f

        // Late caffeine within 8-10h of bed prolongs evening cortisol
        score += lateCaffeineRate * 14f

        if (overtrainingFlag) {
            score += 12f
        }

        return score.roundToInt().coerceIn(5, 98)
    }

    private fun computeRecoveryIndex(
        avgSleep: Float,
        latestSleep: Float,
        weeklyResistanceCount: Int,
        stressLoad: Int,
        overtrainingFlag: Boolean,
        sunlightRate: Float
    ): Int {
        var score = 42f

        // Blended sleep (65% 7-day rolling avg, 35% last night)
        val effectiveSleep = (avgSleep * 0.65f) + (latestSleep * 0.35f)
        val sleepContribution = when {
            effectiveSleep >= 8.0f -> 30f
            effectiveSleep >= 7.5f -> 25f
            effectiveSleep >= 6.5f -> 6f + ((effectiveSleep - 6.5f) * 19f)
            else -> {
                // Steep non-linear penalty below 6.5h
                val deficit = 6.5f - effectiveSleep
                -(6f + (deficit.pow(2) * 14f))
            }
        }
        score += sleepContribution

        // Optimal 3-4x/week resistance training boost
        val trainingContribution = when (weeklyResistanceCount) {
            0 -> 0f
            1, 2 -> 8f
            3, 4 -> 18f
            else -> 11f // 5+ sessions without extra recovery yields diminishing returns
        }
        score += trainingContribution

        // Morning sunlight & vitamin D circadian support
        score += sunlightRate * 6f

        // HPA-HPG inverse coupling: high Stress Load drags down Recovery
        if (stressLoad > 58) {
            score -= (stressLoad - 58) * 0.38f
        } else if (stressLoad <= 40) {
            score += 4f
        }

        // Burnout / Overtraining modifier
        if (overtrainingFlag) {
            score -= 16f
        }

        return score.roundToInt().coerceIn(5, 99)
    }

    private fun computeDriveAndFriction(
        window: List<DailyInput>,
        weights: List<Float>,
        weightSum: Float,
        avgSleep: Float,
        stressLoad: Int
    ): Pair<Int, Float> {
        val dailyEffort = window.map { day ->
            (if (day.deepWorkBlock) 35f else 0f) +
                (if (day.resistanceTraining) 30f else 0f) +
                (if (day.morningSunlight) 15f else 0f) +
                (if (day.coldExposure) 20f else 0f)
        }
        val dailyPassive = window.map { day ->
            (if (!day.lowDoomscroll) 45f else 10f) +
                (if (!day.noLateCaffeine) 10f else 0f)
        }

        val avgEffort = weightedAverage(dailyEffort, weights, weightSum)
        val avgPassive = weightedAverage(dailyPassive, weights, weightSum).coerceAtLeast(10f)
        val rawRatio = avgEffort / avgPassive
        val roundedRatio = (rawRatio * 10f).roundToInt() / 10f

        var drive = 42f

        // Sleep restores striatal D2 receptor sensitivity
        drive += when {
            avgSleep >= 7.5f -> 16f
            avgSleep >= 6.8f -> 8f
            else -> -12f
        }

        // Dopamine Friction Ratio impact
        drive += when {
            rawRatio >= 2.5f -> 30f
            rawRatio >= 1.2f -> 8f + ((rawRatio - 1.2f) / 1.3f) * 20f
            else -> -18f
        }

        if (stressLoad > 70) {
            drive -= 8f
        }

        return drive.roundToInt().coerceIn(5, 99) to roundedRatio
    }

    private fun buildPlainEnglishInsight(
        avgSleep: Float,
        latestSleep: Float,
        avgWakeDrift: Int,
        lateCaffeineRate: Float,
        overtrainingFlag: Boolean,
        frictionRatio: Float,
        weeklyResistanceCount: Int,
        stressLoad: Int
    ): String {
        return when {
            overtrainingFlag ->
                "High training load ($weeklyResistanceCount sessions) paired with ${String.format(java.util.Locale.US, "%.1fh", avgSleep)} avg sleep is elevating stress load—take a gentle recovery day."
            latestSleep < 6.5f || avgSleep < 6.5f ->
                "Sleep below 6.5 hours is the largest drag on your Recovery and Drive right now; an extra hour tonight gives the biggest rebound."
            lateCaffeineRate >= 0.4f && stressLoad >= 55 ->
                "Late caffeine within 8–10h of bed is keeping your Stress Load elevated; shifting your last cup earlier will calm your evening curve."
            avgWakeDrift > 45 ->
                "Wake-time shifts averaging ±${avgWakeDrift}m are adding circadian friction—anchoring tomorrow's wake time will stabilize Stress Load."
            frictionRatio < 1.2f ->
                "Passive stimulation is outweighing effortful blocks (${frictionRatio}x ratio); one 45m Deep Work block or walk will lift Drive."
            else ->
                "Strong 7-day rhythm: ${String.format(java.util.Locale.US, "%.1fh", avgSleep)} avg sleep and a ${frictionRatio}x Dopamine Friction Ratio are supporting steady recovery."
        }
    }

    private fun weightedAverage(values: List<Float>, weights: List<Float>, weightSum: Float): Float {
        var total = 0f
        for (i in values.indices) {
            total += values[i] * weights[i]
        }
        return total / weightSum
    }

    private fun circularDiffMinutes(a: Int, b: Int): Int {
        val normA = ((a % 1440) + 1440) % 1440
        val normB = ((b % 1440) + 1440) % 1440
        val diff = abs(normA - normB)
        return minOf(diff, 1440 - diff)
    }

    private fun circularMeanMinutes(times: List<Int>): Int {
        // Shift early-morning/late-night values smoothly around average
        var sumSin = 0.0
        var sumCos = 0.0
        times.forEach { t ->
            val angle = (t.toDouble() / 1440.0) * 2.0 * Math.PI
            sumSin += kotlin.math.sin(angle)
            sumCos += kotlin.math.cos(angle)
        }
        val meanAngle = kotlin.math.atan2(sumSin / times.size, sumCos / times.size)
        val normalized = if (meanAngle < 0) meanAngle + 2.0 * Math.PI else meanAngle
        return ((normalized / (2.0 * Math.PI)) * 1440.0).roundToInt() % 1440
    }
}
