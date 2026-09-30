package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.DefaultPeriodTimings
import com.example.data.model.PeriodSchedule
import com.example.viewmodel.DashboardUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    uiState: DashboardUiState,
    onMarkPeriodAttendance: (courseId: Long, periodNumber: Int, status: AttendanceStatus) -> Unit,
    onAssignPeriodSchedule: (dayOfWeek: String, periodNumber: Int, courseId: Long, room: String) -> Unit,
    onRemovePeriodSchedule: (dayOfWeek: String, periodNumber: Int) -> Unit,
    onSelectCourse: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val todayDayCode = today.dayOfWeek.name.take(3)

    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    var selectedDayCode by remember { mutableStateOf(todayDayCode) }

    var editingPeriodNumber by remember { mutableStateOf<Int?>(null) }
    var editingSchedule by remember { mutableStateOf<PeriodSchedule?>(null) }

    val fullDayName = dayLabels.getOrNull(days.indexOf(selectedDayCode)) ?: selectedDayCode

    val periodsForDay = remember(selectedDayCode, uiState.allPeriodSchedules, uiState.courses) {
        val schedulesForDay = uiState.allPeriodSchedules.filter { it.dayOfWeek.equals(selectedDayCode, ignoreCase = true) }
        (1..7).map { pNum ->
            val schedule = schedulesForDay.firstOrNull { it.periodNumber == pNum }
            val course = if (schedule != null) uiState.courses.firstOrNull { it.id == schedule.courseId } else null
            val (defStart, defEnd) = DefaultPeriodTimings.getSlotTiming(pNum)
            val startTime = schedule?.startTime?.ifBlank { defStart } ?: defStart
            val endTime = schedule?.endTime?.ifBlank { defEnd } ?: defEnd
            val stat = if (course != null) uiState.courseStats.firstOrNull { it.course.id == course.id } else null

            SchedulePeriodItemData(
                periodNumber = pNum,
                startTime = startTime,
                endTime = endTime,
                schedule = schedule,
                course = course,
                courseStat = stat
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_screen")
    ) {
        TopAppBar(
            title = {
                Text("Period Timetable", fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Day of Week Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days.indices.toList()) { index ->
                val dayCode = days[index]
                val isSelected = dayCode == selectedDayCode
                val isToday = dayCode == todayDayCode

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .clickable { selectedDayCode = dayCode }
                        .testTag("schedule_tab_$dayCode")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayCode,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                        if (isToday) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$fullDayName's Periods",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Tap to change class",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(periodsForDay) { itemData ->
                SchedulePeriodCard(
                    data = itemData,
                    onEditPeriod = {
                        editingPeriodNumber = itemData.periodNumber
                        editingSchedule = itemData.schedule
                    },
                    onCardClick = {
                        itemData.course?.let { onSelectCourse(it) }
                    }
                )
            }
        }
    }

    // Dialog for Assigning / Editing Period Schedule
    if (editingPeriodNumber != null) {
        val pNum = editingPeriodNumber!!
        var selectedCourseId by remember {
            mutableStateOf(editingSchedule?.courseId ?: uiState.courses.firstOrNull()?.id ?: 0L)
        }
        var roomText by remember {
            mutableStateOf(editingSchedule?.room ?: "")
        }
        var courseDropdownExpanded by remember { mutableStateOf(false) }

        val selectedCourse = uiState.courses.firstOrNull { it.id == selectedCourseId }

        AlertDialog(
            onDismissRequest = { editingPeriodNumber = null },
            title = {
                Text("Configure Period $pNum ($selectedDayCode)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Assign which class is taught during Period $pNum on $fullDayName.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Course Dropdown
                    ExposedDropdownMenuBox(
                        expanded = courseDropdownExpanded,
                        onExpandedChange = { courseDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse?.name ?: "Select Class",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            uiState.courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code} - ${course.name}".trimStart(' ', '-')) },
                                    onClick = {
                                        selectedCourseId = course.id
                                        courseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = roomText,
                        onValueChange = { roomText = it },
                        label = { Text("Room / Location (Optional)") },
                        placeholder = { Text("e.g. Science Lab 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedCourseId != 0L) {
                            onAssignPeriodSchedule(selectedDayCode, pNum, selectedCourseId, roomText.trim())
                        }
                        editingPeriodNumber = null
                    }
                ) {
                    Text("Save Period")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (editingSchedule != null) {
                        OutlinedButton(
                            onClick = {
                                onRemovePeriodSchedule(selectedDayCode, pNum)
                                editingPeriodNumber = null
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                    OutlinedButton(onClick = { editingPeriodNumber = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

data class SchedulePeriodItemData(
    val periodNumber: Int,
    val startTime: String,
    val endTime: String,
    val schedule: PeriodSchedule?,
    val course: Course?,
    val courseStat: com.example.data.model.CourseStats?
)

@Composable
fun SchedulePeriodCard(
    data: SchedulePeriodItemData,
    onEditPeriod: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFree = data.course == null
    val accentColor = data.course?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_period_${data.periodNumber}")
            .clickable {
                if (!isFree) onCardClick() else onEditPeriod()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFree) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFree) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFree) MaterialTheme.colorScheme.surfaceVariant
                            else accentColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P${data.periodNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFree) MaterialTheme.colorScheme.onSurfaceVariant else accentColor
                        )
                    )
                }

                Column {
                    if (data.course != null) {
                        Text(
                            text = data.course.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${data.startTime} - ${data.endTime} • ${data.schedule?.room?.ifBlank { data.course.room } ?: data.course.room}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    } else {
                        Text(
                            text = "Free Period",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "${data.startTime} - ${data.endTime} • Tap to assign class",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            IconButton(onClick = onEditPeriod) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
