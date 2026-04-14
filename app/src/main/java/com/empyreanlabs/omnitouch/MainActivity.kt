package com.empyreanlabs.omnitouch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.empyreanlabs.omnitouch.service.OverlayService
import com.empyreanlabs.omnitouch.ui.settings.SettingsSliderItem
import com.empyreanlabs.omnitouch.ui.MainViewModel
import com.empyreanlabs.omnitouch.ui.settings.SettingsScreen
import com.empyreanlabs.omnitouch.ui.theme.OmniTouchTheme
import com.empyreanlabs.omnitouch.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main configuration activity for Omni Touch.
 * Handles permission setup and provides access to settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Permission result handled in UI
    }

    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Permission result handled in UI
    }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Device admin activated
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniTouchTheme {
                var showSettings by remember { mutableStateOf(false) }
                val viewModel: MainViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showSettings = false }
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onRequestOverlayPermission = {
                                overlayPermissionLauncher.launch(
                                    PermissionUtils.requestOverlayPermission(this)
                                )
                            },
                            onRequestAccessibilityPermission = {
                                accessibilityPermissionLauncher.launch(
                                    PermissionUtils.requestAccessibilityPermission(this)
                                )
                            },
                            onRequestDeviceAdmin = {
                                deviceAdminLauncher.launch(
                                    PermissionUtils.requestDeviceAdminPermission(this)
                                )
                            },
                            onStartService = {
                                if (PermissionUtils.hasOverlayPermission(this)) {
                                    OverlayService.start(this)
                                }
                            },
                            onStopService = {
                                OverlayService.stop(this)
                            },
                            onNavigateToSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission states when returning from settings
        // The polling in ViewModel will pick up changes within 1 second
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val buttonOpacity by viewModel.buttonOpacity.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Omni Touch") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Status Card
            ServiceStatusCard(
                isRunning = isServiceRunning,
                canStart = permissionState.hasOverlayPermission,
                onStart = onStartService,
                onStop = onStopService
            )

            // Permissions Card
            PermissionsCard(
                permissionState = permissionState,
                onRequestOverlay = onRequestOverlayPermission,
                onRequestAccessibility = onRequestAccessibilityPermission,
                onRequestDeviceAdmin = onRequestDeviceAdmin
            )

            // Quick Settings Card
            QuickSettingsCard(
                buttonOpacity = buttonOpacity,
                onOpacityChange = { viewModel.updateButtonOpacity(it) }
            )
        }
    }
}

@Composable
fun ServiceStatusCard(
    isRunning: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                // Status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (isRunning) "Service Running" else "Service Stopped",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = if (isRunning) onStop else onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning && canStart || isRunning,
                colors = if (isRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(if (isRunning) "Stop Service" else "Start Service")
            }

            if (!canStart && !isRunning) {
                Text(
                    text = "Enable overlay permission to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PermissionsCard(
    permissionState: PermissionUtils.PermissionState,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestDeviceAdmin: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium
            )
            val grantedCount = listOf(
                permissionState.hasOverlayPermission,
                permissionState.hasAccessibilityService,
                permissionState.hasDeviceAdmin
            ).count { it }
            Text(
                text = "Setup: $grantedCount of 3 complete",
                style = MaterialTheme.typography.bodySmall,
                color = if (grantedCount == 3)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            PermissionItem(
                title = "Overlay Permission",
                description = "Required to display floating button",
                isGranted = permissionState.hasOverlayPermission,
                isRequired = true,
                onRequest = onRequestOverlay
            )

            PermissionItem(
                title = "Accessibility Service",
                description = "Required for system actions",
                isGranted = permissionState.hasAccessibilityService,
                isRequired = true,
                onRequest = onRequestAccessibility
            )

            PermissionItem(
                title = "Device Admin",
                description = "Required for lock screen action",
                isGranted = permissionState.hasDeviceAdmin,
                isRequired = false,
                onRequest = onRequestDeviceAdmin
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                if (isRequired) {
                    Text(
                        text = " *",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            OutlinedButton(onClick = onRequest) {
                Text("Enable")
            }
        }
    }
}

@Composable
fun QuickSettingsCard(
    buttonOpacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium
            )
            SettingsSliderItem(
                label = "Button Opacity",
                value = buttonOpacity,
                valueRange = 0.3f..1.0f,
                valueLabel = "${(buttonOpacity * 100).toInt()}%",
                onValueChange = onOpacityChange
            )
        }
    }
}