package com.empyreanlabs.omnitouch.ui.overlay

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Edge-snapping floating button with move-aside functionality.
 * Features:
 * - Snaps to nearest edge (all 4 sides) with 50dp threshold
 * - Moves aside (semi-transparent + partial off-screen) after 5s inactivity
 * - Smooth animations for snapping and move-aside
 * - Position persistence
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun EdgeSnappingFloatingButton(
    settingsRepository: SettingsRepository,
    actionExecutor: ActionExecutor,
    windowManager: WindowManager,
    layoutParams: WindowManager.LayoutParams,
    view: android.view.View,
    onMenuVisibilityChange: (Boolean) -> Unit,
    isMenuOpen: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Screen dimensions
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Button settings
    var buttonSize by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_SIZE) }
    var buttonOpacity by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_OPACITY) }
    var singleTapActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_SINGLE_TAP_ACTION) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Move-aside settings (hardcoded for now, will be configurable later)
    val moveAsideEnabled = true
    val moveAsideDelay = 5000L // 5 seconds
    val moveAsideOpacity = 0.4f

    // Drag state
    var initialX by remember { mutableIntStateOf(0) }
    var initialY by remember { mutableIntStateOf(0) }
    var initialTouchX by remember { mutableFloatStateOf(0f) }
    var initialTouchY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Move-aside state
    var isMovedAside by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Animation state for opacity
    val targetOpacity = if (isMovedAside) moveAsideOpacity else buttonOpacity
    val animatedOpacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = tween(durationMillis = 300),
        label = "opacity"
    )

    // Load settings
    LaunchedEffect(Unit) {
        launch { settingsRepository.buttonSize.collect { buttonSize = it } }
        launch { settingsRepository.buttonOpacity.collect { buttonOpacity = it } }
        launch { settingsRepository.singleTapAction.collect { singleTapActionId = it } }
        launch { settingsRepository.hapticFeedback.collect { hapticFeedback = it } }
    }

    // Move-aside timer
    LaunchedEffect(lastInteractionTime, moveAsideEnabled) {
        if (!moveAsideEnabled) return@LaunchedEffect

        while (true) {
            delay(1000) // Check every second
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime
            if (timeSinceInteraction >= moveAsideDelay && !isMovedAside && !isDragging) {
                isMovedAside = true
                // Move button partially off-screen based on which edge it's at
                moveButtonAside(layoutParams, windowManager, view, screenWidth, screenHeight, buttonSize, density)
            }
        }
    }

    /**
     * Snap button to nearest edge
     */
    fun snapToEdge(currentX: Int, currentY: Int) {
        val snapThreshold = with(density) { 50.dp.toPx() }.toInt()

        // Calculate distances to each edge
        val distToLeft = currentX
        val distToRight = with(density) { screenWidth.dp.toPx() }.toInt() - currentX
        val distToTop = currentY
        val distToBottom = with(density) { screenHeight.dp.toPx() }.toInt() - currentY

        // Find minimum distance
        val minDist = minOf(distToLeft, distToRight, distToTop, distToBottom)

        // Snap to nearest edge if within threshold
        if (minDist <= snapThreshold) {
            when (minDist) {
                distToLeft -> layoutParams.x = 0
                distToRight -> layoutParams.x = with(density) { screenWidth.dp.toPx() }.toInt()
                distToTop -> layoutParams.y = 0
                distToBottom -> layoutParams.y = with(density) { screenHeight.dp.toPx() }.toInt()
            }
            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .alpha(animatedOpacity)
            .clip(CircleShape)
            .background(Color(0xFF2196F3))
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        lastInteractionTime = System.currentTimeMillis()

                        // If moved aside, bring back to full visibility immediately
                        if (isMovedAside) {
                            isMovedAside = false
                            // Restore position (move back on-screen)
                            restoreButtonPosition(layoutParams, windowManager, view, buttonSize, density)
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        // If moved more than threshold, it's a drag
                        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                            // Dismiss the menu before dragging so the window collapses
                            // back to WRAP_CONTENT; otherwise layoutParams.x/y are both 0
                            // (full-screen window origin) and position will be wrong.
                            if (!isDragging && isMenuOpen) {
                                onMenuVisibilityChange(false)
                            }
                            isDragging = true
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        lastInteractionTime = System.currentTimeMillis()

                        if (!isDragging) {
                            // It's a tap
                            val currentTime = System.currentTimeMillis()
                            val tapGap = currentTime - lastTapTime

                            // Check for double tap
                            if (tapGap < 300) {
                                // Double tap - ignore for now
                                lastTapTime = 0
                            } else {
                                // Single tap
                                lastTapTime = currentTime
                                scope.launch {
                                    delay(300) // Wait to see if double tap
                                    if (lastTapTime == currentTime) {
                                        val action = OmniTouchAction.fromId(singleTapActionId)
                                        if (action != null) {
                                            if (action == OmniTouchAction.ShowMenu) {
                                                onMenuVisibilityChange(true)
                                            } else {
                                                actionExecutor.executeAction(action, hapticFeedback)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // After drag, snap to nearest edge
                            snapToEdge(layoutParams.x, layoutParams.y)

                            // Save position
                            scope.launch {
                                settingsRepository.updateButtonPosition(layoutParams.x, layoutParams.y)
                            }
                        }

                        isDragging = false
                        true
                    }

                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = "Omni Touch Button",
            tint = Color.White,
            modifier = Modifier.size((buttonSize * 0.6f).dp)
        )
    }
}

/**
 * Move button aside (partially off-screen) based on its current position
 */
private fun moveButtonAside(
    layoutParams: WindowManager.LayoutParams,
    windowManager: WindowManager,
    view: android.view.View,
    screenWidth: Int,
    screenHeight: Int,
    buttonSize: Float,
    density: androidx.compose.ui.unit.Density
) {
    val currentX = layoutParams.x
    val currentY = layoutParams.y

    val screenWidthPx = with(density) { screenWidth.dp.toPx() }.toInt()
    val screenHeightPx = with(density) { screenHeight.dp.toPx() }.toInt()
    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()

    // Determine which edge the button is closest to
    val distToLeft = currentX
    val distToRight = screenWidthPx - currentX
    val distToTop = currentY
    val distToBottom = screenHeightPx - currentY

    val minDist = minOf(distToLeft, distToRight, distToTop, distToBottom)

    // Move button partially off-screen (50% visible)
    val offsetAmount = buttonSizePx / 2

    when (minDist) {
        distToLeft -> layoutParams.x = -offsetAmount
        distToRight -> layoutParams.x = screenWidthPx - buttonSizePx + offsetAmount
        distToTop -> layoutParams.y = -offsetAmount
        distToBottom -> layoutParams.y = screenHeightPx - buttonSizePx + offsetAmount
    }

    windowManager.updateViewLayout(view, layoutParams)
}

/**
 * Restore button to full on-screen position
 */
private fun restoreButtonPosition(
    layoutParams: WindowManager.LayoutParams,
    windowManager: WindowManager,
    view: android.view.View,
    buttonSize: Float,
    density: androidx.compose.ui.unit.Density
) {
    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()
    val offsetAmount = buttonSizePx / 2

    // If button is partially off-screen, move it back
    if (layoutParams.x < 0) {
        layoutParams.x = 0
    }
    if (layoutParams.y < 0) {
        layoutParams.y = 0
    }

    windowManager.updateViewLayout(view, layoutParams)
}