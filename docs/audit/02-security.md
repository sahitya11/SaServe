# 2. Security

**Definition for this codebase:** trust boundaries are explicit and enforced. The trust
boundaries in this stack are: Firestore security rules (who may read/write which document),
the phone/OTP authentication flow (who may act as which customer), local credential storage
(what an attacker with device access can recover), and secrets management (what is committed
to git vs. kept out of it). Every finding below was confirmed by reading the actual
rule/config/code, not inferred.

## Findings

### S1 — Critical: full authentication bypass — any customer account can be taken over with only their phone number

Chain of evidence:

1. `AuthScreen.kt:355-359` generates a 4-digit OTP with `(1000..9999).random()` **on the
   device performing the login**, not on a server.
2. `NotificationHelper.sendOtpSmsNotification` (`NotificationHelper.kt:139-161`) displays that
   OTP in a local Android notification — on the same device that just generated it. No SMS is
   sent to the phone number entered; the number is never actually contacted.
3. `AuthScreen.kt:364` additionally accepts the hardcoded literal `"1234"` unconditionally, on
   top of whatever OTP was generated.
4. `ServiceSyncRepository.loginWithPhoneOtp` (`:595-625`) then looks up (or silently creates) a
   `User` by phone number digits alone and marks the caller as logged in as that user — with
   no password, no possession check, and no server-side verification of any kind.

Put together: an attacker who knows (or guesses/enumerates) a registered customer's phone
number can open the app, enter that number, tap through the on-device "OTP" (which is
generated on *their own* device and shown to *them*), or simply type `1234`, and is now fully
authenticated as that customer — able to view and create bookings, addresses, and wallet
transactions under that identity. There is no phone-possession proof anywhere in this flow.

**This is the highest-severity finding in the entire audit.** It affects the only
authentication path actually reachable from the UI (see
[01-correctness.md](01-correctness.md) C3 for the parallel dead-but-safer paths).

### S2 — Critical: Firestore security rules grant anonymous read and write to three of four collections

`firestore.rules:11-23`:

```
match /providers/{providerId} {
  allow read, write: if true;
}
match /bookings/{bookingId} {
  allow read, write: if true;
}
match /feedback/{feedbackId} {
  allow read, write: if true;
}
```

Only the `users/{userId}` collection is scoped to its owner (`firestore.rules:5-8`, correctly
written). Every `bookings`, `providers`, and `feedback` document — which, per
`FirebaseSyncService.kt`, includes customer name/phone/address, provider name/phone,
**both `startOtp` and `completionOtp` in plaintext** (`bookingToMap`, `:371-372`), and
feedback author name/phone — is readable and writable by any unauthenticated client with the
project's API key. Combined with S3 below, this means the project's entire operational
dataset is currently exposed to the public internet, and any anonymous caller can also
*write* arbitrary data into these collections (inject fake bookings, overwrite a real
customer's booking status, delete/alter provider listings), because Firestore rules are the
only authorization layer — there is no backend server enforcing anything further.

### S3 — Critical: `app/google-services.json` (live Firebase API key and project ID) is committed to git

Confirmed tracked: `git ls-files | grep google-services` returns `app/google-services.json`.
`.gitignore` (repo root) has no entry for it. The file's `project_info.project_id` is
`"saserve"` and it contains a populated `api_key[0].current_key`. A Firebase Web/Android API
key is not a secret in the way a server-side secret is — but combined with S2 (open rules),
it is the only credential an attacker needs to reach the fully-open Firestore project from
outside the app, and it is sitting in git history for anyone who clones or forks the public
repository. This should be treated as a live-project security incident, not a style nit:
rotating it means regenerating Firebase project credentials, which the repository shows no
sign anyone has planned for (see [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md)).

### S4 — High: user passwords are stored in plaintext on-device, with backups enabled

- `Models.kt:107`: `User.password: String` — no hash, no salt, stored as-is.
- `ServiceSyncRepository.saveRegisteredUsers` (`:780-785`) serializes the entire `User` list
  (including this plaintext field) via Gson into `SharedPreferences` under
  `key_registered_users_v2`.
- `saveUser` (`:440-449`) does the same for the single "current user" record under
  `key_current_user`.
- `AndroidManifest.xml:11`: `android:allowBackup="true"` — the default Android auto-backup
  mechanism (and `adb backup`, on a device with USB debugging enabled) can export this
  `SharedPreferences` file off the device.

This particular password field is, per S1/C3, on a dead code path not reachable from the
current UI — but it is still shipped, still compiles, still runs if invoked, and still
represents exactly the anti-pattern (plaintext credential storage) that a security review
must flag regardless of current reachability, since dead code has a way of getting
reconnected.

### S5 — Medium: no input validation on email or phone at any trust boundary

Grepping the whole module for email-format validation (`Regex` containing `@`,
`isValidEmail`, `Patterns.EMAIL_ADDRESS`) returns nothing. Phone number handling is limited to
stripping non-digit characters (`Regex("[^0-9]")`, duplicated 8 times — see
[06-no-duplication.md](06-no-duplication.md)) and a bare length check (`>= 10` digits); no
validation that the result is a plausible number, and no validation at all on the `email`
field accepted by `registerWithFirebase`/`FirebaseAuthService.registerUser` before it is
handed to `auth.createUserWithEmailAndPassword`.

### S6 — Low: no rate limiting or lockout on OTP send/resend

`AuthScreen.kt`'s "Resend OTP" button (`:325-334`) can be tapped without limit, each time
generating and locally-notifying a fresh code with no cooldown, no maximum-attempts counter,
and no lockout after repeated failed verification attempts. In the current implementation
this has no real teeth (per S1, verification itself is bypassable), but it should be listed
now so it is not forgotten when S1 is fixed and a real SMS provider is introduced — at that
point, unlimited resends become a direct cost/abuse vector against the SMS provider.

## Recommendation summary

| Finding | Action |
|---|---|
| S1 | Do not ship this login path as-is. Integrate a real phone-verification provider (Firebase Phone Auth is already a dependency-compatible option given `firebase-auth` is present) so the OTP is generated and checked server-side, and delete the `"1234"` bypass unconditionally. |
| S2 | Rewrite `firestore.rules` so `bookings`/`providers`/`feedback` require `request.auth != null` at minimum, and scope `bookings`/`feedback` reads/writes to the authenticated customer's own `customerId`/`userId` field, and provider-side writes to a verified provider role. |
| S3 | Rotate the Firebase project's API key and restrict it (HTTP referrer / package name / SHA-1 restriction in Google Cloud Console), then remove `app/google-services.json` from git history and add it to `.gitignore`, distributing it to developers via a secrets mechanism instead. |
| S4 | If the password-based path (C3) is kept, hash passwords (e.g., via Firebase Auth, which already does this correctly for the unused `FirebaseAuthService` path) instead of storing them raw; if it is deleted as recommended in C3, this finding is moot. Independently, reconsider `allowBackup` given any locally-cached PII. |
| S5 | Add a shared email-format validator and a shared phone-normalization/validation helper (see D-series in [06-no-duplication.md](06-no-duplication.md)) used at every entry point that accepts these fields. |
| S6 | Add a per-phone-number cooldown and max-attempt counter once a real SMS provider is in place. |
