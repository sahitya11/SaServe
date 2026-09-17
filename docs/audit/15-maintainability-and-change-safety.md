# 15. Maintainability and Change Safety

**Definition for this codebase:** it is easy and safe to change this code later. The
applicable questions are: is there a CI gate before code reaches `main`; is the blast radius
of a typical change small; and does the codebase have an established, visible mechanism for
evolving a data shape without silently coercing old and new data together.

## Findings

### M1 — High: no CI configuration exists anywhere in the repository

`find . -iname "*.yml" -o -iname "*.yaml"` (excluding Gradle wrapper internals) and a check
for a `.github/` directory both return nothing. There is no automated build, lint, or test
gate of any kind before a commit reaches `main` — combined with
[10-testability.md](10-testability.md) T1 (zero tests exist to run in a CI job anyway), every
change today ships on the strength of manual review alone, with no automated signal at all.

### M2 — High: two files carry a blast radius far larger than any single change should need to touch

`ServiceSyncRepository.kt` (2,278 lines, six unrelated responsibilities — see
[05-reusability.md](05-reusability.md) RU1) and `CustomerScreens.kt` (4,344 lines, nine
unrelated screens — see [05-reusability.md](05-reusability.md) RU2) mean that almost any
change to booking logic, wallet logic, or any customer-facing screen requires opening a file
whose other 80-95% is unrelated to that change. This has two concrete maintainability
consequences beyond the readability angle already covered in
[08-readability.md](08-readability.md): (a) a code reviewer cannot scope a diff's blast
radius from the file list alone — "changed `CustomerScreens.kt`" could mean anything from a
wallet-screen typo fix to a home-screen logic change — and (b) two engineers working on
different screens/features that happen to live in the same file are far more likely to hit
merge conflicts than they would be with one-file-per-screen/responsibility.

### M3 — Medium: malformed or older Firestore documents are silently coerced with hardcoded fallback values instead of an explicit migration path

`FirebaseSyncService.documentToBooking` (`:383-418`) supplies a literal fallback for every
field read from a Firestore document, e.g. `doc.getString("startOtp") ?: "1234"` and
`doc.getString("completionOtp") ?: "5678"` (`:408-409`) — note that the `startOtp` fallback
value is the exact same string as the hardcoded OTP bypass in
[02-security.md](02-security.md) S1, which is either a striking coincidence or evidence the
same placeholder value was reused across both, unintentionally connecting two unrelated
pieces of "temporary" logic. More generally: if a document's shape changes in the future
(a field renamed, a new required field added), old documents will not be flagged, logged, or
migrated — they will simply be read back with silently substituted defaults, indistinguishable
from a real document that happens to have those values. There is no schema version field on
any synced document and no migration mechanism of any kind.

### M4 — Medium: `app/google-services.json` being committed to git means the project's core credential cannot be rotated by a code change alone

Restated from the maintainability angle (full detail in
[02-security.md](02-security.md) S3): because the Firebase API key lives in git history,
"rotate this credential" is not a pull request — it requires purging or accepting the old
key's continued presence in history, regenerating the Firebase project credential, and
distributing the new file out-of-band to every developer. The repository currently has no
process, script, or documentation describing how this would be done, which means the team's
practical ability to respond quickly to a credential-compromise event is currently zero.

### M5 — Low: three unused dependencies and one disabled-but-configured build feature quietly resist future cleanup

`firebase-database`, `navigation-compose`, and `kotlinx-coroutines-play-services` ([11-performance-and-cost.md](11-performance-and-cost.md) PC4), plus the inert `proguard-rules.pro` rule ([07-simplicity.md](07-simplicity.md) SI5), are all small individually but compound the same maintainability problem: a future contributor auditing "what does this app actually depend on and use" cannot answer that question from the build file alone, and has to do the same kind of cross-referencing this audit did to find out.

## Recommendation summary

| Finding | Action |
|---|---|
| M1 | Add a minimal CI workflow (even just `./gradlew assembleDebug` and, once [10-testability.md](10-testability.md) T1 is addressed, `./gradlew test`) gating merges to `main`. |
| M2 | Split both files per the recommendations in [05-reusability.md](05-reusability.md) RU1/RU2; this is the single highest-leverage maintainability fix in the entire audit, since it also improves readability, testability, and reusability simultaneously. |
| M3 | Add a schema-version field to synced documents, and log (rather than silently default) whenever a document is missing an expected field, so drift is visible instead of invisible. |
| M4 | Write down a short credential-rotation runbook now, before it is needed under time pressure, and complete the rotation recommended in [02-security.md](02-security.md) S3. |
| M5 | Remove unused dependencies and dead configuration as part of ordinary hygiene (see [11-performance-and-cost.md](11-performance-and-cost.md) PC4, [07-simplicity.md](07-simplicity.md) SI5) so the build file stays a trustworthy record of what the app actually uses. |
