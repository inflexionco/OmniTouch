package com.empyreanlabs.omnitouch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empyreanlabs.omnitouch.model.MenuLayoutType
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val buttonSize      by viewModel.buttonSize.collectAsStateWithLifecycle()
    val buttonOpacity   by viewModel.buttonOpacity.collectAsStateWithLifecycle()
    val menuLayoutType  by viewModel.menuLayoutType.collectAsStateWithLifecycle()
    val menuGridSize    by viewModel.menuGridSize.collectAsStateWithLifecycle()
    val menuActions     by viewModel.menuActions.collectAsStateWithLifecycle()
    val startOnBoot     by viewModel.startOnBoot.collectAsStateWithLifecycle()
    val hapticFeedback  by viewModel.hapticFeedback.collectAsStateWithLifecycle()

    var showMenuActionEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Omni Touch",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp).size(22.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Large title header (matches screen 3 design)
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Omni Touch Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize your floating assistant experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Button Appearance ───────────────────────────────────────────
            SettingsSectionCard(
                title = "Button Appearance",
                icon = Icons.Default.Palette,
                iconBackground = Color(0xFFEDE7F6)
            ) {
                SettingsSliderItem(
                    label = "Button Size",
                    value = buttonSize,
                    valueRange = 40f..80f,
                    valueLabel = "${buttonSize.toInt()}dp",
                    minLabel = "SMALL",
                    maxLabel = "LARGE",
                    onValueChange = { scope.launch { viewModel.updateButtonSize(it) } }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsSliderItem(
                    label = "Idle Opacity",
                    value = buttonOpacity,
                    valueRange = 0.3f..1.0f,
                    valueLabel = "${(buttonOpacity * 100).toInt()}%",
                    minLabel = "TRANSPARENT",
                    maxLabel = "OPAQUE",
                    onValueChange = { scope.launch { viewModel.updateButtonOpacity(it) } }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsSwitchItem(
                    label = "Use Custom Icon",
                    description = "Replace the default accessibility icon",
                    checked = false,
                    onCheckedChange = { }
                )
            }

            // ── Button Behavior ─────────────────────────────────────────────
            SettingsSectionCard(
                title = "Button Behavior",
                icon = Icons.Default.Swipe,
                iconBackground = Color(0xFFECEFF1),
                iconTint = Color(0xFF607D8B)
            ) {
                SettingsSwitchRow(
                    label = "Auto-Hide on Keyboard",
                    description = "Hide button when typing",
                    checked = false,
                    onCheckedChange = { }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsSwitchRow(
                    label = "Stick to Edges",
                    description = "Always snap to the nearest side",
                    checked = true,
                    onCheckedChange = { }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                var showEdgeSnapInfo by remember { mutableStateOf(false) }
                SettingsDropdownItem(
                    label = "Long Press Action",
                    value = "Toggle Menu Visibility",
                    options = listOf("Toggle Menu Visibility", "Show Quick Actions", "None"),
                    onValueChange = { }
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

            // ── Menu Configuration ───────────────────────────────────────────
            SettingsSectionCard(
                title = "Menu Configuration",
                icon = Icons.Default.GridView,
                iconBackground = Color(0xFF4CAF50),
                iconTint = Color.White
            ) {
                Text(
                    text = "Menu Layout Style",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                // Segmented-style layout selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuLayoutType.entries.forEach { type ->
                        val selected = menuLayoutType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    scope.launch { viewModel.updateMenuLayoutType(type) }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (type == MenuLayoutType.GRID)
                                        Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = type.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (menuLayoutType == MenuLayoutType.GRID) {
                    Spacer(Modifier.height(12.dp))
                    SettingsSliderItem(
                        label = "Grid Columns",
                        value = menuGridSize.toFloat(),
                        valueRange = 2f..4f,
                        valueLabel = "${menuGridSize} Columns",
                        onValueChange = { scope.launch { viewModel.updateMenuGridSize(it.toInt()) } },
                        steps = 2
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Dashed "Rearrange Shortcuts" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { showMenuActionEditor = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Rearrange Shortcuts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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

            // ── App Settings ────────────────────────────────────────────────
            SettingsSectionCard(
                title = "App Settings",
                icon = Icons.Default.Tune,
                iconBackground = Color(0xFFE3F2FD)
            ) {
                // Each row is its own surfaceVariant tile (matches screenshot)
                AppSettingsTile {
                    SettingsIconRow(
                        icon = Icons.Default.Notifications,
                        label = "Push Notifications",
                        trailing = {
                            Switch(
                                checked = startOnBoot,
                                onCheckedChange = { scope.launch { viewModel.updateStartOnBoot(it) } }
                            )
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                AppSettingsTile {
                    SettingsIconRow(
                        icon = Icons.Default.Language,
                        label = "Language",
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "English",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                AppSettingsTile {
                    SettingsIconRow(
                        icon = Icons.Default.RestartAlt,
                        label = "Reset All Settings",
                        labelColor = MaterialTheme.colorScheme.error,
                        iconTint = MaterialTheme.colorScheme.error,
                        trailing = { }
                    )
                }
            }

            // ── Developer Mode dark gradient card ────────────────────────────
            DeveloperModeCard()

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Section Card ─────────────────────────────────────────────────────────────

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBackground, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ─── App Settings individual tile ────────────────────────────────────────────

@Composable
private fun AppSettingsTile(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            content()
        }
    }
}

// ─── Developer Mode dark gradient card ───────────────────────────────────────

@Composable
private fun DeveloperModeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A1035), Color(0xFF2D1B69), Color(0xFF1A237E))
                )
            )
            .padding(24.dp)
    ) {
        // Decorative blurred circle accent
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 16.dp)
                .background(Color(0xFF7C4DFF).copy(alpha = 0.35f), RoundedCornerShape(50.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF7C4DFF), RoundedCornerShape(50.dp))
                )
                Text(
                    text = "DEVELOPER MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                        1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                )
            }
            Text(
                text = "Explore Advanced\nConfigurations",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Slider Item with min/max labels ─────────────────────────────────────────

@Composable
fun SettingsSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    minLabel: String? = null,
    maxLabel: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
        if (minLabel != null && maxLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = minLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = maxLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Switch row ───────────────────────────────────────────────────────────────

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
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) = SettingsSwitchItem(label, description, checked, onCheckedChange)

// ─── Icon-leading row (App Settings style) ────────────────────────────────────

@Composable
private fun SettingsIconRow(
    icon: ImageVector,
    label: String,
    labelColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (iconTint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (labelColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else labelColor,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

// ─── Dropdown ─────────────────────────────────────────────────────────────────

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
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onValueChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

// ─── Navigation Item ─────────────────────────────────────────────────────────

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
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
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

// ─── Menu Action Editor Dialog ────────────────────────────────────────────────

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
            TextButton(onClick = { onSave(selectedActions) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                            Icon(imageVector = action.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(text = action.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                            selectedActions = selectedActions.toMutableList().apply { removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                OutlinedButton(onClick = { showActionPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Action")
                }
                if (showActionPicker) {
                    ActionPickerDialog(
                        availableActions = OmniTouchAction.getAllPredefinedActions()
                            .filter { it.id != "no_action" && it.id != "show_menu" },
                        onActionSelected = { action ->
                            if (!selectedActions.any { it.id == action.id }) {
                                selectedActions = selectedActions.toMutableList().apply { add(action) }
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

// ─── Action Picker Dialog ─────────────────────────────────────────────────────

@Composable
fun ActionPickerDialog(
    availableActions: List<OmniTouchAction>,
    onActionSelected: (OmniTouchAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Choose Action") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                        Icon(imageVector = action.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        Column {
                            Text(text = action.displayName, style = MaterialTheme.typography.bodyMedium)
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