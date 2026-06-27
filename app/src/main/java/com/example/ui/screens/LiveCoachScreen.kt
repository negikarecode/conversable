package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch
import java.util.UUID

enum class CoachState { SETUP, ACTIVE, FEEDBACK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCoachScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var coachState by remember { mutableStateOf(CoachState.SETUP) }
    
    // Setup inputs
    var goalInput by remember { mutableStateOf("") }
    var scenarioInput by remember { mutableStateOf("") }
    var generatedStrategy by remember { mutableStateOf("") }
    var isGeneratingStrategy by remember { mutableStateOf(false) }

    // Active conversation state
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var currentMessageInput by remember { mutableStateOf("") }
    var isUserTurn by remember { mutableStateOf(true) } // toggles between User and Partner
    
    var currentCoachTip by remember { mutableStateOf<String>("Type the first message to receive your first coaching tip.") }
    var isGeneratingTip by remember { mutableStateOf(false) }
    var tipsCount by remember { mutableStateOf(1) }

    // Post feedback state
    var feedbackReport by remember { mutableStateOf("") }
    var isGeneratingFeedback by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LIVE CONVERSATION COACH", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (coachState != CoachState.SETUP) {
                            coachState = CoachState.SETUP
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
            when (coachState) {
                CoachState.SETUP -> {
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
                                    Text("DEFINE CONVERSATION GOAL", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = goalInput,
                                        onValueChange = { goalInput = it },
                                        placeholder = { Text("e.g. Ask roommate to clean their dishes more consistently") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SleekPrimary,
                                            unfocusedBorderColor = SleekBorder
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("SCENARIO / RELATIONSHIP CONTEXT", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = scenarioInput,
                                        onValueChange = { scenarioInput = it },
                                        placeholder = { Text("e.g. My roommate of 1 year. We are generally friendly but get passive aggressive about chores.") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SleekPrimary,
                                            unfocusedBorderColor = SleekBorder
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            if (goalInput.isNotBlank() && scenarioInput.isNotBlank()) {
                                                isGeneratingStrategy = true
                                                coroutineScope.launch {
                                                    val strategy = viewModel.generateLiveStrategy(goalInput, scenarioInput)
                                                    generatedStrategy = strategy ?: "No strategy generated."
                                                    isGeneratingStrategy = false
                                                    coachState = CoachState.ACTIVE
                                                    // Set initial messages empty
                                                    messages.clear()
                                                    currentCoachTip = "Introduce the topic calmly. Focus on your own feelings using 'I' statements."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = goalInput.isNotBlank() && scenarioInput.isNotBlank() && !isGeneratingStrategy,
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                                    ) {
                                        if (isGeneratingStrategy) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("CREATE PRE-GAME STRATEGY", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CoachState.ACTIVE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Strategy Banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("YOUR PRE-GAME STRATEGY", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(generatedStrategy, style = TextXs.copy(color = SleekTextDark), maxLines = 3)
                            }
                        }

                        // Message Transcript List
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (messages.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No messages logged yet. Log your messages as the conversation progresses to get real-time coaching tips.",
                                            style = TextXs.copy(color = SleekTextGray),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
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
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    if (isMe) "ME" else "PARTNER",
                                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = if (isMe) SleekBubbleUserText else SleekTextGray)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(msg.text, style = TextSm.copy(color = if (isMe) SleekBubbleUserText else SleekTextDark))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live Coaching Suggestions Panel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Info, contentDescription = "Tip", tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("LIVE COACH SUGGESTION", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                                    }
                                    TextButton(
                                        onClick = {
                                            // Request alternate coaching suggestion
                                            isGeneratingTip = true
                                            coroutineScope.launch {
                                                val nextTip = viewModel.generateLiveCoachingTip(goalInput, scenarioInput, messages, alternate = true)
                                                currentCoachTip = nextTip ?: "No alternate tip available."
                                                isGeneratingTip = false
                                                tipsCount++
                                            }
                                        },
                                        enabled = !isGeneratingTip
                                    ) {
                                        Text("Next Suggestion", style = TextXs.copy(color = SleekPrimary, fontWeight = FontWeight.Bold))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isGeneratingTip) {
                                    Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = SleekPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Text(currentCoachTip, style = TextXs.copy(color = SleekTextDark, fontWeight = FontWeight.Medium))
                                }
                            }
                        }

                        // Message Logging Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Turn Switcher
                            TextButton(
                                onClick = { isUserTurn = !isUserTurn },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(if (isUserTurn) "Me:" else "Partner:", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                            }

                            OutlinedTextField(
                                value = currentMessageInput,
                                onValueChange = { currentMessageInput = it },
                                placeholder = { Text(if (isUserTurn) "Log what you said..." else "Log what they said...") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (currentMessageInput.isNotBlank()) {
                                                val text = currentMessageInput
                                                messages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = isUserTurn))
                                                currentMessageInput = ""
                                                // auto flip turn
                                                isUserTurn = !isUserTurn
                                                
                                                // Trigger next coaching tip
                                                isGeneratingTip = true
                                                coroutineScope.launch {
                                                    val newTip = viewModel.generateLiveCoachingTip(goalInput, scenarioInput, messages)
                                                    currentCoachTip = newTip ?: "Listen actively and formulate your reply patiently."
                                                    isGeneratingTip = false
                                                    lazyListState.animateScrollToItem(messages.size)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Send, contentDescription = "Log Message", tint = SleekPrimary)
                                    }
                                }
                            )
                        }

                        // Finish Button
                        Button(
                            onClick = {
                                isGeneratingFeedback = true
                                coachState = CoachState.FEEDBACK
                                coroutineScope.launch {
                                    val feedback = viewModel.generateLivePostFeedback(goalInput, scenarioInput, strategy = generatedStrategy, messages)
                                    feedbackReport = feedback ?: "Post-conversation summary is ready."
                                    viewModel.saveLiveCoachingSession(
                                        goal = goalInput,
                                        scenario = scenarioInput,
                                        strategy = generatedStrategy,
                                        transcriptJson = viewModel.moshi.adapter(List::class.java).toJson(messages.map { mapOf("text" to it.text, "isUser" to it.isUser) }),
                                        suggestionsShown = tipsCount,
                                        feedback = feedbackReport
                                    )
                                    isGeneratingFeedback = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Text("END & GENERATE POST-GAME FEEDBACK", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                CoachState.FEEDBACK -> {
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
                                    Text("POST-CONVERSATION FEEDBACK", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isGeneratingFeedback) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = SleekPrimary)
                                        }
                                    } else {
                                        Text(feedbackReport, style = TextSm.copy(color = SleekTextDark))
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = {
                                                goalInput = ""
                                                scenarioInput = ""
                                                coachState = CoachState.SETUP
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                                        ) {
                                            Text("START NEW SESSION", style = TextSm.copy(fontWeight = FontWeight.Bold))
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
