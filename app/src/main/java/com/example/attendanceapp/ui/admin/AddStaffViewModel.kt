package com.example.attendanceapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddStaffUiState(
    val errorMessage: String? = null,
    val createdStaffId: Long? = null
)

class AddStaffViewModel(private val staffRepository: StaffRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddStaffUiState())
    val uiState: StateFlow<AddStaffUiState> = _uiState.asStateFlow()

    fun addStaff(name: String, employeeId: String) {
        if (name.isBlank() || employeeId.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name and employee ID are required")
            return
        }
        viewModelScope.launch {
            val id = staffRepository.addStaff(name.trim(), employeeId.trim())
            _uiState.value = _uiState.value.copy(createdStaffId = id, errorMessage = null)
        }
    }
}
