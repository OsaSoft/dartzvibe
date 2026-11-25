package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual

class ResultColorsTest : FreeSpec({

    /** Surfaces [ResultColors.success] sits on: Game Details leg cards and the Checkout Practice round rows. */
    fun backgrounds(scheme: ColorScheme): Map<String, Color> = mapOf(
        "surface (leg cards)" to scheme.surface,
        "Checkout Practice round row" to scheme.surfaceVariant.copy(alpha = 0.5f).compositeOver(scheme.surface),
        "surfaceVariant" to scheme.surfaceVariant,
        "background" to scheme.background,
    )

    data class ContrastCase(
        val theme: String,
        val backgroundName: String,
        val text: Color,
        val background: Color,
    )

    val cases = listOf(
        "light" to (LightResultColors to LightColorScheme),
        "dark" to (DarkResultColors to DarkColorScheme),
    ).flatMap { (theme, colors) ->
        val (result, scheme) = colors
        backgrounds(scheme).map { (name, background) -> ContrastCase(theme, name, result.success, background) }
    }

    "Success text" - {

        withData(
            nameFn = { "Should reach 4.5:1 on ${it.backgroundName} in ${it.theme} theme" },
            cases,
        ) { case ->
            // GIVEN the theme's success colour and a surface it is drawn on
            // WHEN measuring their contrast
            val ratio = contrastRatio(case.text, case.background)

            // THEN it meets WCAG AA for normal-size text
            ratio shouldBeGreaterThanOrEqual 4.5f
        }

        "Should stay green in both themes" {
            // GIVEN the success colours
            listOf(LightResultColors, DarkResultColors).map { it.success }.forEach { success ->
                // WHEN comparing the channels
                // THEN green clearly dominates, so PASS reads as positive next to the red FAIL
                (success.green - maxOf(success.red, success.blue)) shouldBeGreaterThanOrEqual 0.2f
            }
        }
    }
})
