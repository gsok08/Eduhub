package com.example.eduhub20.ui.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.eduhub20.data.model.GroupMember
import com.example.eduhub20.data.model.PomodoroRoomState
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.ui.common.UserAvatar
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onGroupLeftOrKicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = AuthRepository.currentUser.collectAsState().value

    val group = remember(groupId) {
        StudyGroupRepository.getGroups().find { it.id == groupId }
    }
    val groupName = group?.name ?: "Study Group"
    val groupHost = group?.host ?: "Host"
    val hostUserId = group?.hostUserId ?: ""
    val maxMembers = group?.maxMembers ?: 6

    val members = remember { mutableStateListOf<GroupMember>() }
    var isLoadingMembers by remember { mutableStateOf(true) }

    var showClearChatDialog by remember { mutableStateOf(false) }
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var showDisbandGroupDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<GroupMember?>(null) }

    fun refreshMembers() {
        scope.launch {
            isLoadingMembers = true
            val list = StudyGroupRepository.fetchGroupMembers(groupId, groupHost, hostUserId)
            members.clear()
            members.addAll(list)
            isLoadingMembers = false
        }
    }

    LaunchedEffect(groupId) {
        refreshMembers()
    }

    val myMemberRole = remember(members.toList(), currentUser?.id) {
        val me = members.find { it.userId == currentUser?.id }
        when {
            me != null -> me.role
            currentUser != null && (currentUser.id == hostUserId || groupHost.equals(currentUser.name, true)) -> "HOST"
            else -> "MEMBER"
        }
    }
    val isHost = myMemberRole.equals("HOST", ignoreCase = true)
    val isAdmin = myMemberRole.equals("ADMIN", ignoreCase = true)
    val canManage = isHost || isAdmin

    fun shareInvitationLink() {
        val shortCode = PomodoroRoomState.formatRoomCode(groupId)
        val inviteLink = "eduhub://group/join/$shortCode"
        val shareText = "📚 Join our study group \"$groupName\" on EduHub!\n\nGroup Code: $shortCode\n👉 Click to join: $inviteLink"

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
            TopAppBar(
                title = { Text("Group Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareInvitationLink() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = EduHubPrimary)
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Group Header Card ───────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EduHubPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = EduHubPrimary, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(groupName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Host: $groupHost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EduHubAccentOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("👑 Host", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EduHubAccentOrange)
                            }
                        }

                        if (!group?.details.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                group!!.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Group Code Row
                        val shortGroupCode = PomodoroRoomState.formatRoomCode(groupId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Group Code", style = MaterialTheme.typography.labelSmall, color = EduHubPrimary)
                                Text(shortGroupCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EduHubPrimary)
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Group Code", shortGroupCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Group Code $shortGroupCode copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = EduHubPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { shareInvitationLink() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Invitation Link")
                        }
                    }
                }
            }

            // ── Members Section ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members (${members.size}/$maxMembers)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoadingMembers) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }

            items(members) { member ->
                val isMemberHost = member.role.equals("HOST", ignoreCase = true)
                val isMemberAdmin = member.role.equals("ADMIN", ignoreCase = true)
                val isSelf = member.userId == currentUser?.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(avatarUrl = member.userAvatarUrl, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSelf) "${member.userName} (You)" else member.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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

                        // Administrative action buttons (Promote / Demote / Kick)
                        if (!isSelf) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Host can appoint or remove Admins
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
                                                Toast.makeText(
                                                    context,
                                                    "${member.userName} is now ${if (newRole == "ADMIN") "Admin" else "Member"}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isMemberAdmin) Icons.Default.ShieldMoon else Icons.Default.Shield,
                                            contentDescription = if (isMemberAdmin) "Demote to Member" else "Promote to Admin",
                                            tint = if (isMemberAdmin) EduHubAccentOrange else EduHubPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Host can kick non-hosts. Admin can kick regular members.
                                val canKick = (isHost && !isMemberHost) || (isAdmin && !isMemberHost && !isMemberAdmin)
                                if (canKick) {
                                    IconButton(onClick = { memberToKick = member }) {
                                        Icon(
                                            Icons.Default.PersonRemove,
                                            contentDescription = "Kick Member",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Danger / Action Zone ────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canManage) {
                        OutlinedButton(
                            onClick = { showClearChatDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Chat History", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Disband Group button for host, Leave Group button for non-hosts
                    if (isHost) {
                        Button(
                            onClick = { showDisbandGroupDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disband Study Group (Host)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showLeaveGroupDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave Study Group", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── Clear Chat History Confirmation Dialog ──────────────────────────────
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all messages in \"$groupName\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatDialog = false
                        scope.launch {
                            StudyGroupRepository.clearChatHistory(groupId)
                            Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Leave Group Confirmation Dialog ─────────────────────────────────────
    if (showLeaveGroupDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveGroupDialog = false },
            title = { Text("Leave Study Group", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave \"$groupName\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveGroupDialog = false
                        scope.launch {
                            StudyGroupRepository.leaveGroup(groupId)
                            Toast.makeText(context, "You left $groupName", Toast.LENGTH_SHORT).show()
                            onGroupLeftOrKicked()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Disband Group Confirmation Dialog (Host) ───────────────────────────
    if (showDisbandGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandGroupDialog = false },
            title = { Text("Disband Study Group", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You are the host of \"$groupName\". Quitting will permanently delete this group and remove all members.\n\nAre you sure you want to disband this study group?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisbandGroupDialog = false
                        scope.launch {
                            StudyGroupRepository.disbandGroup(groupId)
                            Toast.makeText(context, "Study group disbanded", Toast.LENGTH_SHORT).show()
                            onGroupLeftOrKicked()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disband Group")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisbandGroupDialog = false }) {
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
}
