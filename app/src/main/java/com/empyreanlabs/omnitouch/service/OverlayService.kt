package com.empyreanlabs.omnitouch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.empyreanlabs.omnitouch.MainActivity
import com.empyreanlabs.omnitouch.R
import com.empyreanlabs.omnitouch.data.SettingsRepository
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import com.empyreanlabs.omnitouch.model.MenuLayoutType
import com.empyreanlabs.omnitouch.ui.overlay.GridPopupMenu
import com.empyreanlabs.omnitouch.ui.overlay.RadialWheelMenu
import com.empyreanlabs.omnitouch.ui.overlay.EdgeSnappingFloatingButton
import com.empyreanlabs.omnitouch.util.ActionExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that manages the floating button overlay.
 * Uses Jetpack Compose for the floating button UI.
 */
@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var actionExecutor: ActionExecutor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var composeView: ComposeView? = null
    private lateinit var windowManager: WindowManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    // Overlay state
    private var isMenuVisible by mutableStateOf(false)
    private var overlayParams: WindowManager.LayoutParams? = null
    // Saved button position (in screen coords) used to pass to menus when window goes full-screen
    private var savedButtonX by mutableStateOf(0)
    private var savedButtonY by mutableStateOf(0)

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "omni_touch_overlay"

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Start foreground service
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Add floating button overlay
        addFloatingButton()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeFloatingButton()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun addFloatingButton() {
        try {
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Flags for button-only mode: small window, touches outside pass through
            val buttonFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Flags for menu mode: full-screen, captures all touches to allow dismiss on outside tap
            val menuFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Start with button-only params (WRAP_CONTENT, touches pass through)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                buttonFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START

                // Load saved position or use default
                serviceScope.launch {
                    val (x, y) = settingsRepository.buttonPosition.first()
                    if (x != -1 && y != -1) {
                        this@apply.x = x
                        this@apply.y = y
                    }
                }
            }
            overlayParams = params

            // Create Compose view
            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    // Track button size and menu layout type from settings
                    var buttonSize by remember { mutableFloatStateOf(SettingsRepository.DEFAULT_BUTTON_SIZE) }
                    var menuLayoutType by remember { mutableStateOf(MenuLayoutType.GRID) }

                    LaunchedEffect(Unit) {
                        launch {
                            settingsRepository.buttonSize.collect { buttonSize = it }
                        }
                        launch {
                            settingsRepository.menuLayoutType.collect { menuLayoutType = it }
                        }
                    }

                    Box {
                        // Edge-snapping floating button with move-aside
                        EdgeSnappingFloatingButton(
                            settingsRepository = settingsRepository,
                            actionExecutor = actionExecutor,
                            windowManager = windowManager,
                            layoutParams = params,
                            view = this@apply,
                            onMenuVisibilityChange = { visible ->
                                if (visible) {
                                    // Save current button screen position before expanding
                                    savedButtonX = params.x
                                    savedButtonY = params.y
                                }
                                isMenuVisible = visible
                                // Expand window to full-screen when menu opens so items are fully
                                // visible and touches outside the menu area dismiss it.
                                // Shrink back to WRAP_CONTENT when menu closes.
                                params.width = if (visible) {
                                    WindowManager.LayoutParams.MATCH_PARENT
                                } else {
                                    WindowManager.LayoutParams.WRAP_CONTENT
                                }
                                params.height = if (visible) {
                                    WindowManager.LayoutParams.MATCH_PARENT
                                } else {
                                    WindowManager.LayoutParams.WRAP_CONTENT
                                }
                                params.flags = if (visible) menuFlags else buttonFlags
                                if (visible) {
                                    // Full-screen: reset x/y to 0 (composable handles positioning)
                                    params.x = 0
                                    params.y = 0
                                } else {
                                    // Restore the saved button position
                                    params.x = savedButtonX
                                    params.y = savedButtonY
                                }
                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (_: Exception) {}
                            }
                        )

                        // Show menu overlay when visible (switch between Grid and Radial)
                        if (isMenuVisible) {
                            when (menuLayoutType) {
                                MenuLayoutType.GRID -> GridPopupMenu(
                                    settingsRepository = settingsRepository,
                                    actionExecutor = actionExecutor,
                                    buttonX = savedButtonX,
                                    buttonY = savedButtonY,
                                    buttonSize = buttonSize,
                                    onDismiss = {
                                        isMenuVisible = false
                                    }
                                )
                                MenuLayoutType.RADIAL -> RadialWheelMenu(
                                    settingsRepository = settingsRepository,
                                    actionExecutor = actionExecutor,
                                    buttonX = savedButtonX,
                                    buttonY = savedButtonY,
                                    buttonSize = buttonSize,
                                    onDismiss = {
                                        isMenuVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            composeView = view

            // Add view to window
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
        }
    }

    private fun removeFloatingButton() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_service_notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_notification_title))
            .setContentText(getString(R.string.overlay_service_notification_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}