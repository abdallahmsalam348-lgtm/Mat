# Roadmap: MVP to production

## Phase 0: make it build and run (days)
1. Run the cloud build; fix any compile errors reported by the log.
2. Install on a real phone in demo mode: click through every screen in Arabic and English (RTL), all three themes.
3. Deploy the backend, add a free API-Football key, check which competitions and seasons the free plan really returns.

## Phase 1: trustworthy data (1-2 weeks)
* Upgrade to API-Football Pro, set `PLAN_PRESET=paid`.
* Verify the assumptions flagged in code: substitution player order, `goals` for penalty-decided matches, `injuries` shape, `next/last` availability.
* Read the provider's terms of use for public/commercial display; add attribution if required.
* Add a durable backend cache (Redis or SQLite) so a restart or cold start does not reset the daily request counter.

## Phase 2: real push notifications (1-2 weeks)
* Firebase project + Cloud Messaging (FCM message delivery itself is free; Cloud Functions/scheduled jobs need the pay-as-you-go plan, so check current Firebase pricing). Alternative: send FCM from your existing backend using a service account.
* Backend job polls live matches (one `live=all` call serves everyone), diffs events, and sends topic messages (`team-123`, `match-456`).
* App subscribes to topics for favorites; remove the 15-minute polling worker.

## Phase 3: product quality (2-4 weeks)
* Room database instead of JSON file cache; paging for long fixture lists.
* Player search + favorite players, team pages with transfers/trophies, competition "countries" browser.
* Real-time match screen with faster refresh (server-sent events or short-interval polling only for the open match).
* Large-screen/tablet layouts, TalkBack and font-scale audit, contrast audit of the Light theme.
* Final logo/icon artwork, onboarding illustrations, app-store screenshots (Arabic + English).

## Phase 4: release (1-2 weeks)
* Release signing key (kept as GitHub secrets), R8 minification with tested keep-rules, release workflow producing an AAB.
* Crash reporting + privacy-friendly analytics, privacy policy, Play Console listing (needs a one-time developer fee; check current amount).
* Load-test the backend; move off Render's free tier (it sleeps after 15 minutes without traffic, wakes in about a minute, and Render says not to use free instances for production).

## Phase 5: grow
* Second provider behind the same interface for redundancy or extra leagues.
* Personalised feed, match reminders, widgets that follow your teams, Wear OS tile.
