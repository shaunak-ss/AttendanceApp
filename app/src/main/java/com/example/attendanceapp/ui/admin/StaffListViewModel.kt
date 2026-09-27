package com.example.attendanceapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.entity.StaffEntity
import com.example.attendanceapp.data.repository.StaffRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StaffListViewModel(staffRepository: StaffRepository) : ViewModel() {
    val staff: StateFlow<List<StaffEntity>> = staffRepository.observeAllStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
