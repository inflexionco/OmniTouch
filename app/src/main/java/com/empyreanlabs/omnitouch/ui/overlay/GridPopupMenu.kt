package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.launch

/**
 * Compact grid popup menu that appears near the floating button.
 * Features:
 * - Position-aware (appears near button, not full-screen)
 * - Maximum 3x3 grid size
 * - Customizable appearance (background, tile colors, labels)
 * - No empty slots (only shows actual actions)
 * - Proper background dimming
 * - Smooth fade-in animation
 */
@Composable
fun GridPopupMenu(
    settingsRepository: SettingsRepository,
    actionExecutor: ActionExecutor,
    buttonX: Int,
    buttonY: Int,
    buttonSize: Float,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Screen dimensions
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Menu settings
    var menuActions by remember { mutableStateOf<List<OmniTouchAction>>(emptyList()) }
    var gridSize by remember { mutableIntStateOf(SettingsRepository.DEFAULT_MENU_GRID_SIZE) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Appearance: use MaterialTheme surface tokens for adaptive light/dark support
    val menuBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tileColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tileIconColor = MaterialTheme.colorScheme.onSurface
    val tileLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dimLevel = 0.15f

    // Animation state — spring for expressive entry
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "menu_scale"
    )

    // Load settings
    LaunchedEffect(Unit) {
        launch {
            settingsRepository.menuGridSize.collect { gridSize = it }
        }
        launch {
            settingsRepository.hapticFeedback.collect { hapticFeedback = it }
        }
        launch {
            settingsRepository.menuActions.collect { actionIds ->
                menuActions = actionIds.mapNotNull { OmniTouchAction.fromId(it) }
            }
        }
        // Trigger animation
        isVisible = true
    }

    // Calculate menu position based on button location
    val menuPosition = calculateMenuPosition(
        buttonX = buttonX,
        buttonY = buttonY,
        buttonSize = buttonSize,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        menuActions = menuActions,
        gridSize = gridSize,
        density = density
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Dimmed background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimLevel * animatedAlpha))
                .clickable(onClick = onDismiss)
        )

        // Menu grid positioned near button — scale+fade spring entry
        Box(
            modifier = Modifier
                .offset { IntOffset(menuPosition.first, menuPosition.second) }
                .scale(animatedScale)
                .alpha(animatedAlpha)
        ) {
            Column(
                modifier = Modifier
                    .shadow(12.dp, MaterialTheme.shapes.large)
                    .background(
                        color = menuBackgroundColor,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(12.dp)
                    .clickable(enabled = false) { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rows = (menuActions.size + gridSize - 1) / gridSize

                for (row in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until gridSize) {
                            val index = row * gridSize + col
                            if (index < menuActions.size) {
                                val action = menuActions[index]
                                GridMenuItem(
                                    action = action,
                                    index = index,
                                    backgroundColor = tileColor,
                                    iconColor = tileIconColor,
                                    labelColor = tileLabelColor,
                                    actionExecutor = actionExecutor,
                                    onClick = {
                                        scope.launch {
                                            actionExecutor.executeAction(action, hapticFeedback)
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate grid menu position based on button location.
 * Simple rule: Button on left → menu on right, Button on right → menu on left
 * Similar to iOS AssistiveTouch behavior
 */
private fun calculateMenuPosition(
    buttonX: Int,
    buttonY: Int,
    buttonSize: Float,
    screenWidth: Int,
    screenHeight: Int,
    menuActions: List<OmniTouchAction>,
    gridSize: Int,
    density: androidx.compose.ui.unit.Density
): Pair<Int, Int> {
    // Menu dimensions (approximate)
    val itemSize = with(density) { 60.dp.toPx() }.toInt() // 60dp per item
    val spacing = with(density) { 8.dp.toPx() }.toInt()
    val padding = with(density) { 12.dp.toPx() }.toInt()

    val cols = minOf(menuActions.size, gridSize)
    val rows = (menuActions.size + gridSize - 1) / gridSize

    val menuWidth = cols * itemSize + (cols - 1) * spacing + 2 * padding
    val menuHeight = rows * itemSize + (rows - 1) * spacing + 2 * padding

    val screenWidthPx = with(density) { screenWidth.dp.toPx() }.toInt()
    val screenHeightPx = with(density) { screenHeight.dp.toPx() }.toInt()
    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()

    // Determine which edge the button is closest to (left or right)
    val distToLeft = buttonX
    val distToRight = screenWidthPx - buttonX - buttonSizePx

    // Simple horizontal positioning: Button on left → menu on right, and vice versa
    val menuX: Int = if (distToLeft < distToRight) {
        // Button on left edge - place menu to the right
        buttonX + buttonSizePx + spacing
    } else {
        // Button on right edge - place menu to the left
        buttonX - menuWidth - spacing
    }

    // Center menu vertically relative to button
    var menuY: Int = buttonY + (buttonSizePx / 2) - (menuHeight / 2)

    // Ensure menu stays fully on-screen (vertical bounds only)
    val margin = with(density) { 16.dp.toPx() }.toInt()
    menuY = menuY.coerceIn(margin, screenHeightPx - menuHeight - margin)

    return Pair(menuX, menuY)
}

/**
 * Individual menu item in the grid.
 * Staggered spring scale-in per index gives an expressive cascade effect.
 */
@Composable
private fun GridMenuItem(
    action: OmniTouchAction,
    index: Int,
    backgroundColor: Color,
    iconColor: Color,
    labelColor: Color,
    actionExecutor: ActionExecutor,
    onClick: () -> Unit
) {
    val canExecute = remember(action) { actionExecutor.canExecuteAction(action) }
    val itemTileColor = if (canExecute) backgroundColor
                        else backgroundColor.copy(alpha = 0.4f)
    val itemIconColor = if (canExecute) iconColor else iconColor.copy(alpha = 0.4f)

    // Staggered spring: later items have slightly lower stiffness → cascade feel
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val itemScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = (Spring.StiffnessMedium - index * 30f).coerceAtLeast(Spring.StiffnessLow)
        ),
        label = "item_scale_$index"
    )

    Column(
        modifier = Modifier
            .size(60.dp)
            .scale(itemScale)
            .clip(MaterialTheme.shapes.medium)
            .background(itemTileColor)
            .clickable(enabled = canExecute, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.displayName,
            tint = itemIconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (canExecute) labelColor else labelColor.copy(alpha = 0.4f)
        )
    }
}