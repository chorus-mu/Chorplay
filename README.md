<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="Assets/white.png">
    <img alt="Chorplay" src="Assets/black.png" width="320">
  </picture>

  <br>

  <p>
    <a href="https://github.com/chorus-mu/Chorplay/releases/latest"><img alt="Latest Release" src="https://img.shields.io/github/v/release/chorus-mu/Chorplay?style=for-the-badge&label=Release&color=007AFF"></a>
    <a href="https://github.com/chorus-mu/Chorplay/releases"><img alt="Total Downloads" src="https://img.shields.io/github/downloads/chorus-mu/Chorplay/total?style=for-the-badge&color=34C759"></a>
    <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
    <a href="License"><img alt="License: GPL-3.0" src="https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=for-the-badge"></a>
    <a href="https://github.com/chorus-mu/Chorplay/stargazers"><img alt="GitHub Stars" src="https://img.shields.io/github/stars/chorus-mu/Chorplay?style=for-the-badge&color=FFCC00"></a>
  </p>

  <h3>A privacy-first YouTube & YouTube Music client for Android with on-device recommendations.</h3>

  <p>
    Built from the ground up with <b>Jetpack Compose</b> and <b>Material 3</b>.<br>
    Powered by <b>ChorplayNeuroEngine</b> — a private, on-device recommendation algorithm that keeps your viewing habits on your phone.
  </p>

  <p>
    <a href="#-download"><b>Download APK</b></a> •
    <a href="#-features"><b>Features</b></a> •
    <a href="#-recommendation-engine-chorplayneuro"><b>ChorplayNeuro</b></a> •
    <a href="#-building-from-source"><b>Build Guide</b></a> •
    <a href="#-support--donations"><b>Donate</b></a> •
    <a href="https://github.com/chorus-mu/Chorplay/discussions"><b>Community</b></a>
  </p>
</div>

---

## 💡 Why Chorplay?

Most third-party YouTube clients give you playback, but strip away recommendations completely. You are forced to choose between the official app (which tracks your every tap and search) or privacy-preserving alternatives that leave your feed empty and stagnant.

**Chorplay gives you the best of both worlds.**

With **ChorplayNeuroEngine**, your recommendations are generated 100% on-device. The algorithm studies your viewing habits locally, adapting to your taste across different times of day without sending a single byte of telemetry or history to any external server. You have full transparency: inspect what the algorithm has learned, adjust weights, or wipe your profile anytime.

---

## ✨ Features

### 🎬 Video Playback & Streaming
- **ExoPlayer / Media3 Engine**: Smooth playback with adaptive bitrate streaming up to 4K 60fps.
- **Native InnerTube Client**: Fast, direct extraction with robust BotGuard/PoToken handling.
- **SponsorBlock**: Automatically skip sponsored segments, intros, outros, and filler.
- **DeArrow**: Replaces clickbait thumbnails and sensationalist titles with community-verified alternatives.
- **Return YouTube Dislike (RYD)**: Restores public dislike counts on all videos.
- **Background Playback**: Continuous background audio playback with screen off and lockscreen media controls.
- **Picture-in-Picture (PiP)**: Keep watching in a floating window while multitasking.
- **Smart TV Casting**: Seamless casting via DLNA / UPnP to smart TVs and networked media players.
- **Gesture Controls**: Intuitive vertical swipes for volume and brightness, double-tap to seek.
- **Custom Subtitles**: Full styling controls for subtitle font size, color, background, and timing.
- **Downloads**: High-speed offline video and audio downloads with VP9, AV1, Opus, and MP4 support.
- **Playback Speed Control**: Variable speed adjustment from 0.25x to 2.0x with pitch correction.

### 🎵 Dedicated Music Hub
- **Immersive Music Experience**: Dedicated music navigation with animated cover art and synchronized palette theming.
- **Synchronized Lyrics**: Time-coded, word-by-word synchronized lyrics display.
- **Persistent Mini Player**: Smooth transition between full-screen playback and persistent bottom mini-bar.
- **Queue Management**: Intuitive drag-and-drop reordering, swipe-to-dismiss, shuffle, and repeat modes.
- **Audio Offload**: Hardware audio offload for maximum battery savings during long listening sessions.
- **Equalizer**: Built-in parametric equalizer support with AutoEQ and EqualizerAPO presets.

### 🧠 Recommendation Engine (ChorplayNeuro)
- **100% On-Device**: Zero servers, zero telemetry, zero account required.
- **Contextual Intelligence**: Dynamically adjusts recommendations based on weekday vs. weekend patterns and morning vs. evening preferences.
- **Topic Fatigue Mitigation**: Detects when you have consumed too much of a single topic and gently introduces fresh interests.
- **Anti-Echo-Chamber**: Intelligently balances exploitation (familiar content) with exploration (new discoveries).
- **Engagement-Weighted Quality**: Filters out low-effort or low-engagement uploads using view-to-like heuristics.
- **Complete Transparency**: Open the *Your Taste* dashboard to inspect your profile, view topic confidence scores, and export/import your taste profile.

### 📚 Library & Offline Media
- **Local History**: Detailed watch and search history kept exclusively in encrypted local database.
- **Custom Playlists**: Create, edit, and organize local playlists and favorites.
- **Local Storage Playback**: Play offline audio and video files stored directly on your device.
- **Shorts Feed**: Clean, distraction-minimized Shorts viewer with bookmarking.
- **NewPipe Data Import**: One-click import for subscriptions and playlists exported from NewPipe.

### 🔄 Device-to-Device LAN Sync
- **Local Network Sync**: Discover and pair devices on the same Wi-Fi network via mDNS.
- **Serverless**: Synchronize playlists and libraries directly between your devices without any cloud intermediary.

### 🎨 Material 3 Design & Theming
- **Material 3 Expressive**: Modern typography, fluid shape morphing, and native M3 motion schemes.
- **Dynamic Color (Monet)**: Automatically coordinates with your Android system wallpaper on Android 12+.
- **AMOLED Pure Black**: True `#000000` dark theme for OLED power savings.
- **11 Handcrafted Color Themes**: Light, Dark, OLED Black, Ocean Blue, Forest Green, Sunset Orange, Purple Nebula, Midnight Black, Rose Gold, Arctic Ice, and Crimson Red.
- **Adaptive Branding**: Header emblem automatically matches system light/dark mode.

---

## 📥 Download

### Stable Releases

<div align="center">
  <a href="https://github.com/chorus-mu/Chorplay/releases/latest"><img alt="Get it on GitHub" src="https://img.shields.io/badge/GitHub_Releases-Download_APK-181717?style=for-the-badge&logo=github"></a>
  &nbsp;
  <a href="https://apt.izzysoft.de/packages/io.github.pushkar.chorplay"><img alt="Get it on IzzyOnDroid" src="https://img.shields.io/badge/IzzyOnDroid-F--Droid_Repo-5B3993?style=for-the-badge&logo=fdroid"></a>
  &nbsp;
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/chorus-mu/Chorplay/"><img alt="Get it on Obtainium" src="https://img.shields.io/badge/Obtainium-Track_Updates-007ACC?style=for-the-badge"></a>
</div>

<br>

#### Which APK should I install?

| Architecture | GitHub Build (with in-app updater) | FOSS Build (without updater) | Description |
| :--- | :--- | :--- | :--- |
| **arm64-v8a** *(Recommended)* | `chorplay-arm64-v8a.apk` | `chorplay-foss-arm64-v8a.apk` | Modern 64-bit phones & tablets (most devices from 2017+). |
| **armeabi-v7a** | `chorplay-armeabi-v7a.apk` | `chorplay-foss-armeabi-v7a.apk` | Older 32-bit hardware. |
| **Universal** | `chorplay.apk` | `chorplay-foss.apk` | Compatible with all devices; larger file size. |

- **GitHub Build**: Includes the seamless in-app updater and Discord Rich Presence integration.
- **FOSS Build**: Pure open-source build without updater libraries, designed for F-Droid / IzzyOnDroid users.

---

### 🧪 Nightly Builds

Bleeding-edge builds compiled automatically from every commit to the `main` branch:

<div align="center">
  <a href="https://github.com/chorus-mu/Chorplay/releases/tag/nightly"><img alt="Try the Nightly Build" src="https://img.shields.io/badge/Nightly_Build-Bleeding_Edge-E0245E?style=for-the-badge&logo=githubactions&logoColor=white"></a>
</div>

> [!NOTE]
> Nightly builds install side-by-side with the stable version as **Chorplay Nightly**, maintaining independent data and their own update channel.

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
- JDK 17 (Eclipse Temurin recommended)
- Android SDK (compileSdk 35, minSdk 26)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/chorus-mu/Chorplay.git
cd Chorplay

# Build the GitHub Debug variant
./gradlew :app:assembleGithubDebug

# Or build the pure FOSS Debug variant
./gradlew :app:assembleFossDebug

# Run unit tests
./gradlew :app:testGithubDebugUnitTest
```

The resulting APKs will be generated in `app/build/outputs/apk/<flavor>/debug/`.

---

<a id="support--donations"></a>
## 💖 Support Development

Chorplay is completely free, ad-free, and open-source software built by an independent developer. Community contributions make ongoing development, bug fixes, and feature additions possible.

If you enjoy using Chorplay, consider supporting development:

### Direct Cryptocurrency Wallets

| Currency | Network | Wallet Address |
| :--- | :--- | :--- |
| **USDT** | TRC-20 (Tron) | `TRz7VDrTWwCLCfQmYBEJakqcZgbFNWfUMP` |
| **Bitcoin (BTC)** | Native SegWit | `bc1qgmkkxxvzvsymtpfazqfl93jw6k4jgy0xmrtnv8` |
| **Ethereum (ETH)** | ERC-20 | `0xfbac6f464fec7fe458e318971a42ba45b305b70e` |
| **Solana (SOL)** | Solana | `7b3SLgiVPb8qQUvERSPGRWoFoiGEDvkFuY98M1GEngug` |
| **Monero (XMR)** | Monero | `8AgaxZnpEvT8VXJpczpL7BQejwSEw97saJmKYqq4zKErbe9bkYSwUhJ813msPPbdYhF11oz4N7tfEj4Zi6k27fKD83ca1if` |

---

## 🤝 Acknowledgments

Chorplay stands on the shoulders of wonderful open-source projects:

- **[PipePipe](https://codeberg.org/NullPointerException/PipePipe)**: For insights into SABR streaming and InnerTube playback architectures.
- **[NewPipe](https://github.com/TeamNewPipe/NewPipe)**: For pioneering Android YouTube client development.
- **[SponsorBlock](https://sponsor.ajay.app/)** & **[DeArrow](https://dearrow.ajay.app/)**: For crowdsourced segment skipping and clean thumbnail metadata.
- **[Return YouTube Dislike](https://returnyoutubedislike.com/)**: For restoring engagement transparency.
- **[ExoPlayer / Media3](https://github.com/androidx/media)**: Google's media playback framework for Android.
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: Modern declarative UI toolkit.
- **[Material Design 3](https://m3.material.io/)**: Next-generation Android design tokens and specifications.

---

## 📜 License & Copyright

**Chorplay** is free software licensed under the **GNU General Public License v3.0 (GPLv3)**.

**Copyright: Pushkar (2026) under GPLv3**

> [!IMPORTANT]
> Any project incorporating source code from Chorplay (including the `ChorplayNeuroEngine` algorithm) must also be licensed under the GNU General Public License v3.0 (GPLv3) and made publicly available in source form. Closed-source or proprietary distribution is strictly prohibited.

---

## 🌟 Star History

<div align="center">
  <a href="https://github.com/chorus-mu/Chorplay/stargazers">
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=chorus-mu/Chorplay&type=Date" width="100%">
  </a>

  <br><br>
  <sub>Crafted with care by <b>Pushkar</b></sub>
</div>
