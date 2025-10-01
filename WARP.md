# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project summary
- Kotlin Multiplatform + Compose Multiplatform chat app targeting Android, iOS (via SwiftUI wrapper), and Desktop (JVM).
- Modules: androidApp (Android launcher), iosApp (SwiftUI launcher bridging to Compose), desktopApp (Compose Desktop launcher), shared (common business logic + UI + data).
- Key libs: Compose, Ktor, SQLDelight, kotlinx.serialization, coroutines, moko-mvvm, Kamel image, Markdown renderer.

Important project rule
- Do not run the aggregate build task. Instead of `./gradlew build`, build platform-specific targets only (Android/iOS). This mirrors the rule in .trae/rules/project_rules.md.

Prerequisites
- JDK 17+
- Android Studio or IntelliJ IDEA for Android/Desktop; Xcode for iOS

Common commands
- Wrapper: always use the Gradle wrapper from the repo root: `./gradlew …`

Android
- Install debug build on a connected device/emulator
  - ./gradlew :androidApp:installDebug
- Assemble without installing
  - ./gradlew :androidApp:assembleDebug
- Run Android Lint
  - ./gradlew :androidApp:lintDebug
- Unit tests (Android JVM tests)
  - ./gradlew :androidApp:testDebugUnitTest

Desktop (Compose Desktop)
- Run the desktop app
  - ./gradlew :desktopApp:run
- Package native distributions (configured for DMG/MSI/Deb)
  - macOS: ./gradlew :desktopApp:packageDmg
  - Windows: ./gradlew :desktopApp:packageMsi
  - Linux: ./gradlew :desktopApp:packageDeb

iOS
- Open in Xcode and run (recommended)
  - open iosApp/iosApp.xcodeproj
- Notes
  - The iOS UI is a SwiftUI host that embeds Compose via Main_iosKt.MainViewController(). Build and run targets from Xcode.

Shared module tests
- Run all shared tests across targets
  - ./gradlew :shared:allTests
- JVM target (desktop) tests only
  - ./gradlew :shared:desktopTest

Running a single test
- Gradle test filtering works per target. Replace the fully-qualified class and optional method below with your test.
  - Android unit test (JVM):
    - ./gradlew :androidApp:testDebugUnitTest --tests "com.example.YourTestClass.testMethod"
  - Shared JVM target (desktop):
    - ./gradlew :shared:desktopTest --tests "com.example.YourTestClass"

High-level architecture
- Module wiring
  - androidApp depends on shared (kotlin { androidTarget() }), providing the Android entry point and Android-specific artifacts.
  - iosApp is a SwiftUI app; it hosts a UIKit UIViewController built from the Compose UI exposed by the shared module. The shared module produces a static framework named "shared" for iOS targets (iosX64/Arm64/SimulatorArm64).
  - desktopApp is a JVM target using Compose Desktop; it depends on shared and declares mainClass = MainKt with nativeDistributions for DMG/MSI/Deb.
  - shared contains common UI, domain, and data layers with per-platform source sets (commonMain, androidMain, iosMain, desktopMain). It centralizes business logic and UI composables used on all platforms.

- UI layer
  - Compose Multiplatform drives UI across platforms (compose.runtime/foundation/material/material3/materialIconsExtended). Desktop uses compose.desktop.*; iOS uses a SwiftUI wrapper to host a Compose-provided UIViewController; Android uses Activity Compose deps.

- State and architecture patterns
  - MVVM with moko-mvvm for ViewModel patterns; state management via Kotlin coroutines, Flow/StateFlow.

- Networking and serialization
  - Ktor client with content negotiation, JSON serialization (kotlinx.serialization), logging. Platform engines: Android (ktor-client-android), iOS (ktor-client-darwin), Desktop (ktor-client-cio).

- Data and persistence
  - SQLDelight configured in shared (database ChatDatabase, package data.database). Drivers per platform: android-driver, native-driver (iOS), sqlite-driver (JVM/Desktop). Coroutines extensions used for async DB operations.

- Media and markdown
  - Image loading via Kamel; Markdown rendering via multiplatform markdown renderer; image picker utilities via peekaboo.

Notes distilled from README
- Run targets:
  - Android: ./gradlew :androidApp:installDebug
  - Desktop: ./gradlew :desktopApp:run
  - iOS: open iosApp/iosApp.xcodeproj in Xcode and run
- API keys are configured at runtime in the app’s settings (per README). No keys are stored in the repo.

Tips for Warp usage here
- Prefer per-module Gradle tasks (e.g., :androidApp:…, :desktopApp:…, :shared:…) instead of aggregate build commands.
- When running tests, pick the target-specific task and use --tests filtering to run a single test quickly.
