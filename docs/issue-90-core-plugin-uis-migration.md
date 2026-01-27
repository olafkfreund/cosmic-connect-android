# Issue #90: Core Plugin UIs Migration to Compose

**Status**: ✅ COMPLETE
**Date**: 2026-01-26
**Phase**: Phase 4.3 - Screen Migrations

## Overview

Migrated the core interactive plugin user interfaces from legacy Android Views/Activities to Jetpack Compose. This covers the primary functional screens for the most commonly used plugins.

## Migrated Screens

1.  **FindMyPhoneScreen**
    -   Replaces `FindMyPhoneActivity` legacy UI.
    -   Displays a prominent notification icon and the device name.
    -   Includes a "Found it" button to stop the ringing and return.
    -   Automatically starts ringing on launch and stops on exit.

2.  **RunCommandScreen**
    -   Replaces `RunCommandActivity` legacy UI.
    -   Lists all available remote commands configured on the desktop.
    -   FAB to initiate the "Add Command" setup flow on the desktop.
    -   Handles empty states with helpful instructions on how to add commands.
    -   Maintains real-time command list updates via plugin callbacks.

3.  **PresenterScreen**
    -   Replaces `PresenterActivity` internal Compose implementation with a modernized version.
    -   Integrated with `CosmicTheme` and `CosmicTopAppBar`.
    -   Large, touch-friendly buttons for "Next" and "Previous" slide navigation.
    -   Interactive "Pointer" area that uses the device's gyroscope to control the remote mouse pointer.
    -   Dropdown menu for Fullscreen and Exit (Esc) actions.
    -   Maintains MediaSession integration for volume key navigation support.

4.  **MprisScreen**
    -   Replaces `MprisActivity` legacy ViewPager implementation.
    -   Uses Compose `HorizontalPager` with two tabs: "Now Playing" and "Devices" (System Volume).
    -   **Now Playing**: Displays album art, track info, seek bar, and playback controls (Play/Pause, Next, Prev). Handles multiple media players with a selection row.
    -   **System Volume**: Lists all remote audio sinks with individual volume sliders and mute status.

## Technical Implementation

-   **ViewModels**:
    -   `FindMyPhoneViewModel`: Manages the ringing state and plugin integration.
    -   `RunCommandViewModel`: Manages the command list and triggers remote execution.
    -   `MprisViewModel`: A unified ViewModel managing both media player states and system volume sinks, handling complex callbacks from both plugins.
-   **Architecture**:
    -   Converted existing Activities (`FindMyPhoneActivity`, `RunCommandActivity`, `PresenterActivity`, `MprisActivity`) to `ComponentActivity` and hosted the new Compose screens.
    -   Removed dependency on legacy ViewBinding and XML layouts for these screens.
-   **Design System**:
    -   Used `CosmicTheme` and standard components (`CosmicTopAppBar`, `SimpleListItem`, `Card`, `Slider`).
    -   Integrated specialized icons from `CosmicIcons`.

## Files Created

-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/FindMyPhoneScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/FindMyPhoneViewModel.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/RunCommandScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/RunCommandViewModel.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/PresenterScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/MprisScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/plugins/MprisViewModel.kt`

## Files Modified

-   `app/src/org/cosmic/cosmicconnect/Plugins/FindMyPhonePlugin/FindMyPhoneActivity.kt`
-   `app/src/org/cosmic/cosmicconnect/Plugins/RunCommandPlugin/RunCommandActivity.kt`
-   `app/src/org/cosmic/cosmicconnect/Plugins/PresenterPlugin/PresenterActivity.kt`
-   `app/src/org/cosmic/cosmicconnect/Plugins/MprisPlugin/MprisActivity.kt`
-   `app/src/org/cosmic/cosmicconnect/Helpers/VideoUrlsHelper.kt` (Fixed Hilt injection access)

## Verification

-   ✅ Build successful (`./gradlew compileDebugKotlin`).
-   ✅ Verified gyro sensor integration in `PresenterScreen`.
-   ✅ Verified tab switching and pager logic in `MprisScreen`.
-   ✅ Verified command list population in `RunCommandScreen`.
