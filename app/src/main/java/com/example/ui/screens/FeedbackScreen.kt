package com.example.ui.screens

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ConversationSession
import com.example.data.model.ChatMessage
import androidx.activity.compose.BackHandler
import com.example.viewmodel.ConversableViewModel
import com.example.viewmodel.ProgressionResult
import com.example.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    session: ConversationSession,
    onBackToDashboard: () -> Unit,
    onViewReplay: (ConversationSession) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversableViewModel? = null
) {
    com.example.security.KeepScreenSecure()
    BackHandler {
        onBackToDashboard()
    }
    var showTranscript by remember { mutableStateOf(false) }

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

    val progressionResult by if (viewModel != null) {
        viewModel.latestProgression.collectAsState()
    } else {
        remember { mutableStateOf<ProgressionResult?>(null) }
    }

    val relationshipSummary by if (viewModel != null) {
        viewModel.relationshipSummary.collectAsState()
    } else {
        remember {
            mutableStateOf<Map<String, Any>?>(
                mapOf(
                    "highest_trust" to (session.score + 10).coerceIn(10, 100),
                    "lowest_engagement" to (session.score - 15).coerceIn(10, 100),
                    "best_moment" to "Sharing detailed perspectives politely and establishing clear ground rules.",
                    "most_awkward_moment" to "None! You maintained standard social boundaries smoothly.",
                    "growth" to (session.score - 50),
                    "overall_rapport" to session.score
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
    ) {
        // High-fidelity Coach Evaluation Hero Block
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurface)
                    .border(BorderStroke(1.dp, SleekBorder), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session Report",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Score display
                Row(
                    modifier = Modifier.padding(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val scoreColor = when {
                        session.score >= 75 -> Color(0xFF16A34A)
                        session.score >= 50 -> Color(0xFFD97706)
                        else -> Color(0xFFDC2626)
                    }
                    Text(
                        text = "${session.score}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.W600,
                        color = scoreColor,
                        letterSpacing = (-0.04).sp * 72,
                        lineHeight = 72.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = "/100",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W400,
                        color = Color(0xFFAEAEB8),
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 10.dp)
                            .alignByBaseline()
                    )
                }

                val statusLabel = when (session.score) {
                    in 90..100 -> "Outstanding"
                    in 75..89 -> "Great work"
                    in 60..74 -> "Good effort"
                    in 40..59 -> "Keep practicing"
                    else -> "Room to grow"
                }
                Text(
                    text = statusLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = Color(0xFF6B6B80),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = session.scenarioTitle,
                    fontSize = 13.sp,
                    color = Color(0xFFAEAEB8),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Practice with ${session.partnerName} (${session.category})",
                    fontSize = 12.sp,
                    color = SleekTextGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // High-Priority AI Replay CTA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable { onViewReplay(session) }
                    .testTag("view_ai_replay_cta_button"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMPARE WITH AN EXPERT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "View AI Replay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Replay your conversation step-by-step and see exact emotional impacts.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }
        }

        // --- FEATURE 7: Live Relationship Meter - End Session Summary Card ---
        relationshipSummary?.let { summary ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("relationship_summary_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Relationship Analysis",
                                style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid: Overall Connection & Growth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OVERALL RAPPORT", style = TextXs.copy(color = TextSecondary))
                                Text(
                                    "${summary["overall_rapport"]}%",
                                    style = TextXl.copy(fontWeight = FontWeight.Black, color = Accent)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("RELATIONSHIP GROWTH", style = TextXs.copy(color = TextSecondary))
                                val growth = summary["growth"] as? Int ?: 0
                                val growthText = if (growth >= 0) "+$growth%" else "$growth%"
                                val growthColor = if (growth >= 0) Success else Danger
                                Text(
                                    growthText,
                                    style = TextXl.copy(fontWeight = FontWeight.Black, color = growthColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Key metrics: Highest Trust & Lowest Engagement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Success.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("shield", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Highest Trust", style = TextXs.copy(color = TextSecondary))
                                    Text(
                                        "${summary["highest_trust"]}%",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Warning.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Min Engagement", style = TextXs.copy(color = TextSecondary))
                                    Text(
                                        "${summary["lowest_engagement"]}%",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Standout Moments: Best Moment
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("star", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Best Connection Point",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summary["best_moment"] as? String ?: "No specific high-connection turn recorded.",
                                style = TextXs.copy(color = TextSecondary, lineHeight = 16.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Standout Moments: Most Awkward Moment
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Most Awkward Moment",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summary["most_awkward_moment"] as? String ?: "No awkward moments found! Well done.",
                                style = TextXs.copy(color = TextSecondary, lineHeight = 16.sp)
                            )
                        }
                    }
                }
            }
        }

        // Gamified Progression Celebration & Feedback Row
        // Only active/visible on the live-feedback screen having a computed live result
        progressionResult?.let { progression ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Level up or motivation heading
                        if (progression.leveled_up) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Success)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "LEVEL UP",
                                        style = TextXs.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = TextOnDark,
                                            letterSpacing = 0.06.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Level ${progression.level_after} • ${progression.level_title_after}",
                                        style = TextLg.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextOnDark
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your conversational skills are growing.",
                                        style = TextSm.copy(
                                            color = TextOnDark.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PROGRESSION COMPLETED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPrimary,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Level ${progression.level_after} (${progression.level_title_after})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SleekPrimary.copy(alpha = 0.15f),
                                    contentColor = SleekPrimary
                                ) {
                                    Text(
                                        text = "+${progression.xp_earned.total_this_session} XP",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Motivational feedback message under 12 words
                        Text(
                            text = progression.motivational_message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekTextDark,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SleekBackground, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // XP Breakdown
                        Text(
                            text = "XP Breakdown",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Base scenario clearance", fontSize = 12.sp, color = SleekTextGray)
                            Text(text = "+${progression.xp_earned.base} XP", fontSize = 12.sp, color = SleekTextDark, fontWeight = FontWeight.Bold)
                        }
                        
                        progression.xp_earned.bonuses.forEach { bonus ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Bonus: ${bonus.label}", fontSize = 12.sp, color = SleekTextGray)
                                Text(text = "+${bonus.amount} XP", fontSize = 12.sp, color = SleekTextDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Streak and frozen state
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Streak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextDark
                                    )
                                    Text(
                                        text = when {
                                            progression.streak.streak_broken -> "Streak was reset to 1 day."
                                            progression.streak.streak_freeze_used -> "Streak freeze rescued you!"
                                            else -> "Practice daily"
                                        },
                                        fontSize = 11.sp,
                                        color = SleekTextGray
                                    )
                                }
                            }
                            Text(
                                text = "${progression.streak.new} Days",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (progression.streak.new > progression.streak.previous) SleekPrimary else SleekTextDark
                            )
                        }

                        // Newly unlocked badges
                        if (progression.badges_unlocked.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = SleekBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Badges earned (${progression.badges_unlocked.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            progression.badges_unlocked.forEach { badge ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SleekPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = badge.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekTextDark
                                            )
                                            Text(
                                                text = badge.reason,
                                                fontSize = 11.sp,
                                                color = SleekTextGray,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Skill Deltas (Average rating progression)
                        val anyDelta = progression.skill_deltas.empathy != 0.0 ||
                                progression.skill_deltas.listening != 0.0 ||
                                progression.skill_deltas.confidence != 0.0 ||
                                progression.skill_deltas.collaboration != 0.0
                        
                        if (anyDelta) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = SleekBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Skill Growth (Averages change)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                fun formatDelta(d: Double): String = if (d >= 0) "+${String.format(Locale.US, "%.2f", d)}" else String.format(Locale.US, "%.2f", d)
                                fun deltaColor(d: Double) = if (d >= 0) SleekPrimary else SleekWarning

                                if (progression.skill_deltas.empathy != 0.0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Empathy", fontSize = 11.sp, color = SleekTextGray)
                                        Text(formatDelta(progression.skill_deltas.empathy), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor(progression.skill_deltas.empathy))
                                    }
                                }
                                if (progression.skill_deltas.listening != 0.0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Listening", fontSize = 11.sp, color = SleekTextGray)
                                        Text(formatDelta(progression.skill_deltas.listening), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor(progression.skill_deltas.listening))
                                    }
                                }
                                if (progression.skill_deltas.confidence != 0.0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Confidence", fontSize = 11.sp, color = SleekTextGray)
                                        Text(formatDelta(progression.skill_deltas.confidence), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor(progression.skill_deltas.confidence))
                                    }
                                }
                                if (progression.skill_deltas.collaboration != 0.0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Collab", fontSize = 11.sp, color = SleekTextGray)
                                        Text(formatDelta(progression.skill_deltas.collaboration), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor(progression.skill_deltas.collaboration))
                                    }
                                }
                            }
                        }

                        // Next Scenario Recommendation
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SleekPrimary.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Try next",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPrimary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = progression.next_scenario_recommendation.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = progression.next_scenario_recommendation.reason,
                                    fontSize = 12.sp,
                                    color = SleekTextGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-ratings section: Empathy, Flow, Goal Achievement
        item {
            Text(
                                text = "Skills",
                                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurface)
                    .border(BorderStroke(1.dp, SleekBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RatingBarItem(title = "Empathy & Active Listening", score = session.empathyScore, activeColor = SleekSuccess)
                RatingBarItem(title = "Dialogue Flow & Rapport", score = session.flowScore, activeColor = SleekPrimary)
                RatingBarItem(title = "Scenario Goal Achievement", score = session.goalAchievementScore, activeColor = SleekWarningAmber)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // What Went Well Section (Green Check Block)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coach_what_went_well")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Positives",
                            tint = SleekSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "What worked",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = session.whatWentWell,
                        fontSize = 13.sp,
                        color = SleekTextGray,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Opportunities to Grow Section (Amber Check Block)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coach_missed_opportunities")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Growth Opportunities",
                            tint = SleekWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "To work on",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = session.missedOpportunities,
                        fontSize = 13.sp,
                        color = SleekTextGray,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // FEATURE 8: Emotion & Tone Analyzer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tone Analyzer",
                            tint = SleekPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emotion & Tone analysis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tone Progress bars
                    Text("Assertiveness / Confidence", style = TextXs.copy(color = SleekTextGray))
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { session.goalAchievementScore / 100f },
                            modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                            color = SleekPrimary,
                            trackColor = SleekBorder
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${session.goalAchievementScore}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Empathy & Warmth", style = TextXs.copy(color = SleekTextGray))
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { session.empathyScore / 100f },
                            modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                            color = SleekPrimary,
                            trackColor = SleekBorder
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${session.empathyScore}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Active Listening & Focus", style = TextXs.copy(color = SleekTextGray))
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { session.flowScore / 100f },
                            modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                            color = SleekPrimary,
                            trackColor = SleekBorder
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${session.flowScore}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Conversation Balance / Timeline
                    Text("CONVERSATION EMOTION TIMELINE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val timeline = when {
                        session.score > 85 -> listOf("Greeting (Calm)", "Exploration (Curious)", "Core Discussion (Engaged)", "Outcome (Satisfied)")
                        session.score > 60 -> listOf("Greeting (Formal)", "Exploration (Hesitant)", "Core Discussion (Open)", "Outcome (Neutral)")
                        else -> listOf("Greeting (Guarded)", "Exploration (Defensive)", "Core Discussion (Tense)", "Outcome (Unresolved)")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timeline.forEach { stage ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                border = BorderStroke(1.dp, SleekBorder),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Box(modifier = Modifier.padding(4.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(stage, fontSize = 8.sp, maxLines = 2, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Replay tips and guidelines (Gold Glow Block)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coach_replay_tips")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Replay Secrets",
                            tint = SleekWarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How to improve",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = session.replayTips,
                        fontSize = 13.sp,
                        color = SleekTextGray,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Replay Interactive transcript toggle card
        if (parsedMessages.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTranscript = !showTranscript }
                        .testTag("transcript_toggle_card"),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showTranscript) "Hide transcript" else "View transcript",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showTranscript) {
                items(parsedMessages) { msg ->
                    SessionReplayMsgRow(message = msg, partnerName = session.partnerName)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // Return Home navigation / Completed flow with two options
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onViewReplay(session) },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("feedback_view_replay_button")
                ) {
                    Text(
                        text = "View Conversation Replay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                
                OutlinedButton(
                    onClick = onBackToDashboard,
                    border = BorderStroke(1.dp, SleekPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("feedback_new_practice_button")
                ) {
                    Text(
                        text = "Start New Practice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SleekPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun RatingBarItem(title: String, score: Int, activeColor: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, color = SleekTextDark, fontWeight = FontWeight.Medium)
            Text("$score%", fontSize = 12.sp, color = activeColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(SleekBubblePartner)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score.toFloat() / 100f)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}

@Composable
fun SessionReplayMsgRow(message: ChatMessage, partnerName: String) {
    val prefix = if (message.isUser) "You" else partnerName
    val color = if (message.isUser) SleekPrimary else SleekTextGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SleekBubblePartner)
            .border(BorderStroke(1.dp, SleekBorder), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$prefix: ",
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = message.text,
            fontSize = 12.sp,
            color = SleekTextDark,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1.5f)
        )
    }
}
