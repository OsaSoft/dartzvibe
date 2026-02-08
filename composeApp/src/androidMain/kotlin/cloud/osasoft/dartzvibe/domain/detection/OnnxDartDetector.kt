package cloud.osasoft.dartzvibe.domain.detection

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlin.math.sqrt

class OnnxDartDetector(
    private val context: Context,
    private val modelFileName: String = "dart_detect.onnx",
) : DartDetector {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val preprocessor = ImagePreprocessor()
    private val postProcessor = YoloPostProcessor()
    private var session: OrtSession? = null
    private var modelLoadAttempted = false

    private fun getOrCreateSession(): OrtSession? {
        if (session != null) return session
        if (modelLoadAttempted) return null

        modelLoadAttempted = true
        return try {
            val modelBytes = context.assets.open(modelFileName).use { it.readBytes() }
            env.createSession(modelBytes).also { session = it }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load ONNX model '$modelFileName': ${e.message}")
            null
        }
    }

    override suspend fun detect(
        imageData: ByteArray,
        width: Int,
        height: Int,
    ): DetectionResult {
        val startTime = System.currentTimeMillis()

        val ortSession = getOrCreateSession()
            ?: return emptyResult(System.currentTimeMillis() - startTime)

        return try {
            runInference(ortSession, imageData, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
            emptyResult(System.currentTimeMillis() - startTime)
        }
    }

    private fun runInference(
        ortSession: OrtSession,
        imageData: ByteArray,
        startTime: Long,
    ): DetectionResult {
        val preprocessed = preprocessor.preprocess(env, imageData)

        val inputName = ortSession.inputNames.first()
        val results = ortSession.run(mapOf(inputName to preprocessed.tensor))

        val outputTensor = results.first().value
        val outputShape = (outputTensor as ai.onnxruntime.OnnxTensor).info.shape
        val outputData = (outputTensor).floatBuffer.array()

        preprocessed.tensor.close()
        results.close()

        val rawDetections = postProcessor.process(outputData, outputShape, preprocessed.modelInputSize)

        return buildDetectionResult(rawDetections, System.currentTimeMillis() - startTime)
    }

    private fun buildDetectionResult(
        rawDetections: List<RawDetection>,
        processingTimeMs: Long,
    ): DetectionResult {
        val calibrationPoints = rawDetections
            .filter { it.classId != DetectionClass.DART }
            .associateBy { it.classId }

        val dartDetections = rawDetections.filter { it.classId == DetectionClass.DART }

        val boardDetected = calibrationPoints.size == 4
        val homography = if (boardDetected) computeCalibrationHomography(calibrationPoints) else null

        val darts = dartDetections.map { raw ->
            if (homography != null) {
                val (boardX, boardY) = applyHomography(homography, raw.cx, raw.cy)
                val mapping = mapPosition(boardX, boardY)
                DetectedDart(
                    normalizedX = boardX,
                    normalizedY = boardY,
                    confidence = raw.confidence,
                    segment = mapping?.first,
                    multiplier = mapping?.second,
                )
            } else {
                DetectedDart(
                    normalizedX = raw.cx,
                    normalizedY = raw.cy,
                    confidence = raw.confidence,
                    segment = null,
                    multiplier = null,
                )
            }
        }

        val (boardCenterX, boardCenterY, boardRadius) = computeBoardOverlayParams(calibrationPoints)

        return DetectionResult(
            darts = darts,
            boardDetected = boardDetected,
            boardCenterX = boardCenterX,
            boardCenterY = boardCenterY,
            boardRadius = boardRadius,
            processingTimeMs = processingTimeMs,
        )
    }

    private fun computeCalibrationHomography(
        calibrationPoints: Map<Int, RawDetection>,
    ): FloatArray? {
        val srcPoints = listOf(
            DetectionClass.CAL_20,
            DetectionClass.CAL_3,
            DetectionClass.CAL_11,
            DetectionClass.CAL_6,
        ).map { classId ->
            val det = calibrationPoints[classId] ?: return null
            Pair(det.cx, det.cy)
        }

        val dstPoints = listOf(
            DetectionClass.CAL_20,
            DetectionClass.CAL_3,
            DetectionClass.CAL_11,
            DetectionClass.CAL_6,
        ).map { classId ->
            CALIBRATION_BOARD_POSITIONS[classId]!!
        }

        return computeHomography(srcPoints, dstPoints)
    }

    private fun computeBoardOverlayParams(
        calibrationPoints: Map<Int, RawDetection>,
    ): Triple<Float?, Float?, Float?> {
        if (calibrationPoints.size < 2) return Triple(null, null, null)

        val points = calibrationPoints.values.toList()
        val centerX = points.map { it.cx }.average().toFloat()
        val centerY = points.map { it.cy }.average().toFloat()

        val avgRadius = points.map { det ->
            val dx = det.cx - centerX
            val dy = det.cy - centerY
            sqrt(dx * dx + dy * dy)
        }.average().toFloat()

        return Triple(centerX, centerY, avgRadius)
    }

    fun close() {
        session?.close()
        session = null
    }

    private fun emptyResult(processingTimeMs: Long) = DetectionResult(
        darts = emptyList(),
        boardDetected = false,
        boardCenterX = null,
        boardCenterY = null,
        boardRadius = null,
        processingTimeMs = processingTimeMs,
    )

    companion object {
        private const val TAG = "OnnxDartDetector"
    }
}
