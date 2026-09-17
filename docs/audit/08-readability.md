# 8. Readability

**Definition for this codebase:** a stranger understands the code at 3 a.m., without needing
to trace three other files first. Names say what a thing is; comments (where present) explain
why, not what; and a file's size and shape don't force a reader to hold unrelated context in
their head at once.

## Findings

### RE1 — High: `BackgroundLight`, `SurfaceLight`, `TextPrimary`, `TextSecondary` are misleadingly named — they are theme-adaptive, not fixed light-mode values

`Color.kt:33-39`:

```kotlin
val BackgroundLight: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceLight: Color @Composable get() = MaterialTheme.colorScheme.surface
...
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
```

Despite the `...Light` suffix and the plain names `TextPrimary`/`TextSecondary` (which read
as fixed brand colors), every one of these properties actually resolves dynamically through
`MaterialTheme.colorScheme`, and therefore correctly follows the active dark/light theme. The
naming actively misleads: 294 call sites across the screen layer use `BackgroundLight` inside
what is, in every one of those files, dark-theme-aware Compose code (confirmed by grep — see
[09-consistency.md](09-consistency.md) C9 for the full count). A future engineer skimming
`containerColor = BackgroundLight` in `AuthScreen.kt` would reasonably conclude this ignores
dark mode and "fix" it by hardcoding a real light color — which would break dark mode for
that screen. The property names should describe what they *are* (theme-relative semantic
roles), not what they resemble.

### RE2 — Medium: `displayBookingId`'s fallback branch is a dense one-liner with no comment explaining its purpose or risk

`Models.kt:188-189` (quoted in full in
[01-correctness.md](01-correctness.md) C6) packs a conditional check, an absolute-value hash,
string padding, and truncation into a single expression with zero comment. A reader needs to
mentally execute the whole expression to discover it's a fallback ID synthesizer for
malformed/legacy `Booking` records, and nothing tells them whether hash-collision risk here
was considered and accepted, or simply never thought about.

### RE3 — Medium: `CustomerScreens.kt` (4,344 lines) requires scrolling through unrelated screens to find any one of them

Named in full in [05-reusability.md](05-reusability.md) RU2. From a pure readability
standpoint (independent of the reusability argument): a reader who wants to understand
`WalletScreen` must open a file whose surrounding ~4,000 lines are `CustomerHomeScreen`,
`ProviderListScreen`, `ProviderDetailScreen`, and six other unrelated screens. IDE
"jump to definition" mitigates this somewhat, but a linear read, a diff review, or a code
search grep result all become noisier than they would be with one file per screen — which is
exactly the layout the other five screen files in the same directory already use.

### RE4 — Medium: repository methods interleave three unrelated concerns inline, obscuring what each method is actually "about"

Almost every mutating method on `ServiceSyncRepository` (e.g. `cancelBookingWithDetails`,
`:1123-1162`) performs, inline and in sequence: (1) the actual business-rule computation
(here, the late-cancellation fee logic), (2) local-list mutation and `SharedPreferences`
persistence, and (3) construction and dispatch of an in-app notification (see
[06-no-duplication.md](06-no-duplication.md) D4). A reader trying to answer "what is the
cancellation fee rule?" must first read past unrelated list-splicing and notification-string
assembly to find the two lines that actually compute the fee. Extracting the notification
boilerplate (recommended in D4) would, as a side effect, substantially improve this: each
method would shrink to roughly its actual business logic plus one call.

### RE5 — Low: `SubServiceItem.rating`/`reviewCount` are formatted display strings baked into the domain model

`SubServices.kt:14-15` — `rating: String` holds values like `"4.88 ★"` and `reviewCount:
String` holds values like `"3.2k"`. A reader encountering `subService.rating` in a call site
cannot tell, without checking the model definition, whether they're holding a raw number or a
pre-formatted, unicode-glyph-embedded display string — and the two other rating
representations in the codebase (`ServiceProvider.rating: Float`, `Review.rating: Float`) use
the numeric form, so the inconsistency (detailed further in
[09-consistency.md](09-consistency.md)) doubles as a readability trap for anyone assuming
"rating" means the same shape everywhere in this codebase.

## Recommendation summary

| Finding | Action |
|---|---|
| RE1 | Rename to reflect what the values are, e.g. `ThemeBackground`, `ThemeSurface`, `ThemeTextPrimary`/`ThemeTextSecondary`, or simply delete the aliases and call `MaterialTheme.colorScheme.*` directly everywhere (removing the indirection entirely — see [09-consistency.md](09-consistency.md)). |
| RE2 | Add a one-line comment stating which caller (if any) needs the fallback and why a hash-derived ID is an acceptable choice there. |
| RE3 | Split into one file per screen, per [05-reusability.md](05-reusability.md) RU2. |
| RE4 | Extract the notification-dispatch boilerplate per [06-no-duplication.md](06-no-duplication.md) D4; this directly shortens every affected method to its real logic. |
| RE5 | Store `rating` as `Float` and `reviewCount` as `Int` on `SubServiceItem` as well, and format at the UI layer (where `ServiceProvider`/`Review` already correctly leave formatting to the caller). |
