package cloud.osasoft.dartzvibe.domain.detection

/**
 * Placeholder for future ONNX Runtime-based dart detection.
 * Will load a YOLOv8n model and run inference on camera frames.
 */
class OnnxDartDetector : DartDetector {

    override suspend fun detect(imageData: ByteArray, width: Int, height: Int): DetectionResult =
        DetectionResult(
            darts = emptyList(),
            boardDetected = false,
            boardCenterX = null,
            boardCenterY = null,
            boardRadius = null,
            processingTimeMs = 0L,
        )
}
