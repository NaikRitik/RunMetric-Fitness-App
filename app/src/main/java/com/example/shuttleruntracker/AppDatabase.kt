package com.example.shuttleruntracker // Or your package name

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// BUMP THE VERSION FROM 1 TO 2
@Database(entities = [Run::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shuttle_run_database"
                )
                    // ADD THIS LINE TO HANDLE THE VERSION CHANGE
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}