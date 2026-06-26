package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ConversableViewModel
import com.example.ui.theme.*

data class ProfileBadge(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ConversableViewModel,
    username: String,
    email: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedSessions by viewModel.savedSessions.collectAsState()
    val unlockedBadgeIds by viewModel.unlockedBadgeIds.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var selectedBadgeForDetail by remember { mutableStateOf<ProfileBadge?>(null) }

    val userLevel = getLevelForXp(totalXp)

    // Compute stats
    val totalSessions = savedSessions.size
    val avgScore = if (totalSessions > 0) "${savedSessions.map { it.social_score }.average().toInt()}%" else "--"
    val bestScore = if (totalSessions > 0) "${savedSessions.maxOf { it.social_score }}" else "0"
    
    val totalSeconds = savedSessions.sumOf { it.duration_seconds }
    val totalTimeStr = "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"

    // Skills averages
    val empathyAvg = if (totalSessions > 0) savedSessions.map { it.skills.empathy }.average() else 0.0
    val listeningAvg = if (totalSessions > 0) savedSessions.map { it.skills.listening }.average() else 0.0
    val confidenceAvg = if (totalSessions > 0) savedSessions.map { it.skills.confidence }.average() else 0.0
    val collaborationAvg = if (totalSessions > 0) savedSessions.map { it.skills.collaboration }.average() else 0.0

    val allBadgesList = remember {
        listOf(
            ProfileBadge("First Blood", "First Blood", "Completed your first ever social skills training session!", "A", Color(0xFFEF4444)),
            ProfileBadge("Date Whisperer", "Date Whisperer", "Scored 80+ in a Dating and Romance scenario.", "B", Color(0xFFEC4899)),
            ProfileBadge("Boardroom Boss", "Boardroom Boss", "Scored 80+ in a Professional and Career scenario.", "C", Color(0xFF3B82F6)),
            ProfileBadge("Peace Keeper", "Peace Keeper", "Scored 80+ in a Conflict Resolution scenario.", "D", Color(0xFF14B8A6)),
            ProfileBadge("Networker", "Networker", "Successfully completed 3 or more Networking scenarios.", "E", Color(0xFF10B981)),
            ProfileBadge("Perfect Ten", "Perfect Ten", "Achieved a perfect score of 100 in a session!", "F", Color(0xFFF59E0B)),
            ProfileBadge("Week Warrior", "Week Warrior", "Earned by reaching a 7-day practice streak.", "G", Color(0xFFF59E0B)),
            ProfileBadge("Month Legend", "Month Legend", "Earned by reaching an elite 30-day practice streak.", "crown", Color(0xFF8B5CF6)),
            ProfileBadge("Variety Pack", "Variety Pack", "Completed scenarios across 4 different social categories.", "I", Color(0xFF6366F1)),
            ProfileBadge("Speed Demon", "Speed Demon", "Completed a social scenario in under 3 minutes.", "J", Color(0xFFEF4444)),
            ProfileBadge("Deep Diver", "Deep Diver", "Sustained a deep conversation with 20+ turns.", "K", Color(0xFF0EA5E9)),
            ProfileBadge("Hard Mode Hero", "Hard Mode Hero", "Won a Hard difficulty scenario with a score of 75+.", "shield", Color(0xFF7C3AED)),
            ProfileBadge("Comeback Kid", "Comeback Kid", "Bounced back to score 70+ after a previous low score.", "M", Color(0xFFF97316))
        )
    }

    val unlockedCount = allBadgesList.count { unlockedBadgeIds.contains(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Placeholder box to balance the navigationIcon for centered text
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard),
            )
        },
        containerColor = SleekBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Section
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (username.isNotEmpty()) username.trim().take(1).uppercase() else "U",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.W600, color = Color.White)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = username.ifEmpty { "Aryan" },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Level $userLevel",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.testTag("reset_progress_button")
            ) {
                Text(
                    text = "Reset all progress",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileStatCard(modifier = Modifier.weight(1f), title = "Sessions", value = "$totalSessions")
                ProfileStatCard(modifier = Modifier.weight(1f), title = "Avg Score", value = avgScore)
                ProfileStatCard(modifier = Modifier.weight(1f), title = "Best Score", value = bestScore)
                ProfileStatCard(modifier = Modifier.weight(1f), title = "Total Time", value = totalTimeStr)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skills Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Your skills",
                    style = TextXs.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.06.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileSkillProgress(title = "Heart & Empathy", score = empathyAvg, color = Color(0xFFEC4899))
                        ProfileSkillProgress(title = "Active Listening", score = listeningAvg, color = Color(0xFF3B82F6))
                        ProfileSkillProgress(title = "Confidence", score = confidenceAvg, color = Color(0xFFF59E0B))
                        ProfileSkillProgress(title = "Collaboration", score = collaborationAvg, color = Color(0xFF10B981))
                        
                        Text(
                            text = if (totalSessions == 1) "Based on 1 session" else "Based on $totalSessions sessions",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Badges Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Badges ($unlockedCount/13)",
                    style = TextXs.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.06.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth()
                    ) {
                        // Vertical column-based flow mimicking a Grid
                        val chunkedBadges = allBadgesList.chunked(4)
                        chunkedBadges.forEach { rowBadges ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBadges.forEach { badge ->
                                    val isUnlocked = unlockedBadgeIds.contains(badge.id)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .bounceClick()
                                            .clickable { selectedBadgeForDetail = badge }
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isUnlocked) AccentLight else Color(0xFFF1F5F9))
                                                    .border(BorderStroke(1.dp, if (isUnlocked) Accent.copy(alpha = 0.3f) else Color(0xFFE2E8F0)), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BadgeSvgIcon(
                                                    badgeId = badge.id,
                                                    color = if (isUnlocked) Accent else TextMuted,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .align(Alignment.Center)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = badge.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isUnlocked) TextPrimary else TextMuted,
                                                maxLines = 2,
                                                lineHeight = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                // Fill missing items to keep Row spacing perfectly balanced
                                if (rowBadges.size < 4) {
                                    for (i in 0 until (4 - rowBadges.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Language Settings Option
            val selectedLanguageState by viewModel.selectedLanguage.collectAsState()
            val isHinglishModeEnabled by viewModel.isHinglishModeEnabled.collectAsState()
            val isCorrectMeEnabled by viewModel.isCorrectMeEnabled.collectAsState()
            val activeLangObj = remember(selectedLanguageState) {
                com.example.data.model.LanguageCatalog.languages.find { it.name.equals(selectedLanguageState, ignoreCase = true) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onLanguageClick() }
                    .testTag("profile_language_settings_button"),
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = activeLangObj?.flag ?: "GL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Language & Culture Settings",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val modes = mutableListOf(selectedLanguageState)
                        if (isCorrectMeEnabled) modes.add("Correct Me")
                        if (isHinglishModeEnabled) modes.add("Hinglish")
                        Text(
                            text = "Active: " + modes.joinToString(", "),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out Button
            Button(
                onClick = { showSignOutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2FE)),
                border = BorderStroke(1.dp, Color(0xFFFCE7FC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Text(
                    text = "Sign Out",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Badge details modal sheet/dialog
    selectedBadgeForDetail?.let { badge ->
        val isUnlocked = unlockedBadgeIds.contains(badge.id)
        AlertDialog(
            onDismissRequest = { selectedBadgeForDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedBadgeForDetail = null }) {
                    Text("OK", color = SleekPrimary)
                }
            },
            containerColor = BgCard,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) AccentLight else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    BadgeSvgIcon(
                        badgeId = badge.id,
                        color = if (isUnlocked) Accent else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = badge.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = if (isUnlocked) badge.description else "Locked: ${badge.description}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reset Progress confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset all progress?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently wipe all practice sessions, levels, XP, and earned badges. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllProgress()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset everything", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Sign Out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign out of Convertible?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your account?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Sign Out", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProfileStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier.height(72.dp),
        color = BgCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProfileSkillProgress(
    title: String,
    score: Double, // Double representing value out of 5.0
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1f", score),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((score.toFloat() / 5.0f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun BadgeSvgIcon(badgeId: String, color: Color, modifier: Modifier = Modifier) {
    val pathData = when (badgeId) {
        "First Blood" -> "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"
        "Date Whisperer", "Empath" -> "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        "Speed Demon" -> "M13 2 L3 14 L12 14 L11 22 L21 10 L12 10 Z"
        "Week Warrior" -> "M3 4 h18 v18 H3 Z M16 2 v4 M8 2 v4 M3 10 h18"
        "Deep Diver", "Daily Grinder" -> "M12 2 A10 10 0 1 0 22 12 A10 10 0 0 0 12 2 Z M12 6 v6 l4 2"
        "Hard Mode Hero" -> "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"
        "Perfect Ten", "Month Legend", "Boardroom Boss", "Peace Keeper" -> "M12 2 L15.09 8.26 L22 9.27 L17 14.14 L18.18 21.02 L12 17.77 L5.82 21.02 L7 14.14 L2 9.27 L8.91 8.26 Z"
        "Networker" -> "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11 a4 4 0 1 0 0 -8 a4 4 0 0 0 0 8 M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"
        "Variety Pack" -> "M3 3 h7 v7 H3 Z M14 3 h7 v7 h-7 Z M14 14 h7 v7 h-7 Z M3 14 h7 v7 H3 Z"
        "Comeback Kid" -> "M23 4 v6 h-6 M20.49 15a9 9 0 1 1-2.12-9.36 L23 10"
        "Listener" -> "M3 18v-6a9 9 0 0 1 18 0v6 M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3 Z M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3 Z"
        else -> "M12 2 L15.09 8.26 L22 9.27 L17 14.14 L18.18 21.02 L12 17.77 L5.82 21.02 L7 14.14 L2 9.27 L8.91 8.26 Z"
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        try {
            val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString(pathData).toPath()
            
            val scaleX = size.width / 24f
            val scaleY = size.height / 24f
            
            drawContext.canvas.save()
            drawContext.canvas.scale(scaleX, scaleY)
            
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.0f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            drawContext.canvas.restore()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
