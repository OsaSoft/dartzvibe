package cloud.osasoft.dartzvibe.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HTTP clients.
 *
 * Similar to how you might configure RestTemplate or WebClient in Spring Boot,
 * this sets up Ktor client with JSON serialization, logging, and timeouts.
 */
object HttpClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true  // Like @JsonIgnoreProperties(ignoreUnknown = true)
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun create(): HttpClient {
        return HttpClient {
            // JSON serialization - like Jackson in Spring
            install(ContentNegotiation) {
                json(json)
            }

            // Logging - like Spring's RestTemplate interceptors
            install(Logging) {
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        Logger.d("HTTP") { message }
                    }
                }
                level = LogLevel.INFO
            }

            // Timeouts - like RestTemplate's timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            // Default request configuration
            defaultRequest {
                // Add common headers here if needed
                // header("Authorization", "Bearer token")
            }
        }
    }
}

