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

---

### Phase P1: Essential Usability, Financial Metrics, & Design System (COMPLETED)
- [x] **P1-1: Correct InsightsViewModel Donut & Weekly Pacing Logic**
  - Fixed monthly savings breakdown calculation to use planned monthly contributions rather than lifetime cumulative balances.
  - Eliminated false-positive "Over Expense" alert when spending from a zero-spend prior week baseline.
  - File: `InsightsViewModel.kt`.
- [x] **P1-2: Multi-Currency Minor Unit Precision in Integrity Validator**
  - Integrated canonical ISO 4217 currency metadata lookup in `DatabaseIntegrityValidator.kt` to preserve 0-decimal currencies (JPY).
  - File: `DatabaseIntegrityValidator.kt`.
- [x] **P1-3: "Quiet Wealth" Editorial Design System Implementation**
  - Implemented high-contrast, restrained dark/light tokens in `index.css`.
  - Removed gratuitous cyber-neon glows, neon badges, and rainbow gradients.
  - Added `@media (prefers-reduced-motion: reduce)` accessibility rules.
  - File: `wealthos-web/src/index.css`.
- [x] **P1-4: Automated Regression Suite**
  - Added comprehensive unit tests in `FinancialIntegrityTest.kt` verifying statement extraction, pacing, and savings calculations.
  - Files: `FinancialIntegrityTest.kt`, `app/build.gradle.kts`.

---

### Phase P2: Valuable Product Enhancements (NEXT STAGE)
- [ ] **P2-1: Web Client Encrypted Backup (.wosenc) Export & Import**
  - Port PBKDF2 + AES-GCM-128 cryptographic envelope decryption to WebAssembly / Web Crypto API to enable full client-side encrypted backup portability between Android and Web.
  - Dependencies: Web Crypto API (`SubtleCrypto`).
- [ ] **P2-2: Custom SMTP Gateway Configuration for Supabase**
  - Configure production transactional email provider (Resend, AWS SES) in Supabase Dashboard to remove the 3-4 OTP emails/hour limit on the free tier.
- [ ] **P2-3: Chit Fund / ROSCA Dashboard on Web**
  - Port the native Android Chit Fund tracker to the web React frontend to achieve feature parity for Indian financial instruments.

---

### Phase P3: Optional Polish & Future Capabilities (ROADMAP)
- [ ] **P3-1: Real-Time Multi-Master Row-Level Synchronization**
  - Design a Conflict-Free Replicated Data Type (CRDT) or vector-clock synchronization engine to support concurrent multi-device editing without master-slave overwrite hazards.
- [ ] **P3-2: Android Direct Statement PDF Native Text Rendering**
  - Integrate on-device MLKit text recognition for parsing photographed bank statements or scanned receipts entirely on-device without network transmission.

---

## 2. Manual Approvals & Deployment Actions Required

1. **Supabase SMTP Activation**:
   - Navigate to Supabase Dashboard > Authentication > SMTP Settings.
   - Configure custom SMTP credentials to support unthrottled customer onboarding.
2. **Web Production Deployment**:
   - The tested production bundle is ready in `wealthos-web/dist/`.
   - Deploy to Cloudflare Pages via GitHub Actions or manual commit push.
3. **Android Release Keystore**:
   - Debug build passes all tests. Release signing keys remain protected and uncommitted in local `keystore.properties`.
