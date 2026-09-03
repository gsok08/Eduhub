package com.example.eduhub20.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.StudyGroup
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.data.repository.StudyRoomMember
import com.example.eduhub20.ui.theme.CardBlue
import com.example.eduhub20.ui.theme.CardCoral
import com.example.eduhub20.ui.theme.EduHubAccentGreen
import com.example.eduhub20.ui.theme.EduHubPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

@Composable
fun StudyGroupScreen(
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    val currentUser = AuthRepository.currentUser.collectAsState().value

    val groups = remember(currentUser?.id) {
        mutableStateListOf(
            *StudyGroupRepository.getGroups().toTypedArray()
        )
    }

    // Search
    var searchGroupQuery by remember {
        mutableStateOf("")
    }

    // Live Auto-Refresh: Fetch live study groups from Supabase continuously every 3s
    LaunchedEffect(currentUser?.id) {
        while (true) {
            val remoteGroups = StudyGroupRepository.fetchGroupsFromSupabase()
            val currentIds = groups.map { it.id }.toSet()
            val remoteIds = remoteGroups.map { it.id }.toSet()
            if (currentIds != remoteIds || groups.size != remoteGroups.size ||
                remoteGroups.any { r -> groups.find { it.id == r.id }?.currentMembers != r.currentMembers }
            ) {
                groups.clear()
                groups.addAll(remoteGroups)
            }
            delay(3000L)
        }
    }

    var newGroupName by remember { mutableStateOf("") }
    var newGroupDetails by remember { mutableStateOf("") }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var inviteCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var isJoiningByCode by remember { mutableStateOf(false) }

    // Selected course for new group
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    // Courses loaded from Supabase
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }

    // Dropdown state
    var courseMenuExpanded by remember { mutableStateOf(false) }

    // Fetch courses
    LaunchedEffect(currentUser?.id) {
        courses = CourseRepository.fetchCoursesFromSupabase()
    }

    // Recommended groups
    val recommended = remember(
        groups.size,
        searchGroupQuery,
        groups.map { it.isJoined }
    ) {
        groups.filter { group ->

            !group.isJoined &&

                    (
                            searchGroupQuery.isBlank() ||

                                    group.name.contains(
                                        searchGroupQuery,
                                        true
                                    ) ||

                                    group.details.contains(
                                        searchGroupQuery,
                                        true
                                    ) ||

                                    group.host.contains(
                                        searchGroupQuery,
                                        true
                                    ) ||

                                    group.courseCode.contains(
                                        searchGroupQuery,
                                        true
                                    ) ||

                                    group.courseTitle.contains(
                                        searchGroupQuery,
                                        true
                                    )
                            )
        }
    }

    // My groups
    val myGroups = remember(
        groups.size,
        groups.map { it.isJoined }
    ) {
        groups.filter {
            it.isJoined
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
        ) {

            Text(
                "Study Group",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(12.dp)
                    ),
                containerColor = MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.5f)
            ) {

                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            "Join",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            "Create",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    text = {
                        Text(
                            "My Group (${myGroups.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            when (selectedTab) {

                // =====================================================
                // JOIN TAB
                // =====================================================

                0 -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        item {
                            // Join by invite code/link button
                            Button(
                                onClick = {
                                    inviteCodeInput = ""
                                    joinError = null
                                    showJoinByCodeDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EduHubPrimary.copy(alpha = 0.10f),
                                    contentColor = EduHubPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Have an Invite Link or Code? Click to Join", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = searchGroupQuery,
                                onValueChange = {
                                    searchGroupQuery = it
                                },
                                placeholder = {
                                    Text(
                                        "Search recommended groups..."
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {

                                    if (
                                        searchGroupQuery.isNotBlank()
                                    ) {

                                        IconButton(
                                            onClick = {
                                                searchGroupQuery = ""
                                            }
                                        ) {

                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            Text(
                                "Recommended Groups",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme
                                    .typography
                                    .titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                        }

                        if (recommended.isEmpty()) {

                            item {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults
                                        .cardColors(
                                            containerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant
                                                    .copy(alpha = 0.5f)
                                        )
                                ) {

                                    Text(
                                        text =
                                            if (
                                                searchGroupQuery.isBlank()
                                            ) {
                                                "No recommended groups to join right now. Tap \"Create\" to start a new group!"
                                            } else {
                                                "No study groups found matching \"$searchGroupQuery\"."
                                            },
                                        modifier = Modifier.padding(20.dp),
                                        style = MaterialTheme
                                            .typography
                                            .bodyMedium,
                                        color = MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    )
                                }
                            }

                        } else {

                            items(recommended) { grp ->

                                GroupCardItem(
                                    group = grp,

                                    onJoinClick = {

                                        scope.launch {

                                            StudyGroupRepository
                                                .joinGroup(grp.id)

                                            val i =
                                                groups.indexOfFirst {
                                                    it.id == grp.id
                                                }

                                            if (i != -1) {

                                                groups[i] =
                                                    groups[i].copy(
                                                        isJoined = true,
                                                        currentMembers =
                                                            groups[i]
                                                                .currentMembers + 1
                                                    )
                                            }

                                            selectedTab = 2
                                        }
                                    },

                                    onChatClick = {
                                        onNavigateToChat(grp.id)
                                    }
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )
                            }
                        }
                    }
                }

                // =====================================================
                // CREATE TAB
                // =====================================================

                1 -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                Text(
                                    "Create a New Study Group",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(14.dp)
                                )

                                // GROUP NAME
                                OutlinedTextField(
                                    value = newGroupName,
                                    onValueChange = {
                                        newGroupName = it
                                    },
                                    label = {
                                        Text("Group Name")
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                // DETAILS
                                OutlinedTextField(
                                    value = newGroupDetails,
                                    onValueChange = {
                                        newGroupDetails = it
                                    },
                                    label = {
                                        Text(
                                            "Details / Topics to Discuss"
                                        )
                                    },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                // COURSE SELECTION
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    OutlinedButton(
                                        onClick = {
                                            courseMenuExpanded = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {

                                        Text(
                                            text =
                                                selectedCourse?.let {
                                                    "${it.code} - ${it.title}"
                                                }
                                                    ?: "Select Course"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded =
                                            courseMenuExpanded,

                                        onDismissRequest = {
                                            courseMenuExpanded = false
                                        }
                                    ) {

                                        if (courses.isEmpty()) {

                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "No courses available"
                                                    )
                                                },
                                                onClick = {
                                                    courseMenuExpanded =
                                                        false
                                                }
                                            )

                                        } else {

                                            courses.forEach { course ->

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "${course.code} - ${course.title}"
                                                        )
                                                    },

                                                    onClick = {

                                                        selectedCourse =
                                                            course

                                                        courseMenuExpanded =
                                                            false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(18.dp)
                                )

                                // CREATE BUTTON
                                Button(
                                    onClick = {

                                        if (
                                            newGroupName.isNotBlank() &&
                                            selectedCourse != null
                                        ) {

                                            scope.launch {

                                                val g =
                                                    StudyGroupRepository
                                                        .createGroup(
                                                            name =
                                                                newGroupName
                                                                    .trim(),

                                                            details =
                                                                newGroupDetails
                                                                    .trim(),

                                                            course =
                                                                selectedCourse!!,

                                                            hostUser =
                                                                currentUser
                                                        )

                                                groups.add(
                                                    0,
                                                    g
                                                )

                                                newGroupName = ""
                                                newGroupDetails = ""
                                                selectedCourse = null

                                                selectedTab = 2
                                            }
                                        }
                                    },

                                    enabled =
                                        newGroupName.isNotBlank() &&
                                                selectedCourse != null,

                                    shape = RoundedCornerShape(12.dp),

                                    colors =
                                        ButtonDefaults
                                            .buttonColors(
                                                containerColor =
                                                    EduHubPrimary
                                            ),

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {

                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )

                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )

                                    Text(
                                        "Create Group",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // =====================================================
                // MY GROUPS TAB
                // =====================================================

                2 -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        if (myGroups.isEmpty()) {

                            item {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                                .copy(alpha = 0.5f)
                                    )
                                ) {

                                    Text(
                                        "You haven't joined any study groups yet.\nGo to \"Join\" to find recommended groups or \"Create\" to start your own!",
                                        modifier =
                                            Modifier.padding(20.dp),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }

                        } else {

                            items(myGroups) { grp ->

                                GroupCardItem(
                                    group = grp,

                                    onJoinClick = {},

                                    onChatClick = {
                                        onNavigateToChat(grp.id)
                                    }
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )
                            }

                            item {

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                LiveStudyRoomCard(
                                    groupName =
                                        myGroups.first().name,

                                    members =
                                        StudyGroupRepository
                                            .getStudyRoomMembers()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showJoinByCodeDialog) {
            AlertDialog(
                onDismissRequest = { if (!isJoiningByCode) showJoinByCodeDialog = false },
                title = { Text("Join Study Group", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Paste the group invitation link (eduhub://group/join/...) or enter the Group Code below:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it; joinError = null },
                            placeholder = { Text("e.g. eduhub://group/join/abc-123") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (joinError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = joinError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val input = inviteCodeInput.trim()
                            if (input.isNotBlank()) {
                                isJoiningByCode = true
                                scope.launch {
                                    val res = StudyGroupRepository.joinGroupByCodeOrLink(input)
                                    isJoiningByCode = false
                                    res.fold(
                                        onSuccess = { joinedGroup ->
                                            showJoinByCodeDialog = false
                                            onNavigateToChat(joinedGroup.id)
                                        },
                                        onFailure = { e ->
                                            joinError = e.message ?: "Failed to join group. Please check the code."
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isJoiningByCode && inviteCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EduHubPrimary)
                    ) {
                        if (isJoiningByCode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Join Group")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showJoinByCodeDialog = false },
                        enabled = !isJoiningByCode
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


// =============================================================
// GROUP CARD
// =============================================================

@Composable
fun GroupCardItem(
    group: StudyGroup,
    onJoinClick: () -> Unit,
    onChatClick: () -> Unit
) {

    val cardColor =
        if (group.id.hashCode() % 2 == 0)
            CardCoral
        else
            CardBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    // COURSE
                    if (group.courseCode.isNotBlank()) {

                        Text(
                            "${group.courseCode} • ${group.courseTitle}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A)
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )
                    }

                    // STATUS
                    if (group.status == "ACTIVE") {

                        Text(
                            "🟢 Ongoing",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )
                    }

                    Text(
                        "Host: ${group.host}",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color = Color(0xFF475569)
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        "Details: ${group.details}",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color = Color(0xFF475569)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            Color.White.copy(
                                alpha = 0.85f
                            )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = EduHubPrimary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    "${group.currentMembers}/${group.maxMembers} Members",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )

                if (group.isJoined) {

                    Button(
                        onClick = onChatClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                        shape =
                            RoundedCornerShape(8.dp),
                        modifier =
                            Modifier.height(34.dp)
                    ) {

                        Text(
                            "Open Chat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                } else {

                    Button(
                        onClick = onJoinClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                        shape =
                            RoundedCornerShape(8.dp),
                        modifier =
                            Modifier.height(34.dp)
                    ) {

                        Text(
                            "Join",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// LIVE STUDY ROOM
// =============================================================

@Composable
fun LiveStudyRoomCard(
    groupName: String,
    members: List<StudyRoomMember>
) {

    var secondsElapsed by remember {
        mutableIntStateOf(794)
    }

    LaunchedEffect(Unit) {

        while (true) {

            delay(1000)

            secondsElapsed++
        }
    }

    val timerText =
        "%02d:%02d".format(
            secondsElapsed / 60,
            secondsElapsed % 60
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBlue
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "Active Study Room",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1E3A8A)
            )

            Text(
                groupName,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color = Color(0xFF334155)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row {

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            EduHubAccentGreen
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        "Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            Color.White.copy(
                                alpha = 0.8f
                            )
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        "${members.size} Members Online",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                "Time Duration",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color = Color(0xFF334155)
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        Color(0xFF0F172A)
                    )
                    .padding(
                        horizontal = 24.dp,
                        vertical = 10.dp
                    )
            ) {

                Text(
                    timerText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }

            if (members.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    "Status:",
                    fontWeight = FontWeight.Bold,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color = Color(0xFF1E293B)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                members.forEach { m ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Column {

                            Text(
                                m.userId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Joined ${m.joinedAt}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}