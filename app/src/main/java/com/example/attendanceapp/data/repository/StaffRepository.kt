package com.example.attendanceapp.data.repository

import android.graphics.Bitmap
import com.example.attendanceapp.data.db.StaffDao
import com.example.attendanceapp.data.entity.EnrolledFaceEntity
import com.example.attendanceapp.data.entity.StaffEntity
import com.example.attendanceapp.data.face.FaceCaptureResult
import com.example.attendanceapp.data.face.FaceLandmarkExtractor
import com.example.attendanceapp.data.face.FaceMatcher
import com.example.attendanceapp.data.storage.ImageStorage
import kotlinx.coroutines.flow.Flow

sealed class EnrolmentResult {
    data object Success : EnrolmentResult()
    data class Rejected(val reason: FaceCaptureResult) : EnrolmentResult()
}

class StaffRepository(
    private val staffDao: StaffDao,
    private val imageStorage: ImageStorage,
    private val faceExtractor: FaceLandmarkExtractor
) {
    fun observeAllStaff(): Flow<List<StaffEntity>> = staffDao.observeAllStaff()

    fun observeEnrolledFace(staffId: Long): Flow<EnrolledFaceEntity?> =
        staffDao.observeEnrolledFaceForStaff(staffId)

    suspend fun getStaff(staffId: Long): StaffEntity? = staffDao.getStaffById(staffId)

    suspend fun addStaff(name: String, employeeId: String): Long =
        staffDao.insertStaff(StaffEntity(name = name, employeeId = employeeId))

    /** Runs ML Kit on each captured selfie, averages the resulting vectors, and stores them. */
    suspend fun enrolFace(staffId: Long, captures: List<Bitmap>): EnrolmentResult {
        val vectors = mutableListOf<FloatArray>()
        for (bitmap in captures) {
            when (val result = faceExtractor.extract(bitmap)) {
                is FaceCaptureResult.Success -> vectors.add(result.vector)
                else -> return EnrolmentResult.Rejected(result)
            }
        }

        val averaged = FaceMatcher.averageVectors(vectors)
        val imagePath = imageStorage.saveFaceEnrolmentImage(captures.first())

        staffDao.deleteEnrolledFaceForStaff(staffId)
        staffDao.insertEnrolledFace(
            EnrolledFaceEntity(
                staffId = staffId,
                imagePath = imagePath,
                landmarkVector = FaceMatcher.vectorToString(averaged)
            )
        )
        return EnrolmentResult.Success
    }
}
