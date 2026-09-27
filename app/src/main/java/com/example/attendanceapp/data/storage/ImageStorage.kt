package com.example.attendanceapp.data.storage

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Saves capture selfies under app-private storage and hands back relative paths for Room. */
class ImageStorage(private val context: Context) {

    fun saveFaceEnrolmentImage(bitmap: Bitmap): String =
        saveBitmap(bitmap, "faces")

    fun saveAttendanceImage(bitmap: Bitmap): String =
        saveBitmap(bitmap, "attendance")

    fun resolveFile(relativePath: String): File =
        File(context.filesDir, relativePath)

    private fun saveBitmap(bitmap: Bitmap, subDir: String): String {
        val dir = File(context.filesDir, subDir).apply { mkdirs() }
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return "$subDir/$fileName"
    }
}
