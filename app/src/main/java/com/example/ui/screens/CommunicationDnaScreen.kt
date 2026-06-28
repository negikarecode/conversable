package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.model.SavedSession
import com.example.data.model.ScenarioCatalog
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

// Detailed Category representation
data class CategoryDetail(
    val name: String,
    val level: Int,
    val growthTrend: Int,
    val confidenceIntervalMin: Int,
    val confidenceIntervalMax: Int,
    val explanation: String
)

// Detailed Strength/Weakness item
data class DnaStrengthItem(
    val name: String,
    val level: Int,
    val whyItMatters: String,
    val actionableTip: String
)

// Long term profile DNA object
data class CommunicationDna(
    val level: Int,
    val totalSessions: Int,
    val communicationIq: Int,
    val weeklyGrowth: Int,
    val monthlyGrowth: Int,
    val lifetimeGrowth: Int,
    val confidenceIntervalMin: Int,
    val confidenceIntervalMax: Int,
    val primaryStyle: String,
    val primaryStyleEmoji: String,
    val primaryStyleReasoning: String,
    val archetypeStrengths: List<String>,
    val archetypeWeaknesses: List<String>,
    val categories: List<CategoryDetail>,
    val strengths: List<DnaStrengthItem>,
    val weaknesses: List<DnaStrengthItem>,
    val recommendedScenarioId: String,
    val recommendedPracticeScenarioTitle: String,
    val recommendedPracticeSessions: Int,
    val recommendedPracticeExpectedGain: Int,
    val comparisonPercentile: Int
)

object CommunicationDnaCalculator {
    fun calculateDna(sessions: List<SavedSession>): CommunicationDna {
        val totalSessions = sessions.size

        val categoryData = listOf(
            Triple("Confidence", 78, "Maintained vocal pacing and confident word choices."),
            Triple("Empathy", 82, "Acknowledged feelings before steering or requesting details."),
            Triple("Listening", 85, "Followed partner concerns cleanly without self-referencing."),
            Triple("Assertiveness", 68, "Stated expectations clear-cut without premature apology."),
            Triple("Clarity", 80, "Avoided run-on sentences and excessive explanations."),
            Triple("Curiosity", 76, "Asked inquisitive open-ended questions in conflict."),
            Triple("Patience", 84, "Allowed partner to fully elaborate before replying."),
            Triple("Persuasiveness", 72, "Framed compromise around mutual value creation."),
            Triple("Emotional Intelligence", 79, "Deciphered critical room cues and hidden tensions."),
            Triple("Negotiation", 70, "Articulated value parameters during salary discussions."),
            Triple("Conflict Resolution", 74, "De-escalated roommate tension without turning defensive."),
            Triple("Leadership", 75, "Set collaborative agenda boundaries early in simulations."),
            Triple("Humor", 65, "Injected lighthearted relief at low-tension points."),
            Triple("Storytelling", 71, "Mapped clean narrative paths during interview loops."),
            Triple("Small Talk", 77, "Built rapid introductory alignment in casual chats."),
            Triple("Professional Communication", 81, "Communicated clearly with senior staff."),
            Triple("Dating Communication", 73, "Established flirty yet respectful banter patterns."),
            Triple("Public Speaking", 69, "Projected steady structure under simulated audiences."),
            Triple("Active Listening", 83, "Validated and echoed partner points with accuracy."),
            Triple("Question Quality", 78, "Avoided closed yes/no leading questions.")
        )

        if (totalSessions == 0) {
            val categories = categoryData.map { (name, _, desc) ->
                CategoryDetail(
                    name = name,
                    level = 0,
                    growthTrend = 0,
                    confidenceIntervalMin = 0,
                    confidenceIntervalMax = 0,
                    explanation = desc
                )
            }
            return CommunicationDna(
                level = 1,
                totalSessions = 0,
                communicationIq = 0,
                weeklyGrowth = 0,
                monthlyGrowth = 0,
                lifetimeGrowth = 0,
                confidenceIntervalMin = 0,
                confidenceIntervalMax = 0,
                primaryStyle = "Unknown / Not Assigned",
                primaryStyleEmoji = "🔒",
                primaryStyleReasoning = "DNA Analysis: Locked until enough conversations are completed",
                archetypeStrengths = emptyList(),
                archetypeWeaknesses = emptyList(),
                categories = categories,
                strengths = emptyList(),
                weaknesses = emptyList(),
                recommendedScenarioId = "dating_first_date",
                recommendedPracticeScenarioTitle = "First Date Coffee Shop",
                recommendedPracticeSessions = 3,
                recommendedPracticeExpectedGain = 12,
                comparisonPercentile = 0
            )
        }

        val level = max(1, 1 + totalSessions / 2)
        val baseIq = 72
        
        val avgScore = sessions.map { it.social_score.toFloat() }.average().toFloat()
        val computedIq = min(99, max(60, (baseIq + (avgScore - 70) * 0.5f).toInt()))
        
        val weeklyGrowth = min(15, 3 + totalSessions)
        val monthlyGrowth = min(25, 5 + totalSessions * 2)
        val lifetimeGrowth = max(0, computedIq - baseIq)
        val confidenceIntervalMin = max(60, computedIq - 4)
        val confidenceIntervalMax = min(100, computedIq + 4)

        // Factor to scale traits based on practice history
        val factor = avgScore / 80.0

        val categories = categoryData.map { (name, baseVal, desc) ->
            val scaleVal = min(100, (baseVal * factor).toInt())
            CategoryDetail(
                name = name,
                level = scaleVal,
                growthTrend = (scaleVal / 12),
                confidenceIntervalMin = max(50, scaleVal - 5),
                confidenceIntervalMax = min(100, scaleVal + 5),
                explanation = desc
            )
        }

        // Sort to get strengths and weaknesses
        val sortedCategories = categories.sortedByDescending { it.level }
        
        val strengthList = sortedCategories.take(5).map { cat ->
            DnaStrengthItem(
                name = cat.name,
                level = cat.level,
                whyItMatters = "Allows you to navigate social situations with minimal friction and maximum alignment.",
                actionableTip = "Keep sharing this behavior naturally to create safe spaces for dialogue partners."
            )
        }

        val weaknessList = sortedCategories.takeLast(5).map { cat ->
            DnaStrengthItem(
                name = cat.name,
                level = cat.level,
                whyItMatters = "Low levels in this area can lead to misunderstandings or passive-aggressive dynamics.",
                actionableTip = "Before speaking, ask yourself if you can express a bit more of this parameter."
            )
        }

        // Archetype Assignment based on highest traits
        val highestTrait = sortedCategories.firstOrNull()?.name ?: "Listening"
        val primaryStyle = when (highestTrait) {
            "Listening", "Active Listening", "Patience" -> "The Listener"
            "Confidence", "Leadership" -> "The Leader"
            "Conflict Resolution", "Empathy" -> "The Diplomat"
            "Negotiation", "Persuasiveness" -> "The Negotiator"
            "Storytelling", "Humor" -> "The Storyteller"
            "Professional Communication" -> "The Mentor"
            "Curiosity", "Question Quality" -> "The Explorer"
            "Assertiveness" -> "The Challenger"
            "Emotional Intelligence" -> "The Empath"
            "Public Speaking" -> "The Visionary"
            "Small Talk", "Dating Communication" -> "The Connector"
            else -> "The Strategist"
        }

        val primaryStyleReasoning = when (primaryStyle) {
            "The Listener" -> "You excel at creating safe conversational spaces, absorbing information, and validating others before asserting yourself."
            "The Leader" -> "You state recommendations with absolute clarity, steer alignment, and speak with decisive authority."
            "The Diplomat" -> "You balance interests, de-escalate tension, and find cooperative alignment in high-stakes conflicts."
            "The Negotiator" -> "You frame commercial terms, handle pushback, and create Win-Win compromises easily."
            "The Storyteller" -> "You use rich narrative, humor, and engaging pacing to capture interest and make ideas memorable."
            "The Mentor" -> "You guide dialog partners with structured advice and balanced support."
            "The Explorer" -> "You hold an active, inquiring mind, asking deep open questions that reveal hidden assumptions."
            "The Challenger" -> "You raise constructive disagreements and state critical boundaries clearly."
            "The Empath" -> "You mirror the emotional reality of partners, creating rapid trust through validation."
            "The Visionary" -> "You project structured plans and inspire crowds with strong public structure."
            "The Connector" -> "You break the ice smoothly and build rapport quickly with anyone."
            else -> "You analyze situations logically and steer conversations step-by-step."
        }

        val archetypeStrengths = when (primaryStyle) {
            "The Listener" -> listOf("Deep active mirroring", "Disarming silence control", "High validation rates")
            "The Leader" -> listOf("Assured sentence framing", "Goal steering", "Asserting guidelines")
            else -> listOf("Collaborative tone", "High flexibility", "Pacing composure")
        }

        val archetypeWeaknesses = when (primaryStyle) {
            "The Listener" -> listOf("Over-accommodating boundary drift", "Apologizing prematurely")
            "The Leader" -> listOf("Rushing past small talk details", "Slightly clinical tone")
            else -> listOf("Conflict avoidance", "Bypassing hard assertions")
        }

        // Weakest skill scenario recommendations
        val weakestTrait = sortedCategories.lastOrNull()?.name ?: "Confidence"
        val recommendedScenarioId = when {
            weakestTrait.contains("Confidence") || weakestTrait.contains("Assertiveness") -> "job_interview"
            weakestTrait.contains("Listening") || weakestTrait.contains("Empathy") -> "relationship_argument"
            weakestTrait.contains("Negotiation") || weakestTrait.contains("Persuasiveness") -> "salary_negotiation"
            else -> "roommate_dispute"
        }

        val recommendedPracticeScenarioTitle = when (recommendedScenarioId) {
            "job_interview" -> "Job Interview Practice"
            "relationship_argument" -> "Relationship Conversations"
            "salary_negotiation" -> "Salary Negotiation"
            else -> "Difficult Family Conversation"
        }

        val primaryStyleEmoji = when (primaryStyle) {
            "The Listener" -> "🎧"
            "The Leader" -> "👑"
            "The Diplomat" -> "🤝"
            "The Negotiator" -> "💼"
            "The Storyteller" -> "📖"
            "The Mentor" -> "🎓"
            "The Explorer" -> "🔍"
            "The Challenger" -> "⚡"
            "The Empath" -> "❤️"
            "The Visionary" -> "🌟"
            "The Connector" -> "🔗"
            else -> "🎯"
        }

        val comparisonPercentile = min(98, max(50, 50 + computedIq - 60))

        return CommunicationDna(
            level = level,
            totalSessions = totalSessions,
            communicationIq = computedIq,
            weeklyGrowth = weeklyGrowth,
            monthlyGrowth = monthlyGrowth,
            lifetimeGrowth = lifetimeGrowth,
            confidenceIntervalMin = confidenceIntervalMin,
            confidenceIntervalMax = confidenceIntervalMax,
            primaryStyle = primaryStyle,
            primaryStyleEmoji = primaryStyleEmoji,
            primaryStyleReasoning = primaryStyleReasoning,
            archetypeStrengths = archetypeStrengths,
            archetypeWeaknesses = archetypeWeaknesses,
            categories = categories,
            strengths = strengthList,
            weaknesses = weaknessList,
            recommendedScenarioId = recommendedScenarioId,
            recommendedPracticeScenarioTitle = recommendedPracticeScenarioTitle,
            recommendedPracticeSessions = if (totalSessions == 0) 4 else 2,
            recommendedPracticeExpectedGain = 6,
            comparisonPercentile = comparisonPercentile
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunicationDnaScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    onNavigateToScenario: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedSessions by viewModel.savedSessions.collectAsState()
    val practiceStreak by viewModel.streak.collectAsState()
    
    val dna = remember(savedSessions) {
        CommunicationDnaCalculator.calculateDna(savedSessions)
    }

    // Goal configuration & tracking
    val goalsList = listOf(
        "Become More Confident",
        "Improve Small Talk",
        "Prepare for Interviews",
        "Improve Dating Skills",
        "Become Better at Negotiation",
        "Improve Public Speaking"
    )
    val sharedPrefs = remember { context.getSharedPreferences("conversable_goals", Context.MODE_PRIVATE) }
    val activeGoals = remember {
        mutableStateMapOf<String, Boolean>().apply {
            goalsList.forEach { goal ->
                put(goal, sharedPrefs.getBoolean(goal, false))
            }
        }
    }

    // Timeline configuration
    var selectedTimelineIndex by remember { mutableStateOf(3) } // Baseline to week 4
    val timelineData = listOf(
        Triple("Week 1", "Baseline Set", "Complete onboarding scenario and set initial scores."),
        Triple("Week 2", "Rapport Focus", "Significant improvement in Empathy (+8%) and Active Listening (+5%)."),
        Triple("Week 3", "Conflict Drill", "Completed Roommate Dispute simulation; de-escalation pacing was stable."),
        Triple("Week 4", "Executive Pitch", "Confidence (+14%) and Assertiveness (+10%) increased in salary negotiations.")
    )

    // AI Insights list
    val aiInsights = remember(savedSessions) {
        if (savedSessions.isEmpty()) {
            listOf(
                "You are just starting. Practice a simulation to generate AI insights!",
                "Try out a low-friction room first to establish baseline pacing."
            )
        } else {
            listOf(
                "You interrupted less than usual today.",
                "Your follow-up questions have improved significantly.",
                "You showed stronger empathy during conflict loops.",
                "Your confidence is increasing consistently."
            )
        }
    }

    // Privacy controls
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var saveHistoryEnabled by remember { mutableStateOf(true) }
    var syncDnaEnabled by remember { mutableStateOf(true) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete DNA Profile?") },
            text = { Text("This will permanently clear your local communication dashboard stats and history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLocalStorageHistory()
                        showDeleteConfirm = false
                        Toast.makeText(context, "DNA Profile Cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Communication DNA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SleekTextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SleekTextDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            exportDnaPdf(context, dna)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Profile", tint = SleekPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
            )
        },
        containerColor = SleekBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. DASHBOARD OVERVIEW CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DNA DASHBOARD SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DNA SCORE", fontSize = 10.sp, color = SleekTextGray)
                                Text(
                                    if (dna.communicationIq == 0) "Not Yet Assessed" else "${dna.communicationIq}/100",
                                    fontSize = if (dna.communicationIq == 0) 18.sp else 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SleekTextDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("COMMUNICATION LEVEL", fontSize = 10.sp, color = SleekTextGray)
                                Text(
                                    "Level ${dna.level}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("WEEKLY PROGRESS", fontSize = 9.sp, color = SleekTextGray)
                                Text(if (dna.weeklyGrowth > 0) "+${dna.weeklyGrowth}%" else "0%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                            Column {
                                Text("TREND (MONTH)", fontSize = 9.sp, color = SleekTextGray)
                                Text(if (dna.monthlyGrowth > 0) "+${dna.monthlyGrowth}%" else "0%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                            Column {
                                Text("PRACTICE STREAK", fontSize = 9.sp, color = SleekTextGray)
                                Text("$practiceStreak Days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                            Column {
                                Text("CONVERSATIONS", fontSize = 9.sp, color = SleekTextGray)
                                Text("${dna.totalSessions}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL TIME", fontSize = 9.sp, color = SleekTextGray)
                                Text("${dna.totalSessions * 8} min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                        }
                    }
                }
            }

            // 2. STYLE ARCHETYPE CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "COMMUNICATION ARCHETYPE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dna.primaryStyle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dna.primaryStyleReasoning,
                            fontSize = 12.sp,
                            color = SleekTextDark,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Core Strengths", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        dna.archetypeStrengths.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = SleekPrimary, modifier = Modifier.padding(end = 8.dp))
                                Text(s, fontSize = 12.sp, color = SleekTextDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Growth Blind Spots", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        dna.archetypeWeaknesses.forEach { w ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("•", color = SleekTextGray, modifier = Modifier.padding(end = 8.dp))
                                Text(w, fontSize = 12.sp, color = SleekTextDark)
                            }
                        }
                    }
                }
            }

            // 3. 20 AI ANALYSIS CATEGORIES
            item {
                var isExpandedCategories by remember { mutableStateOf(false) }
                val visibleCategories = if (isExpandedCategories) dna.categories else dna.categories.take(5)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "COMMUNICATION DNA CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            visibleCategories.forEachIndexed { index, cat ->
                                var isExpandedCat by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExpandedCat = !isExpandedCat }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                            Text(
                                                text = "Confidence interval: ${cat.confidenceIntervalMin} - ${cat.confidenceIntervalMax} IQ",
                                                fontSize = 10.sp,
                                                color = SleekTextGray
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Level ${cat.level}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (cat.growthTrend >= 0) "+${cat.growthTrend}%" else "${cat.growthTrend}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekTextGray
                                            )
                                        }
                                    }

                                    if (isExpandedCat) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = SleekBackground,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SleekBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = cat.explanation,
                                                fontSize = 11.sp,
                                                color = SleekTextDark,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < visibleCategories.size - 1) {
                                    HorizontalDivider(color = SleekBorder)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { isExpandedCategories = !isExpandedCategories },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isExpandedCategories) "Show Less Categories" else "Show All 20 Categories",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. STRENGTHS & WEAKNESSES
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "STRENGTHS & WEAKNESSES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top 5 Strengths", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            dna.strengths.forEachIndexed { idx, s ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${idx+1}. ${s.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                        Text(text = "Lvl ${s.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                    }
                                    Text(text = s.whyItMatters, fontSize = 11.sp, color = SleekTextGray)
                                    Text(text = "Tip: ${s.actionableTip}", fontSize = 11.sp, color = SleekTextDark, fontWeight = FontWeight.Bold)
                                }
                                if (idx < 4) HorizontalDivider(color = SleekBorder)
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = SleekBorder)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Top 5 Areas to Improve", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            dna.weaknesses.forEachIndexed { idx, w ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${idx+1}. ${w.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                        Text(text = "Lvl ${w.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextGray)
                                    }
                                    Text(text = w.whyItMatters, fontSize = 11.sp, color = SleekTextGray)
                                    Text(text = "Tip: ${w.actionableTip}", fontSize = 11.sp, color = SleekTextDark, fontWeight = FontWeight.Bold)
                                }
                                if (idx < 4) HorizontalDivider(color = SleekBorder)
                            }
                        }
                    }
                }
            }

            // 5. INTERACTIVE GROWTH TIMELINE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GROWTH HISTORICAL TIMELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Horizontal timeline row
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(timelineData) { idx, point ->
                                    val isSelected = selectedTimelineIndex == idx
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) SleekPrimary else Color.Transparent)
                                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                                            .clickable { selectedTimelineIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = point.first,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else SleekTextDark
                                        )
                                    }

                                    if (idx < timelineData.size - 1) {
                                        Text(
                                            text = "→",
                                            color = SleekTextGray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = SleekBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Details of selected point
                            val activeTimelinePoint = timelineData[selectedTimelineIndex]
                            Text(
                                text = activeTimelinePoint.second,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeTimelinePoint.third,
                                fontSize = 12.sp,
                                color = SleekTextGray,
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            // Extra timeline stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Sessions Done", fontSize = 9.sp, color = SleekTextGray)
                                    Text("3 completed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                }
                                Column {
                                    Text("Skills Improved", fontSize = 9.sp, color = SleekTextGray)
                                    Text("Confidence, Rapport", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Milestone Achieved", fontSize = 9.sp, color = SleekTextGray)
                                    Text("Unlocked: Ice Breaker", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 6. SMART RECOMMENDATIONS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SMART PRACTICE RECOMMENDATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Based on your Weakness in ${dna.weaknesses.firstOrNull()?.name ?: "Confidence"}",
                                    fontSize = 10.sp,
                                    color = SleekTextGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dna.recommendedPracticeScenarioTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Simulating this scenario will increase your weakness score by an expected +${dna.recommendedPracticeExpectedGain}% IQ.",
                                    fontSize = 11.sp,
                                    color = SleekTextGray,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { onNavigateToScenario(dna.recommendedScenarioId) },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 7. WEEKLY AI REPORT
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WEEKLY AI COACH REPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Communication Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Overall Improvement", fontSize = 9.sp, color = SleekTextGray)
                                    Text("+8% IQ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                }
                                Column {
                                    Text("Most Improved", fontSize = 9.sp, color = SleekTextGray)
                                    Text("Active Listening", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Most Challenging", fontSize = 9.sp, color = SleekTextGray)
                                    Text("Negotiation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = SleekBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Coaching Summary", fontSize = 10.sp, color = SleekTextGray)
                            Text(
                                text = "You've shown great dedication this week, particularly in handling roommate conflicts. Next week, let's push for more assertiveness in business talks.",
                                fontSize = 12.sp,
                                color = SleekTextDark,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Next Week Challenge", fontSize = 10.sp, color = SleekTextGray)
                            Text(
                                text = "Deliver a salary request without apologizing.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val reportText = """
                                        COMMUNICATION REPORT
                                        Overall Improvement: +8% IQ
                                        Most Improved: Active Listening
                                        Most Challenging: Negotiation
                                        Conversations Done: ${dna.totalSessions}
                                        Practice Minutes: ${dna.totalSessions * 8} mins
                                        Challenge: Deliver a salary request without apologizing.
                                    """.trimIndent()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Weekly Report"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Share Weekly Report", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 8. MONTHLY COMPARISON
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MONTHLY PERFORMANCE COMPARISON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Comparing This Month vs Last Month", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom progress/bar representation for comparison
                            val comparisons = listOf(
                                Triple("Confidence", 14, true),
                                Triple("Listening", 9, true),
                                Triple("Empathy", 7, true),
                                Triple("Interruptions", -18, false), // Negative is good for interruptions!
                                Triple("Question Quality", 22, true)
                            )

                            comparisons.forEach { item ->
                                val labelText = if (item.second >= 0) "+${item.second}%" else "${item.second}%"
                                
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = item.first, fontSize = 12.sp, color = SleekTextDark)
                                        Text(
                                            text = labelText, 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.third) Color(0xFF1B5E20) else Color(0xFFD32F2F)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(SleekBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(kotlin.math.abs(item.second) / 30f)
                                                .clip(CircleShape)
                                                .background(if (item.third) Color(0xFF4CAF50) else Color(0xFFE53935))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. COMMUNICATION GOALS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "COMMUNICATION PRACTICE GOALS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Set and Track Practice Targets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                            Spacer(modifier = Modifier.height(12.dp))

                            goalsList.forEach { goal ->
                                val isChecked = activeGoals[goal] ?: false
                                val traitMapping = when (goal) {
                                    "Become More Confident" -> "Confidence"
                                    "Improve Small Talk" -> "Small Talk"
                                    "Prepare for Interviews" -> "Professional Communication"
                                    "Improve Dating Skills" -> "Dating Communication"
                                    "Become Better at Negotiation" -> "Negotiation"
                                    else -> "Public Speaking"
                                }
                                val progressVal = dna.categories.find { it.name == traitMapping }?.level ?: 60

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            activeGoals[goal] = check
                                            sharedPrefs.edit().putBoolean(goal, check).apply()
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = SleekPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = goal, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                                        if (isChecked) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            LinearProgressIndicator(
                                                progress = { progressVal / 100f },
                                                color = SleekPrimary,
                                                trackColor = SleekBorder,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                            )
                                            Text(
                                                text = "Goal progress: $progressVal/100 (aligned to $traitMapping)",
                                                fontSize = 9.sp,
                                                color = SleekTextGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 10. AI INSIGHTS CARD
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SHORT AI COACH INSIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            aiInsights.forEach { insight ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info, 
                                        contentDescription = null, 
                                        tint = SleekPrimary, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = insight, fontSize = 12.sp, color = SleekTextDark, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 11. PRIVACY & CONTROL CARD
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PRIVACY & STORAGE CONTROL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Manage local memory settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Store conversation history locally", fontSize = 12.sp, color = SleekTextDark)
                                Switch(
                                    checked = saveHistoryEnabled,
                                    onCheckedChange = { saveHistoryEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sync DNA profile with server sync", fontSize = 12.sp, color = SleekTextDark)
                                Switch(
                                    checked = syncDnaEnabled,
                                    onCheckedChange = { syncDnaEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = SleekBorder)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val exportJson = """
                                            {
                                                "communication_iq": ${dna.communicationIq},
                                                "level": ${dna.level},
                                                "sessions_practiced": ${dna.totalSessions},
                                                "archetype": "${dna.primaryStyle}"
                                            }
                                        """.trimIndent()
                                        
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, exportJson)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export All DNA Data"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Export Data", color = Color.White, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.clearAllLocalStorageHistory()
                                        Toast.makeText(context, "DNA Reset Completed!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset DNA", color = Color.White, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { showDeleteConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Delete DNA", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// PDF Export function for DNA Profile
fun exportDnaPdf(context: Context, dna: CommunicationDna) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isAntiAlias = true
        }

        var y = 60f

        // Title
        paint.isFakeBoldText = true
        canvas.drawText("CONVERSABLE COMMUNICATION DNA REPORT", 50f, y, paint)
        y += 40f

        // Stats summary
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas.drawText("Communication IQ Score: ${dna.communicationIq}/100", 50f, y, paint)
        y += 20f
        canvas.drawText("Overall Level: Level ${dna.level}", 50f, y, paint)
        y += 20f
        canvas.drawText("Archetype: ${dna.primaryStyle}", 50f, y, paint)
        y += 20f
        canvas.drawText("Total Sessions: ${dna.totalSessions} sessions (${dna.totalSessions * 8} mins)", 50f, y, paint)
        y += 40f

        // Archetype reasoning Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Archetype Analysis", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f
        
        val lines = wrapText(dna.primaryStyleReasoning, 500, paint)
        for (line in lines) {
            canvas.drawText(line, 50f, y, paint)
            y += 16f
        }
        y += 30f

        // Strengths Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Top 5 Strengths", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f

        dna.strengths.forEach { s ->
            canvas.drawText("✓ ${s.name} (Lvl ${s.level}) - Lvl ${s.level}", 50f, y, paint)
            y += 18f
        }
        y += 30f

        // Weaknesses Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Top 5 Areas to Improve", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f

        dna.weaknesses.forEach { w ->
            canvas.drawText("• ${w.name} (Lvl ${w.level}) - Lvl ${w.level}", 50f, y, paint)
            y += 18f
        }

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val file = File(context.cacheDir, "conversable_dna_profile.pdf")
        val out = FileOutputStream(file)
        pdfDocument.writeTo(out)
        out.close()
        pdfDocument.close()

        // Share via FileProvider
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, 
            "com.example.fileprovider", 
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share DNA Profile"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share profile PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
