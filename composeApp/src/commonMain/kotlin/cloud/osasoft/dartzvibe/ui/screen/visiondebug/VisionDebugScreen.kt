package cloud.osasoft.dartzvibe.ui.screen.visiondebug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.domain.detection.DetectedDart
import cloud.osasoft.dartzvibe.domain.detection.DetectionStatus
import cloud.osasoft.dartzvibe.ui.screen.visiondebug.components.DetectionOverlay

class VisionDebugScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberVisionDebugScreenModel()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        VisionDebugScreenContent(
            state = state,
            onBackClick = { navigator.pop() },
            onFrameAvailable = screenModel::onFrameAvailable,
            onToggleDetection = screenModel::toggleDetection,
            onToggleBoardOverlay = screenModel::toggleBoardOverlay,
            onToggleRingGuides = screenModel::toggleRingGuides,
        )
    }
}

@Composable
expect fun rememberVisionDebugScreenModel(): VisionDebugScreenModel

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionDebugScreenContent(
    state: VisionDebugState,
    onBackClick: () -> Unit,
    onFrameAvailable: (ByteArray, Int, Int) -> Unit,
    onToggleDetection: () -> Unit,
    onToggleBoardOverlay: () -> Unit,
    onToggleRingGuides: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vision Debug") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Camera preview with detection overlay (top ~60%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                CameraPreviewContent(
                    onFrameAvailable = onFrameAvailable,
                    modifier = Modifier.fillMaxSize(),
                )

                val detectionResult = (state.detectionStatus as? DetectionStatus.Success)?.result
                DetectionOverlay(
                    result = detectionResult,
                    showBoardOverlay = state.showBoardOverlay,
                    showRingGuides = state.showRingGuides,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Controls and results (bottom ~40%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(16.dp),
            ) {
                // Toggle controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.isDetectionEnabled,
                        onClick = onToggleDetection,
                        label = { Text("Detection") },
                    )
                    FilterChip(
                        selected = state.showBoardOverlay,
                        onClick = onToggleBoardOverlay,
                        label = { Text("Board") },
                    )
                    FilterChip(
                        selected = state.showRingGuides,
                        onClick = onToggleRingGuides,
                        label = { Text("Rings") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detection info
                DetectionInfoPanel(state.detectionStatus)
            }

            // Attribution / model error indicator
            if (state.modelError != null) {
                Text(
                    text = state.modelError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "Vision model based on dart-sense by Ben Willshaw (CC BY-NC 4.0)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/bnww/dart-sense") }
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
expect fun CameraPreviewContent(
    onFrameAvailable: (ByteArray, Int, Int) -> Unit,
    modifier: Modifier,
)

@Suppress("ktlint:standard:function-naming")
@Composable
private fun DetectionInfoPanel(status: DetectionStatus) {
    Column {
        Text(
            text = "Detection Status",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (status) {
            is DetectionStatus.Idle -> {
                InfoRow("Status", "Idle")
            }

            is DetectionStatus.Detecting -> {
                InfoRow("Status", "Processing...")
            }

            is DetectionStatus.Success -> {
                val result = status.result
                InfoRow("Status", "Active")
                InfoRow("Board Detected", if (result.boardDetected) "Yes" else "No")
                InfoRow("Darts Found", "${result.darts.size}")
                InfoRow("Processing Time", "${result.processingTimeMs}ms")

                if (result.darts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detected Darts",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    result.darts.forEach { dart ->
                        val label = formatDartInfo(dart)
                        InfoRow("Dart", "$label (${(dart.confidence * 100).toInt()}%)")
                    }
                }
            }

            is DetectionStatus.Error -> {
                InfoRow("Status", "Error")
                InfoRow("Message", status.message)
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatDartInfo(dart: DetectedDart): String {
    if (dart.segment == null || dart.multiplier == null) return "MISS"

    return when {
        dart.segment == 25 && dart.multiplier == Multiplier.DOUBLE -> "DB (50)"

        dart.segment == 25 -> "SB (25)"

        else -> {
            val prefix = when (dart.multiplier) {
                Multiplier.SINGLE -> "S"
                Multiplier.DOUBLE -> "D"
                Multiplier.TRIPLE -> "T"
            }
            val score = dart.segment * dart.multiplier.value
            "$prefix${dart.segment} ($score)"
        }
    }
}
