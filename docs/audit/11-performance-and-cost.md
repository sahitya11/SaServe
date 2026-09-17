# 11. Performance and Cost

**Definition for this codebase:** fast enough for a mobile app, and inside whatever free-tier
limits the backend actually has — the applicable constraint is the **Firebase Spark (free)
plan's Firestore quotas**: a fixed daily budget of document reads, writes, and deletes, plus a
per-project concurrent-connection ceiling for realtime listeners. Everything below is
evaluated against that constraint, plus ordinary mobile-app performance hygiene (APK size,
unnecessary work per launch).

## Findings

### PC1 — High: every app cold start performs a full, unconditional re-sync of all cached data to Firestore

`ServiceSyncRepository.kt:73-96`, inside `init {}`:

```kotlin
repositoryScope.launch {
    try {
        syncAllDataToFirebase()
        ...
```

`syncAllDataToFirebase()` (`:790-797`) calls `firebaseSyncService.syncAllInitialData`, which
batch-writes **every** locally cached provider, booking, feedback, and user document back to
Firestore (`FirebaseSyncService.kt:225-237`) — regardless of whether anything actually
changed since the last launch. On a device with, say, 20 cached bookings and 15 providers,
that is 35+ Firestore document writes on every single cold start, for a user who may have
changed nothing. Against the Spark plan's fixed daily write quota (shared across the whole
project, all users), this is the single largest avoidable cost driver in the codebase — it
scales with (app opens) × (cached documents), not with (actual changes).

### PC2 — High: two permanent realtime listeners fan out every remote change to every installed client, because Firestore rules don't scope by user

`startFirestoreRealtimeSync` (`ServiceSyncRepository.kt:98-189`) opens
`listenToBookings`/`listenToProviders` for the lifetime of the singleton with no
unsubscribe/pause on app background. Because `firestore.rules` currently allows
`read: if true` on both collections (see [02-security.md](02-security.md) S2), every
customer's app receives a snapshot update for *every other customer's* booking and provider
change, not just their own — Firestore bills/quotas realtime listener updates per document
delivered to each connected listener, so this multiplies read/listener cost by the total
number of active app installs, for data most of those installs have no use for. Fixing the
security-rule scoping (S2) is a direct cost fix here as well, not just a security fix.

### PC3 — Medium: release builds ship with `isMinifyEnabled = false` — no code shrinking or resource shrinking

`app/build.gradle.kts:22-27` disables minification for the `release` build type entirely. No
R8/ProGuard code shrinking, no unused-resource removal, no obfuscation. Combined with the
unused-dependency finding below (PC4), the shipped APK is measurably larger than it needs to
be, with slower install/update download times on the metered/low-bandwidth connections common
among the app's stated target audience (home-service customers, not necessarily on unlimited
high-speed data).

### PC4 — Medium: three declared dependencies are never used at runtime

Confirmed by grep across `app/src/main/java`:

- `com.google.firebase:firebase-database` (Realtime Database) — zero `FirebaseDatabase`
  references anywhere; the app only uses Firestore.
- `androidx.navigation:navigation-compose:2.8.5` — zero `NavHost`/`rememberNavController`
  references (see [09-consistency.md](09-consistency.md) C9-3).
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0` — pulled in for Play
  Services task-to-coroutine interop, but `FirebaseAuthService`/`FirebaseSyncService` already
  use `kotlinx.coroutines.tasks.await` from the core `kotlinx-coroutines-core` artifact, and a
  grep for any other Play Services task-adapter usage (`asDeferred`, `awaitFirstOrNull` from
  this specific library's API) finds none.

Each of these adds method count, APK size, and (for `firebase-database`) an entire additional
Firebase product surface to initialize, for zero functional benefit.

### PC5 — Low: `broadcastServiceDispatch` spends a fixed 2.6-second delay on every dispatch regardless of real conditions

`ServiceSyncRepository.kt:1005`: `kotlinx.coroutines.delay(2600)`. This is a deliberate UX
pacing choice (simulating a "searching for specialists" radar animation), not a real
network/matching cost — noted here because it is pure latency added to every booking flow
with no way to shorten it if a real match is found instantly, and no way to extend it
gracefully if a real match takes longer (see also
[04-reliability.md](04-reliability.md) R4 for the cancellation-safety angle on the same
code).

## Recommendation summary

| Finding | Action |
|---|---|
| PC1 | Track a local "dirty" flag (or a last-synced timestamp per collection) and only sync documents that changed since the last successful sync, instead of unconditionally re-pushing the entire cache on every cold start. |
| PC2 | Fixed by the same Firestore rule scoping recommended in [02-security.md](02-security.md) S2 — scoping reads to the owning user's documents directly reduces listener fan-out cost. |
| PC3 | Enable `isMinifyEnabled = true` (with the existing `proguard-rules.pro` Gson rule, which is otherwise dead — see [07-simplicity.md](07-simplicity.md) SI5) for release builds. |
| PC4 | Remove `firebase-database`, `navigation-compose`, and `kotlinx-coroutines-play-services` from `app/build.gradle.kts` unless a concrete near-term plan exists to use them. |
| PC5 | If the pacing is intentional UX, no change needed; if it should reflect real dispatch latency once a real matching backend exists, replace the fixed delay with the actual matching call's latency. |
