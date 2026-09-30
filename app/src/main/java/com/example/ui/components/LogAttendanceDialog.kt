package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogAttendanceDialog(
    courses: List<Course>,
    preselectedCourseId: Long? = null,
    preselectedPeriod: Int = 1,
    onDismiss: () -> Unit,
    onSave: (courseId: Long, periodNumber: Int, status: AttendanceStatus, dateEpochDay: Long, note: String) -> Unit
) {
    if (courses.isEmpty()) return

    var selectedCourse by remember {
        mutableStateOf(courses.firstOrNull { it.id == preselectedCourseId } ?: courses.first())
    }
    var selectedPeriodNumber by remember { mutableIntStateOf(preselectedPeriod) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var note by remember { mutableStateOf("") }

    val today = LocalDate.now()
    var selectedDateEpochDay by remember { mutableLongStateOf(today.toEpochDay()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Log Period Attendance", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Course Dropdown
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCourse.code} - ${selectedCourse.name}".trimStart(' ', '-'),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class / Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} ${course.name}".trim()) },
                                onClick = {
                                    selectedCourse = course
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Period Number Selector (Periods 1 to 7)
                Column {
                    Text(
                        text = "School Period",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items((1..7).toList()) { pNum ->
                            FilterChip(
                                selected = selectedPeriodNumber == pNum,
                                onClick = { selectedPeriodNumber = pNum },
                                label = { Text("P$pNum", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Status Chips
                Column {
                    Text(
                        text = "Attendance Status",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.values().forEach { status ->
                            val isSelected = selectedStatus == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatus = status },
                                label = {
                                    Text(
                                        when (status) {
                                            AttendanceStatus.PRESENT -> "Present"
                                            AttendanceStatus.ABSENT -> "Absent"
                                            AttendanceStatus.LATE -> "Late"
                                            AttendanceStatus.CANCELLED -> "Cancelled"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Date Selector Quick Chips
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dates = listOf(
                            "Today" to today.toEpochDay(),
                            "Yesterday" to today.minusDays(1).toEpochDay(),
                            "2 Days Ago" to today.minusDays(2).toEpochDay()
                        )
                        dates.forEach { (label, epoch) ->
                            FilterChip(
                                selected = selectedDateEpochDay == epoch,
                                onClick = { selectedDateEpochDay = epoch },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Text(
                        text = LocalDate.ofEpochDay(selectedDateEpochDay).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Test, Substitution teacher") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_record_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCourse.id, selectedPeriodNumber, selectedStatus, selectedDateEpochDay, note.trim())
                },
                modifier = Modifier.testTag("save_attendance_log_btn")
            ) {
                Text("Log Period")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
