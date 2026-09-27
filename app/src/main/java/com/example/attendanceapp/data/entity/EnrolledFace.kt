package com.example.attendanceapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "enrolled_face",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("staffId", unique = true)]
)
data class EnrolledFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val imagePath: String,
    /** Comma-separated normalized landmark-distance feature vector. */
    val landmarkVector: String,
    val enrolledAt: Long = System.currentTimeMillis()
)
