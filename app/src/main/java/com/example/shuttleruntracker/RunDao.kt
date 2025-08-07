package com.example.shuttleruntracker // Or com.example.fitnessapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert
    suspend fun insertRun(run: Run)

    @Query("SELECT * FROM run_history_table ORDER BY id DESC")
    fun getAllRuns(): Flow<List<Run>>

    @Delete
    suspend fun deleteRun(run: Run)
}