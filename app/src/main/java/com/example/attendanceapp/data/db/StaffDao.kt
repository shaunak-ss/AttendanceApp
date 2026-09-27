package com.example.attendanceapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.attendanceapp.data.entity.EnrolledFaceEntity
import com.example.attendanceapp.data.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Insert
    suspend fun insertStaff(staff: StaffEntity): Long

    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun observeAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :staffId")
    suspend fun getStaffById(staffId: Long): StaffEntity?

    @Query("SELECT * FROM staff WHERE employeeId = :employeeId")
    suspend fun getStaffByEmployeeId(employeeId: String): StaffEntity?

    @Insert
    suspend fun insertEnrolledFace(face: EnrolledFaceEntity)

    @Query("DELETE FROM enrolled_face WHERE staffId = :staffId")
    suspend fun deleteEnrolledFaceForStaff(staffId: Long)

    @Query("SELECT * FROM enrolled_face WHERE staffId = :staffId LIMIT 1")
    suspend fun getEnrolledFaceForStaff(staffId: Long): EnrolledFaceEntity?

    @Query("SELECT * FROM enrolled_face WHERE staffId = :staffId LIMIT 1")
    fun observeEnrolledFaceForStaff(staffId: Long): Flow<EnrolledFaceEntity?>
}
