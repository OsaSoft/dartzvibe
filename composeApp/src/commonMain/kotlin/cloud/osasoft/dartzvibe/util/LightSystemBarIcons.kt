package cloud.osasoft.dartzvibe.util

import androidx.compose.runtime.Composable

/**
 * While in the composition, draws the status and navigation bar icons light, for content that is always dark whatever
 * the theme (the scoreboard view). The previous look comes back when it leaves. No-op on iOS.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
expect fun LightSystemBarIcons()
