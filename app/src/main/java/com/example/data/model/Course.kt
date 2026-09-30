package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val instructor: String = "",
    val room: String = "",
    val colorHex: Long = 0xFF4F46E5, // Default Indigo
    val targetPercentage: Double = 75.0,
    val daysOfWeek: String = "MON,WED,FRI", // Comma-separated: MON, TUE, WED, THU, FRI, SAT, SUN
    val startTime: String = "09:00 AM",
    val endTime: String = "10:00 AM",
    val initialAttended: Int = 0,
    val initialMissed: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
