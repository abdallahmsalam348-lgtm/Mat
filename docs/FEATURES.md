# Feature checklist / قائمة الميزات

**Read this first.** Status words: **Implemented** = code is written and wired up; **Partial** = works with stated limits; **Not implemented** = not in this version.

**What was and was not verified (13-15 Nov 2026):**
* ✅ Backend: 13 automated tests pass (run here, against a mock of the provider's response format).
* ✅ Core logic (status mapping, score rules, formatting, time zones, widget selection): compiled with the Kotlin compiler and 12 behaviour checks passed here.
* ✅ The GitHub workflow's "unpack ZIP" step was run against a throwaway repository and worked.
* ❌ **The Android app was NOT compiled or run here** (no Android SDK available). The first cloud build may report compile errors; send me the log and I will fix them. Nothing has been tested on a real phone, with a real API key, or with TalkBack.
* ❌ Nothing was tested against the live API-Football service (no key was available).

| # | Feature | Status | Notes |
|---|---|---|---|
| 1 | Live scores, status, match clock, today/upcoming/results | **Implemented** | Date strip (-3 to +10 days), All/Live/Favorites filters, auto-refresh only while the screen is visible (60 s if a match is live, 5 min otherwise). Real data needs the backend + key. |
| 2 | Competitions + searchable countries/leagues/clubs/national teams | **Partial** | Competition list (current seasons) with search by name or country; team search (incl. national teams). No dedicated "countries" page; player search not implemented. |
| 3 | Match center: goals, cards, subs, VAR, lineups, formations, injuries, H2H, stats | **Implemented** | Pitch drawing for formations when the provider supplies grid positions, list otherwise. Availability depends on provider/match. |
| 4 | Standings, top scorers, team, player, competition pages | **Partial** | All exist. Team and player profiles are basic (no transfers, trophies, career history). |
| 5 | Search and favorites | **Partial** | Favorites for teams and competitions. Players cannot be favorited. |
| 6 | Push notifications (kickoff, goals, red cards, HT, FT) | **Partial** | Local notifications from a background check about every 15 minutes (Android's minimum). Not instant. Real push needs Firebase Cloud Messaging + a backend job (see ROADMAP). |
| 7 | Home-screen widgets (compact, medium, large) | **Implemented** | Jetpack Glance. Refresh about every 30 minutes. "Glass" look is a translucent gradient + thin border (launchers do not allow blur). Large widget shows up to 7 matches, not scrollable. Row order follows the phone's layout direction. Not visually tested. |
| 8 | Settings: language, theme, notifications, time zone, favorites | **Implemented** | Also backend address/token, force-demo switch, Arabic-Indic digits switch. |
| 9 | Accessibility, responsive, loading/empty/offline/error/rate-limit | **Partial** | All states exist; match rows and icons have content descriptions. No TalkBack testing, no tablet/landscape-specific layouts, no font-scale testing. |
| – | Arabic RTL + English LTR everywhere | **Implemented** (untested on device) | Locale-driven layout direction, localized dates/numbers/notifications/settings. Provider text such as stat names is mapped for common items, otherwise shown as the provider sends it (English). Team/league names are as the provider supplies. |
| – | Onboarding + persistent language | **Implemented** | 3 steps. Language is remembered. |
| – | Premium dark theme with glass look | **Partial** | Compose has no backdrop blur: glass is emulated (translucent gradients, luminous borders, top reflection, glowing background). Three themes: Forest, Black, Light. |
| – | Local cache + offline viewing | **Implemented** | JSON file cache per request, stale data is shown with a banner when offline. Not a database. |
| – | Provider abstraction | **Implemented** | App talks to a provider-neutral API; providers live in `backend/src/providers`. Only API-Football is implemented. |
| – | Demo mode | **Implemented** | Fictional teams ("Nile City FC"...), labelled DEMO on every list and in settings; never cached, never notified. |
| – | Server-side caching, budget, rate-limit handling | **Implemented + tested** | See backend/README.md. |
| – | Unit tests (score parsing, status, dates/time zones, localization) | **Implemented** | 4 test files in `app/src/test`, run in CI after the APK is built (failures do not block the APK). |
| – | Firebase push / analytics | **Not implemented** | |
| – | Release signing, Play Store listing, final icon artwork | **Not implemented** | Placeholder vector icon; debug APK only. |
| – | Odds, news, transfers, tablets | **Not implemented** | |
