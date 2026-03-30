package com.empyreanlabs.omnitouch.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Accessibility Service for Omni Touch.
 * Provides system-level action capabilities like Home, Back, Recent Apps, Screenshot, etc.
 *
 * Design Note: This service uses a companion object with a static reference to make it
 * accessible from other components. This is a common pattern for accessibility services
 * as they need to be invoked from various parts of the app.
 */
class OmniTouchAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        // Static reference to the service instance
        // This allows other components to execute actions through the accessibility service
        @Volatile
        private var instance: OmniTouchAccessibilityService? = null

        fun getInstance(): OmniTouchAccessibilityService? = instance

        fun isServiceAvailable(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Configure the service
        serviceInfo = serviceInfo.apply {
            // We don't need to monitor any specific events, just perform global actions
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for Omni Touch functionality
        // This service is primarily used for performing global actions
    }

    override fun onInterrupt() {
        // Called when the system wants to interrupt the feedback this service is providing
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    // ========== Global Action Methods ==========

    /**
     * Navigate to home screen.
     * @return true if action was performed successfully
     */
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Navigate back.
     * @return true if action was performed successfully
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Show recent apps.
     * @return true if action was performed successfully
     */
    fun performRecentApps(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Take a screenshot.
     * Requires Android 9.0 (API 28) or higher.
     * @return true if action was performed successfully
     */
    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            // Screenshot not supported on older versions via accessibility service
            false
        }
    }

    /**
     * Pull down notification panel.
     * @return true if action was performed successfully
     */
    fun pullNotificationPanel(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Pull down quick settings panel.
     * @return true if action was performed successfully
     */
    fun pullQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * Show power dialog (requires Android 6.0+).
     * @return true if action was performed successfully
     */
    fun showPowerDialog(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        } else {
            false
        }
    }

    /**
     * Toggle split screen mode (requires Android 7.0+).
     * @return true if action was performed successfully
     */
    fun toggleSplitScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        } else {
            false
        }
    }

    /**
     * Lock the screen (requires Android 9.0+).
     * Note: This requires the GLOBAL_ACTION_LOCK_SCREEN action.
     * For older versions, use DevicePolicyManager instead.
     * @return true if action was performed successfully
     */
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            // Use DevicePolicyManager for older versions
            false
        }
    }

    /**
     * Accessibility button clicked (requires Android 8.0+).
     * @return true if action was performed successfully
     */
    fun clickAccessibilityButton(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON)
        } else {
            false
        }
    }

    /**
     * Show accessibility button menu (requires Android 11+).
     * @return true if action was performed successfully
     */
    fun showAccessibilityButtonMenu(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER)
        } else {
            false
        }
    }

    /**
     * Dismiss notification shade (requires Android 12+).
     * @return true if action was performed successfully
     */
    fun dismissNotificationShade(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            false
        }
    }

    // ========== Helper Methods ==========

    /**
     * Get the root node of the current window for advanced interactions.
     */
    override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a specific global action is available.
     */
    fun isGlobalActionAvailable(action: Int): Boolean {
        return when (action) {
            GLOBAL_ACTION_TAKE_SCREENSHOT -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            GLOBAL_ACTION_LOCK_SCREEN -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            GLOBAL_ACTION_ACCESSIBILITY_BUTTON -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            else -> true // Home, Back, Recents, Notifications, Quick Settings, Power Dialog are always available
        }
    }
}