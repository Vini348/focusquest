package com.focusquest.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :id AND isCompleted = 0")
    suspend fun complete(id: Long, completedAt: Long): Int

    @Query("SELECT (SELECT COALESCE(SUM(xp), 0) FROM tasks WHERE isCompleted = 1) + (SELECT COALESCE(SUM(xp), 0) FROM focus_sessions)")
    fun observeTotalXp(): Flow<Int>

    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
