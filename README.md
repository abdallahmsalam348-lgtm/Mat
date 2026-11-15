# MATCHLY ⚽  |  ماتشلي

Premium global football scores & fixtures for Android (Kotlin, Jetpack Compose, Material 3, Jetpack Glance widgets) with a small secure backend proxy. Arabic RTL + English LTR.

**Status: foundation / MVP.** Not a finished product. Read [`docs/FEATURES.md`](docs/FEATURES.md) for an honest list of what works, what is partial and what was (not) verified. In particular the Android app has **not yet been compiled or run** by its author's tooling; the cloud build is the first real test.

| | |
|---|---|
| Setup for beginners (phone only) | [English](docs/SETUP_EN.md) · [العربية](docs/SETUP_AR.md) |
| Data providers & costs | [docs/PROVIDERS.md](docs/PROVIDERS.md) |
| Feature checklist | [docs/FEATURES.md](docs/FEATURES.md) |
| Roadmap to production | [docs/ROADMAP.md](docs/ROADMAP.md) |
| Backend | [backend/README.md](backend/README.md) |

## Layout
```
app/                 Android app (com.matchly.app)
  data/              models, repository, cache, demo source, Retrofit client
  core/              settings, locale, formatters, favorites
  ui/                theme, glass components, screens, navigation
  widget/            3 Glance widgets + refresh worker
  notifications/     local notifications + background check
  src/test/          unit tests
backend/             Node proxy (no dependencies) + tests
.github/workflows/   build-apk.yml: cloud build, uploads the debug APK
render.yaml          optional one-click Render blueprint
matchly.properties   build-time config (placeholders only)
```

## Security rules
* The sports API key lives **only** in the backend's environment variables. Never in the app, never in Git.
* `matchly.properties` holds only the public backend address and an optional (non-secret) app token.

---
## بالعربية
MATCHLY تطبيق أندرويد لنتائج ومباريات كرة القدم (كوتلن + Jetpack Compose + ويدجت Glance) مع خادم وسيط آمن، ويدعم العربية (RTL) والإنجليزية.

**الحالة: أساس/نسخة أولية وليست منتجًا نهائيًا.** اقرأ [`docs/FEATURES.md`](docs/FEATURES.md) لقائمة صريحة بما يعمل وما هو جزئي وما لم يُختبر. وتحديدًا: **لم يُجمَّع تطبيق أندرويد ولم يُشغَّل بعد**، والبناء السحابي هو أول اختبار حقيقي له. ابدأ من [دليل الإعداد](docs/SETUP_AR.md).

**قاعدة أمان:** مفتاح بيانات الكرة يوضع في متغيرات بيئة الخادم فقط، وليس في التطبيق ولا في GitHub.
