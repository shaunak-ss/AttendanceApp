package com.example.attendanceapp.data.face

import android.content.Context
import android.graphics.Bitmap
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt
import org.tensorflow.lite.Interpreter

/**
 * Runs the bundled MobileFaceNet TFLite model (assets/mobilefacenet.tflite) to turn a 112x112
 * aligned face crop into a 192-d L2-normalized identity embedding for [FaceMatcher] to compare.
 */
class FaceEmbedder(context: Context) {

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile(context))
    }

    fun embed(faceCrop: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(faceCrop, INPUT_SIZE, INPUT_SIZE, true)
        val input = bitmapToByteBuffer(resized)
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.putFloat((((pixel shr 16) and 0xFF) - 128f) / 128f)
            buffer.putFloat((((pixel shr 8) and 0xFF) - 128f) / 128f)
            buffer.putFloat(((pixel and 0xFF) - 128f) / 128f)
        }
        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (value in vector) norm += value * value
        norm = sqrt(norm)
        if (norm < 1e-6f) return vector
        return FloatArray(vector.size) { vector[it] / norm }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    companion object {
        private const val MODEL_FILE = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
    }
}
