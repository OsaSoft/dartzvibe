package cloud.osasoft.dartzvibe.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Type of darts game.
 */
enum class GameType(val displayName: String, val startingScore: Int) {
    CLASSIC_501("501", 501),
    CLASSIC_301("301", 301),
}

/**
 * Multiplier for a dart throw.
 */
enum class Multiplier(val value: Int) {
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3),
}

/**
 * Status of a game session.
 */
enum class GameStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

/**
 * Configuration for a new game.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class GameConfig(
    val gameType: GameType,
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    val playerIds: List<Uuid>,
    val legsToWin: Int = 1,
) {
    val startingScore: Int get() = gameType.startingScore
}

/**
 * Represents a single dart throw.
 */
@Serializable
data class Throw(
    val segment: Int, // 1-20, 25 (outer bull), 50 (bullseye)
    val multiplier: Multiplier,
) {
    val score: Int get() = segment * multiplier.value
}

/**
 * Represents a player's turn (up to 3 throws).
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Turn(
    val playerId: Uuid,
    val throws: List<Throw>,
    val scoreBeforeTurn: Int,
    val scoreAfterTurn: Int,
    val isBust: Boolean = false,
) {
    val totalScore: Int get() = throws.sumOf { it.score }
}

/**
 * Represents a single leg in a match.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Leg(
    val turns: List<Turn> = emptyList(),
    val winnerId: Uuid? = null,
)

/**
 * Represents a complete game session.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class GameSession(
    val id: Uuid,
    val config: GameConfig,
    val legs: List<Leg> = listOf(Leg()),
    val currentLegIndex: Int = 0,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val winnerId: Uuid? = null,
) {
    val currentLeg: Leg get() = legs[currentLegIndex]
}
