package com.example

import com.example.domain.DailyInput
import com.example.domain.HormoneProxyCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HormoneProxyCalculatorTest {

    @Test
    fun `scenario 1 - balanced university week yields high recovery and drive`() {
        val balancedWeek = listOf(
            DailyInput("2026-11-07", sleepHours = 7.8f, wakeTimeMinutes = 450, stressRating = 2, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-08", sleepHours = 8.0f, wakeTimeMinutes = 460, stressRating = 2, resistanceTraining = false, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-09", sleepHours = 7.6f, wakeTimeMinutes = 445, stressRating = 2, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-10", sleepHours = 8.1f, wakeTimeMinutes = 455, stressRating = 2, resistanceTraining = false, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-11", sleepHours = 7.7f, wakeTimeMinutes = 450, stressRating = 2, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-12", sleepHours = 8.0f, wakeTimeMinutes = 450, stressRating = 2, resistanceTraining = false, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-13", sleepHours = 7.9f, wakeTimeMinutes = 455, stressRating = 2, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true)
        )

        val report = HormoneProxyCalculator.calculateRollingProfile(balancedWeek)
        assertTrue(report.recoveryIndex >= 80)
        assertTrue(report.stressLoadIndex <= 35)
        assertTrue(report.driveIndex >= 80)
        assertTrue(report.dopamineFrictionRatio >= 2.5f)
        assertFalse(report.overtrainingFlag)
    }

    @Test
    fun `scenario 2 - exam cramming with sub-6_5h sleep and late caffeine elevates stress and drops recovery`() {
        val cramWeek = listOf(
            DailyInput("2026-11-09", sleepHours = 5.5f, wakeTimeMinutes = 540, stressRating = 4, resistanceTraining = false, deepWorkBlock = true, morningSunlight = false, noLateCaffeine = false, lowDoomscroll = false),
            DailyInput("2026-11-10", sleepHours = 5.2f, wakeTimeMinutes = 420, stressRating = 5, resistanceTraining = false, deepWorkBlock = false, morningSunlight = false, noLateCaffeine = false, lowDoomscroll = false),
            DailyInput("2026-11-11", sleepHours = 5.8f, wakeTimeMinutes = 600, stressRating = 4, resistanceTraining = false, deepWorkBlock = true, morningSunlight = false, noLateCaffeine = false, lowDoomscroll = false),
            DailyInput("2026-11-12", sleepHours = 5.0f, wakeTimeMinutes = 450, stressRating = 5, resistanceTraining = false, deepWorkBlock = false, morningSunlight = false, noLateCaffeine = false, lowDoomscroll = false),
            DailyInput("2026-11-13", sleepHours = 5.3f, wakeTimeMinutes = 510, stressRating = 5, resistanceTraining = false, deepWorkBlock = false, morningSunlight = false, noLateCaffeine = false, lowDoomscroll = false)
        )

        val report = HormoneProxyCalculator.calculateRollingProfile(cramWeek)
        assertTrue(report.recoveryIndex < 35)
        assertTrue(report.stressLoadIndex >= 75)
        assertTrue(report.driveIndex < 30)
        assertTrue(report.dopamineFrictionRatio < 1.0f)
        assertTrue(report.avgWakeDriftMinutes > 45)
    }

    @Test
    fun `scenario 3 - heavy gym frequency combined with poor sleep triggers overtraining flag`() {
        val overtrainedWeek = listOf(
            DailyInput("2026-11-09", sleepHours = 5.8f, wakeTimeMinutes = 450, stressRating = 4, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-10", sleepHours = 5.9f, wakeTimeMinutes = 450, stressRating = 4, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-11", sleepHours = 5.6f, wakeTimeMinutes = 450, stressRating = 4, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true),
            DailyInput("2026-11-12", sleepHours = 6.0f, wakeTimeMinutes = 450, stressRating = 4, resistanceTraining = true, deepWorkBlock = true, morningSunlight = true, noLateCaffeine = true, lowDoomscroll = true)
        )

        val report = HormoneProxyCalculator.calculateRollingProfile(overtrainedWeek)
        assertTrue(report.overtrainingFlag)
        assertTrue(report.recoveryIndex < 45)
    }
}
