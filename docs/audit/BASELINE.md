# WealthOS — Baseline Audit & Environment State

**Date**: October 22, 2026  
**Working Branch**: `fix/financial-integrity-audit`  
**Base Commit**: `72c633f` (*refactor(ui): simplify terminology across the entire app*)  
**Corpus / Repository**: `c:\Projects\WealthOS`

---

## 1. System & Toolchain Environment

| Component | Version / Identifier | Notes |
| :--- | :--- | :--- |
| **Operating System** | Windows 11 10.0 amd64 | Shell: PowerShell |
| **Java Runtime** | Oracle JDK 21.0.10+8-LTS-217 | 64-bit Server VM |
| **Gradle** | 9.5.0 | Kotlin 2.3.20, Groovy 4.0.29 |
| **Node.js** | Node v20+ / npm v10+ | Client web workspace |
| **Vite** | 8.3.4 | TypeScript 5.8+ |
| **Android SDK** | Platforms 36, 37 | Build tools installed |

---

## 2. Repository Architecture & Layout

```
c:\Projects\WealthOS\
├── app\                       # Android Jetpack Compose application module
│   ├── src\main\kotlin\       # ViewModels, UI, Room DAOs, CloudSync, Biometrics
│   └── src\test\kotlin\       # JUnit unit tests (BackupEnvelopeTest, FinancialIntegrityTest)
├── domain\                    # Pure Kotlin domain logic module (zero Android dependencies)
│   ├── src\main\kotlin\       # Ledger, Double-Entry Invariants, RecurrenceEngine, SafeToSpend
│   └── src\test\kotlin\       # JUnit domain tests (LedgerTest, PlanReportTest, MoneyTest)
├── wealthos-web\              # React 19 + TypeScript + Vite web client
│   ├── src\components\        # Dashboard, Ledger, Accounts, Plans, Insights, Settings
│   ├── src\hooks\             # useData (Supabase CRUD & Local Cache), useAuth (GoTrue OTP)
│   ├── src\lib\               # backupCrypto (PBKDF2/AES-GCM), supabase client, types
│   └── test\                  # Node test runner suite (audit_integrity, backup_interop, etc.)
├── supabase\                  # Database migrations & Edge functions
│   ├── migrations\            # SQL migrations with RLS, triggers, functions, indexes
│   └── functions\             # Deno Edge Functions (gemini-coach, etc.)
└── docs\                      # Architecture, threat model, parity matrix, and audit records
```

---

## 3. Baseline Verification Runs

### 3.1 Web Application Build (`npm run build`)
- **Working Directory**: `c:\Projects\WealthOS\wealthos-web`
- **Command**: `npm run build`
- **Output**:
  ```
  > wealthos-web@0.0.0 build
  > tsc && vite build

  vite v8.3.4 building client environment for production...
  transforming...
  ✓ 2868 modules transformed.
  rendering chunks...
  computing gzip size...
  dist/index.html                   1.03 kB │ gzip:  0.56 kB
  dist/assets/index-QMgpus9u.css   19.68 kB │ gzip:  4.91 kB
  dist/assets/index-Cva3tZEm.js   262.48 kB │ gzip: 83.77 kB
  ✓ built in 795ms
  ```
- **Result**: PASS (0 compiler errors, clean bundle generated).

### 3.2 Web Test Suite (`npm test`)
- **Working Directory**: `c:\Projects\WealthOS\wealthos-web`
- **Command**: `npm test`
- **Output**:
  - `audit_integrity.test.js`: 15 tests pass (F-01, F-02, F-04, F-11, F-13)
  - `backup_interop.test.js`: 8 tests pass (PBKDF2/AES-GCM-128, bilingual normalization)
  - `financial_invariants.test.js`: 9 tests pass (Lakhs/Crores, minor unit math, CSV sanitization)
  - **Summary**: 32 tests passed across 7 test suites; 0 failures; 0 skipped.
- **Result**: PASS.

### 3.3 Android Domain Logic Tests (`:domain:test`)
- **Working Directory**: `c:\Projects\WealthOS`
- **Command**: `.\gradlew :domain:test`
- **Output**:
  ```
  BUILD SUCCESSFUL in 3s
  4 actionable tasks: 4 up-to-date
  ```
- **Result**: PASS (Ledger balancing, invariant validator, recurrence, plan allocation).

### 3.4 Android App Unit Tests (`:app:testDebugUnitTest`)
- **Working Directory**: `c:\Projects\WealthOS`
- **Command**: `.\gradlew :app:testDebugUnitTest`
- **Output**:
  ```
  BUILD SUCCESSFUL in 8s
  30 actionable tasks: 30 up-to-date
  ```
- **Result**: PASS (Backup envelope AEAD crypto, tamper rejection, viewmodels).

---

## 4. Initial Audit Findings Summary (Pre-Fix)

A total of 15 confirmed findings were identified during static and dynamic code inspection:
- **7 Critical P0 Findings**: Data loss, balance inflation, unhandled write failures, transfer corruption.
- **8 High P1 Findings**: Misleading analytics, fake AI output, silent truncation, security key fallbacks.
Full details and resolutions are cataloged in `docs/audit/FINDINGS.md`.
