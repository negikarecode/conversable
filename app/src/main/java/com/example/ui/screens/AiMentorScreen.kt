package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMentorScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val savedSessions by viewModel.savedSessions.collectAsState()
    val listState = rememberLazyListState()
    
    // Mentor interactive conversation state
    val messages = remember { mutableStateListOf<ChatMessage>(
        ChatMessage(
            id = "mentor_init",
            text = "Welcome to AI Mentorship. I've analyzed your practice history. How can I help you improve your communication skills today?",
            isUser = false
        )
    ) }
    var chatInput by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    // Curate recurring mistakes and stats based on history
    val mistakes = remember(savedSessions) {
        val list = mutableListOf<String>()
        if (savedSessions.isEmpty()) {
            list.add("Initial hesitation in starting conversations (small talk).")
            list.add("Direct questions can sometimes feel like resumes/interviews.")
        } else {
            val avgScore = savedSessions.map { it.social_score }.average()
            if (avgScore < 70) {
                list.add("Struggles to de-escalate defensiveness under medium and hard conflicts.")
            }
            val voiceCount = savedSessions.count { it.voice_mode_used }
            if (voiceCount == 0) {
                list.add("Avoidance of live voice practice mode (recommend trying voice conversations).")
            }
            val turns = savedSessions.map { it.turn_count }.average()
            if (turns < 4) {
                list.add("Conversation endings are abrupt; practice developing a smooth transition.")
            }
        }
        list
    }

    val weeklyGoal = remember(savedSessions) {
        if (savedSessions.size < 3) {
            "Complete 3 Practice sessions in the Negotiation Simulator."
        } else {
            "Reach a Social Score above 85% on a Hard difficulty roleplay scenario."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI LONG-TERM MENTOR", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            // Mentor Overview Dashboard (Scrollable stats)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WEEKLY GOAL", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(weeklyGoal, style = TextXs.copy(color = SleekTextDark, fontWeight = FontWeight.Medium))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("RECURRING CRITICAL BLUNDERS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                    Spacer(modifier = Modifier.height(6.dp))
                    mistakes.forEach { blunder ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text(blunder, style = TextXs.copy(color = SleekTextDark))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mentor Chat Area
            Text("CONSULT WITH YOUR AI MENTOR", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.isUser
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) SleekBubbleUser else SleekBubblePartner
                                ),
                                border = if (isMe) null else BorderStroke(1.dp, SleekBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = msg.text,
                                        style = TextXs.copy(color = if (isMe) SleekBubbleUserText else SleekTextDark)
                                    )
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

                // Chat Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask for career advice, review, etc...") },
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
                                        val question = chatInput
                                        messages.add(ChatMessage(id = UUID.randomUUID().toString(), text = question, isUser = true))
                                        chatInput = ""
                                        isReplying = true
                                        
                                        coroutineScope.launch {
                                            val systemPrompt = "You are a senior executive communication mentor. The user has practiced scenarios in an app. Answer their social/career queries directly, providing 1-2 practical takeaways. Keep answer under 75 words."
                                            val reply = viewModel.queryAiPublic(systemPrompt, question)
                                            messages.add(ChatMessage(
                                                id = UUID.randomUUID().toString(),
                                                text = reply ?: "I am looking into that. Practice another scenario to gain more insights.",
                                                isUser = false
                                            ))
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
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
