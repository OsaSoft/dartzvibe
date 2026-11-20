---
paths:
  - "**/*AppSettings*"
  - "**/data/model/ThemeMode.kt"
  - "**/data/model/SwipeMultiplierMode.kt"
  - "**/ui/screen/settings/**"
---

# App settings

- **New app setting:** a field on `AppSettingsData`, a key plus getter/setter in the `AppSettingsRepository` interface and impl, a control in `SettingsScreen`, and support in `FakeAppSettingsRepository`. The typed `combine` overloads stop at 5 flows, so `getSettings()` combines the first five into `baseSettings()` and layers later settings on with a second `combine` + `copy`; add new ones there. Enum settings are stored by a stable string `value` with a `fromValue` fallback (`ThemeMode`, `SwipeMultiplierMode`), not by name.
