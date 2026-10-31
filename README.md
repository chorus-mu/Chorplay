<div align="center">
  <img alt="Chorplay Logo" src="Assets/logo.png" width="200">

  <h1>Chorplay</h1>

  <p>
    <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
    <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
    <a href="License"><img alt="License: GPL-3.0" src="https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=for-the-badge"></a>
  </p>

  <h3>A privacy-first YouTube & Shorts client for Android with on-device recommendations.</h3>

  <p>
    Built from the ground up with <b>Jetpack Compose</b> and <b>Material 3 Expressive</b>.<br>
    Powered by <b>ChorplayNeuroEngine</b> — a private, on-device recommendation algorithm that keeps your viewing habits strictly on your phone.
  </p>

  <p>
    <a href="#-features"><b>Features</b></a> •
    <a href="#-recommendation-engine-chorplayneuro"><b>ChorplayNeuro</b></a> •
    <a href="#-flavors--downloads"><b>Downloads</b></a> •
    <a href="#-building-from-source"><b>Build Guide</b></a> •
    <a href="#-license--copyright"><b>License</b></a>
  </p>
</div>

---

## 💡 Why Chorplay?

Most third-party YouTube clients give you playback, but strip away recommendations entirely. You are forced to choose between the official app (which tracks your every tap, search, and pause) or privacy-preserving alternatives that leave your home feed empty and stagnant.

**Chorplay gives you the best of both worlds.**

With **ChorplayNeuroEngine**, your recommendations are computed 100% on-device. The algorithm studies your viewing habits locally, adapting to your taste across different times of day without sending a single byte of telemetry, history, or identifiers to any remote server. You have complete transparency: inspect what the algorithm has learned, adjust weights, or wipe your profile anytime.

---

## ✨ Features

### 🎬 Video Streaming & Playback Engine
- **ExoPlayer / Media3 Engine**: Butter-smooth playback with adaptive bitrate streaming up to 4K 60fps.
- **Native InnerTube Client**: High-speed, direct stream extraction with robust BotGuard / PoToken handling.
- **SponsorBlock**: Automatically skip sponsored segments, intros, outros, and filler segments.
- **DeArrow**: Replaces clickbait thumbnails and sensationalist titles with community-verified alternatives.
- **Return YouTube Dislike (RYD)**: Restores public dislike counts on all videos.
- **Background Playback**: Continuous background audio playback with screen off and lockscreen media controls.
- **Picture-in-Picture (PiP)**: Keep watching in a floating PiP window or in-app popup player while multitasking.
- **Gesture Controls**: Intuitive vertical swipes for volume and brightness, double-tap to seek.
- **Custom Subtitles**: Full styling controls for subtitle font size, color, background opacity, and timing.
- **Playback Speed Control**: Variable speed adjustment from 0.25x to 2.0x with pitch correction.

### ⚡ Shorts & Enhanced Viewing
- **Dedicated Shorts Feed**: Clean, distraction-minimized vertical video player with auto-scroll and seamless gestures.
- **Multi-Track Audio & Dubbing**: Select alternative and dubbed audio streams directly within the player.
- **Built-in Equalizer**: Audio enhancement processor with custom presets for dialogue clarity and immersive sound.
- **Persistent Mini Player**: Fluid transition between full video player and bottom mini-bar controls.
- **Queue Management**: Intuitive drag-and-drop reordering, repeat modes, and quick video queuing.
- **Hardware Acceleration**: Efficient hardware decoding for maximum battery savings and cooler thermals.

### 🧠 Recommendation Engine (ChorplayNeuro)
- **100% On-Device**: Zero servers, zero telemetry, zero account required.
- **Contextual Intelligence**: Dynamically adjusts recommendations based on weekday vs. weekend patterns and morning vs. evening preferences.
- **Topic Fatigue Mitigation**: Detects when you have consumed too much of a single topic and gently introduces fresh interests.
- **Anti-Echo-Chamber**: Intelligently balances exploitation (familiar content) with exploration (new discoveries).
- **Engagement-Weighted Quality**: Filters out low-effort or low-engagement uploads using view-to-like heuristics.
- **Complete Transparency**: Open the *Your Taste* dashboard to inspect your profile, view topic confidence scores, and export/import your taste profile.

### 📥 High-Performance Downloader
- **Multi-Stream Downloads**: Fast offline video and audio downloads with format selection (MP4, MKV, Opus, WebM).
- **Clean Tagging**: Automatic embedded video metadata (clean title, channel name, release year, high-res thumbnail).
- **Background Downloads**: Resilient background downloading powered by Android foreground services.

### 📚 Library & Offline Media
- **Local History**: Detailed watch and search history kept exclusively in an encrypted local database.
- **Custom Playlists**: Create, edit, and organize local playlists and bookmarked videos.
- **Local Storage Playback**: Play offline video files stored directly on your device storage.
- **NewPipe Data Import**: One-click import for subscriptions, watch history, and playlists exported from NewPipe.

### 🔄 Device-to-Device LAN Sync
- **Local Network Discovery**: Discover and pair devices on the same Wi-Fi network via mDNS.
- **Serverless**: Synchronize playlists, subscriptions, and history directly between your devices without any cloud intermediary.
- **End-to-End Encrypted**: Authenticated pairing with SAS verification and secure transport.

### 🎨 Material 3 Design & Theming
- **Material 3 Expressive**: Modern typography, fluid shape morphing, and native M3 motion schemes.
- **Dynamic Color (Monet)**: Automatically coordinates with your Android system wallpaper on Android 12+.
- **AMOLED Pure Black**: True `#000000` dark theme for OLED power savings.
- **11 Handcrafted Color Themes**: Light, Dark, OLED Black, Ocean Blue, Forest Green, Sunset Orange, Purple Nebula, Midnight Black, Rose Gold, Arctic Ice, and Crimson Red.

---

## 📦 Flavors & Downloads

Chorplay is available in two distinct product flavors:

| Flavor | In-App Updater | Description |
| :--- | :--- | :--- |
| **GitHub** *(Default)* | ✅ Yes | Seamless automatic updates with background download and install prompt. |
| **FOSS** | ❌ No | Fully standalone build without any updater service or network update checks. |

### Architecture Targets

| Architecture | Artifact Name | Target Devices |
| :--- | :--- | :--- |
| **arm64-v8a** *(Recommended)* | `chorplay-arm64-v8a.apk` | Modern 64-bit phones & tablets (most devices from 2017+). |
| **armeabi-v7a** | `chorplay-armeabi-v7a.apk` | Older 32-bit hardware. |
| **Universal** | `chorplay.apk` | Compatible across all supported CPU architectures. |

---

### 🔒 Verifying Authenticity

To verify that an APK has not been modified or tampered with, inspect the signing certificate fingerprint using tools like [AppVerifier](https://github.com/soupslurpr/AppVerifier):

**Official Release Certificate SHA-256 Fingerprint:**
```
43:22:29:4E:D4:CA:A2:D4:29:41:40:09:58:18:08:0F:FE:8A:CC:1F:BE:3C:DC:76:10:7D:F4:5C:52:86:BE:40
```

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17 or 21** (Eclipse Temurin recommended)
- **Android SDK** (`compileSdk 35`, `minSdk 26`)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/chorus-mu/Chorplay.git
cd Chorplay

# Build the GitHub Debug variant (with updater)
./gradlew :app:assembleGithubDebug

# Or build the pure FOSS Debug variant (without updater)
./gradlew :app:assembleFossDebug

# Run unit tests
./gradlew :app:testGithubDebugUnitTest
```

The compiled APKs will be located in:
`app/build/outputs/apk/<flavor>/debug/`

---

## 🏛️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3 Expressive](https://m3.material.io/)
- **Media Engine**: [AndroidX Media3 / ExoPlayer](https://github.com/androidx/media)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) & [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Networking**: [OkHttp](https://square.github.io/okhttp/) & [Ktor Client](https://ktor.io/)
- **Concurrency**: Kotlin Coroutines & `StateFlow` / `SharedFlow`

---

## 🤝 Acknowledgments

Chorplay stands on the shoulders of wonderful open-source projects:

- **[PipePipe](https://codeberg.org/NullPointerException/PipePipe)**: For insights into SABR streaming and InnerTube playback architectures.
- **[NewPipe](https://github.com/TeamNewPipe/NewPipe)**: For pioneering Android YouTube client development.
- **[SponsorBlock](https://sponsor.ajay.app/)** & **[DeArrow](https://dearrow.ajay.app/)**: For crowdsourced segment skipping and clean thumbnail metadata.
- **[Return YouTube Dislike](https://returnyoutubedislike.com/)**: For restoring engagement transparency.
- **[ExoPlayer / Media3](https://github.com/androidx/media)**: Google's modern media playback framework for Android.
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: Modern declarative UI toolkit.
- **[Material Design 3](https://m3.material.io/)**: Next-generation Android design tokens and specifications.

---

## 📜 License & Copyright

**Chorplay** is free software licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [License](License) file for details.

**Copyright: Pushkar (2026) under GPLv3**

> [!IMPORTANT]
> Any project incorporating source code from Chorplay (including the `ChorplayNeuroEngine` algorithm) must also be licensed under the GNU General Public License v3.0 (GPLv3) and made publicly available in source form. Closed-source or proprietary distribution is strictly prohibited.

<div align="center">
  <sub>Crafted with care by <b>Pushkar</b></sub>
</div>
