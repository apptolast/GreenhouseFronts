# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Development Guidelines (IMPORTANT)

1. **Do NOT invent or hallucinate** — Verify with official documentation. Use web search if unsure.
2. **Ask if unclear** — Ask for clarification rather than guessing on architecture or requirements.
3. **Follow existing patterns** — MVVM + Repository are already implemented. Match them.
4. **Code comments in English** — All technical comments and KDoc in English. UI-facing strings stay in Spanish for the
   target audience.

## Project Overview

**Kotlin Multiplatform** project using **Compose Multiplatform** for shared UI across Android, iOS, Desktop (JVM), and
Web (Wasm/JS). Architecture: **MVVM** + **Repository pattern**. Network: **Ktor Client**. DI: **Koin**.

## Build and Run Commands

### Android
```bash
./gradlew :composeApp:assembleDebug    # Build debug APK
./gradlew :composeApp:run              # Run on emulator/device
```

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Web
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Wasm (recommended)
./gradlew :composeApp:jsBrowserDevelopmentRun       # JS (legacy)
```

### iOS

Open `iosApp/` in Xcode or use the IDE run configuration.

### Yarn Lock File Management (IMPORTANT)

When adding/modifying dependencies that have npm transitive deps (Krossbow, Ktor WebSockets, etc.), you **must manually
update** the yarn.lock file for web targets:

```bash
./gradlew kotlinUpgradeYarnLock        # JS target
./gradlew kotlinWasmUpgradeYarnLock    # WASM target
```

Run after adding/updating deps in `gradle/libs.versions.toml`, or when the build error says
`"Lock file was changed. Run the kotlinUpgradeYarnLock task"`. **Always commit the updated `yarn.lock` files** alongside
your code changes — they're tracked for build reproducibility.

## API Configuration

### Environments

Configured in `util/Environment.kt`. Switch by modifying `Environment.current`.

- **DEV**: `https://inverapi-dev.apptolast.com`
- **PROD**: `https://inverapi-prod.apptolast.com`

### Swagger

- DEV: https://inverapi-dev.apptolast.com/swagger-ui/index.html
- PROD: https://inverapi-prod.apptolast.com/swagger-ui/index.html

### Key API Endpoints

#### GET /api/greenhouse/messages/recent
Retrieves recent greenhouse sensor messages.

**Response**: Array of GreenhouseMessage:
```json
[{
  "timestamp": "2025-11-11T21:54:17.336Z",
  "sensor01": 0.1,
  "sensor02": 0.1,
  "setpoint01": 0.1,
  "setpoint02": 0.1,
  "setpoint03": 0.1,
  "greenhouseId": "string",
  "rawPayload": "string"
}]
```

#### POST /api/mqtt/publish/custom

Publishes a custom MQTT message.

**Query params**: `topic` (default `"GREENHOUSE/RESPONSE"`), `qos` (0/1/2, default 0).
**Body**: GreenhouseMessage (same as above).
**Auth**: None for current endpoints.

## Architecture

### MVVM Layout

```
presentation/
├── ui/              # Composable UI (View)
└── viewmodel/       # ViewModels

domain/
└── repository/      # Repository interfaces

data/
├── model/           # DTOs
├── remote/api/      # API service definitions
├── remote/KtorClient.kt
└── repository/      # Repository implementations

util/
├── Environment.kt
└── DateTimeProvider.kt   # expect/actual for timestamps

di/                  # Koin modules (see DI section)
```

### Multiplatform Source Sets
```
composeApp/src/
├── commonMain/{kotlin,composeResources}
├── androidMain/, iosMain/, jvmMain/, jsMain/, wasmJsMain/
└── commonTest/
```

### Network Layer (Ktor)

- Configuration: `data/remote/KtorClient.kt`
- Features: ContentNegotiation (JSON), Logging, Serialization
- Engines: OkHttp (Android/JVM), Darwin (iOS)

### Adding New Code

- **Models** → `data/model/`
- **API services** → `data/remote/api/`
- **Repositories** → interface in `domain/repository/`, impl in `data/repository/`
- **ViewModels** → `presentation/viewmodel/`
- **UI** → `presentation/ui/`
- **Platform-specific** → respective platform source set

## Key Versions (`gradle/libs.versions.toml`)

- Kotlin 2.2.20, Compose Multiplatform 1.9.1
- Android minSdk 24, targetSdk 36
- Ktor 3.0.3, kotlinx.serialization 1.8.0
- Koin BOM 4.1.1
- Android namespace: `com.apptolast.greenhousefronts`
- JVM max memory: 4GB, configuration cache enabled

## Current Main UI

`presentation/ui/App.kt` connects to `GreenhouseViewModel` and shows:

- Sensor data from the most recent API message
- Greenhouse ID
- OutlinedTextField for setpoint input + "Enviar" button (POST to MQTT)
- Loading via `CircularProgressIndicator`, errors/success via Snackbar

State is `StateFlow` in the ViewModel, collected with `collectAsState()`.

## Development Notes

- All technical docs/comments in English; user-facing UI strings in Spanish
- Material Design 3 throughout
- Network through Ktor Client (not Retrofit)
- All API access goes through the Repository pattern
- ViewModels use Coroutines + StateFlow

---

## Dependency Injection (Koin 4.1.1+)

### Why Koin

Multiplatform-native, lightweight (no codegen/reflection), first-class Compose support via `koinViewModel()`, easy fakes
for testing.

### DI Structure
```
di/
├── KoinInitializer.kt       # initKoin() entry point
├── DataModule.kt            # HttpClient, API, Repository
├── DomainModule.kt          # use cases
├── PresentationModule.kt    # ViewModels
└── PlatformModule.kt        # expect/actual platform deps
```

### Versions (libs.versions.toml)
```toml
[versions]
koin-bom = "4.1.1"

[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin-bom" }
koin-core = { module = "io.insert-koin:koin-core" }
koin-compose = { module = "io.insert-koin:koin-compose" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel" }
koin-compose-viewmodel-navigation = { module = "io.insert-koin:koin-compose-viewmodel-navigation" }
koin-android = { module = "io.insert-koin:koin-android" }
koin-test = { module = "io.insert-koin:koin-test" }
```

Wire in `composeApp/build.gradle.kts`: BOM + `koin-core/compose/compose-viewmodel/compose-viewmodel-navigation` in
`commonMain`, `koin-android` in `androidMain`, `koin-test` in `commonTest`.

### Module Examples

```kotlin
val dataModule = module {
    single { createHttpClient() }
    single { createStompClient() }
    singleOf(::GreenhouseApiService)
    singleOf(::StompWebSocketClient)
    singleOf(::GreenhouseRepositoryImpl) bind GreenhouseRepository::class
}

val presentationModule = module {
    viewModelOf(::GreenhouseViewModel)
}
```

- `single` → app-lifetime singleton; `singleOf(::Class)` → ctor injection
- `bind` exposes the impl by interface
- `viewModelOf` → ViewModel-scoped (survives config changes)

### Injecting ViewModels in Composables

Use `koinViewModel()` for **all scenarios**, including Navigation Compose. `koinNavViewModel()` is **deprecated** in
Koin 4.1+.

```kotlin
import org.koin.compose.viewmodel.koinViewModel

NavHost(navController, startDestination = LoginRoute) {
    composable<HomeRoute> {
        val viewModel: GreenhouseViewModel = koinViewModel()
        HomeScreen(viewModel = viewModel)
    }
}
```

`koinViewModel()` automatically handles NavBackStackEntry integration, SavedStateHandle, navigation argument injection,
and lifecycle scoping.

**Shared ViewModel across destinations** — scope to a parent route's `NavBackStackEntry`:
```kotlin
NavHost(navController, startDestination = "screenA", route = "parentRoute") {
    composable("screenA") { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry("parentRoute")
        }
        val shared: SharedViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        ScreenA(shared)
    }
    // screenB does the same → same instance
}
```

### Constructor Injection

Always constructor injection — never field injection. Koin resolves deps automatically:
```kotlin
class GreenhouseViewModel(private val repository: GreenhouseRepository) : ViewModel()
class GreenhouseRepositoryImpl(
    private val apiService: GreenhouseApiService,
    private val webSocketClient: StompWebSocketClient
) : GreenhouseRepository
```

### Platform Initialization

**Android** (`GreenhouseApplication`, registered in `AndroidManifest.xml` via `android:name`):
```kotlin
class GreenhouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@GreenhouseApplication)
        }
    }
}
```

**iOS** (`iOSApp.swift`): `KoinInitializerKt.doInitKoin()` in `init()`.
**Desktop / Web**: call `initKoin()` from `main()` before composing.

### Scoping

| Scope         | Use                             | Lifetime                |
|---------------|---------------------------------|-------------------------|
| `single`      | HttpClient, repos, API services | App                     |
| `factory`     | Use cases, transient objects    | Per injection           |
| `viewModelOf` | ViewModels                      | Survives config changes |

### Best Practices

- Constructor injection only — never field injection
- Depend on interfaces (`GreenhouseRepository`), not impls
- Separate modules by layer (data/domain/presentation)
- Platform deps via `expect`/`actual` `platformModule`

### Testing
```kotlin
class FakeGreenhouseRepository : GreenhouseRepository {
    var shouldReturnError = false
    var fakeMessages = emptyList<GreenhouseMessage>()
    override suspend fun getRecentMessages() =
        if (shouldReturnError) Result.failure(Exception("Test error"))
        else Result.success(fakeMessages)
}

class GreenhouseViewModelTest : KoinTest {
    @Before
    fun setup() = startKoin { modules(testModule) }
    @After
    fun teardown() = stopKoin()
    companion object {
        private val testModule = module {
            single<GreenhouseRepository> { FakeGreenhouseRepository() }
            viewModelOf(::GreenhouseViewModel)
        }
    }
}
```

### Common Issues

- **`KoinAppAlreadyStartedException`** → `initKoin()` called more than once. Call only at app entry point.
- **Missing dependency** → Class not registered. Add to the right module.
- **Circular dependency** → Refactor; introduce a mediator.

Docs: https://insert-koin.io · KMP: https://insert-koin.io/docs/reference/koin-mp/kmp/

---

## Expect/Actual Pattern

### When to Use

Only when there's a real platform need: no multiplatform library exists, factory functions returning platform impls,
inheriting platform classes, or direct native API access.

### When NOT to Use

**Prefer interfaces** over expect/actual in most cases. Don't use it if:

- A multiplatform library already covers it (kotlinx-datetime, kotlinx-coroutines, etc.)
- An interface + DI would suffice

Interfaces allow multiple impls per platform, are easier to test, and avoid Beta limitations.

### Example in This Project

`util/DateTimeProvider.kt`:
```kotlin
// commonMain
expect fun getCurrentTimestamp(): String

// androidMain / jvmMain / wasm / js
actual fun getCurrentTimestamp(): String =
    kotlin.time.Clock.System.now().toString()

// iosMain — uses Foundation directly for native integration
actual fun getCurrentTimestamp(): String {
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
```

### Rules

1. `expect` in `commonMain`, `actual` in each platform source set
2. Identical package on both sides
3. Names, parameters, return types must match exactly
4. `expect` declarations contain no implementation
5. Every target platform must provide an `actual`

### Process Before Reaching for Expect/Actual

1. Search kotlinx.* and Maven Central for existing KMP libraries
2. Verify multiplatform support and target compatibility
3. Test integration in `commonMain`
4. Only then implement expect/actual; document why and the alternatives evaluated

### Beta Warning

Expected/actual classes are still **Beta**. Suppress with:
```kotlin
freeCompilerArgs.add("-Xexpect-actual-classes")
```

---

## UI Design & Theming

Custom Material 3 theme — **dark-first** with neon green accents, designed for greenhouse monitoring dashboards. Located
in `presentation/ui/theme/`:

```
theme/
├── Color.kt    # light + dark color schemes
├── Font.kt     # appFontFamily() composable
├── Type.kt     # Material 3 type scale
└── Theme.kt    # GreenhouseTheme + ConfigureSystemUI() expect
```

### Color Palette (Dark — Primary)

| Role                         | Hex       | Use                               |
|------------------------------|-----------|-----------------------------------|
| `primary`                    | `#00E676` | Neon green; FABs, primary actions |
| `onPrimary`                  | `#003300` | Text on primary                   |
| `primaryContainer`           | `#1E3A34` | Dark green-gray; emphasized cards |
| `onPrimaryContainer`         | `#B2DFDB` | Light teal text                   |
| `background`                 | `#0F1419` | Near-black with blue tint         |
| `surface`                    | `#1A1E23` | Dark gray; cards                  |
| `surfaceVariant`             | `#1E3A34` | Greenhouse-branded surfaces       |
| `onBackground` / `onSurface` | `#E6E1E5` | Body text                         |
| `tertiary`                   | `#4ECDC4` | Teal accent (humidity displays)   |
| `onTertiary`                 | `#002020` | Dark teal text                    |
| `tertiaryContainer`          | `#1A3635` | Dark teal-gray                    |

**Always use `MaterialTheme.colorScheme.*`** — never hardcode `Color(0x…)` in components. For custom hues outside
Material 3 roles, define them in `Color.kt` and assign via `surfaceTint` or other flexible roles.

### Material 3 Color Roles (Quick Reference)

| Role                           | Typical Use                             |
|--------------------------------|-----------------------------------------|
| `primary` / `primaryContainer` | Brand color, prominent buttons          |
| `secondary`                    | Less prominent actions, filter chips    |
| `tertiary`                     | Contrasting accents, special highlights |
| `surface` / `surfaceVariant`   | Cards, dialogs, input fields            |
| `outline`                      | Borders, dividers                       |
| `error`                        | Error/validation states                 |

### Typography

Uses Material 3's 5-category type scale (Display, Headline, Title, Body, Label). Always reference via
`MaterialTheme.typography.*` — e.g., `headlineMedium` for page titles, `titleLarge` for sections, `bodyMedium` for
content, `labelLarge` for buttons.

### Custom Fonts

**Current**: System default (`FontFamily.Default`).

The font system is composable-based: `Font.kt` exposes `appFontFamily()`, `Type.kt` calls it, `Theme.kt` calls
`appTypography()`. Swapping fonts only requires editing `Font.kt`.

**To add a font** (e.g. Inter):

1. Drop `.ttf` files into `composeApp/src/commonMain/composeResources/font/` with lowercase, underscore-separated
   names (e.g. `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`).
2. Run `./gradlew build` to generate `Res.font.*` accessors.
3. Update `appFontFamily()`:
```kotlin
@Composable
fun appFontFamily(): FontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )
```

**Compose Multiplatform note**: `Font()` is a `@Composable` function (unlike Android-only Compose), so `FontFamily` and
`Typography` must be created inside `@Composable` functions, not as top-level `val`s. That's why `appFontFamily()` and
`appTypography()` are functions.

Recommended fonts for dashboards: Inter (general UI), Roboto (data tables), Manrope, DM Sans, Geist Sans.

### UI Conventions

- **Spacing**: multiples of 4dp (4, 8, 12, 16, 24, 32)
- **Corner radii**: small components 8–12dp, buttons/text fields 12dp, cards/dialogs 16–24dp
- **Touch targets**: minimum 48dp × 48dp
- **Icons**: `androidx.compose.material.icons.Icons.Default.*`, tinted with theme colors
- **Always provide `contentDescription`** on `Icon`/`Image`
- **Use semantic colors** (`error` for errors, etc.) — don't hardcode red/green

### Quick Component Patterns

```kotlin
// Outlined input
OutlinedTextField(
    value = value, onValueChange = { value = it },
    label = { Text("Label") },
    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
)

// Primary / secondary / text buttons
Button(
    onClick = { }, colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) { Text("Primary") }
OutlinedButton(onClick = { }) { Text("Secondary") }
TextButton(onClick = { }) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }

// Emphasized card
Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
) { /* … */ }
```

### Dark Theme Best Practices

- In light theme, swap the bright neon `#00E676` for a darker green like `#1B5E20`
- Avoid pure `#000000`; use `#0F1419`
- Use color tints for elevation rather than shadows
- Test with `GreenhouseTheme(darkTheme = false/true)` previews

### Status Bar / System UI

Edge-to-edge with adaptive icon colors that switch based on `darkTheme`. Implementation uses `expect/actual`:

```
theme/
├── Theme.kt          # ConfigureSystemUI() expect
├── Theme.android.kt  # WindowCompat-based actual
├── Theme.ios.kt      # placeholder
└── Theme.{jvm,js,wasmJs}.kt  # no-op
```

**MainActivity**:
```kotlin
enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
)
```

**Theme.android.kt**:
```kotlin
@Composable
actual fun ConfigureSystemUI(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            // true → dark icons (light bg); false → light icons (dark bg)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
```

If status-bar icons disappear or don't update on theme switch:

1. Confirm `enableEdgeToEdge(SystemBarStyle.auto(...))` runs in `MainActivity`.
2. `GreenhouseTheme` calls `ConfigureSystemUI(darkTheme)` on every recomposition.
3. The logic is inverted: `isAppearanceLightStatusBars = !darkTheme`.

iOS implementation is currently a no-op placeholder; extend with `UIApplication.shared.statusBarStyle` /
`preferredStatusBarStyle` if needed.

### Accessibility

- `contentDescription` on all icons/images
- Semantic colors over raw hex
- 48dp minimum touch targets
- Contrast ≥ 4.5:1 (body), ≥ 3:1 (large text)

---

## Useful Resources

- KMP guide: https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html
- Compose Multiplatform: https://www.jetbrains.com/compose-multiplatform/
- Ktor Client: https://ktor.io/docs/client-create-multiplatform-application.html
- Material 3: https://m3.material.io
- Koin: https://insert-koin.io
- Edge-to-Edge: https://developer.android.com/develop/ui/compose/system/system-bars
