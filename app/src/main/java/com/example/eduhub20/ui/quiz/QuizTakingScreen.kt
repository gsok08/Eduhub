package com.example.eduhub20.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.ai.EduHubAiGenerator
import com.example.eduhub20.data.model.Quiz
import com.example.eduhub20.data.repository.NoteQuizRepository
import com.example.eduhub20.ui.components.ConfettiEffect
import com.example.eduhub20.ui.theme.CardYellow
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(
    noteId: String,
    courseCode: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawNote = NoteQuizRepository.getNoteById(noteId)
    val haptic = LocalHapticFeedback.current

    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var currentIdx by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var showReview by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(noteId) {
        isLoading = true
        if (rawNote != null) {
            val aiNote = NoteQuizRepository.getOrGenerateAiNote(rawNote)
            quiz = EduHubAiGenerator.generateQuizFromNote(aiNote, courseCode)
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Quiz Mode", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            when {
                rawNote == null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("Note not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                isLoading || quiz == null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = EduHubPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("EduHub AI is preparing your interactive quiz...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    val currentQuiz = quiz!!
                    val questions = currentQuiz.questions
                    if (questions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            Text("Could not generate questions. Try again later.")
                        }
                        return@Scaffold
                    }

                    val q = questions[currentIdx]
                    val animatedProgress by animateFloatAsState(
                        targetValue = (currentIdx + 1).toFloat() / questions.size,
                        label = "QuizProgress"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Progress header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Question ${currentIdx + 1} / ${questions.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${(animatedProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = EduHubPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = EduHubPrimary
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Question
                        Text(q.questionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

                        if (!q.tableOrDiagram.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(q.tableOrDiagram, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Answer options with smooth animation & haptic feedback
                        q.options.forEachIndexed { idx, optText ->
                            val isSelected = selectedOption == idx
                            val isCorrect = idx == q.correctOptionIndex

                            val targetBorderColor = when {
                                showReview && isCorrect -> EduHubAccentGreen
                                showReview && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                isSelected -> EduHubPrimary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                            val targetBgColor = when {
                                showReview && isCorrect -> EduHubAccentGreen.copy(alpha = 0.10f)
                                showReview && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                isSelected -> EduHubPrimary.copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val animatedBorderColor by animateColorAsState(targetValue = targetBorderColor, label = "BorderColor")
                            val animatedBgColor by animateColorAsState(targetValue = targetBgColor, label = "BgColor")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .border(1.5.dp, animatedBorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!showReview) {
                                            selectedOption = idx
                                            showReview = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (idx == q.correctOptionIndex) correctCount++
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = animatedBgColor)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (!showReview) {
                                                selectedOption = idx
                                                showReview = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (idx == q.correctOptionIndex) correctCount++
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        optText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Review box
                        AnimatedVisibility(visible = showReview) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardYellow)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Let's review", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(q.reviewExplanation, style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentIdx < questions.size - 1) {
                                    currentIdx++
                                    selectedOption = -1
                                    showReview = false
                                } else {
                                    val pct = ((correctCount.toFloat() / questions.size) * 100).toInt()
                                    NoteQuizRepository.recordQuizCompletion(noteId, courseCode, currentQuiz.title, pct)
                                    if (pct >= 70) {
                                        showConfetti = true
                                    }
                                    showCompletionDialog = true
                                }
                            },
                            enabled = showReview,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                if (currentIdx < questions.size - 1) "Next Question" else "Finish Quiz",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Completion dialog
            if (showCompletionDialog && quiz != null) {
                val total = quiz!!.questions.size
                val pct = ((correctCount.toFloat() / total) * 100).toInt()
                AlertDialog(
                    onDismissRequest = {
                        showCompletionDialog = false
                        onNavigateBack()
                    },
                    icon = {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (pct >= 70) EduHubAccentGreen else EduHubPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    },
                    title = {
                        Text(if (pct >= 70) "Outstanding! Quiz Master 🎉" else "Quiz Completed!")
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your Score: $correctCount / $total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$pct%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = if (pct >= 70) EduHubAccentGreen else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (pct >= 70) "Great job! Your score of $pct% has been recorded to your history." else "Keep revising your notes to improve next time!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showCompletionDialog = false
                            onNavigateBack()
                        }) {
                            Text("Back to Notes")
                        }
                    }
                )
            }
        }

        // Particle Confetti Celebration overlay
        ConfettiEffect(
            visible = showConfetti,
            onFinished = { showConfetti = false }
        )
    }
}