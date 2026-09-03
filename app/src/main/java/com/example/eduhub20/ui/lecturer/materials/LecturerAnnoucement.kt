package com.example.eduhub20.ui.lecturer.materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.repository.CourseRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LecturerAnnouncementsTab(
    courseId: String,
    canManage: Boolean,
    onAddAnnouncement: () -> Unit
) {

    var announcements by remember(courseId) {
        mutableStateOf<List<Announcement>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(courseId) {

        loading = true

        announcements =
            CourseRepository
                .fetchAnnouncementsFromSupabase(
                    courseId
                )
                .sortedByDescending {
                    it.date.toLongOrNull() ?: 0L
                }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (canManage) {

            Button(
                onClick = onAddAnnouncement,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("+ Add Announcement")
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

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
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "No announcements yet",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            else -> {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = announcements,
                        key = { it.id }
                    ) { announcement ->

                        AnnouncementCard(
                            announcement =
                                announcement
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.Top
        ) {

            Icon(
                imageVector =
                    Icons.Default.Campaign,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = announcement.title,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = announcement.content,
                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text =
                        announcement.lecturerName,
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        formatAnnouncementDate(
                            announcement.date
                        ),
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAnnouncementDate(
    value: String
): String {

    val millis =
        value.toLongOrNull()
            ?: return value

    return SimpleDateFormat(
        "dd MMM yyyy, h:mm a",
        Locale.getDefault()
    ).format(Date(millis))
}