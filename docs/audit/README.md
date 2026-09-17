# SaServe — 15-Dimension Engineering Audit

Scope: full repository at the time of audit (commit `9a4b5ee`), covering the Android app
(`app/src/main/java/com/servicesync/app/**`), Firebase configuration (`firebase.json`,
`firestore.rules`, `app/google-services.json`), build configuration (`build.gradle.kts`,
`app/build.gradle.kts`, `settings.gradle.kts`), and the orphaned React landing-page sources
(`components/`, `src/components/`).

This audit is documentation only. No source file was modified to produce it.

**Follow-up:** the orphaned React landing-page sources mentioned above got their own deep
audit and rebuild plan — see [../landing-page-rebuild-plan.md](../landing-page-rebuild-plan.md),
tracked in [todo.md](todo.md) Phase 15.

**Revision note:** the first pass of this audit deep-read the highest-risk files
(`ServiceSyncRepository.kt`, both Firebase service classes, `AuthScreen.kt`,
`NotificationHelper.kt`, the manifest, Gradle files, and the theme layer) but only
grep-verified the remaining ~3,800 lines of UI screens. A follow-up pass read every remaining
file in full (`CustomerScreens.kt`, `ServiceCatalogScreen.kt`, `SettingsScreen.kt`,
`FeedbackScreen.kt`, `AddressSetupScreen.kt`, `CommonComponents.kt`, `ApplianceClayHelper.kt`,
`Logo.kt`, `Type.kt`, and the rest of `BookingStatusScreen.kt`) and found several additional,
more severe findings that are now reflected below and in the per-dimension files — most
notably that the wallet's "Add Money via UPI" flow is entirely fabricated. Every dimension
file has been updated in place; nothing from the first pass was found to be inaccurate, only
incomplete.

## How this audit was scoped

SaServe is a single-module Kotlin/Jetpack Compose Android app backed by Firebase
Auth/Firestore, plus a handful of unused React components (see the follow-up above). Each
dimension below is a standard engineering-quality checklist item (correctness, security,
privacy, and so on), with the "what to check" column written specifically against what
actually exists in this repository: Kotlin/Compose conventions, the `ServiceSyncRepository`
data layer, Firestore security rules, Gradle configuration, and the Android manifest.

## Files

| # | File | Dimension |
|---|---|---|
| 1 | [01-correctness.md](01-correctness.md) | Correctness |
| 2 | [02-security.md](02-security.md) | Security |
| 3 | [03-privacy.md](03-privacy.md) | Privacy |
| 4 | [04-reliability.md](04-reliability.md) | Reliability |
| 5 | [05-reusability.md](05-reusability.md) | Reusability |
| 6 | [06-no-duplication.md](06-no-duplication.md) | No duplication |
| 7 | [07-simplicity.md](07-simplicity.md) | Simplicity |
| 8 | [08-readability.md](08-readability.md) | Readability |
| 9 | [09-consistency.md](09-consistency.md) | Consistency |
| 10 | [10-testability.md](10-testability.md) | Testability |
| 11 | [11-performance-and-cost.md](11-performance-and-cost.md) | Performance and cost |
| 12 | [12-observability.md](12-observability.md) | Observability |
| 13 | [13-accessibility-and-ux.md](13-accessibility-and-ux.md) | Accessibility and UX |
| 14 | [14-documentation-and-traceability.md](14-documentation-and-traceability.md) | Documentation and traceability |
| 15 | [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) | Maintainability and change safety |

## Severity key used throughout

- **Critical** — actively exploitable today, or actively misrepresents what the product does, with real-world impact on live user data.
- **High** — a defect that will cause incorrect behavior, data loss, or a security/privacy exposure under realistic conditions, but is not immediately catastrophic.
- **Medium** — a maintainability, consistency, or quality defect that will cost real time and will compound as the codebase grows.
- **Low** — a valid finding with limited blast radius; worth fixing opportunistically.

## Top findings across the whole audit (cross-referenced from the detail files)

1. **The wallet's "Add Money" flow fabricates a real UPI payment**, complete with a "UPI Verified" badge and a picker for Google Pay/PhonePe/Paytm/BHIM — tapping "Proceed" never contacts any payment provider (confirmed: no payment SDK or UPI intent exists anywhere in the repository); it only increments a local number and fabricates a fake transaction reference (`CustomerScreens.kt:2691-2708`). See [01-correctness.md](01-correctness.md) C7, [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A1, and [03-privacy.md](03-privacy.md) P5.
2. **The OTP verification flow has a universal bypass code (`"1234"`) and never sends a real SMS** — `AuthScreen.kt:364`. Anyone who knows a customer's 10-digit phone number can log in as that customer. See [02-security.md](02-security.md) and [01-correctness.md](01-correctness.md).
3. **Firestore security rules allow anonymous read and write on `providers`, `bookings`, and `feedback`** — `firestore.rules:11-23`. Every customer's name, phone, address, and both booking OTPs are world-readable and world-writable. See [02-security.md](02-security.md) and [03-privacy.md](03-privacy.md).
4. **`app/google-services.json` (a live Firebase API key and project ID) is committed to git**, compounding finding 3 by making the open project trivially discoverable from the public repository. See [02-security.md](02-security.md).
5. **User passwords are stored in plaintext** in `SharedPreferences` via Gson (`ServiceSyncRepository.kt`), with `android:allowBackup="true"` left on, so a device backup can exfiltrate every locally cached password. See [02-security.md](02-security.md).
6. **Three parallel, fully-implemented authentication systems coexist**, and two of the three (password + SharedPreferences, and real Firebase email/password auth) are entirely dead code never called from any screen. See [05-reusability.md](05-reusability.md), [07-simplicity.md](07-simplicity.md), and [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md).
7. **The "Live Google Maps Tracking" feature does not use Google Maps or any location API** — it is a hand-drawn `Canvas` grid that even renders a fake "Google Maps Live" watermark (`BookingStatusScreen.kt:744,937-944`). The same fabrication pattern repeats in the "Uber & Ola"-style specialist-search radar animation and in a customer-facing "Simulate Acceptance" button. See [01-correctness.md](01-correctness.md) and [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A2.
8. **GPS auto-detect fabricates a fake "success" address** ("Simulated City Center, Karnataka") and shows a green "✓ Detected" badge when real location lookup fails, and calls the blocking `Geocoder` API on the main thread. See [01-correctness.md](01-correctness.md) C8 and [04-reliability.md](04-reliability.md) R7.
9. **Every notification/dispatch preference toggle and the language selector in Settings are decorative** — none of them changes any app behavior, and the same screen falsely labels the (bypassable) OTP flow "ACTIVE" security. The "Clear App Cache" button's confirmation dialog also never performs the clearing it promises. See [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A4-A5.
10. **Zero automated tests exist in the repository**, and the core data/business logic class (`ServiceSyncRepository`) is architecturally untestable as written. See [10-testability.md](10-testability.md).
11. **`CustomerScreens.kt` is 4,344 lines and `ServiceSyncRepository.kt` is 2,278 lines** — both are God objects mixing unrelated responsibilities. See [05-reusability.md](05-reusability.md), [07-simplicity.md](07-simplicity.md), and [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md).
12. **"Upload from Gallery" custom profile photo is completely non-functional** — the picked image is never rendered anywhere; the code explicitly discards it and always shows a placeholder instead. See [01-correctness.md](01-correctness.md) C10.
