package com.example.attendanceapp.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    viewModel: StaffProfileViewModel,
    onEnrolFace: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val staff = uiState.staff
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Staff Member") },
            text = {
                Text("Are you sure you want to delete ${staff?.name ?: "this employee"}? This action will also delete their enrolled face data and attendance history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteStaff(onDeleted = onBack)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(staff?.name ?: "Staff") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Staff",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (staff != null) {
                Text("Employee ID: ${staff.employeeId}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))

            val isEnrolled = uiState.enrolledFace != null
            AssistChip(
                onClick = {},
                label = { Text(if (isEnrolled) "Face enrolled" else "Not enrolled") },
                leadingIcon = {
                    Icon(
                        if (isEnrolled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(12.dp))

            Button(onClick = onEnrolFace, modifier = Modifier.fillMaxWidth()) {
                Text(if (isEnrolled) "Re-enrol face" else "Enrol face")
            }

            Spacer(Modifier.height(20.dp))
            Text("Attendance history", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (uiState.attendance.isEmpty()) {
                Text(
                    "No attendance recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(uiState.attendance, key = { it.id }) { record ->
                        AttendanceRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(record: AttendanceRecordEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }
    Card {
        ListItem(
            headlineContent = { Text(dateFormat.format(Date(record.timestamp))) },
            supportingContent = {
                Text(record.address ?: "Location unavailable")
            }
        )
    }
}
