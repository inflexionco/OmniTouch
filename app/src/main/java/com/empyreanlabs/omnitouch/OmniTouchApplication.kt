package com.empyreanlabs.omnitouch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Omni Touch.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class OmniTouchApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here if needed
    }
}