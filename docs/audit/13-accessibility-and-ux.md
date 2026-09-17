# 13. Accessibility and UX

**Definition for this codebase:** adoption-first design — plain language, mobile-first
(automatically true here, since this is a mobile-only app), keyboard/screen-reader usability,
and no jargon shown to end users. This audit also treats *honesty* about what a feature does
as part of UX quality: a feature that visually claims a capability it doesn't have is a UX
defect, not just a correctness one.

## Findings

### A1 — Critical (UX honesty): the wallet "Add Money" flow simulates a real UPI payment using real payment-app brand names

Full technical detail in [01-correctness.md](01-correctness.md) C7. `WalletScreen`
(`CustomerScreens.kt:2495-2721`) shows a "UPI Verified" badge, lets the user pick **Google
Pay, PhonePe, Paytm, or BHIM**, enter a UPI ID, and tap "Proceed with $selectedUpiApp" — and
then simply increments a local number with no payment ever taking place. This is the single
most serious finding in the entire audit: it does not just fail to deliver a promised
capability (the pattern in A2-A4 below), it actively performs the *appearance* of a completed
financial transaction, naming specific real, trademarked third-party payment providers as if
they processed it, and shows the resulting balance next to a "UPI Verified" trust badge. A
user reasonably relying on this screen could believe money they "added" is real and
recoverable when it exists only as a local number on their device.

### A2 — Critical (UX honesty): a screen labelled "Live Google Maps Tracking" with a "GPS Active" indicator shows no real location data, and even renders a fake "Google Maps Live" watermark

Full technical detail in [01-correctness.md](01-correctness.md) C2. From a pure UX
standpoint: a customer waiting for a home-service specialist, looking at a screen that says
"GPS Active" and shows a moving dot on what appears to be a live map, will make real
decisions based on that appearance (e.g., deciding whether they have time to step out, or how
urgently to call the specialist) based on a rendering that has no relationship to where
anyone actually is. The card even renders the literal text "Google Maps Live" as a bottom-right
watermark (`BookingStatusScreen.kt:937-944`), imitating the branding of a real product this
screen has no integration with. The same pattern repeats in `ProviderSearchingRadarView`
(`ServiceCatalogScreen.kt:857-1030`), whose copy states *"Connecting you with highest-rated
nearby verified pros, just like Uber & Ola"* (`:988`) while the actual backing logic
(`broadcastServiceDispatch`) is a client-side `list.random()` pick among already-known mock
providers — and in the "Simulate Acceptance" button (`CustomerScreens.kt:2350`) that lets a
customer accept their own booking request themselves, exposed unconditionally in the
production customer app (the README's own Testing Flow section documents this as a
test-only action, but nothing in code gates it out of a real build). Taken together, this
audit finds no working simulation of a genuine specialist-matching or location-tracking
backend anywhere in the codebase — every appearance of one is a client-side animation or a
manual self-trigger.

### A3 — High: the OTP flow's error copy does not tell the user that no SMS was ever sent

`AuthScreen.kt:365`: `"Invalid OTP code. Please check the 4-digit code and try again."` A real
user, told an OTP was sent to their phone (`:174-176`: "We'll send an OTP to verify your
account"), will check their SMS inbox, find nothing, and have no path forward except to
notice (or be told by someone else) that the correct code is actually sitting in their own
notification tray. This is a direct UX consequence of [01-correctness.md](01-correctness.md)
C1/[02-security.md](02-security.md) S1 — the interface promises a mechanism (SMS delivery)
that does not exist, and the error-recovery copy does not acknowledge that gap.

### A4 — High: every notification/dispatch preference toggle and the language selector in Settings are decorative — none of them do anything

`SettingsScreen.kt:39-43` declares `smsNotifications`, `pushNotifications`,
`arrivalSoundAlerts`, `instantSpecialistAutoMatch`, and `selectedLanguage` as purely local
`remember { mutableStateOf(...) }` values. None is persisted to the repository, and grepping
the rest of the module confirms none is ever read anywhere else — `NotificationHelper` fires
its notifications unconditionally regardless of the "Push Notifications" or "SMS Booking
Updates" toggle state, and there is no i18n layer for "App Language" to switch (see A6). A
user who toggles "SMS Booking Updates" off, believing they've opted out, will keep receiving
exactly the same behavior as before — there was never an SMS to opt out of, and toggling the
switch changes nothing. Selecting "हिन्दी (Hindi)" shows a snackbar confirming the change and
updates one line of text on this screen only; the rest of the app remains entirely in English.
Additionally, the same screen displays a "Two-Step Safety Verification ... ACTIVE" badge
(`SettingsScreen.kt:373-397`) asserting the OTP flow is a genuine active security control,
compounding [02-security.md](02-security.md) S1.

### A5 — High: the "Clear App Cache" button's confirmation dialog promises an action it never performs

`SettingsScreen.kt:592-621`. The confirmation dialog text promises: *"This will clear
temporary cache and reset service provider mock listings to latest Indian specialists."* Its
`confirmButton` `onClick` (`:602-609`) does not call `repository.clearAllBookings()`,
`clearAllNotifications()`, or any provider-reset method — it only shows a snackbar saying
*"Cache cleared and provider index refreshed! ✨"*. The user is told an action succeeded that
never ran.

### A6 — Medium: `contentDescription` is `null` on 82 of 130 icon usages, with no documented policy distinguishing decorative from informational icons

Spot-checked examples that are correctly `null` (purely decorative, adjacent to text that
already conveys the same meaning): the `Icons.Default.Navigation` pill icon next to the text
"GPS Active" in `GoogleMapsLiveTrackingCard` (`BookingStatusScreen.kt:787`). This audit did
not exhaustively verify all 82 `null` instances individually — doing so is a larger effort
than this pass covered — but flags that **no comment, style guide, or lint rule in the
repository states the policy** ("decorative icon next to descriptive text → null; standalone
actionable icon → required description"), so consistency across 82 instances is currently
unverifiable without a manual pass, and any new icon added by a future contributor has no
written rule to follow.

### A7 — Medium: the product is structurally India-only, with the assumption baked into the UI rather than stated as a decision

`AuthScreen.kt:245`: a hardcoded `"+91"` prefix with no country-code selector. The README
confirms this is intentional ("All rates and price estimations are rendered in Indian Rupees
(₹)"), so this is very likely a correct product decision, not an oversight — it is flagged
here only because the decision lives implicitly in a hardcoded UI string rather than being
recorded anywhere as a stated scope boundary (see
[14-documentation-and-traceability.md](14-documentation-and-traceability.md) for the
documentation-traceability angle on the same fact). A future contributor extending this app
to another market would discover the India-only assumption by finding hardcoded strings, not
by reading a decision record.

### A8 — Low: no visible internationalization/localization layer

All user-facing strings are hardcoded English literals directly in Compose call sites (no
`strings.xml` resource usage was found for the bulk of UI copy — spot-checked across
`AuthScreen.kt`, `FeedbackScreen.kt`, `SettingsScreen.kt`). For a consumer product, this means
supporting a second language later requires a full string-extraction pass across every
screen file rather than translating a resource bundle. This is also why A4's language
selector cannot possibly work today — there is no resource bundle for it to switch.

## Recommendation summary

| Finding | Action |
|---|---|
| A1 | Do not ship this flow with real payment-app branding attached to fabricated transactions. Either integrate a real UPI/payment gateway before showing any "money added" state, or rebuild the wallet as an explicit mock/credits system with no reference to real payment brands. |
| A2 | Either integrate the Google Maps SDK and real location updates, or drop the "Live Google Maps Tracking" / "GPS Active" / "Google Maps Live" copy and the Uber/Ola comparison entirely, and gate "Simulate Acceptance" out of any build a real customer can install. |
| A3 | Fixed as a side effect of [02-security.md](02-security.md) S1 (real SMS delivery); until then, at minimum update the copy to state the code appears as a device notification, not an SMS. |
| A4 | Either wire these toggles to real behavior (suppressing the corresponding `NotificationHelper` calls, gating dispatch speed) or remove them until they do something; do not ship a security-status "ACTIVE" badge next to a flow described in [02-security.md](02-security.md) S1. |
| A5 | Wire the confirm button to the repository's actual cache-clearing methods, or remove the promise from the dialog copy. |
| A6 | Write down the decorative-vs-informational icon policy (even a short comment convention or a one-paragraph note in this docs folder) and do a follow-up pass verifying all 82 `null` instances against it. |
| A7 | Record the India-only scope as an explicit decision (e.g., in a short `docs/decisions.md` or equivalent), so it is discoverable without grepping for hardcoded strings. |
| A8 | If multi-language support is on the roadmap, begin migrating hardcoded strings to Android string resources now, before the surface area grows further, and remove the non-functional language selector until then. |
