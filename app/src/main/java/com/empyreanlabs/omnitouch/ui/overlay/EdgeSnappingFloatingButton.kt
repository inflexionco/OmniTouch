package com.empyreanlabs.omnitouch.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating button that:
 *  - Docks exclusively to the left or right vertical edge (never top/bottom, never centre).
 *  - Drags freely while the finger is down; on release it spring-animates to the nearest edge.
 *  - Active state: button fully on-screen (x=0 or x=screenWidth-buttonSize).
 *  - Inactive state: after 5s of no interaction the button slides 50% off the nearest
 *    horizontal edge (move-aside). A touch anywhere on the visible half restores it.
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
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Screen dimensions in dp (as reported by the configuration)
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Button settings
    var buttonSize by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_SIZE) }
    var buttonOpacity by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_OPACITY) }
    var singleTapActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_SINGLE_TAP_ACTION) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Move-aside constants
    val moveAsideDelay = 5000L
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

    // Animated opacity: dim when moved aside — spring for expressive feel
    val targetOpacity = if (isMovedAside) moveAsideOpacity else buttonOpacity
    val animatedOpacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "opacity"
    )

    // Load settings
    LaunchedEffect(Unit) {
        launch { settingsRepository.buttonSize.collect { buttonSize = it } }
        launch { settingsRepository.buttonOpacity.collect { buttonOpacity = it } }
        launch { settingsRepository.singleTapAction.collect { singleTapActionId = it } }
        launch { settingsRepository.hapticFeedback.collect { hapticFeedback = it } }
    }

    // Move-aside timer — fires when the user hasn't touched the button for moveAsideDelay ms.
    LaunchedEffect(lastInteractionTime) {
        while (true) {
            delay(1000)
            val elapsed = System.currentTimeMillis() - lastInteractionTime
            if (elapsed >= moveAsideDelay && !isMovedAside && !isDragging) {
                isMovedAside = true
                animateToX(
                    from = layoutParams.x,
                    to = moveAsideTargetX(layoutParams.x, density, screenWidthDp, buttonSize),
                    layoutParams = layoutParams,
                    windowManager = windowManager,
                    view = view
                )
            }
        }
    }

    /**
     * Animate layoutParams.x from [from] to [to] using a spring-style ValueAnimator.
     * The button snaps smoothly without any jump.
     */
    fun springSnapToX(from: Int, to: Int) {
        animateToX(from, to, layoutParams, windowManager, view)
    }

    /**
     * On ACTION_UP: snap X to the nearest horizontal edge (left=0, right=screenWidth-buttonSize).
     * Y is unconstrained. Uses a spring animation.
     */
    fun snapToNearestHorizontalEdge() {
        val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }.toInt()
        val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()

        val leftEdge = 0
        val rightEdge = screenWidthPx - buttonSizePx

        val targetX = if (layoutParams.x <= screenWidthPx / 2 - buttonSizePx / 2) leftEdge else rightEdge
        springSnapToX(layoutParams.x, targetX)

        // Clamp Y so the button never goes off top or bottom
        val screenHeightPx = with(density) { screenHeightDp.dp.toPx() }.toInt()
        val clampedY = layoutParams.y.coerceIn(0, screenHeightPx - buttonSizePx)
        if (clampedY != layoutParams.y) {
            layoutParams.y = clampedY
            try { windowManager.updateViewLayout(view, layoutParams) } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .alpha(animatedOpacity)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        lastInteractionTime = System.currentTimeMillis()

                        // Restore from move-aside: animate back to the fully-visible edge position
                        if (isMovedAside) {
                            isMovedAside = false
                            val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }.toInt()
                            val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()
                            // Determine which edge the button is currently on
                            val restoredX = if (layoutParams.x < 0) 0 else screenWidthPx - buttonSizePx
                            springSnapToX(layoutParams.x, restoredX)
                            initialX = restoredX
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                            // Dismiss menu before starting a drag so the window is already
                            // WRAP_CONTENT when we read/write layoutParams.x/y
                            if (!isDragging && isMenuOpen) {
                                onMenuVisibilityChange(false)
                            }
                            isDragging = true
                            // Free drag: follow the finger in both axes
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            try { windowManager.updateViewLayout(view, layoutParams) } catch (_: Exception) {}
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        lastInteractionTime = System.currentTimeMillis()

                        if (!isDragging) {
                            // Tap logic
                            val currentTime = System.currentTimeMillis()
                            val tapGap = currentTime - lastTapTime
                            if (tapGap < 300) {
                                // Double tap — reserved for future use
                                lastTapTime = 0
                            } else {
                                lastTapTime = currentTime
                                scope.launch {
                                    delay(300) // Wait to confirm it's not a double tap
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
                            // Drag ended: spring-snap to the nearest horizontal edge
                            snapToNearestHorizontalEdge()
                            scope.launch {
                                // Small delay so the animation has started before we read the
                                // final target position for persistence
                                delay(350)
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
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size((buttonSize * 0.6f).dp)
        )
    }
}

/**
 * Calculates the move-aside target X: slides the button 50% off the nearest horizontal edge.
 * Only left/right edges are considered.
 */
private fun moveAsideTargetX(
    currentX: Int,
    density: androidx.compose.ui.unit.Density,
    screenWidthDp: Int,
    buttonSize: Float
): Int {
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }.toInt()
    val buttonSizePx = with(density) { buttonSize.dp.toPx() }.toInt()
    val halfButton = buttonSizePx / 2

    // Whichever horizontal edge is closest determines which side to hide on
    return if (currentX <= screenWidthPx / 2 - halfButton) {
        -halfButton          // Left edge: slide left until 50% hidden
    } else {
        screenWidthPx - halfButton  // Right edge: slide right until 50% hidden
    }
}

/**
 * Animates layoutParams.x from [from] to [to] using a spring-style ValueAnimator.
 * Calls windowManager.updateViewLayout on every animation frame.
 */
private fun animateToX(
    from: Int,
    to: Int,
    layoutParams: WindowManager.LayoutParams,
    windowManager: WindowManager,
    view: android.view.View
) {
    if (from == to) return

    ValueAnimator.ofInt(from, to).apply {
        duration = 350L
        interpolator = android.view.animation.OvershootInterpolator(1.5f)
        addUpdateListener { animator ->
            layoutParams.x = animator.animatedValue as Int
            try { windowManager.updateViewLayout(view, layoutParams) } catch (_: Exception) {}
        }
        start()
    }
}