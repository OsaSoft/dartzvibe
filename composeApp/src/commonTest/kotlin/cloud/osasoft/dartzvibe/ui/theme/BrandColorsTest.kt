package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

class BrandColorsTest : FreeSpec({

    /** Neutral surfaces [BrandColors.text] sits on: screens, cards, dialogs and the current player's red-tinted card. */
    fun textBackgrounds(scheme: ColorScheme): Map<String, Color> = mapOf(
        "background" to scheme.background,
        "surface" to scheme.surface,
        "surfaceVariant (score and Cricket cards)" to scheme.surfaceVariant,
        "surfaceContainerHigh (dialogs)" to scheme.surfaceContainerHigh,
        "current player's card" to currentPlayerCardColor(scheme),
        "leaderboard leader's card" to scheme.primaryContainer.copy(alpha = 0.5f).compositeOver(scheme.background),
    )

    /** Darker surfaces [BrandColors.textOnContainer] sits on: default cards and the Cricket PTS row's red tint. */
    fun containerBackgrounds(
        scheme: ColorScheme,
        brand: BrandColors,
    ): Map<String, Color> = mapOf(
        "surfaceContainerHighest (default cards)" to scheme.surfaceContainerHighest,
        "Cricket PTS row tint" to brand.highlight.copy(alpha = 0.1f).compositeOver(scheme.surfaceVariant),
    )

    data class ContrastCase(
        val theme: String,
        val token: String,
        val text: Color,
        val backgroundName: String,
        val background: Color,
    )

    val cases = listOf(
        "light" to (LightBrandColors to LightColorScheme),
        "dark" to (DarkBrandColors to DarkColorScheme),
    )
        .flatMap { (theme, colors) ->
            val (brand, scheme) = colors
            val onText = (textBackgrounds(scheme) + containerBackgrounds(scheme, brand)).map { (name, background) ->
                // textOnContainer is at least as strong as text, so it must pass everywhere text does too
                ContrastCase(theme, "textOnContainer", brand.textOnContainer, name, background)
            }
            textBackgrounds(scheme).map { (name, background) ->
                ContrastCase(theme, "text", brand.text, name, background)
            } + onText
        }

    "Brand text" - {

        withData(
            nameFn = { "Should reach 4.5:1 for ${it.token} on ${it.backgroundName} in ${it.theme} theme" },
            cases,
        ) { case ->
            // GIVEN the theme's brand text colour and a surface it is drawn on
            // WHEN measuring their contrast
            val ratio = contrastRatio(case.text, case.background)

            // THEN it meets WCAG AA for normal-size text
            ratio shouldBeGreaterThanOrEqual 4.5f
        }

        "Should stay red in both themes" {
            // GIVEN the brand text colours
            listOf(LightBrandColors, DarkBrandColors).flatMap {
                listOf(it.text, it.textOnContainer, it.highlight)
            }.forEach { text ->
                // WHEN comparing the channels
                // THEN red clearly dominates, so the text keeps the brand's red accent
                (text.red - maxOf(text.green, text.blue)) shouldBeGreaterThanOrEqual 0.4f
            }
        }

        "Should leave the brand fill colour and light theme text unchanged" {
            // GIVEN the colour schemes
            // WHEN reading the fill colour of buttons and badges, and light theme's brand text
            // THEN both themes keep the dartboard red with white text on it, and light text stays that red
            LightColorScheme.primary shouldBe Color(0xFFC62828)
            DarkColorScheme.primary shouldBe Color(0xFFC62828)
            DarkColorScheme.onPrimary shouldBe Color.White
            LightBrandColors.text shouldBe LightColorScheme.primary
            LightBrandColors.highlight shouldBe LightColorScheme.primary
        }
    }

    "Cricket PTS label" - {

        withData(
            nameFn = { (theme, _) -> "Should reach 5:1 on its strip in $theme theme" },
            "light" to (LightBrandColors to LightColorScheme),
            "dark" to (DarkBrandColors to DarkColorScheme),
        ) { (_, colors) ->
            // GIVEN the PTS strip: a 10% tint of the highlight red over the Cricket card
            val (brand, scheme) = colors
            val strip = brand.highlight.copy(alpha = 0.1f).compositeOver(scheme.surfaceVariant)

            // WHEN measuring the small bold PTS label (and the points, same colour) on it
            val ratio = contrastRatio(brand.textOnContainer, strip)

            // THEN it clears AA with a margin for its small size
            ratio shouldBeGreaterThanOrEqual 5f
        }
    }

    "Highlight" - {

        "Should match the score text in dark theme, so the border, strip and score read as one red" {
            // GIVEN the dark theme's brand colours
            // WHEN comparing the highlight border/strip colour with the score text
            // THEN they are the same red
            DarkBrandColors.highlight shouldBe DarkBrandColors.text
        }
    }

    "Current player's card" - {

        withData(
            nameFn = { (theme, _) ->
                "Should be opaque in $theme theme, so its shadow can't show through as a grey ring"
            },
            "light" to LightColorScheme,
            "dark" to DarkColorScheme,
        ) { (_, scheme) ->
            // GIVEN the theme's colour scheme
            // WHEN reading the current player's card fill
            val fill = currentPlayerCardColor(scheme)

            // THEN it is fully opaque and still the faint red tint over the screen background
            fill.alpha shouldBe 1f
            fill shouldBe scheme.primaryContainer.copy(alpha = 0.3f).compositeOver(scheme.background)
        }
    }

    "contrastRatio" - {

        "Should be 21 for black on white and 1 for a colour on itself" {
            // GIVEN the extremes
            // WHEN measuring contrast
            // THEN black on white is the maximum, in either order, and equal colours are the minimum
            contrastRatio(Color.Black, Color.White) shouldBe (21f plusOrMinus 0.01f)
            contrastRatio(Color.White, Color.Black) shouldBe (21f plusOrMinus 0.01f)
            contrastRatio(Color(0xFFC62828), Color(0xFFC62828)) shouldBe (1f plusOrMinus 0.001f)
        }

        "Should match the WCAG value for the brand red on white" {
            // GIVEN the brand red on white (5.62:1 by the WCAG formula)
            // WHEN measuring contrast
            // THEN it matches
            contrastRatio(Color(0xFFC62828), Color.White) shouldBe (5.62f plusOrMinus 0.01f)
        }
    }
})
