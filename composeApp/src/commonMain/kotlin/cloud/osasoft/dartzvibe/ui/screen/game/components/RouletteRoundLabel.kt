package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.GameType

/**
 * The round line above the target segment in the Roulette header, such as "Round 3 of 10" or "Round 2 - Target: 100 pts".
 *
 * Games saved without a Roulette target (null or 0) show just the round rather than "Target: 0 pts".
 */
fun rouletteRoundLabel(
    gameType: GameType?,
    roundNumber: Int,
    totalRounds: Int?,
    targetScore: Int?,
): String = when (gameType) {
    GameType.ROULETTE_ROUNDS -> "Round $roundNumber of ${totalRounds ?: 0}"

    GameType.ROULETTE_SCORE -> targetScore?.takeIf { it > 0 }
        ?.let { "Round $roundNumber - Target: $it pts" }
        ?: "Round $roundNumber"

    else -> "Round $roundNumber"
}
