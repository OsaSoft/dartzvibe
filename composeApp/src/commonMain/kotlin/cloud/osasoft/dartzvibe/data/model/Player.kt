package cloud.osasoft.dartzvibe.data.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Represents a player profile in the darts game.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Player(
    val id: Uuid,
    val name: String,
    val nickname: String? = null,
    val avatarColor: Int = 0,
    val createdAt: Long
)
