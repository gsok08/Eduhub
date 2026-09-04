package com.example.eduhub20.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eduhub20.data.model.EduHubUser
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.StudyGroupRepository
import com.example.eduhub20.ui.calendar.CalendarScreen
import com.example.eduhub20.ui.course.CourseDetailScreen
import com.example.eduhub20.ui.group.ChatRoomScreen
import com.example.eduhub20.ui.group.StudyGroupScreen
import com.example.eduhub20.ui.home.HomeScreen
import com.example.eduhub20.ui.note.NoteDetailAiScreen
import com.example.eduhub20.ui.note.NoteQuizScreen
import com.example.eduhub20.ui.pastyear.PastYearPaperScreen
import com.example.eduhub20.ui.profile.ProfileScreen
import com.example.eduhub20.ui.quiz.QuizTakingScreen
import com.example.eduhub20.ui.theme.EduHubPrimary
import com.example.eduhub20.ui.lecturer.home.LecturerHomeScreen
import com.example.eduhub20.ui.lecturer.course.CreateCourseScreen
import com.example.eduhub20.ui.lecturer.course.CourseMainScreen
import com.example.eduhub20.ui.lecturer.materials.AnnouncementScreen
import com.example.eduhub20.ui.lecturer.materials.MaterialsScreen
import com.example.eduhub20.ui.lecturer.materials.UploadLectureNoteScreen
import com.example.eduhub20.ui.lecturer.course.StudentsScreen
import com.example.eduhub20.ui.lecturer.profile.LecturerProfileScreen
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.example.eduhub20.ui.lecturer.components.LecturerBottomBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.eduhub20.ui.lecturer.course.LecturerCoursesScreen
import com.example.eduhub20.ui.lecturer.course.LecturerCourseMode
import com.example.eduhub20.ui.lecturer.materials.PastYearPaperUploadScreen
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.ui.lecturer.papers.LecturerPapersScreen
@Composable
fun EduHubNavHost(
    currentUser: EduHubUser?,
    onUpdateName: (String) -> Unit,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // For Lecturers, remove the Group chat system from the bottom navigation bar (4 tabs)
    // For Students, show all 5 tabs including Group
    val visibleTabs = remember(currentUser?.role) {
        if (currentUser?.role == UserRole.LECTURER) {
            bottomNavScreens.filter { it.route != Screen.Group.route }
        } else {
            bottomNavScreens
        }
    }

    val isTopLevelDestination = visibleTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    visibleTabs.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.18f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                            label = "NavIconScale"
                        )

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title,
                                        modifier = Modifier.scale(iconScale)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EduHubPrimary,
                                selectedTextColor = EduHubPrimary,
                                indicatorColor = EduHubPrimary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination =   if (currentUser?.role == UserRole.LECTURER) {
                Screen.LecturerHome.route
            } else {
                Screen.Home.route
            },
            enterTransition = { fadeIn(animationSpec = tween(160)) },
            exitTransition = { fadeOut(animationSpec = tween(160)) },
            popEnterTransition = { fadeIn(animationSpec = tween(160)) },
            popExitTransition = { fadeOut(animationSpec = tween(160)) },
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Home
            composable(Screen.Home.route) {
                HomeScreen(
                    currentUser = currentUser,
                    onNavigateToCourse = { courseId ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId))
                    },
                    onNavigateToLecturerPortal = {
                        navController.navigate(Screen.LecturerHome.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Screen.Calendar.route)
                    }
                )
            }

            // Tab 2: Past Year Paper
            composable(Screen.PastYear.route) {
                PastYearPaperScreen()
            }

            // Tab 3: Note / Quiz
            composable(Screen.NoteQuiz.route) {
                NoteQuizScreen(
                    currentUser = currentUser,
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate(Screen.NoteDetailAi.createRoute(noteId))
                    },
                    onNavigateToQuiz = { noteId, courseCode ->
                        navController.navigate(Screen.QuizTaking.createRoute(noteId, courseCode))
                    }
                )
            }

            // Tab 4: Calendar
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }

            // Tab 5: Group (Students Only)
            composable(Screen.Group.route) {
                StudyGroupScreen(
                    onNavigateToChat = { groupId ->
                        navController.navigate(Screen.ChatRoom.createRoute(groupId))
                    },
                    onNavigateToPomodoro = { roomId, roomName ->
                        navController.navigate(Screen.Pomodoro.createRoute(roomId, roomName))
                    }
                )
            }

            // Profile Screen
            // Profile Screen
            composable(
                Screen.Profile.route
            ) {

                if (
                    currentUser?.role ==
                    UserRole.LECTURER
                ) {

                    Scaffold(
                        bottomBar = {

                            LecturerBottomBar(
                                selectedItem = 3,

                                onItemSelected = { index ->

                                    when (index) {

                                        0 -> {
                                            navController.navigate(
                                                Screen.LecturerHome.route
                                            )
                                        }

                                        1 -> {
                                            navController.navigate(
                                                Screen.LecturerCourses.route
                                            )
                                        }

                                        2 -> {
                                            navController.navigate(
                                                Screen.LecturerPapers.route
                                            )
                                        }

                                        3 -> {
                                            // Already on Profile
                                        }
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->

                        LecturerProfileScreen(
                            onSignOut = onSignOut,

                            modifier = Modifier.padding(
                                paddingValues
                            )
                        )
                    }

                } else {

                    // Student profile stays unchanged
                    ProfileScreen(
                        currentUser = currentUser,

                        onUpdateName = onUpdateName,

                        onNavigateBack = {
                            navController.popBackStack()
                        },

                        onSignOut = onSignOut
                    )
                }
            }

            // Course Detail Flow
            composable(
                route = Screen.CourseDetail.route,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                CourseDetailScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate(Screen.NoteDetailAi.createRoute(noteId))
                    }
                )
            }

            // AI Note Detail Screen
            composable(
                route = Screen.NoteDetailAi.route,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                NoteDetailAiScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuiz = { nId, cCode ->
                        navController.navigate(Screen.QuizTaking.createRoute(nId, cCode))
                    }
                )
            }

            // Quiz Mode Screen
            composable(
                route = Screen.QuizTaking.route,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("courseCode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val courseCode = backStackEntry.arguments?.getString("courseCode") ?: ""
                QuizTakingScreen(
                    noteId = noteId,
                    courseCode = courseCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Chat Room Screen
            composable(
                route = Screen.ChatRoom.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink { uriPattern = "eduhub://group/join/{groupId}" }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val group = StudyGroupRepository.getGroups().find { it.id == groupId }
                val groupName = group?.name ?: "Study Group"
                androidx.compose.runtime.LaunchedEffect(groupId) {
                    if (group == null || !group.isJoined) {
                        StudyGroupRepository.joinGroup(groupId)
                    }
                }
                ChatRoomScreen(
                    groupId = groupId,
                    groupName = groupName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGroupInfo = { gid ->
                        navController.navigate(Screen.GroupInfo.createRoute(gid))
                    }
                )
            }

            // Pomodoro Focus Room Screen
            composable(
                route = Screen.Pomodoro.route,
                arguments = listOf(
                    navArgument("roomId") {
                        type = NavType.StringType
                        defaultValue = "general"
                    },
                    navArgument("roomName") {
                        type = NavType.StringType
                        defaultValue = "Focus Room"
                    }
                ),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink { uriPattern = "eduhub://pomodoro/join/{roomId}" }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: "general"
                val rawRoomName = backStackEntry.arguments?.getString("roomName") ?: "Focus Room"
                val roomName = try {
                    java.net.URLDecoder.decode(rawRoomName, "UTF-8")
                } catch (_: Exception) { rawRoomName }

                com.example.eduhub20.ui.pomodoro.PomodoroScreen(
                    roomId = roomId,
                    roomName = roomName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPayment = { navController.navigate(Screen.TngPayment.route) }
                )
            }

            // Touch 'n Go AI Receipt Verification Screen
            composable(Screen.TngPayment.route) {
                com.example.eduhub20.ui.pomodoro.TngPaymentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSuccess = { navController.popBackStack() }
                )
            }

            // Group Info / Details Screen
            composable(
                route = Screen.GroupInfo.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                com.example.eduhub20.ui.group.GroupInfoScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onGroupLeftOrKicked = {
                        navController.popBackStack(Screen.Group.route, inclusive = false)
                    }
                )
            }

            // Lecturer Dashboard Screen
            // Lecturer Home Screen
            composable(Screen.LecturerHome.route) {
                var selectedItem by remember {
                    mutableIntStateOf(0)
                }
                Scaffold(
                    bottomBar = {
                        LecturerBottomBar(
                            selectedItem = selectedItem,
                            onItemSelected = { index ->
                                selectedItem = index
                                when(index) {
                                    0 -> {
                                        navController.navigate(
                                            Screen.LecturerHome.route
                                        )
                                    }
                                    1 -> {
                                        // Course page
                                        navController.navigate(
                                            Screen.LecturerCourses.route
                                        )
                                    }
                                    2 -> {
                                        // Past year paper
                                        navController.navigate(
                                            Screen.LecturerPapers.route
                                        )
                                    }
                                    3 -> {
                                        navController.navigate(
                                            Screen.Profile.route
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    LecturerHomeScreen(
                        lecturerName = currentUser?.name ?: "Lecturer",

                        onCourseClick = { courseId ->

                            navController.navigate(
                                Screen.CourseMain.createRoute(
                                    courseId = courseId,
                                    mode = "view"
                                )
                            )

                        },
                        modifier = Modifier.padding(paddingValues)
                    )

                }
            }

            composable(Screen.CreateCourse.route) {
                CreateCourseScreen(
                    lecturerName = currentUser?.name ?: "Lecturer",
                    onBack = {
                        navController.popBackStack()
                    },
                    onCreated = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.LecturerCourses.route
            ) {

                Scaffold(
                    bottomBar = {

                        LecturerBottomBar(
                            selectedItem = 1,

                            onItemSelected = { index ->

                                when (index) {

                                    0 -> {
                                        navController.navigate(
                                            Screen.LecturerHome.route
                                        )
                                    }

                                    1 -> {
                                        // Already on Courses
                                    }

                                    2 -> {
                                        navController.navigate(
                                            Screen.LecturerPapers.route
                                        )
                                    }

                                    3 -> {
                                        navController.navigate(
                                            Screen.Profile.route
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    LecturerCoursesScreen(

                        onCourseClick = { courseId ->

                            navController.navigate(
                                Screen.CourseMain.createRoute(
                                    courseId = courseId,
                                    mode = "manage"
                                )
                            )
                        },

                        onCreateCourse = {

                            navController.navigate(
                                Screen.CreateCourse.route
                            )
                        },

                        modifier = Modifier.padding(
                            paddingValues
                        )
                    )
                }
            }

            composable(
                Screen.LecturerPapers.route
            ) {

                Scaffold(
                    bottomBar = {

                        LecturerBottomBar(
                            selectedItem = 2,

                            onItemSelected = { index ->

                                when (index) {

                                    0 -> {
                                        navController.navigate(
                                            Screen.LecturerHome.route
                                        )
                                    }

                                    1 -> {
                                        navController.navigate(
                                            Screen.LecturerCourses.route
                                        )
                                    }

                                    2 -> {
                                        // Already on Papers
                                    }

                                    3 -> {
                                        navController.navigate(
                                            Screen.Profile.route
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->

                    LecturerPapersScreen(
                        modifier =
                            Modifier.padding(
                                paddingValues
                            )
                    )
                }
            }

            composable(
                route = Screen.CourseMain.route,

                arguments = listOf(

                    navArgument("courseId") {
                        type = NavType.StringType
                    },

                    navArgument("mode") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val courseId =
                    backStackEntry.arguments
                        ?.getString("courseId")
                        ?: ""

                val modeString =
                    backStackEntry.arguments
                        ?.getString("mode")
                        ?: "view"

                val courseMode =
                    LecturerCourseMode.fromString(
                        modeString
                    )

                Scaffold(
                    bottomBar = {

                        LecturerBottomBar(

                            selectedItem =
                                if (
                                    courseMode ==
                                    LecturerCourseMode.MANAGE
                                ) {
                                    1
                                } else {
                                    0
                                },

                            onItemSelected = { index ->

                                when (index) {

                                    0 -> {
                                        navController.navigate(
                                            Screen.LecturerHome.route
                                        )
                                    }

                                    1 -> {
                                        navController.navigate(
                                            Screen.LecturerCourses.route
                                        )
                                    }

                                    2 -> {
                                        navController.navigate(
                                            Screen.LecturerPapers.route
                                        )
                                    }

                                    3 -> {
                                        navController.navigate(
                                            Screen.Profile.route
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->

                    Box(
                        modifier =
                            Modifier.padding(paddingValues)
                    ) {

                        CourseMainScreen(
                            courseId = courseId,
                            mode = courseMode,

                            onBack = {
                                navController.popBackStack()
                            },

                            onAddAnnouncement = {

                                navController.navigate(
                                    Screen.Announcement
                                        .createRoute(courseId)
                                )
                            },

                            onUploadNote = { courseCode ->

                                navController.navigate(
                                    Screen.UploadLectureNote
                                        .createRoute(courseCode)
                                )
                            },

                            onUploadPaper = { courseCode ->

                                navController.navigate(
                                    Screen.PastYearPaperUpload
                                        .createRoute(courseCode)
                                )
                            }
                        )
                    }
                }
            }

            composable(
                route = Screen.Announcement.route,
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val courseId =
                    backStackEntry.arguments
                        ?.getString("courseId")
                        ?: ""

                AnnouncementScreen(
                    courseId = courseId,

                    lecturerName =
                        currentUser?.name
                            ?: "Lecturer",

                    onBack = {
                        navController.popBackStack()
                    },


                    onPublished = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Materials.route,
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.StringType
                    }
                )
            ){
                val courseId =
                    it.arguments?.getString("courseId") ?: ""
                MaterialsScreen(
                    courseCode = courseId,
                    onUploadNote = {
                        navController.navigate(
                            Screen.UploadLectureNote.createRoute(courseId)
                        )

                    },
                    onOpenPaper = {
                        navController.navigate(
                            Screen.PastYearPaperUpload.createRoute(courseId)
                        )
                    }
                )
            }

            composable(
                route = Screen.UploadLectureNote.route,
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                // This value may be a course ID or course code
                val courseKey =
                    backStackEntry.arguments
                        ?.getString("courseId")
                        ?: ""

                // Find the actual Course object
                val course =
                    CourseRepository.getCourseById(
                        courseKey
                    )

                UploadLectureNoteScreen(
                    // Example: AMIT3353
                    courseCode =
                        course?.code ?: courseKey,

                    // Example: Mobile Application Development
                    courseTitle =
                        course?.title ?: "",

                    onBack = {
                        navController.popBackStack()
                    },

                    onUploaded = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.PastYearPaperUpload.route,

                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val courseCode =
                    backStackEntry.arguments
                        ?.getString("courseId")
                        ?: ""

                PastYearPaperUploadScreen(
                    courseCode = courseCode,

                    onBack = {
                        navController.popBackStack()
                    },

                    onUploaded = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Students.route,
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.StringType
                    }
                )
            ){
                val courseId =
                    it.arguments?.getString("courseId") ?: ""
                StudentsScreen(
                    courseId = courseId
                )
            }
        }
    }
}