# 14. Documentation and Traceability

**Definition for this codebase:** anyone can see why a change exists. There is no `state/`
or `architecture/` folder convention, no P-item/plan-task ticketing system, and no
decision-record process in this repository today — this dimension's findings are therefore
about the *absence* of that infrastructure as much as about specific defects.

## Findings

### DT1 — High: `README.md` contains a stale, machine-specific local path as its primary setup instruction

`README.md:33-43`, "How to Run in Android Studio," step 3:

```
Select the folder:
C:\Users\sk621\.gemini\antigravity\scratch\servicesync
```

This is a Windows path from what appears to be a specific developer's local scratch
directory (the `.gemini/antigravity/scratch/servicesync` structure suggests it was generated
by an AI coding tool's scratch workspace), copy-pasted into the README rather than replaced
with "clone this repository and open its root folder." Anyone following this instruction
literally will be looking for a folder that does not exist on their machine.

### DT2 — High: no architecture, decision, or change-rationale documentation existed anywhere prior to this audit

Before this audit folder was created, the *only* prose documentation in the entire
repository was `README.md`. There is no record anywhere of:

- Why three authentication systems coexist ([01-correctness.md](01-correctness.md) C3).
- Why "Live Google Maps Tracking" is simulated rather than integrated
  ([01-correctness.md](01-correctness.md) C2).
- Why the OTP flow was built the way it was ([02-security.md](02-security.md) S1) — whether
  this was a deliberate, temporary demo shortcut or simply never revisited.
- Why Firestore rules are fully open ([02-security.md](02-security.md) S2) — whether this was
  a deliberate development-phase choice meant to be tightened before any real launch, or an
  oversight.

Without a decision record, this audit cannot distinguish "known, temporary, tracked
shortcut" from "unknown, permanent, accidental gap" for any of the above — which materially
changes how urgently each should be treated. This audit has assumed the more conservative
reading (that these are unintentional, unless the repository owner states otherwise) for
severity purposes in the other fourteen files.

### DT3 — Medium: commit messages describe *what* changed, never *why*

From `git log --oneline -5` at the time of cloning: `"fix: remove 24x7 customer service card
from homescreen"`, `"feat: complete UI enhancements, Google Maps live tracking, OTP
progression, specialist portraits, tipping, dedicated My Profile page, and dark mode
default"`, `"Eliminate initial white window screen and keep only the animated dark loading
logo screen on app launch"`, `"Restore broadcast service dispatch..."`, `"Fix logo alignment
and app icon centering..."`. These are accurate, readable descriptions of the change itself —
but none references a requirement, an issue, a user report, or a design rationale. There is
no ticketing system referenced anywhere (no issue-tracker links, no `Fixes #N`), so "why did
we build the OTP flow this way" or "why is Google Maps simulated" cannot be answered by
`git log` or `git blame` the way the audit brief's own traceability standard expects.

### DT4 — Low: no code comments anywhere flag intentional simplifications or known limitations

Cross-referencing every finding in [07-simplicity.md](07-simplicity.md): none of the
deliberate-looking shortcuts in this codebase (the mock map, the seed data, the unused auth
paths) carry any marker comment distinguishing "temporary, known limitation" from
"permanent design." This is the same gap as DT2, restated at the code level rather than the
repository level: even a single-line comment at each shortcut's definition site would have
closed most of this gap without requiring a separate documentation process.

## Recommendation summary

| Finding | Action |
|---|---|
| DT1 | Replace the README's setup instructions with generic clone-and-open steps that don't reference any specific machine's local path. |
| DT2 | Now that `docs/audit/` exists, add a short `docs/decisions.md` (or similar) recording, going forward, why each major shortcut in this codebase exists and what would need to happen to remove it — starting with the four listed here. |
| DT3 | Adopt a lightweight convention (even just "one sentence of why, in the commit body, for any change bigger than a typo fix") going forward; retrofitting historical commits is not necessary or recommended. |
| DT4 | Add a one-line comment at each shortcut's definition site (the mock map, the seed data block, the unused auth paths) stating it is intentional and provisional, cross-referencing the relevant audit finding if useful. |
