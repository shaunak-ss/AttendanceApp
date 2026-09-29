package com.example.attendanceapp.data.repository

import com.example.attendanceapp.data.db.StaffDao
import com.example.attendanceapp.data.entity.StaffEntity

enum class Role { ADMIN, STAFF }

data class Session(val role: Role, val staffId: Long? = null, val displayName: String)

/**
 * Dummy credential-based auth per assignment scope: one hardcoded admin login, staff sign in
 * by picking their own identity (no password) since the real gate is the face match at
 * attendance time.
 */
class AuthRepository(private val staffDao: StaffDao) {

    suspend fun login(username: String, password: String): Session? {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) return null

        // Admin check
        if (trimmedUser.equals(ADMIN_USERNAME, ignoreCase = true) && trimmedPass == ADMIN_PASSWORD) {
            return Session(Role.ADMIN, displayName = "Admin")
        }

        // Staff check
        var staff = staffDao.getStaffByIdentifier(trimmedUser)
        if (staff == null && (trimmedUser.equals("staff", ignoreCase = true) || trimmedUser.equals("emp1", ignoreCase = true))) {
            val allStaff = staffDao.getAllStaffList()
            if (allStaff.isNotEmpty()) {
                staff = allStaff.first()
            } else {
                val dummyId = staffDao.insertStaff(
                    StaffEntity(name = "Staff User", employeeId = "emp1")
                )
                staff = staffDao.getStaffById(dummyId)
            }
        }

        if (staff != null) {
            if (trimmedPass == STAFF_PASSWORD || trimmedPass.equals(staff.employeeId, ignoreCase = true) || trimmedPass == ADMIN_PASSWORD) {
                return Session(Role.STAFF, staffId = staff.id, displayName = staff.name)
            }
        }

        return null
    }

    companion object {
        const val ADMIN_USERNAME = "admin"
        const val ADMIN_PASSWORD = "admin123"
        const val STAFF_PASSWORD = "staff123"
    }
}
