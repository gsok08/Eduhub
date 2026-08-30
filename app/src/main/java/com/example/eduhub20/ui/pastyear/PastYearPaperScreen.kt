package com.example.eduhub20.ui.pastyear

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.ui.components.PdfAnnotationViewer
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastYearPaperScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by AuthRepository.currentUser.collectAsState()
    val isLecturer = currentUser?.role == UserRole.LECTURER

    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("All") }
    var yearFilter by remember { mutableStateOf("All") }
    var viewingPaper by remember { mutableStateOf<PastYearPaper?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val paperList = remember { mutableStateListOf(*PastYearRepository.getPapers().toTypedArray()) }

    LaunchedEffect(Unit) {
        val remote = PastYearRepository.fetchPapersFromSupabase()
        paperList.clear()
        paperList.addAll(remote)
    }

    val subjectFilters = listOf("All", "Mobile App", "Computer Science", "Calculus", "Software Engineering", "English", "Data Science")
    val yearFilters = listOf("All", "2025/2026", "2024/2025", "2023/2024", "2022/2023")

    val filteredPapers = paperList.filter { p ->
        val q = searchQuery.isBlank() || p.courseCode.contains(searchQuery, true) ||
                p.courseTitle.contains(searchQuery, true) || p.session.contains(searchQuery, true)
        val s = subjectFilter == "All" || p.subjectCategory.contains(subjectFilter, true)
        val y = yearFilter == "All" || p.year.contains(yearFilter, true)
        q && s && y
    }

    if (viewingPaper != null) {
        val paper = viewingPaper!!
        PdfAnnotationViewer(
            documentTitle = "${paper.courseCode} - ${paper.session}",
            courseCode = paper.courseCode,
            contentPages = listOf(
                "=== ${paper.courseCode}: ${paper.courseTitle} ===\nSession: ${paper.session} | Duration: ${paper.durationMinutes} mins | Total Marks: ${paper.totalMarks}\n\nSECTION A: STRUCTURED QUESTIONS\n\nQuestion 1:\nExplain the core architectural differences between Android Jetpack Compose declarative UI and traditional XML imperative layouts.\n\nQuestion 2:\nDescribe the role of State Hoisting in Jetpack Compose and how it promotes separation of concerns.\n\nQuestion 3:\nIn mobile database architecture, contrast offline-first local caching with direct cloud REST querying."
            ),
            pdfUrl = paper.pdfUrl,
            onDismiss = { viewingPaper = null }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isLecturer) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = EduHubPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Past Year Paper", tint = Color.White)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Past Year Paper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Exam papers uploaded by lecturers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isLecturer) {
                    Button(
                        onClick = { showUploadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Paper", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search past paper by code, title, session...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subject filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjectFilters) { filter ->
                    FilterChip(
                        selected = subjectFilter == filter,
                        onClick = { subjectFilter = filter },
                        label = { Text("Subject: $filter", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Year filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(yearFilters) { yr ->
                    FilterChip(
                        selected = yearFilter == yr,
                        onClick = { yearFilter = yr },
                        label = { Text("Year: $yr", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Search results (${filteredPapers.size}):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (filteredPapers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No past year papers found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Papers uploaded by lecturers will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPapers) { paper ->
                        PaperCardItem(
                            paper = paper,
                            onClick = { viewingPaper = paper }
                        )
                    }
                }
            }
        }

        // Lecturer Upload Past Year Paper Modal Dialog
        if (showUploadDialog) {
            UploadPastYearPaperDialog(
                onDismiss = { showUploadDialog = false },
                onUploaded = { newPaper ->
                    paperList.add(0, newPaper)
                    showUploadDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Past year paper uploaded successfully!")
                    }
                }
            )
        }
    }
}

@Composable
fun UploadPastYearPaperDialog(
    onDismiss: () -> Unit,
    onUploaded: (PastYearPaper) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var courseCode by remember { mutableStateOf("AMIT3353") }
    var courseTitle by remember { mutableStateOf("Mobile Application Development") }
    var session by remember { mutableStateOf("2025/2026 Semester 1 Final Exam") }

    val categories = listOf("Mobile App", "Computer Science", "Calculus", "Software Engineering", "English", "Data Science")
    var selectedCategory by remember { mutableStateOf("Mobile App") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val years = listOf("2025/2026", "2024/2025", "2023/2024", "2022/2023", "2021/2022")
    var selectedYear by remember { mutableStateOf("2025/2026") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            var name = "Selected_Paper.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
            selectedPdfName = name
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Upload Past Year Paper", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g. AMIT3353)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = courseTitle,
                    onValueChange = { courseTitle = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    label = { Text("Session / Exam Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject Category") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { categoryDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Academic Year Selector Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedYear,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Academic Year") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { yearDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { yearDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = yearDropdownExpanded,
                        onDismissRequest = { yearDropdownExpanded = false }
                    ) {
                        years.forEach { yr ->
                            DropdownMenuItem(
                                text = { Text(yr) },
                                onClick = {
                                    selectedYear = yr
                                    yearDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // PDF Attachment Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PDF Exam Paper", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (selectedPdfName.isNotBlank()) selectedPdfName else "No PDF selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedPdfName.isNotBlank()) EduHubPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select PDF", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseCode.isBlank() || session.isBlank()) {
                        Toast.makeText(context, "Please fill in course code and exam session.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isUploading = true
                    scope.launch {
                        var uploadedPdfUrl = ""
                        if (selectedPdfUri != null) {
                            uploadedPdfUrl = PastYearRepository.uploadPdfToSupabase(context, selectedPdfUri!!, selectedPdfName) ?: ""
                        }

                        val newPaper = PastYearPaper(
                            id = UUID.randomUUID().toString(),
                            courseCode = courseCode.trim().uppercase(),
                            courseTitle = courseTitle.trim(),
                            session = session.trim(),
                            subjectCategory = selectedCategory,
                            year = selectedYear,
                            durationMinutes = 120,
                            totalMarks = 100,
                            pdfUrl = uploadedPdfUrl
                        )

                        PastYearRepository.addPaper(newPaper)
                        isUploading = false
                        onUploaded(newPaper)
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uploading...")
                } else {
                    Text("Publish Paper")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaperCardItem(paper: PastYearPaper, onClick: () -> Unit) {
    val cardColor = if (paper.subjectCategory.contains("Mobile", true) || paper.subjectCategory.contains("CS", true)) CardCoral else CardBlue

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(paper.session, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(4.dp))
                Text("${paper.courseCode} · ${paper.courseTitle}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Category: ${paper.subjectCategory}")
                    Chip(paper.year)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
    }
}