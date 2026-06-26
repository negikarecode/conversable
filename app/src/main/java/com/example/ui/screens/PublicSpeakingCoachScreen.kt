package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicSpeakingCoachScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val voiceRecordingState by viewModel.voiceRecordingState.collectAsState()
    val voiceFeedbackMessage by viewModel.voiceFeedbackMessage.collectAsState()

    var speechText by remember { mutableStateOf("") }
    var slideOutline by remember { mutableStateOf("") }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<Map<String, Any>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PUBLIC SPEAKING & PRESENTATION", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (analysisResult != null) {
                            analysisResult = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SleekPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
            )
        },
        containerColor = SleekBackground,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (analysisResult != null) {
                // Analysis Results View
                val pace = analysisResult!!["pace"] as? String ?: "Normal (130 WPM)"
                val clarity = (analysisResult!!["clarity"] as? Double ?: 80.0).toInt()
                val fillers = analysisResult!!["fillers"] as? String ?: "2 fillers detected (um, like)"
                val confidence = (analysisResult!!["confidence"] as? Double ?: 75.0).toInt()
                val evaluation = analysisResult!!["evaluation"] as? String ?: "Great presentation structure."
                
                val slideTips = (analysisResult!!["slideTips"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SPEECH COHERENCE & METRICS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Clarity", style = TextXs.copy(color = SleekTextGray))
                                        Text("$clarity%", style = TextLg.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Confidence", style = TextXs.copy(color = SleekTextGray))
                                        Text("$confidence%", style = TextLg.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = SleekBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Speaking Pace:", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                    Text(pace, style = TextXs.copy(color = SleekTextDark))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Filler Words:", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                    Text(fillers, style = TextXs.copy(color = SleekTextDark))
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("COACH EVALUATION", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(evaluation, style = TextSm.copy(color = SleekTextDark))
                            }
                        }
                    }

                    if (slideTips.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("SLIDE & OUTLINE STRUCTURAL FEEDBACK", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    slideTips.forEach { tip ->
                                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text("• ", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                            Text(tip, style = TextXs.copy(color = SleekTextDark))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Config Setup View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("RECORD OR PASTE YOUR SPEECH", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = speechText,
                                    onValueChange = { speechText = it },
                                    placeholder = { Text("Pasted speech script, slides outline, or transcribed audio draft...") },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                // Microphone Record Button Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val isRecording = voiceRecordingState == ConversableViewModel.VoiceRecordingState.RECORDING
                                    Button(
                                        onClick = {
                                            if (isRecording) {
                                                viewModel.stopRecordingAudio(discard = false)
                                            } else {
                                                viewModel.startRecordingAudio(context)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) Color.Black else SleekPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                                                contentDescription = if (isRecording) "Stop" else "Record",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isRecording) "STOP RECORDING" else "RECORD SPEECH DRAFT", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                if (voiceFeedbackMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = voiceFeedbackMessage!!,
                                        style = TextXs.copy(color = SleekPrimary, fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Presentation Upload Area (Feature 19)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("PRESENTATION SLIDES & OUTLINE (OPTIONAL)", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = slideOutline,
                                    onValueChange = { slideOutline = it },
                                    placeholder = { Text("Paste Slide outline titles or speech structure here...\ne.g.\nSlide 1: Intro\nSlide 2: Problem statement\nSlide 3: Proposed Architecture") },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Mock file picker
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedCard(
                                        onClick = {
                                            slideOutline = "Slide 1: Executive Summary\nSlide 2: Market Pain Point\nSlide 3: Our Solution & Tech Stack\nSlide 4: Financial Trajectory & Call to Action"
                                            Toast.makeText(context, "Mock Pitch Deck slides outline loaded!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).padding(2.dp),
                                        colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                        border = BorderStroke(1.dp, SleekBorder)
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text("Load Mock Slides", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (speechText.isNotBlank()) {
                                    isAnalyzing = true
                                    coroutineScope.launch {
                                        val result = viewModel.analyzePublicSpeaking(speechText, slideOutline)
                                        analysisResult = result
                                        isAnalyzing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            enabled = speechText.isNotBlank() && !isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("ANALYZE SPEECH & SLIDES", style = TextSm.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
