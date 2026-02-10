package cloud.osasoft.dartzvibe.ui.screen.visiondebug

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.domain.detection.DartDetector
import cloud.osasoft.dartzvibe.domain.detection.DetectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisionDebugState(
    val detectionStatus: DetectionStatus = DetectionStatus.Idle,
    val isDetectionEnabled: Boolean = true,
    val showBoardOverlay: Boolean = true,
    val showRingGuides: Boolean = true,
    val cameraPermissionGranted: Boolean = false,
    val modelError: String? = null,
)

class VisionDebugScreenModel(
    private val dartDetector: DartDetector?,
) : ScreenModel {

    private val _state = MutableStateFlow(
        VisionDebugState(
            modelError = if (dartDetector == null) "ONNX model not found" else null,
            isDetectionEnabled = dartDetector != null,
        ),
    )
    val state: StateFlow<VisionDebugState> = _state.asStateFlow()

    private var isProcessing = false

    fun onFrameAvailable(imageData: ByteArray, width: Int, height: Int) {
        val detector = dartDetector ?: return
        if (!_state.value.isDetectionEnabled || isProcessing) return

        isProcessing = true
        if (_state.value.detectionStatus !is DetectionStatus.Success) {
            _state.update { it.copy(detectionStatus = DetectionStatus.Detecting) }
        }

        screenModelScope.launch {
            try {
                val result = detector.detect(imageData, width, height)
                _state.update { it.copy(detectionStatus = DetectionStatus.Success(result)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(detectionStatus = DetectionStatus.Error(e.message ?: "Detection failed"))
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun toggleDetection() {
        if (dartDetector == null) return
        _state.update {
            val newEnabled = !it.isDetectionEnabled
            it.copy(
                isDetectionEnabled = newEnabled,
                detectionStatus = if (!newEnabled) DetectionStatus.Idle else it.detectionStatus,
            )
        }
    }

    fun toggleBoardOverlay() {
        _state.update { it.copy(showBoardOverlay = !it.showBoardOverlay) }
    }

    fun toggleRingGuides() {
        _state.update { it.copy(showRingGuides = !it.showRingGuides) }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _state.update { it.copy(cameraPermissionGranted = granted) }
    }
}
