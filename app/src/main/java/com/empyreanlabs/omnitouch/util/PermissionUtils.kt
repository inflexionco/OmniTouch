package com.empyreanlabs.omnitouch.util

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import com.empyreanlabs.omnitouch.receiver.OmniTouchDeviceAdminReceiver
import com.empyreanlabs.omnitouch.service.OmniTouchAccessibilityService

/**
 * Utility object for handling all permission-related checks and requests.
 */
object PermissionUtils {

    /**
     * Check if SYSTEM_ALERT_WINDOW permission is granted.
     * This permission is required to draw overlay windows.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // Permission is automatically granted on Android < 6.0
            true
        }
    }

    /**
     * Request SYSTEM_ALERT_WINDOW permission by opening the settings screen.
     * Returns an Intent that should be launched with startActivityForResult.
     */
    fun requestOverlayPermission(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            // For older versions, open app settings
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Check if Accessibility Service is enabled.
     * This is required for performing system-level actions.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = ":"
        val componentName = ComponentName(context, OmniTouchAccessibilityService::class.java)
        val flatComponentName = componentName.flattenToString()

        return enabledServices.split(colonSplitter)
            .any { it.equals(flatComponentName, ignoreCase = true) }
    }

    /**
     * Request Accessibility Service permission by opening the settings screen.
     */
    fun requestAccessibilityPermission(context: Context): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Check if Device Admin is active.
     * This is required for locking the screen.
     */
    fun isDeviceAdminActive(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService<DevicePolicyManager>()
            ?: return false
        val componentName = ComponentName(context, OmniTouchDeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }

    /**
     * Request Device Admin activation.
     * Returns an Intent that should be launched with startActivityForResult.
     */
    fun requestDeviceAdminPermission(context: Context): Intent {
        val componentName = ComponentName(context, OmniTouchDeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Omni Touch needs Device Admin permission to lock your screen"
            )
        }
    }

    /**
     * Remove Device Admin permission.
     */
    fun removeDeviceAdmin(context: Context) {
        val devicePolicyManager = context.getSystemService<DevicePolicyManager>() ?: return
        val componentName = ComponentName(context, OmniTouchDeviceAdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.removeActiveAdmin(componentName)
        }
    }

    /**
     * Check if a service is running.
     */
    @Suppress("DEPRECATION")
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService<ActivityManager>() ?: return false
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    /**
     * Data class to hold the overall permission state.
     */
    data class PermissionState(
        val hasOverlayPermission: Boolean,
        val hasAccessibilityService: Boolean,
        val hasDeviceAdmin: Boolean
    ) {
        val hasAllRequiredPermissions: Boolean
            get() = hasOverlayPermission && hasAccessibilityService

        val hasAllPermissions: Boolean
            get() = hasOverlayPermission && hasAccessibilityService && hasDeviceAdmin
    }

    /**
     * Get the current state of all permissions.
     */
    fun getPermissionState(context: Context): PermissionState {
        return PermissionState(
            hasOverlayPermission = hasOverlayPermission(context),
            hasAccessibilityService = isAccessibilityServiceEnabled(context),
            hasDeviceAdmin = isDeviceAdminActive(context)
        )
    }
}