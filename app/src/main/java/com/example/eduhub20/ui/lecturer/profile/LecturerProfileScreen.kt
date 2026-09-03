package com.example.eduhub20.ui.lecturer.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.data.repository.PastYearRepository


@Composable
fun LecturerProfileScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {

    val user by
    AuthRepository.currentUser.collectAsState()


    var courseCount by remember {
        mutableIntStateOf(0)
    }

    var paperCount by remember {
        mutableIntStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }


    // =========================================
    // LOAD LECTURER INFORMATION
    // =========================================

    LaunchedEffect(user?.id) {

        loading = true


        CourseRepository
            .fetchCoursesFromSupabase()


        PastYearRepository
            .fetchPapersFromSupabase()


        val lecturerCourses =
            CourseRepository
                .getCoursesForUser(user)


        courseCount =
            lecturerCourses.size


        val lecturerCourseCodes =
            lecturerCourses
                .map { course ->
                    course.code
                }
                .toSet()


        paperCount =
            PastYearRepository
                .getPapers()
                .count { paper ->

                    lecturerCourseCodes.contains(
                        paper.courseCode
                    )
                }


        loading = false
    }


    // =========================================
    // SCROLLABLE PAGE
    // =========================================

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {


        // =====================================
        // PAGE TITLE
        // =====================================

        Text(
            text = "Profile",

            style =
                MaterialTheme.typography
                    .headlineMedium,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =====================================
        // PROFILE HEADER
        // =====================================

        Column(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Surface(
                modifier =
                    Modifier.size(92.dp),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {


                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Person,

                        contentDescription =
                            "Profile",

                        modifier =
                            Modifier.size(52.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Text(
                text =
                    user?.name
                        ?: "Lecturer",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Surface(
                shape =
                    RoundedCornerShape(20.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {


                Text(
                    text = "Lecturer",

                    modifier =
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text =
                    user?.email ?: "-",

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


        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        // =====================================
        // ACCOUNT INFORMATION
        // =====================================

        Text(
            text = "Account Information",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = 0.45f)
                )
        ) {


            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {


                ProfileInfoRow(
                    icon =
                        Icons.Default.Email,

                    title =
                        "Email",

                    value =
                        user?.email ?: "-"
                )


                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 16.dp
                        )
                )


                ProfileInfoRow(
                    icon =
                        Icons.Default.Person,

                    title =
                        "Role",

                    value =
                        "Lecturer"
                )


                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 16.dp
                        )
                )


                ProfileInfoRow(
                    icon =
                        Icons.Default.School,

                    title =
                        "Courses Taught",

                    value =
                        if (loading) {

                            "Loading..."

                        } else {

                            "$courseCount Active ${
                                if (courseCount == 1) {
                                    "Course"
                                } else {
                                    "Courses"
                                }
                            }"
                        }
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =====================================
        // TEACHING OVERVIEW
        // =====================================

        Text(
            text =
                "Teaching Overview",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {


            OverviewCard(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.School,

                value =
                    if (loading) {
                        "-"
                    } else {
                        courseCount.toString()
                    },

                label =
                    "Courses"
            )


            OverviewCard(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.PictureAsPdf,

                value =
                    if (loading) {
                        "-"
                    } else {
                        paperCount.toString()
                    },

                label =
                    "Past Year Papers"
            )
        }


        // IMPORTANT:
        // Do NOT use Modifier.weight(1f) here
        // because this whole page is scrollable.

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        // =====================================
        // SIGN OUT
        // =====================================

        OutlinedButton(
            onClick = {

                AuthRepository.signOut()

                onSignOut()
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),

            shape =
                RoundedCornerShape(14.dp),

            colors =
                ButtonDefaults
                    .outlinedButtonColors(
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
        ) {


            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Filled
                        .Logout,

                contentDescription =
                    null
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            Text(
                text =
                    "Sign Out",

                fontWeight =
                    FontWeight.SemiBold
            )
        }


        // Extra space so the button is comfortable
        // above the bottom navigation bar.

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )
    }
}


// =================================================
// ACCOUNT INFORMATION ROW
// =================================================

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {


    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {


        Surface(
            modifier =
                Modifier.size(42.dp),

            shape =
                RoundedCornerShape(12.dp),

            color =
                MaterialTheme
                    .colorScheme
                    .primaryContainer
        ) {


            Box(
                contentAlignment =
                    Alignment.Center
            ) {


                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(22.dp),

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }


        Spacer(
            modifier =
                Modifier.width(14.dp)
        )


        Column {


            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            Text(
                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}


// =================================================
// OVERVIEW CARD
// =================================================

@Composable
private fun OverviewCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {


    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(alpha = 0.45f)
            )
    ) {


        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Icon(
                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            Text(
                text =
                    label,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}