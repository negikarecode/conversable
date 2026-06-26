package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedSession
import com.example.data.model.SavedSessionMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    onViewDetail: (SavedSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedSessions by viewModel.savedSessions.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Constants or derived states
    val categories = listOf("All", "Dating", "Small Talk", "Networking", "Conflict Resolution")
    
    val filteredSessions = remember(savedSessions, searchQuery, selectedCategory) {
        savedSessions.filter { session ->
            val matchQuery = searchQuery.isBlank() || 
                    session.scenario_title.contains(searchQuery, ignoreCase = true) ||
                    session.partner_name.contains(searchQuery, ignoreCase = true)
            
            val matchCategory = selectedCategory == "All" || 
                    session.scenario_category.equals(selectedCategory, ignoreCase = true)
            
            matchQuery && matchCategory
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Records?") },
            text = { Text("Are you sure you want to delete all saved sessions? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLocalStorageHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Confirm Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
    ) {
        // --- HEADER ---
        TopAppBar(
            title = {
                Text(
                    text = "Session History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SleekTextDark
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("history_back_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = SleekTextDark)
                }
            },
            actions = {
                if (savedSessions.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text(text = "Clear all", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (savedSessions.isEmpty()) {
                item {
                    HistoryEmptyState(onBrowse = onBack)
                }
            } else {
                // --- STATS BAR (Part 3) ---
                item {
                    HistoryStatsBar(sessions = savedSessions)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- SKILL PROGRESS OVER TIME (Part 5) ---
                item {
                    SkillTrendsSection(sessions = savedSessions)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- FILTERS ROW (Part 3) ---
                item {
                    Text(
                        text = "Past sessions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = { Text("Search by scenario or partner...", color = SleekTextGray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SleekTextGray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SleekTextGray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface,
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedTextColor = SleekTextDark,
                            unfocusedTextColor = SleekTextDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories lazy row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_category_filters")
                    ) {
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) SleekPrimary else SleekSurface)
                                    .border(1.dp, if (isSel) SleekPrimary else SleekBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSel) Color.White else SleekTextDark
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- SESSIONS LIST ---
                if (filteredSessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No sessions match your search filters.", color = SleekTextGray)
                        }
                    }
                } else {
                    items(filteredSessions, key = { it.id }) { s ->
                        HistorySessionCard(
                            session = s,
                            onClick = { onViewDetail(s) },
                            onDelete = { viewModel.deleteLocalStorageSessionById(s.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEmptyState(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .testTag("history_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp).size(64.dp),
            tint = SleekTextGray
        )
        Text(
            text = "No Saved History Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SleekTextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete your first practice scenario and end session to analyze reports. Your achievements and skill scores will accumulate here!",
            fontSize = 13.sp,
            color = SleekTextGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBrowse,
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Browse Sessions", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryStatsBar(sessions: List<SavedSession>) {
    val totalCount = sessions.size
    val avgScore = if (totalCount > 0) sessions.map { it.social_score }.average().toInt() else 0
    val maxRapport = if (totalCount > 0) sessions.maxOf { it.rapport_final } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_stats_bar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), icon = androidx.compose.material.icons.Icons.Default.Info, title = "Sessions", value = "$totalCount")
        StatCard(modifier = Modifier.weight(1f), icon = androidx.compose.material.icons.Icons.Default.Star, title = "Avg Score", value = "$avgScore%")
        StatCard(modifier = Modifier.weight(1f), icon = androidx.compose.material.icons.Icons.Default.Star, title = "Highest", value = "$maxRapport%")
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Surface(
        modifier = modifier,
        color = SleekSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 4.dp).size(20.dp),
                tint = SleekTextDark
            )
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
            Text(title, fontSize = 10.sp, color = SleekTextGray, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun HistorySessionCard(
    session: SavedSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val avatar = ""

    // Parse date beautifully
    val dateStr = try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = isoParser.parse(session.date)
        val formatter = SimpleDateFormat("MMM d", Locale.US)
        if (date != null) formatter.format(date) else "Practice"
    } catch (e: Exception) {
        "Practice"
    }

    // Parse duration as m and s
    val min = session.duration_seconds / 60
    val sec = session.duration_seconds % 60
    val durationText = if (min > 0) "${min}m ${sec}s" else "${sec}s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_record_card_${session.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // circular Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SleekBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Text Stack
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.scenario_title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "with ${session.partner_name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SleekTextGray
                    )
                    Text(" · ", color = SleekTextGray, fontSize = 11.sp)
                    com.example.ui.theme.DifficultyPill(session.difficulty)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Info Block
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Score Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    session.social_score >= 80 -> Color(0xFF2E7D32) // Success Green
                                    session.social_score >= 60 -> Color(0xFFEF6C00) // Warning Orange
                                    else -> Color(0xFFC62828) // Danger Red
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${session.social_score}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View detail",
                        tint = SleekTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dateStr · $durationText",
                        fontSize = 10.sp,
                        color = SleekTextGray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("delete_session_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete record",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkillTrendsSection(sessions: List<SavedSession>) {
    // Collect stats
    val total = sessions.size
    if (total == 0) return

    val currentListening = sessions.take(3).map { it.skills.listening }.average()
    val currentEmpathy = sessions.take(3).map { it.skills.empathy }.average()
    val currentConfidence = sessions.take(3).map { it.skills.confidence }.average()
    val currentCollaboration = sessions.take(3).map { it.skills.collaboration }.average()

    val prevListening = if (total > 3) sessions.drop(3).take(3).map { it.skills.listening }.average() else 3.0
    val prevEmpathy = if (total > 3) sessions.drop(3).take(3).map { it.skills.empathy }.average() else 3.0
    val prevConfidence = if (total > 3) sessions.drop(3).take(3).map { it.skills.confidence }.average() else 3.0
    val prevCollaboration = if (total > 3) sessions.drop(3).take(3).map { it.skills.collaboration }.average() else 3.0

    Surface(
        color = SleekSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skill_trends_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Skill trends",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Text(
                text = "Track average skill values over your last 3 sessions compared to past baselines.",
                fontSize = 11.sp,
                color = SleekTextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkillTrendCard(
                    modifier = Modifier.weight(1f),
                    name = "Listening",
                    currentValue = currentListening,
                    prevValue = prevListening
                )
                SkillTrendCard(
                    modifier = Modifier.weight(1f),
                    name = "Empathy",
                    currentValue = currentEmpathy,
                    prevValue = prevEmpathy
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkillTrendCard(
                    modifier = Modifier.weight(1f),
                    name = "Confidence",
                    currentValue = currentConfidence,
                    prevValue = prevConfidence
                )
                SkillTrendCard(
                    modifier = Modifier.weight(1f),
                    name = "Collaboration",
                    currentValue = currentCollaboration,
                    prevValue = prevCollaboration
                )
            }
        }
    }
}

@Composable
fun SkillTrendCard(
    modifier: Modifier = Modifier,
    name: String,
    currentValue: Double,
    prevValue: Double
) {
    val delta = currentValue - prevValue
    val dirText = if (delta >= 0.05) "▲ +${String.format("%.1f", delta)}" else if (delta <= -0.05) "▼ ${String.format("%.1f", delta)}" else " Steady"
    val color = if (delta >= 0.05) Color(0xFF2E7D32) else if (delta <= -0.05) Color(0xFFC62828) else SleekTextGray

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SleekBackground)
            .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${String.format("%.1f", currentValue)}/5.0",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekTextDark
                )
                Text(
                    text = dirText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}


// =========================================================================
// PART 4 — SESSION DETAIL VIEW
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: SavedSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Transcript", "Performance", "Rapport map")

    // Formatting date beautiful
    val fullDateStr = try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = isoParser.parse(session.date)
        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)
        if (date != null) formatter.format(date) else "Unknown Date"
    } catch (e: Exception) {
        "Practice Record"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .testTag("session_detail_screen")
    ) {
        // --- HEADER ---
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = session.scenario_title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SleekTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fullDateStr,
                        fontSize = 11.sp,
                        color = SleekTextGray
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = SleekTextDark)
                }
            },
            actions = {
                // Large Score Badge
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                session.social_score >= 80 -> Color(0xFF2E7D32)
                                session.social_score >= 60 -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${session.social_score} XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
        )

        // --- TABS (Material 3) ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SleekBackground,
            contentColor = SleekPrimary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("detail_tab_$index")
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(vertical = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) SleekPrimary else SleekTextGray
                    )
                }
            }
        }

        // --- CONTENT TABS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> TranscriptTab(session = session)
                1 -> PerformanceTab(session = session)
                2 -> RapportMapTab(session = session)
            }
        }
    }
}

@Composable
fun TranscriptTab(session: SavedSession) {
    if (session.messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transcript available.", color = SleekTextGray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            items(session.messages) { msg ->
                TranscriptBubbleItem(msg = msg, partnerName = session.partner_name)
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun TranscriptBubbleItem(msg: SavedSessionMessage, partnerName: String) {
    val isUser = msg.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Speaker name label
        Text(
            text = if (isUser) "You" else partnerName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SleekTextGray,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 3.dp)
        )

        // Bubble Box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) SleekPrimary else SleekSurface)
                .border(1.dp, if (isUser) SleekPrimary else SleekBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                if (msg.was_voice) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp).padding(end = 4.dp), tint = if (isUser) Color.White.copy(alpha = 0.8f) else SleekPrimary)
                        Text(
                            text = "Voice message",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color.White.copy(alpha = 0.8f) else SleekPrimary
                        )
                    }
                }
                Text(
                    text = msg.content,
                    fontSize = 13.sp,
                    color = if (isUser) Color.White else SleekTextDark,
                    lineHeight = 18.sp
                )
            }
        }

        // Subtext (timestamp and rapport)
        val formattedTime = try {
            val s = msg.timestamp.toInt()
            val m = s / 60
            val sec = s % 60
            String.format("+%02d:%02d", m, sec)
        } catch (e: Exception) {
            "+0:00"
        }

        Text(
            text = "$formattedTime  ·  Rapport: ${msg.rapport_at_this_point}%",
            fontSize = 9.sp,
            color = SleekTextGray,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 3.dp)
        )
    }
}

@Composable
fun PerformanceTab(session: SavedSession) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // --- Circular Progress Gauge ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overall Performance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                session.social_score >= 85 -> "Elite communication! You established fantastic connection."
                                session.social_score >= 70 -> "Solid conversation! You showed great social flexibility."
                                else -> "Keep practicing! Take action on the feedback recommendations below."
                            },
                            fontSize = 11.sp,
                            color = SleekTextGray,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // Circular Gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { session.social_score / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                session.social_score >= 80 -> Color(0xFF2E7D32)
                                session.social_score >= 60 -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            },
                            strokeWidth = 8.dp,
                            trackColor = SleekBorder
                        )
                        Text(
                            text = "${session.social_score}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekTextDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Skills Breakdown ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Social Skills Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SkillProgressBar(name = "Empathy & Sensitivity", rating = session.skills.empathy)
                    Spacer(modifier = Modifier.height(10.dp))
                    SkillProgressBar(name = "Active Listening & Flow", rating = session.skills.listening)
                    Spacer(modifier = Modifier.height(10.dp))
                    SkillProgressBar(name = "Social Confidence & Assertiveness", rating = session.skills.confidence)
                    Spacer(modifier = Modifier.height(10.dp))
                    SkillProgressBar(name = "Collaboration & Rapport", rating = session.skills.collaboration)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Coach's lists ---
        item {
            Column {
                CoachBulletSection(title = "What You Did Well", items = session.strengths, iconEmoji = "check", iconColor = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(14.dp))
                CoachBulletSection(title = "Areas to Improve", items = session.improvements, iconEmoji = "warning", iconColor = Color(0xFFEF6C00))
                Spacer(modifier = Modifier.height(14.dp))
                CoachBulletSection(title = "Actionable Coach Tips", items = session.tips, iconEmoji = "info", iconColor = Color(0xFF1976D2))
            }
        }
    }
}

@Composable
fun SkillProgressBar(name: String, rating: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
            Text(
                text = "${String.format("%.1f", rating)} / 5.0",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SleekTextDark
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (rating / 5.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = SleekPrimary,
            trackColor = SleekBorder
        )
    }
}

@Composable
fun CoachBulletSection(
    title: String,
    items: List<String>,
    iconEmoji: String,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (items.isEmpty()) {
                Text(
                    text = "No detailed bullets generated for this area.",
                    fontSize = 11.sp,
                    color = SleekTextGray
                )
            } else {
                items.forEach { bullet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val imageVector = when (iconEmoji) {
                            "check" -> androidx.compose.material.icons.Icons.Default.CheckCircle
                            "warning" -> androidx.compose.material.icons.Icons.Default.Warning
                            "info" -> androidx.compose.material.icons.Icons.Default.Info
                            else -> androidx.compose.material.icons.Icons.Default.Info
                        }
                        Icon(
                            imageVector = imageVector,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp).size(12.dp),
                            tint = iconColor
                        )
                        Text(
                            text = bullet,
                            fontSize = 12.sp,
                            color = SleekTextDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RapportMapTab(session: SavedSession) {
    if (session.messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No rapport map data.", color = SleekTextGray)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dynamic Rapport Map",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Text(
                text = "Brush or tap point markers on the chart below to inspect real-time dialogue evaluations.",
                fontSize = 11.sp,
                color = SleekTextGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Canvas Line Chart
            InteractiveRapportChart(messages = session.messages, partnerName = session.partner_name)
        }
    }
}

@Composable
fun InteractiveRapportChart(messages: List<SavedSessionMessage>, partnerName: String) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }
    val gridStrokePx = with(density) { 1.dp.toPx() }
    val dotRadiusPx = with(density) { 6.dp.toPx() }
    val hoverDotRadiusPx = with(density) { 9.dp.toPx() }

    val dataPoints = messages.map { it.rapport_at_this_point.coerceIn(0, 100) }
    val sizePoints = dataPoints.size

    var selectedIndex by remember { mutableStateOf(-1) }
    var hoverOffset by remember { mutableStateOf<Offset?>(null) }

    // Chart Dimensions
    val chartHeight = 220.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The Canvas Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .background(SleekSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val stepX = width / (if (sizePoints > 1) sizePoints - 1 else 1)

                            // Find nearest node index
                            var bestIndex = -1
                            var minDistance = Float.MAX_VALUE
                            for (i in 0 until sizePoints) {
                                val x = i * stepX
                                val dist = Math.abs(offset.x - x)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    bestIndex = i
                                }
                            }

                            if (bestIndex != -1 && minDistance < 60f) {
                                selectedIndex = bestIndex
                                hoverOffset = offset
                            } else {
                                selectedIndex = -1
                                hoverOffset = null
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                val paddingX = 0f
                val paddingY = 40f

                val usableHeight = height - (2 * paddingY)
                val stepX = width / (if (sizePoints > 1) sizePoints - 1 else 1)

                // Draw Horizontal Grid lines (y = 20, 50, 80)
                val gridList = listOf(20f, 50f, 80f)
                gridList.forEach { score ->
                    val y = paddingY + usableHeight * (1f - (score / 100f))
                    drawLine(
                        color = SleekBorder,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = gridStrokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                if (sizePoints > 0) {
                    val path = Path()
                    val points = mutableListOf<Offset>()

                    for (i in 0 until sizePoints) {
                        val score = dataPoints[i]
                        val x = i * stepX
                        val y = paddingY + usableHeight * (1f - (score / 100f))
                        val offset = Offset(x, y)
                        points.add(offset)
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            // Draw smooth bezier curve or line
                            val prev = points[i - 1]
                            val contr1 = Offset(prev.x + stepX / 2f, prev.y)
                            val contr2 = Offset(x - stepX / 2f, y)
                            path.cubicTo(contr1.x, contr1.y, contr2.x, contr2.y, x, y)
                        }
                    }

                    // Fill curve underneath with a flat, clean translucent tint
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        color = Accent.copy(alpha = 0.08f)
                    )

                    // Draw main Line
                    drawPath(
                        path = path,
                        color = Accent,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Draw Point Nodes
                    for (i in 0 until sizePoints) {
                        val pt = points[i]
                        val isHovered = selectedIndex == i

                        // Draw outer circle glow
                        if (isHovered) {
                            drawCircle(
                                color = SleekPrimary.copy(alpha = 0.3f),
                                radius = hoverDotRadiusPx,
                                center = pt
                            )
                        }

                        // Draw inner point dot
                        drawCircle(
                            color = if (isHovered) Color.White else SleekPrimary,
                            radius = if (isHovered) dotRadiusPx + 1f else dotRadiusPx - 1f,
                            center = pt
                        )

                        if (isHovered) {
                            drawCircle(
                                color = SleekPrimary,
                                radius = dotRadiusPx - 2f,
                                center = pt,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tooltip presentation card
            AnimatedVisibility(
                visible = selectedIndex != -1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (selectedIndex >= 0 && selectedIndex < messages.size) {
                    val selectedMessage = messages[selectedIndex]
                    val isUsr = selectedMessage.role == "user"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Turn #${selectedIndex + 1} (${if (isUsr) "You" else partnerName})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SleekBackground)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Rapport: ${selectedMessage.rapport_at_this_point}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SleekTextDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${selectedMessage.content}\"",
                                fontSize = 12.sp,
                                color = SleekTextDark,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            if (selectedIndex == -1) {
                Text(
                    text = "Tap any dot coordinate on the graph line to inspect dialog performance.",
                    fontSize = 10.sp,
                    color = SleekTextGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
