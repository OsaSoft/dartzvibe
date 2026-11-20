package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.material3.ColorScheme
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.domain.game.CheckoutPath
import cloud.osasoft.dartzvibe.domain.game.KnockoutRoute
import cloud.osasoft.dartzvibe.ui.theme.DarkColorScheme
import cloud.osasoft.dartzvibe.ui.theme.LightColorScheme
import cloud.osasoft.dartzvibe.ui.theme.contrastRatio
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
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

    "knockoutSlotText" - {

        val bob = Uuid.parse("00000000-0000-0000-0000-0000000000b0")
        val dave = Uuid.parse("00000000-0000-0000-0000-0000000000d0")
        val names = mapOf(bob to "Bob", dave to "Dave")

        "Should be null when nobody can be knocked out" {
            // GIVEN no knockout routes
            // WHEN formatting the slot
            // THEN there is nothing to show
            knockoutSlotText(emptyList()) { names.getValue(it) }.shouldBeNull()
        }

        "Should name the opponent before the darts and offer the second as the alternative" {
            // GIVEN a two-dart knockout on Bob and a one-dart knockout on Dave with the outer bull
            val routes = listOf(
                KnockoutRoute(opponentId = bob, opponentScore = 81, throws = listOf(triple(20), triple(7))),
                KnockoutRoute(
                    opponentId = dave,
                    opponentScore = 25,
                    throws = listOf(Throw(segment = 25, multiplier = Multiplier.SINGLE)),
                ),
            )

            // WHEN formatting the slot
            val text = knockoutSlotText(routes) { names.getValue(it) }

            // THEN it is a knockout text, the outer bull reads "25" and the slot draws the KO marker itself
            text shouldBe CheckoutSlotText(primary = "Bob: T20 → T7", secondary = "Dave: 25", isKnockout = true)
        }

        "Should keep Bull for the inner bull" {
            // GIVEN a knockout on Bob with the bull
            val routes = listOf(KnockoutRoute(opponentId = bob, opponentScore = 50, throws = listOf(bull())))

            // WHEN formatting the slot
            // THEN the bull keeps the checkout notation
            knockoutSlotText(routes) { names.getValue(it) }?.primary shouldBe "Bob: Bull"
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
            CheckoutSlotText(primary = "Bob: T7", secondary = null, isKnockout = true) to "Knockout Bob: T7",
            CheckoutSlotText(
                primary = "Carol: T20 → T7",
                secondary = "Dave: 25",
                isKnockout = true,
            ) to "Knockout Carol: T20, T7, or knockout Dave: 25",
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

    "fitSlotLine" - {

        data class FitCase(
            val description: String,
            val primaryTextWidth: Int,
            val secondaryTextWidth: Int?,
            val maxWidth: Int,
            val primaryFixedWidth: Int = 0,
            val secondaryFixedWidth: Int = 0,
            val expected: SlotLineFit,
        )

        withData(
            nameFn = { "Should ${it.description}" },
            FitCase(
                description = "keep both at full size when they fit",
                primaryTextWidth = 120,
                secondaryTextWidth = 140,
                maxWidth = 300,
                expected = SlotLineFit(primaryScale = 1f, secondaryScale = 1f),
            ),
            FitCase(
                description = "keep the main suggestion at full size and shrink only the alternative into what's left",
                primaryTextWidth = 190,
                secondaryTextWidth = 140,
                maxWidth = 320,
                expected = SlotLineFit(primaryScale = 1f, secondaryScale = 130f / 140f * 0.97f),
            ),
            FitCase(
                description = "shrink only the alternative's text, not the fixed room around the line",
                primaryTextWidth = 160,
                secondaryTextWidth = 120,
                maxWidth = 320,
                primaryFixedWidth = 20,
                secondaryFixedWidth = 30,
                expected = SlotLineFit(primaryScale = 1f, secondaryScale = 110f / 120f * 0.97f),
            ),
            FitCase(
                description = "keep the alternative down to the floor",
                primaryTextWidth = 200,
                secondaryTextWidth = 100,
                maxWidth = 280,
                expected = SlotLineFit(primaryScale = 1f, secondaryScale = 80f / 100f * 0.97f),
            ),
            FitCase(
                description = "drop the alternative when it would shrink below the floor",
                primaryTextWidth = 200,
                secondaryTextWidth = 150,
                maxWidth = 300,
                expected = SlotLineFit(primaryScale = 1f, secondaryScale = null),
            ),
            FitCase(
                description = "shrink a main suggestion wider than the strip and drop the alternative",
                primaryTextWidth = 400,
                secondaryTextWidth = 200,
                maxWidth = 300,
                primaryFixedWidth = 20,
                expected = SlotLineFit(primaryScale = 280f / 400f * 0.97f, secondaryScale = null),
            ),
            FitCase(
                description = "shrink a lone main suggestion that is too wide",
                primaryTextWidth = 400,
                secondaryTextWidth = null,
                maxWidth = 300,
                expected = SlotLineFit(primaryScale = 300f / 400f * 0.97f, secondaryScale = null),
            ),
        ) { case ->
            // GIVEN the measured text widths, the room that doesn't shrink, and the strip width
            // WHEN fitting the knockout line
            val fit = fitSlotLine(
                primaryTextWidth = case.primaryTextWidth,
                secondaryTextWidth = case.secondaryTextWidth,
                maxWidth = case.maxWidth,
                primaryFixedWidth = case.primaryFixedWidth,
                secondaryFixedWidth = case.secondaryFixedWidth,
            )

            // THEN the main suggestion keeps its size where it fits, and the alternative shrinks into the rest or is
            // dropped
            fit shouldBe case.expected
        }
    }

    "slotSecondaryColor" - {

        data class SecondaryCase(
            val theme: String,
            val scheme: ColorScheme,
            val minRatio: Float,
        )

        withData(
            nameFn = { "Should reach ${it.minRatio}:1 but stay dimmer than the route in ${it.theme} theme" },
            SecondaryCase(theme = "light", scheme = LightColorScheme, minRatio = 6f),
            SecondaryCase(theme = "dark", scheme = DarkColorScheme, minRatio = 8f),
        ) { case ->
            // GIVEN the slot's background and text colour
            val container = case.scheme.tertiaryContainer
            val onContainer = case.scheme.onTertiaryContainer

            // WHEN picking the colour of the "or …" alternative
            val secondary = slotSecondaryColor(onContainer = onContainer, container = container)

            // THEN it is well above WCAG AA on the slot, yet has less contrast than the route itself
            val ratio = contrastRatio(secondary, container)
            ratio shouldBeGreaterThanOrEqual case.minRatio
            ratio shouldBeLessThan contrastRatio(onContainer, container)
        }
    }
})
