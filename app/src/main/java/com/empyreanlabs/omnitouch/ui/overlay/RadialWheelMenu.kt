package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.*

/**
 * MI Quick Ball-inspired radial wheel menu.
 * Features:
 * - Semicircle (default) and full circle options
 * - Variable number of actions (3-8)
 * - Configurable radius (120dp default)
 * - Position-aware orientation (opens away from screen edges)
 * - Smooth spring animations - icons spring out from center
 * - Labels appear outside the wheel
 */
@Composable
fun RadialWheelMenu(
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
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Appearance settings (hardcoded for now, will be configurable later)
    val wheelRadius = 120.dp // Configurable radius
    val isFullCircle = false // false = semicircle (default), true = full circle
    val itemSize = 56.dp
    val dimLevel = 0.15f // Reduced from 0.5f to barely visible

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

    // Calculate center position and orientation
    // buttonX and buttonY are screen coordinates in the full-screen window
    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()
    val screenWidthPx = with(density) { screenWidth.dp.toPx() }.toInt()

    // Button center in screen/composable coordinates (window is full-screen so they match)
    val centerX = buttonX + buttonSizePx / 2
    val centerY = buttonY + buttonSizePx / 2

    // Determine orientation: button on left → open right, button on right → open left
    val orientation: Double = if (buttonX < screenWidthPx / 2) 0.0 else 180.0

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Dimmed background — captures all touches outside menu items to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimLevel * animatedAlpha))
                .clickable(onClick = onDismiss)
        )

        // Radial menu items positioned in a circle/semicircle
        menuActions.forEachIndexed { index, action ->
            // Calculate angle for this item
            val angle = calculateItemAngle(
                index = index,
                totalItems = menuActions.size,
                isFullCircle = isFullCircle,
                orientation = orientation
            )

            // Animated radius - spring out from center
            val animatedRadius by animateFloatAsState(
                targetValue = if (isVisible) with(density) { wheelRadius.toPx() } else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = 0.1f
                ),
                label = "radius_$index"
            )

            // Calculate item position
            val itemX = centerX + (animatedRadius * cos(angle)).toInt()
            val itemY = centerY + (animatedRadius * sin(angle)).toInt()

            RadialMenuItem(
                action = action,
                x = itemX,
                y = itemY,
                itemSize = itemSize,
                alpha = animatedAlpha,
                angle = angle,
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

/**
 * Calculate angle for a specific item in the radial menu.
 * Angle is in radians.
 */
private fun calculateItemAngle(
    index: Int,
    totalItems: Int,
    isFullCircle: Boolean,
    orientation: Double
): Float {
    val angleRange = if (isFullCircle) 360.0 else 180.0
    val startAngle = orientation - (angleRange / 2.0)

    // Distribute items evenly across the angle range
    val angleStep = if (totalItems > 1) {
        angleRange / (totalItems - 1)
    } else {
        0.0
    }

    val angleDegrees = startAngle + (index * angleStep)
    return Math.toRadians(angleDegrees).toFloat()
}

/**
 * Individual item in the radial wheel menu.
 */
@Composable
private fun RadialMenuItem(
    action: OmniTouchAction,
    x: Int,
    y: Int,
    itemSize: androidx.compose.ui.unit.Dp,
    alpha: Float,
    angle: Float,
    actionExecutor: ActionExecutor,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val canExecute = remember(action) { actionExecutor.canExecuteAction(action) }
    val itemAlpha = if (canExecute) alpha else alpha * 0.4f
    val itemBackgroundColor = if (canExecute) Color(0xFF2196F3) else Color(0xFF2196F3).copy(alpha = 0.5f)

    // Calculate offset to center the item at (x, y)
    // The Column is 80dp wide, and the icon + label is approximately 80dp tall
    val itemWidth = with(density) { 80.dp.toPx() }.toInt()
    val itemHeight = with(density) { 80.dp.toPx() }.toInt()
    val centeredX = x - itemWidth / 2
    val centeredY = y - itemHeight / 2

    Box(
        modifier = Modifier
            .offset { IntOffset(centeredX, centeredY) }
            .alpha(itemAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(80.dp)
        ) {
            // Icon button
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(itemBackgroundColor)
                    .clickable(enabled = canExecute, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.displayName,
                    tint = Color.White.copy(alpha = if (canExecute) 1f else 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = action.displayName,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = if (canExecute) 1f else 0.6f)
            )
        }
    }
}