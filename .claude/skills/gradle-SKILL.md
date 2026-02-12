# Gradle Build System Skill for Android

## Overview
This skill provides comprehensive guidance for Gradle build configuration, optimization, and troubleshooting in Android projects, particularly for COSMIC Connect Android app modernization.

## Modern Gradle Project Structure

### Project-Level build.gradle.kts
```kotlin
// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48.1")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

### App-Level build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "org.cosmicext.connect"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "org.cosmicext.connect"
        minSdk = 24
        targetSdk = 34
        versionCode = 15070
        versionName = "1.24.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables.useSupportLibrary = true
        
        // Protocol version
        buildConfigField("int", "PROTOCOL_VERSION", "7")
        buildConfigField("int", "MIN_PROTOCOL_VERSION", "7")
        buildConfigField("int", "MAX_PROTOCOL_VERSION", "8")
        
        // Network configuration
        buildConfigField("int", "MIN_PORT", "1714")
        buildConfigField("int", "MAX_PORT", "1764")
        buildConfigField("String", "MULTICAST_GROUP", "\"224.0.0.251\"")
        buildConfigField("int", "DISCOVERY_PORT", "1716")
    }
    
    signingConfigs {
        create("release") {
            // Configure signing for release builds
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "cosmicconnect"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    
    flavorDimensions += "version"
    productFlavors {
        create("fdroid") {
            dimension = "version"
            // F-Droid specific configuration
            buildConfigField("boolean", "INCLUDE_GOOGLE_SERVICES", "false")
        }
        
        create("playstore") {
            dimension = "version"
            buildConfigField("boolean", "INCLUDE_GOOGLE_SERVICES", "true")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }
    
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
    
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = true
        abortOnError = false
        warningsAsErrors = false
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON
    implementation("org.json:json:20231013")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Cryptography
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48.1")
}

// Kapt configuration
kapt {
    correctErrorTypes = true
    useBuildCache = true
}
```

## Gradle Wrapper Configuration

### gradle/wrapper/gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "COSMIC Connect"
include(":app")

// Enable Gradle configuration cache
enableFeaturePreview("CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

## gradle.properties
```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kotlin.incremental.usePreciseJavaTracking=true

# Android
android.useAndroidX=true
android.enableJetifier=false
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true

# Build optimization
android.enableR8.fullMode=true
android.nonFinalResIds=true
```

## ProGuard/R8 Configuration

### proguard-rules.pro
```pro
# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializables
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# COSMIC Connect specific
-keep class org.cosmicext.connect.** { *; }
-keep interface org.cosmicext.connect.** { *; }
-keepclassmembers class org.cosmicext.connect.** {
    public <methods>;
    public <fields>;
}

# Keep plugin classes
-keep class * extends org.cosmicext.connect.Plugins.Plugin {
    public <init>(...);
}

# Keep packet classes
-keep class org.cosmicext.connect.NetworkPacket { *; }
-keep class org.cosmicext.connect.NetworkPacket$* { *; }

# Hilt
-dontwarn com.google.errorprone.annotations.*
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

## Custom Gradle Tasks

### Version Management
```kotlin
// In app/build.gradle.kts

tasks.register("printVersion") {
    doLast {
        val versionCode = android.defaultConfig.versionCode
        val versionName = android.defaultConfig.versionName
        println("Version: $versionName ($versionCode)")
    }
}

tasks.register("incrementVersionCode") {
    doLast {
        val propertiesFile = file("version.properties")
        val properties = Properties()
        
        if (propertiesFile.exists()) {
            properties.load(FileInputStream(propertiesFile))
        }
        
        val currentCode = properties.getProperty("versionCode", "1").toInt()
        val newCode = currentCode + 1
        
        properties.setProperty("versionCode", newCode.toString())
        properties.store(FileOutputStream(propertiesFile), null)
        
        println("Version code incremented to $newCode")
    }
}
```

### APK Signing Verification
```kotlin
tasks.register("verifyApkSigning") {
    dependsOn("assembleRelease")
    
    doLast {
        val apkPath = "app/build/outputs/apk/release/app-release.apk"
        
        exec {
            commandLine("apksigner", "verify", "--verbose", apkPath)
        }
    }
}
```

### Generate Build Info
```kotlin
tasks.register("generateBuildInfo") {
    val outputDir = file("$buildDir/generated/buildinfo")
    
    inputs.property("versionCode", android.defaultConfig.versionCode)
    inputs.property("versionName", android.defaultConfig.versionName)
    outputs.dir(outputDir)
    
    doLast {
        outputDir.mkdirs()
        
        val buildInfoFile = file("$outputDir/BuildInfo.kt")
        buildInfoFile.writeText("""
            package org.cosmicext.connect
            
            object BuildInfo {
                const val VERSION_CODE = ${android.defaultConfig.versionCode}
                const val VERSION_NAME = "${android.defaultConfig.versionName}"
                const val BUILD_TIME = "${System.currentTimeMillis()}"
                const val GIT_COMMIT = "${getGitCommit()}"
            }
        """.trimIndent())
    }
}

fun getGitCommit(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// Add generated source to source sets
android.sourceSets.getByName("main") {
    java.srcDir("$buildDir/generated/buildinfo")
}

tasks.named("preBuild").configure {
    dependsOn("generateBuildInfo")
}
```

## Dependency Management

### Version Catalog (gradle/libs.versions.toml)
```toml
[versions]
kotlin = "1.9.21"
agp = "8.2.0"
compose = "1.5.6"
coroutines = "1.7.3"
hilt = "2.48.1"
lifecycle = "2.7.0"
room = "2.6.1"

[libraries]
# Android
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.12.0" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version = "1.6.1" }
androidx-activity-ktx = { module = "androidx.activity:activity-ktx", version = "1.8.2" }

# Lifecycle
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version = "2023.10.01" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

[bundles]
lifecycle = ["androidx-lifecycle-runtime", "androidx-lifecycle-viewmodel"]
coroutines = ["kotlinx-coroutines-core", "kotlinx-coroutines-android"]
room = ["room-runtime", "room-ktx"]
```

### Using Version Catalog
```kotlin
// In app/build.gradle.kts
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.coroutines)
    
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

## Build Optimization

### Gradle Daemon Tuning
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.daemon.idletimeout=3600000
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError \
                   -XX:MaxMetaspaceSize=1024m \
                   -Dfile.encoding=UTF-8 \
                   -Dkotlin.daemon.jvm.options="-Xmx2048m"
```

### Build Cache Configuration
```kotlin
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}
```

### Configuration Cache
```bash
# Enable configuration cache
./gradlew build --configuration-cache

# Store configuration cache
./gradlew build --configuration-cache-problems=warn
```

## Troubleshooting Common Issues

### Dependency Resolution Conflicts
```kotlin
// Force specific version
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.21")
    }
}

// Exclude transitive dependency
implementation("some.library:module:1.0") {
    exclude(group = "com.google.code.gson", module = "gson")
}

// Substitute module
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("old.group:old-module"))
            .using(module("new.group:new-module:1.0"))
    }
}
```

### Memory Issues
```bash
# Increase Gradle heap
export GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=1024m"

# Disable parallel execution if needed
./gradlew build --no-parallel

# Clear Gradle cache
./gradlew --stop
rm -rf ~/.gradle/caches/
```

### Build Speed Analysis
```bash
# Profile build
./gradlew assembleDebug --profile --scan

# Analyze task dependencies
./gradlew app:dependencies --configuration releaseRuntimeClasspath

# Check for outdated dependencies
./gradlew dependencyUpdates
```

## CI/CD Integration

### GitHub Actions
```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build --stacktrace
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Generate APK
      run: ./gradlew assembleRelease
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-release
        path: app/build/outputs/apk/release/app-release.apk
```

## Gradle Kotlin DSL Best Practices

1. **Use Type-Safe Accessors**: Enable `TYPESAFE_PROJECT_ACCESSORS`
2. **Avoid String Literals**: Use constants and version catalogs
3. **Delegate Configuration**: Extract common configuration to convention plugins
4. **Use Extension Functions**: Create reusable configuration blocks
5. **Proper Task Configuration**: Use `register` instead of `create`
6. **Lazy Configuration**: Use providers and lazy properties

## Resources

- [Gradle User Manual](https://docs.gradle.org/)
- [Android Gradle Plugin Documentation](https://developer.android.com/build)
- [Gradle Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Android Build Optimization](https://developer.android.com/studio/build/optimize-your-build)
