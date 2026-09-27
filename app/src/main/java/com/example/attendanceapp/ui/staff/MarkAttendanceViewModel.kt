package com.example.attendanceapp.ui.staff

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.repository.AttendanceOutcome
import com.example.attendanceapp.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MarkAttendanceStep { CAPTURING, PROCESSING, MATCHED, NOT_MATCHED }

data class MarkAttendanceUiState(
    val step: MarkAttendanceStep = MarkAttendanceStep.CAPTURING,
    val message: String? = null,
    val confidence: Float = 0f
)

class MarkAttendanceViewModel(
    private val staffId: Long,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkAttendanceUiState())
    val uiState: StateFlow<MarkAttendanceUiState> = _uiState.asStateFlow()

    fun onSelfieCaptured(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(step = MarkAttendanceStep.PROCESSING)
        viewModelScope.launch {
            when (val outcome = attendanceRepository.markAttendance(staffId, bitmap)) {
                is AttendanceOutcome.Matched -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.MATCHED,
                    confidence = outcome.confidence
                )
                is AttendanceOutcome.NotMatched -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.NOT_MATCHED,
                    message = "Face didn't match your enrolled photo.",
                    confidence = outcome.confidence
                )
                is AttendanceOutcome.NoEnrolledFace -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.NOT_MATCHED,
                    message = "No enrolled face found. Ask an admin to enrol you first."
                )
                is AttendanceOutcome.CaptureRejected -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.NOT_MATCHED,
                    message = describeRejection(outcome.reason)
                )
            }
        }
    }

    fun retry() {
        _uiState.value = MarkAttendanceUiState()
    }

    private fun describeRejection(reason: FaceCaptureResult): String = when (reason) {
        is FaceCaptureResult.NoFaceDetected -> "No face detected. Center your face and try again."
        is FaceCaptureResult.MultipleFacesDetected -> "More than one face detected."
        is FaceCaptureResult.EyesClosed -> "Eyes appeared closed. Try again."
        is FaceCaptureResult.HeadNotFrontal -> "Face the camera directly and try again."
        is FaceCaptureResult.LandmarksMissing -> "Couldn't read your face clearly. Try better lighting."
        is FaceCaptureResult.Success -> ""
    }
}
