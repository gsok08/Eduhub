package com.example.eduhub20.ui.group

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.ChatMessage
import com.example.eduhub20.data.model.GroupMember
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.ui.common.UserAvatar
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    groupId: String,
    groupName: String,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = AuthRepository.currentUser.collectAsState().value
    val currentUserName = currentUser?.name ?: "Me"
    val currentUserRole = if (currentUser?.role?.name == "LECTURER") "Lecturer" else "Student"

    val group = remember(groupId) { StudyGroupRepository.getGroups().find { it.id == groupId } }
    val groupHostName = group?.host ?: ""
    val hostUserId = group?.hostUserId ?: ""

    val messages = remember(groupId) {
        mutableStateListOf(*StudyGroupRepository.getChatMessages(groupId).toTypedArray())
    }
    val members = remember(groupId) { mutableStateListOf<GroupMember>() }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val myMemberRole = remember(members.toList(), currentUser?.id) {
        val me = members.find { it.userId == currentUser?.id }
        when {
            me != null -> me.role
            currentUser != null && (currentUser.id == hostUserId || groupHostName.equals(currentUser.name, true)) -> "HOST"
            else -> "MEMBER"
        }
    }
    val isHost = myMemberRole.equals("HOST", ignoreCase = true)
    val isAdmin = myMemberRole.equals("ADMIN", ignoreCase = true)
    val canManage = isHost || isAdmin

    // Fetch members initially to get the accurate member count
    LaunchedEffect(groupId) {
        val list = StudyGroupRepository.fetchGroupMembers(groupId, groupHostName, hostUserId)
        members.clear()
        members.addAll(list)
    }

    // Real-time live polling from Supabase (every 2.5 seconds)
    LaunchedEffect(groupId) {
        while (isActive) {
            val allGroups = StudyGroupRepository.fetchGroupsFromSupabase()
            val groupStillExists = allGroups.any { it.id == groupId }
            if (!groupStillExists) {
                Toast.makeText(context, "This study group was deleted.", Toast.LENGTH_SHORT).show()
                onNavigateBack()
                break
            }

            val remoteMessages = StudyGroupRepository.fetchChatMessages(groupId, currentUserName)
            val currentIds = messages.map { it.id }
            val remoteIds = remoteMessages.map { it.id }
            if (currentIds != remoteIds) {
                val oldSize = messages.size
                messages.clear()
                messages.addAll(remoteMessages)
                if (messages.size > oldSize && messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }
            delay(2500)
        }
    }

    fun shareInvitationLink() {
        val inviteLink = "eduhub://group/join/$groupId"
        val shareText = "📚 Join our study group \"$groupName\" on EduHub!\n\n👉 Click to join: $inviteLink\nOr enter Group ID: $groupId"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Group Invitation")
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(groupName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${members.size.coerceAtLeast(1)} members · ${messages.size} msgs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareInvitationLink() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Link", tint = EduHubPrimary)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Group Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToGroupInfo(groupId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Invitation Link") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    shareInvitationLink()
                                }
                            )
                            if (canManage) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Clear Chat History", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showClearHistoryDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Say hi to the study group! 👋", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { shareInvitationLink() }) {
                    Icon(Icons.Default.Share, contentDescription = "Invite", tint = EduHubPrimary)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Message...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotBlank()) {
                            val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                            val msgId = java.util.UUID.randomUUID().toString()
                            val newMsg = ChatMessage(
                                id = msgId,
                                groupId = groupId,
                                senderName = currentUserName,
                                senderRole = currentUserRole,
                                message = text,
                                timestamp = now,
                                isFromMe = true,
                                senderAvatarUrl = currentUser?.avatarUrl,
                                senderId = currentUser?.id ?: ""
                            )
                            messages.add(newMsg)
                            inputText = ""

                            scope.launch {
                                StudyGroupRepository.sendMessage(groupId, text, currentUserName, currentUserRole, customMsgId = msgId)
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = EduHubPrimary)
                }
            }
        }
    }

    // ── Clear Chat History Confirmation Dialog ──────────────────────────────
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all messages in \"$groupName\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        scope.launch {
                            StudyGroupRepository.clearChatHistory(groupId)
                            messages.clear()
                            Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isMe = message.isFromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            UserAvatar(
                avatarUrl = message.senderAvatarUrl,
                size = 32.dp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe) {
                Text(
                    text = "${message.senderName} (${message.senderRole})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(if (isMe) EduHubPrimary else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }

        if (isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar(
                avatarUrl = message.senderAvatarUrl,
                size = 28.dp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}