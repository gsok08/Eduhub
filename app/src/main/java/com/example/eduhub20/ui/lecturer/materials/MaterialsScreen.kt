package com.example.eduhub20.ui.lecturer.materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.data.repository.PastYearRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler


@Composable
fun MaterialsScreen(
    courseCode: String,
    canManage: Boolean = false,
    onUploadNote: () -> Unit = {},
    onOpenPaper: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    var openError by remember {
        mutableStateOf<String?>(null)
    }

    var notes by remember { mutableStateOf(
            NoteQuizRepository.getNotes()
        )
    }
    var papers by remember { mutableStateOf(
            PastYearRepository.getPapers()
        )
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            notes =
                NoteQuizRepository.fetchNotesFromSupabase()
            papers =
                PastYearRepository.fetchPapersFromSupabase()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Materials",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (canManage) {

            Button(
                onClick = onUploadNote,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Upload Lecture Note")
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = onOpenPaper,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Upload Past Year Paper")
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ){
            item {
                Text(
                    text = "Lecture Notes",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(
                notes.filter {
                    it.courseCode == courseCode
                }
            ){ note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                val url = note.pdfUrl

                                if (!url.isNullOrBlank()) {

                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        openError =
                                            "Unable to open this lecture note."
                                    }

                                } else {

                                    openError =
                                        "No PDF file is available for this lecture note."
                                }
                            },

                        shape = RoundedCornerShape(16.dp)
                ){
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ){
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null
                        )

                        Spacer(

                            modifier = Modifier.width(12.dp)
                        )

                        Column {
                            Text(
                                text = note.chapterTitle,
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "PDF Lecture Note"
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Past Year Papers",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                papers.filter {
                    it.courseCode == courseCode
                }
            ){ paper ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            val url = paper.pdfUrl

                            if (url.isNotBlank()) {

                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    openError =
                                        "Unable to open this past year paper."
                                }

                            } else {

                                openError =
                                    "No PDF file is available for this paper."
                            }
                        },

                    shape = RoundedCornerShape(16.dp)
                ){
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ){
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column {
                            Text(
                                text = paper.session,
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text =
                                    "${paper.year} • ${paper.durationMinutes} mins • ${paper.totalMarks} marks"
                            )
                        }
                    }
                    openError?.let { message ->

                        AlertDialog(
                            onDismissRequest = {
                                openError = null
                            },

                            title = {
                                Text("Unable to Open File")
                            },

                            text = {
                                Text(message)
                            },

                            confirmButton = {

                                TextButton(
                                    onClick = {
                                        openError = null
                                    }
                                ) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}