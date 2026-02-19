# AntiAlarm

An Android alarm clock app that forces you to solve math problems before you can dismiss the alarm — making sure you're actually awake.

## Features

- **Multiple Alarms** — Create and manage as many alarms as you need.
- **Math Challenge Dismissal** — Solve a math problem to turn off a ringing alarm. Configurable difficulty (easy / medium / hard).
- **Custom Alarm Sounds** — Pick any audio file from your device storage as the alarm sound.
- **Repeat Days** — Set alarms to repeat on specific days of the week (Mon–Sun).
- **Custom Snooze** — Configure the snooze duration per alarm.
- **Enable / Disable** — Toggle individual alarms on or off without deleting them.
- **Vibration** — Optional vibration when the alarm fires.
- **Survives Reboot** — Alarms are automatically rescheduled after the device restarts.
- **Full-Screen Alarm** — The alarm fires with a full-screen intent, showing on the lock screen and turning the screen on.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| DI | Hilt |
| Database | Room |
| Navigation | Jetpack Navigation Compose |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 16) |

## Project Structure

```
app/src/main/java/com/antialarm/
├── alarm/                  # Alarm scheduling, receiver, service
│   ├── AlarmReceiver.kt
│   ├── AlarmScheduler.kt
│   ├── AlarmSchedulerImpl.kt
│   ├── AlarmService.kt
│   ├── BootReceiver.kt
│   └── MathProblemGenerator.kt
├── data/                   # Persistence layer
│   ├── dao/AlarmDao.kt
│   ├── db/AlarmDatabase.kt
│   ├── model/Alarm.kt
│   └── repository/AlarmRepository.kt
├── di/                     # Hilt modules
│   └── AppModule.kt
├── ui/                     # Compose UI
│   ├── editor/             # Create / edit alarm screen
│   ├── firing/             # Alarm ringing & math challenge screens
│   ├── list/               # Alarm list screen
│   ├── navigation/         # NavGraph
│   └── theme/              # Colors, typography, theme
├── AntiAlarmApp.kt         # Application class
└── MainActivity.kt         # Entry point
```

## Prerequisites

- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 17**
- **Android SDK** with API level 36 installed

## How to Run

### 1. Clone the repository

```bash
git clone <repository-url>
cd antialarm
```

### 2. Open in Android Studio

Open the project root folder (`antialarm/`) in Android Studio. Gradle sync will start automatically.

### 3. Build the project

Via the IDE toolbar click **Build → Make Project**, or from the terminal:

```bash
./gradlew assembleDebug
```

On Windows:

```cmd
gradlew.bat assembleDebug
```

### 4. Run on a device or emulator

1. Connect a physical Android device (USB debugging enabled) **or** start an Android emulator (API 26+).
2. Select the target device in the Android Studio toolbar.
3. Click the **Run ▶** button, or from the terminal:

```bash
./gradlew installDebug
```

### 5. Grant required permissions

On first launch the app will request the following permissions:

| Permission | Why |
|---|---|
| Exact alarms | Required to fire alarms at the precise time |
| Notifications | Show alarm notifications |
| Audio file access | Let you pick custom alarm sounds |

## Building a Release APK

```bash
./gradlew assembleRelease
```

The unsigned APK will be located at `app/build/outputs/apk/release/`. To install on a device you will need to sign the APK with your own keystore.

## License

This project is not yet licensed. Add a `LICENSE` file to specify terms of use.
