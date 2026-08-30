package com.example.eduhub20.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.local.PdfAnnotationData
import com.example.eduhub20.data.local.SerializedDrawStroke
import com.example.eduhub20.data.local.SerializedStickyNote
import com.example.eduhub20.data.local.SerializedStrokePoint
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class AnnotationTool {
    NONE, PEN, HIGHLIGHTER
}

data class DrawStroke(
    val path: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val pageIndex: Int = 0
)

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val storageKey = if (pdfUrl.isNotBlank()) pdfUrl else "$courseCode-$documentTitle"

    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    var activeTool by remember { mutableStateOf(AnnotationTool.NONE) }
    var penColor by remember { mutableStateOf(Color(0xFF2563EB)) } // Blue Pen
    var highlighterColor by remember { mutableStateOf(Color(0xFFFEF08A)) } // Yellow Highlighter

    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val stickyNotes = remember { mutableStateListOf<StickyNote>() }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var saveStatusText by remember { mutableStateOf("Saved") }

    // Helper to persist annotations to local storage
    fun persistAnnotations(showFeedback: Boolean = false) {
        val serializedStrokes = strokes.map { s ->
            SerializedDrawStroke(
                points = s.path.map { SerializedStrokePoint(it.x, it.y) },
                colorArgb = s.color.toArgb().toLong(),
                strokeWidth = s.strokeWidth,
                isHighlighter = s.isHighlighter,
                pageIndex = s.pageIndex
            )
        }
        val serializedNotes = stickyNotes.map { n ->
            SerializedStickyNote(
                id = n.id,
                pageNumber = n.pageNumber,
                content = n.content,
                colorHex = n.colorHex
            )
        }
        EduHubLocalStorage.savePdfAnnotations(
            storageKey,
            PdfAnnotationData(strokes = serializedStrokes, stickyNotes = serializedNotes)
        )
        saveStatusText = "Saved"
        if (showFeedback) {
            scope.launch {
                snackbarHostState.showSnackbar("All annotations & edits saved successfully!")
            }
        }
    }

    // Real PDF rendering state
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPdfLoading by remember { mutableStateOf(true) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pdfFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Load saved annotations from storage on initial composition
    LaunchedEffect(storageKey) {
        val savedData = EduHubLocalStorage.loadPdfAnnotations(storageKey)
        if (savedData != null) {
            strokes.clear()
            strokes.addAll(savedData.strokes.map { s ->
                DrawStroke(
                    path = s.points.map { Offset(it.x, it.y) },
                    color = Color(s.colorArgb.toInt()),
                    strokeWidth = s.strokeWidth,
                    isHighlighter = s.isHighlighter,
                    pageIndex = s.pageIndex
                )
            })
            stickyNotes.clear()
            stickyNotes.addAll(savedData.stickyNotes.map { n ->
                StickyNote(
                    id = n.id,
                    pageNumber = n.pageNumber,
                    content = n.content,
                    colorHex = n.colorHex
                )
            })
        }
    }

    // Download / Open PDF and initialize PdfRenderer
    LaunchedEffect(pdfUrl) {
        if (pdfUrl.isNotBlank()) {
            isPdfLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val localFile = File(context.cacheDir, "pdf_${UUID.nameUUIDFromBytes(pdfUrl.toByteArray())}.pdf")
                    if (!localFile.exists() || localFile.length() == 0L) {
                        val url = URL(pdfUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 20000
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            conn.inputStream.use { input ->
                                FileOutputStream(localFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }

                    if (localFile.exists() && localFile.length() > 0) {
                        val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        pdfFileDescriptor = pfd
                        pdfRenderer = renderer
                        totalPages = renderer.pageCount.coerceAtLeast(1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isPdfLoading = false
                }
            }
        } else {
            isPdfLoading = false
            totalPages = contentPages.size.coerceAtLeast(1)
        }
    }

    // Render current page bitmap whenever currentPage or pdfRenderer changes
    LaunchedEffect(currentPage, pdfRenderer) {
        val renderer = pdfRenderer
        if (renderer != null && currentPage < renderer.pageCount) {
            withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(currentPage)
                    val density = context.resources.displayMetrics.density
                    val width = (page.width * density * 1.5f).toInt().coerceAtLeast(100)
                    val height = (page.height * density * 1.5f).toInt().coerceAtLeast(100)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderedBitmap = bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            persistAnnotations(showFeedback = false)
            try {
                pdfRenderer?.close()
                pdfFileDescriptor?.close()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(documentTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Page ${currentPage + 1} of $totalPages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Check, contentDescription = null, tint = EduHubAccentGreen, modifier = Modifier.size(12.dp))
                            Text(saveStatusText, style = MaterialTheme.typography.labelSmall, color = EduHubAccentGreen)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        persistAnnotations(showFeedback = false)
                        onDismiss()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    // Manual Save Button on top
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        persistAnnotations(showFeedback = true)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Annotations Manually",
                            tint = EduHubAccentGreen
                        )
                    }

                    // Pen Tool
                    IconButton(onClick = {
                        activeTool = if (activeTool == AnnotationTool.PEN) AnnotationTool.NONE else AnnotationTool.PEN
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Pen Tool",
                            tint = if (activeTool == AnnotationTool.PEN) EduHubPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Highlighter Tool
                    IconButton(onClick = {
                        activeTool = if (activeTool == AnnotationTool.HIGHLIGHTER) AnnotationTool.NONE else AnnotationTool.HIGHLIGHTER
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Highlight,
                            contentDescription = "Highlight Tool",
                            tint = if (activeTool == AnnotationTool.HIGHLIGHTER) EduHubAccentOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (strokes.any { it.pageIndex == currentPage }) {
                        IconButton(onClick = {
                            val lastIdx = strokes.indexOfLast { it.pageIndex == currentPage }
                            if (lastIdx != -1) {
                                strokes.removeAt(lastIdx)
                                persistAnnotations(showFeedback = false)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = {
                            strokes.removeAll { it.pageIndex == currentPage }
                            persistAnnotations(showFeedback = false)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Page Annotations")
                        }
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
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Annotation Toolbar (Zoom & Palette)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (zoomScale > 0.8f) zoomScale -= 0.1f }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                        }
                        Text("${(zoomScale * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (zoomScale < 1.6f) zoomScale += 0.1f }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                        }
                    }

                    if (activeTool == AnnotationTool.PEN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Pen:", style = MaterialTheme.typography.labelSmall)
                            listOf(Color(0xFF2563EB), Color(0xFFDC2626), Color(0xFF059669), Color(0xFF1E293B)).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .clickable { penColor = col }
                                )
                            }
                        }
                    } else if (activeTool == AnnotationTool.HIGHLIGHTER) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Highlighter:", style = MaterialTheme.typography.labelSmall)
                            listOf(Color(0xFFFEF08A), Color(0xFFA7F3D0), Color(0xFFFED7AA), Color(0xFFFBCFE8)).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .clickable { highlighterColor = col }
                                )
                            }
                        }
                    } else {
                        Text("Select Pen / Highlighter to annotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main PDF Viewer Canvas (Real PDF Slide Bitmap + Interactive Drawing Layer)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPdfLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = EduHubPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading original PDF slides...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (renderedBitmap != null) {
                        // Real PDF Slide Page
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeTool, currentPage) {
                                    if (activeTool != AnnotationTool.NONE) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentStrokePoints = listOf(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentStrokePoints = currentStrokePoints + change.position
                                            },
                                            onDragEnd = {
                                                if (currentStrokePoints.size > 1) {
                                                    val isHl = activeTool == AnnotationTool.HIGHLIGHTER
                                                    strokes.add(
                                                        DrawStroke(
                                                            path = currentStrokePoints,
                                                            color = if (isHl) highlighterColor.copy(alpha = 0.45f) else penColor,
                                                            strokeWidth = if (isHl) 24f else 5f,
                                                            isHighlighter = isHl,
                                                            pageIndex = currentPage
                                                        )
                                                    )
                                                    persistAnnotations(showFeedback = false)
                                                }
                                                currentStrokePoints = emptyList()
                                            },
                                            onDragCancel = {
                                                currentStrokePoints = emptyList()
                                            }
                                        )
                                    }
                                }
                        ) {
                            Image(
                                bitmap = renderedBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Slide Page ${currentPage + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Overlay Drawing Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw existing strokes on this page
                                strokes.filter { it.pageIndex == currentPage }.forEach { stroke ->
                                    if (stroke.path.size > 1) {
                                        val p = Path().apply {
                                            moveTo(stroke.path[0].x, stroke.path[0].y)
                                            for (i in 1 until stroke.path.size) {
                                                lineTo(stroke.path[i].x, stroke.path[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = p,
                                            color = stroke.color,
                                            style = Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }

                                // Draw live stroke being drawn
                                if (currentStrokePoints.size > 1) {
                                    val isHl = activeTool == AnnotationTool.HIGHLIGHTER
                                    val p = Path().apply {
                                        moveTo(currentStrokePoints[0].x, currentStrokePoints[0].y)
                                        for (i in 1 until currentStrokePoints.size) {
                                            lineTo(currentStrokePoints[i].x, currentStrokePoints[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = p,
                                        color = if (isHl) highlighterColor.copy(alpha = 0.45f) else penColor,
                                        style = Stroke(
                                            width = if (isHl) 24f else 5f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback text reader if no PDF file exists
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp)
                        ) {
                            val raw = if (contentPages.isNotEmpty() && currentPage < contentPages.size) contentPages[currentPage]
                            else "=== $courseCode : $documentTitle ===\n\nNo PDF uploaded for this lecture note. Raw lecturer summary:\n${contentPages.firstOrNull() ?: ""}"
                            Text(raw, fontSize = (14 * zoomScale).sp, lineHeight = (22 * zoomScale).sp)
                        }
                    }

                    // Sticky notes attached to this page
                    val currentNotes = stickyNotes.filter { it.pageNumber == currentPage }
                    if (currentNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            currentNotes.forEach { note ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(note.colorHex)),
                                    modifier = Modifier.padding(vertical = 4.dp).width(200.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(note.content, fontSize = 12.sp, color = Color(0xFF713F12), modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                stickyNotes.remove(note)
                                                persistAnnotations(showFeedback = false)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF854D0E), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Page Navigation Footer
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

                Text("Page ${currentPage + 1} / $totalPages", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                    enabled = currentPage < totalPages - 1,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Next Page", fontSize = 12.sp)
                }
            }
        }

        // Add Sticky Note Modal
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
                                        id = UUID.randomUUID().toString(),
                                        pageNumber = currentPage,
                                        content = newNoteText.trim()
                                    )
                                )
                                persistAnnotations(showFeedback = false)
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
