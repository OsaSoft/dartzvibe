package cloud.osasoft.dartzvibe.data.repository

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty

/**
 * Tests for GreetingRepository using Kotest FreeSpec.
 *
 * Similar to testing a Spring @Repository class,
 * but using Kotest's coroutine-friendly testing style.
 */
class GreetingRepositoryTest : FreeSpec({

    val repository = GreetingRepositoryImpl()

    "GreetingRepository" - {
        "getGreeting" - {
            "should return a greeting containing Hello" {
                val greeting = repository.getGreeting()

                greeting.shouldNotBeEmpty()
                greeting shouldContain "Hello"
            }

            "should include platform name in greeting" {
                val greeting = repository.getGreeting()

                greeting shouldContain "from"
            }
        }

        "getGreetingForName" - {
            "should return personalized greeting" {
                val name = "Alice"
                val greeting = repository.getGreetingForName(name)

                greeting shouldContain name
                greeting shouldContain "Hello"
            }

            "should welcome user to platform" {
                val greeting = repository.getGreetingForName("Bob")

                greeting shouldContain "Welcome"
            }
        }
    }
})
