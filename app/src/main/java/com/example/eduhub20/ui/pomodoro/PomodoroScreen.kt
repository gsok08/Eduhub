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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.eduhub20.R
import com.example.eduhub20.data.model.PomodoroPhase
import com.example.eduhub20.data.model.ShopCategory
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
    var selectedSoundId by remember { mutableStateOf("sound_none") }
    var coinsBalance by remember {
        mutableStateOf(if (currentUser != null) PomodoroRepository.getStudyCoins(currentUser.id) else 120)
    }
    var equippedThemeId by remember {
        mutableStateOf(if (currentUser != null) PomodoroRepository.getEquippedThemeId(currentUser.id) else "theme_classic")
    }
    var equippedBadge by remember {
        mutableStateOf(if (currentUser != null) PomodoroRepository.getEquippedBadge(currentUser.id) else null)
    }
    var purchasedItemIds by remember {
        mutableStateOf(if (currentUser != null) PomodoroRepository.getPurchasedItemIds(currentUser.id) else emptySet())
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

    val equippedTheme = remember(equippedThemeId) {
        PomodoroRepository.getThemes().find { it.id == equippedThemeId } ?: PomodoroRepository.getThemes().first()
    }

    val themePrimary = Color(equippedTheme.primaryColorHex)
    val themeAccent = Color(equippedTheme.accentColorHex)

    val phaseColor by animateColorAsState(
        targetValue = when (roomState.phase) {
            PomodoroPhase.FOCUS -> themePrimary
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
        targetValue = if (roomState.isRunning) 1.02f else 1f,
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
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP BAR ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                renameInputText = roomState.roomName
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Room",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            color = phaseColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Room Code",
                            tint = phaseColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // StudyCoins Shop Badge Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable { showStoreDialog = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coinsBalance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── PHASE SELECTOR CHIPS ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PomodoroPhase.values().forEach { phase ->
                    val isSelected = roomState.phase == phase
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) phaseColor else Color.Transparent)
                            .clickable { PomodoroRepository.switchPhase(roomId, phase) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = phase.badgeLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── CIRCULAR ANIMATED COUNTDOWN TIMER ────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background track
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = size.minDimension / 2 - 12.dp.toPx(),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress arc with theme gradient
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                phaseColor,
                                themeAccent,
                                phaseColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = roomState.phase.title.uppercase(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = phaseColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timerText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (roomState.isRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (roomState.isRunning) "In Session" else "Paused",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cycle ${roomState.completedIntervals % 4 + 1}/4 • +25 StudyCoins",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── COMPACT PRIMARY CONTROLS ─────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Reset Button (Compact 38dp)
                IconButton(
                    onClick = { PomodoroRepository.resetTimer(roomId) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play / Pause Button (Compact 40dp Pill)
                Button(
                    onClick = {
                        if (roomState.isRunning) {
                            PomodoroRepository.pauseTimer(roomId)
                        } else {
                            PomodoroRepository.startTimer(roomId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (roomState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (roomState.isRunning) "Pause" else "Start",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (roomState.isRunning) "PAUSE" else "START FOCUS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Skip Button (Compact 38dp)
                IconButton(
                    onClick = { PomodoroRepository.skipPhase(roomId) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── AMBIENT SOUNDSCAPE BAR ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = phaseColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Ambient Study Soundscape",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { showStoreDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Rewards Shop 🛍️", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PomodoroRepository.getAmbientSounds()) { sound ->
                            val isSelected = selectedSoundId == sound.id
                            val isUnlocked = PomodoroRepository.isItemPurchased(currentUser?.id ?: "", sound.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) phaseColor.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) phaseColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (!isUnlocked) {
                                            showStoreDialog = true
                                        } else {
                                            selectedSoundId = sound.id
                                            Toast.makeText(context, "Soundscape: " + sound.title, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sound.iconEmoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        sound.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) phaseColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isUnlocked) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🔒", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── LIVE STUDY SQUAD PRESENCE (Multi-User Roster) ────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Live Study Squad (${roomState.participants.size} Online)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { showStatusDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Set Status ✍️", fontSize = 11.sp, color = EduHubPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (roomState.participants.isEmpty()) {
                        Text(
                            "You are currently studying solo. Share the room code to invite classmates!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(roomState.participants) { participant ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
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
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = participant.userName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = participant.status,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── STUDYCOIN SHOP & REWARDS STORE DIALOG ────────────────────────────────
    if (showStoreDialog) {
        var selectedStoreTab by remember { mutableStateOf(0) }
        val tabs = listOf("🎨 Themes", "🎧 Sounds", "🏆 Badges", "⚡ Boosters", "💎 Pro Plan")
        val shopItems = remember { PomodoroRepository.getShopCatalog() }

        Dialog(
            onDismissRequest = { showStoreDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "StudyCoin Rewards Store",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showStoreDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🪙 Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$coinsBalance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD97706))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥 Streak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val streak = if (currentUser != null) PomodoroRepository.getDailyStreak(currentUser.id) else 3
                                Text("$streak Days", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFEA580C))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏱️ Focused", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val minutes = if (currentUser != null) PomodoroRepository.getTotalFocusMinutes(currentUser.id) else 75
                                Text("${minutes}m", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduHubPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = selectedStoreTab,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedStoreTab == index,
                                onClick = { selectedStoreTab = index },
                                text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedStoreTab == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedStoreTab) {
                            0 -> { // Themes
                                val items = shopItems.filter { it.category == ShopCategory.THEMES }
                                items.forEach { item ->
                                    val isPurchased = purchasedItemIds.contains(item.id)
                                    val isEquipped = equippedThemeId == item.id
                                    val itemTheme = item.themeData

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEquipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                if (itemTheme != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(itemTheme.primaryColorHex))
                                                            .border(2.dp, Color(itemTheme.accentColorHex), CircleShape)
                                                    )
                                                } else {
                                                    Text(item.iconEmoji, fontSize = 20.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (isEquipped) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF059669).copy(alpha = 0.15f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("✓ Equipped", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            } else if (isPurchased) {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            PomodoroRepository.setEquippedThemeId(currentUser.id, item.id)
                                                            equippedThemeId = item.id
                                                            Toast.makeText(context, "${item.title} equipped!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Equip", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            val success = PomodoroRepository.purchaseItem(currentUser.id, item.id, item.priceCoins)
                                                            if (success) {
                                                                coinsBalance = PomodoroRepository.getStudyCoins(currentUser.id)
                                                                purchasedItemIds = PomodoroRepository.getPurchasedItemIds(currentUser.id)
                                                                PomodoroRepository.setEquippedThemeId(currentUser.id, item.id)
                                                                equippedThemeId = item.id
                                                                Toast.makeText(context, "Unlocked & Equipped ${item.title}!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Not enough StudyCoins! Earn more by studying.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Buy ${item.priceCoins} 🪙", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> { // Sounds
                                val items = shopItems.filter { it.category == ShopCategory.SOUNDS }
                                items.forEach { item ->
                                    val isPurchased = purchasedItemIds.contains(item.id)
                                    val isSelected = selectedSoundId == item.id

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(item.iconEmoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF059669).copy(alpha = 0.15f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("✓ Active", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            } else if (isPurchased) {
                                                Button(
                                                    onClick = {
                                                        selectedSoundId = item.id
                                                        Toast.makeText(context, "${item.title} selected!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Play", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            val success = PomodoroRepository.purchaseItem(currentUser.id, item.id, item.priceCoins)
                                                            if (success) {
                                                                coinsBalance = PomodoroRepository.getStudyCoins(currentUser.id)
                                                                purchasedItemIds = PomodoroRepository.getPurchasedItemIds(currentUser.id)
                                                                selectedSoundId = item.id
                                                                Toast.makeText(context, "Unlocked ${item.title}!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Not enough StudyCoins!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Buy ${item.priceCoins} 🪙", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Badges
                                val items = shopItems.filter { it.category == ShopCategory.BADGES }

                                items.forEach { item ->
                                    val isPurchased = purchasedItemIds.contains(item.id)
                                    val isEquipped = equippedBadge == item.badgeTitle

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEquipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(item.iconEmoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (isEquipped) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF7C3AED).copy(alpha = 0.15f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("✓ Equipped", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            } else if (isPurchased) {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            PomodoroRepository.setEquippedBadge(currentUser.id, item.badgeTitle)
                                                            equippedBadge = item.badgeTitle
                                                            Toast.makeText(context, "Equipped ${item.title} to Profile!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Equip", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            val success = PomodoroRepository.purchaseItem(currentUser.id, item.id, item.priceCoins)
                                                            if (success) {
                                                                coinsBalance = PomodoroRepository.getStudyCoins(currentUser.id)
                                                                purchasedItemIds = PomodoroRepository.getPurchasedItemIds(currentUser.id)
                                                                PomodoroRepository.setEquippedBadge(currentUser.id, item.badgeTitle)
                                                                equippedBadge = item.badgeTitle
                                                                Toast.makeText(context, "Unlocked & Equipped ${item.title}!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Not enough StudyCoins!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Buy ${item.priceCoins} 🪙", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> { // Boosters
                                val items = shopItems.filter { it.category == ShopCategory.BOOSTERS }
                                items.forEach { item ->
                                    val isActive = currentUser != null && PomodoroRepository.hasActiveBooster(currentUser.id, item.id)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(item.iconEmoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (isActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF059669).copy(alpha = 0.15f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("✓ Active", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (currentUser != null) {
                                                            val success = PomodoroRepository.purchaseItem(currentUser.id, item.id, item.priceCoins)
                                                            if (success) {
                                                                PomodoroRepository.activateBooster(currentUser.id, item.id)
                                                                coinsBalance = PomodoroRepository.getStudyCoins(currentUser.id)
                                                                purchasedItemIds = PomodoroRepository.getPurchasedItemIds(currentUser.id)
                                                                Toast.makeText(context, "${item.title} activated!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Not enough StudyCoins!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Get ${item.priceCoins} 🪙", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> { // Pro Plan
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = EduHubPrimary.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("EduHub Pro Membership 💎", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduHubPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("• Unlimited access to all premium sounds & themes", fontSize = 12.sp)
                                        Text("• 2x StudyCoins booster automatically enabled", fontSize = 12.sp)
                                        Text("• Verified Campus Study Badge on squad rosters", fontSize = 12.sp)
                                        Text("• Exam Readiness Heatmap and priority AI hints", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("RM 7.00 / mo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduHubPrimary)
                                            }

                                            if (isProUser) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF059669).copy(alpha = 0.15f))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                                    Text("Upgrade with TNG", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showStoreDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // ── SHARE ROOM DIALOG ────────────────────────────────────────────────────
    if (showShareDialog) {
        val shareLink = "eduhub://pomodoro/join/$roomId"
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Invite Study Buddies", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Share this code with classmates so they can synchronize focus sessions and study together:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                    color = phaseColor
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
                                    tint = phaseColor,
                                    modifier = Modifier.size(20.dp)
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

    // ── STATUS DIALOG ────────────────────────────────────────────────────────
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

    // ── RENAME DIALOG ────────────────────────────────────────────────────────
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
