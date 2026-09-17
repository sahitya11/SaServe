# 6. No Duplication

**Definition for this codebase:** each type, constant, rule, and fact exists in exactly one
place. This is distinct from [05-reusability.md](05-reusability.md), which is about where
logic *lives*; this dimension is about logic (or a fact) being *repeated* rather than shared,
regardless of where it lives.

## Findings

### D1 — High: category → visual representation encoded at least four times, with no shared source of truth

1. `Models.kt:14-68` — every `ServiceCategory` carries an `iconName: String` field
   (`"Bolt"`, `"WaterDrop"`, etc.) that is never read anywhere (`grep -rn "\.iconName"` across
   the whole module returns zero results).
2. `CommonComponents.kt:100-114` — an independent `getCategoryIcon(category)` function with
   its own `when` block mapping each `ServiceCategory` to an `Icons.Default.X` value.
3. `CommonComponents.kt:330-339` — a third `when` block (`getSpecialistAvatarDrawable`) mapping
   category to a drawable resource ID for provider profile art — and this one only explicitly
   covers 5 of the 10 categories, silently defaulting the other 5 to one generic image.
4. `CustomerScreens.kt:905-918` — a fourth mapping (`CommonApplianceFixCard`'s
   `gradientColors`) giving every category its own three-color gradient list.

Adding a new `ServiceCategory` entry requires remembering to update four places by hand, one
of which (`iconName`) does nothing even when updated correctly.

### D2 — High: 4-digit OTP generation logic (`(1000..9999).random().toString()`) is copy-pasted eight times

Exact or near-exact repetitions of `(1000..9999).random().toString()`:

- `Models.kt:179-180` (twice, as `Booking`'s default parameter values for `startOtp`/`completionOtp`)
- `ServiceSyncRepository.kt:234-235` (the `loadOrInitializeData` sanitizer)
- `ServiceSyncRepository.kt:890-891` (`createBooking`)
- `ServiceSyncRepository.kt:961-962` (`broadcastServiceDispatch`)
- `AuthScreen.kt:327` and `AuthScreen.kt:355` (OTP send and resend)

No shared `fun generateOtp(): String` exists. The OTP length (`4` digits, range `1000..9999`)
is a business rule repeated eight times as a literal; changing OTP length to 6 digits (a
realistic future requirement) means finding and editing all eight call sites correctly.

### D3 — High: phone-digit normalization (`Regex("[^0-9]")`) is inlined eight separate times

`ServiceSyncRepository.kt` alone contains eight separate
`it.phone.replace(Regex("[^0-9]"), "")` / `phone.replace(Regex("[^0-9]"), "")` expressions
across `registerCustomer`, `registerWithPhoneOtp`, `loginWithPhoneOtp`, and `loginCustomer`
(confirmed by direct grep count: 8 matches). Each call site constructs a new `Regex` object
at the call site rather than reusing a compiled pattern, and the normalization rule itself
(strip everything but digits) exists as a repeated expression rather than a single
`String.digitsOnly()`-style extension function.

### D4 — Medium: the "build, prepend, and persist a notification" block is repeated roughly fifteen times with only the strings changed

Representative instances in `ServiceSyncRepository.kt`: `:114-121` (booking accepted),
`:124-131` (in progress), `:134-141` (completed), `:144-151` (cancelled), `:392-399`
(feedback received), `:537-544` (registration welcome), `:583-590` (OTP welcome), `:724-731`
(Firebase welcome), `:860-867` (new specialist), `:917-925` (booking request sent),
`:995-1002` (broadcasting), `:1059-1066` (broadcast accepted), `:1105-1113` (accept booking),
`:1152-1159` (cancellation), `:1184-1192` (service started), `:1213-1221` (service
completed), `:1307-1314` (tip added). Every instance follows the identical shape:

```kotlin
val notif = AppNotification(id = UUID.randomUUID().toString(), title = "...", message = "...", bookingId = ...)
val updatedNotifs = listOf(notif) + _notifications.value
_notifications.value = updatedNotifs
saveNotifications(updatedNotifs)
```

A single `private fun pushNotification(title: String, message: String, bookingId: String? = null)`
would collapse roughly 100 lines of repeated boilerplate into one function and one call per
site.

### D5 — Medium: `components/*.jsx` and `src/components/*.jsx` are byte-for-byte identical duplicate files

`diff components/Navbar.jsx src/components/Navbar.jsx` and
`diff components/SaServe3DHelixLogo.jsx src/components/SaServe3DHelixLogo.jsx` both produced
**no output** (confirmed identical). Two on-disk copies of the same source exist with no
build tool, symlink, or import statement connecting them — a future edit to one has no
mechanism to propagate to the other, guaranteeing eventual silent drift.

### D6 — Low: currency/rupee formatting (`"₹${value.toInt()}"`) is repeated ad hoc rather than through one formatter

Throughout `ServiceSyncRepository.kt` and the screen files, rupee amounts are formatted
inline as `"₹${x.toInt()}"` at dozens of call sites rather than through one shared
`formatRupees(amount: Double): String` (or a proper `NumberFormat.getCurrencyInstance`-based
helper). `.toInt()` additionally truncates rather than rounds, which is a minor correctness
risk layered on top of the duplication (e.g., ₹99.9 displays as ₹99).

## Recommendation summary

| Finding | Action |
|---|---|
| D1 | Collapse to one mapping — extend the `ServiceCategory` enum constructor to carry the `ImageVector` and drawable resource directly, delete `getCategoryIcon` and the unused `iconName` field. |
| D2 | Extract `object OtpGenerator { fun generate(): String = ... }` (or similar) with the digit count as a named constant, and use it at all eight call sites. |
| D3 | Extract a single `fun String.digitsOnly(): String = replace(Regex("[^0-9]"), "")` extension (compiled `Regex` as a top-level `val`) and use it everywhere phone comparison happens. |
| D4 | Extract `private fun pushNotification(title: String, message: String, bookingId: String? = null)` on `ServiceSyncRepository` and route all ~15 call sites through it. |
| D5 | Delete one copy and have the other be the single source; if both are meant to diverge in the future, this note should be revisited once real per-directory purpose is established. |
| D6 | Extract one shared rupee-formatting helper; decide and apply a single, deliberate rounding rule instead of implicit truncation. |
