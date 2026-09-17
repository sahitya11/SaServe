# 3. Privacy

**Definition for this codebase:** personal data should be minimized, access-scoped, and never
exposed somewhere broader than the person who owns it can control. That principle is judged
here against what data this app actually collects (name, phone, address, booking history,
wallet transactions, feedback) and where it ends up.

This dimension is tightly coupled to [02-security.md](02-security.md) — a Firestore rule
misconfiguration is a security defect *and* a privacy defect, because the same open rule that
lets an anonymous caller write also lets them read every customer's personal data. Findings
that are pure duplicates of the security file are not repeated in full here; only the
privacy-specific angle is detailed.

## Findings

### P1 — Critical: full customer PII and live booking OTPs are world-readable

This is the privacy-impact restatement of [02-security.md](02-security.md) S2. Concretely,
`FirebaseSyncService.bookingToMap` (`FirebaseSyncService.kt:353-381`) writes, per booking:

- `customerName`, `customerPhone`, `customerAddress` — full real-world identity and home
  address of a customer requesting a home-service visit.
- `providerName`, `providerPhone` — the specialist's identity.
- `startOtp`, `completionOtp` — the exact codes meant to prove, in person, that the right
  provider showed up and the right service was completed.

All of it is stored in the `bookings` collection, which `firestore.rules:16-18` opens to
`allow read, write: if true`. Anyone with the project's API key (see S3 in
[02-security.md](02-security.md) — that key is itself committed to git) can enumerate every
booking ever made and read this data directly, with no authentication. This is the single
most serious privacy exposure in the codebase: it is not a hypothetical leak, it is a fully
open, indexed, queryable collection of real names, phone numbers, home addresses, and
security codes.

### P2 — High: no data-deletion path — "clear" operations only touch the local device cache

`ServiceSyncRepository.clearAllBookings()` (`:1528-1531`) and `clearAllNotifications()`
(`:1289-1292`) remove data from the local `SharedPreferences` cache only. Neither function —
nor any other function in the repository — issues a corresponding Firestore delete. Once a
booking, provider, or feedback document is synced (which happens automatically on almost
every write path — see `saveBookings`, `saveProviders`, `submitFeedback`), it persists in
Firestore indefinitely with no TTL, no archival policy, and no user-facing way to actually
delete it from the cloud. A user who taps "clear" reasonably believes their data is gone; it
is not.

### P3 — Medium: real phone number is the primary identity key, with no separation between an internal ID and the PII

Multiple lookups throughout `ServiceSyncRepository` (`registerCustomer`, `loginCustomer`,
`registerWithPhoneOtp`, `loginWithPhoneOtp`) match users by normalized phone-number digits
directly (`it.phone.replace(Regex("[^0-9]"), "") == cleanPhone`). While every `User` does have
an opaque `id` (e.g. `"cust_" + UUID...`), the phone number itself is the effective lookup key
for "who is this," which means the PII value doubles as the identity index everywhere it is
stored (local prefs, Firestore, in-memory maps) rather than being a separately-protected
attribute of an opaque identity.

### P4 — Medium: feedback documents retain full contact details indefinitely, with no purpose limitation

`AppFeedback` (`Models.kt:210-219`) stores `userName` and `userPhone` alongside free-text
`suggestions`. `submitFeedback` (`ServiceSyncRepository.kt:369-406`) syncs this to the open
`feedback` Firestore collection (P1) with no expiry and no stated retention policy anywhere
in the repository. A user submitting a complaint about a specialist has no way to know their
name and phone number attached to that complaint will remain queryable by anyone,
indefinitely.

### P5 — High: wallet transaction descriptions embed a fabricated UPI reference ID that looks like a real payment record

`ServiceSyncRepository.addMoneyToWallet` (`:1338-1355`) generates a synthetic
`"UPI/$refNum"` string and stores it as `upiRefId` on a `WalletTransaction` that is purely
local, mock currency (the entire wallet system has no real payment gateway integration
anywhere in the codebase). Revised upward from the initial pass of this audit: this is not
merely an internal-confusion risk — per [01-correctness.md](01-correctness.md) C7 and
[13-accessibility-and-ux.md](13-accessibility-and-ux.md) A1, the UI actively presents this
fabricated reference to the *user* as a real transaction record (`"Ref: ${tx.upiRefId}"`,
`CustomerScreens.kt:2793`) next to a named real payment app and a "UPI Verified" badge. A
user who disputes a "missing deposit" with real-world support has nothing but this synthetic
string to point to, and it was never associated with any real payment rail.

### P6 — Low: customer support contact channels route to a personal Gmail address, not a business-owned alias

`sahaditya1804@gmail.com` and `7488274632` are hardcoded as the app's official
"Customer Care" contact everywhere support is offered (`SettingsScreen.kt:532`,
`HelpSupportScreen.kt:2822-2823`). Both the "Email Support" button and the in-app "Send Help
Message" form (`HelpSupportScreen.kt:3013-3101`) route every customer support request —
including whatever personal details a customer includes — directly to what appears to be an
individual's personal Gmail inbox rather than a monitored, access-controlled business support
channel. This is an operational/data-handling observation rather than a code defect: personal
inboxes typically lack the access controls, retention policies, and staff-turnover handling a
business alias would have.

## Recommendation summary

| Finding | Action |
|---|---|
| P1 | Fixed by the same rule change recommended in [02-security.md](02-security.md) S2: scope reads/writes to the owning authenticated user. Additionally consider whether `startOtp`/`completionOtp` need to be stored in Firestore at all, versus generated and verified through a narrower, purpose-built endpoint. |
| P2 | Add a real deletion path: when a user clears bookings/notifications, issue the corresponding Firestore document deletes (or a soft-delete flag honored everywhere the data is read back), and state a retention policy. |
| P3 | Continue using the opaque `User.id` as the storage/lookup key internally, and treat phone number purely as a verification input, not an index, once S1 (real OTP) is implemented server-side. |
| P4 | Define and enforce a retention window for feedback records, or strip direct contact fields from the synced document and keep them only where support actually needs to reach the user. |
| P5 | Do not ship the fake wallet flow (see C7/A1 recommendations); if a mock mode is kept for development, label the reference visibly as simulated (e.g. `mockUpiRefId`) rather than `upiRefId`. |
| P6 | Route support contact through a business-owned email/phone with proper access control before real customers use this app. |
