# 1. Correctness

**Definition for this codebase:** the app does what its UI copy, its feature names, and its
data model claim it does, including in edge cases. There is no written spec or plan document
in this repository, so "correctness" here is judged against the product's own stated behavior
(README, on-screen copy, function/feature names) and against internal logical consistency
(a value computed one way is not silently recomputed a different, incompatible way elsewhere).

There are no automated tests in this repository (see
[10-testability.md](10-testability.md)), so every finding below was verified by direct code
reading, not by a failing/passing test.

## Findings

### C1 — Critical: OTP verification has a universal bypass and never sends an SMS

`app/src/main/java/com/servicesync/app/ui/screens/auth/AuthScreen.kt:364`

```kotlin
if (otpInput.trim() != generatedOtp && otpInput.trim() != "1234") {
    errorMessage = "Invalid OTP code. Please check the 4-digit code and try again."
    return@Button
}
```

The 4-digit code is generated on-device (`(1000..9999).random()`), never transmitted to any
SMS gateway, and shown to the same device via a local Android notification
(`NotificationHelper.sendOtpSmsNotification`, `NotificationHelper.kt:139`). The literal string
`"1234"` is additionally accepted unconditionally, regardless of what code was generated. The
feature is named and copy-written as if it verifies phone ownership ("We'll send an OTP to
verify your account", `AuthScreen.kt:174`); it verifies nothing. This is scored under
Correctness because the feature does not do what it is named and described as doing; the
exploitability of this gap is scored separately under
[02-security.md](02-security.md) (S1).

### C2 — Critical: "Live Google Maps Tracking" does not use Google Maps, or any location data

`app/src/main/java/com/servicesync/app/ui/screens/customer/BookingStatusScreen.kt:744-900`
(function `GoogleMapsLiveTrackingCard`)

The card's header text is literally "Live Google Maps Tracking" with a "GPS Active" pill next
to it (`BookingStatusScreen.kt:772,789`). The map itself is a `Canvas` composable that
procedurally draws a grid of streets and a glowing polyline with no relationship to the
Google Maps SDK, no `MapView`/`GoogleMap` composable, and no `FusedLocationProviderClient` or
`LocationManager` call anywhere in this file. `grep`-ing the whole module for
`GoogleMap`/`FusedLocationProviderClient` returns zero matches outside this cosmetic label.
The app does request `ACCESS_FINE_LOCATION` elsewhere (for one-tap address autofill in
`AddressSetupScreen.kt`), but that permission is never used to power this "tracking" screen.
The feature is decorative and mislabeled as real-time telemetry it does not have.

### C3 — High: three parallel authentication systems, two of which are entirely unreachable dead code

`ServiceSyncRepository.kt` implements four separate sign-in/registration code paths:

- `registerWithPhoneOtp` / `loginWithPhoneOtp` (`:549`, `:595`) — the only path called from any
  screen (`AuthScreen.kt:371,382`).
- `registerCustomer` / `loginCustomer` (`:466`, `:660`) — a full phone+password path with its
  own plaintext password storage, matching logic, and error handling. **Not called from any
  screen anywhere in `app/src/main/java`** (verified by grepping every caller of these two
  functions across the module: the only matches are the function definitions themselves).
- `registerWithFirebase` / `loginWithFirebase` (`:710`, `:736`) — a third path that delegates to
  the real, correctly-implemented `FirebaseAuthService` (hashed passwords, real Firebase Auth).
  **Also never called from any screen.**

Three-quarters of the authentication surface area is dead code that nonetheless compiles,
ships in the APK, and must be read and reasoned about by anyone auditing "how does login
work" — including this audit, which had to trace all four before determining which one is
actually live. See [05-reusability.md](05-reusability.md) and
[15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) for the
duplication/maintenance angle on the same fact.

### C4 — Medium: `ServiceCategory.iconName` is a dead field; the real mapping lives elsewhere and can drift

`app/src/main/java/com/servicesync/app/data/model/Models.kt:14-68` declares an `iconName:
String` on every `ServiceCategory` entry (e.g. `"Bolt"`, `"WaterDrop"`). Grepping the entire
module for `.iconName` usage returns **zero** call sites — the field is written but never
read. The actual category-to-icon logic lives in a completely separate, hand-maintained
`when` block: `CommonComponents.kt:100-114` (`getCategoryIcon`), and a *third*,
independent category-to-drawable-resource mapping exists at `CommonComponents.kt:330-339`
(`getSpecialistAvatarDrawable`) for provider profile art — which itself only explicitly
covers 5 of the 10 `ServiceCategory` values (ELECTRICIAN, PLUMBER, CARPENTER, MECHANIC,
HOUSE_CLEANING); the other five (APPLIANCE_REPAIR, PAINTER, MASON, GARDENER, OTHER) silently
fall through to one generic `specialist_default` image. A *fourth* mapping
(`CommonApplianceFixCard`'s `gradientColors`, `CustomerScreens.kt:905-918`) maps every category
to its own three-color gradient. Nothing enforces that these four encodings of "what does this
category look like" stay in sync; adding a new `ServiceCategory` entry today would compile
successfully while silently falling through to a default in three of the four places.
Cross-referenced in [06-no-duplication.md](06-no-duplication.md) (D1).

### C5 — Medium: rating average is round-tripped through a locale-formatted string

`ServiceSyncRepository.kt:1259-1263`, inside `addReviewForBooking`:

```kotlin
val newAvgRating = updatedReviews.map { it.rating }.average().toFloat()
val updatedProvider = provider.copy(
    ...
    rating = String.format(java.util.Locale.US, "%.1f", newAvgRating).toFloat()
)
```

The average is computed correctly, then deliberately formatted to one decimal place as a
`Locale.US` string, then parsed straight back into a `Float`. The `Locale.US` pin is a
correct defensive choice against the classic "device locale uses a comma decimal separator"
crash, but the format-then-reparse round trip only exists to truncate to one decimal — the
same result is available directly and more cheaply with
`kotlin.math.round(newAvgRating * 10f) / 10f`, with no string allocation and no locale
dependency to get right in the first place.

### C6 — Low: `displayBookingId` fallback is a dense, undocumented hash-based synthesis

`app/src/main/java/com/servicesync/app/data/model/Models.kt:188-189`:

```kotlin
val displayBookingId: String
    get() = if (id.startsWith("10") && id.length == 10 && id.all { it.isDigit() }) id else "10" + Math.abs(id.hashCode()).toString().padStart(8, '0').take(8)
```

Every booking created through `generate10DigitBookingId()` (`ServiceSyncRepository.kt:878`)
already satisfies the `id.startsWith("10") && length == 10` condition, so in the app's only
real code path this getter's `else` branch is unreachable — it exists purely to cosmetically
re-derive a "10-digit-looking" ID for a `Booking` that was constructed some other way (e.g. a
future caller, or a malformed Firestore document deserialized by `documentToBooking`). Two
different `Booking` instances can legitimately collide on this synthesized display ID if their
real `id.hashCode()` values collide, and nothing in the codebase asserts, tests, or comments
on that possibility.

### C7 — Critical: the wallet "Add Money" flow fabricates a real UPI payment, naming real third-party payment brands

`app/src/main/java/com/servicesync/app/ui/screens/customer/CustomerScreens.kt:2495-2721`
(`WalletScreen`) presents a "UPI Verified" badge (`:2579`), a UPI-app selector with the real,
named, trademarked apps **Google Pay, PhonePe, Paytm, and BHIM** (`:2667`), a "UPI ID" input
field, and a button labelled "Proceed with $selectedUpiApp" (`:2717`). Its `onClick` handler
(`:2691-2708`) does not launch a UPI payment intent, does not call any payment SDK, and does
not contact any payment processor of any kind — confirmed by grepping the entire repository
(`app/src/main/java`, `app/build.gradle.kts`) for any UPI intent (`upi://`), payment SDK
(`PaymentsClient`, `play-services-wallet`), or payment gateway (`Razorpay`, `PayU`, `Stripe`):
**zero matches**. The handler calls `repository.addMoneyToWallet(amount, upiApp, upiId)`
(`ServiceSyncRepository.kt:1338-1355`) directly, which purely increments a local `Double`
balance stored in `SharedPreferences` and fabricates a synthetic `upiRefId` string
(`"UPI/${(100000..999999).random()}"`) shaped to look like a real payment reference. A Toast
then confirms: `"₹${amt.toInt()} added to SaServe Wallet via $selectedUpiApp!"` — falsely
attributing the "deposit" to whichever real payment app the user selected. The in-app FAQ
(`HelpSupportScreen.kt:3117`) documents this fake flow as if it were real: *"select your UPI
app (Google Pay, PhonePe, Paytm, or BHIM), and proceed."* This is the most severe correctness
defect in the audit: it does not merely fail to do what it claims, it actively simulates a
named, branded financial transaction with a wallet balance a user could reasonably believe
represents real, spendable money. See [13-accessibility-and-ux.md](13-accessibility-and-ux.md)
for the honesty angle and [03-privacy.md](03-privacy.md)/[02-security.md](02-security.md)
cross-references.

### C8 — High: GPS auto-detect fabricates a fake "success" address when real location lookup fails

`app/src/main/java/com/servicesync/app/ui/screens/customer/CustomerScreens.kt:3674-3731`
(`detectCurrentGpsLocation`, called from `AddressSetupScreen.kt:56,81` and
`CustomerScreens.kt:3196` in `ManageAddressesScreen`). The function tries
`LocationManager.getLastKnownLocation` across GPS/network/passive providers; when all three
return `null` (a real and common case — `getLastKnownLocation` only returns a **cached** fix,
never requesting a new one, and can easily be null on an emulator, a fresh install, or a
device with location services recently toggled), the function does not report failure. It
returns the literal string `"Current GPS Location (Simulated City Center, Karnataka)"`
(`:3726`) through the same success callback used for a real fix. The caller
(`AddressSetupScreen.kt:56-61`) treats this exactly like a successful detection: it sets
`gpsDetectedSuccess = true` and renders a green "✓ Detected" badge (`:204-217`). A user can
end up with a fabricated "Karnataka" address silently saved as their real service address,
believing GPS succeeded. Separately, `Geocoder.getFromLocation` (`:3708`) is a blocking,
potentially network-bound call invoked synchronously from `LaunchedEffect(Unit) {
triggerGpsDetection() }` (`AddressSetupScreen.kt:99-101`) with no `withContext(Dispatchers.IO)`
— this runs on the composition's effect dispatcher (effectively the main thread) and risks an
ANR on a slow or offline geocoder lookup.

### C9 — Medium: the direct provider-booking flow shows hardcoded, permanently stale calendar dates

`app/src/main/java/com/servicesync/app/ui/screens/customer/CustomerScreens.kt:1388-1393`
(`ProviderDetailScreen`):

```kotlin
val availableDates = listOf(
    "Today, Sep 5",
    "Tomorrow, Sep 6",
    "Sunday, Sep 7",
    "Monday, Sep 8"
)
```

These are literal strings, not derived from the device's actual current date in any way. A
user booking a specialist directly through a provider's profile (as opposed to the on-demand
catalog flow in `ServiceCatalogScreen.kt:436`, which correctly uses relative labels — "Today",
"Tomorrow", "In 2 Days" — with no hardcoded calendar date) will see "Today, Sep 5" and
"Monday, Sep 8" regardless of what today's real date is, any day of the year.

### C10 — Medium: "Upload from Gallery" custom profile photo is silently discarded and never displayed

`app/src/main/java/com/servicesync/app/ui/screens/customer/CustomerScreens.kt:4020-4112`
(`MyProfileScreen`). Picking a photo (`photoPickerLauncher`, `:4020-4027`) stores its
`content://` URI in `selectedAvatar`, shows a "Photo selected! Don't forget to save." Toast,
and — on save — persists it as `User.profileImageUri` via `repository.updateUserProfile`
(`:4311-4317`). But the rendering code that is supposed to show it explicitly special-cases
this exact case and ignores the URI:

```kotlin
if (selectedAvatar.startsWith("content://") || selectedAvatar.startsWith("file://")) {
    // Loaded custom URI
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.avatar_user_1),
        ...
```

(`CustomerScreens.kt:4085-4092`) Every uploaded photo renders as the generic
`avatar_user_1` placeholder — on this screen, and everywhere else `profileImageUri` is read
(e.g. the home-screen drawer header, `getCustomerAvatarDrawable`, `CustomerScreens.kt:3990-3998`,
which only maps the four preset keys and falls through to the same placeholder for any other
value). The upload feature has no working path to ever display a user's own photo.

## Recommendation summary

| Finding | Action |
|---|---|
| C1 | Replace the fake local-notification OTP with a real SMS/telephony verification provider, or clearly relabel the flow as a demo/mock in both code and UI copy until one is integrated. Remove the `"1234"` bypass entirely from any code path that will ship. |
| C2 | Either integrate the Google Maps SDK and real location updates, or rename the feature and remove the "GPS Active" / "Live Google Maps Tracking" copy so it does not misrepresent capability. |
| C3 | Delete the two unreachable auth paths, or if they are intentionally kept for a future rollout, mark them clearly (e.g. a top-of-function comment stating they are not yet wired to any screen) so the next reader doesn't have to rediscover this by tracing every call site. |
| C4 | Pick one canonical category→icon mapping (extend the enum itself with the `ImageVector` and drawable resource, since Kotlin enums can hold non-primitive constructor arguments) and delete the other three. |
| C5 | Replace the format/parse round trip with direct numeric rounding. |
| C6 | Either remove the dead `else` branch (if no caller can ever construct a non-conforming `Booking.id`) or add a one-line comment stating which caller relies on it and why hash collision is an accepted risk. |
| C7 | Do not ship this flow. Either integrate a real UPI/payment gateway (e.g. via an approved PSP SDK) before any real money claim is shown to a user, or rebuild the wallet as an explicitly-labelled mock/credits system with no reference to real payment app brands. |
| C8 | Return failure (`onResult(null)`) instead of a fabricated address when no location is available, and let the caller show a real "couldn't detect location, please enter manually" state. Move the `Geocoder` call to `Dispatchers.IO`. |
| C9 | Compute the date list from the device's actual current date (e.g. `LocalDate.now()` + relative offsets), matching the pattern already used correctly in `ServiceCatalogScreen`. |
| C10 | Actually render the picked `content://`/`file://` URI (e.g. via Coil/`AsyncImage` or a `BitmapFactory` load with a persisted URI permission) instead of silently substituting the placeholder. |
