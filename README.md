# Your Gallery 📸

[![Android](https://img.shields.io/badge/Platform-Android%2014+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Room DB](https://img.shields.io/badge/Storage-Room%20Database-FA7B17?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Web / PWA](https://img.shields.io/badge/Companion-React%2019%20%7C%20Vite%20PWA-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

> **The 100% Offline, Ad-Free Native Gallery Solution for Android.**  
> Combining the signature superpowers of **iPhone Photos** (Years/Months timeline, Ken Burns slideshow), **Samsung Gallery** (Quick Crop on zoom, video swipe gestures, Auto Remaster), and **Honor Gallery** (multi-photo Collage Studio) into a blazing-fast, 120Hz native Jetpack Compose experience.

---

## 💡 Why I Built This (The Problem & The Solution)

### The Problem with Stock Android & The Play Store
Many modern Android phones (including Google Pixels and stock Android devices) ship without a true, offline local gallery app. Instead, users are forced into Google Photos—an app designed around cloud storage subscriptions, continuous backup nag prompts, account sync locks, and slow network-dependent loading just to browse photos already saved on the device.

When searching for alternatives on the Google Play Store, the situation is even worse:
- **Aggressive Full-Screen Ads**: Unclosable 30-second popups interrupting you every few swipes.
- **Predatory Subscriptions**: Charging $4.99/week just for basic editing or trash bin recovery.
- **Invasive Permissions**: Demanding contacts, location, and internet access just to view photos.
- **Sluggish Clunky WebViews**: Stuttering 30fps scrolling that wastes modern 120Hz/144Hz OLED screens.

### The Solution: A True Native Offline Gallery
A friend recently purchased a stock Android device that had no native gallery app and asked for a recommendation. After reviewing dozens of third-party apps and finding zero clean, private options, I decided to engineer the definitive solution: **Your Gallery**.

A gallery built on three non-negotiable engineering principles:
1. **Zero Ads, Zero Cloud, Zero Tracking**: 100% offline. No internet permission in the manifest. Your photos never leave your device.
2. **Flagship Hybrid UX**: Bringing the best signature interaction patterns from iOS, One UI, and MagicOS into one unified Android app.
3. **120Hz Hardware-Accelerated Performance**: Built with modern Jetpack Compose, Room SQLite virtualized state, Coil image engine, and Media3 ExoPlayer for instant 0ms touch response.

---

## 🌟 Signature Hybrid Features

### 👑 Honor Gallery: Multi-Photo Collage Studio
- **Dynamic 2 to 6 Photo Compositor**: Select photos directly in the gallery and launch into a full-featured Collage Studio.
- **Multi-Ratio Framing**: Fluidly switch between Square (`1:1`), Portrait (`4:5`), and Story (`9:16`).
- **Interactive Customization**: Real-time sliders for corner roundness (`0dp`–`32dp`) and cell spacing (`0dp`–`24dp`).
- **High-Resolution Composite Export**: Direct lossless composite rendering to `Pictures/YourGallery` at 95% JPEG quality.

### 🌌 Samsung Gallery: Quick Crop & Video Gestures
- **Instant Floating Quick Crop**: When zooming into any photo past `1.25x`, a floating frosted glass `[ ✂ Quick Crop ]` button animates in. Tapping it unprojects screen coordinates back to source bitmap pixels and exports the exact visible viewport without opening an editor.
- **Video Player Swipe Gestures**:
  - Vertical swipe on the **left half** adjusts screen brightness with a dynamic Sun HUD.
  - Vertical swipe on the **right half** adjusts audio volume with a Speaker HUD.
  - Horizontal swipe scrubs seek position (`±seconds`) with a timestamped preview pill.
  - Double-tap left/right rewinds/fast-forwards by 10s.
- **One-Tap Auto Remaster**: Analyzes and equalizes luminance, contrast, and color vibrancy with a single tap.

### 🍎 iPhone Photos: Segmented Timeline & Slideshow
- **Years & Months Timeline Cards**:
  - **Years**: Dynamic 4-photo montage preview cards with photo counters.
  - **Months**: Curated monthly hero cards with glassmorphic gradients.
  - Tapping any card drills smoothly into that date's grid.
- **Ken Burns Slideshow**: Hands-free auto-advance playback (3.5s per image) with subtle animated zoom (`1.0x -> 1.08x`) and gentle panning.

### ⚡ 120Hz Gesture-First Grid & Fast Scroller
- **Adaptive Column Scaling**: Physical pinch-to-zoom scaling layer toggling grid density from **1 to 8 columns**.
- **Dual-State Fullscreen Pager**: Single-finger horizontal swipes navigate between photos natively (`HorizontalPager`), while two-finger pinch-to-zoom (`1.0x` to `5.0x`) smoothly locks page navigation to allow fluid panning.
- **Sub-Pixel Date Fast-Scroller**: Floating date capsule badge (`[ Nov 2024 ]`) tracking precise item offsets without jitter or duplicate scrollbars.

### 🛡️ Non-Destructive Storage & Privacy Vault
- **Offline Room Database**: Operations (Favorites, Trash, Custom Albums, Tags, Edits) update an isolated local Room DB (`MediaState`) without altering original file binaries.
- **Locked Vault with 4-Digit PIN**: Cryptographically hashed (SHA-256) local Vault lock guarding private media from unauthorized access.
- **Live Media Synchronization**: Automatic background `ContentObserver` reacting immediately when new camera photos, screenshots, or downloads arrive.
- **Instant UI Reactivity**: State mutations update the in-memory `mediaList` synchronously for 0ms visual feedback, offloading SQLite persistence to `Dispatchers.IO`.

---

## 📱 Technical Architecture

```
your-gallery/
├── android-native/                  # Native Android Application (Production)
│   ├── app/src/main/java/com/yourgallery/app/
│   │   ├── MainActivity.kt          # Compose UI root, Navigation & App State
│   │   ├── MediaRepository.kt       # MediaStore query engine & Album grouping
│   │   ├── data/                    # Room Database (GalleryDatabase, MediaState)
│   │   └── ui/                      # Jetpack Compose UI Screens & Design System
│   │       ├── CollageScreen.kt     # Multi-photo collage studio & composite canvas
│   │       ├── ViewerScreen.kt      # ExoPlayer, Quick Crop, video gestures, slideshow
│   │       ├── PhotosTab.kt         # Years/Months timeline, 1-8 col grid, fast-scroller
│   │       ├── EditorScreen.kt      # ColorMatrix adjustments, Auto Remaster, filters
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

### Key Libraries & Components
- **UI Framework**: Jetpack Compose BOM `2024.10.00`, Material 3, Extended Icons
- **Image Pipeline**: Coil 2.7.0 (`coil-compose`, `coil-video` with `VideoFrameDecoder`)
- **Video Engine**: Jetpack Media3 ExoPlayer 1.4.1
- **Persistence**: AndroidX Room 2.6.1 with KSP (Kotlin Symbol Processing)
- **Web Companion**: React 19, TypeScript 7, Vite 8, Tailwind CSS 4, Motion

---

## 🚀 Getting Started

### 📦 Download Pre-Built Release APK
1. Download **`your-gallery-v1.1.0.apk`** from the [GitHub Releases](https://github.com/NiL4gh/your-gallery/releases/tag/v1.1.0) page or directly from the `release/` directory.
2. Transfer the APK to your Android device and install (allow *Install from Unknown Sources* if prompted).
3. Grant storage permissions on first launch.

### 🛠️ Build from Source

```bash
# Clone the repository
git clone https://github.com/NiL4gh/your-gallery.git
cd your-gallery/android-native

# Build debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 🔒 Security & Privacy Guarantee

- **No Internet Permission**: The app manifest does not request `android.permission.INTERNET`. Network communication is physically impossible.
- **100% Offline Storage**: All thumbnails, metadata, and database records remain strictly on your local device.
- **Non-Destructive**: Original file binaries are never altered; all tags and albums are maintained in a local SQLite database.

---

## 📄 License
This project is open-source under the [Apache 2.0 License](LICENSE).
