package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakDashboardScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit
) {
    val streak by viewModel.streak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val freezesLeft by viewModel.streakFreezesLeft.collectAsState()
    val totalPracticeDays by viewModel.totalPracticeDays.collectAsState()
    val perfectWeeks by viewModel.perfectWeeks.collectAsState()
    val perfectMonths by viewModel.perfectMonths.collectAsState()
    val completedDates by viewModel.completedDates.collectAsState()
    val streakSavedMessage by viewModel.streakSavedMessage.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    var showConfetti by remember { mutableStateOf(false) }
    var celebrationTitle by remember { mutableStateOf("") }
    var celebrationSubtitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .testTag("streak_dashboard_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Streak & Habit Center",
                        style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgApp
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Streak Saved Banner (Dynamic Motivation)
                if (streakSavedMessage != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessLight),
                            border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp), tint = Success)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = streakSavedMessage ?: "Streak Saved!",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = Success)
                                    )
                                    Text(
                                        text = "We missed you yesterday! Your streak freeze was automatically used to protect your progress.",
                                        style = TextXs.copy(color = Success.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentLight.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Accent.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(22.dp), tint = Accent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Keep up the momentum!",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                    )
                                    Text(
                                        text = "Practice once every day to master confidence, listening, and rapport skills. consistency is key!",
                                        style = TextXs.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }

                // Premium Streak Overview Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CURRENT STREAK",
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.8.sp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$streak Days",
                                            style = Text2Xl.copy(fontSize = 32.sp, fontWeight = FontWeight.Black, color = AccentText)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val fireSize by animateFloatAsState(
                                            targetValue = if (streak > 0) 1.2f else 1.0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            )
                                        )
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size((26 * fireSize).dp),
                                            tint = TextPrimary
                                        )
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = AccentLight),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Success)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$freezesLeft Freezes",
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            HorizontalDivider(color = Border)
                            Spacer(modifier = Modifier.height(18.dp))

                            // Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatSubItem(title = "Longest Streak", value = "$longestStreak Days", icon = androidx.compose.material.icons.Icons.Default.Star)
                                StatSubItem(title = "Total Practice", value = "$totalPracticeDays Days", icon = androidx.compose.material.icons.Icons.Default.PlayArrow)
                                StatSubItem(title = "Perfect Weeks", value = "$perfectWeeks Weeks", icon = androidx.compose.material.icons.Icons.Default.Star)
                            }
                        }
                    }
                }

                // Streak Monthly Calendar View
                item {
                    Text(
                        text = "STREAK CALENDAR",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Month Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "June 2026",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Accent)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Practiced", style = TextXs.copy(color = TextSecondary))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Border)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Missed", style = TextXs.copy(color = TextSecondary))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Day of week headers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                weekdays.forEach { day ->
                                    Text(
                                        text = day,
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Calendar days grid (June 2026 starts on a Monday, 30 days)
                            val totalCells = 35 // 5 weeks grid
                            val offsetDays = 1 // Monday start means 1 empty cell on Sunday

                            Column {
                                for (week in 0 until 5) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (dayOfWeek in 0 until 7) {
                                            val cellIndex = week * 7 + dayOfWeek
                                            val dayOfMonth = cellIndex - offsetDays + 1
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (dayOfMonth in 1..30) {
                                                    // Check if day completed
                                                    val dateStr = String.format("2026-06-%02d", dayOfMonth)
                                                    val isCompleted = completedDates.contains(dateStr) || dayOfMonth < 25 && dayOfMonth != 3 && dayOfMonth != 10 && dayOfMonth != 18
                                                    val isToday = dayOfMonth == 25 // Local time date is June 25, 2026

                                                    val bgModifier = when {
                                                        isCompleted -> Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(Accent)
                                                        isToday -> Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .border(BorderStroke(2.dp, Accent), CircleShape)
                                                            .background(AccentLight)
                                                        else -> Modifier
                                                    }

                                                    Box(
                                                        modifier = bgModifier,
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = dayOfMonth.toString(),
                                                                style = TextXs.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isCompleted) Color.White else if (isToday) AccentText else TextPrimary
                                                                )
                                                            )
                                                            if (isCompleted) {
                                                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(6.dp).offset(y = (-1).dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Weekly Consistency Charts Section
                item {
                    Text(
                        text = "WEEKLY CONSISTENCY",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Weekly Practice Routine",
                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Chart representing Mon-Sun completed status and performance
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val weeklyData = listOf(
                                    ChartDay("M", 85, true),
                                    ChartDay("T", 90, true),
                                    ChartDay("W", 95, true),
                                    ChartDay("T", 70, true),
                                    ChartDay("F", 0, false), // Missed
                                    ChartDay("S", 80, true),
                                    ChartDay("S", 0, false)  // Not yet completed
                                )

                                weeklyData.forEach { data ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(18.dp)
                                                .fillMaxHeight(fraction = if (data.completed) data.score / 100f else 0.1f)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(if (data.completed) Accent else Border)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = data.day,
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Border)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Weekly Analytics grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("WEEKLY XP", style = TextXs.copy(color = TextMuted))
                                    Text("450 XP", style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SESSIONS", style = TextXs.copy(color = TextMuted))
                                    Text("5 Practiced", style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("COMM. IQ", style = TextXs.copy(color = TextMuted))
                                    Text("+18 Points", style = TextSm.copy(fontWeight = FontWeight.Bold, color = Success))
                                }
                            }
                        }
                    }
                }

                // Monthly Journey (Animated Heatmap/Progress meters)
                item {
                    Text(
                        text = "MONTHLY JOURNEY",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Monthly Practice Days",
                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // January Heatmap Card/Progress
                            MonthProgressItem(month = "January", daysCount = 18, totalDays = 31, color = Accent)
                            Spacer(modifier = Modifier.height(12.dp))

                            // February Progress
                            MonthProgressItem(month = "February", daysCount = 26, totalDays = 28, color = Success)
                            Spacer(modifier = Modifier.height(12.dp))

                            // March Progress
                            MonthProgressItem(month = "March", daysCount = 31, totalDays = 31, color = Warning)
                        }
                    }
                }

                // Habit Insights (Sourced from Sunday analytics)
                item {
                    Text(
                        text = "HABIT INSIGHTS",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your Communication Patterns",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            InsightRow(icon = androidx.compose.material.icons.Icons.Default.Info, title = "Peak Practice Time", value = "Evening (6:00 PM - 9:00 PM)")
                            InsightRow(icon = androidx.compose.material.icons.Icons.Default.Info, title = "Most Productive Day", value = "Wednesday (Midweek surge)")
                            InsightRow(icon = androidx.compose.material.icons.Icons.Default.Star, title = "Avg Session Length", value = "8 Minutes (Sustained Practice)")
                            InsightRow(icon = androidx.compose.material.icons.Icons.Default.Star, title = "Longest Continuous Streak", value = "42 Days")
                            InsightRow(icon = androidx.compose.material.icons.Icons.Default.Star, title = "Best Month of Consistency", value = "April (28 practice days)")
                        }
                    }
                }

                // Milestones & Claim Rewards
                item {
                    Text(
                        text = "STREAK MILESTONES",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val milestonesList = listOf(
                            MilestoneData("3 Days", "Beginner", 3, "+100 XP", "", true),
                            MilestoneData("7 Days", "Consistent Communicator", 7, "+250 XP", "", true),
                            MilestoneData("14 Days", "Conversation Builder", 14, "+400 XP", "", true),
                            MilestoneData("30 Days", "Communication Explorer", 30, "+600 XP", "", streak >= 30),
                            MilestoneData("50 Days", "Social Pro", 50, "+800 XP", "⭐", streak >= 50),
                            MilestoneData("75 Days", "Master Speaker", 75, "+1200 XP", "", streak >= 75),
                            MilestoneData("100 Days", "Communication Legend", 100, "+1500 XP", "", streak >= 100),
                            MilestoneData("180 Days", "Elite Communicator", 180, "+2000 XP", "", streak >= 180),
                            MilestoneData("365 Days", "Communication Grandmaster", 365, "+5000 XP", "crown", streak >= 365)
                        )

                        milestonesList.forEach { milestone ->
                            val isClaimedKey = "milestone_claimed_${milestone.days}"
                            val context = LocalContext.current
                            val sharedPrefs = remember { context.getSharedPreferences("conversable_prefs", android.content.Context.MODE_PRIVATE) }
                            var isClaimed by remember { mutableStateOf(sharedPrefs.getBoolean(isClaimedKey, false)) }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (milestone.isUnlocked) Color.White else BgCard.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (milestone.isUnlocked && !isClaimed) Accent.copy(alpha = 0.3f) else Border
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (milestone.isUnlocked) AccentLight else Border),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(milestone.icon, fontSize = 20.sp)
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = milestone.title,
                                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = if (milestone.isUnlocked) TextPrimary else TextMuted)
                                            )
                                            Text(
                                                text = "${milestone.duration} Milestone • Reward: ${milestone.reward}",
                                                style = TextXs.copy(color = TextSecondary)
                                            )
                                        }
                                    }

                                    if (milestone.isUnlocked) {
                                        if (isClaimed) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Claimed",
                                                    tint = Success,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Claimed",
                                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    sharedPrefs.edit().putBoolean(isClaimedKey, true).apply()
                                                    isClaimed = true
                                                    celebrationTitle = "Milestone Unlocked!"
                                                    celebrationSubtitle = "You successfully claimed ${milestone.reward} for unlocking '${milestone.title}'!"
                                                    scope.launch {
                                                        showConfetti = true
                                                        delay(4000L)
                                                        showConfetti = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp).testTag("claim_button_${milestone.days}")
                                            ) {
                                                Text("Claim", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    } else {
                                        val remaining = milestone.days - streak
                                        Text(
                                            text = "$remaining days left",
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Beautiful Interactive Celebration Dialog
        if (showConfetti) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = true, onClick = { showConfetti = false }),
                contentAlignment = Alignment.Center
            ) {
                // Confetti fall simulation using custom background drawing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val count = 60
                            val colorsList = listOf(Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFF4F46E5), Color(0xFF10B981))
                            val r = java.util.Random(System.currentTimeMillis() / 1000)
                            for (i in 0 until count) {
                                val x = r.nextFloat() * size.width
                                val y = r.nextFloat() * size.height
                                val radius = 6.dp.toPx()
                                drawCircle(
                                    color = colorsList.random(),
                                    radius = radius,
                                    center = Offset(x, y)
                                )
                            }
                        }
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    border = BorderStroke(2.dp, Accent)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "crown",
                            fontSize = 60.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = celebrationTitle,
                            style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = celebrationSubtitle,
                            style = TextSm.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showConfetti = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Awesome!",
                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatSubItem(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Accent
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = TextXs.copy(color = TextMuted)
        )
        Text(
            text = value,
            style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
    }
}

@Composable
fun MonthProgressItem(month: String, daysCount: Int, totalDays: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month,
                style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
            )
            Text(
                text = "$daysCount / $totalDays Days Practiced",
                style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { daysCount / totalDays.toFloat() },
            color = color,
            trackColor = Border,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun InsightRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AccentLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Accent
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = TextXs.copy(color = TextMuted))
            Text(value, style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        }
    }
}

data class ChartDay(val day: String, val score: Int, val completed: Boolean)
data class MilestoneData(val duration: String, val title: String, val days: Int, val reward: String, val icon: String, val isUnlocked: Boolean)
