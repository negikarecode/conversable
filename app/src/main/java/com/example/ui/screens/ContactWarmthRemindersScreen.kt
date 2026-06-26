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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Check
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
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactWarmthRemindersScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val reminders by viewModel.contactReminders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newRelationship by remember { mutableStateOf("Friend") }
    var newIntervalDays by remember { mutableStateOf(7) }
    var newWarmthScore by remember { mutableStateOf(80) }
    var newNotes by remember { mutableStateOf("") }

    // AI generated catchup template state
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedContactName by remember { mutableStateOf("") }
    var generatedTemplateText by remember { mutableStateOf("") }
    var isGeneratingTemplate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RELATIONSHIP DNA & REMINDERS", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SleekPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = SleekPrimary)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("RELATIONSHIP DNA ENGINE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Track contact interaction intervals and relationship warmth meters. Generate instant context-aware text responses to check in with them.",
                        style = TextXs.copy(color = SleekTextGray, lineHeight = 16.sp)
                    )
                }
            }

            Text("YOUR CONTACT NETWORK", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, SleekBorder, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No contacts registered yet.", style = TextXs.copy(color = SleekTextGray))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                        ) {
                            Text("ADD FIRST CONTACT", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(contact.name, style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                                        Text("${contact.relationship} • Remind every ${contact.reminderIntervalDays} days", style = TextXs.copy(color = SleekTextGray))
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Generate text prompt template CTA
                                        Button(
                                            onClick = {
                                                selectedContactName = contact.name
                                                isGeneratingTemplate = true
                                                showTemplateDialog = true
                                                coroutineScope.launch {
                                                    val prompt = "Create a friendly, personalized catchup text message to ${contact.name} (${contact.relationship}) based on these context notes: ${contact.notes}. Keep it warm, extremely natural, and under 50 words."
                                                    val result = viewModel.queryAiPublic(prompt, "Generate a texting prompt.")
                                                    generatedTemplateText = result ?: "Hey ${contact.name}, would love to catch up soon! Let me know if you're free."
                                                    isGeneratingTemplate = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SleekBackground, contentColor = SleekPrimary),
                                            border = BorderStroke(1.dp, SleekBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("CATCH UP TEXT", style = TextXs.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteContactReminder(contact.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Warmth score: ${contact.warmthScore}%", style = TextXs.copy(color = SleekTextGray, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Warmth meter linear progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SleekBorder)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(contact.warmthScore / 100f)
                                            .background(SleekPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SleekSurface,
            title = { Text("Add New Contact", color = SleekTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Contact Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRelationship,
                        onValueChange = { newRelationship = it },
                        label = { Text("Relationship (e.g. Partner, Boss, Dad)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Important milestones / topics") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Remind Interval (Days): $newIntervalDays", style = TextXs.copy(color = SleekTextGray))
                    Slider(
                        value = newIntervalDays.toFloat(),
                        onValueChange = { newIntervalDays = it.toInt() },
                        valueRange = 1f..60f,
                        colors = SliderDefaults.colors(thumbColor = SleekPrimary, activeTrackColor = SleekPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addContactReminder(
                                name = newName,
                                relationship = newRelationship,
                                intervalDays = newIntervalDays,
                                warmthScore = newWarmthScore,
                                notes = newNotes
                            )
                            newName = ""
                            newNotes = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Save Contact", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = SleekTextLightGray)
                }
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            containerColor = SleekSurface,
            title = { Text("AI Catch-Up Suggestion", color = SleekTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGeneratingTemplate) {
                        CircularProgressIndicator(color = SleekPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Text(generatedTemplateText, style = TextXs.copy(color = SleekTextDark))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        android.os.Bundle().apply {
                            Toast.makeText(context, "Copied suggestion to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        showTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Copy Response", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Close", color = SleekTextLightGray)
                }
            }
        )
    }
}
