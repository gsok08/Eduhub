package com.example.eduhub20.ui.lecturer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.ui.theme.EduHubPrimary


@Composable
fun LecturerHomeScreen(
    lecturerName: String,
    onCourseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {


    val currentUser by AuthRepository.currentUser.collectAsState()


    var courses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }


    var searchText by remember {
        mutableStateOf("")
    }



    LaunchedEffect(currentUser) {

        courses =
            CourseRepository.getCoursesForUser(
                currentUser
            )

    }



    val filteredCourses =
        courses.filter {

            it.code.contains(
                searchText,
                ignoreCase = true
            )
                    ||
                    it.title.contains(
                        searchText,
                        ignoreCase = true
                    )

        }




    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(16.dp)
    ) {



        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {


            Column {

                Text(
                    text = "Good morning,"
                )


                Text(
                    text = "$lecturerName 👋",
                    style =
                        MaterialTheme.typography.headlineSmall
                )

            }



            Surface(
                modifier =
                    Modifier.size(48.dp),
                shape =
                    RoundedCornerShape(50),
                color =
                    EduHubPrimary.copy(alpha = 0.15f)
            ) {


                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier =
                        Modifier.padding(12.dp),
                    tint = EduHubPrimary
                )

            }

        }



        Spacer(
            Modifier.height(20.dp)
        )



        OutlinedTextField(

            value = searchText,

            onValueChange = {
                searchText = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {
                Text(
                    "Search courses..."
                )
            },

            leadingIcon = {

                Icon(
                    Icons.Default.Search,
                    null
                )

            },

            singleLine = true,

            shape =
                RoundedCornerShape(14.dp)

        )



        Spacer(
            Modifier.height(25.dp)
        )



        Text(
            text = "My Courses",
            style =
                MaterialTheme.typography.titleLarge
        )



        Spacer(
            Modifier.height(12.dp)
        )



        LazyColumn(
            modifier = Modifier
                .weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ){


            items(filteredCourses){ course ->


                var studentCount by remember {
                    mutableStateOf(0)
                }


                var noteCount by remember {
                    mutableStateOf(0)
                }


                var pastYearCount by remember {
                    mutableStateOf(0)
                }



                LaunchedEffect(course.id){


                    studentCount =
                        CourseRepository
                            .getCourseStudentCount(
                                course.id
                            )


                    noteCount =
                        CourseRepository
                            .getLectureNoteCount(
                                course.code
                            )


                    pastYearCount =
                        CourseRepository
                            .getPastYearCount(
                                course.code
                            )

                }





                Card(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                onCourseClick(course.id)

                            },

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme
                                    .primaryContainer
                                    .copy(alpha = 0.35f)
                        )

                ){


                    Column(

                        modifier =
                            Modifier.padding(16.dp)

                    ){



                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically

                        ){



                            Column {


                                Text(

                                    text = course.code,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    fontSize = 16.sp

                                )



                                Text(

                                    text = course.title,

                                    fontSize = 13.sp

                                )



                                Text(

                                    text =
                                        "Lecturer: ${course.lecturerName}",

                                    fontSize = 12.sp

                                )


                            }



                            Text(

                                text = "→",

                                fontSize = 24.sp

                            )


                        }





                        Spacer(
                            Modifier.height(20.dp)
                        )





                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween

                        ){



                            Column {


                                Text(

                                    text = "Students",

                                    fontSize = 12.sp

                                )



                                Text(

                                    text = studentCount.toString(),

                                    fontSize = 14.sp

                                )


                            }






                            Column {


                                Text(

                                    text = "Materials",

                                    fontSize = 12.sp

                                )



                                Text(

                                    text =
                                        "${noteCount + pastYearCount} Files",

                                    fontSize = 14.sp

                                )



                                Text(

                                    text =
                                        "Notes: $noteCount",

                                    fontSize = 12.sp

                                )



                                Text(

                                    text =
                                        "Past Year: $pastYearCount",

                                    fontSize = 12.sp

                                )


                            }


                        }


                    }


                }


            }


        }


    }


}