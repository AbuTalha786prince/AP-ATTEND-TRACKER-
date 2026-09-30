package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceCalculator
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AttendTrack", appName)
  }

  @Test
  fun `attendance calculator computes exact safe bunk allowance`() {
    // 15 attended, 5 missed out of 20 conducted = 75%
    val course = Course(
      name = "Algorithms",
      targetPercentage = 75.0,
      initialAttended = 15,
      initialMissed = 5
    )
    val stats = AttendanceCalculator.calculateCourseStats(course, emptyList())
    assertEquals(75.0, stats.percentage, 0.01)
    assertEquals(HealthStatus.ON_TRACK, stats.healthStatus)
    assertEquals(0, stats.bunkAllowance) // At boundary: 0 safe bunks

    // If student attends 3 more classes: 18 attended, 5 missed out of 23 conducted (78.26%)
    // (18 / 0.75) - 23 = 24 - 23 = 1 safe bunk!
    val records = listOf(
      AttendanceRecord(courseId = course.id, dateEpochDay = 1, status = AttendanceStatus.PRESENT),
      AttendanceRecord(courseId = course.id, dateEpochDay = 2, status = AttendanceStatus.PRESENT),
      AttendanceRecord(courseId = course.id, dateEpochDay = 3, status = AttendanceStatus.PRESENT)
    )
    val statsWithBunk = AttendanceCalculator.calculateCourseStats(course, records)
    assertEquals(1, statsWithBunk.bunkAllowance)
  }

  @Test
  fun `attendance calculator computes consecutive classes needed when below target`() {
    // 6 attended, 4 missed out of 10 conducted = 60%, target 75%
    val course = Course(
      name = "Physics",
      targetPercentage = 75.0,
      initialAttended = 6,
      initialMissed = 4
    )
    val stats = AttendanceCalculator.calculateCourseStats(course, emptyList())
    assertEquals(60.0, stats.percentage, 0.01)
    assertEquals(HealthStatus.CRITICAL, stats.healthStatus)
    // Formula: ceil((0.75 * 10 - 6) / 0.25) = ceil(1.5 / 0.25) = 6 classes needed
    assertEquals(-6, stats.bunkAllowance)
    assertTrue(stats.statusMessage.contains("6 periods"))
  }
}
