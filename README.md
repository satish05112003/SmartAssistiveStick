# Smart Assistive Stick System

An end-to-end assistive safety platform that combines hardware sensing, a user mobile app, and a caregiver monitoring app. The goal is to improve safety, navigation support, and emergency response for visually impaired users.

## Why This Project Matters

This project demonstrates practical engineering across embedded systems, Android development, real-time cloud sync, and UX for accessibility-focused use cases.

## System Architecture

The system has three main layers:

1. Hardware Layer
- Smart stick built with Arduino + sensors to detect environment and trigger events.

2. Mobile Apps Layer
- SmartAssistiveStickApp: User-facing Android app for alerts, location, and assistance features.
- Caregiver: Caregiver-facing Android app for live monitoring and emergency follow-up.

3. Cloud Layer
- Firebase Authentication + Realtime Database/Firestore for secure identity and real-time data sync.

## Repository Structure

```text
SmartAssistiveStick/
|- SmartAssistiveStickApp/
|  |- app/
|  |- gradle/
|  |- build.gradle.kts
|  |- settings.gradle.kts
|  `- gradle.properties
|- Caregiver/
|  |- app/
|  |- gradle/
|  |- build.gradle.kts
|  |- settings.gradle.kts
|  `- gradle.properties
`- README.md
```

## Key Features

### SmartAssistiveStickApp (User App)
- Bluetooth communication with stick hardware.
- Voice and audio guidance flow.
- Live location tracking with Google Maps.
- SOS/emergency action support.
- Firebase sync for status and location updates.

### Caregiver App
- Live map view for user location.
- Real-time status monitoring from Firebase.
- Emergency alert visibility for quick response.
- Simple interface for continuous supervision.

## Hardware Setup (Arduino + Sensors)

This section is written as a clear template so recruiters can understand hardware integration quickly.

Typical components:
- Arduino board (Uno/Nano or equivalent)
- Ultrasonic sensor(s) for obstacle detection
- Buzzer / vibration motor for alerts
- Bluetooth module (for app communication)
- Optional GPS module and power management module

Example pin mapping (update as per your final wiring):

| Component | Pin Mapping (Example) |
| --- | --- |
| Ultrasonic TRIG | D9 |
| Ultrasonic ECHO | D10 |
| Buzzer | D6 |
| Vibration Motor (via driver) | D5 |
| Bluetooth TX/RX | D2 / D3 (SoftwareSerial) |

Note:
- Keep this table aligned with your final Arduino sketch for demo consistency.

## Full Working Flow

1. Sensors detect obstacle, movement context, or trigger condition.
2. Arduino processes input and sends event data over Bluetooth.
3. SmartAssistiveStickApp receives event and updates UI/alerts.
4. App pushes key status/location updates to Firebase.
5. Caregiver app reads real-time data and shows alerts/location.
6. Caregiver can react quickly in emergencies using live information.

## Screenshots

Add screenshots here when available:

- User app home screen (SmartAssistiveStickApp)
- User app map and alert screen
- Caregiver live tracking screen
- Caregiver emergency alert screen

Suggested structure:

```text
docs/screenshots/user-home.png
docs/screenshots/user-map.png
docs/screenshots/caregiver-tracking.png
docs/screenshots/caregiver-alert.png
```

## Setup Instructions

### Prerequisites
- Android Studio (latest stable)
- Android SDK 34+
- Firebase project
- Google Maps Android API key

### 1) Clone

```bash
git clone https://github.com/satish05112003/SmartAssistiveStick.git
cd SmartAssistiveStick
```

### 2) Add API key safely (do not commit)

Create/update local properties in each app project:

- SmartAssistiveStickApp/local.properties
- Caregiver/local.properties

Add:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

The manifest already reads:

```xml
android:value="${MAPS_API_KEY}"
```

### 3) Add Firebase config files (do not commit)

Place `google-services.json` in:

- SmartAssistiveStickApp/app/google-services.json
- Caregiver/app/google-services.json

### 4) Build and run

Build each app from its own folder in Android Studio, or run Gradle from each project directory.

## Tech Stack

- Kotlin
- Jetpack Compose
- Android SDK / Google Play Services (Maps, Location)
- Firebase Authentication
- Firebase Realtime Database / Firestore
- Gradle Kotlin DSL
- Arduino (embedded controller)

## Security Practices Used

- No hardcoded API keys in source.
- `local.properties` excluded from version control.
- `google-services.json` excluded from version control.
- Build outputs and IDE files ignored via `.gitignore`.

## Future Improvements

- Add offline-first sync strategy for unstable connectivity.
- Add unit and instrumentation test coverage reports.
- Add CI pipeline for build + lint + security checks.
- Improve multilingual voice guidance.
- Add wearable integration for quicker caregiver notifications.

## Recruiter Note

This project highlights cross-domain capability: embedded hardware integration, mobile app engineering, cloud backend integration, and real-time safety workflows for a meaningful real-world use case.
