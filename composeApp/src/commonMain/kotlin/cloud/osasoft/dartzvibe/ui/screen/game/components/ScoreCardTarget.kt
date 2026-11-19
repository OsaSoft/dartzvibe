package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType

/**
 * The " / T" suffix after a player's score on the score card, or null for just the score.
 *
 * Only modes with a score to reach show it: Parcheesi (the 301/501 to count up to) and Roulette Score (the configured
 * target). Roulette Rounds has no target, and a missing or zero Roulette target (old saves) shows no suffix rather
 * than "/ 0".
 */
fun scoreCardTargetSuffix(
    gameMode: GameMode,
    gameType: GameType,
    rouletteTargetScore: Int?,
): String? {
    val target = when (gameMode) {
        GameMode.CLASSIC, GameMode.CRICKET, GameMode.CHECKOUT_PRACTICE -> null
        GameMode.PARCHEESI -> gameType.startingScore
        GameMode.ROULETTE -> rouletteTargetScore.takeIf { gameType == GameType.ROULETTE_SCORE }
    }
    return target?.takeIf { it > 0 }?.let { " / $it" }
}
