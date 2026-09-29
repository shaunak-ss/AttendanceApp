package com.example.attendanceapp.data.face

import kotlin.math.sqrt

object FaceMatcher {

    /**
     * Minimum cosine similarity between two L2-normalized MobileFaceNet embeddings (see
     * [FaceEmbedder]) for the live capture to count as the enrolled person. MobileFaceNet
     * (ArcFace-trained) typically separates same-identity from different-identity pairs somewhere
     * around 0.5-0.7 cosine similarity; start here and retune against real enrolment/attendance
     * captures — raise it if different people are still matching, lower it if the enrolled person
     * is being rejected too often.
     */
    const val MATCH_THRESHOLD = 0.65f

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
        return l2Normalize(result)
    }

    /** Returns (isMatch, similarity) — cosine similarity in [-1,1]; higher means a closer match. */
    fun match(enrolled: FloatArray, live: FloatArray): Pair<Boolean, Float> {
        val similarity = cosineSimilarity(enrolled, live)
        return (similarity >= MATCH_THRESHOLD) to similarity
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must be the same length to compare." }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA <= 0f || normB <= 0f) return -1f
        return dot / (sqrt(normA) * sqrt(normB))
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (value in vector) norm += value * value
        norm = sqrt(norm)
        if (norm < 1e-6f) return vector
        return FloatArray(vector.size) { vector[it] / norm }
    }
}
