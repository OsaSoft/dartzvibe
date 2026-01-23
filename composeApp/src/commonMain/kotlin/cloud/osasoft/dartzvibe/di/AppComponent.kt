package cloud.osasoft.dartzvibe.di

import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.repository.GreetingRepository
import cloud.osasoft.dartzvibe.data.repository.GreetingRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.GameRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepositoryImpl
import cloud.osasoft.dartzvibe.network.HttpClientFactory
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/**
 * Main application component for dependency injection.
 *
 * Think of this like a Spring @Configuration class - it defines how
 * dependencies are created and wired together.
 *
 * Unlike Spring's runtime reflection-based DI, kotlin-inject generates
 * code at compile time, making it faster and safer.
 *
 * Usage:
 * ```
 * val database = DatabaseDriverFactory(context).createDriver().let { DartzVibeDatabase(it) }
 * val component = AppComponent::class.create(database)
 * val repository = component.playerRepository
 * ```
 */
@Component
abstract class AppComponent(
    @get:Provides val database: DartzVibeDatabase
) {

    /**
     * Provides the HTTP client - similar to defining a @Bean in Spring
     */
    @Provides
    fun provideHttpClient(): HttpClient = HttpClientFactory.create()

    /**
     * Binds the repository interface to its implementation.
     */
    abstract val greetingRepository: GreetingRepository

    @Provides
    fun provideGreetingRepository(impl: GreetingRepositoryImpl): GreetingRepository = impl

    /**
     * Player repository for managing player profiles.
     */
    abstract val playerRepository: PlayerRepository

    @Provides
    fun providePlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository = impl

    /**
     * Game repository for managing game sessions.
     */
    abstract val gameRepository: GameRepository

    @Provides
    fun provideGameRepository(impl: GameRepositoryImpl): GameRepository = impl
}
