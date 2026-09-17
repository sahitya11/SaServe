# 5. Reusability

**Definition for this codebase:** logic lives where every caller that needs it can reach it,
at the right granularity — a function, class, or file should have one reason to exist and one
reason to change. For this single-module Android/Compose app, that means: data/persistence
logic, Firebase orchestration, and UI composables should be independently reachable and
independently swappable.

## Findings

### RU1 — High: `ServiceSyncRepository` is a single 2,278-line God Object with at least six unrelated responsibilities

Reading the full file (`app/src/main/java/com/servicesync/app/data/repository/ServiceSyncRepository.kt`)
end to end, one class owns:

1. Local persistence (raw `SharedPreferences` + manual `Gson` serialization for every model).
2. Firebase orchestration (owns and drives both `FirebaseAuthService` and
   `FirebaseSyncService` instances directly, `:23-24`).
3. Three parallel authentication systems (see [01-correctness.md](01-correctness.md) C3).
4. A wallet ledger (`addMoneyToWallet`, `deductWallet`, `:1338-1373`).
5. An address book (`addSavedAddress` through `setDefaultAddress`, `:1375-1526`).
6. In-app notification composition and dispatch (a `notify(title, message, ...)`-shaped block
   repeated roughly fifteen times inline — see [06-no-duplication.md](06-no-duplication.md)).
7. ~720 lines of hardcoded seed data (`getInitialIndianProviders`, `:1533-2253`).

None of these six concerns can be used, replaced, or unit-tested independently of the other
five, because they are all private state and private helper methods on one class with a
`Context`-taking constructor. A caller who only wants "the wallet" (for example, a future
admin/provider-side app reusing the same domain logic) must pull in Firebase auth,
notification helpers, and 700 lines of seed data to get it.

### RU2 — High: nine unrelated screens plus five shared widgets are physically located in one file

`app/src/main/java/com/servicesync/app/ui/screens/customer/CustomerScreens.kt` (4,344 lines)
contains, as top-level `@Composable` functions: `CustomerHomeScreen`, `ProviderListScreen`,
`ProviderDetailScreen`, `CustomerBookingsScreen`, `NotificationsScreen`, `WalletScreen`,
`HelpSupportScreen`, `ManageAddressesScreen`, and `MyProfileScreen` — nine full, independently
navigable screens — interleaved with five reusable pieces (`CommonApplianceFixCard`,
`CancellationReasonDialog`, `CategoryGridItem`, `BookingItemCard`,
`HomeScreenCompletedRatingCard`). This is inconsistent with the rest of the same package:
`AddressSetupScreen.kt`, `BookingStatusScreen.kt`, `FeedbackScreen.kt`,
`ServiceCatalogScreen.kt`, and `SettingsScreen.kt` all correctly follow one-screen-per-file.
Nothing about `CustomerScreens.kt`'s nine screens is more tightly coupled to each other than
those five already-separated screens are; the file is simply where new screens kept getting
appended.

### RU3 — Medium: category → visual representation is implemented three separate times instead of once

Detailed with line numbers in [01-correctness.md](01-correctness.md) C4 and
[06-no-duplication.md](06-no-duplication.md) D1. From a reusability standpoint: a caller that
needs "the icon for this category" has three different functions/fields to choose from
(`ServiceCategory.iconName`, `CommonComponents.getCategoryIcon()`,
`CommonComponents.kt:332`'s drawable mapping), and picking the wrong one silently produces a
different (and possibly stale) answer than picking the right one.

### RU4 — Medium: `FirebaseAuthService` and `FirebaseSyncService` are correctly reusable in isolation, but nothing else in the codebase follows their example

By contrast to RU1, `FirebaseAuthService.kt` and `FirebaseSyncService.kt` are both clean,
single-responsibility, constructor-injectable classes (`FirebaseAuthService(private val auth:
FirebaseAuth = ..., private val firestore: FirebaseFirestore = ...)`, defaulted for
convenience but overridable for a test double) — this is exactly the shape `ServiceSyncRepository`
should have been decomposed into, and the two service classes already prove the team knows
how to write it that way. The inconsistency is itself worth noting: the codebase contains its
own best-practice example right next to its worst offender.

### RU5 — Low: `components/*.jsx` and `src/components/*.jsx` — a React landing page with no build tooling, unreachable from the Android app

`components/Navbar.jsx`, `components/SaServe3DHelixLogo.jsx`, `components/Logo.tsx`, and their
`src/components/` duplicates (see [06-no-duplication.md](06-no-duplication.md) D3) are not
referenced by anything else in the repository — no `package.json`, no bundler config, no
import from the Android app (which is a different language and runtime entirely). This code
cannot currently be reused by anything, because there is nothing set up to consume it.

## Recommendation summary

| Finding | Action |
|---|---|
| RU1 | Split `ServiceSyncRepository` along its six responsibilities (auth, persistence, wallet, addresses, notifications-composition, seed data) into separate classes injected where needed, following the pattern already used correctly by `FirebaseAuthService`/`FirebaseSyncService` (RU4). Move seed data to a JSON asset loaded at startup rather than inline Kotlin. |
| RU2 | Split `CustomerScreens.kt` into one file per screen (nine files) plus one file for the shared widgets, matching the convention already used by the rest of the `screens/customer` package. |
| RU3 | See [01-correctness.md](01-correctness.md) C4 recommendation — collapse to one canonical mapping. |
| RU4 | No action needed on these two files; use them as the internal reference implementation when refactoring RU1. |
| RU5 | Either wire this into an actual build (add `package.json`/bundler config and a real deployment target for the landing page) or move it out of the app repository into a dedicated marketing-site repository if it is not meant to be built from here. |
