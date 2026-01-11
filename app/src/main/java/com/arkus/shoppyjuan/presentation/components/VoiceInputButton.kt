package com.arkus.shoppyjuan.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.data.speech.VoiceInputState

@Composable
fun VoiceInputButton(
    onVoiceResult: (String) -> Unit,
    onStartListening: () -> Unit,
    voiceInputState: VoiceInputState,
    modifier: Modifier = Modifier
) {
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scale by animateFloatAsState(
        targetValue = if (voiceInputState is VoiceInputState.Listening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_scale"
    )

    LaunchedEffect(voiceInputState) {
        when (voiceInputState) {
            is VoiceInputState.Result -> {
                onVoiceResult(voiceInputState.text)
            }
            is VoiceInputState.Error -> {
                errorMessage = voiceInputState.message
                showError = true
            }
            else -> {}
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                if (voiceInputState !is VoiceInputState.Listening) {
                    onStartListening()
                }
            },
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Entrada por voz",
                tint = when (voiceInputState) {
                    is VoiceInputState.Listening -> MaterialTheme.colorScheme.primary
                    is VoiceInputState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (voiceInputState is VoiceInputState.Listening) {
            Text(
                text = "Escuchando...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error de voz") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}
