package cloud.osasoft.dartzvibe.domain.detection

import kotlinx.coroutines.delay
import kotlin.random.Random

class MockDartDetector : DartDetector {

    override suspend fun detect(imageData: ByteArray, width: Int, height: Int): DetectionResult {
        delay(150L)

        val dartCount = Random.nextInt(1, 4)
        val darts = (1..dartCount).map { generateRandomDart() }

        return DetectionResult(
            darts = darts,
            boardDetected = true,
            boardCenterX = 0.5f,
            boardCenterY = 0.45f,
            boardRadius = 0.35f,
            processingTimeMs = Random.nextLong(100, 200),
        )
    }

    private fun generateRandomDart(): DetectedDart {
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val radius = Random.nextFloat() * 0.9f
        val normalizedX = radius * kotlin.math.cos(angle)
        val normalizedY = radius * kotlin.math.sin(angle)

        val mapping = mapPosition(normalizedX, normalizedY)

        return DetectedDart(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            confidence = Random.nextFloat() * 0.3f + 0.7f,
            segment = mapping?.first,
            multiplier = mapping?.second,
        )
    }
}
