package cloud.osasoft.dartzvibe.ui.screen.visiondebug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun rememberVisionDebugScreenModel(): VisionDebugScreenModel = remember {
    VisionDebugScreenModel(null)
}

@Suppress("ktlint:standard:function-naming")
@Composable
actual fun CameraPreviewContent(
    onFrameAvailable: (ByteArray, Int, Int) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Camera not available on iOS yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
