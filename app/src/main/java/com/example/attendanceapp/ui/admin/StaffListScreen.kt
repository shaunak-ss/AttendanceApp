package com.example.attendanceapp.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.data.entity.StaffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    viewModel: StaffListViewModel,
    onAddStaff: () -> Unit,
    onOpenStaff: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val staff by viewModel.staff.collectAsState()
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
                title = {
                    Column {
                        Text(
                            text = "Hi Admin",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Staff Management",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStaff) {
                Icon(Icons.Filled.Add, contentDescription = "Add staff")
            }
        }
    ) { padding ->
        if (staff.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No staff yet. Tap + to add your first staff member.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(staff, key = { it.id }) { member ->
                    StaffRow(member, onClick = { onOpenStaff(member.id) })
                }
            }
        }
    }
}

@Composable
private fun StaffRow(staff: StaffEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(staff.name) },
            supportingContent = { Text("ID: ${staff.employeeId}") },
            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) }
        )
    }
}
