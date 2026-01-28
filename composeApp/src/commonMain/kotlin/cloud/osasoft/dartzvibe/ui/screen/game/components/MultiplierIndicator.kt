package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier

/**
 * Floating indicator that displays the current multiplier during swipe gestures.
 *
 * Shows "DOUBLE" with primary background when swiping up,
 * or "TRIPLE" with tertiary background when swiping down.
 * Hidden when multiplier is null or SINGLE.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun MultiplierIndicator(
    multiplier: Multiplier?,
    modifier: Modifier = Modifier,
) {
    val isVisible = multiplier != null && multiplier != Multiplier.SINGLE

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier,
    ) {
        val backgroundColor = when (multiplier) {
            Multiplier.DOUBLE -> MaterialTheme.colorScheme.primary
            Multiplier.TRIPLE -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        val textColor = when (multiplier) {
            Multiplier.DOUBLE -> MaterialTheme.colorScheme.onPrimary
            Multiplier.TRIPLE -> MaterialTheme.colorScheme.onTertiary
            else -> MaterialTheme.colorScheme.onPrimary
        }
        val text = when (multiplier) {
            Multiplier.DOUBLE -> "DOUBLE"
            Multiplier.TRIPLE -> "TRIPLE"
            else -> ""
        }

        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}
