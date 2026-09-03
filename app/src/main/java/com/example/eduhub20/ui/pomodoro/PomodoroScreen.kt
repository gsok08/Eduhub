package com.example.eduhub20.ui.pomodoro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eduhub20.R
import com.example.eduhub20.data.model.PomodoroPhase
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.PomodoroRepository
import com.example.eduhub20.ui.theme.EduHubPrimary

@Composable
fun PomodoroScreen(
    roomId: String,
    roomName: String = "Focus Room",
    onNavigateBack: () -> Unit,
    onNavigateToPayment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        PomodoroRepository.init(context)
    }

    val currentUser = AuthRepository.currentUser.collectAsState().value
    val isProUser = if (currentUser != null) PomodoroRepository.isProUser(currentUser.id) else false

    val roomStateFlow = remember(roomId) {
        PomodoroRepository.getRoomState(roomId, roomName, currentUser)
    }
    val roomState by roomStateFlow.collectAsState()

    var showStoreDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }
    var selectedSoundId by remember { mutableStateOf("none") }
    var coinsBalance by remember {
        mutableStateOf(if (currentUser != null) PomodoroRepository.getStudyCoins(currentUser.id) else 120)
    }

    DisposableEffect(roomId, currentUser?.id) {
        PomodoroRepository.joinRoom(roomId, roomName, currentUser)
        onDispose {
            if (currentUser != null) {
                PomodoroRepository.leaveRoom(roomId, currentUser.id)
            }
        }
    }

    LaunchedEffect(roomState.completedIntervals) {
        if (currentUser != null) {
            coinsBalance = PomodoroRepository.getStudyCoins(currentUser.id)
        }
    }

    val phaseColor by animateColorAsState(
        targetValue = when (roomState.phase) {
            PomodoroPhase.FOCUS -> Color(0xFF2563EB)
            PomodoroPhase.SHORT_BREAK -> Color(0xFF059669)
            PomodoroPhase.LONG_BREAK -> Color(0xFFD97706)
        },
        label = "PhaseColor"
    )

    val progress = if (roomState.totalSeconds > 0) {
        roomState.remainingSeconds.toFloat() / roomState.totalSeconds.toFloat()
    } else 1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = LinearEasing),
        label = "Progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (roomState.isRunning) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val minutes = roomState.remainingSeconds / 60
    val seconds = roomState.remainingSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0B1329)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = roomState.roomName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                renameInputText = roomState.roomName
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Room",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Pomodoro Room Code", roomState.roomCode))
                                Toast.makeText(context, "Room Code ${roomState.roomCode} copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Code: ${roomState.roomCode}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Room Code",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { showStoreDialog = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coinsBalance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PHASE SELECTOR CHIPS
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PomodoroPhase.values().forEach { phase ->
                    val isSelected = roomState.phase == phase
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) phaseColor else Color.Transparent)
                            .clickable { PomodoroRepository.switchPhase(roomId, phase) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = phase.badgeLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CIRCULAR ANIMATED COUNTDOWN TIMER
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .scale(pulseScale)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.minDimension / 2 - 12.dp.toPx(),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                phaseColor,
                                phaseColor.copy(alpha = 0.8f),
                                phaseColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = roomState.phase.title.uppercase(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = phaseColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timerText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 54.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (roomState.isRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (roomState.isRunning) "In Session" else "Paused",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cycle ${roomState.completedIntervals % 4 + 1}/4 • +25 StudyCoins",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // PRIMARY CONTROLS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { PomodoroRepository.resetTimer(roomId) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Button(
                    onClick = {
                        if (roomState.isRunning) {
                            PomodoroRepository.pauseTimer(roomId)
                        } else {
                            PomodoroRepository.startTimer(roomId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (roomState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (roomState.isRunning) "Pause" else "Start",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (roomState.isRunning) "PAUSE" else "FOCUS TOGETHER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(
                    onClick = { PomodoroRepository.skipPhase(roomId) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AMBIENT SOUNDSCAPE BAR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Ambient Study Soundscape",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = { showStoreDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (isProUser) "EduHub Pro 💎" else "Unlock Pro 👑", fontSize = 11.sp, color = Color(0xFFF59E0B))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PomodoroRepository.getAmbientSounds()) { sound ->
                            val isSelected = selectedSoundId == sound.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB).copy(alpha = 0.4f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF60A5FA) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (sound.isProOnly && !isProUser) {
                                            showStoreDialog = true
                                        } else {
                                            selectedSoundId = sound.id
                                            Toast.makeText(context, "Soundscape: " + sound.title, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sound.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        sound.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White
                                    )
                                    if (sound.isProOnly && !isProUser) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("👑", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE STUDY SQUAD PRESENCE (Multi-User Roster)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Live Study Squad (" + roomState.participants.size.toString() + " Online)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = { showStatusDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Set Status ✍️", fontSize = 11.sp, color = Color(0xFF60A5FA))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (roomState.participants.isEmpty()) {
                        Text(
                            "You are currently studying solo. Share the room link to invite classmates!",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(roomState.participants) { participant ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, phaseColor, CircleShape)
                                        ) {
                                            if (!participant.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = participant.avatarUrl,
                                                    contentDescription = participant.userName,
                                                    placeholder = painterResource(id = R.drawable.default_avatar),
                                                    error = painterResource(id = R.drawable.default_avatar),
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.default_avatar),
                                                    contentDescription = participant.userName,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                                .border(1.5.dp, Color(0xFF0B1329), CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = participant.userName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = participant.status,
                                            fontSize = 9.sp,
                                            color = Color(0xFF93C5FD),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EduHub Pro & Rewards Store", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Earn StudyCoins by completing uninterrupted 25-minute Pomodoro focus cycles. Spend them to unlock ambient sounds and pro themes, or subscribe to EduHub Pro!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🪙 Coins", fontSize = 11.sp)
                                Text("$coinsBalance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD97706))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥 Streak", fontSize = 11.sp)
                                val streak = if (currentUser != null) PomodoroRepository.getDailyStreak(currentUser.id) else 3
                                Text(streak.toString() + " Days", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFEA580C))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏱️ Focused", fontSize = 11.sp)
                                val minutes = if (currentUser != null) PomodoroRepository.getTotalFocusMinutes(currentUser.id) else 75
                                Text(minutes.toString() + "m", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("💎 EduHub Pro Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A).copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("• Unlock all 10+ Ambient Soundscapes (Rain, Cafe, Binaural)", fontSize = 12.sp)
                            Text("• Zen Forest, Cyberpunk & OLED Dark Timer Themes", fontSize = 12.sp)
                            Text("• Detailed Productivity Analytics & Exam Readiness Heatmap", fontSize = 12.sp)
                            Text("• Verified Campus Study Badge on Group & Profile", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RM 7.00 / Month", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduHubPrimary)
                                if (isProUser) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF059669).copy(alpha = 0.15f))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("PRO ACTIVE 💎", fontWeight = FontWeight.Bold, color = Color(0xFF059669), fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            showStoreDialog = false
                                            onNavigateToPayment()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Upgrade with TNG", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoreDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showShareDialog) {
        val shareLink = "eduhub://pomodoro/join/$roomId"
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Invite Study Buddies", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Share this link with classmates so they can synchronize focus sessions and study together:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Short Room Code Card with copy button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Room Code (Easy to Share)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = roomState.roomCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pomodoro Code", roomState.roomCode))
                                    Toast.makeText(context, "Room Code ${roomState.roomCode} copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Room Code",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Link",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = shareLink,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pomodoro Link", shareLink))
                                    Toast.makeText(context, "Room link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Link",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my Pomodoro study room on EduHub to study together! Room Code: ${roomState.roomCode} (Tap: $shareLink)"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Study Room via"))
                        showShareDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatusDialog) {
        val statusOptions = listOf(
            "Focusing 🎯",
            "Resting ☕",
            "Taking Notes 📝",
            "Solving Past Papers 📄",
            "Ask Me Anything 🙋",
            "Muted 🎧"
        )
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Your Study Status", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    statusOptions.forEach { st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentUser != null) {
                                        PomodoroRepository.setParticipantStatus(roomId, currentUser.id, st)
                                    }
                                    showStatusDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(st, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Focus Room", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Give this synchronized study room a custom name:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Exam Cramming, Math Sprint") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            PomodoroRepository.renameRoom(roomId, renameInputText)
                            Toast.makeText(context, "Renamed to '$renameInputText'", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
