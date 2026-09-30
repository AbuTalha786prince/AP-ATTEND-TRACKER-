package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AttendanceCalculator
import com.example.data.model.Course
import com.example.ui.components.AddEditCourseDialog
import com.example.ui.components.LogAttendanceDialog
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AttendTrackTheme
import com.example.viewmodel.AttendanceViewModel

enum class NavigationScreen(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    COURSES("Classes", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_courses"),
    SCHEDULE("Timetable", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "nav_schedule"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendTrackTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: AttendanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val historyFilterCourseId by viewModel.historyFilterCourseId.collectAsStateWithLifecycle()
    val historyFilterStatus by viewModel.historyFilterStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }
    var selectedCourseForDetail by remember { mutableStateOf<Course?>(null) }

    var showAddEditCourseDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }

    var showLogAttendanceDialog by remember { mutableStateOf(false) }
    var logAttendancePreselectedCourseId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedCourseForDetail == null) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedCourseForDetail = null
                                currentScreen = screen
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(screen.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedCourseForDetail == null && (currentScreen == NavigationScreen.DASHBOARD || currentScreen == NavigationScreen.COURSES)) {
                FloatingActionButton(
                    onClick = {
                        courseToEdit = null
                        showAddEditCourseDialog = true
                    },
                    modifier = Modifier.testTag("fab_add_class"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedCourseForDetail != null) {
                val activeCourse = uiState.courses.firstOrNull { it.id == selectedCourseForDetail?.id }
                    ?: selectedCourseForDetail!!
                val courseStat = uiState.courseStats.firstOrNull { it.course.id == activeCourse.id }
                    ?: AttendanceCalculator.calculateCourseStats(activeCourse, uiState.allRecords)

                CourseDetailScreen(
                    course = activeCourse,
                    stats = courseStat,
                    onBack = { selectedCourseForDetail = null },
                    onEditCourse = {
                        courseToEdit = activeCourse
                        showAddEditCourseDialog = true
                    },
                    onMarkAttendance = { status ->
                        viewModel.markPeriodAttendance(activeCourse.id, 1, status)
                    },
                    onDeleteRecord = { recordId ->
                        viewModel.deleteRecord(recordId)
                    },
                    onOpenLogCustomDialog = {
                        logAttendancePreselectedCourseId = activeCourse.id
                        showLogAttendanceDialog = true
                    }
                )
            } else {
                when (currentScreen) {
                    NavigationScreen.DASHBOARD -> {
                        DashboardScreen(
                            uiState = uiState,
                            onMarkPeriodAttendance = { courseId, periodNumber, status ->
                                viewModel.markPeriodAttendance(courseId, periodNumber, status)
                            },
                            onMarkAllTodayPeriods = { status ->
                                viewModel.markAllTodayPeriods(status)
                            },
                            onSelectCourse = { course ->
                                selectedCourseForDetail = course
                            },
                            onNavigateToCourses = {
                                currentScreen = NavigationScreen.COURSES
                            },
                            onOpenAddCourseDialog = {
                                courseToEdit = null
                                showAddEditCourseDialog = true
                            },
                            onOpenLogAttendanceDialog = { courseId ->
                                logAttendancePreselectedCourseId = courseId
                                showLogAttendanceDialog = true
                            }
                        )
                    }

                    NavigationScreen.COURSES -> {
                        CoursesScreen(
                            courseStats = uiState.courseStats,
                            onMarkAttendance = { courseId, status ->
                                viewModel.markPeriodAttendance(courseId, 1, status)
                            },
                            onSelectCourse = { course ->
                                selectedCourseForDetail = course
                            },
                            onEditCourse = { course ->
                                courseToEdit = course
                                showAddEditCourseDialog = true
                            },
                            onDeleteCourse = { courseId ->
                                viewModel.deleteCourse(courseId)
                            },
                            onOpenAddCourseDialog = {
                                courseToEdit = null
                                showAddEditCourseDialog = true
                            }
                        )
                    }

                    NavigationScreen.SCHEDULE -> {
                        ScheduleScreen(
                            uiState = uiState,
                            onMarkPeriodAttendance = { courseId, periodNumber, status ->
                                viewModel.markPeriodAttendance(courseId, periodNumber, status)
                            },
                            onAssignPeriodSchedule = { dayOfWeek, periodNumber, courseId, room ->
                                viewModel.assignPeriodSchedule(dayOfWeek, periodNumber, courseId, room)
                            },
                            onRemovePeriodSchedule = { dayOfWeek, periodNumber ->
                                viewModel.removePeriodSchedule(dayOfWeek, periodNumber)
                            },
                            onSelectCourse = { course ->
                                selectedCourseForDetail = course
                            }
                        )
                    }

                    NavigationScreen.HISTORY -> {
                        HistoryScreen(
                            courses = uiState.courses,
                            records = uiState.allRecords,
                            selectedCourseId = historyFilterCourseId,
                            selectedStatus = historyFilterStatus,
                            onSelectCourseFilter = { courseId ->
                                viewModel.setHistoryFilterCourse(courseId)
                            },
                            onSelectStatusFilter = { status ->
                                viewModel.setHistoryFilterStatus(status)
                            },
                            onDeleteRecord = { recordId ->
                                viewModel.deleteRecord(recordId)
                            },
                            onOpenLogDialog = {
                                logAttendancePreselectedCourseId = null
                                showLogAttendanceDialog = true
                            }
                        )
                    }

                    NavigationScreen.SETTINGS -> {
                        SettingsScreen(
                            currentTarget = uiState.overallStats.averageTargetPercentage,
                            onUpdateGlobalTarget = { newTarget ->
                                viewModel.updateGlobalTarget(newTarget)
                            },
                            onResetToDemoData = {
                                viewModel.resetToDemoData()
                            },
                            onClearAllData = {
                                viewModel.clearAllData()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add or Edit Course Dialog
    if (showAddEditCourseDialog) {
        AddEditCourseDialog(
            initialCourse = courseToEdit,
            onDismiss = {
                showAddEditCourseDialog = false
                courseToEdit = null
            },
            onSave = { name, code, instructor, room, colorHex, targetPercentage, daysOfWeek, startTime, endTime, initialAttended, initialMissed ->
                if (courseToEdit != null) {
                    val updated = courseToEdit!!.copy(
                        name = name,
                        code = code,
                        instructor = instructor,
                        room = room,
                        colorHex = colorHex,
                        targetPercentage = targetPercentage,
                        daysOfWeek = daysOfWeek.joinToString(","),
                        startTime = startTime,
                        endTime = endTime,
                        initialAttended = initialAttended,
                        initialMissed = initialMissed
                    )
                    viewModel.updateCourse(updated)
                } else {
                    viewModel.addCourse(
                        name = name,
                        code = code,
                        instructor = instructor,
                        room = room,
                        colorHex = colorHex,
                        targetPercentage = targetPercentage,
                        daysOfWeek = daysOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        initialAttended = initialAttended,
                        initialMissed = initialMissed
                    )
                }
                showAddEditCourseDialog = false
                courseToEdit = null
            }
        )
    }

    // Log Past/Custom Attendance Dialog
    if (showLogAttendanceDialog) {
        LogAttendanceDialog(
            courses = uiState.courses,
            preselectedCourseId = logAttendancePreselectedCourseId,
            onDismiss = {
                showLogAttendanceDialog = false
                logAttendancePreselectedCourseId = null
            },
            onSave = { courseId, periodNumber, status, dateEpochDay, note ->
                viewModel.markPeriodAttendance(courseId, periodNumber, status, dateEpochDay, note)
                showLogAttendanceDialog = false
                logAttendancePreselectedCourseId = null
            }
        )
    }
}
