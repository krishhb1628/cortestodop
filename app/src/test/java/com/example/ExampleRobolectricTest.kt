package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EndoPulseDatabase
import com.example.data.local.EndoPulseRepository
import com.example.domain.model.AlcoholLevel
import com.example.domain.model.DailyHabitInput
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneResearchEngine
import com.example.domain.model.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var db: EndoPulseDatabase
    private lateinit var repo: EndoPulseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EndoPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EndoPulseRepository(db.endoPulseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches EndoPulse`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("EndoPulse", context.getString(R.string.app_name))
    }

    @Test
    fun `bedtime wakeup and caffeine cutoff calculations handle overnight wraparound`() {
        val hours = DateKeyUtils.calculateSleepDurationHours(23 * 60 + 15, 7 * 60)
        assertEquals(7.75f, hours, 0.01f)
        assertEquals("7h 45m", DateKeyUtils.formatDurationHours(hours))

        // 4:00 PM caffeine before 11:00 PM bed = 7.0h -> late caffeine penalty (<10h)
        val gap = DateKeyUtils.calculateHoursBeforeBedtime(16 * 60, 23 * 60)
        assertEquals(7.0f, gap, 0.01f)
    }

    @Test
    fun `non linear sleep penalty below 6_5h and late caffeine penalize testosterone and cortisol`() {
        val goodDay = DailyHabitInput(
            dateKey = "2026-11-13",
            sleepHours = 8.0f,
            sleepQuality = 5,
            hasCaffeine = true,
            lastCaffeineMinutes = 9 * 60, // 14h before 11 PM bed
            bedtimeMinutes = 23 * 60,
            wakeTimeMinutes = 7 * 60,
            morningLightWithin60Mins = true,
            consistentWindDown = true
        )
        val mildShort = goodDay.copy(sleepHours = 6.0f)
        val severeShort = goodDay.copy(
            sleepHours = 5.0f,
            lastCaffeineMinutes = 17 * 60 // 6h before bed -> penalty
        )

        val pGood = HormoneResearchEngine.evaluate(goodDay)
        val pMild = HormoneResearchEngine.evaluate(mildShort)
        val pSevere = HormoneResearchEngine.evaluate(severeShort)

        val dropMild = pGood.testosterone.score - pMild.testosterone.score
        val dropSevere = pMild.testosterone.score - pSevere.testosterone.score
        assertTrue("Expected steep non-linear drop below 6.5h", dropSevere > 20)
        assertTrue(pSevere.cortisol.score > pGood.cortisol.score + 30)
        assertTrue(dropMild > 20)
    }

    @Test
    fun `resistance 3_4x week multiplier rewards recovery while poor sleep triggers overtraining flag`() {
        val historyWithTwoLifts = listOf(
            DailyHabitInput(dateKey = "2026-11-11", sleepHours = 8.0f, workoutType = WorkoutType.RESISTANCE),
            DailyHabitInput(dateKey = "2026-11-12", sleepHours = 8.0f, workoutType = WorkoutType.RESISTANCE)
        )
        val recoveredThirdLift = DailyHabitInput(
            dateKey = "2026-11-13",
            sleepHours = 8.0f,
            sleepQuality = 5,
            workoutType = WorkoutType.RESISTANCE
        )
        val overtrainedLift = recoveredThirdLift.copy(
            sleepHours = 5.0f,
            sleepQuality = 2
        )

        val pRecovered = HormoneResearchEngine.evaluate(recoveredThirdLift, recentHistory = historyWithTwoLifts)
        val pOvertrained = HormoneResearchEngine.evaluate(overtrainedLift, recentHistory = historyWithTwoLifts)

        assertFalse(pRecovered.overtrainingFlag)
        assertEquals(3, pRecovered.weeklyResistanceCount)
        assertTrue(pRecovered.testosterone.contributions.any { it.label.contains("3–4x/Wk Resistance Multiplier") })

        assertTrue(pOvertrained.overtrainingFlag)
        assertTrue(pOvertrained.testosterone.contributions.any { it.label.contains("Overtraining Flag") })
        assertTrue(pRecovered.testosterone.score > pOvertrained.testosterone.score + 40)
    }

    @Test
    fun `dopamine friction ratio compares effortful habits against passive consumption`() {
        val highFriction = DailyHabitInput(
            dateKey = "2026-11-13",
            sleepHours = 7.5f,
            deepWorkMins = 90,
            workoutType = WorkoutType.RESISTANCE,
            coldExposure = true,
            doomscrollMins = 0
        )
        val passiveOverload = DailyHabitInput(
            dateKey = "2026-11-13",
            sleepHours = 7.5f,
            deepWorkMins = 0,
            workoutType = WorkoutType.NONE,
            coldExposure = false,
            doomscrollMins = 90,
            nutritionClean = false,
            alcoholLevel = AlcoholLevel.MODERATE
        )

        val pHigh = HormoneResearchEngine.evaluate(highFriction)
        val pPassive = HormoneResearchEngine.evaluate(passiveOverload)

        assertTrue(pHigh.frictionMetrics.ratio >= 2.5f)
        assertTrue(pPassive.frictionMetrics.ratio < 1.0f)
        assertTrue(pHigh.dopamine.score - pPassive.dopamine.score >= 30)
    }

    @Test
    fun `room repository awards coins, levels up, and unlocks milestone chests`() = runTest {
        repo.addCustomHabit(
            title = "Zone-2 Walk",
            pathway = HormonePathway.CORTISOL,
            impactPoints = 6,
            reminderEnabled = false
        )

        val saveResult = repo.saveDailyCheckIn(
            DailyHabitInput(
                dateKey = "2026-11-13",
                bedtimeMinutes = 22 * 60 + 30,
                wakeTimeMinutes = 6 * 60 + 30,
                sleepQuality = 5,
                morningLightWithin60Mins = true,
                morningSunlightMins = 30,
                consistentWindDown = true,
                workoutType = WorkoutType.RESISTANCE,
                deepWorkMins = 90,
                doomscrollMins = 0,
                hasCaffeine = true,
                lastCaffeineMinutes = 9 * 60
            )
        )

        assertTrue(saveResult.dailyReward.totalCoins > 0)
        assertTrue(saveResult.newlyUnlockedMilestones.any { it.milestoneId == "milestone_first_checkin" })

        val logs = repo.allLogs.first()
        val milestones = repo.milestones.first()
        val totalCoins = logs.sumOf { it.coinsEarned } + milestones.sumOf { it.coinsAwarded }
        assertTrue(totalCoins >= 80)
        assertTrue(HormoneResearchEngine.computeBioLevel(250).level == 2)
    }
}
