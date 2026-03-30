package com.empyreanlabs.omnitouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.empyreanlabs.omnitouch.data.SettingsRepository
import com.empyreanlabs.omnitouch.service.OverlayService
import com.empyreanlabs.omnitouch.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver that starts the overlay service on device boot.
 * Only starts if user has enabled "Start on Boot" in settings and has necessary permissions.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Use goAsync() to allow async operations
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if start on boot is enabled
                val startOnBoot = settingsRepository.startOnBoot.first()

                if (startOnBoot) {
                    // Check if we have necessary permissions
                    val hasOverlay = PermissionUtils.hasOverlayPermission(context)
                    val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)

                    // Only start if we have overlay permission (minimum requirement)
                    if (hasOverlay) {
                        OverlayService.start(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}