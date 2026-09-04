package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.repository.CourseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun LecturerAnnouncementsSection(
    courseId: String,
    canManage: Boolean,
    onAddAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scope = rememberCoroutineScope()

    var announcements by remember(courseId) {
        mutableStateOf(
            CourseRepository.getAnnouncementsForCourse(courseId)
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var announcementToEdit by remember {
        mutableStateOf<Announcement?>(null)
    }

    var announcementToDelete by remember {
        mutableStateOf<Announcement?>(null)
    }


    // =========================================
    // REFRESH ANNOUNCEMENTS
    // =========================================

    fun refreshAnnouncements() {

        announcements =
            CourseRepository.getAnnouncementsForCourse(
                courseId
            )
    }


    // =========================================
    // LOAD FROM SUPABASE
    // =========================================

    LaunchedEffect(courseId) {

        loading = true

        announcements =
            CourseRepository.fetchAnnouncementsFromSupabase(
                courseId
            )

        loading = false
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        // =====================================
        // HEADER
        // =====================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Announcements",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = if (canManage) {
                        "Manage announcements for this course"
                    } else {
                        "Course announcements"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            // Only MANAGE mode can create
            if (canManage) {

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Button(
                    onClick = onAddAnnouncement,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = "Add",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =====================================
        // CONTENT
        // =====================================

        when {

            loading -> {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }


            announcements.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Text(
                                text = "No announcements yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = if (canManage) {
                                    "Tap Add to publish an announcement."
                                } else {
                                    "Announcements from your lecturer will appear here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }


            else -> {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = announcements,
                        key = { it.id }
                    ) { announcement ->

                        LecturerAnnouncementCard(
                            announcement =
                                announcement,

                            canManage =
                                canManage,

                            onEdit = {
                                announcementToEdit =
                                    announcement
                            },

                            onDelete = {
                                announcementToDelete =
                                    announcement
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


    // =========================================
    // EDIT DIALOG
    // =========================================

    announcementToEdit?.let { announcement ->

        EditAnnouncementDialog(
            announcement = announcement,

            onDismiss = {
                announcementToEdit = null
            },

            onSave = { updatedAnnouncement ->

                scope.launch {

                    CourseRepository.updateAnnouncement(
                        updatedAnnouncement
                    )

                    refreshAnnouncements()

                    announcementToEdit =
                        null
                }
            }
        )
    }


    // =========================================
    // DELETE CONFIRMATION
    // =========================================

    announcementToDelete?.let { announcement ->

        AlertDialog(
            onDismissRequest = {
                announcementToDelete = null
            },

            title = {
                Text(
                    text = "Delete Announcement?"
                )
            },

            text = {

                Text(
                    text =
                        "Are you sure you want to delete " +
                                "\"${announcement.title}\"?\n\n" +
                                "This action cannot be undone."
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        scope.launch {

                            CourseRepository
                                .deleteAnnouncement(
                                    announcement.id
                                )

                            refreshAnnouncements()

                            announcementToDelete =
                                null
                        }
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        announcementToDelete = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}


// =====================================================
// ANNOUNCEMENT CARD
// =====================================================

@Composable
private fun LecturerAnnouncementCard(
    announcement: Announcement,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }


    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // =================================
            // TITLE + MENU
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "${announcement.lecturerName} • " +
                                    formatAnnouncementDate(
                                        announcement.date
                                    ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                // Only MANAGE mode has the menu
                if (canManage) {

                    Box {

                        IconButton(
                            onClick = {
                                menuExpanded = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Announcement options"
                            )
                        }


                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Edit")
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },

                                onClick = {

                                    menuExpanded = false

                                    onEdit()
                                }
                            )


                            DropdownMenuItem(
                                text = {

                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },

                                onClick = {

                                    menuExpanded = false

                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =================================
            // CONTENT
            // =================================

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


// =====================================================
// EDIT ANNOUNCEMENT
// =====================================================

@Composable
private fun EditAnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onSave: (Announcement) -> Unit
) {

    var title by remember(announcement.id) {
        mutableStateOf(
            announcement.title
        )
    }

    var content by remember(announcement.id) {
        mutableStateOf(
            announcement.content
        )
    }

    var saving by remember {
        mutableStateOf(false)
    }


    AlertDialog(
        onDismissRequest = {

            if (!saving) {
                onDismiss()
            }
        },

        title = {
            Text(
                text =
                    "Edit Announcement",
                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Title")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                OutlinedTextField(
                    value =
                        content,
                    onValueChange = {
                        content = it
                    },
                    label = {
                        Text("Message")
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                )
            }
        },

        confirmButton = {

            Button(
                enabled =
                    title.isNotBlank() &&
                            content.isNotBlank() &&
                            !saving,

                onClick = {

                    saving = true

                    onSave(
                        announcement.copy(
                            title =
                                title.trim(),
                            content =
                                content.trim()
                        )
                    )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Edit,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text(
                    if (saving) {
                        "Saving..."
                    } else {
                        "Save Changes"
                    }
                )
            }
        },

        dismissButton = {

            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}


// =====================================================
// DATE FORMATTER
// =====================================================

private fun formatAnnouncementDate(
    value: String
): String {

    return try {

        val milliseconds =
            value.toLong()

        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(
            Date(milliseconds)
        )

    } catch (_: Exception) {

        value
    }
}