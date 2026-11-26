package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.ui.screen.game.ActiveGameState
import cloud.osasoft.dartzvibe.ui.screen.game.CheckoutSlotHint
import cloud.osasoft.dartzvibe.ui.theme.AvatarColors
import cloud.osasoft.dartzvibe.util.KeepScreenOn
import cloud.osasoft.dartzvibe.util.LightSystemBarIcons
import dartzvibe.composeapp.generated.resources.Res
import dartzvibe.composeapp.generated.resources.roboto_condensed_bold
import org.jetbrains.compose.resources.Font
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Colours of the scoreboard view. Always dark, whatever the app theme: white and amber on near-black read best from
 * across a room. Amber is the dark theme's treble colour; red is `brandColors.text` of the dark theme.
 */
private object ScoreboardColors {
    val background = Color(0xFF0D0D0E)
    val text = Color(0xFFFFFFFF)
    val muted = Color(0xFFA0A0A6)
    val accent = Color(0xFFFFB300)
    val points = Color(0xFFFF8080)
    val tile = Color(0xFF1E1F22)
    val divider = Color(0xFF333438)
}

/**
 * The layout is designed for a 360 × 720 dp screen and scales with the real one, within these limits. The low floor is
 * for landscape (about 640 × 330 dp), where the portrait layout shrinks to fit the height rather than run off it.
 */
private const val DESIGN_WIDTH_DP = 360f
private const val DESIGN_HEIGHT_DP = 720f
private const val MIN_SCALE = 0.4f
private const val MAX_SCALE = 1.4f

/** How much the scoreboard scales on a [widthDp] × [heightDp] screen: the tighter of the two ratios, clamped. */
fun scoreboardScale(
    widthDp: Float,
    heightDp: Float,
): Float = minOf(widthDp / DESIGN_WIDTH_DP, heightDp / DESIGN_HEIGHT_DP).coerceIn(MIN_SCALE, MAX_SCALE)

/** The other players in throwing order, starting with the one after [currentPlayerId]. */
@OptIn(ExperimentalUuidApi::class)
fun scoreboardStandingsOrder(
    playerIds: List<Uuid>,
    currentPlayerId: Uuid?,
): List<Uuid> {
    val index = playerIds.indexOf(currentPlayerId)
    if (index < 0) return playerIds
    return playerIds.drop(index + 1) + playerIds.take(index)
}

/** The checkout slot's route or knockout as one line for the scoreboard: darts separated by spaces, no arrows. */
private fun String.asScoreboardRoute(): String = replace(" → ", " ")

/**
 * Scoreboard mode's view of the game, readable from the oche: the thrower's name and number in very large type, their
 * route, hint or target, and the other players below in throwing order. The whole view is one tap target that opens
 * the normal game screen ([onOpenScoring]). Keeps the screen on while it shows.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun ScoreboardView(
    state: ActiveGameState,
    settings: AppSettingsData,
    onOpenScoring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The app keeps the screen on already when the setting is on; only add it here when it's off, so leaving the view
    // can't switch off the setting's own keep-on
    if (!settings.keepScreenOn) KeepScreenOn(enabled = true)
    // Dark icons (the light system theme's) would vanish on the near-black background
    LightSystemBarIcons()

    val font = FontFamily(Font(Res.font.roboto_condensed_bold, FontWeight.Bold))
    BoxWithConstraints(
        modifier = modifier
            .background(ScoreboardColors.background)
            .clickable(onClickLabel = "Enter your darts", onClick = onOpenScoring)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val scale = scoreboardScale(widthDp = maxWidth.value, heightDp = maxHeight.value)
        val sizes = ScoreboardSizes(scale = scale, font = font, density = LocalDensity.current)
        Column(modifier = Modifier.fillMaxSize()) {
            TopLine(state = state, sizes = sizes)
            ThrowerBlock(state = state, settings = settings, sizes = sizes)
            Spacer(modifier = Modifier.height(sizes.dp(28)))
            Box(
                modifier = Modifier
                    .padding(horizontal = sizes.dp(22))
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(ScoreboardColors.divider),
            )
            Standings(
                state = state,
                sizes = sizes,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            TapHint(sizes = sizes)
        }
    }
}

/** Every size on the scoreboard, scaled from the 360 × 720 dp design. */
private class ScoreboardSizes(
    val scale: Float,
    val font: FontFamily,
    private val density: Density,
) {
    fun dp(value: Int): Dp = (value * scale).dp

    /**
     * A font size of [value] design dp. In dp, not sp: the layout is sized to fill the screen, and the system font
     * size would push the standings off it. The text is far above any font-size setting anyway.
     */
    fun sp(value: Int): TextUnit = with(density) { dp(value).toSp() }

    fun sp(value: Dp): TextUnit = with(density) { value.toSp() }

    /** A bold condensed style at [size] whose line is exactly as tall as the font size. */
    fun style(size: TextUnit): TextStyle = TextStyle(
        fontFamily = font,
        fontWeight = FontWeight.Bold,
        fontSize = size,
        lineHeight = size,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
}

/**
 * One line of scoreboard text in the width it's given: the font shrinks to fit, down to [minFraction] of [size], then
 * the text is cut with an ellipsis. Never wraps. The line keeps the height of the full-size text however far it
 * shrinks, so nothing below it moves when the thrower or the hint changes.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun FitText(
    text: String,
    size: TextUnit,
    sizes: ScoreboardSizes,
    color: Color,
    modifier: Modifier = Modifier,
    minFraction: Float = 0.55f,
    textAlign: TextAlign = TextAlign.Start,
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val style = LocalTextStyle.current.merge(sizes.style(size))
        val (fitScale, fullHeightPx) = remember(text, style, maxWidthPx, textMeasurer) {
            val measured = textMeasurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size
            fitTextScale(textWidth = measured.width, maxWidth = maxWidthPx).coerceAtLeast(minFraction) to
                measured.height
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { fullHeightPx.toDp() }),
        ) {
            Text(
                text = text,
                style = if (fitScale < 1f) style.copy(fontSize = size * fitScale) else style,
                color = color,
                textAlign = textAlign,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TopLine(
    state: ActiveGameState,
    sizes: ScoreboardSizes,
) {
    val config = state.session?.config
    val left = when (config?.gameMode) {
        GameMode.CLASSIC, GameMode.PARCHEESI, GameMode.CRICKET -> {
            val name = when (config.gameMode) {
                GameMode.PARCHEESI -> "Parcheesi ${config.targetScore}"
                GameMode.CRICKET -> "Cricket · ${config.gameType.displayName}"
                GameMode.CLASSIC, GameMode.ROULETTE, GameMode.CHECKOUT_PRACTICE -> config.gameType.displayName
            }
            val leg = (state.session?.currentLegIndex ?: 0) + 1
            if (config.legsToWin > 1) "$name · Leg $leg" else name
        }

        GameMode.ROULETTE -> "Roulette"

        GameMode.CHECKOUT_PRACTICE -> "Checkout Practice"

        null -> ""
    }
    Text(
        text = left.uppercase(),
        style = sizes.style(sizes.sp(16)),
        color = ScoreboardColors.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(
            start = sizes.dp(22),
            top = sizes.dp(14),
            end = sizes.dp(22),
            bottom = sizes.dp(10),
        ),
    )
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ThrowerBlock(
    state: ActiveGameState,
    settings: AppSettingsData,
    sizes: ScoreboardSizes,
) {
    val mode = state.session?.config?.gameMode ?: return
    val gap = sizes.dp(if (mode == GameMode.CRICKET) 10 else 14)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        when (mode) {
            GameMode.CLASSIC, GameMode.PARCHEESI -> {
                ThrowerName(state = state, sizes = sizes)
                FitText(
                    text = state.currentPlayerScore.toString(),
                    size = sizes.sp(170),
                    sizes = sizes,
                    color = ScoreboardColors.text,
                    minFraction = 0.5f,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sizes.dp(16)),
                )
                // Always laid out, so the standings never move when a route comes or goes
                val hint = when (
                    val slot = state.checkoutSlotHint(
                        settings.showCheckoutHints,
                        settings.showKnockoutHints,
                    )
                ) {
                    is CheckoutSlotHint.Checkout -> checkoutSlotText(slot.options)?.primary

                    is CheckoutSlotHint.Knockout -> knockoutSlotText(slot.routes) { id ->
                        state.getPlayer(id)?.name ?: "Player"
                    }?.primary?.let { "KO $it" }

                    null -> null
                }
                HintLine(text = hint?.asScoreboardRoute(), sizes = sizes)
            }

            GameMode.CRICKET -> {
                ThrowerName(state = state, sizes = sizes)
                val cricketState = state.cricketState
                val playerId = state.currentPlayerId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = sizes.dp(22)),
                ) {
                    Text(
                        text = "POINTS",
                        style = sizes.style(sizes.sp(26)),
                        color = ScoreboardColors.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = (playerId?.let { cricketState?.getPlayerState(it)?.points } ?: 0).toString(),
                        style = sizes.style(sizes.sp(96)),
                        color = ScoreboardColors.points,
                        maxLines = 1,
                    )
                }
                if (cricketState != null && playerId != null) {
                    CricketTiles(
                        cricketState = cricketState,
                        playerIds = state.session?.config?.playerIds.orEmpty(),
                        currentPlayerId = playerId,
                        sizes = sizes,
                    )
                }
            }

            GameMode.ROULETTE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = sizes.dp(22)),
                ) {
                    Text(
                        text = "AIM",
                        style = sizes.style(sizes.sp(56)),
                        color = ScoreboardColors.muted,
                    )
                    Spacer(modifier = Modifier.width(sizes.dp(16)))
                    FitText(
                        text = rouletteRoundLabel(
                            gameType = state.session?.config?.gameType,
                            roundNumber = state.rouletteRoundNumber,
                            totalRounds = state.rouletteTotalRounds,
                            targetScore = state.rouletteTargetScore,
                        ).uppercase(),
                        size = sizes.sp(22),
                        sizes = sizes,
                        color = ScoreboardColors.muted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
                val target = state.currentRouletteTarget
                FitText(
                    text = if (target == 25) "BULL" else target?.toString().orEmpty(),
                    size = sizes.sp(220),
                    sizes = sizes,
                    color = ScoreboardColors.accent,
                    minFraction = 0.5f,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sizes.dp(16)),
                )
                ThrowerName(state = state, sizes = sizes, size = 60, score = state.currentPlayerScore.toString())
            }

            GameMode.CHECKOUT_PRACTICE -> {
                Text(
                    text = "CHECK OUT",
                    style = sizes.style(sizes.sp(50)),
                    color = ScoreboardColors.muted,
                    modifier = Modifier.padding(horizontal = sizes.dp(22)),
                )
                // The darts left this round, from the running score; the target before the first dart
                FitText(
                    text = state.currentPlayerScore.toString(),
                    size = sizes.sp(170),
                    sizes = sizes,
                    color = ScoreboardColors.text,
                    minFraction = 0.5f,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sizes.dp(16)),
                )
                val route = state.visibleCheckoutOptions(settings.showCheckoutHints)
                    ?.let { checkoutSlotText(it) }
                    ?.primary
                HintLine(text = route?.asScoreboardRoute(), sizes = sizes)
            }
        }
    }
}

/** The thrower's avatar colour and name, and in Roulette their [score] at the end of the line. */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ThrowerName(
    state: ActiveGameState,
    sizes: ScoreboardSizes,
    size: Int = 60,
    score: String? = null,
) {
    val player = state.currentPlayerId?.let { state.getPlayer(it) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = sizes.dp(22)),
    ) {
        AvatarDot(colorIndex = player?.avatarColor ?: 0, sizes = sizes)
        Spacer(modifier = Modifier.width(sizes.dp(12)))
        FitText(
            text = (player?.name ?: "Player").uppercase(),
            size = sizes.sp(size),
            sizes = sizes,
            color = ScoreboardColors.text,
            modifier = Modifier.weight(1f),
        )
        if (score != null) {
            Spacer(modifier = Modifier.width(sizes.dp(24)))
            Text(text = score, style = sizes.style(sizes.sp(size)), color = ScoreboardColors.text, maxLines = 1)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun AvatarDot(
    colorIndex: Int,
    sizes: ScoreboardSizes,
) {
    Box(
        modifier = Modifier
            .size(sizes.dp(20))
            .background(AvatarColors.getColor(colorIndex), CircleShape),
    )
}

/** The amber route or hint line, at a fixed height so nothing below moves when it's blank. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun HintLine(
    text: String?,
    sizes: ScoreboardSizes,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.dp(60))
            .padding(horizontal = sizes.dp(22)),
    ) {
        if (text != null) {
            FitText(
                text = text.uppercase(),
                size = sizes.sp(60),
                sizes = sizes,
                color = ScoreboardColors.accent,
                // A long knockout ("KO CHRISTOPHER: T20 T20 T20") shrinks rather than lose the route
                minFraction = 0.3f,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The thrower's marks on each Cricket target, four tiles to a row in the scoreboard's order. A target everyone has
 * closed is dimmed; one the thrower has closed and an opponent hasn't (the thrower can score there) is outlined.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CricketTiles(
    cricketState: CricketState,
    playerIds: List<Uuid>,
    currentPlayerId: Uuid,
    sizes: ScoreboardSizes,
) {
    val segments = cricketState.segments.segments
    Column(
        verticalArrangement = Arrangement.spacedBy(sizes.dp(8)),
        modifier = Modifier.padding(horizontal = sizes.dp(16)),
    ) {
        segments.chunked(4).forEach { rowSegments ->
            Row(horizontalArrangement = Arrangement.spacedBy(sizes.dp(8))) {
                rowSegments.forEach { segment ->
                    val marks = cricketState.getPlayerState(currentPlayerId).getMarks(segment)
                    val closedByAll = playerIds.all { cricketState.getPlayerState(it).isClosed(segment) }
                    val canScore = marks >= 3 && !closedByAll
                    val label = if (segment == 25) "B" else segment.toString()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(1f)
                            .height(sizes.dp(122))
                            .alpha(if (closedByAll) 0.3f else 1f)
                            .background(ScoreboardColors.tile, RoundedCornerShape(sizes.dp(14)))
                            .then(
                                if (canScore) {
                                    Modifier.border(3.dp, ScoreboardColors.accent, RoundedCornerShape(sizes.dp(14)))
                                } else {
                                    Modifier
                                },
                            ).padding(vertical = sizes.dp(10))
                            .semantics { contentDescription = "$label: $marks of 3 marks" },
                    ) {
                        Text(text = label, style = sizes.style(sizes.sp(50)), color = ScoreboardColors.text)
                        CricketMarks(marks = marks, size = sizes.dp(50))
                    }
                }
                // Keep the last row's tiles the same width as the full rows
                repeat(4 - rowSegments.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** Cricket marks drawn large: a dot for none, a slash for one, a cross for two, a circled cross when closed. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CricketMarks(
    marks: Int,
    size: Dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val color = ScoreboardColors.text
        val stroke = this.size.minDimension * 0.13f
        val lo = this.size.minDimension * 0.22f
        val hi = this.size.minDimension - lo
        when {
            marks <= 0 -> drawCircle(color = color.copy(alpha = 0.35f), radius = stroke / 2)

            else -> {
                drawLine(color, Offset(lo, hi), Offset(hi, lo), strokeWidth = stroke, cap = StrokeCap.Round)
                val secondStroke = marks >= 2
                if (secondStroke) {
                    drawLine(color, Offset(lo, lo), Offset(hi, hi), strokeWidth = stroke, cap = StrokeCap.Round)
                }
                if (marks >= 3) {
                    drawCircle(
                        color = color,
                        radius = this.size.minDimension / 2 - stroke / 3,
                        style = Stroke(width = stroke * 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * The other players in throwing order (or, in Checkout Practice, the round and hits), filling the height that's
 * left. Names shrink to fit before they're cut; scores never shrink.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun Standings(
    state: ActiveGameState,
    sizes: ScoreboardSizes,
    modifier: Modifier = Modifier,
) {
    val mode = state.session?.config?.gameMode ?: return
    val rowSize = sizes.sp(
        when (mode) {
            GameMode.CLASSIC, GameMode.PARCHEESI -> 72

            GameMode.CRICKET -> 48

            // Smaller than the thrower's line above, so the thrower stands out
            GameMode.ROULETTE -> 42

            GameMode.CHECKOUT_PRACTICE -> 60
        },
    )
    val rows: List<Triple<Int?, String, String>> = if (mode == GameMode.CHECKOUT_PRACTICE) {
        val practice = state.checkoutPracticeState
        listOf(
            Triple(null, "ROUND", "${state.checkoutRoundNumber}/${state.checkoutTotalRounds}"),
            Triple(null, "HITS", "${practice?.successCount ?: 0}/${practice?.roundResults?.size ?: 0}"),
        )
    } else {
        scoreboardStandingsOrder(state.session?.config?.playerIds.orEmpty(), state.currentPlayerId).map { id ->
            val player = state.getPlayer(id)
            val value = if (mode == GameMode.CRICKET) {
                state.cricketState?.getPlayerState(id)?.points ?: 0
            } else {
                state.getPlayerScore(id)
            }
            Triple(player?.avatarColor ?: 0, (player?.name ?: "Player").uppercase(), value.toString())
        }
    }
    BoxWithConstraints(modifier = modifier.padding(horizontal = sizes.dp(22), vertical = sizes.dp(6))) {
        // Short phones: the rows shrink until all of them fit the height the thrower's block leaves
        val heightFit = sizes.sp(maxHeight / rows.size.coerceAtLeast(1) * 0.8f)
        // One or two rows (2-3 players, Practice) stay smaller than the thrower's name, which also leaves room for
        // whole names next to the score
        val cap = if (rows.size <= 2) minOf(rowSize.value, sizes.sp(FEW_ROWS_SIZE_DP).value) else rowSize.value
        val fittedSize = minOf(heightFit.value, cap).sp
        StandingsRows(rows = rows, rowSize = fittedSize, sizes = sizes)
    }
}

/**
 * The standings rows. Every name is drawn at one shared size, the size the longest one needs (down to 55% of
 * [rowSize]; a name still too wide is cut), so the column reads as one list. Scores keep [rowSize].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun StandingsRows(
    rows: List<Triple<Int?, String, String>>,
    rowSize: TextUnit,
    sizes: ScoreboardSizes,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val textMeasurer = rememberTextMeasurer()
        val style = LocalTextStyle.current.merge(sizes.style(rowSize))
        val density = LocalDensity.current
        val dotPx = with(density) { (sizes.dp(20) + sizes.dp(12)).roundToPx() }
        val gapPx = with(density) { sizes.dp(24).roundToPx() }
        val maxWidthPx = constraints.maxWidth
        val nameScale = remember(rows, style, maxWidthPx, textMeasurer) {
            rows.minOfOrNull { (avatarColor, name, value) ->
                fun width(text: String) = textMeasurer.measure(text, style, maxLines = 1, softWrap = false).size.width
                val nameWidth = maxWidthPx - (if (avatarColor != null) dotPx else 0) - gapPx - width(value)
                fitTextScale(textWidth = width(name), maxWidth = nameWidth)
            }?.coerceAtLeast(MIN_STANDINGS_NAME_SCALE) ?: 1f
        }
        val nameStyle = if (nameScale < 1f) style.copy(fontSize = rowSize * nameScale) else style
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize(),
        ) {
            rows.forEach { (avatarColor, name, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (avatarColor != null) {
                        AvatarDot(colorIndex = avatarColor, sizes = sizes)
                        Spacer(modifier = Modifier.width(sizes.dp(12)))
                    }
                    Text(
                        text = name,
                        style = nameStyle,
                        color = if (avatarColor == null) ScoreboardColors.muted else ScoreboardColors.text,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(sizes.dp(24)))
                    Text(text = value, style = style, color = ScoreboardColors.text, maxLines = 1)
                }
            }
        }
    }
}

/** The standings row size, in design dp, when there are only one or two rows. */
private const val FEW_ROWS_SIZE_DP = 54

/** How far the standings names shrink before the longest is cut: big text matters more than whole names. */
private const val MIN_STANDINGS_NAME_SCALE = 0.55f

/** Only readable up close, which is where it's needed. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun TapHint(sizes: ScoreboardSizes) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = sizes.dp(6), bottom = sizes.dp(14)),
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            tint = ScoreboardColors.muted,
            modifier = Modifier.size(sizes.dp(18)),
        )
        Spacer(modifier = Modifier.width(sizes.dp(8)))
        Text(
            text = "Tap anywhere to enter your darts",
            fontSize = sizes.sp(16),
            color = ScoreboardColors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
