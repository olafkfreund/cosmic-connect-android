# Issue #95: Final UI Cleanup and Polish

**Status:** Completed
**Date:** January 27, 2026

## Summary
Executed a comprehensive cleanup of the codebase following the migration to Jetpack Compose. Removed legacy View-based components, XML layouts, and the ViewBinding dependency.

## Completed Tasks

### 1. Code Removal
- **Deleted Legacy Activities/Fragments:**
    - `BigscreenActivity` (Legacy remote input)
    - `PluginSettingsListFragment` (Redundant)
    - `BaseActivity`, `BaseFragment` (Legacy base classes)
- **Deleted Legacy Helpers:**
    - `PluginPreference` (Legacy XML preference)
    - `EntryItem`, `ListAdapter` (Legacy list adapters)
    - `ViewBinding` extensions.

### 2. XML Cleanup
- **Deleted 17+ Unused Layouts:** including `activity_bigscreen.xml`, `popup_notificationsfilter.xml`, `about_person_list_item_entry.xml`, etc.
- **Deleted Unused Resources:** `nav_header.xml` and various layout-v23/w820dp variants.
- **Retained Minimal Set:** Only widgets (`RemoteViews`) and the legacy SFTP settings dialog (`StoragePreference`) retain XML layouts.

### 3. Dependency Removal
- **Removed ViewBinding:** `buildFeatures { viewBinding = false }`.
- Refactored `PluginSettingsActivity` and `StoragePreferenceDialogFragment` to use `findViewById` instead of binding, allowing the removal of the heavy ViewBinding annotation processor/library.

### 4. Fixes & Refactoring
- **ClipboardFloatingActivity:** Migrated to Jetpack Compose (Invisible Activity).
- **DeviceDetailScreen:** Fixed bug where "Settings" for plugins (like SFTP) were inaccessible. Now correctly launches `onPluginSettings` if available.
- **RunCommandPlugin:** Decoupled `CommandEntry` from legacy adapter code.
- **MousePadPlugin:** Removed dead code related to TV remote (BigscreenActivity).

## Verification
- **Build:** `./gradlew compileDebugKotlin` passes.
- **Accessibility:** Verified `DeviceDetailScreen` uses semantic content descriptions for Cards, ensuring TalkBack users get a summary of the device/plugin state.
- **APK Size:** Expected reduction due to removal of generated Binding classes and resources.

## Remaining Technical Debt
- **SftpSettingsFragment:** Remains as a legacy `PreferenceFragmentCompat`. It uses standard Android Preferences (XML). Migrating this to Compose would require a full rewrite of the storage picker logic. It is the only non-Compose UI screen left (besides widgets).
- **RunCommandWidget:** Home screen widgets must use XML `RemoteViews` by design.

The application is now fully modernized with a significantly lighter codebase.
