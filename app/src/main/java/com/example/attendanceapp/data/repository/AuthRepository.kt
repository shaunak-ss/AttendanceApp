package com.example.attendanceapp.data.repository

import com.example.attendanceapp.data.db.StaffDao

enum class Role { ADMIN, STAFF }

data class Session(val role: Role, val staffId: Long? = null, val displayName: String)

/**
 * Dummy credential-based auth per assignment scope: one hardcoded admin login, staff sign in
 * by picking their own identity (no password) since the real gate is the face match at
 * attendance time.
 */
class AuthRepository(private val staffDao: StaffDao) {

    fun loginAdmin(username: String, password: String): Session? =
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            Session(Role.ADMIN, displayName = "Admin")
        } else null

    suspend fun loginStaff(staffId: Long): Session? {
        val staff = staffDao.getStaffById(staffId) ?: return null
        return Session(Role.STAFF, staff.id, staff.name)
    }

    companion object {
        const val ADMIN_USERNAME = "admin"
        const val ADMIN_PASSWORD = "admin123"
    }
}
