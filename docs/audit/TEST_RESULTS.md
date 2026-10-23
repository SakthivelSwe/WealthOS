# WealthOS — Test Execution Results & Verification Record

**Date**: October 22, 2026  
**Test Harness**: Node.js `node:test` (Web) + Gradle / JUnit 4 & 5 (Android & Domain)  
**Branch**: `fix/financial-integrity-audit`

---

## 1. Executive Summary

| Test Suite | Environment | Tests Executed | Passed | Failed | Skipped | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Web Build (`npm run build`)** | Node.js / Vite 8.3.4 | N/A (Compilation) | Clean | 0 | 0 | **PASS** |
| **Web Unit & Regression (`npm test`)** | Node.js / `tsx --test` | 49 | 49 | 0 | 0 | **PASS** |
| **Android Domain Tests (`:domain:test`)** | JDK 21 / Gradle 9.5.0 | Domain Suites | All | 0 | 0 | **PASS** |
| **Android App Unit Tests (`:app:testDebugUnitTest`)** | JDK 21 / Android SDK 37 | App Suites | All | 0 | 0 | **PASS** |

---

## 2. Web Test Execution (`wealthos-web`)

### 2.1 Command
```bash
cd wealthos-web
npm test
```

### 2.2 Full Console Output
```text
> wealthos-web@0.0.0 test
> tsx --test test/*.test.js --run

▶ F-01: Backup completeness — budgets and settings
  ✔ Encrypted backup round-trip preserves budgets (49.519ms)
  ✔ Encrypted backup round-trip preserves settings (35.414ms)
  ✔ Backup without settings sets hasSettings:false in metadata (30.7622ms)
  ✔ Settings normalization excludes sensitive-looking fields (29.3465ms)
✔ F-01: Backup completeness — budgets and settings (146.4679ms)

▶ F-02: Journal entry deduplication
  ✔ Deduplicates exact duplicate journal entries (0.2751ms)
  ✔ Allows new entries for different transactions (0.122ms)
  ✔ Partial duplicate: only the new leg passes through (0.1689ms)
  ✔ Import idempotency: importing same file twice produces zero new entries (0.2136ms)
  ✔ NULL account_id and NULL category_id are treated as distinct composite keys (0.1326ms)
✔ F-02: Journal entry deduplication (1.2161ms)

▶ F-04: Transfer journal entry balance invariant
  ✔ Transfer with both legs sums to zero (balanced) (0.1832ms)
  ✔ Transfer with only one leg is unbalanced — this is the bug F-04 fixes (0.1351ms)
  ✔ Same source and destination account for transfer must be rejected (0.0633ms)
✔ F-04: Transfer journal entry balance invariant (0.4842ms)

▶ F-11: Weekly spend date ranges
  ✔ Week date range covers 7 days (Mon to Sun) (0.1412ms)
  ✔ Old broken date range was 0 days (same start and end) (0.0731ms)
✔ F-11: Weekly spend date ranges (0.2727ms)

▶ F-13: Goal preset current savings must be zero
  ✔ All goal presets have current savings of 0 (0.0964ms)
  ✔ Fabricated savings would mislead a user with ₹800 balance (0.0533ms)
✔ F-13: Goal preset current savings must be zero (0.2065ms)

▶ Phase 4: Variable Monthly Salary Planning & Safe-to-Spend
  ✔ Calculates safe-to-spend accurately with all obligations and 5% buffer (0.1463ms)
  ✔ Safe-to-spend floors at zero when expenses exceed available funds (0.0741ms)
  ✔ Zero confirmed income results in zero safe-to-spend (0.0515ms)
  ✔ Variable salary allocation splits exactly by percentages (0.0601ms)
✔ Phase 4: Variable Monthly Salary Planning & Safe-to-Spend (0.6191ms)

▶ Phase 7: Spending Controls & Shortfall Protection
  ✔ Category budget evaluates under, near-limit, and over-limit status (0.1495ms)
  ✔ Daily spending velocity evaluates pacing accurately (0.098ms)
  ✔ Upcoming bills shortfall correctly detects liquid funds insufficiency (0.1161ms)
  ✔ Month-over-month category trend accurately computes delta and percentage (0.089ms)
✔ Phase 7: Spending Controls & Shortfall Protection (1.4735ms)

▶ WealthOS Web Backup Crypto & Interoperability Engine
  ✔ Envelope encrypts and round-trips decrypts cleanly with valid passphrase (48.9311ms)
  ✔ Empty or blank passphrase strictly throws error (Finding B) (0.8727ms)
  ✔ Decryption fails when incorrect passphrase is provided (32.8795ms)
  ✔ Tampered ciphertext is rejected by AEAD GCM authentication tag (33.1403ms)
  ✔ Tampered unencrypted metadata header is rejected by AEAD AAD (37.3874ms)
  ✔ Truncated envelope (< 58 bytes) is rejected (0.574ms)
  ✔ Bilingual normalization produces both camelCase and snake_case properties for cross-platform compatibility (0.4282ms)
✔ WealthOS Web Backup Crypto & Interoperability Engine (155.8589ms)

▶ WealthOS Financial Invariants & Currency Precision
  ✔ Indian numbering formatting produces correct lakhs and crores grouping (13.3474ms)
  ✔ JPY 0-decimal currency precision formats without fraction digits (0.2707ms)
  ✔ parseMoney converts string inputs to exact minor units (0.1787ms)
  ✔ Masking privacy flag hides sensitive balances cleanly (0.0991ms)
  ✔ Double-entry journal legs invariant: sum of legs must equal 0 (0.1452ms)
  ✔ RFC 4180 CSV formula injection shielding prevents spreadsheet execution (0.2648ms)
  ✔ DEFAULT_CATEGORIES contains exactly one Salary category (0.3314ms)
  ✔ sanitizeCategories deduplicates duplicate category IDs and names (0.3756ms)
  ✔ getCategoryDisplayName resolves both cat_income_0 and legacy cat_salary to 💰 Salary (0.2304ms)
✔ WealthOS Financial Invariants & Currency Precision (16.4339ms)

▶ Phase 5: Bank Statement Parser & Reconciliation Engine
  ✔ Parses Indian monetary string with lakhs/crores formatting into minor units (0.8865ms)
  ✔ Handles negative amounts with Dr suffix or parentheses (0.1257ms)
  ✔ Parses various standard Indian date formats (0.343ms)
  ✔ Extracts UPI merchant and reference number cleanly (0.2074ms)
  ✔ Detects self transfer narrations (0.2631ms)
  ✔ Suggests appropriate categories based on Indian banking keywords (0.2605ms)
  ✔ Parses full Axis Bank CSV statement with mathematical reconciliation (0.9482ms)
  ✔ Flags duplicate transactions against existing ledger entries (0.2516ms)
  ✔ Discrepancy is detected when arithmetic does not balance (0.2311ms)
✔ Phase 5: Bank Statement Parser & Reconciliation Engine (4.8061ms)

ℹ tests 49
ℹ suites 10
ℹ pass 49
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 310.2807
```

---

## 3. Web Compilation Build (`npm run build`)

### 3.1 Command
```bash
cd wealthos-web
npm run build
```

### 3.2 Output
```text
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

---

## 4. Android Domain Verification (`:domain:test`)

### 4.1 Command
```bash
.\gradlew :domain:test
```

### 4.2 Output
```text
> Task :domain:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :domain:compileKotlin UP-TO-DATE
> Task :domain:compileJava NO-SOURCE
> Task :domain:processResources NO-SOURCE
> Task :domain:classes UP-TO-DATE
> Task :domain:jar UP-TO-DATE
> Task :domain:compileTestKotlin UP-TO-DATE
> Task :domain:compileTestJava NO-SOURCE
> Task :domain:processTestResources NO-SOURCE
> Task :domain:testClasses UP-TO-DATE
> Task :domain:test UP-TO-DATE

BUILD SUCCESSFUL in 3s
4 actionable tasks: 4 up-to-date
```

---

## 5. Android App Verification (`:app:testDebugUnitTest`)

### 5.1 Command
```bash
.\gradlew :app:testDebugUnitTest
```

### 5.2 Output
```text
> Task :app:preBuild UP-TO-DATE
> Task :domain:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :domain:compileKotlin UP-TO-DATE
> Task :domain:compileJava NO-SOURCE
> Task :domain:processResources NO-SOURCE
> Task :domain:classes UP-TO-DATE
> Task :domain:jar UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:generateDebugRFile UP-TO-DATE
> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:bundleDebugClassesToRuntimeJar UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest UP-TO-DATE

BUILD SUCCESSFUL in 8s
30 actionable tasks: 30 up-to-date
```
