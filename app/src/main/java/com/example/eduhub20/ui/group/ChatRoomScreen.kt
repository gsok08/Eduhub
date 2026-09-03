package com.example.eduhub20.ui.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
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
    var showMembersDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<GroupMember?>(null) }

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

    // Fetch members initially
    fun refreshMembers() {
        scope.launch {
            val list = StudyGroupRepository.fetchGroupMembers(groupId, groupHostName, hostUserId)
            members.clear()
            members.addAll(list)
        }
    }

    LaunchedEffect(groupId) {
        refreshMembers()
    }

    // Real-time live polling from Supabase (every 2.5 seconds)
    LaunchedEffect(groupId) {
        while (isActive) {
            val remoteMessages = StudyGroupRepository.fetchChatMessages(groupId, currentUserName)
            if (remoteMessages.size != messages.size || remoteMessages != messages.toList()) {
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showMembersDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(groupName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Info, contentDescription = "Group Info", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                                text = { Text("Group Members & Roles") },
                                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    refreshMembers()
                                    showMembersDialog = true
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
                            val newMsg = ChatMessage(
                                id = java.util.UUID.randomUUID().toString(),
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
                                StudyGroupRepository.sendMessage(groupId, text, currentUserName, currentUserRole)
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

    // ── Kick Member Confirmation Dialog ─────────────────────────────────────
    memberToKick?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Kick Member", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove \"${target.userName}\" from \"$groupName\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toKick = target
                        memberToKick = null
                        scope.launch {
                            StudyGroupRepository.kickMember(groupId, toKick.userId)
                            members.removeAll { it.userId == toKick.userId }
                            Toast.makeText(context, "${toKick.userName} has been removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Kick Member")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Group Members & Roles Modal Dialog ───────────────────────────────────
    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Group Members (${members.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showMembersDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    // Invite link banner
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = EduHubPrimary.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Group Code", style = MaterialTheme.typography.labelSmall, color = EduHubPrimary)
                                Text(groupId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Group Invite Link", "eduhub://group/join/$groupId")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = EduHubPrimary)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(members) { member ->
                            val isMemberHost = member.role.equals("HOST", ignoreCase = true)
                            val isMemberAdmin = member.role.equals("ADMIN", ignoreCase = true)
                            val isSelf = member.userId == currentUser?.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(avatarUrl = member.userAvatarUrl, size = 42.dp)
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isSelf) "${member.userName} (You)" else member.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Role Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    isMemberHost -> EduHubAccentOrange.copy(alpha = 0.15f)
                                                    isMemberAdmin -> EduHubPrimary.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isMemberHost -> "👑 Host"
                                                isMemberAdmin -> "🛡️ Admin"
                                                else -> "👤 Member"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isMemberHost -> EduHubAccentOrange
                                                isMemberAdmin -> EduHubPrimary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                // Actions for Host & Admin
                                if (!isSelf) {
                                    Row {
                                        // Only Host can appoint or remove Admins
                                        if (isHost && !isMemberHost) {
                                            IconButton(
                                                onClick = {
                                                    val newRole = if (isMemberAdmin) "MEMBER" else "ADMIN"
                                                    scope.launch {
                                                        StudyGroupRepository.setMemberRole(groupId, member.userId, newRole)
                                                        val idx = members.indexOfFirst { it.userId == member.userId }
                                                        if (idx != -1) {
                                                            members[idx] = members[idx].copy(role = newRole)
                                                        }
                                                        Toast.makeText(context, "${member.userName} is now ${if (newRole == "ADMIN") "Admin" else "Member"}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isMemberAdmin) Icons.Default.ShieldMoon else Icons.Default.Shield,
                                                    contentDescription = if (isMemberAdmin) "Demote to Member" else "Promote to Admin",
                                                    tint = if (isMemberAdmin) EduHubAccentOrange else EduHubPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Host can kick anyone except themselves. Admin can kick regular members.
                                        val canKick = (isHost && !isMemberHost) || (isAdmin && !isMemberHost && !isMemberAdmin)
                                        if (canKick) {
                                            IconButton(
                                                onClick = { memberToKick = member }
                                            ) {
                                                Icon(
                                                    Icons.Default.PersonRemove,
                                                    contentDescription = "Kick Member",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { shareInvitationLink() },
                    colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invite Friends")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMembersDialog = false }) {
                    Text("Close")
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