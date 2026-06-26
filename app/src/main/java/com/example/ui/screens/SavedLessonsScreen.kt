package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedLesson
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLessonsScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedLessons by viewModel.savedLessons.collectAsState()
    
    // Category tabs
    val categories = listOf("All Insights", "My Biggest Mistakes", "My Best Replies", "Best Conversation Openers")
    var selectedCategory by remember { mutableStateOf("All Insights") }
    
    // Filtering lessons
    val filteredLessons = remember(savedLessons, selectedCategory) {
        if (selectedCategory == "All Insights") {
            savedLessons.sortedByDescending { it.timestamp }
        } else {
            savedLessons.filter { it.category == selectedCategory }.sortedByDescending { it.timestamp }
        }
    }

    var lessonToDelete by remember { mutableStateOf<SavedLesson?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Lessons",
                        style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.testTag("saved_lessons_title")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header stats panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Color(0xFFE5E5EA))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "COMMUNICATION HANDBOOK",
                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText, letterSpacing = 0.5.sp)
                        )
                        Text(
                            text = "${savedLessons.size} insights curated",
                            style = TextLg.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                        )
                        Text(
                            text = "These insights were bookmarked during AI Replays to accelerate your learning.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Category filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) Accent else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Accent else Color(0xFFE5E5EA),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_chip_${cat.replace(" ", "_")}")
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            // Bookmarked Lessons List
            if (filteredLessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(
                            text = "No saved lessons in this category",
                            style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Bookmark learning moments when reviewing your AI Replays to populate your handbook.",
                            style = TextSm.copy(color = TextMuted),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLessons) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            onDeleteClick = { lessonToDelete = lesson }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    lessonToDelete?.let { lesson ->
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = {
                Text(
                    text = "Delete Saved Lesson",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this bookmarked insight from your learning handbook?",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLesson(lesson.id)
                        lessonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun LessonCard(
    lesson: SavedLesson,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(lesson.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
            sdf.format(Date(lesson.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_lesson_card_${lesson.id}"),
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Color(0xFFE5E5EA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category badge and action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = when (lesson.category) {
                        "My Biggest Mistakes" -> Color(0xFFFEE2E2)
                        "My Best Replies" -> Color(0xFFDCFCE7)
                        else -> Color(0xFFFEF9C3)
                    }
                ) {
                    Text(
                        text = lesson.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (lesson.category) {
                            "My Biggest Mistakes" -> Color(0xFFDC2626)
                            "My Best Replies" -> Color(0xFF16A34A)
                            else -> Color(0xFFCA8A04)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Lesson",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scenario details
            Text(
                text = "${lesson.scenarioTitle} • ${lesson.partnerName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentText
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            // Original response
            Surface(
                color = Color(0xFFFAF9F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Original Response",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "\"${lesson.originalMessage}\"",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expert recommendation
            Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Expert Suggested Response",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "\"${lesson.expertMessage}\"",
                        fontSize = 13.sp,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coach Tactical analysis
            Column {
                Text(
                    text = "Coach Pivot Insight:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = lesson.reasoning,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF2F2F7))
            Spacer(modifier = Modifier.height(6.dp))

            // Footer Timestamp
            Text(
                text = "Saved $dateStr",
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
