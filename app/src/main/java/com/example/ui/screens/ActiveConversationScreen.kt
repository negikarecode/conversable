package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import com.example.viewmodel.RealTimeCoachTip
import com.example.viewmodel.RoleplayState
import androidx.compose.ui.text.TextStyle
import com.example.data.model.ChatMessage
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActiveConversationScreen(
    viewModel: ConversableViewModel,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.security.KeepScreenSecure()
    val scenario = viewModel.activeScenario ?: return

    var showQuitWarning by remember { mutableStateOf(false) }

    BackHandler {
        showQuitWarning = true
    }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isPartnerTyping.collectAsState()
    val rapport by viewModel.rapportLevel.collectAsState()
    val roleplayState by viewModel.roleplayState.collectAsState()
    val suggestionState by viewModel.suggestionState.collectAsState()

    var textInput by remember { mutableStateOf(TextFieldValue("")) }
    var showVoiceSettings by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val voiceModeEnabled by viewModel.voiceModeEnabled.collectAsState()
    val voiceRecordingState by viewModel.voiceRecordingState.collectAsState()
    val voiceFeedbackMessage by viewModel.voiceFeedbackMessage.collectAsState()
    val currentlySpeakingMessageId by viewModel.currentlySpeakingMessageId.collectAsState()

    // States for coach feedback card/pill
    var isCoachTipCollapsed by remember { mutableStateOf(false) }
    var isCoachTipTouched by remember { mutableStateOf(false) }
    var showCoachPill by remember { mutableStateOf(false) }
    var previousCoachTip by remember { mutableStateOf<RealTimeCoachTip?>(null) }

    val coachTipAlpha = remember { Animatable(0f) }
    val coachTipOffsetY = remember { Animatable(-20f) }

    val coachTip by viewModel.latestCoachTip.collectAsState()

    val density = androidx.compose.ui.platform.LocalDensity.current
    val startOffsetYPx = with(density) { -20.dp.toPx() }
    val swipeThresholdPx = with(density) { 60.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }
    val dismissOffsetYPx = with(density) { -300.dp.toPx() }

    LaunchedEffect(coachTip) {
        if (coachTip != null && coachTip != previousCoachTip) {
            previousCoachTip = coachTip
            isCoachTipCollapsed = false
            showCoachPill = false
            isCoachTipTouched = false
            coachTipAlpha.snapTo(0f)
            coachTipOffsetY.snapTo(startOffsetYPx)
            launch { coachTipAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
            launch { coachTipOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
        }
    }

    LaunchedEffect(coachTip, isCoachTipCollapsed, isCoachTipTouched) {
        if (coachTip != null && !isCoachTipCollapsed && !isCoachTipTouched) {
            delay(4000)
            launch { coachTipAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
            launch { coachTipOffsetY.animateTo(dismissOffsetYPx, tween(200, easing = FastOutSlowInEasing)) }
            isCoachTipCollapsed = true
            showCoachPill = true
        }
    }

    val dismissCoachCard = {
        coroutineScope.launch {
            launch { coachTipAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
            launch { coachTipOffsetY.animateTo(dismissOffsetYPx, tween(200, easing = FastOutSlowInEasing)) }
            isCoachTipCollapsed = true
            showCoachPill = true
        }
    }

    val expandCoachCard = {
        isCoachTipCollapsed = false
        showCoachPill = false
        isCoachTipTouched = true
        coroutineScope.launch {
            coachTipAlpha.snapTo(0f)
            coachTipOffsetY.snapTo(startOffsetYPx)
            launch { coachTipAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
            launch { coachTipOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
        }
    }

    val coachTipSwipeModifier = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isCoachTipTouched = true
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    isCoachTipTouched = true
                },
                onDragEnd = {
                    val deltaUp = -coachTipOffsetY.value
                    if (deltaUp > swipeThresholdPx) {
                        dismissCoachCard()
                    } else {
                        coroutineScope.launch {
                            launch { coachTipOffsetY.animateTo(0f, spring()) }
                            launch { coachTipAlpha.animateTo(1f, spring()) }
                        }
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        launch { coachTipOffsetY.animateTo(0f, spring()) }
                        launch { coachTipAlpha.animateTo(1f, spring()) }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val currentOffset = coachTipOffsetY.value
                    val newOffset = (currentOffset + dragAmount.y).coerceAtMost(0f)
                    val deltaUp = -newOffset
                    coroutineScope.launch {
                        coachTipOffsetY.snapTo(newOffset)
                        val progress = (deltaUp / maxDragPx).coerceIn(0f, 1f)
                        coachTipAlpha.snapTo(1f - progress)
                    }
                }
            )
        }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            // Permission denied visual hint
        }
    }

    // Collapsed and expanded states for relationship card
    var isRelationshipExpanded by remember { mutableStateOf(true) }
    var hasAutoCollapsed by remember { mutableStateOf(false) }
    val aiMessageCount = remember(messages) { messages.count { !it.isUser } }

    LaunchedEffect(aiMessageCount) {
        if (aiMessageCount > 0 && !hasAutoCollapsed) {
            isRelationshipExpanded = false
            hasAutoCollapsed = true
        }
    }

    // Auto scroll behavior
    val isKeyboardVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    LaunchedEffect(messages.size, isTyping, isKeyboardVisible, isRelationshipExpanded) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1 + (if (isTyping) 1 else 0)
            if (lastIndex >= 0) {
                listState.scrollToItem(lastIndex)
                delay(100)
                listState.animateScrollToItem(lastIndex)
                delay(150)
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = scenario.partnerName,
                                style = TextBase.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                            Text(
                                text = if (isTyping) "typing..." else scenario.title,
                                style = TextXs.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showQuitWarning = true },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit practice",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.finishSession() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BgInput,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, Border),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("finish_session_button")
                    ) {
                        Text("Finish", style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextPrimary))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard),
            )
        },
        containerColor = SleekBackground
    ) { innerPadding ->
        if (roleplayState is RoleplayState.LoadingFeedback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SleekBackground.copy(alpha = 0.95f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SleekPrimary, strokeWidth = 5.dp, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Social Coach Analyzing...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your conversation is being evaluated for empathy, flow, and hidden goal achievement.",
                        fontSize = 14.sp,
                        color = SleekTextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            val goodStreak by viewModel.goodStreak.collectAsState()
            val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }

            LaunchedEffect(goodStreak) {
                if (goodStreak == 0) {
                    for (i in 0..2) {
                        shakeOffset.animateTo(10f, androidx.compose.animation.core.tween(50))
                        shakeOffset.animateTo(-10f, androidx.compose.animation.core.tween(50))
                    }
                    shakeOffset.animateTo(0f, androidx.compose.animation.core.tween(50))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SleekBackground)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 0.dp
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    com.example.ui.components.AiErrorBanner(
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )

                    val isDailyChallengeActive by viewModel.isDailyChallengeActive.collectAsState()
                    val currentDailyChallenge by viewModel.currentDailyChallenge.collectAsState()
                    val userTurnCount = remember(messages) { messages.count { it.isUser } }

                    if (isDailyChallengeActive && currentDailyChallenge != null) {
                        DailyChallengeActiveDashboard(
                            challenge = currentDailyChallenge!!,
                            rapportScore = rapport,
                            userTurnCount = userTurnCount
                        )
                    }

                    PremiumRelationshipCard(
                        viewModel = viewModel,
                        isExpanded = isRelationshipExpanded,
                        onExpandedChange = { isRelationshipExpanded = it }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { message ->
                                if (message.isError) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewModel.retryLastMessage() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${message.text} Tap to retry.",
                                            fontSize = 11.sp,
                                            color = SleekTextGray,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                } else {
                                    ChatBubble(message = message, currentlySpeakingMessageId = currentlySpeakingMessageId)
                                }
                            }

                            if (isTyping) {
                                item {
                                    PartnerTypingIndicator(partnerName = scenario.partnerName)
                                }
                            }

                            if (roleplayState is RoleplayState.Error) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SleekWarning.copy(alpha = 0.1f)),
                                        border = BorderStroke(1.dp, SleekWarning.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = (roleplayState as RoleplayState.Error).message,
                                            color = SleekWarning,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        coachTip?.let {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (showCoachPill) {
                                    CollapsedCoachPill(
                                        coachTip = it,
                                        onClick = { expandCoachCard() }
                                    )
                                } else {
                                    LiveCoachTipCard(
                                        coachTip = it,
                                        alpha = coachTipAlpha.value,
                                        offsetY = coachTipOffsetY.value,
                                        onDismiss = { dismissCoachCard() },
                                        modifier = coachTipSwipeModifier
                                    )
                                }
                            }
                        }
                    }

                    if (voiceModeEnabled) {
                        RecordButtonSection(
                            state = voiceRecordingState,
                            feedback = voiceFeedbackMessage,
                            onPressStart = {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startRecording()
                                } else {
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onPressEnd = {
                                viewModel.stopRecordingAudio()
                            },
                            onStopSpeaking = {
                                viewModel.stopSpeaking()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        )
                    }

                    if (roleplayState !is RoleplayState.LoadingFeedback) {
                        // Fixed bottom composer container
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                            color = SleekSurface,
                            tonalElevation = 8.dp,
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
                            ) {
                                // Real-time suggestions - redesigned custom suggestion chip
                                val isShowing = suggestionState !is ConversableViewModel.SuggestionState.Hidden
                                val animatedHeight by animateDpAsState(
                                    targetValue = if (isShowing) 40.dp else 0.dp,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "suggestion_height"
                                )
                                val animatedPaddingVertical by animateDpAsState(
                                    targetValue = if (isShowing) 6.dp else 0.dp,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "suggestion_padding"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(animatedHeight)
                                        .background(SleekSurface)
                                        .clipToBounds()
                                        .drawWithContent {
                                            drawContent()
                                            drawLine(
                                                color = Border,
                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = animatedPaddingVertical)
                                        .testTag("custom_suggestion_chip_container")
                                ) {
                                    if (isShowing) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LightbulbIcon()

                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                when (val state = suggestionState) {
                                                    is ConversableViewModel.SuggestionState.Loading -> {
                                                        StaggeredDotsLoading()
                                                    }
                                                    is ConversableViewModel.SuggestionState.Loaded -> {
                                                        Text(
                                                            text = state.text,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF6B6B80),
                                                            style = TextStyle(fontStyle = FontStyle.Italic),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    val text = state.text
                                                                    textInput = TextFieldValue(text, selection = androidx.compose.ui.text.TextRange(text.length))
                                                                    viewModel.useSuggestion(text)
                                                                }
                                                                .testTag("suggestion_text_clickable")
                                                        )
                                                    }
                                                    else -> {}
                                                }
                                            }

                                            Text(
                                                text = "×",
                                                fontSize = 16.sp,
                                                color = Color(0xFFAEAEB8),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.dismissSuggestion()
                                                    }
                                                    .padding(4.dp)
                                                    .testTag("dismiss_suggestion_button")
                                            )
                                        }
                                    }
                                }

                                if (voiceModeEnabled && showVoiceSettings) {
                                    val voiceAiSpeaks by viewModel.voiceAiSpeaks.collectAsState()
                                    val voiceSpeed by viewModel.voiceSpeed.collectAsState()
                                    val soundsEnabled by viewModel.soundsEnabled.collectAsState()
                                    VoiceSettingsPanel(
                                        voiceAiSpeaks = voiceAiSpeaks,
                                        onAiSpeaksChange = { viewModel.setVoiceAiSpeaks(it) },
                                        voiceSpeed = voiceSpeed,
                                        onSpeedChange = { viewModel.setVoiceSpeed(it) },
                                        soundsEnabled = soundsEnabled,
                                        onSoundsEnabledChange = { viewModel.setSoundsEnabled(it) },
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VoiceToggleBtn(
                                        enabled = voiceModeEnabled,
                                        onToggle = {
                                            viewModel.setVoiceModeEnabled(!voiceModeEnabled)
                                        }
                                    )

                                    if (voiceModeEnabled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { showVoiceSettings = !showVoiceSettings },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SleekBubblePartner)
                                                .border(BorderStroke(1.dp, SleekBorder), RoundedCornerShape(12.dp))
                                                .testTag("voice_settings_toggle_button")
                                        ) {
                                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val isSendEnabled = !voiceModeEnabled && textInput.text.isNotBlank() && !isTyping

                                    OutlinedTextField(
                                        value = if (voiceModeEnabled) TextFieldValue("") else textInput,
                                        onValueChange = { 
                                            if (!voiceModeEnabled) {
                                                textInput = it
                                                viewModel.onTextInputChanged(it.text)
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = if (voiceModeEnabled) "(Voice Mode Active)" else "Message...",
                                                style = TextSm.copy(color = SleekTextGray)
                                            )
                                        },
                                        enabled = !voiceModeEnabled,
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 56.dp)
                                            .testTag("chat_input_field"),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = if (voiceModeEnabled) SleekBorder else SleekBubblePartner,
                                            unfocusedContainerColor = if (voiceModeEnabled) SleekBubblePartner else SleekBubblePartner,
                                            disabledContainerColor = SleekBorder,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            disabledBorderColor = Color.Transparent,
                                            focusedTextColor = SleekTextDark,
                                            unfocusedTextColor = SleekTextDark,
                                            disabledTextColor = SleekTextGray,
                                            cursorColor = SleekPrimary,
                                            selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                                                handleColor = SleekPrimary,
                                                backgroundColor = SleekPrimaryLight
                                            )
                                        ),
                                        maxLines = 3,
                                        textStyle = TextStyle(
                                            color = SleekTextDark,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (isSendEnabled) {
                                                viewModel.sendMessage(textInput.text)
                                                textInput = TextFieldValue("")
                                            }
                                        },
                                        enabled = isSendEnabled,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (isSendEnabled) SleekPrimary else SleekBorder)
                                            .testTag("send_message_button")
                                    ) {
                                        if (isTyping) {
                                            CircularProgressIndicator(
                                                color = SleekPrimary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send phrase",
                                                tint = if (isSendEnabled) Color.White else SleekTextGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Streak Pill Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp, end = 20.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = goodStreak > 0,
                        enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .offset(x = shakeOffset.value.dp)
                                .bounceClick()
                                .testTag("streak_counter_pill"),
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "$goodStreak Turn Streak",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal alerting user that leaving wipes dialogue
    if (showQuitWarning) {
        AlertDialog(
            onDismissRequest = { showQuitWarning = false },
            containerColor = SleekSurface,
            title = { Text("Exit session?", color = SleekTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Any current conversation history in this scenario will be lost. Are you sure you want to go back?",
                    color = SleekTextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitWarning = false
                        viewModel.resetStateToIdle()
                        onBackToDashboard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekWarning)
                ) {
                    Text("Exit & Wipe", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitWarning = false }) {
                    Text("Continue practicing", color = SleekTextLightGray)
                }
            }
        )
    }
}


@Composable
fun RapportMeter(rapportScore: Int) {
    val moodWord = remember(rapportScore) {
        when {
            rapportScore >= 80 -> "Captivated"
            rapportScore >= 65 -> "Engaging"
            rapportScore >= 50 -> "Attentive"
            rapportScore >= 35 -> "Alert"
            else -> "Disinterested"
        }
    }

    val barColor = remember(rapportScore) {
        when {
            rapportScore <= 40 -> Danger
            rapportScore <= 70 -> Warning
            else -> Success
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("rapport_meter_container")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connection",
                style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextSecondary)
            )
            Text(
                text = "$rapportScore%",
                style = TextSm.copy(fontWeight = FontWeight.SemiBold, color = Accent)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Single color bar using current score
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(rapportScore.toFloat() / 100f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(barColor)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = moodWord,
            style = TextXs.copy(color = TextMuted)
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage, currentlySpeakingMessageId: String?) {
    val isSpoken = currentlySpeakingMessageId == message.id
    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.isUser) Accent else BgCard
    val bubbleTextColor = if (message.isUser) TextOnDark else TextPrimary
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.75f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (message.isUser) "user_message_bubble" else "partner_message_bubble"),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.isUser) {
            if (message.isVoice) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Sent via voice",
                    tint = TextSecondary,
                    modifier = Modifier.padding(end = 6.dp).size(14.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = bubbleShape,
            border = if (!message.isUser) {
                if (isSpoken) {
                    BorderStroke(1.5.dp, Success)
                } else {
                    BorderStroke(1.dp, Border)
                }
            } else null,
            elevation = CardDefaults.cardElevation(defaultElevation = if (!message.isUser) 1.dp else 0.dp),
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = message.text,
                    style = TextBase.copy(
                        color = bubbleTextColor,
                        lineHeight = 22.5.sp
                    )
                )
            }
        }

        if (!message.isUser && isSpoken) {
            Spacer(modifier = Modifier.width(6.dp))
            val infiniteTransition = rememberInfiniteTransition(label = "speaking_dots")
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val delays = listOf(0, 150, 300)
                for (i in 0 until 3) {
                    val opacity by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 400, delayMillis = delays[i], easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_opacity"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SleekSuccess.copy(alpha = opacity))
                    )
                }
            }
        }
    }
}

@Composable
fun MicOffIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val capsuleWidth = 6.dp.toPx()
        val capsuleHeight = 11.dp.toPx()
        val rx = 3.dp.toPx()
        val ry = rx
        val left = (size.width - capsuleWidth) / 2
        val top = 2.dp.toPx()
        val capsuleRect = androidx.compose.ui.geometry.RoundRect(
            left = left,
            top = top,
            right = left + capsuleWidth,
            bottom = top + capsuleHeight,
            radiusX = rx,
            radiusY = ry
        )
        val pathCapsule = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(capsuleRect)
        }
        drawPath(
            path = pathCapsule,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        val cradlePath = androidx.compose.ui.graphics.Path().apply {
            val cradleRect = androidx.compose.ui.geometry.Rect(
                left = (size.width / 2) - 7.dp.toPx(),
                top = 3.dp.toPx(),
                right = (size.width / 2) + 7.dp.toPx(),
                bottom = 17.dp.toPx()
            )
            addArc(cradleRect, 0f, 180f)
        }
        drawPath(
            path = cradlePath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width / 2, 17.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(size.width / 2, 21.dp.toPx()),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun VoiceToggleBtn(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) SleekPrimary else SleekBubblePartner
    val iconColor = if (enabled) Color.White else SleekTextDark
    val borderStroke = if (enabled) null else BorderStroke(1.dp, SleekBorder)

    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(borderStroke ?: BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .testTag("voice_mode_toggle_button")
    ) {
        MicOffIcon(color = iconColor)
    }
}

@Composable
fun VoiceSettingsPanel(
    voiceAiSpeaks: Boolean,
    onAiSpeaksChange: (Boolean) -> Unit,
    voiceSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    soundsEnabled: Boolean,
    onSoundsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_settings_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Voice Settings",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI speaks replies out loud",
                    fontSize = 12.sp,
                    color = SleekTextGray
                )
                Switch(
                    checked = voiceAiSpeaks,
                    onCheckedChange = onAiSpeaksChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SleekPrimary,
                        uncheckedThumbColor = SleekTextGray,
                        uncheckedTrackColor = SleekBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "In-app Sound Effects",
                    fontSize = 12.sp,
                    color = SleekTextGray
                )
                Switch(
                    checked = soundsEnabled,
                    onCheckedChange = onSoundsEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SleekPrimary,
                        uncheckedThumbColor = SleekTextGray,
                        uncheckedTrackColor = SleekBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Delivery Speed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekTextGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                    options.forEach { speed ->
                        val isSelected = speed == voiceSpeed
                        val bg = if (isSelected) SleekPrimary else SleekBubblePartner
                        val fg = if (isSelected) Color.White else SleekTextDark
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, SleekBorder), RoundedCornerShape(12.dp))
                                .clickable { onSpeedChange(speed) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = fg
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BouncingSoundWaves(isRed: Boolean = false, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "freq_sound_waves")
    val waveColor = if (isRed) SleekWarning else SleekPrimary

    Row(
        modifier = modifier
            .width(120.dp)
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val animationDurations = listOf(450, 600, 500, 700, 550)
        for (i in 0 until 5) {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = animationDurations[i], easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "freq_sound_waves_scale"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(waveColor)
            )
        }
    }
}

@Composable
fun RecordButtonSection(
    state: com.example.viewmodel.ConversableViewModel.VoiceRecordingState,
    feedback: String?,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_recording_panel"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        feedback?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                color = SleekWarning,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (state) {
            com.example.viewmodel.ConversableViewModel.VoiceRecordingState.PROCESSING -> {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SleekBubblePartner)
                        .border(1.5.dp, SleekBorder, CircleShape)
                        .testTag("record_button_processing"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SleekPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "pulse_opacity")
                val opacity by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "opacity"
                )
                Text(
                    text = "Processing audio...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleekTextGray.copy(alpha = opacity)
                )
            }

            com.example.viewmodel.ConversableViewModel.VoiceRecordingState.SPEAKING -> {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SleekPrimary.copy(alpha = 0.15f))
                        .border(1.5.dp, SleekPrimary, CircleShape)
                        .clickable { onStopSpeaking() }
                        .testTag("record_button_speaking"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "AI partner is speaking...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                BouncingSoundWaves(isRed = false)
            }

            else -> {
                val isRecording = state == com.example.viewmodel.ConversableViewModel.VoiceRecordingState.RECORDING
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by if (isRecording) {
                    infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                } else {
                    remember { mutableStateOf(1.0f) }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (isRecording) 1.1f else 1.0f)
                        .clip(CircleShape)
                        .background(if (isRecording) SleekWarning else SleekPrimary.copy(alpha = 0.1f))
                        .border(
                            if (isRecording) {
                                BorderStroke(4.dp * pulseScale, SleekWarning.copy(alpha = 0.3f))
                            } else {
                                BorderStroke(1.5.dp, SleekPrimary)
                            },
                            CircleShape
                        )
                        .testTag(if (isRecording) "record_button_recording" else "record_button_idle")
                        .pointerInput(onPressStart, onPressEnd) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onPressStart()
                                var isDown = true
                                while (isDown) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) {
                                        isDown = false
                                        onPressEnd()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MicOffIcon(
                        color = if (isRecording) Color.White else SleekPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isRecording) "Recording... Release to Send" else "Hold to Speak",
                    fontSize = 12.sp,
                    fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Medium,
                    color = if (isRecording) SleekWarning else SleekTextGray
                )
                if (isRecording) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BouncingSoundWaves(isRed = true)
                }
            }
        }
    }
}

@Composable
fun PartnerTypingIndicator(partnerName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("partner_typing_indicator"),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green online dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekBubblePartner),
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.width(185.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$partnerName is typing",
                    fontSize = 11.sp,
                    color = SleekTextGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                ThreeDotsAnimation()
            }
        }
    }
}

@Composable
fun ThreeDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        val dotColor = SleekTextGray.copy(alpha = 0.7f)
        Box(Modifier.size(4.dp).graphicsLayer(scaleX = dot1Scale, scaleY = dot1Scale).clip(CircleShape).background(dotColor))
        Box(Modifier.size(4.dp).graphicsLayer(scaleX = dot2Scale, scaleY = dot2Scale).clip(CircleShape).background(dotColor))
        Box(Modifier.size(4.dp).graphicsLayer(scaleX = dot3Scale, scaleY = dot3Scale).clip(CircleShape).background(dotColor))
    }
}

@Composable
fun LiveCoachTipCard(
    coachTip: RealTimeCoachTip,
    alpha: Float,
    offsetY: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (coachTip.signal) {
        "GREAT" -> SleekSuccess
        "GOOD" -> SleekPrimary
        "NEUTRAL" -> SleekBorder
        "AWKWARD" -> SleekWarningAmber
        else -> SleekWarning
    }
    val signalBgColor = when (coachTip.signal) {
        "GREAT" -> SleekSuccess.copy(alpha = 0.15f)
        "GOOD" -> SleekPrimary.copy(alpha = 0.15f)
        "NEUTRAL" -> SleekBubblePartner
        "AWKWARD" -> SleekWarningAmber.copy(alpha = 0.15f)
        else -> SleekWarning.copy(alpha = 0.15f)
    }
    val signalTextColor = when (coachTip.signal) {
        "GREAT" -> SleekSuccess
        "GOOD" -> SleekPrimary
        "NEUTRAL" -> SleekTextGray
        "AWKWARD" -> SleekWarningAmber
        else -> SleekWarning
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("live_coach_tip_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Coach Tip Icon",
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COACH",
                            style = TextXs.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                letterSpacing = 0.06.sp
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Signal Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(signalBgColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = coachTip.signal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = signalTextColor
                            )
                        }

                        // Delta Indicator
                        val deltaText = if (coachTip.rapport_delta >= 0) "+${coachTip.rapport_delta}" else "${coachTip.rapport_delta}"
                        val deltaColor = if (coachTip.rapport_delta >= 0) SleekSuccess else SleekWarning
                        Text(
                            text = "$deltaText",
                            color = deltaColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = coachTip.micro_tip,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleekTextDark,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Engagement trend:",
                        fontSize = 11.sp,
                        color = SleekTextLightGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val trendArrow = when (coachTip.engagement_trend) {
                        "RISING" -> "Rising"
                        "FALLING" -> "Falling"
                        else -> "Steady"
                    }
                    val trendColor = when (coachTip.engagement_trend) {
                        "RISING" -> SleekSuccess
                        "FALLING" -> SleekWarning
                        else -> SleekTextGray
                    }
                    Text(
                        text = trendArrow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
        }

        // Close × button top right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 20.dp)
                .size(24.dp)
                .testTag("close_coach_tip_button")
        ) {
            Text(
                text = "×",
                fontSize = 18.sp,
                color = SleekTextGray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CollapsedCoachPill(
    coachTip: RealTimeCoachTip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillColor = when (coachTip.signal) {
        "GREAT", "GOOD" -> SleekSuccess
        "NEUTRAL" -> SleekWarningAmber
        else -> SleekWarning
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
            .testTag("collapsed_coach_pill"),
        shape = RoundedCornerShape(20.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Coach Pill Icon",
                tint = SleekTextGray,
                modifier = Modifier.size(12.dp)
            )

            val deltaText = if (coachTip.rapport_delta >= 0) "+${coachTip.rapport_delta}" else "${coachTip.rapport_delta}"
            Text(
                text = "$deltaText",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = pillColor
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand Coach Tip",
                tint = SleekTextGray,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun LightbulbIcon(modifier: Modifier = Modifier) {
    val strokeColor = Color(0xFF4F46E5)
    androidx.compose.foundation.Canvas(modifier = modifier.size(14.dp).testTag("lightbulb_icon")) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        
        drawLine(
            color = strokeColor,
            start = androidx.compose.ui.geometry.Offset(9f * scaleX, 18f * scaleY),
            end = androidx.compose.ui.geometry.Offset(15f * scaleX, 18f * scaleY),
            strokeWidth = 1.5f * scaleX,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = androidx.compose.ui.geometry.Offset(10f * scaleX, 22f * scaleY),
            end = androidx.compose.ui.geometry.Offset(14f * scaleX, 22f * scaleY),
            strokeWidth = 1.5f * scaleX,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(15.09f * scaleX, 14f * scaleY)
            cubicTo(15.27f * scaleX, 13.02f * scaleY, 15.74f * scaleX, 12.26f * scaleY, 16.5f * scaleX, 11.5f * scaleY)
            cubicTo(18f * scaleX, 10f * scaleY, 18f * scaleX, 4f * scaleY, 12f * scaleX, 2f * scaleY)
            cubicTo(6f * scaleX, 2f * scaleY, 6f * scaleX, 10f * scaleY, 7.5f * scaleX, 11.5f * scaleY)
            cubicTo(8.77f * scaleX, 12.77f * scaleY, 9f * scaleX, 14f * scaleY, 8.91f * scaleX, 14f * scaleY)
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5f * scaleX,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
fun StaggeredDotsLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    
    @Composable
    fun animateDotOffset(delayMillis: Int): State<Float> {
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0f at 0 with FastOutSlowInEasing
                    -6f at 150 with FastOutSlowInEasing
                    0f at 300 with FastOutSlowInEasing
                    0f at 600 with FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dot_offset_$delayMillis"
        )
    }

    val dot1 by animateDotOffset(0)
    val dot2 by animateDotOffset(100)
    val dot3 by animateDotOffset(200)

    Row(
        modifier = modifier.testTag("staggered_dots_loading"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotColor = Color(0xFFAEAEB8)
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer(translationY = dot1)
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer(translationY = dot2)
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer(translationY = dot3)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
fun DailyChallengeActiveDashboard(
    challenge: com.example.data.model.DailyChallenge,
    rapportScore: Int,
    userTurnCount: Int
) {
    var timeLeftSec by remember { mutableStateOf(360) } // 6 minutes
    LaunchedEffect(Unit) {
        while (timeLeftSec > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeftSec--
        }
    }
    val minutesStr = (timeLeftSec / 60).toString()
    val secondsStr = (timeLeftSec % 60).toString().padStart(2, '0')

    val moodEmoji = remember(rapportScore) {
        when {
            rapportScore >= 80 -> "Connected"
            rapportScore >= 65 -> "Comfortable"
            rapportScore >= 45 -> "Neutral"
            else -> "Distant"
        }
    }

    val relationshipStatus = remember(rapportScore) {
        when {
            rapportScore >= 80 -> "Extremely Warm"
            rapportScore >= 65 -> "Friendly"
            rapportScore >= 45 -> "Neutral"
            else -> "Frustrated / Strained"
        }
    }

    val barColor = remember(rapportScore) {
        when {
            rapportScore <= 40 -> Danger
            rapportScore <= 70 -> Warning
            else -> Success
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("daily_challenge_active_dashboard")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY MISSION: ${challenge.title.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentText,
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Warning.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Target Weakness",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Warning
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Goal: ${challenge.hiddenGoal}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "Personalized focus: ${challenge.targetWeakness}",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Border)

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Relationship Meter (Dynamic)
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(moodEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rapport: $rapportScore% ($relationshipStatus)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { rapportScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = barColor,
                        trackColor = Border
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Turn Counter
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.7f)) {
                    Text(
                        text = "TURNS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$userTurnCount / 5",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userTurnCount >= 5) Success else TextPrimary
                    )
                }

                // Countdown Timer
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.7f)) {
                    Text(
                        text = "TIME LEFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (timeLeftSec > 0) "$minutesStr:$secondsStr" else "0:00 ⏳",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (timeLeftSec <= 60) Danger else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumRelationshipCard(
    viewModel: com.example.viewmodel.ConversableViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val scenario = viewModel.activeScenario ?: return
    val rapport by viewModel.rapportLevel.collectAsState()
    val mood by viewModel.relationshipMood.collectAsState()
    val metrics by viewModel.relationshipMetrics.collectAsState()
    val timeline by viewModel.relationshipTimeline.collectAsState()
    val achievements by viewModel.relationshipAchievements.collectAsState()
    val smartAlert by viewModel.smartAlert.collectAsState()
    
    var selectedMetricExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTimelinePoint by remember { mutableStateOf<com.example.viewmodel.RelationshipTimelinePoint?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .animateContentSize(animationSpec = tween(250))
            .clickable { onExpandedChange(!isExpanded) }
            .testTag("relationship_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Relationship Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.FavoriteBorder, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = Accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Relationship Status",
                        style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Accent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rapport% Connection",
                            style = TextSm.copy(fontWeight = FontWeight.SemiBold, color = Accent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mood,
                            style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Main stats row: Overall Connection & Mood
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Overall Connection",
                            style = TextXs.copy(color = TextSecondary)
                        )
                        Text(
                            text = "$rapport%",
                            style = TextXl.copy(fontWeight = FontWeight.ExtraBold, color = Accent)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Mood",
                            style = TextXs.copy(color = TextSecondary)
                        )
                        Text(
                            text = mood,
                            style = TextBase.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Scenario Metrics
                Text(
                    text = "SCENARIO METRICS (Tap to see why they changed)",
                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                metrics.values.forEach { metric ->
                    val barColor = when {
                        metric.score <= 40 -> Danger
                        metric.score <= 70 -> Warning
                        else -> Success
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMetricExplanation = Pair(metric.name, metric.explanation) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = metric.name,
                                    style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                                )
                                if (metric.delta != 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val deltaText = if (metric.delta > 0) "+${metric.delta}" else "${metric.delta}"
                                    val deltaColor = if (metric.delta > 0) Success else Danger
                                    Text(
                                        text = deltaText,
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = deltaColor)
                                    )
                                }
                            }
                            Text(
                                text = "${metric.score}%",
                                style = TextSm.copy(fontWeight = FontWeight.SemiBold, color = barColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(metric.score / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }

                // Optional Smart Alert suggestion
                smartAlert?.let { alertText ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentLight.copy(alpha = 0.15f))
                            .border(1.dp, AccentLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = alertText,
                            style = TextXs.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Section 1: Interactive Conversation Timeline
                Text(
                    text = "RELATIONSHIP GRAPH (Tap turn to view details)",
                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (timeline.isEmpty()) {
                    Text(
                        text = "No turns completed yet. Send a message to see your graph!",
                        style = TextXs.copy(color = TextSecondary, fontStyle = FontStyle.Italic)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        timeline.forEachIndexed { index, point ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedTimelinePoint = point }
                                    .background(
                                        if (selectedTimelinePoint == point) AccentLight.copy(alpha = 0.1f)
                                        else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Turn ${index + 1}",
                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                point.overallConnection >= 75 -> Success.copy(alpha = 0.2f)
                                                point.overallConnection >= 50 -> Warning.copy(alpha = 0.2f)
                                                else -> Danger.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${point.overallConnection}%",
                                        style = TextXs.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                point.overallConnection >= 75 -> Success
                                                point.overallConnection >= 50 -> Warning
                                                else -> Danger
                                            }
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = point.mood,
                                    style = TextXs.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal for Metric explanation details
    selectedMetricExplanation?.let { (metricName, explanation) ->
        AlertDialog(
            onDismissRequest = { selectedMetricExplanation = null },
            title = {
                Text(
                    text = metricName.uppercase(),
                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            },
            text = {
                Text(
                    text = explanation,
                    style = TextSm.copy(color = TextSecondary, lineHeight = 18.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedMetricExplanation = null }) {
                    Text("OK", color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal for Timeline turn detail explanation details
    selectedTimelinePoint?.let { point ->
        AlertDialog(
            onDismissRequest = { selectedTimelinePoint = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Info, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TURN DETAIL REVIEW",
                        style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("You said:", style = TextXs.copy(fontWeight = FontWeight.Bold, color = Accent))
                        Text(
                            point.userMessageText,
                            style = TextSm.copy(color = TextPrimary, fontStyle = FontStyle.Italic)
                        )
                    }
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Column {
                        Text("${scenario.partnerName} replied:", style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                        Text(
                            point.partnerResponseText,
                            style = TextSm.copy(color = TextPrimary)
                        )
                    }
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Text(
                        text = "Mood at this point: ${point.mood}",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTimelinePoint = null }) {
                    Text("Close", color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
