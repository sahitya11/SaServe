# SaServe — Remediation To-Do List

Derived from the 15-dimension audit in this folder (see [README.md](README.md) for the
index and severity key). This list does not re-explain any finding — each item names the
finding ID and file to read for full detail, evidence, and the recommended fix. Work through
the phases in order: later phases assume earlier ones are done (e.g., you cannot sensibly
add tests for `ServiceSyncRepository` before it is split into testable pieces).

Check items off as they land. Nothing here touches code by itself — this is the plan, not
the patch.

---

## Phase 0 — Stop the live exposure (do first, today)

These are active, exploitable-right-now conditions on whatever Firebase project this app
currently points to. Everything else can wait a day; this cannot.

- [ ] Rotate the Firebase project's API key / credentials — [02-security.md](02-security.md) S3, [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M4
- [ ] Remove `app/google-services.json` from git history and add it to `.gitignore`; redistribute it to developers out-of-band — [02-security.md](02-security.md) S3, [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M4
- [ ] Rewrite `firestore.rules` so `providers`, `bookings`, and `feedback` require authentication and are scoped to their owner — [02-security.md](02-security.md) S2, [03-privacy.md](03-privacy.md) P1
- [ ] Decide whether any already-exposed real user data (if this project has ever been used by a real customer) needs a breach/incident response — [03-privacy.md](03-privacy.md) P1

## Phase 1 — Stop the app from claiming things it doesn't do

Every item here is a feature that actively tells the user something false (a payment
happened, a location is tracked, an SMS was sent, a setting took effect). Fix or remove
before anything else ships, independent of severity label, because these are trust and
correctness problems at once.

- [ ] Replace the fake on-device OTP with real phone verification; delete the `"1234"` bypass — [01-correctness.md](01-correctness.md) C1, [02-security.md](02-security.md) S1, [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A3
- [ ] Replace or remove the fake wallet "Add Money via UPI" flow (Google Pay/PhonePe/Paytm/BHIM picker that never processes a payment) — [01-correctness.md](01-correctness.md) C7, [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A1, [03-privacy.md](03-privacy.md) P5
- [ ] Replace or remove the fake "Live Google Maps Tracking" card, its "Google Maps Live" watermark, and the "just like Uber & Ola" fake radar search — [01-correctness.md](01-correctness.md) C2, [07-simplicity.md](07-simplicity.md) SI2, [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A2
- [ ] Gate the customer-facing "Simulate Acceptance" button out of any real build, or remove it — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A2
- [ ] Fix GPS auto-detect so it reports failure instead of fabricating a "Simulated City Center, Karnataka" success — [01-correctness.md](01-correctness.md) C8
- [ ] Wire up the Settings notification/dispatch toggles and language selector to real behavior, or remove them — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A4
- [ ] Wire up "Clear App Cache" to an actual cache-clearing action, or remove the button — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A5
- [ ] Fix "Upload from Gallery" profile photo so the picked image is actually rendered — [01-correctness.md](01-correctness.md) C10
- [ ] Replace hardcoded stale calendar dates (`"Today, Sep 5"`) in the direct provider-booking flow with dates computed from the device clock — [01-correctness.md](01-correctness.md) C9

## Phase 2 — Close the remaining security gaps

- [ ] Decide the fate of the plaintext-password local auth path (see Phase 5 dead-code decision) and stop storing raw passwords if it's kept — [02-security.md](02-security.md) S4
- [ ] Add shared email-format and phone-number validation at every entry point — [02-security.md](02-security.md) S5
- [ ] Add OTP resend rate limiting / lockout once real SMS verification exists (Phase 1) — [02-security.md](02-security.md) S6

## Phase 3 — Close the remaining privacy gaps

- [ ] Add a real deletion path: local "clear" actions must also delete the corresponding Firestore documents — [03-privacy.md](03-privacy.md) P2
- [ ] Stop using the phone number as the identity/lookup key; use the opaque `User.id` internally — [03-privacy.md](03-privacy.md) P3
- [ ] Define and enforce a retention policy for feedback records containing name/phone — [03-privacy.md](03-privacy.md) P4
- [ ] Move customer support contact off a personal Gmail address onto a business-owned, access-controlled channel — [03-privacy.md](03-privacy.md) P6

## Phase 4 — Reliability

- [ ] Log (don't silently swallow) background Firebase sync failures in `ServiceSyncRepository.init` — [04-reliability.md](04-reliability.md) R1
- [ ] Add retry-with-backoff for realtime listener registration, plus a manual retry affordance — [04-reliability.md](04-reliability.md) R2
- [ ] Protect booking mutations from concurrent overwrite (Firestore transaction or version field) — [04-reliability.md](04-reliability.md) R3
- [ ] Give `broadcastServiceDispatch` a real timeout and a sweep for bookings orphaned mid-dispatch — [04-reliability.md](04-reliability.md) R4
- [ ] Add a reconciliation pass between local cache and Firestore once R1–R3 land — [04-reliability.md](04-reliability.md) R5
- [ ] Inspect the `Result` returned from every fire-and-forget Firestore sync call instead of discarding it — [04-reliability.md](04-reliability.md) R6
- [ ] Move the `Geocoder` reverse-lookup call off the main/effect thread — [04-reliability.md](04-reliability.md) R7

## Phase 5 — Break up the two God objects

Do this before Phase 9 (testability) — you cannot unit-test logic that is still welded into
an untestable class.

- [ ] Split `ServiceSyncRepository` along its six responsibilities (auth, persistence, wallet, addresses, notification-composition, seed data); inject `FirebaseAuthService`/`FirebaseSyncService` instead of constructing them internally — [05-reusability.md](05-reusability.md) RU1, [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M2
  - Use `FirebaseAuthService`/`FirebaseSyncService` as the reference shape for the split — they are already correctly decomposed — [05-reusability.md](05-reusability.md) RU4 (no fix needed, reference only)
- [ ] Delete the two unreachable authentication paths (password+SharedPreferences, unused real Firebase email/password), or mark them explicitly provisional — [01-correctness.md](01-correctness.md) C3, [07-simplicity.md](07-simplicity.md) SI1
- [ ] Split `CustomerScreens.kt` into one file per screen, matching the rest of the `screens/customer` package — [05-reusability.md](05-reusability.md) RU2, [08-readability.md](08-readability.md) RE3, [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M2
- [ ] Move the ~720 lines of hardcoded seed-provider data out of `ServiceSyncRepository` into a bundled JSON asset — [07-simplicity.md](07-simplicity.md) SI3

## Phase 6 — Kill duplication

- [ ] Collapse the category→icon/drawable/gradient/iconName mapping (4 separate places) into one canonical source on the `ServiceCategory` enum — [01-correctness.md](01-correctness.md) C4, [06-no-duplication.md](06-no-duplication.md) D1, [05-reusability.md](05-reusability.md) RU3
- [ ] Extract one shared OTP-generation function/constant (used 8 times today) — [06-no-duplication.md](06-no-duplication.md) D2
- [ ] Extract one shared phone-digit-normalization helper (used 8 times today) — [06-no-duplication.md](06-no-duplication.md) D3
- [ ] Extract one shared `pushNotification(title, message, bookingId)` helper (repeated ~15 times) — [06-no-duplication.md](06-no-duplication.md) D4, [08-readability.md](08-readability.md) RE4
- [ ] Resolve the byte-identical duplicate React files (`components/*.jsx` vs `src/components/*.jsx`) — [06-no-duplication.md](06-no-duplication.md) D5, [05-reusability.md](05-reusability.md) RU5 (superseded by the full rebuild in Phase 15 below, which replaces rather than just de-duplicates this code)
- [ ] Extract one shared rupee-currency formatter (25+ ad hoc call sites) — [06-no-duplication.md](06-no-duplication.md) D6
- [ ] Give `SubServiceItem` numeric `price`/`rating` fields instead of pre-formatted strings; format only at render time — [09-consistency.md](09-consistency.md) C9-1, C9-2, [08-readability.md](08-readability.md) RE5
- [ ] Replace the rating-average format/reparse round trip with direct numeric rounding — [01-correctness.md](01-correctness.md) C5
- [ ] Resolve or document the dead `else` branch in `displayBookingId`'s hash-based fallback — [01-correctness.md](01-correctness.md) C6

## Phase 7 — Consistency cleanup

- [ ] Remove the unused `navigation-compose` dependency, or migrate `MainAppHost` onto it — [09-consistency.md](09-consistency.md) C9-3
- [ ] Pick one convention for color access (semantic alias vs. `MaterialTheme.colorScheme` direct) and apply it everywhere — [09-consistency.md](09-consistency.md) C9-4
- [ ] Pick one way to obtain the repository singleton (`ServiceSyncApp.repository` vs. direct `getInstance`) and delete the other — [09-consistency.md](09-consistency.md) C9-5
- [ ] Normalize phone-number storage to one format everywhere (ties into D3) — [09-consistency.md](09-consistency.md) C9-6
- [ ] Replace the hardcoded light-gray background in `ApplianceClayImage` with a theme-aware token — [09-consistency.md](09-consistency.md) C9-7
- [ ] Derive the "N categories" home-screen label from `ServiceCategory.values().size` instead of a hardcoded literal — [09-consistency.md](09-consistency.md) C9-8
- [ ] Replace the `Color`-equality dark-mode check in `StatusXBg` properties with the `isDark` boolean already computed in `ServiceSyncTheme` — [07-simplicity.md](07-simplicity.md) SI4

## Phase 8 — Readability polish

- [ ] Rename `BackgroundLight`/`SurfaceLight`/`TextPrimary`/`TextSecondary` to names that reflect their theme-adaptive behavior (or delete the aliases) — [08-readability.md](08-readability.md) RE1
- [ ] Add a one-line comment on `displayBookingId`'s fallback explaining which caller needs it and the accepted collision risk — [08-readability.md](08-readability.md) RE2

*(RE3, RE4, RE5 are covered by Phases 5 and 6 above.)*

## Phase 9 — Testability

Requires Phase 5 (God-object split) to be underway — see [10-testability.md](10-testability.md) T2.

- [ ] Add `app/src/test/` and at least one runnable check per non-trivial rule — [10-testability.md](10-testability.md) T1
- [ ] Extract pure business rules (cancellation fee, wallet guard, OTP/ID generation) out of `ServiceSyncRepository` so they can be unit-tested without a `Context`/Firebase — [10-testability.md](10-testability.md) T2
- [ ] Add unit tests for each extracted rule and its edge cases — [10-testability.md](10-testability.md) T3
- [ ] Add a round-trip serialization test per model (`Booking`, `ServiceProvider`) against `FirebaseSyncService`'s mappers — [10-testability.md](10-testability.md) T4

## Phase 10 — Performance and cost

- [ ] Stop the unconditional full re-sync of all cached data on every cold start; sync only what changed — [11-performance-and-cost.md](11-performance-and-cost.md) PC1
- [ ] Confirm listener fan-out cost drops once Firestore rules are scoped per-user (Phase 0) — [11-performance-and-cost.md](11-performance-and-cost.md) PC2
- [ ] Enable `isMinifyEnabled = true` for release builds — [11-performance-and-cost.md](11-performance-and-cost.md) PC3, [07-simplicity.md](07-simplicity.md) SI5
- [ ] Remove unused dependencies: `firebase-database`, `navigation-compose` (unless adopted in Phase 7), `kotlinx-coroutines-play-services` — [11-performance-and-cost.md](11-performance-and-cost.md) PC4, [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M5
- [ ] Revisit the fixed 2.6s dispatch delay once a real matching backend exists — [11-performance-and-cost.md](11-performance-and-cost.md) PC5

## Phase 11 — Observability

- [ ] Add logging to every currently-silent `catch` block in `ServiceSyncRepository` (ties into R1) — [12-observability.md](12-observability.md) O1
- [ ] Log the booking `id` alongside every lifecycle state-transition line, for end-to-end traceability — [12-observability.md](12-observability.md) O2
- [ ] Audit `Log.d` calls for embedded names/identifiers before adding any persisted/off-device logging (e.g. Crashlytics) — [12-observability.md](12-observability.md) O3

## Phase 12 — Accessibility and UX remainder

- [ ] Write down the decorative-vs-informational icon `contentDescription` policy and re-verify all 82 `null` instances against it — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A6
- [ ] Record the India-only scope as an explicit, discoverable product decision — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A7
- [ ] If multi-language support is intended, begin migrating strings to Android string resources — [13-accessibility-and-ux.md](13-accessibility-and-ux.md) A8

## Phase 13 — Documentation and traceability

- [ ] Replace the stale machine-specific local path in `README.md`'s setup instructions with generic clone-and-open steps — [14-documentation-and-traceability.md](14-documentation-and-traceability.md) DT1
- [ ] Write a short decision record covering why each major shortcut existed (OTP, maps, wallet, Firestore rules) and what replaced it — [14-documentation-and-traceability.md](14-documentation-and-traceability.md) DT2
- [ ] Adopt a "why, not just what" convention in commit messages going forward — [14-documentation-and-traceability.md](14-documentation-and-traceability.md) DT3
- [ ] Add a one-line comment at any remaining deliberate shortcut, naming it as intentional — [14-documentation-and-traceability.md](14-documentation-and-traceability.md) DT4

## Phase 14 — Maintainability and change safety

- [ ] Add a CI workflow gating merges on build (and, once Phase 9 lands, test) — [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M1
- [ ] Add a schema-version field to synced documents; log instead of silently defaulting missing fields — [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M3
- [ ] Write a credential-rotation runbook (ties back to Phase 0) — [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M4
- [ ] Confirm dead dependencies/config are fully removed after Phase 10 — [15-maintainability-and-change-safety.md](15-maintainability-and-change-safety.md) M5

## Phase 15 — Marketing website rebuild (independent of Phases 0–14, no shared dependency)

Derived from [../landing-page-rebuild-plan.md](../landing-page-rebuild-plan.md) (deep audit +
rebuild plan for the React landing-page sources, run separately from the 15-dimension audit
above). This phase touches only `components/`, `src/components/`, and `assets/logo.svg` —
none of it depends on or blocks the Android-app phases above, and none of them block it.

- [ ] Decide repo placement: in-repo `/web` vs. a dedicated marketing-site repo — [landing-page-rebuild-plan.md](../landing-page-rebuild-plan.md) Part H Phase 0, Part I
- [ ] Scaffold Next.js (App Router) + TypeScript + Tailwind + Framer Motion; delete the duplicate `components/`/`src/components/` split; wire the design tokens (colors, Space Grotesk + Plus Jakarta Sans) — Part G, Part C, Part H Phase 1
- [ ] Build the shared layout: rebuilt Navbar (real destinations, working CTAs, mobile drawer) and Footer, plus base components (Button, Badge, Section, Card) — Part H Phase 2
- [ ] Build Home (`/`) — Hero, trust bar, problem/solution, how-it-works, categories, trust & safety, for-professionals CTA, testimonials, investor teaser, download CTA, footer — Part E, Part H Phase 3
- [ ] Build `/for-professionals` (supply-side pitch — currently zero acquisition content for professionals anywhere) — Part F1
- [ ] Build `/about` (story, team, milestones) — Part F2
- [ ] Build `/investors` (market size, business model, traction, roadmap, team, contact — guardrail: do not repeat the app's fabricated-feature claims from Phase 1 above) — Part F3
- [ ] Build `/download` (store badges, QR code, real screenshots) — Part F4
- [ ] Build `/legal/privacy` and `/legal/terms` — higher priority than typical boilerplate, since the app has no privacy policy anywhere today (ties to Phase 3 above, [03-privacy.md](03-privacy.md)) and Google requires one for the Play Store listing — Part F5
- [ ] Build `/contact` (support + investor inquiry paths) — Part F6
- [ ] SEO pass (per-route metadata, OG images, `sitemap.xml`, `robots.txt`), accessibility pass (WCAG AA), performance pass (Lighthouse ≥ 90 mobile, LCP < 2.5s, CLS < 0.1) — Part G, Part H Phase 5
- [ ] Analytics wiring, deploy (Firebase Hosting or separate target per the repo-placement decision), cross-device/breakpoint QA (375/768/1024/1440px) — Part H Phase 6
- [ ] Fill in the placeholder content fields as real inputs arrive (trust-bar numbers, monetization model, founder bios, Play Store link, testimonials) rather than shipping invented numbers or quotes — Part I

---

## Final gate — before calling any of this "done"

- [ ] Re-run all 15 dimension checks in this folder against the changed code, per the original audit brief's own rule: *"Before calling anything done, check it against all fifteen dimensions."*
