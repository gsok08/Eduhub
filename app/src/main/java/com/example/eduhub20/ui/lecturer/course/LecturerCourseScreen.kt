package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import androidx.compose.material.icons.filled.Add

@Composable
fun LecturerCoursesScreen(
    onCourseClick: (String) -> Unit,
    onCreateCourse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by AuthRepository.currentUser.collectAsState()

    var courses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(currentUser) {
        loading = true

        CourseRepository.fetchCoursesFromSupabase()

        courses = CourseRepository.getCoursesForUser(
            currentUser
        )

        loading = false
    }

    val filteredCourses = courses.filter { course ->
        course.code.contains(
            searchText,
            ignoreCase = true
        ) ||
                course.title.contains(
                    searchText,
                    ignoreCase = true
                )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "Courses",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Manage your courses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            Button(
                onClick = onCreateCourse,
                shape = RoundedCornerShape(12.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text("Create")
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = {
                Text("Search courses...")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "My Courses",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        when {

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredCourses.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text =
                                if (searchText.isBlank()) {
                                    "No courses created yet"
                                } else {
                                    "No courses found"
                                },
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (searchText.isBlank()) {

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Create your first course to get started.",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Button(
                                onClick = onCreateCourse
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.width(6.dp)
                                )

                                Text("Create New Course")
                            }
                        }
                    }
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = filteredCourses,
                        key = { it.id }
                    ) { course ->

                        LecturerCourseCard(
                            course = course,
                            onClick = {
                                onCourseClick(course.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerCourseCard(
    course: Course,
    onClick: () -> Unit
) {

    var studentCount by remember(course.id) {
        mutableIntStateOf(0)
    }

    var noteCount by remember(course.code) {
        mutableIntStateOf(0)
    }

    var pastYearCount by remember(course.code) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(course.id, course.code) {

        studentCount =
            CourseRepository.getCourseStudentCount(
                course.id
            )

        noteCount =
            CourseRepository.getLectureNoteCount(
                course.code
            )

        pastYearCount =
            CourseRepository.getPastYearCount(
                course.code
            )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme
                    .primaryContainer
                    .copy(alpha = 0.35f)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = course.code,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp
                    )

                    Text(
                        text = course.title,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Lecturer: ${course.lecturerName}",
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "→",
                    fontSize = 24.sp
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Students",
                        fontSize = 12.sp
                    )

                    Text(
                        text = studentCount.toString(),
                        fontSize = 14.sp
                    )
                }

                Column {

                    Text(
                        text = "Materials",
                        fontSize = 12.sp
                    )

                    Text(
                        text =
                            "${noteCount + pastYearCount} Files",
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Notes: $noteCount",
                        fontSize = 12.sp
                    )

                    Text(
                        text =
                            "Past Year: $pastYearCount",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}