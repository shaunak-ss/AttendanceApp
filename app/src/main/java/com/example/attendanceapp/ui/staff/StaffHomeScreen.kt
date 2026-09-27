package com.example.attendanceapp.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    displayName: String,
    viewModel: StaffHomeViewModel,
    onMarkAttendance: () -> Unit
) {
    val attendance by viewModel.attendance.collectAsState()
    val dateFormat = remember { dateFormatter() }

    Scaffold(topBar = { TopAppBar(title = { Text("Hi, $displayName") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = onMarkAttendance, modifier = Modifier.fillMaxWidth()) {
                Text("Mark Attendance")
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
