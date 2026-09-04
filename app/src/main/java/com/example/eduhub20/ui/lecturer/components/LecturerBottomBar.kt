package com.example.eduhub20.ui.lecturer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.eduhub20.ui.theme.EduHubPrimary

data class LecturerBottomItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun LecturerBottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val items = listOf(
        LecturerBottomItem(
            title = "Home",
            icon = Icons.Default.Home
        ),

        LecturerBottomItem(
            title = "Courses",
            icon = Icons.Default.School
        ),

        LecturerBottomItem(
            title = "Past Papers",
            icon = Icons.Default.Description
        ),

        LecturerBottomItem(
            title = "Profile",
            icon = Icons.Default.Person
        )
    )


    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {

        items.forEachIndexed { index, item ->

            NavigationBarItem(
                selected = selectedItem == index,

                onClick = {
                    onItemSelected(index)
                },

                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },

                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
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