# WealthOS — Implementation Plan & Strategic Roadmap

**Date**: October 18, 2026  
**Status**: Active Execution / Phase 13 Finalization  
**Architecture Classification**: Local-First, Encrypted, Privacy-Preserving Financial Operating System

---

## 1. Prioritized Implementation Backlog

### Phase P0: Correctness, Security, & Data Integrity (COMPLETED)
- [x] **P0-1: Correct LedgerViewModel Update Field Mapping**
  - Resolved `ledger.updateDetails` passing `merchant` twice, which erased custom transaction descriptions.
  - Files: `LedgerViewModel.kt`, `LedgerScreen.kt`.
- [x] **P0-2: Re-architect GeminiAIService Statement Extraction Engine**
  - Replaced hardcoded negative amount coercion with format-aware debit/credit detection.
  - Added support for Indian numbering (`1,50,000.00`), date isolation to prevent year misidentification, and ambiguous direction review flagging.
  - File: `GeminiAIService.kt`.
- [x] **P0-3: Align Android Cloud Backup Upload Path with Supabase Storage RLS**
  - Updated storage path from hardcoded `"vault/$fileName"` to authenticated user folder `"$userId/$fileName"`.
  - Files: `AppSettings.kt`, `CloudSyncService.kt`.
- [x] **P0-4: Prevent Authoritative Float/Double Calculations**
  - Enforced strict integer minor unit arithmetic across `Money.kt`, `SafeToSpendEngine.kt`, and journal postings.
- [x] **P0-5 (Finding A & B): Encrypted Backup Interoperability & Passphrase Security**
  - Enforced mandatory user passphrases in `backupCrypto.ts` (removed predictable `wealthos_default_vault_key`).
  - Added envelope bounds validation (`MIN_ENVELOPE_SIZE = 58`, `MAX_ENVELOPE_SIZE = 50MB`, `MAX_METADATA_SIZE = 64KB`).
  - Implemented bilingual dual-case payload normalization (`normalizePayloadForExport`) generating both camelCase and snake_case for all 9 entity collections.
  - Exported `journal_entries` and `budgets` in web vault payloads.
  - Updated Android `CloudSyncService.kt` with lenient key/type parsers (`optStringLenient`, `optLongLenient`, `parseTimestampOrNull`).
  - Replaced raw browser `prompt()` with accessible modal dialogs in `SettingsPage.tsx`.
- [x] **P0-6 (Finding C): Web Database Error Handling & Journal Lifecycle**
  - Verified Supabase `{ error }` on every CRUD operation in `useData.tsx`.
  - Added cascading deletion of `user_journal_entries` when deleting a transaction in `deleteTransaction`, keeping balance totals balanced.
  - Added support for `journal_entries` and `budgets` in `importAllData`.
- [x] **P0-7 (Finding D): Cloud Account Deletion Safety**
  - Rewrote `deleteAllUserData` in `useAuth.tsx` to delete records in reverse foreign-key order across all 11 user tables.
  - Purged user backup objects under Supabase storage bucket `backups/${userId}/`.
  - Preserved local storage and returned an explicit failure list if cloud deletion fails, preventing false success reports.
- [x] **P0-8 (Finding E & F): Financial Integrity & Currency Aggregation**
  - Decoupled `journalSum` and account balance calculations from the 500-transaction pagination limit in `Dashboard.tsx` and `AccountsPage.tsx`.
  - Segregated Net Worth and Liquid Assets by currency (`balancesByCurrency`), avoiding raw addition of minor units across JPY, USD, and INR.
  - Relabeled dashboard metric from `safeToSpend` to "Monthly Net Surplus" with transparent daily pacing explanation.
  - Shielded CSV export in `SettingsPage.tsx` against formula injection (`=`, `+`, `-`, `@`, `\t`, `\r`) with RFC 4180 quote escaping.
- [x] **P0-9: Double-Entry Journal Invariants & Atomic Edit Persistence**
  - Updated `NewTransactionModal.tsx` to generate balanced debit/credit legs for EXPENSE, INCOME, and TRANSFER (`sum === 0`).
  - Updated `useData.tsx`'s `updateTransaction` to accept updated journal entries and synchronize `user_journal_entries` in cloud and local cache.
  - Updated `LedgerPage.tsx` edit modal to construct balanced journal legs when editing transactions.

---

### Phase P1: Usability, Design System, & CI/CD Verification (COMPLETED)
- [x] **P1-1: Quiet Wealth Design System Alignment (Finding G)**
  - Updated Android `primary` from neon green `0xFF00E599` to Quiet Wealth Forest Emerald `0xFF10B981` in `Theme.kt`.
  - Suppressed 60fps Three.js animation in `Canvas3DBackground.tsx`, 3D tilts in `Tilt3DCard.tsx`, and rotation in `HoloCard3D.tsx` on mobile devices and when `prefers-reduced-motion` is active.
  - Aligned `.btn-primary` and `.modal-content` in `index.css` with Quiet Wealth Forest Emerald and neutral elevated borders.
  - Redesigned `LoginPage.tsx` with Quiet Wealth styling and accurate architecture disclosures.
- [x] **P1-2: Settings Hydration & Theme Consistency (Finding H)**
  - Added asynchronous hydration `useEffect` in `SettingsPage.tsx` when cloud settings load.
  - Unified `themeMode` persistence with `wealthos_theme` in `localStorage`.
- [x] **P1-3: Web CI/CD & Automated Regression Suites (Finding I)**
  - Created automated test suite `wealthos-web/test/backupCrypto.test.js` covering envelope wire framing, AEAD AAD authentication, ciphertext tampering rejection, empty passphrase rejection, and bilingual normalization.
  - Added `"test": "node --test test/*.test.js"` script to `wealthos-web/package.json`.
  - Added `web-verify` job to `.github/workflows/verify.yml` running Node.js 20, `npm ci`, `npm test`, and `npm run build`.

---

### Phase P2: Valuable Product Enhancements (ACTIVE STAGE)
- [x] **P2-1: Accessible Vault Management Modals** (COMPLETED)
  - Export Vault Modal with passphrase confirmation and strength indicator.
  - Ingestion Preview Modal with pre-import entity counts.
- [ ] **P2-2: Custom SMTP Gateway Configuration for Supabase**
  - Configure production transactional email provider (Resend, AWS SES) in Supabase Dashboard to remove the 3-4 OTP emails/hour limit on the free tier.
- [x] **P2-3: Chit Fund / ROSCA Dashboard on Web** (COMPLETED)
  - Port the native Android Chit Fund tracker to the web React frontend to achieve feature parity for Indian financial instruments. Integrated in `PlansPage.tsx` with full `.wosenc` backup interoperability.

---

### Phase P3: Optional Polish & Future Capabilities (ROADMAP)
- [ ] **P3-1: Real-Time Multi-Master Row-Level Synchronization**
  - Design a Conflict-Free Replicated Data Type (CRDT) or vector-clock synchronization engine to support concurrent multi-device editing without master-slave overwrite hazards.
- [ ] **P3-2: Android Direct Statement PDF Native Text Rendering**
  - Integrate on-device MLKit text recognition for parsing photographed bank statements or scanned receipts entirely on-device without network transmission.

---

## 2. Safe Rollout & Verification Steps

1. **Local Test Execution**:
   - `npm test` in `wealthos-web` (All unit tests PASS).
   - `npm run build` in `wealthos-web` (Production Vite bundle builds cleanly in < 1.2s).
   - `./gradlew.bat test` in project root (Android unit & domain test suite passes 100%).
2. **CI Pipeline Verification**:
   - Push triggers both `android-verify` and `web-verify` jobs in `.github/workflows/verify.yml`.
3. **Database Migration Policy**:
   - Backward-compatible; dual-case payload normalization supports both legacy and updated schemas without requiring destructive DDL.
4. **Production Deployment Gate**:
   - Web production build is ready in `wealthos-web/dist/`.
   - Live authenticated deployment verification on Cloudflare Pages is marked BLOCKED pending owner-provided test credentials. Never deploy or modify production without explicit owner approval.
