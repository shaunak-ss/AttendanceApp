package com.example.attendanceapp.data.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

/** Outcome of running face detection + embedding on a captured frame, before any match decision. */
sealed class FaceCaptureResult {
    data class Success(val vector: FloatArray, val face: Face) : FaceCaptureResult()
    data object NoFaceDetected : FaceCaptureResult()
    data object MultipleFacesDetected : FaceCaptureResult()
    data object EyesClosed : FaceCaptureResult()
    data object HeadNotFrontal : FaceCaptureResult()
    data object LandmarksMissing : FaceCaptureResult()
}

/**
 * Uses ML Kit to detect a face, gate on basic liveness/framing quality (eyes open, head frontal),
 * then crops the face and runs it through [FaceEmbedder] to get an identity embedding vector
 * that [FaceMatcher] compares against the enrolled embedding.
 */
class FaceEmbeddingExtractor(context: Context) {

    private val embedder = FaceEmbedder(context.applicationContext)

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun extract(bitmap: Bitmap): FaceCaptureResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = suspendCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

        if (faces.isEmpty()) return FaceCaptureResult.NoFaceDetected
        if (faces.size > 1) return FaceCaptureResult.MultipleFacesDetected

        val face = faces[0]

        // Liveness/quality gate: reject clearly-closed eyes or a strongly turned head so a
        // low-effort spoof (or an unusable frame) doesn't get enrolled/matched.
        val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
        if (leftEyeOpen < 0.4f || rightEyeOpen < 0.4f) return FaceCaptureResult.EyesClosed
        if (abs(face.headEulerAngleY) > 20f || abs(face.headEulerAngleZ) > 20f) {
            return FaceCaptureResult.HeadNotFrontal
        }

        val crop = cropFace(bitmap, face.boundingBox) ?: return FaceCaptureResult.LandmarksMissing
        val vector = embedder.embed(crop)
        return FaceCaptureResult.Success(vector, face)
    }

    private fun cropFace(bitmap: Bitmap, box: Rect): Bitmap? {
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.coerceIn(top + 1, bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
