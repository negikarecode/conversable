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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
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
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerCoachedLobbyScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var state by remember { mutableStateOf("lobby") } // "lobby", "matching", "active", "finished"
    var queueTimerSeconds by remember { mutableStateOf(0) }

    var selectedTopic by remember { mutableStateOf("Rent Control Policy") }
    var opponentName by remember { mutableStateOf("Alex (Level 8)") }

    // Dialogue chat states
    val debateMessages = remember { mutableStateListOf<ChatMessage>() }
    var inputMsg by remember { mutableStateOf("") }
    var isOpponentTyping by remember { mutableStateOf(false) }

    // Referee scores
    var refereeFeedback by remember { mutableStateOf("Waiting for dialogue points...") }

    // Fluctuate match timer
    LaunchedEffect(state) {
        if (state == "matching") {
            queueTimerSeconds = 0
            while (state == "matching") {
                delay(1000)
                queueTimerSeconds++
                if (queueTimerSeconds >= 4) {
                    opponentName = listOf("Taylor (Level 5)", "Derrick (Level 9)", "Sarah (Level 7)").random()
                    debateMessages.clear()
                    debateMessages.add(
                        ChatMessage(
                            id = "referee_init",
                            text = "SYSTEM REFEREE: Welcome to the Peer debate lobby. Today's topic: $selectedTopic. Speak concisely. I will evaluate each turn's logical leverage.",
                            isUser = false
                        )
                    )
                    state = "active"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PEER COACHED ROOMS", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            if (state == "lobby") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("MULTIPLAYER COOPERATIVE ROOMS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Match with active peers for mock debates or sales negotiations. An AI referee will watch and score the conversation quality live.",
                            style = TextXs.copy(color = SleekTextGray, lineHeight = 16.sp)
                        )
                    }
                }

                Text("CHOOSE DEBATE TOPIC", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))

                listOf("Rent Control Policy", "Remote vs In-Office Mandates", "AI Ethics & Copyrights").forEach { topic ->
                    val isSelected = selectedTopic == topic
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedTopic = topic },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SleekPrimary else SleekSurface,
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTopic = topic },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isSelected) Color.White else SleekPrimary,
                                    unselectedColor = SleekTextGray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topic,
                                style = TextXs.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SleekTextDark
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { state = "matching" },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("FIND PEER MATCH", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            } else if (state == "matching") {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SleekPrimary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("FINDING ACTIVE PEER FOR DEBATE...", style = TextXs.copy(color = SleekTextDark, fontWeight = FontWeight.Bold))
                        Text("Queue time: ${queueTimerSeconds}s", style = TextXs.copy(color = SleekTextGray))
                    }
                }
            } else if (state == "active") {
                // Opponent card header
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SleekBorder), contentAlignment = Alignment.Center) {
                                Text(opponentName.take(1), style = TextXs.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opponentName, style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                        }
                        Text("Topic: $selectedTopic", style = TextXs.copy(color = SleekTextGray))
                    }
                }

                // AI Referee live scorecard
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(refereeFeedback, style = TextXs.copy(color = SleekPrimary, fontWeight = FontWeight.Bold))
                    }
                }

                // Active conversation feed
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(debateMessages) { msg ->
                            val isMe = msg.isUser
                            val isSystem = msg.id.startsWith("referee_")
                            if (isSystem) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        msg.text,
                                        style = TextXs.copy(color = SleekTextGray, fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier.background(SleekBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
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
                                            Text(msg.text, style = TextXs.copy(color = if (isMe) SleekBubbleUserText else SleekTextDark))
                                        }
                                    }
                                }
                            }
                        }

                        if (isOpponentTyping) {
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
                            value = inputMsg,
                            onValueChange = { inputMsg = it },
                            placeholder = { Text("Enter your argument...", style = TextXs) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (inputMsg.isNotBlank() && !isOpponentTyping) {
                                            val text = inputMsg
                                            debateMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                                            inputMsg = ""
                                            isOpponentTyping = true

                                            coroutineScope.launch {
                                                // Trigger AI referee to score live
                                                val refereePrompt = "You are a debate referee. Evaluate the User's point: '$text'. State in 10 words or less who has the logical upper hand."
                                                val scoreFeedbackResult = viewModel.queryAiPublic(refereePrompt, "Evaluate.")
                                                refereeFeedback = scoreFeedbackResult ?: "User presented structured argument."

                                                delay(2500) // Simulated opponent delay
                                                val opponentPrompt = "You are the opponent $opponentName in a debate about $selectedTopic. Counter the User's point: '$text'. Keep it under 40 words."
                                                val reply = viewModel.queryAiPublic(opponentPrompt, "Draft counter-argument.")
                                                debateMessages.add(
                                                    ChatMessage(
                                                        id = UUID.randomUUID().toString(),
                                                        text = reply ?: "I disagree. The economic tradeoffs do not support that.",
                                                        isUser = false
                                                    )
                                                )
                                                isOpponentTyping = false
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
                    onClick = { state = "lobby" },
                    modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                ) {
                    Text("EXIT ACTIVE ROOM", style = TextXs.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
