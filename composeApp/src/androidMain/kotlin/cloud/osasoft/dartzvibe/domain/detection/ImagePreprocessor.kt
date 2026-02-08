package cloud.osasoft.dartzvibe.domain.detection

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.FloatBuffer

data class PreprocessedImage(
    val tensor: OnnxTensor,
    val originalWidth: Int,
    val originalHeight: Int,
    val modelInputSize: Int,
)

class ImagePreprocessor {

    fun preprocess(
        env: OrtEnvironment,
        jpegData: ByteArray,
        modelInputSize: Int = 640,
    ): PreprocessedImage {
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            ?: throw IllegalArgumentException("Failed to decode JPEG data")

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val resized = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true)
        if (resized !== bitmap) {
            bitmap.recycle()
        }

        val pixels = IntArray(modelInputSize * modelInputSize)
        resized.getPixels(pixels, 0, modelInputSize, 0, 0, modelInputSize, modelInputSize)
        resized.recycle()

        // Convert HWC (ARGB packed int) → CHW float normalized [0,1], RGB order
        val channelSize = modelInputSize * modelInputSize
        val buffer = FloatBuffer.allocate(3 * channelSize)
        pixels.forEachIndexed { i, pixel ->
            // Red channel
            buffer.put(i, ((pixel shr 16) and 0xFF) / 255f)
            // Green channel
            buffer.put(channelSize + i, ((pixel shr 8) and 0xFF) / 255f)
            // Blue channel
            buffer.put(2 * channelSize + i, (pixel and 0xFF) / 255f)
        }

        val shape = longArrayOf(1L, 3L, modelInputSize.toLong(), modelInputSize.toLong())
        val tensor = OnnxTensor.createTensor(env, buffer, shape)

        return PreprocessedImage(
            tensor = tensor,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            modelInputSize = modelInputSize,
        )
    }
}
