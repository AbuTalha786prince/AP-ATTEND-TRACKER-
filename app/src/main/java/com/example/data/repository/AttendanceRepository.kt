package com.example.data.repository

import com.example.data.db.AttendanceDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.DefaultPeriodTimings
import com.example.data.model.PeriodSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class AttendanceRepository(private val dao: AttendanceDao) {

    val allCourses: Flow<List<Course>> = dao.getAllCourses()
    val allRecords: Flow<List<AttendanceRecord>> = dao.getAllRecords()
    val allPeriodSchedules: Flow<List<PeriodSchedule>> = dao.getAllPeriodSchedules()

    fun getCourseById(courseId: Long): Flow<Course?> = dao.getCourseById(courseId)

    fun getRecordsForCourse(courseId: Long): Flow<List<AttendanceRecord>> =
        dao.getRecordsForCourse(courseId)

    fun getRecordsForDate(dateEpochDay: Long): Flow<List<AttendanceRecord>> =
        dao.getRecordsForDate(dateEpochDay)

    fun getPeriodSchedulesForDay(dayOfWeek: String): Flow<List<PeriodSchedule>> =
        dao.getPeriodSchedulesForDay(dayOfWeek)

    suspend fun insertCourse(course: Course): Long = dao.insertCourse(course)

    suspend fun updateCourse(course: Course) = dao.updateCourse(course)

    suspend fun deleteCourse(courseId: Long) {
        dao.deletePeriodSchedulesByCourseId(courseId)
        dao.deleteRecordsByCourseId(courseId)
        dao.deleteCourseById(courseId)
    }

    suspend fun setPeriodSchedule(
        dayOfWeek: String,
        periodNumber: Int,
        courseId: Long,
        room: String = ""
    ) {
        val (startTime, endTime) = DefaultPeriodTimings.getSlotTiming(periodNumber)
        val schedule = PeriodSchedule(
            dayOfWeek = dayOfWeek.uppercase(),
            periodNumber = periodNumber,
            courseId = courseId,
            startTime = startTime,
            endTime = endTime,
            room = room
        )
        dao.insertPeriodSchedule(schedule)
    }

    suspend fun removePeriodSchedule(dayOfWeek: String, periodNumber: Int) {
        dao.deletePeriodSchedule(dayOfWeek.uppercase(), periodNumber)
    }

    suspend fun markPeriodAttendance(
        courseId: Long,
        periodNumber: Int,
        status: AttendanceStatus,
        dateEpochDay: Long = LocalDate.now().toEpochDay(),
        note: String = ""
    ): Long {
        val existingRecord = dao.getRecordForDateAndPeriod(dateEpochDay, periodNumber)
        val record = if (existingRecord != null) {
            existingRecord.copy(
                courseId = courseId,
                status = status,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        } else {
            AttendanceRecord(
                courseId = courseId,
                dateEpochDay = dateEpochDay,
                periodNumber = periodNumber,
                status = status,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        }
        return dao.insertRecord(record)
    }

    suspend fun deleteRecord(recordId: Long) = dao.deleteRecordById(recordId)

    suspend fun updateRecord(record: AttendanceRecord) = dao.updateRecord(record)

    suspend fun clearAll() {
        dao.clearAllRecords()
        dao.clearAllPeriodSchedules()
        dao.clearAllCourses()
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingCourses = dao.getAllCourses().first()
        if (existingCourses.isEmpty()) {
            seedSampleData()
        }
    }

    suspend fun seedSampleData() {
        dao.clearAllRecords()
        dao.clearAllPeriodSchedules()
        dao.clearAllCourses()

        val today = LocalDate.now()

        val sampleCourses = listOf(
            Course(
                id = 1,
                name = "Mathematics",
                code = "MATH-101",
                instructor = "Mr. Henderson",
                room = "Room 201",
                colorHex = 0xFF4F46E5, // Indigo
                targetPercentage = 75.0,
                daysOfWeek = "MON,TUE,WED,THU,FRI",
                startTime = "08:30 AM",
                endTime = "09:15 AM",
                initialAttended = 28,
                initialMissed = 4
            ),
            Course(
                id = 2,
                name = "Physics",
                code = "PHY-102",
                instructor = "Dr. Katherine Shaw",
                room = "Physics Lab",
                colorHex = 0xFF059669, // Emerald
                targetPercentage = 80.0,
                daysOfWeek = "MON,WED,FRI",
                startTime = "09:20 AM",
                endTime = "10:05 AM",
                initialAttended = 20,
                initialMissed = 3
            ),
            Course(
                id = 3,
                name = "Chemistry",
                code = "CHEM-103",
                instructor = "Mrs. Martinez",
                room = "Chem Lab 2",
                colorHex = 0xFFD97706, // Amber
                targetPercentage = 75.0,
                daysOfWeek = "MON,TUE,THU",
                startTime = "10:15 AM",
                endTime = "11:00 AM",
                initialAttended = 16,
                initialMissed = 6
            ),
            Course(
                id = 4,
                name = "English Literature",
                code = "ENG-201",
                instructor = "Ms. Eleanor Vance",
                room = "Room 105",
                colorHex = 0xFF7C3AED, // Purple
                targetPercentage = 75.0,
                daysOfWeek = "TUE,WED,THU,FRI",
                startTime = "11:05 AM",
                endTime = "11:50 AM",
                initialAttended = 24,
                initialMissed = 2
            ),
            Course(
                id = 5,
                name = "Computer Science",
                code = "CS-105",
                instructor = "Mr. David Miller",
                room = "Computer Lab A",
                colorHex = 0xFF0284C7, // Sky
                targetPercentage = 75.0,
                daysOfWeek = "MON,WED,FRI",
                startTime = "12:35 PM",
                endTime = "01:20 PM",
                initialAttended = 22,
                initialMissed = 3
            ),
            Course(
                id = 6,
                name = "Physical Education",
                code = "PE-100",
                instructor = "Coach Cooper",
                room = "Sports Gymnasium",
                colorHex = 0xFFE11D48, // Rose
                targetPercentage = 75.0,
                daysOfWeek = "TUE,THU",
                startTime = "01:25 PM",
                endTime = "02:10 PM",
                initialAttended = 15,
                initialMissed = 1
            )
        )

        dao.insertCourses(sampleCourses)

        // Seed Period Timetable Schedules (Monday through Friday, Periods 1 to 7)
        val periodGrid = listOf(
            // MON
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 1, courseId = 1, room = "Room 201"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 2, courseId = 2, room = "Physics Lab"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 3, courseId = 3, room = "Chem Lab 2"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 4, courseId = 4, room = "Room 105"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 5, courseId = 5, room = "Comp Lab A"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 6, courseId = 2, room = "Physics Lab"),
            PeriodSchedule(dayOfWeek = "MON", periodNumber = 7, courseId = 1, room = "Room 201"),

            // TUE
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 1, courseId = 1, room = "Room 201"),
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 2, courseId = 3, room = "Chem Lab 2"),
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 3, courseId = 4, room = "Room 105"),
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 4, courseId = 6, room = "Gymnasium"),
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 5, courseId = 5, room = "Comp Lab A"),
            PeriodSchedule(dayOfWeek = "TUE", periodNumber = 6, courseId = 1, room = "Room 201"),

            // WED
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 1, courseId = 2, room = "Physics Lab"),
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 2, courseId = 1, room = "Room 201"),
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 3, courseId = 4, room = "Room 105"),
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 4, courseId = 5, room = "Comp Lab A"),
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 5, courseId = 3, room = "Chem Lab 2"),
            PeriodSchedule(dayOfWeek = "WED", periodNumber = 6, courseId = 5, room = "Comp Lab A"),

            // THU
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 1, courseId = 1, room = "Room 201"),
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 2, courseId = 4, room = "Room 105"),
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 3, courseId = 3, room = "Chem Lab 2"),
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 4, courseId = 6, room = "Gymnasium"),
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 5, courseId = 2, room = "Physics Lab"),
            PeriodSchedule(dayOfWeek = "THU", periodNumber = 6, courseId = 4, room = "Room 105"),

            // FRI
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 1, courseId = 1, room = "Room 201"),
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 2, courseId = 2, room = "Physics Lab"),
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 3, courseId = 5, room = "Comp Lab A"),
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 4, courseId = 4, room = "Room 105"),
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 5, courseId = 5, room = "Comp Lab A"),
            PeriodSchedule(dayOfWeek = "FRI", periodNumber = 6, courseId = 3, room = "Chem Lab 2")
        ).map { schedule ->
            val (st, et) = DefaultPeriodTimings.getSlotTiming(schedule.periodNumber)
            schedule.copy(startTime = st, endTime = et)
        }

        dao.insertPeriodSchedules(periodGrid)

        // Seed recent period attendance records for the past 10 school days
        val sampleRecords = mutableListOf<AttendanceRecord>()

        for (daysAgo in 1..10) {
            val date = today.minusDays(daysAgo.toLong())
            val dayCode = date.dayOfWeek.name.take(3)
            val daySchedules = periodGrid.filter { it.dayOfWeek == dayCode }

            for (schedule in daySchedules) {
                val status = when {
                    (daysAgo + schedule.periodNumber) % 8 == 0 -> AttendanceStatus.ABSENT
                    (daysAgo + schedule.periodNumber) % 11 == 0 -> AttendanceStatus.LATE
                    (daysAgo + schedule.periodNumber) % 19 == 0 -> AttendanceStatus.CANCELLED
                    else -> AttendanceStatus.PRESENT
                }

                sampleRecords.add(
                    AttendanceRecord(
                        courseId = schedule.courseId,
                        dateEpochDay = date.toEpochDay(),
                        periodNumber = schedule.periodNumber,
                        status = status,
                        note = if (status == AttendanceStatus.ABSENT) "Absent Period ${schedule.periodNumber}" else "",
                        timestamp = System.currentTimeMillis() - (daysAgo * 86400000L)
                    )
                )
            }
        }

        dao.insertRecords(sampleRecords)
    }
}
