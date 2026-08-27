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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val course = CourseRepository.getCourseById(courseId) ?: CourseRepository.getCourses().first()
    val announcements = CourseRepository.getAnnouncementsForCourse(course.id)
    val notes = NoteQuizRepository.getNotes().filter { it.courseCode == course.code || course.code.isBlank() }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Course", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 20.dp)
        ) {
            // Course Header Banner (From Slide 6 Screen 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardCoral)
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
                            text = " ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "2025/2026",
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lecturer: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFE07A5F))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-module Tabs: Announcement & Lecturer Note (From Slide 6 Screens 3 & 4)
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
                    text = { Text("Announcement", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Lecturer Note", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content List
            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(announcements) { ann ->
                        AnnouncementItem(announcement = ann)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notes) { note ->
                        LecturerNoteItem(
                            note = note,
                            onClick = { onNavigateToNoteDetail(note.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = announcement.lecturerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Text(
                text = announcement.date,
                style = MaterialTheme.typography.bodySmall,
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
    onClick: () -> Unit
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