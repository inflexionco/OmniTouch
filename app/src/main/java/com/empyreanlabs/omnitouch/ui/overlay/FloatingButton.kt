package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.launch

/**
 * Floating button overlay composable.
 * Handles drag, tap, long press, and double tap gestures.
 */
@Composable
fun FloatingButton(
    settingsRepository: SettingsRepository,
    actionExecutor: ActionExecutor,
    onMenuVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Button settings
    var buttonSize by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_SIZE) }
    var buttonOpacity by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_OPACITY) }
    var singleTapActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_SINGLE_TAP_ACTION) }
    var longPressActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_LONG_PRESS_ACTION) }
    var doubleTapActionId by remember { mutableStateOf(SettingsRepository.DEFAULT_DOUBLE_TAP_ACTION) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

    // Load settings
    LaunchedEffect(Unit) {
        launch {
            settingsRepository.buttonSize.collect { buttonSize = it }
        }
        launch {
            settingsRepository.buttonOpacity.collect { buttonOpacity = it }
        }
        launch {
            settingsRepository.singleTapAction.collect { singleTapActionId = it }
        }
        launch {
            settingsRepository.longPressAction.collect { longPressActionId = it }
        }
        launch {
            settingsRepository.doubleTapAction.collect { doubleTapActionId = it }
        }
        launch {
            settingsRepository.hapticFeedback.collect { hapticFeedback = it }
        }
    }

    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .alpha(buttonOpacity)
            .clip(CircleShape)
            .background(Color(0xFF2196F3))
            .pointerInput(singleTapActionId, longPressActionId, doubleTapActionId) {
                detectTapGestures(
                    onTap = {
                        // Handle single tap
                        scope.launch {
                            val action = OmniTouchAction.fromId(singleTapActionId)
                            if (action != null) {
                                if (action == OmniTouchAction.ShowMenu) {
                                    onMenuVisibilityChange(true)
                                } else {
                                    actionExecutor.executeAction(action, hapticFeedback)
                                }
                            }
                        }
                    },
                    onLongPress = {
                        // Handle long press
                        scope.launch {
                            val action = OmniTouchAction.fromId(longPressActionId)
                            if (action != null && action != OmniTouchAction.NoAction) {
                                actionExecutor.executeAction(action, hapticFeedback)
                            }
                        }
                    },
                    onDoubleTap = {
                        // Handle double tap
                        scope.launch {
                            val action = OmniTouchAction.fromId(doubleTapActionId)
                            if (action != null && action != OmniTouchAction.NoAction) {
                                actionExecutor.executeAction(action, hapticFeedback)
                            }
                        }
                    }
                )
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