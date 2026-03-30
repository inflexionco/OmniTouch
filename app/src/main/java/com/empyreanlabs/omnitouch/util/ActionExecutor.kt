package com.empyreanlabs.omnitouch.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.getSystemService
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import com.empyreanlabs.omnitouch.receiver.OmniTouchDeviceAdminReceiver
import com.empyreanlabs.omnitouch.service.OmniTouchAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central executor for all Omni Touch actions.
 * Coordinates between AccessibilityService, DevicePolicyManager, and system APIs
 * to execute the requested actions.
 */
@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator = context.getSystemService<Vibrator>()
    private val audioManager = context.getSystemService<AudioManager>()
    private val cameraManager = context.getSystemService<CameraManager>()
    private var isFlashlightOn = false

    /**
     * Execute an Omni Touch action.
     * @param action The action to execute
     * @param enableHapticFeedback Whether to provide haptic feedback
     * @return true if action was executed successfully
     */
    fun executeAction(action: OmniTouchAction, enableHapticFeedback: Boolean = true): Boolean {
        if (enableHapticFeedback) {
            performHapticFeedback()
        }

        val result = when (action) {
            // System Navigation
            is OmniTouchAction.Home -> performHome()
            is OmniTouchAction.Back -> performBack()
            is OmniTouchAction.RecentApps -> performRecentApps()

            // Screen Actions
            is OmniTouchAction.TakeScreenshot -> takeScreenshot()
            is OmniTouchAction.LockScreen -> lockScreen()
            is OmniTouchAction.PowerDialog -> showPowerDialog()
            is OmniTouchAction.SplitScreen -> toggleSplitScreen()

            // Notification Panel
            is OmniTouchAction.NotificationPanel -> pullNotificationPanel()
            is OmniTouchAction.QuickSettings -> pullQuickSettings()

            // Device Features
            is OmniTouchAction.ToggleFlashlight -> toggleFlashlight()
            is OmniTouchAction.VolumeUp -> adjustVolume(true)
            is OmniTouchAction.VolumeDown -> adjustVolume(false)
            is OmniTouchAction.ToggleMute -> toggleMute()

            // App Shortcuts
            is OmniTouchAction.LaunchApp -> launchApp(action.packageName)
            is OmniTouchAction.OpenSettings -> openSettings(action.settingAction)

            // Other Actions
            is OmniTouchAction.ShowMenu -> true // Handled by OverlayService
            is OmniTouchAction.NoAction -> true
        }

        if (!result) {
            showError("Failed to execute action: ${action.displayName}")
        }

        return result
    }

    // ========== System Navigation Actions ==========

    private fun performHome(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.performHome()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    private fun performBack(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.performBack()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    private fun performRecentApps(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.performRecentApps()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    // ========== Screen Actions ==========

    private fun takeScreenshot(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.takeScreenshot()
            } else {
                showError("Screenshot requires Android 9.0 or higher")
                false
            }
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    private fun lockScreen(): Boolean {
        // Try using accessibility service first (Android 9.0+)
        val service = OmniTouchAccessibilityService.getInstance()
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val result = service.lockScreen()
            if (result) return true
        }

        // Fall back to Device Admin
        if (PermissionUtils.isDeviceAdminActive(context)) {
            return try {
                val devicePolicyManager = context.getSystemService<DevicePolicyManager>()
                val componentName = ComponentName(context, OmniTouchDeviceAdminReceiver::class.java)
                devicePolicyManager?.lockNow()
                true
            } catch (e: Exception) {
                showError("Failed to lock screen: ${e.message}")
                false
            }
        } else {
            showError("Device Admin permission required for lock screen")
            // Optionally, open device admin settings
            false
        }
    }

    private fun showPowerDialog(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.showPowerDialog()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    private fun toggleSplitScreen(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.toggleSplitScreen()
            } else {
                showError("Split screen requires Android 7.0 or higher")
                false
            }
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    // ========== Notification Panel Actions ==========

    private fun pullNotificationPanel(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.pullNotificationPanel()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    private fun pullQuickSettings(): Boolean {
        val service = OmniTouchAccessibilityService.getInstance()
        return if (service != null) {
            service.pullQuickSettings()
        } else {
            showAccessibilityServiceError()
            false
        }
    }

    // ========== Device Features ==========

    private fun toggleFlashlight(): Boolean {
        return try {
            if (cameraManager == null) {
                showError("Camera manager not available")
                return false
            }

            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
                true
            } else {
                showError("Flashlight toggle requires Android 6.0 or higher")
                false
            }
        } catch (e: Exception) {
            showError("Failed to toggle flashlight: ${e.message}")
            false
        }
    }

    private fun adjustVolume(increase: Boolean): Boolean {
        return try {
            if (audioManager == null) {
                showError("Audio manager not available")
                return false
            }

            val direction = if (increase) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            showError("Failed to adjust volume: ${e.message}")
            false
        }
    }

    private fun toggleMute(): Boolean {
        return try {
            if (audioManager == null) {
                showError("Audio manager not available")
                return false
            }

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            showError("Failed to toggle mute: ${e.message}")
            false
        }
    }

    // ========== App Shortcuts ==========

    private fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                showError("App not found: $packageName")
                false
            }
        } catch (e: Exception) {
            showError("Failed to launch app: ${e.message}")
            false
        }
    }

    private fun openSettings(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            showError("Failed to open settings: ${e.message}")
            false
        }
    }

    // ========== Helper Methods ==========

    private fun performHapticFeedback() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic feedback errors
        }
    }

    private fun showAccessibilityServiceError() {
        showError("Accessibility Service not available. Please enable it in settings.")
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Check if an action can be executed with current permissions and API level.
     */
    fun canExecuteAction(action: OmniTouchAction): Boolean {
        return when (action) {
            is OmniTouchAction.Home,
            is OmniTouchAction.Back,
            is OmniTouchAction.RecentApps,
            is OmniTouchAction.NotificationPanel,
            is OmniTouchAction.QuickSettings,
            is OmniTouchAction.PowerDialog -> OmniTouchAccessibilityService.isServiceAvailable()

            is OmniTouchAction.TakeScreenshot ->
                OmniTouchAccessibilityService.isServiceAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

            is OmniTouchAction.SplitScreen ->
                OmniTouchAccessibilityService.isServiceAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

            is OmniTouchAction.LockScreen ->
                OmniTouchAccessibilityService.isServiceAvailable() || PermissionUtils.isDeviceAdminActive(context)

            is OmniTouchAction.ToggleFlashlight -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            is OmniTouchAction.VolumeUp,
            is OmniTouchAction.VolumeDown,
            is OmniTouchAction.ToggleMute -> true

            is OmniTouchAction.LaunchApp,
            is OmniTouchAction.OpenSettings,
            is OmniTouchAction.ShowMenu,
            is OmniTouchAction.NoAction -> true
        }
    }
}