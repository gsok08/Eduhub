package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.EnrolledStudent
import kotlinx.coroutines.launch


@Composable
fun StudentsScreen(
    courseId: String,
    canManage: Boolean = false
) {

    var students by remember {
        mutableStateOf<List<EnrolledStudent>>(
            emptyList()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    val scope = rememberCoroutineScope()


    suspend fun refreshStudents() {

        loading = true

        students =
            CourseRepository
                .fetchEnrolledStudents(
                    courseId
                )

        loading = false
    }


    LaunchedEffect(courseId) {
        refreshStudents()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "${students.size} Students",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        } else if (students.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "No students enrolled"
                )
            }

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = students,
                    key = { student ->
                        student.userId
                    }
                ) { student ->

                    StudentCard(
                        student = student,
                        canManage = canManage,

                        onRemove = {

                            scope.launch {

                                CourseRepository
                                    .removeStudentFromCourse(
                                        courseId = courseId,
                                        studentUserId =
                                            student.userId
                                    )

                                // Reload after remove
                                refreshStudents()
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun StudentCard(
    student: EnrolledStudent,
    canManage: Boolean,
    onRemove: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // Student icon
            androidx.compose.material3.Icon(
                imageVector =
                    Icons.Default.Person,

                contentDescription =
                    "Student"
            )


            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )


            // Student information
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        student.fullName,

                    style =
                        MaterialTheme.typography
                            .titleMedium
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        if (
                            student.email.isNotBlank()
                        ) {

                            student.email

                        } else {

                            "Email unavailable"
                        },

                    style =
                        MaterialTheme.typography
                            .bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }


            // Only shown when course opened
            // from lecturer Courses page
            if (canManage) {

                TextButton(
                    onClick = onRemove
                ) {

                    Text(
                        text = "Remove"
                    )
                }
            }
        }
    }
}