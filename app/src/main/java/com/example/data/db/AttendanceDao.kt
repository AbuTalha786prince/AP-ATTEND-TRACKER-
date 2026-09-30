package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.Course
import com.example.data.model.PeriodSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    // Courses
    @Query("SELECT * FROM courses WHERE archived = 0 ORDER BY id ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseById(courseId: Long): Flow<Course?>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseByIdSync(courseId: Long): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>): List<Long>

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: Long)

    // Period Timetable Schedule
    @Query("SELECT * FROM period_schedules ORDER BY periodNumber ASC")
    fun getAllPeriodSchedules(): Flow<List<PeriodSchedule>>

    @Query("SELECT * FROM period_schedules WHERE dayOfWeek = :dayOfWeek ORDER BY periodNumber ASC")
    fun getPeriodSchedulesForDay(dayOfWeek: String): Flow<List<PeriodSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodSchedule(schedule: PeriodSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodSchedules(schedules: List<PeriodSchedule>): List<Long>

    @Query("DELETE FROM period_schedules WHERE dayOfWeek = :dayOfWeek AND periodNumber = :periodNumber")
    suspend fun deletePeriodSchedule(dayOfWeek: String, periodNumber: Int)

    @Query("DELETE FROM period_schedules WHERE courseId = :courseId")
    suspend fun deletePeriodSchedulesByCourseId(courseId: Long)

    @Query("DELETE FROM period_schedules")
    suspend fun clearAllPeriodSchedules()

    // Attendance Records
    @Query("SELECT * FROM attendance_records ORDER BY dateEpochDay DESC, periodNumber ASC, timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE courseId = :courseId ORDER BY dateEpochDay DESC, periodNumber ASC, timestamp DESC")
    fun getRecordsForCourse(courseId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :dateEpochDay ORDER BY periodNumber ASC, timestamp DESC")
    fun getRecordsForDate(dateEpochDay: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :dateEpochDay AND periodNumber = :periodNumber LIMIT 1")
    suspend fun getRecordForDateAndPeriod(dateEpochDay: Long, periodNumber: Int): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE courseId = :courseId AND dateEpochDay = :dateEpochDay ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRecordForCourseOnDate(courseId: Long, dateEpochDay: Long): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>): List<Long>

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Long)

    @Query("DELETE FROM attendance_records WHERE courseId = :courseId")
    suspend fun deleteRecordsByCourseId(courseId: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllRecords()

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()
}
