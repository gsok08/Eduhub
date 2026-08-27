package com.example.eduhub20.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.example.eduhub20.ui.lecturer.LecturerDashboardScreen
import com.example.eduhub20.ui.note.NoteDetailAiScreen
import com.example.eduhub20.ui.note.NoteQuizScreen
import com.example.eduhub20.ui.pastyear.PastYearPaperScreen
import com.example.eduhub20.ui.profile.ProfileScreen
import com.example.eduhub20.ui.quiz.QuizTakingScreen
import com.example.eduhub20.ui.theme.EduHubPrimary

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

    val isTopLevelDestination = bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    tonalElevation = 8.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
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
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EduHubPrimary,
                                selectedTextColor = EduHubPrimary,
                                indicatorColor = EduHubPrimary.copy(alpha = 0.12f)
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
            startDestination = if (currentUser?.role == UserRole.LECTURER) Screen.LecturerDashboard.route else Screen.Home.route,
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
                        navController.navigate(Screen.LecturerDashboard.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
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

            // Tab 5: Group
            composable(Screen.Group.route) {
                StudyGroupScreen(
                    onNavigateToChat = { groupId ->
                        navController.navigate(Screen.ChatRoom.createRoute(groupId))
                    }
                )
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    currentUser = currentUser,
                    onUpdateName = onUpdateName,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = onSignOut
                )
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

            // Chat Room Screen (clean groupId route)
            composable(
                route = Screen.ChatRoom.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val group = StudyGroupRepository.getGroups().find { it.id == groupId }
                val groupName = group?.name ?: "Study Group"
                ChatRoomScreen(
                    groupId = groupId,
                    groupName = groupName,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Lecturer Dashboard Screen
            composable(Screen.LecturerDashboard.route) {
                LecturerDashboardScreen(
                    onNavigateBack = {
                        if (currentUser?.role == UserRole.LECTURER) {
                            onSignOut()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onSignOut = onSignOut
                )
            }
        }
    }
}