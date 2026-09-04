package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.repository.CourseRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack


@Composable
fun CreateCourseScreen(
    lecturerName: String,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("2025/2026 Semester 1") }
    var classGroup by remember { mutableStateOf("DCS3A") }
    val scope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ) {
        Row {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Text(
                text = "Create New Course",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = courseName,
            onValueChange = { courseName = it },
            label = { Text("Course Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = courseCode,
            onValueChange = { courseCode = it },
            label = { Text("Course Code") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = semester,
            onValueChange = { semester = it },
            label = { Text("Semester") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = classGroup,
            onValueChange = { classGroup = it },
            label = { Text("Class / Group") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = {
                scope.launch {
                    CourseRepository.createCourse(
                        code = courseCode,
                        title = courseName,
                        lecturerName = lecturerName
                    )

                    onCreated()
                }


            },


            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape = RoundedCornerShape(14.dp)

        ) {

            Text(
                "Create Course"
            )

        }

    }

}