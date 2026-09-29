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
        val trimmedName = name.trim()
        val trimmedEmpId = employeeId.trim()
        if (trimmedName.isBlank() || trimmedEmpId.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name and employee ID are required")
            return
        }
        viewModelScope.launch {
            try {
                val existing = staffRepository.getStaffByEmployeeId(trimmedEmpId)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "An employee with Employee ID '$trimmedEmpId' already exists"
                    )
                    return@launch
                }
                val id = staffRepository.addStaff(trimmedName, trimmedEmpId)
                _uiState.value = _uiState.value.copy(createdStaffId = id, errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add staff"
                )
            }
        }
    }
}
