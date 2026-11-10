package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.Multiplier

private const val BULL_SEGMENT = 25
private const val MISS_SEGMENT = 0

/**
 * The four quick-input buttons shown in Roulette games, in display order.
 */
enum class RouletteInputKind {
    MISS,
    HIT,
    DOUBLE,
    TRIPLE,
}

/**
 * One Roulette quick-input button, aimed at the current target.
 *
 * Pure data (no Compose) so the target to button mapping can be unit tested.
 *
 * @property segment segment the dart is recorded on (0 for a miss)
 * @property points points shown on the button (multiplier value on target, 0 for a miss). Display only:
 * `RouletteModeEngine` does the actual scoring.
 * @property enabled false for the impossible triple bull
 */
data class RouletteInputOption(
    val kind: RouletteInputKind,
    val segment: Int,
    val multiplier: Multiplier,
    val label: String,
    val points: Int,
    val enabled: Boolean,
) {
    /** Points line under the label, e.g. "+2", or why the button is disabled. */
    val pointsLabel: String
        get() = if (enabled) "+$points" else "No triple bull"

    /**
     * One-line description used as the button's accessibility label, e.g. "Double 7 · +2".
     * The pad itself renders [label] and [pointsLabel] on separate lines.
     */
    val fullLabel: String
        get() = "$label · $pointsLabel"
}

/**
 * Maps the current Roulette [target] to the Miss / Hit / Double / Triple buttons.
 *
 * Bull (25) has no triple, so its Triple button is disabled; Hit is the outer bull (+1) and
 * Double the bullseye (+2).
 */
fun rouletteInputOptions(target: Int): List<RouletteInputOption> {
    val isBull = target == BULL_SEGMENT
    return RouletteInputKind.entries.map { kind ->
        when (kind) {
            RouletteInputKind.MISS -> RouletteInputOption(
                kind = kind,
                segment = MISS_SEGMENT,
                multiplier = Multiplier.SINGLE,
                label = "Miss",
                points = 0,
                enabled = true,
            )

            RouletteInputKind.HIT -> targetOption(
                kind = kind,
                target = target,
                multiplier = Multiplier.SINGLE,
                label = if (isBull) "Outer bull" else "Hit $target",
                enabled = true,
            )

            RouletteInputKind.DOUBLE -> targetOption(
                kind = kind,
                target = target,
                multiplier = Multiplier.DOUBLE,
                label = if (isBull) "Bullseye" else "Double $target",
                enabled = true,
            )

            RouletteInputKind.TRIPLE -> targetOption(
                kind = kind,
                target = target,
                multiplier = Multiplier.TRIPLE,
                label = if (isBull) "Triple bull" else "Triple $target",
                enabled = !isBull,
            )
        }
    }
}

private fun targetOption(
    kind: RouletteInputKind,
    target: Int,
    multiplier: Multiplier,
    label: String,
    enabled: Boolean,
): RouletteInputOption = RouletteInputOption(
    kind = kind,
    segment = target,
    multiplier = multiplier,
    label = label,
    points = if (enabled) multiplier.value else 0,
    enabled = enabled,
)
