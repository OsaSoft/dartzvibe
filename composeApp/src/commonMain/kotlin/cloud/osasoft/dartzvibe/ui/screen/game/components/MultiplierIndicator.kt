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
import cloud.osasoft.dartzvibe.ui.theme.MultiplierColors
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors

/**
 * Floating indicator that displays the current multiplier during swipe gestures.
 *
 * Shows "DOUBLE" with the red double accent or "TRIPLE" with the amber triple accent
 * (see [MultiplierColors]), for whichever multiplier the current swipe will record.
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
        val multiplierColors = MaterialTheme.multiplierColors
        // SINGLE/null are never visible; they fall back to the double colors for the exit animation
        val backgroundColor = when (multiplier) {
            Multiplier.TRIPLE -> multiplierColors.triple
            Multiplier.DOUBLE, Multiplier.SINGLE, null -> multiplierColors.double
        }
        val textColor = when (multiplier) {
            Multiplier.TRIPLE -> multiplierColors.onTriple
            Multiplier.DOUBLE, Multiplier.SINGLE, null -> multiplierColors.onDouble
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
