package com.example.attendanceapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.repository.AuthRepository
import com.example.attendanceapp.data.repository.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val errorMessage: String? = null,
    val session: Session? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val session = authRepository.login(username, password)
            if (session == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Invalid username or password")
            } else {
                _uiState.value = _uiState.value.copy(session = session, errorMessage = null)
            }
        }
    }

    fun logout() {
        _uiState.value = LoginUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
