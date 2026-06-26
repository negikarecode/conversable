package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageCatalog
import com.example.data.model.SupportedLanguage
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // ViewModel state bindings
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val selectedAccent by viewModel.selectedAccent.collectAsState()
    val selectedScript by viewModel.selectedScript.collectAsState()
    val isCorrectMeEnabled by viewModel.isCorrectMeEnabled.collectAsState()
    val isHinglishModeEnabled by viewModel.isHinglishModeEnabled.collectAsState()
    val favoriteLanguages by viewModel.favoriteLanguages.collectAsState()
    val recentLanguages by viewModel.recentLanguages.collectAsState()
    val mixedLanguagePartner by viewModel.mixedLanguagePartner.collectAsState()

    // Local UI states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("All") } // "All", "Global", "Indian", "Favorites"
    var activeDetailsLanguage by remember { mutableStateOf<SupportedLanguage?>(null) }
    
    // Bottom Sheet State for Language Details Configuration
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // Search Filtering
    val filteredLanguages = remember(searchQuery, selectedCategoryTab, favoriteLanguages, recentLanguages) {
        val baseList = when (selectedCategoryTab) {
            "Global" -> LanguageCatalog.languages.filter { it.category == "Global" }
            "Indian" -> LanguageCatalog.languages.filter { it.category == "Indian" }
            "Favorites" -> LanguageCatalog.languages.filter { favoriteLanguages.contains(it.name) }
            "Recent" -> LanguageCatalog.languages.filter { recentLanguages.contains(it.name) }
            else -> LanguageCatalog.languages
        }
        
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nativeName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Language Coach",
                        style = TextBase.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("language_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Quick stats/info indicator or help button
                    IconButton(onClick = { /* Help context */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard),
            )
        },
        containerColor = SleekBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Current Active Coach Panel
            ActiveLanguageBanner(
                languageName = selectedLang,
                accentName = selectedAccent,
                scriptName = selectedScript,
                isCorrectMe = isCorrectMeEnabled,
                isHinglish = isHinglishModeEnabled,
                mixedPartner = mixedLanguagePartner
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Spotify-Style Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 70+ languages, scripts, or dialects...", style = TextSm.copy(color = TextMuted)) },
                prefix = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = TextSecondary,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp)
                    )
                },
                suffix = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Spotify-Style Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("All", "Global", "Indian", "Favorites", "Recent")
                items(categories) { category ->
                    val isSelected = selectedCategoryTab == category
                    val chipBg = if (isSelected) Accent else BgCard
                    val chipText = if (isSelected) Color.White else TextSecondary
                    val chipBorder = if (isSelected) Accent else Border
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedCategoryTab = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_chip_$category"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = chipText)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Main Language List
            if (filteredLanguages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Not found",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No languages found matching \"$searchQuery\"",
                            style = TextSm.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Show sections if search query is empty to make it highly structured
                    if (searchQuery.isEmpty() && selectedCategoryTab == "All") {
                        // Quick recents section
                        val recentItems = LanguageCatalog.languages.filter { recentLanguages.contains(it.name) }
                        if (recentItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Recently Used",
                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            items(recentItems) { lang ->
                                LanguageCardItem(
                                    lang = lang,
                                    isFavorite = favoriteLanguages.contains(lang.name),
                                    isActive = selectedLang == lang.name,
                                    onFavoriteToggle = { viewModel.toggleFavorite(lang.name) },
                                    onClick = {
                                        activeDetailsLanguage = lang
                                        showBottomSheet = true
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }

                        // Favorites section
                        val favItems = LanguageCatalog.languages.filter { favoriteLanguages.contains(it.name) }
                        if (favItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Your Favorite Languages",
                                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            items(favItems) { lang ->
                                LanguageCardItem(
                                    lang = lang,
                                    isFavorite = true,
                                    isActive = selectedLang == lang.name,
                                    onFavoriteToggle = { viewModel.toggleFavorite(lang.name) },
                                    onClick = {
                                        activeDetailsLanguage = lang
                                        showBottomSheet = true
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }

                        // Divider or Heading for main list
                        item {
                            Text(
                                text = "All Available Languages",
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = TextMuted),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    items(filteredLanguages) { lang ->
                        LanguageCardItem(
                            lang = lang,
                            isFavorite = favoriteLanguages.contains(lang.name),
                            isActive = selectedLang == lang.name,
                            onFavoriteToggle = { viewModel.toggleFavorite(lang.name) },
                            onClick = {
                                activeDetailsLanguage = lang
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Modern Slide-up Bottom Sheet for Language Detail Configuration
    if (showBottomSheet && activeDetailsLanguage != null) {
        val lang = activeDetailsLanguage!!
        
        // Temporarily hold screen changes before confirmation
        var tempAccent by remember(lang) { mutableStateOf(if (selectedLang == lang.name) selectedAccent else lang.accents.firstOrNull() ?: "Standard") }
        var tempScript by remember(lang) { mutableStateOf(if (selectedLang == lang.name) selectedScript else lang.scripts.firstOrNull() ?: "Standard") }
        var tempCorrectMe by remember(lang) { mutableStateOf(if (selectedLang == lang.name) isCorrectMeEnabled else false) }
        var tempHinglishMode by remember(lang) { mutableStateOf(if (selectedLang == lang.name) isHinglishModeEnabled else (lang.id == "hinglish" || lang.name == "Hinglish")) }
        var tempMixedPartner by remember(lang) { mutableStateOf(if (selectedLang == lang.name) mixedLanguagePartner else "None") }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = BgCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Header with Flag and Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AccentLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = lang.flag, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.name,
                            style = TextLg.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = lang.nativeName,
                            style = TextSm.copy(color = TextSecondary)
                        )
                    }
                    
                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (lang.category == "Indian") Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = lang.category,
                            style = TextXs.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (lang.category == "Indian") Color(0xFFD97706) else Color(0xFF2563EB)
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(20.dp))

                // 1. Accents & Culture Mode Selection
                if (lang.accents.isNotEmpty()) {
                    Text(
                        text = "Regional Accent & Culture Style",
                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Changes vocabulary, common slang, greetings, and pronunciation matching regional habits.",
                        style = TextXs.copy(color = TextSecondary, lineHeight = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Chips row/flow for accents
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lang.accents.forEach { accent ->
                            val isAccentSelected = tempAccent == accent
                            val chipBg = if (isAccentSelected) AccentLight else BgInput
                            val chipText = if (isAccentSelected) AccentText else TextPrimary
                            val chipBorder = if (isAccentSelected) Accent else Color.Transparent
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable { tempAccent = accent }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isAccentSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Accent,
                                            modifier = Modifier.padding(end = 4.dp).size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = accent,
                                        style = TextXs.copy(fontWeight = FontWeight.SemiBold, color = chipText)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 2. Script Selector (if language supports multiple scripts)
                if (lang.scripts.size > 1) {
                    Text(
                        text = "Preferred Script / Alphabet",
                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose your reading preference. Choosing Latin writes phonetically (Hinglish/transliterated).",
                        style = TextXs.copy(color = TextSecondary, lineHeight = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lang.scripts.forEach { script ->
                            val isScriptSelected = tempScript == script
                            val chipBg = if (isScriptSelected) AccentLight else BgInput
                            val chipText = if (isScriptSelected) AccentText else TextPrimary
                            val chipBorder = if (isScriptSelected) Accent else Color.Transparent
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        tempScript = script
                                        // Auto-toggle Hinglish Mode toggle if Hindi has Latin script chosen
                                        if (lang.id == "hindi") {
                                            tempHinglishMode = script == "Latin"
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isScriptSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Accent,
                                            modifier = Modifier.padding(end = 4.dp).size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = script,
                                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = chipText)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 3. Learning Assistance: "Correct Me" Toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BgInput.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Check",
                            tint = Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable \"Correct Me\" Mode",
                                style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Corrects grammar, explains mistakes, suggests phrasing in voice/text chats.",
                                style = TextXs.copy(color = TextSecondary, lineHeight = 13.sp)
                            )
                        }
                        Switch(
                            checked = tempCorrectMe,
                            onCheckedChange = { tempCorrectMe = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Success, checkedTrackColor = SuccessLight)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Special Hinglish Mode for Hindi users
                if (lang.id == "hindi" || lang.id == "hinglish" || lang.name == "Hinglish") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AccentLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "IN", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Speak in Hinglish (Phonetic)",
                                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = AccentText)
                                )
                                Text(
                                    text = "Speak Hindi using Latin alphabet (e.g., \"Aap kaise ho?\"). Sounds natural and includes common local slangs.",
                                    style = TextXs.copy(color = TextSecondary, lineHeight = 13.sp)
                                )
                            }
                            Switch(
                                checked = tempHinglishMode,
                                onCheckedChange = { 
                                    tempHinglishMode = it
                                    if (it && lang.id == "hindi") {
                                        tempScript = "Latin"
                                    } else if (!it && lang.id == "hindi" && tempScript == "Latin") {
                                        tempScript = "Devanagari"
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Accent, checkedTrackColor = AccentLight)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 5. Mixed Language Mode Selection
                Text(
                    text = "Mixed Language Mode (Bilingual)",
                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Seamlessly blend your native tongue and secondary language together in a natural conversational flow.",
                    style = TextXs.copy(color = TextSecondary, lineHeight = 13.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val partners = listOf("None", "English", "Hindi", "Tamil", "Telugu", "Bengali")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(partners) { partner ->
                        val isPartnerSelected = tempMixedPartner == partner
                        val pBg = if (isPartnerSelected) Accent else BgInput
                        val pText = if (isPartnerSelected) Color.White else TextPrimary
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(pBg)
                                .clickable { tempMixedPartner = partner }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = partner,
                                style = TextXs.copy(fontWeight = FontWeight.Bold, color = pText)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Confirm CTA Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Update values in ViewModel
                            viewModel.setLanguage(lang.name, tempAccent, tempScript)
                            viewModel.toggleCorrectMe(tempCorrectMe)
                            viewModel.toggleHinglishMode(tempHinglishMode)
                            viewModel.setMixedLanguage(tempMixedPartner)
                            
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_language_button")
                ) {
                    Text(
                        text = "Apply Practice Environment Settings",
                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveLanguageBanner(
    languageName: String,
    accentName: String,
    scriptName: String,
    isCorrectMe: Boolean,
    isHinglish: Boolean,
    mixedPartner: String
) {
    val activeLangObj = remember(languageName) {
        LanguageCatalog.languages.find { it.name.equals(languageName, ignoreCase = true) }
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.4f), Color(0xFF8B5CF6).copy(alpha = 0.1f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = activeLangObj?.flag ?: "GL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE PRACTICE ENVIRONMENT",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = AccentText, letterSpacing = 0.05.sp)
                    )
                    Text(
                        text = "$languageName (${accentName} Style)",
                        style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessLight,
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Ready to speak",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = Success),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Border.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics and active special modes
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Alphabet/Script badge
                ActiveBadge(label = "Script: $scriptName", icon = Icons.Default.Edit, color = AccentText, bg = AccentLight)
                
                // Correct Me badge
                if (isCorrectMe) {
                    ActiveBadge(label = "Correct Me: ON", icon = Icons.Default.CheckCircle, color = Success, bg = SuccessLight)
                } else {
                    ActiveBadge(label = "Correct Me: OFF", icon = Icons.Default.Close, color = TextSecondary, bg = BgInput)
                }

                // Hinglish badge
                if (isHinglish) {
                    ActiveBadge(label = "Hinglish Mode", icon = Icons.Default.Face, color = Warning, bg = WarningLight)
                }

                // Bilingual mixed badge
                if (mixedPartner != "None") {
                    ActiveBadge(label = "Mixed with $mixedPartner", icon = Icons.Default.Share, color = Color(0xFF7C3AED), bg = Color(0xFFF3E8FF))
                }
            }
        }
    }
}

@Composable
fun ActiveBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bg: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = TextXs.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
    }
}

@Composable
fun LanguageCardItem(
    lang: SupportedLanguage,
    isFavorite: Boolean,
    isActive: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else if (isHovered) 1.01f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
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
            .testTag("language_card_${lang.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) AccentLight.copy(alpha = 0.4f) else BgCard,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) Accent else Border
        ),
        shadowElevation = if (isHovered) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isActive) AccentLight else BgInput),
                contentAlignment = Alignment.Center
            ) {
                Text(text = lang.flag, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(14.dp))

            // Text names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lang.name,
                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lang.nativeName,
                        style = TextXs.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${lang.popularity}% Popular",
                        style = TextXs.copy(fontWeight = FontWeight.SemiBold, color = AccentText)
                    )
                }
            }

            // Right Actions: Favorite Heart & Arrow Right
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag("fav_heart_${lang.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) Color(0xFFEF4444) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open Setup",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// FlowRow layout implementation backport to avoid dependency issues
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        val spacingPx = 8.dp.roundToPx()

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width + (if (currentRow.isNotEmpty()) spacingPx else 0) > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + spacingPx
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * spacingPx
        val maxWidth = if (rows.isNotEmpty()) rows.maxOf { row -> row.sumOf { it.width } + (row.size - 1) * spacingPx } else 0

        layout(
            width = constraints.maxWidth,
            height = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOf { it.height }
                row.forEach { placeable ->
                    placeable.placeRelative(x = x, y = y)
                    x += placeable.width + spacingPx
                }
                y += rowHeight + spacingPx
            }
        }
    }
}
