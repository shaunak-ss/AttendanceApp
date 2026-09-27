package com.example.attendanceapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.entity.StaffEntity
import com.example.attendanceapp.data.repository.AuthRepository
import com.example.attendanceapp.data.repository.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val staffOptions: List<StaffEntity> = emptyList(),
    val errorMessage: String? = null,
    val session: Session? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    observeStaff: kotlinx.coroutines.flow.Flow<List<StaffEntity>>
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeStaff.collect { staff ->
                _uiState.value = _uiState.value.copy(staffOptions = staff)
            }
        }
    }

    fun loginAsAdmin(username: String, password: String) {
        val session = authRepository.loginAdmin(username, password)
        if (session == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid admin credentials")
        } else {
            _uiState.value = _uiState.value.copy(session = session, errorMessage = null)
        }
    }

    fun loginAsStaff(staffId: Long) {
        viewModelScope.launch {
            val session = authRepository.loginStaff(staffId)
            _uiState.value = _uiState.value.copy(session = session)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
