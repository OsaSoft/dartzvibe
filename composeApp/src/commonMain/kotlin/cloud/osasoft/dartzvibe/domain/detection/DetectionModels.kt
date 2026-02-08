package cloud.osasoft.dartzvibe.domain.detection

import cloud.osasoft.dartzvibe.data.model.Multiplier

data class DetectedDart(
    val normalizedX: Float,
    val normalizedY: Float,
    val confidence: Float,
    val segment: Int?,
    val multiplier: Multiplier?,
)

data class DetectionResult(
    val darts: List<DetectedDart>,
    val boardDetected: Boolean,
    val boardCenterX: Float?,
    val boardCenterY: Float?,
    val boardRadius: Float?,
    val processingTimeMs: Long,
)

data class RawDetection(
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
    val classId: Int,
)

object DetectionClass {
    const val CAL_20 = 0
    const val CAL_3 = 1
    const val CAL_11 = 2
    const val CAL_6 = 3
    const val DART = 4
}

sealed interface DetectionStatus {
    data object Idle : DetectionStatus
    data object Detecting : DetectionStatus
    data class Success(val result: DetectionResult) : DetectionStatus
    data class Error(val message: String) : DetectionStatus
}
