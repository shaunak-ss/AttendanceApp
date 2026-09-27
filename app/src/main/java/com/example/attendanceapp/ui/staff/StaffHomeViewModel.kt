package com.example.attendanceapp.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import com.example.attendanceapp.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StaffHomeViewModel(
    staffId: Long,
    attendanceRepository: AttendanceRepository
) : ViewModel() {
    val attendance: StateFlow<List<AttendanceRecordEntity>> =
        attendanceRepository.observeAttendance(staffId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
