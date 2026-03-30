package com.empyreanlabs.omnitouch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empyreanlabs.omnitouch.data.SettingsRepository
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

    // Update settings
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
}