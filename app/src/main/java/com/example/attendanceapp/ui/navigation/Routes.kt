package com.example.attendanceapp.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val STAFF_LIST = "staff_list"
    const val ADD_STAFF = "add_staff"
    const val STAFF_PROFILE = "staff_profile/{staffId}"
    const val ENROL_FACE = "enrol_face/{staffId}"
    const val STAFF_HOME = "staff_home/{staffId}/{displayName}"
    const val MARK_ATTENDANCE = "mark_attendance/{staffId}"

    fun staffProfile(staffId: Long) = "staff_profile/$staffId"
    fun enrolFace(staffId: Long) = "enrol_face/$staffId"
    fun staffHome(staffId: Long, displayName: String) =
        "staff_home/$staffId/${java.net.URLEncoder.encode(displayName, "UTF-8")}"
    fun markAttendance(staffId: Long) = "mark_attendance/$staffId"
    fun decode(value: String): String = java.net.URLDecoder.decode(value, "UTF-8")
}
