package cloud.osasoft.dartzvibe.di

import cloud.osasoft.dartzvibe.data.repository.GreetingRepository
import cloud.osasoft.dartzvibe.data.repository.GreetingRepositoryImpl
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
 * val component = AppComponent::class.create()
 * val repository = component.greetingRepository
 * ```
 */
@Component
abstract class AppComponent {

    /**
     * Provides the HTTP client - similar to defining a @Bean in Spring
     */
    @Provides
    fun provideHttpClient(): HttpClient = HttpClientFactory.create()

    /**
     * Binds the repository interface to its implementation.
     * This is like Spring's component scanning but explicit.
     */
    abstract val greetingRepository: GreetingRepository

    /**
     * Provides the implementation for GreetingRepository.
     * The @Provides annotation is like @Bean in Spring.
     */
    @Provides
    fun provideGreetingRepository(impl: GreetingRepositoryImpl): GreetingRepository = impl
}
