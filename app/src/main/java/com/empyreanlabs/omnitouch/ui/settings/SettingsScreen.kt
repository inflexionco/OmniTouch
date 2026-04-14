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
import com.empyreanlabs.omnitouch.model.OmniTouchAction
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
    val menuActions by viewModel.menuActions.collectAsStateWithLifecycle()

    // App settings
    val startOnBoot by viewModel.startOnBoot.collectAsStateWithLifecycle()
    val hapticFeedback by viewModel.hapticFeedback.collectAsStateWithLifecycle()

    // Menu action editor dialog state
    var showMenuActionEditor by remember { mutableStateOf(false) }

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
                colors = TopAppBarDefaults.topAppBarColors()
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
                var showEdgeSnapInfo by remember { mutableStateOf(false) }
                AssistChip(
                    onClick = { showEdgeSnapInfo = true },
                    label = { Text("Edge snapping: Always on") },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                if (showEdgeSnapInfo) {
                    AlertDialog(
                        onDismissRequest = { showEdgeSnapInfo = false },
                        confirmButton = {
                            TextButton(onClick = { showEdgeSnapInfo = false }) { Text("Got it") }
                        },
                        title = { Text("Edge Snapping") },
                        text = { Text("The floating button always snaps to the nearest screen edge and moves aside automatically when it overlaps content. This cannot be disabled.") }
                    )
                }
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Menu Actions Editor
                SettingsNavigationItem(
                    label = "Menu Actions",
                    description = "${menuActions.size} actions configured",
                    onClick = { showMenuActionEditor = true }
                )
            }

            // Menu Action Editor Dialog
            if (showMenuActionEditor) {
                MenuActionEditorDialog(
                    currentActions = menuActions.mapNotNull { OmniTouchAction.fromId(it) },
                    onDismiss = { showMenuActionEditor = false },
                    onSave = { actions ->
                        scope.launch {
                            viewModel.updateMenuActions(actions.map { it.id })
                            showMenuActionEditor = false
                        }
                    }
                )
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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

/**
 * Navigation setting item (clickable item that opens another screen/dialog)
 */
@Composable
fun SettingsNavigationItem(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dialog for editing menu actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuActionEditorDialog(
    currentActions: List<OmniTouchAction>,
    onDismiss: () -> Unit,
    onSave: (List<OmniTouchAction>) -> Unit
) {
    var selectedActions by remember { mutableStateOf(currentActions.toMutableList()) }
    var showActionPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(selectedActions) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Edit Menu Actions") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selected actions (${selectedActions.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // List of current actions with delete buttons
                selectedActions.forEachIndexed { index, action ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedActions = selectedActions.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Add action button
                OutlinedButton(
                    onClick = { showActionPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Action")
                }

                // Action picker dialog
                if (showActionPicker) {
                    ActionPickerDialog(
                        availableActions = OmniTouchAction.getAllPredefinedActions()
                            .filter { it.id != "no_action" && it.id != "show_menu" },
                        onActionSelected = { action ->
                            if (!selectedActions.any { it.id == action.id }) {
                                selectedActions = selectedActions.toMutableList().apply {
                                    add(action)
                                }
                            }
                            showActionPicker = false
                        },
                        onDismiss = { showActionPicker = false }
                    )
                }
            }
        }
    )
}

/**
 * Dialog for picking an action from available actions
 */
@Composable
fun ActionPickerDialog(
    availableActions: List<OmniTouchAction>,
    onActionSelected: (OmniTouchAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Choose Action") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableActions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}