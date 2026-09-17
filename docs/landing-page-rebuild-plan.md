# SaServe Marketing Website — Deep Audit + Rebuild Plan

Scope: `components/*.jsx|tsx` and `src/components/*.jsx` (the "React landing page" sources),
cross-referenced with the prior 15-dimension engineering audit
([docs/audit/05-reusability.md](audit/05-reusability.md) RU5,
[docs/audit/06-no-duplication.md](audit/06-no-duplication.md) D5). Method: manual file read of
every source file, plus `ui-ux-pro-max` design-system queries for
palette/typography/landing-structure recommendations.

This document is planning only. No source file was modified to produce it.

---

## Part A — Deep audit of the current "landing page"

### Inventory

Six files, four unique sources:

| File | Role | Duplicate of |
|---|---|---|
| `components/Navbar.jsx` | Header/nav | `src/components/Navbar.jsx` (byte-identical) |
| `components/SaServe3DHelixLogo.jsx` | Animated WebGL logo | `src/components/SaServe3DHelixLogo.jsx` (byte-identical) |
| `components/Logo.tsx` | Static SVG logo (React component) | — no `.tsx`/`.jsx` duplicate, but unused — nothing imports it |
| `assets/logo.svg` | Standalone logo asset | Not a byte-identical duplicate of `Logo.tsx`, but a hand-kept parallel export of it — same path geometry, same two gradient stops, same two font families, checked line by line |

There is **no page, no app entry point, no router, and no build tooling** — no
`package.json`, no bundler config (Vite/Webpack/Next), no `index.html`, no `App.jsx`. The
"landing page" is a `<header>` with nothing below it, and it cannot currently be installed,
built, or run. This was already flagged in the prior audit
([RU5](audit/05-reusability.md), [D5](audit/06-no-duplication.md)) — this pass confirms it and
goes one level deeper into content and UX.

### Finding 1 (Critical, root cause of "it's a copy of the app") — the nav is app screen names, not a website

`Navbar.jsx:24-55` — the nav links are: **Services, Categories, Specialists, My Bookings, Help
& Support**. These are literally the Android app's bottom-nav/screen names, wired to anchors
(`#bookings`, `#specialists`) that don't exist anywhere because no page content exists. A
first-time visitor to a marketing site is not logged in and has no bookings — "My Bookings" in
a public nav is a direct symptom of the site being treated as an extension of the app instead
of a company website with its own job: explain the product, build trust, and convert visitors
into app installs, bookings, or (for investors) a conversation. This is the concrete evidence
behind the complaint that the site currently reads as a reskinned app screen.

### Finding 2 (High) — both CTAs are dead

`Navbar.jsx:59-64` — "Find Specialist" and "Book Now" are `<button>` elements with zero
`onClick`/`href`. There is no download link, no "Get the app" path, no signup form, nothing
that converts a visitor into anything. A site with no working call-to-action has no purpose.

### Finding 3 (High) — no content below the header at all

There is no Hero, no explanation of what SaServe does, no how-it-works, no categories, no
trust signals, no testimonials, no footer, no legal links (Privacy Policy/Terms — notable
because [docs/audit/03-privacy.md](audit/03-privacy.md) already flags the app has no privacy
policy surfaced anywhere), no contact information, no investor/market content. Everything a
visitor would need to understand or trust the product is missing.

### Finding 4 (Medium) — duplicate logo implementations drifting from each other

`Logo.tsx` (static SVG, unused), `SaServe3DHelixLogo.jsx` (animated WebGL, actually used by
Navbar), and `assets/logo.svg` all hardcode the same brand colors independently:
`primaryColor = '#2563eb'` / `accentColor = '#06b6d4'` as JS props in one file,
`stopColor="#2563eb"` / `stopColor="#06b6d4"` as SVG attributes in another, `stop-color`
(kebab-case, plain SVG) in the third, plus a fourth copy of the same hex pair inline in
`Navbar.jsx`'s own Tailwind gradient classes. Four independent sources of truth for two colors
— a future rebrand or color tweak requires remembering to touch all four. Verified by reading
`assets/logo.svg` directly: it is not just color-duplicate but near-identical to `Logo.tsx` —
same path geometry, same two gradient stops, same two font-family declarations (`Space
Grotesk` for the wordmark, `Plus Jakarta Sans` for the tagline) — i.e. someone hand-exported
`Logo.tsx`'s render to a static file rather than generating it from the component, so the two
will drift on the next edit to either one. `Logo.tsx` itself is unused (nothing imports it), so
it and its hand-export can go stale without anyone noticing either changed.

### Finding 5 (Low) — three different body/label fonts already declared with no single decision

`Logo.tsx` and `assets/logo.svg` both declare `Space Grotesk` for the wordmark and `Plus
Jakarta Sans` for the tagline label; `SaServe3DHelixLogo.jsx`'s rendered text instead falls
back to Tailwind's default `font-sans` stack for the "SaServe" text next to the 3D canvas, with
no explicit font-family at all. So the brand currently has one heading font declared twice
consistently (`Space Grotesk`) but two different, undecided answers for body/label type
(`Plus Jakarta Sans` in the static assets vs. the Tailwind default in the component that's
actually rendered on screen). This needs a single decision before any new page copy is
written — see Part C.

### Finding 6 (Medium) — WebGL logo is a performance and accessibility risk for a marketing page

`SaServe3DHelixLogo.jsx` spins up a full Three.js scene (renderer, two point lights, two tube
meshes with 120-segment geometry, a 65-particle system, and a `requestAnimationFrame` loop) on
every mount, purely as header branding. For a marketing site where first-load performance
directly affects bounce rate and SEO (Core Web Vitals are a Google ranking signal), an
always-running WebGL canvas in the header is expensive: it blocks on GPU init, keeps the main
thread animating indefinitely, and has no `prefers-reduced-motion` handling at all — the
animation and the mouse-tilt interaction run unconditionally for every visitor, including
those who've asked their OS to reduce motion. It also isn't code-split — it would ship in the
initial bundle rather than being lazy-loaded.

### Finding 7 (Low) — no responsive mobile navigation

`Navbar.jsx:24` hides the entire nav (`hidden md:flex`) below the `md` breakpoint with no
hamburger/drawer replacement. Below ~768px, a visitor has no way to reach any nav destination
except the two dead buttons.

### Cross-reference to the prior audit

| Prior finding | This audit |
|---|---|
| [RU5](audit/05-reusability.md) — orphaned React code, no build tooling | Confirmed; extended with the content/UX gap analysis above |
| [D5](audit/06-no-duplication.md) — `components/*` and `src/components/*` byte-identical | Confirmed; also found `assets/logo.svg` as a further, not-byte-identical near-duplicate of `Logo.tsx`, plus the color (Finding 4) and font (Finding 5) drift across all four files, none of which the prior pass called out |

---

## Part B — What a real company website needs that this doesn't have

The user's framing is exactly right: a product marketing site and the app are different
deliverables with different jobs. The app's job is to let a logged-in customer book a
specialist. The website's job is to:

1. **Explain the product to someone who has never used it** — what problem it solves, for whom, why it's trustworthy.
2. **Convert three distinct audiences**, each needing different content:
   - **Customers** — why SaServe over calling a local electrician directly (verified pros, transparent ₹ pricing, tracked bookings).
   - **Service professionals** (supply side of the marketplace — this is a two-sided marketplace and currently has zero acquisition content for the supply side) — why join SaServe as a specialist.
   - **Investors / market observers** — market size, business model, traction, team, roadmap.
3. **Establish trust** before asking for anything — verification badges, ratings, real testimonials, transparent legal pages.
4. **Be findable** — SEO metadata, sitemap, OG tags for link previews, none of which can exist without an actual build pipeline.
5. **Convert to a measurable action** — app store download, partner signup, investor contact — none of which exist today (Finding 2).

None of this requires touching the Android app or its (separately audited, separately
concerning) feature set. It requires building the website that doesn't exist yet.

---

## Part C — Design system (via `ui-ux-pro-max`)

Two `ui-ux-pro-max` design-system queries were run: one generic marketplace query (returned an
orange/blue "Trust & Authority" system meant for services marketplaces broadly) and one
constrained to the dark navy/blue/cyan palette the brand already uses in `Logo.tsx` and
`Navbar.jsx`. Recommendation below **keeps brand continuity** (the blue/cyan on dark-navy
identity already exists and is the right register for a services-trust brand) rather than
adopting the generic orange palette wholesale — a rebrand isn't the ask.

### Colors (extends the existing `#2563EB` / `#06B6D4` / `#090d16` triad into a full token set)

| Token | Value | Use |
|---|---|---|
| `--color-primary` | `#2563EB` | Primary CTA, links, brand blue (existing) |
| `--color-accent` | `#06B6D4` | Secondary accent, highlights (existing) |
| `--color-success` | `#22C55E` | Verified/trust badges, "in progress" states |
| `--color-background` | `#0B0F1A` | Page background (darker, more neutral than `#090d16` for large surfaces) |
| `--color-surface` | `#151B2C` | Cards, elevated panels |
| `--color-border` | `rgba(255,255,255,0.08)` | Hairlines on dark surfaces |
| `--color-foreground` | `#F8FAFC` | Primary text on dark |
| `--color-muted-foreground` | `#94A3B8` | Secondary text |
| `--color-destructive` | `#EF4444` | Errors only |

This is a single source of truth to replace the four independent hardcodings found in Finding
4 — every component pulls from these tokens, nothing hardcodes a hex value again.

### Typography

Resolves Finding 5 (three inconsistent body/label font answers) with one decision: keep
**Space Grotesk** for headings — it's the only piece already declared consistently everywhere
(`Logo.tsx`, `assets/logo.svg`), and it matches `ui-ux-pro-max`'s "Tech Startup" pairing for
this product category. For body copy, default to **Plus Jakarta Sans** — it's already declared
in `Logo.tsx`/`assets/logo.svg` for the tagline, so keeping it costs nothing and resolves the
inconsistency by adoption rather than by introducing a third new font. If a full brand refresh
becomes in scope later, `ui-ux-pro-max`'s "Tech Startup" pairing suggests **DM Sans** as the
alternative body font (highly legible at small sizes, common in SaaS/startup marketing) — but
that's a deliberate swap to make later, not a default for this rebuild.

### Style direction

Blend of two `ui-ux-pro-max` categories: **Tech Startup** (dark, bold, motion-forward — fits an
app-download-driven product) layered with **Trust & Authority** patterns (verification badges,
metrics with real numbers, before/after or problem/solution framing) — because the audience
includes both consumers who need reassurance before letting a stranger into their home *and*
investors who need credibility signals, not just startup flash.

### Motion

Framer Motion, 150–300ms scroll reveals, `prefers-reduced-motion` respected everywhere
(current WebGL logo violates this — Finding 6). The 3D helix logo should be lazy-loaded
(`next/dynamic` with `ssr: false`, mounted only after intersection) and swapped for the static
`Logo.tsx` SVG for reduced-motion users and mobile, instead of running unconditionally.

### Icons

SVG icon set (Lucide or Heroicons) — no emoji as icons, per the `ui-ux-pro-max` checklist.

---

## Part D — Information architecture (multi-page, not a single fake-anchor scroll)

The current nav points at in-page anchors (`#services`, `#specialists`) that don't exist. A
site that needs to serve customers, professionals, and investors with substantively different
content is better served as real routes (also required for per-page SEO metadata):

```
/                     Home — consumer-facing conversion page (includes a "How It Works" section, see Part E §5)
/for-professionals     Supply-side pitch: "Join SaServe as a specialist"
/about                 Company story, mission, founders
/investors             Market size, business model, traction, roadmap, contact
/download              App store badge(s) + QR code
/legal/privacy         Privacy policy (currently absent app-wide — see 03-privacy.md)
/legal/terms           Terms of service
/contact               Support + general contact
```

**Decision:** no standalone `/how-it-works` route. The explanation is a single four-step block
(Part E §5) short enough to live on Home without its own page — a separate route for it would
just be a second, thinner copy of the same content and another thing to keep in sync. If the
content later grows past what fits on Home (e.g. separate flows for customers vs.
professionals), split it out then, not preemptively.

**Deliberately out of scope for this core set:** a `/blog` or `/press` route. Useful for
long-term SEO/content marketing, but there's no content to publish yet and an empty blog looks
worse than no blog. Revisit once there's a cadence of real posts to publish, not as part of the
initial core-pages build.

---

## Part E — Home page, section by section

1. **Navbar (rebuilt)** — `Home / How it Works` (in-page anchor, not a route — see Part D)
   `/ For Professionals / About / Investors` + secondary "Download the App" button + primary
   "Book a Service" button (both wired to real destinations — Play Store link and `/download`,
   not dead buttons).
2. **Hero** — one clear sentence of what SaServe is ("Book verified home service professionals
   in minutes, at transparent ₹ prices"), subhead, dual CTA (Download App / Become a Partner),
   app store badge, hero visual (product screenshot mockup or the *lazy-loaded* 3D logo, not a
   forced-motion header widget).
3. **Trust bar** — key stats (verified professionals count, cities covered, bookings
   completed) — explicit placeholders until real numbers are supplied (see Part I).
4. **Problem → Solution** — the pain of finding a reliable electrician/plumber in India today
   vs. SaServe's verified, tracked, transparently-priced alternative.
5. **How It Works** — Browse → Book → Track → Pay, four-step visual walkthrough of the *real*
   app flow (see caution in Part F3 about not repeating fabricated features from the app audit).
6. **Service categories** — Electrician, Plumber, Carpenter, Mechanic, Appliance Repair,
   Painter (this legitimately mirrors the app's categories — that's correct, it's the actual
   catalog).
7. **Trust & Safety** — verification process, ratings/reviews, transparent pricing, secure
   booking.
8. **For Professionals CTA** — dedicated supply-side pitch block linking to `/for-professionals`.
9. **Testimonials** — real customer/professional quotes with name, role, photo. Until real
   ones exist, mark placeholders explicitly as placeholders in code comments — do not ship
   fabricated testimonials (the app audit already found a pattern of fabricated UI in this
   codebase; the website should not repeat it).
10. **Market/Investor teaser** — one stat + "See our investor overview" link to `/investors`.
11. **Download CTA** — Play Store badge, QR code, App Store badge if iOS is planned.
12. **Footer** — company info, nav, legal links (`/legal/privacy`, `/legal/terms`), social,
    contact email.

---

## Part F1 — `/for-professionals` page content plan

This is the marketplace's supply side and currently has **zero** acquisition content anywhere
in the app or the (nonexistent) site — a two-sided marketplace that only markets to demand is
missing half its growth engine.

1. **Nav/Hero** — "Grow your business with SaServe" — earn more, flexible hours, verified-pro
   badge as a credibility asset for the pro, not just the customer.
2. **Why join** — steady lead flow, transparent ₹ payouts, no cold-calling for work, ratings
   build reputation over time.
3. **How it works for professionals** — Apply → Verification (ID, trade certification) →
   Get matched with nearby bookings → Get paid. This must describe the *real* verification and
   payout process once one exists — do not imply an automated instant-payout system if the
   actual payout flow is manual/placeholder.
4. **Categories accepted** — same six trades as the app (Electrician, Plumber, Carpenter,
   Mechanic, Appliance Repair, Painter).
5. **Earnings potential** — illustrative example, clearly marked illustrative, not a guarantee.
6. **Requirements** — what's needed to apply (ID proof, trade experience, service area).
7. **Application CTA** — form or link to the actual professional-onboarding flow (needs a real
   destination — the app currently has no professional-facing app/portal per the Android audit,
   so this CTA's real destination is itself an open product question, not just a copy one).
8. **FAQ** — payout timing, coverage area, commission structure.

---

## Part F2 — `/about` page content plan

1. **Mission/story** — why SaServe exists, the problem the founders saw in the India home-services market.
2. **What we believe** — trust, verified professionals, transparent pricing — ties back to the Trust & Safety section on Home rather than repeating generic "our values" filler.
3. **Team** — founder(s)/team bios and photos (placeholder until supplied, see Part I).
4. **Timeline/milestones** — founding date, launch, key milestones — honest "early stage" framing is fine and often reads as more credible to investors than inflated claims.
5. **Contact/press CTA** — link to `/contact`.

---

## Part F3 — `/investors` page content plan

Since the user explicitly wants the site to speak to investors and the market, not just users:

- **Market opportunity** — India on-demand home-services market size (TAM/SAM/SOM), growth
  rate, why now.
- **Problem/solution/business model** — how SaServe monetizes (commission per booking,
  subscription for professionals, etc. — placeholder until a real answer exists, see Part I).
- **Traction** — real metrics if any exist yet, or an honest "early stage" framing if not.
- **Product roadmap** — near-term milestones.
- **Team** — founder bios, relevant background (shared content with `/about`, don't duplicate — link to it instead).
- **Contact** — dedicated investor-inquiry contact path.

**Important guardrail carried over from the app audit:** the 15-dimension audit found the
Android app fabricates several features it presents as real — a fake UPI payment flow with a
"UPI Verified" badge, a hand-drawn canvas presented as "Live Google Maps Tracking," and a
bypassable OTP ([docs/audit/README.md](audit/README.md) top findings 1, 2, 7). Investor-facing
copy and the "How It Works" section **must describe only what the app actually does today**,
not what the UI cosmetically claims — an investor site that repeats those claims creates real
diligence risk once the app is looked at directly.

---

## Part F4 — `/download` page content plan

1. **App store badges** — Play Store (live link needed, Part I) and App Store if iOS is planned.
2. **QR code** — for desktop visitors to jump straight to the store listing on mobile.
3. **Screenshot carousel** — real app screenshots (Home feed, booking flow, tracking) — reuse actual Compose UI screenshots, not mockups, since this page's whole job is to set accurate expectations before install.
4. **Why download** — 3-4 bullet recap of the core value prop for a visitor who scrolled straight here from an ad or social link without reading Home first.

---

## Part F5 — `/legal/privacy` and `/legal/terms`

Currently the app has **no privacy policy anywhere** ([docs/audit/03-privacy.md](audit/03-privacy.md)) — this is not just a marketing-site gap, it's a Play Store listing requirement (Google requires a privacy policy URL for apps that collect personal data, which this app does: phone numbers, addresses, booking history). This makes these two pages higher priority than typical "boilerplate legal pages," not lower.

1. **`/legal/privacy`** — what data is collected (phone, name, address, booking history — per the actual `ServiceSyncRepository`/Firestore schema, not a generic template), how it's stored (Firestore — and per [02-security.md](audit/02-security.md), the current rules allow anonymous read/write, which is a real fact a privacy policy would need to either fix first or not misrepresent), third parties (Firebase), user rights, contact for data requests.
2. **`/legal/terms`** — booking terms, cancellation policy, payment terms (must accurately reflect that in-app UPI payment is currently simulated, not real, per [01-correctness.md](audit/01-correctness.md) C7 — terms of service cannot promise a real payment guarantee for a flow that doesn't exist yet).

---

## Part F6 — `/contact` page content plan

1. **General support** — email/phone for customers and professionals.
2. **Investor inquiries** — separate contact path, cross-linked from `/investors`.
3. **Press/media** — optional, if press coverage is a near-term goal.
4. **Office/location** — if applicable, or "remote-first" framing if not.

---

## Part G — Technical rebuild

### Stack decision: Next.js (App Router) + TypeScript + Tailwind CSS + Framer Motion

A bare Vite/CRA React SPA (closer to what the current files imply) has weak SEO — no
server-rendered meta tags, no per-route OG images, no sitemap without extra tooling. For a
site whose job includes being found by search and looking credible in shared links (investor
emails, social), Next.js's built-in metadata API, image optimization, and static/SSR rendering
are the right fit and match what the existing `ui-ux-pro-max` stack guidance recommends for
this kind of build.

### Structural cleanup (fixes D5/RU5 from the prior audit)

- Collapse `components/` and `src/components/` into a single `app/` (or `src/`) tree — delete
  the duplicate directory rather than maintaining two copies.
- Add a real `package.json`, `tsconfig.json`, ESLint/Prettier config, and a `.env.example` if
  analytics/keys are added later.
- `Logo.tsx` becomes the single source of brand color tokens (via the CSS variables in Part C)
  instead of every component hardcoding its own hex pair (Finding 4).
- `SaServe3DHelixLogo` becomes an optional, lazy-loaded enhancement — `Logo.tsx` (static SVG)
  is the default/fallback for reduced-motion and low-end devices, not a second unused file.

### Performance & accessibility budget (from `ui-ux-pro-max` checklist)

- Lighthouse ≥ 90 mobile, LCP < 2.5s, CLS < 0.1.
- WCAG AA: 4.5:1 text contrast, visible focus states, keyboard navigation, alt text on all
  imagery, `prefers-reduced-motion` respected (directly fixes Finding 6).
- Responsive tested at 375 / 768 / 1024 / 1440px (fixes Finding 7 — real mobile nav, not a
  hidden link list).
- No emoji-as-icon, SVG icon set only, `cursor-pointer` on all clickable elements, 150–300ms
  hover transitions.

### Deployment

`firebase.json` already exists in this repo — either add the new site as a Firebase Hosting
target, or, per the prior audit's RU5 recommendation, host it as a separate deployment (e.g.
Vercel) if the intent is to decouple the marketing site's release cadence from the Android
app's. This is a decision for the user (see Part I).

---

## Part H — Phased execution plan

This is a documentation/planning deliverable only — no scaffolding, code, or dependencies get
touched as part of it. The phases below are what *executing* this plan would look like later,
once the user decides to build.

| Phase | Deliverable |
|---|---|
| 0 | Repo placement decision (in-repo `/web` vs. separate marketing repo per RU5) + fill placeholder content inputs (Part I) as far as available |
| 1 | Scaffold Next.js + TypeScript + Tailwind, delete duplicate `components/`/`src/components/` split, wire design tokens from Part C |
| 2 | Shared layout: rebuilt Navbar (real nav items + working CTAs + mobile drawer), Footer, base components (Button, Badge, Section, Card) |
| 3 | Build Home page sections 1–12 (Part E) top to bottom |
| 4 | Build `/for-professionals` (F1), `/about` (F2), `/investors` (F3), `/download` (F4), `/legal/privacy` + `/legal/terms` (F5), `/contact` (F6) |
| 5 | SEO pass (per-route metadata, OG images, `sitemap.xml`, `robots.txt`), accessibility pass, performance pass against the Part G budget |
| 6 | Analytics wiring, deploy, cross-device/breakpoint QA |

---

## Part I — Content inputs to fill in when building (placeholders until then)

None of these block the plan itself — they're the fields each page above should treat as an
explicit, clearly-marked placeholder (e.g. `[[MARKET_SIZE_TAM]]`) until real values exist,
rather than inventing plausible-looking numbers or quotes:

1. Real numbers for the trust bar / `/investors`: verified professionals, cities, bookings
   completed — or an explicit "early stage, pre-launch" framing if there's nothing yet.
2. Monetization model (commission %, subscription, etc.) for `/investors`.
3. Founder/team bios and photos for `/about` and `/investors`.
4. Play Store listing link (and whether iOS/App Store is planned) for `/download`.
5. Real testimonials/case studies for Home and `/for-professionals` — ship without the section
   entirely until real ones exist, rather than placeholder quotes that read as genuine.
6. Repo placement (in-repo `/web` vs. separate marketing repo).

---

## Pre-delivery checklist (from `ui-ux-pro-max`)

- [ ] No emoji as icons — SVG icon set only
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states, 150–300ms transitions
- [ ] Text contrast ≥ 4.5:1 in both light/dark surfaces used
- [ ] Visible focus states for keyboard nav
- [ ] `prefers-reduced-motion` respected (especially the 3D logo)
- [ ] Responsive at 375 / 768 / 1024 / 1440px, real mobile nav (no hidden-with-no-replacement links)
- [ ] Every CTA wired to a real destination — zero dead buttons
