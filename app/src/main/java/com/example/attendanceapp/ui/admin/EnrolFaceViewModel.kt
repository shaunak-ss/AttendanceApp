package com.example.attendanceapp.ui.admin

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.repository.EnrolmentResult
import com.example.attendanceapp.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SHOTS_REQUIRED = 2

enum class EnrolmentStep { CAPTURING, PROCESSING, SUCCESS }

data class EnrolFaceUiState(
    val step: EnrolmentStep = EnrolmentStep.CAPTURING,
    val shotsCaptured: Int = 0,
    val shotsRequired: Int = SHOTS_REQUIRED,
    val errorMessage: String? = null
)

class EnrolFaceViewModel(
    private val staffId: Long,
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnrolFaceUiState())
    val uiState: StateFlow<EnrolFaceUiState> = _uiState.asStateFlow()

    private val captures = mutableListOf<Bitmap>()

    fun onShotCaptured(bitmap: Bitmap) {
        captures.add(bitmap)
        if (captures.size < SHOTS_REQUIRED) {
            _uiState.value = _uiState.value.copy(shotsCaptured = captures.size, errorMessage = null)
            return
        }

        _uiState.value = _uiState.value.copy(step = EnrolmentStep.PROCESSING)
        viewModelScope.launch {
            when (val result = staffRepository.enrolFace(staffId, captures.toList())) {
                is EnrolmentResult.Success -> {
                    _uiState.value = _uiState.value.copy(step = EnrolmentStep.SUCCESS)
                }
                is EnrolmentResult.Rejected -> {
                    captures.clear()
                    _uiState.value = EnrolFaceUiState(
                        step = EnrolmentStep.CAPTURING,
                        errorMessage = describeRejection(result.reason)
                    )
                }
            }
        }
    }

    private fun describeRejection(reason: FaceCaptureResult): String = when (reason) {
        is FaceCaptureResult.NoFaceDetected -> "No face detected. Center your face in the oval and try again."
        is FaceCaptureResult.MultipleFacesDetected -> "More than one face detected. Make sure only you are in frame."
        is FaceCaptureResult.EyesClosed -> "Eyes appeared closed. Keep your eyes open and try again."
        is FaceCaptureResult.HeadNotFrontal -> "Face the camera directly and try again."
        is FaceCaptureResult.LandmarksMissing -> "Couldn't read facial landmarks clearly. Try better lighting."
        is FaceCaptureResult.Success -> "" // Unreachable here.
    }
}
