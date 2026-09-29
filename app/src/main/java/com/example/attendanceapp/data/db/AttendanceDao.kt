package com.example.attendanceapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertAttendance(record: AttendanceRecordEntity): Long

    @Query("SELECT * FROM attendance_record WHERE staffId = :staffId ORDER BY timestamp DESC")
    fun observeAttendanceForStaff(staffId: Long): Flow<List<AttendanceRecordEntity>>

    @Query(
        "SELECT * FROM attendance_record WHERE staffId = :staffId " +
            "AND timestamp BETWEEN :startOfDay AND :endOfDay LIMIT 1"
    )
    suspend fun getAttendanceForStaffOnDay(staffId: Long, startOfDay: Long, endOfDay: Long): AttendanceRecordEntity?

    @Query("DELETE FROM attendance_record WHERE staffId = :staffId")
    suspend fun deleteAttendanceForStaff(staffId: Long)
}
