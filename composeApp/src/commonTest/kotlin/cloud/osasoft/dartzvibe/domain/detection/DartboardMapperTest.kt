package cloud.osasoft.dartzvibe.domain.detection

import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.screen.game.components.DARTBOARD_SEGMENT_ORDER
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tests for DartboardMapper - mapping normalized (x, y) coordinates to dartboard segments.
 */
class DartboardMapperTest : FreeSpec({

    /**
     * Creates a position at a given angle and radius on the board.
     * Angle 0 = top (12 o'clock), increasing clockwise.
     */
    fun positionAt(angleDegrees: Float, radius: Float): Pair<Float, Float> {
        val angleRadians = angleDegrees * PI.toFloat() / 180f
        val x = radius * sin(angleRadians)
        val y = -radius * cos(angleRadians)
        return Pair(x, y)
    }

    "Bull zones" - {
        "Should detect double bull at center" {
            // GIVEN exact center position
            // WHEN mapping the position
            val result = mapPosition(0f, 0f)

            // THEN it should be double bull
            result.shouldNotBeNull()
            result.first shouldBe 25
            result.second shouldBe Multiplier.DOUBLE
        }

        "Should detect double bull within inner ring" {
            // GIVEN a position within double bull radius
            val result = mapPosition(0.01f, 0.01f)

            // THEN it should be double bull
            result.shouldNotBeNull()
            result.first shouldBe 25
            result.second shouldBe Multiplier.DOUBLE
        }

        "Should detect single bull between inner and outer bull rings" {
            // GIVEN a position in the single bull ring (between 0.032 and 0.079)
            val result = mapPosition(0.05f, 0f)

            // THEN it should be single bull
            result.shouldNotBeNull()
            result.first shouldBe 25
            result.second shouldBe Multiplier.SINGLE
        }

        "Should detect single bull at edge of bull zone" {
            // GIVEN a position just inside single bull radius
            val result = mapPosition(0.07f, 0f)

            // THEN it should be single bull
            result.shouldNotBeNull()
            result.first shouldBe 25
            result.second shouldBe Multiplier.SINGLE
        }
    }

    "Inner single ring" - {
        "Should detect inner single at top (segment 20)" {
            // GIVEN a position in the inner single ring, at the top
            val (x, y) = positionAt(angleDegrees = 0f, radius = 0.3f)
            val result = mapPosition(x, y)

            // THEN it should be single 20
            result.shouldNotBeNull()
            result.first shouldBe 20
            result.second shouldBe Multiplier.SINGLE
        }

        "Should detect all 20 segments in inner single ring" {
            // GIVEN each segment position at inner single radius
            DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
                val angle = index * 18f
                val (x, y) = positionAt(angleDegrees = angle, radius = 0.3f)

                // WHEN mapping the position
                val result = mapPosition(x, y)

                // THEN it should be the correct segment as single
                result.shouldNotBeNull()
                result.first shouldBe segment
                result.second shouldBe Multiplier.SINGLE
            }
        }
    }

    "Triple ring" - {
        "Should detect triple 20 at top" {
            // GIVEN a position in the triple ring at the top
            val (x, y) = positionAt(angleDegrees = 0f, radius = 0.51f)
            val result = mapPosition(x, y)

            // THEN it should be triple 20
            result.shouldNotBeNull()
            result.first shouldBe 20
            result.second shouldBe Multiplier.TRIPLE
        }

        "Should detect all 20 segments in triple ring" {
            // GIVEN each segment position at triple radius
            DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
                val angle = index * 18f
                val (x, y) = positionAt(angleDegrees = angle, radius = 0.51f)

                // WHEN mapping the position
                val result = mapPosition(x, y)

                // THEN it should be the correct segment as triple
                result.shouldNotBeNull()
                result.first shouldBe segment
                result.second shouldBe Multiplier.TRIPLE
            }
        }
    }

    "Outer single ring" - {
        "Should detect outer single 20 at top" {
            // GIVEN a position in the outer single ring at the top
            val (x, y) = positionAt(angleDegrees = 0f, radius = 0.7f)
            val result = mapPosition(x, y)

            // THEN it should be single 20
            result.shouldNotBeNull()
            result.first shouldBe 20
            result.second shouldBe Multiplier.SINGLE
        }

        "Should detect all 20 segments in outer single ring" {
            // GIVEN each segment position at outer single radius
            DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
                val angle = index * 18f
                val (x, y) = positionAt(angleDegrees = angle, radius = 0.7f)

                // WHEN mapping the position
                val result = mapPosition(x, y)

                // THEN it should be the correct segment as single
                result.shouldNotBeNull()
                result.first shouldBe segment
                result.second shouldBe Multiplier.SINGLE
            }
        }
    }

    "Double ring" - {
        "Should detect double 20 at top" {
            // GIVEN a position in the double ring at the top
            val (x, y) = positionAt(angleDegrees = 0f, radius = 0.92f)
            val result = mapPosition(x, y)

            // THEN it should be double 20
            result.shouldNotBeNull()
            result.first shouldBe 20
            result.second shouldBe Multiplier.DOUBLE
        }

        "Should detect all 20 segments in double ring" {
            // GIVEN each segment position at double radius
            DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
                val angle = index * 18f
                val (x, y) = positionAt(angleDegrees = angle, radius = 0.92f)

                // WHEN mapping the position
                val result = mapPosition(x, y)

                // THEN it should be the correct segment as double
                result.shouldNotBeNull()
                result.first shouldBe segment
                result.second shouldBe Multiplier.DOUBLE
            }
        }

        "Should detect double at edge of board" {
            // GIVEN a position at the very edge of the board
            val (x, y) = positionAt(angleDegrees = 0f, radius = 0.99f)
            val result = mapPosition(x, y)

            // THEN it should still be in the double ring
            result.shouldNotBeNull()
            result.first shouldBe 20
            result.second shouldBe Multiplier.DOUBLE
        }
    }

    "Miss (outside board)" - {
        "Should return null for position outside board" {
            // GIVEN a position outside the board (radius > 1.0)
            val (x, y) = positionAt(angleDegrees = 0f, radius = 1.1f)

            // WHEN mapping the position
            val result = mapPosition(x, y)

            // THEN it should be null (miss)
            result.shouldBeNull()
        }

        "Should return null for far miss" {
            // GIVEN a position far outside the board
            val result = mapPosition(2f, 2f)

            // THEN it should be null (miss)
            result.shouldBeNull()
        }
    }

    "Specific segment positions" - {
        "Should detect segment 6 at right position" {
            // GIVEN segment 6 is at index 5 in the order (90 degrees)
            val (x, y) = positionAt(angleDegrees = 90f, radius = 0.3f)
            val result = mapPosition(x, y)

            // THEN it should be segment 6
            result.shouldNotBeNull()
            result.first shouldBe 6
            result.second shouldBe Multiplier.SINGLE
        }

        "Should detect segment 3 at bottom-left area" {
            // GIVEN segment 3 is at index 10 (180 degrees)
            val (x, y) = positionAt(angleDegrees = 180f, radius = 0.3f)
            val result = mapPosition(x, y)

            // THEN it should be segment 3
            result.shouldNotBeNull()
            result.first shouldBe 3
            result.second shouldBe Multiplier.SINGLE
        }

        "Should detect segment 5 at left position" {
            // GIVEN segment 5 is at index 19 (342 degrees)
            val (x, y) = positionAt(angleDegrees = 342f, radius = 0.3f)
            val result = mapPosition(x, y)

            // THEN it should be segment 5
            result.shouldNotBeNull()
            result.first shouldBe 5
            result.second shouldBe Multiplier.SINGLE
        }
    }

    "Ring boundary transitions" - {
        "Should transition from double bull to single bull" {
            // GIVEN positions just inside and just outside double bull boundary
            val insideDoubleBull = mapPosition(0.03f, 0f)
            val outsideDoubleBull = mapPosition(0.04f, 0f)

            // THEN inside should be double bull
            insideDoubleBull.shouldNotBeNull()
            insideDoubleBull.first shouldBe 25
            insideDoubleBull.second shouldBe Multiplier.DOUBLE

            // AND outside should be single bull
            outsideDoubleBull.shouldNotBeNull()
            outsideDoubleBull.first shouldBe 25
            outsideDoubleBull.second shouldBe Multiplier.SINGLE
        }

        "Should transition from single bull to inner single" {
            // GIVEN positions at single bull edge and inner single start
            val insideBull = mapPosition(0.07f, 0f)
            val outsideBull = mapPosition(0.09f, 0f)

            // THEN inside should be single bull
            insideBull.shouldNotBeNull()
            insideBull.first shouldBe 25
            insideBull.second shouldBe Multiplier.SINGLE

            // AND outside should be inner single (segment depends on angle)
            outsideBull.shouldNotBeNull()
            outsideBull.second shouldBe Multiplier.SINGLE
            outsideBull.first shouldBe 6 // right side of board
        }

        "Should transition from inner single to triple" {
            // GIVEN positions at inner single edge and triple start
            val (x1, y1) = positionAt(angleDegrees = 0f, radius = 0.48f)
            val (x2, y2) = positionAt(angleDegrees = 0f, radius = 0.50f)

            val innerSingle = mapPosition(x1, y1)
            val triple = mapPosition(x2, y2)

            // THEN first should be single
            innerSingle.shouldNotBeNull()
            innerSingle.second shouldBe Multiplier.SINGLE

            // AND second should be triple
            triple.shouldNotBeNull()
            triple.second shouldBe Multiplier.TRIPLE
        }

        "Should transition from triple to outer single" {
            // GIVEN positions at triple edge and outer single start
            val (x1, y1) = positionAt(angleDegrees = 0f, radius = 0.52f)
            val (x2, y2) = positionAt(angleDegrees = 0f, radius = 0.55f)

            val triple = mapPosition(x1, y1)
            val outerSingle = mapPosition(x2, y2)

            // THEN first should be triple
            triple.shouldNotBeNull()
            triple.second shouldBe Multiplier.TRIPLE

            // AND second should be single
            outerSingle.shouldNotBeNull()
            outerSingle.second shouldBe Multiplier.SINGLE
        }

        "Should transition from outer single to double" {
            // GIVEN positions at outer single edge and double start
            val (x1, y1) = positionAt(angleDegrees = 0f, radius = 0.83f)
            val (x2, y2) = positionAt(angleDegrees = 0f, radius = 0.85f)

            val outerSingle = mapPosition(x1, y1)
            val double = mapPosition(x2, y2)

            // THEN first should be single
            outerSingle.shouldNotBeNull()
            outerSingle.second shouldBe Multiplier.SINGLE

            // AND second should be double
            double.shouldNotBeNull()
            double.second shouldBe Multiplier.DOUBLE
        }
    }
})
