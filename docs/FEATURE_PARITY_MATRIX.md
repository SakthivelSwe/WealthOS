# WealthOS — Feature Parity Matrix & Architecture Trace

**Date**: October 18, 2026  
**Scope**: Android Native App (Kotlin/Compose), Web Client (`wealthos-web`), and Supabase Backend.

---

## 1. Feature Parity Matrix

| Feature / Capability | Android Native | Web App | Backend (Supabase) | Persisted Store | Automated Tests | Actual Verification | Remaining Gap / Architectural Reality |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Authentication (Email OTP / Magic Link)** | Planned | Supported | Supported (GoTrue) | Supabase Auth | Yes | Yes (PASS) | Android uses local PIN/Biometric lock; web uses Supabase Email OTP. SMS phone auth was removed due to third-party SMS gateway fees. |
| **Biometric & App Screen Lock** | Supported | N/A | N/A | Encrypted SharedPreferences | Yes | Yes (PASS) | Native-only capability using Android BiometricPrompt & SQLCipher key unwrap. |
| **Double-Entry Journal & Ledger** | Supported | Supported | Supported | Android: SQLCipher Room<br>Web: Supabase Postgres | Yes | Yes (PASS) | Android builds atomic balanced legs (`JournalPosting`). Web writes to `user_transactions` with triggers and `user_journal_entries`. |
| **Account Creation & Categorization** | Supported | Supported | Supported | Android: Room `account`<br>Web: `user_accounts` | Yes | Yes (PASS) | Both platforms support full CRUD, account types (Bank, Cash, Card, Investment), and custom color icons. |
| **Safe-to-Spend / Net Surplus Engine** | Supported | Supported | Local Logic | Computed State | Yes | Yes (PASS) | Android computes safe-to-spend buffer. Web computes Monthly Net Surplus with transparent daily pacing (relabeled from safeToSpend). |
| **Budgeting (50/30/20 Needs/Wants/Savings)** | Supported | Supported | Supported | Android: Room `budget`<br>Web: `user_budgets` | Yes | Yes (PASS) | Visual progress meters, category lane allocations, and monthly boundary calculations. Budgets included in .wosenc payload. |
| **Financial Goals & Debt Paydown** | Supported | Supported | Supported | Android: Room `goal`/`debt`<br>Web: `user_goals`/`user_debts` | Yes | Yes (PASS) | Track target amounts, monthly planned contributions, target dates, and debt APRs/EMIs. Preserved in backup schema. |
| **Chit Fund / ROSCA Tracker** | Supported | Planned | N/A | Android: Room `chit_fund` | Yes | Yes (PASS) | Native-only Indian financial feature for tracking dividend returns, bid amounts, and monthly installments. |
| **Encrypted Client-Side Backup (.wosenc)** | Supported | Supported | Storage Bucket | Local `.wosenc` / Supabase Storage | Yes | Yes (PASS) | 100,000-iteration PBKDF2 + AES-GCM-128 backup format. Bilingual camelCase/snake_case mapping. Strict passphrase required. |
| **Encrypted Backup Restore & Staging** | Supported | Supported | Storage Bucket | Room Atomic / Web Ingest | Yes | Yes (PASS) | Verifies header magic, version, checksum, and previews entity counts before committing. Leaves DB untouched if tampered. |
| **Bank Statement Import (CSV/Text)** | Supported | Supported | Edge Function | Parsed to Transaction Drafts | Yes | Yes (PASS) | Supports Indian comma numbers, DR/CR detection, reference number extraction, and duplicate candidate flagging. |
| **Deterministic Financial Advice & Lens** | Supported | Supported | Local Engine | In-Memory / Cached | Yes | Yes (PASS) | Rule-based financial principles (Babylon, Psychology of Money, Ramit Sethi) with 0 third-party API exposure. |
| **Live Multi-Device Row-Level Sync** | Roadmap | Roadmap | Supported (RLS) | N/A | N/A | NOT RUN | **Current state is Encrypted Snapshot Backup & Recovery**, NOT live continuous two-way row synchronization. |
| **Data Export (Encrypted & Plain CSV)** | Supported | Supported | Supabase API | Device File System / Browser Download | Yes | Yes (PASS) | Android exports encrypted envelope or CSV; Web exports RFC 4180 CSV with formula injection sanitization (`=`, `+`, `-`, `@`). |
| **Account & Data Deletion** | Supported (Local DB drop) | Supported | Supported (RLS cascade) | SQLCipher / Supabase Postgres + Storage | Yes | Yes (PASS) | Web purges all 11 user tables in reverse dependency order and user storage objects under `${userId}/`. |

---

## 2. Platform Sources of Truth & Data Ownership (Phase 8 Audit)

### 2.1 Where is Android's authoritative financial state stored?
- Android is strictly **local-first**. The single source of truth is an AES-256 encrypted SQLCipher SQLite database stored on the device (`money.db`).
- All reads, writes, journal legs, and aggregations run locally via Room DAOs (`db.accountDao()`, `db.ledgerDao()`).

### 2.2 Where is Web's authoritative financial state stored?
- The web application (`wealthos-web`) is **cloud-authoritative against Supabase Postgres**.
- Each authenticated user operates on private rows governed by Postgres Row Level Security (`auth.uid() = user_id`).
- Local storage and in-memory caches are ephemeral UI performance caches, not the authoritative ledger.

### 2.3 Which features share server records?
- **Zero features share live server records directly between Android and Web**.
- Android does NOT read from or write to the web app's `user_transactions` or `user_accounts` tables.
- The ONLY shared server touchpoint between Android and Web is the Supabase Storage bucket (`backups/<userId>/<fileName>.wosenc`), where encrypted snapshot blobs can be deposited or retrieved.

### 2.4 Which features operate offline?
- **Android**: 100% of core features (account creation, transaction logging, double-entry ledger balance computation, budget tracking, goal planning, chit funds, statement import, and local encrypted vault export) operate completely offline with no network connectivity required.
- **Web**: Read-only navigation operates temporarily on cached in-memory/localStorage data. However, write operations (creating transactions, editing accounts, updating budgets) require network connectivity to Supabase Postgres. If offline, the user is warned and writes cannot silently claim persistence.

### 2.5 Which operations upload an encrypted snapshot?
- Android: `CloudSyncService.backupToCloud(passphrase)` serializes local Room entities, encrypts them via PBKDF2/AES-GCM, and uploads to `${userId}/backup_${timestamp}.wosenc` in Supabase Storage.
- Web: In `SettingsPage.tsx`, clicking "Export Encrypted Vault" generates a `.wosenc` file and triggers a browser file download.

### 2.6 Which operations restore an encrypted snapshot?
- Android: `CloudSyncService.restoreFromCloud(passphrase, backupFile)` or local file import parses the `.wosenc` envelope, decrypts in memory, validates integrity, and executes an atomic Room database transaction.
- Web: In `SettingsPage.tsx`, selecting a `.wosenc` file prompts for the passphrase, validates integrity, opens the Ingestion Preview Modal showing exact entity counts, and imports records into Supabase Postgres.

### 2.7 Is live row-level synchronization actually implemented for each required table?
- **NO**. The application implements **Encrypted Snapshot Backup and Restore**, not live multi-master row-level replication.
- Labeling the current feature as "live sync" or "bidirectional sync" is explicitly incorrect and forbidden by architectural rules.

### 2.8 What happens when two devices edit the same record?
- Because Android operates on local SQLite and Web operates on Supabase Postgres without a shared multi-master replication protocol, edits on one platform do not reflect on the other until an explicit backup snapshot is exported and restored.
- Restoring a snapshot uses merge or staging semantics based on record primary keys (`id`), rather than CRDTs or operational transforms.

### 2.9 How are deletions and restored records handled?
- On Web, deleting a transaction removes both the `user_transactions` record and its corresponding `user_journal_entries` legs, ensuring account balances calculated from active journal entries remain mathematically balanced.
- On Android, soft-deleted or deleted transactions remove their journal legs atomically via Room.
- Restoring a backup into an existing dataset performs duplicate detection by ID; existing records with the same UUID are updated or preserved based on user selection.

### 2.10 What happens after a partial write or authentication expiry?
- In `useData.tsx`, every Supabase operation explicitly checks `{ data, error }`. If a network failure, RLS rejection, or token expiration occurs, the error is surfaced to the UI with an actionable message, preventing false success reports.
- Account deletion in `useAuth.tsx` executes table deletions in reverse foreign-key order. If any table deletion fails, local storage is preserved and the exact failed tables are returned to the user with a retry option.

### 2.11 Audit of Web Realtime Channel Subscriptions
- The web application currently polls or refetches state via `useData.tsx` upon user-initiated mutations.
- Realtime Postgres changes (`supabase.channel('public:user_transactions')`) can be subscribed, but client-side optimistic reconciliation must ensure that stale Realtime events do not overwrite in-flight mutations.

---

## 3. Discovered Redundancies, Stubs, & Navigation Integrity

1. **Phone SMS Authentication Stub**:
   - Web application previously displayed SMS verification UI tabs. Because third-party SMS OTP gateways (Twilio, MessageBird) require paid subscriptions and API keys, the authentication architecture was refactored to **100% Free Supabase Email OTP and Magic Link**. All references to SMS phone auth were removed from active flows.
2. **Demo Data Isolation**:
   - Verified that new users sign in to a completely clean slate with zero transactions and zero accounts.
   - Demo fixtures are strictly gated to explicit demo preview modes and never intermingle with real user ledgers, exports, or summaries.
3. **Dead Navigation Paths**:
   - Verified that all primary routes in web (`/dashboard`, `/ledger`, `/accounts`, `/plans`, `/insights`, `/settings`) and bottom navigation destinations in Android (`Home`, `Ledger`, `Add`, `Plans`, `Insights`, `Settings`) have functional viewmodels, valid routes, and back-stack handlers.
