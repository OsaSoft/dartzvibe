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
 * Game mode variant.
 */
enum class GameMode(val displayName: String) {
    CLASSIC("Classic"), // Count-down (traditional X01)
    PARCHEESI("Parcheesi"), // Count-up with knockout mechanics
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
    val gameMode: GameMode = GameMode.CLASSIC,
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    val playerIds: List<Uuid>,
    val legsToWin: Int = 1,
) {
    val startingScore: Int
        get() = when (gameMode) {
            GameMode.CLASSIC -> gameType.startingScore
            GameMode.PARCHEESI -> 0
        }

    val targetScore: Int get() = gameType.startingScore

    val isCountUp: Boolean get() = gameMode == GameMode.PARCHEESI

    /**
     * Human-readable description of the game configuration.
     * Example: "Classic 501 Double-Out" or "Parcheesi 301"
     */
    val displayDescription: String
        get() = buildString {
            append(gameMode.displayName)
            append(" ")
            append(gameType.displayName)
            if (doubleIn) append(" Double-In")
            if (doubleOut) append(" Double-Out")
        }
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
 *
 * @property isPhantom True for system-generated turns (e.g., knockout resets).
 *   Phantom turns are excluded from [Leg.playerTurns] and should not be
 *   counted for turn order or displayed in UI.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Turn(
    val playerId: Uuid,
    val throws: List<Throw>,
    val scoreBeforeTurn: Int,
    val scoreAfterTurn: Int,
    val isBust: Boolean = false,
    val isBounce: Boolean = false,
    val isPhantom: Boolean = false,
) {
    val totalScore: Int get() = throws.sumOf { it.score }
}

/**
 * Represents a single leg in a match.
 *
 * @property turns All turns in this leg, including phantom turns (system-generated
 *   turns for knockouts). Use [playerTurns] for display and counting.
 * @property winnerId The player who won this leg, or null if not yet won.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Leg(
    val turns: List<Turn> = emptyList(),
    val winnerId: Uuid? = null,
) {
    /**
     * Player turns only (excludes phantom/system-generated turns).
     * Use this for display, counting, and statistics.
     */
    val playerTurns: List<Turn> get() = turns.filter { !it.isPhantom }
}

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
