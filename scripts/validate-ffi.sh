#!/usr/bin/env bash

# COSMIC Connect FFI Validation Script
#
# This script validates the Rust FFI bindings without requiring a full Android APK build.
# It checks native libraries, bindings, and FFI infrastructure.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASS=0
FAIL=0
WARN=0

print_header() {
    echo -e "${BLUE}======================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}======================================================================${NC}"
    echo
}

print_test() {
    echo -e "${BLUE}Test $1: $2${NC}"
    echo "----------------------------------------------------------------------"
}

print_pass() {
    echo -e "${GREEN}✅ PASS${NC}: $1"
    ((PASS++))
}

print_fail() {
    echo -e "${RED}❌ FAIL${NC}: $1"
    ((FAIL++))
}

print_warn() {
    echo -e "${YELLOW}⚠️  WARN${NC}: $1"
    ((WARN++))
}

print_info() {
    echo -e "   $1"
}

cd "$(dirname "$0")/.."

print_header "COSMIC Connect FFI Validation - Standalone Test"

# Test 1: Check native libraries exist
print_test "1" "Native Library Existence"

LIBS_FOUND=0
LIBS_TOTAL=4

for arch in arm64-v8a armeabi-v7a x86_64 x86; do
    lib_path="build/rustJniLibs/android/$arch/libcosmic_connect_core.so"
    if [ -f "$lib_path" ]; then
        size=$(du -h "$lib_path" | cut -f1)
        print_pass "Found $arch library ($size)"
        ((LIBS_FOUND++))
    else
        print_fail "Missing $arch library"
        print_info "Expected: $lib_path"
    fi
done

echo
print_info "Native libraries: $LIBS_FOUND/$LIBS_TOTAL architectures"
echo

# Test 2: Check UniFFI generated bindings
print_test "2" "UniFFI Generated Bindings"

bindings_file="src/uniffi/cosmic_connect_core/cosmic_connect_core.kt"
if [ -f "$bindings_file" ]; then
    size=$(du -h "$bindings_file" | cut -f1)
    lines=$(wc -l < "$bindings_file")
    print_pass "UniFFI bindings exist ($size, $lines lines)"

    # Analyze bindings content
    if grep -q "class NetworkPacket" "$bindings_file"; then
        print_info "Contains NetworkPacket class"
    fi
    if grep -q "interface Plugin" "$bindings_file"; then
        print_info "Contains Plugin interface"
    fi
    if grep -q "fun initialize" "$bindings_file"; then
        print_info "Contains initialize() function"
    fi
else
    print_fail "UniFFI bindings file not found"
    print_info "Expected: $bindings_file"
fi

echo

# Test 3: Check FFI wrapper
print_test "3" "FFI Wrapper Layer"

wrapper_file="src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt"
if [ -f "$wrapper_file" ]; then
    size=$(du -h "$wrapper_file" | cut -f1)
    lines=$(wc -l < "$wrapper_file")
    print_pass "FFI wrapper exists ($size, $lines lines)"

    # Check wrapper features
    if grep -q "System.loadLibrary" "$wrapper_file"; then
        print_info "✓ Has library loading"
    fi
    if grep -q "fun initialize" "$wrapper_file"; then
        print_info "✓ Has initialize() method"
    fi
    if grep -q "val version" "$wrapper_file"; then
        print_info "✓ Has version property"
    fi
else
    print_fail "FFI wrapper file not found"
    print_info "Expected: $wrapper_file"
fi

echo

# Test 4: Check test suite
print_test "4" "FFI Test Suite"

test_file="tests/org/cosmic/cosmicconnect/FFIValidationTest.kt"
if [ -f "$test_file" ]; then
    size=$(du -h "$test_file" | cut -f1)
    lines=$(wc -l < "$test_file")
    test_count=$(grep -c "@Test" "$test_file" || echo "0")
    print_pass "Test suite exists ($size, $lines lines, $test_count tests)"

    # List test methods
    print_info "Tests:"
    grep "@Test" -A 1 "$test_file" | grep "fun test" | sed 's/.*fun /  - /' | sed 's/().*/()/'
else
    print_warn "Test suite not found"
    print_info "Expected: $test_file"
fi

echo

# Test 5: Check native library symbols (if nm available)
print_test "5" "Native Library Symbols"

if command -v nm &> /dev/null; then
    lib_path="build/rustJniLibs/android/x86_64/libcosmic_connect_core.so"
    if [ -f "$lib_path" ]; then
        symbol_count=$(nm -D "$lib_path" 2>/dev/null | grep -c " T " || echo "0")
        print_pass "Found $symbol_count exported symbols"

        # Check for key symbols
        if nm -D "$lib_path" 2>/dev/null | grep -q "uniffi"; then
            print_info "✓ Contains UniFFI symbols"
        fi
        if nm -D "$lib_path" 2>/dev/null | grep -q -i "packet"; then
            print_info "✓ Contains packet-related symbols"
        fi
        if nm -D "$lib_path" 2>/dev/null | grep -q -i "plugin"; then
            print_info "✓ Contains plugin-related symbols"
        fi
    else
        print_warn "Library not found for symbol analysis"
    fi
else
    print_warn "nm command not available, skipping symbol analysis"
fi

echo

# Test 6: Check Rust source
print_test "6" "Rust Core Library"

rust_lib="../cosmic-connect-core/src/lib.rs"
if [ -f "$rust_lib" ]; then
    print_pass "Rust core library found"

    # Check for FFI module
    if grep -q "mod ffi" "$rust_lib"; then
        print_info "✓ Has FFI module"
    fi
    if grep -q "uniffi" "$rust_lib"; then
        print_info "✓ Uses UniFFI"
    fi
else
    print_warn "Rust core library not found at $rust_lib"
fi

echo

# Summary
print_header "Validation Summary"

echo -e "${GREEN}✅ Passed:${NC} $PASS"
echo -e "${RED}❌ Failed:${NC} $FAIL"
echo -e "${YELLOW}⚠️  Warnings:${NC} $WARN"
echo

# Overall assessment
if [ "$FAIL" -eq 0 ] && [ "$LIBS_FOUND" -eq 4 ]; then
    print_header "✅ FFI Infrastructure is Ready!"
    echo
    echo "The Rust FFI bindings are properly built and configured."
    echo "Native libraries are compiled for all 4 Android architectures."
    echo "UniFFI bindings and FFI wrapper are in place."
    echo
    echo "Next Steps:"
    echo "1. Complete Android SDK configuration to run full tests"
    echo "2. Run: ./gradlew test --tests FFIValidationTest"
    echo "3. Measure FFI performance benchmarks"
    echo
elif [ "$LIBS_FOUND" -gt 0 ]; then
    print_header "⚠️  FFI Infrastructure Partially Ready"
    echo
    echo "Some native libraries are missing, but FFI infrastructure exists."
    echo "Run './gradlew cargoBuild' to build all native libraries."
    echo
else
    print_header "❌ FFI Infrastructure Not Ready"
    echo
    echo "Native libraries are not built."
    echo "Run './gradlew cargoBuild' to build native libraries first."
    echo
    exit 1
fi

print_header "Validation Complete"

exit 0
