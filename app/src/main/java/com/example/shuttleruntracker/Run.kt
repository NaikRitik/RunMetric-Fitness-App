package com.example.shuttleruntracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_history_table")
data class Run(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val date: String,
    val duration: String,
    val shuttles: Int,
    val distanceInMeters: Double = 0.0 // <-- ADD THIS NEW LINE
)