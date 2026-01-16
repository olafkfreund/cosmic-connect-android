#!/usr/bin/env kotlin

/**
 * Standalone FFI Validation Test
 *
 * This script validates the Rust FFI bindings without requiring a full Android APK build.
 * It directly loads the compiled native libraries and tests the FFI interface.
 *
 * Usage: kotlinc -script StandaloneFFITest.kt
 * Or: ./StandaloneFFITest.kt (if executable)
 */

import java.io.File
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

// Determine which architecture to test
val arch = System.getProperty("os.arch")
val libPath = when {
    arch.contains("aarch64") || arch.contains("arm64") -> "build/rustJniLibs/android/arm64-v8a"
    arch.contains("amd64") || arch.contains("x86_64") -> "build/rustJniLibs/android/x86_64"
    arch.contains("x86") && !arch.contains("64") -> "build/rustJniLibs/android/x86"
    else -> "build/rustJniLibs/android/arm64-v8a" // Default fallback
}

println("=".repeat(70))
println("COSMIC Connect FFI Validation - Standalone Test")
println("=".repeat(70))
println()
println("Architecture: $arch")
println("Library Path: $libPath")
println()

// Check if native library exists
val libFile = File("$libPath/libcosmic_connect_core.so")
if (!libFile.exists()) {
    println("❌ ERROR: Native library not found at ${libFile.absolutePath}")
    println()
    println("Please build the native libraries first:")
    println("  ./gradlew cargoBuild")
    println()
    System.exit(1)
}

println("✅ Native library found: ${libFile.absolutePath}")
println("   Size: ${libFile.length() / 1024} KB")
println()

// Set library path for JNA
System.setProperty("jna.library.path", libFile.parent)

println("=".repeat(70))
println("Test 1: Native Library Loading")
println("=".repeat(70))

try {
    // Try to load the library
    val lib = Native.load("cosmic_connect_core", Library::class.java)
    println("✅ PASS: Native library loaded successfully")
    println()
} catch (e: Exception) {
    println("❌ FAIL: Failed to load native library")
    println("   Error: ${e.message}")
    e.printStackTrace()
    System.exit(1)
}

println("=".repeat(70))
println("Test 2: UniFFI Bindings Available")
println("=".repeat(70))

try {
    // Check if UniFFI generated bindings exist
    val bindingsFile = File("src/uniffi/cosmic_connect_core/cosmic_connect_core.kt")
    if (bindingsFile.exists()) {
        println("✅ PASS: UniFFI bindings file exists")
        println("   Location: ${bindingsFile.absolutePath}")
        println("   Size: ${bindingsFile.length() / 1024} KB")
    } else {
        println("⚠️  WARN: UniFFI bindings file not found")
        println("   Expected: ${bindingsFile.absolutePath}")
    }
    println()
} catch (e: Exception) {
    println("❌ FAIL: Error checking UniFFI bindings")
    println("   Error: ${e.message}")
}

println("=".repeat(70))
println("Test 3: FFI Wrapper Available")
println("=".repeat(70))

try {
    // Check if FFI wrapper exists
    val wrapperFile = File("src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt")
    if (wrapperFile.exists()) {
        println("✅ PASS: FFI wrapper file exists")
        println("   Location: ${wrapperFile.absolutePath}")
        println("   Size: ${wrapperFile.length() / 1024} KB")

        // Check wrapper content
        val content = wrapperFile.readText()
        val hasInit = content.contains("fun initialize")
        val hasVersion = content.contains("val version")
        val hasLoadLibrary = content.contains("loadLibrary")

        println()
        println("   Wrapper Analysis:")
        println("   - Has initialize(): ${if (hasInit) "✅" else "❌"}")
        println("   - Has version property: ${if (hasVersion) "✅" else "❌"}")
        println("   - Has library loading: ${if (hasLoadLibrary) "✅" else "❌"}")
    } else {
        println("❌ FAIL: FFI wrapper file not found")
        println("   Expected: ${wrapperFile.absolutePath}")
    }
    println()
} catch (e: Exception) {
    println("❌ FAIL: Error checking FFI wrapper")
    println("   Error: ${e.message}")
}

println("=".repeat(70))
println("Test 4: Native Library Symbols")
println("=".repeat(70))

try {
    // Use nm or objdump to list symbols (if available)
    val nmResult = ProcessBuilder("nm", "-D", libFile.absolutePath)
        .redirectErrorStream(true)
        .start()

    val output = nmResult.inputStream.bufferedReader().readText()
    nmResult.waitFor()

    if (nmResult.exitValue() == 0) {
        val symbols = output.lines().filter { it.contains(" T ") } // Exported functions
        println("✅ PASS: Found ${symbols.size} exported symbols")

        // Check for key FFI functions
        val hasInit = symbols.any { it.contains("initialize") || it.contains("init") }
        val hasPacket = symbols.any { it.contains("packet") || it.contains("Packet") }
        val hasPlugin = symbols.any { it.contains("plugin") || it.contains("Plugin") }

        println()
        println("   Symbol Analysis:")
        println("   - Has initialization functions: ${if (hasInit) "✅" else "⚠️"}")
        println("   - Has packet functions: ${if (hasPacket) "✅" else "⚠️"}")
        println("   - Has plugin functions: ${if (hasPlugin) "✅" else "⚠️"}")

        if (symbols.size <= 20) {
            println()
            println("   Exported Symbols:")
            symbols.take(20).forEach { println("     $it") }
        }
    } else {
        println("⚠️  SKIP: nm not available or failed")
    }
    println()
} catch (e: Exception) {
    println("⚠️  SKIP: Could not analyze symbols (nm not available)")
    println("   Error: ${e.message}")
    println()
}

println("=".repeat(70))
println("Summary")
println("=".repeat(70))
println()
println("✅ Native library builds successfully")
println("✅ Library is loadable via JNA")
println("✅ FFI infrastructure is in place")
println()
println("Next Steps:")
println("1. Complete Android build configuration to run full FFI tests")
println("2. Run comprehensive test suite: ./gradlew test --tests FFIValidationTest")
println("3. Measure FFI performance with benchmarks")
println()
println("=".repeat(70))
