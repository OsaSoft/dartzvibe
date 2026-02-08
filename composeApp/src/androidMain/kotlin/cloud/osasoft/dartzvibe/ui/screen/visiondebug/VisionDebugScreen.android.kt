package cloud.osasoft.dartzvibe.ui.screen.visiondebug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cloud.osasoft.dartzvibe.domain.detection.MockDartDetector
import cloud.osasoft.dartzvibe.domain.detection.OnnxDartDetector
import cloud.osasoft.dartzvibe.ui.screen.visiondebug.components.CameraPreviewView

@Composable
actual fun rememberVisionDebugScreenModel(): VisionDebugScreenModel {
    val context = LocalContext.current
    return remember {
        val detector = try {
            context.assets.open("dart_detect.onnx").close()
            OnnxDartDetector(context)
        } catch (_: Exception) {
            MockDartDetector()
        }
        VisionDebugScreenModel(detector)
    }
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
