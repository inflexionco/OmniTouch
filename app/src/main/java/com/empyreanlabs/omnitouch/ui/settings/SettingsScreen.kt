package com.empyreanlabs.omnitouch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.MenuLayoutType
import com.empyreanlabs.omnitouch.ui.MainViewModel
import kotlinx.coroutines.launch

/**
 * Comprehensive settings screen for Omni Touch customization.
 * Organized into sections:
 * - Button Appearance (size, opacity, color)
 * - Button Behavior (edge snapping, move-aside, tap actions)
 * - Menu Configuration (layout type, grid size, actions)
 * - App Settings (start on boot, haptic feedback)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Button settings
    val buttonSize by viewModel.buttonSize.collectAsStateWithLifecycle()
    val buttonOpacity by viewModel.buttonOpacity.collectAsStateWithLifecycle()

    // Menu settings
    val menuLayoutType by viewModel.menuLayoutType.collectAsStateWithLifecycle()
    val menuGridSize by viewModel.menuGridSize.collectAsStateWithLifecycle()

    // App settings
    val startOnBoot by viewModel.startOnBoot.collectAsStateWithLifecycle()
    val hapticFeedback by viewModel.hapticFeedback.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Button Appearance Section
            SettingsSectionCard(title = "Button Appearance") {
                // Button Size
                SettingsSliderItem(
                    label = "Button Size",
                    value = buttonSize,
                    valueRange = 40f..80f,
                    valueLabel = "${buttonSize.toInt()}dp",
                    onValueChange = { scope.launch { viewModel.updateButtonSize(it) } }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Button Opacity
                SettingsSliderItem(
                    label = "Button Opacity",
                    value = buttonOpacity,
                    valueRange = 0.3f..1.0f,
                    valueLabel = "${(buttonOpacity * 100).toInt()}%",
                    onValueChange = { scope.launch { viewModel.updateButtonOpacity(it) } }
                )
            }

            // Button Behavior Section
            SettingsSectionCard(title = "Button Behavior") {
                Text(
                    text = "Edge snapping and move-aside are always enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu Configuration Section
            SettingsSectionCard(title = "Menu Configuration") {
                // Menu Layout Type
                SettingsDropdownItem(
                    label = "Menu Layout",
                    value = menuLayoutType.displayName,
                    options = MenuLayoutType.entries.map { it.displayName },
                    onValueChange = { displayName ->
                        val layoutType = MenuLayoutType.entries.find { it.displayName == displayName }
                        if (layoutType != null) {
                            scope.launch { viewModel.updateMenuLayoutType(layoutType) }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Grid Size (only for Grid layout)
                if (menuLayoutType == MenuLayoutType.GRID) {
                    SettingsSliderItem(
                        label = "Grid Columns",
                        value = menuGridSize.toFloat(),
                        valueRange = 2f..3f,
                        valueLabel = "$menuGridSize columns",
                        onValueChange = { scope.launch { viewModel.updateMenuGridSize(it.toInt()) } },
                        steps = 1
                    )
                }
            }

            // App Settings Section
            SettingsSectionCard(title = "App Settings") {
                // Start on Boot
                SettingsSwitchItem(
                    label = "Start on Boot",
                    description = "Automatically start service when device boots",
                    checked = startOnBoot,
                    onCheckedChange = { scope.launch { viewModel.updateStartOnBoot(it) } }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Haptic Feedback
                SettingsSwitchItem(
                    label = "Haptic Feedback",
                    description = "Vibrate when actions are executed",
                    checked = hapticFeedback,
                    onCheckedChange = { scope.launch { viewModel.updateHapticFeedback(it) } }
                )
            }
        }
    }
}

/**
 * Card wrapper for settings section
 */
@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

/**
 * Slider setting item with label and value display
 */
@Composable
fun SettingsSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * Switch setting item with label and description
 */
@Composable
fun SettingsSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Dropdown setting item with label
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownItem(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}