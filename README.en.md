# OpenTune

<div align="center">
  <img src="https://raw.githubusercontent.com/Erorr40/OpenTune/master/fastlane/metadata/android/en-US/images/featureGraphic.png" alt="OpenTune Banner" width="100%"/>
  
  ### Advanced YouTube Music Client with Material Design 3 for Android
  
  [![Latest Release](https://img.shields.io/github/v/release/Erorr40/OpenTune?style=flat-square&logo=github&color=0D1117&labelColor=161B22)](https://github.com/Erorr40/OpenTune/releases)
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
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**OpenTune** is a modern, lightweight, and privacy-friendly open-source client for YouTube Music built with Android Jetpack Compose and Material Design 3. It delivers a fast, ad-free listening experience with background playback, synchronized lyrics, offline downloads, audio normalization, and seamless library management.

### Key Highlights

- **Ad-free Listening**: Seamless playback without advertising interruptions
- **Background & Lockscreen Playback**: Keep listening with your screen off or while multitasking
- **Privacy First**: No telemetry, tracking, or user data collection
- **Material You Design**: Fully dynamic themes adapting to album art colors
- **Offline Mode**: Cache and download your favorite songs for offline listening
- **High Audio Quality**: Support for Opus audio streaming and replay gain normalization

---

## Key Features

### Core Functionality
| Feature | Description |
|---|---|
| **🎵 Ad-free Playback** | Uninterrupted audio streaming without advertisements |
| **🔄 Background Audio** | Continuous background service with media session controls |
| **🔍 Smart Search** | Fast search for songs, videos, albums, artists, and playlists |
| **👤 Account Integration** | Sign in securely to sync playlists and personal library |
| **📚 Library Management** | Create and organize local and online playlists |
| **📱 Offline Cache** | Download songs with embedded metadata and artwork |

### Audio Enhancements
| Feature | Description |
|---|---|
| **🎤 Synchronized Lyrics** | Real-time synchronized lyrics via LRCLIB and BetterLyrics |
| **⚡ Skip Silence** | Automatically detect and skip silent parts between songs |
| **🔊 Audio Normalization** | Volume leveling across varying track masterings |
| **🎛️ Pitch & Tempo** | Fine-tune playback speed and pitch |
| **🎧 Equalizer** | Native Android audio effects panel integration |

### Design & Connectivity
| Feature | Description |
|---|---|
| **🎨 Material Design 3** | Expressive UI with adaptive color schemes |
| **🚗 Android Auto** | Vehicle dashboard display and controls |
| **💬 Discord RPC** | Optional rich presence support showing current song |
| **🌐 Multi-language** | Localized in over 20 languages |

---

## Technology Stack

<div align="center">

| Frontend | Backend & Networking | Build & Tools |
|:---:|:---:|:---:|
| Jetpack Compose | Ktor Client | Gradle 9 |
| Material 3 Expressive | Kotlinx Serialization | Kotlin 2.3 |
| Coil Image Loader | Room Database | Android NDK / CMake |

</div>

---

## Installation

### GitHub Releases (Recommended)

1. Go to the [Releases](https://github.com/Erorr40/OpenTune/releases) page.
2. Download the latest `app-universal-release.apk` (Version 4.0.1).
3. If prompted, enable "Install unknown apps" for your browser or file manager.
4. Tap the downloaded APK to install.

---

## Building from Source

### Prerequisites

- **JDK 21** (Eclipse Adoptium or OpenJDK 21)
- **Android SDK** (API level 36, NDK 27+)
- **Git**

### Build Steps

```bash
# Clone the repository
git clone https://github.com/Erorr40/OpenTune.git
cd OpenTune

# Build debug APK
./gradlew assembleUniversalDebug

# Build release APK
./gradlew assembleUniversalRelease

# Run unit tests
./gradlew testUniversalDebugUnitTest
```

Compiled APK files are output to `app/build/outputs/apk/`.

---

## Contributing

Contributions, bug reports, and suggestions are welcome!

1. Check existing issues or open a new one in [GitHub Issues](https://github.com/Erorr40/OpenTune/issues).
2. Fork the repository: `https://github.com/Erorr40/OpenTune`.
3. Create a descriptive feature branch (`git checkout -b feature/amazing-feature`).
4. Commit your changes (`git commit -m 'feat: add amazing feature'`).
5. Push to the branch (`git push origin feature/amazing-feature`).
6. Open a Pull Request.

Please review our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Original Author & Rights

* **Original Creator & App Owner**: [Arturo Cervantes (@Arturo254)](https://github.com/Arturo254)
* **Current Maintainer**: [Ahmed Raafat (@Erorr40)](https://github.com/Erorr40)

All original application rights, architecture, UI design, and development of OpenTune belong to [Arturo254](https://github.com/Arturo254) as the original creator and owner of the application. This repository continues the maintenance, compatibility updates, and community support for the project.

---

## Contact

For security reports, inquiries, or support, contact the maintainer at:
- **Email**: [ahmedrafatbad666655557777@gmail.com](mailto:ahmedrafatbad666655557777@gmail.com)
- **GitHub**: [https://github.com/Erorr40/OpenTune](https://github.com/Erorr40/OpenTune)

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
See the [LICENSE](LICENSE) file for the full license text.

