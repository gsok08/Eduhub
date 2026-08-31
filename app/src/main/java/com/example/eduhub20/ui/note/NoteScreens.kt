package com.example.eduhub20.ui.note

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.eduhub20.data.ai.EduHubAiGenerator
import com.example.eduhub20.data.ai.GeminiConfig
import com.example.eduhub20.data.model.AiGeneratedNote
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.QuizHistoryItem
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.ui.components.PdfAnnotationViewer
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.launch

@Composable
fun NoteQuizScreen(
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToQuiz: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val notesList = remember { mutableStateListOf(*NoteQuizRepository.getNotes().toTypedArray()) }

    // Fetch notes from Supabase
    LaunchedEffect(Unit) {
        val remote = NoteQuizRepository.fetchNotesFromSupabase()
        notesList.clear()
        notesList.addAll(remote)
    }

    val filteredNotes = notesList.filter {
        searchQuery.isBlank() ||
                it.chapterTitle.contains(searchQuery, true) ||
                it.semesterPeriod.contains(searchQuery, true) ||
                it.courseCode.contains(searchQuery, true)
    }
    val quizHistory = NoteQuizRepository.getQuizHistory()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search notes & quizzes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Note (${filteredNotes.size})", fontWeight = FontWeight.SemiBold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Quiz", fontWeight = FontWeight.SemiBold) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No lecture notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Notes uploaded by your lecturer will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredNotes) { note ->
                        NoteCardItem(note = note, onClick = { onNavigateToNoteDetail(note.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        } else {
            val completed = quizHistory.filter { it.isCompleted }
            val incomplete = quizHistory.filter { !it.isCompleted }

            if (quizHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No quiz history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Complete a quiz from your notes to see results here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (completed.isNotEmpty()) {
                        item {
                            Text("Completed", style = MaterialTheme.typography.bodyMedium, color = EduHubAccentGreen, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(completed) { item ->
                            QuizHistoryCardItem(item = item, onClick = { onNavigateToQuiz(item.noteId, item.courseCode) })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    if (incomplete.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Incomplete", style = MaterialTheme.typography.bodyMedium, color = EduHubAccentOrange, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(incomplete) { item ->
                            QuizHistoryCardItem(item = item, onClick = { onNavigateToQuiz(item.noteId, item.courseCode) })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(note: LectureNote, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.semesterPeriod, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.chapterTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!note.pdfFileName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("PDF: ${note.pdfFileName}", style = MaterialTheme.typography.labelSmall, color = EduHubPrimary)
                }
            }
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(EduHubPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = EduHubPrimary)
            }
        }
    }
}

@Composable
fun QuizHistoryCardItem(item: QuizHistoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.courseCode, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(item.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(
                    text = if (item.isCompleted) "Score: ${item.scorePercentage}%" else "Progress: ${item.scorePercentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isCompleted) EduHubAccentGreen else EduHubAccentOrange
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${item.scorePercentage}%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.Quiz,
                    contentDescription = null,
                    tint = if (item.isCompleted) EduHubAccentGreen else EduHubPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailAiScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawNote = NoteQuizRepository.getNoteById(noteId) ?: return
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var aiNote by remember { mutableStateOf<AiGeneratedNote?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    var showPdfViewer by remember { mutableStateOf(false) }

    // Edit Study Note state
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editSummary by remember { mutableStateOf("") }
    var editTakeawaysText by remember { mutableStateOf("") }
    var editTerminologyText by remember { mutableStateOf("") }

    // API Key & Flask Backend Settings state
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(GeminiConfig.GEMINI_API_KEY) }
    var backendUrlInput by remember { mutableStateOf(GeminiConfig.BACKEND_URL) }
    var testConnectionStatus by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        GeminiConfig.init(context)
        apiKeyInput = GeminiConfig.GEMINI_API_KEY
        backendUrlInput = GeminiConfig.BACKEND_URL
    }

    fun loadOrGenerate(force: Boolean = false) {
        scope.launch {
            isGenerating = true
            aiNote = NoteQuizRepository.getOrGenerateAiNote(rawNote, forceRegenerate = force)
            isGenerating = false
        }
    }

    LaunchedEffect(noteId) {
        loadOrGenerate(force = false)
    }

    if (showPdfViewer) {
        PdfAnnotationViewer(
            documentTitle = "${rawNote.courseCode} - ${rawNote.chapterTitle}",
            courseCode = rawNote.courseCode,
            contentPages = listOf(rawNote.rawContent),
            pdfUrl = rawNote.pdfUrl ?: "",
            onDismiss = { showPdfViewer = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Study Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        apiKeyInput = GeminiConfig.GEMINI_API_KEY
                        showApiKeyDialog = true
                    }) {
                        Icon(Icons.Default.Key, contentDescription = "Gemini API Key", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (aiNote != null && !isGenerating) {
                        IconButton(onClick = {
                            val current = aiNote!!
                            editTitle = current.title
                            editSummary = current.summary
                            editTakeawaysText = current.keyTakeaways.joinToString("\n")
                            editTerminologyText = current.keyTerminology.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            showEditDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = EduHubPrimary)
                        }
                        IconButton(onClick = { loadOrGenerate(force = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate AI", tint = EduHubAccentOrange)
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isGenerating || aiNote == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EduHubPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("EduHub AI is analyzing lecture materials...", fontWeight = FontWeight.Medium)
                    Text("Reading PDF slides & extracting core takeaways", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val note = aiNote!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBlue)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Study Guide (Saved)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EduHubPrimary)
                            }
                            OutlinedButton(
                                onClick = { showPdfViewer = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = EduHubPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Slides", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                        Text(rawNote.courseTitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Section
                if (note.summary.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EduHubAccentOrange)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(note.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Key Takeaways Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Key Takeaways", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EduHubPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        note.keyTakeaways.forEach { point ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold, color = EduHubPrimary)
                                Text(point, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Key Terminology Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Key Terminology", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EduHubAccentGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        note.keyTerminology.forEach { (term, def) ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold, color = EduHubAccentGreen)
                                Column {
                                    Text("$term:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(def, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onNavigateToQuiz(rawNote.id, rawNote.courseCode) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Quiz from Notes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Gemini AI & Python Backend Setup Dialog ───────────────────────
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { Text("AI & Backend Settings", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Connect to your laptop's Python Flask backend (app.py) or use direct Gemini API:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Flask Backend URL Field
                        OutlinedTextField(
                            value = backendUrlInput,
                            onValueChange = {
                                backendUrlInput = it
                                testConnectionStatus = null
                            },
                            label = { Text("Python Flask Backend URL") },
                            placeholder = { Text("http://192.168.1.100:5000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Test Connection Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Emulator: 10.0.2.2:5000\nPhone: Laptop Wi-Fi IP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    isTestingConnection = true
                                    testConnectionStatus = null
                                    scope.launch {
                                        val (success, msg) = EduHubAiGenerator.testBackendConnection(backendUrlInput)
                                        testConnectionStatus = if (success) "✅ $msg" else "❌ $msg"
                                        isTestingConnection = false
                                    }
                                },
                                enabled = !isTestingConnection,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Test Connection", fontSize = 11.sp)
                            }
                        }

                        if (testConnectionStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = testConnectionStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testConnectionStatus!!.startsWith("✅")) EduHubAccentGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct Gemini API Key Field
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API Key (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        GeminiConfig.saveBackendUrl(context, backendUrlInput)
                        GeminiConfig.saveApiKey(context, apiKeyInput)
                        showApiKeyDialog = false
                        loadOrGenerate(force = true)
                    }) {
                        Text("Save & Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ── Edit AI Study Note Dialog ─────────────────────────────────────
        if (showEditDialog && aiNote != null) {
            val current = aiNote!!
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Study Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editSummary,
                            onValueChange = { editSummary = it },
                            label = { Text("Summary") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editTakeawaysText,
                            onValueChange = { editTakeawaysText = it },
                            label = { Text("Key Takeaways (one per line)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editTerminologyText,
                            onValueChange = { editTerminologyText = it },
                            label = { Text("Key Terminology (Format: Term: Definition)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedTakeaways = editTakeawaysText.split("\n")
                                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                                .filter { it.isNotBlank() }

                            val parsedTerminology = mutableMapOf<String, String>()
                            editTerminologyText.split("\n").forEach { line ->
                                if (line.contains(":")) {
                                    val parts = line.split(":", limit = 2)
                                    val k = parts[0].trim().removePrefix("•").removePrefix("-").trim()
                                    val v = parts[1].trim()
                                    if (k.isNotBlank()) parsedTerminology[k] = v
                                }
                            }

                            val updated = current.copy(
                                title = editTitle.trim(),
                                summary = editSummary.trim(),
                                keyTakeaways = if (parsedTakeaways.isNotEmpty()) parsedTakeaways else current.keyTakeaways,
                                keyTerminology = if (parsedTerminology.isNotEmpty()) parsedTerminology else current.keyTerminology
                            )

                            aiNote = updated
                            scope.launch {
                                NoteQuizRepository.saveOrUpdateAiNote(updated)
                                snackbarHostState.showSnackbar("Study note saved! Available offline.")
                            }
                            showEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                    ) {
                        Text("Save Note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}