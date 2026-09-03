package com.example.eduhub20.ui.lecturer.papers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.CourseRepository

@Composable
fun LecturerPapersScreen(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val currentUser by
    AuthRepository.currentUser.collectAsState()


    var papers by remember {
        mutableStateOf<List<PastYearPaper>>(
            PastYearRepository.getPapers()
        )
    }


    var loading by remember {
        mutableStateOf(true)
    }


    // -----------------------------
    // SEARCH
    // -----------------------------

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }


    // -----------------------------
    // FILTERS
    // -----------------------------

    var selectedCourse by remember {
        mutableStateOf("All")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedYear by remember {
        mutableStateOf("All")
    }

    var selectedScope by remember {
        mutableStateOf("All Papers")
    }

    var myCourseCodes by remember {
        mutableStateOf<Set<String>>(emptySet())
    }


    // -----------------------------
    // LOAD FROM SUPABASE
    // -----------------------------

    LaunchedEffect(currentUser?.id) {

        loading = true


        CourseRepository
            .fetchCoursesFromSupabase()


        val lecturerCourses =
            CourseRepository
                .getCoursesForUser(
                    currentUser
                )


        myCourseCodes =
            lecturerCourses
                .map { course ->
                    course.code
                }
                .toSet()


        papers =
            PastYearRepository
                .fetchPapersFromSupabase()


        loading = false
    }


    // -----------------------------
    // FILTER OPTIONS
    // -----------------------------

    val courseOptions =
        listOf("All") +
                papers
                    .map { it.courseCode }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()


    val categoryOptions =
        listOf("All") +
                papers
                    .map { it.subjectCategory }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()


    val yearOptions =
        listOf("All") +
                papers
                    .map { it.year }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedDescending()


    // -----------------------------
    // SEARCH + FILTER RESULT
    // -----------------------------

    val filteredPapers =
        papers.filter { paper ->

            val matchesScope =

                selectedScope == "All Papers" ||

                        myCourseCodes.contains(
                            paper.courseCode
                        )


            val matchesSearch =

                searchText.isBlank() ||

                        paper.courseCode.contains(
                            searchText,
                            ignoreCase = true
                        ) ||

                        paper.courseTitle.contains(
                            searchText,
                            ignoreCase = true
                        ) ||

                        paper.session.contains(
                            searchText,
                            ignoreCase = true
                        ) ||

                        paper.year.contains(
                            searchText,
                            ignoreCase = true
                        ) ||

                        paper.subjectCategory.contains(
                            searchText,
                            ignoreCase = true
                        )


            val matchesCourse =

                selectedCourse == "All" ||
                        paper.courseCode ==
                        selectedCourse


            val matchesCategory =

                selectedCategory == "All" ||
                        paper.subjectCategory ==
                        selectedCategory


            val matchesYear =

                selectedYear == "All" ||
                        paper.year ==
                        selectedYear


            matchesSearch &&
                    matchesScope &&
                    matchesCourse &&
                    matchesCategory &&
                    matchesYear
        }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {


        // =============================
        // HEADER
        // =============================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Column {

                Text(
                    text = "Past Year Papers",

                    style =
                        MaterialTheme.typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        "Papers uploaded from your courses",

                    style =
                        MaterialTheme.typography
                            .bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }


            IconButton(
                onClick = {

                    showSearch =
                        !showSearch

                    if (!showSearch) {
                        searchText = ""
                    }
                }
            ) {

                Icon(
                    imageVector =
                        if (showSearch) {
                            Icons.Default.Close
                        } else {
                            Icons.Default.Search
                        },

                    contentDescription =
                        if (showSearch) {
                            "Close Search"
                        } else {
                            "Search"
                        }
                )
            }
        }


        // =============================
        // SEARCH BOX
        // =============================

        if (showSearch) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
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
                        "Search course, title, session..."
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            null
                    )
                },

                singleLine = true,

                shape =
                    RoundedCornerShape(14.dp)
            )
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected =
                    selectedScope ==
                            "All Papers",

                onClick = {
                    selectedScope =
                        "All Papers"
                },

                label = {
                    Text("All Papers")
                }
            )


            FilterChip(
                selected =
                    selectedScope ==
                            "My Courses Only",

                onClick = {
                    selectedScope =
                        "My Courses Only"
                },

                label = {
                    Text("My Courses Only")
                }
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =============================
        // FILTER ROW
        // =============================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {


            Box(
                modifier =
                    Modifier.weight(1f)
            ) {

                PaperFilterDropdown(
                    label = "Course",
                    selectedValue =
                        selectedCourse,
                    options =
                        courseOptions,

                    onValueSelected = {
                        selectedCourse = it
                    }
                )
            }


            Box(
                modifier =
                    Modifier.weight(1f)
            ) {

                PaperFilterDropdown(
                    label = "Category",
                    selectedValue =
                        selectedCategory,
                    options =
                        categoryOptions,

                    onValueSelected = {
                        selectedCategory = it
                    }
                )
            }


            Box(
                modifier =
                    Modifier.weight(1f)
            ) {

                PaperFilterDropdown(
                    label = "Year",
                    selectedValue =
                        selectedYear,
                    options =
                        yearOptions,

                    onValueSelected = {
                        selectedYear = it
                    }
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // =============================
        // RESULT COUNT
        // =============================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Past Year Papers",

                style =
                    MaterialTheme.typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "${filteredPapers.size} found",

                style =
                    MaterialTheme.typography
                        .bodySmall,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =============================
        // CONTENT
        // =============================

        when {

            loading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }


            filteredPapers.isEmpty() -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {


                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {


                        Icon(
                            imageVector =
                                Icons.Default
                                    .PictureAsPdf,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    52.dp
                                ),

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Text(
                            text =
                                "No papers found",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    6.dp
                                )
                        )


                        Text(
                            text =
                                "Try changing the search or filters.",

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


            else -> {

                LazyColumn(
                    modifier =
                        Modifier.weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {


                    items(
                        items =
                            filteredPapers,

                        key = {
                            it.id
                        }
                    ) { paper ->


                        LecturerPaperCard(
                            paper = paper,

                            onOpen = {

                                if (
                                    paper.pdfUrl
                                        .isNotBlank()
                                ) {

                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                paper.pdfUrl
                                            )
                                        )


                                    context.startActivity(
                                        intent
                                    )
                                }
                            }
                        )
                    }


                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    80.dp
                                )
                        )
                    }
                }
            }
        }
    }
}


// =====================================================
// FILTER DROPDOWN
// =====================================================

@Composable
private fun PaperFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }


    Box {

        OutlinedButton(
            onClick = {
                expanded = true
            },

            modifier =
                Modifier.fillMaxWidth(),

            contentPadding =
                PaddingValues(
                    horizontal = 10.dp
                )
        ) {


            Text(
                text =
                    if (
                        selectedValue == "All"
                    ) {
                        "$label: All"
                    } else {
                        selectedValue
                    },

                modifier =
                    Modifier.weight(1f),

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )


            Icon(
                imageVector =
                    Icons.Default
                        .KeyboardArrowDown,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(
                        18.dp
                    )
            )
        }


        DropdownMenu(
            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {


            options.forEach { option ->

                DropdownMenuItem(

                    text = {

                        Text(
                            text =
                                if (
                                    option == "All"
                                ) {
                                    "$label: All"
                                } else {
                                    option
                                }
                        )
                    },

                    onClick = {

                        onValueSelected(
                            option
                        )

                        expanded = false
                    }
                )
            }
        }
    }
}


// =====================================================
// PAPER CARD
// =====================================================

@Composable
private fun LecturerPaperCard(
    paper: PastYearPaper,
    onOpen: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.35f)
            )
    ) {


        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Surface(
                modifier =
                    Modifier.size(
                        44.dp
                    ),

                shape =
                    RoundedCornerShape(
                        12.dp
                    ),

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
                            Icons.Default
                                .PictureAsPdf,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {


                Text(
                    text =
                        paper.session,

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,

                    fontWeight =
                        FontWeight
                            .SemiBold,

                    maxLines = 2,

                    overflow =
                        TextOverflow
                            .Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "${paper.courseCode} • ${paper.courseTitle}",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines = 1,

                    overflow =
                        TextOverflow
                            .Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(
                    text =
                        "${paper.year} • ${paper.durationMinutes} mins • ${paper.totalMarks} marks",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            Button(
                onClick = onOpen,

                enabled =
                    paper.pdfUrl
                        .isNotBlank(),

                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {

                Text(
                    text = "Open"
                )
            }
        }
    }
}