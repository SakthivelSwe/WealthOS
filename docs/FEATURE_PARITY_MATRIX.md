# WealthOS — Feature Parity Matrix & Architecture Trace

**Date**: October 18, 2026  
**Scope**: Android Native App (Kotlin/Compose), Web Client (`wealthos-web`), and Supabase Backend.

---

## 1. Feature Parity Matrix

| Feature / Capability | Android Native | Web App | Backend (Supabase) | Persisted Store | Automated Tests | Actual Verification | Architectural Details |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Zero-Float Money Math & Invariants** | Supported (`Money.kt`) | Supported (`money.ts`) | Integer BigInt / numeric | Long (Paise) / BigInt | Yes | PASS (Appendix A.1–A.6) | 100% integer paise representation across Kotlin & TypeScript. Integer division with HALF_UP `(2*num + den) / (2*den)`. |
| **Universal Statement Import Pipeline** | Supported (`StatementParser.kt`) | Supported (`statementParser.ts`) | Client-side | Local Ingest / Atomic Batch | Yes | PASS (Axis & HDFC) | CSV/TSV/pasted text with column mapper fallback, RRN & counterparty extraction, mandate radar, small scan aggregation, undoable batch ID. |
| **Database & Schema Migration (v2 / v5)** | Supported (Room v5 `5.json`) | Supported | Supported (`20261024000000_wealthos_v2.sql`) | Room SQLite / Postgres | Yes | PASS | Room v5 export matches Supabase v2 migration schema: `salary_cycle`, `user_financial_profile`, `sinking_fund`, `merchant_rule`, `mandate`. |
| **Salary Cycles & Survival Mode** | Supported (`Planning.kt`) | Supported (`cycle.ts`, `useData.tsx`) | Supported | Room / Postgres | Yes | PASS (§5.3–§5.5) | Variable payday windows, survival mode triggered on < critical threshold (e.g. ₹800 crisis), daily safe UPI allowance meter. |
| **Plans & Simulators (Jars, SIP, Debt, EMI, 7-7-7)** | Supported (`domain/planning`) | Supported (`allocation.ts`, `plans/`) | Client-side engine | In-Memory / Profile | Yes | PASS (Appendix A.1–A.5) | 6-Jar, 3-Box, 50/30/20, milestone speedometers, Snowball vs Avalanche advantage, reducing-balance EMI with job-loss runway. |
| **Cycle Forensics & 4 Crisis Buckets** | Supported (`Forensics.kt`) | Supported (`forensics.ts`, `ledger/`) | Client-side engine | Ledger Aggregates | Yes | PASS (Appendix A.6) | ₹31,400 over 17 days -> 184,705 paise/day golden check. 4 Crisis Buckets: Fixed bills/rent/EMI, Food/tea/snacks, Shopping/subs, Transfers. 7-day velocity alert (>50%). |
| **AI Money Coach & Live Market Pulse** | Planned Client | Supported (`InsightsPage.tsx`) | Supported (`ai-analyze`, `ai-categorize`) | Supabase Edge Functions | Yes | PASS (§9.1–§9.3) | Gemini 2.5 Flash. AI never computes arithmetic. Coach mode receives precomputed integers. Market Pulse uses Google Search grounding. |
| **Payday Wizard (First 48h Protocol)** | Supported (`PlanningForms.kt`) | Supported (`PaydayWizardModal.tsx`) | Supported | `SalaryCycle.checklist_state` | Yes | PASS (§8.8, Videos 3 & 4) | 4-step execution: move emergency & SIP, load UPI Lite, pay fixed bills, audit mandates. Persisted checklist state. |
| **Behavioural Guardrails** | Supported | Supported (`BehaviouralGuardrailsCard.tsx`) | Local Storage | `wealthos_cooldown_wishlist` | Yes | PASS (§5.14, Videos 4, 5, 15) | 48-Hour impulse cooldown wishlist, lifestyle freeze tracker (24 months), rotating Quiet Wealth wisdom quotes. |
| **Multi-Account Roles & Vault Isolation** | Supported (`AccountsScreen.kt`) | Supported (`AccountsPage.tsx`) | Supported | Room `accountRole` / `user_accounts` | Yes | PASS (§8.6) | Strategic roles: `INCOME_VAULT`, `DAILY_UPI_SPEND`, `EMERGENCY_RESERVE`, `WEALTH_PORTFOLIO`, `LIABILITY_LOAN`. Prominent vault warning banner. |
| **Payday Transfer Plan & Configurable UPI Lite** | Supported | Supported (`AccountsPage.tsx`) | Client-side engine | Local Settings | Yes | PASS (§8.6) | Zero-drift step-by-step routing from vault to emergency, wealth, and daily UPI. Configurable UPI Lite per-txn and balance caps. |
| **Nightly Check-in Protocol (9:30 PM)** | Supported (`NightlyReminderReceiver`) | Supported (`NightlyCheckInModal.tsx`) | Inexact Alarms / Web Modal | Local Storage / Ledger | Yes | PASS (§8.8) | Daily spend vs safe allowance, quick 1-tap review for untagged spends, 48-hour cooldown audit. |
| **Encrypted Client-Side Backup (.wosenc)** | Supported (`DatabaseKeyProvider`) | Supported (`backupCrypto.ts`) | Storage Bucket | Local `.wosenc` / Supabase Storage | Yes | PASS (Finding B) | 100,000-iteration PBKDF2 + AES-GCM-128 backup format. Bilingual camelCase/snake_case mapping. Strict passphrase required. |
| **Double-Entry Journal & Ledger** | Supported | Supported | Supported | Room SQLite / Postgres | Yes | PASS | Atomic balanced double-entry legs. Account balances computed from journal legs + opening balance. |


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
- **Shared Snapshot Storage**: Both platforms support creating, depositing, and restoring encrypted snapshot archives (`.wosenc`) via the Supabase Storage bucket (`backups/<userId>/<fileName>.wosenc`).
- **Experimental On-Demand Sync**: Android contains `SupabaseSyncService` (commit `8074bd7`) which can manually push/pull Room records to/from Supabase tables for 6 core entities (categories, accounts, txns, journal entries, goals, debts). However, it does NOT provide continuous real-time replication, conflict resolution, or coverage of budgets, chits, recurring rules, and settings.
- **Authoritative Ledger Separation**: Without automated conflict resolution (CRDTs or vector clocks), concurrent edits on both platforms can diverge unless synchronized via export/import or clean on-demand sync.

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
