# OmniTouch — Claude Context

## Project Overview
Android floating assistant button app (like iOS AssistiveTouch / MIUI Quick Ball).
A persistent overlay button docks to the left or right screen edge. Tapping it opens
a configurable action menu. Package: `com.empyreanlabs.omnitouch`.

## Build Commands
```bash
# Compile only (fast check)
./gradlew :app:compileDebugKotlin

# Full debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Tech Stack
- Kotlin + Jetpack Compose (Material 3 **1.4.0 Expressive**)
- Hilt (dependency injection)
- DataStore Preferences (settings persistence)
- WindowManager overlay (`TYPE_APPLICATION_OVERLAY`)
- AccessibilityService for system actions (Back, Recents, etc.)
- DeviceAdminReceiver for Lock Screen action
- Compose BOM: `2025.05.01`, material3 explicitly pinned to `1.4.0`

## Source Structure
```
app/src/main/java/com/empyreanlabs/omnitouch/
├── MainActivity.kt               # Main app screen (service status, permissions)
├── OmniTouchApplication.kt       # Hilt application class
├── data/
│   └── SettingsRepository.kt     # DataStore: all persistent settings + flows
├── model/
│   ├── OmniTouchAction.kt        # Sealed class of all triggerable actions
│   └── MenuLayoutType.kt         # GRID | RADIAL enum
├── receiver/
│   ├── BootReceiver.kt           # Auto-start on boot
│   └── OmniTouchDeviceAdminReceiver.kt
├── service/
│   ├── OverlayService.kt         # WindowManager overlay lifecycle + menu toggle
│   └── OmniTouchAccessibilityService.kt
├── ui/
│   ├── MainViewModel.kt          # StateFlows + update functions for all settings
│   ├── overlay/
│   │   ├── EdgeSnappingFloatingButton.kt  # THE floating button composable
│   │   ├── GridPopupMenu.kt              # Grid tile menu (no card background)
│   │   ├── RadialWheelMenu.kt            # Radial/semicircle menu
│   │   └── AssistiveMenu.kt, DraggableFloatingButton.kt, FloatingButton.kt  # Legacy/unused
│   ├── settings/
│   │   └── SettingsScreen.kt     # Full settings UI
│   └── theme/
│       ├── Theme.kt              # OmniTouchTheme, dynamic color, M3
│       ├── Color.kt
│       └── Type.kt
└── util/
    ├── ActionExecutor.kt         # Executes OmniTouchAction via Accessibility/DeviceAdmin
    └── PermissionUtils.kt
```

## Key Architecture Decisions

### Overlay Window Management (OverlayService.kt)
- Single `ComposeView` with one `WindowManager.LayoutParams` instance (`params`)
- Button mode: `WRAP_CONTENT`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`
- Menu mode: `MATCH_PARENT`, `FLAG_NOT_FOCUSABLE` (no TOUCH_MODAL so bg taps dismiss)
- `savedButtonX/Y` captures button position before expanding to full-screen
- `params.x = 0, params.y = 0` when menu opens; restored on close

### EdgeSnappingFloatingButton.kt
- Docks only to left/right edges (never top/bottom/center)
- Move-aside: after 5s idle → slides 50% off edge, dims to 40% opacity
- Restores on any touch (including settings slider changes for size/opacity)
- Menu dismissed automatically when button goes inactive (`isMovedAside = true`)
- Spring animations: `DampingRatioNoBouncy + StiffnessMediumLow` for opacity

### RadialWheelMenu.kt — Orientation Math (critical, has been fixed twice)
```
orientation=0.0   → startAngle = -90°..+90° → cos positive → fans RIGHT (left-edge button)
orientation=180.0 → startAngle = 90°..270°  → cos negative → fans LEFT  (right-edge button)
```
- Left button: `buttonX < screenWidthPx/2` → `orientation = 0.0`
- Right button: → `orientation = 180.0`
- Items are Y-clamped to keep them within screen bounds
- `isVisible = true` fires AFTER first `menuActions` emission (not immediately)

### GridPopupMenu.kt
- No wrapping card background — tiles float freely with spacing
- Guarded by `if (menuActions.isNotEmpty())` to avoid flash at (0,0)
- Position: left button → menu to right; right button → menu to left
- `isVisible = true` fires AFTER first `menuActions` emission

### Settings (SettingsRepository.kt)
All settings use DataStore. Key flows:
`buttonSize`, `buttonOpacity`, `menuLayoutType`, `menuGridSize`, `menuActions`,
`startOnBoot`, `hapticFeedback`, `useCustomIcon`, `autoHideOnKeyboard`,
`stickToEdges`, `longPressAction`, `singleTapAction`, `pushNotifications`, `appLanguage`

### MainViewModel.kt
Exposes all repository flows as `StateFlow`s via `stateIn(WhileSubscribed(5000))`.
Every setting has a corresponding `fun update*()` that `viewModelScope.launch`es.
`resetAllSettings()` calls `dataStore.edit { it.clear() }`.

## Material 3 Expressive Guidelines
- **No hardcoded `Color(0xFF...)`** — use `MaterialTheme.colorScheme.*` tokens
- **No hardcoded `RoundedCornerShape(N.dp)`** — use `MaterialTheme.shapes.*`
  - Cards/hero sections: `extraLarge`, tiles: `medium`, chips: `extraSmall`, pills: `CircleShape`
- **No `tween()` animations** — use `spring()` throughout
  - Spatial/scale: `DampingRatioMediumBouncy + StiffnessMedium/Low`
  - Opacity only: `DampingRatioNoBouncy + StiffnessMediumLow`
  - Staggered cascade: `(StiffnessMedium - index * 30f).coerceAtLeast(StiffnessLow)`
- **Typography**: use `MaterialTheme.typography.*` tokens, not hardcoded `sp`
- `MotionScheme` is `internal` in M3 1.4.0 — do NOT attempt to use it directly

## Commit Convention
- No Claude attribution lines (`🤖 Generated with...` / `Co-Authored-By: Claude`)
- Always add: `Co-Authored-By: Indrajeetsinh Chauhan <indrajeetsinhchauhan@outlook.com>`
- Commit message style: imperative subject, body explains the *why*

## Known Pitfalls / Past Bugs
1. **Radial orientation inverted** (fixed twice) — see orientation math above, do not change
2. **Animation before data** — `isVisible = true` must go inside `menuActions.collect` callback
3. **MotionScheme is internal** — upgrading Theme.kt to use it causes compile errors
4. **BOM version** — `2026.04.00` does not exist; latest is `2025.05.01`
5. **Menu on inactive button** — always call `onMenuVisibilityChange(false)` in move-aside timer
6. **Grid menu flash at 0,0** — guard render with `if (menuActions.isNotEmpty())`
