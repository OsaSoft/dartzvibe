package cloud.osasoft.dartzvibe.domain.detection

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.math.abs

class HomographyTest : FreeSpec({

    fun assertPointNear(
        actual: Pair<Float, Float>,
        expectedX: Float,
        expectedY: Float,
        tolerance: Double = 1.0,
    ) {
        actual.first.shouldBeWithinPercentageOf(expectedX, tolerance)
        actual.second.shouldBeWithinPercentageOf(expectedY, tolerance)
    }

    "Identity transform" - {
        "Should produce identity when source and destination are the same" {
            // GIVEN 4 points that map to themselves
            val points = listOf(
                Pair(0f, 0f),
                Pair(1f, 0f),
                Pair(1f, 1f),
                Pair(0f, 1f),
            )

            // WHEN computing the homography
            val matrix = computeHomography(points, points)

            // THEN matrix should not be null
            matrix.shouldNotBeNull()

            // AND applying it to any source point should return the same point
            val result = applyHomography(matrix, 0.5f, 0.5f)
            result.first.shouldBeWithinPercentageOf(0.5f, 1.0)
            result.second.shouldBeWithinPercentageOf(0.5f, 1.0)
        }
    }

    "Known transform" - {
        "Should correctly transform with a scaling homography" {
            // GIVEN source points in a unit square
            val src = listOf(
                Pair(0f, 0f),
                Pair(1f, 0f),
                Pair(1f, 1f),
                Pair(0f, 1f),
            )
            // AND destination points scaled by 2x
            val dst = listOf(
                Pair(0f, 0f),
                Pair(2f, 0f),
                Pair(2f, 2f),
                Pair(0f, 2f),
            )

            // WHEN computing the homography
            val matrix = computeHomography(src, dst)
            matrix.shouldNotBeNull()

            // THEN transforming (0.5, 0.5) should give (1.0, 1.0)
            val result = applyHomography(matrix, 0.5f, 0.5f)
            result.first.shouldBeWithinPercentageOf(1.0f, 1.0)
            result.second.shouldBeWithinPercentageOf(1.0f, 1.0)
        }

        "Should correctly transform with translation" {
            // GIVEN source points and destination shifted by (10, 20)
            val src = listOf(
                Pair(0f, 0f),
                Pair(100f, 0f),
                Pair(100f, 100f),
                Pair(0f, 100f),
            )
            val dst = listOf(
                Pair(10f, 20f),
                Pair(110f, 20f),
                Pair(110f, 120f),
                Pair(10f, 120f),
            )

            // WHEN computing the homography
            val matrix = computeHomography(src, dst)
            matrix.shouldNotBeNull()

            // THEN transforming (50, 50) should give (60, 70)
            val result = applyHomography(matrix, 50f, 50f)
            result.first.shouldBeWithinPercentageOf(60.0f, 1.0)
            result.second.shouldBeWithinPercentageOf(70.0f, 1.0)
        }
    }

    "Round-trip" - {
        "Should transform source points to destination points" {
            // GIVEN arbitrary source and destination points
            val src = listOf(
                Pair(100f, 50f),
                Pair(400f, 60f),
                Pair(380f, 420f),
                Pair(120f, 400f),
            )
            val dst = listOf(
                Pair(-1f, -1f),
                Pair(1f, -1f),
                Pair(1f, 1f),
                Pair(-1f, 1f),
            )

            // WHEN computing the homography
            val matrix = computeHomography(src, dst)
            matrix.shouldNotBeNull()

            // THEN each source point should map to its corresponding destination
            src.forEachIndexed { i, (sx, sy) ->
                val result = applyHomography(matrix, sx, sy)
                assertPointNear(result, dst[i].first, dst[i].second, tolerance = 1.0)
            }
        }
    }

    "Dart transform" - {
        "Should transform dart position through calibration homography" {
            // GIVEN calibration points detected at known image positions
            // Simulating a centered, upright board in the image
            val imgSrc = listOf(
                Pair(320f, 50f), // cal_20 at top
                Pair(320f, 590f), // cal_3 at bottom
                Pair(50f, 320f), // cal_11 at left
                Pair(590f, 320f), // cal_6 at right
            )
            val boardDst = listOf(
                CALIBRATION_BOARD_POSITIONS[DetectionClass.CAL_20]!!,
                CALIBRATION_BOARD_POSITIONS[DetectionClass.CAL_3]!!,
                CALIBRATION_BOARD_POSITIONS[DetectionClass.CAL_11]!!,
                CALIBRATION_BOARD_POSITIONS[DetectionClass.CAL_6]!!,
            )

            // WHEN computing the homography
            val matrix = computeHomography(imgSrc, boardDst)
            matrix.shouldNotBeNull()

            // THEN a dart at the image center (320, 320) should map near board center (0, 0)
            val centerResult = applyHomography(matrix, 320f, 320f)
            abs(centerResult.first).shouldBeLessThan(0.05f)
            abs(centerResult.second).shouldBeLessThan(0.05f)

            // AND calibration source points should map to their board positions
            imgSrc.forEachIndexed { i, (sx, sy) ->
                val result = applyHomography(matrix, sx, sy)
                assertPointNear(result, boardDst[i].first, boardDst[i].second, tolerance = 1.0)
            }
        }
    }

    "Degenerate case" - {
        "Should return null for collinear points" {
            // GIVEN 4 collinear source points (all on the same line)
            val src = listOf(
                Pair(0f, 0f),
                Pair(1f, 0f),
                Pair(2f, 0f),
                Pair(3f, 0f),
            )
            val dst = listOf(
                Pair(0f, 0f),
                Pair(1f, 1f),
                Pair(2f, 2f),
                Pair(3f, 3f),
            )

            // WHEN computing the homography
            val matrix = computeHomography(src, dst)

            // THEN it should return null (degenerate configuration)
            matrix.shouldBeNull()
        }
    }
})
