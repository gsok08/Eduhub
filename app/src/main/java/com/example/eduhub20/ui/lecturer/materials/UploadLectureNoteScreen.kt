package com.example.eduhub20.ui.lecturer.materials

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.repository.NoteQuizRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadLectureNoteScreen(
    courseCode: String,
    courseTitle: String,
    onBack: () -> Unit,
    onUploaded: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var semester by remember {
        mutableStateOf("")
    }

    // Chapter
    var chapter by remember {
        mutableStateOf("")
    }

    // File
    var selectedFile by remember {
        mutableStateOf<Uri?>(null)
    }

    var fileName by remember {
        mutableStateOf("")
    }

    var uploading by remember {
        mutableStateOf(false)
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            selectedFile = uri

            fileName =
                uri?.lastPathSegment
                    ?: "document.pdf"
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Row {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier =
                    Modifier.padding(top = 10.dp)
            ) {

                Text(
                    text = "Upload Lecture Note",
                    style =
                        MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /*
         * COURSE
         *
         * Important:
         * use readOnly instead of enabled = false.
         *
         * enabled = false makes the text grey and
         * difficult to read.
         */
        OutlinedTextField(
            value =
                "$courseCode - $courseTitle",

            onValueChange = {},

            readOnly = true,

            label = {
                Text("Course")
            },

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(10.dp),

            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Semester dropdown
        OutlinedTextField(
            value = semester,

            onValueChange = {
                semester = it
            },

            label = {
                Text("Semester")
            },

            placeholder = {
                Text(
                    "e.g. 2025/2026 Semester 1"
                )
            },

            supportingText = {
                Text(
                    "Example: 2025/2026 Semester 2"
                )
            },

            modifier = Modifier.fillMaxWidth(),

            singleLine = true,

            shape = RoundedCornerShape(10.dp)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Chapter / topic
        OutlinedTextField(
            value = chapter,

            onValueChange = {
                chapter = it
            },

            label = {
                Text("Chapter / Topic")
            },

            placeholder = {
                Text(
                    "e.g. Chapter 5 - RecyclerView"
                )
            },

            supportingText = {

                Text(
                    "Example: Chapter 3 - UI Components"
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine = true,

            shape =
                RoundedCornerShape(10.dp)
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Upload File",
            style =
                MaterialTheme.typography.labelLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {
                launcher.launch(
                    "application/pdf"
                )
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),

            shape =
                RoundedCornerShape(10.dp)
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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "PDF files only",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            enabled =
                selectedFile != null &&
                        semester.isNotBlank() &&
                        chapter.isNotBlank() &&
                        !uploading,

            onClick = {

                scope.launch {

                    uploading = true

                    val url =
                        NoteQuizRepository
                            .uploadPdfToSupabase(
                                context = context,
                                uri = selectedFile!!,
                                fileName = fileName
                            )

                    if (url != null) {

                        val note =
                            LectureNote(
                                id =
                                    UUID.randomUUID()
                                        .toString(),

                                courseCode =
                                    courseCode,

                                courseTitle =
                                    courseTitle,

                                semesterPeriod =
                                    semester,

                                chapterTitle =
                                    chapter.trim(),

                                rawContent = "",

                                pdfFileName =
                                    fileName,

                                pdfUrl =
                                    url
                            )

                        NoteQuizRepository
                            .addLectureNote(
                                note
                            )

                        onUploaded()
                    }

                    uploading = false
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape =
                RoundedCornerShape(10.dp)
        ) {

            if (uploading) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text("Uploading...")

            } else {

                Text("Upload")
            }
        }
    }
}