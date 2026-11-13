package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.CustomHabitBonus
import com.example.domain.model.HormonePathway

@Entity(tableName = "custom_habits")
data class CustomHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val pathway: String,
    val impactPoints: Int,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPathway(): HormonePathway =
        runCatching { HormonePathway.valueOf(pathway) }.getOrDefault(HormonePathway.TESTOSTERONE)

    fun toBonus(): CustomHabitBonus = CustomHabitBonus(
        title = title,
        pathway = toPathway(),
        impactPoints = impactPoints
    )
}
