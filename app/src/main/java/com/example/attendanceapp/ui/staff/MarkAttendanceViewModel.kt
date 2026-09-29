package com.example.attendanceapp.ui.staff

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.location.LocationAcquisition
import com.example.attendanceapp.data.repository.AttendanceOutcome
import com.example.attendanceapp.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MarkAttendanceStep { CHECKING, CAPTURING, PROCESSING, MATCHED, NOT_MATCHED, ALREADY_MARKED }

data class MarkAttendanceUiState(
    val step: MarkAttendanceStep = MarkAttendanceStep.CAPTURING,
    val message: String? = null,
    val confidence: Float = 0f
)

class MarkAttendanceViewModel(
    private val staffId: Long,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkAttendanceUiState(step = MarkAttendanceStep.CHECKING))
    val uiState: StateFlow<MarkAttendanceUiState> = _uiState.asStateFlow()
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    // Started once location permission is confirmed granted (see ensureLocationAcquisitionStarted)
    // so the wait overlaps with face matching instead of following it. Must not start any earlier
    // than that: at screen-open time permission may not be granted yet (the system dialog hasn't
    // resolved), and a job started then would permanently resolve to "no location" before the
    // user has even had a chance to grant it.
    private var locationAcquisition: LocationAcquisition? = null

    init {
        viewModelScope.launch {
            val existing = attendanceRepository.getTodayAttendance(staffId)
            _uiState.value = if (existing != null) {
                MarkAttendanceUiState(
                    step = MarkAttendanceStep.ALREADY_MARKED,
                    message = "You already marked attendance today at " +
                        timeFormatter.format(Date(existing.timestamp)) + "."
                )
            } else {
                MarkAttendanceUiState(step = MarkAttendanceStep.CAPTURING)
            }
        }
    }

    /** Idempotent: call whenever location permission is confirmed available (may be called more than once). */
    fun ensureLocationAcquisitionStarted() {
        if (locationAcquisition == null) {
            locationAcquisition = attendanceRepository.beginLocationAcquisition(viewModelScope)
        }
    }

    fun onSelfieCaptured(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(step = MarkAttendanceStep.PROCESSING)
        viewModelScope.launch {
            when (val outcome = attendanceRepository.markAttendance(staffId, bitmap, locationAcquisition)) {
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
                is AttendanceOutcome.AlreadyMarkedToday -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.ALREADY_MARKED,
                    message = "You already marked attendance today at " +
                        timeFormatter.format(Date(outcome.record.timestamp)) + "."
                )
                is AttendanceOutcome.LocationPermissionRequired -> _uiState.value = MarkAttendanceUiState(
                    step = MarkAttendanceStep.NOT_MATCHED,
                    message = "Location permission is required to mark attendance. Please grant it and try again."
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
