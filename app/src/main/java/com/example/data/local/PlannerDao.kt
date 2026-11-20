package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DeliveryLogEntity
import com.example.data.model.HabitCompletionEntity
import com.example.data.model.PlannerItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    @Query("SELECT * FROM planner_items ORDER BY CASE WHEN startMinutesOfDay IS NULL THEN 1 ELSE 0 END, startMinutesOfDay ASC, createdAtMillis ASC")
    fun observeAllItems(): Flow<List<PlannerItemEntity>>

    @Query("SELECT * FROM planner_items WHERE isEnabled = 1")
    suspend fun getAllEnabledItemsOnce(): List<PlannerItemEntity>

    @Query("SELECT * FROM planner_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): PlannerItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: PlannerItemEntity): Long

    @Update
    suspend fun updateItem(item: PlannerItemEntity)

    @Query("DELETE FROM planner_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE planner_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setItemCompleted(id: Long, completed: Boolean)

    @Query("UPDATE planner_items SET isEnabled = :enabled WHERE id = :id")
    suspend fun setItemEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM habit_completions")
    fun observeAllHabitCompletions(): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE itemId = :itemId AND epochDay = :epochDay")
    suspend fun deleteHabitCompletion(itemId: Long, epochDay: Long)

    @Query("SELECT * FROM daily_notes WHERE epochDay = :epochDay LIMIT 1")
    fun observeNoteForDay(epochDay: Long): Flow<DailyNoteEntity?>

    @Query("SELECT * FROM daily_notes ORDER BY epochDay DESC")
    fun observeAllNotes(): Flow<List<DailyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: DailyNoteEntity)

    @Query("SELECT * FROM delivery_logs ORDER BY timestampMillis DESC LIMIT 60")
    fun observeDeliveryLogs(): Flow<List<DeliveryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryLog(log: DeliveryLogEntity)

    @Query("DELETE FROM delivery_logs")
    suspend fun clearDeliveryLogs()
}
