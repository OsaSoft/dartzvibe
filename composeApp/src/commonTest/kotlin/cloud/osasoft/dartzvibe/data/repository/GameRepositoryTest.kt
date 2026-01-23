package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for GameRepository using a fake implementation.
 * These tests verify the repository contract/interface behavior.
 */
@OptIn(ExperimentalUuidApi::class)
class GameRepositoryTest : FreeSpec({

    lateinit var repository: FakeGameRepository
    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId1 = Uuid.parse("00000000-0000-0000-0000-000000000101")
    val sessionId2 = Uuid.parse("00000000-0000-0000-0000-000000000102")
    val nonExistentId = Uuid.parse("00000000-0000-0000-0000-000000000999")

    beforeTest {
        repository = FakeGameRepository()
    }

    "GameRepository" - {
        "getAllGameSessions" - {
            "should return empty list when no sessions exist" {
                val sessions = repository.getAllGameSessions().first()
                sessions.shouldBeEmpty()
            }

            "should return all sessions sorted by startedAt descending" {
                val session1 = createSession(sessionId1, listOf(playerId1, playerId2), startedAt = 1000L)
                val session2 = createSession(sessionId2, listOf(playerId1, playerId2), startedAt = 2000L)
                repository.insertSession(session1)
                repository.insertSession(session2)

                val sessions = repository.getAllGameSessions().first()

                sessions shouldHaveSize 2
                sessions[0].id shouldBe sessionId2 // More recent first
                sessions[1].id shouldBe sessionId1
            }
        }

        "getGameSessionById" - {
            "should return null when session does not exist" {
                val session = repository.getGameSessionById(nonExistentId).first()
                session shouldBe null
            }

            "should return session when exists" {
                val testSession = createSession(sessionId1, listOf(playerId1, playerId2))
                repository.insertSession(testSession)

                val session = repository.getGameSessionById(sessionId1).first()

                session shouldNotBe null
                session?.id shouldBe sessionId1
            }
        }

        "getGameSessionsByStatus" - {
            "should return only sessions with matching status" {
                val inProgressSession = createSession(
                    sessionId1,
                    listOf(playerId1, playerId2),
                    status = GameStatus.IN_PROGRESS,
                )
                val completedSession = createSession(
                    sessionId2,
                    listOf(playerId1, playerId2),
                    status = GameStatus.COMPLETED,
                )
                repository.insertSession(inProgressSession)
                repository.insertSession(completedSession)

                val inProgressSessions = repository.getGameSessionsByStatus(GameStatus.IN_PROGRESS).first()
                val completedSessions = repository.getGameSessionsByStatus(GameStatus.COMPLETED).first()

                inProgressSessions shouldHaveSize 1
                inProgressSessions[0].id shouldBe sessionId1
                completedSessions shouldHaveSize 1
                completedSessions[0].id shouldBe sessionId2
            }
        }

        "getInProgressGame" - {
            "should return null when no in-progress game exists" {
                val completedSession = createSession(
                    sessionId1,
                    listOf(playerId1, playerId2),
                    status = GameStatus.COMPLETED,
                )
                repository.insertSession(completedSession)

                val inProgress = repository.getInProgressGame().first()

                inProgress shouldBe null
            }

            "should return in-progress game when exists" {
                val inProgressSession = createSession(
                    sessionId1,
                    listOf(playerId1, playerId2),
                    status = GameStatus.IN_PROGRESS,
                )
                repository.insertSession(inProgressSession)

                val inProgress = repository.getInProgressGame().first()

                inProgress shouldNotBe null
                inProgress?.id shouldBe sessionId1
            }
        }

        "createGameSession" - {
            "should create session with correct config" {
                val config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleIn = false,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2),
                    legsToWin = 3,
                )

                val session = repository.createGameSession(config)

                session.id shouldNotBe null
                session.config.gameType shouldBe GameType.CLASSIC_501
                session.config.doubleOut shouldBe true
                session.config.legsToWin shouldBe 3
                session.config.playerIds shouldHaveSize 2
                session.status shouldBe GameStatus.IN_PROGRESS
                session.legs shouldHaveSize 1
            }

            "should persist created session" {
                val config = GameConfig(
                    gameType = GameType.CLASSIC_301,
                    playerIds = listOf(playerId1, playerId2),
                )

                val created = repository.createGameSession(config)
                val retrieved = repository.getGameSessionById(created.id).first()

                retrieved shouldBe created
            }
        }

        "updateGameSession" - {
            "should update existing session" {
                val session = createSession(sessionId1, listOf(playerId1, playerId2))
                repository.insertSession(session)

                val updatedSession = session.copy(
                    status = GameStatus.COMPLETED,
                    winnerId = playerId1,
                )
                repository.updateGameSession(updatedSession)

                val result = repository.getGameSessionById(sessionId1).first()
                result?.status shouldBe GameStatus.COMPLETED
                result?.winnerId shouldBe playerId1
            }
        }

        "deleteGameSession" - {
            "should remove session from repository" {
                val session = createSession(sessionId1, listOf(playerId1, playerId2))
                repository.insertSession(session)

                repository.deleteGameSession(sessionId1)

                val result = repository.getGameSessionById(sessionId1).first()
                result shouldBe null
            }

            "should not affect other sessions" {
                val session1 = createSession(sessionId1, listOf(playerId1, playerId2))
                val session2 = createSession(sessionId2, listOf(playerId1, playerId2))
                repository.insertSession(session1)
                repository.insertSession(session2)

                repository.deleteGameSession(sessionId1)

                val allSessions = repository.getAllGameSessions().first()
                allSessions shouldHaveSize 1
                allSessions[0].id shouldBe sessionId2
            }
        }
    }
})

@OptIn(ExperimentalUuidApi::class)
private fun createSession(
    id: Uuid,
    playerIds: List<Uuid>,
    gameType: GameType = GameType.CLASSIC_501,
    status: GameStatus = GameStatus.IN_PROGRESS,
    startedAt: Long = currentTimeMillis(),
): GameSession = GameSession(
    id = id,
    config = GameConfig(
        gameType = gameType,
        playerIds = playerIds,
    ),
    legs = listOf(Leg()),
    currentLegIndex = 0,
    status = status,
    startedAt = startedAt,
)
