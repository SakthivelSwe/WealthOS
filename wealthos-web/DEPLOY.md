# WealthOS Web & Cloud Synchronization Deployment Guide

WealthOS Web is a companion web application for WealthOS Android. Both apps share a unified Supabase cloud database with end-to-end security, instant bidirectional sync, and local-first resilience.

---

## 1. Cloudflare Pages vs. Render.com: Which One to Choose?

Both platforms support 100% free static site hosting, but **Cloudflare Pages is strongly recommended** for WealthOS Web for permanent, reliable zero-cost hosting.

| Feature | Cloudflare Pages (Recommended 🏆) | Render.com Static Site |
| :--- | :--- | :--- |
| **Price** | **100% Free Forever** | 100% Free |
| **Bandwidth** | **Unlimited** (Zero bill shock) | 100 GB / month |
| **Edge Network** | 330+ cities worldwide (Fastest load) | Regional edge (US / EU / SG) |
| **Build Limits** | 500 builds / month | 500 build minutes / month |
| **Cold Starts** | Zero (Static files cached on Cloudflare Edge) | Zero for static sites |
| **DDoS Protection** | Industry-standard enterprise shield | Standard protection |
| **SPA Client Routing**| Native via `_redirects` (Included) | Native via `_redirects` (Included) |
| **Custom Domains & SSL**| Free 1-click automatic HTTPS | Free automated HTTPS |
| **Deploy Options** | GitHub connection OR 10s CLI deploy (`wrangler`) | GitHub connection |

> [!TIP]
> **Recommendation**: Deploy on **Cloudflare Pages** as your primary production domain. You can also deploy to Render as a secondary mirror if desired.

---

## 2. Supabase Database Setup (Required for Both)

WealthOS uses Supabase (PostgreSQL) for phone auth, live WebSocket sync, and encrypted ledger storage.

You can run database migrations **automatically with Flyway** or through the web dashboard:

### Automated Method 1: Using Flyway via Gradle (Recommended)
Flyway is integrated into the root Gradle project. Run from the root directory:
```bash
# Windows PowerShell:
.\gradlew.bat flywayMigrate -Pflyway.password="YOUR_SUPABASE_DB_PASSWORD"

# Linux / macOS / CI:
./gradlew flywayMigrate -Pflyway.password="YOUR_SUPABASE_DB_PASSWORD"
```

### Automated Method 2: Using NPM (Zero-Java / Web Pipeline)
If you are working inside `wealthos-web` or in a Node.js CI/CD pipeline:
```bash
cd wealthos-web

# Windows PowerShell:
$env:SUPABASE_DB_PASSWORD="YOUR_SUPABASE_DB_PASSWORD"; npm run db:migrate

# Linux / macOS:
SUPABASE_DB_PASSWORD="YOUR_SUPABASE_DB_PASSWORD" npm run db:migrate
```
*(Both Flyway and `npm run db:migrate` share the exact same `flyway_schema_history` table in Supabase PostgreSQL, ensuring perfect interoperability and zero duplicate runs).*

### Automated Method 3: CI/CD via GitHub Actions
A workflow at `.github/workflows/db-migrate.yml` is preconfigured. Simply set `SUPABASE_DB_PASSWORD` in **GitHub Repository Settings → Secrets and variables → Actions**, and every new SQL migration file pushed to `main` will be automatically applied!

### Manual Fallback: Supabase Web SQL Editor
If you prefer not to enter a database password in the terminal:
1. Open your [Supabase Dashboard](https://supabase.com/dashboard) and navigate to your project.
2. In the left navigation, click **SQL Editor** → **New Query**.
3. Copy the entire contents of [20261014000000_web_sync.sql](file:///c:/Projects/WealthOS/supabase/migrations/20261014000000_web_sync.sql) and paste them in.
4. Click **Run** (Ctrl+Enter).
5. Obtain your Supabase API keys from **Project Settings** → **API** (`Project URL` and `anon public key`).

---

## 3. Option A: Deploy to Cloudflare Pages (Recommended)

### Method 1: Git Integration (Continuous Automated Deployment)

1. **Sign Up / Log In**:
   - Go to [dash.cloudflare.com](https://dash.cloudflare.com) and create a free account or log in.
2. **Create New Project**:
   - In the left sidebar, click **Workers & Pages**.
   - Click the **Create application** button, then select the **Pages** tab.
   - Click **Connect to Git**.
3. **Select Repository**:
   - Choose your GitHub account and select your `WealthOS` repository.
4. **Configure Build Settings**:
   - **Project Name**: `wealthos-web` (or your preferred name).
   - **Production Branch**: `main` (or `master`).
   - **Framework Preset**: Select **Vite**.
   - **Root Directory**: `wealthos-web`
   - **Build Command**: `npm run build`
   - **Build Output Directory**: `dist`
5. **Environment Variables**:
   Expand **Environment variables (advanced)** and add:
   - `VITE_SUPABASE_URL` = `https://zahzagnindkkttdpmqii.supabase.co`
   - `VITE_SUPABASE_ANON_KEY` = `your-supabase-anon-key`
6. **Deploy**:
   - Click **Save and Deploy**.
   - Cloudflare will build the site in ~45 seconds and provide you with a live HTTPS address:
     `https://wealthos-web.pages.dev`
   - All subsequent git commits pushed to `main` will automatically build and deploy!

---

### Method 2: Instant 10-Second CLI Deploy (No Git Connection Required)

If you want to deploy right now directly from your terminal:

```bash
# 1. Navigate to wealthos-web and build
cd wealthos-web
npm run build

# 2. Deploy directly with Cloudflare Wrangler (no installation needed)
npx wrangler pages deploy dist --project-name=wealthos-web
```
Cloudflare will prompt you to authenticate in your browser once, upload the static `dist/` directory, and output your live production URL immediately.

---

## 4. Option B: Deploy to Render.com

1. **Log in to Render**:
   - Go to [render.com](https://render.com) and log in.
2. **Create Static Site**:
   - Click **New +** (top right) → **Static Site**.
   - Connect your GitHub repository `WealthOS`.
3. **Configure Build & Output**:
   - **Name**: `wealthos-web`
   - **Branch**: `main`
   - **Root Directory**: `wealthos-web`
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: `dist`
4. **Environment Variables**:
   Click **Advanced** → **Add Environment Variable**:
   - `VITE_SUPABASE_URL`: `https://zahzagnindkkttdpmqii.supabase.co`
   - `VITE_SUPABASE_ANON_KEY`: `your-supabase-anon-key`
5. **Create Static Site**:
   - Click **Create Static Site**.
   - Render detects `public/_redirects` (`/* /index.html 200`) automatically to handle Single Page App client routing.
   - Your site will be live at `https://wealthos-web.onrender.com`.

---

## 5. Client Routing & SPA Deep Linking

WealthOS Web uses client-side routing (`react-router-dom`). When refreshing or opening deep URLs directly (e.g., `/accounts`, `/ledger`, `/insights`, `/plans`, `/config`):

- **Cloudflare Pages & Render**: Both platforms read the bundled `public/_redirects` file:
  ```
  /* /index.html 200
  ```
  This ensures that any requested path returns `index.html` with HTTP 200 status, allowing React Router to render the appropriate page instantly without 404 errors.

---

## 6. Testing & Verifying Mobile-Web Live Synchronization

1. **Sign In**:
   - Open your deployed URL on laptop, desktop, or mobile.
   - Sign in with your phone number, or click **"Instant Demo / Guest Access"** to preview immediately.
2. **Add a Transaction or Account**:
   - Press `Ctrl + K` or click **New Transaction**.
   - Select an account, specify category, and enter an amount.
3. **Observe Realtime Push**:
   - In Supabase, open the **Table Editor** for `user_transactions`. You will see the record appear with real-time replication.
   - Any other open browser tab or Android device running WealthOS updates immediately via WebSocket.
4. **Export / Backup Verification**:
   - Navigate to `/config` (Settings) → **Cloud Sync & Backups**.
   - Click **Export All (JSON)** to test exporting your complete financial ledger for offline custody.
