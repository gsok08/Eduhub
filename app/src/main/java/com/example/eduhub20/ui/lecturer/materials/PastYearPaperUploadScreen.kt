package com.example.eduhub20.ui.lecturer.materials


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.repository.PastYearRepository
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment

@Composable
fun PastYearPaperUploadScreen(
    courseCode: String,
    onBack: () -> Unit,
    onUploaded: () -> Unit
) {

    var session by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("120") }
    var marks by remember { mutableStateOf("100") }
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }

    val launcher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            selectedPdf = uri

            fileName =
                uri?.lastPathSegment
                    ?: ""
        }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    fun uploadPaper(){
        val uri = selectedPdf ?: return
        scope.launch {
            val pdfUrl =
                PastYearRepository.uploadPdfToSupabase(
                    context = context,
                    uri = uri,
                    fileName = fileName
                )?: ""
            val paper = PastYearPaper(
                id = UUID.randomUUID().toString(),
                courseCode = courseCode,
                courseTitle = "",
                session = session,
                subjectCategory = "Mobile App",
                year = year,
                durationMinutes =
                    duration.toIntOrNull() ?: 120,
                totalMarks =
                    marks.toIntOrNull() ?: 100,
                pdfUrl = pdfUrl
            )
            PastYearRepository.addPaper(
                paper
            )
            onUploaded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ){
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
                text = "Upload Past Year Paper",
                style =
                    MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = session,
            onValueChange = { session = it },
            label = { Text("Exam Session") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Example: 2025/2026 Semester 1 Final Exam",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 4.dp
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Example: 2025/2026",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 4.dp
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it },
            label = { Text("Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Example: 120",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 4.dp
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = marks,
            onValueChange = { marks = it },
            label = { Text("Total Marks") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Example: 100",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 4.dp
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Upload File",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        OutlinedButton(
            onClick = {
                launcher.launch("application/pdf")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text =
                    if (fileName.isBlank()) {
                        "Select PDF File"
                    } else {
                        fileName
                    }
            )
        }

        Text(
            text = "PDF files only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = 4.dp
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                uploadPaper()

            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                "Upload"
            )
        }

    }
}