# 4. Reliability

**Definition for this codebase:** failures are handled on purpose, nothing is silently
swallowed, and repeating an operation does no additional harm. The applicable surfaces here
are the two permanent Firestore realtime listeners and the various "write local, then
fire-and-forget sync to cloud" operations threaded through `ServiceSyncRepository`.

## Findings

### R1 — High: background Firebase sync failures are caught and silently discarded

`ServiceSyncRepository.kt:73-96`, inside `init {}`:

```kotlin
repositoryScope.launch {
    try {
        syncAllDataToFirebase()
        firebaseSyncService.fetchProviders().onSuccess { ... }
    } catch (e: Exception) {
        // Background sync gracefully non-blocking
    }
}
```

Every exception from the initial full-data sync and provider fetch — network failure, auth
failure, quota exceeded, malformed data — is caught and discarded with a comment but no log
call, no retry, and no user-visible or developer-visible signal of any kind. This is the
canonical "silently swallowed" failure the dimension explicitly warns against. If Firestore
sync has been failing for every user since a given release, nothing in this codebase would
ever surface that fact.

### R2 — High: realtime listeners have no reconnect/backoff strategy, and their setup failure is logged but not recovered

`startFirestoreRealtimeSync` (`ServiceSyncRepository.kt:98-189`) wraps listener registration
in a `try { ... } catch (e: Exception) { android.util.Log.e(...) }`. If listener setup throws
(e.g., transient network issue at cold start), the catch block logs the error and returns —
the app is left in a state where `bookingsListener`/`providersListener` remain `null` and are
never retried. Since the app's "real-time" booking-status promise as advertised to the
customer depends entirely on these listeners, a customer who happens to open the app during
whatever caused the initial failure will silently never receive a live status update for the
rest of that app session, with no error banner and no manual "retry sync" affordance
anywhere in the UI.

### R3 — High: booking status updates are not protected against concurrent overwrite (lost-update risk)

Every booking mutation in `ServiceSyncRepository` — `acceptBooking` (`:1075`),
`cancelBookingWithDetails` (`:1123`), `startBookingWithOtp` (`:1168`),
`completeBookingWithOtp` (`:1197`), `addTipToBooking` (`:1294`) — follows the same pattern:
read the entire `_bookings.value` list, find the target booking by index, produce a modified
copy, write the entire list back locally and to Firestore. Meanwhile, the realtime listener
installed in `startFirestoreRealtimeSync` (`:98-189`) can fire concurrently on the same
`_bookings` `MutableStateFlow` from a Firestore snapshot callback and perform its own
read-merge-write of the same list. There is no Firestore transaction, no optimistic-
concurrency token (e.g., a version field), and no synchronization between the "local user
action" write path and the "remote listener" write path. Two updates racing (e.g., a customer
taps "Cancel" at the same moment the provider's app pushes an "Accepted" status) can produce
a lost update in either direction, with the loser's write silently overwritten.

### R4 — Medium: the on-demand dispatch simulation has no timeout or cleanup if cancelled mid-flight

`broadcastServiceDispatch` (`ServiceSyncRepository.kt:953-1069`) creates a `PENDING` booking,
persists it, then does `kotlinx.coroutines.delay(2600)` to simulate a matching delay before
assigning a provider and flipping status to `ACCEPTED`. This is a `suspend` function launched
from UI-scoped code; if the calling coroutine is cancelled during the 2.6-second delay (screen
rotation navigating away, process death, user backgrounding the app at exactly the wrong
moment), the function throws `CancellationException` at the `delay` call and never reaches
the provider-assignment/status-update code below it. The booking that was already written at
step 1 (`_bookings.value` updated and persisted, `:990-992`) is left permanently in
`PENDING` with `providerName = "Searching Nearby Specialists..."` — there is no timeout, no
background retry, and no UI path that revisits an orphaned `PENDING` booking to resolve it.

### R5 — Medium: every `SharedPreferences` write is fire-and-forget with no verification

All persistence in `ServiceSyncRepository` uses `prefs.edit().put...().apply()` — `apply()`
is asynchronous and returns immediately with no completion signal or error callback (this is
the standard, correct choice for UI-thread-safe writes on Android, so it is not wrong in
isolation). Combined with R1-R3, though, the app has no layer, anywhere, that reconciles "what
I optimistically wrote locally" against "what Firestore actually accepted" — a failed local
write and a failed remote sync look identical (nothing happens) from the user's perspective.

### R6 — Low: `Result<T>` return values from Firestore sync calls are computed and then ignored at most call sites

`saveProviders`, `saveBookings`, `saveUser`, `saveRegisteredUsers` (`ServiceSyncRepository.kt`,
various) each `repositoryScope.launch { firebaseSyncService.syncX(...) }` without ever
inspecting the `Result` that `syncAllProviders`/`syncAllBookings`/etc. return. The sync
functions themselves are well-written (proper `Result.success`/`Result.failure` wrapping,
`FirebaseSyncService.kt:31-237`) — the defect is entirely on the calling side, which throws
that signal away.

### R7 — Medium: reverse-geocoding runs synchronously on the UI-effect thread, risking an ANR

`detectCurrentGpsLocation` (`CustomerScreens.kt:3677-3731`) calls the blocking
`Geocoder.getFromLocation(...)` (`:3708`) directly. Its two callers —
`AddressSetupScreen.kt:99-101` (`LaunchedEffect(Unit) { triggerGpsDetection() }`, run
automatically the instant the screen opens) and `ManageAddressesScreen`'s GPS button
(`CustomerScreens.kt:3196`) — invoke it with no `withContext(Dispatchers.IO)` or other
dispatcher switch. On a device where the geocoder backend is slow or network-bound, this can
block Compose's effect execution and contribute to an ANR. See
[01-correctness.md](01-correctness.md) C8 for the accompanying fake-success-on-failure defect
in the same function.

## Recommendation summary

| Finding | Action |
|---|---|
| R1 | Log the caught exception at minimum (`Log.e`), and surface a retry affordance or a non-blocking "sync pending" indicator rather than a bare comment. |
| R2 | Add a retry-with-backoff for listener registration, and expose a manual "retry sync" action if automatic retries exhaust. |
| R3 | Move multi-step booking mutations into a Firestore transaction, or add an optimistic-concurrency version field checked before each local+remote write, so the realtime listener and local user actions cannot silently clobber each other. |
| R4 | Give the broadcast/dispatch flow a real timeout with a background-safe continuation (e.g., a `WorkManager` job or a server-side Cloud Function) instead of a UI-coroutine-scoped `delay`, and add a sweep that resolves bookings stuck in `PENDING` past a threshold. |
| R5 | Once R1-R3 are addressed, add a lightweight reconciliation pass (e.g., on app foreground, diff local cache against a fresh Firestore fetch) to catch silently-diverged state. |
| R6 | Inspect the `Result` at each fire-and-forget call site and route failures into the same logging/retry mechanism recommended for R1. |
| R7 | Wrap the `Geocoder` call in `withContext(Dispatchers.IO)` before returning to the caller. |
