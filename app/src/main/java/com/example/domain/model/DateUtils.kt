package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class DateStripItem(
    val dateKey: String,
    val dayShort: String,
    val dayNumber: String,
    val isToday: Boolean
)

object DateKeyUtils {
    private val keyFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val dayShortFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE", Locale.US)

    private val dayNumFormat: SimpleDateFormat
        get() = SimpleDateFormat("d", Locale.US)

    private val friendlyFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE, MMM d", Locale.US)

    fun todayKey(): String = keyFormat.format(Calendar.getInstance().time)

    fun recentDaysKeys(count: Int): List<String> {
        val result = mutableListOf<String>()
        for (offset in -(count - 1)..0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            result += keyFormat.format(cal.time)
        }
        return result
    }

    fun recentSevenDays(): List<DateStripItem> {
        val today = todayKey()
        val result = mutableListOf<DateStripItem>()
        for (offset in -6..0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val date = cal.time
            val key = keyFormat.format(date)
            result += DateStripItem(
                dateKey = key,
                dayShort = dayShortFormat.format(date).uppercase(Locale.US),
                dayNumber = dayNumFormat.format(date),
                isToday = key == today
            )
        }
        return result
    }

    fun formatFriendly(dateKey: String): String {
        if (dateKey == todayKey()) return "Today"
        return try {
            val parsed = keyFormat.parse(dateKey)
            if (parsed != null) friendlyFormat.format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }
    }

    fun shortLabel(dateKey: String): String {
        return try {
            val parsed = keyFormat.parse(dateKey)
            if (parsed != null) {
                SimpleDateFormat("M/d", Locale.US).format(parsed)
            } else {
                dateKey.takeLast(5)
            }
        } catch (_: Exception) {
            dateKey.takeLast(5)
        }
    }

    fun calculateSleepDurationHours(bedtimeMinutes: Int, wakeTimeMinutes: Int): Float {
        val normalizedBed = ((bedtimeMinutes % 1440) + 1440) % 1440
        val normalizedWake = ((wakeTimeMinutes % 1440) + 1440) % 1440
        val diffMinutes = ((normalizedWake - normalizedBed) + 1440) % 1440
        val effectiveMinutes = if (diffMinutes == 0) 8 * 60 else diffMinutes
        return ((effectiveMinutes / 60f) * 100f).roundToInt() / 100f
    }

    /**
     * Hours between last caffeine intake and bedtime (handling evening/midnight wraparound).
     */
    fun calculateHoursBeforeBedtime(caffeineMinutes: Int, bedtimeMinutes: Int): Float {
        val normCaf = ((caffeineMinutes % 1440) + 1440) % 1440
        val normBed = ((bedtimeMinutes % 1440) + 1440) % 1440
        val diff = ((normBed - normCaf) + 1440) % 1440
        return ((diff / 60f) * 10f).roundToInt() / 10f
    }

    /**
     * Shortest circular distance in minutes between two times of day (0..720).
     */
    fun circularTimeDiffMinutes(timeA: Int, timeB: Int): Int {
        val a = ((timeA % 1440) + 1440) % 1440
        val b = ((timeB % 1440) + 1440) % 1440
        val raw = abs(a - b)
        return minOf(raw, 1440 - raw)
    }

    fun formatMinutesOfDay(minutesFromMidnight: Int): String {
        val norm = ((minutesFromMidnight % 1440) + 1440) % 1440
        val hour24 = norm / 60
        val minute = norm % 60
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun formatDurationHours(hours: Float): String {
        val totalMinutes = (hours * 60f).roundToInt().coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "${h}h ${m}m"
    }
}
