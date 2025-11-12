# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Guidelines (IMPORTANT)

1. **Do NOT invent or hallucinate information** - Always verify facts using official documentation
2. **Use web search when needed** - Consult official Kotlin Multiplatform and Ktor documentation for implementation details
3. **Ask questions if unclear** - If requirements are ambiguous or you're unsure about an approach, ask the user for clarification before proceeding
4. **Follow established patterns** - Use the MVVM architecture and repository pattern already implemented in this project
5. **Code comments must be in English** - All technical comments, documentation (KDoc), and code-level explanations must be written in English. User-facing strings in the UI (button labels, messages, etc.) can remain in Spanish for the target audience

## Project Overview

This is a **Kotlin Multiplatform (KMP) project** using **Compose Multiplatform** for shared UI across Android, iOS, Desktop (JVM), and Web (Wasm/JS) platforms. The project implements a **MVVM (Model-View-ViewModel)** architecture with **Ktor Client** for API communication.

## Build and Run Commands

### Android
```bash
./gradlew :composeApp:assembleDebug    # Build debug APK
./gradlew :composeApp:run              # Run on emulator/device
```

### Desktop (JVM)
```bash
./gradlew :composeApp:run              # Run desktop application
```

### Web
```bash
# Wasm target (recommended, modern browsers)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# JavaScript target (legacy, older browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### iOS
Open the `iosApp/` directory in Xcode or use the IDE run configuration.

## API Configuration

### Environments
The project supports multiple environments configured in `util/Environment.kt`:

- **DEV**: `https://inverapi-dev.apptolast.com`
- **PROD**: `https://inverapi-prod.apptolast.com`

To switch environments, modify `Environment.current` in `util/Environment.kt`.

### API Swagger Documentation
- **DEV**: https://inverapi-dev.apptolast.com/swagger-ui/index.html
- **PROD**: https://inverapi-prod.apptolast.com/swagger-ui/index.html

### Key API Endpoints

#### GET /api/greenhouse/messages/recent
Retrieves recent greenhouse sensor messages.

**Response**: Array of GreenhouseMessage objects
```json
[{
  "timestamp": "2025-11-11T21:54:17.336Z",
  "sensor01": 0.1,
  "sensor02": 0.1,
  "setpoint01": 0.1,
  "setpoint02": 0.1,
  "setpoint03": 0.1,
  "greenhouseId": "string",
  "rawPayload": "string",
}]
```

#### POST /api/mqtt/publish/custom
Publishes a custom MQTT message to the greenhouse.

**Query Parameters**:
- `topic` (string): MQTT topic (default: "GREENHOUSE/RESPONSE")
- `qos` (integer): Quality of Service: 0, 1, or 2 (default: 0)

**Request Body**: GreenhouseMessage object (same structure as above)

**Authentication**: No authentication required for current endpoints

## Architecture

### MVVM Architecture Pattern
This project follows **MVVM (Model-View-ViewModel)** architecture as recommended by Google for Kotlin Multiplatform:

```
presentation/
├── ui/              # Composable UI (View)
│   └── App.kt
└── viewmodel/       # ViewModels
    └── GreenhouseViewModel.kt

domain/
└── repository/      # Repository interfaces
    └── GreenhouseRepository.kt

data/
├── model/           # Data models (DTOs)
│   └── GreenhouseMessage.kt
├── remote/          # Network layer
│   ├── api/         # API service definitions
│   │   └── GreenhouseApiService.kt
│   └── KtorClient.kt  # Ktor HTTP client configuration
└── repository/      # Repository implementations
    └── GreenhouseRepositoryImpl.kt

util/
├── Environment.kt         # Multi-environment configuration
└── DateTimeProvider.kt    # Expect/actual for platform-specific timestamps
```

### Multiplatform Source Structure
```
composeApp/src/
├── commonMain/          # Shared code for all platforms
│   ├── kotlin/          # Common Kotlin code (MVVM architecture)
│   └── composeResources/ # Shared resources (images, strings, etc.)
├── androidMain/         # Android-specific code
├── iosMain/            # iOS-specific code (Kotlin)
├── jvmMain/            # Desktop-specific code
├── jsMain/             # JavaScript target code
├── wasmJsMain/         # WebAssembly target code
└── commonTest/         # Shared test code
```

### Network Layer (Ktor Client)
The project uses **Ktor Client** for HTTP communication:
- **Configuration**: `data/remote/KtorClient.kt`
- **Features**: Content negotiation (JSON), Logging, Serialization
- **Engines**: OkHttp (Android/JVM), Darwin (iOS)

### Expect/Actual Pattern
Use the `expect`/`actual` pattern for platform-specific implementations:
- Define `expect` declarations in `commonMain/` (e.g., `Platform.kt`)
- Provide `actual` implementations in platform-specific source sets (e.g., `Platform.android.kt`, `Platform.ios.kt`)

### Adding New Code
- **Models**: Add to `data/model/`
- **API Services**: Add to `data/remote/api/`
- **Repositories**: Interface in `domain/repository/`, implementation in `data/repository/`
- **ViewModels**: Add to `presentation/viewmodel/`
- **UI**: Add Composable functions in `presentation/ui/`
- **Platform-specific**: Add to respective platform source sets (androidMain, iosMain, etc.)

## Key Configuration

### Version Catalog (`gradle/libs.versions.toml`)
- Kotlin: 2.2.20
- Compose Multiplatform: 1.9.1
- Android minSdk: 24, targetSdk: 36
- Ktor: 3.0.3
- Kotlinx Serialization: 1.8.0
- Centralized dependency management

### Build Configuration (`composeApp/build.gradle.kts`)
- Defines all platform targets
- Configures sourceSets and dependencies
- Android namespace: `com.apptolast.greenhousefronts`

### Gradle Properties (`gradle.properties`)
- JVM max memory: 4GB (`-Xmx4g`)
- Configuration cache enabled

## Current Project State

### Main UI (`presentation/ui/App.kt`)
The UI connects to `GreenhouseViewModel` and displays:
- **Sensor data** from the most recent API message (sensor01 or setpoint01)
- **Greenhouse ID** from the last message
- **OutlinedTextField** for user input (setpoint value)
- **Button** "Enviar" to publish setpoint to MQTT via API
- **Loading state** with CircularProgressIndicator
- **Error/Success messages** using Snackbar

State is managed using StateFlow in the ViewModel and collected in the UI with `collectAsState()`.

### Data Flow
1. **ViewModel initialization** → Calls `loadRecentMessages()`
2. **Repository** → Calls `GreenhouseApiService.getRecentMessages()`
3. **Ktor Client** → Makes HTTP GET request
4. **StateFlow updates** → UI recomposes with new data
5. **User clicks "Enviar"** → `publishSetpoint()` called
6. **Repository** → Calls `GreenhouseApiService.publishMessage()`
7. **Success** → Reloads messages and shows confirmation

## Development Notes

- All code comments and technical documentation must be in English
- User-facing UI strings (button labels, messages) are in Spanish for the target audience
- Project uses Material Design 3 for consistent UI
- Compose Multiplatform enables write-once UI code across all platforms
- Network calls are made with Ktor Client (not Retrofit)
- All API communication goes through the Repository pattern
- ViewModels use Kotlin Coroutines and StateFlow for reactive state management

## Expect/Actual Pattern - Platform-Specific Code

### When to Use Expect/Actual

The expect/actual mechanism enables accessing platform-specific APIs when:

1. **No multiplatform library exists** - The functionality is not available through official KMP libraries
2. **Factory functions** - Need to return platform-specific implementations
3. **Inheriting platform classes** - Must extend existing platform-specific base classes
4. **Direct native API access** - Require direct access to platform APIs for performance or features

### When NOT to Use (IMPORTANT)

**Official Recommendation**: Prefer interfaces over expect/actual in most cases.

❌ **DO NOT use expect/actual if:**
- A multiplatform library already exists (e.g., kotlinx-datetime, kotlinx-coroutines)
- An interface would be sufficient
- You can use dependency injection with interfaces
- Standard Kotlin constructs solve the problem

✅ **Interfaces are better because:**
- Allow multiple implementations per platform
- Make testing easier with fake/mock implementations
- More flexible and standard Kotlin approach
- Avoid Beta feature limitations

### Example in This Project

The project uses expect/actual for `getCurrentTimestamp()` in `util/DateTimeProvider.kt`:

```kotlin
// commonMain/util/DateTimeProvider.kt
expect fun getCurrentTimestamp(): String

// androidMain/util/DateTimeProvider.android.kt
actual fun getCurrentTimestamp(): String {
    return kotlin.time.Clock.System.now().toString()
}

// iosMain/util/DateTimeProvider.ios.kt
actual fun getCurrentTimestamp(): String {
    // iOS uses Foundation NSDate directly for better platform integration
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
```

**Note**: Most platforms use `kotlin.time.Clock` from the Kotlin standard library. iOS uses Foundation's NSDate directly for optimal platform integration.

### Rules for Expect/Actual Declarations

1. **Declaration Location**: `expect` in `commonMain`, `actual` in each platform source set
2. **Same Package**: Both must be in the identical package
3. **Matching Signatures**: Names, parameters, and return types must match exactly
4. **No Implementation in Expect**: Expected declarations cannot contain implementation code
5. **All Platforms**: Every platform must provide an `actual` implementation

### Compiler Behavior

- Validates all declarations during compilation
- Merges expected and actual declarations
- Ensures signature consistency across platforms
- Generates one declaration with appropriate implementation per platform

### Process for Handling Missing Multiplatform Libraries

When encountering functionality without multiplatform support:

1. **Search for Official KMP Libraries**
   - Check JetBrains kotlinx.* libraries first
   - Search Maven Central for "kmp-*" or "kmm-*" prefixed libraries
   - Verify library supports all your target platforms

2. **Verify Library Documentation**
   - Read official documentation to confirm multiplatform support
   - Check GitHub releases for latest stable versions
   - Review platform compatibility matrix

3. **Test Library Integration**
   - Add dependency to `commonMain`
   - Sync Gradle and verify no errors
   - Test compilation for each platform target

4. **Implement Expect/Actual as Last Resort**
   - Only when no suitable multiplatform library exists
   - Document the decision and alternatives evaluated
   - Create expect declaration in `commonMain`
   - Provide actual implementations for each platform
   - Use platform-native APIs (e.g., NSDate for iOS, java.time for JVM)

5. **Document the Implementation**
   - Add comments explaining why expect/actual was necessary
   - Reference any GitHub issues or documentation consulted
   - Note future migration path if library becomes available

### Best Practices

- **Verify First**: Always search for existing multiplatform solutions before implementing expect/actual
- **Use Web Search**: When unsure, search official Kotlin and library documentation
- **Ask Questions**: If requirements are unclear, ask for clarification rather than guessing
- **Document Decisions**: Explain why expect/actual was chosen over alternatives
- **Keep It Simple**: Minimize the surface area of platform-specific code
- **Test All Platforms**: Verify implementation works on every target platform

### Beta Feature Warning

Expected/actual classes are in **Beta status** - migration steps may be required in future Kotlin versions. Suppress warnings if needed:

```kotlin
freeCompilerArgs.add("-Xexpect-actual-classes")
```

### Resources for Expect/Actual

- **Official Documentation**: https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-expect-actual.html
- **Kotlin Language Docs**: https://kotlinlang.org/docs/multiplatform-expect-actual.html
- **Connect to Platform APIs**: https://kotlinlang.org/docs/multiplatform-connect-to-apis.html

## Useful Resources

When implementing new features or troubleshooting, consult these official resources:

### Kotlin Multiplatform
- **Official Guide**: https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html
- **Compose Multiplatform**: https://www.jetbrains.com/compose-multiplatform/
- **KMP Architecture**: https://kotlinlang.org/docs/multiplatform-mobile-understand-project-structure.html

### Ktor Client
- **Official Documentation**: https://ktor.io/docs/client-create-multiplatform-application.html
- **Ktor Client Setup**: https://ktor.io/docs/client-create-new-application.html
- **Content Negotiation**: https://ktor.io/docs/serialization-client.html

### Android/Compose
- **Compose Documentation**: https://developer.android.com/jetpack/compose
- **ViewModel Guide**: https://developer.android.com/topic/libraries/architecture/viewmodel
- **StateFlow**: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
