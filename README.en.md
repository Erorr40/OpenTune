# OpenTune

<div align="center">
  <img src="https://raw.githubusercontent.com/Erorr40/OpenTune/master/fastlane/metadata/android/en-US/images/featureGraphic.png" alt="OpenTune Banner" width="100%"/>
  
  ### Advanced YouTube Music Client with Material Design 3 for Android
  
  [![Latest Release](https://img.shields.io/github/v/release/Erorr40/OpenTune?style=flat-square&logo=github&color=0D1117&labelColor=161B22)](https://github.com/Erorr40/OpenTune/releases)
  [![Version](https://img.shields.io/badge/Version-4.0.5-6366F1.svg?style=flat-square&logo=android&labelColor=161B22)](https://github.com/Erorr40/OpenTune/releases/tag/v4.0.5)
  [![XERJ Corpus Hub](https://img.shields.io/badge/XERJ%20Hub-Reference%20Solution-09B6A2.svg?style=flat-square&logo=rust&labelColor=161B22)](https://github.com/xerj-org/xerj)
  [![License](https://img.shields.io/github/license/Erorr40/OpenTune?style=flat-square&logo=gnu&color=2B3137&labelColor=161B22)](https://github.com/Erorr40/OpenTune/blob/master/LICENSE)
  [![Platform](https://img.shields.io/badge/Platform-Android%208.0+-3DDC84.svg?style=flat-square&logo=android&logoColor=white&labelColor=161B22)](https://www.android.com)
  [![Stars](https://img.shields.io/github/stars/Erorr40/OpenTune?style=flat-square&logo=github&color=yellow&labelColor=161B22&cacheSeconds=21600)](https://github.com/Erorr40/OpenTune/stargazers)
  [![Forks](https://img.shields.io/github/forks/Erorr40/OpenTune?style=flat-square&logo=github&color=blue&labelColor=161B22&cacheSeconds=21600)](https://github.com/Erorr40/OpenTune/network/members)
</div>

---

## Legal Notice & DMCA Compliance

> **Important Legal Disclaimer**:
> 
> OpenTune is an independent, non-commercial, open-source Android client designed for personal use, interoperability, and study under fair use doctrines.
> 
> - **No Hosting of Copyrighted Content**: OpenTune does **not** host, store, stream, pirate, or redistribute any audio, video, or copyright-protected media files on any server.
> - **Client-Side Interoperability**: The application acts strictly as an interoperable player interface communicating with public web endpoints on behalf of the user.
> - **Trademark Notice**: YouTube, YouTube Music, Google, Spotify, Discord, and Android are registered trademarks of their respective owners. OpenTune is not affiliated with, sponsored by, or endorsed by Google LLC, Alphabet Inc., Spotify AB, or Discord Inc.
> - **DMCA Compliance**: If you are a copyright owner or an agent thereof and believe that any content referenced in this repository infringes upon your copyrights, please contact the repository maintainers directly or submit an issue before taking formal action.

---

## Table of Contents

- [Legal Notice & DMCA Compliance](#legal-notice--dmca-compliance)
- [Overview](#overview)
- [What's New in v4.0.5](#whats-new-in-v405)
- [What's New in v4.0.4](#whats-new-in-v404)
- [Key Features](#key-features)
  - [Equal-Power Mixing & Automix](#-equal-power-mixing--automix)
  - [Liquid Frosted Glass Visuals](#-liquid-frosted-glass-visuals)
  - [High Performance & 120Hz Smoothness](#-high-performance--120hz-smoothness)
  - [Core Audio & Streaming](#-core-audio--streaming)
  - [Synchronized Lyrics & Romanization](#-synchronized-lyrics--romanization)
  - [Audio Processing & Effects](#-audio-processing--effects)
  - [Smart Ecosystem](#-smart-ecosystem)
  - [In-App Auto-Updates](#-in-app-auto-updates)
- [Technology Stack](#technology-stack)
- [Installation & Assets](#installation--assets)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [Support & Donations](#-support--donations--ادعم-المشروع)
- [Original Author & Rights](#original-author--rights)
- [Contact](#contact)
- [License](#license)

---

## Overview

**OpenTune** is a modern, lightweight, and privacy-friendly open-source client for YouTube Music built with Android Jetpack Compose and Material Design 3 Expressive. It delivers a fast, ad-free listening experience with background playback, synchronized lyrics, offline downloads, audio normalization, and seamless library management.

### Key Highlights

- **Ad-free Listening**: Seamless playback without advertising interruptions
- **Spotify-Style Automix**: Equal-power crossfading preventing volume dips between songs
- **Liquid Frosted Glass**: Real-time translucent backdrop blur with specular edge highlights
- **Background & Lockscreen Playback**: Keep listening with your screen off or while multitasking
- **Privacy First**: No telemetry, tracking, or user data collection
- **Material You Dynamic Theming**: Fully dynamic themes adapting in real-time to album artwork
- **Offline Mode**: Cache and download songs with embedded metadata and high-res artwork
- **High Audio Fidelity**: Opus audio streaming support and ReplayGain volume normalization

---

## What's New in v4.0.5

### 🎮 Official Discord Social SDK Integration & Fallback Resilience
- **Official Discord Login**: Integrated and configured the official Discord Social SDK with registered OAuth2 Client ID for seamless, tokenless Rich Presence authentication.
- **Graceful Unconfigured Fallback**: Resolves [Issue #4](https://github.com/Erorr40/OpenTune/issues/4) where missing SDK binaries or client IDs caused a persistent, dead-end error on prebuilt releases.
- **1-Tap Legacy Switch**: Added automatic detection that indicates when the proprietary SDK is unavailable in a build and provides a direct, 1-tap button to use the working Legacy (WebView) login.
- **Improved Error Guidance**: Clear, actionable error messaging explaining the exact missing components for self-builders.

---
## What's New in v4.0.4

### 📱 Full Responsive Layout & Dynamic Font Scaling
- **Extreme Font Size Resilience**: System font scaling up to maximum/largest settings without distorted layouts, clipped text, or overlapping elements.
- **Scroll Unlocked Across All Menus**: Unlocked full vertical scrolling across all 10 bottom sheet menu dialogs and views in portrait mode (`SongMenu`, `AlbumMenu`, `ArtistMenu`, `PlaylistMenu`, `LyricsMenu`, `SelectionSongsMenu`, etc.).
- **Auto-Wrapping Dialog Actions**: Replaced rigid horizontal button rows in dialogs with auto-wrapping `FlowRow` to ensure buttons remain reachable and unclipped.
- **Dynamic Flexible Heights**: Replaced fixed heights with flexible `heightIn(min = ...)` across list items, action grids, and the miniplayer.

### 💎 Liquid Glass Topbar Artifact Fix
- **Eliminated White Inset Artifact**: Resolved the white band artifact appearing above the top bar under the dark Liquid Glass theme when system display or font scale is small.
- **Safe Drawing & Decor Insets**: Unified window background handling in night mode and clamped app bar scroll behavior height offsets to prevent downward gaps.

### 🛡️ Infinite Constraint Resilience & Compose Architecture
- **Eliminated Infinite Constraint Crash**: Solved the nested scrolling crash in `ExpressiveListBottomSheet` by eliminating conflicting `verticalScroll` modifiers and unifying item rendering under an optimized `LazyColumn`.
- **Architectural Verification Test Suite**: Added automated invariant tests in `ExpressiveListPreferenceTest` to guard against nested scroll regressions and ensure Compose layout stability.
- **AI Reference Implementation**: Selected and indexed by the [XERJ Corpus Hub](https://github.com/xerj-org/xerj) as a standard reference solution for AI coding agents handling Jetpack Compose layout constraints.

### 🖥️ Tablet & Foldable Form Factor Adaptations
- **Ergonomic Playback Controls**: Clamped player control button sizing (`coerceIn(48.dp, 80.dp)`) with centered max-width bounds (`540.dp`) to prevent oversized controls on tablets.
- **Adaptive Multi-Column Grids**: Dynamic 2-to-6 column layouts on wide displays and tablets for playlist shortcuts and library collections.

---

## What's New in v4.0.3

### 🎚️ Spotify-Style Equal-Power Mixing Mode
- **Equal-Power Trigonometric Curve**: Replaced linear crossfading with an equal-power sinusoidal curve ($\sin^2(\theta) + \cos^2(\theta) = 1$) to eliminate perceived acoustic volume dips between outgoing and incoming tracks.
- **Continuous 0–12s Squiggly Slider**: Ultra-fluid slider with haptic feedback ticks for micro-precision duration adjustment.
- **5 Quick-Select Preset Pills**: Instant switching between **Off**, **3s Subtle**, **6s Standard**, **9s Club**, and **12s Flow**.
- **Smart Automix Engine**: Dynamic DJ-style auto-mixing intelligently bridging consecutive songs.
- **Album Gapless Playback Bypass**: Intelligently disables crossfading when consecutive tracks belong to the same album, preserving live and conceptual album transitions as intended by the artist.

### 💎 Liquid Frosted Glass UI Enhancements
- **Real-Time Translucency**: High-performance backdrop sampling that blurs and tints underlying artwork dynamically.
- **Specular Border Contouring**: 1dp translucent edge contours reflecting light naturally across sheet headers and cards.
- **Robust Lens Guards**: Protected `CornerBasedShape` transformations to ensure zero visual artifacts across rectangular backdrops.
- **Floating Navigation Pill**: Frosted glass surface styling applied to the main floating navigation toolbar.

### ⚡ Performance & Fluid 120Hz Scrolling
- **Coil 3 Image Engine**: Memory cache upgraded to 25% JVM heap allocation with strong LRU references, delivering instantaneous artwork loading without garbage collection stutters.
- **Stable Collection Keys**: Integrated explicit, unique item keys across all `LazyColumn`, `LazyRow`, and `LazyVerticalGrid` feeds (Explore, Playlists, Library, and Player components) for butter-smooth 120Hz scroll performance.

### 🏷️ Modernized UI Terminology
- Completely refreshed UI strings and setting descriptors across all player menus, sheets, and preferences to reflect modern audio engineering standards.

---

## Key Features

### 🎚️ Equal-Power Mixing & Automix
| Capability | Details |
|---|---|
| **Constant Energy Curve** | Sinusoidal equal-power curve maintaining 100% acoustic energy throughout transitions |
| **Duration Range** | Continuous adjustment from 0 to 12 seconds in 1-second increments |
| **Quick Presets** | One-tap selection: `Off`, `3s Subtle`, `6s Standard`, `9s Club`, `12s Flow` |
| **Smart Automix** | Background automation dynamically tailoring transition timing |
| **Gapless Album Detection** | Automatically bypasses mixing when playing albums gaplessly |

### 💎 Liquid Frosted Glass Visuals
| Component | Visual Treatment |
|---|---|
| **Player Action Sheets** | Translucent tinted backdrop with specular contour highlight |
| **Floating Nav Toolbar** | Circular frosted pill surface floating cleanly above content |
| **Adaptive Theme Creator** | Dynamic palette extraction harmonizing with active album colors |

### ⚡ High Performance & 120Hz Smoothness
| Layer | Optimization |
|---|---|
| **Image Loading** | Coil 3 with 25% JVM heap LRU bitmap cache and disk caching |
| **Compose Feeds** | Explicit keys on every lazy item eliminating redundant recompositions |
| **Memory Footprint** | Zero socket leaks, singleton HTTP pooling, and strict daemon optimization |

### 🎵 Core Audio & Streaming
| Feature | Description |
|---|---|
| **Ad-free Playback** | Uninterrupted audio streaming without advertisements |
| **Background Audio** | Continuous background service with full MediaSession controls |
| **Smart Search** | Fast multi-category search for songs, videos, albums, artists, and playlists |
| **Account Integration** | Secure sign-in to sync YouTube Music playlists and library |
| **Offline Cache** | Download songs locally with embedded metadata, ID3 tags, and album art |

### 🎤 Synchronized Lyrics & Romanization
| Feature | Description |
|---|---|
| **Real-Time Sync** | Word-by-word and line-by-line synchronized scrolling lyrics via LRCLIB & BetterLyrics |
| **Phonetic Romanization** | Automatic romanization for Japanese (Romaji/Hiragana/Katakana), Chinese (Pinyin), Korean (Hangul), and Russian (Cyrillic) |
| **Translation Engine** | In-app lyric translations powered by Together AI |

### 🎛️ Audio Processing & Effects
| Feature | Description |
|---|---|
| **Audio Normalization** | ReplayGain volume leveling preventing sudden loudness spikes |
| **Skip Silence** | High-precision silence detection skipping pauses between tracks |
| **Tempo & Pitch Control** | Independent adjustment of playback tempo (0.5x–2.0x) and musical pitch |
| **Native Equalizer** | Direct access to system audio equalizer and DSP presets |

### 🚗 Smart Ecosystem
| Feature | Description |
|---|---|
| **Android Auto** | Dedicated car dashboard interface with driver-safe media controls |
| **Discord Rich Presence** | Live Discord RPC showing active song, artist, album, and playback timestamps |
| **Last.fm Scrobbling** | Built-in Last.fm scrobbling integration |

### 🔄 In-App Auto-Updates
| Feature | Description |
|---|---|
| **Direct Download** | In-app download with live progress bar without opening external browsers |
| **Zero-Mismatch Upgrade** | Signed with the official release keystore ensuring smooth upgrade over existing installs |

---

## Technology Stack

<div align="center">

| Frontend & UI | Audio & Playback | Architecture & Data |
|:---:|:---:|:---:|
| **Jetpack Compose 1.8** | **AndroidX Media3 (ExoPlayer)** | **Clean Architecture & MVI** |
| Material 3 Expressive | Custom Equal-Power Audio Sink | Kotlin Coroutines & Flow |
| Liquid Frosted Glass Engine | Ktor HTTP Client (Engine CIO) | Room Database |
| Coil 3 Image Loader | Opus / AAC Audio Decoding | Kotlinx Serialization |

</div>

---

## Installation & Assets

### Official Release Downloads (v4.0.5)

| Package | Architecture | Description | Download | SHA-256 Checksum |
|---|---|---|---|---|
| **Universal APK** | All devices (arm64, armeabi-v7a, x86, x86_64) | Compatible with any Android device | [app-universal-release.apk](https://github.com/Erorr40/OpenTune/releases/download/v4.0.5/app-universal-release.apk) | `6775e42cb744c2d8e3d04f7785ef982f88082f1d2df0508aaf09eb0fbe94ec03` |
| **ARM64 APK** | `arm64-v8a` | Optimized, smaller binary for modern 64-bit Android phones | [app-arm64-release.apk](https://github.com/Erorr40/OpenTune/releases/download/v4.0.5/app-arm64-release.apk) | `3a0cd8954b3002e4e79b669bd546e7e7b5cefa8dbe81dfad01382158713826f0` |

### Installation Instructions
1. Download the desired APK from the table above or the [Releases](https://github.com/Erorr40/OpenTune/releases) page.
2. If prompted by Android, grant permission to "Install unknown apps" for your browser or file manager.
3. Tap the downloaded file to complete the installation.
4. *Existing users:* You can update directly over your previous installation without losing playlists, settings, or offline cache.

### Automated Updates via Obtainium
You can install OpenTune and receive seamless background update notifications directly from our GitHub Releases using [Obtainium](https://github.com/ImranR98/Obtainium):

[![Add to Obtainium](https://img.shields.io/badge/Obtainium-Add%20to%20Obtainium-107C41?style=for-the-badge&logo=android&logoColor=white)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.Arturo254.opentune%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FErorr40%2FOpenTune%22%2C%22author%22%3A%22Erorr40%22%2C%22name%22%3A%22OpenTune%22%7D)

*Or manually add the repository URL in Obtainium:* `https://github.com/Erorr40/OpenTune`

---

## Building from Source

### Prerequisites
- **JDK 21** (Eclipse Adoptium or OpenJDK 21)
- **Android SDK** (API 36, NDK 27+)
- **Git**

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Erorr40/OpenTune.git
cd OpenTune

# Build Universal Release APK
./gradlew assembleUniversalRelease

# Build ARM64 Release APK
./gradlew assembleArm64Release

# Run unit and stress tests
./gradlew testUniversalDebugUnitTest
```

Compiled APK binaries are located at:
- `app/build/outputs/apk/universal/release/app-universal-release.apk`
- `app/build/outputs/apk/arm64/release/app-arm64-release.apk`

---

## Contributing

Contributions, bug reports, and suggestions are welcome!

1. Check existing issues or submit a new one on [GitHub Issues](https://github.com/Erorr40/OpenTune/issues).
2. Fork the repository: `https://github.com/Erorr40/OpenTune`.
3. Create a descriptive feature branch (`git checkout -b feature/amazing-feature`).
4. Commit your changes (`git commit -m 'feat: add amazing feature'`).
5. Push to the branch (`git push origin feature/amazing-feature`).
6. Open a Pull Request.

Please review our [Contributing Guidelines](CONTRIBUTING.md), [Code of Conduct](CODE_OF_CONDUCT.md), and [Security Policy](SECURITY.md).

---

## ❤️ Support & Donations / ادعم المشروع

If you enjoy using **OpenTune** and want to support ongoing development, continuous updates, and testing:

### 🇪🇬 Support from Egypt (الدعم من داخل مصر):

| Method / وسيلة الدفع | Account / الحساب | How to Send / طريقة الإرسال |
|:---|:---|:---|
| **InstaPay (إنستاباي)** | `erorr404` | Send via InstaPay app (IPA / Username: `erorr404`) |
| **Vodafone Cash (فودافون كاش)** | `01067671979` | Transfer to wallet number (`01067671979`) |

> [!NOTE]
> Every contribution directly supports development time, testing on new Android versions, and keeping OpenTune 100% free, ad-free, and open-source for everyone! Thank you for your support ❤️

---

## Lineage, Copyright & Open Source Attribution

OpenTune is an open-source collaborative project built upon the foundation of incredible open-source developers under the **GNU General Public License v3.0 (GPL-3.0)**:

* **ArchiveTune**: Core upstream project created and copyrighted by [Rukamori (@rukamori)](https://github.com/rukamori) ([ArchiveTuneApp/ArchiveTune](https://github.com/ArchiveTuneApp/ArchiveTune)), providing Spotify integration, Canvas models, and playback error handling.
* **InnerTune**: Foundational YouTube Music client architecture created and copyrighted by [Zion Huang (@z-huang)](https://github.com/z-huang) ([z-huang/InnerTune](https://github.com/z-huang/InnerTune)).
* **OpenTune Authors & Maintainers**: 
  - [Arturo Cervantes (@Arturo254)](https://github.com/Arturo254): Original OpenTune author and design contributor.
  - [Ahmed Raafat (@Erorr40)](https://github.com/Erorr40): Current project maintainer, Automix engine, performance optimization, and release coordinator.
* **Community & AI Indexing Recognition**:
  - [XERJ Corpus Hub](https://github.com/xerj-org/xerj): OpenTune's Compose constraint architecture fix and invariant test suite were selected and indexed under MIT terms as a benchmark reference solution for AI coding agents.

All original copyright notices from upstream projects are preserved, respected, and acknowledged in full compliance with GPL-3.0. For complete attribution details, see [ATTRIBUTION.md](ATTRIBUTION.md) and [NOTICE](NOTICE).

---

## Contact

For security reports, please review our [Security Policy](SECURITY.md) or contact us directly:
- **Maintainer**: Ahmed Raafat
- **Email**: [ahmedrafatbad666655557777@gmail.com](mailto:ahmedrafatbad666655557777@gmail.com)
- **Telegram Channel**: [https://t.me/E2_opentune](https://t.me/E2_opentune)
- **Instagram**: [https://www.instagram.com/error_1693/](https://www.instagram.com/error_1693/)
- **GitHub**: [https://github.com/Erorr40/OpenTune](https://github.com/Erorr40/OpenTune)

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
See the [LICENSE](LICENSE) file for full details.
