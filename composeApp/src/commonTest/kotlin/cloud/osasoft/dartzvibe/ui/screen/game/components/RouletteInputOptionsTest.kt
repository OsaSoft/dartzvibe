package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.Multiplier
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class RouletteInputOptionsTest : FreeSpec({

    data class TargetCase(
        val target: Int,
        val kind: RouletteInputKind,
        val expectedMultiplier: Multiplier,
        val expectedLabel: String,
        val expectedPoints: Int,
        val expectedFullLabel: String,
    ) {
        override fun toString(): String = "target $target, $kind -> $expectedFullLabel"
    }

    val allTargets = (1..20).toList() + 25

    "Roulette input options" - {
        "Should list Miss, Hit, Double and Triple in display order" {
            // GIVEN a normal target
            val target = 7

            // WHEN the options are built
            val options = rouletteInputOptions(target)

            // THEN there are exactly four buttons in the expected order
            options.map { it.kind } shouldBe listOf(
                RouletteInputKind.MISS,
                RouletteInputKind.HIT,
                RouletteInputKind.DOUBLE,
                RouletteInputKind.TRIPLE,
            )
        }

        "Should aim each scoring button at the target with matching points" - {
            withData(
                TargetCase(7, RouletteInputKind.HIT, Multiplier.SINGLE, "Hit 7", 1, "Hit 7 · +1"),
                TargetCase(7, RouletteInputKind.DOUBLE, Multiplier.DOUBLE, "Double 7", 2, "Double 7 · +2"),
                TargetCase(7, RouletteInputKind.TRIPLE, Multiplier.TRIPLE, "Triple 7", 3, "Triple 7 · +3"),
                TargetCase(20, RouletteInputKind.DOUBLE, Multiplier.DOUBLE, "Double 20", 2, "Double 20 · +2"),
                TargetCase(20, RouletteInputKind.TRIPLE, Multiplier.TRIPLE, "Triple 20", 3, "Triple 20 · +3"),
                TargetCase(25, RouletteInputKind.HIT, Multiplier.SINGLE, "Outer bull", 1, "Outer bull · +1"),
                TargetCase(25, RouletteInputKind.DOUBLE, Multiplier.DOUBLE, "Bullseye", 2, "Bullseye · +2"),
            ) { case ->
                // GIVEN the options for the target
                val options = rouletteInputOptions(case.target)

                // WHEN the button of the given kind is picked
                val option = options.single { it.kind == case.kind }

                // THEN it records a dart on the target with the expected multiplier
                option.segment shouldBe case.target
                option.multiplier shouldBe case.expectedMultiplier
                // AND it shows the target and its points
                option.label shouldBe case.expectedLabel
                option.points shouldBe case.expectedPoints
                option.fullLabel shouldBe case.expectedFullLabel
                // AND it is enabled
                option.enabled shouldBe true
            }
        }

        "Should record a miss for the Miss button" - {
            withData(7, 20, 25) { target ->
                // GIVEN the options for the target
                val options = rouletteInputOptions(target)

                // WHEN the Miss button is picked
                val miss = options.single { it.kind == RouletteInputKind.MISS }

                // THEN it records a miss (segment 0, single) like the keypad Miss button
                miss.segment shouldBe 0
                miss.multiplier shouldBe Multiplier.SINGLE
                // AND it scores nothing and says so
                miss.points shouldBe 0
                miss.fullLabel shouldBe "Miss · +0"
                miss.enabled shouldBe true
            }
        }

        "Should disable Triple for the bull target" {
            // GIVEN the bull target
            val target = 25

            // WHEN the options are built
            val triple = rouletteInputOptions(target).single { it.kind == RouletteInputKind.TRIPLE }

            // THEN Triple is disabled because there is no triple bull
            triple.enabled shouldBe false
            triple.points shouldBe 0
            triple.label shouldBe "Triple bull"
            triple.pointsLabel shouldBe "No triple bull"
        }

        "Should enable Triple exactly when the target is not the bull" - {
            withData(allTargets) { target ->
                // GIVEN the options for the target
                val options = rouletteInputOptions(target)

                // WHEN the Triple button is picked
                val triple = options.single { it.kind == RouletteInputKind.TRIPLE }

                // THEN it is enabled for every target except the bull
                triple.enabled shouldBe (target != 25)
                // AND Miss, Hit and Double are always enabled
                options.filter { it.kind != RouletteInputKind.TRIPLE }.all { it.enabled } shouldBe true
            }
        }
    }
})
