package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.repository.CourseRepository

enum class LecturerCourseMode {
    VIEW,
    MANAGE;

    companion object {
        fun fromString(value: String): LecturerCourseMode {
            return if (
                value.equals("manage", ignoreCase = true)
            ) {
                MANAGE
            } else {
                VIEW
            }
        }
    }
}

@Composable
fun CourseMainScreen(
    courseId: String,
    mode: LecturerCourseMode,
    onBack: () -> Unit,
    onAddAnnouncement: () -> Unit,
    onUploadNote: (String) -> Unit,
    onUploadPaper: (String) -> Unit
) {

    var selectedTab by rememberSaveable {
        mutableIntStateOf(0)
    }

    var course by remember(courseId) {
        mutableStateOf<Course?>(
            CourseRepository.getCourseById(courseId)
        )
    }

    var loading by remember {
        mutableStateOf(course == null)
    }

    LaunchedEffect(courseId) {
        loading = true

        CourseRepository.fetchCoursesFromSupabase()

        course =
            CourseRepository.getCourseById(courseId)

        loading = false
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        return
    }

    val selectedCourse = course

    if (selectedCourse == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text("Course not found")
        }

        return
    }

    val canManage =
        mode == LecturerCourseMode.MANAGE

    val tabs = listOf(
        "Announcements",
        "Materials",
        "Students"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // =====================================================
        // COURSE HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(
                modifier = Modifier.width(2.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = selectedCourse.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(1.dp)
                )

                Text(
                    text = selectedCourse.title,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // =====================================================
        // COMPACT JOIN CODE CARD — MANAGE MODE ONLY
        // =====================================================

        if (canManage) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 12.dp
                    ),
                shape = RoundedCornerShape(16.dp),
                color =
                    MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = 0.30f)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Course Join Code",
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Share this code with students to join the course.",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text =
                                if (selectedCourse.joinCode.isNotBlank()) {
                                    selectedCourse.joinCode
                                } else {
                                    "No code"
                                },
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 9.dp
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =====================================================
        // TABS
        // =====================================================

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            fontWeight =
                                if (selectedTab == index) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                        )
                    }
                )
            }
        }

        // =====================================================
        // TAB CONTENT
        // =====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val safeCourseCode =
                course?.code ?: courseId

            when (selectedTab) {

                0 -> {
                    LecturerAnnouncementsSection(
                        courseId = courseId,
                        canManage = canManage,
                        onAddAnnouncement = onAddAnnouncement,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 18.dp,
                                bottom = 8.dp
                            )
                    )
                }

                1 -> {
                    LecturerMaterialsSection(
                        courseCode = safeCourseCode,
                        canManage = canManage,
                        onUploadNote = {
                            onUploadNote(safeCourseCode)
                        },
                        onUploadPaper = {
                            onUploadPaper(safeCourseCode)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 18.dp,
                                bottom = 8.dp
                            )
                    )
                }

                2 -> {
                    StudentsScreen(
                        courseId = selectedCourse.id,
                        canManage = canManage
                    )
                }
            }
        }
    }
}