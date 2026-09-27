package com.example.attendanceapp.data.repository

import android.graphics.Bitmap
import com.example.attendanceapp.data.db.AttendanceDao
import com.example.attendanceapp.data.db.StaffDao
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.face.FaceLandmarkExtractor
import com.example.attendanceapp.data.face.FaceMatcher
import com.example.attendanceapp.data.location.LocationProvider
import com.example.attendanceapp.data.storage.ImageStorage
import kotlinx.coroutines.flow.Flow

sealed class AttendanceOutcome {
    data class Matched(val confidence: Float) : AttendanceOutcome()
    data class NotMatched(val confidence: Float) : AttendanceOutcome()
    data object NoEnrolledFace : AttendanceOutcome()
    data class CaptureRejected(val reason: FaceCaptureResult) : AttendanceOutcome()
}

class AttendanceRepository(
    private val staffDao: StaffDao,
    private val attendanceDao: AttendanceDao,
    private val imageStorage: ImageStorage,
    private val faceExtractor: FaceLandmarkExtractor,
    private val locationProvider: LocationProvider
) {
    fun observeAttendance(staffId: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.observeAttendanceForStaff(staffId)

    suspend fun markAttendance(staffId: Long, liveSelfie: Bitmap): AttendanceOutcome {
        val enrolled = staffDao.getEnrolledFaceForStaff(staffId)
            ?: return AttendanceOutcome.NoEnrolledFace

        val captureResult = faceExtractor.extract(liveSelfie)
        val liveVector = when (captureResult) {
            is FaceCaptureResult.Success -> captureResult.vector
            else -> return AttendanceOutcome.CaptureRejected(captureResult)
        }

        val enrolledVector = FaceMatcher.stringToVector(enrolled.landmarkVector)
        val (isMatch, distance) = FaceMatcher.match(enrolledVector, liveVector)
        // Distance closer to 0 is a stronger match; report a bounded 0..1 confidence for the UI.
        val confidence = (1f - (distance / FaceMatcher.MATCH_THRESHOLD)).coerceIn(0f, 1f)

        if (!isMatch) return AttendanceOutcome.NotMatched(confidence)

        val location = locationProvider.getCurrentLocation()
        val imagePath = imageStorage.saveAttendanceImage(liveSelfie)
        attendanceDao.insertAttendance(
            AttendanceRecordEntity(
                staffId = staffId,
                timestamp = System.currentTimeMillis(),
                selfieImagePath = imagePath,
                lat = location.lat,
                lng = location.lng,
                address = location.address,
                matchConfidence = confidence
            )
        )
        return AttendanceOutcome.Matched(confidence)
    }
}
