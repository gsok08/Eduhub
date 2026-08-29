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
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.EduHubUser
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.CardGreen
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: EduHubUser?,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToLecturerPortal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courses = remember { mutableStateListOf(*CourseRepository.getCourses().toTypedArray()) }

    // Fetch live courses from Supabase
    LaunchedEffect(Unit) {
        val remoteCourses = CourseRepository.fetchCoursesFromSupabase()
        courses.clear()
        courses.addAll(remoteCourses)
        NoteQuizRepository.fetchNotesFromSupabase()
    }

    // Global search
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

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
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(EduHubPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = EduHubPrimary, modifier = Modifier.size(34.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Welcome back!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser?.name ?: "User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
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
                            CourseCardItem(course = c, onClick = { onNavigateToCourse(c.id) })
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
                                    Icon(Icons.Default.Article, contentDescription = null, tint = EduHubAccentOrange)
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
                        if (currentUser?.role == UserRole.STUDENT) {
                            TextButton(onClick = { showJoinDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = EduHubPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Join Course", fontWeight = FontWeight.Bold, color = EduHubPrimary)
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
                                    Text("No courses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
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
                                        "Tap the '+' button below or 'Join Course' above to enroll with a code from your lecturer.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(courses) { course ->
                        CourseCardItem(course = course, onClick = { onNavigateToCourse(course.id) })
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
                        onValueChange = { joinCode = it.uppercase(); joinError = null },
                        label = { Text("Join Code (e.g. MAD335)") },
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
                Button(onClick = {
                    CourseRepository.joinCourseWithCode(joinCode).fold(
                        onSuccess = { c ->
                            if (!courses.any { it.id == c.id }) courses.add(0, c)
                            showJoinDialog = false
                            joinCode = ""
                            joinError = null
                        },
                        onFailure = { e -> joinError = e.message }
                    )
                }) { Text("Join") }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false; joinError = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CourseCardItem(course: Course, onClick: () -> Unit) {
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
                Text(
                    "Join Code: ${course.joinCode}", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { course.progress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF334155),
                    trackColor = Color.White.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                when (course.iconCategory) {
                    "CODE" -> Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFE07A5F))
                    "MATH" -> Icon(Icons.Default.Functions, contentDescription = null, tint = Color(0xFF059669))
                    else -> Text("ENG", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                }
            }
        }
    }
}