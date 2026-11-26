package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The Scoreboard mode toggle's icon: a phone showing the scoreboard view in miniature (one big number, two standings
 * lines below it). Material's icon set has no scoreboard. Drawn in black; `Icon` tints it.
 */
val ScoreboardModeIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "ScoreboardMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Phone outline
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8.5f, 2f)
                horizontalLineTo(15.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18f, y1 = 4.5f)
                verticalLineTo(19.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.5f, y1 = 22f)
                horizontalLineTo(8.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6f, y1 = 19.5f)
                verticalLineTo(4.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 8.5f, y1 = 2f)
                close()
            }
            // The thrower's big number
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 5f)
                horizontalLineTo(15f)
                verticalLineTo(11.5f)
                horizontalLineTo(9f)
                close()
            }
            // The other players
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(9.5f, 14.75f)
                horizontalLineTo(14.5f)
                moveTo(9.5f, 18f)
                horizontalLineTo(13f)
            }
        }.build()
}
