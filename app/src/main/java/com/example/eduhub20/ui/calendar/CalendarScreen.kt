package com.example.eduhub20.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.CalendarTask
import com.example.eduhub20.data.model.ExamCountdown
import com.example.eduhub20.data.repository.CalendarRepository
import com.example.eduhub20.ui.components.ConfettiEffect
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val daysInMonthList = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    var currentMonthIndex by remember { mutableIntStateOf(5) }
    var currentYear by remember { mutableIntStateOf(2026) }
    var selectedDay by remember { mutableIntStateOf(28) }

    val monthName = monthNames[currentMonthIndex]
    val maxDays = daysInMonthList[currentMonthIndex]
    val formattedDate = "%04d-%02d-%02d".format(currentYear, currentMonthIndex + 1, selectedDay)

    val countdowns = remember { mutableStateListOf(*CalendarRepository.getCountdowns().toTypedArray()) }
    val tasks = remember { mutableStateListOf(*CalendarRepository.getTasks().toTypedArray()) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newExamCourse by remember { mutableStateOf("") }
    var newExamDays by remember { mutableStateOf("") }
    var showCelebrationConfetti by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reminder & Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EduHubAccentOrange)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Exam Countdown Card ──────────────────────────────────
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
                        Text("Exam Count Down", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E3A8A))
                        IconButton(onClick = { showAddExamDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add Exam", tint = EduHubPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (countdowns.isEmpty()) {
                        Text(
                            text = "No upcoming exams. Tap '+' to add your exam countdown!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        countdowns.forEachIndexed { idx, exam ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${exam.courseCode} Exam : ${exam.daysLeft} Days Left",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        countdowns.removeAt(idx)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Task List Card ───────────────────────────────────────
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EduHubPrimary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tasks – $monthName $selectedDay, $currentYear", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = { showAddTaskDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = EduHubPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val dayTasks = tasks.filter { it.date == formattedDate }

                    if (dayTasks.isEmpty()) {
                        Text(
                            text = "No study tasks for $monthName $selectedDay. Tap '+' to add a plan!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        dayTasks.forEach { task ->
                            val idx = tasks.indexOf(task)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        if (idx != -1) {
                                            tasks[idx] = task.copy(isCompleted = checked)
                                            CalendarRepository.toggleTask(task.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                            // Check if all tasks for today are now completed!
                                            val remaining = tasks.filter { it.date == formattedDate && !it.isCompleted }
                                            if (remaining.isEmpty() && checked) {
                                                showCelebrationConfetti = true
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = EduHubAccentGreen)
                                )
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Calendar Grid ────────────────────────────────────────
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
                            if (selectedDay > daysInMonthList[currentMonthIndex]) selectedDay = daysInMonthList[currentMonthIndex]
                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Prev") }

                        Text("$monthName $currentYear", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentMonthIndex < 11) currentMonthIndex++ else {
                                currentMonthIndex = 0
                                currentYear++
                            }
                            if (selectedDay > daysInMonthList[currentMonthIndex]) selectedDay = daysInMonthList[currentMonthIndex]
                        }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                            Text(d, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day grid (35 cells)
                    for (row in 0 until 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 1..7) {
                                val dayNum = row * 7 + col
                                if (dayNum <= maxDays) {
                                    val isSelected = dayNum == selectedDay
                                    val hasTask = tasks.any {
                                        it.date == "%04d-%02d-%02d".format(currentYear, currentMonthIndex + 1, dayNum)
                                    }
                                    val targetBg = when {
                                        isSelected -> Color(0xFF1E293B)
                                        hasTask -> EduHubPrimary.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                    val animatedBg by animateColorAsState(targetValue = targetBg, label = "CalDayBg")

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(animatedBg)
                                            .clickable {
                                                selectedDay = dayNum
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(34.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Task Dialog
        if (showAddTaskDialog) {
            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                title = { Text("Add Task – $monthName $selectedDay, $currentYear") },
                text = {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val date = "%04d-%02d-%02d".format(currentYear, currentMonthIndex + 1, selectedDay)
                            val task = CalendarTask(java.util.UUID.randomUUID().toString(), newTaskTitle.trim(), false, date)
                            CalendarRepository.addTask(newTaskTitle.trim(), date)
                            tasks.add(task)
                            newTaskTitle = ""
                            showAddTaskDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") } }
            )
        }

        // Add Exam Dialog
        if (showAddExamDialog) {
            AlertDialog(
                onDismissRequest = { showAddExamDialog = false },
                title = { Text("Add Exam Countdown") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newExamCourse,
                            onValueChange = { newExamCourse = it },
                            label = { Text("Course Code (e.g. AMIT 3353)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newExamDays,
                            onValueChange = { newExamDays = it.filter { c -> c.isDigit() } },
                            label = { Text("Days Until Exam") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val days = newExamDays.toIntOrNull() ?: 0
                        if (newExamCourse.isNotBlank() && days > 0) {
                            val cd = ExamCountdown(newExamCourse.trim().uppercase(), newExamCourse.trim(), days)
                            CalendarRepository.addCountdown(cd)
                            countdowns.add(cd)
                            newExamCourse = ""
                            newExamDays = ""
                            showAddExamDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddExamDialog = false }) { Text("Cancel") } }
            )
        }

        // Confetti celebration when all daily tasks are completed
        ConfettiEffect(
            visible = showCelebrationConfetti,
            onFinished = { showCelebrationConfetti = false }
        )
    }
}