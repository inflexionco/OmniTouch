package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Appearance settings (hardcoded for now, will be configurable later)
    val menuBackgroundColor = Color(0xFF1E1E1E) // Dark background
    val tileColor = Color(0xFF2C2C2C) // Slightly lighter tile background
    val tileIconColor = Color.White
    val tileLabelColor = Color.White.copy(alpha = 0.9f)
    val dimLevel = 0.5f // 50% dim

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menu_alpha"
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

        // Menu grid positioned near button
        Box(
            modifier = Modifier
                .offset { IntOffset(menuPosition.first, menuPosition.second) }
        ) {
            Column(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(
                        color = menuBackgroundColor.copy(alpha = animatedAlpha),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
                    .clickable(enabled = false) { }, // Prevent dismissal when clicking menu
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Calculate rows based on grid size
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
                                    backgroundColor = tileColor,
                                    iconColor = tileIconColor,
                                    labelColor = tileLabelColor,
                                    alpha = animatedAlpha,
                                    onClick = {
                                        scope.launch {
                                            actionExecutor.executeAction(action, hapticFeedback)
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                            // No empty spacer slots - just end the row early
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate optimal menu position based on button location.
 * Menu should appear near the button but stay fully on-screen.
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

    // Determine which edge the button is closest to
    val distToLeft = buttonX
    val distToRight = screenWidthPx - buttonX - buttonSizePx
    val distToTop = buttonY
    val distToBottom = screenHeightPx - buttonY - buttonSizePx

    val minHorizontalDist = minOf(distToLeft, distToRight)
    val minVerticalDist = minOf(distToTop, distToBottom)

    // Default position: try to place menu to the right/left of button
    var menuX: Int
    var menuY: Int

    if (minHorizontalDist < minVerticalDist) {
        // Button is closer to left/right edge - place menu above/below
        if (distToTop < distToBottom) {
            // Button is near top - place menu below
            menuY = buttonY + buttonSizePx + spacing
        } else {
            // Button is near bottom - place menu above
            menuY = buttonY - menuHeight - spacing
        }
        // Center menu horizontally relative to button
        menuX = buttonX + (buttonSizePx / 2) - (menuWidth / 2)
    } else {
        // Button is closer to top/bottom edge - place menu to the side
        if (distToLeft < distToRight) {
            // Button is near left edge - place menu to the right
            menuX = buttonX + buttonSizePx + spacing
        } else {
            // Button is near right edge - place menu to the left
            menuX = buttonX - menuWidth - spacing
        }
        // Center menu vertically relative to button
        menuY = buttonY + (buttonSizePx / 2) - (menuHeight / 2)
    }

    // Ensure menu stays fully on-screen
    val margin = with(density) { 16.dp.toPx() }.toInt()
    menuX = menuX.coerceIn(margin, screenWidthPx - menuWidth - margin)
    menuY = menuY.coerceIn(margin, screenHeightPx - menuHeight - margin)

    return Pair(menuX, menuY)
}

/**
 * Individual menu item in the grid.
 */
@Composable
private fun GridMenuItem(
    action: OmniTouchAction,
    backgroundColor: Color,
    iconColor: Color,
    labelColor: Color,
    alpha: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = alpha))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.displayName,
            tint = iconColor.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.displayName,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = labelColor.copy(alpha = alpha)
        )
    }
}