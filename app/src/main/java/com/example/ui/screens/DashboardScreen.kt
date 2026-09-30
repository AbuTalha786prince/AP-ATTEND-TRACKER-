package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.CourseStats
import com.example.data.model.HealthStatus
import com.example.ui.components.AttendanceDonutGauge
import com.example.ui.components.HealthStatusBadge
import com.example.ui.components.PeriodAttendanceItemCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.WeeklyAttendanceBarChart
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import com.example.viewmodel.DashboardUiState
import com.example.viewmodel.TodayPeriodItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onMarkPeriodAttendance: (courseId: Long, periodNumber: Int, status: AttendanceStatus) -> Unit,
    onMarkAllTodayPeriods: (AttendanceStatus) -> Unit,
    onSelectCourse: (Course) -> Unit,
    onNavigateToCourses: () -> Unit,
    onOpenAddCourseDialog: () -> Unit,
    onOpenLogAttendanceDialog: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val overall = uiState.overallStats
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val activeTodayPeriods = uiState.todayPeriods.filter { !it.isFreePeriod && it.course != null }
    val allMarkedPresent = activeTodayPeriods.isNotEmpty() && activeTodayPeriods.all { it.todayRecord?.status == AttendanceStatus.PRESENT }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Banner with Title & Art
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner_dashboard_hero),
                    contentDescription = "Academic Dashboard Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Period Attendance",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    Text(
                        text = "${overall.totalCourses} classes • ${uiState.todayPeriods.count { !it.isFreePeriod }} periods scheduled today",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    )
                }
            }
        }

        // Attendance Percentage Gauge Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .testTag("attendance_gauge_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Period Attendance Rate",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Total periods attended vs held",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        val healthStatus = when {
                            overall.overallPercentage >= overall.averageTargetPercentage -> HealthStatus.ON_TRACK
                            overall.overallPercentage >= (overall.averageTargetPercentage - 5.0) -> HealthStatus.WARNING
                            else -> HealthStatus.CRITICAL
                        }
                        HealthStatusBadge(status = healthStatus)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AttendanceDonutGauge(
                        percentage = overall.overallPercentage,
                        targetPercentage = overall.averageTargetPercentage,
                        size = 190.dp,
                        strokeWidth = 18.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val summaryText = if (overall.coursesAtRisk == 0) {
                        "🎉 Great standing! All ${overall.totalCourses} classes meet your ${overall.averageTargetPercentage.toInt()}% period target."
                    } else {
                        "⚠️ ${overall.coursesAtRisk} class${if (overall.coursesAtRisk > 1) "es" else ""} below target! Attend upcoming periods to stay safe."
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (overall.coursesAtRisk == 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (overall.coursesAtRisk == 0) Color(0xFF065F46) else Color(0xFF991B1B)
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // 4 Quick Metrics Grid (Period Focused)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Periods Attended",
                        value = "${overall.totalAttended}",
                        subtitle = "of ${overall.totalConducted} periods held",
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Periods Missed",
                        value = "${overall.totalMissed}",
                        subtitle = "Missed school periods",
                        icon = Icons.Default.Close,
                        iconTint = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Classes on Track",
                        value = "${overall.coursesOnTrack}/${overall.totalCourses}",
                        subtitle = "Meeting period criteria",
                        icon = Icons.Default.School,
                        iconTint = IndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "At Risk",
                        value = "${overall.coursesAtRisk}",
                        subtitle = "Requires attendance",
                        icon = Icons.Default.Warning,
                        iconTint = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weekly Activity Chart
        item {
            WeeklyAttendanceBarChart(
                weeklyStats = overall.weeklyStats,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            )
        }

        // Period-by-Period School Timeline Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Periods",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Track each period as it happens",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Fast Action: Mark All Present
                    if (activeTodayPeriods.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { onMarkAllTodayPeriods(AttendanceStatus.PRESENT) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("mark_all_present_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (allMarkedPresent) "All Present ✓" else "Mark All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.todayPeriods.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Classes Scheduled Today",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add classes or check the Schedule tab to customize your period timetable.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.todayPeriods) { periodItem ->
                PeriodAttendanceItemCard(
                    periodNumber = periodItem.periodNumber,
                    startTime = periodItem.startTime,
                    endTime = periodItem.endTime,
                    course = periodItem.course,
                    record = periodItem.todayRecord,
                    isFreePeriod = periodItem.isFreePeriod,
                    onMarkStatus = { status ->
                        periodItem.course?.let { course ->
                            onMarkPeriodAttendance(course.id, periodItem.periodNumber, status)
                        }
                    },
                    onCardClick = {
                        periodItem.course?.let { onSelectCourse(it) }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                )
            }
        }

        // Period Attendance Rate Matrix (P1 to P7 Distribution)
        if (overall.periodSlotStats.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Period Attendance Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Your attendance rate across periods of the day",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(overall.periodSlotStats.take(7)) { slotStat ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "P${slotStat.periodNumber}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.0f%%", slotStat.percentage),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (slotStat.percentage >= 75.0) EmeraldSuccess else RoseError
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${slotStat.attendedCount}/${slotStat.totalCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Classes Snapshot Carousel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Period Stats",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "See All (${uiState.courses.size})",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { onNavigateToCourses() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.courseStats) { stat ->
                        CourseMiniSnapshotCard(
                            stats = stat,
                            onClick = { onSelectCourse(stat.course) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseMiniSnapshotCard(
    stats: CourseStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val course = stats.course
    val accentColor = Color(course.colorHex)

    Card(
        modifier = modifier
            .width(170.dp)
            .clickable { onClick() }
            .testTag("mini_card_${course.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = course.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = course.code.ifBlank { "Class" },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", stats.percentage),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = when (stats.healthStatus) {
                        HealthStatus.ON_TRACK -> EmeraldSuccess
                        HealthStatus.WARNING -> AmberWarning
                        HealthStatus.CRITICAL -> RoseError
                    }
                )
            )

            Text(
                text = "${stats.attendedCount}/${stats.totalConducted} periods",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}
