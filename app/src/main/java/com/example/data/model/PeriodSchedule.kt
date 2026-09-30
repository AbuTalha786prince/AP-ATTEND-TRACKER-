package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "period_schedules",
    indices = [
        Index(value = ["dayOfWeek", "periodNumber"], unique = true),
        Index(value = ["courseId"])
    ]
)
data class PeriodSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: String, // "MON", "TUE", "WED", "THU", "FRI", "SAT"
    val periodNumber: Int, // 1 to 8
    val courseId: Long,
    val startTime: String = "", // e.g. "08:30 AM"
    val endTime: String = "",   // e.g. "09:15 AM"
    val room: String = ""
)

data class PeriodSlotConfig(
    val periodNumber: Int,
    val defaultStartTime: String,
    val defaultEndTime: String
)

object DefaultPeriodTimings {
    val SLOTS = listOf(
        PeriodSlotConfig(1, "08:30 AM", "09:15 AM"),
        PeriodSlotConfig(2, "09:20 AM", "10:05 AM"),
        PeriodSlotConfig(3, "10:15 AM", "11:00 AM"),
        PeriodSlotConfig(4, "11:05 AM", "11:50 AM"),
        PeriodSlotConfig(5, "12:35 PM", "01:20 PM"),
        PeriodSlotConfig(6, "01:25 PM", "02:10 PM"),
        PeriodSlotConfig(7, "02:15 PM", "03:00 PM"),
        PeriodSlotConfig(8, "03:05 PM", "03:50 PM")
    )

    fun getSlotTiming(periodNumber: Int): Pair<String, String> {
        val slot = SLOTS.firstOrNull { it.periodNumber == periodNumber }
        return if (slot != null) {
            slot.defaultStartTime to slot.defaultEndTime
        } else {
            "Period $periodNumber" to ""
        }
    }
}
