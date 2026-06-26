package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SleekWarning
import com.example.viewmodel.ConversableViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close

@Composable
fun AiErrorBanner(
    viewModel: ConversableViewModel,
    modifier: Modifier = Modifier
) {
    val errorText by viewModel.aiError.collectAsState()
    if (errorText != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekWarning.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, SleekWarning.copy(alpha = 0.3f)),
            modifier = modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = SleekWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorText ?: "",
                        color = SleekWarning,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
                IconButton(
                    onClick = { viewModel.dismissAiError() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = SleekWarning,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
