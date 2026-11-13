package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.CustomHabitEntity
import java.util.Calendar

object HabitReminderScheduler {

    fun scheduleOrCancel(context: Context, habit: CustomHabitEntity) {
        if (habit.reminderEnabled) {
            scheduleDailyReminder(context, habit)
        } else {
            cancelReminder(context, habit.id)
        }
    }

    fun scheduleDailyReminder(context: Context, habit: CustomHabitEntity) {
        HabitReminderReceiver.createChannelIfNeeded(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, habit)

        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habit.reminderHour.coerceIn(0, 23))
            set(Calendar.MINUTE, habit.reminderMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerCal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, habitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(context: Context, habit: CustomHabitEntity): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_TITLE, habit.title)
            putExtra(HabitReminderReceiver.EXTRA_PATHWAY_LABEL, habit.toPathway().displayName)
        }
        return PendingIntent.getBroadcast(
            context,
            habit.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
