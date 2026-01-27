# Issue #92: Complete Dialog Migration to Compose

**Status**: ✅ COMPLETE
**Date**: 2026-01-26
**Phase**: Phase 4.3 - Screen Migrations

## Overview

Migrated all remaining `DialogFragment` implementations to use Jetpack Compose for their UI. This ensures a consistent look and feel using the Material3 design system across all interactive prompts and alerts.

## Migrated Dialogs

1.  **AlertDialogFragment**
    -   Base class refactored to use `ComposeDialogFragment`.
    -   Uses `ConfirmationDialog` for standard Material3 styling.
    -   Maintains callback compatibility for legacy View-based Activities.

2.  **EditTextAlertDialogFragment**
    -   Now uses the modern `InputDialog` component.
    -   Handles single-line text input with automatic focus.

3.  **PermissionsAlertDialogFragment**
    -   Uses `PermissionDialog` to explain and request Android permissions.
    -   Standardized Material3 appearance for permission rationales.

4.  **DefaultSmsAppAlertDialogFragment**
    -   Uses `DefaultSmsAppDialog` (specialized component).
    -   Handles the complex role request logic for Android 10+ and legacy default SMS app change intents.

5.  **DeviceSettingsAlertDialogFragment**
    -   Uses `DeviceSettingsDialog` (specialized component).
    -   Provides a clean prompt before navigating to plugin-specific settings.

6.  **StartActivityAlertDialogFragment**
    -   Uses `StartActivityDialog` (specialized component).
    -   Triggers external intents (URLs, system settings) upon user confirmation.

## Technical Implementation

-   **Base Classes**:
    -   `ComposeDialogFragment`: A new base class that provides a `ComposeView` and standard `CosmicTheme` wrapper for dialog content.
    -   `AlertDialogFragment`: Refactored to inherit from `ComposeDialogFragment` and provide a default `ConfirmationDialog` implementation.
-   **Architecture**:
    -   Maintained the "Builder" pattern for all legacy dialogs to minimize changes in existing code that triggers these dialogs.
    -   State management is handled via Compose internally, with callbacks notifying the hosting Fragment/Activity.
-   **Specialized Components**:
    -   Created `SpecializedDialogs.kt` to house reusable Compose components for specific application logic (SMS roles, settings navigation).

## Files Created

-   `app/src/org/cosmic/cosmicconnect/UserInterface/ComposeDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/compose/SpecializedDialogs.kt`

## Files Modified

-   `app/src/org/cosmic/cosmicconnect/UserInterface/AlertDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/EditTextAlertDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/PermissionsAlertDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/DefaultSmsAppAlertDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/DeviceSettingsAlertDialogFragment.kt`
-   `app/src/org/cosmic/cosmicconnect/UserInterface/StartActivityAlertDialogFragment.kt`

## Verification

-   ✅ Build successful (`./gradlew compileDebugKotlin`).
-   ✅ Verified dialog rendering and Material3 theme application.
-   ✅ Verified callback functionality for confirmation and input actions.
