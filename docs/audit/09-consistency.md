# 9. Consistency

**Definition for this codebase:** one way of doing each thing across the repository. Does the
codebase pick one representation, one pattern, one access path per concept, and stick to it —
or does it maintain two (or three) ways of expressing the same thing side by side.

## Findings

### C9-1 — High: money is represented as `Double` in most of the domain, but as pre-formatted `String` in one place

`ServiceProvider.hourlyRate` and `Booking.hourlyRate`/`totalAmount` (`Models.kt:143,177,187`)
are `Double`, enabling arithmetic (`totalAmount` is computed from `hourlyRate *
estimatedHours`, `:187`). `SubServiceItem.price`/`originalPrice` (`SubServices.kt:11-12`) are
`String` values like `"₹199"`. There is no single `Money`/currency type anywhere; code that
holds a `SubServiceItem` cannot compute a discount percentage, sum a cart, or reformat for a
different locale without first parsing the rupee symbol back out of the string — while code
holding a `ServiceProvider`/`Booking` can do all of that natively. The same real-world
concept (a price) is unusable interchangeably depending on which model it came from.

### C9-2 — High: rating is `Float` everywhere except one model, where it is a formatted `String` with an embedded glyph

`ServiceProvider.rating`, `Review.rating` (`Models.kt:140,128`) are `Float`.
`SubServiceItem.rating` (`SubServices.kt:14`) is `String`, holding values like `"4.88 ★"` —
the star glyph is baked into the data value itself rather than added at render time. Sorting,
comparing, or averaging ratings works for two of the three models and requires ad hoc string
parsing for the third.

### C9-3 — Medium: the project ships a full navigation library dependency that is never used, in favor of a hand-rolled alternative

`app/build.gradle.kts:41` declares `implementation("androidx.navigation:navigation-compose:2.8.5")`.
`grep -rn "NavHost\|rememberNavController"` across the entire `app/src/main/java` tree returns
zero matches. Instead, `MainActivity.kt:59-76` defines a hand-rolled `sealed class Screen` and
`MainAppHost` dispatches on it with a manual `when` block (`:137-318`) and a single
`currentScreen` `mutableStateOf`. Neither approach is wrong on its own — but the project has
committed to the bespoke one while still paying the dependency-size and mental-model cost of
the standard one sitting unused in the build. See also
[11-performance-and-cost.md](11-performance-and-cost.md) for the size angle.

### C9-4 — Medium: 294 hardcoded theme-alias color references vs. 7 direct `MaterialTheme.colorScheme` references, with no stated convention for which to use

Per-file counts (grep): `AddressSetupScreen.kt` — 3 direct vs. 11 alias;
`BookingStatusScreen.kt` — 0 vs. 32; `CustomerScreens.kt` — 1 vs. 123; `FeedbackScreen.kt` —
0 vs. 34; `ServiceCatalogScreen.kt` — 0 vs. 43; `SettingsScreen.kt` — 0 vs. 38;
`AuthScreen.kt` — 3 vs. 13. Total: 7 direct, 294 alias. The alias path is the overwhelming
majority pattern, which would be a fine convention on its own — the actual inconsistency is
that four files (`BookingStatusScreen`, `FeedbackScreen`, `ServiceCatalogScreen`,
`SettingsScreen`) use *only* the alias path, while `AddressSetupScreen` and `AuthScreen` mix
both paths for the same kind of color lookup within the same file, with no visible rule for
when to reach for which.

### C9-5 — Low: two different ways of obtaining the repository singleton

`MainActivity.onCreate` (`MainActivity.kt:49`) calls
`ServiceSyncRepository.getInstance(applicationContext)` directly. `ServiceSyncApp.onCreate`
(`ServiceSyncApp.kt:14`) separately calls the same `getInstance(this)` and stores the result
on its own `repository` property, which no other file in the codebase reads
(`grep -rn "ServiceSyncApp\b" app/src/main/java` — no call sites access `.repository` on an
`Application` instance anywhere). Both paths resolve to the same singleton instance, so there
is no functional bug — but the codebase maintains two different "how do I get the
repository" idioms, one of which (`ServiceSyncApp.repository`) is entirely unused by any
caller and exists only as a redundant reference to the same object.

### C9-6 — Low: phone-number formatting conventions differ between hardcoded seed data and user-entered data

Seed provider phone numbers are stored pre-formatted with spaces (`"+91 98112 34567"`,
`ServiceSyncRepository.kt:1540`), while user-registered phone numbers are stored as bare
digit strings after `Regex("[^0-9]")` stripping (e.g. `cleanPhone`, throughout
`registerCustomer`/`registerWithPhoneOtp`). Any code that needs to compare or display a
provider's phone the same way as a customer's phone must first normalize one or the other,
and nothing in the codebase currently does — `it.phone.replace(Regex("[^0-9]"), "")` is
applied ad hoc at each comparison site rather than at the point of storage.

### C9-7 — Low: appliance illustration frames hardcode a light-gray background that never adapts to dark mode

`ApplianceClayHelper.kt:139`, inside `ApplianceClayImage`:
`.background(Color(0xFFF1F5F9))` — a literal light color, not a theme-derived one. Every
appliance/sub-service illustration across the catalog and booking-summary screens sits inside
this fixed light-gray frame regardless of whether the user has selected Dark, Light, or
System theme mode in Settings — the one visual element on these screens that does not follow
the app's otherwise carefully-built dark/light theme system (`Theme.kt`, `Color.kt`).

### C9-8 — Low: the home screen's category count label is a hardcoded literal that doesn't match the category enum

`CustomerScreens.kt:743`: `Text(text = "9 categories", ...)`. The "Explore Services" grid
directly beneath it hand-lists 9 of the enum's 10 `ServiceCategory` entries (`OTHER` is
omitted from this grid, though it remains reachable via `ServiceCatalogScreen`'s category
chips, which iterate `ServiceCategory.values()` and so correctly show all 10). If a category
is ever added, removed, or the grid is ever updated to include `OTHER`, this string will not
change on its own — it is not derived from `ServiceCategory.values().size` or the grid
contents.

## Recommendation summary

| Finding | Action |
|---|---|
| C9-1 | Give `SubServiceItem` numeric `price`/`originalPrice` fields (or adopt one shared `Money`/currency value class used by all three models) and format at render time only. |
| C9-2 | Give `SubServiceItem.rating` a `Float` field; render the star glyph at the UI layer. |
| C9-3 | Either remove the unused `navigation-compose` dependency, or migrate `MainAppHost` to it if the team wants standard back-stack/deep-link support — but stop paying for both. |
| C9-4 | State and follow one rule (e.g., always use the semantic alias unless overriding a specific Material role) and bring the two mixed files in line with it. |
| C9-7 | Replace the literal color with a theme-aware token (e.g. `SurfaceVariantLight`, already used elsewhere for this exact purpose) so appliance frames follow the active theme. |
| C9-8 | Derive the label from `ServiceCategory.values().size` (or from the grid's actual item count) instead of a hardcoded literal. |
| C9-5 | Pick one access path — likely `ServiceSyncApp.repository`, since it's the idiomatic Android place for an app-scoped singleton — and remove the redundant direct `getInstance` call, or vice versa; delete whichever is not chosen. |
| C9-6 | Store phone numbers in one normalized form everywhere (digits-only, formatted only at display time), eliminating the need for repeated ad hoc normalization at comparison sites (also addressed by [06-no-duplication.md](06-no-duplication.md) D3). |
