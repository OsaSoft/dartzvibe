package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.getPlatform
import co.touchlab.kermit.Logger
import me.tatarka.inject.annotations.Inject

/**
 * Repository interface - like a Spring Data repository interface.
 * Defines the contract for data access operations.
 */
interface GreetingRepository {
    suspend fun getGreeting(): String

    suspend fun getGreetingForName(name: String): String
}

/**
 * Repository implementation - like a Spring @Repository class.
 *
 * The @Inject annotation tells kotlin-inject this can be instantiated
 * and its dependencies (constructor parameters) should be injected.
 * Similar to Spring's constructor injection with @Autowired (or implicit in Spring Boot).
 */
@Inject
class GreetingRepositoryImpl : GreetingRepository {

    private val log = Logger.withTag("GreetingRepository")

    override suspend fun getGreeting(): String {
        log.d { "Fetching greeting for platform" }
        val platform = getPlatform()
        return "Hello from ${platform.name}!"
    }

    override suspend fun getGreetingForName(name: String): String {
        log.d { "Fetching greeting for name: $name" }
        val platform = getPlatform()
        return "Hello, $name! Welcome to ${platform.name}!"
    }
}
