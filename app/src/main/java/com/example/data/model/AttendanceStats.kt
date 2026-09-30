package com.example.data.model

import kotlin.math.ceil
import kotlin.math.floor

enum class HealthStatus {
    ON_TRACK,
    WARNING,
    CRITICAL
}

data class CourseStats(
    val course: Course,
    val attendedCount: Int,   // Periods attended
    val missedCount: Int,     // Periods missed
    val lateCount: Int,
    val cancelledCount: Int,
    val totalConducted: Int,  // Total periods held
    val percentage: Double,
    val targetPercentage: Double,
    val healthStatus: HealthStatus,
    val bunkAllowance: Int,   // Safe periods to miss (>0) or periods needed to recover (<0)
    val statusMessage: String,
    val records: List<AttendanceRecord> = emptyList()
)

data class DayAttendanceStat(
    val dayName: String, // e.g. "Mon"
    val dateEpochDay: Long,
    val attended: Int,
    val missed: Int,
    val total: Int
)

data class PeriodSlotAttendance(
    val periodNumber: Int,
    val timeLabel: String,
    val attendedCount: Int,
    val totalCount: Int,
    val percentage: Double
)

data class OverallStats(
    val totalCourses: Int = 0,
    val totalConducted: Int = 0,     // Total periods conducted
    val totalAttended: Int = 0,      // Total periods attended
    val totalMissed: Int = 0,        // Total periods missed
    val totalLate: Int = 0,
    val totalCancelled: Int = 0,
    val overallPercentage: Double = 0.0,
    val coursesOnTrack: Int = 0,
    val coursesAtRisk: Int = 0,
    val averageTargetPercentage: Double = 75.0,
    val weeklyStats: List<DayAttendanceStat> = emptyList(),
    val periodSlotStats: List<PeriodSlotAttendance> = emptyList()
)

object AttendanceCalculator {

    fun calculateCourseStats(
        course: Course,
        records: List<AttendanceRecord>
    ): CourseStats {
        val courseRecords = records.filter { it.courseId == course.id }

        var attended = course.initialAttended
        var missed = course.initialMissed
        var late = 0
        var cancelled = 0

        for (record in courseRecords) {
            when (record.status) {
                AttendanceStatus.PRESENT -> attended++
                AttendanceStatus.ABSENT -> missed++
                AttendanceStatus.LATE -> {
                    late++
                    attended++ // late counts as attended
                }
                AttendanceStatus.CANCELLED -> cancelled++
            }
        }

        val totalConducted = attended + missed
        val percentage = if (totalConducted > 0) {
            (attended.toDouble() / totalConducted) * 100.0
        } else {
            100.0
        }

        val target = course.targetPercentage
        val targetFraction = target / 100.0

        val healthStatus = when {
            totalConducted == 0 -> HealthStatus.ON_TRACK
            percentage >= target -> HealthStatus.ON_TRACK
            percentage >= (target - 5.0) -> HealthStatus.WARNING
            else -> HealthStatus.CRITICAL
        }

        val bunkAllowance: Int
        val statusMessage: String

        if (totalConducted == 0) {
            bunkAllowance = 0
            statusMessage = "No periods conducted yet"
        } else if (percentage >= target) {
            val maxCanMiss = floor((attended / targetFraction) - totalConducted).toInt()
            bunkAllowance = maxCanMiss.coerceAtLeast(0)
            statusMessage = if (bunkAllowance == 0) {
                "On the edge! Don't miss next period"
            } else if (bunkAllowance == 1) {
                "Safe to miss 1 more period"
            } else {
                "Safe to miss $bunkAllowance more periods"
            }
        } else {
            val needed = ceil((targetFraction * totalConducted - attended) / (1.0 - targetFraction)).toInt()
            val safeNeeded = needed.coerceAtLeast(1)
            bunkAllowance = -safeNeeded
            statusMessage = if (safeNeeded == 1) {
                "Must attend next period to recover"
            } else {
                "Must attend next $safeNeeded periods consecutively"
            }
        }

        return CourseStats(
            course = course,
            attendedCount = attended,
            missedCount = missed,
            lateCount = late,
            cancelledCount = cancelled,
            totalConducted = totalConducted,
            percentage = percentage,
            targetPercentage = target,
            healthStatus = healthStatus,
            bunkAllowance = bunkAllowance,
            statusMessage = statusMessage,
            records = courseRecords
        )
    }

    fun calculateOverallStats(
        courses: List<Course>,
        records: List<AttendanceRecord>,
        weeklyStats: List<DayAttendanceStat>
    ): OverallStats {
        if (courses.isEmpty()) {
            return OverallStats(weeklyStats = weeklyStats)
        }

        var totalAttended = 0
        var totalMissed = 0
        var totalLate = 0
        var totalCancelled = 0
        var coursesOnTrack = 0
        var coursesAtRisk = 0
        var sumTarget = 0.0

        val courseStatsList = courses.map { course ->
            val stat = calculateCourseStats(course, records)
            totalAttended += stat.attendedCount
            totalMissed += stat.missedCount
            totalLate += stat.lateCount
            totalCancelled += stat.cancelledCount
            sumTarget += course.targetPercentage

            if (stat.healthStatus == HealthStatus.ON_TRACK) {
                coursesOnTrack++
            } else {
                coursesAtRisk++
            }
            stat
        }

        val totalConducted = totalAttended + totalMissed
        val overallPercentage = if (totalConducted > 0) {
            (totalAttended.toDouble() / totalConducted) * 100.0
        } else {
            100.0
        }

        // Calculate period slots (1..8) breakdown
        val periodSlotStats = (1..8).map { pNum ->
            val pRecords = records.filter { it.periodNumber == pNum }
            val pAttended = pRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            val pMissed = pRecords.count { it.status == AttendanceStatus.ABSENT }
            val pTotal = pAttended + pMissed
            val pPercent = if (pTotal > 0) (pAttended.toDouble() / pTotal) * 100.0 else 100.0
            val (st, _) = DefaultPeriodTimings.getSlotTiming(pNum)

            PeriodSlotAttendance(
                periodNumber = pNum,
                timeLabel = st,
                attendedCount = pAttended,
                totalCount = pTotal,
                percentage = pPercent
            )
        }

        return OverallStats(
            totalCourses = courses.size,
            totalConducted = totalConducted,
            totalAttended = totalAttended,
            totalMissed = totalMissed,
            totalLate = totalLate,
            totalCancelled = totalCancelled,
            overallPercentage = overallPercentage,
            coursesOnTrack = coursesOnTrack,
            coursesAtRisk = coursesAtRisk,
            averageTargetPercentage = sumTarget / courses.size,
            weeklyStats = weeklyStats,
            periodSlotStats = periodSlotStats
        )
    }
}
