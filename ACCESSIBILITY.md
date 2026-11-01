# Accessibility Statement

The OpenTune project is dedicated to providing a music listening experience that is inclusive, barrier-free, and accessible to everyone, including people with visual, hearing, motor, or cognitive impairments. We believe that access to music, lyrics, and audio customization should be universal.

---

## Our Commitment

We actively work to conform to the **Web Content Accessibility Guidelines (WCAG) 2.1 Level AA** and follow Google's **Android Accessibility Guidelines** across our Jetpack Compose UI architecture.

---

## Accessibility Features & Supported Environments

### 1. Screen Reader Compatibility (Android TalkBack)
- **Comprehensive Semantic Labels**: Interactive UI elements (buttons, sliders, toggles, icons, and menus) are annotated with descriptive `contentDescription` attributes and Compose semantics.
- **State Announcements**: Playback states (Playing, Paused, Buffering), track progress, and repeat/shuffle modes are announced to assistive technologies.
- **Queue & Navigation**: Lists and grids provide clear index semantics and item descriptions for smooth TalkBack traversal.

### 2. Visual & Display Accessibility
- **Dynamic Type & Font Scaling**: All UI text respects Android's system font size and display scale preferences without text truncation or broken container boundaries.
- **Color Contrast**: Text and primary iconography maintain a minimum contrast ratio of 4.5:1 against backgrounds (3:1 for large headings and active player controls).
- **Theme Versatility**: High-contrast dark themes and AMOLED pure-black modes are available for low-vision users and users sensitive to light.
- **Liquid Frosted Glass Adaptability**: Visual blur overlays and specular edges maintain strict readability over varied album art backdrops.

### 3. Motor & Touch Accessibility
- **Minimum Touch Targets**: All interactive controls, navigation tabs, and playback triggers meet or exceed the recommended **48 × 48 dp** touch target requirement.
- **Haptic Feedback**: Subtle haptic clicks accompany slider adjustments, automix duration changes, and button presses to provide tactile confirmation.
- **Single-Gesture Controls**: Navigation and playback require simple taps without requiring complex multi-finger gestures or timed interactions.

### 4. Hearing & Lyrics Accessibility
- **Synchronized Lyrics**: Word-by-word and line-by-line synchronized lyrics via LRCLIB and KuGou provide an accessible visual representation of song vocals.
- **Romanization**: Pinyin, Romaji, and phonetic transliterations help users follow lyrics in unfamiliar scripts.

---

## Known Limitations & Workarounds

While we continuously strive for complete accessibility, some areas may have limitations:

- **Backdrop Blur & Motion Effects**: Dynamic frosted glass blurs and album art transitions can be visually intensive. OpenTune respects Android's system-level **"Remove animations"** setting to minimize motion for motion-sensitive users.
- **Third-Party Lyrics Accuracy**: Some lyrics sourced from community databases may occasionally lack millisecond-accurate sync for assistive speech devices.
- **Custom Visualizers**: Canvas visualizers are purely decorative; all underlying controls remain accessible via standard screen reader focus.

---

## How to Report Accessibility Barriers

If you encounter an accessibility barrier or have suggestions to make OpenTune more accessible, we welcome your feedback:

1. **GitHub Issues**: Open a new issue labeled `accessibility` via [GitHub Issues](https://github.com/Erorr40/OpenTune/issues) using the bug report or feature request template.
2. **Direct Contact**: Email maintainer Ahmed Raafat at [ahmedrafatbad666655557777@gmail.com](mailto:ahmedrafatbad666655557777@gmail.com) with the subject `[ACCESSIBILITY] OpenTune Feedback`.
3. **Telegram Community**: Join our [Telegram Channel](https://t.me/E2_opentune) to discuss accessibility improvements directly with the development team.

When reporting an issue, please specify:
- The device model and Android OS version.
- Any assistive technologies in use (e.g., TalkBack, Switch Access, font enlargement).
- A description of the barrier and what you expected to experience.

We review accessibility feedback promptly and prioritize fixes in upcoming releases.
