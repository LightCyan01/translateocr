# TranslateOCR

An Android app that captures on-screen text using OCR and translates it in real time, displaying the result as a floating overlay — no copy-pasting required.

---

## Features

- **Real-time screen capture** – Uses the MediaProjection API to grab screen frames on demand via a floating button
- **OCR text extraction** – Extracts text from captured frames with Google ML Kit Text Recognition
- **Multiple translation engines**
  - Google Translate (free tier or bring your own API key)
  - DeepL (free tier or bring your own API key)
  - Offline translation powered by ML Kit on-device models
- **Language selection** – Choose source and target languages; swap them with one tap
- **High-precision mode** – Optional Accessibility Service integration for deeper, hierarchy-level text detection
- **Overlay display** – Translated text is shown in a transparent, adjustable overlay without leaving the current app
- **Translation history** – Browse past translations stored locally and synced to the cloud
- **User accounts** – Firebase Authentication (email/password) with a profile page and statistics (words translated)
- **Model download management** – Background service downloads offline ML Kit translation models as needed

---

## Screenshots

> _Add screenshots here once the app is running on a device or emulator._

---

## Requirements

| Item | Version |
|------|---------|
| Android | 12+ (API 31) |
| Target SDK | 33 |
| Compile SDK | 35 |
| Kotlin | 2.x |
| Gradle | See `gradle/libs.versions.toml` |

---

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/LightCyan01/translateocr.git
cd translateocr
```

### 2. Add a Firebase project

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.jaymie.translateocr`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable **Authentication** (Email/Password) and **Firestore** in the Firebase Console.

### 3. (Optional) Add translation API keys

Open the app, navigate to **Settings**, and enter your Google Translate and/or DeepL API keys. Without keys the app uses the free-tier endpoints.

### 4. Build and install

Open the project in Android Studio, sync Gradle, then run on a device or emulator:

```bash
./gradlew installDebug
```

---

## Permissions

The app requests the following permissions at runtime:

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Draw the overlay and floating button on top of other apps |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Keep screen-capture running in the background |
| `POST_NOTIFICATIONS` | Show a persistent notification while screen capture is active |
| `READ_MEDIA_IMAGES` | Pick images from the gallery for OCR |
| `INTERNET` | Call translation APIs and sync with Firebase |
| `BIND_ACCESSIBILITY_SERVICE` | High-precision text detection (optional, user-enabled) |

---

## Architecture

The app follows **MVVM** and is structured as a single-`Activity` host with Jetpack Navigation fragments.

```
TranslateOCRApp (Application)
│
├── MainActivity  ← Navigation host
│   ├── HomeFragment
│   ├── SettingsFragment
│   ├── HistoryFragment
│   └── ProfileFragment
│
├── Activities
│   ├── Login
│   ├── Profile
│   └── LanguageSelect
│
├── Services
│   ├── ScreenCaptureService   ← MediaProjection foreground service
│   ├── TranslateAccessibilityService
│   └── ModelDownloadService
│
├── Repositories
│   ├── OCRRepository          ← ML Kit text recognition
│   ├── TranslationRepository  ← Google / DeepL / offline
│   ├── FirestoreRepository    ← Cloud sync
│   └── TranslationHistoryRepository
│
└── Utils
    ├── ScreenCaptureManager
    ├── OverlayManager
    └── FloatingButtonManager
```

**Key libraries**

- [Google ML Kit](https://developers.google.com/ml-kit) – OCR & offline translation
- [Firebase](https://firebase.google.com/) – Auth, Firestore, Storage
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Navigation](https://developer.android.com/guide/navigation)
- [PermissionsDispatcher](https://github.com/permissions-dispatcher/PermissionsDispatcher)
- [Glide](https://github.com/bumptech/glide)

---

## License

This project is provided as-is for educational purposes. No license has been specified; all rights remain with the original author.
