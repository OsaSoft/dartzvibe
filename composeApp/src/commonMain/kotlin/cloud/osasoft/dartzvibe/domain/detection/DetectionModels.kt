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

sealed interface DetectionStatus {
    data object Idle : DetectionStatus
    data object Detecting : DetectionStatus
    data class Success(val result: DetectionResult) : DetectionStatus
    data class Error(val message: String) : DetectionStatus
}
