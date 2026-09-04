package com.example.eduhub20.ui.lecturer.course

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.ui.components.PdfAnnotationViewer
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun LecturerMaterialsSection(
    courseCode: String,
    canManage: Boolean,
    onUploadNote: () -> Unit,
    onUploadPaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notes by remember(courseCode) {
        mutableStateOf(NoteQuizRepository.getNotes())
    }

    var papers by remember(courseCode) {
        mutableStateOf(PastYearRepository.getPapers())
    }

    var loading by remember { mutableStateOf(true) }

    var noteToEdit by remember { mutableStateOf<LectureNote?>(null) }
    var noteToDelete by remember { mutableStateOf<LectureNote?>(null) }

    // This is used by the three-dot button.
    // We intentionally use an AlertDialog rather than DropdownMenu because it is
    // more reliable inside the LazyColumn/card hierarchy used by this screen.
    var noteOptions by remember { mutableStateOf<LectureNote?>(null) }

    var paperOptions by remember {
        mutableStateOf<PastYearPaper?>(null)
    }

    var paperToEdit by remember {
        mutableStateOf<PastYearPaper?>(null)
    }

    var paperToDelete by remember {
        mutableStateOf<PastYearPaper?>(null)
    }

    // PDF viewers
    var viewingNote by remember { mutableStateOf<LectureNote?>(null) }
    var viewingPaper by remember { mutableStateOf<PastYearPaper?>(null) }

    fun refreshMaterials() {
        notes = NoteQuizRepository.getNotes()
        papers = PastYearRepository.getPapers()
    }

    LaunchedEffect(courseCode) {
        loading = true

        notes = NoteQuizRepository.fetchNotesFromSupabase()
        papers = PastYearRepository.fetchPapersFromSupabase()

        loading = false
    }

    val courseNotes = notes.filter { note ->
        note.courseCode.equals(courseCode, ignoreCase = true)
    }

    val coursePapers = papers.filter { paper ->
        paper.courseCode.equals(courseCode, ignoreCase = true)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // =====================================================
        // PAGE HEADER
        // =====================================================

        Text(
            text = "Materials",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (canManage) {
                "Manage course learning materials"
            } else {
                "Course learning materials"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================
        // COMPACT UPLOAD ACTIONS
        // =====================================================

        if (canManage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUploadNote,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Lecture Note",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onUploadPaper,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Past Paper",
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // =====================================================
                // LECTURE NOTES
                // =====================================================

                item {
                    MaterialSectionHeader(
                        title = "Lecture Notes",
                        count = courseNotes.size
                    )
                }

                if (courseNotes.isEmpty()) {
                    item {
                        EmptyMaterialCard(
                            message = "No lecture notes uploaded yet."
                        )
                    }
                } else {
                    items(
                        items = courseNotes,
                        key = { it.id }
                    ) { note ->
                        LecturerLectureNoteCard(
                            note = note,
                            canManage = canManage,
                            onOpen = {
                                if (note.pdfUrl.isNullOrBlank()) {
                                    Toast.makeText(
                                        context,
                                        "No PDF file is attached to this lecture note.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewingNote = note
                                }
                            },
                            onMore = {
                                noteOptions = note
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // =====================================================
                // PAST YEAR PAPERS
                // =====================================================

                item {
                    MaterialSectionHeader(
                        title = "Past Year Papers",
                        count = coursePapers.size
                    )
                }

                if (coursePapers.isEmpty()) {
                    item {
                        EmptyMaterialCard(
                            message = "No past year papers uploaded yet."
                        )
                    }
                } else {
                    items(
                        items = coursePapers,
                        key = { it.id }
                    ) { paper ->
                        PastYearPaperMaterialCard(
                            paper = paper,
                            canManage = canManage,
                            onOpen = {
                                if (paper.pdfUrl.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "No PDF file is attached to this past year paper.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewingPaper = paper
                                }
                            },
                            onMore = {
                                paperOptions = paper
                            }
                        )
                    }
                }
            }
        }
    }

    // =====================================================
    // OPEN LECTURE NOTE PDF
    // =====================================================

    viewingNote?.let { note ->
        Dialog(
            onDismissRequest = {
                viewingNote = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                PdfAnnotationViewer(
                    documentTitle = note.chapterTitle,
                    courseCode = note.courseCode,
                    contentPages = listOf(
                        note.rawContent.ifBlank {
                            "Lecture Note: ${note.chapterTitle}"
                        }
                    ),
                    pdfUrl = note.pdfUrl.orEmpty(),
                    onDismiss = {
                        viewingNote = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // =====================================================
    // OPEN PAST YEAR PAPER PDF
    // =====================================================

    viewingPaper?.let { paper ->
        Dialog(
            onDismissRequest = {
                viewingPaper = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                PdfAnnotationViewer(
                    documentTitle = "${paper.courseCode} - ${paper.session}",
                    courseCode = paper.courseCode,
                    contentPages = listOf(
                        """
                        ${paper.courseCode}
                        ${paper.courseTitle}
                        ${paper.session}
                        ${paper.year}
                        Duration: ${paper.durationMinutes} minutes
                        Total Marks: ${paper.totalMarks}
                        """.trimIndent()
                    ),
                    pdfUrl = paper.pdfUrl,
                    onDismiss = {
                        viewingPaper = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // =====================================================
    // THREE-DOT OPTIONS
    // =====================================================

    noteOptions?.let { note ->
        AlertDialog(
            onDismissRequest = {
                noteOptions = null
            },
            title = {
                Text(
                    text = "Lecture Note Options",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.chapterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            noteOptions = null
                            noteToEdit = note
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Edit Lecture Note")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            noteOptions = null
                            noteToDelete = note
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Delete Lecture Note")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        noteOptions = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // =====================================================
// PAST YEAR PAPER THREE-DOT OPTIONS
// =====================================================

    paperOptions?.let { paper ->

        AlertDialog(
            onDismissRequest = {
                paperOptions = null
            },

            title = {
                Text(
                    text = "Past Year Paper Options",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = paper.session,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = paper.courseCode,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )


                    // EDIT
                    OutlinedButton(
                        onClick = {

                            paperOptions = null
                            paperToEdit = paper
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text("Edit Past Year Paper")
                    }


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    // DELETE
                    OutlinedButton(
                        onClick = {

                            paperOptions = null
                            paperToDelete = paper
                        },

                        modifier = Modifier.fillMaxWidth(),

                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor =
                                    MaterialTheme.colorScheme.error
                            )
                    ) {

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text("Delete Past Year Paper")
                    }
                }
            },

            confirmButton = {},

            dismissButton = {

                TextButton(
                    onClick = {
                        paperOptions = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }




    // =====================================================
    // EDIT LECTURE NOTE
    // =====================================================

    noteToEdit?.let { note ->
        EditLectureNoteDialog(
            note = note,
            onDismiss = {
                noteToEdit = null
            },
            onSaved = {
                refreshMaterials()
                noteToEdit = null
            }
        )
    }

    // =====================================================
    // DELETE CONFIRMATION
    // =====================================================

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = {
                noteToDelete = null
            },
            title = {
                Text("Delete Lecture Note?")
            },
            text = {
                Text(
                    text = "Are you sure you want to delete " +
                            "\"${note.chapterTitle}\"?\n\n" +
                            "Students will no longer be able to access this lecture note."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            NoteQuizRepository.deleteLectureNote(note.id)
                            refreshMaterials()
                            noteToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        noteToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // =====================================================
    // EDIT PAST YEAR PAPER
    // =====================================================

    paperToEdit?.let { paper ->
        EditPastYearPaperDialog(
            paper = paper,
            onDismiss = {
                paperToEdit = null
            },
            onSaved = {
                refreshMaterials()
                paperToEdit = null
            }
        )
    }

    // =====================================================
    // DELETE PAST YEAR PAPER
    // =====================================================

    paperToDelete?.let { paper ->
        AlertDialog(
            onDismissRequest = {
                paperToDelete = null
            },
            title = {
                Text("Delete Past Year Paper?")
            },
            text = {
                Text(
                    text = "Are you sure you want to delete " +
                            "\"${paper.session}\"?\n\n" +
                            "Students will no longer be able to access this past year paper."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            PastYearRepository.deletePaper(paper.id)
                            refreshMaterials()
                            paperToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        paperToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =====================================================
// MATERIALS UI HELPERS
// =====================================================

@Composable
private fun MaterialSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Text(
                text = "$count ${if (count == 1) "file" else "files"}",
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyMaterialCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================
// LECTURE NOTE CARD
// =====================================================

@Composable
private fun LecturerLectureNoteCard(
    note: LectureNote,
    canManage: Boolean,
    onOpen: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                    end = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onOpen()
                    }
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = note.chapterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = note.semesterPeriod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!note.pdfFileName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = note.pdfFileName.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (canManage) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Lecture note options"
                    )
                }
            }
        }
    }
}

// =====================================================
// EDIT LECTURE NOTE DIALOG
// =====================================================

@Composable
private fun EditLectureNoteDialog(
    note: LectureNote,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var semester by remember(note.id) {
        mutableStateOf(note.semesterPeriod)
    }

    var chapter by remember(note.id) {
        mutableStateOf(note.chapterTitle)
    }

    var replacementPdfUri by remember(note.id) {
        mutableStateOf<Uri?>(null)
    }

    var replacementPdfName by remember(note.id) {
        mutableStateOf("")
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            replacementPdfUri = uri

            var displayName = "replacement.pdf"

            context.contentResolver
                .query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (cursor.moveToFirst() && nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }

            replacementPdfName = displayName
            errorMessage = null
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!saving) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Edit Lecture Note",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = note.courseCode,
                    onValueChange = {},
                    enabled = false,
                    label = {
                        Text("Course")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = semester,
                    onValueChange = {
                        semester = it
                    },
                    label = {
                        Text("Semester")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = chapter,
                    onValueChange = {
                        chapter = it
                    },
                    label = {
                        Text("Chapter / Topic")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Current PDF",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = note.pdfFileName ?: "No PDF file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        pdfPickerLauncher.launch("application/pdf")
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = if (replacementPdfName.isBlank()) {
                            "Replace PDF File"
                        } else {
                            replacementPdfName
                        }
                    )
                }

                Text(
                    text = "Leave this unchanged to keep the current PDF.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving &&
                        semester.isNotBlank() &&
                        chapter.isNotBlank(),
                onClick = {
                    scope.launch {
                        saving = true
                        errorMessage = null

                        var finalPdfName = note.pdfFileName
                        var finalPdfUrl = note.pdfUrl

                        if (replacementPdfUri != null) {
                            val newUrl = NoteQuizRepository.uploadPdfToSupabase(
                                context = context,
                                uri = replacementPdfUri!!,
                                fileName = if (replacementPdfName.isNotBlank()) {
                                    replacementPdfName
                                } else {
                                    "replacement.pdf"
                                }
                            )

                            if (newUrl == null) {
                                errorMessage =
                                    "Unable to upload the replacement PDF. Please try again."
                                saving = false
                                return@launch
                            }

                            finalPdfName = replacementPdfName
                            finalPdfUrl = newUrl
                        }

                        val updatedNote = note.copy(
                            semesterPeriod = semester.trim(),
                            chapterTitle = chapter.trim(),
                            rawContent = note.rawContent,
                            pdfFileName = finalPdfName,
                            pdfUrl = finalPdfUrl
                        )

                        NoteQuizRepository.updateLectureNote(updatedNote)

                        saving = false
                        onSaved()
                    }
                }
            ) {
                Text(
                    if (saving) {
                        "Saving..."
                    } else {
                        "Save Changes"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

// =====================================================
// PAST YEAR PAPER CARD
// =====================================================

@Composable
private fun PastYearPaperMaterialCard(
    paper: PastYearPaper,
    canManage: Boolean,
    onOpen: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                    end = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onOpen()
                    }
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = paper.session,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = paper.subjectCategory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${paper.year} • ${paper.durationMinutes} min • ${paper.totalMarks} marks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (canManage) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Past year paper options"
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPastYearPaperDialog(
    paper: PastYearPaper,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()


    // =====================================================
    // FORM STATE
    // =====================================================

    var session by remember(paper.id) {
        mutableStateOf(
            paper.session
        )
    }

    var category by remember(paper.id) {
        mutableStateOf(
            paper.subjectCategory
        )
    }

    var year by remember(paper.id) {
        mutableStateOf(
            paper.year
        )
    }

    var duration by remember(paper.id) {
        mutableStateOf(
            paper.durationMinutes.toString()
        )
    }

    var totalMarks by remember(paper.id) {
        mutableStateOf(
            paper.totalMarks.toString()
        )
    }


    var replacementPdfUri by remember(paper.id) {
        mutableStateOf<Uri?>(null)
    }

    var replacementPdfName by remember(paper.id) {
        mutableStateOf("")
    }


    var saving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }


    // =====================================================
    // PDF PICKER
    // =====================================================

    val pdfPickerLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri: Uri? ->

            if (uri != null) {

                replacementPdfUri =
                    uri


                var displayName =
                    "replacement.pdf"


                context
                    .contentResolver
                    .query(
                        uri,
                        null,
                        null,
                        null,
                        null
                    )
                    ?.use { cursor ->

                        val nameIndex =
                            cursor.getColumnIndex(
                                OpenableColumns
                                    .DISPLAY_NAME
                            )


                        if (
                            cursor.moveToFirst() &&
                            nameIndex != -1
                        ) {

                            displayName =
                                cursor.getString(
                                    nameIndex
                                )
                        }
                    }


                replacementPdfName =
                    displayName

                errorMessage =
                    null
            }
        }


    // =====================================================
    // DIALOG
    // =====================================================

    AlertDialog(

        onDismissRequest = {

            if (!saving) {
                onDismiss()
            }
        },


        title = {

            Text(
                text =
                    "Edit Past Year Paper",

                fontWeight =
                    FontWeight.Bold
            )
        },


        text = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {


                // COURSE — LOCKED

                OutlinedTextField(
                    value =
                        "${paper.courseCode} - ${paper.courseTitle}",

                    onValueChange = {},

                    enabled =
                        false,

                    label = {
                        Text("Course")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // EXAM SESSION

                OutlinedTextField(
                    value =
                        session,

                    onValueChange = {
                        session = it
                    },

                    label = {
                        Text("Exam Session")
                    },

                    supportingText = {
                        Text(
                            "Example: 2025/2026 Semester 1 Final Exam"
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                // CATEGORY

                OutlinedTextField(
                    value =
                        category,

                    onValueChange = {
                        category = it
                    },

                    label = {
                        Text("Subject Category")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                // YEAR

                OutlinedTextField(
                    value =
                        year,

                    onValueChange = {
                        year = it
                    },

                    label = {
                        Text("Academic Year")
                    },

                    supportingText = {
                        Text(
                            "Example: 2025/2026"
                        )
                    },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                // DURATION

                OutlinedTextField(
                    value =
                        duration,

                    onValueChange = {
                        duration = it.filter {
                                character ->
                            character.isDigit()
                        }
                    },

                    label = {
                        Text(
                            "Duration (minutes)"
                        )
                    },

                    supportingText = {
                        Text(
                            "Example: 120"
                        )
                    },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                // TOTAL MARKS

                OutlinedTextField(
                    value =
                        totalMarks,

                    onValueChange = {
                        totalMarks = it.filter {
                                character ->
                            character.isDigit()
                        }
                    },

                    label = {
                        Text(
                            "Total Marks"
                        )
                    },

                    supportingText = {
                        Text(
                            "Example: 100"
                        )
                    },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                // =========================================
                // CURRENT / REPLACE PDF
                // =========================================

                Text(
                    text =
                        "Current PDF",

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Text(
                    text =
                        if (
                            paper.pdfUrl
                                .isNotBlank()
                        ) {

                            "PDF file attached"

                        } else {

                            "No PDF file"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                OutlinedButton(
                    onClick = {

                        pdfPickerLauncher
                            .launch(
                                "application/pdf"
                            )
                    },

                    enabled =
                        !saving,

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.UploadFile,

                        contentDescription =
                            null
                    )


                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )


                    Text(
                        text =
                            if (
                                replacementPdfName
                                    .isBlank()
                            ) {

                                "Replace PDF File"

                            } else {

                                replacementPdfName
                            }
                    )
                }


                Text(
                    text =
                        "Leave this unchanged to keep the current PDF.",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )


                if (
                    errorMessage != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )


                    Text(
                        text =
                            errorMessage!!,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        },


        // =================================================
        // SAVE
        // =================================================

        confirmButton = {

            val durationValue =
                duration.toIntOrNull()

            val marksValue =
                totalMarks.toIntOrNull()


            Button(

                enabled =
                    !saving &&
                            session.isNotBlank() &&
                            category.isNotBlank() &&
                            year.isNotBlank() &&
                            durationValue != null &&
                            durationValue > 0 &&
                            marksValue != null &&
                            marksValue > 0,


                onClick = {

                    scope.launch {

                        saving =
                            true

                        errorMessage =
                            null


                        var finalPdfUrl =
                            paper.pdfUrl


                        // =================================
                        // REPLACE PDF ONLY IF SELECTED
                        // =================================

                        if (
                            replacementPdfUri != null
                        ) {

                            val newUrl =
                                PastYearRepository
                                    .uploadPdfToSupabase(

                                        context =
                                            context,

                                        uri =
                                            replacementPdfUri!!,

                                        fileName =
                                            if (
                                                replacementPdfName
                                                    .isNotBlank()
                                            ) {

                                                replacementPdfName

                                            } else {

                                                "replacement.pdf"
                                            }
                                    )


                            if (
                                newUrl == null
                            ) {

                                errorMessage =
                                    "Unable to upload the replacement PDF. Please try again."

                                saving =
                                    false

                                return@launch
                            }


                            finalPdfUrl =
                                newUrl
                        }


                        // =================================
                        // BUILD UPDATED PAPER
                        // =================================

                        val updatedPaper =
                            paper.copy(

                                // course remains locked
                                courseCode =
                                    paper.courseCode,

                                courseTitle =
                                    paper.courseTitle,

                                session =
                                    session.trim(),

                                subjectCategory =
                                    category.trim(),

                                year =
                                    year.trim(),

                                durationMinutes =
                                    durationValue!!,

                                totalMarks =
                                    marksValue!!,

                                pdfUrl =
                                    finalPdfUrl
                            )


                        // =================================
                        // SAVE
                        // =================================

                        PastYearRepository
                            .updatePaper(
                                updatedPaper
                            )


                        saving =
                            false

                        onSaved()
                    }
                }
            ) {

                Text(
                    if (saving) {
                        "Saving..."
                    } else {
                        "Save Changes"
                    }
                )
            }
        },


        dismissButton = {

            TextButton(
                enabled =
                    !saving,

                onClick =
                    onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}