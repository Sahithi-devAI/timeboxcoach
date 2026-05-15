# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean
./gradlew clean

# Install on connected device
./gradlew installDebug
```

## Architecture Overview

**TimeBoxCoach** is an Android app that uses the Accessibility Service API to block specific apps outside allowed time windows. When a blocked app is detected, an overlay is shown prompting the user to justify the access attempt.

### Core Data Flow

1. `AppBlockerService` (AccessibilityService) receives `TYPE_WINDOW_STATE_CHANGED` events
2. If the foreground app is blocked and outside the allowed time window, it inflates `block_overlay.xml` as a `TYPE_APPLICATION_OVERLAY` window
3. The overlay's "Explain / Request Access" button launches `ReasonActivity` (single-task, non-exported)
4. `ReasonActivity` collects the user's reason, passes it back to `AppBlockerService` via **companion object static fields** (`lastReason`, `lastReasonSubmittedAt`), and logs the entry via `ReasonLogger`
5. `AppBlockerService` re-evaluates: if the reason passes heuristics, it grants a 5-minute access window; otherwise it re-displays the overlay

### Component Responsibilities

| Component | Role |
|---|---|
| `AppBlockerService` | Core loop: detects blocked apps, shows/hides overlay, evaluates reasons, manages access windows |
| `ReasonActivity` | Collects user input, logs via `ReasonLogger`, writes back to `AppBlockerService` static fields |
| `ReasonLogger` | Persists `ReasonLog` entries as JSON to `reason_logs.json` in app-private storage |
| `ReasonLog` | Plain data class for a single logged access attempt |
| `MainActivity` | Placeholder Compose UI (not yet functional) |

### Hardcoded Values (MVP)

- **Blocked app**: `com.instagram.android`
- **Allowed window**: 5:00 PM – 5:30 PM
- **Overlay cooldown**: 3 seconds between overlay displays
- **Temporary access**: 5 minutes after an approved reason
- **Reason heuristics**: minimum 10 chars; rejects: "bored", "just checking", "timepass", "scroll", "nothing", "kill time"
- **Decision**: always written as `"denied"` in `ReasonActivity` regardless of heuristic result — the service decides access independently

### Communication Pattern: Service ↔ Activity

`AppBlockerService` and `ReasonActivity` coordinate via companion object static fields. This is intentional for MVP but fragile (potential race conditions). Any refactor should migrate to `SharedPreferences` or a `ViewModel` backed by a repository.

### Accessibility Service Setup

The service config is in `res/xml/accessibility_service_config.xml`. It listens only to `typeWindowStateChanged` events with a 100ms notification timeout. The service must be manually enabled by the user in Android Settings → Accessibility.

## Build Configuration

- **Min SDK**: 27 (Android 8.1)
- **Compile/Target SDK**: 36 (Android 15)
- **Kotlin**: 2.0.21, JVM target 11
- **UI**: Jetpack Compose with Material3; legacy XML layouts for overlay and reason input
- **Permissions**: `SYSTEM_ALERT_WINDOW` (overlay), `BIND_ACCESSIBILITY_SERVICE` (service)
