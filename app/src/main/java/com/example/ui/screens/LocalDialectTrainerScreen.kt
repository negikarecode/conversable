package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun LocalDialectTrainerScreen(
    viewModel: ConversableViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var sourceText by remember { mutableStateOf("Hello, my friend. Let us meet tomorrow for coffee and have a talk.") }
    var selectedDialect by remember { mutableStateOf("Australian Slang (Strine)") }
    var showDialectMenu by remember { mutableStateOf(false) }

    var translationResult by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    val dialects = listOf(
        "Australian Slang (Strine)",
        "London Cockney Rhyming",
        "Scottish Highlands Dialect",
        "Southern US Drawl",
        "New York Brooklyn Accent",
        "Jamaican Patois"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LOCAL DIALECT TRAINER", style = TextSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)) },
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DIALECT & LOCAL IDIOMS COACH", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Learn local social phrasing, slang, and cultural idioms. Type standard sentences and translate them into regions to practice native conversations.",
                        style = TextXs.copy(color = SleekTextGray, lineHeight = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("ENTER STANDARD MESSAGE", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))

            OutlinedTextField(
                value = sourceText,
                onValueChange = { sourceText = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = SleekBorder
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("CHOOSE TARGET DIALECT / REGION", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialectMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = SleekSurface,
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedDialect, style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextDark))
                        Text("▼", style = TextXs.copy(fontSize = 10.sp, color = SleekTextGray))
                    }
                }
                DropdownMenu(
                    expanded = showDialectMenu,
                    onDismissRequest = { showDialectMenu = false },
                    modifier = Modifier.fillMaxWidth().background(SleekSurface)
                ) {
                    dialects.forEach { dialect ->
                        DropdownMenuItem(
                            text = { Text(dialect, style = TextXs.copy(color = SleekTextDark)) },
                            onClick = {
                                selectedDialect = dialect
                                showDialectMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (sourceText.isNotBlank() && !isTranslating) {
                        isTranslating = true
                        coroutineScope.launch {
                            val result = viewModel.translateToLocalDialect(sourceText, selectedDialect)
                            translationResult = result ?: "Failed to retrieve local translation."
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White),
                enabled = sourceText.isNotBlank() && !isTranslating
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("TRANSLATE TO LOCAL SLANG", style = TextXs.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (translationResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("TRANSLATION & EXPLANATIONS", style = TextXs.copy(fontWeight = FontWeight.Bold, color = SleekTextGray), modifier = Modifier.padding(bottom = 6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(translationResult, style = TextXs.copy(color = SleekTextDark, lineHeight = 20.sp))
                    }
                }
            }
        }
    }
}
