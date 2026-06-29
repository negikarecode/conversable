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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MarketplaceScenarioEntity
import com.example.data.model.Scenario
import com.example.data.model.ScenarioCatalog
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioMarketplaceScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    onStartScenario: (Scenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val dbScenarios by viewModel.marketplaceScenarios.collectAsState()
    val followedCreators by viewModel.followedCreators.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // AI Generation input
    var aiGeneratorInput by remember { mutableStateOf("") }
    var isGeneratingScenario by remember { mutableStateOf(false) }

    // Combine static catalog with database scenarios
    val allScenarios = remember(dbScenarios, followedCreators) {
        val catalogScenarios = ScenarioCatalog.scenarios.map {
            MarketplaceScenarioEntity(
                id = it.id,
                title = it.title,
                description = it.scenarioDescription,
                category = it.category,
                difficulty = it.difficulty,
                partnerName = it.partnerName,
                partnerAvatar = it.partnerAvatar,
                systemPrompt = it.hiddenGoal,
                initialMessage = it.initialMessage,
                isBookmarked = false,
                isPublished = true,
                creator = "Official"
            )
        }
        
        val merged = (catalogScenarios + dbScenarios).distinctBy { it.id }
        merged
    }

    // Filter scenarios
    val filteredScenarios = remember(allScenarios, searchQuery, selectedCategory) {
        allScenarios.filter { scenario ->
            val matchesSearch = scenario.title.contains(searchQuery, ignoreCase = true) ||
                    scenario.description.contains(searchQuery, ignoreCase = true) ||
                    scenario.partnerName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || scenario.category.lowercase() == selectedCategory.lowercase()
            matchesSearch && matchesCategory
        }
    }

    // Dynamic Recommendations based on DNA history
    val recommendations = remember(allScenarios) {
        // Recommend assertiveness/negotiation if low assertiveness, empathy if low empathy, etc.
        // For demonstration, recommend scenarios matching "Negotiation" and "Personal"
        allScenarios.filter { it.category.equals("Negotiation", ignoreCase = true) || it.id.contains("salary") }.take(2)
    }

    // Rating dialogue
    var showRatingDialogFor by remember { mutableStateOf<String?>(null) }
    var userRatingInput by remember { mutableStateOf(5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SCENARIO MARKETPLACE", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            // Search & Category Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search scenarios, roles, or partners...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SleekTextGray) }
                )
            }

            // Categories LazyRow
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All", "Work", "Personal", "Negotiation", "Conflict")) { category ->
                    val isSelected = category == selectedCategory
                    OutlinedCard(
                        onClick = { selectedCategory = category },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SleekPrimary else SleekSurface
                        ),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = category,
                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else SleekTextDark),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Scenario Generator Panel
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Build, contentDescription = "AI Builder", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI SCENARIO GENERATOR", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = aiGeneratorInput,
                                onValueChange = { aiGeneratorInput = it },
                                placeholder = { Text("What conversation scenario do you want to practice?\ne.g. Convince roommate to split utilities 50/50.") },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder,
                                    focusedTextColor = SleekTextDark,
                                    unfocusedTextColor = SleekTextDark,
                                    focusedPlaceholderColor = SleekTextGray,
                                    unfocusedPlaceholderColor = SleekTextGray,
                                    cursorColor = SleekPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val trimmed = aiGeneratorInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        isGeneratingScenario = true
                                        coroutineScope.launch {
                                            viewModel.generateScenarioWithAi(trimmed) { created ->
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
                                                    Toast.makeText(context, "Failed to generate scenario. Try again.", Toast.LENGTH_SHORT).show()
                                                }
                                                isGeneratingScenario = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = aiGeneratorInput.isNotBlank() && !isGeneratingScenario,
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                            ) {
                                if (isGeneratingScenario) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Creating your personalized scenario...", style = TextXs.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                    }
                                } else {
                                    Text("GENERATE SCENARIO", style = TextSm.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Recommendations Section
                if (recommendations.isNotEmpty()) {
                    item {
                        Text("RECOMMENDED FOR YOUR DNA PROFILE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                    }
                    items(recommendations) { scenario ->
                        MarketplaceScenarioCard(
                            scenario = scenario,
                            isFollowed = followedCreators.contains(scenario.creator),
                            onFollowToggle = { viewModel.followCreator(scenario.creator) },
                            onBookmarkToggle = { viewModel.bookmarkMarketplaceScenario(scenario.id, !scenario.isBookmarked) },
                            onRateClick = { showRatingDialogFor = scenario.id },
                            onPlayClick = {
                                val modelScenario = Scenario(
                                    id = scenario.id,
                                    title = scenario.title,
                                    category = scenario.category,
                                    partnerName = scenario.partnerName,
                                    partnerAvatar = scenario.partnerAvatar,
                                    partnerPersona = scenario.systemPrompt,
                                    scenarioDescription = scenario.description,
                                    hiddenGoal = scenario.systemPrompt,
                                    difficulty = scenario.difficulty,
                                    initialMessage = scenario.initialMessage
                                )
                                onStartScenario(modelScenario)
                            }
                        )
                    }
                }

                // Browse Results Section
                item {
                    Text("ALL MARKETPLACE SCENARIOS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                }

                if (filteredScenarios.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No scenarios matched your query.", style = TextXs.copy(color = SleekTextGray))
                            }
                        }
                    }
                } else {
                    items(filteredScenarios) { scenario ->
                        MarketplaceScenarioCard(
                            scenario = scenario,
                            isFollowed = followedCreators.contains(scenario.creator),
                            onFollowToggle = { viewModel.followCreator(scenario.creator) },
                            onBookmarkToggle = { viewModel.bookmarkMarketplaceScenario(scenario.id, !scenario.isBookmarked) },
                            onRateClick = { showRatingDialogFor = scenario.id },
                            onPlayClick = {
                                val modelScenario = Scenario(
                                    id = scenario.id,
                                    title = scenario.title,
                                    category = scenario.category,
                                    partnerName = scenario.partnerName,
                                    partnerAvatar = scenario.partnerAvatar,
                                    partnerPersona = scenario.systemPrompt,
                                    scenarioDescription = scenario.description,
                                    hiddenGoal = scenario.systemPrompt,
                                    difficulty = scenario.difficulty,
                                    initialMessage = scenario.initialMessage
                                )
                                onStartScenario(modelScenario)
                            }
                        )
                    }
                }
            }
        }
    }

    // Star Rating Dialog
    if (showRatingDialogFor != null) {
        val scenarioId = showRatingDialogFor!!
        AlertDialog(
            onDismissRequest = { showRatingDialogFor = null },
            title = { Text("RATE SCENARIO", style = TextSm.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Rating (1 to 5 stars)", style = TextXs.copy(color = SleekTextGray))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        (1..5).forEach { star ->
                            val isLit = star <= userRatingInput
                            IconButton(onClick = { userRatingInput = star.toFloat() }) {
                                Icon(
                                    imageVector = if (isLit) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "$star Stars",
                                    tint = if (isLit) SleekPrimary else SleekBorder,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rateScenario(scenarioId, userRatingInput)
                        showRatingDialogFor = null
                        Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Submit", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialogFor = null }) {
                    Text("Cancel", style = TextXs.copy(color = SleekTextGray))
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun MarketplaceScenarioCard(
    scenario: MarketplaceScenarioEntity,
    isFollowed: Boolean,
    onFollowToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onRateClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category, Creator and Follow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scenario.category.uppercase(),
                    style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "By ${scenario.creator}",
                        style = TextXs.copy(fontWeight = FontWeight.Medium, color = SleekTextDark),
                        modifier = Modifier.clickable { onFollowToggle() }
                    )
                    if (scenario.creator != "Official" && scenario.creator != "Community") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFollowed) "(Following)" else "(Follow)",
                            style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary),
                            modifier = Modifier.clickable { onFollowToggle() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = scenario.title,
                    style = TextSm.copy(fontWeight = FontWeight.Bold, color = SleekTextDark),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (scenario.isBookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = scenario.description,
                style = TextXs.copy(color = SleekTextGray),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Difficulty, Rating and Play
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Diff: ${scenario.difficulty.uppercase()}",
                        style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier.clickable { onRateClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Rating", tint = SleekPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${scenario.rating} (${scenario.ratingCount})",
                            style = TextXs.copy(color = SleekTextGray)
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = SleekPrimary),
                    border = BorderStroke(1.dp, SleekPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("PRACTICE", style = TextXs.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
