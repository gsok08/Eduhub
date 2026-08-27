package com.example.eduhub20.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardYellow
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary

data class StickyNote(
    val id: String,
    val pageNumber: Int,
    val content: String,
    val colorHex: Long = 0xFFFEF08A
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAnnotationViewer(
    documentTitle: String,
    courseCode: String,
    contentPages: List<String>,
    pdfUrl: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var currentPage by remember { mutableIntStateOf(0) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isHighlightMode by remember { mutableStateOf(false) }
    var selectedHighlightColor by remember { mutableStateOf(Color(0xFFFEF08A)) } // Yellow
    val highlightedParagraphs = remember { mutableStateListOf<String>() }

    val stickyNotes = remember { mutableStateListOf<StickyNote>() }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    val pages = if (contentPages.isEmpty()) listOf(
        "=== $courseCode : $documentTitle ===\n\n1. Overview & Learning Objectives\nThis examination paper / lecture slide contains standard curriculum materials.\nReview the questions and core principles carefully.\n\n2. Key Architectural Foundations\n• Declarative UI with Jetpack Compose\n• MVVM State Hoisting & Reactive Data Flows\n• Supabase Cloud Data Synchronization & Auth Security\n\n3. Practical Exercise:\nExplain the differences between Stateful and Stateless composables."
    ) else contentPages

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(documentTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text("Page ${currentPage + 1} of ${pages.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isHighlightMode = !isHighlightMode
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Highlight Tool",
                            tint = if (isHighlightMode) EduHubAccentOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (pdfUrl.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddNoteDialog = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                containerColor = EduHubPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "Add Sticky Note", tint = Color.White)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Annotation Toolbar (Zoom & Highlight palette)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (zoomScale > 0.8f) zoomScale -= 0.1f }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                        }
                        Text("${(zoomScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (zoomScale < 1.6f) zoomScale += 0.1f }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                        }
                    }

                    if (isHighlightMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Color:", style = MaterialTheme.typography.labelSmall)
                            listOf(Color(0xFFFEF08A), Color(0xFFA7F3D0), Color(0xFFFED7AA)).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .clickable {
                                            selectedHighlightColor = col
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                )
                            }
                        }
                    } else {
                        Text("Tap 'Brush' to highlight text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Document Reader Canvas Page
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    val rawPageContent = pages[currentPage]
                    val paragraphs = rawPageContent.split("\n\n")

                    paragraphs.forEach { paragraph ->
                        val isHighlighted = highlightedParagraphs.contains(paragraph)
                        val bgColor = if (isHighlighted) selectedHighlightColor.copy(alpha = 0.6f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgColor)
                                .clickable(enabled = isHighlightMode) {
                                    if (isHighlighted) {
                                        highlightedParagraphs.remove(paragraph)
                                    } else {
                                        highlightedParagraphs.add(paragraph)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = paragraph,
                                fontSize = (14 * zoomScale).sp,
                                lineHeight = (22 * zoomScale).sp,
                                fontFamily = FontFamily.Default,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sticky notes attached to this page
                    val currentNotes = stickyNotes.filter { it.pageNumber == currentPage }
                    if (currentNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sticky Notes (Page ${currentPage + 1}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EduHubAccentOrange)
                        Spacer(modifier = Modifier.height(6.dp))

                        currentNotes.forEach { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(note.colorHex))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Note, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(note.content, fontSize = 13.sp, color = Color(0xFF713F12), fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(
                                        onClick = {
                                            stickyNotes.remove(note)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF854D0E), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Page Selector Footer
            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Previous Page", fontSize = 12.sp)
                    }

                    Text("Page ${currentPage + 1} / ${pages.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { if (currentPage < pages.size - 1) currentPage++ },
                        enabled = currentPage < pages.size - 1,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Next Page", fontSize = 12.sp)
                    }
                }
            }
        }

        // Add Sticky Note Dialog
        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAddNoteDialog = false },
                title = { Text("Add Sticky Note (Page ${currentPage + 1})") },
                text = {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        label = { Text("Note content (e.g. Important for Final Exam)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                stickyNotes.add(
                                    StickyNote(
                                        id = java.util.UUID.randomUUID().toString(),
                                        pageNumber = currentPage,
                                        content = newNoteText.trim()
                                    )
                                )
                                newNoteText = ""
                                showAddNoteDialog = false
                            }
                        }
                    ) {
                        Text("Save Note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
