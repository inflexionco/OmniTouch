package com.empyreanlabs.omnitouch.ui.overlay

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Draggable floating button that can be moved around the screen.
 * Uses View-level touch handling for proper drag behavior with WindowManager.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun DraggableFloatingButton(
    settingsRepository: SettingsRepository,
    actionExecutor: ActionExecutor,
    windowManager: WindowManager,
    layoutParams: WindowManager.LayoutParams,
    view: android.view.View,
    onMenuVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Button settings
    var buttonSize by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_SIZE) }
    var buttonOpacity by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_OPACITY) }
    var singleTapActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_SINGLE_TAP_ACTION) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Drag state
    var initialX by remember { mutableIntStateOf(0) }
    var initialY by remember { mutableIntStateOf(0) }
    var initialTouchX by remember { mutableFloatStateOf(0f) }
    var initialTouchY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Load settings
    LaunchedEffect(Unit) {
        launch { settingsRepository.buttonSize.collect { buttonSize = it } }
        launch { settingsRepository.buttonOpacity.collect { buttonOpacity = it } }
        launch { settingsRepository.singleTapAction.collect { singleTapActionId = it } }
        launch { settingsRepository.hapticFeedback.collect { hapticFeedback = it } }
    }

    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .alpha(buttonOpacity)
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
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        // If moved more than threshold, it's a drag
                        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                            isDragging = true
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // It's a tap, not a drag
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
                                    kotlinx.coroutines.delay(300) // Wait to see if double tap
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
                            // Save position after drag
                            scope.launch {
                                settingsRepository.updateButtonPosition(layoutParams.x, layoutParams.y)
                            }
                        }
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