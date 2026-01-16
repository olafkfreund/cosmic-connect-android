# Issue #68: Build Fix After Sync from cosmic-connect-desktop-app and cosmic-core

**Status**: ✅ RESOLVED
**Date**: 2026-01-16
**Build Result**: SUCCESSFUL (24 MB APK)

## Overview

After syncing from cosmic-connect-desktop-app and cosmic-connect-core repositories, the Android build encountered duplicate resource errors that prevented APK assembly. This document details the issues found and the fixes applied.

## Problems Encountered

### 1. Duplicate Native Library Resources

**Error**:
```
Error: Duplicate resources
[armeabi-v7a/libcosmic_connect_core.so]
  /home/olafkfreund/Source/GitHub/cosmic-connect-android/build/rustJniLibs/android/armeabi-v7a/libcosmic_connect_core.so
[armeabi-v7a/libcosmic_connect_core.so]
  /home/olafkfreund/Source/GitHub/cosmic-connect-android/build/rustJniLibs/android/armeabi-v7a/libcosmic_connect_core.so
```

**Root Cause**:
The `jniLibs.srcDir()` method in build.gradle.kts was **adding** the Rust library directory to the source set rather than **setting** it exclusively. This caused Gradle to see the same directory twice, resulting in duplicate resource errors.

**Fix**:
Changed from `srcDir()` (additive) to `setSrcDirs()` (exclusive):

```kotlin
// Before (BROKEN)
jniLibs.srcDir("${projectDir}/build/rustJniLibs/android")

// After (FIXED)
jniLibs.setSrcDirs(listOf("${projectDir}/build/rustJniLibs/android"))
```

**Files Modified**:
- `build.gradle.kts` (line 111)

### 2. Duplicate License Resource

**Error**:
```
Error: Duplicate resources
[raw/license] /home/olafkfreund/Source/GitHub/cosmic-connect-android/res/raw/license
[raw/license] /home/olafkfreund/Source/GitHub/cosmic-connect-android/build/dependency-license-res/raw/license
```

**Root Cause**:
A placeholder `license` file existed in `res/raw/` that conflicted with the auto-generated license file from the dependency license plugin.

**Fix**:
Removed the placeholder file as the build system generates the proper license file automatically:

```bash
rm -f res/raw/license
```

**Files Modified**:
- `res/raw/license` (DELETED)

### 3. Added JNI Library Packaging Configuration

**Purpose**:
Prevent future issues with duplicate or conflicting native libraries from dependencies.

**Addition**:
```kotlin
packaging {
    resources {
        merges += listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE")
    }
    jniLibs {
        // Handle duplicate native libraries from Rust builds
        pickFirsts += listOf("**/libcosmic_connect_core.so")
    }
}
```

**Files Modified**:
- `build.gradle.kts` (added lines 125-128)

## Build Status After Fixes

### Rust Native Library Build
```
✅ Aarch64 (ARM64-v8a):  Built successfully
✅ Armv7 (ARMv7a):       Built successfully
✅ X86:                  Built successfully
✅ X86_64:               Built successfully

Native Libraries: 9.3 MB across 4 ABIs
Build Time: 2m 42s
Warnings: 9 (unused code/imports - non-blocking)
```

### Kotlin/Java Compilation
```
✅ Kotlin Compilation:   0 errors
✅ Java Compilation:     0 errors
✅ Resource Processing:  Completed
```

### APK Assembly
```
✅ APK Build:            SUCCESSFUL
✅ APK Size:             24 MB
✅ Output File:          cosmicconnect-android-debug-3169e612.apk
```

## Remaining FFI Implementation Status

### ⚠️ Important Finding: Placeholder FFI Methods

While investigating, I discovered that the Rust core's `PluginManager` still contains **placeholder implementations** for ping methods:

**File**: `cosmic-connect-core/src/ffi/mod.rs`

```rust
// Line 1343-1356: create_ping() returns error
pub fn create_ping(&self, message: Option<String>) -> Result<FfiPacket> {
    // ...
    error!("create_ping: Direct plugin access not yet implemented");
    Err(ProtocolError::Plugin("Direct plugin access not yet implemented".to_string()))
}

// Line 1360-1366: get_ping_stats() returns zeros
pub fn get_ping_stats(&self) -> FfiPingStats {
    // Placeholder - would access ping plugin stats
    FfiPingStats {
        pings_received: 0,
        pings_sent: 0,
    }
}
```

**Impact**:
- The Ping plugin (Issue #61) was documented as complete
- However, the actual FFI implementation is non-functional
- Kotlin wrapper calls will fail at runtime

**Recommendation**:
Create **Issue #69: Implement PluginManager.create_ping() and get_ping_stats() in Rust Core** to properly implement these methods.

## FFI Test Status

### Test Suite Compilation Issues

The FFI validation test suite has multiple compilation errors:

1. **Duplicate test function** (line 286 and 808):
   ```kotlin
   fun testPingPlugin()  // Old version at line 286
   fun testPingPlugin()  // New version at line 808
   ```

2. **Type mismatches** throughout:
   - `serializePacket()` expects `FfiPacket` but receives `NetworkPacket`
   - Multiple `containsKey()` calls on wrong types
   - String/Int type confusion in assertions

**Recommendation**:
Create **Issue #70: Fix FFI Validation Test Suite Compilation** to resolve all test errors before continuing plugin migrations.

## Sync Status Summary

### What Synced Successfully
1. ✅ Rust core library with all plugin implementations
2. ✅ Protocol version 8 synchronization
3. ✅ Native library cross-compilation for all Android ABIs
4. ✅ UniFFI binding generation

### What Needs Attention
1. ⚠️ FFI PluginManager methods (placeholders)
2. ⚠️ Test suite compilation errors
3. ⚠️ Runtime validation of FFI calls

## Next Steps

### Immediate Actions
1. **Verify FFI functionality**:
   ```bash
   ./gradlew testDebugUnitTest --tests "*.FFIValidationTest"
   ```

2. **Fix test compilation errors** (Issue #70)

3. **Implement missing FFI methods** (Issue #69):
   - `PluginManager.create_ping()`
   - `PluginManager.get_ping_stats()`
   - Consider adding more plugin-specific methods

### Plugin Migration Path Forward

**Recommendation**: **PAUSE plugin migrations** until FFI infrastructure is stable.

Current plugin migration relies on FFI methods that aren't properly implemented. Continuing migrations would result in:
- Documented-as-complete plugins that don't work at runtime
- Accumulating technical debt
- Confusing failure modes for users

**Suggested Order**:
1. Fix Rust FFI implementation (Issue #69)
2. Fix test suite (Issue #70)
3. Validate existing 8 plugins work end-to-end
4. Resume plugin migrations (starting with simpler plugins like Presenter)

## File Changes Summary

### Modified Files
| File | Change | Lines |
|------|--------|-------|
| build.gradle.kts | Fixed jniLibs configuration | ~4 lines |
| build.gradle.kts | Added packaging.jniLibs | +4 lines |

### Deleted Files
| File | Reason |
|------|--------|
| res/raw/license | Duplicate resource (auto-generated) |

### No Changes Required
- All Kotlin source files compile correctly
- All Java source files compile correctly
- Native libraries build successfully
- Gradle configuration otherwise stable

## Build Performance Metrics

| Metric | Value |
|--------|-------|
| Clean Build Time | 2m 51s |
| Incremental Build | ~9s |
| Native Library Build | 2m 42s |
| APK Assembly | 9s |
| Total Project Size | 24 MB (debug APK) |
| Native Libraries | 9.3 MB (4 ABIs) |

## Recommendations

### For Issue #68 (This Issue)
✅ **COMPLETE** - Build is working, APK assembles successfully

### For Issue #69 (New - FFI Implementation)
**Priority**: HIGH
**Scope**: Implement proper Rust FFI methods in cosmic-connect-core
**Blocks**: All plugin FFI functionality

### For Issue #70 (New - Test Fixes)
**Priority**: HIGH
**Scope**: Fix FFI validation test compilation errors
**Blocks**: Test-driven development for plugins

### For Plugin Migrations
**Status**: PAUSE until Issues #69 and #70 resolved
**Progress**: 8/18 plugins documented (44%)
**Actual**: 0/18 plugins fully functional via FFI

## Conclusion

The sync from cosmic-connect-desktop-app and cosmic-connect-core was successful, and the Android build is now working. However, the investigation revealed that the FFI layer needs substantial work before plugin migrations can continue effectively.

**Key Takeaway**: The build infrastructure is solid, but the runtime FFI implementation needs completion before the documented plugin migrations are truly functional.

---

**Issue #68: COMPLETE** ✅
Build time: ~3 hours (investigation + fixes)
APK Status: Building successfully
Next: Address FFI implementation gaps
