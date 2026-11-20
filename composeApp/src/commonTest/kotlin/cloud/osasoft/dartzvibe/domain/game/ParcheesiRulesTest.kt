package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Multiplier.DOUBLE
import cloud.osasoft.dartzvibe.data.model.Multiplier.SINGLE
import cloud.osasoft.dartzvibe.data.model.Throw
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ParcheesiRulesTest : FreeSpec({

    fun t(segment: Int, multiplier: Multiplier = SINGLE) = Throw(segment, multiplier)

    val config = GameConfig(
        gameType = GameType.CLASSIC_301,
        gameMode = GameMode.PARCHEESI,
        doubleOut = true,
        playerIds = listOf(Uuid.parse("00000000-0000-0000-0000-000000000001")),
    )

    data class Case(
        val name: String,
        val config: GameConfig,
        val startScore: Int,
        val darts: List<Throw>,
        val doubledIn: Boolean,
        val expected: List<Boolean>,
    )

    "Should mark the darts that didn't count" - {
        withData(
            nameFn = { it.name },
            Case("Should mark S20 from 281 under double-out", config, 281, listOf(t(20)), true, listOf(true)),
            Case("Should not mark D10 from 281", config, 281, listOf(t(10, DOUBLE)), true, listOf(false)),
            Case(
                "Should mark only the dart that reached the target from where the turn stood",
                config,
                271,
                listOf(t(10), t(20), t(0)),
                true,
                listOf(false, true, false),
            ),
            Case(
                "Should not mark an exact hit without double-out",
                config.copy(doubleOut = false),
                281,
                listOf(t(20)),
                true,
                listOf(false),
            ),
            Case(
                "Should not mark darts before doubling in, which score nothing",
                config.copy(doubleIn = true),
                281,
                listOf(t(20), t(5, DOUBLE), t(1)),
                false,
                listOf(false, false, false),
            ),
            Case(
                "Should mark after doubling in mid-turn",
                config.copy(doubleIn = true),
                261,
                listOf(t(1), t(5, DOUBLE), t(10, Multiplier.TRIPLE)),
                false,
                listOf(false, false, true),
            ),
        ) { (_, caseConfig, startScore, darts, doubledIn, expected) ->
            // GIVEN the turn's start score and darts
            // WHEN replaying them
            val flags = ParcheesiRules.nonCountingDarts(startScore, darts, caseConfig, doubledIn)

            // THEN only the darts that hit the target without the double are marked
            flags shouldBe expected
        }
    }
})
