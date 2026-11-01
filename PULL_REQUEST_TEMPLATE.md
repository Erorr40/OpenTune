## Description

<!-- Provide a clear, concise summary of what this pull request changes or accomplishes. -->

---

## Type of Change

<!-- Please mark all relevant options with an 'x' like [x] -->

- [ ] 🐛 **Bug fix**: Non-breaking change that fixes an issue or unexpected behavior
- [ ] ✨ **New feature**: Non-breaking change that adds new functionality or UI capabilities
- [ ] ⚡ **Performance improvement**: Optimization in memory, frame rate (120Hz), caching, or startup time
- [ ] 🎨 **UI/UX polish**: Visual updates, animations, Liquid Frosted Glass styling, Material 3 theming
- [ ] 🎚️ **Audio / Playback Engine**: Changes to ExoPlayer/Media3, Automix, volume normalization, or codecs
- [ ] ♻️ **Refactoring**: Code quality improvements without changing external behavior
- [ ] 📝 **Documentation**: Updates to README, architecture specs, or translation files
- [ ] 🔧 **Build / CI / Dependencies**: Gradle updates, GitHub Actions workflow adjustments, or library bumps

---

## Related Issue(s)

<!-- Link the related issue(s) resolved or impacted by this PR.
     Examples: Closes #123, Fixes #456, Resolves #789 -->

Closes #

---

## Motivation & Context

<!-- Why is this change required? What problem does it solve?
     If discussing design or architectural decisions, please explain your reasoning. -->

---

## Visual Changes / Screenshots (UI PRs)

<!-- If this PR changes any visual components, screens, dialogs, or animations,
     please attach Before & After screenshots or a screen recording (GIF / MP4). -->

| Before | After |
|:---:|:---:|
| <!-- Image / Video --> | <!-- Image / Video --> |

---

## Testing & Verification

<!-- Describe how you verified your changes. -->

- [ ] Tested on physical device: <!-- e.g. Google Pixel 8 Pro (Android 14) -->
- [ ] Tested on Android Emulator: <!-- e.g. API 34 Emulator -->
- [ ] `./gradlew assembleDebug` builds successfully without warnings or errors
- [ ] `./gradlew lint` passes with no new lint errors introduced

---

## Pre-Submission Checklist

- [ ] My code adheres to the project's [Contribution Guidelines](https://github.com/Erorr40/OpenTune/blob/master/CONTRIBUTING.md) and [Code of Conduct](https://github.com/Erorr40/OpenTune/blob/master/CODE_OF_CONDUCT.md).
- [ ] I have performed a self-review of my own code changes.
- [ ] I have commented complex or non-obvious logic (explaining *why*, not *what*).
- [ ] Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) format (`feat:`, `fix:`, `refactor:`, `perf:`, `docs:`, `chore:`).
- [ ] No hardcoded secrets, private API tokens, or temporary debug logs are included.
- [ ] If new user-facing strings were added, they are placed in `res/values/strings.xml` for localization.
