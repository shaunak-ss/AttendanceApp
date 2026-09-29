package com.example.attendanceapp.ui.staff

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MarkAttendanceScreen(
    viewModel: MarkAttendanceViewModel,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.step) {
        MarkAttendanceStep.CHECKING -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        MarkAttendanceStep.CAPTURING, MarkAttendanceStep.PROCESSING -> {
            com.example.attendanceapp.ui.common.CameraCaptureScreen(
                instructionText = "Center your face and capture a selfie to mark attendance.",
                isProcessing = uiState.step == MarkAttendanceStep.PROCESSING,
                onCaptured = viewModel::onSelfieCaptured,
                onCancel = onFinished,
                requireLocationPermission = true,
                onLocationPermissionAvailable = viewModel::ensureLocationAcquisitionStarted
            )
        }
        MarkAttendanceStep.MATCHED -> {
            ResultState(
                success = true,
                title = "Attendance marked",
                subtitle = "Match confidence: ${(uiState.confidence * 100).toInt()}%",
                onDone = onFinished,
                autoDismiss = true
            )
        }
        MarkAttendanceStep.NOT_MATCHED -> {
            ResultState(
                success = false,
                title = "No match",
                subtitle = uiState.message ?: "Please try again.",
                onDone = viewModel::retry,
                buttonLabel = "Try again",
                autoDismiss = false
            )
        }
        MarkAttendanceStep.ALREADY_MARKED -> {
            ResultState(
                success = true,
                title = "Already marked",
                subtitle = uiState.message ?: "You already marked attendance today.",
                onDone = onFinished,
                buttonLabel = "OK",
                autoDismiss = false
            )
        }
    }
}

@Composable
private fun ResultState(
    success: Boolean,
    title: String,
    subtitle: String,
    onDone: () -> Unit,
    autoDismiss: Boolean,
    buttonLabel: String = "Try again"
) {
    val scale = androidx.compose.runtime.remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(350))
        if (autoDismiss) {
            kotlinx.coroutines.delay(1400)
            onDone()
        }
    }

    val color = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(96.dp).scale(scale.value)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!autoDismiss) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))
            Button(onClick = onDone) {
                Text(buttonLabel)
            }
        }
    }
}
