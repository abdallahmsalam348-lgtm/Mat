# MATCHLY setup guide (English): only an Android phone needed

You will: (A) build the app in the cloud and install it, then optionally (B) connect real football data.
**Cost:** GitHub, Render and an API-Football key all have free plans. The free API-Football plan (100 requests/day) is for testing only; real use needs about $19/month. See `PROVIDERS.md`.

> **Be aware:** the Android code has never been compiled by me (no Android tools were available). The first build may fail. If it does, see "If the build fails" at the bottom and send me the log.

---
## Part A: Build the APK (demo mode, no API key needed)

Use **Chrome** on your phone. Open the Chrome menu (⋮) and turn on **Desktop site**, because GitHub's upload page works best that way.

1. **Create a GitHub account** at github.com (free) and verify your email.
2. **Create a repository:** tap **+** → **New repository**. Name it `matchly`. Choose **Private** (recommended). Tick **Add a README file**. Tap **Create repository**.
3. **Upload the ZIP (do not unzip it):** tap **Add file** → **Upload files** → **choose your files** → pick `MATCHLY-project.zip` from your Downloads → wait until it finishes → tap **Commit changes**.
4. **Add the build recipe:** tap **Add file** → **Create new file**. In the name box type exactly `.github/workflows/build-apk.yml` (typing `/` makes the folders). Open the file `build-apk.yml` I sent you, select all, copy, and paste it into the big text area. Tap **Commit changes** → **Commit changes**.
5. **Start/watch the build:** open the **Actions** tab. If GitHub asks, tap **I understand my workflows, go ahead and enable them**. A run called **Build MATCHLY debug APK** starts by itself (first run: it unpacks your ZIP, adds the real project files to the repository, then builds). It usually takes several minutes; the first run is the slowest.
6. **Download the APK:** when the run shows a green ✓, open it and scroll to **Artifacts** at the bottom. Tap **MATCHLY-debug-apk**. A small ZIP downloads.
7. **Install:** open the downloaded ZIP with the **Files** app (tap it → Extract), tap `app-debug.apk`, allow **Install unknown apps** for the app you are using if asked, then **Install**. If Play Protect warns, tap **Install anyway** (this is a debug build you made yourself).
8. Open **MATCHLY**: choose language → continue → finish. You will see lists marked **DEMO DATA** with fictional teams. That is expected until Part B.

**Re-building later:** Actions → *Build MATCHLY debug APK* → **Run workflow**.

---
## Part B: Connect real football data

### B1. Get an API-Football key (free)
1. Go to **dashboard.api-football.com**, register (no credit card for the free plan), confirm your email.
2. Open **Account → My Access** and copy your API key. **This is a secret: never paste it into GitHub files, chats or screenshots.** If it leaks, regenerate it on the same page.

### B2. Deploy the backend on Render (free)
1. Sign up at **render.com** (you can sign in with GitHub).
2. Tap **New +** → **Web Service** → connect your `matchly` repository.
3. Fill in: **Name** `matchly-api`, **Root Directory** `backend`, **Runtime** `Node`, **Build Command** `echo ok`, **Start Command** `node src/server.js`, **Instance Type** `Free`.
4. Under **Environment Variables** add:
   * `API_FOOTBALL_KEY` = your key
   * `PLAN_PRESET` = `free` (use `paid` after upgrading to Pro)
   * `APP_TOKEN` = any long random text you invent (optional but recommended)
5. Tap **Create Web Service**. Wait for **Live**, then copy the address at the top (like `https://matchly-api-xxxx.onrender.com`).
6. Test: open `https://YOUR-ADDRESS/v1/health` in Chrome. You should see `"ok":true,"configured":true`.

*Render's free service sleeps after 15 minutes without traffic and takes about a minute to wake, so the first load after a pause is slow. Render itself advises against free instances for production.*

### B3. Tell the app about the backend (pick one)
* **Quick:** in the app go to **Settings → Data source**, paste the address (and the `APP_TOKEN` if you set one), tap **Save and test**. It should say "Connected".
* **Built in:** on GitHub open **Settings → Secrets and variables → Actions → Variables → New repository variable**, name `MATCHLY_BACKEND_URL`, value = your address. Then run the workflow again and install the new APK. (The address is public information; it is not a secret.)

### What to expect on the free key
Only 100 requests a day, and the free plan may not include the current season. If the app says **"not included in the current data plan"**, that is the provider's restriction, not a bug. The backend refuses to go above 90 requests/day and shows saved data when it can.

---
## If the build fails
1. Actions → tap the red ✗ run → tap the **build** job → open the step marked ✗ → copy the red error lines (or tap ⋮ → **View raw logs**).
2. Send them to me. Compile errors are normally quick to fix. Then run the workflow again.

## Safety checklist
* Never put your API key in the repository, in `matchly.properties`, or in the app.
* The backend `APP_TOKEN` is only a deterrent (it is inside the APK).
* Do not share the debug APK publicly; build a signed release first (see ROADMAP).
