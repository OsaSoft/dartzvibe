package cloud.osasoft.dartzvibe.domain.statistics

import cloud.osasoft.dartzvibe.data.model.CheckoutPracticeState
import cloud.osasoft.dartzvibe.data.model.CheckoutRoundResult
import cloud.osasoft.dartzvibe.data.model.CricketPlayerState
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.ModeStatistics
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.RouletteState
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for StatisticsCalculator — mode-scoped statistics calculation.
 */
@OptIn(ExperimentalUuidApi::class)
class StatisticsCalculatorTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    val player1 = Player(id = playerId1, name = "Alice", createdAt = currentTimeMillis())
    val player2 = Player(id = playerId2, name = "Bob", createdAt = currentTimeMillis())
    val players = listOf(player1, player2)

    val calculator = StatisticsCalculator()

    fun createTurn(
        playerId: Uuid,
        throws: List<Throw>,
        scoreBeforeTurn: Int,
        isBust: Boolean = false,
    ): Turn {
        val totalScore = throws.sumOf { it.score }
        val scoreAfterTurn = if (isBust) scoreBeforeTurn else scoreBeforeTurn - totalScore
        return Turn(
            playerId = playerId,
            throws = throws,
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = scoreAfterTurn,
            isBust = isBust,
        )
    }

    fun t(segment: Int, multiplier: Multiplier = Multiplier.SINGLE): Throw =
        Throw(segment = segment, multiplier = multiplier)

    fun createClassicGame(
        id: Uuid = sessionId,
        gameType: GameType = GameType.CLASSIC_501,
        legs: List<Leg>,
        winnerId: Uuid,
    ): GameSession = GameSession(
        id = id,
        config = GameConfig(gameType = gameType, playerIds = listOf(playerId1, playerId2)),
        legs = legs,
        status = GameStatus.COMPLETED,
        startedAt = currentTimeMillis(),
        finishedAt = currentTimeMillis(),
        winnerId = winnerId,
    )

    fun classicStats(
        playerId: Uuid,
        games: List<GameSession>,
        gameType: GameType? = null,
    ): PlayerStatisticsView {
        val stats = calculator.calculatePlayerStatistics(
            playerId = playerId,
            games = games,
            players = players,
            filter = StatisticsFilter(gameMode = GameMode.CLASSIC, gameType = gameType),
        )
        return PlayerStatisticsView(stats)
    }

    "Classic statistics" - {
        "Should return empty stats when no games" {
            val stats = calculator.calculatePlayerStatistics(
                playerId1,
                emptyList(),
                players,
                StatisticsFilter(gameMode = GameMode.CLASSIC),
            )
            stats.gamesPlayed shouldBe 0
            stats.gamesWon shouldBe 0
            stats.modeStats shouldBe null
        }

        "Should ignore in-progress games" {
            val games = listOf(
                GameSession(
                    id = sessionId,
                    config = GameConfig(gameType = GameType.CLASSIC_501, playerIds = listOf(playerId1, playerId2)),
                    legs = listOf(Leg()),
                    status = GameStatus.IN_PROGRESS,
                    startedAt = currentTimeMillis(),
                ),
            )
            val stats = calculator.calculatePlayerStatistics(
                playerId1,
                games,
                players,
                StatisticsFilter(gameMode = GameMode.CLASSIC),
            )
            stats.gamesPlayed shouldBe 0
        }

        "Should count games played and won" {
            val game1 = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                legs = listOf(
                    Leg(
                        turns = listOf(createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 501)),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val game2 = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                legs = listOf(
                    Leg(
                        turns = listOf(createTurn(playerId2, listOf(t(20, Multiplier.TRIPLE)), 501)),
                        winnerId = playerId2,
                    ),
                ),
                winnerId = playerId2,
            )
            val stats = classicStats(playerId1, listOf(game1, game2))
            stats.raw.gamesPlayed shouldBe 2
            stats.raw.gamesWon shouldBe 1
            stats.raw.gamesWonList.size shouldBe 1
        }

        "Should calculate 3-dart average correctly" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(playerId2, listOf(t(20)), 501),
                createTurn(playerId1, listOf(t(20), t(20), t(20)), 321),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            classicStats(playerId1, listOf(game)).classic.threeDartAverage shouldBe FixedDecimal.fromInt(120)
        }

        "Should exclude bust turns from 3-dart average" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    50,
                    isBust = true,
                ),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            classicStats(playerId1, listOf(game)).classic.threeDartAverage shouldBe FixedDecimal.fromInt(180)
        }

        "Should calculate first 9 average" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(playerId2, listOf(t(20)), 501),
                createTurn(
                    playerId1,
                    listOf(t(19, Multiplier.TRIPLE), t(19, Multiplier.TRIPLE), t(19, Multiplier.TRIPLE)),
                    321,
                ),
                createTurn(playerId2, listOf(t(20)), 481),
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(19, Multiplier.TRIPLE), t(18, Multiplier.TRIPLE)),
                    150,
                ),
                createTurn(playerId2, listOf(t(20)), 461),
                createTurn(playerId1, listOf(t(10), t(10), t(10)), 0),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            classicStats(playerId1, listOf(game)).classic.first9Average shouldBe FixedDecimal.fromInt(174)
        }

        "Should count checkout attempts and percentage" {
            val turns = listOf(
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 170),
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 110),
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 200),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val classic = classicStats(playerId1, listOf(game)).classic
            classic.checkoutAttempts shouldBe 2
            classic.checkoutsHit shouldBe 1
            classic.checkoutPercentage shouldBe FixedDecimal.fromInt(50)
        }

        "Should find best checkout" {
            val turns = listOf(
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE), t(10), t(20, Multiplier.DOUBLE)), 110),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val best = classicStats(playerId1, listOf(game)).classic.bestCheckout
            best.shouldNotBeNull()
            best.value shouldBe 110
        }

        "Should count 180s, 140+ and 100+" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(playerId2, listOf(t(20)), 501),
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20)), 321),
                createTurn(playerId2, listOf(t(20)), 481),
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE), t(20), t(20)), 181),
                createTurn(playerId2, listOf(t(20)), 461),
                createTurn(playerId1, listOf(t(20), t(20), t(19)), 81),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val classic = classicStats(playerId1, listOf(game)).classic
            classic.count180s shouldBe 1
            classic.count140Plus shouldBe 2
            classic.count100Plus shouldBe 3
            classic.games180s.size shouldBe 1
        }

        "Should track highest turn score" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE), t(20), t(20)), 321),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val highest = classicStats(playerId1, listOf(game)).classic.highestTurnScore
            highest.shouldNotBeNull()
            highest.value shouldBe 180
        }

        "Should filter by game type within a mode" {
            val game501 = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                gameType = GameType.CLASSIC_501,
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(
                                playerId1,
                                listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                                501,
                            ),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val game301 = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                gameType = GameType.CLASSIC_301,
                legs = listOf(
                    Leg(turns = listOf(createTurn(playerId1, listOf(t(20), t(20), t(20)), 301)), winnerId = playerId1),
                ),
                winnerId = playerId1,
            )
            val stats = classicStats(playerId1, listOf(game501, game301), gameType = GameType.CLASSIC_501)
            stats.raw.gamesPlayed shouldBe 1
            stats.classic.count180s shouldBe 1
        }

        "Should count legs played and won" {
            val game = createClassicGame(
                legs = listOf(
                    Leg(turns = listOf(createTurn(playerId1, listOf(t(20)), 501)), winnerId = playerId1),
                    Leg(turns = listOf(createTurn(playerId1, listOf(t(20)), 501)), winnerId = playerId2),
                    Leg(turns = listOf(createTurn(playerId1, listOf(t(20)), 501)), winnerId = playerId1),
                ),
                winnerId = playerId1,
            )
            val stats = classicStats(playerId1, listOf(game)).raw
            stats.legsPlayed shouldBe 3
            stats.legsWon shouldBe 2
        }

        "Should calculate win rate as a percentage" {
            val games = (1..3).map { i ->
                val won = i <= 2
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-00000000010$i"),
                    legs = listOf(Leg(winnerId = if (won) playerId1 else playerId2)),
                    winnerId = if (won) playerId1 else playerId2,
                )
            }
            classicStats(playerId1, games).raw.winRate.format(1) shouldBe "66.6"
        }
    }

    "Mode isolation (regression guards)" - {
        "Should not blend Cricket points into Classic 3-dart average" {
            // GIVEN one Classic 501 game (a single 180 turn) and one Cricket game
            val classicGame = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000201"),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(
                                playerId1,
                                listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                                501,
                            ),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val cricketGame = GameSession(
                id = Uuid.parse("00000000-0000-0000-0000-000000000202"),
                config = GameConfig(
                    gameType = GameType.CRICKET_REGULAR,
                    gameMode = GameMode.CRICKET,
                    playerIds = listOf(playerId1, playerId2),
                ),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 0).copy(scoreAfterTurn = 105),
                        ),
                        winnerId = playerId1,
                        cricketState = CricketState(
                            playerStates = mapOf(playerId1 to CricketPlayerState(marks = mapOf(20 to 3), points = 105)),
                        ),
                    ),
                ),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId1,
            )

            // WHEN computing Classic stats for the player
            val stats = classicStats(playerId1, listOf(classicGame, cricketGame))

            // THEN only the Classic game contributes — average is 180, not blended with 105
            stats.raw.gamesPlayed shouldBe 1
            stats.classic.threeDartAverage shouldBe FixedDecimal.fromInt(180)
        }

        "Should not produce checkout stats from Parcheesi count-up scores" {
            // GIVEN a Parcheesi game where the player's score crosses the 1..170 checkout range
            val parcheesiGame = GameSession(
                id = Uuid.parse("00000000-0000-0000-0000-000000000203"),
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    gameMode = GameMode.PARCHEESI,
                    playerIds = listOf(playerId1, playerId2),
                ),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE)), 110).copy(scoreAfterTurn = 170),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId1,
            )

            // WHEN computing CLASSIC stats — the Parcheesi game must be excluded entirely
            classicStats(playerId1, listOf(parcheesiGame)).raw.gamesPlayed shouldBe 0

            // AND computing PARCHEESI stats yields Parcheesi-shaped stats (no checkout concept)
            val parcheesi = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(parcheesiGame),
                players,
                StatisticsFilter(gameMode = GameMode.PARCHEESI),
            )
            parcheesi.modeStats.shouldBeInstanceOf<ModeStatistics.Parcheesi>()
        }
    }

    "Parcheesi statistics" - {
        fun parcheesiGame(
            id: Uuid = sessionId,
            legs: List<Leg>,
            winnerId: Uuid,
        ): GameSession = GameSession(
            id = id,
            config = GameConfig(
                gameType = GameType.CLASSIC_501,
                gameMode = GameMode.PARCHEESI,
                playerIds = listOf(playerId1, playerId2),
            ),
            legs = legs,
            status = GameStatus.COMPLETED,
            startedAt = currentTimeMillis(),
            finishedAt = currentTimeMillis(),
            winnerId = winnerId,
        )

        fun phantom(playerId: Uuid, scoreBeforeTurn: Int): Turn = Turn(
            playerId = playerId,
            throws = emptyList(),
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = 0,
            isPhantom = true,
        )

        fun parcheesi(playerId: Uuid, games: List<GameSession>): ModeStatistics.Parcheesi =
            calculator.calculatePlayerStatistics(
                playerId,
                games,
                players,
                StatisticsFilter(gameMode = GameMode.PARCHEESI),
            ).modeStats as ModeStatistics.Parcheesi

        "Should count knockouts dealt" {
            val turns = listOf(
                createTurn(playerId1, listOf(t(20, Multiplier.TRIPLE), t(20), t(20)), 0).copy(scoreAfterTurn = 100),
                phantom(playerId2, scoreBeforeTurn = 100),
            )
            val game = parcheesiGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val stats = parcheesi(playerId1, listOf(game))
            stats.knockoutsDealt shouldBe 1
            stats.timesKnockedOut shouldBe 0
        }

        "Should count times knocked out" {
            val turns = listOf(
                createTurn(playerId2, listOf(t(20, Multiplier.TRIPLE), t(20), t(20)), 0).copy(scoreAfterTurn = 100),
                phantom(playerId1, scoreBeforeTurn = 100),
            )
            val game = parcheesiGame(legs = listOf(Leg(turns = turns, winnerId = playerId2)), winnerId = playerId2)
            val stats = parcheesi(playerId1, listOf(game))
            stats.knockoutsDealt shouldBe 0
            stats.timesKnockedOut shouldBe 1
        }

        "Should compute bounce-back rate" {
            // GIVEN player1 has 4 turns, 1 of which bounced
            val turns = listOf(
                createTurn(playerId1, listOf(t(20)), 0),
                createTurn(playerId1, listOf(t(20)), 20).copy(isBounce = true),
                createTurn(playerId1, listOf(t(20)), 40),
                createTurn(playerId1, listOf(t(20)), 60),
            )
            val game = parcheesiGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            // THEN bounce-back rate is 1/4 = 25%
            parcheesi(playerId1, listOf(game)).bounceBackRate shouldBe FixedDecimal.fromInt(25)
        }

        "Should be scoped to Parcheesi only (Classic games excluded)" {
            val classicGame = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000301"),
                legs = listOf(Leg(winnerId = playerId1)),
                winnerId = playerId1,
            )
            val pGame = parcheesiGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000302"),
                legs = listOf(Leg(winnerId = playerId1)),
                winnerId = playerId1,
            )
            val stats = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(classicGame, pGame),
                players,
                StatisticsFilter(gameMode = GameMode.PARCHEESI),
            )
            stats.gamesPlayed shouldBe 1
            stats.modeStats.shouldBeInstanceOf<ModeStatistics.Parcheesi>()
        }
    }

    "Cricket statistics" - {
        "Should compute MPR, close rate and hit rate" {
            // GIVEN one Cricket leg: player1 throws T20, T20, T20 (9 marks on segment 20)
            val game = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CRICKET_REGULAR,
                    gameMode = GameMode.CRICKET,
                    playerIds = listOf(playerId1, playerId2),
                ),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(
                                playerId1,
                                listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                                0,
                            ).copy(scoreAfterTurn = 0),
                        ),
                        winnerId = playerId1,
                        cricketState = CricketState(
                            segments = CricketSegments.standard(),
                            playerStates = mapOf(playerId1 to CricketPlayerState(marks = mapOf(20 to 3), points = 0)),
                        ),
                    ),
                ),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId1,
            )
            val cricket = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(game),
                players,
                StatisticsFilter(gameMode = GameMode.CRICKET),
            ).modeStats as ModeStatistics.Cricket

            // 9 marks over 1 round = 9.0 MPR
            cricket.marksPerRound shouldBe FixedDecimal.fromInt(9)
            // all 3 darts on a target segment = 100% hit rate
            cricket.hitRate shouldBe FixedDecimal.fromInt(100)
            // 1 of 7 standard segments closed
            cricket.closeRate.format(1) shouldBe "14.2"
        }
    }

    "Roulette statistics" - {
        "rouletteTargetFor matches round-major turn ordering" {
            val targets = listOf(20, 19)
            calculator.rouletteTargetFor(targets, playerCount = 2, turnIndex = 0) shouldBe 20
            calculator.rouletteTargetFor(targets, playerCount = 2, turnIndex = 1) shouldBe 20
            calculator.rouletteTargetFor(targets, playerCount = 2, turnIndex = 2) shouldBe 19
            calculator.rouletteTargetFor(targets, playerCount = 2, turnIndex = 3) shouldBe 19
        }

        "Should compute hit rate and points per round from stored data" {
            // 2 players, targets [20, 19]. Player1 throws in rounds 0 (target 20) and 1 (target 19).
            val turns = listOf(
                // idx0 p1 round0 target20: two hits on 20 -> 2 points
                createTurn(playerId1, listOf(t(20), t(20), t(5)), 0).copy(scoreAfterTurn = 2),
                // idx1 p2 round0
                createTurn(playerId2, listOf(t(10), t(10), t(10)), 0).copy(scoreAfterTurn = 0),
                // idx2 p1 round1 target19: one triple-19 hit -> 3 points
                createTurn(playerId1, listOf(t(19, Multiplier.TRIPLE), t(1), t(1)), 2).copy(scoreAfterTurn = 5),
                // idx3 p2 round1
                createTurn(playerId2, listOf(t(1), t(1), t(1)), 0).copy(scoreAfterTurn = 0),
            )
            val game = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.ROULETTE_ROUNDS,
                    gameMode = GameMode.ROULETTE,
                    playerIds = listOf(playerId1, playerId2),
                    rouletteTargetSegments = listOf(20, 19),
                    rouletteRounds = 2,
                ),
                legs = listOf(
                    Leg(turns = turns, winnerId = playerId1, rouletteState = RouletteState(currentRoundIndex = 2)),
                ),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId1,
            )
            val roulette = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(game),
                players,
                StatisticsFilter(gameMode = GameMode.ROULETTE),
            ).modeStats as ModeStatistics.Roulette

            // hits: 2 (round0) + 1 (round1) = 3 of 6 darts = 50%
            roulette.hitRate shouldBe FixedDecimal.fromInt(50)
            // points: 2 + 3 = 5 over 2 rounds = 2.5
            roulette.pointsPerRound.format(1) shouldBe "2.5"
            roulette.bestRoundScore shouldBe 3
        }

        "Should stay deterministic for an abandoned partial round" {
            // p1 plays round0 (target 20) then the game ends mid-round (no p2 turn for round1)
            val turns = listOf(
                createTurn(playerId1, listOf(t(20), t(20), t(20)), 0).copy(scoreAfterTurn = 3),
                createTurn(playerId2, listOf(t(1)), 0).copy(scoreAfterTurn = 0),
                createTurn(playerId1, listOf(t(20), t(1), t(1)), 3).copy(scoreAfterTurn = 4),
            )
            val game = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.ROULETTE_ROUNDS,
                    gameMode = GameMode.ROULETTE,
                    playerIds = listOf(playerId1, playerId2),
                    rouletteTargetSegments = listOf(20, 19),
                    rouletteRounds = 5,
                ),
                legs = listOf(
                    Leg(turns = turns, winnerId = null, rouletteState = RouletteState(currentRoundIndex = 1)),
                ),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = null,
            )
            val roulette = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(game),
                players,
                StatisticsFilter(gameMode = GameMode.ROULETTE),
            ).modeStats as ModeStatistics.Roulette
            // p1 round0 target20: 3 hits. round1 target19: the single 20 is NOT a hit.
            // 3 hits of 6 darts = 50%
            roulette.hitRate shouldBe FixedDecimal.fromInt(50)
        }
    }

    "Checkout practice statistics" - {
        fun checkoutGame(
            gameType: GameType,
            targets: List<Int>,
            results: List<CheckoutRoundResult>,
            id: Uuid = sessionId,
        ): GameSession = GameSession(
            id = id,
            config = GameConfig(
                gameType = gameType,
                gameMode = GameMode.CHECKOUT_PRACTICE,
                playerIds = listOf(playerId1),
                checkoutPracticeTargets = targets,
            ),
            legs = listOf(
                Leg(checkoutPracticeState = CheckoutPracticeState(targets = targets, roundResults = results)),
            ),
            status = GameStatus.COMPLETED,
            startedAt = currentTimeMillis(),
            finishedAt = currentTimeMillis(),
            winnerId = playerId1,
        )

        "Should compute success rate, avg darts, best target and bands" {
            val game = checkoutGame(
                gameType = GameType.CHECKOUT_EASY,
                targets = listOf(40, 36),
                results = listOf(
                    CheckoutRoundResult(target = 40, success = true, dartsUsed = 3, throws = emptyList()),
                    CheckoutRoundResult(target = 36, success = false, dartsUsed = 3, throws = emptyList()),
                ),
            )
            val checkout = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(game),
                players,
                StatisticsFilter(gameMode = GameMode.CHECKOUT_PRACTICE),
            ).modeStats as ModeStatistics.CheckoutPractice

            checkout.successRate shouldBe FixedDecimal.fromInt(50)
            checkout.avgDartsToCheckout shouldBe FixedDecimal.fromInt(3)
            checkout.bestTarget shouldBe 40
            checkout.sessionsCompleted shouldBe 1
            checkout.bands.size shouldBe 1
            checkout.bands[0].band shouldBe GameType.CHECKOUT_EASY
            checkout.bands[0].successCount shouldBe 1
            checkout.bands[0].attempts shouldBe 2
        }

        "Should not divide by zero for a solo-only player" {
            // GIVEN a player who has only played Checkout Practice
            val game = checkoutGame(
                gameType = GameType.CHECKOUT_EASY,
                targets = listOf(40),
                results = listOf(CheckoutRoundResult(40, true, 3, emptyList())),
            )
            // WHEN computing CLASSIC stats — none exist
            val classic = calculator.calculatePlayerStatistics(
                playerId1,
                listOf(game),
                players,
                StatisticsFilter(gameMode = GameMode.CLASSIC),
            )
            // THEN no games, zero win rate, null mode stats, no exception
            classic.gamesPlayed shouldBe 0
            classic.winRate shouldBe FixedDecimal.ZERO
            classic.modeStats shouldBe null

            // AND head-to-head with another player is empty (solo games never match both players)
            val h2h = calculator.calculateHeadToHeadStatistics(playerId1, playerId2, listOf(game))
            h2h.gamesPlayed shouldBe 0
        }
    }

    "Head-to-Head Statistics" - {
        "Should return empty stats when no H2H games" {
            val stats = calculator.calculateHeadToHeadStatistics(playerId1, playerId2, emptyList())
            stats.gamesPlayed shouldBe 0
            stats.player1Wins shouldBe 0
            stats.player2Wins shouldBe 0
            stats.recentGames.size shouldBe 0
        }

        "Should only count games where both players participated" {
            val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
            val h2hGame = createClassicGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                legs = listOf(Leg(winnerId = playerId1)),
                winnerId = playerId1,
            )
            val otherGame = GameSession(
                id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                config = GameConfig(gameType = GameType.CLASSIC_501, playerIds = listOf(playerId1, playerId3)),
                legs = listOf(Leg(winnerId = playerId1)),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId1,
            )
            val stats = calculator.calculateHeadToHeadStatistics(playerId1, playerId2, listOf(h2hGame, otherGame))
            stats.gamesPlayed shouldBe 1
        }

        "Should count wins correctly for each player" {
            val games = listOf(
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                    legs = listOf(Leg(winnerId = playerId1)),
                    winnerId = playerId1,
                ),
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                    legs = listOf(Leg(winnerId = playerId2)),
                    winnerId = playerId2,
                ),
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000103"),
                    legs = listOf(Leg(winnerId = playerId1)),
                    winnerId = playerId1,
                ),
            )
            val stats = calculator.calculateHeadToHeadStatistics(playerId1, playerId2, games)
            stats.gamesPlayed shouldBe 3
            stats.player1Wins shouldBe 2
            stats.player2Wins shouldBe 1
        }

        "Should calculate 3-dart average for each player" {
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE), t(20, Multiplier.TRIPLE)),
                    501,
                ),
                createTurn(playerId2, listOf(t(20), t(20), t(20)), 501),
            )
            val game = createClassicGame(legs = listOf(Leg(turns = turns, winnerId = playerId1)), winnerId = playerId1)
            val stats = calculator.calculateHeadToHeadStatistics(
                playerId1,
                playerId2,
                listOf(game),
                players,
                GameMode.CLASSIC,
            )
            (stats.player1Stats.modeStats as ModeStatistics.Classic).threeDartAverage shouldBe FixedDecimal.fromInt(180)
            (stats.player2Stats.modeStats as ModeStatistics.Classic).threeDartAverage shouldBe FixedDecimal.fromInt(60)
        }

        "Should track legs won for each player" {
            val game = createClassicGame(
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(t(20)), 501),
                            createTurn(playerId2, listOf(t(20)), 501),
                        ),
                        winnerId = playerId1,
                    ),
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(t(20)), 501),
                            createTurn(playerId2, listOf(t(20)), 501),
                        ),
                        winnerId = playerId2,
                    ),
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(t(20)), 501),
                            createTurn(playerId2, listOf(t(20)), 501),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val stats = calculator.calculateHeadToHeadStatistics(
                playerId1,
                playerId2,
                listOf(game),
                players,
                GameMode.CLASSIC,
            )
            stats.player1Stats.legsWon shouldBe 2
            stats.player1Stats.legsPlayed shouldBe 3
            stats.player2Stats.legsWon shouldBe 1
            stats.player2Stats.legsPlayed shouldBe 3
        }

        "Should filter by game type" {
            val game501 =
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                    gameType = GameType.CLASSIC_501,
                    legs = listOf(Leg(winnerId = playerId1)),
                    winnerId = playerId1,
                )
            val game301 =
                createClassicGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                    gameType = GameType.CLASSIC_301,
                    legs = listOf(Leg(winnerId = playerId2)),
                    winnerId = playerId2,
                )
            val stats = calculator.calculateHeadToHeadStatistics(
                playerId1,
                playerId2,
                listOf(game501, game301),
                gameTypeFilter = GameType.CLASSIC_501,
            )
            stats.gamesPlayed shouldBe 1
            stats.player1Wins shouldBe 1
            stats.player2Wins shouldBe 0
        }
    }
})

/** Small view wrapper so Classic tests read mode-specific fields without repeating the cast. */
@OptIn(ExperimentalUuidApi::class)
private class PlayerStatisticsView(val raw: cloud.osasoft.dartzvibe.data.model.PlayerStatistics) {
    val classic: ModeStatistics.Classic
        get() = raw.modeStats as ModeStatistics.Classic
}
