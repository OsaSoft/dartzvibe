---
paths:
  - "**/domain/statistics/**"
  - "**/data/model/StatisticsModels.kt"
  - "**/data/model/FixedDecimal.kt"
  - "**/ui/screen/statistics/**"
  - "**/ui/screen/leaderboard/**"
---

# Statistics

- **Statistics are per mode:**
  - `PlayerStatistics` holds the fields every mode shares; rates such as `winRate` are **percentages**. It also holds `modeStats: ModeStatistics?`, which is sealed with one subtype per mode. Each subtype's `primaryMetric` is the leaderboard sort axis.
  - `StatisticsCalculator` dispatches with an exhaustive `when`, and computes everything live from stored throws.
  - Use `FixedDecimal` rather than `Double` for decimals, so JVM and Native produce identical output.
