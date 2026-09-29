package com.example.attendanceapp.data.repository

import android.graphics.Bitmap
import com.example.attendanceapp.data.db.AttendanceDao
import com.example.attendanceapp.data.db.StaffDao
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.face.FaceEmbeddingExtractor
import com.example.attendanceapp.data.face.FaceMatcher
import com.example.attendanceapp.data.location.CapturedLocation
import com.example.attendanceapp.data.location.LocationAcquisition
import com.example.attendanceapp.data.location.LocationProvider
import com.example.attendanceapp.data.storage.ImageStorage
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

sealed class AttendanceOutcome {
    data class Matched(val confidence: Float) : AttendanceOutcome()
    data class NotMatched(val confidence: Float) : AttendanceOutcome()
    data object NoEnrolledFace : AttendanceOutcome()
    data class CaptureRejected(val reason: FaceCaptureResult) : AttendanceOutcome()
    data class AlreadyMarkedToday(val record: AttendanceRecordEntity) : AttendanceOutcome()
    /** Location permission isn't granted at the moment of marking; attendance is not saved. */
    data object LocationPermissionRequired : AttendanceOutcome()
}

class AttendanceRepository(
    private val staffDao: StaffDao,
    private val attendanceDao: AttendanceDao,
    private val imageStorage: ImageStorage,
    private val faceExtractor: FaceEmbeddingExtractor,
    private val locationProvider: LocationProvider
) {
    fun observeAttendance(staffId: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.observeAttendanceForStaff(staffId)

    /**
     * Starts acquiring a location fix in the background right away, so the wait overlaps with
     * face matching instead of happening after it. Pass the returned handle into [markAttendance].
     */
    fun beginLocationAcquisition(scope: CoroutineScope): LocationAcquisition =
        locationProvider.beginAcquisition(scope)

    /** Returns today's attendance record for this staff member, if one was already marked. */
    suspend fun getTodayAttendance(staffId: Long): AttendanceRecordEntity? {
        val (startOfDay, endOfDay) = todayRange()
        return attendanceDao.getAttendanceForStaffOnDay(staffId, startOfDay, endOfDay)
    }

    suspend fun markAttendance(
        staffId: Long,
        liveSelfie: Bitmap,
        locationAcquisition: LocationAcquisition?
    ): AttendanceOutcome {
        getTodayAttendance(staffId)?.let {
            return AttendanceOutcome.AlreadyMarkedToday(it)
        }

        // Hard requirement: never save attendance without location permission, regardless of
        // what the capture screen's own permission gate showed at capture time.
        if (!locationProvider.hasLocationPermission()) {
            return AttendanceOutcome.LocationPermissionRequired
        }

        val enrolled = staffDao.getEnrolledFaceForStaff(staffId)
            ?: return AttendanceOutcome.NoEnrolledFace

        val captureResult = faceExtractor.extract(liveSelfie)
        val liveVector = when (captureResult) {
            is FaceCaptureResult.Success -> captureResult.vector
            else -> return AttendanceOutcome.CaptureRejected(captureResult)
        }

        val enrolledVector = FaceMatcher.stringToVector(enrolled.landmarkVector)
        val (isMatch, similarity) = FaceMatcher.match(enrolledVector, liveVector)
        // Similarity closer to 1 is a stronger match; report a bounded 0..1 confidence for the UI,
        // scaled so the match threshold itself sits at 0% confidence.
        val confidence = ((similarity - FaceMatcher.MATCH_THRESHOLD) / (1f - FaceMatcher.MATCH_THRESHOLD))
            .coerceIn(0f, 1f)

        if (!isMatch) return AttendanceOutcome.NotMatched(confidence)

        val location = locationAcquisition?.awaitBest()
            ?.let { locationProvider.toCapturedLocation(it) }
            ?: CapturedLocation(null, null, null)
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

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis - 1
        return startOfDay to endOfDay
    }
}
