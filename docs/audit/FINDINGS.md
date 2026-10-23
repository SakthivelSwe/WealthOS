# WealthOS — Financial Integrity & Security Findings Log

**Date**: October 22, 2026  
**Scope**: Code audit across Android native client, Web client (`wealthos-web`), Supabase schema, and Domain logic.  
**Branch**: `fix/financial-integrity-audit`

---

## Findings Matrix

| Finding ID | Severity | Module / File | Category | Status | Automated Test |
| :--- | :---: | :--- | :--- | :---: | :---: |
| **F-01** | **P0** | `wealthos-web/src/lib/backupCrypto.ts` | Data Loss / Backup Round-Trip | **RESOLVED** | `audit_integrity.test.js` |
| **F-02** | **P0** | `wealthos-web/src/hooks/useData.tsx` | Balance Inflation / Dedup | **RESOLVED** | `audit_integrity.test.js` |
| **F-03** | **P0** | `wealthos-web/src/hooks/useData.tsx` | Silent Cloud Write Failure | **RESOLVED** | Code & UI Return Structure |
| **F-04** | **P0** | `wealthos-web/src/components/shared/NewTransactionModal.tsx` | Unbalanced Ledger Legs | **RESOLVED** | `audit_integrity.test.js` |
| **F-05** | **P0** | `wealthos-web/src/hooks/useData.tsx` | Optimistic Write Rollback | **RESOLVED** | Granular Sync State Tracking |
| **F-06** | **P0** | `wealthos-web/src/hooks/useData.tsx` & SQL Migration | Non-Atomic Journal Replacement | **RESOLVED** | `replace_journal_entries` RPC |
| **F-07** | **P0** | `docs/FEATURE_PARITY_MATRIX.md` | Inaccurate Sync Documentation | **RESOLVED** | Matrix Realigned with Code |
| **F-08** | **P1** | `wealthos-web/src/lib/supabase.ts` | Fake Production Anon Key | **RESOLVED** | Production Key Guard |
| **F-09** | **P1** | `wealthos-web/src/hooks/useAuth.tsx` | Storage Cleanup Silently Failed | **RESOLVED** | Table & Storage Error Tracking |
| **F-10** | **P1** | `wealthos-web/src/hooks/useData.tsx` & SQL Migration | 500-Row Truncation on Ledger | **RESOLVED** | Server-Side Aggregates RPC |
| **F-11** | **P1** | `wealthos-web/src/components/insights/InsightsPage.tsx` | 0-Day Weekly Chart Date Bug | **RESOLVED** | `audit_integrity.test.js` |
| **F-12** | **P1** | `wealthos-web/src/components/insights/InsightsPage.tsx` | Fabricated Fake AI Fallback | **RESOLVED** | Honest Unavailable Indicator |
| **F-13** | **P1** | `wealthos-web/src/components/plans/PlansPage.tsx` | Fabricated Goal Presets Savings | **RESOLVED** | `audit_integrity.test.js` |
| **F-14** | **P1** | `domain/.../PlanReport.kt` & `Dashboard.tsx` | Variable Salary Unhandled | **RESOLVED** | Dynamic Basis Income Support |
| **F-15** | **P1** | `wealthos-web/src/hooks/useAuth.tsx` | Demo OTP Fallback Risk | **RESOLVED** | Strict Localhost Dev Guard |

---

## Detailed Finding Analysis & Technical Fixes

### Finding F-01 (P0): Backup Export Omitted Budgets and Settings
- **Location**: `wealthos-web/src/lib/backupCrypto.ts` (`normalizePayloadForExport()`)
- **Vulnerability**: While `BackupPayload` interface declared `budgets?: any[]`, `normalizePayloadForExport` only returned `accounts, categories, txns, journal_entries, goals, debts, chits, recurring`.
- **Financial Impact**: Exporting an encrypted `.wosenc` backup resulted in silent permanent loss of all user-configured 50/30/20 budgets and user settings (currency, salary baseline, pay day).
- **Resolution**: Added bilingual normalization for `budgets` (both camelCase and snake_case properties), included `settings` in export payload, and updated metadata with `budgetCount` and `hasSettings` flags.
- **Verification**: Verified via `audit_integrity.test.js` ("Encrypted backup round-trip preserves budgets" & "preserves settings").

---

### Finding F-02 (P0): Duplicate Journal Entries on Repeated Ingest
- **Location**: `wealthos-web/src/hooks/useData.tsx` (`importAllData()`)
- **Vulnerability**: Journal entries were merged via naive array concatenation: `const combined = [...prev, ...incomingEntries]`.
- **Financial Impact**: Restoring the same backup file twice doubled all journal entries. Asset accounts with ₹800 balance would have debits duplicated, reducing calculated balance to -₹30,600 or inflating income to ₹64,400.
- **Resolution**: 
  1. Client-side deduplication using composite key: `${txn_id}|${account_id ?? ''}|${category_id ?? ''}`.
  2. Database migration `20261023000000_financial_integrity.sql` adding partial unique indexes `user_journal_entries_dedup_acct_idx` and `user_journal_entries_dedup_cat_idx`.
- **Verification**: Verified via `audit_integrity.test.js` ("Import idempotency: importing same file twice produces zero new entries").

---

### Finding F-03 (P0): False Success Reported on Cloud Sync Failure
- **Location**: `wealthos-web/src/hooks/useData.tsx` (`importAllData()`)
- **Vulnerability**: Errors during Supabase upserts were caught with `console.warn` only, and the method unconditionally returned `{ success: true, count }`.
- **Financial Impact**: User saw "Successfully imported 142 records", assuming their financial history was safely backed up in the cloud, while all Supabase writes had failed and data existed only in volatile browser `localStorage`.
- **Resolution**: Restructured `importAllData` return signature to track `cloudSyncedCount`, `cloudFailedCount`, `errors`, and return `success: false, partialSuccess: true` whenever any cloud write fails.

---

### Finding F-04 (P0): Unbalanced Transfer Journal Legs Allowed
- **Location**: `wealthos-web/src/components/shared/NewTransactionModal.tsx`
- **Vulnerability**: Missing validation on destination account (`toAccountId`) and a conditional guard `if (toAccountId)` on the credit leg.
- **Financial Impact**: A user could create a TRANSFER where money left Account A (debit leg recorded) but was never credited to any destination account (credit leg omitted). This broke the fundamental double-entry zero-sum invariant ($\sum \text{legs} = 0$).
- **Resolution**: Enforced strict pre-submission validation: `toAccountId` must be present and distinct from `accountId`. Removed the conditional guard so both legs are guaranteed atomically.
- **Verification**: Verified via `audit_integrity.test.js` ("Transfer with only one leg is unbalanced", "Same source and destination account for transfer must be rejected").

---

### Finding F-05 (P0): Optimistic UI Update Without Rollback or Sync State
- **Location**: `wealthos-web/src/hooks/useData.tsx` (`addTransaction()`)
- **Vulnerability**: React state and localStorage were mutated before cloud writes. If Supabase write failed, no retry was scheduled, no error was raised, and no pending flag was attached.
- **Financial Impact**: Data drift between local browser storage and cloud database. Clearing cache or opening the app on a second device revealed missing transactions.
- **Resolution**: Surfaced cloud write warnings, established consistent sync tracking, and enabled recovery re-fetch on failures.

---

### Finding F-06 (P0): Non-Atomic Journal Replacement Risk
- **Location**: `wealthos-web/src/hooks/useData.tsx` (`updateTransaction()`)
- **Vulnerability**: Code performed `delete().eq('txn_id', id)` followed by `insert(...)`. If the network dropped after delete succeeded, journal legs were permanently lost while the transaction header remained.
- **Financial Impact**: Account balance suddenly jumped or dropped without any matching transaction entries.
- **Resolution**: 
  1. Created Postgres function `replace_journal_entries(p_user_id, p_txn_id, p_entries)` wrapped in an atomic PL/pgSQL transaction block.
  2. Updated `useData.tsx` to invoke `supabase.rpc('replace_journal_entries', ...)` first, with safe verified fallback.
- **Verification**: Encapsulated in database migration `20261023000000_financial_integrity.sql`.

---

### Finding F-07 (P0): Documentation Contradicted Actual Sync Implementation
- **Location**: `docs/FEATURE_PARITY_MATRIX.md` vs Android Commit `8074bd7`
- **Vulnerability**: Documentation claimed zero server sync existed, whereas `SupabaseSyncService.kt` had been committed with manual push/pull for 6 entities, but lacked coverage for budgets, chits, recurring, and settings.
- **Financial Impact**: Confusion over where real money transactions were persisted; risk of relying on unproven bidirectional sync.
- **Resolution**: Realigned `docs/FEATURE_PARITY_MATRIX.md` to state the exact technical reality: Android possesses an experimental on-demand sync for 6 entities, but reliable cross-platform state transfer remains Encrypted Snapshot Backup (`.wosenc`).

---

### Finding F-08 (P1): Production App Initialized with Fake Anon Key
- **Location**: `wealthos-web/src/lib/supabase.ts`
- **Vulnerability**: Fallback literal `'public-anon-key'` allowed the client to initialize even if `VITE_SUPABASE_ANON_KEY` was missing in production builds.
- **Financial Impact**: Application appeared to function from local storage, but all cloud interactions silently failed with 401 Unauthorized.
- **Resolution**: Updated `supabase.ts` to throw a fatal error immediately in production if `VITE_SUPABASE_ANON_KEY` is undefined.

---

### Finding F-09 (P1): Account Deletion Storage Cleanup Silently Failed
- **Location**: `wealthos-web/src/hooks/useAuth.tsx` (`deleteAccount()`)
- **Vulnerability**: Errors deleting encrypted `.wosenc` backups from Supabase Storage were logged via `console.warn` and omitted from the returned failed tables list.
- **Financial Impact**: Users were informed their data was completely expunged, while encrypted backups containing financial histories remained in cloud buckets.
- **Resolution**: Added storage cleanup error tracking to `failedTables` array so failures are surfaced to the user with actionable retry steps.

---

### Finding F-10 (P1): 500-Row Client Fetch Truncation on Ledger
- **Location**: `wealthos-web/src/hooks/useData.tsx` (`fetchTransactions`)
- **Vulnerability**: Transactions query was capped with `.limit(500)`.
- **Financial Impact**: Users with more than 500 lifetime transactions had their dashboard metrics, net surplus, and account balances computed on truncated records, producing mathematically wrong numbers.
- **Resolution**: Created server-side aggregation RPCs `get_account_balance_totals` and `get_period_totals` in `20261023000000_financial_integrity.sql` that calculate exact totals across all historical rows without client-side truncation.

---

### Finding F-11 (P1): Weekly Insights Chart 0-Day Date Range Bug
- **Location**: `wealthos-web/src/components/insights/InsightsPage.tsx`
- **Vulnerability**: `wStart` and `wEnd` were both formatted from `wDate`, querying a single day rather than a 7-day week.
- **Financial Impact**: The 4-week spending chart showed single-day expenses labeled as weekly spending, misleading users regarding their weekly burn rate.
- **Resolution**: Corrected calculation to span `startOfWeek` to `endOfWeek` for each 7-day period.
- **Verification**: Verified via `audit_integrity.test.js` ("Week date range covers 7 days (Mon to Sun)").

---

### Finding F-12 (P1): Fabricated AI Output on Service Failure
- **Location**: `wealthos-web/src/components/insights/InsightsPage.tsx`
- **Vulnerability**: When the Gemini AI Edge Function failed or was offline, code simulated a 900ms delay and displayed hardcoded fake insights with a false confidence score of `0.94`.
- **Financial Impact**: Fabricated financial analysis was masqueraded as real machine learning insights.
- **Resolution**: Replaced fabricated fallback with an honest service unavailable alert, alongside deterministic rule-based analysis labeled `source: 'rules-based'` with confidence 0.

---

### Finding F-13 (P1): Fabricated Savings in Goal Presets
- **Location**: `wealthos-web/src/components/plans/PlansPage.tsx`
- **Vulnerability**: Goal templates had hardcoded `current: '25000'`, `current: '20000'`, `current: '100000'` pre-populated in the form.
- **Financial Impact**: Selecting "Emergency Fund" claimed the user already possessed ₹25,000 in savings, creating a dangerous false sense of security for a user who actually had ₹800 in their account.
- **Resolution**: Set all preset `current` values to `'0'` and updated helper copy directing users to enter their actual saved amounts.
- **Verification**: Verified via `audit_integrity.test.js` ("All goal presets have current savings of 0").

---

### Finding F-14 (P1): Variable Salary Unhandled in Planning
- **Location**: `domain/.../PlanReport.kt` & `wealthos-web/src/components/dashboard/Dashboard.tsx`
- **Vulnerability**: Budget allocation assumed a static monthly `basis_income_minor`.
- **Financial Impact**: If actual credited salary was ₹32,200 this month (versus a static settings assumption), the 50/30/20 budget envelope did not dynamically adjust to actual received funds.
- **Resolution**: Updated `Dashboard.tsx` and planning engines to prioritize actual monthly credited income (`monthlyIncome > 0 ? monthlyIncome : basisIncome`), ensuring budgets scale to real cash inflows.

---

### Finding F-15 (P1): Demo OTP Fallback Risk in Production
- **Location**: `wealthos-web/src/hooks/useAuth.tsx` (`signInWithEmail()`)
- **Vulnerability**: Demo OTP code `123456` was triggered if `isApiKeyError` occurred, without strictly checking if running on `localhost`.
- **Financial Impact**: A production Supabase misconfiguration could expose demo login simulation to production users.
- **Resolution**: Added strict environment guard requiring `window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'`, rejecting demo fallbacks in production.
