package cloud.osasoft.dartzvibe.domain.detection

interface DartDetector {
    suspend fun detect(imageData: ByteArray, width: Int, height: Int): DetectionResult
}
