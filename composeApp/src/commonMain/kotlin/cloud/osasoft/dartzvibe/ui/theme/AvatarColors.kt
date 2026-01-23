package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Predefined avatar colors for player profiles.
 */
object AvatarColors {
    private val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFF44336), // Red
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFE91E63), // Pink
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF3F51B5), // Indigo
        Color(0xFFCDDC39), // Lime
        Color(0xFF009688)  // Teal
    )

    fun getColor(index: Int): Color {
        return colors[index.coerceIn(0, colors.lastIndex)]
    }

    fun getColorCount(): Int = colors.size

    fun getAllColors(): List<Color> = colors
}
