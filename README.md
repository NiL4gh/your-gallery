# Your Gallery 📸

[![Android](https://img.shields.io/badge/Platform-Android%2014+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Room DB](https://img.shields.io/badge/Storage-Room%20Database-FA7B17?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Web / PWA](https://img.shields.io/badge/Companion-React%2019%20%7C%20Vite%20PWA-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

> A fluid, modern photo and video gallery combining **iOS fluid gestures** with **Android Material 3 precision**. Designed for high refresh rate displays (120Hz/144Hz) with instant in-memory state reactivity, non-destructive editing, and zero bloat.

---

## 🌟 Highlights & Features

### ⚡ 120Hz Gesture-First Interaction
- **Adaptive Column Scaling**: Pinch-to-zoom gesture layer seamlessly shifts photo grid density from **1 to 8 columns**.
- **Dual-State Fullscreen Pager**: Single-finger horizontal swipes navigate between photos natively (`HorizontalPager`), while two-finger pinch-to-zoom (`1.0x` to `5.0x`) smoothly locks page navigation to allow fluid panning within zoomed images.
- **Sub-Pixel Date Fast-Scroller**: Floating date capsule badge (`[ Nov 2024 ]`) tracking precise item offsets without jitter or duplicate scrollbars.

### 🛡️ Non-Destructive Media Management
- **Offline Room Database**: User operations (Favorites, Trash, Custom Albums, Tags, Edits) update an isolated local Room DB (`MediaState`) without altering original file binaries on storage.
- **Instant UI Reactivity**: State mutations update the in-memory `mediaList` synchronously for 0ms lag, offloading SQLite persistence to `Dispatchers.IO`.
- **Soft Recycle Bin**: Trashed items are isolated with timestamped retention before permanent deletion.

### 🎨 Design & Personalization
- **True OLED Dark Mode**: Deep `#000000` dark theme with automatic status bar & navigation bar contrast adjustment (`WindowCompat.getInsetsController`).
- **Dynamic Gallery Personalization**: Customize the gallery title directly from the Settings sheet—name it *"Your Gallery"*, *"Family Photos"*, or your own custom brand.
- **Built-in Photo Editor**: Integrated controls for Brightness, Contrast, Saturation, Warmth, Rotation, and Aspect Ratios.

### 🌐 Cross-Platform Companion (Web & PWA)
- Full-featured **React 19 + TypeScript + Vite PWA** companion app featuring:
  - Client-side IndexedDB persistence (`your-gallery-db`).
  - File System Access API integration for auto-populating local phone folders (DCIM/Pictures).
  - EXIF parser extraction (aperture, focal length, shutter, ISO, camera model).
  - Side-by-side photo comparison modal.

---

## 📱 Architecture & Tech Stack

```
your-gallery/
├── android-native/                  # Native Android Application (Production)
│   ├── app/src/main/java/com/yourgallery/app/
│   │   ├── MainActivity.kt          # Compose UI root, Navigation & App State
│   │   ├── MediaRepository.kt       # MediaStore query engine & Album grouping
│   │   ├── data/                    # Room Database (GalleryDatabase, MediaState)
│   │   └── ui/                      # Jetpack Compose UI Screens & Design System
│   │       ├── PhotosTab.kt         # Masonry grid, fast-scroller, selection mode
│   │       ├── ViewerScreen.kt      # ExoPlayer, pinch-to-zoom, gesture pager
│   │       ├── EditorScreen.kt      # Color matrix adjustments & crop transforms
│   │       ├── AlbumsTab.kt         # Folder & custom album grid
│   │       ├── MemoriesTab.kt       # Curated stories & highlight reels
│   │       └── SearchTab.kt         # Fast client-side metadata & tag filter
│   └── build.gradle.kts             # Gradle build config with decoupled signing
├── src/                             # Companion Web PWA (React 19 + TypeScript)
│   ├── components/                  # Lightbox, Editor, Albums, TopBar
│   ├── utils/storage.ts             # IndexedDB transaction manager
│   └── types/gallery.ts             # Unified media data models
└── release/                         # Signed Release APK ready for sideloading
```

### Key Libraries
- **UI**: Jetpack Compose BOM `2024.10.00`, Material 3, Extended Icons
- **Image Pipeline**: Coil 2.7.0 (`coil-compose`, `coil-video` with `VideoFrameDecoder`)
- **Video Engine**: Jetpack Media3 ExoPlayer 1.4.1
- **Persistence**: AndroidX Room 2.6.1 with KSP (Kotlin Symbol Processing)
- **Web**: React 19, TypeScript 7, Vite 8, Tailwind CSS 4, Motion

---

## 🚀 Getting Started

### 📦 Android App Installation

#### Option 1: Direct APK Download
1. Download `your-gallery-v1.0.0.apk` from the [Releases](https://github.com/your-username/your-gallery/releases) page or `release/` directory.
2. Transfer to your Android device and install (allow *Install from Unknown Sources* if prompted).
3. Grant media permissions on first launch.

#### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/your-username/your-gallery.git
cd your-gallery/android-native

# Build debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

### 💻 Web PWA Companion

```bash
# Navigate to web root
cd your-gallery

# Install dependencies
bun install   # or npm install

# Start local development server
bun run dev   # or npm run dev

# Build production bundle
bun run build # or npm run build
```

---

## 🔒 Security & Privacy

- **100% Offline by Default**: No analytics, telemetry, or remote server uploads.
- **Zero Cloud Storage Dependence**: All thumbnails, metadata, and database records remain strictly on your local device.
- **Decoupled Keystore Secrets**: Keystore paths and passwords are read from `keystore.properties` (gitignored), preventing credential leakage in open-source forks.

---

## 📄 License
This project is open-source and available under the [Apache 2.0 License](LICENSE).
