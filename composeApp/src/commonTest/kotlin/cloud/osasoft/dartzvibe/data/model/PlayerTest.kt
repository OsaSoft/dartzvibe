package cloud.osasoft.dartzvibe.data.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for Player data model serialization.
 */
@OptIn(ExperimentalUuidApi::class)
class PlayerTest : FreeSpec({

    val json = Json { prettyPrint = true }
    val testUuid1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val testUuid2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val testUuid3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val testUuid4 = Uuid.parse("00000000-0000-0000-0000-000000000004")
    val testUuid5 = Uuid.parse("00000000-0000-0000-0000-000000000005")

    "Player" - {
        "serialization" - {
            "should serialize player with all fields" {
                val player = Player(
                    id = testUuid1,
                    name = "John Doe",
                    nickname = "Johnny",
                    avatarColor = 2,
                    createdAt = 1705000000000L
                )

                val jsonString = json.encodeToString(player)

                jsonString shouldNotBe null
            }

            "should serialize player without nickname" {
                val player = Player(
                    id = testUuid2,
                    name = "Jane Doe",
                    nickname = null,
                    avatarColor = 0,
                    createdAt = 1705000000000L
                )

                val jsonString = json.encodeToString(player)

                jsonString shouldNotBe null
            }

            "should deserialize player correctly" {
                val jsonString = """
                    {
                        "id": "00000000-0000-0000-0000-000000000003",
                        "name": "Test Player",
                        "nickname": "Tester",
                        "avatarColor": 5,
                        "createdAt": 1705000000000
                    }
                """.trimIndent()

                val player = json.decodeFromString<Player>(jsonString)

                player.id shouldBe testUuid3
                player.name shouldBe "Test Player"
                player.nickname shouldBe "Tester"
                player.avatarColor shouldBe 5
                player.createdAt shouldBe 1705000000000L
            }

            "should deserialize player with null nickname" {
                val jsonString = """
                    {
                        "id": "00000000-0000-0000-0000-000000000004",
                        "name": "No Nickname",
                        "avatarColor": 0,
                        "createdAt": 1705000000000
                    }
                """.trimIndent()

                val player = json.decodeFromString<Player>(jsonString)

                player.nickname shouldBe null
            }
        }

        "defaults" - {
            "should have default avatarColor of 0" {
                val player = Player(
                    id = testUuid5,
                    name = "Test",
                    createdAt = 0L
                )

                player.avatarColor shouldBe 0
            }

            "should have default nickname of null" {
                val player = Player(
                    id = testUuid5,
                    name = "Test",
                    createdAt = 0L
                )

                player.nickname shouldBe null
            }
        }
    }
})
