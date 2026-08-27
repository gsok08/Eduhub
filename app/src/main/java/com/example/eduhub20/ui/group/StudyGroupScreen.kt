package com.example.eduhub20.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.StudyGroup
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.delay

@Composable
fun StudyGroupScreen(
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val groups = remember { mutableStateListOf(*StudyGroupRepository.getGroups().toTypedArray()) }

    // Live study-room timer
    var secondsElapsed by remember { mutableIntStateOf(794) }
    LaunchedEffect(Unit) { while (true) { delay(1000); secondsElapsed++ } }
    val timerText = "%02d:%02d".format(secondsElapsed / 60, secondsElapsed % 60)

    var newGroupName    by remember { mutableStateOf("") }
    var newGroupDetails by remember { mutableStateOf("") }

    val recommended = groups.filter { !it.isJoined }
    val myGroups    = groups.filter { it.isJoined }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Study Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Join", fontWeight = FontWeight.SemiBold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Create", fontWeight = FontWeight.SemiBold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("My Group (${myGroups.size})", fontWeight = FontWeight.SemiBold) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            // ── Join Tab ───────────────────────────────────────────
            0 -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text("Recommended Groups", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (recommended.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "No groups available yet. Tap \"Create\" tab to start a study group!",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(recommended) { grp ->
                            GroupCardItem(
                                group = grp,
                                onJoinClick = {
                                    StudyGroupRepository.joinGroup(grp.id)
                                    val i = groups.indexOfFirst { it.id == grp.id }
                                    if (i != -1) groups[i] = grp.copy(isJoined = true, currentMembers = grp.currentMembers + 1)
                                    selectedTab = 2
                                },
                                onChatClick = { onNavigateToChat(grp.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // ── Create Tab ─────────────────────────────────────────
            1 -> {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Create a New Study Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                label = { Text("Group Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newGroupDetails,
                                onValueChange = { newGroupDetails = it },
                                label = { Text("Details / Topics to Discuss") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    if (newGroupName.isNotBlank()) {
                                        val g = StudyGroupRepository.createGroup(newGroupName.trim(), newGroupDetails.trim())
                                        groups.add(0, g)
                                        newGroupName = ""
                                        newGroupDetails = ""
                                        selectedTab = 2
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Group", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── My Groups Tab ──────────────────────────────────────
            2 -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (myGroups.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "You haven't joined any study groups yet.\nGo to \"Join\" to find one or \"Create\" to start your own!",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(myGroups) { grp ->
                            GroupCardItem(
                                group = grp,
                                onJoinClick = {},
                                onChatClick = { onNavigateToChat(grp.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Live Study Room widget
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            LiveStudyRoomCard(
                                timerText = timerText,
                                groupName = myGroups.first().name,
                                members   = StudyGroupRepository.getStudyRoomMembers()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCardItem(
    group: StudyGroup,
    onJoinClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val cardColor = if (group.id.hashCode() % 2 == 0) CardCoral else CardBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Host: ${group.host}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Details: ${group.details}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                }
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = EduHubPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${group.currentMembers}/${group.maxMembers} Members",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155)
                )
                if (group.isJoined) {
                    Button(
                        onClick = onChatClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) { Text("Open Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) { Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun LiveStudyRoomCard(
    timerText: String,
    groupName: String,
    members: List<com.example.eduhub20.data.model.StudyRoomMember>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active Study Room", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E3A8A))
            Text(groupName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(EduHubAccentGreen).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.8f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${members.size} Members Online", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Time Duration", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0F172A)).padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(timerText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
            }

            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Status:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
                members.forEach { m ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(m.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                            Text(m.currentStatus, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }
}