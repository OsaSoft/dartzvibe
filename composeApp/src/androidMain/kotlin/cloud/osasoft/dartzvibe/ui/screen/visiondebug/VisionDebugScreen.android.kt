package cloud.osasoft.dartzvibe.ui.screen.visiondebug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cloud.osasoft.dartzvibe.domain.detection.MockDartDetector
import cloud.osasoft.dartzvibe.ui.screen.visiondebug.components.CameraPreviewView

@Composable
actual fun rememberVisionDebugScreenModel(): VisionDebugScreenModel = remember {
    VisionDebugScreenModel(MockDartDetector())
}

@Suppress("ktlint:standard:function-naming")
@Composable
actual fun CameraPreviewContent(
    onFrameAvailable: (ByteArray, Int, Int) -> Unit,
    modifier: Modifier,
) {
    CameraPreviewView(
        onFrameAvailable = onFrameAvailable,
        modifier = modifier,
    )
}
