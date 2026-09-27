package com.example.attendanceapp.data.face

import kotlin.math.sqrt

object FaceMatcher {

    /**
     * Below this normalized Euclidean distance the live capture is considered a match.
     * Tuned empirically against the landmark-distance vector produced by [FaceLandmarkExtractor];
     * lower = stricter. Exposed as a single constant so it's easy to retune during testing.
     */
    const val MATCH_THRESHOLD = 0.18f

    fun vectorToString(vector: FloatArray): String = vector.joinToString(",")

    fun stringToVector(value: String): FloatArray =
        value.split(",").map { it.toFloat() }.toFloatArray()

    fun averageVectors(vectors: List<FloatArray>): FloatArray {
        require(vectors.isNotEmpty())
        val size = vectors[0].size
        val result = FloatArray(size)
        for (v in vectors) {
            for (i in 0 until size) result[i] += v[i]
        }
        for (i in 0 until size) result[i] /= vectors.size
        return result
    }

    /** Returns (isMatch, distance) — smaller distance means a closer match. */
    fun match(enrolled: FloatArray, live: FloatArray): Pair<Boolean, Float> {
        val distance = euclideanDistance(enrolled, live)
        return (distance <= MATCH_THRESHOLD) to distance
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Feature vectors must be the same length to compare." }
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum / a.size)
    }
}
