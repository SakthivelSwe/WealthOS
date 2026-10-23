# WealthOS — Test Execution Results & Verification Record

**Date**: October 22, 2026  
**Test Harness**: Node.js `node:test` (Web) + Gradle / JUnit 4 & 5 (Android & Domain)  
**Branch**: `fix/financial-integrity-audit`

---

## 1. Executive Summary

| Test Suite | Environment | Tests Executed | Passed | Failed | Skipped | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Web Build (`npm run build`)** | Node.js / Vite 8.3.4 | N/A (Compilation) | Clean | 0 | 0 | **PASS** |
| **Web Unit & Regression (`npm test`)** | Node.js / `tsx --test` | 32 | 32 | 0 | 0 | **PASS** |
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
> tsx --test test/*.test.js

▶ F-01: Backup completeness — budgets and settings
  ✔ Encrypted backup round-trip preserves budgets (48.4426ms)
  ✔ Encrypted backup round-trip preserves settings (38.648ms)
  ✔ Backup without settings sets hasSettings:false in metadata (36.2214ms)
  ✔ Settings normalization excludes sensitive-looking fields (32.2643ms)
✔ F-01: Backup completeness — budgets and settings (156.8325ms)

▶ F-02: Journal entry deduplication
  ✔ Deduplicates exact duplicate journal entries (0.3109ms)
  ✔ Allows new entries for different transactions (0.1498ms)
  ✔ Partial duplicate: only the new leg passes through (0.1308ms)
  ✔ Import idempotency: importing same file twice produces zero new entries (0.1456ms)
  ✔ NULL account_id and NULL category_id are treated as distinct composite keys (0.1673ms)
✔ F-02: Journal entry deduplication (1.2127ms)

▶ F-04: Transfer journal entry balance invariant
  ✔ Transfer with both legs sums to zero (balanced) (0.2189ms)
  ✔ Transfer with only one leg is unbalanced — this is the bug F-04 fixes (0.1135ms)
  ✔ Same source and destination account for transfer must be rejected (0.0641ms)
✔ F-04: Transfer journal entry balance invariant (0.5134ms)

▶ F-11: Weekly spend date ranges
  ✔ Week date range covers 7 days (Mon to Sun) (0.1346ms)
  ✔ Old broken date range was 0 days (same start and end) (0.096ms)
✔ F-11: Weekly spend date ranges (0.3231ms)

▶ F-13: Goal preset current savings must be zero
  ✔ All goal presets have current savings of 0 (0.1285ms)
  ✔ Fabricated savings would mislead a user with ₹800 balance (0.0681ms)
✔ F-13: Goal preset current savings must be zero (0.2826ms)

▶ WealthOS Web Backup Crypto & Interoperability Engine
  ✔ Envelope encrypts and round-trips decrypts cleanly with valid passphrase (48.8611ms)
  ✔ Empty or blank passphrase strictly throws error (Finding B) (0.6809ms)
  ✔ Decryption fails when incorrect passphrase is provided (29.2966ms)
  ✔ Tampered ciphertext is rejected by AEAD GCM authentication tag (29.1533ms)
  ✔ Tampered unencrypted metadata header is rejected by AEAD AAD (37.3067ms)
  ✔ Truncated envelope (< 58 bytes) is rejected (0.3208ms)
  ✔ Bilingual normalization produces both camelCase and snake_case properties for cross-platform compatibility (0.2159ms)
✔ WealthOS Web Backup Crypto & Interoperability Engine (147.2776ms)

▶ WealthOS Financial Invariants & Currency Precision
  ✔ Indian numbering formatting produces correct lakhs and crores grouping (16.6264ms)
  ✔ JPY 0-decimal currency precision formats without fraction digits (0.2175ms)
  ✔ parseMoney converts string inputs to exact minor units (0.1422ms)
  ✔ Masking privacy flag hides sensitive balances cleanly (0.0726ms)
  ✔ Double-entry journal legs invariant: sum of legs must equal 0 (0.1225ms)
  ✔ RFC 4180 CSV formula injection shielding prevents spreadsheet execution (0.2045ms)
  ✔ DEFAULT_CATEGORIES contains exactly one Salary category (0.1242ms)
  ✔ sanitizeCategories deduplicates duplicate category IDs and names (0.2032ms)
  ✔ getCategoryDisplayName resolves both cat_income_0 and legacy cat_salary to 💰 Salary (0.1266ms)
✔ WealthOS Financial Invariants & Currency Precision (18.8329ms)

ℹ tests 32
ℹ suites 7
ℹ pass 32
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 351.2581
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
