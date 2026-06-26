package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ConversableViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRewriteStudioScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var inputText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Professional") }
    var rewrittenResult by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val styles = listOf(
        "Professional", "Friendly", "Confident", "Assertive", 
        "Empathetic", "Funny", "Romantic", "Persuasive", 
        "Shorter", "Longer", "Natural"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI REWRITE STUDIO", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("INPUT TEXT", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type or paste a message you want to rewrite...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder
                        )
                    )
                }
            }

            Text("SELECT REWRITE STYLE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(vertical = 8.dp))

            // Style Selection Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Chunk styles into rows of 3
                val rows = styles.chunked(3)
                rows.forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowStyles.forEach { style ->
                            val isSelected = style == selectedStyle
                            OutlinedCard(
                                onClick = { selectedStyle = style },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SleekPrimary else SleekSurface
                                ),
                                border = BorderStroke(1.dp, SleekBorder),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
                                ) {
                                    Text(
                                        style,
                                        style = TextXs.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else SleekTextDark
                                        )
                                    )
                                }
                            }
                        }
                        // Fill empty slots if row has less than 3 elements
                        if (rowStyles.size < 3) {
                            repeat(3 - rowStyles.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        isGenerating = true
                        coroutineScope.launch {
                            val result = viewModel.generateRewritePublic(
                                originalText = inputText,
                                style = selectedStyle.lowercase(),
                                scenarioTitle = "Rewrite Studio"
                            )
                            rewrittenResult = result ?: "Failed to generate rewrite."
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = inputText.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("GENERATE REWRITE", style = TextSm.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (rewrittenResult.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("REWRITTEN TEXT", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Rewritten Text", rewrittenResult)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Copy", tint = SleekPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(rewrittenResult, style = TextSm.copy(color = SleekTextDark))
                    }
                }
            }
        }
    }
}
