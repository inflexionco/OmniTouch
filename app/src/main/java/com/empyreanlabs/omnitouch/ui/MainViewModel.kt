package com.empyreanlabs.omnitouch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.model.MenuLayoutType
import com.empyreanlabs.omnitouch.service.OverlayService
import com.empyreanlabs.omnitouch.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 * Manages permission states, service status, and user settings.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Manual refresh trigger
    private val _refreshTrigger = MutableStateFlow(0)

    fun refreshPermissions() {
        _refreshTrigger.value++
    }

    // Permission state flow
    val permissionState: StateFlow<PermissionUtils.PermissionState> = flow {
        while (true) {
            emit(PermissionUtils.getPermissionState(context))
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PermissionUtils.getPermissionState(context)
    )

    // Service running state
    val isServiceRunning: StateFlow<Boolean> = flow {
        while (true) {
            emit(PermissionUtils.isServiceRunning(context, OverlayService::class.java))
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PermissionUtils.isServiceRunning(context, OverlayService::class.java)
    )

    // Button settings
    val buttonOpacity: StateFlow<Float> = settingsRepository.buttonOpacity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_BUTTON_OPACITY
        )

    val buttonSize: StateFlow<Float> = settingsRepository.buttonSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_BUTTON_SIZE
        )

    // Menu settings
    val menuLayoutType: StateFlow<MenuLayoutType> = settingsRepository.menuLayoutType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MenuLayoutType.GRID
        )

    val menuGridSize: StateFlow<Int> = settingsRepository.menuGridSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_MENU_GRID_SIZE
        )

    val menuActions: StateFlow<List<String>> = settingsRepository.menuActions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_MENU_ACTIONS.map { it.id }
        )

    // App settings
    val startOnBoot: StateFlow<Boolean> = settingsRepository.startOnBoot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_START_ON_BOOT
        )

    val hapticFeedback: StateFlow<Boolean> = settingsRepository.hapticFeedback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_HAPTIC_FEEDBACK
        )

    // Button appearance
    val useCustomIcon: StateFlow<Boolean> = settingsRepository.useCustomIcon
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Button behavior
    val autoHideOnKeyboard: StateFlow<Boolean> = settingsRepository.autoHideOnKeyboard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val stickToEdges: StateFlow<Boolean> = settingsRepository.stickToEdges
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val longPressAction: StateFlow<String> = settingsRepository.longPressAction
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_LONG_PRESS_ACTION
        )

    // App preferences
    val pushNotifications: StateFlow<Boolean> = settingsRepository.pushNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val appLanguage: StateFlow<String> = settingsRepository.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "English"
        )

    // Update button settings
    fun updateButtonOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.updateButtonOpacity(opacity)
        }
    }

    fun updateButtonSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateButtonSize(size)
        }
    }

    // Update menu settings
    fun updateMenuLayoutType(layoutType: MenuLayoutType) {
        viewModelScope.launch {
            settingsRepository.updateMenuLayoutType(layoutType)
        }
    }

    fun updateMenuGridSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateMenuGridSize(size)
        }
    }

    fun updateMenuActions(actions: List<String>) {
        viewModelScope.launch {
            settingsRepository.updateMenuActions(actions)
        }
    }

    // Update app settings
    fun updateStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStartOnBoot(enabled)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedback(enabled)
        }
    }

    fun updateUseCustomIcon(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateUseCustomIcon(enabled) }
    }

    fun updateAutoHideOnKeyboard(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoHideOnKeyboard(enabled) }
    }

    fun updateStickToEdges(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStickToEdges(enabled) }
    }

    fun updateLongPressAction(actionId: String) {
        viewModelScope.launch { settingsRepository.updateLongPressAction(actionId) }
    }

    fun updatePushNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updatePushNotifications(enabled) }
    }

    fun updateAppLanguage(language: String) {
        viewModelScope.launch { settingsRepository.updateAppLanguage(language) }
    }

    fun resetAllSettings() {
        viewModelScope.launch { settingsRepository.resetAllSettings() }
    }
}