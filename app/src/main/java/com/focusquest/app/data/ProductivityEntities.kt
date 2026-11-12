package com.focusquest.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val horizon: String = "Médio prazo",
    val targetDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val frequency: String = "Diário",
    val createdAt: Long = System.currentTimeMillis(),
    val lastCompletedDate: String? = null,
    val currentStreak: Int = 0,
    val totalCompletions: Int = 0,
    val isArchived: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subtasks", indices = [Index(value = ["taskId"])])
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
