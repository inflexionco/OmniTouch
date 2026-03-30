package com.empyreanlabs.omnitouch.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents all possible actions that can be triggered by Omni Touch.
 * Each action has a unique ID, display name, description, and icon.
 */
sealed class OmniTouchAction(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    // System Navigation Actions
    data object Home : OmniTouchAction(
        id = "home",
        displayName = "Home",
        description = "Go to home screen",
        icon = Icons.Default.Home
    )

    data object Back : OmniTouchAction(
        id = "back",
        displayName = "Back",
        description = "Go back",
        icon = Icons.Default.ArrowBack
    )

    data object RecentApps : OmniTouchAction(
        id = "recent_apps",
        displayName = "Recent Apps",
        description = "Show recent apps",
        icon = Icons.Default.Apps
    )

    // Screen Actions
    data object TakeScreenshot : OmniTouchAction(
        id = "screenshot",
        displayName = "Screenshot",
        description = "Take a screenshot",
        icon = Icons.Default.CameraAlt
    )

    data object LockScreen : OmniTouchAction(
        id = "lock_screen",
        displayName = "Lock Screen",
        description = "Lock the device",
        icon = Icons.Default.Lock
    )

    data object PowerDialog : OmniTouchAction(
        id = "power_dialog",
        displayName = "Power Menu",
        description = "Show power dialog",
        icon = Icons.Default.PowerSettingsNew
    )

    data object SplitScreen : OmniTouchAction(
        id = "split_screen",
        displayName = "Split Screen",
        description = "Toggle split screen",
        icon = Icons.Default.ViewAgenda
    )

    // Notification Panel Actions
    data object NotificationPanel : OmniTouchAction(
        id = "notification_panel",
        displayName = "Notifications",
        description = "Pull down notification panel",
        icon = Icons.Default.Notifications
    )

    data object QuickSettings : OmniTouchAction(
        id = "quick_settings",
        displayName = "Quick Settings",
        description = "Open quick settings",
        icon = Icons.Default.Settings
    )

    // Device Features
    data object ToggleFlashlight : OmniTouchAction(
        id = "toggle_flashlight",
        displayName = "Flashlight",
        description = "Toggle flashlight on/off",
        icon = Icons.Default.Flashlight
    )

    data object VolumeUp : OmniTouchAction(
        id = "volume_up",
        displayName = "Volume Up",
        description = "Increase volume",
        icon = Icons.Default.VolumeUp
    )

    data object VolumeDown : OmniTouchAction(
        id = "volume_down",
        displayName = "Volume Down",
        description = "Decrease volume",
        icon = Icons.Default.VolumeDown
    )

    data object ToggleMute : OmniTouchAction(
        id = "toggle_mute",
        displayName = "Mute",
        description = "Toggle mute",
        icon = Icons.Default.VolumeOff
    )

    // App Shortcuts
    data class LaunchApp(
        val packageName: String,
        val appName: String,
        @DrawableRes val appIconRes: Int? = null
    ) : OmniTouchAction(
        id = "launch_app_$packageName",
        displayName = appName,
        description = "Launch $appName",
        icon = Icons.Default.Apps
    )

    // Settings Shortcuts
    data class OpenSettings(
        val settingAction: String,
        val settingName: String
    ) : OmniTouchAction(
        id = "open_settings_$settingAction",
        displayName = settingName,
        description = "Open $settingName settings",
        icon = Icons.Default.Settings
    ) {
        companion object {
            // Common settings actions
            const val WIFI_SETTINGS = "android.settings.WIFI_SETTINGS"
            const val BLUETOOTH_SETTINGS = "android.settings.BLUETOOTH_SETTINGS"
            const val LOCATION_SETTINGS = "android.settings.LOCATION_SOURCE_SETTINGS"
            const val DISPLAY_SETTINGS = "android.settings.DISPLAY_SETTINGS"
            const val SOUND_SETTINGS = "android.settings.SOUND_SETTINGS"
            const val ACCESSIBILITY_SETTINGS = "android.settings.ACCESSIBILITY_SETTINGS"
        }
    }

    // Custom Actions
    data object ShowMenu : OmniTouchAction(
        id = "show_menu",
        displayName = "Show Menu",
        description = "Show Omni Touch menu",
        icon = Icons.Default.Menu
    )

    data object NoAction : OmniTouchAction(
        id = "no_action",
        displayName = "No Action",
        description = "Do nothing",
        icon = Icons.Default.Block
    )

    companion object {
        /**
         * Get all predefined actions (excluding LaunchApp and OpenSettings with parameters)
         */
        fun getAllPredefinedActions(): List<OmniTouchAction> = listOf(
            Home,
            Back,
            RecentApps,
            TakeScreenshot,
            LockScreen,
            PowerDialog,
            SplitScreen,
            NotificationPanel,
            QuickSettings,
            ToggleFlashlight,
            VolumeUp,
            VolumeDown,
            ToggleMute,
            ShowMenu,
            NoAction
        )

        /**
         * Get common settings shortcuts
         */
        fun getCommonSettingsActions(): List<OpenSettings> = listOf(
            OpenSettings(OpenSettings.WIFI_SETTINGS, "Wi-Fi"),
            OpenSettings(OpenSettings.BLUETOOTH_SETTINGS, "Bluetooth"),
            OpenSettings(OpenSettings.LOCATION_SETTINGS, "Location"),
            OpenSettings(OpenSettings.DISPLAY_SETTINGS, "Display"),
            OpenSettings(OpenSettings.SOUND_SETTINGS, "Sound"),
            OpenSettings(OpenSettings.ACCESSIBILITY_SETTINGS, "Accessibility")
        )

        /**
         * Get action by ID
         */
        fun fromId(id: String): OmniTouchAction? {
            return getAllPredefinedActions().find { it.id == id }
                ?: getCommonSettingsActions().find { it.id == id }
        }
    }
}