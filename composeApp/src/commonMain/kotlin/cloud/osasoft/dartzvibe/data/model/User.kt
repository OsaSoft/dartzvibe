package cloud.osasoft.dartzvibe.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Example data class with Kotlinx Serialization.
 *
 * Similar to using @JsonProperty in Jackson, but with compile-time safety.
 * The @Serializable annotation generates serialization code at compile time.
 */
@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,

    // Like @JsonProperty("created_at") in Jackson
    @SerialName("created_at")
    val createdAt: String? = null,

    // Like @JsonProperty("is_active") in Jackson
    @SerialName("is_active")
    val isActive: Boolean = true
)

/**
 * Example API response wrapper - common pattern in REST APIs
 */
@Serializable
data class ApiResponse<T>(
    val data: T,
    val message: String? = null,
    val success: Boolean = true
)

