# Security Policy

The OpenTune team takes the security of our application and the privacy of our users very seriously. As an open-source, client-side Android music player, OpenTune operates with a strict "privacy-first" philosophy: no telemetry, no remote tracking, and no external data storage.

This document outlines our security policies, supported versions, and how to responsibly report vulnerabilities.

---

## Supported Versions

We provide security updates and patches for the current major release line. We strongly recommend that all users and distributors always run the latest available release.

| Version | Supported | Notes |
|:---|:---:|:---|
| **4.0.x** | :white_check_mark: | Currently active release line (receives active bug fixes & security patches) |
| **< 4.0.0** | :x: | End of Life (no longer supported; please upgrade to v4.0.x) |

---

## Reporting a Vulnerability

If you discover a security vulnerability or privacy concern in OpenTune, **please do NOT create a public GitHub issue**. Disclosing vulnerabilities publicly before a fix is released can put users at risk.

Instead, please use one of the following secure disclosure channels:

### Option 1: GitHub Private Vulnerability Reporting (Recommended)

GitHub provides a confidential channel to report security issues directly to repository maintainers:

1. Navigate to the [OpenTune Security Advisories](https://github.com/Erorr40/OpenTune/security/advisories) tab.
2. Click on **"Report a vulnerability"** (`/security/advisories/new`).
3. Fill out the report form with a detailed summary, severity assessment, and reproduction steps.
4. Maintainers will receive the report privately and collaborate with you on a fix within a private fork before publishing an advisory.

### Option 2: Direct Email to Maintainer

If you cannot use GitHub Security Advisories, send an encrypted or standard email directly to the project maintainer:

- **Maintainer**: Ahmed Raafat
- **Email**: [ahmedrafatbad666655557777@gmail.com](mailto:ahmedrafatbad666655557777@gmail.com)
- **Subject Line**: `[SECURITY] OpenTune Vulnerability Report - <Brief Description>`

---

## What to Include in Your Report

To help us investigate, triage, and resolve the issue quickly, please provide as much relevant information as possible:

1. **Description**: Clear description of the vulnerability and its potential impact.
2. **Affected Version**: Exact OpenTune version(s) and build type (Release / Debug / F-Droid).
3. **Environment**: Android OS version, device model, and ROM / manufacturer (e.g., Pixel / Samsung / LineageOS).
4. **Proof of Concept (PoC)**: Step-by-step reproduction instructions, scripts, or ADB commands.
5. **Sample Payloads or Crash Logs**: Sanitized logcat traces or relevant network interaction traces if applicable.
6. **Suggested Mitigation**: If you know how to fix or mitigate the vulnerability, please share your thoughts or a patch.

> [!NOTE]
> Please ensure you redact any personal API tokens, OAuth credentials, or private information before attaching logs or screenshots.

---

## Response Timeline & SLA

When you submit a vulnerability report:

| Milestone | Expected Timeframe |
|:---|:---|
| **Initial Acknowledgment** | Within **48 hours** |
| **Triage & Validation** | Within **3 to 5 business days** |
| **Fix Development & Testing** | Within **7 to 14 business days** (depending on complexity) |
| **Release & Disclosure** | Coordinated with reporter upon release of the patched APK |

We will keep you informed of our progress throughout the triage and patching process.

---

## Security Scope & Threat Model

OpenTune is a client-side Android application. Our security boundaries and attack surfaces include:

### In-Scope Areas
- **Local Credential Storage**: Secure handling of optional user login tokens (YouTube Music, Spotify, Last.fm) stored in local Android Keystore / encrypted preferences.
- **IPC & Android Components**: Exposed Activities, Broadcast Receivers, Services (`MediaSessionService`, `DownloadService`), and Deeplink intent filters.
- **Network Transport**: Secure HTTPS communication with upstream streaming and metadata APIs (InnerTube, LRCLIB, KuGou, Last.fm, Spotify).
- **Local Data & Caching**: Integrity and sandboxing of downloaded audio files, artwork caches, and SQLite Room databases against unauthorized access from other apps.
- **Dependency Vulnerabilities**: Security flaws in direct Gradle dependencies, ExoPlayer/Media3 libraries, and native binaries.

### Out-of-Scope Areas
- Attacks requiring physical device access with USB Debugging (ADB) unlocked and root privileges.
- Vulnerabilities residing exclusively within third-party external services (e.g. upstream YouTube API outages or changes) unless caused by OpenTune's integration.
- Modified, repacked, or unofficial APK builds distributed outside the official GitHub releases or verified distribution channels.
- Denial of Service caused by exhausting local device storage via legitimate song downloads initiated by the user.

---

## Attribution & Public Recognition

We believe in responsible disclosure and giving credit where credit is due:
- Unless you prefer to remain anonymous, we will gladly credit you in our [Release Notes](https://github.com/Erorr40/OpenTune/releases) and the published GitHub Security Advisory.
- We appreciate the security research community's efforts in keeping open-source mobile software safe and private for everyone.
