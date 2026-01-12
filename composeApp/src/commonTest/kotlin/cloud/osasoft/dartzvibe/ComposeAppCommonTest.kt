package cloud.osasoft.dartzvibe

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

/**
 * Example tests using Kotest FreeSpec style.
 *
 * FreeSpec provides a flexible, BDD-style DSL that's more expressive than JUnit.
 * Coming from Spring Boot, you might be used to AssertJ - Kotest is similar but
 * designed for Kotlin with better null safety and coroutine support.
 *
 * Key differences from JUnit/AssertJ:
 * - `shouldBe` instead of `assertEquals` or `assertThat().isEqualTo()`
 * - `shouldContain` instead of `assertThat().contains()`
 * - Infix notation makes tests read more naturally
 * - Nested test blocks with `-` operator for grouping
 */
class ComposeAppCommonTest : FreeSpec(
    {

        "basic assertions" - {
            "should add numbers correctly" {
                val result = 1 + 2
                result shouldBe 3
            }
        }

        "string assertions" - {
            "should match string content" {
                val greeting = "Hello, World!"
                greeting shouldContain "World"
                greeting shouldStartWith "Hello"
            }
        }

        "collection assertions" - {
            "should validate collection size and contents" {
                val numbers = listOf(1, 2, 3, 4, 5)
                numbers shouldHaveSize 5
                numbers shouldContain 3
            }
        }

        "null safety" - {
            "should handle nullable values" {
                val nullableValue: String? = "not null"
                val actualNull: String? = null

                nullableValue shouldNotBe null
                actualNull shouldBe null
            }
        }

        "exception testing" - {
            "should catch expected exceptions" {
                shouldThrow<IllegalArgumentException> {
                    require(false) { "This should fail" }
                }
            }
        }

        "Greeting class" - {
            "should return greeting with Hello" {
                val greeting = Greeting()
                val result = greeting.greet()
                result shouldContain "Hello"
            }
        }
    })
