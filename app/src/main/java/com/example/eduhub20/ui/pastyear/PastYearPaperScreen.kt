package com.example.eduhub20.ui.pastyear

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.PastYearPaper
import com.example.eduhub20.data.repository.AuthRepository
import com.example.eduhub20.data.repository.PastYearRepository
import com.example.eduhub20.ui.components.PdfAnnotationViewer
import com.example.eduhub20.ui.components.buildPdfAnnotationStorageKey


@Composable
fun PastYearPaperScreen(
    modifier: Modifier = Modifier
) {

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


    // =========================================
    // SEARCH
    // =========================================

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }


    // =========================================
    // ANNOTATION SCOPE
    // =========================================

    var selectedScope by remember {
        mutableStateOf("All Papers")
    }

    var annotatedPaperIds by remember(
        currentUser?.id
    ) {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }


    // =========================================
    // FILTERS
    // =========================================

    var selectedCourse by remember {
        mutableStateOf("All")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedYear by remember {
        mutableStateOf("All")
    }


    // =========================================
    // CURRENT PAPER
    // =========================================

    var viewingPaper by remember {
        mutableStateOf<PastYearPaper?>(null)
    }


    // =========================================
    // CHECK SAVED ANNOTATIONS
    // =========================================

    fun refreshAnnotatedPapers(
        sourcePapers: List<PastYearPaper> = papers
    ) {

        val userId =
            currentUser?.id

        if (userId.isNullOrBlank()) {

            annotatedPaperIds =
                emptySet()

            return
        }


        annotatedPaperIds =
            sourcePapers
                .filter { paper ->

                    val documentTitle =
                        "${paper.courseCode} - ${paper.session}"


                    val key =
                        buildPdfAnnotationStorageKey(
                            ownerId = userId,
                            pdfUrl = paper.pdfUrl,
                            courseCode = paper.courseCode,
                            documentTitle = documentTitle
                        )


                    val savedData =
                        EduHubLocalStorage
                            .loadPdfAnnotations(
                                key
                            )


                    savedData != null &&
                            (
                                    savedData.strokes.isNotEmpty() ||
                                            savedData.stickyNotes.isNotEmpty()
                                    )
                }
                .map { paper ->
                    paper.id
                }
                .toSet()
    }


    // =========================================
    // LOAD ALL PAST PAPERS
    // =========================================

    LaunchedEffect(
        currentUser?.id
    ) {

        loading = true


        val remotePapers =
            PastYearRepository
                .fetchPapersFromSupabase()


        papers =
            remotePapers


        refreshAnnotatedPapers(
            remotePapers
        )


        loading = false
    }


    // =========================================
    // FILTER OPTIONS
    // =========================================

    val courseOptions =
        listOf("All") +
                papers
                    .map {
                        it.courseCode
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .sorted()


    val categoryOptions =
        listOf("All") +
                papers
                    .map {
                        it.subjectCategory
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .sorted()


    val yearOptions =
        listOf("All") +
                papers
                    .map {
                        it.year
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .sortedDescending()


    // =========================================
    // APPLY SEARCH + FILTER
    // =========================================

    val filteredPapers =
        papers.filter { paper ->


            val matchesScope =

                selectedScope == "All Papers" ||

                        annotatedPaperIds.contains(
                            paper.id
                        )


            val matchesSearch =

                searchQuery.isBlank() ||

                        paper.courseCode.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||

                        paper.courseTitle.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||

                        paper.session.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||

                        paper.year.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||

                        paper.subjectCategory.contains(
                            searchQuery,
                            ignoreCase = true
                        )


            val matchesCourse =

                selectedCourse == "All" ||

                        paper.courseCode.equals(
                            selectedCourse,
                            ignoreCase = true
                        )


            val matchesCategory =

                selectedCategory == "All" ||

                        paper.subjectCategory.equals(
                            selectedCategory,
                            ignoreCase = true
                        )


            val matchesYear =

                selectedYear == "All" ||

                        paper.year.equals(
                            selectedYear,
                            ignoreCase = true
                        )


            matchesScope &&
                    matchesSearch &&
                    matchesCourse &&
                    matchesCategory &&
                    matchesYear
        }


    // =========================================
    // OPEN PDF VIEWER
    // =========================================

    if (viewingPaper != null) {

        val paper =
            viewingPaper!!


        PdfAnnotationViewer(

            documentTitle =
                "${paper.courseCode} - ${paper.session}",

            courseCode =
                paper.courseCode,

            contentPages =
                listOf(

                    """
                    === ${paper.courseCode}: ${paper.courseTitle} ===
                    Session: ${paper.session} | Duration: ${paper.durationMinutes} mins | Total Marks: ${paper.totalMarks}
                    
                    SECTION A: STRUCTURED QUESTIONS
                    
                    Question 1:
                    Explain the core architectural differences between Android Jetpack Compose declarative UI and traditional XML imperative layouts.
                    
                    Question 2:
                    Describe the role of State Hoisting in Jetpack Compose and how it promotes separation of concerns.
                    
                    Question 3:
                    In mobile database architecture, contrast offline-first local caching with direct cloud REST querying.
                    """.trimIndent()
                ),

            pdfUrl =
                paper.pdfUrl,

            // IMPORTANT:
            // each student's annotations now use
            // a different local-storage key.
            annotationOwnerId =
                currentUser?.id.orEmpty(),

            onAnnotationsChanged = {
                    hasAnnotations ->

                annotatedPaperIds =
                    if (hasAnnotations) {

                        annotatedPaperIds +
                                paper.id

                    } else {

                        annotatedPaperIds -
                                paper.id
                    }
            },

            onDismiss = {

                viewingPaper =
                    null

                refreshAnnotatedPapers()
            }
        )

        return
    }


    // =========================================
    // MAIN SCREEN
    // =========================================

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {


        // =====================================
        // HEADER
        // =====================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {


                Text(
                    text =
                        "Past Year Papers",

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
                        "Exam papers uploaded by lecturers",

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


            IconButton(
                onClick = {

                    showSearch =
                        !showSearch


                    if (!showSearch) {
                        searchQuery = ""
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
                            "Search Papers"
                        }
                )
            }
        }


        // =====================================
        // SEARCH
        // =====================================

        if (showSearch) {


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            OutlinedTextField(
                value =
                    searchQuery,

                onValueChange = {
                    searchQuery = it
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


        // =====================================
        // ALL / ANNOTATED
        // =====================================

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
                    Text(
                        "All Papers"
                    )
                }
            )


            FilterChip(
                selected =
                    selectedScope ==
                            "My Annotated Papers",

                onClick = {

                    refreshAnnotatedPapers()

                    selectedScope =
                        "My Annotated Papers"
                },

                label = {
                    Text(
                        "My Annotated Papers"
                    )
                }
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================
        // COURSE / CATEGORY / YEAR
        // =====================================

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

                StudentPaperFilterDropdown(
                    label =
                        "Course",

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

                StudentPaperFilterDropdown(
                    label =
                        "Category",

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

                StudentPaperFilterDropdown(
                    label =
                        "Year",

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


        // =====================================
        // RESULT HEADER
        // =====================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Text(
                text =
                    if (
                        selectedScope ==
                        "My Annotated Papers"
                    ) {

                        "My Annotated Papers"

                    } else {

                        "Past Year Papers"
                    },

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "${filteredPapers.size} found",

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


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================
        // CONTENT
        // =====================================

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
                                Icons.Default.SearchOff,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(52.dp),

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                                    .copy(
                                        alpha = 0.4f
                                    )
                        )


                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        Text(
                            text =
                                if (
                                    selectedScope ==
                                    "My Annotated Papers"
                                ) {

                                    "No annotated papers yet"

                                } else {

                                    "No past year papers found"
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        Text(
                            text =
                                if (
                                    selectedScope ==
                                    "My Annotated Papers"
                                ) {

                                    "Highlight, draw, or add a note to a paper and save it."

                                } else {

                                    "Try changing your search or filters."
                                },

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


                        StudentPastYearPaperCard(
                            paper =
                                paper,

                            isAnnotated =
                                annotatedPaperIds
                                    .contains(
                                        paper.id
                                    ),

                            onOpen = {
                                viewingPaper =
                                    paper
                            }
                        )
                    }


                    item {

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
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
private fun StudentPaperFilterDropdown(
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
                    horizontal = 9.dp
                ),

            shape =
                RoundedCornerShape(14.dp)
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
                    Modifier.size(18.dp)
            )
        }


        DropdownMenu(
            expanded =
                expanded,

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
private fun StudentPastYearPaperCard(
    paper: PastYearPaper,
    isAnnotated: Boolean,
    onOpen: () -> Unit
) {


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(
                            alpha = 0.45f
                        )
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


            // =================================
            // PDF ICON
            // =================================

            Surface(
                modifier =
                    Modifier.size(44.dp),

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
                    Modifier.width(12.dp)
            )


            // =================================
            // PAPER DETAILS
            // =================================

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
                        FontWeight.SemiBold,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(3.dp)
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
                        TextOverflow.Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "${paper.year} • " +
                                "${paper.durationMinutes} mins • " +
                                "${paper.totalMarks} marks",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                // =================================
                // ANNOTATED BADGE
                // =================================

                if (isAnnotated) {


                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
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
                            text =
                                "✓ Annotated",

                            modifier =
                                Modifier.padding(
                                    horizontal = 9.dp,
                                    vertical = 4.dp
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            fontWeight =
                                FontWeight.SemiBold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            // =================================
            // OPEN / CONTINUE
            // =================================

            Button(
                onClick =
                    onOpen,

                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {


                Text(
                    text =
                        if (isAnnotated) {
                            "Continue"
                        } else {
                            "Open"
                        }
                )
            }
        }
    }
}