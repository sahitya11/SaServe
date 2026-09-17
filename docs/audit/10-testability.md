# 10. Testability

**Definition for this codebase:** tests prove behavior, not implementation, and non-trivial
logic carries at least one runnable check that fails when the logic breaks.

## Findings

### T1 — Critical: there are zero automated tests anywhere in the repository

`find . -iname "*test*"` (excluding the Gradle wrapper's own internal directories) returns no
matches. There is no `app/src/test/` (JVM unit tests) or `app/src/androidTest/` (instrumented
tests) directory, despite the build already being configured to support both:
`app/build.gradle.kts:16` sets `testInstrumentationRunner =
"androidx.test.runner.AndroidJUnitRunner"` and `:47` pulls in
`debugImplementation("androidx.compose.ui:ui-test-manifest")` — the scaffolding for testing
exists in the build file, and nothing has ever been written to use it. Every finding in this
entire audit (all fifteen files) was therefore verified exclusively by direct source reading;
none of them are backed by a test that would catch a regression if someone "fixes" one bug
and reintroduces another.

### T2 — High: `ServiceSyncRepository`'s constructor makes it architecturally impossible to unit test in isolation

```kotlin
class ServiceSyncRepository(private val context: Context) {
    val firebaseAuthService: FirebaseAuthService = FirebaseAuthService()
    val firebaseSyncService: FirebaseSyncService = FirebaseSyncService()
    ...
    private val prefs: SharedPreferences = context.getSharedPreferences(...)
```

(`ServiceSyncRepository.kt:21-29`) The class takes a live Android `Context` and, inside its
own body, directly constructs `FirebaseAuthService()` and `FirebaseSyncService()` using their
default constructor arguments — which themselves resolve to `FirebaseAuth.getInstance()` and
`FirebaseFirestore.getInstance()` (`FirebaseAuthService.kt:17-19`,
`FirebaseSyncService.kt:16-17`), i.e., real, singleton, network-backed Firebase SDK objects.
There is no constructor seam to inject a fake `SharedPreferences`, a fake
`FirebaseAuthService`, or a fake `FirebaseSyncService`. Every one of `ServiceSyncRepository`'s
~50 public methods — including business-logic-bearing ones like the cancellation-fee
calculation in `cancelBookingWithDetails` — can only be exercised by running the whole
Android app (or an instrumented/emulator test) with a real device `Context` and a real (or
emulator-attached) Firebase project. This is the single biggest structural blocker to writing
any of the tests recommended below.

### T3 — Medium: business rules with real edge-case risk have no runnable check

Concretely, none of the following — despite each having non-trivial, edge-case-bearing logic
already documented elsewhere in this audit — has a test of any kind:

- The late-cancellation ₹99 fee rule (`cancelBookingWithDetails`, `ServiceSyncRepository.kt:1123-1162`).
- The wallet balance/deduction guard (`deductWallet`, `:1357-1373`, which must reject when
  `amount <= 0 || balance < amount`).
- The `displayBookingId` hash-based fallback ([01-correctness.md](01-correctness.md) C6).
- The rating-average round trip ([01-correctness.md](01-correctness.md) C5).
- Every phone/email normalization and matching rule ([06-no-duplication.md](06-no-duplication.md) D3).

None of this logic actually requires a `Context` or Firebase to test — the specific
calculations are pure functions of their inputs — but because they are implemented as
private methods on the untestable `ServiceSyncRepository` (T2), there is currently no way to
call them without the full class.

### T4 — Medium: the deserialization fallback logic in `FirebaseSyncService` has no check that it round-trips correctly

`FirebaseSyncService.kt`'s `providerToMap`/`documentToProvider` and
`bookingToMap`/`documentToBooking` pairs are hand-written, field-by-field serializers with
individual fallback defaults for every field (e.g. `doc.getString("startOtp") ?: "1234"`,
`:408`). A single round-trip test (`documentToBooking(bookingToMap(booking))` should recover
the original `booking`, modulo timestamp fields) would catch drift immediately if a field is
ever renamed on one side and not the other; today, that drift would only surface as a silent
wrong value in production.

## Recommendation summary

| Finding | Action |
|---|---|
| T1 | Establish `app/src/test/` and add at least one runnable check per non-trivial piece of logic before further feature work — per the audit brief's own testing discipline ("one runnable check per piece of non-trivial logic"), this does not require full coverage, just a first check per behavior that would otherwise fail silently. |
| T2 | Extract the pure business-rule methods (fee calculation, wallet guard, OTP/ID generation) out of `ServiceSyncRepository` into a small object/class with no `Context` or Firebase dependency, so they can be unit-tested directly; inject `FirebaseAuthService`/`FirebaseSyncService`/a `SharedPreferences`-like abstraction into the repository's constructor instead of constructing them internally. |
| T3 | Once T2 creates a seam, add a focused unit test per rule listed (cancellation fee at each `BookingStatus`, wallet deduct at boundary amounts, OTP length/character set, rating rounding). |
| T4 | Add one round-trip serialization test per model (`Booking`, `ServiceProvider`) asserting `documentToX(xToMap(x)) == x` for the fields that should survive the trip. |
