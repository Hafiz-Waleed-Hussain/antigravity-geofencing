# Android Evolution: 2010 - 2025

This project demonstrates the evolution of Android Development by implementing the same **Geofenced Alarm** feature using 6 different architectural approaches corresponding to different eras.

## Flavors Overview

| Flavor | Era | Architecture | Async | UI | Geofencing API | Background |
|--------|-----|--------------|-------|----|----------------|------------|
| `mvp` | ~2010-2015 | MVP | `Handler` / `AsyncTask` style | XML | `LocationManager` (Legacy) | `Service` (Legacy) |
| `mvvmLiveData` | ~2017 | MVVM | `LiveData` | XML + DataBinding | Play Services Geofencing | `JobIntentService` (Simulated) |
| `mvi` | ~2018 | MVI | `RxJava 3` | XML | Play Services | Rx Streams |
| `mvvmCoroutines` | ~2020 | MVVM | `Coroutines` | XML + ViewBinding | Play Services | `WorkManager` (Foreground) |
| `mvvmFlow` | ~2023 | MVVM (Clean) | `StateFlow` | Jetpack Compose | Play Services | BroadcastReceiver |
| `machine` | 2025 (AI) | Functional Core / Hexagonal | `Coroutines` | Compose | Play Services (Wrapped) | Decoupled |

## Key Differences & Evolution

### 1. Legacy (MVP)
In the early days, we put logic in Activities or Presenters. We dealt with `LocationManager` directly, handling raw GPS callbacks. `ProximityAlert` was the old way to do geofencing before Play Services. Background work was done in `Service` or `IntentService`, which are now discouraged or restricted.

### 2. The Architecture Components Era (MVVM LiveData)
Google introduced **Architecture Components**. `LiveData` became the standard for observing data. `DataBinding` allowed us to put logic in XML (controversial but popular). Apps became more robust vs memory leaks.

### 3. The Reactive Era (MVI)
RxJava took over for complex async flows. MVI (Model-View-Intent) introduced **Unidirectional Data Flow**, making state predictable. Every action is an `Intent` (not android Intent), and the state is immutable.

### 4. The Kotlin First Era (MVVM Coroutines)
Kotlin Coroutines replaced RxJava for most use cases, offering cleaner imperative-style async code. `ViewBinding` replaced DataBinding/Synthetics. `WorkManager` became the standard for reliable background work.

### 5. The Modern Era (MVVM Flow + Compose)
`StateFlow` (Hot streams) replaced LiveData in the Domain/Data layers. **Jetpack Compose** revolutionized UI, moving away from XML to declarative Kotlin code.

### 6. The Machine Era (AI Optimized)
Designed for readability by AI agents. Code is broken into small, strictly typed interfaces (`GeofenceSystem`, `MachineController`). Logic is pure where possible. Documentation (`KDoc`) explains *intent* rather than just mechanics.

## How to Run
This project uses Gradle Product Flavors. You cannot run "app"; you must select a flavor.

### Build & Install
```bash
./gradlew installMvpDebug
./gradlew installMvvmLiveDataDebug
./gradlew installMviDebug
./gradlew installMvvmCoroutinesDebug
./gradlew installMvvmFlowDebug
./gradlew installMachineDebug
```
