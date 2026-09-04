package com.example.eduhub20.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.EduHubUser
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.CalendarRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.data.service.NotificationService
import com.example.eduhub20.data.service.NotificationSeverity
import com.example.eduhub20.data.service.NotificationType
import com.example.eduhub20.ui.components.NotificationDialog
import com.example.eduhub20.ui.components.ScheduleItemType
import com.example.eduhub20.ui.components.UniversalScheduleDialog
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.CardGreen
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: EduHubUser?,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToLecturerPortal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val courses = remember(currentUser?.id) {
        mutableStateListOf(*CourseRepository.getCoursesForUser(currentUser).toTypedArray())
    }

    var showHiddenCoursesDialog by remember { mutableStateOf(false) }

    fun refreshCourses() {
        courses.clear()
        courses.addAll(CourseRepository.getCoursesForUser(currentUser))
    }

    // Live Periodic Auto-Refresh (every 15s) from Supabase
    LaunchedEffect(currentUser?.id) {
        while (true) {
            CourseRepository.fetchCoursesFromSupabase()
            refreshCourses()
            NoteQuizRepository.fetchNotesFromSupabase()
            delay(15_000L)
        }
    }

    // ── Schedule & Notifications System ─────────────────────────────────────
    val exams by CalendarRepository.exams.collectAsState()
    val tasks by CalendarRepository.tasks.collectAsState()
    val reminders by CalendarRepository.reminders.collectAsState()

    var dismissedNotifIds by remember(currentUser?.id) {
        mutableStateOf(
            if (currentUser != null) EduHubLocalStorage.loadDismissedNotifications(currentUser.id)
            else emptySet()
        )
    }

    val activeNotifications = remember(exams, reminders, tasks, dismissedNotifIds) {
        NotificationService.computeNotifications(
            exams = exams,
            reminders = reminders,
            tasks = tasks,
            dismissedIds = dismissedNotifIds
        )
    }

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddInitialType by remember { mutableStateOf(ScheduleItemType.EXAM) }

    // Init Calendar Repository for active user
    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            CalendarRepository.initForUser(currentUser.id)
        }
    }

    // Global search
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var joining by remember { mutableStateOf(false) }

    // Global search results
    val matchedCourses = if (searchQuery.isBlank()) emptyList()
    else CourseRepository.getCourses().filter {
        it.code.contains(searchQuery, true) || it.title.contains(searchQuery, true) || it.lecturerName.contains(searchQuery, true)
    }
    val matchedNotes = if (searchQuery.isBlank()) emptyList()
    else NoteQuizRepository.getNotes().filter {
        it.chapterTitle.contains(searchQuery, true) || it.courseCode.contains(searchQuery, true)
    }
    val matchedPapers = if (searchQuery.isBlank()) emptyList()
    else PastYearRepository.searchPapers(searchQuery, "All", "All")

    Scaffold(
        floatingActionButton = {
            if (currentUser?.role == UserRole.STUDENT) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = EduHubPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Join Course", tint = Color.White)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // ── Top Bar ────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToProfile() }
                        ) {
                            com.example.eduhub20.ui.common.UserAvatar(
                                avatarUrl = currentUser?.avatarUrl,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Welcome back!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser?.name ?: "User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        BadgedBox(
                            badge = {
                                if (activeNotifications.isNotEmpty()) {
                                    val hasUrgent = activeNotifications.any { it.severity == NotificationSeverity.URGENT }
                                    Badge(
                                        containerColor = if (hasUrgent) Color(0xFFDC2626) else Color(0xFFF59E0B),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (activeNotifications.size > 9) "9+" else "${activeNotifications.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { showNotificationDialog = true }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (activeNotifications.isNotEmpty()) EduHubPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Global Search Bar ──────────────────────────────────
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; searchActive = it.isNotBlank() },
                        placeholder = { Text("Search courses, notes, papers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = ""; searchActive = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Global Search Results ──────────────────────────────
                if (searchActive) {
                    if (matchedCourses.isEmpty() && matchedNotes.isEmpty() && matchedPapers.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    "No results found for \"$searchQuery\"",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (matchedCourses.isNotEmpty()) {
                        item { Text("Courses", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = EduHubPrimary) }
                        items(matchedCourses) { c ->
                            Spacer(modifier = Modifier.height(8.dp))
                            CourseCardItem(
                                course = c,
                                isStudent = currentUser?.role == UserRole.STUDENT,
                                onClick = { onNavigateToCourse(c.id) },
                                onHide = {
                                    if (currentUser != null) {
                                        CourseRepository.hideCourse(currentUser.id, c.id)
                                        refreshCourses()
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (matchedNotes.isNotEmpty()) {
                        item { Text("Lecture Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = EduHubAccentGreen) }
                        items(matchedNotes) { n ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = EduHubAccentGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(n.chapterTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "${n.courseCode} · ${n.semesterPeriod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (matchedPapers.isNotEmpty()) {
                        item { Text("Past Year Papers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = EduHubAccentOrange) }
                        items(matchedPapers) { p ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = EduHubAccentOrange)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(p.session, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "${p.courseCode} · ${p.subjectCategory} · ${p.year}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    return@LazyColumn
                }

                // ── Lecturer Portal Banner ─────────────────────────────
                if (currentUser?.role == UserRole.LECTURER) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EduHubAccentOrange.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToLecturerPortal() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = EduHubAccentOrange)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Lecturer Portal", fontWeight = FontWeight.Bold, color = EduHubAccentOrange)
                                        Text("Upload notes & announcements", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                    tint = EduHubAccentOrange, modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ── Planner & Reminders Quick Section ────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = EduHubPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Planner & Reminders",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                if (activeNotifications.isNotEmpty()) {
                                    Surface(
                                        color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { showNotificationDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${activeNotifications.size} alert${if (activeNotifications.size > 1) "s" else ""}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }
                            }

                            // If there is an urgent or upcoming alert, show preview card
                            if (activeNotifications.isNotEmpty()) {
                                val topNotif = activeNotifications.first()
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showNotificationDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (topNotif.type) {
                                                NotificationType.EXAM_COUNTDOWN -> "🎯"
                                                NotificationType.REMINDER -> "⏰"
                                                NotificationType.TASK -> "📋"
                                            },
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                topNotif.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                topNotif.timeRemainingText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = EduHubPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3 Quick Add Buttons: Exam, To-Do, Reminder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        quickAddInitialType = ScheduleItemType.EXAM
                                        showQuickAddDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("🎯 + Exam", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        quickAddInitialType = ScheduleItemType.TASK
                                        showQuickAddDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("📋 + To-Do", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        quickAddInitialType = ScheduleItemType.REMINDER
                                        showQuickAddDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("⏰ + Reminder", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── My Courses Header ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentUser?.role == UserRole.LECTURER) "Courses I Teach" else "My Courses",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentUser?.role == UserRole.STUDENT) {
                                val hiddenCount = CourseRepository.getHiddenCoursesForUser(currentUser).size
                                if (hiddenCount > 0) {
                                    TextButton(onClick = { showHiddenCoursesDialog = true }) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Hidden ($hiddenCount)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                TextButton(onClick = { showJoinDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = EduHubPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Course", fontWeight = FontWeight.Bold, color = EduHubPrimary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Course List (empty state) ──────────────────────────
                if (courses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.School, contentDescription = null,
                                    modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                if (currentUser?.role == UserRole.LECTURER) {
                                    Text("No courses created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Go to your Lecturer Portal to create your first course.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = onNavigateToLecturerPortal) {
                                        Text("Open Lecturer Portal")
                                    }
                                } else {
                                    Text("You haven't joined any courses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Tap the '+' button below or 'Join Course' above to enter a join code provided by your lecturer.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(courses) { course ->
                        CourseCardItem(
                            course = course,
                            isStudent = currentUser?.role == UserRole.STUDENT,
                            onClick = { onNavigateToCourse(course.id) },
                            onHide = {
                                if (currentUser != null) {
                                    CourseRepository.hideCourse(currentUser.id, course.id)
                                    refreshCourses()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ── Join Course Dialog ─────────────────────────────────────────
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; joinError = null },
            title = { Text("Join Course") },
            text = {
                Column {
                    Text("Enter the join code shared by your lecturer:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode =
                            it.uppercase()
                                .trim()
                            joinError = null },
                        label = { Text("Join Code ") },
                        placeholder = { Text("e.g. AMIK7P4W") },
                        singleLine = true,
                        isError = joinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (joinError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(joinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled =
                        joinCode.isNotBlank() &&
                                !joining,
                    onClick = {
                        scope.launch {
                            joining = true
                            joinError = null

                            val result =
                                CourseRepository
                                    .joinCourseWithCode(
                                        code = joinCode,
                                        studentUser = currentUser
                                    )
                            if (result.isSuccess) {
                                // Get latest courses from Supabase
                                CourseRepository
                                    .fetchCoursesFromSupabase()
                                // Refresh student's My Courses
                                refreshCourses()
                                showJoinDialog = false
                                joinCode = ""
                                joinError = null
                            } else {
                                joinError =
                                    result
                                        .exceptionOrNull()
                                        ?.message
                                        ?: "Unable to join course."
                            }
                            joining = false
                        }
                    }
                ) {
                    if (joining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )
                        Text("Joining...")
                    } else {
                        Text("Join")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false; joinError = null }) { Text("Cancel") } }
        )
    }

    // ── Hidden Courses Manager Dialog ──────────────────────────────
    if (showHiddenCoursesDialog) {
        val hiddenList = CourseRepository.getHiddenCoursesForUser(currentUser)
        AlertDialog(
            onDismissRequest = { showHiddenCoursesDialog = false },
            title = { Text("Hidden Courses", fontWeight = FontWeight.Bold) },
            text = {
                if (hiddenList.isEmpty()) {
                    Text("No hidden courses.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(hiddenList) { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${c.code} ${c.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Lecturer: ${c.lecturerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        if (currentUser != null) {
                                            CourseRepository.unhideCourse(currentUser.id, c.id)
                                            refreshCourses()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Unhide", fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHiddenCoursesDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // ── Interactive Notifications Modal ──────────────────────────────────────
    if (showNotificationDialog) {
        NotificationDialog(
            notifications = activeNotifications,
            onDismissRequest = { showNotificationDialog = false },
            onNavigateToCalendar = {
                showNotificationDialog = false
                onNavigateToCalendar()
            },
            onDismissNotification = { notifId ->
                val updated = dismissedNotifIds + notifId
                dismissedNotifIds = updated
                if (currentUser != null) {
                    EduHubLocalStorage.saveDismissedNotifications(currentUser.id, updated)
                }
            },
            onClearAll = {
                val allIds = activeNotifications.map { it.id }.toSet()
                val updated = dismissedNotifIds + allIds
                dismissedNotifIds = updated
                if (currentUser != null) {
                    EduHubLocalStorage.saveDismissedNotifications(currentUser.id, updated)
                }
            }
        )
    }

    // ── Universal Quick Add Dialog (Exam / Task / Reminder) ──────────────────
    if (showQuickAddDialog) {
        UniversalScheduleDialog(
            initialType = quickAddInitialType,
            onDismiss = { showQuickAddDialog = false },
            onCreated = {
                // CalendarRepository updates reactively, recalculating notifications instantly
            }
        )
    }
}

@Composable
fun CourseCardItem(
    course: Course,
    isStudent: Boolean = false,
    onClick: () -> Unit,
    onHide: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardColor = when (course.iconCategory) {
        "CODE" -> CardCoral
        "ENG" -> CardBlue
        "MATH" -> CardGreen
        else -> CardCoral
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${course.code} ${course.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lecturer: ${course.lecturerName}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { course.progress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF334155),
                    trackColor = Color.White.copy(alpha = 0.6f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (course.iconCategory) {
                        "CODE" -> Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFE07A5F))
                        "MATH" -> Icon(Icons.Default.Functions, contentDescription = null, tint = Color(0xFF059669))
                        else -> Text("ENG", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                    }
                }

                if (isStudent) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Course Options", tint = Color(0xFF334155))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hide Course") },
                                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onHide()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}