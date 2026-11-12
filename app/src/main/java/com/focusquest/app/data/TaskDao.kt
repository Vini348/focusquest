package com.focusquest.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :id AND isCompleted = 0")
    suspend fun complete(id: Long, completedAt: Long): Int

    @Query("SELECT (SELECT COALESCE(SUM(xp), 0) FROM tasks WHERE isCompleted = 1) + (SELECT COALESCE(SUM(xp), 0) FROM focus_sessions)")
    fun observeTotalXp(): Flow<Int>

    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Query("DELETE FROM subtasks WHERE taskId = :id")
    suspend fun deleteSubtasksForTask(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>
    @Insert suspend fun insertGoal(goal: GoalEntity)
    @Query("UPDATE goals SET isCompleted = :completed WHERE id = :id")
    suspend fun setGoalCompleted(id: Long, completed: Boolean)
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeHabits(): Flow<List<HabitEntity>>
    @Insert suspend fun insertHabit(habit: HabitEntity)
    @Query("UPDATE habits SET lastCompletedDate = :date, currentStreak = :streak, totalCompletions = totalCompletions + 1 WHERE id = :id")
    suspend fun completeHabit(id: Long, date: String, streak: Int)
    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveHabit(id: Long)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>
    @Insert suspend fun insertNote(note: NoteEntity)
    @Query("UPDATE notes SET title = :title, content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(id: Long, title: String, content: String, updatedAt: Long)
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>>
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getSubtasks(taskId: Long): List<SubtaskEntity>
    @Insert suspend fun insertSubtask(subtask: SubtaskEntity)
    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setSubtaskCompleted(id: Long, completed: Boolean)
    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: Long)
}
