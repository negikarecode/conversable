package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ConversationSession
import com.example.data.model.Scenario
import com.example.data.model.ScenarioCatalog
import androidx.activity.compose.BackHandler
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel

enum class DashboardView {
    WELCOME,
    HISTORY,
    HISTORY_DETAIL,
    PROFILE,
    PERSONA_CLONE,
    LANGUAGE_SELECT,
    COMMUNICATION_DNA,
    SAVED_LESSONS,
    DAILY_CHALLENGES,
    DAILY_CHALLENGE_INTRO,
    STREAK_DASHBOARD,
    REAL_CHAT_ANALYZER,
    LIVE_COACH,
    AI_MEMORY,
    SCENARIO_MARKETPLACE,
    AI_REWRITE_STUDIO,
    INTERVIEW_HUB,
    PUBLIC_SPEAKING_COACH,
    NEGOTIATION_SIMULATOR,
    AI_MENTOR,
    RELATIONSHIP_SIMULATOR,
    PERSONALITY_SIMULATOR,
    CAREER_COACH,
    SOCIAL_CONFIDENCE_TRAINER,
    INSIGHTS_DASHBOARD,
    GROUP_MEETING_SIMULATOR,
    VOCAL_CAMERA_COACH,
    CONTACT_WARMTH_REMINDERS,
    LOCAL_DIALECT_TRAINER,
    PEER_COACHED_LOBBY
}

@Composable
fun DashboardScreen(
    viewModel: ConversableViewModel,
    username: String,
    email: String,
    age: Int,
    gender: String,
    onStartScenario: (Scenario) -> Unit,
    onSignOut: () -> Unit,
    onViewPastFeedback: (ConversationSession) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeView by remember { mutableStateOf(DashboardView.WELCOME) }
    var selectedScenarioForDetail by remember { mutableStateOf<Scenario?>(null) }
    var selectedSavedSession by remember { mutableStateOf<com.example.data.model.SavedSession?>(null) }
    var selectedHubType by remember { mutableStateOf("Interview") }
    
    var selectedCategory by remember { mutableStateOf("All") }
    val allScenariosFlow by viewModel.allScenariosFlow.collectAsState()
    
    val filteredScenarios = remember(selectedCategory, allScenariosFlow) {
        if (selectedCategory == "All") {
            allScenariosFlow
        } else {
            allScenariosFlow.filter { scenario: Scenario -> 
                scenario.category.contains(selectedCategory, ignoreCase = true) ||
                (selectedCategory == "Professional" && (scenario.category.contains("Professional", ignoreCase = true) || scenario.category.contains("Work", ignoreCase = true) || scenario.category.contains("Career", ignoreCase = true))) ||
                (selectedCategory == "Conflict" && scenario.category.contains("Conflict", ignoreCase = true))
            }
        }
    }

    val compactLazyListState = rememberLazyListState()
    var isArchitectCardExpanded by remember { mutableStateOf(false) }
    var customScenarioInput by remember { mutableStateOf("") }
    
    val sessions by viewModel.allSessions.collectAsState()
    val sessionCount = sessions.size
    
    var showCustomScenarioSheet by remember { mutableStateOf(false) }
    val isGeneratingScenario by viewModel.isGeneratingScenario.collectAsState()
    
    var showWelcomeTooltip by remember { mutableStateOf(true) }
    var showDiscardScenarioDialog by remember { mutableStateOf(false) }
    var lastBackTime by remember { mutableStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // 1. Double-back press to exit at root (WELCOME view) when sheet is closed
    BackHandler(enabled = activeView == DashboardView.WELCOME && !showCustomScenarioSheet) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackTime = currentTime
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Navigation-back inside DashboardScreen
    BackHandler(enabled = activeView != DashboardView.WELCOME || showCustomScenarioSheet) {
        if (showCustomScenarioSheet) {
            if (customScenarioInput.isNotBlank()) {
                showDiscardScenarioDialog = true
            } else {
                showCustomScenarioSheet = false
            }
        } else {
            when (activeView) {
                DashboardView.HISTORY_DETAIL -> {
                    activeView = DashboardView.HISTORY
                }
                DashboardView.HISTORY, DashboardView.PROFILE, DashboardView.PERSONA_CLONE, DashboardView.LANGUAGE_SELECT, DashboardView.COMMUNICATION_DNA, DashboardView.SAVED_LESSONS, DashboardView.DAILY_CHALLENGES, DashboardView.DAILY_CHALLENGE_INTRO, DashboardView.STREAK_DASHBOARD, DashboardView.REAL_CHAT_ANALYZER, DashboardView.LIVE_COACH, DashboardView.AI_MEMORY, DashboardView.SCENARIO_MARKETPLACE, DashboardView.AI_REWRITE_STUDIO, DashboardView.INTERVIEW_HUB, DashboardView.PUBLIC_SPEAKING_COACH, DashboardView.NEGOTIATION_SIMULATOR, DashboardView.AI_MENTOR, DashboardView.RELATIONSHIP_SIMULATOR, DashboardView.PERSONALITY_SIMULATOR, DashboardView.CAREER_COACH, DashboardView.SOCIAL_CONFIDENCE_TRAINER, DashboardView.INSIGHTS_DASHBOARD, DashboardView.GROUP_MEETING_SIMULATOR, DashboardView.VOCAL_CAMERA_COACH, DashboardView.CONTACT_WARMTH_REMINDERS, DashboardView.LOCAL_DIALECT_TRAINER, DashboardView.PEER_COACHED_LOBBY -> {
                    activeView = DashboardView.WELCOME
                }
                else -> {}
            }
        }
    }
    
    // Auto-dismiss welcome tooltip when list scrolls
    LaunchedEffect(compactLazyListState.firstVisibleItemIndex, compactLazyListState.firstVisibleItemScrollOffset) {
        if (compactLazyListState.firstVisibleItemIndex > 0 || compactLazyListState.firstVisibleItemScrollOffset > 50) {
            showWelcomeTooltip = false
        }
    }

    // Auto-close bottom sheet on success scenario generation
    LaunchedEffect(allScenariosFlow) {
        if (showCustomScenarioSheet && !isGeneratingScenario && customScenarioInput.isNotEmpty()) {
            // Find newly generated scenario
            val newlyCreated = allScenariosFlow.firstOrNull { it.id.startsWith("custom_") }
            if (newlyCreated != null) {
                showCustomScenarioSheet = false
                customScenarioInput = ""
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Main centered viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            when (activeView) {
                DashboardView.WELCOME -> {
                    CompactDashboardColumn(
                        viewModel = viewModel,
                        username = username,
                        email = email,
                        selectedCategory = selectedCategory,
                        onSelectedCategoryChange = { selectedCategory = it },
                        filteredScenarios = filteredScenarios,
                        sessionCount = sessionCount,
                        onStartScenario = { s -> onStartScenario(s) },
                        onHistoryLinkClick = { activeView = DashboardView.HISTORY },
                        onCreateScenarioClick = { showCustomScenarioSheet = true },
                        onSignOut = onSignOut,
                        lazyListState = compactLazyListState,
                        showWelcomeTooltip = showWelcomeTooltip,
                        onProfileClick = { activeView = DashboardView.PROFILE },
                        onPersonaCloneClick = { activeView = DashboardView.PERSONA_CLONE },
                        onLanguageClick = { activeView = DashboardView.LANGUAGE_SELECT },
                        onDnaClick = { activeView = DashboardView.COMMUNICATION_DNA },
                        onSavedLessonsClick = { activeView = DashboardView.SAVED_LESSONS },
                        onDailyChallengesHubClick = { activeView = DashboardView.DAILY_CHALLENGES },
                        onStreakCenterClick = { activeView = DashboardView.STREAK_DASHBOARD },
                        onRealChatAnalyzerClick = { activeView = DashboardView.REAL_CHAT_ANALYZER },
                        onLiveCoachClick = { activeView = DashboardView.LIVE_COACH },
                        onAiMemoryClick = { activeView = DashboardView.AI_MEMORY },
                        onMarketplaceClick = { activeView = DashboardView.SCENARIO_MARKETPLACE },
                        onRewriteStudioClick = { activeView = DashboardView.AI_REWRITE_STUDIO },
                        onPublicSpeakingClick = { activeView = DashboardView.PUBLIC_SPEAKING_COACH },
                        onInsightsDashboardClick = { activeView = DashboardView.INSIGHTS_DASHBOARD },
                        onAiMentorClick = { activeView = DashboardView.AI_MENTOR },
                        onGroupMeetingClick = { activeView = DashboardView.GROUP_MEETING_SIMULATOR },
                        onVocalCameraCoachClick = { activeView = DashboardView.VOCAL_CAMERA_COACH },
                        onContactWarmthClick = { activeView = DashboardView.CONTACT_WARMTH_REMINDERS },
                        onLocalDialectClick = { activeView = DashboardView.LOCAL_DIALECT_TRAINER },
                        onPeerCoachedLobbyClick = { activeView = DashboardView.PEER_COACHED_LOBBY },
                        onSpecializedHubClick = { type ->
                            selectedHubType = type
                            activeView = when (type) {
                                "Interview" -> DashboardView.INTERVIEW_HUB
                                "Negotiation" -> DashboardView.NEGOTIATION_SIMULATOR
                                "Relationship" -> DashboardView.RELATIONSHIP_SIMULATOR
                                "Personality" -> DashboardView.PERSONALITY_SIMULATOR
                                "Career" -> DashboardView.CAREER_COACH
                                else -> DashboardView.SOCIAL_CONFIDENCE_TRAINER
                            }
                        }
                    )
                }
                DashboardView.HISTORY -> {
                    com.example.ui.screens.HistoryScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME },
                        onViewDetail = { s ->
                            selectedSavedSession = s
                            activeView = DashboardView.HISTORY_DETAIL
                        }
                    )
                }
                DashboardView.HISTORY_DETAIL -> {
                    selectedSavedSession?.let { s ->
                        com.example.ui.screens.SessionDetailScreen(
                            session = s,
                            onBack = { activeView = DashboardView.HISTORY }
                        )
                    } ?: run {
                        activeView = DashboardView.HISTORY
                    }
                }
                DashboardView.PROFILE -> {
                    com.example.ui.screens.ProfileScreen(
                        viewModel = viewModel,
                        username = username,
                        email = email,
                        onBack = { activeView = DashboardView.WELCOME },
                        onSignOut = onSignOut,
                        onLanguageClick = { activeView = DashboardView.LANGUAGE_SELECT }
                    )
                }
                DashboardView.PERSONA_CLONE -> {
                    com.example.ui.screens.PersonaCloneScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.LANGUAGE_SELECT -> {
                    com.example.ui.screens.LanguageSelectionScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.COMMUNICATION_DNA -> {
                    com.example.ui.screens.CommunicationDnaScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME },
                        onNavigateToScenario = { scenarioId ->
                            val targetScenario = ScenarioCatalog.scenarios.find { it.id == scenarioId }
                            if (targetScenario != null) {
                                onStartScenario(targetScenario)
                            } else {
                                activeView = DashboardView.WELCOME
                            }
                        }
                    )
                }
                DashboardView.SAVED_LESSONS -> {
                    com.example.ui.screens.SavedLessonsScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.DAILY_CHALLENGES -> {
                    com.example.ui.screens.DailyChallengesScreen(
                        viewModel = viewModel,
                        onNavigateToIntro = { activeView = DashboardView.DAILY_CHALLENGE_INTRO },
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.DAILY_CHALLENGE_INTRO -> {
                    com.example.ui.screens.DailyChallengeIntroScreen(
                        viewModel = viewModel,
                        onStart = {
                            viewModel.currentDailyChallenge.value?.let { challenge ->
                                onStartScenario(challenge.toScenario())
                            }
                        },
                        onBack = { activeView = DashboardView.DAILY_CHALLENGES }
                    )
                }
                DashboardView.STREAK_DASHBOARD -> {
                    com.example.ui.screens.StreakDashboardScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.REAL_CHAT_ANALYZER -> {
                    com.example.ui.screens.RealChatAnalyzerScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.LIVE_COACH -> {
                    com.example.ui.screens.LiveCoachScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.AI_MEMORY -> {
                    com.example.ui.screens.AiMemoryScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.SCENARIO_MARKETPLACE -> {
                    com.example.ui.screens.ScenarioMarketplaceScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { scenario ->
                            onStartScenario(scenario)
                        }
                    )
                }
                DashboardView.AI_REWRITE_STUDIO -> {
                    com.example.ui.screens.AiRewriteStudioScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.PUBLIC_SPEAKING_COACH -> {
                    com.example.ui.screens.PublicSpeakingCoachScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.INSIGHTS_DASHBOARD -> {
                    com.example.ui.screens.InsightsDashboardScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.AI_MENTOR -> {
                    com.example.ui.screens.AiMentorScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.INTERVIEW_HUB -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Interview",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.NEGOTIATION_SIMULATOR -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Negotiation",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.RELATIONSHIP_SIMULATOR -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Relationship",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.PERSONALITY_SIMULATOR -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Personality",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.CAREER_COACH -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Career",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.SOCIAL_CONFIDENCE_TRAINER -> {
                    com.example.ui.screens.SpecializedHubScreen(
                        viewModel = viewModel,
                        hubType = "Social",
                        onBack = { activeView = DashboardView.WELCOME },
                        onStartScenario = { onStartScenario(it) }
                    )
                }
                DashboardView.GROUP_MEETING_SIMULATOR -> {
                    com.example.ui.screens.GroupMeetingSimulatorScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.VOCAL_CAMERA_COACH -> {
                    com.example.ui.screens.VocalCameraCoachScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.CONTACT_WARMTH_REMINDERS -> {
                    com.example.ui.screens.ContactWarmthRemindersScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.LOCAL_DIALECT_TRAINER -> {
                    com.example.ui.screens.LocalDialectTrainerScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
                DashboardView.PEER_COACHED_LOBBY -> {
                    com.example.ui.screens.PeerCoachedLobbyScreen(
                        viewModel = viewModel,
                        onBack = { activeView = DashboardView.WELCOME }
                    )
                }
            }
        }

        // Custom Bottom Sheet for "Create Scenario"
        AnimatedVisibility(
            visible = showCustomScenarioSheet,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = EaseOutCubic)
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = EaseInCubic)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { 
                            if (!isGeneratingScenario) {
                                if (customScenarioInput.isNotBlank()) {
                                    showDiscardScenarioDialog = true
                                } else {
                                    showCustomScenarioSheet = false
                                }
                            }
                        })
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = false, onClick = {}) // block taps to parent
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, Border),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Create scenario",
                                style = TextLg.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                            if (!isGeneratingScenario) {
                                TextButton(
                                    onClick = {
                                        if (customScenarioInput.isNotBlank()) {
                                            showDiscardScenarioDialog = true
                                        } else {
                                            showCustomScenarioSheet = false
                                        }
                                    }
                                ) {
                                    Text("Cancel", style = TextSm.copy(color = TextSecondary))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Describe any specific social situation, job interview, or difficult conversation you want to practice. Our AI coach will prepare it instantly.",
                            style = TextSm.copy(color = TextSecondary, lineHeight = 20.sp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = customScenarioInput,
                            onValueChange = { customScenarioInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g., Ask my manager for a project deadline extension...",
                                    style = TextSm.copy(color = TextMuted)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("architect_scenario_input"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isGeneratingScenario,
                            textStyle = TextSm.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                disabledBorderColor = Border,
                                focusedContainerColor = BgInput,
                                unfocusedContainerColor = BgInput,
                                disabledContainerColor = BgInput
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                if (customScenarioInput.isNotBlank()) {
                                    viewModel.generateScenario(
                                        userInput = customScenarioInput,
                                        onSuccess = { _ -> 
                                            // Managed via LaunchedEffect / flow update
                                        },
                                        onError = { _ ->
                                            // Handled internally or silently dismissed
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("generate_scenario_btn"),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            enabled = customScenarioInput.isNotBlank() && !isGeneratingScenario,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = TextOnDark,
                                disabledContainerColor = Border,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            if (isGeneratingScenario) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TextOnDark,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Generate practice scenario", style = TextSm.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        }

        if (showDiscardScenarioDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardScenarioDialog = false },
                containerColor = SleekSurface,
                title = { Text("Discard scenario?", color = SleekTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "You have unsaved changes. If you close this now, your custom scenario details will be lost.",
                        color = SleekTextGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardScenarioDialog = false
                            showCustomScenarioSheet = false
                            customScenarioInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekWarning)
                    ) {
                        Text("Discard", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardScenarioDialog = false }) {
                        Text("Continue Editing", color = SleekTextLightGray)
                    }
                }
            )
        }
    }
}

@Composable
fun CompactDashboardColumn(
    viewModel: ConversableViewModel,
    username: String,
    email: String,
    selectedCategory: String,
    onSelectedCategoryChange: (String) -> Unit,
    filteredScenarios: List<Scenario>,
    sessionCount: Int,
    onStartScenario: (Scenario) -> Unit,
    onHistoryLinkClick: () -> Unit,
    onCreateScenarioClick: () -> Unit,
    onSignOut: () -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    showWelcomeTooltip: Boolean,
    onProfileClick: () -> Unit,
    onPersonaCloneClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onDnaClick: () -> Unit,
    onSavedLessonsClick: () -> Unit,
    onDailyChallengesHubClick: () -> Unit,
    onStreakCenterClick: () -> Unit,
    onRealChatAnalyzerClick: () -> Unit,
    onLiveCoachClick: () -> Unit,
    onAiMemoryClick: () -> Unit,
    onMarketplaceClick: () -> Unit,
    onRewriteStudioClick: () -> Unit,
    onPublicSpeakingClick: () -> Unit,
    onInsightsDashboardClick: () -> Unit,
    onAiMentorClick: () -> Unit,
    onGroupMeetingClick: () -> Unit,
    onVocalCameraCoachClick: () -> Unit,
    onContactWarmthClick: () -> Unit,
    onLocalDialectClick: () -> Unit,
    onPeerCoachedLobbyClick: () -> Unit,
    onSpecializedHubClick: (String) -> Unit
) {
    val totalXp by viewModel.totalXp.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val unlockedBadgeIds by viewModel.unlockedBadgeIds.collectAsState()
    val savedSessions by viewModel.savedSessions.collectAsState()
    val dna = remember(savedSessions) { CommunicationDnaCalculator.calculateDna(savedSessions) }

    val userLevel = getLevelForXp(totalXp)
    val userLevelTitle = getLevelTitle(userLevel)
    val (progress, xpLeft) = getXpProgress(totalXp)

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Redesigned Top Header Panel (Clean Apple Slate visual style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (username.isNotEmpty()) username else "Welcome",
                            style = TextXl.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val selectedLanguageState by viewModel.selectedLanguage.collectAsState()
                        val activeLangObj = remember(selectedLanguageState) {
                            com.example.data.model.LanguageCatalog.languages.find { it.name.equals(selectedLanguageState, ignoreCase = true) }
                        }
                        
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentLight)
                                .clickable { onLanguageClick() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("active_language_header_chip"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = activeLangObj?.flag ?: "GL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedLanguageState,
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AccentText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    // User Menu / Profile trigger
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Border)
                            .clickable { onProfileClick() }
                            .testTag("profile_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (username.isNotEmpty()) username.trim().take(1).uppercase() else "U",
                            style = TextSm.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Spacing and level progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LEVEL $userLevel",
                        style = TextXs.copy(fontWeight = FontWeight.Medium, color = TextSecondary, letterSpacing = 0.06.sp)
                    )
                    Text(
                        text = "$totalXp XP total",
                        style = TextXs.copy(fontWeight = FontWeight.Medium, color = Accent, letterSpacing = 0.06.sp)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Flat Linear Progress Track (No 3D details)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(Accent)
                    )
                }
                
                if (userLevel < 10) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$xpLeft XP until Level ${userLevel + 1}",
                        style = TextXs.copy(color = TextMuted)
                    )
                }
            }
        }

        // Communication DNA Card (FLAGSHIP FEATURE)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDnaClick() }
                        .testTag("communication_dna_home_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AccentLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                                }
                                Column {
                                    Text(
                                        text = "COMMUNICATION DNA",
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText, letterSpacing = 0.5.sp)
                                    )
                                    Text(
                                        text = "Level ${dna.level}",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                }
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = SuccessLight
                            ) {
                                Text(
                                    text = if (dna.weeklyGrowth > 0) "+${dna.weeklyGrowth}% Weekly" else "0% Growth",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Communication IQ", style = TextXs.copy(color = TextMuted))
                                Text(
                                    text = "${dna.communicationIq} / 100",
                                    style = TextLg.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                                )
                            }
                            Column {
                                Text(text = "Your Style", style = TextXs.copy(color = TextMuted))
                                Text(
                                    text = "${dna.primaryStyleEmoji} ${dna.primaryStyle}",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                            }
                            Button(
                                onClick = onDnaClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("dna_view_button")
                            ) {
                                Text("View DNA", style = TextXs.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }
                }
            }
        }

        // Premium Conversation Streak Card
        item {
            val milestoneTarget = when {
                streak < 3 -> 3
                streak < 7 -> 7
                streak < 14 -> 14
                streak < 30 -> 30
                streak < 50 -> 50
                streak < 75 -> 75
                streak < 100 -> 100
                streak < 180 -> 180
                else -> 365
            }
            val progressFraction = if (milestoneTarget > 0) (streak.toFloat() / milestoneTarget).coerceAtMost(1f) else 1f
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStreakCenterClick() }
                        .testTag("conversation_streak_home_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONVERSATION STREAK",
                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText, letterSpacing = 0.5.sp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = AccentLight
                            ) {
                                Text(
                                    text = "Active Habit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Streak",
                                    style = TextXs.copy(color = TextMuted)
                                )
                                Text(
                                    text = "$streak Days",
                                    style = TextXl.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                                )
                            }
                            Column {
                                Text(
                                    text = "Today's Goal",
                                    style = TextXs.copy(color = TextMuted)
                                )
                                Text(
                                    text = "Complete 1 Conversation",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Reward",
                                    style = TextXs.copy(color = TextMuted)
                                )
                                Text(
                                    text = "+50 XP",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = Success)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next Milestone: $milestoneTarget Days",
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}% Progress",
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Progress Bar with a glowing or solid accent fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(Accent)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onStreakCenterClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentLight, contentColor = AccentText),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("view_streak_center_button")
                        ) {
                            Text(
                                text = "Explore Streak & Habit Center",
                                style = TextXs.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Premium Daily Challenge Card
        item {
            val challenge by viewModel.currentDailyChallenge.collectAsState()
            val completedHistory by viewModel.completedDailyChallengesList.collectAsState()
            val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
            val isTodayCompleted = remember(completedHistory) {
                completedHistory.any { it.date == todayStr }
            }

            challenge?.let { activeChallenge ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDailyChallengesHubClick() }
                            .testTag("daily_challenge_home_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = BgCard,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DAILY CHALLENGE",
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = Danger, letterSpacing = 0.5.sp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(AccentLight)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeChallenge.category,
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Today's Mission",
                                style = TextXs.copy(fontWeight = FontWeight.Medium, color = TextSecondary)
                            )
                            Text(
                                text = activeChallenge.title,
                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = activeChallenge.description,
                                style = TextXs.copy(color = TextSecondary),
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("Difficulty", style = TextXs.copy(color = TextMuted))
                                        Row(modifier = Modifier.padding(top = 2.dp)) {
                                            val stars = when (activeChallenge.difficulty) {
                                                "Easy" -> 1
                                                "Medium" -> 2
                                                "Hard" -> 3
                                                else -> 5
                                            }
                                            for (i in 1..3) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (i <= stars) Warning else Border,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text("Est. Time", style = TextXs.copy(color = TextMuted))
                                        Text(
                                            text = "${activeChallenge.estimatedTimeMinutes} Min",
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Column {
                                        Text("Reward", style = TextXs.copy(color = TextMuted))
                                        Text(
                                            text = "+${activeChallenge.xpReward} XP",
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onDailyChallengesHubClick() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTodayCompleted) SuccessLight else Accent,
                                        contentColor = if (isTodayCompleted) Success else Color.White
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp).testTag("home_start_challenge_button")
                                ) {
                                    Text(
                                        text = if (isTodayCompleted) "Completed" else "▶ Start",
                                        style = TextXs.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persona Clone Banner (FEATURE 5: Persona Clone Chat)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPersonaCloneClick() }
                        .testTag("persona_clone_banner_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = Accent,
                    border = BorderStroke(1.dp, Border),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "NEW PREMIUM FEATURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Persona Clone AI",
                            style = TextLg.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Analyze anyone's texting habits and practice simulated conversations with their AI replica before talking to them.",
                            style = TextXs.copy(color = Color.White.copy(alpha = 0.85f), lineHeight = 15.sp)
                        )
                    }
                }
            }
        }

        // Premium Coaching Tools Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SectionTitle(text = "Coaching & Tools")
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Scenario Marketplace Card
                    Card(
                        onClick = onMarketplaceClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Build, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Marketplace", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("AI Scenarios", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Real Chat Analyzer Card
                    Card(
                        onClick = onRealChatAnalyzerClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Chat Analyzer", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Scrub & Inspect", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Live Coach Card
                    Card(
                        onClick = onLiveCoachClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Live Coach", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Real-time Tips", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // AI Memory Card
                    Card(
                        onClick = onAiMemoryClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI Memories", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Manage Context", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // AI Rewrite Studio Card
                    Card(
                        onClick = onRewriteStudioClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Create, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Rewrite Studio", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Tone adjustments", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Public Speaking Coach Card
                    Card(
                        onClick = onPublicSpeakingClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Speaking Coach", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Pace & Pauses", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // AI Mentor Card
                    Card(
                        onClick = onAiMentorClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI Mentor", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Growth journey", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Insights & Stats Dashboard Card
                    Card(
                        onClick = onInsightsDashboardClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("DNA & Insights", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Stats Center", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Specialized Simulators Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SectionTitle(text = "Specialized Simulators")
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Interview Hub Card
                    Card(
                        onClick = { onSpecializedHubClick("Interview") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Interview Hub", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Mock & Technical", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Negotiation Sim Card
                    Card(
                        onClick = { onSpecializedHubClick("Negotiation") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Negotiation Sim", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Salary & Business", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Relationship Sim Card
                    Card(
                        onClick = { onSpecializedHubClick("Relationship") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Relationship Sim", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Dating & Coworkers", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Personality Sim Card
                    Card(
                        onClick = { onSpecializedHubClick("Personality") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Face, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Personality Sim", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Introvert & Extrovert", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Career Coach Card
                    Card(
                        onClick = { onSpecializedHubClick("Career") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Career Coach", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Reviews & Leadership", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Social Confidence Trainer Card
                    Card(
                        onClick = { onSpecializedHubClick("Social") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.ThumbUp, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Social Confidence", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Small Talk & Strangers", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Premium Advanced Labs Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SectionTitle(text = "Advanced Labs")
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Group Meeting Card
                    Card(
                        onClick = onGroupMeetingClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Group Meetings", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Multi-persona boardrooms", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Vocal & Camera Coach Card
                    Card(
                        onClick = onVocalCameraCoachClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Presence Coach", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Vocal & Posture analytics", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Relationship DNA & Warmth Card
                    Card(
                        onClick = onContactWarmthClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Relationship DNA", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Warmth tracking & checkins", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Local Dialect Trainer Card
                    Card(
                        onClick = onLocalDialectClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Dialect Trainer", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Slang & region idioms", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Peer Coached Matchmaking Card
                    Card(
                        onClick = onPeerCoachedLobbyClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Build, contentDescription = null, tint = Accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Peer Co-Op", style = TextXs.copy(fontWeight = FontWeight.Bold))
                            Text("Lobby matching & debates", style = TextXs.copy(color = TextMuted), fontSize = 10.sp)
                        }
                    }

                    // Blank spacer card
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        // Daily Challenge (FEATURE 4)
        item {
            val scenariosCompletedToday by viewModel.scenariosCompletedToday.collectAsState()
            val isChallengeCompleted = scenariosCompletedToday.contains("networking_tech_conference")
            val challengeScenario = remember { com.example.data.model.ScenarioCatalog.scenarios.first { it.id == "networking_tech_conference" } }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SectionTitle(text = "Daily Practice")
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick()
                        .testTag("daily_challenge_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, Border),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "DAILY PRACTICE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.W500,
                                    color = Color(0xFFAEAEB8),
                                    letterSpacing = 0.06.sp
                                )
                                // Timer / Countdown
                                val countdownText by produceState(initialValue = "14h 22m") {
                                    while (true) {
                                        val now = java.util.Calendar.getInstance()
                                        val endOfDay = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, 23)
                                            set(java.util.Calendar.MINUTE, 59)
                                            set(java.util.Calendar.SECOND, 59)
                                            set(java.util.Calendar.MILLISECOND, 999)
                                        }
                                        val diffMs = endOfDay.timeInMillis - now.timeInMillis
                                        if (diffMs <= 0) {
                                            value = "0h 00m"
                                        } else {
                                            val h = diffMs / 3600000
                                            val m = (diffMs % 3600000) / 60000
                                            value = "${h}h ${m}m"
                                        }
                                        kotlinx.coroutines.delay(1000)
                                    }
                                }
                                Text(
                                    text = "•  Ends in $countdownText",
                                    fontSize = 11.sp,
                                    color = Color(0xFFAEAEB8)
                                )
                            }

                            // XP Bonus Pill: "+5" only
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = BgInput
                            ) {
                                Text(
                                    text = "+5",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.W600,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = challengeScenario.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = challengeScenario.scenarioDescription,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyPill(difficulty = challengeScenario.difficulty)

                            if (isChallengeCompleted) {
                                Text(
                                    text = "✓ Completed",
                                    color = Success,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Button(
                                    onClick = { onStartScenario(challengeScenario) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Start",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Row of Unlocked Badges (Part 8)
        if (unlockedBadgeIds.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT BADGES",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W500,
                        color = TextMuted,
                        letterSpacing = 0.06.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(unlockedBadgeIds.toList()) { badgeId ->
                        RecentBadgeCard(badgeId = badgeId)
                    }
                }
            }
        }

        // History Row Link (Part 9)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onHistoryLinkClick() }
                    .testTag("history_link_row"),
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Border),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClockIcon(tint = TextSecondary, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Session history",
                                style = TextBase.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (sessionCount == 1) "1 session completed" else "$sessionCount sessions completed",
                                style = TextSm.copy(color = TextSecondary)
                            )
                        }
                    }
                    ChevronIcon(tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Custom Scenario Link (Part 10)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onCreateScenarioClick() }
                    .testTag("custom_scenario_architect_card"),
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Border),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlusIcon(tint = Accent, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Create scenario",
                                style = TextBase.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Describe any situation to practice",
                                style = TextSm.copy(color = TextSecondary)
                            )
                        }
                    }
                    ChevronIcon(tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Saved Lessons Card (Part 9.5)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onSavedLessonsClick() }
                    .testTag("saved_lessons_home_card"),
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Border),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Saved lessons handbook",
                                style = TextBase.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val lessons by viewModel.savedLessons.collectAsState()
                            Text(
                                text = if (lessons.isEmpty()) "No bookmarked insights yet" else "${lessons.size} curated lessons",
                                style = TextSm.copy(color = TextSecondary)
                            )
                        }
                    }
                    ChevronIcon(tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Category Filter Horizontal Row (Part 6)
        item {
            SectionTitle(text = "Practice")
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("category_filter_row")
            ) {
                items(listOf("All", "Dating", "Small Talk", "Networking", "Professional", "Conflict")) { category ->
                    CategoryChip(
                        categoryName = category,
                        isSelected = selectedCategory == category,
                        onClick = { onSelectedCategoryChange(category) }
                    )
                }
            }
        }

        // Practice Scenarios list
        itemsIndexed(filteredScenarios) { index, scenario ->
            if (index == 0 && showWelcomeTooltip) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    WelcomeTooltip()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
            ) {
                ScenarioCard(
                    scenario = scenario,
                    onClick = { onStartScenario(scenario) }
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = TextXs.copy(
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.06.sp
        ),
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)
    )
}

@Composable
fun CategoryChip(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) Accent else BgCard,
        border = BorderStroke(1.dp, if (isSelected) Accent else Border)
    ) {
        Text(
            text = categoryName,
            style = TextSm.copy(
                fontWeight = FontWeight.Medium,
                color = if (isSelected) TextOnDark else TextSecondary
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ScenarioCard(
    scenario: Scenario,
    onClick: () -> Unit
) {
    val letterAvatar = scenario.partnerName.trim().take(1).uppercase()
    
    // Choose tint category color
    val tintColor = Color(0xFFFFFFFF)
    val avatarTextColor = Color(0xFF000000)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("scenario_card_${scenario.id}"),
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Letter avatar with tint category bg
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tintColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letterAvatar,
                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = avatarTextColor)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scenario.title,
                        style = TextBase.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Difficulty Badge
                    DifficultyPill(scenario.difficulty)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = scenario.scenarioDescription,
                    style = TextSm.copy(color = TextSecondary, lineHeight = 18.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BadgeCard(badgeId: String) {
    val (title, color) = remember(badgeId) {
        when (badgeId) {
            "first_convo" -> Pair("First step", Color(0xFFEFF6FF))
            "high_rapport" -> Pair("Charismatic", Color(0xFFFEF3C7))
            "five_sessions" -> Pair("Committed", Color(0xFFECFDF5))
            else -> Pair("Milestone", Color(0xFFF1F5F9))
        }
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Accent)
            )
            Text(
                text = title,
                style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
            )
        }
    }
}

// Vector Icon builders to completely avoid dependencies on old graphics or emojis

@Composable
fun ClockIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(
            color = tint,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = tint,
            start = center,
            end = center.copy(y = center.y - radius * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = center.copy(x = center.x + radius * 0.35f),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun PlusIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pad = size.width * 0.15f
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, pad),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height - pad),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(pad, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun ChevronIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.3f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.5f)
            lineTo(w * 0.3f, h * 0.85f)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

fun getLevelForXp(xp: Int): Int {
    return when {
        xp < 500 -> 1
        xp < 1200 -> 2
        xp < 2200 -> 3
        xp < 3500 -> 4
        xp < 5200 -> 5
        xp < 7500 -> 6
        xp < 10500 -> 7
        xp < 14500 -> 8
        xp < 20000 -> 9
        else -> 10
    }
}

fun getLevelTitle(level: Int): String {
    return when (level) {
        1 -> "Beginner"
        2 -> "Getting Started"
        3 -> "Building Confidence"
        4 -> "Socially Aware"
        5 -> "Smooth Talker"
        6 -> "Charisma Rising"
        7 -> "Natural Connector"
        8 -> "Social Dynamo"
        9 -> "Elite"
        else -> "Convertible Master"
    }
}

fun getXpProgress(xp: Int): Pair<Float, Int> {
    val level = getLevelForXp(xp)
    val lowerBound = when (level) {
        1 -> 0
        2 -> 500
        3 -> 1200
        4 -> 2200
        5 -> 3500
        6 -> 5200
        7 -> 7500
        8 -> 10500
        9 -> 14500
        else -> 20000
    }
    val upperBound = when (level) {
        1 -> 500
        2 -> 1200
        3 -> 2200
        4 -> 3500
        5 -> 5200
        6 -> 7500
        7 -> 10500
        8 -> 14500
        9 -> 20000
        else -> 20000
    }
    if (level >= 10) return Pair(1f, 0)
    val range = upperBound - lowerBound
    val progressVal = (xp - lowerBound).toFloat() / range.toFloat()
    val xpLeft = upperBound - xp
    return Pair(progressVal, xpLeft)
}

@Composable
fun WelcomeTooltip() {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentLight)
            .border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp, horizontal = 10.dp)
            .testTag("welcome_tooltip")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Start here",
                style = TextXs.copy(fontWeight = FontWeight.Medium, color = Accent)
            )
        }
    }
}

@Composable
fun RecentBadgeCard(badgeId: String) {
    val badgeName = remember(badgeId) {
        when (badgeId) {
            "First Blood" -> "First Blood"
            "Date Whisperer" -> "Date Whisperer"
            "Boardroom Boss" -> "Boardroom Boss"
            "Peace Keeper" -> "Peace Keeper"
            "Networker" -> "Networker"
            "Perfect Ten" -> "Perfect Ten"
            "Week Warrior" -> "Week Warrior"
            "Month Legend" -> "Month Legend"
            "Variety Pack" -> "Variety Pack"
            "Speed Demon" -> "Speed Demon"
            "Deep Diver" -> "Deep Diver"
            "Hard Mode Hero" -> "Hard Mode Hero"
            "Comeback Kid" -> "Comeback Kid"
            "first_convo" -> "First Step"
            "high_rapport" -> "Charismatic"
            "five_sessions" -> "Committed"
            else -> badgeId
        }
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AccentLight),
                contentAlignment = Alignment.Center
            ) {
                BadgeSvgIcon(
                    badgeId = badgeId,
                    color = Accent,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = badgeName,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
        }
    }
}
