package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeCatalog
import com.example.data.model.CompletedDailyChallenge
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengesScreen(
    viewModel: ConversableViewModel,
    onNavigateToIntro: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val challenge by viewModel.currentDailyChallenge.collectAsState()
    val completedHistory by viewModel.completedDailyChallengesList.collectAsState()
    val streak by viewModel.streak.collectAsState()

    var activeTab by remember { mutableStateOf("challenges") } // "challenges", "analytics", "history"

    // Parse completions for current week
    val currentWeekCompletions = remember(completedHistory) {
        val completions = BooleanArray(7) // Mon-Sun
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Find Monday of current week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = calendar.time
        calendar.add(Calendar.DATE, 6)
        val endOfWeek = calendar.time

        completedHistory.forEach { ch ->
            try {
                val date = sdf.parse(ch.date)
                if (date != null && !date.before(startOfWeek) && !date.after(endOfWeek)) {
                    val compCal = Calendar.getInstance().apply { time = date }
                    // Map Sunday (1) to index 6, Monday (2) to 0, etc.
                    val dayOfWeek = compCal.get(Calendar.DAY_OF_WEEK)
                    val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                    if (index in 0..6) {
                        completions[index] = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        completions
    }

    val weekCompletedCount = currentWeekCompletions.count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Daily Challenges Hub",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgApp)
            )
        },
        containerColor = BgApp,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Elegant Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgInput)
                    .padding(4.dp)
            ) {
                listOf(
                    "challenges" to "Today",
                    "analytics" to "Stats",
                    "history" to "History"
                ).forEach { (tabId, label) ->
                    val isSelected = activeTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BgCard else Color.Transparent)
                            .clickable { activeTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Accent else TextSecondary
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "tab_navigation"
            ) { targetTab ->
                when (targetTab) {
                    "challenges" -> {
                        TodayChallengesTab(
                            viewModel = viewModel,
                            challenge = challenge,
                            streak = streak,
                            currentWeekCompletions = currentWeekCompletions,
                            weekCompletedCount = weekCompletedCount,
                            completedHistory = completedHistory,
                            onNavigateToIntro = onNavigateToIntro
                        )
                    }
                    "analytics" -> {
                        ChallengeAnalyticsTab(completedHistory = completedHistory, streak = streak)
                    }
                    "history" -> {
                        ChallengeHistoryTab(completedHistory = completedHistory)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayChallengesTab(
    viewModel: ConversableViewModel,
    challenge: DailyChallenge?,
    streak: Int,
    currentWeekCompletions: BooleanArray,
    weekCompletedCount: Int,
    completedHistory: List<CompletedDailyChallenge>,
    onNavigateToIntro: () -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val isTodayCompleted = remember(completedHistory) {
        completedHistory.any { it.date == todayStr }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: WEEKLY THEME STATUS ROW
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WEEKLY ROADMAP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        val themes = listOf("Professional", "Social", "Dating", "Leadership", "Conflict", "Fun", "Surprise")
                        val calendar = Calendar.getInstance()
                        val currentDayIdx = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else calendar.get(Calendar.DAY_OF_WEEK) - 2

                        days.forEachIndexed { idx, day ->
                            val isCompleted = currentWeekCompletions[idx]
                            val isToday = idx == currentDayIdx

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCompleted -> Success
                                                isToday -> Accent
                                                else -> BgInput
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = day,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToday) Color.White else TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isToday) "Today" else themes[idx].take(3),
                                    fontSize = 10.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Accent else TextSecondary
                                )
                            }
                        }
                    }

                    if (weekCompletedCount == 7) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Success.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Weekly Champion Unlocked!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Success)
                                    Text("Finished all 7 challenges this week. +500 XP bonus awarded!", fontSize = 12.sp, color = Success.copy(alpha = 0.8f))
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { weekCompletedCount / 7f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Success,
                            trackColor = Border
                        )
                        Text(
                            text = "$weekCompletedCount of 7 completed. Complete all 7 for Weekly Champion Badge!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        // SECTION 2: TODAY'S CHALLENGE PREMIUM CARD
        item {
            if (challenge == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DAILY CHALLENGE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Danger,
                                    letterSpacing = 1.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Accent.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    challenge.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = challenge.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = challenge.description,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Difficulty Stars, Time, and Reward Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("DIFFICULTY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    val starCount = when (challenge.difficulty) {
                                        "Easy" -> 1
                                        "Medium" -> 2
                                        "Hard" -> 3
                                        else -> 5
                                    }
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = if (i <= starCount) Warning else Border,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text("ESTIMATED TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(
                                    "${challenge.estimatedTimeMinutes} Min",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("XP REWARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(
                                    "+${challenge.xpReward} XP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Success,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isTodayCompleted) {
                            Button(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Success.copy(alpha = 0.15f),
                                    disabledContentColor = Success
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Completed")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Challenge Completed Today!", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = onNavigateToIntro,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("start_daily_challenge_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("▶ Start Challenge", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: MONTHLY CHALLENGE PASS CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY CHALLENGE PASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Warning.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("SEASON PASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Warning)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Progress ring diagram using circular progress
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(56.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { completedHistory.size / 30f },
                                modifier = Modifier.fillMaxSize(),
                                color = Warning,
                                strokeWidth = 5.dp,
                                trackColor = Border
                            )
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "June Legendary Pass",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val remaining = (30 - completedHistory.size).coerceAtLeast(0)
                            Text(
                                text = "${completedHistory.size} / 30 Completed ($remaining more for Legendary Badge)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // SECTION 4: FUTURE SEASONAL EVENTS TEASER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Summer Communication Season 2026",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                        Text(
                            "Special events unlock on July 1st. Maintain your streak to earn exclusive, limited-edition profile rewards and themes!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeAnalyticsTab(completedHistory: List<CompletedDailyChallenge>, streak: Int) {
    if (completedHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No challenge data available yet.",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    "Complete your first daily challenge to unlock advanced analytics and learning curve diagrams!",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics Highlights Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Challenges
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                "${completedHistory.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Average Score
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AVG SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            val avg = completedHistory.map { it.score }.average()
                            Text(
                                String.format("%.1f%%", avg),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Success,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Longest Streak
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("STREAK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Danger
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$streak Days",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Total XP
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TOTAL XP EARNED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            val totalXp = completedHistory.sumOf { it.xpEarned }
                            Text(
                                "+$totalXp XP",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Category breakdown card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "STRENGTH ANALYSIS BY CATEGORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val categories = listOf("Professional", "Social", "Dating", "Conflict", "Emotional Intelligence", "Fun")
                        val scores = listOf(84, 76, 80, 68, 88, 72) // Mock strengths for radar feel
                        val colors = listOf(Accent, Success, Danger, Warning, AccentText, SuccessLight)

                        categories.forEachIndexed { idx, category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    category,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.width(120.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { scores[idx] / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = colors[idx % colors.size],
                                    trackColor = Border
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${scores[idx]}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeHistoryTab(completedHistory: List<CompletedDailyChallenge>) {
    if (completedHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No challenge history yet.",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    "Completed challenges will appear here as a complete learning roadmap archive.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(completedHistory.reversed()) { record ->
                val matchingCatalogItem = DailyChallengeCatalog.challenges.find { it.id == record.id }
                val title = matchingCatalogItem?.title ?: "Unknown Challenge"
                val category = matchingCatalogItem?.category ?: "Social"
                val avatar = "P"

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Category: $category • ${record.date}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${record.score}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Success
                            )
                            Text(
                                text = "+${record.xpEarned} XP",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
