package cloud.osasoft.dartzvibe.domain.game

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class RouletteRulesTest : FreeSpec({

    data class TargetCase(
        val name: String,
        val targets: List<Int>?,
        val roundIndex: Int,
        val expected: Int,
    ) {
        override fun toString(): String = name
    }

    "Roulette target" - {
        "Should pick the round's target, wrap around and fall back to 1" - {
            withData(
                TargetCase("first round", listOf(7, 13, 25), roundIndex = 0, expected = 7),
                TargetCase("last listed round", listOf(7, 13, 25), roundIndex = 2, expected = 25),
                TargetCase("wraps past the end", listOf(7, 13, 25), roundIndex = 4, expected = 13),
                TargetCase("missing list", null, roundIndex = 3, expected = 1),
                TargetCase("empty list", emptyList(), roundIndex = 0, expected = 1),
            ) { case ->
                // GIVEN a target list and a round index
                // WHEN the target is resolved
                val target = RouletteRules.targetFor(case.targets, case.roundIndex)

                // THEN it matches the expected segment
                target shouldBe case.expected
            }
        }
    }
})
