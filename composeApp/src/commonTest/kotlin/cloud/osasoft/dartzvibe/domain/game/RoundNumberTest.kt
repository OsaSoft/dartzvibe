package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for [GameEngineHelper.getRoundNumber] - the round shown in the active game header.
 */
@OptIn(ExperimentalUuidApi::class)
class RoundNumberTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")

    fun turn(playerId: Uuid, isPhantom: Boolean = false): Turn = Turn(
        playerId = playerId,
        throws = if (isPhantom) emptyList() else listOf(Throw(segment = 20, multiplier = Multiplier.SINGLE)),
        scoreBeforeTurn = 0,
        scoreAfterTurn = 0,
        isPhantom = isPhantom,
    )

    // Plays [turnCount] regular turns in rotation order
    fun turnsInRotation(playerIds: List<Uuid>, turnCount: Int): List<Turn> = (0 until turnCount).map { index ->
        turn(playerIds[index % playerIds.size])
    }

    fun session(
        playerIds: List<Uuid>,
        turns: List<Turn>,
        gameMode: GameMode = GameMode.CLASSIC,
    ): GameSession = GameSession(
        id = Uuid.parse("00000000-0000-0000-0000-000000000100"),
        config = GameConfig(
            gameType = GameType.CLASSIC_501,
            gameMode = gameMode,
            playerIds = playerIds,
        ),
        legs = listOf(Leg(turns = turns)),
        startedAt = 0L,
    )

    data class RoundCase(
        val playerCount: Int,
        val turnsPlayed: Int,
        val expectedRound: Int,
    ) {
        override fun toString(): String = "$playerCount players, $turnsPlayed turns played -> round $expectedRound"
    }

    "Round number" - {
        "Should be the same for every player within a round" - {
            withData(
                // 2 players
                RoundCase(playerCount = 2, turnsPlayed = 0, expectedRound = 1), // first player, first round
                RoundCase(playerCount = 2, turnsPlayed = 1, expectedRound = 1), // second player, still round 1
                RoundCase(playerCount = 2, turnsPlayed = 2, expectedRound = 2), // back to first player
                RoundCase(playerCount = 2, turnsPlayed = 5, expectedRound = 3), // mid-round 3
                // 3 players
                RoundCase(playerCount = 3, turnsPlayed = 0, expectedRound = 1),
                RoundCase(playerCount = 3, turnsPlayed = 2, expectedRound = 1), // last player of round 1
                RoundCase(playerCount = 3, turnsPlayed = 3, expectedRound = 2),
                RoundCase(playerCount = 3, turnsPlayed = 7, expectedRound = 3), // mid-round 3
                // Solo (e.g. Checkout Practice): every turn is a new round
                RoundCase(playerCount = 1, turnsPlayed = 0, expectedRound = 1),
                RoundCase(playerCount = 1, turnsPlayed = 4, expectedRound = 5),
            ) { case ->
                // GIVEN a game with the given number of players
                val playerIds = listOf(playerId1, playerId2, playerId3).take(case.playerCount)
                // AND the given number of turns already played in rotation
                val gameSession = session(playerIds, turnsInRotation(playerIds, case.turnsPlayed))

                // WHEN the round number is computed
                val round = GameEngineHelper.getRoundNumber(gameSession)

                // THEN it matches the expected round
                round shouldBe case.expectedRound
            }
        }

        "Should ignore Parcheesi phantom knockout turns" {
            // GIVEN a 2-player Parcheesi game
            val playerIds = listOf(playerId1, playerId2)
            // AND a full round plus player 1's turn, with a phantom knockout turn for player 2 in between
            val turns = listOf(
                turn(playerId1),
                turn(playerId2),
                turn(playerId1),
                turn(playerId2, isPhantom = true),
            )
            val gameSession = session(playerIds, turns, gameMode = GameMode.PARCHEESI)

            // WHEN the round number is computed
            val round = GameEngineHelper.getRoundNumber(gameSession)

            // THEN only the 3 real turns count, so player 2 is still in round 2
            round shouldBe 2
        }

        "Should restart at round 1 in a new leg" {
            // GIVEN a 2-player game where leg 1 had 6 turns
            val playerIds = listOf(playerId1, playerId2)
            val gameSession = session(playerIds, turnsInRotation(playerIds, 6)).let { finished ->
                // AND a fresh second leg has started
                finished.copy(legs = finished.legs + Leg(), currentLegIndex = 1)
            }

            // WHEN the round number is computed
            val round = GameEngineHelper.getRoundNumber(gameSession)

            // THEN it is round 1 of the new leg
            round shouldBe 1
        }
    }
})
