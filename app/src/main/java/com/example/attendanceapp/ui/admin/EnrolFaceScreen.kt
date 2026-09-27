package com.example.attendanceapp.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EnrolFaceScreen(
    viewModel: EnrolFaceViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.step) {
        EnrolmentStep.CAPTURING, EnrolmentStep.PROCESSING -> {
            com.example.attendanceapp.ui.common.CameraCaptureScreen(
                instructionText = uiState.errorMessage
                    ?: "Shot ${uiState.shotsCaptured + 1} of ${uiState.shotsRequired}. Center your face and hold still.",
                isProcessing = uiState.step == EnrolmentStep.PROCESSING,
                onCaptured = viewModel::onShotCaptured,
                onCancel = onCancel
            )
        }
        EnrolmentStep.SUCCESS -> {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                onDone()
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(72.dp).padding(bottom = 16.dp)
                )
                Text("Face enrolled successfully", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
