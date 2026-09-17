# 12. Observability

**Definition for this codebase:** when something goes wrong, it can be found without
guessing. There is no server-side request path here to attach a request ID to; the
applicable version of this dimension is: does a failure leave a trace, and can a booking's
lifecycle be reconstructed after the fact.

## Findings

### O1 — High: the large majority of `catch (e: Exception)` blocks in the core repository leave no trace at all

34 `catch (e: Exception)` blocks exist in `ServiceSyncRepository.kt`. Spot-checking their
bodies:

- `:92-94` — comment only, no log: `// Background sync gracefully non-blocking`.
- `:240-242` — comment only: `_bookings.value = emptyList()` with no log of what parse error
  occurred.
- `:296-301`, `:363-365`, `:754-756` — silently reset state or comment-only, no log.

By contrast, `startFirestoreRealtimeSync`'s catch (`:186-188`) *does* call
`android.util.Log.e("ServiceSyncRepository", "Failed to start Firestore realtime
listeners", e)` — proving the team knows how to log a caught exception correctly, but applies
it inconsistently. `FirebaseSyncService.kt` is the positive counter-example throughout: every
one of its `catch` blocks logs via `Log.e(TAG, "...", e)` before returning
`Result.failure(e)` (`:40-43`, `:57-59`, `:69-71`, etc.) — the calling code in
`ServiceSyncRepository` then discards that `Result` without inspecting it (see
[04-reliability.md](04-reliability.md) R6), so even the one place that *does* log correctly
has its signal thrown away one layer up in most call paths.

### O2 — Medium: no correlation ID threads a booking's lifecycle across the systems that touch it

A single booking passes through: local `SharedPreferences` (create), the `bookings`
Firestore collection (sync), a realtime listener callback (status change detection), a local
Android notification (accepted/started/completed), and an in-app `AppNotification` record —
five different storage/delivery mechanisms, none of which share any identifier beyond the
booking's own `id` (which is present, but nothing logs it consistently alongside each state
transition). Reconstructing "what happened to booking `1084213765`" after a customer
complaint requires manually cross-referencing SharedPreferences dumps, Firestore document
history, and Android notification history with no single log line per transition to anchor
the timeline.

### O3 — Low: `Log.d` calls in `FirebaseSyncService` include names and identifiers that would appear in production logcat

E.g. `Log.d(TAG, "Provider synced to Firestore: ${provider.id} (${provider.name})")`
(`FirebaseSyncService.kt:38`) and the equivalent for users/bookings. These are `Log.d` (debug
level, typically stripped or ignored in release logcat filtering, and this app ships without
any centralized crash/log reporting SDK such as Crashlytics — grep confirms no
`firebase-crashlytics` dependency exists) so the practical exposure is low, but the pattern
of including a real name/identifier in a log call is worth flagging now, before a future
change (e.g., adding Crashlytics, which is one dependency-line away given the existing
Firebase BoM) turns these into persisted, off-device log records.

## Recommendation summary

| Finding | Action |
|---|---|
| O1 | Add a minimum of `Log.w`/`Log.e` with the caught exception to every currently-silent `catch` block in `ServiceSyncRepository`; inspect the `Result` returned by `FirebaseSyncService` calls at every call site instead of discarding it (ties directly to [04-reliability.md](04-reliability.md) R1/R6). |
| O2 | Log booking `id` alongside every state-transition log line (creation, each status change, each notification dispatch) using a consistent tag/format, so a single logcat/log-aggregator search for that ID reconstructs the full timeline. |
| O3 | If Crashlytics or any persisted logging is added later, audit and redact identifiers (names, phone numbers) from log statements before they can be captured off-device. |
