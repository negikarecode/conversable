package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PersonaMemoryEntity
import com.example.data.model.ScenarioCatalog
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMemoryScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val isMemoryEnabled by viewModel.isMemoryEnabled.collectAsState()
    val customClones by viewModel.allPersonas.collectAsState()
    
    // Combine built-in scenario personas and custom clones for selection
    val personas = remember(customClones) {
        val builtIn = ScenarioCatalog.scenarios.map { it.partnerName }.distinct()
        val custom = customClones.map { it.name }
        (builtIn + custom).distinct()
    }
    
    var selectedPersona by remember { mutableStateOf(personas.firstOrNull() ?: "") }
    
    // Load memories for the selected persona
    val memoriesFlow = remember(selectedPersona) {
        viewModel.getMemoriesForPersona(selectedPersona)
    }
    val memories by memoriesFlow.collectAsState(initial = emptyList())
    
    // Input dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var newMemoryText by remember { mutableStateOf("") }
    
    var editingMemory by remember { mutableStateOf<PersonaMemoryEntity?>(null) }
    var editMemoryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI PERSONA MEMORIES", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
            // Master Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable AI Memory", style = TextSm.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "When enabled, training partners will remember previous facts, preferences, and conversations.",
                            style = TextXs.copy(color = SleekTextGray)
                        )
                    }
                    Switch(
                        checked = isMemoryEnabled,
                        onCheckedChange = { viewModel.setMemoryEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SleekPrimary)
                    )
                }
            }

            // Persona Selector Row
            Text("SELECT PERSONA TO MANAGE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(personas) { persona ->
                    val isSelected = persona == selectedPersona
                    OutlinedCard(
                        onClick = { selectedPersona = persona },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SleekPrimary else SleekSurface
                        ),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = persona,
                            style = TextXs.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SleekTextDark
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Memory Control Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MEMORIES SAVED (${memories.size})", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                
                Row {
                    // Export
                    IconButton(onClick = {
                        if (memories.isNotEmpty()) {
                            val textToCopy = memories.joinToString("\n") { "• ${it.memoryText}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Persona Memories", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Memories exported to clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No memories to export.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Export memories", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                    }

                    // Add Manual Memory
                    IconButton(onClick = {
                        newMemoryText = ""
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add memory", tint = SleekPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Memories List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (memories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No memories recorded yet.\nThey will accumulate automatically as you finish conversations, or you can add them manually above.",
                                    style = TextXs.copy(color = SleekTextGray),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(memories) { memory ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = memory.memoryText,
                                        style = TextSm.copy(color = SleekTextDark)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Recorded ${
                                            java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date(memory.timestamp))
                                        }",
                                        style = TextXs.copy(color = SleekTextGray)
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        editingMemory = memory
                                        editMemoryText = memory.memoryText
                                    }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = SleekTextGray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteMemory(memory.id)
                                    }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = SleekTextGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (memories.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.clearMemoriesForPersona(selectedPersona)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CLEAR ALL MEMORIES FOR THIS PERSONA", style = TextXs.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("ADD FACT TO REMEMBER", style = TextSm.copy(fontWeight = FontWeight.Bold)) },
            text = {
                OutlinedTextField(
                    value = newMemoryText,
                    onValueChange = { newMemoryText = it },
                    placeholder = { Text("e.g. Enjoys talking about books; dislikes micromanagement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMemoryText.isNotBlank()) {
                            viewModel.addMemory(selectedPersona, newMemoryText)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", style = TextXs.copy(color = SleekTextGray))
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Edit Memory Dialog
    if (editingMemory != null) {
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("EDIT REMEMBERED FACT", style = TextSm.copy(fontWeight = FontWeight.Bold)) },
            text = {
                OutlinedTextField(
                    value = editMemoryText,
                    onValueChange = { editMemoryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editMemoryText.isNotBlank()) {
                            viewModel.updateMemory(editingMemory!!.id, editMemoryText)
                            editingMemory = null
                        }
                    }
                ) {
                    Text("Save", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) {
                    Text("Cancel", style = TextXs.copy(color = SleekTextGray))
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
