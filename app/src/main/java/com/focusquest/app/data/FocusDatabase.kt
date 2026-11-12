package com.focusquest.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, FocusSessionEntity::class, GoalEntity::class, HabitEntity::class, NoteEntity::class, SubtaskEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS focus_sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "durationMinutes INTEGER NOT NULL, " +
                        "xp INTEGER NOT NULL, " +
                        "completedAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'Pessoal'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueDate TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN goalId INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, horizon TEXT NOT NULL, targetDate TEXT, createdAt INTEGER NOT NULL, isCompleted INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, frequency TEXT NOT NULL, createdAt INTEGER NOT NULL, lastCompletedDate TEXT, currentStreak INTEGER NOT NULL, totalCompletions INTEGER NOT NULL, isArchived INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS subtasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_taskId ON subtasks(taskId)")
            }
        }

        @Volatile private var INSTANCE: FocusDatabase? = null

        fun get(context: Context): FocusDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focusquest.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
