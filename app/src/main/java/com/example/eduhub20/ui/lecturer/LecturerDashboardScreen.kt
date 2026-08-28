package com.example.eduhub20.ui.lecturer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerDashboardScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val courses = remember { mutableStateListOf(*CourseRepository.getCourses().toTypedArray()) }
    var selectedCourse by remember { mutableStateOf<Course?>(courses.firstOrNull()) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    // Note upload form
    var noteSemester by remember { mutableStateOf("2025/2026, Semester 1") }
    var noteChapterTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    // PDF Selection
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfFileName by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
        selectedPdfFileName = uri?.lastPathSegment?.substringAfterLast("/") ?: "lecture_notes.pdf"
    }

    // Announcement form
    var annTitle by remember { mutableStateOf("") }
    var annContent by remember { mutableStateOf("") }

    // Create Course Modal
    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var newCourseCode by remember { mutableStateOf("") }
    var newCourseTitle by remember { mutableStateOf("") }

    val uploadedNotes = remember { mutableStateListOf(*NoteQuizRepository.getNotes().toTypedArray()) }

    // Edit/Delete modals state for Material Notes
    var noteToEdit by remember { mutableStateOf<LectureNote?>(null) }
    var noteToDelete by remember { mutableStateOf<LectureNote?>(null) }
    var editNoteTitle by remember { mutableStateOf("") }
    var editNoteSemester by remember { mutableStateOf("") }
    var editNoteContent by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lecturer Portal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        AuthRepository.signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Lecturer Profile Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EduHubAccentOrange.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(EduHubAccentOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Teoh Li Wen (Lecturer)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("lecturer@eduhub.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showCreateCourseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EduHubAccentOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Course", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Upload Note", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Announcement", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Materials (${uploadedNotes.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Upload Note Form
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Upload Lecture Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Students will be able to generate AI notes & quizzes from this content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(14.dp))

                                if (courses.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EduHubAccentOrange.copy(alpha = 0.1f))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            "No courses created yet. Please tap '+ Course' above to create a course first.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EduHubAccentOrange
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                } else {
                                    // Select Course Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = courseDropdownExpanded,
                                        onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = if (selectedCourse != null) "${selectedCourse!!.code} ${selectedCourse!!.title} (Code: ${selectedCourse!!.joinCode})" else "Select Course",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Target Course") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = courseDropdownExpanded,
                                            onDismissRequest = { courseDropdownExpanded = false }
                                        ) {
                                            courses.forEach { course ->
                                                DropdownMenuItem(
                                                    text = { Text("${course.code} ${course.title} (Code: ${course.joinCode})") },
                                                    onClick = {
                                                        selectedCourse = course
                                                        courseDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                OutlinedTextField(
                                    value = noteSemester,
                                    onValueChange = { noteSemester = it },
                                    label = { Text("Semester / Subject Reference") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = noteChapterTitle,
                                    onValueChange = { noteChapterTitle = it },
                                    label = { Text("Chapter Title (e.g. Chapter 1 Intro)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // PDF Document File Picker
                                OutlinedButton(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFE11D48))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedPdfFileName ?: "Select & Upload Lecture PDF",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = noteContent,
                                    onValueChange = { noteContent = it },
                                    label = { Text("Lecture Slide Text / Key Summary") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (selectedCourse == null) {
                                            scope.launch { snackbarHostState.showSnackbar("Please create or select a course first.") }
                                            return@Button
                                        }
                                        if (noteChapterTitle.isNotBlank()) {
                                            val course = selectedCourse!!
                                            val newNote = LectureNote(
                                                id = UUID.randomUUID().toString(),
                                                courseCode = course.code,
                                                courseTitle = course.title,
                                                semesterPeriod = noteSemester.trim(),
                                                chapterTitle = noteChapterTitle.trim(),
                                                rawContent = if (noteContent.isBlank()) "Lecture content for ${noteChapterTitle.trim()}" else noteContent.trim(),
                                                pdfFileName = selectedPdfFileName
                                            )
                                            NoteQuizRepository.addLectureNote(newNote)
                                            uploadedNotes.add(0, newNote)
                                            noteChapterTitle = ""
                                            noteContent = ""
                                            selectedPdfFileName = null
                                            selectedPdfUri = null
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Note uploaded to ${course.code}! Students can now view & generate quizzes.")
                                            }
                                        }
                                    },
                                    enabled = selectedCourse != null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduHubAccentOrange),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload & Publish Note", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Post Announcement Form
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Post Course Announcement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(14.dp))

                                if (courses.isEmpty()) {
                                    Text("Please create a course before posting announcements.", style = MaterialTheme.typography.bodySmall, color = EduHubAccentOrange)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                OutlinedTextField(
                                    value = annTitle,
                                    onValueChange = { annTitle = it },
                                    label = { Text("Announcement Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = annContent,
                                    onValueChange = { annContent = it },
                                    label = { Text("Message Content") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (selectedCourse == null) {
                                            scope.launch { snackbarHostState.showSnackbar("Please create or select a course first.") }
                                            return@Button
                                        }
                                        if (annContent.isNotBlank()) {
                                            val course = selectedCourse!!
                                            val newAnn = Announcement(
                                                id = UUID.randomUUID().toString(),
                                                courseId = course.id,
                                                lecturerName = "Teoh Li Wen",
                                                date = "2026/08/28",
                                                title = if (annTitle.isBlank()) "Announcement" else annTitle.trim(),
                                                content = annContent.trim()
                                            )
                                            CourseRepository.addAnnouncement(newAnn)
                                            annTitle = ""
                                            annContent = ""
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Announcement published to ${course.code}.")
                                            }
                                        }
                                    },
                                    enabled = selectedCourse != null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Post Announcement", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Uploaded Materials List with Edit & Delete actions
                    if (uploadedNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No materials published yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uploadedNotes) { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${note.courseCode} · ${note.semesterPeriod}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(note.chapterTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!note.pdfFileName.isNullOrBlank()) {
                                                Text("PDF: ${note.pdfFileName}", style = MaterialTheme.typography.labelSmall, color = EduHubPrimary)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    noteToEdit = note
                                                    editNoteTitle = note.chapterTitle
                                                    editNoteSemester = note.semesterPeriod
                                                    editNoteContent = note.rawContent
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EduHubAccentOrange, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { noteToDelete = note },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Edit Lecture Note Dialog ─────────────────────────────────────────
    if (noteToEdit != null) {
        val target = noteToEdit!!
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Lecture Note", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editNoteTitle,
                        onValueChange = { editNoteTitle = it },
                        label = { Text("Chapter Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editNoteSemester,
                        onValueChange = { editNoteSemester = it },
                        label = { Text("Semester Period") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editNoteContent,
                        onValueChange = { editNoteContent = it },
                        label = { Text("Slide Content / Text") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(
                            chapterTitle = editNoteTitle.trim(),
                            semesterPeriod = editNoteSemester.trim(),
                            rawContent = editNoteContent.trim()
                        )
                        NoteQuizRepository.updateLectureNote(updated)
                        val idx = uploadedNotes.indexOfFirst { it.id == target.id }
                        if (idx != -1) uploadedNotes[idx] = updated
                        noteToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Note updated successfully.") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubAccentOrange)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // ── Delete Lecture Note Dialog ───────────────────────────────────────
    if (noteToDelete != null) {
        val target = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Lecture Note") },
            text = { Text("Are you sure you want to delete \"${target.chapterTitle}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        NoteQuizRepository.deleteLectureNote(target.id)
                        uploadedNotes.removeAll { it.id == target.id }
                        noteToDelete = null
                        scope.launch { snackbarHostState.showSnackbar("Note deleted.") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Create Course Dialog ─────────────────────────────────────────────
    if (showCreateCourseDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCourseDialog = false },
            title = { Text("Create Course & Join Code") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCourseCode,
                        onValueChange = { newCourseCode = it },
                        label = { Text("Course Code (e.g. AMIT 3353)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCourseTitle,
                        onValueChange = { newCourseTitle = it },
                        label = { Text("Course Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCourseCode.isNotBlank() && newCourseTitle.isNotBlank()) {
                            val created = CourseRepository.createCourse(newCourseCode, newCourseTitle, "Teoh Li Wen")
                            courses.add(0, created)
                            selectedCourse = created
                            showCreateCourseDialog = false
                            newCourseCode = ""
                            newCourseTitle = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("Course created! Share Join Code '${created.joinCode}' with students.")
                            }
                        }
                    }
                ) {
                    Text("Create Course")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCourseDialog = false }) { Text("Cancel") }
            }
        )
    }
}