package com.focusquest.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationMinutes: Int,
    val xp: Int,
    val completedAt: Long = System.currentTimeMillis()
)
