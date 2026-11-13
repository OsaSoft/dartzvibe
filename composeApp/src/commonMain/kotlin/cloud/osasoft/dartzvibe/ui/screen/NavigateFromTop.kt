package cloud.osasoft.dartzvibe.ui.screen

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator

/**
 * Whether [current] may navigate: only while it is the top of the stack ([top]).
 *
 * A second tap during the slide comes from a screen that is no longer on top. Letting it through would stack two
 * screens for the same thing (e.g. Home, game, game), and since each keeps its own ScreenModel, the covered game's
 * stale engine could later save over the other one's progress.
 */
fun canNavigateFrom(top: Screen?, current: Screen): Boolean = top?.key == current.key

/** Pushes [next] only if [current] is still the top screen; see [canNavigateFrom]. */
fun Navigator.pushFrom(current: Screen, next: Screen) {
    if (canNavigateFrom(top = lastItemOrNull, current = current)) push(next)
}

/** Replaces [current] with [next] only if [current] is still the top screen; see [canNavigateFrom]. */
fun Navigator.replaceFrom(current: Screen, next: Screen) {
    if (canNavigateFrom(top = lastItemOrNull, current = current)) replace(next)
}
