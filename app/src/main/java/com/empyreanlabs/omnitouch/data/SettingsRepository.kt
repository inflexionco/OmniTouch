package com.empyreanlabs.omnitouch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.empyreanlabs.omnitouch.model.MenuLayoutType
import com.empyreanlabs.omnitouch.model.OmniTouchAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omni_touch_settings")

/**
 * Repository for managing app settings using DataStore.
 * Handles all user preferences for floating button, menu configuration, and app behavior.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Preference Keys
    private object PreferenceKeys {
        // Floating Button Settings
        val BUTTON_SIZE = floatPreferencesKey("button_size")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val BUTTON_POSITION_X = intPreferencesKey("button_position_x")
        val BUTTON_POSITION_Y = intPreferencesKey("button_position_y")

        // Button Actions
        val SINGLE_TAP_ACTION = stringPreferencesKey("single_tap_action")
        val LONG_PRESS_ACTION = stringPreferencesKey("long_press_action")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")

        // Menu Configuration (storing action IDs as comma-separated string)
        val MENU_LAYOUT_TYPE = stringPreferencesKey("menu_layout_type")
        val MENU_GRID_SIZE = intPreferencesKey("menu_grid_size")
        val MENU_ACTIONS = stringPreferencesKey("menu_actions")

        // App Settings
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // Default Values
    companion object {
        const val DEFAULT_BUTTON_SIZE = 60f
        const val DEFAULT_BUTTON_OPACITY = 0.8f
        val DEFAULT_MENU_LAYOUT_TYPE = MenuLayoutType.GRID.id
        const val DEFAULT_MENU_GRID_SIZE = 3
        val DEFAULT_SINGLE_TAP_ACTION = OmniTouchAction.ShowMenu.id
        val DEFAULT_LONG_PRESS_ACTION = OmniTouchAction.NoAction.id
        val DEFAULT_DOUBLE_TAP_ACTION = OmniTouchAction.NoAction.id
        const val DEFAULT_START_ON_BOOT = true
        const val DEFAULT_HAPTIC_FEEDBACK = true
        const val DEFAULT_FIRST_LAUNCH = true

        // Default menu actions (9 slots for 3x3 grid)
        val DEFAULT_MENU_ACTIONS = listOf(
            OmniTouchAction.Home,
            OmniTouchAction.Back,
            OmniTouchAction.RecentApps,
            OmniTouchAction.TakeScreenshot,
            OmniTouchAction.NotificationPanel,
            OmniTouchAction.QuickSettings,
            OmniTouchAction.LockScreen,
            OmniTouchAction.ToggleFlashlight,
            OmniTouchAction.NoAction
        )
    }

    // Floating Button Settings
    val buttonSize: Flow<Float> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.BUTTON_SIZE] ?: DEFAULT_BUTTON_SIZE
        }

    val buttonOpacity: Flow<Float> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.BUTTON_OPACITY] ?: DEFAULT_BUTTON_OPACITY
        }

    val buttonPosition: Flow<Pair<Int, Int>> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            val x = preferences[PreferenceKeys.BUTTON_POSITION_X] ?: -1
            val y = preferences[PreferenceKeys.BUTTON_POSITION_Y] ?: -1
            Pair(x, y)
        }

    // Button Actions
    val singleTapAction: Flow<String> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.SINGLE_TAP_ACTION] ?: DEFAULT_SINGLE_TAP_ACTION
        }

    val longPressAction: Flow<String> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.LONG_PRESS_ACTION] ?: DEFAULT_LONG_PRESS_ACTION
        }

    val doubleTapAction: Flow<String> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.DOUBLE_TAP_ACTION] ?: DEFAULT_DOUBLE_TAP_ACTION
        }

    // Menu Configuration
    val menuLayoutType: Flow<MenuLayoutType> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            val layoutTypeId = preferences[PreferenceKeys.MENU_LAYOUT_TYPE] ?: DEFAULT_MENU_LAYOUT_TYPE
            MenuLayoutType.fromId(layoutTypeId)
        }

    val menuGridSize: Flow<Int> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.MENU_GRID_SIZE] ?: DEFAULT_MENU_GRID_SIZE
        }

    val menuActions: Flow<List<String>> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            val actionsString = preferences[PreferenceKeys.MENU_ACTIONS]
            if (actionsString.isNullOrBlank()) {
                DEFAULT_MENU_ACTIONS.map { it.id }
            } else {
                actionsString.split(",").filter { it.isNotBlank() }
            }
        }

    // App Settings
    val startOnBoot: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.START_ON_BOOT] ?: DEFAULT_START_ON_BOOT
        }

    val hapticFeedback: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.HAPTIC_FEEDBACK] ?: DEFAULT_HAPTIC_FEEDBACK
        }

    val isFirstLaunch: Flow<Boolean> = dataStore.data
        .catch { handleException(it) }
        .map { preferences ->
            preferences[PreferenceKeys.FIRST_LAUNCH] ?: DEFAULT_FIRST_LAUNCH
        }

    // Update functions
    suspend fun updateButtonSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BUTTON_SIZE] = size
        }
    }

    suspend fun updateButtonOpacity(opacity: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BUTTON_OPACITY] = opacity
        }
    }

    suspend fun updateButtonPosition(x: Int, y: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BUTTON_POSITION_X] = x
            preferences[PreferenceKeys.BUTTON_POSITION_Y] = y
        }
    }

    suspend fun updateSingleTapAction(actionId: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SINGLE_TAP_ACTION] = actionId
        }
    }

    suspend fun updateLongPressAction(actionId: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LONG_PRESS_ACTION] = actionId
        }
    }

    suspend fun updateDoubleTapAction(actionId: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DOUBLE_TAP_ACTION] = actionId
        }
    }

    suspend fun updateMenuLayoutType(layoutType: MenuLayoutType) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.MENU_LAYOUT_TYPE] = layoutType.id
        }
    }

    suspend fun updateMenuGridSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.MENU_GRID_SIZE] = size
        }
    }

    suspend fun updateMenuActions(actions: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.MENU_ACTIONS] = actions.joinToString(",")
        }
    }

    suspend fun updateStartOnBoot(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.START_ON_BOOT] = enabled
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FIRST_LAUNCH] = false
        }
    }

    private fun handleException(exception: Throwable): Preferences {
        if (exception is IOException) {
            // Handle IOException appropriately
            return emptyPreferences()
        } else {
            throw exception
        }
    }
}