# WealthOS — Phase 0 Audit
Date: 2026-10-24  Re-verified: 2026-10-25  Spec: WEALTHOS_FINAL_SPEC.md v3
Branch audited: current HEAD (git pull was already up to date)

## 1. Actual File Tree

### 1.1 domain/ (pure Kotlin)

EXISTS:
  domain/imports/DuplicateDetector.kt, ImportSecurity.kt (also CsvFormat/FileSniffer/ZipGuard), StatementParser.kt
  domain/ledger/Ledger.kt, LedgerIntegrityValidator.kt
  domain/model/Catalog.kt, Money.kt
  domain/planning/Planning.kt, PlanReport.kt, RecurrenceEngine.kt
  domain/test/ (8 test files - all green per PROJECT_STATUS.md)

MISSING from domain/planning/ (spec 3.2):
  Allocation.kt    HealthRules.kt    SipProjection.kt    DebtPayoff.kt
  EmiSimulator.kt  Cycle.kt          Baseline.kt         Forensics.kt
  Goals.kt         Insurance.kt

MISSING from domain/imports/ (spec 3.2):
  NarrationDecoder.kt   CsvReader.kt   ColumnMapper.kt   Classifier.kt

### 1.2 app/src/main/kotlin/app/privatemoney/

EXISTS: Full UI (home, plans, ledger, imports, insights, accounts, onboarding, notifications),
  cloud/, data/, data/local/ (AppDatabase v4, Entities, DatabaseFactory), security/, work/
  Note: OnboardingScreen.kt + OnboardingViewModel.kt EXIST (spec 3.1 said they did not)
  Note: ImportStatementModal.tsx (web) ALREADY EXISTS 21KB (spec 3.1 said no web import page)

Room DB version: 4. Spec 4.5 requires v5 with salary_cycles + new TxnEntity columns.

### 1.3 wealthos-web/src/

EXISTS:
  components/ (accounts, auth, dashboard, insights, ledger/ImportStatementModal.tsx,
               notifications, plans, settings, shared - 15 files)
  hooks/ (useAuth, useData)
  lib/ (backupCrypto, mockData, statementParser, supabase)
  types/index.ts
  test/ (audit_integrity, backupCrypto, financial, statementParser - 4 test files)

MISSING from wealthos-web/src/lib/:
  money.ts  allocation.ts  healthRules.ts  sipProjection.ts  debtPayoff.ts
  emiSimulator.ts  cycle.ts  baseline.ts  forensics.ts  csvReader.ts
  narrationDecoder.ts  classifier.ts  duplicateDetector.ts

MISSING from wealthos-web/src/components/:
  onboarding/Onboarding.tsx

### 1.4 supabase/

MIGRATIONS PRESENT:
  20261007000000_init.sql
  20261014000000_web_sync.sql
  20261023000000_financial_integrity.sql  (dedup indexes, replace_journal_entries RPC)
  20261023000001_monthly_salary_plans.sql (user_monthly_plans, get_safe_to_spend RPC)

MISSING (spec 4.7):
  20261024000000_wealthos_v2.sql — new tx columns, RRN index, salary_cycles,
    user_financial_profiles, sinking_funds, merchant_rules, mandates

### 1.5 shared-test-vectors/
  DOES NOT EXIST. Spec 3.2 and 11 require allocation.json, sip.json, debt.json,
  emi.json, parser/*.csv, parser/*.expected.json

### 1.6 .github/workflows/
  EXISTS: verify.yml (Kotlin tests + npm test + build), db-migrate.yml, release.yml
  MISSING: float-math grep CI step (spec 11.3)

---

## 2. Float-Math Audit

CRITICAL-1: wealthos-web/src/lib/statementParser.ts lines 53-55
  const num = parseFloat(str);
  const minor = Math.round(Math.abs(num) * 100);
  parseStatementAmount() called for every import debit/credit. Violates spec 2.1.

CRITICAL-2: wealthos-web/src/types/index.ts lines 238-240
  const num = parseFloat(text.replace(/[^0-9.-]/g, ''));
  return Math.round(num * Math.pow(10, info.minorUnit));
  parseMoney() is the canonical string-to-minor-units fn used by NewTransactionModal,
  LedgerPage, PlansPage. Float multiply-then-round violates spec 2.1.

LOW / DISPLAY-ONLY (web):
  types/index.ts:231  amountMinor / Math.pow(10, minorUnit) — for toLocaleString, no calc
  Dashboard.tsx:362,371,380  Math.round(minor/income*100) — screen display only
  InsightsPage.tsx:133,135,333,367  same pattern — display only
  PlansPage.tsx:1612,1673  same pattern — display only
  NewTransactionModal.tsx:42, PlansPage.tsx:743, LedgerPage.tsx:43 — parseFloat as guards only

MEDIUM-3 (Android UI — INPUT path, not display):
  PlansForms.kt:128-130  (target/current/monthly).toDoubleOrNull() * 100  — goal form
  PlansForms.kt:182-184  (principal/balance/monthly).toDoubleOrNull() * 100 — loan form
  PlansForms.kt:215       monthly.toDoubleOrNull() — SIP form
  PlansScreen.kt:193     incomeStr.toDoubleOrNull() * 100 — income entry
  Spec 2.1: all string-to-minor-units conversions must use integer path.
  Fix: replace with integer string-split (same algorithm as parseAmountMinor in domain).

LOW / DISPLAY-ONLY (Android UI — canvas/progress bars, no money stored):
  DuplicateDetector.kt:56  levenshtein similarity ratio — similarity score only, not money
  GeminiAIService.kt:74    cleanDigits.toDoubleOrNull() — display stat label
  GeminiAIService.kt:200,205  weekly spend delta percent — display insight text
  HomeScreen.kt:675        LocalDate.lengthOfMonth().toFloat() — progress bar denominator
  InsightsScreen.kt:265,360-362,704,745,796-797  fraction / sweep — canvas drawing only
  PlansScreen.kt:479-493   split percent weights — UI layout weight only
  PlansScreen.kt:522,557,587  progress bar fraction — display only

Kotlin domain: CLEAN. No toDouble/toFloat in any money arithmetic path.
MoneyFormat.kt: CLEAN. BigDecimal.valueOf(minor, minorUnit) for display.

---

## 3. Logic Duplicated Between Android and Web

CSV parsing: Android=CsvFormat.parse() correct RFC4180 | Web=splitCsvLine() separate impl
Amount parsing: Android=parseAmountMinor() integer | Web=parseStatementAmount() FLOAT
Header detection: different fallback logic
Dedup: Android=Levenshtein+SHA-256 | Web=date+amount+type key only (no RRN)
Allocation: Android=BudgetAllocator 50/30/20 only | Web=Dashboard inline math | Six-jar nowhere
Loan/EMI: Android=LoanEngine BigDecimal | Web=NONE
Goal engine: Android=GoalEngine ceil | Web=PlansPage inline (different rounding)
Date parsing: different supported format sets

---

## 4. Bugs Found (19 total)

BUG-01: StatementParser.kt line 58 - contains("date") unbounded substring match.
  Spec 6.2: whole-word match against explicit list [tran date, transaction date, etc.]

BUG-02: StatementParser.kt lines 67-68 - contains("debit")/contains("credit") substring.
  contains("dr") matches "Address", "Sundry Dr". Spec 14.7 lists this as defect to avoid.

BUG-03: StatementParser.kt - no indicator-column handling for single-amount files.
  W/D or Dr/Cr suffix column not handled. All unsigned-amount files default to EXPENSE.

BUG-04: StatementParser.kt fallback lines 78-80 - coerceAtLeast(0/1/2) silently picks wrong column.
  Spec 6.2: show column mapper when detection is low-confidence.

BUG-05: statementParser.ts lines 53-55 - parseFloat+Math.round in money parse path. (=CRITICAL-1)

BUG-06: types/index.ts lines 238-240 - parseMoney uses float path. (=CRITICAL-2)

BUG-07: Planning.kt BudgetAllocator - only 50/30/20, no six-jar/three-box/CUSTOM.
  Missing: largest-remainder, HALF_UP, creep lock, emergency fill, sink redirect,
  surplus/deficit correction, invariant assertions, flags, actual daysInCycle.

BUG-08: LoanEngine.payoff() - single-loan, does not match Appendix B multi-loan algorithm.
  Appendix B: multiple loans, constant budget, automatic roll-forward of cleared minimums.

BUG-09: LoanEngine.emi() - last-payment rounding not implemented. Spec 5.10 requires it.

BUG-10: Money.kt parse() - BigDecimal(text.trim()) throws on Indian-formatted strings (commas, rupee symbol).
  Spec 2.1: strip rupee/INR/commas/spaces, split at dot, pad fraction to 2 digits.

BUG-11: RecurrenceEngine.kt - no CYCLE_RELATIVE frequency. Spec 2.4: items anchored to cycle start + N days.

BUG-12: DuplicateDetector.kt - RRN exact-match not wired as primary dedup key.
  Spec 4.6: unique (user_id, rrn) is primary. Current: Levenshtein similarity on reference text.

BUG-13: statementParser.ts cleanNarration() - UPI slash-format positional parsing wrong.
  parts[2] may be mode token (P2M/P2A), not RRN. Spec 6.3 rule 1 requires mode-aware parsing.

BUG-14: statementParser.ts - no hyphen-format UPI decoder. Spec 6.3 rule 2 absent.

BUG-15: statementParser.ts suggestCategory() - text.includes() substring matching.
  text.includes('pg') matches "Singapore". Spec 6.4: word boundaries only.

BUG-16 (formerly BUG-16): user_monthly_plans SQL - p_month||'-31' is wrong for 30-day months.
  Should be: (p_month||'-01')::date + interval '1 month' - interval '1 day'

BUG-17: LedgerIntegrityValidator.kt - missing spec 10.2 invariants:
  jars sum to cycle salary; no duplicate (user,rrn); batch totals vs running balance.

BUG-18: verify.yml CI - no float-math grep step. Spec 11.3 requires it.

BUG-19: AccountEntity + user_transactions - missing spec 4.2 and 4.4 fields:
  accountRole, jarBucket, spendEmotionTag, paymentRail, rrn, counterpartyName, vpa,
  importBatchId, userOverridden, isRecurringMandate. (Phase 3 change, logged here.)

---

## 5. Spec Paths That Do Not Exist (30+ items)

shared-test-vectors/ directory
domain/planning/ - Allocation, HealthRules, SipProjection, DebtPayoff, EmiSimulator, Cycle, Baseline, Forensics, Goals, Insurance
domain/imports/ - NarrationDecoder, CsvReader, ColumnMapper, Classifier
wealthos-web/src/lib/ - money, allocation, healthRules, sipProjection, debtPayoff, emiSimulator, cycle, baseline, forensics, csvReader, narrationDecoder, classifier, duplicateDetector
wealthos-web/src/components/onboarding/Onboarding.tsx
Room DB v5 migration
supabase/migrations/20261024000000_wealthos_v2.sql
user_financial_profiles Supabase table
salary_cycles Supabase table
Survival mode logic (HomeViewModel)
Payday cycle detection and wizard (HomeViewModel)
Insurance shield implementation
Sinking funds implementation
Mandate radar
ai-analyze market_pulse mode with googleSearch grounding

---

## 6. Spec vs Repo Disagreements (10 items)

1. Spec says no web import page. Reality: ImportStatementModal.tsx (21KB) exists. Keep it.
2. Spec says no Android onboarding. Reality: OnboardingScreen.kt + ViewModel exist.
3. Spec says no prior v2 migrations. Reality: 20261023000000 and 20261023000001 exist.
   New migration must timestamp after 20261023000001.
4. Spec 2.1 says bigint. Reality: entire web uses number. Within safe range but parse is float.
5. Spec 5.8 SIP. No SipProjection anywhere. Appendix B is the only reference.
6. Gemini model name: read from GEMINI_MODEL env (correct). Must VERIFY before Phase 5.
7. Spec 4.2 accountRole. AccountEntity has type not role. Phase 3 migration needed.
8. PlansForms.kt has EMI/SIP logic in UI layer. Spec requires it in domain. Phase 1/4 split.
9. user_monthly_plans vs salary_cycles: different tables for different purposes. Do not replace, add.
10. BUG-16 SQL fix: needs next migration, not edit of existing.

---

## 7. Must Not Change

DatabaseKeyProvider.kt, backupCrypto.ts (encryption intact)
LedgerIntegrityValidator.kt (extend only)
ImportSecurity.kt CsvFormat.parse() (correct RFC4180 reference impl)
ImportSecurity.kt FileSniffer, ZipGuard
Money.kt arithmetic operators (addExact/subtractExact)
LoanEngine.emi() (BigDecimal/DECIMAL128)
RecurrenceEngine.kt (extend only - add CYCLE_RELATIVE)
DuplicateDetector.kt rowFingerprint() (wire it in, don't replace)
supabase/_shared/gemini.ts, guard.ts, validate.ts
All existing Supabase migrations
verify.yml structure (add float-grep step only)
MoneyFormat.kt

---

## 8. Phase 1 Scope (pending confirmation)

NEW Kotlin files (7):
  domain/planning/ Allocation.kt, HealthRules.kt, SipProjection.kt, DebtPayoff.kt,
                   EmiSimulator.kt, Cycle.kt, Baseline.kt

NEW TypeScript files (6):
  wealthos-web/src/lib/ money.ts, allocation.ts, healthRules.ts, sipProjection.ts,
                         debtPayoff.ts, emiSimulator.ts

NEW shared-test-vectors (5 files):
  allocation.json, sip.json, debt.json, emi.json (Appendix A golden values)

MODIFY (5 files):
  domain/model/Money.kt (add parseIndian(), roundHalfUp())
  domain/planning/Planning.kt (mark BudgetAllocator deprecated)
  wealthos-web/src/types/index.ts (parseMoney integer path)
  wealthos-web/src/lib/statementParser.ts (parseStatementAmount integer path)
  .github/workflows/verify.yml (add float-grep step)

NEW TESTS (10 files):
  Kotlin: AllocationTest, HealthRulesTest, SipProjectionTest, DebtPayoffTest, EmiSimulatorTest
  TypeScript: allocation.test.js, sip.test.js, debt.test.js, emi.test.js, money.test.js

EXIT: All Appendix A golden values match exactly on both platforms. CI float-grep passes.

---

## 9. Summary

Missing domain files: 14
Missing web lib files: 13
Missing supabase artefacts: 2
Missing shared-test-vectors: 1 directory
Missing web components: 1
CRITICAL float-math bugs (data path): 2  [web: CRITICAL-1, CRITICAL-2]
MEDIUM float-math bugs (Android UI input path): 1  [MEDIUM-3: PlansForms + PlansScreen]
Low float usages (display/canvas only): ~15 across Android + Web — no fix required
Other bugs (BUG-07 to BUG-19): 13
Spec vs repo disagreements: 10
Room DB version: v4 (v5 needed in Phase 3)
CI missing step: 1
Total actionable bugs: 16 (2 critical + 1 medium + 13 logic)
Re-verified: 2026-10-25 — no new files changed since 2026-10-24 audit
