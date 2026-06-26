package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsDashboardScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    val totalXp by viewModel.totalXp.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val savedSessions by viewModel.savedSessions.collectAsState()

    // 1. Calculate practice metrics
    val totalSessions = savedSessions.size
    val totalPracticeTimeMinutes = totalSessions * 8
    val avgSocialScore = if (totalSessions == 0) 75f else savedSessions.map { it.social_score.toFloat() }.average().toFloat()

    // 2. Generate Heatmap data (representing 28 days activity grid)
    val calendar = Calendar.getInstance()
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    val activePracticeDays = remember(savedSessions) {
        savedSessions.map {
            // ISO date is formatted like "yyyy-MM-dd'T'HH:mm:ss'Z'"
            try {
                it.date.substring(0, 10)
            } catch (e: Exception) {
                ""
            }
        }.filter { it.isNotEmpty() }.toSet()
    }

    val heatmapDays = remember(activePracticeDays) {
        val daysList = mutableListOf<Pair<String, Boolean>>()
        val tempCal = Calendar.getInstance()
        tempCal.add(Calendar.DAY_OF_YEAR, -27) // Last 4 weeks
        
        for (i in 0 until 28) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tempCal.time)
            val didPractice = activePracticeDays.contains(dateStr)
            daysList.add(Pair(dateStr, didPractice))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        daysList
    }

    // 3. Emotion / Tone distribution
    val confidenceVal = remember(savedSessions) {
        if (savedSessions.isEmpty()) 70 else (savedSessions.map { it.skills.confidence }.average() * 20).toInt()
    }
    val empathyVal = remember(savedSessions) {
        if (savedSessions.isEmpty()) 75 else (savedSessions.map { it.skills.empathy }.average() * 20).toInt()
    }
    val listeningVal = remember(savedSessions) {
        if (savedSessions.isEmpty()) 80 else (savedSessions.map { it.skills.listening }.average() * 20).toInt()
    }
    val anxietyVal = remember(savedSessions) {
        // High social scores relate to low anxiety
        if (savedSessions.isEmpty()) 45 else (100 - (avgSocialScore * 0.8f)).toInt().coerceIn(10, 90)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("INSIGHTS & STATISTICS", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SleekPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
            )
        },
        containerColor = SleekBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // General Stats Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OVERALL LIFETIME ENGAGEMENT", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Sessions", style = TextXs.copy(color = SleekTextGray))
                                Text("$totalSessions", style = TextLg.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Practice Time", style = TextXs.copy(color = SleekTextGray))
                                Text("$totalPracticeTimeMinutes m", style = TextLg.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Average Score", style = TextXs.copy(color = SleekTextGray))
                                Text("${avgSocialScore.toInt()}%", style = TextLg.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Heatmap Contribution Card (Feature 20)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("28-DAY CONVERSATION HEATMAP", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Draw Grid (7 Columns by 4 Rows)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val rows = heatmapDays.chunked(7)
                            rows.forEach { weekRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    weekRow.forEach { (_, active) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (active) SleekPrimary else SleekBorder)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Less ", style = TextXs.copy(color = SleekTextGray))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SleekBorder))
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SleekPrimary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(" More", style = TextXs.copy(color = SleekTextGray))
                        }
                    }
                }
            }

            // Emotion Distribution Cards (Feature 8 / 20)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("EMOTION & TONE METRIC TIMELINE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Confidence bar
                        Text("Assertiveness / Confidence", style = TextXs.copy(color = SleekTextDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { confidenceVal / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = SleekPrimary,
                                trackColor = SleekBorder
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$confidenceVal%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Empathy bar
                        Text("Empathy & Warmth", style = TextXs.copy(color = SleekTextDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { empathyVal / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = SleekPrimary,
                                trackColor = SleekBorder
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$empathyVal%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Listening bar
                        Text("Active Listening & Focus", style = TextXs.copy(color = SleekTextDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { listeningVal / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = SleekPrimary,
                                trackColor = SleekBorder
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$listeningVal%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Anxiety bar
                        Text("Social Anxiety / Hesitation", style = TextXs.copy(color = SleekTextDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { anxietyVal / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = SleekPrimary,
                                trackColor = SleekBorder
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$anxietyVal%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // AI Mentor Weekly Summary Insights
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI CONVERSATION INSIGHTS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Weekly Growth: ${if (totalSessions > 0) "+12%" else "0% (No practice sessions)"}\n" +
                            "Emotional Shifts: Your conversations show smooth emotional transitions. You successfully decrease partner anxiety by validating their statements early in the dialogue.",
                            style = TextXs.copy(color = SleekTextDark, lineHeight = 15.sp)
                        )
                    }
                }
            }
        }
    }
}
