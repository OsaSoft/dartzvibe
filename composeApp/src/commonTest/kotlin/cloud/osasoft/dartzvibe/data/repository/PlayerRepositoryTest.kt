package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for PlayerRepository using a fake implementation.
 * These tests verify the repository contract/interface behavior.
 */
@OptIn(ExperimentalUuidApi::class)
class PlayerRepositoryTest : FreeSpec({

    lateinit var repository: FakePlayerRepository
    val uuid1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val uuid2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val uuid3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val testUuid = Uuid.parse("00000000-0000-0000-0000-000000000010")
    val nonExistentUuid = Uuid.parse("00000000-0000-0000-0000-000000000099")

    beforeTest {
        repository = FakePlayerRepository()
    }

    "PlayerRepository" - {
        "getAllPlayers" - {
            "should return empty list when no players exist" {
                val players = repository.getAllPlayers().first()
                players.shouldBeEmpty()
            }

            "should return all players sorted by name" {
                repository.insertPlayer(createPlayer(uuid1, "Zack"))
                repository.insertPlayer(createPlayer(uuid2, "Alice"))
                repository.insertPlayer(createPlayer(uuid3, "Mike"))

                val players = repository.getAllPlayers().first()

                players shouldHaveSize 3
                players[0].name shouldBe "Alice"
                players[1].name shouldBe "Mike"
                players[2].name shouldBe "Zack"
            }
        }

        "getPlayerById" - {
            "should return null when player does not exist" {
                val player = repository.getPlayerById(nonExistentUuid).first()
                player shouldBe null
            }

            "should return player when exists" {
                val testPlayer = createPlayer(testUuid, "Test Player")
                repository.insertPlayer(testPlayer)

                val player = repository.getPlayerById(testUuid).first()

                player shouldNotBe null
                player?.name shouldBe "Test Player"
            }
        }

        "insertPlayer" - {
            "should add player to repository" {
                val player = createPlayer(uuid1, "New Player")

                repository.insertPlayer(player)

                val allPlayers = repository.getAllPlayers().first()
                allPlayers shouldHaveSize 1
                allPlayers shouldContain player
            }
        }

        "updatePlayer" - {
            "should update existing player" {
                val player = createPlayer(uuid1, "Original Name")
                repository.insertPlayer(player)

                val updatedPlayer = player.copy(name = "Updated Name")
                repository.updatePlayer(updatedPlayer)

                val result = repository.getPlayerById(uuid1).first()
                result?.name shouldBe "Updated Name"
            }
        }

        "deletePlayer" - {
            "should remove player from repository" {
                val player = createPlayer(uuid1, "To Delete")
                repository.insertPlayer(player)

                repository.deletePlayer(uuid1)

                val result = repository.getPlayerById(uuid1).first()
                result shouldBe null
            }

            "should not affect other players" {
                repository.insertPlayer(createPlayer(uuid1, "Player 1"))
                repository.insertPlayer(createPlayer(uuid2, "Player 2"))

                repository.deletePlayer(uuid1)

                val allPlayers = repository.getAllPlayers().first()
                allPlayers shouldHaveSize 1
                allPlayers[0].id shouldBe uuid2
            }
        }

        "createPlayer" - {
            "should create player with generated ID" {
                val player = repository.createPlayer(
                    name = "Created Player",
                    nickname = "CP",
                    avatarColor = 3,
                )

                player.id shouldNotBe null
                player.name shouldBe "Created Player"
                player.nickname shouldBe "CP"
                player.avatarColor shouldBe 3
            }

            "should persist created player" {
                val player = repository.createPlayer(
                    name = "Persisted Player",
                    nickname = null,
                    avatarColor = 0,
                )

                val retrieved = repository.getPlayerById(player.id).first()
                retrieved shouldBe player
            }
        }
    }
})

@OptIn(ExperimentalUuidApi::class)
private fun createPlayer(id: Uuid, name: String, nickname: String? = null): Player = Player(
    id = id,
    name = name,
    nickname = nickname,
    avatarColor = 0,
    createdAt = currentTimeMillis(),
)
