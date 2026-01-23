package cloud.osasoft.dartzvibe.data.model

import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for Game-related data models.
 */
@OptIn(ExperimentalUuidApi::class)
class GameModelsTest : FreeSpec({

    val json = Json { prettyPrint = true }
    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    "GameType" - {
        "CLASSIC_501 should have starting score of 501" {
            GameType.CLASSIC_501.startingScore shouldBe 501
        }

        "CLASSIC_301 should have starting score of 301" {
            GameType.CLASSIC_301.startingScore shouldBe 301
        }

        "CLASSIC_501 displayName should be 501" {
            GameType.CLASSIC_501.displayName shouldBe "501"
        }

        "CLASSIC_301 displayName should be 301" {
            GameType.CLASSIC_301.displayName shouldBe "301"
        }
    }

    "Multiplier" - {
        "SINGLE should have value 1" {
            Multiplier.SINGLE.value shouldBe 1
        }

        "DOUBLE should have value 2" {
            Multiplier.DOUBLE.value shouldBe 2
        }

        "TRIPLE should have value 3" {
            Multiplier.TRIPLE.value shouldBe 3
        }
    }

    "Throw" - {
        "should calculate score correctly for single" {
            val dartThrow = Throw(segment = 20, multiplier = Multiplier.SINGLE)
            dartThrow.score shouldBe 20
        }

        "should calculate score correctly for double" {
            val dartThrow = Throw(segment = 20, multiplier = Multiplier.DOUBLE)
            dartThrow.score shouldBe 40
        }

        "should calculate score correctly for triple" {
            val dartThrow = Throw(segment = 20, multiplier = Multiplier.TRIPLE)
            dartThrow.score shouldBe 60
        }

        "should handle bullseye (25 single = outer bull)" {
            val dartThrow = Throw(segment = 25, multiplier = Multiplier.SINGLE)
            dartThrow.score shouldBe 25
        }

        "should handle bullseye (25 double = inner bull/bullseye)" {
            val dartThrow = Throw(segment = 25, multiplier = Multiplier.DOUBLE)
            dartThrow.score shouldBe 50
        }
    }

    "Turn" - {
        "should calculate total score from throws" {
            val turn = Turn(
                playerId = playerId1,
                throws = listOf(
                    Throw(segment = 20, multiplier = Multiplier.TRIPLE), // 60
                    Throw(segment = 20, multiplier = Multiplier.TRIPLE), // 60
                    Throw(segment = 20, multiplier = Multiplier.TRIPLE), // 60
                ),
                scoreBeforeTurn = 501,
                scoreAfterTurn = 321,
            )

            turn.totalScore shouldBe 180
        }

        "should serialize and deserialize correctly" {
            val turn = Turn(
                playerId = playerId1,
                throws = listOf(
                    Throw(segment = 20, multiplier = Multiplier.SINGLE),
                ),
                scoreBeforeTurn = 100,
                scoreAfterTurn = 80,
                isBust = false,
            )

            val jsonString = json.encodeToString(turn)
            val decoded = json.decodeFromString<Turn>(jsonString)

            decoded.playerId shouldBe turn.playerId
            decoded.throws.size shouldBe 1
            decoded.scoreBeforeTurn shouldBe 100
            decoded.scoreAfterTurn shouldBe 80
            decoded.isBust shouldBe false
        }
    }

    "GameConfig" - {
        "should have correct starting score for 501" {
            val config = GameConfig(
                gameType = GameType.CLASSIC_501,
                playerIds = listOf(playerId1, playerId2),
            )

            config.startingScore shouldBe 501
        }

        "should have default doubleIn as false" {
            val config = GameConfig(
                gameType = GameType.CLASSIC_501,
                playerIds = listOf(playerId1),
            )

            config.doubleIn shouldBe false
        }

        "should have default doubleOut as true" {
            val config = GameConfig(
                gameType = GameType.CLASSIC_501,
                playerIds = listOf(playerId1),
            )

            config.doubleOut shouldBe true
        }

        "should have default legsToWin as 1" {
            val config = GameConfig(
                gameType = GameType.CLASSIC_501,
                playerIds = listOf(playerId1),
            )

            config.legsToWin shouldBe 1
        }
    }

    "GameSession" - {
        "should return current leg correctly" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    playerIds = listOf(playerId1, playerId2),
                ),
                legs = listOf(
                    Leg(turns = emptyList(), winnerId = playerId1),
                    Leg(turns = emptyList()),
                ),
                currentLegIndex = 1,
                startedAt = currentTimeMillis(),
            )

            session.currentLeg shouldBe session.legs[1]
        }

        "should have default status of IN_PROGRESS" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    playerIds = listOf(playerId1),
                ),
                startedAt = currentTimeMillis(),
            )

            session.status shouldBe GameStatus.IN_PROGRESS
        }
    }
})
