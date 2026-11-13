package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EndoPulseDao {
    @Query("SELECT * FROM daily_logs ORDER BY dateKey DESC")
    fun observeAllLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogByDate(dateKey: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs ORDER BY dateKey DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE dateKey = :dateKey")
    suspend fun deleteLogByDate(dateKey: String)

    @Query("DELETE FROM daily_logs")
    suspend fun clearAllLogs()

    @Query("SELECT * FROM custom_habits ORDER BY createdAt ASC")
    fun observeCustomHabits(): Flow<List<CustomHabitEntity>>

    @Query("SELECT * FROM custom_habits ORDER BY createdAt ASC")
    suspend fun getAllCustomHabits(): List<CustomHabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomHabit(habit: CustomHabitEntity): Long

    @Update
    suspend fun updateCustomHabit(habit: CustomHabitEntity)

    @Query("DELETE FROM custom_habits WHERE id = :habitId")
    suspend fun deleteCustomHabit(habitId: Int)

    @Query("DELETE FROM custom_habits")
    suspend fun clearCustomHabits()

    @Query("SELECT * FROM custom_habit_completions WHERE dateKey = :dateKey")
    fun observeCompletionsForDate(dateKey: String): Flow<List<CustomHabitCompletionEntity>>

    @Query("SELECT * FROM custom_habit_completions")
    fun observeAllCompletions(): Flow<List<CustomHabitCompletionEntity>>

    @Query("SELECT * FROM custom_habit_completions WHERE dateKey = :dateKey")
    suspend fun getCompletionsForDate(dateKey: String): List<CustomHabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: CustomHabitCompletionEntity)

    @Query("DELETE FROM custom_habit_completions WHERE dateKey = :dateKey AND habitId = :habitId")
    suspend fun deleteCompletion(dateKey: String, habitId: Int)

    @Query("DELETE FROM custom_habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: Int)

    @Query("DELETE FROM custom_habit_completions")
    suspend fun clearAllCompletions()

    @Query("SELECT * FROM milestone_rewards ORDER BY unlockedAt DESC")
    fun observeMilestones(): Flow<List<MilestoneRewardEntity>>

    @Query("SELECT * FROM milestone_rewards")
    suspend fun getAllMilestones(): List<MilestoneRewardEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMilestoneIfNew(milestone: MilestoneRewardEntity)

    @Query("DELETE FROM milestone_rewards")
    suspend fun clearMilestones()
}
