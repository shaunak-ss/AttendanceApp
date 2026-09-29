package com.example.attendanceapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import com.example.attendanceapp.data.entity.EnrolledFaceEntity
import com.example.attendanceapp.data.entity.StaffEntity
import com.example.attendanceapp.data.repository.AttendanceRepository
import com.example.attendanceapp.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class StaffProfileUiState(
    val staff: StaffEntity? = null,
    val enrolledFace: EnrolledFaceEntity? = null,
    val attendance: List<AttendanceRecordEntity> = emptyList()
)

class StaffProfileViewModel(
    private val staffId: Long,
    private val staffRepository: StaffRepository,
    attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffProfileUiState())
    val uiState: StateFlow<StaffProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val staff = staffRepository.getStaff(staffId)
            _uiState.value = _uiState.value.copy(staff = staff)
        }
        viewModelScope.launch {
            staffRepository.observeEnrolledFace(staffId)
                .combine(attendanceRepository.observeAttendance(staffId)) { face, attendance ->
                    _uiState.value.copy(enrolledFace = face, attendance = attendance)
                }
                .collect { _uiState.value = it }
        }
    }

    fun deleteStaff(onDeleted: () -> Unit) {
        viewModelScope.launch {
            staffRepository.deleteStaff(staffId)
            onDeleted()
        }
    }
}
