# WealthOS — Feature Parity Matrix & Architecture Trace

**Date**: October 18, 2026  
**Scope**: Android Native App (Kotlin/Compose), Web Client (`wealthos-web`), and Supabase Backend.

---

## 1. Feature Parity Matrix

| Feature / Capability | Android Native | Web App | Backend (Supabase) | Persisted Store | Tested | Verified | Gaps & Architectural Notes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Authentication (Email OTP / Magic Link)** | Planned | Supported | Supported (GoTrue) | Supabase Auth | Yes | Yes (PASS) | Android uses local PIN/Biometric lock; web uses Supabase Email OTP. SMS phone auth was removed due to SMS provider fees. |
| **Biometric & App Screen Lock** | Supported | N/A | N/A | Encrypted SharedPreferences | Yes | Yes (PASS) | Native-only capability using Android BiometricPrompt & SQLCipher key unwrap. |
| **Double-Entry Journal & Ledger** | Supported | Supported | Supported | Android: SQLCipher Room<br>Web: Supabase Postgres | Yes | Yes (PASS) | Android domain builds atomic balanced legs (`JournalPosting`). Web writes to `user_transactions` with triggers. |
| **Account Creation & Categorization** | Supported | Supported | Supported | Android: Room `account`<br>Web: `user_accounts` | Yes | Yes (PASS) | Both platforms support full CRUD, account types (Bank, Cash, Card, Investment), and custom color icons. |
| **Safe-to-Spend Calculation Engine** | Supported | Supported | Local Logic | Computed State | Yes | Yes (PASS) | Discretionary burn calculation based on liquid accounts, remaining days, committed EMIs, and bills. |
| **Budgeting (50/30/20 Needs/Wants/Savings)** | Supported | Supported | Supported | Android: Room `budget`<br>Web: `user_budgets` | Yes | Yes (PASS) | Visual progress meters, category lane allocations, and monthly boundary calculations. |
| **Financial Goals & Debt Paydown** | Supported | Supported | Supported | Android: Room `goal`/`debt`<br>Web: `user_goals`/`user_debts` | Yes | Yes (PASS) | Track target amounts, monthly planned contributions, target dates, and debt APRs/EMIs. |
| **Chit Fund / ROSCA Tracker** | Supported | Planned | N/A | Android: Room `chit_fund` | Yes | Yes (PASS) | Native-only Indian financial feature for tracking dividend returns, bid amounts, and monthly installments. |
| **Encrypted Client-Side Backup (.wosenc)** | Supported | Supported | Storage Bucket | Local `.wosenc` / Supabase Storage | Yes | Yes (PASS) | 100,000-iteration PBKDF2 + AES-GCM-128 backup format. Seamlessly portable across Android and Web. |
| **Encrypted Backup Restore & Staging** | Supported | Supported | Storage Bucket | Room Atomic / Web Ingest | Yes | Yes (PASS) | Verifies header magic, version, checksum, and previews entity counts before committing. Leaves DB untouched if tampered. |
| **Bank Statement Import (CSV/Text)** | Supported | Supported | Edge Function | Parsed to Transaction Drafts | Yes | Yes (PASS) | Supports Indian comma numbers, DR/CR detection, reference number extraction, and duplicate candidate flagging. |
| **Deterministic Financial Advice & Lens** | Supported | Supported | Local Engine | In-Memory / Cached | Yes | Yes (PASS) | Rule-based financial principles (Babylon, Psychology of Money, Ramit Sethi) with 0 third-party API exposure. |
| **Live Multi-Device Row-Level Sync** | Roadmap | Roadmap | Supported (RLS) | N/A | N/A | NOT RUN | **Current state is Encrypted Snapshot Backup & Recovery**, NOT live continuous two-way row synchronization. |
| **Data Export (Encrypted & Plain CSV)** | Supported | Supported | Supabase API | Device File System / Browser Download | Yes | Yes (PASS) | Android exports encrypted envelope or CSV; Web exports CSV with injection sanitization. |

---

## 2. Platform Sources of Truth & Data Ownership

### 2.1 Android Source of Truth
- The native Android app is **authoritative on the local device**.
- Data resides in an AES-256 encrypted SQLCipher SQLite database (`db.accountDao()`, `db.ledgerDao()`).
- Cloud interactions in Android are limited to:
  1. Uploading client-side encrypted backup snapshots to the private Supabase Storage bucket (`backups/<userId>/backup_<timestamp>.wosenc`).
  2. Downloading and restoring encrypted snapshots.
- When an Android user creates or updates a record, it writes immediately to local SQLite. No remote server is required for full functionality.

### 2.2 Web Source of Truth
- The web application (`wealthos-web`) is **authoritative against Supabase Postgres**.
- Each authenticated user accesses their private rows via Postgres Row Level Security (`user_id = auth.uid()`).
- The web client caches state in memory via React hooks and local state management.

### 2.3 Clarification on "Synchronization"
- **Critical Architectural Distinction**: WealthOS currently implements **Encrypted Backup & Recovery**, not continuous real-time multi-master synchronization.
- Android and Web operate on isolated data stores unless an encrypted snapshot is restored.
- Live real-time bidirectional syncing is an explicit roadmap capability requiring a deterministic CRDT or operational transformation engine to prevent ledger discrepancies.

---

## 3. Discovered Redundancies, Stubs, & Navigation Integrity

1. **Phone SMS Authentication Stub**:
   - Web application previously displayed SMS verification UI tabs. Because third-party SMS OTP gateways (Twilio, MessageBird) require paid subscriptions and API keys, the authentication architecture was refactored to **100% Free Supabase Email OTP and Magic Link**. All references to SMS phone auth were removed from active flows.
2. **Demo Data Isolation**:
   - Verified that new users sign in to a completely clean slate with zero transactions and zero accounts.
   - Demo fixtures are strictly gated to explicit demo preview modes and never intermingle with real user ledgers, exports, or summaries.
3. **Dead Navigation Paths**:
   - Verified that all primary routes in web (`/dashboard`, `/ledger`, `/accounts`, `/plans`, `/insights`, `/settings`) and bottom navigation destinations in Android (`Home`, `Ledger`, `Add`, `Plans`, `Insights`, `Settings`) have functional viewmodels, valid routes, and back-stack handlers.
