# Copilot Instructions for PU850 Android App

## Project Overview

This is an Android application (PU850) built with Java. The app provides indicator functionality with WebSocket communication capabilities.

## Technology Stack

- **Language**: Java
- **Platform**: Android (SDK 24+, Target SDK 33, Compile SDK 34)
- **Build System**: Gradle 8.7.1 with Android Gradle Plugin
- **Architecture**: Single-module Android app
- **Key Libraries**:
  - AndroidX AppCompat, ConstraintLayout, CardView
  - Java-WebSocket for WebSocket communication
  - Persian-Date-Picker-Dialog for date selection

## Project Structure

```
/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── java/com/pandcaspian/indicator/   # Java source files
│   │   │   ├── MainActivity.java             # Main entry point
│   │   │   ├── CommonDefine.java             # Common definitions
│   │   │   └── utils/                        # Utility classes
│   │   │       ├── HttpRequest.java
│   │   │       ├── FileDownloader.java
│   │   │       ├── SocketServer.java
│   │   │       └── SocketClient.java
│   │   └── res/                  # Android resources (layouts, strings, etc.)
│   └── build.gradle              # App-level build configuration
├── build.gradle                  # Project-level build configuration
├── settings.gradle               # Gradle settings
└── .github/
    └── workflows/build.yml       # CI/CD workflow for building APK
```

## Build Commands

Before making changes, ensure you can build and test the project:

```bash
# Grant execute permission to gradlew (if needed)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## Code Style Guidelines

- Follow standard Android/Java coding conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and single-purpose
- Use AndroidX libraries instead of legacy support libraries

## Boundaries and Restrictions

### Never Modify

- `*.jks` or `*.keystore` files (signing keys)
- `local.properties` (local SDK paths)
- Any files containing API keys, secrets, or credentials

### Be Careful With

- `AndroidManifest.xml` - changes can affect app permissions and behavior
- `proguard-rules.pro` - changes can break release builds
- Version codes in `app/build.gradle` - coordinate version changes with releases

## Testing

- Unit tests should be placed in `app/src/test/`
- Instrumented tests should be placed in `app/src/androidTest/`
- Run `./gradlew test` after making changes to verify functionality
- When adding new features, include appropriate test coverage

## CI/CD

The project uses GitHub Actions for continuous integration:
- Workflow file: `.github/workflows/build.yml`
- Triggers on push and pull requests to `main` branch
- Builds debug APK and uploads as artifact
