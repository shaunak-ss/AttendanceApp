package com.example.attendanceapp

import android.content.Context
import com.example.attendanceapp.data.db.AppDatabase
import com.example.attendanceapp.data.face.FaceLandmarkExtractor
import com.example.attendanceapp.data.location.LocationProvider
import com.example.attendanceapp.data.repository.AttendanceRepository
import com.example.attendanceapp.data.repository.AuthRepository
import com.example.attendanceapp.data.repository.StaffRepository
import com.example.attendanceapp.data.storage.ImageStorage

/** Simple manual dependency container — no DI framework needed for this app's scope. */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val imageStorage = ImageStorage(context)
    private val faceExtractor = FaceLandmarkExtractor()
    private val locationProvider = LocationProvider(context)

    val authRepository = AuthRepository(database.staffDao())
    val staffRepository = StaffRepository(database.staffDao(), imageStorage, faceExtractor)
    val attendanceRepository = AttendanceRepository(
        database.staffDao(),
        database.attendanceDao(),
        imageStorage,
        faceExtractor,
        locationProvider
    )
    val imageStorageAccess = imageStorage
}
