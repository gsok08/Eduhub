package com.example.eduhub20.ui.lecturer.materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.repository.CourseRepository
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment

@Composable
fun AnnouncementScreen(
    courseId: String,
    lecturerName: String,
    onBack: () -> Unit,
    onPublished: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ) {
        Row(
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

            Text(
                text = "New Announcement",
                style =
                    MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                "+ Add File"
            )
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Button(
            enabled = !publishing,
            onClick = {
                scope.launch {
                    publishing = true
                    val announcement = Announcement(
                        id = UUID.randomUUID().toString(),
                        courseId = courseId,
                        lecturerName = lecturerName,
                        date = System.currentTimeMillis().toString(),
                        title = title.trim(),
                        content = message.trim()
                    )
                    CourseRepository.addAnnouncement(
                        announcement
                    )
                    publishing = false
                    onPublished()
                }


            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape = RoundedCornerShape(14.dp)
        ){
            Text(
                if(publishing)
                    "Publishing..."
                else
                    "Publish"
            )
        }
    }
}