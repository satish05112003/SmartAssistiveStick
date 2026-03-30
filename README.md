# Smart Assistive Stick

A smart Bluetooth-enabled assistive device that combines location tracking, voice commands, emergency SMS alerts, and real-time monitoring for enhanced personal assistance.

## Features

- **Bluetooth Connectivity**: Seamless pairing with external Bluetooth devices
- **Location Tracking**: Real-time GPS location tracking and sharing
- **Voice Commands**: Hands-free voice interaction support
- **Emergency Alerts**: Quick SMS alerts to configured contacts
- **Google Maps Integration**: Interactive map view for location monitoring
- **Firebase Integration**: Real-time database for data synchronization
- **Jetpack Compose UI**: Modern Android UI framework

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Jetpack components
- **Database**: Room, Firebase Realtime Database
- **Authentication**: Firebase Authentication
- **Location Services**: Google Play Services
- **Build System**: Gradle (Kotlin DSL)

## Project Structure

```
SmartAssistiveStick/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/smartassistivestick/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json (add manually)
├── build.gradle.kts
├── local.properties
├── settings.gradle.kts
└── .gitignore
```

## Setup Instructions

### Prerequisites

- Android SDK 34 or higher
- Gradle 8.1 or higher
- Google account for Firebase
- Google Maps API key

### 1. Clone the Repository

```bash
git clone https://github.com/satish05112003/SmartAssistiveStick.git
cd SmartAssistiveStick
```

### 2. Add Google Maps API Key

Edit `local.properties` and add your API key:

```properties
sdk.dir=/path/to/android/sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
```

### 3. Add Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Download `google-services.json`
4. Place it at `app/google-services.json`:

```bash
cp /path/to/google-services.json app/
```

### 4. Build and Run

```bash
# Build the project
./gradlew build

# Run on emulator or device
./gradlew installDebug
```

## Obtaining API Keys

### Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable Google Maps Android API
4. Create an Android API key with your app's SHA-1 fingerprint
5. Add the key to `local.properties`

### Firebase Setup

1. Visit [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add Android app to the project
4. Download and place `google-services.json`
5. Enable required services (Authentication, Realtime Database, Firestore)

## Security Notes

⚠️ **Important**: 
- Never commit API keys or secrets to version control
- `google-services.json` is in `.gitignore` for security
- `local.properties` contains sensitive data and is not committed
- Always use environment-specific configurations

## Building for Release

```bash
# Create release build
./gradlew assembleRelease

# Find APK at: app/build/outputs/apk/release/app-release.apk
```

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add new feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a pull request

## License

This project is proprietary and confidential. All rights reserved.

## Support

For issues and questions, please open an issue on GitHub.
