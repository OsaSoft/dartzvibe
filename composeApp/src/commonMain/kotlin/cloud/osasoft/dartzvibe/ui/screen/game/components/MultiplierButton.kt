package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors

/**
 * S/D/T selector button shared by [ScoreInputKeypad] and [RadialDartboard].
 *
 * When selected, Double and Triple use the dedicated multiplier accents so the active
 * multiplier reads the same as the throw chips; Single uses the secondary (dartboard green).
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun MultiplierButton(
    multiplier: Multiplier,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = when (multiplier) {
        Multiplier.SINGLE -> "Single"
        Multiplier.DOUBLE -> "Double"
        Multiplier.TRIPLE -> "Triple"
    }

    if (isSelected) {
        val multiplierColors = MaterialTheme.multiplierColors
        val (containerColor, contentColor) = when (multiplier) {
            Multiplier.SINGLE -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            Multiplier.DOUBLE -> multiplierColors.double to multiplierColors.onDouble
            Multiplier.TRIPLE -> multiplierColors.triple to multiplierColors.onTriple
        }
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            Text(text, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
        }
    }
}
