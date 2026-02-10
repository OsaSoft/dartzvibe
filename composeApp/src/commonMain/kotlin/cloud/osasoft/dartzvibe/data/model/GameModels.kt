package cloud.osasoft.dartzvibe.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Type of darts game.
 */
enum class GameType(
    val displayName: String,
    val description: String? = null,
    val startingScore: Int,
) {
    CLASSIC_501(
        displayName = "501",
        startingScore = 501,
    ),
    CLASSIC_301(
        displayName = "301",
        startingScore = 301,
    ),
    CRICKET_REGULAR(
        displayName = "Regular",
        description = "Standard Cricket with segments 15-20 and bull",
        startingScore = 0,
    ),
    CRICKET_RANDOM(
        displayName = "Random",
        description = "Cricket with 7 random segments",
        startingScore = 0,
    ),
    CHECKOUT_EASY(
        displayName = "Easy",
        description = "Targets from 2-40",
        startingScore = 0,
    ),
    CHECKOUT_MEDIUM(
        displayName = "Medium",
        description = "Targets from 41-100",
        startingScore = 0,
    ),
    CHECKOUT_HARD(
        displayName = "Hard",
        description = "Targets from 101-170",
        startingScore = 0,
    ),
    CHECKOUT_FULL(
        displayName = "Full",
        description = "Targets from 2-170",
        startingScore = 0,
    ),
    ROULETTE_ROUNDS(
        displayName = "Rounds",
        description = "Play a fixed number of rounds",
        startingScore = 0,
    ),
    ROULETTE_SCORE(
        displayName = "Score",
        description = "First to reach the target score",
        startingScore = 0,
    ),
}

/**
 * Game mode variant.
 */
enum class GameMode(val displayName: String, val supportedTypes: List<GameType>) {
    CLASSIC("Classic", listOf(GameType.CLASSIC_501, GameType.CLASSIC_301)),
    PARCHEESI("Parcheesi", listOf(GameType.CLASSIC_501, GameType.CLASSIC_301)),
    CRICKET("Cricket", listOf(GameType.CRICKET_REGULAR, GameType.CRICKET_RANDOM)),
    CHECKOUT_PRACTICE(
        "Checkout",
        listOf(GameType.CHECKOUT_EASY, GameType.CHECKOUT_MEDIUM, GameType.CHECKOUT_HARD, GameType.CHECKOUT_FULL),
    ),
    ROULETTE(
        "Roulette",
        listOf(GameType.ROULETTE_ROUNDS, GameType.ROULETTE_SCORE),
    ),
}

/**
 * Defines which segments are active in a Cricket game.
 * Standard Cricket uses 15-20 and bull (25).
 * Random Cricket picks 7 random segments from all available.
 */
@Serializable
data class CricketSegments(
    val segments: List<Int> = STANDARD_CRICKET_SEGMENTS,
) {
    companion object {
        val STANDARD_CRICKET_SEGMENTS = listOf(20, 19, 18, 17, 16, 15, 25)

        fun standard(): CricketSegments = CricketSegments()

        fun random(): CricketSegments = CricketSegments(
            segments = ((1..20).toList() + 25).shuffled().take(7).sortedDescending(),
        )
    }

    fun isTarget(segment: Int): Boolean = segment in segments
}

/**
 * Cricket state for a single player.
 */
@Serializable
data class CricketPlayerState(
    val marks: Map<Int, Int> = emptyMap(), // segment -> marks (0-3)
    val points: Int = 0,
) {
    fun getMarks(segment: Int): Int = marks.getOrElse(segment) { 0 }

    fun isClosed(segment: Int): Boolean = getMarks(segment) >= 3
}

/**
 * Cricket game state for the current leg.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class CricketState(
    val segments: CricketSegments = CricketSegments.standard(),
    val playerStates: Map<Uuid, CricketPlayerState> = emptyMap(),
) {
    fun getPlayerState(playerId: Uuid): CricketPlayerState =
        playerStates.getOrElse(playerId) { CricketPlayerState() }

    fun withPlayerState(playerId: Uuid, state: CricketPlayerState): CricketState = copy(
        playerStates = playerStates + (playerId to state),
    )
}

/**
 * State tracking for roulette mode within a leg.
 */
@Serializable
data class RouletteState(
    val currentRoundIndex: Int = 0,
)

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
    val cricketSegments: CricketSegments? = null,
    val checkoutPracticeTargets: List<Int>? = null,
    val rouletteTargetSegments: List<Int>? = null,
    val rouletteRounds: Int? = null,
    val rouletteTargetScore: Int? = null,
) {
    val startingScore: Int
        get() = when (gameMode) {
            GameMode.CLASSIC -> gameType.startingScore
            GameMode.PARCHEESI -> 0
            GameMode.CRICKET -> 0
            GameMode.CHECKOUT_PRACTICE -> checkoutPracticeTargets?.firstOrNull() ?: 0
            GameMode.ROULETTE -> 0
        }

    val targetScore: Int get() = gameType.startingScore

    val isCountUp: Boolean get() = gameMode in listOf(GameMode.PARCHEESI, GameMode.ROULETTE)

    val isCricket: Boolean get() = gameMode == GameMode.CRICKET

    val isCheckoutPractice: Boolean get() = gameMode == GameMode.CHECKOUT_PRACTICE

    val isRoulette: Boolean get() = gameMode == GameMode.ROULETTE

    /**
     * Human-readable description of the game configuration.
     * Example: "Classic 501 Double-Out" or "Parcheesi 301" or "Cricket"
     */
    val displayDescription: String
        get() = buildString {
            append(gameMode.displayName)
            append(" ")
            append(gameType.displayName)
            if (gameMode in listOf(GameMode.CLASSIC, GameMode.PARCHEESI)) {
                if (doubleIn) append(" Double-In")
                if (doubleOut) append(" Double-Out")
            }
            if (isCheckoutPractice) {
                val rounds = checkoutPracticeTargets?.size ?: 0
                append(" ($rounds rounds)")
            }
            if (isRoulette) {
                when (gameType) {
                    GameType.ROULETTE_ROUNDS -> append(" (${rouletteRounds ?: 0} rounds)")
                    GameType.ROULETTE_SCORE -> append(" (target: ${rouletteTargetScore ?: 0})")
                    else -> {}
                }
            }
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
 * Result of a single checkout practice round.
 */
@Serializable
data class CheckoutRoundResult(
    val target: Int,
    val success: Boolean,
    val dartsUsed: Int,
    val throws: List<Throw>,
)

/**
 * State tracking for checkout practice mode.
 */
@Serializable
data class CheckoutPracticeState(
    val targets: List<Int>,
    val currentRoundIndex: Int = 0,
    val roundResults: List<CheckoutRoundResult> = emptyList(),
) {
    val currentTarget: Int get() = targets[currentRoundIndex]
    val totalRounds: Int get() = targets.size
    val isComplete: Boolean get() = roundResults.size >= totalRounds
    val successCount: Int get() = roundResults.count { it.success }
}

/**
 * Represents a single leg in a match.
 *
 * @property turns All turns in this leg, including phantom turns (system-generated
 *   turns for knockouts). Use [playerTurns] for display and counting.
 * @property winnerId The player who won this leg, or null if not yet won.
 * @property cricketState Cricket-specific state (marks and points per player).
 * @property checkoutPracticeState Checkout practice-specific state (targets and results).
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Leg(
    val turns: List<Turn> = emptyList(),
    val winnerId: Uuid? = null,
    val cricketState: CricketState? = null,
    val checkoutPracticeState: CheckoutPracticeState? = null,
    val rouletteState: RouletteState? = null,
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
