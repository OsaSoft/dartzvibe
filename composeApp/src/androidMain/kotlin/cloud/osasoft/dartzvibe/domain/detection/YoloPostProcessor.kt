package cloud.osasoft.dartzvibe.domain.detection

import kotlin.math.max
import kotlin.math.min

class YoloPostProcessor(
    private val numClasses: Int = 5,
    private val confidenceThreshold: Float = 0.5f,
    private val iouThreshold: Float = 0.5f,
) {

    /**
     * Parses YOLOv8 output tensor and extracts detections with NMS.
     *
     * @param outputData raw output float array
     * @param outputShape shape [1, 4+numClasses, N]
     * @param modelInputSize model input dimension (e.g. 640)
     * @return filtered and NMS-suppressed detections with coordinates normalized to 0..1
     */
    fun process(
        outputData: FloatArray,
        outputShape: LongArray,
        modelInputSize: Int,
    ): List<RawDetection> {
        val channels = outputShape[1].toInt() // 4 + numClasses = 9
        val numDetections = outputShape[2].toInt()

        // Output layout is [1, channels, N] — we need to read column-wise
        // For detection j, channel i: outputData[i * numDetections + j]
        val candidates = mutableListOf<RawDetection>()

        (0 until numDetections).forEach { j ->
            val cx = outputData[0 * numDetections + j]
            val cy = outputData[1 * numDetections + j]
            val w = outputData[2 * numDetections + j]
            val h = outputData[3 * numDetections + j]

            // Find best class
            var bestClassId = 0
            var bestScore = 0f
            (0 until numClasses).forEach { c ->
                val score = outputData[(4 + c) * numDetections + j]
                if (score > bestScore) {
                    bestScore = score
                    bestClassId = c
                }
            }

            if (bestScore >= confidenceThreshold) {
                candidates.add(
                    RawDetection(
                        cx = cx / modelInputSize,
                        cy = cy / modelInputSize,
                        width = w / modelInputSize,
                        height = h / modelInputSize,
                        confidence = bestScore,
                        classId = bestClassId,
                    ),
                )
            }
        }

        return nms(candidates)
    }

    /**
     * Per-class Non-Maximum Suppression.
     */
    private fun nms(detections: List<RawDetection>): List<RawDetection> {
        val result = mutableListOf<RawDetection>()

        detections
            .groupBy { it.classId }
            .values
            .forEach { classDetections ->
                val sorted = classDetections.sortedByDescending { it.confidence }.toMutableList()
                while (sorted.isNotEmpty()) {
                    val best = sorted.removeAt(0)
                    result.add(best)
                    sorted.removeAll { other -> computeIou(best, other) > iouThreshold }
                }
            }

        return result
    }

    private fun computeIou(a: RawDetection, b: RawDetection): Float {
        val aX1 = a.cx - a.width / 2f
        val aY1 = a.cy - a.height / 2f
        val aX2 = a.cx + a.width / 2f
        val aY2 = a.cy + a.height / 2f

        val bX1 = b.cx - b.width / 2f
        val bY1 = b.cy - b.height / 2f
        val bX2 = b.cx + b.width / 2f
        val bY2 = b.cy + b.height / 2f

        val interX1 = max(aX1, bX1)
        val interY1 = max(aY1, bY1)
        val interX2 = min(aX2, bX2)
        val interY2 = min(aY2, bY2)

        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }
}
