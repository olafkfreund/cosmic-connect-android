# Issue #96: Release Preparation (1.0.0-beta)

**Status:** Completed
**Version:** 1.0.0-beta
**Date:** January 27, 2026

## Summary
Finalized the migration of the Android application to Kotlin and Jetpack Compose, removing legacy Java code and XML layouts. The application is now ready for beta testing.

## Changes

### 1. Codebase Modernization
- **Java Removal:** Migrated all remaining Java files to Kotlin.
- **UI Migration:**
    - `RunCommandWidgetConfigActivity` migrated to Jetpack Compose.
    - Legacy `ListAdapter` and `DeviceItem` classes removed.
    - Fragments replaced by Composable screens (`SystemVolumeFragment`, `MprisNowPlayingFragment`).
- **Dependency Injection:** Hilt integration finalized for new ViewModels.

### 2. Build & Infrastructure
- **Version Update:** Bumped version to `1.0.0-beta`.
- **Gradle:** Verified build configuration and dependency compatibility.
- **Resource Cleanup:**
    - Removed `app/res/layout-w820dp/activity_main.xml`.
    - Consolidated resources.
    - Added minimal `nav_header.xml` to satisfy lingering build dependencies (technical debt to be addressed in future cleanup).

### 3. Documentation
- Added `docs/TERMS_OF_SERVICE.md`.
- Added `docs/API.md` (Architecture overview).

### 4. Verification
- **Build:** `./gradlew compileDebugKotlin` passing.
- **Lint:** `./gradlew lint` passing (with known deprecation warnings).
- **Functionality:** Widget configuration refactored to modern standards.

## Next Steps
- **Manual Testing:**
    - Test the Run Command Widget configuration flow on a device.
    - Verify tablet layout behavior (since w820dp layout was removed).
- **Release:**
    - Tag `v1.0.0-beta` in git.
    - Generate signed APK/Bundle.
    - Publish to F-Droid / Play Store tracks.
