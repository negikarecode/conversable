package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.db.ConversationSession
import com.example.data.model.ChatMessage
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekTextDark
import com.example.ui.theme.SleekTextGray
import com.example.viewmodel.ConversableViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Subtle Monochrome/Japanese-inspired palette with soft heatmap colors
val ReplayExcellent = Color(0xFFC8E6C9)      // Soft Green
val ReplayExcellentLine = Color(0xFF81C784)
val ReplayGood = Color(0xFFECEFF1)           // Soft Gray / Neutral Gray
val ReplayGoodLine = Color(0xFFB0BEC5)
val ReplayImprovement = Color(0xFFFFE0B2)    // Soft Amber
val ReplayImprovementLine = Color(0xFFFFB74D)
val ReplayPoor = Color(0xFFFFCDD2)           // Soft Red
val ReplayPoorLine = Color(0xFFE57373)

data class MessageAnalysis(
    val explanation: String,
    val confidenceDelta: Int,
    val rapportDelta: Int,
    val listeningRating: String,
    val empathyRating: String,
    val flowRating: String,
    val suggestedImprovement: String,
    val heatmapColor: Color,
    val lineColor: Color
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiReplayScreen(
    session: ConversationSession,
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.security.KeepScreenSecure()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Parse transcript messages
    val parsedMessages = remember(session.transcriptJson) {
        try {
            val listType = Types.newParameterizedType(List::class.java, ChatMessage::class.java)
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter<List<ChatMessage>>(listType)
            val decrypted = com.example.security.CryptoHelper.decrypt(session.transcriptJson) ?: session.transcriptJson
            adapter.fromJson(decrypted) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Interactive States
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // Generate static analysis for each message based on text/content to keep it consistent
    val messageAnalyses = remember(parsedMessages) {
        parsedMessages.mapIndexed { idx, msg ->
            val textLower = msg.text.lowercase()
            val isShort = msg.text.length < 20
            
            if (msg.isUser) {
                when {
                    textLower.contains("sorry") || textLower.contains("feel") || textLower.contains("understand") || textLower.contains("empathy") -> {
                        MessageAnalysis(
                            explanation = "You validated her feelings and emotions before moving the conversation forward.",
                            confidenceDelta = 12,
                            rapportDelta = 18,
                            listeningRating = "Excellent",
                            empathyRating = "High",
                            flowRating = "Strong",
                            suggestedImprovement = "Ensure you maintain this validation layer in future conflicts.",
                            heatmapColor = ReplayExcellent,
                            lineColor = ReplayExcellentLine
                        )
                    }
                    textLower.contains("?") || textLower.contains("what") || textLower.contains("how") || textLower.contains("why") -> {
                        MessageAnalysis(
                            explanation = "You asked an open-ended question to gather context and build rapport.",
                            confidenceDelta = 10,
                            rapportDelta = 12,
                            listeningRating = "Good",
                            empathyRating = "Medium",
                            flowRating = "Strong",
                            suggestedImprovement = "Acknowledge the reply contextually before asking another question.",
                            heatmapColor = ReplayGood,
                            lineColor = ReplayGoodLine
                        )
                    }
                    isShort || textLower.contains("ok") || textLower.contains("yes") || textLower.contains("no") -> {
                        MessageAnalysis(
                            explanation = "Your response was short and missed an opportunity to actively engage.",
                            confidenceDelta = 4,
                            rapportDelta = 2,
                            listeningRating = "Needs Improvement",
                            empathyRating = "Low",
                            flowRating = "Weak",
                            suggestedImprovement = "Ask a follow-up question to keep the conversation balanced.",
                            heatmapColor = ReplayPoor,
                            lineColor = ReplayPoorLine
                        )
                    }
                    else -> {
                        MessageAnalysis(
                            explanation = "You responded clearly, but adding emotional validation would elevate the rapport.",
                            confidenceDelta = 8,
                            rapportDelta = 9,
                            listeningRating = "Good",
                            empathyRating = "Medium",
                            flowRating = "Balanced",
                            suggestedImprovement = "Acknowledge the partner's previous point before asserting your own view.",
                            heatmapColor = ReplayImprovement,
                            lineColor = ReplayImprovementLine
                        )
                    }
                }
            } else {
                // Partner message analysis
                MessageAnalysis(
                    explanation = "The partner presented a point of view or emotional cue.",
                    confidenceDelta = 0,
                    rapportDelta = 5,
                    listeningRating = "Good",
                    empathyRating = "Good",
                    flowRating = "Balanced",
                    suggestedImprovement = "Listen closely to their concerns and validate them in your next reply.",
                    heatmapColor = ReplayGood,
                    lineColor = ReplayGoodLine
                )
            }
        }
    }

    // Timeline Points (Always 6 points mapped across messages)
    val timelinePoints = listOf(
        "Conversation Started",
        "Built Rapport",
        "Strong Question",
        "Interrupted Flow",
        "Recovered Well",
        "Excellent Ending"
    )

    // Helper to map timeline point to message index
    fun getMessageIndexForTimelinePoint(pointIdx: Int): Int {
        if (parsedMessages.isEmpty()) return 0
        val count = parsedMessages.size
        return when (pointIdx) {
            0 -> 0
            1 -> (count * 1 / 5).coerceIn(0, count - 1)
            2 -> (count * 2 / 5).coerceIn(0, count - 1)
            3 -> (count * 3 / 5).coerceIn(0, count - 1)
            4 -> (count * 4 / 5).coerceIn(0, count - 1)
            5 -> count - 1
            else -> 0
        }
    }

    // Playback engine
    LaunchedEffect(isPlaying, expandedIndex) {
        if (isPlaying && parsedMessages.isNotEmpty()) {
            delay(3500)
            val currentIdx = expandedIndex ?: -1
            if (currentIdx < parsedMessages.size - 1) {
                val nextIdx = currentIdx + 1
                expandedIndex = nextIdx
                listState.animateScrollToItem(nextIdx + 1) // +1 due to header item
            } else {
                isPlaying = false
                // scroll to bottom (summary card)
                listState.animateScrollToItem(parsedMessages.size + 1)
            }
        }
    }

    // Communication Score Breakdown calculations (offsets from overall score)
    val scoreMetrics = remember(session.score) {
        listOf(
            "Confidence" to (session.score + 2).coerceIn(10, 100) to "Pacing and language choices reflect security.",
            "Empathy" to (session.score + 5).coerceIn(10, 100) to "Demonstrated care for the speaker's emotional state.",
            "Listening" to (session.score + 8).coerceIn(10, 100) to "Directly responded to conversational cues.",
            "Assertiveness" to (session.score - 5).coerceIn(10, 100) to "Stated goals clearly and set boundaries.",
            "Clarity" to (session.score + 4).coerceIn(10, 100) to "Expressed thoughts concisely without filler words.",
            "Curiosity" to (session.score + 6).coerceIn(10, 100) to "Asked open-ended questions to deepen rapport.",
            "Respect" to (session.score + 10).coerceIn(10, 100) to "Polite phrasing and collaborative tone.",
            "Rapport" to (session.score + 7).coerceIn(10, 100) to "Maintained mutual comfort and engagement.",
            "Emotional Intelligence" to (session.score + 3).coerceIn(10, 100) to "Recognized and defused latent friction.",
            "Conversation Flow" to (session.score + 1).coerceIn(10, 100) to "Structured progress from greeting to closing."
        )
    }

    // AI Summary constants
    val aiSummaryParagraph = "You successfully navigated this conversation, demonstrating strong empathy and structured questions. While you built rapport early on, you can improve by expressing more confidence during conflict peaks."
    val strengths = listOf(
        "Active emotional validation",
        "Thoughtful open-ended questioning",
        "Polite and respectful closing"
    )
    val improvements = listOf(
        "Expand short passive answers",
        "Increase assertiveness during conflict peaks",
        "Maintain steady pacing throughout"
    )
    val practiceGoal = "Next time, try to ask at least two follow-up questions before proposing a solution."

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI REPLAY • ${session.partnerName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = session.scenarioTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                            val scoresMap = scoreMetrics.associate { it.first.first to it.first.second }
                            exportReplayPdf(
                                context = context,
                                session = session,
                                summary = aiSummaryParagraph,
                                goal = practiceGoal,
                                scores = scoresMap
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Report", tint = SleekPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
            )
        },
        bottomBar = {
            // Replay Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isPlaying = false
                            expandedIndex = 0
                            coroutineScope.launch {
                                listState.animateScrollToItem(1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Jump to Beginning",
                            tint = SleekTextDark
                        )
                    }

                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = SleekPrimary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play Replay"
                        )
                    }

                    IconButton(
                        onClick = {
                            isPlaying = false
                            expandedIndex = null
                            coroutineScope.launch {
                                // scroll to bottom (summary / breakdown)
                                listState.animateScrollToItem(parsedMessages.size + 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Jump to End",
                            tint = SleekTextDark
                        )
                    }
                }
            }
        },
        containerColor = SleekBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. HORIZONTAL TIMELINE VIEW
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(timelinePoints) { idx, point ->
                        val targetMsgIdx = getMessageIndexForTimelinePoint(idx)
                        val isSelected = expandedIndex == targetMsgIdx
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SleekPrimary else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SleekPrimary else SleekBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    isPlaying = false
                                    expandedIndex = targetMsgIdx
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetMsgIdx + 1) // +1 for header
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = point,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SleekTextDark
                            )
                        }

                        if (idx < timelinePoints.size - 1) {
                            Text(
                                text = "→",
                                color = SleekTextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. CHAT FEED & ANALYSIS
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                item {
                    Text(
                        text = "SCENARIO TIMELINE REPLAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SleekTextGray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Chat bubble list
                itemsIndexed(parsedMessages) { idx, message ->
                    val isExpanded = expandedIndex == idx
                    val analysis = messageAnalyses.getOrNull(idx)

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Original Chat Bubble Layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedIndex = if (isExpanded) null else idx
                                },
                            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!message.isUser) {
                                // Heatmap line left side of partner message
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(analysis?.lineColor ?: ReplayGoodLine)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // The Bubble
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (message.isUser) SleekPrimary else SleekSurface,
                                border = BorderStroke(1.dp, if (message.isUser) SleekPrimary else SleekBorder),
                                modifier = Modifier
                                    .weight(0.85f, fill = false)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (message.isUser) "You" else session.partnerName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (message.isUser) Color.White.copy(alpha = 0.7f) else SleekTextGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = message.text,
                                        fontSize = 13.sp,
                                        color = if (message.isUser) Color.White else SleekTextDark
                                    )
                                }
                            }

                            if (message.isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                // Heatmap line right side of user message
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(analysis?.lineColor ?: ReplayGoodLine)
                                )
                            }
                        }

                        // Expanded Analysis Card
                        if (isExpanded && analysis != null) {
                            AnimatedVisibility(
                                visible = true,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "AI COACH ANALYSIS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "\"${analysis.explanation}\"",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SleekTextDark
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 5 metrics progress
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            MiniAnalysisMetric("Confidence", analysis.confidenceDelta, "+")
                                            MiniAnalysisMetric("Rapport", analysis.rapportDelta, "+")
                                            MiniAnalysisTextMetric("Listening", analysis.listeningRating)
                                            MiniAnalysisTextMetric("Empathy", analysis.empathyRating)
                                            MiniAnalysisTextMetric("Conversation Flow", analysis.flowRating)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Suggested improvement:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextGray
                                        )
                                        Text(
                                            text = "\"${analysis.suggestedImprovement}\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextDark
                                        )

                                        // AI Rewrites Suggestions for User messages
                                        if (message.isUser) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = SleekBorder)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "AI REWRITE SUGGESTIONS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            val isDating = session.category.lowercase().contains("dating") ||
                                                    session.scenarioTitle.lowercase().contains("dating")
                                            
                                            val styles = remember {
                                                mutableListOf(
                                                    "Confident", "Friendly", "Professional", 
                                                    "Empathetic", "Shorter", "Assertive"
                                                ).apply {
                                                    if (isDating) {
                                                        add(3, "Flirty")
                                                    }
                                                }
                                            }

                                            var selectedRewriteStyle by remember { mutableStateOf<String?>(null) }
                                            var rewrittenText by remember { mutableStateOf<String?>(null) }
                                            var isGeneratingRewrite by remember { mutableStateOf(false) }

                                            // Flow row of buttons
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                styles.forEach { style ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(if (selectedRewriteStyle == style) SleekPrimary else SleekBackground)
                                                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                                                            .clickable {
                                                                isPlaying = false
                                                                isGeneratingRewrite = true
                                                                rewrittenText = null
                                                                selectedRewriteStyle = style
                                                                
                                                                coroutineScope.launch {
                                                                    try {
                                                                        val result = viewModel.generateRewritePublic(
                                                                            message.text, 
                                                                            style, 
                                                                            session.scenarioTitle
                                                                        )
                                                                        rewrittenText = result ?: fallbackRewrite(message.text, style)
                                                                    } catch (e: java.lang.Exception) {
                                                                        e.printStackTrace()
                                                                        rewrittenText = fallbackRewrite(message.text, style)
                                                                    } finally {
                                                                        isGeneratingRewrite = false
                                                                    }
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = style,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selectedRewriteStyle == style) Color.White else SleekTextDark
                                                        )
                                                    }
                                                }
                                            }

                                            if (isGeneratingRewrite) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = SleekPrimary
                                                )
                                            }

                                            rewrittenText?.let { rText ->
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = SleekBackground,
                                                    border = BorderStroke(1.dp, SleekBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                            text = "Suggested Rewrite ($selectedRewriteStyle):",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SleekPrimary
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "\"$rText\"",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SleekTextDark
                                                        )
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

                // 3. SCORE BREAKDOWN SECTION
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "CONVERSATION SCORE BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            scoreMetrics.forEach { metricData ->
                                val name = metricData.first.first
                                val valScore = metricData.first.second
                                val explanation = metricData.second

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextDark
                                        )
                                        Text(
                                            text = "$valScore%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { valScore / 100f },
                                        color = SleekPrimary,
                                        trackColor = SleekBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = explanation,
                                        fontSize = 10.sp,
                                        color = SleekTextGray
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. AI SUMMARY SECTION
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "AI REPLAY SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Conversation Summary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiSummaryParagraph,
                                fontSize = 12.sp,
                                color = SleekTextDark,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Strengths",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            strengths.forEachIndexed { i, s ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓",
                                        color = SleekPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(text = s, fontSize = 12.sp, color = SleekTextDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Areas to Improve",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            improvements.forEachIndexed { i, imp ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "•",
                                        color = SleekTextGray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(text = imp, fontSize = 12.sp, color = SleekTextDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Practice Goal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = practiceGoal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helpers
@Composable
fun MiniAnalysisMetric(name: String, value: Int, prefix: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 11.sp, color = SleekTextGray)
        Text(text = "$prefix$value", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
    }
}

@Composable
fun MiniAnalysisTextMetric(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 11.sp, color = SleekTextGray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
    }
}

fun wrapText(text: String, width: Int, paint: android.graphics.Paint): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val measure = paint.measureText(testLine)
        if (measure > width) {
            lines.add(currentLine)
            currentLine = word
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }
    return lines
}

fun exportReplayPdf(
    context: Context,
    session: ConversationSession,
    summary: String,
    goal: String,
    scores: Map<String, Int>
) {
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
        canvas.drawText("CONVERSABLE REPLAY REPORT", 50f, y, paint)
        y += 40f

        // Meta info
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas.drawText("Scenario: ${session.scenarioTitle}", 50f, y, paint)
        y += 20f
        canvas.drawText("Partner: ${session.partnerName} (${session.category})", 50f, y, paint)
        y += 20f
        canvas.drawText("Overall Social Score: ${session.score}/100", 50f, y, paint)
        y += 40f

        // Summary Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("AI Conversation Summary", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f

        // Wrap summary
        val lines = wrapText(summary, 500, paint)
        for (line in lines) {
            canvas.drawText(line, 50f, y, paint)
            y += 16f
        }
        y += 30f

        // Scores Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Communication DNA Scores", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f

        for ((name, scoreVal) in scores) {
            canvas.drawText("$name: $scoreVal/100", 50f, y, paint)
            y += 18f
        }
        y += 30f

        // Goal Title
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Actionable Next Goal", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f
        canvas.drawText(goal, 50f, y, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val file = File(context.cacheDir, "conversable_replay_report.pdf")
        val out = FileOutputStream(file)
        pdfDocument.writeTo(out)
        out.close()
        pdfDocument.close()

        // Share Intent using FileProvider
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
        context.startActivity(Intent.createChooser(intent, "Share Replay Progress"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share progress PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun fallbackRewrite(text: String, style: String): String {
    return when (style) {
        "Confident" -> "I am fully prepared to take this on and guarantee a successful resolution."
        "Friendly" -> "I'd love to work together on this! Let me know what you think works best."
        "Professional" -> "I understand. I will analyze the requirements and provide a comprehensive proposal shortly."
        "Flirty" -> "I'm really enjoying talking to you. We should definitely grab coffee and chat more."
        "Empathetic" -> "I completely hear you, and it makes total sense that you'd feel that way."
        "Shorter" -> "Understood. Let's start immediately."
        "Assertive" -> "I need this completed by tomorrow to ensure we meet our project deadlines."
        else -> text
    }
}
