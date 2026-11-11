package cloud.osasoft.dartzvibe.domain.history

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.domain.game.GameEngine
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class GameHistoryContextTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun single(segment: Int) = Throw(segment, Multiplier.SINGLE)
    fun double(segment: Int) = Throw(segment, Multiplier.DOUBLE)
    fun triple(segment: Int) = Throw(segment, Multiplier.TRIPLE)
    val miss = Throw(0, Multiplier.SINGLE)

    fun turn(
        playerId: Uuid,
        vararg throws: Throw,
        before: Int = 0,
        after: Int = 0,
    ) = Turn(playerId = playerId, throws = throws.toList(), scoreBeforeTurn = before, scoreAfterTurn = after)

    fun session(config: GameConfig) = GameSession(id = sessionId, config = config, startedAt = 0L)

    /** Throws [darts] for the current player, then ends the turn. */
    fun GameEngine.playTurn(vararg darts: Throw): GameEngine = darts
        .fold(this) { engine, dart -> engine.addThrow(dart.segment, dart.multiplier).first }
        .endTurn()
        .first

    // ---- Roulette ----

    fun rouletteConfig(targets: List<Int>?, playerIds: List<Uuid> = listOf(playerId1, playerId2)) = GameConfig(
        gameType = GameType.ROULETTE_ROUNDS,
        gameMode = GameMode.ROULETTE,
        doubleOut = false,
        playerIds = playerIds,
        rouletteTargetSegments = targets,
        rouletteRounds = 10,
    )

    data class RouletteCase(
        val name: String,
        val dart: Throw,
        val target: Int,
        val expected: DartOutcome,
    ) {
        override fun toString(): String = name
    }

    "Roulette dart outcome" - {
        "Should judge each dart against the round's target only" - {
            withData(
                RouletteCase("single on target", single(7), 7, DartOutcome.HIT),
                RouletteCase("double on target", double(7), 7, DartOutcome.HIT),
                RouletteCase("triple on target", triple(7), 7, DartOutcome.HIT),
                RouletteCase("triple off target", triple(20), 7, DartOutcome.NO_EFFECT),
                RouletteCase("neighbour single off target", single(8), 7, DartOutcome.NO_EFFECT),
                RouletteCase("miss", miss, 7, DartOutcome.MISS),
                RouletteCase("outer bull on bull target", single(25), 25, DartOutcome.HIT),
                RouletteCase("bullseye on bull target", double(25), 25, DartOutcome.HIT),
                RouletteCase("bullseye off target", double(25), 20, DartOutcome.NO_EFFECT),
            ) { case ->
                // GIVEN a dart and the round's target
                // WHEN its outcome is derived
                val outcome = rouletteDartOutcome(case.dart, case.target)

                // THEN it matches the engine's scoring rule
                outcome shouldBe case.expected
            }
        }
    }

    "Roulette turn contexts" - {
        "Should give every turn its round, target and per-dart outcomes" {
            // GIVEN a 2-player Roulette leg with targets 7 then 13
            val config = rouletteConfig(targets = listOf(7, 13))
            val leg = Leg(
                turns = listOf(
                    turn(playerId1, triple(7), single(8), miss, before = 0, after = 3),
                    turn(playerId2, miss, miss, double(7), before = 0, after = 2),
                    turn(playerId1, single(7), single(13), double(13), before = 3, after = 6),
                ),
            )

            // WHEN the contexts are derived
            val contexts = rouletteTurnContexts(leg, config)

            // THEN rounds advance after every player has thrown
            contexts.map { it.round } shouldBe listOf(1, 1, 2)
            // AND each round has its own target
            contexts.map { it.target } shouldBe listOf(7, 7, 13)
            // AND darts are judged against their own round's target
            contexts[0].dartOutcomes shouldBe listOf(DartOutcome.HIT, DartOutcome.NO_EFFECT, DartOutcome.MISS)
            contexts[1].dartOutcomes shouldBe listOf(DartOutcome.MISS, DartOutcome.MISS, DartOutcome.HIT)
            contexts[2].dartOutcomes shouldBe listOf(DartOutcome.NO_EFFECT, DartOutcome.HIT, DartOutcome.HIT)
            // AND the stored points are kept
            contexts.map { it.points } shouldBe listOf(3, 2, 3)
        }

        "Should agree with the engine on which darts scored" {
            // GIVEN a Roulette game played through the engine
            val config = rouletteConfig(targets = listOf(5, 25, 9))
            val engine = GameEngine.fromSession(session(config))
                .playTurn(triple(5), single(5), single(20))
                .playTurn(miss, double(5), single(1))
                .playTurn(double(25), single(25), triple(19))
                .playTurn(single(25), miss, miss)
            val leg = engine.toGameSession().legs[0]

            // WHEN the contexts are derived
            val contexts = rouletteTurnContexts(leg, config)

            // THEN the hit darts' multipliers add up to the points the engine stored for every turn
            contexts.forEach { context ->
                val hitPoints = context.turn.throws
                    .zip(context.dartOutcomes)
                    .filter { (_, outcome) -> outcome == DartOutcome.HIT }
                    .sumOf { (dart, _) -> dart.multiplier.value }
                hitPoints shouldBe context.points
            }
            contexts.map { it.points } shouldBe listOf(4, 2, 3, 1)
        }

        "Should wrap around the target list with 3 players" {
            // GIVEN a 3-player Roulette leg with only two targets
            val config = rouletteConfig(targets = listOf(7, 13), playerIds = listOf(playerId1, playerId2, playerId3))
            val players = listOf(playerId1, playerId2, playerId3)
            val leg = Leg(turns = (0 until 7).map { index -> turn(players[index % 3], single(7)) })

            // WHEN the contexts are derived
            val contexts = rouletteTurnContexts(leg, config)

            // THEN rounds advance every 3 turns
            contexts.map { it.round } shouldBe listOf(1, 1, 1, 2, 2, 2, 3)
            // AND the third round wraps back to the first target
            contexts.map { it.target } shouldBe listOf(7, 7, 7, 13, 13, 13, 7)
            contexts.map { it.dartOutcomes.single() } shouldBe listOf(
                DartOutcome.HIT,
                DartOutcome.HIT,
                DartOutcome.HIT,
                DartOutcome.NO_EFFECT,
                DartOutcome.NO_EFFECT,
                DartOutcome.NO_EFFECT,
                DartOutcome.HIT,
            )
        }

        "Should fall back to target 1 when no targets were stored" {
            // GIVEN an old Roulette config without targets
            val config = rouletteConfig(targets = null)
            val leg = Leg(turns = listOf(turn(playerId1, single(1), single(2), miss, after = 1)))

            // WHEN the contexts are derived
            val contexts = rouletteTurnContexts(leg, config)

            // THEN target 1 is used, as in the engine
            contexts.single().target shouldBe 1
            contexts.single().dartOutcomes shouldBe listOf(DartOutcome.HIT, DartOutcome.NO_EFFECT, DartOutcome.MISS)
        }

        "Should return no rows for an empty leg" {
            // GIVEN a leg without turns
            // WHEN the contexts are derived
            val contexts = rouletteTurnContexts(Leg(), rouletteConfig(targets = listOf(3)))

            // THEN there are none
            contexts.shouldBeEmpty()
        }
    }

    "History dart label" - {
        "Should keep the multiplier readable" - {
            withData(
                mapOf(
                    "miss" to (miss to "Miss"),
                    "single" to (single(7) to "7"),
                    "double" to (double(7) to "D7"),
                    "triple" to (triple(7) to "T7"),
                    "outer bull" to (single(25) to "25"),
                    "bullseye" to (double(25) to "Bull"),
                    "legacy bullseye" to (single(50) to "Bull"),
                ),
            ) { (dart, expected) ->
                // GIVEN a dart
                // WHEN it is labeled
                // THEN the label shows the multiplier
                historyDartLabel(dart) shouldBe expected
            }
        }
    }

    "History dart description" - {
        "Should name the dart and its outcome for screen readers" - {
            withData(
                mapOf(
                    "hit" to Triple(triple(7), DartOutcome.HIT, "T7, hit"),
                    "no effect" to Triple(single(5), DartOutcome.NO_EFFECT, "5, no effect"),
                    "miss" to Triple(miss, DartOutcome.MISS, "Miss"),
                    "bullseye hit" to Triple(double(25), DartOutcome.HIT, "Bull, hit"),
                ),
            ) { (dart, outcome, expected) ->
                // GIVEN a dart and its outcome
                // WHEN it is described
                // THEN the description names both
                historyDartDescription(dart, outcome) shouldBe expected
            }
        }
    }

    // ---- Cricket ----

    fun cricketConfig(
        playerIds: List<Uuid> = listOf(playerId1, playerId2),
        segments: CricketSegments? = CricketSegments.standard(),
        gameType: GameType = GameType.CRICKET_REGULAR,
        legsToWin: Int = 1,
    ) = GameConfig(
        gameType = gameType,
        gameMode = GameMode.CRICKET,
        doubleOut = false,
        playerIds = playerIds,
        legsToWin = legsToWin,
        cricketSegments = segments,
    )

    fun cricketLeg(vararg turns: Turn) = Leg(turns = turns.toList())

    /** Plays [turns] (one list per turn) through the engine and returns every dart's [ThrowResult]. */
    fun playCricket(config: GameConfig, turns: List<List<Throw>>): Pair<GameSession, List<ThrowResult>> {
        val (engine, results) = turns.fold(
            GameEngine.fromSession(session(config)) to emptyList<ThrowResult>(),
        ) { (turnStart, recorded), darts ->
            val (afterThrows, turnRecorded) = darts.fold(turnStart to recorded) { (engine, acc), dart ->
                val (next, result) = engine.addThrow(dart.segment, dart.multiplier)
                next to acc + result
            }
            afterThrows.endTurn().first to turnRecorded
        }
        return engine.toGameSession() to results
    }

    /**
     * Replays every leg and checks each dart against the engine's [ThrowResult] (a winning dart must
     * count), and each player's replayed totals against the leg's stored Cricket state.
     */
    fun assertCricketParity(played: GameSession, engineResults: List<ThrowResult>) {
        val legEffects = played.legs.map { leg -> leg to cricketTurnEffects(leg, played.config) }
        val replayedDarts = legEffects.flatMap { (_, effects) -> effects.flatMap { it.darts } }
        replayedDarts shouldHaveSize engineResults.size
        replayedDarts.zip(engineResults).forEach { (dart, result) ->
            when (result) {
                is ThrowResult.CricketMarks ->
                    (dart.marksAdded to dart.pointsScored) shouldBe (result.marksAdded to result.pointsScored)

                is ThrowResult.CricketWin -> dart.outcome shouldBe DartOutcome.HIT

                else -> error("Unexpected Cricket throw result: $result")
            }
        }
        legEffects.forEach { (leg, effects) ->
            val storedState = leg.cricketState ?: return@forEach
            played.config.playerIds.forEach { playerId ->
                val playerEffects = effects.filter { it.turn.playerId == playerId }
                playerEffects.sumOf { it.pointsScored } shouldBe storedState.getPlayerState(playerId).points
                playerEffects.sumOf { it.marksAdded } shouldBe storedState.getPlayerState(playerId).marks.values.sum()
            }
        }
    }

    "Cricket turn effects" - {
        "Should count marks added per dart and per turn" {
            // GIVEN a turn with a single 20, a double 19 and a single 18
            val leg = cricketLeg(turn(playerId1, single(20), double(19), single(18)))

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, cricketConfig()).single()

            // THEN every dart added marks and counted
            effect.darts.map { it.marksAdded } shouldBe listOf(1, 2, 1)
            effect.darts.map { it.outcome } shouldBe List(3) { DartOutcome.HIT }
            // AND the turn added 4 marks and no points
            effect.marksAdded shouldBe 4
            effect.pointsScored shouldBe 0
            effect.closedSegments.shouldBeEmpty()
        }

        "Should report numbers closed during the turn" {
            // GIVEN a player with one mark on 20 who then hits a double 20, a bullseye and an outer bull
            val leg = cricketLeg(
                turn(playerId1, single(20), miss, miss),
                turn(playerId2, miss, miss, miss),
                turn(playerId1, double(20), double(25), single(25)),
            )

            // WHEN the turn effects are replayed
            val effects = cricketTurnEffects(leg, cricketConfig())

            // THEN the third turn closed 20 and bull
            effects[2].closedSegments shouldBe listOf(20, 25)
            effects[2].marksAdded shouldBe 5
            // AND earlier turns closed nothing
            effects[0].closedSegments.shouldBeEmpty()
        }

        "Should score overflow points while an opponent still has the number open" {
            // GIVEN player 1 closed 20 and player 2 has not
            val leg = cricketLeg(
                turn(playerId1, triple(20), miss, miss),
                turn(playerId2, single(19), miss, miss),
                turn(playerId1, triple(20), single(20), single(5)),
            )

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, cricketConfig())[2]

            // THEN darts on the closed 20 score points and count
            effect.darts.map { it.pointsScored } shouldBe listOf(60, 20, 0)
            effect.darts.map { it.outcome } shouldBe listOf(DartOutcome.HIT, DartOutcome.HIT, DartOutcome.NO_EFFECT)
            // AND no marks were added
            effect.marksAdded shouldBe 0
            effect.pointsScored shouldBe 80
        }

        "Should split a dart into closing marks and overflow points" {
            // GIVEN player 1 has two marks on 20
            val leg = cricketLeg(
                turn(playerId1, double(20), miss, miss),
                turn(playerId2, miss, miss, miss),
                turn(playerId1, triple(20), miss, miss),
            )

            // WHEN the turn effects are replayed
            val dart = cricketTurnEffects(leg, cricketConfig())[2].darts[0]

            // THEN the triple adds the one missing mark and scores the other two
            dart.marksAdded shouldBe 1
            dart.pointsScored shouldBe 40
        }

        "Should not score once everyone has closed the number" {
            // GIVEN both players closed 20
            val leg = cricketLeg(
                turn(playerId1, triple(20), miss, miss),
                turn(playerId2, triple(20), miss, miss),
                turn(playerId1, triple(20), single(20), miss),
            )

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, cricketConfig())[2]

            // THEN those darts did nothing and are muted
            effect.darts.map { it.outcome } shouldBe listOf(
                DartOutcome.NO_EFFECT,
                DartOutcome.NO_EFFECT,
                DartOutcome.MISS,
            )
            effect.pointsScored shouldBe 0
            effect.marksAdded shouldBe 0
        }

        "Should score while any one of several opponents is still open" {
            // GIVEN 3 players where player 2 closed 20 but player 3 has not
            val config = cricketConfig(playerIds = listOf(playerId1, playerId2, playerId3))
            val leg = cricketLeg(
                turn(playerId1, triple(20), miss, miss),
                turn(playerId2, triple(20), miss, miss),
                turn(playerId3, miss, miss, miss),
                turn(playerId1, single(20), miss, miss),
            )

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, config)[3]

            // THEN player 1 still scores on 20
            effect.pointsScored shouldBe 20
        }

        "Should mute non-Cricket numbers and misses" {
            // GIVEN a turn on 5, 14 and a miss
            val leg = cricketLeg(turn(playerId1, triple(5), single(14), miss))

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, cricketConfig()).single()

            // THEN none of the darts counted
            effect.darts.map { it.outcome } shouldBe listOf(
                DartOutcome.NO_EFFECT,
                DartOutcome.NO_EFFECT,
                DartOutcome.MISS,
            )
            effect.marksAdded shouldBe 0
            effect.pointsScored shouldBe 0
        }

        "Should use the leg's Random Cricket segments" {
            // GIVEN a Random Cricket game on 1, 3, 5, 7, 9, 11 and 13
            val segments = CricketSegments(listOf(13, 11, 9, 7, 5, 3, 1))
            val config = cricketConfig(segments = segments, gameType = GameType.CRICKET_RANDOM)
            val leg = Leg(
                turns = listOf(turn(playerId1, triple(5), triple(20), single(13))),
                cricketState = CricketState(segments = segments),
            )

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, config).single()

            // THEN 5 and 13 count while 20 does not
            effect.darts.map { it.outcome } shouldBe listOf(DartOutcome.HIT, DartOutcome.NO_EFFECT, DartOutcome.HIT)
            effect.closedSegments shouldBe listOf(5)
        }

        "Should fall back to the config's segments for legs saved without cricketState" {
            // GIVEN an old leg without cricketState in a Random Cricket game
            val segments = CricketSegments(listOf(14, 12, 10, 8, 6, 4, 2))
            val config = cricketConfig(segments = segments, gameType = GameType.CRICKET_RANDOM)
            val leg = cricketLeg(turn(playerId1, single(14), single(20), miss))

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, config).single()

            // THEN the config's segments are used
            effect.darts.map { it.outcome } shouldBe listOf(DartOutcome.HIT, DartOutcome.NO_EFFECT, DartOutcome.MISS)
        }

        "Should fall back to standard segments when nothing was stored" {
            // GIVEN neither the leg nor the config stored segments
            val leg = cricketLeg(turn(playerId1, single(20), single(14), miss))

            // WHEN the turn effects are replayed
            val effect = cricketTurnEffects(leg, cricketConfig(segments = null)).single()

            // THEN standard Cricket numbers are used
            effect.darts.map { it.outcome } shouldBe listOf(DartOutcome.HIT, DartOutcome.NO_EFFECT, DartOutcome.MISS)
        }

        "Should return no effects for an empty leg" {
            // GIVEN a leg without turns
            // WHEN the turn effects are replayed
            val effects = cricketTurnEffects(Leg(), cricketConfig())

            // THEN there are none
            effects.shouldBeEmpty()
        }

        "Should match the engine dart by dart and in the final state" {
            // GIVEN a 2-player Cricket game played through the engine
            val (played, results) = playCricket(
                cricketConfig(),
                listOf(
                    listOf(triple(20), double(20), single(19)),
                    listOf(single(20), triple(18), miss),
                    listOf(triple(20), single(5), double(19)),
                    listOf(triple(20), triple(17), double(25)),
                    listOf(single(19), single(20), triple(16)),
                    listOf(double(18), single(17), single(25)),
                ),
            )

            // WHEN the leg is replayed
            // THEN every dart and the stored totals match the engine
            assertCricketParity(played, results)
        }

        "Should match the engine up to a winning dart" {
            // GIVEN player 1 closes every number, finishing on an outer bull
            val (played, results) = playCricket(
                cricketConfig(),
                listOf(
                    listOf(triple(20), triple(19), triple(18)),
                    listOf(miss, miss, miss),
                    listOf(triple(17), triple(16), triple(15)),
                    listOf(single(20), miss, miss),
                    listOf(double(25), single(25)),
                ),
            )
            // AND the engine reported the last dart as the win
            results.last().shouldBeInstanceOf<ThrowResult.CricketWin>()
            played.legs[0].winnerId shouldBe playerId1

            // WHEN the leg is replayed
            // THEN it matches the engine, and the winning dart counted
            assertCricketParity(played, results)
            cricketTurnEffects(played.legs[0], played.config).last().closedSegments shouldBe listOf(25)
        }

        "Should match the engine with 3 players" {
            // GIVEN a 3-player game where one opponent stays open on 20
            val (played, results) = playCricket(
                cricketConfig(playerIds = listOf(playerId1, playerId2, playerId3)),
                listOf(
                    listOf(triple(20), single(20), single(19)),
                    listOf(triple(20), triple(20), miss),
                    listOf(single(20), triple(19), triple(19)),
                    listOf(triple(19), single(5), double(18)),
                    listOf(single(19), double(19), miss),
                    listOf(triple(20), single(20), double(25)),
                ),
            )

            // WHEN the leg is replayed
            // THEN it matches the engine
            assertCricketParity(played, results)
        }

        "Should match the engine on Random Cricket segments" {
            // GIVEN a Random Cricket game on odd numbers up to 13
            val segments = CricketSegments(listOf(13, 11, 9, 7, 5, 3, 1))
            val (played, results) = playCricket(
                cricketConfig(segments = segments, gameType = GameType.CRICKET_RANDOM),
                listOf(
                    listOf(triple(13), single(13), triple(5)),
                    listOf(single(20), triple(5), single(5)),
                    listOf(triple(5), double(11), single(1)),
                    listOf(triple(13), single(13), miss),
                ),
            )

            // WHEN the leg is replayed
            // THEN it matches the engine
            assertCricketParity(played, results)
        }

        "Should replay the second leg from an empty board" {
            // GIVEN player 1 wins leg 1 of a best-of-two after closing 20
            val (played, results) = playCricket(
                cricketConfig(legsToWin = 2),
                listOf(
                    listOf(triple(20), triple(19), triple(18)),
                    listOf(single(20), miss, miss),
                    listOf(triple(17), triple(16), triple(15)),
                    listOf(miss, miss, miss),
                    listOf(double(25), single(25)),
                    // leg 2
                    listOf(triple(20), single(20), miss),
                    listOf(triple(20), triple(20), miss),
                ),
            )
            played.legs shouldHaveSize 2

            // WHEN leg 2 is replayed
            val leg2 = cricketTurnEffects(played.legs[1], played.config)

            // THEN player 1's triple 20 adds marks again instead of carrying leg 1's closed 20
            leg2[0].darts[0].marksAdded shouldBe 3
            leg2[0].darts[1].pointsScored shouldBe 20
            // AND both legs match the engine
            assertCricketParity(played, results)
        }
    }

    // ---- Parcheesi ----

    fun parcheesiEngine(
        playerIds: List<Uuid> = listOf(playerId1, playerId2),
        legsToWin: Int = 1,
    ) = GameEngine.fromSession(
        session(
            GameConfig(
                gameType = GameType.CLASSIC_301,
                gameMode = GameMode.PARCHEESI,
                doubleOut = false,
                playerIds = playerIds,
                legsToWin = legsToWin,
            ),
        ),
    )

    /** Player 2 knocks player 1 out from 241 and checks out on 301 in the same turn. */
    fun GameEngine.playKnockoutCheckoutLeg(): GameEngine = this
        .playTurn(triple(20), triple(20), triple(20)) // P1 180
        .playTurn(triple(20), triple(20), single(1)) // P2 121
        .playTurn(triple(20), single(1), miss) // P1 241
        .playTurn(triple(20), triple(20), triple(20)) // P2 181, 241 (KO P1), 301 (checkout)

    "Parcheesi leg history" - {
        "Should attach a knockout to the turn that caused it" {
            // GIVEN player 1 reaches 20 and player 2 matches it, then keeps throwing
            val leg = parcheesiEngine()
                .playTurn(single(20), miss, miss)
                .playTurn(single(20), single(1), single(1))
                .toGameSession()
                .legs[0]
            // AND the engine stored the phantom reset before the knocking turn
            leg.turns.map { it.isPhantom } shouldBe listOf(false, true, false)

            // WHEN the history is derived
            val history = parcheesiLegHistory(leg)

            // THEN only player 2's turn knocked out player 1, who lost 20 points
            history.turns shouldHaveSize 2
            history.turns[0].knockouts.shouldBeEmpty()
            history.turns[1].knockouts shouldBe listOf(ParcheesiKnockout(playerId = playerId1, scoreBefore = 20))
            history.unfinishedTurnKnockouts.shouldBeEmpty()
        }

        "Should attach two knockouts from one turn to that turn" {
            // GIVEN players 1 and 2 at 10 and 20
            val leg = parcheesiEngine(listOf(playerId1, playerId2, playerId3))
                .playTurn(single(10), miss, miss)
                .playTurn(single(20), miss, miss)
                // WHEN player 3 hits 10 (KO player 1) and 10 again (KO player 2)
                .playTurn(single(10), single(10), miss)
                .toGameSession()
                .legs[0]

            val history = parcheesiLegHistory(leg)

            // THEN player 3's turn lists both knockouts in order
            history.turns[2].knockouts shouldBe listOf(
                ParcheesiKnockout(playerId = playerId1, scoreBefore = 10),
                ParcheesiKnockout(playerId = playerId2, scoreBefore = 20),
            )
            // AND the other turns list none
            history.turns[0].knockouts.shouldBeEmpty()
            history.turns[1].knockouts.shouldBeEmpty()
        }

        "Should attach a knockout dealt on the leg-winning turn" {
            // GIVEN player 2 knocks out player 1 and then checks out in the same turn
            val leg = parcheesiEngine()
                .playKnockoutCheckoutLeg()
                .toGameSession()
                .legs[0]
            leg.winnerId shouldBe playerId2

            // WHEN the history is derived
            val history = parcheesiLegHistory(leg)

            // THEN the winning turn carries the knockout
            history.turns[3].turn.scoreAfterTurn shouldBe 301
            history.turns[3].knockouts shouldBe listOf(ParcheesiKnockout(playerId = playerId1, scoreBefore = 241))
            history.turns.take(3).flatMap { it.knockouts }.shouldBeEmpty()
        }

        "Should keep knockouts within their own leg" {
            // GIVEN a best-of-two where leg 1 ends with a knockout and checkout
            // AND in leg 2 player 2 knocks out player 1 at 20
            val legs = parcheesiEngine(legsToWin = 2)
                .playKnockoutCheckoutLeg()
                .playTurn(single(20), miss, miss)
                .playTurn(single(20), miss, miss)
                .toGameSession()
                .legs
            legs shouldHaveSize 2

            // WHEN each leg's history is derived
            val leg1 = parcheesiLegHistory(legs[0])
            val leg2 = parcheesiLegHistory(legs[1])

            // THEN each knockout belongs to its own leg's causing turn
            leg1.turns[3].knockouts shouldBe listOf(ParcheesiKnockout(playerId = playerId1, scoreBefore = 241))
            leg2.turns shouldHaveSize 2
            leg2.turns[0].knockouts.shouldBeEmpty()
            leg2.turns[1].knockouts shouldBe listOf(ParcheesiKnockout(playerId = playerId1, scoreBefore = 20))
            leg2.unfinishedTurnKnockouts.shouldBeEmpty()
        }

        "Should mark a bounce" {
            // GIVEN player 1 at 180 who overshoots 301 with three triple 20s
            val leg = parcheesiEngine()
                .playTurn(triple(20), triple(20), triple(20))
                .playTurn(single(1), miss, miss)
                .playTurn(triple(20), triple(20), triple(20))
                .toGameSession()
                .legs[0]

            // WHEN the history is derived
            val history = parcheesiLegHistory(leg)

            // THEN only the overshooting turn is a bounce
            history.turns.map { it.isBounce } shouldBe listOf(false, false, true)
            history.turns[2].turn.scoreAfterTurn shouldBe 242
        }

        "Should keep knockouts from a turn that was never finished" {
            // GIVEN player 2 knocks out player 1 but the game is saved before the turn ends
            val (midTurn, _) = parcheesiEngine()
                .playTurn(single(20), miss, miss)
                .addThrow(20, Multiplier.SINGLE)
            val leg = midTurn.toGameSession().legs[0]

            // WHEN the history is derived
            val history = parcheesiLegHistory(leg)

            // THEN the knockout is reported as belonging to the unfinished turn
            history.turns shouldHaveSize 1
            history.turns[0].knockouts.shouldBeEmpty()
            history.unfinishedTurnKnockouts shouldBe listOf(ParcheesiKnockout(playerId = playerId1, scoreBefore = 20))
        }

        "Should return no turns for an empty leg" {
            // GIVEN a leg without turns
            // WHEN the history is derived
            val history = parcheesiLegHistory(Leg())

            // THEN it is empty
            history.turns.shouldBeEmpty()
            history.unfinishedTurnKnockouts.shouldBeEmpty()
        }
    }
})
