package com.example.data.local

import androidx.room.Entity

@Entity(
    tableName = "custom_habit_completions",
    primaryKeys = ["dateKey", "habitId"]
)
data class CustomHabitCompletionEntity(
    val dateKey: String,
    val habitId: Int,
    val completed: Boolean = true
)
