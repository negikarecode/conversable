package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMeetingSimulatorScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var isSessionActive by remember { mutableStateOf(false) }
    var selectedMeetingType by remember { mutableStateOf("Board Room") }
    var userGoal by remember { mutableStateOf("Navigate conflict between marketing and tech leads.") }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var chatInput by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    // Final report metrics
    var showReport by remember { mutableStateOf(false) }
    var reportScore by remember { mutableStateOf(85) }
    var reportFeedback by remember { mutableStateOf("") }

    val participants = remember(selectedMeetingType) {
        when (selectedMeetingType) {
            "Board Room" -> listOf("Sarah (CEO)", "Daniel (Marketing)", "Devin (Principal Eng)")
            "Project Retro" -> listOf("Alex (Scrum Master)", "Emma (Designer)", "Liam (QA)")
            else -> listOf("Mom", "Uncle Bob", "Jamie (Sibling)")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GROUP MEETING SIMULATOR", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            if (!isSessionActive && !showReport) {
                // Setup / Welcome Configuration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MULTIPERSONA SIMULATION", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Practice speaking in high-pressure group environments with up to three distinct AI personalities responding in real-time.",
                            style = TextXs.copy(color = SleekTextGray)
                        )
                    }
                }

                Text("SELECT MEETING CONTEXT", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))

                listOf("Board Room", "Project Retro", "Family Dinner Discussion").forEach { type ->
                    val isSelected = selectedMeetingType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedMeetingType = type },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SleekPrimary else SleekSurface,
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMeetingType = type },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isSelected) Color.White else SleekPrimary,
                                    unselectedColor = SleekTextGray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type,
                                style = TextXs.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SleekTextDark
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("YOUR HIDDEN PRACTICE GOAL", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))

                OutlinedTextField(
                    value = userGoal,
                    onValueChange = { userGoal = it },
                    placeholder = { Text("What is your goal for this meeting?") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        messages.clear()
                        messages.add(
                            ChatMessage(
                                id = "init_msg",
                                text = "Hi everyone. Let's get this meeting started. We have a lot of items on our agenda today.",
                                isUser = false
                            )
                        )
                        isSessionActive = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("START GROUP SIMULATION", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            } else if (isSessionActive) {
                // Active Meeting Simulation
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Participants: ${participants.joinToString(", ")}",
                            style = TextXs.copy(color = SleekTextDark, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.isUser
                            val senderName = if (isMe) "You" else {
                                participants[Math.abs(msg.id.hashCode()) % participants.size]
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    if (!isMe) {
                                        Box(
                                            modifier = Modifier.size(16.dp).clip(CircleShape).background(SleekBorder),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(senderName.take(1), style = TextXs.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                    Text(senderName, style = TextXs.copy(color = SleekTextGray, fontWeight = FontWeight.Bold))
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) SleekBubbleUser else SleekBubblePartner
                                    ),
                                    border = if (isMe) null else BorderStroke(1.dp, SleekBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(msg.text, style = TextXs.copy(color = if (isMe) SleekBubbleUserText else SleekTextDark))
                                    }
                                }
                            }
                        }

                        if (isReplying) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.CenterStart) {
                                    CircularProgressIndicator(color = SleekPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Address the meeting room...", style = TextXs) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (chatInput.isNotBlank() && !isReplying) {
                                            val text = chatInput
                                            messages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                                            chatInput = ""
                                            isReplying = true

                                            coroutineScope.launch {
                                                val adapter = viewModel.moshi.adapter(List::class.java)
                                                val partsStr = adapter.toJson(participants)
                                                val prompt = "You are simulating a group dialog with these members: $partsStr. The overall topic context is a $selectedMeetingType. Act as one of the members replying directly to the user's latest point."
                                                val reply = viewModel.queryGroupMeetingAi(prompt, text, partsStr)

                                                messages.add(
                                                    ChatMessage(
                                                        id = UUID.randomUUID().toString(),
                                                        text = reply ?: "Interesting point. What does everyone else think?",
                                                        isUser = false
                                                    )
                                                )
                                                isReplying = false
                                                listState.animateScrollToItem(messages.size)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = SleekPrimary)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isReplying = true
                        coroutineScope.launch {
                            val prompt = "Evaluate this group conversation. The user's goal was: $userGoal. Rate the performance out of 100 and write a 2-sentence summary of recommendations."
                            val transcriptText = messages.joinToString("\n") { "${if(it.isUser) "User" else "AI"}: ${it.text}" }
                            val response = viewModel.queryAiPublic(prompt, transcriptText)
                            val scoreMatch = "\\b([0-9]{2,3})\\b".toRegex().find(response ?: "")
                            reportScore = scoreMatch?.value?.toIntOrNull() ?: 80
                            reportFeedback = response ?: "Practice went well. Work on coordinating multiple inputs."
                            
                            viewModel.saveGroupMeetingSession(
                                title = selectedMeetingType,
                                participantsJson = viewModel.moshi.adapter(List::class.java).toJson(participants),
                                transcriptJson = transcriptJson(messages),
                                score = reportScore,
                                feedback = reportFeedback
                            )

                            isReplying = false
                            isSessionActive = false
                            showReport = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("END & EVALUATE MEETING", style = TextXs.copy(fontWeight = FontWeight.Bold))
                }
            } else {
                // Post-Meeting Evaluation Report
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MEETING SCORE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                        Text("$reportScore / 100", style = TextXl.copy(fontWeight = FontWeight.Black, color = SleekPrimary))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COACH EVALUATION", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(reportFeedback, style = TextXs.copy(color = SleekTextDark, lineHeight = 16.sp), textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        showReport = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("BACK TO SETUP", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

private fun transcriptJson(messages: List<ChatMessage>): String {
    return "[${messages.joinToString(",") { "{\"text\":\"${it.text}\",\"isUser\":${it.isUser}}" }}]"
}
