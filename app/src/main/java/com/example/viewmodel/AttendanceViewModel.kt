package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceCalculator
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.CourseStats
import com.example.data.model.DayAttendanceStat
import com.example.data.model.DefaultPeriodTimings
import com.example.data.model.OverallStats
import com.example.data.model.PeriodSchedule
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class TodayPeriodItem(
    val periodNumber: Int,
    val startTime: String,
    val endTime: String,
    val schedule: PeriodSchedule?,
    val course: Course?,
    val courseStats: CourseStats?,
    val todayRecord: AttendanceRecord?,
    val isFreePeriod: Boolean
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val courses: List<Course> = emptyList(),
    val courseStats: List<CourseStats> = emptyList(),
    val overallStats: OverallStats = OverallStats(),
    val todayPeriods: List<TodayPeriodItem> = emptyList(),
    val allPeriodSchedules: List<PeriodSchedule> = emptyList(),
    val allRecords: List<AttendanceRecord> = emptyList(),
    val userMessage: String? = null
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage

    // Filtering for History screen
    private val _historyFilterCourseId = MutableStateFlow<Long?>(null)
    val historyFilterCourseId: StateFlow<Long?> = _historyFilterCourseId

    private val _historyFilterStatus = MutableStateFlow<AttendanceStatus?>(null)
    val historyFilterStatus: StateFlow<AttendanceStatus?> = _historyFilterStatus

    init {
        val db = AppDatabase.getInstance(application)
        repository = AttendanceRepository(db.attendanceDao())

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allCourses,
        repository.allRecords,
        repository.allPeriodSchedules,
        _userMessage
    ) { courses, records, schedules, message ->
        val today = LocalDate.now()
        val currentDayCode = today.dayOfWeek.name.take(3) // MON, TUE, etc.
        val todayEpochDay = today.toEpochDay()

        // Calculate course stats (period based)
        val statsList = courses.map { course ->
            AttendanceCalculator.calculateCourseStats(course, records)
        }

        // Calculate weekly stats (last 7 days)
        val weeklyStats = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val epochDay = date.toEpochDay()
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            val recordsOnDate = records.filter { it.dateEpochDay == epochDay }
            val attended = recordsOnDate.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            val missed = recordsOnDate.count { it.status == AttendanceStatus.ABSENT }

            DayAttendanceStat(
                dayName = dayName,
                dateEpochDay = epochDay,
                attended = attended,
                missed = missed,
                total = attended + missed
            )
        }

        val overall = AttendanceCalculator.calculateOverallStats(courses, records, weeklyStats)

        // Compute Today's Periods (Periods 1 to 7 or 8)
        val todaySchedules = schedules.filter { it.dayOfWeek.equals(currentDayCode, ignoreCase = true) }
        val maxPeriods = 7

        val todayPeriods = (1..maxPeriods).map { pNum ->
            val (defStart, defEnd) = DefaultPeriodTimings.getSlotTiming(pNum)
            val schedule = todaySchedules.firstOrNull { it.periodNumber == pNum }
            val course = if (schedule != null) courses.firstOrNull { it.id == schedule.courseId } else null
            val courseStat = if (course != null) statsList.firstOrNull { it.course.id == course.id } else null
            val record = records.firstOrNull { it.dateEpochDay == todayEpochDay && it.periodNumber == pNum }

            TodayPeriodItem(
                periodNumber = pNum,
                startTime = schedule?.startTime?.ifBlank { defStart } ?: defStart,
                endTime = schedule?.endTime?.ifBlank { defEnd } ?: defEnd,
                schedule = schedule,
                course = course,
                courseStats = courseStat,
                todayRecord = record,
                isFreePeriod = schedule == null || course == null
            )
        }

        DashboardUiState(
            isLoading = false,
            courses = courses,
            courseStats = statsList,
            overallStats = overall,
            todayPeriods = todayPeriods,
            allPeriodSchedules = schedules,
            allRecords = records,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun markPeriodAttendance(
        courseId: Long,
        periodNumber: Int,
        status: AttendanceStatus,
        dateEpochDay: Long = LocalDate.now().toEpochDay(),
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.markPeriodAttendance(courseId, periodNumber, status, dateEpochDay, note)
            val statusLabel = when (status) {
                AttendanceStatus.PRESENT -> "Period $periodNumber: Present (+1)"
                AttendanceStatus.ABSENT -> "Period $periodNumber: Marked Absent"
                AttendanceStatus.LATE -> "Period $periodNumber: Marked Late"
                AttendanceStatus.CANCELLED -> "Period $periodNumber: Cancelled"
            }
            _userMessage.value = statusLabel
        }
    }

    fun markAllTodayPeriods(status: AttendanceStatus) {
        viewModelScope.launch {
            val periods = uiState.value.todayPeriods.filter { !it.isFreePeriod && it.course != null }
            val todayEpoch = LocalDate.now().toEpochDay()
            for (p in periods) {
                repository.markPeriodAttendance(p.course!!.id, p.periodNumber, status, todayEpoch)
            }
            _userMessage.value = if (status == AttendanceStatus.PRESENT) {
                "All ${periods.size} periods marked Present!"
            } else {
                "All ${periods.size} periods marked Absent!"
            }
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            _userMessage.value = "Attendance record deleted"
        }
    }

    fun addCourse(
        name: String,
        code: String,
        instructor: String,
        room: String,
        colorHex: Long,
        targetPercentage: Double,
        daysOfWeek: List<String>,
        startTime: String,
        endTime: String,
        initialAttended: Int,
        initialMissed: Int
    ) {
        viewModelScope.launch {
            val daysString = daysOfWeek.joinToString(",")
            val newCourse = Course(
                name = name.trim(),
                code = code.trim(),
                instructor = instructor.trim(),
                room = room.trim(),
                colorHex = colorHex,
                targetPercentage = targetPercentage,
                daysOfWeek = daysString,
                startTime = startTime,
                endTime = endTime,
                initialAttended = initialAttended,
                initialMissed = initialMissed
            )
            repository.insertCourse(newCourse)
            _userMessage.value = "Added class: ${newCourse.name}"
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
            _userMessage.value = "Updated: ${course.name}"
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            _userMessage.value = "Class and its records deleted"
        }
    }

    fun assignPeriodSchedule(
        dayOfWeek: String,
        periodNumber: Int,
        courseId: Long,
        room: String = ""
    ) {
        viewModelScope.launch {
            repository.setPeriodSchedule(dayOfWeek, periodNumber, courseId, room)
            _userMessage.value = "Updated Period $periodNumber timetable"
        }
    }

    fun removePeriodSchedule(dayOfWeek: String, periodNumber: Int) {
        viewModelScope.launch {
            repository.removePeriodSchedule(dayOfWeek, periodNumber)
            _userMessage.value = "Removed Period $periodNumber from $dayOfWeek"
        }
    }

    fun setHistoryFilterCourse(courseId: Long?) {
        _historyFilterCourseId.value = courseId
    }

    fun setHistoryFilterStatus(status: AttendanceStatus?) {
        _historyFilterStatus.value = status
    }

    fun updateGlobalTarget(newTarget: Double) {
        viewModelScope.launch {
            val currentCourses = uiState.value.courses
            for (c in currentCourses) {
                repository.updateCourse(c.copy(targetPercentage = newTarget))
            }
            _userMessage.value = "Updated target for all classes to ${newTarget.toInt()}%"
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _userMessage.value = "Reset to school period timetable and sample attendance"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _userMessage.value = "Cleared all classes, periods, and records"
        }
    }
}
