# AGENTS.md

Guidance for Codex when working in this repository. The global Anthropic AGENTS.md
(`~/.Codex/AGENTS.md`) covers the cross-project KMP standards (DI, theming, GitFlow,
Jira, etc.). This file only contains project-specific facts that override or extend it.

## Ground rules

1. **Verify, don't guess.** Backend contracts, library APIs, gradle tasks — check the
   source / Swagger / official docs. Use web search if unsure.
2. **Ask if unclear** before changing architecture or contracts.
3. **Match existing patterns.** MVVM + Repository + Koin DI is already wired across the
   app — extend it, don't reinvent it.
4. **Comments in English. UI strings in Spanish** (target audience).

## Stack snapshot

- Kotlin 2.2.20, Compose Multiplatform 1.9.1, Ktor 3.0.3, Koin BOM 4.1.1
- Targets: Android (minSdk 24, targetSdk 36), iOS, Desktop JVM, Web (Wasm + JS)
- Architecture: MVVM + Repository · DI: Koin · HTTP: Ktor · Real-time: STOMP via Krossbow
- Android namespace: `com.apptolast.greenhousefronts` · JVM heap: 4 GB · CC enabled

## Build & run

```bash
./gradlew :composeApp:assembleDebug                  # Android APK
./gradlew :composeApp:run                            # Android or Desktop (JVM)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # Web (preferred)
./gradlew :composeApp:jsBrowserDevelopmentRun        # Web (legacy JS)
# iOS: open iosApp/ in Xcode
```

### Yarn lock (web targets)

When you add/update deps that pull npm transitives (Krossbow, Ktor WebSockets, etc.):

```bash
./gradlew kotlinUpgradeYarnLock        # JS
./gradlew kotlinWasmUpgradeYarnLock    # WASM
```

Commit the updated `yarn.lock` files alongside the code change — they're tracked.

## Environment

`util/Environment.kt` — switch by changing `Environment.current`.

| Env  | REST base URL                                | WebSocket URL                                                   |
|------|----------------------------------------------|-----------------------------------------------------------------|
| DEV  | `https://inverapi-dev.apptolast.com/api/v1`  | `wss://inverapi-dev.apptolast.com/ws/greenhouse/status/client`  |
| PROD | `https://inverapi-prod.apptolast.com/api/v1` | `wss://inverapi-prod.apptolast.com/ws/greenhouse/status/client` |

Live OpenAPI: `https://inverapi-{dev,prod}.apptolast.com/v3/api-docs` ·
Swagger UI: `…/swagger-ui.html`. **The live spec is the source of truth** — verify
contracts against it before coding any new endpoint integration.

## Project layout (commonMain)

```
data/
├── local/auth/          # TokenStorage (multiplatform-settings)
├── model/               # DTOs (auth, alerts, greenhouse, …)
├── remote/
│   ├── api/             # ApiService classes (one per resource)
│   ├── push/            # FCM token registrar + provider expect/actual
│   ├── websocket/       # GreenhouseStatusWebSocket (STOMP)
│   └── KtorClient.kt    # Authenticated + unauthenticated HttpClient factories
└── repository/          # *RepositoryImpl

domain/
├── model/               # AuthState, SessionEvent, UserProfile, …
└── repository/          # Repository + SessionInvalidator interfaces

presentation/
├── navigation/          # Routes (Splash, Login, Greenhouses, *Detail, …)
├── ui/                  # Composables (Screen + ScreenContent + Preview per file)
├── ui/components/       # Reusable composables
├── ui/theme/            # Color, Type, Font, Theme + ConfigureSystemUI expect/actual
└── viewmodel/           # ViewModels (StateFlow + sealed UiState)

di/                      # KoinInitializer + dataModule / domainModule / presentationModule / platformModule
util/                    # Environment, DateTimeProvider (expect/actual), JwtDecoder
```

## Authentication & session lifecycle

Backend issues an access JWT (~1 h TTL) plus an opaque rotating refresh token (~30 d TTL).
Refresh rotates both; reusing a revoked refresh revokes the whole family server-side.

### Components

| Type                        | Role                                                                                                                                                                                                |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AuthApiService`            | `/auth/login`, `/register`, `/refresh`, `/forgot-password`, `/reset-password`, `/logout` — wired to the **unauthenticated** HttpClient (refresh would otherwise recurse through the bearer plugin). |
| `TokenStorage`              | Persists access + refresh + refresh expiry + tenantId/displayName on multiplatform-settings.                                                                                                        |
| `JwtDecoder`                | `extractStringClaim` / `extractLongClaim` / `extractExpiration` / `isTokenExpired(skewSeconds=30)`.                                                                                                 |
| `AuthRepository`            | Owns `authState: StateFlow<AuthState>` and `sessionEvents: SharedFlow<SessionEvent>`.                                                                                                               |
| `SessionInvalidator`        | Narrow slice (`tryRefreshOrInvalidate`, `invalidateSession`) injected into the authenticated HttpClient — breaks the otherwise circular Koin graph. Same instance as `AuthRepository`.              |
| `GreenhouseStatusWebSocket` | Subscribes the STOMP stream gated on `AuthState`; reacts to token rotation.                                                                                                                         |

### Login flow

1. `AuthViewModel.login()` → `AuthRepository.login(...)` → `AuthApiService.login(...)`.
2. On 200 `JwtResponse`, `persistSuccessfulAuth` saves access + refresh + computed
   `refreshExpiresAt` and emits `AuthState.Authenticated(token, exp)`.
3. `PushTokenRegistrar` (reactive collector on `authState`) registers the FCM token.
4. `SplashScreen` / global navigation react to the new state and navigate to Home.

### Refresh flow (transparent)

```
HTTP request → 401 → Ktor bearer{ refreshTokens } ─┐
                                                   ├──► AuthRepository.tryRefreshOrInvalidate()
WebSocket pre-check / retryWhen on TokenExpiredEx ─┘                │
                                                                    ▼
                                       refreshMutex (coalesces concurrent callers)
                                                                    │
                                                                    ▼
                              POST /auth/refresh { refreshToken }   │
                                                                    │
                  ┌─────────────┬────────────────────────┬──────────┴──────────┐
                  ▼             ▼                        ▼                     ▼
              200 → persist  4xx → drop refresh       5xx → keep refresh   I/O → keep both,
              new pair,         + invalidate(EXPIRED)    + invalidate          return null,
              return access     return null               (kill-switch)        next caller retries
```

- The Ktor `bearer { refreshTokens { … } }` block calls this on any 401 and retries the
  original request transparently with the new bearer.
- The WebSocket pipeline (`flatMapLatest { state -> createSessionFlow(state.token) }` +
  `retryWhen`) detects an expired bearer pre-connect, calls the same hook, and reopens
  the STOMP session with the rotated token (via `distinctUntilChangedBy { it.token }`).
- An internal `refreshMutex` makes "5 simultaneous 401s + WS reconnect" issue **one**
  HTTP call; the rest receive the cached fresh token.

### Cold-boot (`bootstrap()`)

1. No access stored → Unauthenticated(INITIAL).
2. Access valid → Authenticated.
3. Access expired + refresh still in window → silent refresh; success → Authenticated,
   failure → Unauthenticated(EXPIRED).
4. Both tokens dead → Unauthenticated(EXPIRED) + snackbar "Tu sesión ha caducado".

### Logout

`AuthViewModel.logout()` and `ProfileViewModel.logout()` both:

1. `pushTokenRegistrar.unregisterCurrentToken()` (must run **before** the JWT is wiped).
2. `authRepository.logout()` → backend best-effort, then `tokenStorage.clearAll()`.
3. Emit `AuthState.Unauthenticated(MANUAL_LOGOUT)`.

### TokenStorage keys

Prefix `greenhouse_auth_`: `access_token`, `username`, `tenant_id`, `display_name`,
`refresh_token`, `refresh_exp`. Backed by `multiplatform-settings` (SharedPreferences /
NSUserDefaults / java.util.prefs / localStorage).

## DI specifics (project-only — see global AGENTS.md for general Koin patterns)

Two named HttpClients in `dataModule`:

- `UNAUTHENTICATED_CLIENT` → for `AuthApiService` only.
- `AUTHENTICATED_CLIENT` → for every other `*ApiService` and the WebSocket. Receives
  the `SessionInvalidator` for the bearer refresh hook.

`AuthRepositoryImpl` is bound under **both** `AuthRepository` and `SessionInvalidator` —
this is the cycle-breaking trick that lets the authenticated client depend on the
invalidator interface without pulling in the whole repo (which itself depends on the
unauthenticated client).

`PushTokenRegistrar` is created with `createdAtStart = true` so its reactive collectors
on `authState` and FCM token rotations attach at app boot.

## WebSocket (STOMP) notes

- `GreenhouseStatusWebSocket` exposes a single shared hot `Flow` via `shareIn`. All
  ViewModels that need live status collect this same flow.
- Heartbeat: 10 s/10 s — keeps mobile NATs from dropping the TCP connection.
- No `autoReceipt` (Spring's `SimpleBroker` doesn't send STOMP RECEIPT frames).
- Order is enforced by suspending `subscribeText`: the SUBSCRIBE frame is on the wire
  before the initial `sendEmptyMsg("/app/status/request")` triggers the first snapshot.
- Diagnostic log line `RECV #N Δms B`: if `N` stays at 1 forever the JWT in CONNECT was
  rejected and the STOMP principal is anonymous (broadcasts won't be targeted).

## UI / theming pointers

Material 3 dark-first theme with neon-green accents — see `presentation/ui/theme/`
(`Color.kt`, `Type.kt`, `Font.kt`, `Theme.kt`). Always go through `MaterialTheme.colorScheme.*`
and `MaterialTheme.typography.*`; never hardcode colours.

Conventions: spacing in 4 dp multiples; corner radii 8–12 dp small / 16–24 dp cards;
touch targets ≥ 48 dp; every `Icon`/`Image` carries a `contentDescription`.

Every stateless `*Content` / `*Screen` and reusable component MUST ship with a
`@Preview` (`PreviewXxx`) wrapped in `GreenhouseTheme`. Use realistic fake data.

### Edge-to-edge / status bar

`MainActivity.enableEdgeToEdge(...)` + `ConfigureSystemUI(darkTheme)` (expect in
`Theme.kt`, actual in `Theme.android.kt` — toggles `isAppearanceLightStatusBars`/`-NavigationBars`
to `!darkTheme`). iOS `actual` is currently a no-op placeholder; JVM/Web are no-ops.

Troubleshooting: if status-bar icons disappear on theme switch, confirm
`enableEdgeToEdge(SystemBarStyle.auto(...))` in `MainActivity` and that `GreenhouseTheme`
calls `ConfigureSystemUI(darkTheme)` on every recomposition.

## Tests

Currently only a placeholder `commonTest` (`assertEquals(3, 1+2)`). Real tests would need
Ktor's `MockEngine` for `AuthRepository`/`AuthApiService` coverage (refresh happy path,
4xx/5xx/I-O paths, refreshMutex coalescing) and `koin-test` for the DI graph. Spin up
the harness in a dedicated PR if/when needed.

## Useful resources

- KMP: https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html
- Compose Multiplatform: https://www.jetbrains.com/compose-multiplatform/
- Ktor Client: https://ktor.io/docs/client-create-multiplatform-application.html
- Ktor bearer auth (refreshTokens contract): https://ktor.io/docs/client-bearer-auth.html
- Material 3: https://m3.material.io
- Koin: https://insert-koin.io
- Edge-to-edge: https://developer.android.com/develop/ui/compose/system/system-bars
