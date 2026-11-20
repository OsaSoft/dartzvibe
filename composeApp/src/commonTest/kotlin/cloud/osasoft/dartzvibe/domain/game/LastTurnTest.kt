package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Multiplier.DOUBLE
import cloud.osasoft.dartzvibe.data.model.Multiplier.SINGLE
import cloud.osasoft.dartzvibe.data.model.Multiplier.TRIPLE
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LastTurnTest : FreeSpec({

    val p1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val p2 = Uuid.parse("00000000-0000-0000-0000-000000000002")

    fun t(segment: Int, multiplier: Multiplier = SINGLE) = Throw(segment, multiplier)

    fun engine(config: GameConfig) = GameEngine.fromSession(
        GameSession(
            id = Uuid.parse("00000000-0000-0000-0000-000000000100"),
            config = config,
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = 0L,
        ),
    )

    fun GameEngine.play(vararg throws: Throw): GameEngine =
        throws.fold(this) { engine, throwObj -> engine.addThrow(throwObj.segment, throwObj.multiplier).first }

    fun GameEngine.turn(vararg throws: Throw): GameEngine = play(*throws).endTurn().first

    fun GameEngine.lastTurn(playerId: Uuid): LastTurn = LastTurn.of(
        leg = toGameSession().currentLeg,
        playerId = playerId,
        isRoulette = toGameSession().config.isRoulette,
        parcheesiConfig = config.takeIf { it.gameMode == GameMode.PARCHEESI },
    )

    val classic = GameConfig(
        gameType = GameType.CLASSIC_301,
        gameMode = GameMode.CLASSIC,
        doubleOut = true,
        playerIds = listOf(p1, p2),
        legsToWin = 2,
    )
    val parcheesi = GameConfig(
        gameType = GameType.CLASSIC_301,
        gameMode = GameMode.PARCHEESI,
        doubleOut = false,
        playerIds = listOf(p1, p2),
    )

    "Should label" - {
        data class LabelCase(val name: String, val lastTurn: LastTurn, val isCountUp: Boolean, val expected: String)

        withData(
            nameFn = { it.name },
            LabelCase("Should show a count-down score as a plain number", LastTurn.Score(180), false, "Last: 180"),
            LabelCase("Should show a count-down zero as a plain 0", LastTurn.Score(0), false, "Last: 0"),
            LabelCase("Should show a count-up score with a plus", LastTurn.Score(60), true, "Last: +60"),
            LabelCase("Should show a count-up zero with a plus", LastTurn.Score(0), true, "Last: +0"),
            LabelCase("Should show a count-down bust as BUST", LastTurn.Bust, false, "Last: BUST"),
            LabelCase("Should show a count-up bust as BUST", LastTurn.Bust, true, "Last: BUST"),
            LabelCase("Should show a bounce as BOUNCE", LastTurn.Bounce, true, "Last: BOUNCE"),
            LabelCase("Should show no turn yet as an en dash, counting down", LastTurn.None, false, "Last: –"),
            LabelCase("Should show no turn yet as an en dash, counting up", LastTurn.None, true, "Last: –"),
        ) { (_, lastTurn, isCountUp, expected) ->
            lastTurn.label(isCountUp) shouldBe expected
        }
    }

    "Should derive from the leg" - {
        "Should give None when the player has no turn in the leg" {
            // GIVEN a leg where only P2 has thrown
            val leg = Leg(turns = listOf(Turn(p2, listOf(t(20)), 301, 281)))

            // WHEN / THEN
            LastTurn.of(leg, p1, isRoulette = false) shouldBe LastTurn.None
        }

        "Should give Bust for a busted turn, not None" {
            // GIVEN P1's last turn busted after an earlier scoring turn
            val leg = Leg(
                turns = listOf(
                    Turn(p1, listOf(t(20)), 301, 281),
                    Turn(p1, listOf(t(20, TRIPLE), t(20, TRIPLE)), 41, 41, isBust = true),
                ),
            )

            // WHEN / THEN
            LastTurn.of(leg, p1, isRoulette = false) shouldBe LastTurn.Bust
        }

        "Should skip phantom turns and keep the player's own last real turn" {
            // GIVEN P1 scored 20, then was knocked out (the phantom sits before P2's knocking turn)
            val leg = Leg(
                turns = listOf(
                    Turn(p1, listOf(t(20)), 0, 20),
                    Turn(p1, emptyList(), 20, 0, isPhantom = true),
                    Turn(p2, listOf(t(20)), 0, 20),
                ),
            )

            // WHEN / THEN
            LastTurn.of(leg, p1, isRoulette = false) shouldBe LastTurn.Score(20)
        }

        "Should give None when the player's only turn is a phantom" {
            // GIVEN P1 was knocked out without a real turn of their own in the leg
            val leg = Leg(turns = listOf(Turn(p1, emptyList(), 20, 0, isPhantom = true)))

            // WHEN / THEN
            LastTurn.of(leg, p1, isRoulette = false) shouldBe LastTurn.None
        }

        "Should count Roulette points as the score gained, not the darts' total" {
            // GIVEN a Roulette turn of T7 (60 on the darts) worth 3 points
            val leg = Leg(turns = listOf(Turn(p1, listOf(t(7, TRIPLE)), 4, 7)))

            // WHEN / THEN
            LastTurn.of(leg, p1, isRoulette = true) shouldBe LastTurn.Score(3)
        }
    }

    "Should follow a Classic game" - {
        "Should show 180 after three treble 20s, and an en dash for a player yet to throw" {
            // GIVEN / WHEN P1 throws 180
            val engine = engine(classic).turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))

            // THEN
            engine.lastTurn(p1).label(isCountUp = false) shouldBe "Last: 180"
            engine.lastTurn(p2).label(isCountUp = false) shouldBe "Last: –"
        }

        "Should show 0 after three misses" {
            // GIVEN / WHEN P1 throws 180, P2 misses three times
            val engine = engine(classic)
                .turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))
                .turn(t(0), t(0), t(0))

            // THEN
            engine.lastTurn(p2).label(isCountUp = false) shouldBe "Last: 0"
        }

        "Should show BUST after a bust" {
            // GIVEN P1 on 121
            val start = engine(classic)
                .turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))
                .turn(t(0), t(0), t(0))

            // WHEN P1 leaves 1 with double out
            val engine = start.turn(t(20, TRIPLE), t(20, TRIPLE))

            // THEN
            engine.lastTurn(p1).label(isCountUp = false) shouldBe "Last: BUST"
        }

        "Should show an en dash for every player at the start of a new leg" {
            // GIVEN / WHEN P1 checks out 301 to win the first leg of two
            val engine = engine(classic)
                .turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))
                .turn(t(0), t(0), t(0))
                .turn(t(20, TRIPLE), t(11, TRIPLE), t(14, DOUBLE))

            // THEN the second leg has begun and nobody has a last turn in it
            engine.toGameSession().currentLegIndex shouldBe 1
            engine.lastTurn(p1) shouldBe LastTurn.None
            engine.lastTurn(p2) shouldBe LastTurn.None
        }
    }

    "Should follow a Parcheesi knockout" - {
        "Should keep the knocked-out player's own last turn, mid-turn and after it" {
            // GIVEN P1 scores 20
            val afterP1 = engine(parcheesi).turn(t(20), t(0), t(0))

            // WHEN P2 lands on 20, knocking P1 out
            val midTurn = afterP1.play(t(20))
            val afterP2 = midTurn.play(t(0), t(0)).endTurn().first

            // THEN P1 still shows their own +20, not the phantom's empty turn
            midTurn.toGameSession().currentLeg.turns.any { it.isPhantom } shouldBe true
            midTurn.lastTurn(p1).label(isCountUp = true) shouldBe "Last: +20"
            afterP2.lastTurn(p1).label(isCountUp = true) shouldBe "Last: +20"
            afterP2.lastTurn(p2).label(isCountUp = true) shouldBe "Last: +20"
        }
    }

    "Should follow Parcheesi bounces and non-counting darts" - {
        // P1 to 300 (five T20s over two turns), P2 missing in between
        fun at300(config: GameConfig): GameEngine = engine(config)
            .turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))
            .turn(t(0))
            .turn(t(20, TRIPLE), t(20, TRIPLE))
            .turn(t(0))

        "Should show BOUNCE after a turn that overshot the target" {
            // GIVEN P1 at 300
            val engine = at300(parcheesi)

            // WHEN P1 throws S20 (bounces to 282) and the turn ends
            val afterBounce = engine.turn(t(20))

            // THEN P1's card reads BOUNCE, not +20
            afterBounce.getPlayerScore(p1) shouldBe 282
            afterBounce.lastTurn(p1).label(isCountUp = true) shouldBe "Last: BOUNCE"
        }

        "Should keep +N for a normal Parcheesi turn" {
            // GIVEN / WHEN P1 reaches 300 normally
            val engine = at300(parcheesi)

            // THEN the last turn reads +120
            engine.lastTurn(p1).label(isCountUp = true) shouldBe "Last: +120"
        }

        "Should not count a dart that hit the target without the double under double-out" {
            // GIVEN P1 at 281 under double-out
            val doubleOut = parcheesi.copy(doubleOut = true)
            val at281 = engine(doubleOut)
                .turn(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE))
                .turn(t(0))
                .turn(t(20, TRIPLE), t(20, DOUBLE), t(1))
                .turn(t(0))
            at281.getPlayerScore(p1) shouldBe 281

            // WHEN P1 throws S20 (would be 301 without a double), S1 and a miss
            val after = at281.turn(t(20), t(1), t(0))

            // THEN only the S1 counted
            after.getPlayerScore(p1) shouldBe 282
            after.lastTurn(p1).label(isCountUp = true) shouldBe "Last: +1"
        }
    }

    "Should show Roulette points with a plus" {
        // GIVEN a Roulette game whose first target is 7
        val roulette = GameConfig(
            gameType = GameType.ROULETTE_ROUNDS,
            gameMode = GameMode.ROULETTE,
            doubleOut = false,
            playerIds = listOf(p1, p2),
            rouletteTargetSegments = listOf(7, 12),
            rouletteRounds = 2,
        )

        // WHEN P1 hits T7
        val engine = engine(roulette).turn(t(7, TRIPLE))

        // THEN
        engine.lastTurn(p1).label(isCountUp = true) shouldBe "Last: +3"
    }
})
