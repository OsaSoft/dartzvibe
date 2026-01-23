package cloud.osasoft.dartzvibe.data.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for User serialization/deserialization using Kotest FreeSpec.
 *
 * Similar to how you'd test Jackson serialization in Spring Boot,
 * but using Kotlinx Serialization.
 */
class UserSerializationTest : FreeSpec({

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    "User serialization" - {
        "should serialize User to JSON" {
            val user = User(
                id = 1,
                name = "John Doe",
                email = "john@example.com",
                createdAt = "2024-01-01T00:00:00Z",
                isActive = true,
            )

            val jsonString = json.encodeToString(user)

            jsonString shouldNotBe null
            jsonString.contains("John Doe") shouldBe true
            jsonString.contains("created_at") shouldBe true
            jsonString.contains("is_active") shouldBe true
        }

        "should deserialize JSON to User" {
            val jsonString =
                """
                {
                    "id": 2,
                    "name": "Jane Doe",
                    "email": "jane@example.com",
                    "created_at": "2024-06-15T12:00:00Z",
                    "is_active": false
                }
                """.trimIndent()

            val user = json.decodeFromString<User>(jsonString)

            user.id shouldBe 2
            user.name shouldBe "Jane Doe"
            user.email shouldBe "jane@example.com"
            user.createdAt shouldBe "2024-06-15T12:00:00Z"
            user.isActive shouldBe false
        }

        "should handle default values" {
            val jsonString =
                """
                {
                    "id": 3,
                    "name": "Default User",
                    "email": "default@example.com"
                }
                """.trimIndent()

            val user = json.decodeFromString<User>(jsonString)

            user.createdAt shouldBe null
            user.isActive shouldBe true
        }
    }

    "ApiResponse serialization" - {
        "should serialize ApiResponse with User" {
            val response = ApiResponse(
                data = User(id = 1, name = "Test", email = "test@test.com"),
                message = "Success",
                success = true,
            )

            val jsonString = json.encodeToString(response)

            jsonString.contains("Test") shouldBe true
            jsonString.contains("Success") shouldBe true
        }
    }
})
