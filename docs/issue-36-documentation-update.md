# Issue #36: Update User Documentation

**Status:** Completed
**Date:** January 27, 2026

## Summary
Updated all key documentation files to reflect the v1.0.0-beta release and the new Jetpack Compose UI.

## Completed Tasks

### 1. User Guide (`docs/USER_GUIDE.md`)
- Rewrote the entire guide to match the new Compose navigation flow.
- Updated "Pairing Your Devices" to describe the new card-based UI.
- Updated "Using Features" to reflect new plugin interactions (Plugin Cards).
- Clarified permissions and privacy sections.

### 2. FAQ (`docs/FAQ.md`)
- Updated "What's the difference between COSMIC Connect and KDE Connect?" to highlight the new Kotlin/Compose UI and Rust Core architecture.
- Refreshed "Last Updated" date.

### 3. Getting Started (`docs/guides/GETTING_STARTED.md`)
- Replaced the outdated "Phase 0" planning document with a proper **Developer Setup Guide**.
- Added clear instructions for NixOS and Manual build setups.
- Archived the old roadmap as `docs/guides/PROJECT_HISTORY.md`.

### 4. Screenshot Checklist (`docs/SCREENSHOT_CHECKLIST.md`)
- Created a checklist of 5 required screenshots for the new UI.
- Added placeholders instructions.

## Verification
- Documentation accurately describes the app's current behavior (1.0.0-beta).
- Links between documents are working.
- Technical instructions in `GETTING_STARTED.md` align with `build.gradle.kts`.

## Next Steps
- Capture actual screenshots on a device using the `SCREENSHOT_CHECKLIST.md`.
- Replace placeholder text/links with actual images once available.
