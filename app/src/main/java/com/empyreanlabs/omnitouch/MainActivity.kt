package com.empyreanlabs.omnitouch

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.empyreanlabs.omnitouch.service.OverlayService
import com.empyreanlabs.omnitouch.ui.MainViewModel
import com.empyreanlabs.omnitouch.ui.settings.SettingsScreen
import com.empyreanlabs.omnitouch.ui.settings.SettingsSliderItem
import com.empyreanlabs.omnitouch.ui.theme.OmniTouchTheme
import com.empyreanlabs.omnitouch.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) { }
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
                            onStopService = { OverlayService.stop(this) },
                            onNavigateToSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

// ─── Colours matching the design reference ────────────────────────────────────
private val ServiceGreen       = Color(0xFF1B6B2F)   // hero card bg running
private val ServiceGreenLight  = Color(0xFF2E7D32)   // hero card bg lighter edge
private val ServiceStopped     = Color(0xFF455A64)   // hero card bg stopped
private val ServiceStoppedEdge = Color(0xFF546E7A)
private val GrantedGreen       = Color(0xFF2E7D32)
private val EnabledPill        = Color(0xFFEDE7F6)
private val IgnoredGreen       = Color(0xFF2E7D32)

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
    val buttonOpacity   by viewModel.buttonOpacity.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val hapticFeedback  by viewModel.hapticFeedback.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Omni Touch",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            ServiceStatusCard(
                isRunning = isServiceRunning,
                canStart = permissionState.hasOverlayPermission,
                onStart = onStartService,
                onStop = onStopService
            )

            PermissionsCard(
                permissionState = permissionState,
                onRequestOverlay = onRequestOverlayPermission,
                onRequestAccessibility = onRequestAccessibilityPermission,
                onRequestDeviceAdmin = onRequestDeviceAdmin
            )

            QuickSettingsCard(
                buttonOpacity = buttonOpacity,
                hapticFeedback = hapticFeedback,
                onOpacityChange = { viewModel.updateButtonOpacity(it) },
                onHapticChange = { viewModel.updateHapticFeedback(it) }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Service Status Hero Card ──────────────────────────────────────────────────

@Composable
fun ServiceStatusCard(
    isRunning: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val gradientColors = if (isRunning) {
        listOf(ServiceGreen, ServiceGreenLight)
    } else {
        listOf(ServiceStopped, ServiceStoppedEdge)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
    ) {
        // Decorative large semi-transparent circle (top-right)
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-16).dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = ((-8)).dp, y = 12.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(44.dp)
            )
        }

        // Text + button content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SYSTEM STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(
                    1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                )
            )
            Text(
                text = if (isRunning) "Service Started" else "Service Stopped",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRunning)
                    "Omni Touch is currently active and monitoring for gesture commands."
                else
                    "Start the service to enable the floating assistant button.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.80f)
            )

            Spacer(Modifier.height(8.dp))

            // Pill button
            Button(
                onClick = if (isRunning) onStop else onStart,
                enabled = isRunning || canStart,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = if (isRunning) ServiceGreen else ServiceStopped,
                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                    disabledContentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Stop Service" else "Start Service",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!canStart && !isRunning) {
                Text(
                    text = "Enable overlay permission to start",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// ─── Permissions Card ─────────────────────────────────────────────────────────

@Composable
fun PermissionsCard(
    permissionState: PermissionUtils.PermissionState,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestDeviceAdmin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row with icon badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionIconBadge(
                    icon = Icons.Default.Shield,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                )
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PermissionItem(
                title = "Accessibility",
                isGranted = permissionState.hasAccessibilityService,
                statusLabel = if (permissionState.hasAccessibilityService) "GRANTED" else null,
                onRequest = onRequestAccessibility
            )
            PermissionItem(
                title = "Overlay",
                isGranted = permissionState.hasOverlayPermission,
                statusLabel = null,
                onRequest = onRequestOverlay
            )
            PermissionItem(
                title = "Device Admin",
                isGranted = permissionState.hasDeviceAdmin,
                statusLabel = if (permissionState.hasDeviceAdmin) "IGNORED" else null,
                onRequest = onRequestDeviceAdmin
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    isGranted: Boolean,
    statusLabel: String?,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Filled circle icon — green check or red X
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isGranted) GrantedGreen else MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }

        // Right-side badge / button
        if (isGranted && statusLabel != null) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (statusLabel == "IGNORED") IgnoredGreen else GrantedGreen
            )
        } else if (!isGranted) {
            Surface(
                onClick = onRequest,
                shape = RoundedCornerShape(50.dp),
                color = EnabledPill
            ) {
                Text(
                    text = "ENABLE",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Quick Settings Card ──────────────────────────────────────────────────────

@Composable
fun QuickSettingsCard(
    buttonOpacity: Float,
    hapticFeedback: Boolean,
    onOpacityChange: (Float) -> Unit,
    onHapticChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionIconBadge(
                    icon = Icons.Default.Tune,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                )
                Text(
                    text = "Quick Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Opacity slider with inline value label
            SettingsSliderItem(
                label = "Button Opacity",
                value = buttonOpacity,
                valueRange = 0.3f..1.0f,
                valueLabel = "${(buttonOpacity * 100).toInt()}%",
                onValueChange = onOpacityChange
            )

            // Vibration Feedback toggle row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vibration Feedback",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = hapticFeedback,
                        onCheckedChange = onHapticChange
                    )
                }
            }
        }
    }
}

// ─── Shared: Section icon badge ───────────────────────────────────────────────

@Composable
fun SectionIconBadge(
    icon: ImageVector,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}