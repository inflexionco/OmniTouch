package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * - No background dim — purely floating items
 * - Items are clamped so they never go off-screen vertically
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

    val wheelRadius = 120.dp
    val isFullCircle = false
    val itemSize = 56.dp

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

    // Load settings — trigger the spring animation only after the first menuActions
    // emission so items already exist when the radius animates from 0 → wheelRadius.
    LaunchedEffect(Unit) {
        launch {
            settingsRepository.hapticFeedback.collect { hapticFeedback = it }
        }
        launch {
            var firstEmission = true
            settingsRepository.menuActions.collect { actionIds ->
                menuActions = actionIds.mapNotNull { OmniTouchAction.fromId(it) }
                if (firstEmission) {
                    firstEmission = false
                    isVisible = true
                }
            }
        }
    }

    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()
    val screenWidthPx = with(density) { screenWidth.dp.toPx() }.toInt()
    val screenHeightPx = with(density) { screenHeight.dp.toPx() }.toInt()
    val wheelRadiusPx = with(density) { wheelRadius.toPx() }
    val itemSizePx = with(density) { itemSize.toPx() }.toInt()
    // Item bounding box includes icon (itemSize) + label (~20dp)
    val itemTotalHeightPx = with(density) { (itemSize + 24.dp).toPx() }.toInt()

    // Button center in full-screen composable coordinates
    val centerX = buttonX + buttonSizePx / 2
    val centerY = buttonY + buttonSizePx / 2

    // Orientation: button on LEFT edge → open RIGHT (orientation = 180°),
    // button on RIGHT edge → open LEFT (orientation = 0°).
    // In calculateItemAngle, startAngle = orientation - 90, so:
    //   orientation=180 → startAngle=90..270 → items fan to the right ✓
    //   orientation=0   → startAngle=-90..90 → items fan to the left  ✓
    val orientation: Double = if (buttonX < screenWidthPx / 2) 180.0 else 0.0

    // Vertical margin so items don't clip the status bar or nav bar
    val verticalMarginPx = with(density) { 16.dp.toPx() }.toInt()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent touch-catcher to dismiss on taps outside the menu items.
        // No background colour — zero dim.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        )

        // Radial menu items positioned in a circle/semicircle
        menuActions.forEachIndexed { index, action ->
            val angle = calculateItemAngle(
                index = index,
                totalItems = menuActions.size,
                isFullCircle = isFullCircle,
                orientation = orientation
            )

            // Animated radius - spring out from center
            val animatedRadius by animateFloatAsState(
                targetValue = if (isVisible) wheelRadiusPx else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = 0.1f
                ),
                label = "radius_$index"
            )

            // Raw item center position
            val rawItemX = centerX + (animatedRadius * cos(angle)).toInt()
            val rawItemY = centerY + (animatedRadius * sin(angle)).toInt()

            // Clamp so the full item (icon + label) stays within screen bounds
            val clampedItemY = rawItemY.coerceIn(
                verticalMarginPx + itemTotalHeightPx / 2,
                screenHeightPx - verticalMarginPx - itemTotalHeightPx / 2
            )

            RadialMenuItem(
                action = action,
                x = rawItemX,
                y = clampedItemY,
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
 *
 * orientation=180 → startAngle=90°..270° → semicircle opens to the RIGHT
 * orientation=0   → startAngle=-90°..90° → semicircle opens to the LEFT
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
    val itemBackgroundColor = if (canExecute)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val itemIconColor = if (canExecute)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val itemLabelColor = MaterialTheme.colorScheme.onSurface

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
                    tint = itemIconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = action.displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = itemLabelColor.copy(alpha = if (canExecute) 1f else 0.5f)
            )
        }
    }
}
