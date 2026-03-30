package com.empyreanlabs.omnitouch.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin Receiver for Omni Touch.
 * Enables the app to perform device administration tasks, specifically locking the screen.
 */
class OmniTouchDeviceAdminReceiver : DeviceAdminReceiver() {

    /**
     * Called when the device admin is enabled.
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "Omni Touch Device Admin enabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Called when the device admin is disabled.
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "Omni Touch Device Admin disabled. Lock screen action will not work.",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Called when the user asks to disable the device admin.
     * @return Message to show to the user
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Device Admin will prevent Omni Touch from locking your screen."
    }
}