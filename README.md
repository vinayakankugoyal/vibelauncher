# Vibe Launcher

Vibe Launcher is a minimalist, productivity-focused Android launcher designed to help you stay focused and reduce digital distractions. Built with Kotlin and Jetpack Compose, it offers a clean interface with powerful features like app delay timers and gesture shortcuts.

## Features

*   **Minimalist Design:** clean, transparent interface with grayscale app icons to reduce visual clutter and dopamine triggers.
*   **Smart Search:** Quickly find and launch apps.
*   **Auto Launch:** (Optional) Automatically launches the app when your search narrows down to a single result.
*   **Gesture Shortcuts:** Customize swipes (Left, Right, Up, Down) and Long Press actions to instantly launch your favorite apps.
*   **Digital Wellbeing / Focus Mode:**
    *   **Delayed Apps:** Set a mandatory delay timer (e.g., 60 seconds) for specific "distracting" apps. A countdown screen will appear before the app opens, giving you a moment to reconsider.
    *   Adjustable delay duration.
*   **Work Profile Support:** Seamlessly handles and launches apps from your work profile.

## Getting Started

### Prerequisites

*   Android device running Android 7.0 (Nougat) or higher (API level 24+).

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/yourusername/VibeLauncher.git
    ```
2.  Open the project in Android Studio.
3.  Build and install the app on your device.
4.  Once installed, press your device's "Home" button and select **Vibe Launcher** as your default home app.

## Usage

### Home Screen
*   **Search:** Tap the search bar (or the top area) to start typing.
*   **Launch:** Tap an app from the search results to open it.
*   **Settings:** Tap the gear icon (⚙) at the bottom right to access configuration options.

### Gestures
*   **Swipe Left/Right/Up/Down:** Launch the assigned shortcut app.
*   **Long Press:** Launch the assigned shortcut app.

### Configuration
Go to **Settings** to customize your experience:
*   **Auto Launch:** Toggle auto-launch on/off.
*   **Delayed Apps:** 
    *   Add apps to the "Delayed" list.
    *   Set the delay duration (e.g., 5 seconds to 2 minutes).
*   **Swipe Settings:** Assign specific apps to Left, Right, Up, Down swipes.
*   **Long Press Settings:** Assign an app to the long-press gesture.
*   **Default Launcher:** Shortcut to system settings to make Vibe Launcher the default.

## Privacy

Vibe Launcher respects your privacy.
*   **No Data Collection:** We do not collect, store, or share your personal data.
*   **No Internet Access:** The app functions entirely offline (unless you count the privacy policy link).
*   See `PRIVACY.md` for the full policy.

## License

MIT License
