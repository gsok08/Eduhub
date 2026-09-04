package com.example.eduhub20.ui.calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eduhub20.data.model.ExamEntity
import com.example.eduhub20.data.model.ReminderEntity
import com.example.eduhub20.data.model.TaskEntity
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CalendarRepository
import com.example.eduhub20.ui.navigation.Screen
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.example.eduhub20.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get current user
    val currentUser = AuthRepository.currentUser.collectAsState().value

    // Collect data from CalendarRepository (Supabase synced)
    val exams by CalendarRepository.exams.collectAsState()
    val tasks by CalendarRepository.tasks.collectAsState()
    val reminders by CalendarRepository.reminders.collectAsState()

    // Date and Time formatters
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

    // Edit Exam
    var showEditExamDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamEntity?>(null) }
    var editExamName by remember { mutableStateOf("") }
    var editExamDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEditExamDatePicker by remember { mutableStateOf(false) }

    // Edit Task
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editTaskName by remember { mutableStateOf("") }
    var editTaskDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEditTaskDatePicker by remember { mutableStateOf(false) }

    // Edit Reminder
    var showEditReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var editReminderName by remember { mutableStateOf("") }
    var editReminderDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var editReminderTime by remember { mutableStateOf("") }
    var showEditReminderDatePicker by remember { mutableStateOf(false) }
    var showEditReminderTimePicker by remember { mutableStateOf(false) }

    // State
    val calendar = Calendar.getInstance()
    var currentMonthIndex by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    // Exam Countdown - Dialog states
    var showExamDialog by remember { mutableStateOf(false) }
    var examName by remember { mutableStateOf("") }
    var examDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExamDatePicker by remember { mutableStateOf(false) }

    // To Do List - Dialog states
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var taskDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showTaskDatePicker by remember { mutableStateOf(false) }

    // Reminder - Dialog states
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderName by remember { mutableStateOf("") }
    var reminderDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reminderTime by remember { mutableStateOf("") }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Today's date for filtering
    val todayMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val selectedDateMillis = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonthIndex)
        set(Calendar.DAY_OF_MONTH, selectedDay)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val isToday = selectedDateMillis == todayMillis
    val displayDate = dayFormat.format(Date(selectedDateMillis))

    // ── Load saved data from Supabase when screen opens ──
    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            CalendarRepository.fetchExams(currentUser.id)
            CalendarRepository.fetchTasks(currentUser.id)
            CalendarRepository.fetchReminders(currentUser.id)
        }
    }

    // ── Calculate days left for exams ──
    fun getTimeRemaining(examDate: Long): String {
        val now = System.currentTimeMillis()
        val diff = examDate - now
        if (diff < 0) return "Overdue"

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

        return if (days > 0) {
            "$days days ${hours}h ${minutes}m"
        } else {
            "${hours}h ${minutes}m"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reminder & Planner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Exam Countdown Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBlue)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_timer),
                                contentDescription = null,
                                tint = EduHubAccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Exam Countdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                        IconButton(
                            onClick = { showExamDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add),
                                contentDescription = "Add Exam",
                                tint = EduHubPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (exams.isEmpty()) {
                        Text(
                            text = "No upcoming exams. Tap '+' to add your exam countdown!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        exams.forEach { exam ->
                            val daysLeft = getTimeRemaining(exam.date)
                            val isOverdue = exam.date < System.currentTimeMillis()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOverdue) Color(0xFFFFEBEE)
                                        else if (exam.date - System.currentTimeMillis() < TimeUnit.DAYS.toMillis(
                                                3
                                            )
                                        )
                                            Color(0xFFFFF3E0)
                                        else Color.White.copy(alpha = 0.8f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exam.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isOverdue) Color(0xFFC62828) else Color(
                                            0xFF1E293B
                                        )
                                    )
                                    Text(
                                        text = dateFormat.format(Date(exam.date)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isOverdue) "Overdue! ⚠️" else daysLeft,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = when {
                                            isOverdue -> Color(0xFFC62828)
                                            exam.date - System.currentTimeMillis() < TimeUnit.DAYS.toMillis(
                                                3
                                            ) -> EduHubAccentOrange

                                            else -> EduHubAccentGreen
                                        }
                                    )
                                    IconButton(
                                        onClick = {
                                            editingExam = exam
                                            editExamName = exam.name
                                            editExamDate = exam.date
                                            showEditExamDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_edit),
                                            contentDescription = "Edit",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Delete exam from Supabase
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                CalendarRepository.deleteExam(exam.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Remove",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // To Do List Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_today),
                                contentDescription = null,
                                tint = EduHubPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("To Do List", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = selectedDateMillis
                                cal.add(Calendar.DAY_OF_MONTH, -1)
                                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                currentMonthIndex = cal.get(Calendar.MONTH)
                                currentYear = cal.get(Calendar.YEAR)
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_chevron_left), contentDescription = "Previous Day", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = selectedDateMillis
                                cal.add(Calendar.DAY_OF_MONTH, 1)
                                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                currentMonthIndex = cal.get(Calendar.MONTH)
                                currentYear = cal.get(Calendar.YEAR)
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = "Next Day", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showTaskDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Add Task", tint = EduHubPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isToday) "Today, $displayDate" else displayDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isToday) EduHubAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        if (!isToday) {
                            TextButton(onClick = {
                                val now = Calendar.getInstance()
                                selectedDay = now.get(Calendar.DAY_OF_MONTH)
                                currentMonthIndex = now.get(Calendar.MONTH)
                                currentYear = now.get(Calendar.YEAR)
                            }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                Text("Go to Today", fontSize = 11.sp, color = EduHubPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Task list for selected date
                    val dayTasks = tasks.filter { task ->
                        val taskCal = Calendar.getInstance().apply { timeInMillis = task.date }
                        taskCal.get(Calendar.YEAR) == currentYear &&
                                taskCal.get(Calendar.MONTH) == currentMonthIndex &&
                                taskCal.get(Calendar.DAY_OF_MONTH) == selectedDay
                    }
                    if (dayTasks.isEmpty()) {
                        Text(
                            text = if (isToday) "No tasks for today. Tap '+' to add one!" else "No tasks for this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        dayTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                                    scope.launch {
                                        CalendarRepository.updateTask(updatedTask)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Toggle Task",
                                        tint = if (task.isCompleted) EduHubAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = task.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    editingTask = task
                                    editTaskName = task.name
                                    editTaskDate = task.date
                                    showEditTaskDialog = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        CalendarRepository.deleteTask(task.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Delete Task", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
                //Reminder Card ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_event),
                                    contentDescription = null,
                                    tint = EduHubAccentOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Reminders",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { showReminderDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = "Add Reminder",
                                    tint = EduHubPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (reminders.isEmpty()) {
                            Text(
                                text = "No reminders set. Tap '+' to add a reminder!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            reminders.forEach { reminder ->
                                val isUpcoming = reminder.date > System.currentTimeMillis()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isUpcoming) MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                            else Color(0xFFFFEBEE)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isUpcoming) MaterialTheme.colorScheme.onSurface else Color(
                                                0xFFC62828
                                            )
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(reminder.date))} at ${reminder.time}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isUpcoming) MaterialTheme.colorScheme.onSurfaceVariant else Color(
                                                0xFFC62828
                                            )
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editingReminder = reminder
                                            editReminderName = reminder.name
                                            editReminderDate = reminder.date
                                            editReminderTime = reminder.time
                                            showEditReminderDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_edit),
                                            contentDescription = "Edit",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    // Delete reminder
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                CalendarRepository.deleteReminder(reminder.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Delete Reminder",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Calendar Grid ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month/Year navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentMonthIndex > 0) currentMonthIndex-- else {
                                    currentMonthIndex = 11
                                    currentYear--
                                }
                                if (selectedDay > daysInMonth(currentMonthIndex, currentYear)) {
                                    selectedDay = daysInMonth(currentMonthIndex, currentYear)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_left),
                                    contentDescription = "Prev"
                                )
                            }

                            Text(
                                "${getMonthName(currentMonthIndex)} $currentYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentMonthIndex < 11) currentMonthIndex++ else {
                                    currentMonthIndex = 0
                                    currentYear++
                                }
                                if (selectedDay > daysInMonth(currentMonthIndex, currentYear)) {
                                    selectedDay = daysInMonth(currentMonthIndex, currentYear)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_right),
                                    contentDescription = "Next"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                                Text(
                                    d,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day grid
                        val maxDays = daysInMonth(currentMonthIndex, currentYear)
                        val firstDayOfMonth = getFirstDayOfMonth(currentMonthIndex, currentYear)

                        for (row in 0 until 6) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val dayNum = row * 7 + col - firstDayOfMonth + 1
                                    if (dayNum in 1..maxDays) {
                                        val isSelected = dayNum == selectedDay
                                        val hasTask = tasks.any { task ->
                                            val taskCal = Calendar.getInstance()
                                                .apply { timeInMillis = task.date }
                                            taskCal.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                                    taskCal.get(Calendar.MONTH) == currentMonthIndex &&
                                                    taskCal.get(Calendar.YEAR) == currentYear
                                        }
                                        val hasExam = exams.any { exam ->
                                            val examCal = Calendar.getInstance()
                                                .apply { timeInMillis = exam.date }
                                            examCal.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                                    examCal.get(Calendar.MONTH) == currentMonthIndex &&
                                                    examCal.get(Calendar.YEAR) == currentYear
                                        }
                                        val isToday =
                                            dayNum == Calendar.getInstance()
                                                .get(Calendar.DAY_OF_MONTH) &&
                                                    currentMonthIndex == Calendar.getInstance()
                                                .get(Calendar.MONTH) &&
                                                    currentYear == Calendar.getInstance()
                                                .get(Calendar.YEAR)

                                        val targetBg = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            hasTask -> EduHubPrimary.copy(alpha = 0.15f)
                                            hasExam -> EduHubAccentOrange.copy(alpha = 0.15f)
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(targetBg)
                                                .clickable {
                                                    selectedDay = dayNum
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontWeight = when {
                                                    isSelected -> FontWeight.Bold
                                                    isToday -> FontWeight.Bold
                                                    else -> FontWeight.Normal
                                                },
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontSize = 12.sp
                                            )
                                            if (hasTask || hasExam) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (hasExam) EduHubAccentOrange else EduHubPrimary)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(36.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ── Legend ──
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EduHubAccentOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Exam",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EduHubPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Task",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 Tap a date to view its tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
        }

        // ── Exam Dialog ──
        if (showExamDialog) {
            AlertDialog(
                onDismissRequest = { showExamDialog = false },
                title = { Text("Add Exam Countdown") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = examName,
                            onValueChange = { examName = it },
                            label = { Text("Exam Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showExamDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Date: ${dateFormat.format(Date(examDate))}")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (examName.isNotBlank() && currentUser != null) {
                                val newExam = ExamEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = currentUser.id,
                                    name = examName,
                                    date = examDate
                                )
                                scope.launch {
                                    CalendarRepository.saveExam(newExam)
                                    examName = ""
                                    showExamDialog = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Add Exam")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExamDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Exam Date Picker ──
        if (showExamDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = examDate)
            DatePickerDialog(
                onDismissRequest = { showExamDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                examDate = it
                                showExamDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExamDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Task Dialog ──
        if (showTaskDialog) {
            AlertDialog(
                onDismissRequest = { showTaskDialog = false },
                title = { Text("Add Task") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = taskName,
                            onValueChange = { taskName = it },
                            label = { Text("Task Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showTaskDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: ${dateFormat.format(Date(taskDate))}")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskName.isNotBlank() && currentUser != null) {
                                val newTask = TaskEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = currentUser.id,
                                    name = taskName,
                                    date = taskDate,
                                    isCompleted = false
                                )
                                scope.launch {
                                    CalendarRepository.saveTask(newTask)
                                    taskName = ""
                                    showTaskDialog = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Add Task")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTaskDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Task Date Picker ──
        if (showTaskDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = taskDate)
            DatePickerDialog(
                onDismissRequest = { showTaskDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                taskDate = it
                                showTaskDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTaskDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Reminder Dialog ──
        if (showReminderDialog) {
            AlertDialog(
                onDismissRequest = { showReminderDialog = false },
                title = { Text("Add Reminder") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = reminderName,
                            onValueChange = { reminderName = it },
                            label = { Text("Reminder Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showReminderDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: ${dateFormat.format(Date(reminderDate))}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showReminderTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_timer),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (reminderTime.isBlank()) "Select Time" else "Time: $reminderTime")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reminderName.isNotBlank() && reminderTime.isNotBlank() && currentUser != null) {
                                val newReminder = ReminderEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = currentUser.id,
                                    name = reminderName,
                                    date = reminderDate,
                                    time = reminderTime
                                )
                                scope.launch {
                                    CalendarRepository.saveReminder(newReminder)
                                    reminderName = ""
                                    reminderTime = ""
                                    showReminderDialog = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Add Reminder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Reminder Date Picker ──
        if (showReminderDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = reminderDate)
            DatePickerDialog(
                onDismissRequest = { showReminderDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                reminderDate = it
                                showReminderDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Reminder Time Picker ──
        if (showReminderTimePicker) {
            var hourText by remember { mutableStateOf("") }
            var minuteText by remember { mutableStateOf("") }
            var isAm by remember { mutableStateOf(true) }
            val currentTime = Calendar.getInstance()
            val initialHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val initialMinute = currentTime.get(Calendar.MINUTE)

            LaunchedEffect(Unit) {
                val displayHour =
                    if (initialHour > 12) initialHour - 12 else if (initialHour == 0) 12 else initialHour
                hourText = displayHour.toString()
                minuteText = String.format("%02d", initialMinute)
                isAm = initialHour < 12
            }

            AlertDialog(
                onDismissRequest = { showReminderTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = hourText,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        val value = it.toIntOrNull()
                                        if (value == null || (value in 1..12)) {
                                            hourText = it
                                        }
                                    }
                                },
                                label = { Text("Hour") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Text(
                                ":",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            OutlinedTextField(
                                value = minuteText,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        val value = it.toIntOrNull()
                                        if (value == null || (value in 0..59)) {
                                            minuteText = it
                                        }
                                    }
                                },
                                label = { Text("Minute") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { isAm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAm) EduHubPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 8.dp,
                                    bottomStart = 8.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp
                                ),
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text("AM")
                            }
                            Button(
                                onClick = { isAm = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isAm) EduHubPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 0.dp,
                                    topEnd = 8.dp,
                                    bottomEnd = 8.dp
                                ),
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text("PM")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val hour = hourText.toIntOrNull() ?: 12
                            val minute = minuteText.toIntOrNull() ?: 0
                            val finalHour = if (isAm) {
                                if (hour == 12) 0 else hour
                            } else {
                                if (hour == 12) 12 else hour + 12
                            }
                            reminderTime = String.format(
                                Locale.getDefault(),
                                "%02d:%02d %s",
                                if (finalHour > 12) finalHour - 12 else if (finalHour == 0) 12 else finalHour,
                                minute,
                                if (isAm) "AM" else "PM"
                            )
                            showReminderTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Edit Exam Dialog ──
        if (showEditExamDialog && editingExam != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditExamDialog = false
                    editingExam = null
                },
                title = { Text("Edit Exam") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editExamName,
                            onValueChange = { editExamName = it },
                            label = { Text("Exam Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showEditExamDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: ${dateFormat.format(Date(editExamDate))}")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editExamName.isNotBlank() && currentUser != null && editingExam != null) {
                                val updatedExam = editingExam!!.copy(
                                    name = editExamName,
                                    date = editExamDate
                                )
                                scope.launch {
                                    // Delete old and save new
                                    CalendarRepository.deleteExam(editingExam!!.id)
                                    CalendarRepository.saveExam(updatedExam)
                                    showEditExamDialog = false
                                    editingExam = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditExamDialog = false
                        editingExam = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Edit Exam Date Picker ──
        if (showEditExamDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editExamDate)
            DatePickerDialog(
                onDismissRequest = { showEditExamDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                editExamDate = it
                                showEditExamDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditExamDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Edit Task Dialog ──
        if (showEditTaskDialog && editingTask != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditTaskDialog = false
                    editingTask = null
                },
                title = { Text("Edit Task") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTaskName,
                            onValueChange = { editTaskName = it },
                            label = { Text("Task Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showEditTaskDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: ${dateFormat.format(Date(editTaskDate))}")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editTaskName.isNotBlank() && currentUser != null && editingTask != null) {
                                val updatedTask = editingTask!!.copy(
                                    name = editTaskName,
                                    date = editTaskDate,
                                    isCompleted = editingTask!!.isCompleted
                                )
                                scope.launch {
                                    CalendarRepository.deleteTask(editingTask!!.id)
                                    CalendarRepository.saveTask(updatedTask)
                                    showEditTaskDialog = false
                                    editingTask = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditTaskDialog = false
                        editingTask = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Edit Task Date Picker ──
        if (showEditTaskDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editTaskDate)
            DatePickerDialog(
                onDismissRequest = { showEditTaskDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                editTaskDate = it
                                showEditTaskDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTaskDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Edit Reminder Dialog ──
        if (showEditReminderDialog && editingReminder != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditReminderDialog = false
                    editingReminder = null
                },
                title = { Text("Edit Reminder") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editReminderName,
                            onValueChange = { editReminderName = it },
                            label = { Text("Reminder Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showEditReminderDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Date: ${dateFormat.format(Date(editReminderDate))}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showEditReminderTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_timer),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Time: $editReminderTime")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editReminderName.isNotBlank() && editReminderTime.isNotBlank() && currentUser != null && editingReminder != null) {
                                val updatedReminder = editingReminder!!.copy(
                                    name = editReminderName,
                                    date = editReminderDate,
                                    time = editReminderTime
                                )
                                scope.launch {
                                    CalendarRepository.deleteReminder(editingReminder!!.id)
                                    CalendarRepository.saveReminder(updatedReminder)
                                    showEditReminderDialog = false
                                    editingReminder = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditReminderDialog = false
                        editingReminder = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Edit Reminder Date Picker ──
        if (showEditReminderDatePicker) {
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = editReminderDate)
            DatePickerDialog(
                onDismissRequest = { showEditReminderDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                editReminderDate = it
                                showEditReminderDatePicker = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditReminderDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── Edit Reminder Time Picker ──
        if (showEditReminderTimePicker) {
            var hourText by remember { mutableStateOf("") }
            var minuteText by remember { mutableStateOf("") }
            var isAm by remember { mutableStateOf(true) }
            val currentTime = Calendar.getInstance()
            val initialHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val initialMinute = currentTime.get(Calendar.MINUTE)

            LaunchedEffect(Unit) {
                val displayHour =
                    if (initialHour > 12) initialHour - 12 else if (initialHour == 0) 12 else initialHour
                hourText = displayHour.toString()
                minuteText = String.format("%02d", initialMinute)
                isAm = initialHour < 12
            }

            AlertDialog(
                onDismissRequest = { showEditReminderTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = hourText,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        val value = it.toIntOrNull()
                                        if (value == null || (value in 1..12)) {
                                            hourText = it
                                        }
                                    }
                                },
                                label = { Text("Hour") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Text(
                                ":",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            OutlinedTextField(
                                value = minuteText,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        val value = it.toIntOrNull()
                                        if (value == null || (value in 0..59)) {
                                            minuteText = it
                                        }
                                    }
                                },
                                label = { Text("Minute") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { isAm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAm) EduHubPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 8.dp,
                                    bottomStart = 8.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp
                                ),
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text("AM")
                            }
                            Button(
                                onClick = { isAm = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isAm) EduHubPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 0.dp,
                                    topEnd = 8.dp,
                                    bottomEnd = 8.dp
                                ),
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text("PM")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val hour = hourText.toIntOrNull() ?: 12
                            val minute = minuteText.toIntOrNull() ?: 0
                            val finalHour = if (isAm) {
                                if (hour == 12) 0 else hour
                            } else {
                                if (hour == 12) 12 else hour + 12
                            }
                            editReminderTime = String.format(
                                Locale.getDefault(),
                                "%02d:%02d %s",
                                if (finalHour > 12) finalHour - 12 else if (finalHour == 0) 12 else finalHour,
                                minute,
                                if (isAm) "AM" else "PM"
                            )
                            showEditReminderTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditReminderTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

    // ── Helper Functions ──
    fun getMonthName(monthIndex: Int): String {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return if (monthIndex in months.indices) months[monthIndex] else ""
    }

    fun daysInMonth(month: Int, year: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getFirstDayOfMonth(month: Int, year: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }