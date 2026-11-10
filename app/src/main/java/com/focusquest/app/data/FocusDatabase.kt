package com.focusquest.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, FocusSessionEntity::class],
    version = 2,
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

        @Volatile private var INSTANCE: FocusDatabase? = null

        fun get(context: Context): FocusDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focusquest.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
