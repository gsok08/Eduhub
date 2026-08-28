package com.example.eduhub20.ui.course

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.CardGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = AuthRepository.currentUser.collectAsState().value
    val isLecturer = currentUser?.role == UserRole.LECTURER

    val course = CourseRepository.getCourseById(courseId) ?: CourseRepository.getCourses().find { it.id == courseId || it.code == courseId }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Dynamic state lists for announcements & notes
    val announcements = remember(courseId) {
        mutableStateListOf(*(course?.let { CourseRepository.getAnnouncementsForCourse(it.id).toTypedArray() } ?: emptyArray()))
    }
    val notes = remember(courseId) {
        mutableStateListOf(*(course?.let { c -> NoteQuizRepository.getNotes().filter { it.courseCode.equals(c.code, ignoreCase = true) || c.code.isBlank() }.toTypedArray() } ?: emptyArray()))
    }

    // Edit / Delete dialog states for Announcement
    var announcementToEdit by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }
    var editAnnTitle by remember { mutableStateOf("") }
    var editAnnContent by remember { mutableStateOf("") }

    // Edit / Delete dialog states for Lecture Note
    var noteToEdit by remember { mutableStateOf<LectureNote?>(null) }
    var noteToDelete by remember { mutableStateOf<LectureNote?>(null) }
    var editNoteTitle by remember { mutableStateOf("") }
    var editNoteSemester by remember { mutableStateOf("") }
    var editNoteContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(course?.code ?: "Course Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (course == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Course not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val bannerColor = when (course.iconCategory) {
            "CODE" -> CardCoral
            "ENG" -> CardBlue
            "MATH" -> CardGreen
            else -> CardCoral
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Course Header Banner with Title, Lecturer Name and Join Code
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bannerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${course.code} ${course.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lecturer: ${course.lecturerName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Join Code: ${course.joinCode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (course.iconCategory) {
                            "CODE" -> Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFE07A5F))
                            "MATH" -> Icon(Icons.Default.Functions, contentDescription = null, tint = Color(0xFF059669))
                            else -> Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563EB))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-module Tabs: Announcement & Lecturer Note
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Announcements (${announcements.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Lecture Notes (${notes.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content List
            if (selectedTab == 0) {
                if (announcements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No announcements for this course yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(announcements) { ann ->
                            AnnouncementItem(
                                announcement = ann,
                                isLecturer = isLecturer,
                                onEdit = {
                                    announcementToEdit = ann
                                    editAnnTitle = ann.title
                                    editAnnContent = ann.content
                                },
                                onDelete = { announcementToDelete = ann }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No lecture notes uploaded for this course yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notes) { note ->
                            LecturerNoteItem(
                                note = note,
                                isLecturer = isLecturer,
                                onClick = { onNavigateToNoteDetail(note.id) },
                                onEdit = {
                                    noteToEdit = note
                                    editNoteTitle = note.chapterTitle
                                    editNoteSemester = note.semesterPeriod
                                    editNoteContent = note.rawContent
                                },
                                onDelete = { noteToDelete = note }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Edit Announcement Modal ──────────────────────────────────────────
    if (announcementToEdit != null) {
        val target = announcementToEdit!!
        AlertDialog(
            onDismissRequest = { announcementToEdit = null },
            title = { Text("Edit Announcement", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editAnnTitle,
                        onValueChange = { editAnnTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editAnnContent,
                        onValueChange = { editAnnContent = it },
                        label = { Text("Message Content") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(
                            title = if (editAnnTitle.isBlank()) "Announcement" else editAnnTitle.trim(),
                            content = editAnnContent.trim()
                        )
                        CourseRepository.updateAnnouncement(updated)
                        val idx = announcements.indexOfFirst { it.id == target.id }
                        if (idx != -1) announcements[idx] = updated
                        announcementToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // ── Delete Announcement Confirmation ─────────────────────────────────
    if (announcementToDelete != null) {
        val target = announcementToDelete!!
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            title = { Text("Delete Announcement") },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        CourseRepository.deleteAnnouncement(target.id)
                        announcements.removeAll { it.id == target.id }
                        announcementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Edit Lecture Note Modal ──────────────────────────────────────────
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
                        val idx = notes.indexOfFirst { it.id == target.id }
                        if (idx != -1) notes[idx] = updated
                        noteToEdit = null
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

    // ── Delete Lecture Note Confirmation ─────────────────────────────────
    if (noteToDelete != null) {
        val target = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Lecture Note") },
            text = { Text("Are you sure you want to delete \"${target.chapterTitle}\"? Students will no longer see this note.") },
            confirmButton = {
                Button(
                    onClick = {
                        NoteQuizRepository.deleteLectureNote(target.id)
                        notes.removeAll { it.id == target.id }
                        noteToDelete = null
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
}

@Composable
fun AnnouncementItem(
    announcement: Announcement,
    isLecturer: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = announcement.lecturerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (isLecturer) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EduHubPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text(
                text = "${announcement.date} · ${announcement.title}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, bottom = 8.dp)
            )

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LecturerNoteItem(
    note: LectureNote,
    isLecturer: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.semesterPeriod,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!note.pdfFileName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PDF: ${note.pdfFileName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EduHubPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLecturer) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EduHubAccentOrange, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EduHubPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, contentDescription = "View Note", tint = EduHubPrimary)
                }
            }
        }
    }
}