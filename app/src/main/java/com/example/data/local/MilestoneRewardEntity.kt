package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestone_rewards")
data class MilestoneRewardEntity(
    @PrimaryKey val milestoneId: String,
    val title: String,
    val description: String,
    val coinsAwarded: Int,
    val unlockedAt: Long = System.currentTimeMillis()
)
