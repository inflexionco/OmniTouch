package com.empyreanlabs.omnitouch.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.util.ActionExecutor
import kotlinx.coroutines.launch

/**
 * Assistive Touch menu that displays a grid of actions.
 * Shows when the floating button is tapped.
 */
@Composable
fun AssistiveMenu(
    settingsRepository: SettingsRepository,
    actionExecutor: ActionExecutor,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Menu settings
    var menuActions by remember { mutableStateOf<List<OmniTouchAction>>(emptyList()) }
    var gridSize by remember { mutableIntStateOf(SettingsRepository.DEFAULT_MENU_GRID_SIZE) }
    var hapticFeedback by remember { mutableStateOf(SettingsRepository.DEFAULT_HAPTIC_FEEDBACK) }

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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss), // Dismiss on background tap
        contentAlignment = Alignment.Center
    ) {
        // Menu grid
        Column(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
                .clickable(enabled = false) { }, // Prevent dismissal when clicking menu
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calculate rows based on grid size
            val rows = (menuActions.size + gridSize - 1) / gridSize

            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until gridSize) {
                        val index = row * gridSize + col
                        if (index < menuActions.size) {
                            val action = menuActions[index]
                            MenuItem(
                                action = action,
                                onClick = {
                                    scope.launch {
                                        actionExecutor.executeAction(action, hapticFeedback)
                                        onDismiss()
                                    }
                                }
                            )
                        } else {
                            // Empty slot
                            Spacer(modifier = Modifier.size(64.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    action: OmniTouchAction,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.displayName,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.displayName,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}