package com.example.eduhub20.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.ChatMessage
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    groupId: String,
    groupName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Load messages ONLY for this groupId
    val messages = remember(groupId) {
        mutableStateListOf(*StudyGroupRepository.getChatMessages(groupId).toTypedArray())
    }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(groupName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${messages.size} messages", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
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
                    Text("No messages yet. Say hi! 👋", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                IconButton(onClick = {}) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = EduHubPrimary)
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
                        if (inputText.isNotBlank()) {
                            StudyGroupRepository.sendMessage(groupId, inputText.trim())
                            messages.add(
                                ChatMessage(
                                    id         = java.util.UUID.randomUUID().toString(),
                                    groupId    = groupId,
                                    senderName = "Me",
                                    senderRole = "Student",
                                    message    = inputText.trim(),
                                    timestamp  = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                                    isFromMe   = true
                                )
                            )
                            inputText = ""
                            scope.launch {
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
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = EduHubPrimary,
                modifier = Modifier.size(32.dp).padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
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
                            bottomEnd   = if (isMe) 4.dp else 16.dp
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
    }
}