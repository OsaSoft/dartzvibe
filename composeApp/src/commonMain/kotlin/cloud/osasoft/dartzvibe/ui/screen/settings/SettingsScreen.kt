package cloud.osasoft.dartzvibe.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalAppSettingsRepository
import cloud.osasoft.dartzvibe.config.BuildInfo
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepository

class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val appSettingsRepository = LocalAppSettingsRepository.current
        val screenModel = rememberSettingsScreenModel(appSettingsRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        SettingsScreenContent(
            state = state,
            onBackClick = { navigator.pop() },
            onThemeModeChange = screenModel::setThemeMode,
            onKeepScreenOnChange = screenModel::setKeepScreenOn,
            onShowCheckoutHintsChange = screenModel::setShowCheckoutHints,
            onUseRadialKeypadChange = screenModel::setUseRadialKeypad,
            onShowMultiplierButtonsChange = screenModel::setShowMultiplierButtons,
        )
    }
}

@Composable
fun rememberSettingsScreenModel(
    appSettingsRepository: AppSettingsRepository,
): SettingsScreenModel = remember { SettingsScreenModel(appSettingsRepository) }

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    state: SettingsState,
    onBackClick: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onShowCheckoutHintsChange: (Boolean) -> Unit,
    onUseRadialKeypadChange: (Boolean) -> Unit,
    onShowMultiplierButtonsChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SettingSectionHeader(title = "Appearance")

            ThemeModeSettingItem(
                selectedMode = state.settings.themeMode,
                onThemeModeChange = onThemeModeChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingSectionHeader(title = "Game")

            SwitchSettingItem(
                title = "Keep Screen On",
                description = "Prevent screen from sleeping during games",
                checked = state.settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSettingItem(
                title = "Show Checkout Hints",
                description = "Display possible checkouts during game",
                checked = state.settings.showCheckoutHints,
                onCheckedChange = onShowCheckoutHintsChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSettingItem(
                title = "Radial Dartboard Keypad",
                description = "Use a dartboard-style input instead of grid",
                checked = state.settings.useRadialKeypad,
                onCheckedChange = onUseRadialKeypadChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSettingItem(
                title = "Show Multiplier Buttons",
                description = "Show S/D/T buttons (off = use swipe gestures on both keypads)",
                checked = state.settings.showMultiplierButtons,
                onCheckedChange = onShowMultiplierButtonsChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingSectionHeader(title = "About")

            Text(
                text = "DartzVibe v${BuildInfo.VERSION_NAME}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            Text(
                text = "Source Code on GitHub",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { uriHandler.openUri("https://github.com/OsaSoft/dartzvibe") }
                    .padding(vertical = 8.dp),
            )

            Text(
                text = "Licensed under EUPL 1.2",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SettingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSettingItem(
    selectedMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                ) {
                    Text(
                        text = when (mode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.SYSTEM -> "System"
                        },
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
