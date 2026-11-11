package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.domain.game.CheckoutPath
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CheckoutSlotTextTest : FreeSpec({

    fun single(segment: Int) = Throw(segment = segment, multiplier = Multiplier.SINGLE)
    fun double(segment: Int) = Throw(segment = segment, multiplier = Multiplier.DOUBLE)
    fun triple(segment: Int) = Throw(segment = segment, multiplier = Multiplier.TRIPLE)
    fun bull() = Throw(segment = 25, multiplier = Multiplier.DOUBLE)

    "checkoutSlotText" - {

        "Should be null when there are no checkout options" {
            // GIVEN no checkout (score above 170, bust, or no finish with the darts left)
            // WHEN formatting the slot
            // THEN there is nothing to show
            checkoutSlotText(null).shouldBeNull()
            checkoutSlotText(emptyList()).shouldBeNull()
        }

        "Should show a single option as the primary with no alternative" {
            // GIVEN one checkout option
            val options = listOf(CheckoutPath(listOf(double(20))))

            // WHEN formatting the slot
            val text = checkoutSlotText(options)

            // THEN it is the primary and there is no alternative
            text shouldBe CheckoutSlotText(primary = "D20", secondary = null)
        }

        "Should join the darts with arrows and keep only the top two options" {
            // GIVEN three checkout paths (worth 170, 157 and 120; the formatter only joins the darts)
            val options = listOf(
                CheckoutPath(listOf(triple(20), triple(20), bull())),
                CheckoutPath(listOf(triple(20), triple(19), double(20))),
                CheckoutPath(listOf(single(20), triple(20), double(20))),
            )

            // WHEN formatting the slot
            val text = checkoutSlotText(options)

            // THEN the first is the primary, the second the alternative and the third is dropped
            text shouldBe CheckoutSlotText(
                primary = "T20 → T20 → Bull",
                secondary = "T20 → T19 → D20",
            )
        }
    }

    "checkoutSlotDescription" - {

        withData(
            nameFn = { (text, expected) -> "Should read \"$expected\" for ${text.primary} / ${text.secondary}" },
            CheckoutSlotText(primary = "D20", secondary = null) to "Checkout: D20",
            CheckoutSlotText(
                primary = "T20 → T20 → Bull",
                secondary = "T20 → T19 → D20",
            ) to "Checkout: T20, T20, Bull, or T20, T19, D20",
        ) { (text, expected) ->
            // GIVEN the slot text
            // WHEN building what TalkBack reads
            // THEN the darts are separated by commas and the alternative follows with "or"
            checkoutSlotDescription(text) shouldBe expected
        }
    }

    "fitTextScale" - {

        data class ScaleCase(
            val textWidth: Int,
            val maxWidth: Int,
            val expected: Float,
        )

        withData(
            nameFn = { "Should scale ${it.textWidth} px into ${it.maxWidth} px by ${it.expected}" },
            ScaleCase(textWidth = 300, maxWidth = 400, expected = 1f),
            ScaleCase(textWidth = 400, maxWidth = 400, expected = 1f),
            ScaleCase(textWidth = 500, maxWidth = 400, expected = 400f / 500f * 0.97f),
            ScaleCase(textWidth = 500, maxWidth = 0, expected = 1f),
        ) { case ->
            // GIVEN a measured route width and the strip width
            // WHEN computing the font scale
            val scale = fitTextScale(textWidth = case.textWidth, maxWidth = case.maxWidth)

            // THEN a fitting route keeps its size and a wider one shrinks to just under the strip width
            scale shouldBe case.expected
        }
    }
})
