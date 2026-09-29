package com.example.attendanceapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.attendanceapp.data.entity.AttendanceRecordEntity
import com.example.attendanceapp.data.entity.EnrolledFaceEntity
import com.example.attendanceapp.data.entity.StaffEntity

@Database(
    entities = [StaffEntity::class, EnrolledFaceEntity::class, AttendanceRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_app.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                 .build().also { instance = it }
            }
    }
}
