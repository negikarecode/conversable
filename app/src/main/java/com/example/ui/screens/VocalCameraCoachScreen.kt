package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocalCameraCoachScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeSeconds by remember { mutableStateOf(0) }

    // Live Metrics Mock states
    var wpm by remember { mutableStateOf(0) }
    var fillerWords by remember { mutableStateOf(0) }
    var gazeScore by remember { mutableStateOf(100) }
    var postureState by remember { mutableStateOf("Good alignment") }
    var clarityScore by remember { mutableStateOf(95) }

    // Audio Visualizer lines
    var voiceWavePhase by remember { mutableStateOf(0f) }

    // Completed sessions list
    val dbSessions by viewModel.vocalCameraSessions.collectAsState()

    // Trigger timer & fluctuation loops
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimeSeconds = 0
            wpm = 135
            fillerWords = 0
            gazeScore = 98
            postureState = "Good alignment"
            clarityScore = 96
            
            while (isRecording) {
                delay(1000)
                recordingTimeSeconds++
                
                // Add minor random fluctuation
                wpm = (120..150).random()
                if (recordingTimeSeconds % 8 == 0) {
                    fillerWords++
                }
                gazeScore = (85..100).random()
                postureState = if ((0..10).random() > 8) "Leaning too close" else "Good alignment"
                clarityScore = (90..98).random()
                voiceWavePhase += 0.5f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VOCAL & CAMERA COACH", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isRecording) {
                // Setup and past sessions list
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("SPEECH & PHYSICAL PRESENCE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Practice speaking into your microphone and camera. We will analyze your pacing (WPM), filler words, gaze tracking, and posture alignment.",
                            style = TextXs.copy(color = SleekTextGray, lineHeight = 16.sp)
                        )
                    }
                }

                Text("PAST SESSION RECORDS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))

                if (dbSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No speech practice sessions recorded yet.", style = TextXs.copy(color = SleekTextGray))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dbSessions) { session ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(session.title, style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                                        val dateStr = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date(session.timestamp)) }
                                        Text(dateStr, style = TextXs.copy(color = SleekTextGray))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Filler Words", style = TextXs.copy(color = SleekTextGray))
                                            Text("${session.fillerWordCount}", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Column {
                                            Text("Avg Pace", style = TextXs.copy(color = SleekTextGray))
                                            Text("${session.wpm} WPM", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Column {
                                            Text("Gaze Stability", style = TextXs.copy(color = SleekTextGray))
                                            Text("${session.gazeScore}%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Column {
                                            Text("Clarity", style = TextXs.copy(color = SleekTextGray))
                                            Text("${session.speechClarityScore}%", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isRecording = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("START LIVE COACHING", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            } else {
                // Live recording tracking view
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left webcam canvas simulator
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(Color.Black, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw simulated face alignment outline in monochrome green/white
                            val w = size.width
                            val h = size.height
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f),
                                radius = size.minDimension * 0.25f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            // Shoulder lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.15f),
                                start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.85f),
                                end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.85f),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Guide box lines
                            drawRect(
                                color = Color.White.copy(alpha = 0.1f),
                                size = size.copy(width = w * 0.7f, height = h * 0.7f),
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("WEBCAM TRACKING ACTIVE", style = TextXs.copy(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    // Right Audio frequency visualizer
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val midY = h / 2f
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, midY)
                            for (x in 0..w.toInt() step 5) {
                                val y = midY + kotlin.math.sin((x.toFloat() * 0.05f) + voiceWavePhase) * 15.dp.toPx()
                                path.lineTo(x.toFloat(), y)
                            }
                            drawPath(
                                path = path,
                                color = SleekPrimary,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Live Metrics Column
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RECORDING TIME", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                            val minutes = recordingTimeSeconds / 60
                            val seconds = recordingTimeSeconds % 60
                            Text(
                                String.format("%02d:%02d", minutes, seconds),
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic rows
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("PACE WPM", style = TextXs.copy(color = SleekTextGray))
                                Text("$wpm WPM", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                            }
                            Column {
                                Text("FILLER WORDS", style = TextXs.copy(color = SleekTextGray))
                                Text("$fillerWords", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                            }
                            Column {
                                Text("GAZE LOCK", style = TextXs.copy(color = SleekTextGray))
                                Text("$gazeScore%", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("POSTURE STATE", style = TextXs.copy(color = SleekTextGray))
                                Text(postureState, style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                            }
                            Column {
                                Text("SPEECH CLARITY", style = TextXs.copy(color = SleekTextGray))
                                Text("$clarityScore%", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                            }
                            Spacer(modifier = Modifier.width(40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                Button(
                    onClick = {
                        viewModel.recordVocalCameraPractice(
                            title = "Speech Session #${dbSessions.size + 1}",
                            fillerWords = fillerWords,
                            wpm = wpm,
                            gazeScore = gazeScore,
                            posture = postureState,
                            clarity = clarityScore
                        )
                        isRecording = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("STOP & SAVE ANALYSIS", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
