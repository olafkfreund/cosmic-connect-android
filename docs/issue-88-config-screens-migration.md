# Issue #88: Advanced Configuration Screens Migration to Compose

**Status**: ✅ COMPLETE
**Date**: 2026-01-26
**Phase**: Phase 4.3 - Screen Migrations

## Overview

Migrated the "Custom Devices" and "Trusted Networks" configuration screens from legacy Android Views/Activities to Jetpack Compose. These screens handle manual device discovery and network security settings.

## Migrated Screens

1.  **CustomDevicesScreen**
    -   Replaces `CustomDevicesActivity` legacy UI.
    -   Displays a list of manually added device hostnames or IP addresses.
    -   Includes reachability status (Reachable/Unreachable) using the `DeviceHost` ping mechanism.
    -   Floating Action Button (FAB) to add new devices.
    -   Clicking an item allows editing the hostname/IP.
    -   Delete button for each item.
    -   Uses `InputDialog` for adding and editing entries.

2.  **TrustedNetworksScreen**
    -   Replaces `TrustedNetworksActivity` legacy UI.
    -   "Allow all networks" toggle to enable/disable network verification.
    -   List of trusted WiFi SSIDs.
    -   Ability to remove trusted networks.
    -   One-tap button to add the current WiFi network to the trusted list.
    -   Handles location permission request required for SSID discovery on Android.

## Technical Implementation

-   **ViewModels**:
    -   `CustomDevicesViewModel`: Manages the list of `DeviceHost` objects, handles reachability checks, and persists changes to `PreferenceDataStore`.
    -   `TrustedNetworksViewModel`: Interfaces with `TrustedNetworkHelper` to manage the trusted SSID list and the global "allow all" setting.
-   **Navigation**:
    -   Existing `CustomDevicesActivity` and `TrustedNetworksActivity` were converted to `ComponentActivity` and now host the Compose screens using `setContent`.
    -   Maintained the static helper methods in `CustomDevicesActivity` companion object to avoid breaking external references in the codebase (e.g., in `LanLinkProvider`).
-   **Design System**: Used `CosmicTheme`, `CosmicTopAppBar`, `SimpleListItem`, and `SectionHeader`.

## Files Created

-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/config/CustomDevicesScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/config/CustomDevicesViewModel.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/config/TrustedNetworksScreen.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/screens/config/TrustedNetworksViewModel.kt`

## Files Modified

-   `app/src/org/cosmic/cosmicconnect/UserInterface/CustomDevicesActivity.kt` (Migrated to setContent, retained companion object)
-   `app/src/org/cosmic/cosmicconnect/UserInterface/TrustedNetworksActivity.kt` (Migrated to setContent)

## Verification

-   ✅ Build successful (`./gradlew compileDebugKotlin`).
-   ✅ Verified data persistence through `PreferenceDataStore`.
-   ✅ Verified reachability logic integration.
