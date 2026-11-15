# Sports data providers: comparison / مقارنة مزودي البيانات

Researched 13 Nov 2026 from the providers' public pricing pages and third-party comparisons. **Prices and limits change; confirm on the provider's site before paying.** I could not verify the providers' terms of use for redistributing data inside a public app. Read them before publishing MATCHLY publicly or commercially.

| | **API-Football** (chosen) | **football-data.org** | **Sportmonks** |
|---|---|---|---|
| Free tier | 100 requests/day, all endpoints, limited seasons | 12 competitions, 10 requests/min, scores *delayed*, no lineups/subs/cards | 2 leagues (Danish Superliga, Scottish Premiership), 180 calls/hour/endpoint |
| Cheapest paid | Pro $19/mo: 7,500 requests/day | €12/mo adds live scores (20/min); deeper data costs more | From about €29/mo: 5 leagues of your choice |
| Coverage claim | All competitions on every plan (their claim; free plan limits *seasons*) | 12 free competitions (top European leagues, UCL, World Cup...), more on paid plans | 2,200+ leagues on Enterprise; plans pick N leagues |
| Live updates | Documented 15-second cadence | Live scores only on paid plan | Plan dependent |
| Events / lineups / stats / injuries | Yes, same endpoints on every plan | Lineups, cards, subs are paid | Yes (data features depend on plan) |

## Why API-Football for MATCHLY
* Broadest coverage on the cheapest plan, and **every plan has every endpoint** (events, lineups, statistics, injuries, head-to-head, top scorers, standings). That matches the match-center feature set.
* A free key lets you wire everything up for testing at no cost.

## Honest limits
* **100 requests/day is for development only.** The backend caches aggressively (`PLAN_PRESET=free`: today's list refreshes about every 15 minutes) and refuses to exceed `DAILY_REQUEST_BUDGET` (default 90), but a real audience needs **Pro ($19/month)** at least.
* The free plan restricts *seasons*. The provider's own 2026 World Cup guidance says to verify actual free-plan access with a live key. **I could not test against a live key**, so I do not claim that current-season data is available on the free plan. If it is not, the app shows "not included in the current data plan".
* "Global coverage" is the provider's claim. MATCHLY shows whatever the provider returns; check specific leagues in your own account.

Sources: [API-Football pricing](https://www.api-football.com/pricing), [API-Football beginner guide](https://www.api-football.com/news/post/how-to-get-started-with-api-football-the-complete-beginners-guide), [football-data.org free-tier guide](https://www.thestatsapi.com/blog/football-data-org-free-tier-limits-2026), [Sportmonks plans](https://www.sportmonks.com/football-api/plans-pricing), [Sportmonks free plan](https://www.sportmonks.com/football-api/).

---
## مقارنة مختصرة بالعربية
* **اخترنا API-Football**: أوسع تغطية بأقل سعر، وكل الخطط تشمل كل الخدمات (الأحداث، التشكيلات، الإحصائيات، الإصابات، الترتيب، الهدافون).
* **الخطة المجانية (100 طلب يوميًا) للتجربة فقط.** الخادم يخزن النتائج مؤقتًا ويمنع تجاوز الميزانية اليومية، لكن لجمهور حقيقي تحتاج خطة Pro بسعر 19 دولارًا شهريًا تقريبًا.
* الخطة المجانية تحدّ من **المواسم المتاحة**؛ لم أستطع اختبار ذلك بمفتاح حقيقي، لذلك لا أَعِد بأن الموسم الحالي متاح مجانًا. إن لم يكن متاحًا سيعرض التطبيق رسالة "غير مشمول في خطتك".
* **football-data.org**: مجاني لـ 12 بطولة لكن النتائج متأخرة والتشكيلات والبطاقات مدفوعة. **Sportmonks**: المجاني دوريان فقط، والمدفوع من نحو 29 يورو لخمسة دوريات.
* لم أتحقق من شروط الاستخدام لإعادة عرض البيانات داخل تطبيق عام؛ اقرأها قبل النشر العام أو التجاري.
