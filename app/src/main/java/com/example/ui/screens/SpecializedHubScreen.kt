package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Scenario
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecializedHubScreen(
    viewModel: ConversableViewModel,
    hubType: String, // "Interview", "Negotiation", "Relationship", "Personality", "Career", "Social"
    onBack: () -> Unit,
    onStartScenario: (Scenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val title = when (hubType) {
        "Interview" -> "INTERVIEW HUB"
        "Negotiation" -> "NEGOTIATION SIMULATOR"
        "Relationship" -> "RELATIONSHIP SIMULATOR"
        "Personality" -> "PERSONALITY SIMULATOR"
        "Career" -> "CAREER COACH"
        else -> "SOCIAL CONFIDENCE TRAINER"
    }

    val subtitle = when (hubType) {
        "Interview" -> "Practice mock HR, technical, and resume-based interviews with realistic AI panel members."
        "Negotiation" -> "Sharpen your leverage and tactics for salary, rent, business sales, and conflict resolution."
        "Relationship" -> "Navigate evolving family, friendship, and romantic dynamics where trust changes over time."
        "Personality" -> "Simulate conversations with challenging traits: Passive-Aggressive, Narcissistic, Anxious, or Cold."
        "Career" -> "Master professional dialogue, performance reviews, promotion pitches, and team management."
        else -> "Build everyday confidence starting conversations at gyms, coffee shops, and networking mixers."
    }

    // Static curated scenarios for each hub
    val curatedScenarios = remember(hubType) {
        when (hubType) {
            "Interview" -> listOf(
                Scenario(
                    id = "hub_int_hr",
                    title = "Senior Manager Fit Interview",
                    category = "Interview",
                    partnerName = "Sarah (HR Manager)",
                    partnerAvatar = "H",
                    partnerPersona = "Experienced HR director. Highly professional, looks for structured STAR-method behavioral replies, values team collaboration and stress management.",
                    scenarioDescription = "Job behavioral interview. Answer her questions concisely.",
                    hiddenGoal = "Provide a structured behavioral response using the STAR method for conflict resolution.",
                    difficulty = "Medium",
                    initialMessage = "Welcome. Thanks for taking the time today. Let's start by discussing a time you had a major disagreement with a project lead. How did you handle it?"
                ),
                Scenario(
                    id = "hub_int_tech",
                    title = "System Architecture Mock Panel",
                    category = "Interview",
                    partnerName = "Devin (Principal Engineer)",
                    partnerAvatar = "T",
                    partnerPersona = "No-nonsense tech lead. Bothered by buzzwords or hand-waving explanations. Looks for specific tradeoffs in scalability, database choices, and latency.",
                    scenarioDescription = "Technical mockup review. Defend your design decisions.",
                    hiddenGoal = "Explain architectural tradeoffs clearly, showing self-correction when pressed on latency pitfalls.",
                    difficulty = "Hard",
                    initialMessage = "Hey. Let's get straight to it. Walk me through how you would design a rate-limiter for a service receiving 100k requests per second."
                )
            )
            "Negotiation" -> listOf(
                Scenario(
                    id = "hub_neg_salary",
                    title = "Annual Compensation Review",
                    category = "Negotiation",
                    partnerName = "Richard (VP of Operations)",
                    partnerAvatar = "N",
                    partnerPersona = "VP focused on strict budget constraints. Respects quantitative data showing return on investment. Defensive if salaries are compared without metrics.",
                    scenarioDescription = "Negotiating a raise. Present your achievements.",
                    hiddenGoal = "Secure a 10% compensation increase by presenting 3 quantified contributions and handling budget objections calmly.",
                    difficulty = "Hard",
                    initialMessage = "Hi. Glad we could connect. I reviewed your compensation increase request. Honestly, with our current Q3 overhead, our budget is extremely tight."
                ),
                Scenario(
                    id = "hub_neg_rent",
                    title = "Disputing a Rent Increase",
                    category = "Negotiation",
                    partnerName = "Mrs. Gable (Landlord)",
                    partnerAvatar = "L",
                    partnerPersona = "Grumpy property owner. Values long-term tenant stability and minimal maintenance calls, but complains about rising property tax rates.",
                    scenarioDescription = "Discussing lease renewal. Limit the rent bump.",
                    hiddenGoal = "Keep the monthly rent increase below 3% by leveraging your flawless payment history and minimal repair requests.",
                    difficulty = "Medium",
                    initialMessage = "Hello. Yes, as I wrote in the renewal letter, property costs have risen, so rent will be going up by 8% starting next month."
                )
            )
            "Relationship" -> listOf(
                Scenario(
                    id = "hub_rel_partner",
                    title = "Evolving Relationship Balance",
                    category = "Relationship",
                    partnerName = "Taylor",
                    partnerAvatar = "P",
                    partnerPersona = "Your long-term partner. Feeling overwhelmed by chores and career stress. Needs emotional validation, active listening, and a collaborative effort rather than dry advice.",
                    scenarioDescription = "Addressing relationship balance. Show support.",
                    hiddenGoal = "Build connection score above 80 by validating Taylor's stress and agreeing on a concrete chore redistribution.",
                    difficulty = "Medium",
                    initialMessage = "Hey... do you have a second? I'm just looking at the calendar. I feel like I'm handling everything around the apartment lately, and I'm exhausted."
                ),
                Scenario(
                    id = "hub_rel_sibling",
                    title = "Sibling Reunion Bound",
                    category = "Relationship",
                    partnerName = "Jamie (Sibling)",
                    partnerAvatar = "S",
                    partnerPersona = "Defensive sibling. Often feels compared to you by parents. Quick to take advice as condescending, but responds well to shared childhood memories and vulnerability.",
                    scenarioDescription = "Reconnecting after a disagreement. Resolve sibling tension.",
                    hiddenGoal = "Restore sibling rapport and secure a promise to spend holidays together without parenting topics taking over.",
                    difficulty = "Easy",
                    initialMessage = "Oh, hey. I didn't expect you to call. Mom said you were going to check in. What's up?"
                )
            )
            "Personality" -> listOf(
                Scenario(
                    id = "hub_per_passive",
                    title = "The Passive-Aggressive Reviewer",
                    category = "Personality",
                    partnerName = "Gavin (Design Lead)",
                    partnerAvatar = "A",
                    partnerPersona = "Extremely passive-aggressive. Avoids direct feedback, uses sarcastic filler, and drops hints instead of stating requests. Requires calm, direct verification.",
                    scenarioDescription = "Reviewing design changes. Clarify the feedback.",
                    hiddenGoal = "Get Gavin to state his specific design concerns directly without reacting defensively to his sarcasm.",
                    difficulty = "Hard",
                    initialMessage = "Oh, hi. No, the changes you made are... interesting. I mean, if that's the direction you really think is best, who am I to say otherwise?"
                ),
                Scenario(
                    id = "hub_per_narcissist",
                    title = "The Egotistical Project Director",
                    category = "Personality",
                    partnerName = "Victoria",
                    partnerAvatar = "N",
                    partnerPersona = "Self-centered and dismissive of others' ideas. Likes when her vision is praised first, but respects professionals who hold their ground using objective metrics.",
                    scenarioDescription = "Aligning project strategy. Hold your boundaries.",
                    hiddenGoal = "Defend your division of work without getting defensive, while framing recommendations as supportive of Victoria's overall vision.",
                    difficulty = "Hard",
                    initialMessage = "Look, I've already mapped out the entire presentation structure. You can handle compiling the appendix slides at the end. That should be easy enough for you, right?"
                )
            )
            "Career" -> listOf(
                Scenario(
                    id = "hub_car_promotion",
                    title = "The Promotion Proposal",
                    category = "Career",
                    partnerName = "Derrick (Director)",
                    partnerAvatar = "C",
                    partnerPersona = "Results-driven manager. Values leadership initiative, team alignment, and client satisfaction. Reluctant to promote without clear examples of cross-team coordination.",
                    scenarioDescription = "Proposing a step up. Outline your readiness.",
                    hiddenGoal = "Secure Derrick's sponsorship for promotion by presenting two clear leadership examples and outline next steps.",
                    difficulty = "Medium",
                    initialMessage = "Hey. Happy to check in. You mentioned in your calendar invite that you wanted to discuss your career path for next fiscal year?"
                )
            )
            else -> listOf( // Social
                Scenario(
                    id = "hub_soc_stranger",
                    title = "Coffee Shop Queue Chat",
                    category = "Social",
                    partnerName = "Alex (Stranger)",
                    partnerAvatar = "S",
                    partnerPersona = "Friendly local resident. Polite but initially in their own world. Easily engaged by observations about local architecture, weather, or menu recommendations.",
                    scenarioDescription = "Waiting in line together. Start a casual chat.",
                    hiddenGoal = "Initiate a friendly exchange, share a laugh, and part ways with a warm greeting.",
                    difficulty = "Easy",
                    initialMessage = "(Adjusting umbrella, looking outside) Wow, the rain really started pouring out of nowhere today."
                )
            )
        }
    }

    var customPrompt by remember { mutableStateOf("") }
    var isGeneratingCustom by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            // Header Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(subtitle, style = TextSm.copy(color = SleekTextDark))
                }
            }

            Text("CURATED PRACTICE SCENARIOS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scenario Cards
                items(curatedScenarios) { scenario ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    scenario.title,
                                    style = TextSm.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = SleekBackground,
                                    border = BorderStroke(1.dp, SleekBorder)
                                ) {
                                    Text(
                                        scenario.difficulty.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(scenario.partnerPersona, style = TextXs.copy(color = SleekTextGray), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = SleekTextGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Goal: ${scenario.hiddenGoal}", style = TextXs.copy(color = SleekTextGray), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                                }
                                OutlinedButton(
                                    onClick = { onStartScenario(scenario) },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SleekPrimary, contentColor = Color.White),
                                    border = BorderStroke(1.dp, SleekPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("START MOCK", style = TextXs.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // AI Generator Card inside the Hub
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Build, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI CUSTOM ${title} GENERATOR", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = { customPrompt = it },
                                placeholder = { Text("What specific scenario do you want to practice?\ne.g. Interview for a Product Designer role at Figma") },
                                modifier = Modifier.fillMaxWidth().height(70.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (customPrompt.isNotBlank()) {
                                        isGeneratingCustom = true
                                        coroutineScope.launch {
                                            viewModel.generateScenarioWithAi("$hubType - $customPrompt") { created ->
                                                if (created != null) {
                                                     val modelScenario = Scenario(
                                                         id = created.id,
                                                         title = created.title,
                                                         category = created.category,
                                                         partnerName = created.partnerName,
                                                         partnerAvatar = created.partnerAvatar,
                                                         partnerPersona = created.systemPrompt,
                                                         scenarioDescription = created.description,
                                                         hiddenGoal = created.systemPrompt,
                                                         difficulty = created.difficulty,
                                                         initialMessage = created.initialMessage
                                                     )
                                                    onStartScenario(modelScenario)
                                                } else {
                                                    Toast.makeText(context, "Generation failed. Try again.", Toast.LENGTH_SHORT).show()
                                                }
                                                isGeneratingCustom = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = customPrompt.isNotBlank() && !isGeneratingCustom,
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                            ) {
                                if (isGeneratingCustom) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("GENERATE CUSTOM SCENARIO", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
