package com.example.eduhub20.ui.pastyear

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.ui.components.PdfAnnotationViewer
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastYearPaperScreen(modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("All") }
    var yearFilter by remember { mutableStateOf("All") }
    var viewingPaper by remember { mutableStateOf<PastYearPaper?>(null) }

    val subjectFilters = listOf("All", "Mobile App", "Calculus", "English", "Computer Science")
    val yearFilters = listOf("All", "2025/2026", "2024/2025", "2023/2024")

    val papers = PastYearRepository.searchPapers(searchQuery, subjectFilter, yearFilter)

    if (viewingPaper != null) {
        val paper = viewingPaper!!
        PdfAnnotationViewer(
            documentTitle = "${paper.courseCode} - ${paper.session}",
            courseCode = paper.courseCode,
            contentPages = listOf(
                "=== ${paper.courseCode} ${paper.courseTitle} ===\nSession: ${paper.session} | Duration: ${paper.durationMinutes} mins | Total Marks: ${paper.totalMarks}\n\nSECTION A: MULTIPLE CHOICE & STRUCTURED QUESTIONS\n\nQuestion 1:\nExplain the core architectural differences between Android Jetpack Compose declarative UI and traditional XML imperative layouts.\n\nQuestion 2:\nDescribe the role of State Hoisting in Jetpack Compose and how it promotes separation of concerns.\n\nQuestion 3:\nIn mobile database architecture, contrast offline-first local caching with direct cloud REST querying.",
                "SECTION B: SCENARIO & DESIGN QUESTIONS\n\nQuestion 4:\nA mobile study application requires real-time chat between students and automatic synchronization when network state changes.\n\n(a) Design the data flow using Coroutines StateFlow and Supabase WebSocket channels.\n(b) Illustrate the error handling when network is lost.\n\nQuestion 5:\nExplain how Android Foreground Services ensure long-running timers (e.g. Virtual Study Rooms) persist when the device is locked."
            ),
            pdfUrl = paper.pdfUrl,
            onDismiss = { viewingPaper = null }
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Past Year Paper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Exam papers uploaded by lecturers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search past paper...") },
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

        Text("Search results:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (papers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No past year papers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Papers uploaded by your lecturer will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(papers) { paper ->
                    PaperCardItem(
                        paper = paper,
                        onClick = { viewingPaper = paper }
                    )
                }
            }
        }
    }
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