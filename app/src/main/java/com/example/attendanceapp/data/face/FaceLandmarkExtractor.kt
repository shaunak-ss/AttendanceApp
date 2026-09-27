package com.example.attendanceapp.data.face

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.sqrt

/** Outcome of running ML Kit on a captured frame, before any matching decision is made. */
sealed class FaceCaptureResult {
    data class Success(val vector: FloatArray, val face: Face) : FaceCaptureResult()
    data object NoFaceDetected : FaceCaptureResult()
    data object MultipleFacesDetected : FaceCaptureResult()
    data object EyesClosed : FaceCaptureResult()
    data object HeadNotFrontal : FaceCaptureResult()
    data object LandmarksMissing : FaceCaptureResult()
}

/**
 * Wraps ML Kit face detection and turns detected landmarks into a normalized geometric
 * feature vector (pairwise landmark distances divided by inter-ocular distance, so the
 * vector is roughly scale/translation invariant).
 */
class FaceLandmarkExtractor {

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
        if (kotlin.math.abs(face.headEulerAngleY) > 20f || kotlin.math.abs(face.headEulerAngleZ) > 20f) {
            return FaceCaptureResult.HeadNotFrontal
        }

        val vector = buildVector(face) ?: return FaceCaptureResult.LandmarksMissing
        return FaceCaptureResult.Success(vector, face)
    }

    private fun buildVector(face: Face): FloatArray? {
        val landmarkTypes = intArrayOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK
        )

        val points = landmarkTypes.map { type ->
            face.getLandmark(type)?.position ?: return null
        }

        val leftEye = points[0]
        val rightEye = points[1]
        val interOcularDistance = distance(leftEye.x, leftEye.y, rightEye.x, rightEye.y)
        if (interOcularDistance < 1e-3f) return null

        // All pairwise distances between the chosen landmarks, normalized by inter-ocular
        // distance so the vector doesn't depend on how close the face is to the camera.
        val distances = mutableListOf<Float>()
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                val d = distance(points[i].x, points[i].y, points[j].x, points[j].y)
                distances.add(d / interOcularDistance)
            }
        }
        return distances.toFloatArray()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
