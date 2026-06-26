package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.db.AnalyzedChatEntity
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealChatAnalyzerScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val analyzedChats by viewModel.analyzedChats.collectAsState()
    
    var selectedAnalysis by remember { mutableStateOf<AnalyzedChatEntity?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    var chatTitle by remember { mutableStateOf("") }
    
    // Privacy Controls
    var anonymizeNames by remember { mutableStateOf(true) }
    var scrubTimestamps by remember { mutableStateOf(true) }
    var scrubLocations by remember { mutableStateOf(false) }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Interactive Rewrite Bottom Sheet State
    var selectedMsgToRewrite by remember { mutableStateOf<String?>(null) }
    var showRewriteSheet by remember { mutableStateOf(false) }
    var generatedRewrites by remember { mutableStateOf<Map<String, String>?>(null) }
    var isRewriting by remember { mutableStateOf(false) }

    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REAL CHAT ANALYZER", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedAnalysis != null) {
                            selectedAnalysis = null
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
            if (selectedAnalysis != null) {
                // Analysis Result View
                val report = remember(selectedAnalysis) {
                    try {
                        val adapter = moshi.adapter(Map::class.java)
                        val map = adapter.fromJson(selectedAnalysis!!.analysisResultJson)
                        map
                    } catch (e: Exception) {
                        null
                    }
                }

                if (report != null) {
                    val score = (report["relationshipScore"] as? Double ?: 50.0).toInt()
                    val summary = report["relationshipSummary"] as? String ?: "No summary available."
                    val communicationStyle = report["communicationStyle"] as? String ?: "Balanced"
                    val actionableAdvice = report["actionableAdvice"] as? String ?: "Continue practicing."
                    
                    val redFlags = (report["redFlags"] as? List<*>)?.mapNotNull { item ->
                        val m = item as? Map<*, *>
                        if (m != null) {
                            Triple(m["flag"] as? String ?: "", m["quote"] as? String ?: "", m["explanation"] as? String ?: "")
                        } else null
                    } ?: emptyList()
                    
                    val greenFlags = (report["greenFlags"] as? List<*>)?.mapNotNull { item ->
                        val m = item as? Map<*, *>
                        if (m != null) {
                            Triple(m["flag"] as? String ?: "", m["quote"] as? String ?: "", m["explanation"] as? String ?: "")
                        } else null
                    } ?: emptyList()

                    val messages = (report["messages"] as? List<*>)?.mapNotNull { item ->
                        val m = item as? Map<*, *>
                        if (m != null) {
                            Pair(m["sender"] as? String ?: "Speaker", m["text"] as? String ?: "")
                        } else null
                    } ?: emptyList()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Hero Score Card
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
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("RELATIONSHIP HEALTH INDEX", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(100.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { score / 100f },
                                            modifier = Modifier.fillMaxSize(),
                                            color = SleekPrimary,
                                            strokeWidth = 6.dp,
                                            trackColor = SleekBorder,
                                        )
                                        Text(
                                            text = "$score%",
                                            style = DisplayLg.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = summary,
                                        style = TextSm.copy(color = SleekTextDark),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Primary Dynamic: $communicationStyle",
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary)
                                    )
                                }
                            }
                        }

                        // Flags Section (Red / Green)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("FLAGS DETECTED", style = TextSm.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Red Flags List
                                if (redFlags.isNotEmpty()) {
                                    Text("RED FLAGS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.padding(bottom = 4.dp))
                                    redFlags.forEach { (flag, quote, explanation) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                            border = BorderStroke(1.dp, SleekBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Outlined.Warning, contentDescription = "Red Flag", tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(flag, style = TextSm.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("\"$quote\"", style = TextXs.copy(fontWeight = FontWeight.Medium, color = SleekTextGray))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(explanation, style = TextXs.copy(color = SleekTextDark))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Green Flags List
                                if (greenFlags.isNotEmpty()) {
                                    Text("GREEN FLAGS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.padding(bottom = 4.dp))
                                    greenFlags.forEach { (flag, quote, explanation) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                            border = BorderStroke(1.dp, SleekBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Green Flag", tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(flag, style = TextSm.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("\"$quote\"", style = TextXs.copy(fontWeight = FontWeight.Medium, color = SleekTextGray))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(explanation, style = TextXs.copy(color = SleekTextDark))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Actionable Advice Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("COACH RECOMMENDATIONS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(actionableAdvice, style = TextSm.copy(color = SleekTextDark))
                                }
                            }
                        }

                        // Chat transcript with interactive rewrites
                        item {
                            Text("TAP A MESSAGE TO REWRITE WITH AI", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(vertical = 8.dp))
                        }

                        items(messages) { (sender, text) ->
                            val isUser = sender.lowercase() == "user" || sender.lowercase() == "me"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedMsgToRewrite = text
                                        generatedRewrites = null
                                        showRewriteSheet = true
                                        isRewriting = true
                                        coroutineScope.launch {
                                            // Call API to generate rewrites in 3 styles
                                            val empathyRewrite = viewModel.generateRewritePublic(text, "highly empathetic and understanding", "Real Chat Analyzer") ?: text
                                            val assertiveRewrite = viewModel.generateRewritePublic(text, "direct, assertive, and boundary-setting", "Real Chat Analyzer") ?: text
                                            val playfulRewrite = viewModel.generateRewritePublic(text, "playful, lighthearted, and witty", "Real Chat Analyzer") ?: text
                                            generatedRewrites = mapOf(
                                                "Empathetic" to empathyRewrite,
                                                "Assertive" to assertiveRewrite,
                                                "Playful" to playfulRewrite
                                            )
                                            isRewriting = false
                                        }
                                    },
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) SleekBubbleUser else SleekBubblePartner
                                    ),
                                    border = if (isUser) null else BorderStroke(1.dp, SleekBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(sender, style = TextXs.copy(fontWeight = FontWeight.Bold, color = if (isUser) SleekBubbleUserText else SleekTextGray))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text, style = TextSm.copy(color = if (isUser) SleekBubbleUserText else SleekTextDark))
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    viewModel.deleteAnalyzedChat(selectedAnalysis!!.id)
                                    selectedAnalysis = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                                border = BorderStroke(1.dp, SleekBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Report", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            } else {
                // Analysis Setup & History View
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
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ANALYZE REAL CONVERSATION", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = chatTitle,
                                    onValueChange = { chatTitle = it },
                                    label = { Text("Conversation Title (e.g. Chat with Partner)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Paste conversation messages here...\nFormat example:\nUser: Hi there\nOther: Hello, how are you?") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = SleekBorder
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Source shortcuts
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("WhatsApp", "Telegram", "Discord", "Email", "Screenshot").forEach { source ->
                                        OutlinedCard(
                                            onClick = {
                                                if (source == "Screenshot") {
                                                    // Simulated OCR Autofill
                                                    chatInputText = "Me: Hey, are we still on for dinner tonight?\nTaylor: I don't know. I'm really tired and busy. Why do you always ask last minute?\nMe: I asked yesterday afternoon too, though...\nTaylor: Whatever, forget it. Fine, let's go."
                                                    chatTitle = "Dinner Objections"
                                                } else {
                                                    chatInputText = "Me: [Imported from $source]\nMe: Let's discuss our project division.\nAlex: I think I should do the presentation because you sound too nervous.\nMe: That feels unfair. I worked on 80% of the content.\nAlex: Well, if we want an A, we need someone confident speaking."
                                                    chatTitle = "Project Division ($source)"
                                                }
                                            },
                                            modifier = Modifier.weight(1f).padding(2.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                            border = BorderStroke(1.dp, SleekBorder)
                                        ) {
                                            Box(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text(source, style = TextXs.copy(fontWeight = FontWeight.Medium), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Privacy Controls Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                    border = BorderStroke(1.dp, SleekBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("PRIVACY CONTROLS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Anonymize Participant Names", style = TextXs.copy(color = SleekTextDark))
                                            Switch(
                                                checked = anonymizeNames,
                                                onCheckedChange = { anonymizeNames = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = SleekPrimary)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Strip Timestamps & Meta", style = TextXs.copy(color = SleekTextDark))
                                            Switch(
                                                checked = scrubTimestamps,
                                                onCheckedChange = { scrubTimestamps = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = SleekPrimary)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Scrub Locations", style = TextXs.copy(color = SleekTextDark))
                                            Switch(
                                                checked = scrubLocations,
                                                onCheckedChange = { scrubLocations = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = SleekPrimary)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (chatInputText.isNotBlank()) {
                                            isAnalyzing = true
                                            coroutineScope.launch {
                                                val source = if (chatTitle.contains("WhatsApp")) "WhatsApp" else "CopyPaste"
                                                val title = if (chatTitle.isNotBlank()) chatTitle else "Analyzed Conversation"
                                                viewModel.analyzeChat(
                                                    rawText = chatInputText,
                                                    source = source,
                                                    anonymize = anonymizeNames,
                                                    title = title
                                                )
                                                isAnalyzing = false
                                                chatInputText = ""
                                                chatTitle = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = chatInputText.isNotBlank() && !isAnalyzing,
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("ANALYZE CONVERSATION", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    // History Section
                    item {
                        Text("PAST ANALYSIS HISTORY", style = TextSm.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }

                    if (analyzedChats.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No previous chat analyses found. Submit your first chat above!", style = TextXs.copy(color = SleekTextGray))
                                }
                            }
                        }
                    } else {
                        items(analyzedChats) { chat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { selectedAnalysis = chat },
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chat.chatTitle, style = TextSm.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Source: ${chat.source} • Analyzed on ${
                                                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                    .format(java.util.Date(chat.timestamp))
                                            }",
                                            style = TextXs.copy(color = SleekTextGray)
                                        )
                                    }
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Details", tint = SleekPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Rewrite Bottom Sheet Dialog (Minimal Compose Dialog fallback for compatibility)
            if (showRewriteSheet) {
                AlertDialog(
                    onDismissRequest = { showRewriteSheet = false },
                    title = { Text("AI REWRITE SUGGESTIONS", style = TextSm.copy(fontWeight = FontWeight.Bold)) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Original message: \"$selectedMsgToRewrite\"", style = TextXs.copy(color = SleekTextGray))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isRewriting) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = SleekPrimary)
                                }
                            } else {
                                generatedRewrites?.forEach { (styleName, rewrittenText) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = SleekBackground),
                                        border = BorderStroke(1.dp, SleekBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(styleName.uppercase(), style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(rewrittenText, style = TextXs.copy(color = SleekTextDark))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRewriteSheet = false }) {
                            Text("Done", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = SleekSurface
                )
            }
        }
    }
}
