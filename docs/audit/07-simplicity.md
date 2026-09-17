# 7. Simplicity

**Definition for this codebase:** the smallest design that fully works, with no speculative
layers and no "for later" scaffolding left unmarked. Where a deliberate shortcut is taken, it
should be visibly flagged (the audit brief's own convention is a `ponytail:` comment naming
the limit) rather than left indistinguishable from a permanent design decision.

## Findings

### SI1 — High: three fully-built authentication systems exist where the product uses one, with none of them marked as provisional

As detailed in [01-correctness.md](01-correctness.md) C3, `ServiceSyncRepository` contains
complete implementations of password-based local auth, phone-OTP auth, and real Firebase
email/password auth. Building and maintaining three complete systems to serve one live
feature is the opposite of the smallest design that works, and none of the two unused paths
carries any comment indicating they are provisional, planned-for-later, or intentionally kept
in reserve — a future reader has no way to distinguish "this is dead code that should be
deleted" from "this is about to be wired up next sprint" without asking someone.

### SI2 — High: a full custom vector-drawing routine simulates a Google Maps integration that does not exist

`GoogleMapsLiveTrackingCard` (`BookingStatusScreen.kt:744-900`, roughly 155 lines) hand-draws
a street grid, a highway overlay, and an animated glowing polyline using low-level `Canvas`
drawing primitives (`drawLine`, `Path`, `PathEffect`) — a meaningfully complex piece of
custom rendering code — purely to produce the *appearance* of a live map. This is more code,
not less, than either (a) integrating the real Google Maps Compose SDK, or (b) simply
presenting a static "your specialist is on the way" state without a fake map. The complexity
exists to sell an illusion, which is a worse trade than either the simpler honest option or
the more complete real option.

### SI3 — Medium: ~720 lines of hardcoded seed data live inside the repository class instead of a data asset

`getInitialIndianProviders()` (`ServiceSyncRepository.kt:1533-2253`) constructs eighteen-plus
fully-populated `ServiceProvider` objects, each with nested `Review` lists, directly as
Kotlin code inside the class that also owns auth, sync, and business logic. This data has no
behavior — it is inert seed content — and belongs in a JSON/resource asset loaded at
startup, which would let it be edited, localized, or regenerated without touching (or
recompiling) application logic, and would immediately shrink the "God Object" surface area
described in [05-reusability.md](05-reusability.md) RU1 by roughly a third.

### SI4 — Medium: dark/light color selection is done by comparing a resolved `Color` value to a constant instead of using the boolean that's already available

`Color.kt:50-63` — five near-identical properties like:

```kotlin
val StatusPendingBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF382305) else Color(0xFFFEF3C7)
```

`ServiceSyncTheme` (`Theme.kt:59-63`) already computes an explicit `isDark: Boolean` from the
current `AppThemeMode` before ever building a `ColorScheme` — but that boolean is not
threaded anywhere these five properties could read it, so each one independently re-derives
"is this dark mode" by equality-comparing the *current* background `Color` against the
*specific* constant used to build the dark scheme. It happens to work today only because
`DarkColorScheme.background` is assigned exactly `DarkBackground` — a coincidence of the
current values, not an explicit contract. This is solving "is it dark mode" the hard, fragile
way when the easy, robust way (pass or read the boolean) is one call away.

### SI5 — Low: `proguard-rules.pro` is configured but has no effect, because minification is disabled

`app/build.gradle.kts:22-27` sets `isMinifyEnabled = false` for the `release` build type, so
the custom rules in `proguard-rules.pro` (root-level Gson field-keeping rule) are dead
configuration — present, apparently deliberate, but currently inert. This is a small
inconsistency between stated intent (someone wrote a Gson-specific ProGuard rule, implying an
expectation that minification would run) and actual configuration.

## Recommendation summary

| Finding | Action |
|---|---|
| SI1 | Delete the two unused auth paths (preferred, per [01-correctness.md](01-correctness.md) C3), or mark them explicitly with a comment stating they are intentionally provisional and naming what would need to happen to activate them. |
| SI2 | Replace with either a real Maps integration or a materially simpler honest placeholder; do not maintain bespoke rendering code whose only purpose is to imitate a capability the app does not have. |
| SI3 | Move seed data to a bundled JSON asset (`assets/seed_providers.json`) parsed once at startup. |
| SI4 | Thread the existing `isDark: Boolean` from `ServiceSyncTheme` through a `CompositionLocal` (or pass it explicitly) and have the five `*Bg` properties branch on that boolean directly. |
| SI5 | Either enable `isMinifyEnabled = true` for release builds (recommended for a shipping app, see [11-performance-and-cost.md](11-performance-and-cost.md)) or remove the now-purposeless custom ProGuard rule until it is needed. |
