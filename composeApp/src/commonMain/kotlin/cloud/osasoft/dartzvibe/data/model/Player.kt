package cloud.osasoft.dartzvibe.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    val createdAt: Long,
)
