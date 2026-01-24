package cloud.osasoft.dartzvibe.domain.statistics

import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for StatisticsCalculator - core statistics calculation logic.
 */
@OptIn(ExperimentalUuidApi::class)
class StatisticsCalculatorTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    val player1 = Player(
        id = playerId1,
        name = "Alice",
        createdAt = currentTimeMillis(),
    )
    val player2 = Player(
        id = playerId2,
        name = "Bob",
        createdAt = currentTimeMillis(),
    )
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

    fun createThrow(
        segment: Int,
        multiplier: Multiplier = Multiplier.SINGLE,
    ): Throw = Throw(segment = segment, multiplier = multiplier)

    fun createCompletedGame(
        id: Uuid = sessionId,
        gameType: GameType = GameType.CLASSIC_501,
        legs: List<Leg>,
        winnerId: Uuid,
    ): GameSession = GameSession(
        id = id,
        config = GameConfig(
            gameType = gameType,
            playerIds = listOf(playerId1, playerId2),
        ),
        legs = legs,
        status = GameStatus.COMPLETED,
        startedAt = currentTimeMillis(),
        finishedAt = currentTimeMillis(),
        winnerId = winnerId,
    )

    "StatisticsCalculator" - {
        "Should return empty stats when no games" {
            // GIVEN no games
            val games = emptyList<GameSession>()

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, games, players)

            // THEN all stats are zero
            stats.gamesPlayed shouldBe 0
            stats.gamesWon shouldBe 0
            stats.threeDartAverage shouldBe FixedDecimal.ZERO
            stats.checkoutPercentage shouldBe FixedDecimal.ZERO
            stats.count180s shouldBe 0
        }

        "Should return empty stats when only in-progress games" {
            // GIVEN an in-progress game
            val games = listOf(
                GameSession(
                    id = sessionId,
                    config = GameConfig(
                        gameType = GameType.CLASSIC_501,
                        playerIds = listOf(playerId1, playerId2),
                    ),
                    legs = listOf(Leg()),
                    status = GameStatus.IN_PROGRESS,
                    startedAt = currentTimeMillis(),
                ),
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, games, players)

            // THEN all stats are zero
            stats.gamesPlayed shouldBe 0
        }

        "Should count games played and won" {
            // GIVEN two completed games where player1 won one
            val game1 = createCompletedGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(playerId1, listOf(createThrow(20, Multiplier.TRIPLE)), 501),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val game2 = createCompletedGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(playerId2, listOf(createThrow(20, Multiplier.TRIPLE)), 501),
                        ),
                        winnerId = playerId2,
                    ),
                ),
                winnerId = playerId2,
            )
            val games = listOf(game1, game2)

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, games, players)

            // THEN games played and won are correct
            stats.gamesPlayed shouldBe 2
            stats.gamesWon shouldBe 1
            stats.gamesWonList.size shouldBe 1
        }

        "Should calculate 3-dart average correctly" {
            // GIVEN a completed game with known turn scores
            // Player throws T20, T20, T20 (60+60+60 = 180)
            // Then throws 20, 20, 20 (60)
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ),
                createTurn(
                    playerId2,
                    listOf(createThrow(20, Multiplier.SINGLE)),
                    501,
                ),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(20, Multiplier.SINGLE),
                    ),
                    321, // 501 - 180
                ),
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN 3-dart average is (180 + 60) / 2 = 120
            stats.threeDartAverage shouldBe FixedDecimal.fromInt(120)
        }

        "Should exclude bust turns from 3-dart average" {
            // GIVEN a game with a busted turn
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ), // 180, not busted
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    50,
                    isBust = true, // Busted
                ),
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN only non-busted turn counts (180 / 1 = 180)
            stats.threeDartAverage shouldBe FixedDecimal.fromInt(180)
        }

        "Should calculate first 9 average" {
            // GIVEN a game where player has 3 turns in the first 9
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ), // Turn 0: 180
                createTurn(playerId2, listOf(createThrow(20)), 501),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(19, Multiplier.TRIPLE),
                        createThrow(19, Multiplier.TRIPLE),
                        createThrow(19, Multiplier.TRIPLE),
                    ),
                    321,
                ), // Turn 1: 171
                createTurn(playerId2, listOf(createThrow(20)), 481),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(19, Multiplier.TRIPLE),
                        createThrow(18, Multiplier.TRIPLE),
                    ),
                    150,
                ), // Turn 2: 171
                createTurn(playerId2, listOf(createThrow(20)), 461),
                // Turn 3 would be outside first 9
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(10, Multiplier.SINGLE),
                        createThrow(10, Multiplier.SINGLE),
                        createThrow(10, Multiplier.SINGLE),
                    ),
                    0,
                ), // Turn 3: 30 (outside first 9)
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN first 9 average is (180 + 171 + 171) / 3 = 174
            stats.first9Average shouldBe FixedDecimal.fromInt(174)
        }

        "Should count checkout attempts and calculate checkout percentage" {
            // GIVEN a game where player had checkout opportunities
            val turns = listOf(
                // Score before is 170 (in checkout range)
                createTurn(
                    playerId1,
                    listOf(createThrow(20, Multiplier.TRIPLE)),
                    170,
                ), // Attempt but not finished
                // Score before is 110 (in checkout range)
                createTurn(
                    playerId1,
                    listOf(createThrow(20, Multiplier.TRIPLE)),
                    110,
                ), // Attempt but not finished
                // Score before is 200 (not in checkout range)
                createTurn(
                    playerId1,
                    listOf(createThrow(20, Multiplier.TRIPLE)),
                    200,
                ), // Not a checkout attempt
            )
            val game = createCompletedGame(
                legs = listOf(
                    Leg(turns = turns, winnerId = playerId1), // Player won the leg (= checkout)
                ),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN checkout attempts is 2 and checkouts hit is 1 (leg won)
            stats.checkoutAttempts shouldBe 2
            stats.checkoutsHit shouldBe 1
            stats.checkoutPercentage shouldBe FixedDecimal.fromInt(50)
        }

        "Should find best checkout" {
            // GIVEN a game where player checked out from 110
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(10, Multiplier.SINGLE),
                        createThrow(20, Multiplier.DOUBLE),
                    ),
                    110,
                ), // Checkout from 110
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN best checkout is 110
            stats.bestCheckout.shouldNotBeNull()
            stats.bestCheckout!!.value shouldBe 110
        }

        "Should count 180s correctly" {
            // GIVEN a game with two 180s
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ), // 180
                createTurn(playerId2, listOf(createThrow(20)), 501),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    321,
                ), // Another 180
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN 180 count is 2
            stats.count180s shouldBe 2
            stats.games180s.size shouldBe 1 // Only one game
        }

        "Should count 140+ and 100+ correctly" {
            // GIVEN a game with various scores
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ), // 180: counts as 180, 140+, 100+
                createTurn(playerId2, listOf(createThrow(20)), 501),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.SINGLE),
                    ),
                    321,
                ), // 140: counts as 140+, 100+
                createTurn(playerId2, listOf(createThrow(20)), 481),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(20, Multiplier.SINGLE),
                    ),
                    181,
                ), // 100: counts as 100+ only
                createTurn(playerId2, listOf(createThrow(20)), 461),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(19, Multiplier.SINGLE),
                    ),
                    81,
                ), // 59: doesn't count
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN counts are correct
            stats.count180s shouldBe 1
            stats.count140Plus shouldBe 2 // 180 and 140
            stats.count100Plus shouldBe 3 // 180, 140, and 100
        }

        "Should track highest turn score" {
            // GIVEN a game with various scores
            val turns = listOf(
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.TRIPLE),
                    ),
                    501,
                ), // 180
                createTurn(playerId2, listOf(createThrow(20)), 501),
                createTurn(
                    playerId1,
                    listOf(
                        createThrow(20, Multiplier.TRIPLE),
                        createThrow(20, Multiplier.SINGLE),
                        createThrow(20, Multiplier.SINGLE),
                    ),
                    321,
                ), // 100
            )
            val game = createCompletedGame(
                legs = listOf(Leg(turns = turns, winnerId = playerId1)),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN highest turn score is 180
            stats.highestTurnScore.shouldNotBeNull()
            stats.highestTurnScore!!.value shouldBe 180
        }

        "Should filter by game type" {
            // GIVEN games of different types
            val game501 = createCompletedGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                gameType = GameType.CLASSIC_501,
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(
                                playerId1,
                                listOf(
                                    createThrow(20, Multiplier.TRIPLE),
                                    createThrow(20, Multiplier.TRIPLE),
                                    createThrow(20, Multiplier.TRIPLE),
                                ),
                                501,
                            ),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val game301 = createCompletedGame(
                id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                gameType = GameType.CLASSIC_301,
                legs = listOf(
                    Leg(
                        turns = listOf(
                            createTurn(
                                playerId1,
                                listOf(
                                    createThrow(20, Multiplier.SINGLE),
                                    createThrow(20, Multiplier.SINGLE),
                                    createThrow(20, Multiplier.SINGLE),
                                ),
                                301,
                            ),
                        ),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )
            val games = listOf(game501, game301)

            // WHEN filtering by 501 only
            val stats = calculator.calculatePlayerStatistics(
                playerId1,
                games,
                players,
                StatisticsFilter(gameType = GameType.CLASSIC_501),
            )

            // THEN only 501 games are counted
            stats.gamesPlayed shouldBe 1
            stats.count180s shouldBe 1 // Only from 501 game
        }

        "Should count legs played and won" {
            // GIVEN a game with multiple legs
            val game = createCompletedGame(
                legs = listOf(
                    Leg(
                        turns = listOf(createTurn(playerId1, listOf(createThrow(20)), 501)),
                        winnerId = playerId1,
                    ),
                    Leg(
                        turns = listOf(createTurn(playerId1, listOf(createThrow(20)), 501)),
                        winnerId = playerId2,
                    ),
                    Leg(
                        turns = listOf(createTurn(playerId1, listOf(createThrow(20)), 501)),
                        winnerId = playerId1,
                    ),
                ),
                winnerId = playerId1,
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN legs played and won are correct
            stats.legsPlayed shouldBe 3
            stats.legsWon shouldBe 2
        }

        "Should handle player not in game" {
            // GIVEN a game without player1
            val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
            val game = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    playerIds = listOf(playerId2, playerId3),
                ),
                legs = listOf(Leg(winnerId = playerId2)),
                status = GameStatus.COMPLETED,
                startedAt = currentTimeMillis(),
                finishedAt = currentTimeMillis(),
                winnerId = playerId2,
            )

            // WHEN calculating stats for player1
            val stats = calculator.calculatePlayerStatistics(playerId1, listOf(game), players)

            // THEN no games counted
            stats.gamesPlayed shouldBe 0
        }

        "Should have correct win rate calculation" {
            // GIVEN stats with 3 games, 2 wins
            val games = listOf(
                createCompletedGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000101"),
                    legs = listOf(Leg(winnerId = playerId1)),
                    winnerId = playerId1,
                ),
                createCompletedGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000102"),
                    legs = listOf(Leg(winnerId = playerId1)),
                    winnerId = playerId1,
                ),
                createCompletedGame(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000103"),
                    legs = listOf(Leg(winnerId = playerId2)),
                    winnerId = playerId2,
                ),
            )

            // WHEN calculating stats
            val stats = calculator.calculatePlayerStatistics(playerId1, games, players)

            // THEN win rate is 2/3 ~= 0.666 (formatted as 0.6 with 1 decimal place)
            stats.winRate.format(1) shouldBe "0.6"
        }
    }
})
