package com.example.attendanceapp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff",
    indices = [Index(value = ["employeeId"], unique = true)]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val employeeId: String,
    val createdAt: Long = System.currentTimeMillis()
)
