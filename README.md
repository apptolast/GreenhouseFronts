# 🌱 GreenhouseFronts / Invernaderos Front-End

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.9.3-brightgreen.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![Ktor](https://img.shields.io/badge/Ktor-3.3.2-orange.svg)](https://ktor.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)]()

[![Platforms](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey.svg)]()

**[Language: [English](#english) | [Español](#español)]**

---

<a name="english"></a>
## 🇬🇧 English

### 📖 Table of Contents
- [🎯 What is This?](#what-is-this)
- [✨ Features](#features)
- [🏗️ How It Works](#how-it-works)
- [🚀 Quick Start](#quick-start)
- [📦 Installation](#installation)
- [🔧 Configuration](#configuration)
- [💻 API Reference](#api-reference)
- [🏛️ Architecture](#architecture)
- [🧪 Testing](#testing)
- [🐳 Deployment](#deployment)
- [🔍 Troubleshooting](#troubleshooting)
- [🤝 Contributing](#contributing)
- [📄 License](#license)

---

### 🎯 What is This?

**Think of GreenhouseFronts as your greenhouse's digital dashboard** - like the control panel in a spaceship, but for your plants! 🌿

Imagine you have a greenhouse full of plants that need constant care. You can't be there 24/7 checking thermometers and adjusting controls, right? That's where GreenhouseFronts comes in. It's like having a smart assistant that:

- **Shows you real-time data** from your greenhouse sensors (temperature, humidity, etc.) - like checking your phone to see the weather
- **Updates instantly** when conditions change - think of it as having a live video feed instead of checking snapshots every few minutes
- **Lets you adjust settings remotely** - like using your smart home app to adjust the thermostat, but for your greenhouse

**For the tech-savvy:** This is a Kotlin Multiplatform application using Compose Multiplatform that works on Android, iOS, Desktop, and Web. It connects to a backend API to monitor greenhouse sensors and control environmental settings in real-time via HTTP and WebSocket/STOMP protocols.

---

### ✨ Features

#### 🌡️ Real-Time Sensor Monitoring
**What it does:** Shows you live readings from your greenhouse sensors  
**Analogy:** Like having a dashboard in your car that constantly updates your speed, fuel level, and engine temperature

- View current sensor values (sensor01, sensor02)
- See setpoint configurations (setpoint01, setpoint02, setpoint03)
- Monitor specific greenhouse by ID
- Automatic updates every few seconds

#### ⚡ Instant Updates via WebSocket
**What it does:** Data updates immediately when sensors detect changes  
**Analogy:** Think of it like a group chat where everyone sees messages instantly, vs. refreshing your email every minute

- Real-time STOMP WebSocket connection
- Automatic reconnection if connection drops
- Connection status indicator (connected/disconnected)
- Falls back to HTTP polling if WebSocket fails

#### 🎛️ Remote Control
**What it does:** Change greenhouse settings from anywhere  
**Analogy:** Like using your TV remote, but for your greenhouse's climate control

- Adjust setpoints remotely
- Publish commands via MQTT
- Immediate feedback on successful changes
- Input validation to prevent errors

#### 🌐 Works Everywhere
**What it does:** Use the same app on any device  
**Analogy:** Like Netflix - same content, works on your phone, tablet, computer, or smart TV

- **Android**: Native mobile app
- **iOS**: Native iPhone/iPad app  
- **Desktop**: Windows, macOS, Linux application
- **Web**: Runs in any modern browser (no installation needed!)

---

### 🏗️ How It Works

Think of this application like a **restaurant with a sophisticated ordering system**:

#### 🏢 The Architecture (Restaurant Analogy)

```
┌─────────────────────────────────────────────────────────────┐
│  🖥️ UI (Your Table/Menu)                                     │
│  Where you see information and place orders                  │
│  Built with Compose Multiplatform - Material Design 3        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  📋 ViewModel (The Waiter)                                   │
│  Takes your requests, remembers your orders, updates you     │
│  Manages state, handles user actions, coordinates data flow  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  🗄️ Repository (The Kitchen Manager)                         │
│  Coordinates between different data sources                  │
│  Decides whether to use fresh data or cached data            │
└────────────────────┬────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
┌─────────────────────┐  ┌──────────────────────┐
│  📡 API Service      │  │  🔌 WebSocket Client │
│  (Regular Orders)    │  │  (Live Updates)      │
│  HTTP REST calls     │  │  STOMP protocol      │
│  Ktor Client         │  │  Krossbow library    │
└──────────┬───────────┘  └──────────┬───────────┘
           │                         │
           └────────┬────────────────┘
                    ↓
    ┌───────────────────────────────┐
    │  ☁️ Backend API               │
    │  (The Kitchen)                │
    │  inverapi-dev.apptolast.com   │
    │  inverapi-prod.apptolast.com  │
    └───────────────────────────────┘
```

#### 🔄 Data Flow (How Orders Work)

1. **You want to see sensor data** (Customer looks at menu)
   - UI displays loading state
   - ViewModel asks Repository for data

2. **Repository gets the data** (Manager checks with kitchen)
   - First tries WebSocket connection (real-time channel)
   - Falls back to HTTP request if WebSocket unavailable
   - API Service makes the actual request

3. **Data comes back** (Food arrives at table)
   - Repository receives greenhouse messages
   - ViewModel processes and updates UI state
   - UI automatically refreshes with new data

4. **You change a setpoint** (You customize your order)
   - UI sends your input to ViewModel
   - ViewModel validates the value
   - Repository publishes via MQTT through API
   - Success message appears when confirmed

#### 🧩 MVVM Pattern Explained

**Instead of:** "Model-View-ViewModel architecture pattern"

**Think of it as:** A restaurant service model:
- **Model** (Data): The ingredients and recipes (GreenhouseMessage, sensor values)
- **View** (UI): The dining area where customers see and interact (Compose UI)
- **ViewModel** (Service): The waiter who takes orders, manages requests, and brings updates (GreenhouseViewModel)

The ViewModel is like a waiter who:
- Remembers what you ordered (state management)
- Checks on your food's status (loads data)
- Brings you updates without you asking (reactive StateFlow)
- Takes new orders when you're ready (handles user actions)

---

### 🚀 Quick Start

**Get the app running in under 5 minutes!**

#### Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher
- **For Android**: Android Studio or Android SDK
- **For iOS**: macOS with Xcode installed
- **For Desktop/Web**: Just the JDK!

#### Option 1: Desktop (Fastest!)

```bash
# Clone the repository
git clone https://github.com/apptolast/GreenhouseFronts.git
cd GreenhouseFronts

# Run the desktop app
./gradlew :composeApp:run

# That's it! App will launch in a window 🎉
```

#### Option 2: Web Browser

```bash
# For modern browsers (WebAssembly - faster)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# For older browsers (JavaScript - more compatible)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Browser will open automatically at http://localhost:8080
```

#### Option 3: Android

```bash
# Build the APK
./gradlew :composeApp:assembleDebug

# Or open in Android Studio and click Run ▶️
```

#### Option 4: iOS

```bash
# Open the iOS project in Xcode
open iosApp/iosApp.xcodeproj

# Click Run ▶️ in Xcode
```

**Windows Users:** Replace `./gradlew` with `gradlew.bat`

---

### 📦 Installation

#### System Requirements

| Platform | Minimum Version | Recommended |
|----------|----------------|-------------|
| **Android** | API 24 (Android 7.0) | API 36 (Android 15) |
| **iOS** | iOS 13.0 | iOS 17.0+ |
| **Desktop** | Java 17 | Java 21 |
| **Web** | Modern browsers | Chrome 119+, Firefox 120+, Safari 17+ |

#### Development Environment Setup

1. **Install JDK 17+**
   ```bash
   # Check your Java version
   java -version
   
   # Should show version 17 or higher
   ```

2. **Clone Repository**
   ```bash
   git clone https://github.com/apptolast/GreenhouseFronts.git
   cd GreenhouseFronts
   ```

3. **Sync Dependencies**
   ```bash
   # Gradle will download all dependencies automatically
   ./gradlew build
   ```

4. **Verify Setup**
   ```bash
   # Run tests to ensure everything works
   ./gradlew test
   ```

#### IDE Setup (Optional but Recommended)

**IntelliJ IDEA / Android Studio:**
1. Open project folder
2. Wait for Gradle sync
3. Select run configuration for your target platform
4. Click Run ▶️

---

### 🔧 Configuration

#### Environment Selection

The app can connect to two environments:

| Environment | URL | Purpose |
|------------|-----|---------|
| **DEV** 🧪 | `https://inverapi-dev.apptolast.com` | Development and testing |
| **PROD** 🚀 | `https://inverapi-prod.apptolast.com` | Production use |

**To switch environments:**

Edit `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/util/Environment.kt`:

```kotlin
companion object {
    val current: Environment = DEV  // Change to PROD for production
}
```

#### API Endpoints

The app uses these endpoints:

**📥 GET Recent Messages**
```
GET /api/greenhouse/messages/recent
```
Retrieves the latest sensor readings from greenhouses.

**📤 POST Publish Message**
```
POST /api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0
```
Publishes a control message to the greenhouse via MQTT.

**🔌 WebSocket Connection**
```
WS /gs-guide-websocket
STOMP Subscribe: /topic/greenhouse-messages
```
Real-time sensor updates via WebSocket/STOMP.

#### Yarn Lock Files (Web Development)

**Important:** When adding or updating npm-based dependencies (like Krossbow for WebSockets), you must manually update yarn.lock files:

```bash
# After dependency changes, run:
./gradlew kotlinUpgradeYarnLock        # For JS target
./gradlew kotlinWasmUpgradeYarnLock    # For WebAssembly target
```

**Why?** This ensures build reproducibility and lets you review dependency changes before committing.

---

### 💻 API Reference

#### Greenhouse Message Format

```json
{
  "timestamp": "2025-11-13T10:30:00.000Z",
  "sensor01": 23.5,
  "sensor02": 45.2,
  "setpoint01": 25.0,
  "setpoint02": 50.0,
  "setpoint03": 18.0,
  "greenhouseId": "GH-001",
  "rawPayload": "..."
}
```

#### Example API Calls

**Using curl:**

```bash
# Get recent messages
curl -X GET "https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent"

# Publish a setpoint
curl -X POST "https://inverapi-dev.apptolast.com/api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0" \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2025-11-13T10:30:00.000Z",
    "setpoint01": 26.0,
    "greenhouseId": "GH-001"
  }'
```

**Using JavaScript (fetch):**

```javascript
// Get recent messages
const response = await fetch('https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent');
const messages = await response.json();
console.log(messages);

// Publish a setpoint
await fetch('https://inverapi-dev.apptolast.com/api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    timestamp: new Date().toISOString(),
    setpoint01: 26.0,
    greenhouseId: 'GH-001'
  })
});
```

**Using Python (requests):**

```python
import requests
from datetime import datetime

# Get recent messages
response = requests.get('https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent')
messages = response.json()
print(messages)

# Publish a setpoint
payload = {
    'timestamp': datetime.utcnow().isoformat() + 'Z',
    'setpoint01': 26.0,
    'greenhouseId': 'GH-001'
}
response = requests.post(
    'https://inverapi-dev.apptolast.com/api/mqtt/publish/custom',
    params={'topic': 'GREENHOUSE/RESPONSE', 'qos': 0},
    json=payload
)
```

#### Swagger Documentation

Interactive API documentation available at:
- **DEV**: https://inverapi-dev.apptolast.com/swagger-ui/index.html
- **PROD**: https://inverapi-prod.apptolast.com/swagger-ui/index.html

---

### 🏛️ Architecture

#### Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI Framework** | Compose Multiplatform 1.9.3 | Cross-platform declarative UI |
| **Language** | Kotlin 2.2.21 | Type-safe, modern language |
| **Architecture** | MVVM | Separation of concerns |
| **HTTP Client** | Ktor 3.3.2 | Network requests |
| **WebSocket** | Krossbow 9.3.0 | Real-time STOMP connection |
| **Serialization** | kotlinx.serialization | JSON parsing |
| **Async** | Kotlin Coroutines | Asynchronous operations |
| **State Management** | StateFlow | Reactive state updates |

#### Project Structure

```
GreenhouseFronts/
├── composeApp/              # Main application code
│   ├── src/
│   │   ├── commonMain/      # Shared code (all platforms)
│   │   │   ├── kotlin/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/          # Data classes
│   │   │   │   │   ├── remote/         # API & WebSocket clients
│   │   │   │   │   └── repository/     # Repository implementations
│   │   │   │   ├── domain/
│   │   │   │   │   └── repository/     # Repository interfaces
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── ui/             # Compose UI components
│   │   │   │   │   └── viewmodel/      # ViewModels
│   │   │   │   └── util/               # Utilities
│   │   ├── androidMain/     # Android-specific code
│   │   ├── iosMain/         # iOS-specific code
│   │   ├── jvmMain/         # Desktop-specific code
│   │   ├── jsMain/          # JavaScript-specific code
│   │   └── wasmJsMain/      # WebAssembly-specific code
├── iosApp/                  # iOS app wrapper
├── gradle/                  # Gradle configuration
└── build.gradle.kts         # Build configuration
```

#### Multiplatform Approach

**The Expect/Actual Pattern:**

Think of it like **ordering from a restaurant with regional menus**:
- **expect**: "I want a hot beverage" (common interface)
- **actual**: US gets coffee, UK gets tea, India gets chai (platform-specific implementation)

Example in our code:
```kotlin
// commonMain - the "order"
expect fun getCurrentTimestamp(): String

// androidMain - Android's way
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}

// iosMain - iOS's way
actual fun getCurrentTimestamp(): String {
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
```

---

### 🧪 Testing

#### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific platform
./gradlew :composeApp:testDebugUnitTest        # Android
./gradlew :composeApp:iosSimulatorArm64Test    # iOS
./gradlew :composeApp:jvmTest                  # Desktop
```

#### Test Structure

Tests are located in `composeApp/src/commonTest/kotlin/` for shared tests and platform-specific test folders for platform-specific tests.

---

### 🐳 Deployment

#### Building for Production

**Android APK:**
```bash
./gradlew :composeApp:assembleRelease
# Output: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**iOS App:**
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select "Generic iOS Device"
3. Product → Archive
4. Distribute App

**Desktop JAR:**
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
# Output: composeApp/build/compose/binaries/main/app/
```

**Web (Static Files):**
```bash
# WebAssembly build
./gradlew :composeApp:wasmJsBrowserDistribution
# Output: composeApp/build/dist/wasmJs/productionExecutable/

# JavaScript build
./gradlew :composeApp:jsBrowserDistribution
# Output: composeApp/build/dist/js/productionExecutable/
```

#### Cloud Deployment Options

**Web App → Static Hosting:**
- **GitHub Pages**: Free, simple for open source
- **Netlify/Vercel**: Easy CI/CD integration
- **AWS S3 + CloudFront**: Scalable, professional
- **Firebase Hosting**: Fast global CDN

**Android → Play Store:**
1. Create signed APK/Bundle
2. Upload to Google Play Console
3. Configure store listing
4. Submit for review

**iOS → App Store:**
1. Archive in Xcode
2. Upload to App Store Connect
3. Configure app metadata
4. Submit for review

**Desktop → Distribution:**
- **Direct Download**: Host installers on your website
- **Package Managers**: Homebrew (macOS), Chocolatey (Windows), apt/snap (Linux)

---

### 🔍 Troubleshooting

#### Common Issues

**Problem:** Build fails with "yarn.lock was changed"
```bash
# Solution: Update yarn.lock files
./gradlew kotlinUpgradeYarnLock
./gradlew kotlinWasmUpgradeYarnLock
```

**Problem:** WebSocket won't connect
- Check network connectivity
- Verify API endpoint is reachable
- Check firewall settings
- App will fall back to HTTP polling automatically

**Problem:** "Unable to resolve dependency"
```bash
# Solution: Clear Gradle cache and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

**Problem:** iOS build fails
- Ensure Xcode is installed and up to date
- Run `pod install` in iosApp directory
- Check iOS deployment target in Xcode settings

**Problem:** Out of memory during build
- Increase Gradle memory in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4g
  ```

#### Getting Help

- **Documentation**: Check `CLAUDE.md` for developer guidelines
- **Issues**: Report bugs on [GitHub Issues](https://github.com/apptolast/GreenhouseFronts/issues)
- **API Docs**: Use Swagger UI for API questions

---

### 🤝 Contributing

We welcome contributions! Here's how to get started:

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
   - Follow existing code style
   - Add tests for new features
   - Update documentation
4. **Test thoroughly**
   ```bash
   ./gradlew test
   ./gradlew build
   ```
5. **Commit your changes**
   ```bash
   git commit -m "Add amazing feature"
   ```
6. **Push to your fork**
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open a Pull Request**

#### Code Guidelines

- **Comments**: Write code comments in English
- **UI Strings**: User-facing text in Spanish for target audience
- **Architecture**: Follow MVVM pattern
- **Testing**: Add tests for new features
- **Documentation**: Update README if adding major features

---

### 📄 License

This project is proprietary software developed by AppToLast.

**Copyright © 2025 AppToLast. All rights reserved.**

---

### 👥 Team

Developed with ❤️ by the AppToLast team

**Repository**: [github.com/apptolast/GreenhouseFronts](https://github.com/apptolast/GreenhouseFronts)

---

<a name="español"></a>
## 🇪🇸 Español

### 📖 Tabla de Contenidos
- [🎯 ¿Qué es Esto?](#qué-es-esto)
- [✨ Características](#características)
- [🏗️ Cómo Funciona](#cómo-funciona)
- [🚀 Inicio Rápido](#inicio-rápido)
- [📦 Instalación](#instalación-es)
- [🔧 Configuración](#configuración-es)
- [💻 Referencia de API](#referencia-de-api)
- [🏛️ Arquitectura](#arquitectura-es)
- [🧪 Pruebas](#pruebas)
- [🐳 Despliegue](#despliegue-es)
- [🔍 Solución de Problemas](#solución-de-problemas)
- [🤝 Contribuir](#contribuir-es)
- [📄 Licencia](#licencia-es)

---

### 🎯 ¿Qué es Esto?

**Piensa en GreenhouseFronts como el panel de control digital de tu invernadero** - ¡como el tablero de mando de una nave espacial, pero para tus plantas! 🌿

Imagina que tienes un invernadero lleno de plantas que necesitan cuidado constante. No puedes estar allí 24/7 revisando termómetros y ajustando controles, ¿verdad? Ahí es donde entra GreenhouseFronts. Es como tener un asistente inteligente que:

- **Te muestra datos en tiempo real** de los sensores de tu invernadero (temperatura, humedad, etc.) - como consultar tu móvil para ver el clima
- **Se actualiza instantáneamente** cuando las condiciones cambian - piensa en ello como tener una transmisión en vivo en lugar de revisar capturas cada pocos minutos
- **Te permite ajustar la configuración remotamente** - como usar la aplicación de tu casa inteligente para ajustar el termostato, pero para tu invernadero

**Para los técnicos:** Esta es una aplicación Kotlin Multiplatform usando Compose Multiplatform que funciona en Android, iOS, Escritorio y Web. Se conecta a una API backend para monitorear sensores de invernaderos y controlar configuraciones ambientales en tiempo real vía protocolos HTTP y WebSocket/STOMP.

---

### ✨ Características

#### 🌡️ Monitoreo de Sensores en Tiempo Real
**Qué hace:** Muestra lecturas en vivo de los sensores de tu invernadero  
**Analogía:** Como tener un tablero en tu coche que actualiza constantemente tu velocidad, nivel de combustible y temperatura del motor

- Ver valores actuales de sensores (sensor01, sensor02)
- Ver configuraciones de setpoints (setpoint01, setpoint02, setpoint03)
- Monitorear invernadero específico por ID
- Actualizaciones automáticas cada pocos segundos

#### ⚡ Actualizaciones Instantáneas vía WebSocket
**Qué hace:** Los datos se actualizan inmediatamente cuando los sensores detectan cambios  
**Analogía:** Piensa en ello como un chat grupal donde todos ven los mensajes al instante, vs. actualizar tu correo cada minuto

- Conexión WebSocket STOMP en tiempo real
- Reconexión automática si se cae la conexión
- Indicador de estado de conexión (conectado/desconectado)
- Vuelve a HTTP polling si WebSocket falla

#### 🎛️ Control Remoto
**Qué hace:** Cambia la configuración del invernadero desde cualquier lugar  
**Analogía:** Como usar el control remoto de tu TV, pero para el control climático de tu invernadero

- Ajustar setpoints remotamente
- Publicar comandos vía MQTT
- Retroalimentación inmediata sobre cambios exitosos
- Validación de entrada para prevenir errores

#### 🌐 Funciona en Todas Partes
**Qué hace:** Usa la misma aplicación en cualquier dispositivo  
**Analogía:** Como Netflix - mismo contenido, funciona en tu teléfono, tablet, computadora o TV inteligente

- **Android**: Aplicación móvil nativa
- **iOS**: Aplicación nativa para iPhone/iPad
- **Escritorio**: Aplicación para Windows, macOS, Linux
- **Web**: Funciona en cualquier navegador moderno (¡no requiere instalación!)

---

### 🏗️ Cómo Funciona

Piensa en esta aplicación como un **restaurante con un sistema de pedidos sofisticado**:

#### 🏢 La Arquitectura (Analogía del Restaurante)

```
┌─────────────────────────────────────────────────────────────┐
│  🖥️ Interfaz (Tu Mesa/Menú)                                  │
│  Donde ves la información y haces pedidos                    │
│  Construida con Compose Multiplatform - Material Design 3   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  📋 ViewModel (El Mesero)                                    │
│  Toma tus solicitudes, recuerda tus pedidos, te actualiza   │
│  Gestiona el estado, maneja acciones del usuario            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  🗄️ Repository (El Gerente de Cocina)                        │
│  Coordina entre diferentes fuentes de datos                 │
│  Decide si usar datos frescos o datos en caché              │
└────────────────────┬────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
┌─────────────────────┐  ┌──────────────────────┐
│  📡 Servicio API     │  │  🔌 Cliente WebSocket│
│  (Pedidos Regulares) │  │  (Actualizaciones)   │
│  Llamadas HTTP REST  │  │  Protocolo STOMP     │
│  Cliente Ktor        │  │  Librería Krossbow   │
└──────────┬───────────┘  └──────────┬───────────┘
           │                         │
           └────────┬────────────────┘
                    ↓
    ┌───────────────────────────────┐
    │  ☁️ API Backend               │
    │  (La Cocina)                  │
    │  inverapi-dev.apptolast.com   │
    │  inverapi-prod.apptolast.com  │
    └───────────────────────────────┘
```

#### 🔄 Flujo de Datos (Cómo Funcionan los Pedidos)

1. **Quieres ver datos del sensor** (Cliente mira el menú)
   - La interfaz muestra estado de carga
   - ViewModel pide datos al Repository

2. **Repository obtiene los datos** (Gerente consulta con cocina)
   - Primero intenta conexión WebSocket (canal en tiempo real)
   - Vuelve a solicitud HTTP si WebSocket no disponible
   - Servicio API hace la solicitud real

3. **Los datos regresan** (La comida llega a la mesa)
   - Repository recibe mensajes del invernadero
   - ViewModel procesa y actualiza el estado de la UI
   - La interfaz se actualiza automáticamente con nuevos datos

4. **Cambias un setpoint** (Personalizas tu pedido)
   - La interfaz envía tu entrada al ViewModel
   - ViewModel valida el valor
   - Repository publica vía MQTT a través de la API
   - Aparece mensaje de éxito cuando se confirma

#### 🧩 Patrón MVVM Explicado

**En lugar de:** "Patrón de arquitectura Modelo-Vista-VistaModelo"

**Piensa en ello como:** Un modelo de servicio de restaurante:
- **Modelo** (Datos): Los ingredientes y recetas (GreenhouseMessage, valores de sensores)
- **Vista** (UI): El área de comedor donde los clientes ven e interactúan (UI Compose)
- **ViewModel** (Servicio): El mesero que toma pedidos, gestiona solicitudes y trae actualizaciones (GreenhouseViewModel)

El ViewModel es como un mesero que:
- Recuerda lo que pediste (gestión de estado)
- Revisa el estado de tu comida (carga datos)
- Te trae actualizaciones sin que las pidas (StateFlow reactivo)
- Toma nuevos pedidos cuando estás listo (maneja acciones del usuario)

---

### 🚀 Inicio Rápido

**¡Pon la aplicación en marcha en menos de 5 minutos!**

#### Requisitos Previos
- **Java Development Kit (JDK)**: Versión 17 o superior
- **Para Android**: Android Studio o Android SDK
- **Para iOS**: macOS con Xcode instalado
- **Para Escritorio/Web**: ¡Solo el JDK!

#### Opción 1: Escritorio (¡La más rápida!)

```bash
# Clonar el repositorio
git clone https://github.com/apptolast/GreenhouseFronts.git
cd GreenhouseFronts

# Ejecutar la aplicación de escritorio
./gradlew :composeApp:run

# ¡Eso es todo! La aplicación se abrirá en una ventana 🎉
```

#### Opción 2: Navegador Web

```bash
# Para navegadores modernos (WebAssembly - más rápido)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Para navegadores antiguos (JavaScript - más compatible)
./gradlew :composeApp:jsBrowserDevelopmentRun

# El navegador se abrirá automáticamente en http://localhost:8080
```

#### Opción 3: Android

```bash
# Construir el APK
./gradlew :composeApp:assembleDebug

# O abrir en Android Studio y hacer clic en Ejecutar ▶️
```

#### Opción 4: iOS

```bash
# Abrir el proyecto iOS en Xcode
open iosApp/iosApp.xcodeproj

# Hacer clic en Ejecutar ▶️ en Xcode
```

**Usuarios de Windows:** Reemplaza `./gradlew` con `gradlew.bat`

---

### 📦 Instalación {#instalación-es}

#### Requisitos del Sistema

| Plataforma | Versión Mínima | Recomendado |
|----------|----------------|-------------|
| **Android** | API 24 (Android 7.0) | API 36 (Android 15) |
| **iOS** | iOS 13.0 | iOS 17.0+ |
| **Escritorio** | Java 17 | Java 21 |
| **Web** | Navegadores modernos | Chrome 119+, Firefox 120+, Safari 17+ |

#### Configuración del Entorno de Desarrollo

1. **Instalar JDK 17+**
   ```bash
   # Verificar tu versión de Java
   java -version
   
   # Debe mostrar versión 17 o superior
   ```

2. **Clonar Repositorio**
   ```bash
   git clone https://github.com/apptolast/GreenhouseFronts.git
   cd GreenhouseFronts
   ```

3. **Sincronizar Dependencias**
   ```bash
   # Gradle descargará todas las dependencias automáticamente
   ./gradlew build
   ```

4. **Verificar Configuración**
   ```bash
   # Ejecutar pruebas para asegurar que todo funciona
   ./gradlew test
   ```

#### Configuración del IDE (Opcional pero Recomendado)

**IntelliJ IDEA / Android Studio:**
1. Abrir carpeta del proyecto
2. Esperar sincronización de Gradle
3. Seleccionar configuración de ejecución para tu plataforma objetivo
4. Hacer clic en Ejecutar ▶️

---

### 🔧 Configuración {#configuración-es}

#### Selección de Entorno

La aplicación puede conectarse a dos entornos:

| Entorno | URL | Propósito |
|---------|-----|-----------|
| **DEV** 🧪 | `https://inverapi-dev.apptolast.com` | Desarrollo y pruebas |
| **PROD** 🚀 | `https://inverapi-prod.apptolast.com` | Uso en producción |

**Para cambiar de entorno:**

Edita `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/util/Environment.kt`:

```kotlin
companion object {
    val current: Environment = DEV  // Cambiar a PROD para producción
}
```

#### Endpoints de API

La aplicación usa estos endpoints:

**📥 GET Mensajes Recientes**
```
GET /api/greenhouse/messages/recent
```
Recupera las últimas lecturas de sensores de los invernaderos.

**📤 POST Publicar Mensaje**
```
POST /api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0
```
Publica un mensaje de control al invernadero vía MQTT.

**🔌 Conexión WebSocket**
```
WS /gs-guide-websocket
STOMP Subscribe: /topic/greenhouse-messages
```
Actualizaciones de sensores en tiempo real vía WebSocket/STOMP.

#### Archivos Yarn Lock (Desarrollo Web)

**Importante:** Al agregar o actualizar dependencias basadas en npm (como Krossbow para WebSockets), debes actualizar manualmente los archivos yarn.lock:

```bash
# Después de cambios de dependencias, ejecuta:
./gradlew kotlinUpgradeYarnLock        # Para target JS
./gradlew kotlinWasmUpgradeYarnLock    # Para target WebAssembly
```

**¿Por qué?** Esto asegura reproducibilidad de compilación y te permite revisar cambios de dependencias antes de hacer commit.

---

### 💻 Referencia de API

#### Formato de Mensaje del Invernadero

```json
{
  "timestamp": "2025-11-13T10:30:00.000Z",
  "sensor01": 23.5,
  "sensor02": 45.2,
  "setpoint01": 25.0,
  "setpoint02": 50.0,
  "setpoint03": 18.0,
  "greenhouseId": "GH-001",
  "rawPayload": "..."
}
```

#### Ejemplos de Llamadas a la API

**Usando curl:**

```bash
# Obtener mensajes recientes
curl -X GET "https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent"

# Publicar un setpoint
curl -X POST "https://inverapi-dev.apptolast.com/api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0" \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2025-11-13T10:30:00.000Z",
    "setpoint01": 26.0,
    "greenhouseId": "GH-001"
  }'
```

**Usando JavaScript (fetch):**

```javascript
// Obtener mensajes recientes
const response = await fetch('https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent');
const messages = await response.json();
console.log(messages);

// Publicar un setpoint
await fetch('https://inverapi-dev.apptolast.com/api/mqtt/publish/custom?topic=GREENHOUSE/RESPONSE&qos=0', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    timestamp: new Date().toISOString(),
    setpoint01: 26.0,
    greenhouseId: 'GH-001'
  })
});
```

**Usando Python (requests):**

```python
import requests
from datetime import datetime

# Obtener mensajes recientes
response = requests.get('https://inverapi-dev.apptolast.com/api/greenhouse/messages/recent')
messages = response.json()
print(messages)

# Publicar un setpoint
payload = {
    'timestamp': datetime.utcnow().isoformat() + 'Z',
    'setpoint01': 26.0,
    'greenhouseId': 'GH-001'
}
response = requests.post(
    'https://inverapi-dev.apptolast.com/api/mqtt/publish/custom',
    params={'topic': 'GREENHOUSE/RESPONSE', 'qos': 0},
    json=payload
)
```

#### Documentación Swagger

Documentación interactiva de la API disponible en:
- **DEV**: https://inverapi-dev.apptolast.com/swagger-ui/index.html
- **PROD**: https://inverapi-prod.apptolast.com/swagger-ui/index.html

---

### 🏛️ Arquitectura {#arquitectura-es}

#### Stack Tecnológico

| Capa | Tecnología | Propósito |
|-------|-----------|-----------|
| **Framework UI** | Compose Multiplatform 1.9.3 | UI declarativa multiplataforma |
| **Lenguaje** | Kotlin 2.2.21 | Lenguaje moderno con tipos seguros |
| **Arquitectura** | MVVM | Separación de responsabilidades |
| **Cliente HTTP** | Ktor 3.3.2 | Solicitudes de red |
| **WebSocket** | Krossbow 9.3.0 | Conexión STOMP en tiempo real |
| **Serialización** | kotlinx.serialization | Análisis JSON |
| **Async** | Kotlin Coroutines | Operaciones asíncronas |
| **Gestión de Estado** | StateFlow | Actualizaciones de estado reactivas |

#### Estructura del Proyecto

```
GreenhouseFronts/
├── composeApp/              # Código principal de la aplicación
│   ├── src/
│   │   ├── commonMain/      # Código compartido (todas las plataformas)
│   │   │   ├── kotlin/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/          # Clases de datos
│   │   │   │   │   ├── remote/         # Clientes API & WebSocket
│   │   │   │   │   └── repository/     # Implementaciones de Repository
│   │   │   │   ├── domain/
│   │   │   │   │   └── repository/     # Interfaces de Repository
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── ui/             # Componentes UI Compose
│   │   │   │   │   └── viewmodel/      # ViewModels
│   │   │   │   └── util/               # Utilidades
│   │   ├── androidMain/     # Código específico de Android
│   │   ├── iosMain/         # Código específico de iOS
│   │   ├── jvmMain/         # Código específico de Escritorio
│   │   ├── jsMain/          # Código específico de JavaScript
│   │   └── wasmJsMain/      # Código específico de WebAssembly
├── iosApp/                  # Wrapper de aplicación iOS
├── gradle/                  # Configuración de Gradle
└── build.gradle.kts         # Configuración de compilación
```

#### Enfoque Multiplataforma

**El Patrón Expect/Actual:**

Piensa en ello como **pedir en un restaurante con menús regionales**:
- **expect**: "Quiero una bebida caliente" (interfaz común)
- **actual**: EE.UU. obtiene café, Reino Unido obtiene té, India obtiene chai (implementación específica de plataforma)

Ejemplo en nuestro código:
```kotlin
// commonMain - el "pedido"
expect fun getCurrentTimestamp(): String

// androidMain - forma de Android
actual fun getCurrentTimestamp(): String {
    return Clock.System.now().toString()
}

// iosMain - forma de iOS
actual fun getCurrentTimestamp(): String {
    val formatter = NSISO8601DateFormatter()
    return formatter.stringFromDate(NSDate())
}
```

---

### 🧪 Pruebas

#### Ejecutar Pruebas

```bash
# Ejecutar todas las pruebas
./gradlew test

# Ejecutar pruebas para plataforma específica
./gradlew :composeApp:testDebugUnitTest        # Android
./gradlew :composeApp:iosSimulatorArm64Test    # iOS
./gradlew :composeApp:jvmTest                  # Escritorio
```

#### Estructura de Pruebas

Las pruebas se encuentran en `composeApp/src/commonTest/kotlin/` para pruebas compartidas y en carpetas de pruebas específicas de plataforma para pruebas específicas de plataforma.

---

### 🐳 Despliegue {#despliegue-es}

#### Construir para Producción

**APK de Android:**
```bash
./gradlew :composeApp:assembleRelease
# Salida: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**Aplicación iOS:**
1. Abrir `iosApp/iosApp.xcodeproj` en Xcode
2. Seleccionar "Generic iOS Device"
3. Producto → Archivar
4. Distribuir Aplicación

**JAR de Escritorio:**
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
# Salida: composeApp/build/compose/binaries/main/app/
```

**Web (Archivos Estáticos):**
```bash
# Compilación WebAssembly
./gradlew :composeApp:wasmJsBrowserDistribution
# Salida: composeApp/build/dist/wasmJs/productionExecutable/

# Compilación JavaScript
./gradlew :composeApp:jsBrowserDistribution
# Salida: composeApp/build/dist/js/productionExecutable/
```

#### Opciones de Despliegue en la Nube

**Aplicación Web → Hosting Estático:**
- **GitHub Pages**: Gratis, simple para código abierto
- **Netlify/Vercel**: Fácil integración CI/CD
- **AWS S3 + CloudFront**: Escalable, profesional
- **Firebase Hosting**: CDN global rápido

**Android → Play Store:**
1. Crear APK/Bundle firmado
2. Subir a Google Play Console
3. Configurar listado de la tienda
4. Enviar para revisión

**iOS → App Store:**
1. Archivar en Xcode
2. Subir a App Store Connect
3. Configurar metadatos de la aplicación
4. Enviar para revisión

**Escritorio → Distribución:**
- **Descarga Directa**: Alojar instaladores en tu sitio web
- **Gestores de Paquetes**: Homebrew (macOS), Chocolatey (Windows), apt/snap (Linux)

---

### 🔍 Solución de Problemas

#### Problemas Comunes

**Problema:** La compilación falla con "yarn.lock was changed"
```bash
# Solución: Actualizar archivos yarn.lock
./gradlew kotlinUpgradeYarnLock
./gradlew kotlinWasmUpgradeYarnLock
```

**Problema:** WebSocket no se conecta
- Verificar conectividad de red
- Verificar que el endpoint de API sea alcanzable
- Verificar configuración de firewall
- La aplicación volverá automáticamente a HTTP polling

**Problema:** "Unable to resolve dependency"
```bash
# Solución: Limpiar caché de Gradle y recompilar
./gradlew clean
./gradlew build --refresh-dependencies
```

**Problema:** La compilación de iOS falla
- Asegurar que Xcode esté instalado y actualizado
- Ejecutar `pod install` en el directorio iosApp
- Verificar objetivo de despliegue de iOS en configuración de Xcode

**Problema:** Falta de memoria durante la compilación
- Aumentar memoria de Gradle en `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4g
  ```

#### Obtener Ayuda

- **Documentación**: Consulta `CLAUDE.md` para pautas de desarrollo
- **Issues**: Reporta errores en [GitHub Issues](https://github.com/apptolast/GreenhouseFronts/issues)
- **Documentación API**: Usa Swagger UI para preguntas sobre la API

---

### 🤝 Contribuir {#contribuir-es}

¡Bienvenidas las contribuciones! Así es como empezar:

1. **Hacer fork del repositorio**
2. **Crear una rama de funcionalidad**
   ```bash
   git checkout -b feature/funcionalidad-increible
   ```
3. **Hacer tus cambios**
   - Seguir el estilo de código existente
   - Agregar pruebas para nuevas funcionalidades
   - Actualizar documentación
4. **Probar exhaustivamente**
   ```bash
   ./gradlew test
   ./gradlew build
   ```
5. **Hacer commit de tus cambios**
   ```bash
   git commit -m "Agregar funcionalidad increíble"
   ```
6. **Push a tu fork**
   ```bash
   git push origin feature/funcionalidad-increible
   ```
7. **Abrir un Pull Request**

#### Pautas de Código

- **Comentarios**: Escribir comentarios de código en inglés
- **Strings de UI**: Texto para usuarios en español para la audiencia objetivo
- **Arquitectura**: Seguir patrón MVVM
- **Pruebas**: Agregar pruebas para nuevas funcionalidades
- **Documentación**: Actualizar README si agregas funcionalidades importantes

---

### 📄 Licencia {#licencia-es}

Este proyecto es software propietario desarrollado por AppToLast.

**Copyright © 2025 AppToLast. Todos los derechos reservados.**

---

### 👥 Equipo

Desarrollado con ❤️ por el equipo de AppToLast

**Repositorio**: [github.com/apptolast/GreenhouseFronts](https://github.com/apptolast/GreenhouseFronts)

---

## 🙏 Agradecimientos / Acknowledgments

- **JetBrains** - Por Kotlin y Compose Multiplatform
- **Ktor** - Por el excelente cliente HTTP multiplataforma
- **Krossbow** - Por la implementación STOMP/WebSocket
- **Comunidad de código abierto** - Por todas las increíbles bibliotecas y herramientas

---

**¿Preguntas? ¿Problemas? ¿Sugerencias?** Abre un issue en GitHub o contacta al equipo de AppToLast.

**Made with 🌱 for greenhouse automation**
