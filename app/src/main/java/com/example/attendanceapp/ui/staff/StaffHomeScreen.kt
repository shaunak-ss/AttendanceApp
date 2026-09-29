package com.example.attendanceapp.ui.staff

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    displayName: String,
    viewModel: StaffHomeViewModel,
    onMarkAttendance: () -> Unit,
    onLogout: () -> Unit
) {
    val attendance by viewModel.attendance.collectAsState()
    val dateFormat = remember { dateFormatter() }
    val alreadyMarkedToday = attendance.firstOrNull()?.let { isSameDay(it.timestamp, System.currentTimeMillis()) } == true
    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showLogoutDialog = true
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hi, $displayName") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = onMarkAttendance,
                enabled = !alreadyMarkedToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (alreadyMarkedToday) "Attendance already marked today" else "Mark Attendance")
            }
            Spacer(Modifier.height(20.dp))
            Text("Your attendance history", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (attendance.isEmpty()) {
                Text(
                    "No attendance recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(attendance, key = { it.id }) { record ->
                        AttendanceHistoryRow(record, dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryRow(record: AttendanceRecordEntity, dateFormat: SimpleDateFormat) {
    Card {
        ListItem(
            headlineContent = { Text(dateFormat.format(Date(record.timestamp))) },
            supportingContent = { Text(record.address ?: "Location unavailable") }
        )
    }
}

private fun dateFormatter() = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

private fun isSameDay(timeA: Long, timeB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = timeA }
    val calB = Calendar.getInstance().apply { timeInMillis = timeB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
