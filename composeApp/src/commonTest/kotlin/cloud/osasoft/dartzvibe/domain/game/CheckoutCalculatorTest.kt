package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for CheckoutCalculator - the checkout path calculation logic.
 */
class CheckoutCalculatorTest : FreeSpec({

    // Helper functions to create throws for assertions
    fun single(segment: Int) = Throw(segment = segment, multiplier = Multiplier.SINGLE)
    fun double(segment: Int) = Throw(segment = segment, multiplier = Multiplier.DOUBLE)
    fun triple(segment: Int) = Throw(segment = segment, multiplier = Multiplier.TRIPLE)
    fun outerBull() = Throw(segment = 25, multiplier = Multiplier.SINGLE)
    fun bull() = Throw(segment = 25, multiplier = Multiplier.DOUBLE)

    "getCheckoutOptions with double-out" - {

        "Should return T20, T20, Bull for score 170" {
            // GIVEN a score of 170 (maximum checkout)
            val score = 170

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include T20, T20, Bull
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(triple(20), triple(20), bull())
        }

        "Should return Bull for score 50" {
            // GIVEN a score of 50
            val score = 50

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include Bull
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(bull())
        }

        "Should return D20 for score 40" {
            // GIVEN a score of 40
            val score = 40

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include D20
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(double(20))
        }

        "Should return D1 for score 2" {
            // GIVEN a score of 2 (minimum checkout)
            val score = 2

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include D1
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(double(1))
        }

        "Should return null for score 1 (impossible with double-out)" {
            // GIVEN a score of 1
            val score = 1

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should be null (impossible to finish)
            options.shouldBeNull()
        }

        "Should return null for score 171+ (not checkable in one turn)" {
            // GIVEN a score of 171
            val score = 171

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should be null
            options.shouldBeNull()
        }

        "Should return null for score 0 (already won)" {
            // GIVEN a score of 0
            val score = 0

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should be null
            options.shouldBeNull()
        }

        "Should return null for negative score" {
            // GIVEN a negative score
            val score = -5

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should be null
            options.shouldBeNull()
        }

        "Should return T20, T20, D20 for score 160" {
            // GIVEN a score of 160
            val score = 160

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include T20, T20, D20
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(triple(20), triple(20), double(20))
        }

        "Should return T20, D20 for score 100" {
            // GIVEN a score of 100
            val score = 100

            // WHEN getting checkout options with double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN options should include T20, D20
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(triple(20), double(20))
        }
    }

    "getCheckoutOptions without double-out" - {

        "Should return S1 for score 1 without double-out" {
            // GIVEN a score of 1 without double-out requirement
            val score = 1

            // WHEN getting checkout options without double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = false)

            // THEN options should include S1
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(single(1))
        }

        "Should return S20 for score 20 without double-out" {
            // GIVEN a score of 20 without double-out requirement
            val score = 20

            // WHEN getting checkout options without double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = false)

            // THEN options should include S20 (simpler than D10)
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(single(20))
        }

        "Should return S25 for score 25 without double-out" {
            // GIVEN a score of 25 without double-out requirement
            val score = 25

            // WHEN getting checkout options without double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = false)

            // THEN options should include S25 (outer bull)
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(outerBull())
        }

        "Should return Bull for score 50 without double-out" {
            // GIVEN a score of 50 without double-out requirement
            val score = 50

            // WHEN getting checkout options without double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = false)

            // THEN options should include Bull
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(bull())
        }

        "Should return D20 for score 40 without double-out (when no single available)" {
            // GIVEN a score of 40 without double-out requirement
            val score = 40

            // WHEN getting checkout options without double-out
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = false)

            // THEN options should include D20 (since there's no S40)
            options.shouldNotBeNull()
            options.shouldNotBeEmpty()
            val firstOption = options.first()
            firstOption.throws shouldBe listOf(double(20))
        }
    }

    "toDisplayString extension" - {

        "Should format single correctly" {
            // GIVEN a single throw
            val throwSingle = single(20)

            // WHEN formatting for display
            val display = throwSingle.toDisplayString()

            // THEN should show S20
            display shouldBe "S20"
        }

        "Should format double correctly" {
            // GIVEN a double throw
            val throwDouble = double(16)

            // WHEN formatting for display
            val display = throwDouble.toDisplayString()

            // THEN should show D16
            display shouldBe "D16"
        }

        "Should format triple correctly" {
            // GIVEN a triple throw
            val throwTriple = triple(20)

            // WHEN formatting for display
            val display = throwTriple.toDisplayString()

            // THEN should show T20
            display shouldBe "T20"
        }

        "Should format bull correctly" {
            // GIVEN a bull throw
            val throwBull = bull()

            // WHEN formatting for display
            val display = throwBull.toDisplayString()

            // THEN should show Bull
            display shouldBe "Bull"
        }

        "Should format outer bull correctly" {
            // GIVEN an outer bull throw
            val throwOuterBull = outerBull()

            // WHEN formatting for display
            val display = throwOuterBull.toDisplayString()

            // THEN should show S25
            display shouldBe "S25"
        }
    }

    "Multiple checkout alternatives" - {

        "Should include 2-dart alternatives for 1-dart finishes" {
            // GIVEN a score of 40 (has 1-dart finish D20)
            val score = 40

            // WHEN getting checkout options
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN should include both 1-dart and 2-dart options
            options.shouldNotBeNull()
            options.size shouldNotBe 1
            // First option should be D20 (1-dart)
            options.first().throws shouldHaveSize 1
            options.first().throws shouldBe listOf(double(20))
            // Should also include 2-dart alternatives
            options.any { it.throws.size == 2 } shouldBe true
        }

        "Should include 2-dart alternatives for Bull (score 50)" {
            // GIVEN a score of 50 (has 1-dart finish Bull)
            val score = 50

            // WHEN getting checkout options
            val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

            // THEN should include both 1-dart and 2-dart options
            options.shouldNotBeNull()
            options.size shouldNotBe 1
            // First option should be Bull (1-dart)
            options.first().throws shouldHaveSize 1
            options.first().throws shouldBe listOf(bull())
            // Should also include 2-dart alternatives
            options.any { it.throws.size == 2 } shouldBe true
        }
    }

    "Coverage of common checkouts" - {

        "Should provide options for all doubles (2-40 even numbers)" {
            // GIVEN all even scores from 2 to 40
            val evenScores = (2..40 step 2).toList()

            // WHEN getting checkout options for each
            evenScores.forEach { score ->
                val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

                // THEN options should not be null and should be 1-dart finishes
                options.shouldNotBeNull()
                options.shouldNotBeEmpty()
                options.first().throws shouldHaveSize 1
            }
        }

        "Should provide options for all checkable scores (2-170)" {
            // GIVEN all checkable scores
            val checkableScores = (2..170).toList()

            // WHEN getting checkout options for each
            checkableScores.forEach { score ->
                val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

                // THEN options should not be null (all 2-170 are checkable with double-out)
                options.shouldNotBeNull()
            }
        }

        "Should calculate correct total score for checkout paths" {
            // GIVEN various checkout scores
            val testCases = listOf(40, 100, 160, 170)

            // WHEN getting checkout options for each
            testCases.forEach { score ->
                val options = CheckoutCalculator.getCheckoutOptions(score, doubleOut = true)

                // THEN the total score of throws should equal the checkout score
                options.shouldNotBeNull()
                options.forEach { path ->
                    path.totalScore shouldBe score
                }
            }
        }
    }
})
