package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PersonaEntity
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel

data class ScreenshotItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: android.net.Uri? = null,
    val isDemo: Boolean = false,
    val demoLabel: String = "",
    val progress: Float = 1.0f
)

fun getRepresentativeChatData(name: String, relation: String): String {
    val cleanName = if (name.isBlank()) "Friend" else name
    return """
        $cleanName: hey there!
        $cleanName: lowkey bored, what are you doing tonight?
        $cleanName: lmao that is crazy
        $cleanName: u there??
        $cleanName: lowkey want to grab tacos
        $cleanName: did you see that new show?
        $cleanName: honestly so good haha
    """.trimIndent()
}

enum class PersonaScreenState {
    LIST,
    CREATE,
    ANALYZING,
    DASHBOARD,
    CHAT
}

@Composable
fun PersonaCloneScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf(PersonaScreenState.LIST) }
    val allClones by viewModel.allPersonas.collectAsState()
    var selectedClone by remember { mutableStateOf<PersonaEntity?>(null) }
    var selectedScenarioForPractice by remember { mutableStateOf("General Chat") }

    // Form states
    var personaName by remember { mutableStateOf("") }
    var relationshipType by remember { mutableStateOf("Friend") }
    var notes by remember { mutableStateOf("") }
    var rawChatData by remember { mutableStateOf("") }
    var showTermsDisclaimer by remember { mutableStateOf(false) }
    var userConfirmedPrivacy by remember { mutableStateOf(false) }
    var showDiscardCloneDialog by remember { mutableStateOf(false) }
    var showExitSessionDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var uploadedScreenshots by remember { mutableStateOf<List<ScreenshotItem>>(emptyList()) }
    var isAnalysisCompletedState by remember { mutableStateOf(false) }
    var isApiCallFinished by remember { mutableStateOf(false) }
    var isApiCallSuccess by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var loadingStepText by remember { mutableStateOf("Analyzing vocabulary...") }

    // Chat local state
    var chatInputText by remember { mutableStateOf("") }
    val isCoachEnabled by viewModel.isPersonaCoachModeEnabled.collectAsState()
    val personaMessages by viewModel.personaMessages.collectAsState()
    val isPersonaTyping by viewModel.isPersonaTyping.collectAsState()
    val coachTip by viewModel.personaCoachTip.collectAsState()

    val listState = rememberScrollState()

    val relationshipOptions = listOf(
        "Friend", "Crush", "Girlfriend/Boyfriend", "Colleague", "Teacher", "Parent", "Custom"
    )

    val practiceScenarios = listOf(
        "First conversation",
        "Asking someone out",
        "Apologizing",
        "Networking",
        "Job interviews",
        "Difficult conversations",
        "Making friends",
        "Public speaking preparation",
        "Conflict resolution"
    )

    // Pre-made chat templates for easy demo/sandbox testing
    val templates = listOf(
        Triple(
            "Jessica (Crush)",
            "Crush",
            "hey\nu there?\nhaha omg no way\nliterally so funny\nwhat are u doing tonight??\nidk lowkey bored"
        ),
        Triple(
            "Dad (Parent)",
            "Parent",
            "Hello. Are you coming home today.\nYes.\nOk. Let me know when you arrive. Bring milk."
        ),
        Triple(
            "Alex (Colleague)",
            "Colleague",
            "Hi there, thanks for the update. I'll review the draft this afternoon.\nLet's make sure we align on core deliverables before the meeting.\nLet me know if you need any additional help with the slides."
        )
    )

    BackHandler {
        when (screenState) {
            PersonaScreenState.LIST -> onBack()
            PersonaScreenState.CREATE -> {
                if (personaName.isNotBlank() || notes.isNotBlank() || rawChatData.isNotBlank() || uploadedScreenshots.isNotEmpty()) {
                    showDiscardCloneDialog = true
                } else {
                    screenState = PersonaScreenState.LIST
                }
            }
            PersonaScreenState.ANALYZING -> { /* block */ }
            PersonaScreenState.DASHBOARD -> screenState = PersonaScreenState.LIST
            PersonaScreenState.CHAT -> {
                showExitSessionDialog = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        if (showDiscardCloneDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardCloneDialog = false },
                containerColor = SleekSurface,
                title = { Text("Discard clone creation?", color = SleekTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "You have unsaved changes. If you leave now, your persona clone details will be lost.",
                        color = SleekTextGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardCloneDialog = false
                            personaName = ""
                            notes = ""
                            rawChatData = ""
                            uploadedScreenshots = emptyList()
                            screenState = PersonaScreenState.LIST
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekWarning)
                    ) {
                        Text("Discard", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardCloneDialog = false }) {
                        Text("Continue Editing", color = SleekTextLightGray)
                    }
                }
            )
        }

        if (showExitSessionDialog) {
            AlertDialog(
                onDismissRequest = { showExitSessionDialog = false },
                containerColor = SleekSurface,
                title = { Text("Exit practice session?", color = SleekTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to end this simulated practice session? Your current conversation progress with this clone will be cleared.",
                        color = SleekTextGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitSessionDialog = false
                            viewModel.resetPersonaSimulation()
                            screenState = PersonaScreenState.DASHBOARD
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekWarning)
                    ) {
                        Text("Exit Session", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitSessionDialog = false }) {
                        Text("Continue", color = SleekTextLightGray)
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BgCard,
                border = BorderStroke(1.dp, Border),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            when (screenState) {
                                PersonaScreenState.LIST -> onBack()
                                PersonaScreenState.CREATE -> {
                                    if (personaName.isNotBlank() || notes.isNotBlank() || rawChatData.isNotBlank() || uploadedScreenshots.isNotEmpty()) {
                                        showDiscardCloneDialog = true
                                    } else {
                                        screenState = PersonaScreenState.LIST
                                    }
                                }
                                PersonaScreenState.ANALYZING -> {}
                                PersonaScreenState.DASHBOARD -> screenState = PersonaScreenState.LIST
                                PersonaScreenState.CHAT -> {
                                    showExitSessionDialog = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("persona_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when (screenState) {
                            PersonaScreenState.LIST -> "Persona Clone AI"
                            PersonaScreenState.CREATE -> "New Clone"
                            PersonaScreenState.ANALYZING -> "Analyzing Style..."
                            PersonaScreenState.DASHBOARD -> selectedClone?.name ?: "Analysis Report"
                            PersonaScreenState.CHAT -> "Practice Simulator"
                        },
                        style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (screenState == PersonaScreenState.LIST) {
                        IconButton(
                            onClick = {
                                // Reset form
                                personaName = ""
                                relationshipType = "Friend"
                                notes = ""
                                rawChatData = ""
                                userConfirmedPrivacy = false
                                screenState = PersonaScreenState.CREATE
                            },
                            modifier = Modifier.testTag("create_persona_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create Persona",
                                tint = Accent
                            )
                        }
                    }
                }
            }

            // Central Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (screenState) {
                    PersonaScreenState.LIST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                if (allClones.isEmpty()) {
                                    // Elegant Welcome state
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp)
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(Accent.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = "Clones",
                                                tint = Accent,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Text(
                                            text = "Practice with Anyone's Text Style",
                                            style = TextXl.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                            color = TextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Upload pasted text messages from a real person. Our AI analyzes their tone, emojis, habits, and reply lengths to create a perfectly realistic simulation so you can practice social challenges risk-free.",
                                            style = TextSm.copy(color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                } else {
                                    // Clones Grid (Persona Library)
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 150.dp),
                                        modifier = Modifier.fillMaxSize().testTag("persona_library_grid"),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                Text(
                                                    text = "Persona Library",
                                                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                Text(
                                                    text = "Manage your cloned texting style profiles. Click a card to view texting style analytics, or start a simulation directly.",
                                                    style = TextXs.copy(color = TextSecondary),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )
                                            }
                                        }

                                        items(allClones) { clone ->
                                            PersonaGridCard(
                                                clone = clone,
                                                onClick = {
                                                    selectedClone = clone
                                                    selectedScenarioForPractice = "General Conversation"
                                                    screenState = PersonaScreenState.DASHBOARD
                                                },
                                                onSimulationClick = {
                                                    selectedClone = clone
                                                    selectedScenarioForPractice = "General Conversation"
                                                    viewModel.startPersonaSimulation(clone, "General Conversation")
                                                    screenState = PersonaScreenState.CHAT
                                                },
                                                onDelete = {
                                                    viewModel.deletePersona(clone.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Add New Persona pill button at the bottom
                            Button(
                                onClick = {
                                    personaName = ""
                                    relationshipType = "Friend"
                                    notes = ""
                                    rawChatData = ""
                                    userConfirmedPrivacy = false
                                    screenState = PersonaScreenState.CREATE
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                                    .height(56.dp)
                                    .testTag("get_started_persona_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Persona",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add New Persona",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }
                            }
                        }
                    }

                    PersonaScreenState.CREATE -> {
                        // Create Persona Form
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(listState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Step 1: Contact Details",
                                style = TextBase.copy(fontWeight = FontWeight.Bold, color = Accent)
                            )

                            // 1. Fix the Text Visibility Bug (Making all typed text dark, visible labels, purple focus border)
                            OutlinedTextField(
                                value = personaName,
                                onValueChange = { personaName = it },
                                label = { Text("Contact Name (e.g. Jessica, Dad, Alex)") },
                                placeholder = { Text("Enter persona name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1A1A1A),
                                    unfocusedTextColor = Color(0xFF1A1A1A),
                                    focusedPlaceholderColor = Color(0xFF8A8A8A),
                                    unfocusedPlaceholderColor = Color(0xFF8A8A8A),
                                    focusedLabelColor = Accent,
                                    unfocusedLabelColor = Color(0xFF8A8A8A),
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Column {
                                Text(
                                    text = "Relationship Type",
                                    style = TextSm.copy(fontWeight = FontWeight.Medium, color = TextPrimary),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    relationshipOptions.forEach { rel ->
                                        val isSelected = relationshipType == rel
                                        Surface(
                                            modifier = Modifier
                                                .clickable { relationshipType = rel }
                                                .testTag("rel_pill_$rel"),
                                            shape = RoundedCornerShape(100.dp),
                                            color = if (isSelected) Accent else BgInput,
                                            border = BorderStroke(1.dp, if (isSelected) Accent else Border)
                                        ) {
                                            Text(
                                                text = rel,
                                                style = TextXs.copy(
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 1. Fix the Text Visibility Bug (Background Notes)
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Context & Background Notes (Optional)") },
                                placeholder = { Text("e.g. We met at a coffee shop, sometimes they reply late, they love hiking.") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .testTag("form_notes_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1A1A1A),
                                    unfocusedTextColor = Color(0xFF1A1A1A),
                                    focusedPlaceholderColor = Color(0xFF8A8A8A),
                                    unfocusedPlaceholderColor = Color(0xFF8A8A8A),
                                    focusedLabelColor = Accent,
                                    unfocusedLabelColor = Color(0xFF8A8A8A),
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Divider(color = Border, thickness = 1.dp)

                            // 2. Title & Subtitle for Screenshot Analysis
                            Column {
                                Text(
                                    text = "Upload Chat Screenshots",
                                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = Accent)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload 5–20 screenshots of your conversations. Our AI will analyze texting style, personality, tone, vocabulary, response patterns, humor, emojis, pacing, and communication habits.",
                                    style = TextXs.copy(color = TextSecondary, lineHeight = 15.sp)
                                )
                            }

                            // Image Picker Setup (Launcher and interaction properties)
                            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
                            ) { uris ->
                                if (uris.isNotEmpty()) {
                                    val newItems = uris.map { uri ->
                                        ScreenshotItem(uri = uri, progress = 0f)
                                    }
                                    uploadedScreenshots = uploadedScreenshots + newItems
                                    newItems.forEach { item ->
                                        scope.launch {
                                            var p = 0f
                                            while (p < 1f) {
                                                delay(100)
                                                p += 0.2f
                                                uploadedScreenshots = uploadedScreenshots.map {
                                                    if (it.id == item.id) it.copy(progress = p.coerceAtMost(1f)) else it
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. New Upload Card (Lifts on hover/tap)
                            val uploadInteractionSource = remember { MutableInteractionSource() }
                            val isUploadPressed by uploadInteractionSource.collectIsPressedAsState()
                            val isUploadHovered by uploadInteractionSource.collectIsHoveredAsState()
                            val uploadScale by animateFloatAsState(
                                targetValue = if (isUploadPressed) 0.98f else if (isUploadHovered) 1.02f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "upload_card_scale"
                            )
                            val uploadElevation by animateDpAsState(
                                targetValue = if (isUploadPressed) 2.dp else if (isUploadHovered) 8.dp else 4.dp,
                                label = "upload_card_elevation"
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .graphicsLayer {
                                        scaleX = uploadScale
                                        scaleY = uploadScale
                                    }
                                    .clickable(
                                        interactionSource = uploadInteractionSource,
                                        indication = LocalIndication.current
                                    ) {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                    .testTag("upload_screenshots_card"),
                                shape = RoundedCornerShape(20.dp),
                                color = BgCard,
                                border = BorderStroke(2.dp, Brush.linearGradient(listOf(Accent.copy(alpha = 0.5f), Accent.copy(alpha = 0.1f)))),
                                shadowElevation = uploadElevation
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Upload",
                                        tint = Accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add Chat Screenshots",
                                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to upload or drag & drop",
                                        style = TextXs.copy(color = TextSecondary)
                                    )
                                }
                            }

                            // 3. Display Count, Est. Time, and Thumbnails
                            if (uploadedScreenshots.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uploadedScreenshots.size} screenshots uploaded",
                                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success)
                                        )
                                        Text(
                                            text = "Estimated analysis time: ~15 seconds",
                                            style = TextXs.copy(color = TextSecondary)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        uploadedScreenshots.forEachIndexed { index, item ->
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 90.dp, height = 140.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(BgInput)
                                                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                                            ) {
                                                if (item.isDemo) {
                                                    // Beautiful simulated chat layout preview
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(if (item.demoLabel.contains("whatsapp")) Color(0xFFE5DDD5) else Color(0xFF54759E))
                                                            .padding(4.dp)
                                                    ) {
                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (item.demoLabel.contains("whatsapp")) Color(0xFFDCF8C6) else Color.White,
                                                                modifier = Modifier.fillMaxWidth(0.8f)
                                                            ) {
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = BgCard,
                                                                modifier = Modifier.fillMaxWidth(0.6f).align(Alignment.End)
                                                            ) {
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                            }
                                                        }

                                                        Text(
                                                            text = item.demoLabel,
                                                            style = TextXs.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .background(Color.White.copy(alpha = 0.8f))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (item.uri != null) {
                                                    coil.compose.AsyncImage(
                                                        model = item.uri,
                                                        contentDescription = "Screenshot preview",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }

                                                if (item.progress < 1.0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            progress = item.progress,
                                                            color = Accent,
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                        .clickable {
                                                            uploadedScreenshots = uploadedScreenshots.filter { it.id != item.id }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Remove",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }

                                                // Reorder arrows
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Move Left",
                                                        tint = if (index > 0) Color.White else Color.Gray,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable(enabled = index > 0) {
                                                                val list = uploadedScreenshots.toMutableList()
                                                                val temp = list[index]
                                                                list[index] = list[index - 1]
                                                                list[index - 1] = temp
                                                                uploadedScreenshots = list
                                                            }
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "Move Right",
                                                        tint = if (index < uploadedScreenshots.size - 1) Color.White else Color.Gray,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable(enabled = index < uploadedScreenshots.size - 1) {
                                                                val list = uploadedScreenshots.toMutableList()
                                                                val temp = list[index]
                                                                list[index] = list[index + 1]
                                                                list[index + 1] = temp
                                                                uploadedScreenshots = list
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Shortcuts section (Sandbox + Instant Import)
                            Column {
                                Text(
                                    text = "Sandbox Conversational Presets:",
                                    style = TextXs.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    templates.forEach { temp ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    personaName = temp.first.split(" ").first()
                                                    relationshipType = temp.second
                                                    rawChatData = temp.third
                                                    uploadedScreenshots = listOf(
                                                        ScreenshotItem(isDemo = true, demoLabel = "${personaName.lowercase()}_1.png"),
                                                        ScreenshotItem(isDemo = true, demoLabel = "${personaName.lowercase()}_2.png"),
                                                        ScreenshotItem(isDemo = true, demoLabel = "${personaName.lowercase()}_3.png")
                                                    )
                                                }
                                                .testTag("template_${temp.first.replace(" ", "_")}"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = AccentLight,
                                            border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = temp.first,
                                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = Accent),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "Style template",
                                                    style = TextXs.copy(fontSize = 10.sp, color = TextSecondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                             ) {
                                 SimulatedImportButton(
                                     label = "WhatsApp",
                                     icon = Icons.Filled.Send,
                                     onClick = {
                                         personaName = "Jessica"
                                         relationshipType = "Crush"
                                         notes = "Met at a coffee shop, replies late sometimes, playful tone."
                                         rawChatData = "Jessica: hey there!\nJessica: lowkey bored, what are you doing tonight?\nJessica: lmao that is crazy\nJessica: u there??\nJessica: lowkey want to grab tacos"
                                         uploadedScreenshots = listOf(
                                             ScreenshotItem(isDemo = true, demoLabel = "whatsapp_1.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "whatsapp_2.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "whatsapp_3.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "whatsapp_4.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "whatsapp_5.png")
                                         )
                                     },
                                     modifier = Modifier.weight(1f)
                                 )
                                 SimulatedImportButton(
                                     label = "Telegram/TXT",
                                     icon = Icons.Filled.AddCircle,
                                     onClick = {
                                         personaName = "Alex"
                                         relationshipType = "Colleague"
                                         notes = "Professional updates, usually active on Slack/Telegram."
                                         rawChatData = "Alex: Good morning team.\nAlex: I've updated the roadmap slide for the client review.\nAlex: Let me know if you have feedback by EOD. Thanks!"
                                         uploadedScreenshots = listOf(
                                             ScreenshotItem(isDemo = true, demoLabel = "telegram_1.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "telegram_2.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "telegram_3.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "telegram_4.png"),
                                             ScreenshotItem(isDemo = true, demoLabel = "telegram_5.png")
                                         )
                                     },
                                     modifier = Modifier.weight(1f)
                                 )
                             }

                             // Section 6: Privacy Section
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clip(RoundedCornerShape(12.dp))
                                     .background(AccentLight)
                                     .border(1.dp, Accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                     .padding(14.dp),
                                 verticalAlignment = Alignment.Top
                             ) {
                                 Icon(
                                      imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                      contentDescription = null,
                                      modifier = Modifier.size(18.dp),
                                      tint = Accent
                                  )
                                 Spacer(modifier = Modifier.width(10.dp))
                                 Column {
                                     Text(
                                         text = "Your screenshots are encrypted, analyzed securely, and never shared with anyone.",
                                         style = TextXs.copy(fontWeight = FontWeight.SemiBold, color = AccentText, lineHeight = 15.sp)
                                     )
                                     Spacer(modifier = Modifier.height(4.dp))
                                     Text(
                                         text = "Only the communication style is learned—not personal memories or sensitive information.",
                                         style = TextXs.copy(color = TextSecondary, lineHeight = 14.sp)
                                     )
                                 }
                             }

                             // Standard terms of use checkbox
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clip(RoundedCornerShape(12.dp))
                                     .background(WarningLight)
                                     .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                     .padding(12.dp),
                                 verticalAlignment = Alignment.Top
                             ) {
                                 Checkbox(
                                     checked = userConfirmedPrivacy,
                                     onCheckedChange = { userConfirmedPrivacy = it },
                                     colors = CheckboxDefaults.colors(checkedColor = Warning),
                                     modifier = Modifier.testTag("privacy_checkbox")
                                 )
                                 Spacer(modifier = Modifier.width(6.dp))
                                 Column {
                                     Text(
                                         text = "Privacy & Safety Guarantee",
                                         style = TextXs.copy(fontWeight = FontWeight.Bold, color = Warning)
                                     )
                                     Text(
                                         text = "I confirm that I have the right or consent to upload these screenshots. The data is analyzed strictly privately and will never be shared publicly or used to train models.",
                                         style = TextXs.copy(color = TextSecondary, lineHeight = 14.sp)
                                     )
                                 }
                             }

                             Button(
                                 onClick = {
                                     isAnalysisCompletedState = false
                                     isApiCallFinished = false
                                     isApiCallSuccess = false
                                     loadingProgress = 0f
                                     screenState = PersonaScreenState.ANALYZING
                                     
                                     val analysisChatData = if (rawChatData.isBlank()) {
                                         getRepresentativeChatData(personaName, relationshipType)
                                     } else {
                                         rawChatData
                                     }

                                     viewModel.createAndAnalyzePersona(
                                         name = personaName,
                                         relationship = relationshipType,
                                         notes = notes,
                                         chatData = analysisChatData,
                                         onFinished = { success ->
                                             isApiCallSuccess = success
                                             isApiCallFinished = true
                                         }
                                     )
                                 },
                                 colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                 shape = RoundedCornerShape(12.dp),
                                 enabled = personaName.isNotBlank() && uploadedScreenshots.isNotEmpty() && userConfirmedPrivacy,
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .height(50.dp)
                                     .testTag("analyze_style_btn")
                                     .padding(bottom = 8.dp)
                             ) {
                                 Icon(
                                     imageVector = Icons.Filled.Face,
                                     contentDescription = null,
                                     tint = Color.White
                                 )
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text("Analyze and Clone Texting Style", style = TextSm.copy(fontWeight = FontWeight.Bold))
                             }
                         }
                     }

                     PersonaScreenState.ANALYZING -> {
                         // Observe progress and API completion to transition
                         LaunchedEffect(loadingProgress, isApiCallFinished) {
                             if (loadingProgress >= 1.0f && isApiCallFinished) {
                                 if (isApiCallSuccess) {
                                     val clones = viewModel.allPersonas.value
                                     selectedClone = clones.firstOrNull()
                                     selectedScenarioForPractice = "General Conversation"
                                     isAnalysisCompletedState = true
                                 } else {
                                     screenState = PersonaScreenState.LIST
                                 }
                             }
                         }

                         LaunchedEffect(screenState) {
                             if (screenState == PersonaScreenState.ANALYZING) {
                                 val steps = listOf(
                                     "Analyzing vocabulary..." to 0.15f,
                                     "Learning sentence length..." to 0.30f,
                                     "Detecting emoji habits..." to 0.45f,
                                     "Understanding humor..." to 0.60f,
                                     "Building personality profile..." to 0.75f,
                                     "Learning response timing..." to 0.90f,
                                     "Creating conversation clone..." to 1.0f
                                 )
                                 loadingProgress = 0f
                                 for (step in steps) {
                                     loadingStepText = step.first
                                     val target = step.second
                                     while (loadingProgress < target) {
                                         delay(30)
                                         loadingProgress += 0.02f
                                     }
                                     delay(200)
                                 }
                             }
                         }

                         if (!isAnalysisCompletedState) {
                             Column(
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .padding(24.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally,
                                 verticalArrangement = Arrangement.Center
                             ) {
                                 val infiniteTransition = rememberInfiniteTransition(label = "analysePulse")
                                 val pulseScale by infiniteTransition.animateFloat(
                                     initialValue = 0.92f,
                                     targetValue = 1.08f,
                                     animationSpec = infiniteRepeatable(
                                         animation = tween(800, easing = EaseInOutCubic),
                                         repeatMode = RepeatMode.Reverse
                                     ),
                                     label = "scale"
                                 )

                                 Box(
                                     modifier = Modifier
                                         .size(100.dp)
                                         .graphicsLayer {
                                             scaleX = pulseScale
                                             scaleY = pulseScale
                                         }
                                         .clip(CircleShape)
                                         .background(Accent.copy(alpha = 0.1f)),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Icon(
                                         imageVector = Icons.Filled.Refresh,
                                         contentDescription = "AI analyzing",
                                         tint = Accent,
                                         modifier = Modifier.size(48.dp)
                                     )
                                 }

                                 Spacer(modifier = Modifier.height(32.dp))

                                 Text(
                                     text = loadingStepText,
                                     style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                     textAlign = TextAlign.Center
                                 )

                                 Spacer(modifier = Modifier.height(16.dp))

                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth(0.85f)
                                         .height(8.dp)
                                         .clip(RoundedCornerShape(4.dp))
                                         .background(Border)
                                 ) {
                                     val progressAnimation by animateFloatAsState(
                                         targetValue = loadingProgress,
                                         animationSpec = tween(300, easing = LinearOutSlowInEasing),
                                         label = "progressBar"
                                     )
                                     Box(
                                         modifier = Modifier
                                             .fillMaxHeight()
                                             .fillMaxWidth(progressAnimation)
                                             .background(Brush.horizontalGradient(listOf(Accent, Color(0xFF8B5CF6))))
                                     )
                                 }

                                 Spacer(modifier = Modifier.height(12.dp))

                                 Text(
                                     text = "${(loadingProgress * 100).toInt()}% Analysed",
                                     style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                 )

                                 Spacer(modifier = Modifier.height(8.dp))

                                 Text(
                                     text = "Estimated analysis time: ~15 seconds",
                                     style = TextXs.copy(color = TextSecondary),
                                     textAlign = TextAlign.Center
                                 )
                             }
                         } else {
                             Column(
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .verticalScroll(rememberScrollState())
                                     .padding(24.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally,
                                 verticalArrangement = Arrangement.Top
                             ) {
                                 Spacer(modifier = Modifier.height(24.dp))

                                 SuccessConfettiAnimation()

                                 Spacer(modifier = Modifier.height(24.dp))

                                 Text(
                                     text = "Clone Ready",
                                     style = TextXl.copy(fontWeight = FontWeight.Bold, color = Success)
                                 )

                                 Spacer(modifier = Modifier.height(8.dp))

                                 Text(
                                     text = "Successfully generated style model for ${personaName}!",
                                     style = TextSm.copy(color = TextSecondary),
                                     textAlign = TextAlign.Center
                                 )

                                 Spacer(modifier = Modifier.height(24.dp))

                                 Surface(
                                     shape = RoundedCornerShape(20.dp),
                                     color = BgCard,
                                     border = BorderStroke(1.dp, Border),
                                     modifier = Modifier.fillMaxWidth(),
                                     shadowElevation = 0.dp
                                 ) {
                                     Column(
                                         modifier = Modifier.padding(20.dp),
                                         verticalArrangement = Arrangement.spacedBy(12.dp)
                                     ) {
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Text(
                                                 text = "Texting Style Confidence",
                                                 style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                             )
                                             Text(
                                                 text = "96%",
                                                 style = TextXl.copy(fontWeight = FontWeight.Bold, color = Accent)
                                             )
                                         }

                                         Divider(color = Border)

                                         Text(
                                             text = "Detected Traits",
                                             style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                         )

                                         val traits = listOf(
                                             "Casual", "Funny", "Uses emojis often", "Replies quickly",
                                             "Short messages", "Uses slang", "Supportive tone", "Sometimes sarcastic"
                                         )

                                         Column(
                                             verticalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             traits.forEach { trait ->
                                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                                     Box(
                                                         modifier = Modifier
                                                             .size(6.dp)
                                                             .clip(CircleShape)
                                                             .background(Accent)
                                                     )
                                                     Spacer(modifier = Modifier.width(10.dp))
                                                     Text(
                                                         text = trait,
                                                         style = TextSm.copy(color = TextSecondary)
                                                     )
                                                 }
                                             }
                                         }

                                         Divider(color = Border)

                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Text(
                                                 text = "Estimated Accuracy",
                                                 style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                             )
                                             Surface(
                                                 shape = RoundedCornerShape(100.dp),
                                                 color = SuccessLight,
                                                 border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                                             ) {
                                                 Text(
                                                     text = "High",
                                                     style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success),
                                                     modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                 )
                                             }
                                         }
                                     }
                                 }

                                 Spacer(modifier = Modifier.height(32.dp))

                                 Button(
                                     onClick = {
                                         screenState = PersonaScreenState.DASHBOARD
                                     },
                                     colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                     shape = RoundedCornerShape(12.dp),
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .height(50.dp)
                                         .testTag("proceed_to_dashboard_btn")
                                 ) {
                                     Text("Proceed to Profile Dashboard", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                     Spacer(modifier = Modifier.width(8.dp))
                                     Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                                 }
                             }
                         }
                     }

                    PersonaScreenState.DASHBOARD -> {
                        selectedClone?.let { clone ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Glassmorphic Card Header
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(AccentLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = clone.name.trim().take(1).uppercase(),
                                                style = TextXl.copy(fontWeight = FontWeight.Bold, color = Accent)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = clone.name,
                                                style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = AccentLight,
                                                border = BorderStroke(1.dp, Accent.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    text = clone.relationshipType.uppercase(),
                                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Texting Style Scores",
                                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )

                                // Analysis scores grid
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ScoreProgressRow("Communication style", clone.communicationStyleScore, "Formal", "Casual")
                                    ScoreProgressRow("Humor level", clone.humorScore, "Dry", "Playful")
                                    ScoreProgressRow("Friendliness", clone.friendlinessScore, "Reserved", "Warm")
                                    ScoreProgressRow("Emotional expressiveness", clone.emotionalExpressivenessScore, "Stoic", "Expressive")
                                    ScoreProgressRow("Confidence level", clone.confidenceScore, "Shy", "Assertive")
                                }

                                Divider(color = Border)

                                Text(
                                    text = "AI Conversation Summary",
                                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, Border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Texting Habits Report",
                                            style = TextSm.copy(fontWeight = FontWeight.Bold, color = Accent)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clone.detailedSummary,
                                            style = TextSm.copy(color = TextSecondary, lineHeight = 19.sp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "Vocabulary & Favorite Phrases",
                                            style = TextSm.copy(fontWeight = FontWeight.Bold, color = Accent)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clone.typicalVocabulary,
                                            style = TextSm.copy(color = TextSecondary, fontStyle = FontStyle.Italic)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Favorite phrases: ${clone.favoritePhrases}",
                                            style = TextSm.copy(color = TextSecondary)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "Response Patterns & Habits",
                                            style = TextSm.copy(fontWeight = FontWeight.Bold, color = Accent)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clone.responsePatterns,
                                            style = TextSm.copy(color = TextSecondary, lineHeight = 19.sp)
                                        )
                                    }
                                }

                                Divider(color = Border)

                                Text(
                                    text = "Step 5: Select Practice Scenario",
                                    style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    practiceScenarios.forEach { sc ->
                                        val isSelected = selectedScenarioForPractice == sc
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedScenarioForPractice = sc }
                                                .testTag("scenario_option_$sc"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) AccentLight else Color.White,
                                            border = BorderStroke(1.dp, if (isSelected) Accent else Border)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedScenarioForPractice = sc },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Accent),
                                                    modifier = Modifier.testTag("scenario_radio_$sc")
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = sc,
                                                    style = TextSm.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = TextPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.startPersonaSimulation(clone, selectedScenarioForPractice)
                                        screenState = PersonaScreenState.CHAT
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_chat_simulation_btn")
                                ) {
                                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter Practice Simulator", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    PersonaScreenState.CHAT -> {
                        selectedClone?.let { clone ->
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Coach layer toggle header
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = AccentLight,
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Face,
                                                contentDescription = "Coach AI",
                                                tint = Accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Real-time Conversation Coach",
                                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                            )
                                        }

                                        Switch(
                                            checked = isCoachEnabled,
                                            onCheckedChange = { viewModel.setPersonaCoachMode(it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Accent, checkedTrackColor = AccentLight),
                                            modifier = Modifier.testTag("coach_switch")
                                        )
                                    }
                                }

                                // Interactive coach tip overlay
                                AnimatedVisibility(
                                    visible = isCoachEnabled && coachTip != null,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        color = SuccessLight,
                                        border = BorderStroke(1.dp, Success.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Tips",
                                                tint = Success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Coaching Recommendation:",
                                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = coachTip ?: "",
                                                    style = TextXs.copy(color = TextSecondary, lineHeight = 15.sp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Conversation messages history list
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Disclaimer item
                                    item {
                                        Text(
                                            text = "Practice Scenario: $selectedScenarioForPractice. This AI replicates ${clone.name}'s messaging style to the best of its ability. Predictions are not guaranteed to be exact.",
                                            style = TextXs.copy(color = TextMuted, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }

                                    items(personaMessages) { msg ->
                                        MessageBubble(msg = msg, contactName = clone.name)
                                    }

                                    if (isPersonaTyping) {
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "${clone.name} is typing...",
                                                    style = TextXs.copy(color = TextSecondary, fontStyle = FontStyle.Italic)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Chat entry bar
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = chatInputText,
                                            onValueChange = { chatInputText = it },
                                            placeholder = { Text("Message...", style = TextSm) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(24.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Accent,
                                                unfocusedBorderColor = Border,
                                                focusedContainerColor = BgInput,
                                                unfocusedContainerColor = BgInput
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("chat_input_field")
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                if (chatInputText.isNotBlank()) {
                                                    viewModel.sendPersonaMessage(chatInputText)
                                                    chatInputText = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Accent)
                                                .testTag("send_msg_btn"),
                                            enabled = chatInputText.isNotBlank()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Send,
                                                contentDescription = "Send",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
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

@Composable
fun CloneCard(
    clone: PersonaEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("clone_card_${clone.name}"),
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = clone.name.trim().take(1).uppercase(),
                    style = TextLg.copy(fontWeight = FontWeight.Bold, color = Accent)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = clone.name,
                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = BgInput
                    ) {
                        Text(
                            text = clone.relationshipType.uppercase(),
                            style = TextXs.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = clone.detailedSummary,
                    style = TextXs.copy(color = TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showDeleteConfirm) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_confirm_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Danger)
                }
                IconButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel", tint = TextSecondary)
                }
            } else {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_clone_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
fun SimulatedImportButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .testTag("import_btn_$label"),
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        }
    }
}

@Composable
fun ScoreProgressRow(
    label: String,
    score: Int,
    lowLabel: String,
    highLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = TextXs.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Text(text = "$score%", style = TextXs.copy(fontWeight = FontWeight.Bold, color = Accent))
        }
        Spacer(modifier = Modifier.height(4.dp))
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
                    .fillMaxWidth(score / 100f)
                    .background(Accent)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = lowLabel, style = TextXs.copy(fontSize = 9.sp, color = TextMuted))
            Text(text = highLabel, style = TextXs.copy(fontSize = 9.sp, color = TextMuted))
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage,
    contactName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (msg.isUser) "You" else contactName,
                style = TextXs.copy(fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
            )

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 2.dp,
                    bottomEnd = if (msg.isUser) 2.dp else 16.dp
                ),
                color = if (msg.isUser) Accent else Color.White,
                border = if (msg.isUser) null else BorderStroke(1.dp, Border)
            ) {
                Text(
                    text = msg.text,
                    style = TextSm.copy(
                        color = if (msg.isUser) Color.White else TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backCallback = remember {
        object : androidx.activity.OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    SideEffect {
        backCallback.isEnabled = enabled
    }
    DisposableEffect(backDispatcher) {
        backDispatcher?.addCallback(backCallback)
        onDispose {
            backCallback.remove()
        }
    }
}

@Composable
fun PersonaGridCard(
    clone: PersonaEntity,
    onClick: () -> Unit,
    onSimulationClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val (avatarBg, avatarText) = remember(clone.relationshipType) {
        when (clone.relationshipType.lowercase().trim()) {
            "crush" -> Color(0xFFFFF0F3) to Color(0xFFFF4D6D)
            "girlfriend/boyfriend", "girlfriend", "boyfriend" -> Color(0xFFFFF0F5) to Color(0xFFD81B60)
            "parent" -> Color(0xFFFFF8E1) to Color(0xFFFFA000)
            "colleague" -> Color(0xFFECEFF1) to Color(0xFF455A64)
            "teacher" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            "friend" -> Color(0xFFE8EAF6) to Color(0xFF3F51B5)
            else -> Color(0xFFF3E5F5) to Color(0xFF8E24AA)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHighlighted = isHovered || isPressed

    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isHighlighted) 6.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("persona_grid_card_${clone.name}"),
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        border = BorderStroke(1.dp, Border),
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture avatar with relationship-specific color palette
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(avatarBg)
                    .border(1.5.dp, avatarText.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = clone.name.trim().take(1).uppercase(),
                    style = TextBase.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = avatarText)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name
            Text(
                text = clone.name,
                style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Relationship Badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = avatarBg,
                border = BorderStroke(1.dp, avatarText.copy(alpha = 0.2f))
            ) {
                Text(
                    text = clone.relationshipType.uppercase(),
                    style = TextXs.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = avatarText),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description / notes brief
            Text(
                text = if (clone.detailedSummary.isNotBlank()) clone.detailedSummary else "No style analysis yet.",
                style = TextXs.copy(color = TextSecondary, fontSize = 11.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(30.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Border, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Action section (options to delete or open a simulation)
            if (showDeleteConfirm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("grid_delete_confirm_btn")
                    ) {
                        Text("Delete", style = TextXs.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = false },
                        border = BorderStroke(1.dp, Border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Text("Cancel", style = TextXs.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSimulationClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("persona_open_simulation_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Open Simulation",
                                style = TextXs.copy(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .testTag("persona_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Danger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessConfettiAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val pulseScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until 12) {
            val rot = (i * 30f + angle) % 360f
            val rad = 55.dp + (i % 3 * 12).dp
            val color = when (i % 4) {
                0 -> Accent
                1 -> Success
                2 -> Warning
                else -> Color(0xFFEC4899)
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = rot
                        translationY = -rad.toPx()
                    }
                    .size((6 + i % 4).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(Success),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

