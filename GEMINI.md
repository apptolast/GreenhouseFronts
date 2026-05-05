# GEMINI.md

This file provides context and instructions for the Gemini AI agent when working with this
repository.

## Project Overview

**GreenhouseFronts** is a **Kotlin Multiplatform (KMP)** application designed as a dashboard for
monitoring and controlling greenhouse environments. It supports **Android, iOS, Desktop (JVM), and
Web (Wasm/JS)** from a single codebase.

The application allows users to:

- View real-time sensor data (temperature, humidity, etc.).
- Adjust setpoints and controls remotely.
- Receive instant updates via WebSockets.

## Technology Stack

- **Language:** Kotlin 2.2.20+
- **UI Framework:** Compose Multiplatform 1.9.1+
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Dependency Injection:** Koin 4.1.0+
- **Networking:** Ktor Client 3.0.3+ (HTTP), Krossbow (STOMP/WebSockets).
- **State Management:** Kotlin Coroutines & StateFlow.
- **Serialization:** kotlinx.serialization.
- **Build System:** Gradle (Kotlin DSL).

## Directory Structure

```
/
├── composeApp/                 # Main application module
│   ├── src/
│   │   ├── commonMain/         # Shared code (Business logic & UI)
│   │   │   ├── kotlin/com/apptolast/greenhousefronts/
│   │   │   │   ├── data/       # Data layer (API, Repositories, Models)
│   │   │   │   ├── di/         # Koin modules (DI configuration)
│   │   │   │   ├── domain/     # Domain layer (Interfaces, Use Cases)
│   │   │   │   ├── presentation/# UI layer (Screens, ViewModels, Theme)
│   │   │   │   └── util/       # Utilities (Environment, DateTime)
│   │   │   └── composeResources/# Shared resources (Fonts, Images)
│   │   ├── androidMain/        # Android-specific implementations
│   │   ├── iosMain/            # iOS-specific implementations
│   │   ├── jvmMain/            # Desktop-specific implementations
│   │   ├── jsMain/             # JavaScript (Web) implementations
│   │   └── wasmJsMain/         # WebAssembly (Web) implementations
│   └── build.gradle.kts        # Module build config
├── iosApp/                     # iOS Xcode project wrapper
├── gradle/                     # Gradle wrapper and version catalog (libs.versions.toml)
└── build.gradle.kts            # Root build config
```

## Development & Build Commands

All commands should be executed from the project root.

### Android

* **Build Debug APK:** `./gradlew :composeApp:assembleDebug`
* **Run on Device/Emulator:** `./gradlew :composeApp:run`
* **Unit Tests:** `./gradlew :composeApp:testDebugUnitTest`

### Desktop (JVM)

* **Run Application:** `./gradlew :composeApp:run`
* **Package Distribution:** `./gradlew :composeApp:packageDistributionForCurrentOS`

### Web (Wasm & JS)

* **Run Wasm (Recommended):** `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
* **Run JS (Legacy):** `./gradlew :composeApp:jsBrowserDevelopmentRun`
* **Update Yarn Locks (Important):**
    * Run `./gradlew kotlinUpgradeYarnLock` (for JS)
    * Run `./gradlew kotlinWasmUpgradeYarnLock` (for Wasm)
    * *Note: This is required after changing npm-based dependencies.*

### iOS

* **Run:** Open `iosApp/iosApp.xcodeproj` in Xcode and run.
* **Tests:** `./gradlew :composeApp:iosSimulatorArm64Test`

### General

* **Run All Tests:** `./gradlew test`
* **Clean Build:** `./gradlew clean`

## Architecture & Conventions

### MVVM Pattern

- **Model (`data/model`):** Data classes (DTOs) representing API responses.
- **View (`presentation/ui`):** Composable functions. **Passive View** pattern; they observe state
  from ViewModels.
- **ViewModel (`presentation/viewmodel`):** Manages UI state using `StateFlow`. Interacts with
  Repositories.
- **Repository (`data/repository` & `domain/repository`):** Single source of truth for data.
  Mediates between API/WebSocket and the App.

### Dependency Injection (Koin)

- **Modules:** Defined in `commonMain/.../di/`.
    - `DataModule`: Network clients, APIs, Repositories.
    - `PresentationModule`: ViewModels (`viewModelOf(::MyViewModel)`).
- **Usage:**
    - **ViewModels:** In Composables, use `val viewModel: MyViewModel = koinViewModel()`.
    - **Classes:** Use constructor injection.

### Networking

- **HTTP:** `GreenhouseApiService` uses `HttpClient` (Ktor).
- **WebSockets:** `StompWebSocketClient` handles real-time data via STOMP.
- **Endpoints:** Configured in `util/Environment.kt` (DEV/PROD switching).

### UI & Theming

- **Theme:** Custom Material 3 theme in `presentation/ui/theme/`.
- **Colors:** Dark-first design. Use `MaterialTheme.colorScheme`.
- **Fonts:** Custom fonts supported via `composeResources/font`.
- **Strings:** User-facing strings are currently hardcoded in **Spanish**. Technical comments must
  be in **English**.

### Platform-Specific Code

- Use `expect`/`actual` pattern sparingly.
- Prefer interfaces defined in `commonMain` and implemented in platform source sets, injected via
  Koin.

## Key Files to Reference

- `gradle/libs.versions.toml`: Dependency versions.
- `composeApp/build.gradle.kts`: Project configuration and targets.
- `util/Environment.kt`: API base URLs and environment toggles.
- `presentation/ui/App.kt`: Main entry point for the UI.
