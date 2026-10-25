# WealthOS — Final End-to-End Specification (Android + Web + Supabase)

Version 3 (final). Supersedes every earlier draft, chat answer and prompt. It merges: the October crisis diagnostic, the repo audit and UI plan, the code drafts, the universal multi-bank design, and the 16 reference videos. All money math in this file was **run and verified** with a reference implementation (Appendix B, golden values in Appendix A).

---

## Paste this to the coding agent first

```
Read WEALTHOS_FINAL_SPEC.md completely before touching code.
1. Do Phase 0 only: audit the real repo, write AUDIT.md (actual file tree, every float-math use,
   every place Android and Web duplicate logic, every bug found, every spec path that does not exist).
2. Stop and report. Do not start Phase 1 until I confirm.
3. From then on: one phase at a time, tests first, report results at the end of each phase.
Rules: smallest correct change; extend existing files; never hardcode a user, bank, salary,
baseline or payday; no floating point in any money path; AI never does arithmetic.
Where this spec and the real repo disagree, tell me instead of guessing.
```

---

## 0. How the agent must work

1. **Audit first.** Paths in this spec came from a zip listing and were never opened. Verify each against the repo.
2. **Smallest correct change.** Extend existing files. Keep the existing encryption (`DatabaseKeyProvider`, `backupCrypto.ts`), auth, and sync behaviour intact.
3. **Tests before UI.** Domain logic is written test-first in `domain/` (Kotlin) and mirrored in `wealthos-web/src/lib/` (TypeScript). Both must reproduce the golden values in Appendix A exactly.
4. **Never hardcode a user.** No fixed bank, salary, baseline, payday, city or currency.
5. **Report at the end of each phase**: what changed, which tests ran and their results, what could not be verified.
6. **Anything marked "VERIFY" in this spec** is a claim I could not check (bank formats, API shapes, third-party limits). Check it against real data or current documentation before relying on it.

---

## 1. Product

### 1.1 Goal
A private, offline-first personal finance OS for Indian users with **variable salary dates and several UPI apps on one bank account**. Its first job is to answer *"How much can I safely spend today?"*. It applies the habits taught in the user's 16 reference videos using deterministic math. AI is only used for categorisation suggestions and coaching text.

### 1.2 The problem that started this
A salary of ₹32,200 credited on 5 Oct fell to ₹800 by 22 Oct (₹31,400 gone in 17 days, an average of ₹1,847.05 per day) across GPay, BHIM and Amazon Pay on a single Axis account. Each UPI app only shows its own transactions, but **every UPI debit lands in the bank ledger**, so the bank statement is the single source of truth. The app must make the leak visible and prevent a repeat.

### 1.3 Users and constraints
- Salaried, often with a variable amount (example range ₹32,200 to ₹45,000) credited between the 1st and 5th. Also supports a fixed date or irregular freelance income.
- Multiple UPI apps. Multiple banks over time. Likely mid-range Android phone. Intermittent connectivity.
- Languages: English first, Tamil optional for coaching text and key labels.

### 1.4 Non-goals for v1
No bank-login scraping or Account Aggregator integration (future). No trade execution. No tax filing. No shared household accounts. No iOS. PDF statement parsing is a later phase; CSV/TSV/XLS-as-CSV/pasted text only at first.

---

## 2. Engineering rules (non-negotiable)

### 2.1 Money
- All money is **integer paise**: `Long` in Kotlin, `bigint` (or integer `number` within safe range, enforced by a guard) in TypeScript.
- **No `Double`/float** in any allocation, split, percentage, EMI, amortisation or parsing path. Parse amount strings with integer logic: strip `₹`, `INR`, commas and spaces; split at `.`; pad or truncate the fraction to two digits; build `rupees*100 + paise`. Never `toDouble()*100` or `parseFloat*100`.
- Percentages: `total * bps / 10000` using integer division, reconciled with **largest-remainder** so parts sum exactly to the total (ties broken by jar order).
- Rounding rule everywhere: **HALF_UP on non-negative values**, implemented as `(2*num + den) / (2*den)` with integer division.
- Projections (SIP, amortisation) round to paise **every month** using the same rule and the same month convention on both platforms (section 5.8).
- Kotlin and TypeScript must produce byte-identical results on all golden values.

### 2.2 Deterministic first, AI second
Net worth, runway, 7-7-7, jar splits, SIP milestones, debt payoff and EMI are computed in code. Gemini receives the computed JSON and returns text and category suggestions. It must never be asked to compute, restate or adjust a number. Market data from search is displayed with a timestamp and never enters any calculation.

### 2.3 Dates and time
- Statement dates are **calendar dates without time** (`LocalDate` / `YYYY-MM-DD` strings). Never convert them through UTC instants.
- Day boundaries use the user's timezone (default `Asia/Kolkata`).
- "Days in cycle" = days from cycle start to the next expected payday (section 5.4), not a fixed 30.

### 2.4 Payday-to-payday cycles
A budget cycle starts when the user logs (or the importer detects and the user confirms) a salary credit, and ends at the next one. All percentages recalculate from that cycle's actual salary. No "month start" logic may fire before salary arrives. Existing `RecurrenceEngine` / `RecurringWorker` must support items anchored to the **cycle start** (e.g. "SIP on cycle start + 2 days") as well as calendar dates.

### 2.5 Privacy and security
- The Gemini key lives only in Supabase Edge Function secrets. Never in the Android build, `local.properties` committed to git, or any `VITE_*` variable.
- Keep `guard.ts` (auth, rate limit) and `validate.ts` (schema validation) in front of every function. Extend, never bypass.
- Send AI the minimum. For categorisation: merchant strings, amount **bucket**, rail. For P2A (person) transfers send no personal name. For coaching: aggregates only. Never send account numbers, card numbers, VPAs, or full raw narrations.
- Statements are parsed on-device or in-browser. Raw files are not uploaded.
- Supabase tables use RLS `auth.uid() = user_id`. Keep client-side backup encryption.
- Logs never contain amounts, merchant names or narrations.
- Web: no secrets in the bundle; set a restrictive Content-Security-Policy on Cloudflare Pages; no inline script injection from imported data.

---

## 3. Repository map and target architecture

### 3.1 What exists (from the audited zip listing, to be re-verified in Phase 0)
```
domain/ (pure Kotlin)
  model/      Money.kt  Catalog.kt
  ledger/     Ledger.kt  LedgerIntegrityValidator.kt
  planning/   Planning.kt  PlanReport.kt  RecurrenceEngine.kt
  imports/    StatementParser.kt  DuplicateDetector.kt  ImportSecurity.kt
  test/       FinancialIntegrityTest.kt PlanningTest.kt PlanReportTest.kt StatementParserTest.kt
app/src/main/kotlin/app/privatemoney/
  data/local/ AppDatabase.kt (schemas v1-v4)  Entities.kt  DatabaseKeyProvider.kt
  cloud/      GeminiAIService.kt  SupabaseSyncService.kt  CloudSyncService.kt
  work/       DailyCheckInWorker.kt  RecurringWorker.kt  NightlyReminderScheduler.kt  NightlyReminderReceiver.kt
  ui/         PmosRoot.kt  MoneyFormat.kt  theme/Theme.kt  components/Components.kt
              home/{HomeScreen,HomeViewModel}.kt
              plans/{PlansScreen,PlansForms,PlansViewModel}.kt
              ledger/{LedgerScreen,LedgerViewModel,TransactionRowUi}.kt
              imports/{ImportScreen,ImportViewModel}.kt
              insights/{InsightsScreen,InsightsViewModel}.kt
              accounts/AccountsScreen.kt   notifications/CatchUpSheet.kt
wealthos-web/src/ (React + Vite + TS)
  App.tsx  index.css  types/index.ts  hooks/{useData,useAuth}.tsx  lib/{backupCrypto,supabase}.ts
  components/{dashboard/Dashboard, plans/PlansPage, ledger/LedgerPage, insights/InsightsPage,
              accounts/AccountsPage, settings, notifications, auth,
              shared/{NewTransactionModal,HoloCard3D,Tilt3DCard,Canvas3DBackground,FloatingNav,Sidebar}}.tsx
  test/financial.test.js
supabase/
  functions/ _shared/{gemini,guard,validate}.ts  ai-analyze  ai-categorize  health
  migrations/ 20261007000000_init.sql  20261014000000_web_sync.sql
.github/workflows/db-migrate.yml     (Cloudflare Pages hosts the web build)
```
There is **no** web import page, **no** onboarding screen, and **no** shared test vectors today.

### 3.2 Target additions
```
domain/planning/   Allocation.kt  HealthRules.kt  SipProjection.kt  DebtPayoff.kt  EmiSimulator.kt
                   Cycle.kt  Baseline.kt  Forensics.kt  Goals.kt  Insurance.kt
domain/imports/    NarrationDecoder.kt (rule table)  CsvReader.kt  ColumnMapper.kt  Classifier.kt
wealthos-web/src/lib/  money.ts allocation.ts healthRules.ts sipProjection.ts debtPayoff.ts emiSimulator.ts
                       cycle.ts baseline.ts forensics.ts csvReader.ts statementParser.ts
                       narrationDecoder.ts classifier.ts duplicateDetector.ts
wealthos-web/src/components/ledger/StatementImportModal.tsx   (+ ColumnMapper)
wealthos-web/src/components/onboarding/Onboarding.tsx
shared-test-vectors/   allocation.json sip.json debt.json emi.json parser/*.csv parser/*.expected.json
```
Both platforms load `shared-test-vectors/` in their test suites. This is what prevents Android/Web drift. Web must **not** keep a second copy of any formula: one `allocation.ts` serves Dashboard, Plans, the simulator and sync.

### 3.3 File-level change map
| Layer | File | Change |
|---|---|---|
| domain | `Money.kt` | integer-only parse/format, HALF_UP helper, Indian digit grouping |
| domain | `Planning.kt` / `PlanReport.kt` | allocation (5.3), health (5.6), SIP (5.8), debt (5.9), EMI (5.10), forensics (7) |
| domain | `RecurrenceEngine.kt` | cycle-anchored recurrence (2.4) |
| domain | `StatementParser.kt` | split into CsvReader, ColumnMapper, NarrationDecoder, Classifier (6) |
| domain | `DuplicateDetector.kt` | RRN + fallback hash (4.6) |
| domain | `ImportSecurity.kt` | size/encoding/CSV-injection checks (6.7) |
| domain | `LedgerIntegrityValidator.kt` | new invariants (section 10, item 2) |
| app | `Entities.kt`, `AppDatabase.kt` | v5 migration + exported schema (4) |
| app | `SupabaseSyncService.kt`, `CloudSyncService.kt` | sync new columns, cycles, profile |
| app | `HomeScreen`/`HomeViewModel` | meter, survival, payday wizard, vital signs (8.2) |
| app | `PlansScreen`/`PlansForms` | jars, gauges, simulators, goals (8.3) |
| app | `LedgerScreen`/`TransactionRowUi` | tags, badges, velocity, forensics (8.4) |
| app | `ImportScreen`/`ImportViewModel` | universal importer + mapper + preview (6, 8.5) |
| app | `AccountsScreen` | roles + transfer plan (8.6) |
| app | `InsightsScreen`, `GeminiAIService` | market pulse, coach, categorise (9) |
| app | `CatchUpSheet`, `DailyCheckInWorker`, `NightlyReminderScheduler` | 9:30 PM check-in, cooldowns (8.8) |
| app | new Onboarding screen | 8.1 |
| web | `Dashboard`, `PlansPage`, `LedgerPage`, `AccountsPage`, `InsightsPage`, `NewTransactionModal`, `useData` | parity with Android |
| web | new `StatementImportModal`, `Onboarding`, simulator tab | 8 |
| supabase | `_shared/gemini.ts`, `ai-analyze`, `ai-categorize`, `health` | 9 |
| supabase | new migration `20261024000000_wealthos_v2.sql` (any timestamp **after** `20261014000000`) | 4.7 |

---

## 4. Data model

### 4.1 Settings (Room `AppSettings` + Supabase `user_financial_profiles`)
| Field | Notes |
|---|---|
| `incomeScheduleType` | `VARIABLE_WINDOW` \| `FIXED_DATE` \| `IRREGULAR` |
| `paydayWindowStartDay`, `paydayWindowEndDay` | defaults 2 and 5, editable |
| `expectedSalaryMinPaise`, `expectedSalaryMaxPaise` | optional |
| `fixedBaselinePaise` | user-entered or auto-estimated (4.3). **No default constant.** |
| `baselineMethod` | `USER_ENTERED` \| `HISTORY_MEDIAN` \| `ESTIMATE_55_PERCENT` (always shown in the UI) |
| `lifestyleCreepLock` | boolean, default on |
| `needsBufferPaise` | headroom above baseline+EMI before surplus is redirected; default ₹1,500 (configurable) |
| `allocationFramework` | `SIX_JAR` \| `THREE_BOX` \| `FIFTY_THIRTY_TWENTY` \| `CUSTOM` |
| `customBps` | jar → basis points, must sum to 10000 |
| `playToLongTermShift` | boolean; moves 500 bps from Play to Long-Term (SIX_JAR only) |
| `upiNeedsAllowancePaise` | portion of Needs the user pays by UPI daily (food, transport); added to the daily meter as a separate line |
| `emergencyTargetMonths` | default 7 |
| `survivalThresholdPaise` | default `min(₹2,000, 10% of baseline)` |
| `smallScanThresholdPaise` | default ₹200, for "small UPI scan" aggregation |
| `egoCheckThresholdPaise` | default ₹2,000 |
| `cooldownHours` | default 48 |
| `currency`, `locale`, `timezone` | defaults INR, `en-IN`, `Asia/Kolkata` |
| `language` | `en` \| `ta` |

### 4.2 Accounts
`accountRole`: `INCOME_VAULT`, `DAILY_UPI_SPEND`, `EMERGENCY_RESERVE`, `WEALTH_PORTFOLIO`, `LIABILITY_LOAN`. Also `bankName`, `last4` (optional, never full number). Loans/cards carry `interestRatePercentBps`, `monthlyEmiPaise`, `outstandingPaise`, `isBnpl`.

### 4.3 Baseline estimation (replaces the old hardcoded ₹17,710)
Priority:
1. **User-entered** fixed expenses (rent/home contribution, food/mess, transport, mobile/internet/EB, medical/toiletries, other) summed. Appendix C has an example template.
2. Else the **median of the last 1 to 3 complete cycles** of transactions tagged `NEED` (needs at least one complete cycle).
3. Else **55% of the lowest recent salary**, labelled "estimate, replace with your real number".
Always display which method produced it. ₹17,710 is 55% of ₹32,200 and was only an example.

### 4.4 Transaction additions
`jarBucket`, `spendEmotionTag` (`NEED`, `PLANNED_WANT`, `REGRET_LEAK`, `UNPLANNED_TRANSFER`, `INCOME`, `UNCATEGORISED`), `paymentRail` (`UPI_P2M`, `UPI_P2A`, `UPI_AUTOPAY`, `IMPS_NEFT_RTGS`, `NON_UPI`), `rrn` (nullable), `counterpartyName`, `vpa` (nullable, stays on device), `importBatchId` (nullable), `userOverridden` (a user tag is never overwritten by re-import or AI), `isRecurringMandate`.

### 4.5 Cycle (`salary_cycles`)
`id, user_id, cycle_start_date, salary_paise, baseline_paise, emi_paise, framework, jar amounts (one column per jar of the framework, stored as JSON or normalised), days_in_cycle, daily_safe_upi_paise, net_worth_snapshot_paise, checklist_state (JSON), created_at`. Room v5 migration (4→5) with exported schema.

### 4.6 Dedup
Unique `(user_id, rrn)` where `rrn is not null`. Fallback hash of `(date, amountPaise, normalisedCounterparty, rail)` when no RRN. Manual entries matching an imported row (same date, amount, counterparty) are flagged **"possible duplicate"** for the user to merge, not silently dropped.

### 4.7 Supabase migration
One new idempotent migration with: transaction columns, unique RRN index, `salary_cycles`, `user_financial_profiles`, `goals`, `sinking_funds`, `merchant_rules`, `mandates`, all with RLS, `CHECK` constraints and **no hardcoded money defaults** (the earlier draft defaulted `fixed_baseline_paise` to 1771000; do not). Do not edit earlier migrations. Back up the Supabase database before the first push.

---

## 5. Domain logic

### 5.1 Frameworks (basis points, each sums to 10000)
| Framework | Jars |
|---|---|
| `SIX_JAR` (video 2) | NEEDS 5500, FREEDOM_SIP 1000, EDUCATION 1000, PLAY 1000, LONG_TERM 1000, GIVING 500 |
| `THREE_BOX` (videos 3, 15) | NEEDS 5000, EMERGENCY 1000, BUFFER 1000, MEDIUM_TERM 1000, LONG_TERM_SIP 2000 |
| `FIFTY_THIRTY_TWENTY` (video 13) | NEEDS 5000, WANTS 3000, SAVINGS 2000 |
| `CUSTOM` | user bps, validated to 10000 |

Per-framework roles used by the allocator:
| Framework | emergency jar | sink jar | reduce order when Needs overflows | savings jars |
|---|---|---|---|---|
| SIX_JAR | LONG_TERM | FREEDOM_SIP | PLAY, EDUCATION, GIVING | FREEDOM_SIP, LONG_TERM |
| THREE_BOX | EMERGENCY | LONG_TERM_SIP | MEDIUM_TERM, BUFFER | EMERGENCY, MEDIUM_TERM, LONG_TERM_SIP |
| 50/30/20 | SAVINGS | SAVINGS | WANTS | SAVINGS |
CUSTOM lets the user assign the roles. In the six-jar framework the sixth jar ("long-term savings and big purchases" in the video) doubles as the emergency-fund destination until the emergency target is met.

THREE_BOX has no wants jar. Its daily UPI allowance comes from `upiNeedsAllowancePaise` only; the UI should say so and offer SIX_JAR if the user wants a guilt-free budget.

Three-box notes from video 3 (informational text, not advice): the unplanned buffer covers repairs and functions; the medium-term box suits FD or bonds; bond laddering is mentioned for staggering maturities.

### 5.2 Why the old draft was replaced
The old allocator could break "sum equals salary", over-allocate in the deficit branch, and lost paise in `max(0, …)`. The version below is the only one to implement. It was run against 200,000 random inputs and always satisfied the invariants.

### 5.3 Allocation algorithm
Inputs: `S` (salary), framework, `baseline`, `emi`, `emergencyBalance`, `emergencyTarget` (default `baseline × emergencyTargetMonths`), `creepLock`, `buffer`, `playToLongTermShift`, `daysInCycle`.

```
mandatory = baseline + emi
if mandatory >= S:  NEEDS = S, every other jar 0, flag SHORTFALL, stop

1. If shift is on (SIX_JAR only): PLAY bps -= 500, LONG_TERM bps += 500
2. shares = largestRemainder(S, bps)             # sums to exactly S
3. needsShare = shares[NEEDS]
   if creepLock: needs = max(mandatory, min(needsShare, mandatory + buffer))
   else:         needs = max(mandatory, needsShare)
4. jars = shares; jars[NEEDS] = needs
5. emergencyFull = emergencyBalance >= emergencyTarget
   diff = needsShare - needs
   if diff > 0 (surplus):  add diff to sinkJar if emergencyFull else emergencyJar
   if diff < 0 (deficit):  take -diff from the reduce-order jars one by one (each down to 0);
                           if still short, take from the remaining non-Needs jars in reverse order
                           and flag SAVINGS_REDUCED
6. if emergencyFull and emergencyJar != sinkJar: move the whole emergencyJar amount to sinkJar
7. if sum(savingsJars) * 100 < 7 * S: flag BELOW_SAVINGS_FLOOR
8. daily  = spendJar / daysInCycle   (integer floor)  where spendJar = PLAY or WANTS
   weekly = spendJar * 7 / daysInCycle (integer floor)
   if spendJar == 0: flag SPEND_JAR_EMPTY
```
Invariants to assert in code and tests: every jar ≥ 0; `sum(jars) == S`; `NEEDS ≥ mandatory` (unless SHORTFALL).

The daily UPI meter shows two lines: **guilt-free** (`spendJar` based) and **needs allowance** (`upiNeedsAllowancePaise / daysInCycle`), plus their total. `daysInCycle` = days from cycle start to the next expected payday (start of the payday window of next month, or the user's next expected date); if unknown, fall back to 30 and say so.

Lifestyle-creep rule (videos 4 and 12): when salary rises, Needs stays at baseline + EMI + buffer and the extra goes to savings (emergency first, then the sink jar).

### 5.4 Cycles and paydays
- `VARIABLE_WINDOW`: during days `start..end` of the month, show the **Salary Credited** banner. A credit above a user threshold imported inside the window prompts "Is this your salary? Start a new cycle?". Never auto-start a cycle.
- `FIXED_DATE`: next payday is that date. `IRREGULAR`: the user starts cycles manually; `daysInCycle` uses a user-set planning horizon (default 30).
- Closing a cycle stores a net-worth snapshot and a summary (planned vs actual per jar).

### 5.5 Survival mode
Active when the spendable balance across `DAILY_UPI_SPEND` and `INCOME_VAULT` is below `survivalThresholdPaise`. Show `balance / daysUntilNextExpectedPayday` as the daily cap, the next payday date, and a checklist: pause UPI AutoPay mandates (list the detected mandates), stop QR scans for non-essentials, move nothing out of the vault. The ₹800 scenario triggers it.

### 5.6 Health rules (7-7-7 and vital signs)
All comparisons use integers.
- **EMI** (video 14, cap from video 11): green if `emi × 7 ≤ salary`; amber if `emi × 100 ≤ salary × 30`; else red. Show the ratio as a percentage to one decimal for display only.
- **Savings rate:** green `≥ 20%`, amber `≥ 7%`, else red.
- **Emergency runway:** `liquidEmergency / baseline` in months: green `≥ 7`, amber `≥ 3`, else red (target months configurable).
- **Emergency tiers** on the target: 20% instant-access cash, 50% auto-sweep FD/RD, 30% liquid fund or gold. Rounding: first two tiers floor, third takes the remainder.
- **Two-step fund (video 13):** step 1 mini fund = 1 month of baseline by a user date; step 2 = full target.
- **Vital signs:** `netWorth = assets − liabilities`; `cashFlow = inflow − outflow` (per cycle); `runwayMonths = liquid / baseline`; `freedomYears = investedAssets / (baseline × 12)`.
- **Term cover:** guidance `15 × annualIncome` (range 15 to 20 in video 11). Present as the video's rule of thumb, not individual advice.
- **EMI affordability extras:** 30 to 35% of income as the hard ceiling from video 11.

### 5.7 Insurance shield (video 1, video 11)
Checklist with a readiness score (count of passed items out of total, each item weighted equally unless the user changes weights):
sum insured ≥ ₹10L base, 0% co-pay, cashless network in the user's city, pre-existing-disease waiting period, pre and post hospitalisation days, unlimited restoration, consumables add-on, room-rent cap, super top-up present, term cover vs 15× income. Flag high risk if no health cover, co-pay > 0%, or no restoration. Premium estimates from the old chat (₹700 to ₹900 a month) were unverified; do not ship any price estimate.

### 5.8 SIP projection (video 7, video 5)
Convention (identical on both platforms): contribution at the start of each month; `balance = roundHalfUp((balance + sip) × (1200 + annualPct) / 1200)`; milestone check after growth; **step-up applied after every 12th month**: `sip = roundHalfUp(sip × (100 + stepUpPct) / 100)`. Cap at 600 months and return "not reached". Show two curves: expected (default 12%) and margin-of-safety (default 8%). Milestones ₹10L, ₹50L, ₹1Cr, ₹1.5Cr. Label: "illustrative, not guaranteed". Compute the years; never hardcode the video's "7-3-2" figures (they assume a different SIP).

### 5.9 Debt payoff (video 13)
Month loop: accrue interest on every open loan (`roundHalfUp(balance × ratePctBps / 120000)` where `ratePctBps = annual percent × 100`); pay each loan's minimum (capped at its balance); apply the rest of the **constant monthly budget** (`sum of minimums + extra`) to the target loan in strategy order; a cleared loan's minimum rolls forward automatically because the budget is constant.
- Snowball order: smallest current balance first (ties by name). Avalanche: highest rate first (ties by smaller balance, then name).
- Output months, total interest, and the differences between strategies.
- Guard: if the budget is ≤ the first month's total interest, return a clear error (non-terminating). Cap at 1200 months.
- BNPL and card balances can be added as loans. A rolled-over card shows its annual cost (monthly rate × 12, with a compounding view).

### 5.10 EMI affordability simulator (videos 15, 16, 11)
Inputs: price, down payment, annual rate, tenure months. EMI = `P r (1+r)^n / ((1+r)^n − 1)` with `r = annualPct/1200`, computed with exact decimals and rounded HALF_UP to paise; the last payment absorbs rounding. Output EMI, total repayment, total interest, new EMI-to-income ratio against the 1/7 and 30% limits, and a **job-loss survival check**: months of runway if income stops while EMI and baseline continue. Offer the 48-hour cooldown.

### 5.11 Budgeting modes (video 13)
- Zero-based: every rupee assigned; "unassigned: ₹X" must reach 0 before a cycle is marked ready.
- Digital envelopes: each jar is a capped envelope; overspend warns and proposes which envelope to borrow from (user confirms the move).
- Sinking funds: `annualCost / 12` per item, remainder paise on the last month. Templates: health premium, festivals, family functions (Moi), vehicle service, annual subscriptions.

### 5.12 Goals (SMART, video 13)
Goal: name, target, date, linked jar/account, progress. Required monthly = `ceil((target − current) / monthsLeft)`. Status on-track or behind. The Play→Long-Term shift can be suggested when a deadline is near (video 2).

### 5.13 Portfolio and wealth checks (videos 8, 9, 10, 11)
- Classify each holding as appreciating asset, depreciating asset or liability.
- 6-island diversification check: equity/MF, gold/SGB, PPF/post office, bonds, real estate, liquid.
- 60:40 equity:debt check with EPF/PPF counted as debt. Realistic return range 10 to 12% post-tax from video 11, shown as text, never fed to calculations unless the user sets a rate.
- Gold and index tracker: monthly gold grams or ETF units, and index SIP amount, as separate assets.
- Savings-rate guideline 10 to 30% (higher if unmarried) shown as informational text citing the user's videos.

### 5.14 Behavioural guardrails (videos 5, 6, 12, 15, 16)
- **7-day velocity:** share of the cycle's discretionary budget spent in the first 7 days; alert above 50%. Also report the share of salary left in 7 days.
- **48-hour cooldown** list for wishlist, flash-sale and EMI purchases.
- **Ego vs Utility** prompt before saving a `PLANNED_WANT` over the threshold.
- **Credit-card rollover** alert whenever a card balance is carried.
- **Lifestyle freeze** (video 4): user sets a freeze period (default 24 months); baseline rising past a user threshold triggers a gentle alert.
- **Enough number:** user target monthly expense and corpus with progress.
- **Fixed-expense ratio nudge** (video 7): baseline above 60 to 70% of income shows the "income − savings = expenses" view and links to the Education jar.
- **Wisdom cards:** short rotating cards from the videos (room for error and margin of safety, define enough, savings = income minus ego, rich versus wealthy, tail events). Static text, user can hide them.

---

## 6. Universal statement import

### 6.1 Pipeline
`file or pasted text → decode (BOM, encoding) → detect delimiter → find header row → map columns → parse rows → decode narration → classify → dedupe → preview → confirm → commit (atomic, undoable)`

### 6.2 Reading and header discovery
- Use a **real CSV reader** (quoted fields with commas, escaped quotes `""`, BOM, CRLF, TSV, semicolons). Never `split(",")` and never a quote toggle that ignores escapes.
- Scan the first ~40 rows for a row that has a date-like header **and** a narration-like header. Use **exact or whole-word match** after trim and lowercase. Never `contains("dr")`-style substring matching.
  - Date: `tran date`, `transaction date`, `value date`, `txn date`, `posting date`, `date`
  - Narration: `particulars`, `narration`, `description`, `remarks`, `transaction remarks`, `details`
  - Amount: split (`debit`/`credit`, `withdrawal amt.`/`deposit amt.`) or single (`amount` plus `dr/cr` or `type`)
  - Balance: `balance`, `closing balance`, `running balance`
- Dates: explicit candidate formats (`dd/MM/yyyy`, `dd-MM-yyyy`, `dd MMM yyyy`, `yyyy-MM-dd`, `dd/MM/yy`); an ambiguous parse shows a visible warning.
- Single-amount files: sign and indicator logic must treat a hyphen inside a date or narration as **not** a negative sign. Use the indicator column or the amount cell only. Ambiguous indicators (`W`, `D`) are resolved by file-level consistency checks against the running balance; otherwise ask the user.
- A zero running balance is a valid value; do not drop it.
- If detection is missing or low-confidence, show the **column mapper** (dropdowns for Date, Narration, Debit, Credit/Amount, Dr/Cr, Balance). Remember the mapping per header signature.
- Preamble rows, trailing summary rows and page-break repeats are skipped and counted.
- PDF: not in phase 1. Say "CSV or Excel exported as CSV" in the UI.

### 6.3 Narration decoding (data-driven rule table)
Each rule = regex + extractor + rail + format name. Adding a bank format means adding a rule and a fixture. Required rules (all **VERIFY** against real redacted rows from the user's statements, Axis first):
1. Slash UPI: `UPI/<mode?>/<rrn 9-15 digits>/<payee>/<…>`; modes `P2M`, `P2A`, `P2P`, `DR`, `CR`, `MANDATE`, `AUTOPAY`; mode optional.
2. Hyphen UPI: `UPI-<payee>-<vpa>-<ifsc>-<rrn>-<…>`.
3. IMPS, NEFT, RTGS, ACH, NACH, ECS references.
4. Generic fallback: first 12-digit token as RRN, cleaned narration as counterparty.
An unknown format degrades to the fallback and never throws.

### 6.4 Classification
Precedence, first match wins: user override > saved merchant rule > investment keywords (→ FREEDOM_SIP) > education (→ EDUCATION) > loan/EMI (→ NEEDS) > autopay or subscription (→ PLAY, `isRecurringMandate`) > utilities, transport, medical, groceries (→ NEEDS) > food delivery and quick-commerce (→ PLAY) > P2A heuristics > default PLAY with `PLANNED_WANT`.
- Heuristics only **suggest**. **Never auto-tag `REGRET_LEAK`**; regret is the user's judgement (video 12). Suggest "habit candidate".
- Small P2A amounts become `UNCATEGORISED` for one-tap review, not "tea stall leak". Large P2A amounts are "possible rent or family support", suggested not asserted.
- Keyword lists live in `Catalog` as editable config, matched on **word boundaries** (fixes false hits such as `"VI "`, `"ACT "`, `"COIN"`, `"TEA"` inside other words).
- User corrections create a saved merchant rule offered on future imports.
- Unresolved merchants may go to `ai-categorize` (merchant string only) for a suggestion.

### 6.5 Credits
Classified as income, refund, or transfer-in. See 5.4 for salary detection.

### 6.6 Preview
"Detected: <format> · N rows · M duplicates skipped · K uncategorised". Show top merchants, **detected recurring mandates**, and a "small UPI scans" total grouped by merchant (for example "₹4,200 across 38 small scans") before commit. Commit is atomic per batch with undo by `importBatchId`.

### 6.7 Import security (`ImportSecurity`)
Max file size and row count; reject binary; strip control characters; neutralise CSV-injection cells (leading `=`, `+`, `-`, `@`) on any later export; never evaluate content; stream large files (10,000+ rows) without blocking the UI thread.

### 6.8 Mandate radar
Recurring debits with the same counterparty and amount on a regular interval, or narrations with `MANDATE`/`AUTOPAY`, are listed as mandates with next expected date. The UI gives generic steps to pause or cancel mandates in each UPI app ("open the app, find AutoPay or mandates in your profile or payments settings"). Do not hardcode app menu paths; they change.

---

## 7. Cycle forensics: "where did my money go?"
Directly answers the original crisis. Inputs: transactions from cycle start to today, opening balance (from the first statement row's running balance or user entry).
- **Reconciliation check:** `opening + credits − debits` must equal the latest running balance. If not, show the difference and the likely cause (missing rows, duplicates, mixed accounts).
- **Four crisis buckets** (mapped from jar and tag, user can remap): (1) Fixed bills, rent, EMI; (2) Food, tea, snacks; (3) Shopping and subscriptions; (4) Transfers to friends and family.
- **Metrics:** total debited, average burn per day (`floor(debit / daysElapsed)`), share of salary spent in the first 7 days, small-scan count and total, top 10 merchants, mandates found.
- Output also feeds the AI coach as aggregates only.
Golden check: ₹31,400 over 17 days → 184,705 paise per day (₹1,847.05).

---

## 8. Screens (Android and Web parity)

### 8.1 Onboarding questionnaire (new on both)
1. Income: type, payday window, expected range, currency.
2. Accounts: bank, role (vault vs daily UPI), UPI Lite use.
3. Baseline: enter by line item, or "estimate" (shows method). Appendix C template.
4. Liabilities: loans, cards, BNPL (balance, rate, EMI).
5. Safety: current emergency fund and where it is held; health and term cover status.
6. Investments: SIP amount, gold, EPF/PPF, others.
7. Goals: up to three SMART goals.
8. Framework choice with a live preview using their own salary.
Every step is skippable with a stated default. Persist after each step.

### 8.2 Home / Dashboard
- Hero (HoloCard3D on web): **Daily Safe UPI meter**: remaining today of allowance, weekly UPI Lite amount, guilt-free and needs-allowance lines. Amber at 80% of the day's allowance, crimson at 100%.
- Vault balance hidden behind an eye toggle by default.
- **Salary Credited banner** in the payday window launching the Payday Wizard.
- **Survival banner** (5.5).
- **Vital signs strip**: net worth, cash flow, runway, freedom years.
- 7-day velocity indicator.

### 8.3 Plans
- Tabs: Jars and framework switcher (liquid-fill jar cards: allocated, spent, remaining); 7-7-7 rings and emergency tiers; SIP chart (two curves); debt simulator; EMI simulator; insurance shield; sinking funds; goals; budgeting modes.
- **Web simulator tab:** salary slider ₹25,000 to ₹60,000, editable baseline, EMI, framework; calls the same `allocation.ts`; shows jars, daily meter, 7-7-7 and flags.

### 8.4 Ledger
Emotion tag chips (Need, Want, Habit/Regret) and UPI badge (merchant vs person); 7-day velocity bar; top-leak merchants; search and filter; importer entry; forensics card (section 7); month-end **joyless leak review** (user-tagged total, top merchants, a suggested cap the user can edit); 7-day micro-audit flow (tag each spend Need/Want/Habit); ego-vs-utility prompt on large wants.

### 8.5 Importer UI
Step 1 drop zone (or paste). Step 2 detected-format pill and column mapper fallback. Step 3 preview with mandate radar and small-scan aggregation. Step 4 confirm and undo toast.

### 8.6 Accounts
Role tagging with a warning on `INCOME_VAULT` ("don't link daily UPI apps here"). **Payday transfer plan**: exact amounts to move from vault to emergency, wealth, and daily-spend accounts. UPI Lite limits (per-transaction and balance caps) must be **configurable settings, not constants**. VERIFY current limits.

### 8.7 Insights
AI coach and market pulse (section 9), always with a timestamp and sources, offline-safe empty state.

### 8.8 Payday Wizard and nightly check-in
- **Payday Wizard (first 48 hours):** enter salary → show allocation → checklist with persisted state: move emergency and SIP money out of the UPI-linked account, load the weekly UPI Lite amount, pay fixed bills, audit subscriptions and mandates. Shows the net-worth snapshot and delta before the allocation (video 4).
- **Nightly check-in (9:30 PM, configurable):** today's UPI spend against the allowance, tag untagged items, cooldown timers. Use WorkManager (inexact scheduling); request `POST_NOTIFICATIONS` on Android 13+ with a graceful denied state; avoid exact-alarm permission.

### 8.9 Design system
Tokens shared by `Theme.kt` and `index.css`: background `#070B14`; glass card `rgba(17,24,39,.78)` with 1px `rgba(255,255,255,.08)` border and blur; emerald `#10B981` (growth, SIP, net worth); gold `#F59E0B` (caution, sinking, gold assets); crimson `#EF4444` (leaks, survival, over-limit); cyan `#06B6D4` (play and UPI meter). Fonts: Plus Jakarta Sans for text, JetBrains Mono with tabular numerals for every currency figure. Indian digit grouping everywhere (₹1,23,970). Never convey status by colour alone (add icon or text). Respect reduced-motion; disable the 3D canvas and liquid animations on low-end devices. Contrast ≥ 4.5:1. Touch targets ≥ 48dp. Screen-reader labels on meters and rings.

### 8.10 Copy and tone
Plain, non-judgmental. Say "habit candidate", not "waste". Every projection says "illustrative". Every insurance, investment or market card carries "educational, not financial advice".

---

## 9. Supabase and AI

### 9.1 `ai-categorize`
Batch of `{merchantString, amountBucket, rail}` → structured JSON through `responseSchema`: `category`, `jarBucket`, `suggestedTag`, `isSubscriptionMandate`, `cleanMerchantName`, `confidence`. Validate every field against the enum sets; discard invalid items. Suggestions only.

### 9.2 `ai-analyze`
- Input (validated, size-capped): `salaryRupees`, `fixedBaselineRupees`, `currentLiquidBalanceRupees`, `dailySafeUpiRupees`, `sevenDayBurnPercent`, `regretLeakTotalRupees` (user-tagged only), `topLeakMerchants[{name,totalRupees,count}]` (merchants only, no persons), `sevenSevenSevenStatus`, `emergencyRunwayMonths`, `equityToDebtRatio`.
- Modes: `coach` (no search) and `market_pulse` (search grounding on). Enable grounding (`tools: [{googleSearch: {}}]`) only for `market_pulse`. Ask for Nifty 50 and Sensex level and trend, 22K and 24K gold per gram in the user's city, RBI repo rate, inflation context. Return grounding source URLs and show them.
- Coach output sections: (1) cash-flow and leak diagnosis and how to reach the next payday; (2) payday first-48-hours transfer plan using the supplied numbers; (3) market and allocation context as **general frameworks** from the user's references (for example index SIP for long horizons, liquid or gold for short horizons, the 60:40 idea); (4) 7-7-7 and emergency progress.
- System prompt rules: never recompute provided numbers; no named-security or "buy X" advice; always include "educational, not financial advice"; simple English with an optional Tamil summary; say when data is stale.
- Rate limit per user, timeouts, graceful degradation. The app must be fully usable with AI off.
- **VERIFY** the current Gemini model name and the grounding request shape against live documentation before coding (`gemini-2.5-flash` was used in drafts).

### 9.3 `health`
Extend to report schema version and function versions. No user data.

---

## 10. Non-functional requirements

1. **Sync:** last-write-wins per record with `updated_at`, except `userOverridden` tags, which always win. Conflicts on cycles prefer the newer `updated_at` and log a non-PII note.
2. **Integrity:** extend `LedgerIntegrityValidator` to assert: jars sum to cycle salary; every amount integer paise; no duplicate `(user, rrn)`; imported-batch totals reconcile with the statement running balance when present. Run it after import, sync and restore. Web `useData` must use the shared integer engine so synced values match Android to the paisa.
3. **Offline:** all core features work offline. Imports and allocation need no network.
4. **Performance:** import 10,000 rows in under 3 seconds on a mid-range phone without blocking the UI; dashboard first paint under 1.5 seconds from local data.
5. **Accessibility and i18n:** all strings externalised; English and Tamil; RTL not required; Indian number formatting via one helper.
6. **Errors and empty states:** every screen has loading, empty, error and offline states. Parse failures list the first failing rows with reasons.
7. **Backup and delete:** encrypted export and restore; "delete my account and data" removes local and cloud data.
8. **Permissions:** notification permission requested with context; SMS access only behind a feature flag (10.1).
9. **Release/ops:** CI runs Kotlin tests, web tests and build, the float-math grep check, and the migration job only on main. Cloudflare Pages deploy kept; confirm no secret in the bundle.

### 10.1 Optional later: SMS or notification parsing (Android, feature-flagged)
Detect bank debit alerts, parse locally, never upload. Play Store policy on `READ_SMS` is strict; prefer a notification listener with a clear privacy explanation. Off by default.

---

## 11. Tests and definition of done

### 11.1 Golden values
Appendix A. Both platforms must match exactly. Load them from `shared-test-vectors/`.

### 11.2 Required test sets
- **Allocation:** every row of A.1 and A.2; the invariants property test over random inputs (millions of cases is cheap; at least 100,000).
- **Parser:** redacted real exports (Axis, HDFC, SBI, ICICI, Kotak as the user supplies) covering split and single amount columns, quoted commas, BOM, preamble and summary rows, each narration style, IMPS/NEFT, garbage rows, and a re-import of overlapping statements with zero duplicates.
- **Health:** exact boundaries (EMI exactly 1/7 and exactly 30%; savings exactly 7% and 20%; runway exactly 3 and 7 months).
- **SIP, debt, EMI:** A.3 to A.5. **Money:** parse and format round trips; Indian grouping; HALF_UP edge values.
- **Forensics:** A.6.
- **Sync parity:** Android and Web compute the same allocation for the same inputs after a Supabase round trip.

### 11.3 Definition of done
- `./gradlew :domain:test :app:testDebugUnitTest` green; `npm test && npm run build` in `wealthos-web` green.
- Migration applies cleanly on a fresh and on the existing database; RLS verified with a second user.
- No `Double`/float in any money path (CI grep check for `toDouble`, `Double`, `parseFloat`, `Number(` on money).
- Manual run: import a real statement → preview → commit → tag → forensics → payday wizard → meter updates; works offline; AI off works.

---

## 12. Phases

| Phase | Scope | Exit |
|---|---|---|
| 0 | Audit, `AUDIT.md`, shared-vectors scaffold, CI float check | Report, wait for confirmation |
| 1 | Money, allocation, cycles, baseline, health rules, SIP, debt, EMI (Kotlin and TS) | Appendix A passes on both |
| 2 | CSV reader, column mapper, narration rules, classifier, dedupe, preview logic | Parser vectors pass |
| 3 | Room v5, Supabase migration, sync of new fields, integrity validator | Migration and sync verified |
| 4 | Onboarding, Home meter, Payday Wizard, Ledger tags and forensics, Plans, Accounts (Android first, then Web) | Manual run-through |
| 5 | Edge functions (categorise, analyze with grounding), Insights UI | AI on and off both work |
| 6 | Nightly check-in, cooldowns, velocity alerts, micro-audit, goals; optional SMS or notification parser behind a flag | Behind flag |

---

## 13. Coverage matrix (every video → where it lives)
| # | Video | Spec sections |
|---|---|---|
| 1 | Health insurance | 5.7 |
| 2 | Six-jar money system | 5.1, 5.3, 5.12 |
| 3 | Salary day first 48 hours | 5.1, 5.3, 5.4, 8.8 |
| 4 | 7 habits of quiet wealth | 5.6, 5.14, 8.8 |
| 5, 6 | Psychology of money | 5.8, 5.14 |
| 7 | 7-3-2 compounding and two accounts | 5.8, 5.14, 8.6 |
| 8 | Assets vs liabilities | 5.13 |
| 9, 10 | Gold, index, credit-card trap | 5.9, 5.13 |
| 11 | Goal-based allocation and insurance trio | 5.6, 5.7, 5.13 |
| 12 | Brain tricks to stop leaks | 5.11, 5.14, 8.4 |
| 13 | Financial literacy stack | 5.6, 5.9, 5.11, 5.12 |
| 14 | 7-7-7 rule | 5.6 |
| 15, 16 | Three boxes, peace of mind | 5.1, 5.10, 5.14, 8.4 |
| Crisis | October diagnostic | 1.2, 5.5, 6.8, 7 |

---

## 14. Defects in earlier drafts (do not reproduce)
1. Hardcoded `₹17,710` baseline, including as a SQL column default and a function default.
2. Axis-only parsing; `Tran Date, Chq No, Particulars…` column positions hardcoded.
3. `parseRupeesToPaise` via `toDouble() * 100`; web `parseFloat(...) * 100`.
4. Web CSV parsing with `line.split(',')`; Kotlin quote toggle that ignores `""`.
5. Kotlin typo `enum classRuleStatus`; `RuleStatus` undefined; `adjustedEmergency = max(0, …)` breaking the sum invariant; deficit branch over-allocating.
6. The ₹45,000 table summed to ₹44,500, and its "cap" grew with salary (18,500 / 19,000 / 19,500) with no rule behind it.
7. Substring header matching (`contains`) and unbounded keyword matching.
8. Hyphen in a date or narration treated as a negative sign in single-amount files; zero balance dropped by `takeIf { it > 0 }`; `W`/`D` indicators guessed.
9. Auto-tagging food, mandates and small transfers as `REGRET_LEAK`, and large transfers as rent.
10. `daily = play / 30`, `weekly = play / 4`, ignoring actual cycle length.
11. Fixed-calendar recurrence firing before salary arrives.
12. EMI ratio compared as a rounded double (`14.28`) instead of an integer test.
13. AI prompt told the model what to buy; market data mixed with user numbers.
14. Unverified facts presented as certain: Axis WhatsApp number, PDF password rule, statement column order, UPI Lite caps, FD sweep rates, health premium estimates.
15. `weeklyUpiLite` figure of "₹2,000 per week" hardcoded in chat advice; must be a setting.

---

## 15. Open questions (ask once, then proceed with defaults)
1. Real monthly fixed expenses (default: estimate at 55% of lowest recent salary, labelled).
2. Loans, card dues or BNPL (default: none).
3. Banks beyond Axis to support first (default: Axis, then HDFC, SBI, ICICI, Kotak from fixtures you provide).
4. Play→Long-Term shift on by default (default: off).
5. SMS or notification parsing now or later (default: later, off).
6. Framework default for new users (default: SIX_JAR).
7. Tamil UI strings now or later (default: coaching text only).

---

# Appendix A — Golden values (generated by running Appendix B)

Notation: amounts in rupees. Baseline ₹17,710, buffer ₹1,500, creep lock on, emergency balance 0, target = 7 × baseline, days in cycle 30, SIX_JAR, no EMI unless stated.

## A.1 Six-jar allocation across salaries
| Salary | Needs | Freedom SIP | Education | Play | Long-Term | Giving | Daily (floor) | Weekly (floor) | Flags |
|---|---|---|---|---|---|---|---|---|---|
| 25,000 | 17,710 | 2,500 | 1,040 | 0 | 2,500 | 1,250 | 0.00 | 0.00 | SPEND_JAR_EMPTY |
| 32,200 | 17,710 | 3,220 | 3,220 | 3,220 | 3,220 | 1,610 | 107.33 | 751.33 | none |
| 35,000 | 19,210 | 3,500 | 3,500 | 3,500 | 3,540 | 1,750 | 116.66 | 816.66 | none |
| 40,000 | 19,210 | 4,000 | 4,000 | 4,000 | 6,790 | 2,000 | 133.33 | 933.33 | none |
| 45,000 | 19,210 | 4,500 | 4,500 | 4,500 | 10,040 | 2,250 | 150.00 | 1,050.00 | none |
| 60,000 | 19,210 | 6,000 | 6,000 | 6,000 | 19,790 | 3,000 | 200.00 | 1,400.00 | none |
Every row sums to its salary exactly. At ₹25,000 the baseline leaves no Play money, which the app must say plainly.

## A.2 Edge cases
| Case | Result (rupees) | Flags |
|---|---|---|
| 32,200 with EMI 5,000 | Needs 22,710; Freedom 3,220; Edu 1,440; Play 0; Long-Term 3,220; Giving 1,610 | SPEND_JAR_EMPTY |
| 45,000, emergency fund complete | Needs 19,210; Freedom 14,540; Edu 4,500; Play 4,500; Long-Term 0; Giving 2,250; daily 150.00 | none |
| 32,200, baseline 20,000 + EMI 5,000 (deficit) | Needs 25,000; Freedom 3,220; Edu 0; Play 0; Long-Term 3,220; Giving 760 | SPEND_JAR_EMPTY |
| 32,200, baseline 30,000 + EMI 3,000 | Needs 32,200; all others 0 | SHORTFALL |
| 32,200 with Play→Long-Term shift | Needs 17,710; Freedom 3,220; Edu 3,220; Play 1,610; Long-Term 4,830; Giving 1,610; daily 53.66 | none |
| 45,000, creep lock off | Needs 24,750; Freedom 4,500; Edu 4,500; Play 4,500; Long-Term 4,500; Giving 2,250 | none |
| THREE_BOX, 40,000 | Needs 19,210; Emergency 4,790; Buffer 4,000; Medium-term 4,000; Long-term SIP 8,000 | none |
| 50/30/20, 32,200 | Needs 17,710; Wants 8,050; Savings 6,440; daily 268.33 | none |
Property test: 200,000 random cases across all three frameworks (random salary, baseline up to 130% of salary, EMI, emergency state, buffer, shift) satisfied every invariant.

## A.3 SIP milestones
Start SIP ₹10,000 per month, 10% annual step-up, convention in 5.8.
| Return | ₹10L | ₹50L | ₹1Cr | ₹1.5Cr |
|---|---|---|---|---|
| 12% (expected) | month 61 | month 144 | month 190 | month 219 |
| 8% (margin of safety) | month 65 | month 162 | month 216 | month 251 |

## A.4 Debt payoff
Loans: A card ₹60,000 at 36% (min ₹3,000); B personal ₹20,000 at 18% (min ₹1,500); C vehicle ₹1,20,000 at 14% (min ₹4,000); extra ₹2,000 per month.
| Strategy | Months | Total interest |
|---|---|---|
| Snowball (B first) | 24 | ₹41,656.14 |
| Avalanche (A first) | 23 | ₹39,706.46 |
Avalanche saves ₹1,949.68 and 1 month.

## A.5 EMI
₹5,00,000 at 12% for 36 months: EMI ₹16,607.15; total interest ₹97,857.63 (last payment absorbs rounding).

## A.6 Forensics and small checks
- ₹31,400 over 17 days → 184,705 paise per day.
- ₹1,23,970 formats as `₹1,23,970`; emergency target 7 × ₹17,710 = ₹1,23,970 (tiers: ₹24,794 / ₹61,985 / ₹37,191).
- EMI test: ₹4,600 on ₹32,200 passes the 1/7 rule (`4,600×7 = 32,200 ≤ 32,200`); ₹4,601 does not.

---

# Appendix B — Reference implementation (port exactly)

```python
def hu(num, den):                      # round half up, num >= 0, den > 0
    return (2 * num + den) // (2 * den)

def largest_remainder(total, bps, order):
    base = {j: total * bps[j] // 10000 for j in order}
    rem  = {j: total * bps[j] %  10000 for j in order}
    left = total - sum(base.values())
    for j in sorted(order, key=lambda j: (-rem[j], order.index(j)))[:left]:
        base[j] += 1
    return base

def allocate(S, F, baseline, emi, emergency_balance, emergency_target,
             creep_lock, buffer, shift_play, days):
    order, bps = F["order"], dict(F["bps"])
    mandatory = baseline + emi
    if mandatory >= S:
        jars = {j: 0 for j in order}; jars["NEEDS"] = S
        return jars, ["SHORTFALL"], 0, 0
    if shift_play: bps["PLAY"] -= 500; bps["LONG_TERM"] += 500
    shares = largest_remainder(S, bps, order)
    needs_share = shares["NEEDS"]
    needs = (max(mandatory, min(needs_share, mandatory + buffer)) if creep_lock
             else max(mandatory, needs_share))
    jars = dict(shares); jars["NEEDS"] = needs
    flags = []
    full = emergency_balance >= emergency_target
    diff = needs_share - needs
    if diff > 0:
        jars[F["sink"] if full else F["emergency"]] += diff
    elif diff < 0:
        deficit = -diff
        for j in F["reduce"]:
            t = min(jars[j], deficit); jars[j] -= t; deficit -= t
        if deficit > 0:
            flags.append("SAVINGS_REDUCED")
            for j in reversed([x for x in order if x != "NEEDS"]):
                t = min(jars[j], deficit); jars[j] -= t; deficit -= t
    if full and F["emergency"] != F["sink"]:
        jars[F["sink"]] += jars[F["emergency"]]; jars[F["emergency"]] = 0
    if sum(jars[j] for j in F["savings"]) * 100 < 7 * S: flags.append("BELOW_SAVINGS_FLOOR")
    daily = weekly = 0
    if F["spend"]:
        daily  = jars[F["spend"]] // days
        weekly = jars[F["spend"]] * 7 // days
        if jars[F["spend"]] == 0: flags.append("SPEND_JAR_EMPTY")
    assert all(v >= 0 for v in jars.values()) and sum(jars.values()) == S
    return jars, flags, daily, weekly

def sip_months(sip, step_pct, annual_pct, target, cap=600):
    bal = 0
    for m in range(1, cap + 1):
        bal = hu((bal + sip) * (1200 + annual_pct), 1200)
        if bal >= target: return m
        if m % 12 == 0: sip = hu(sip * (100 + step_pct), 100)
    return None

def interest(bal, rate_pct_bps):       # rate_pct_bps = annual percent * 100
    return hu(bal * rate_pct_bps, 120000)

def payoff(loans, extra, strategy):    # loans: name, bal, rate (pct*100), min
    ls = [dict(l) for l in loans]
    budget = sum(l["min"] for l in ls) + extra
    months = total_int = 0
    while any(l["bal"] > 0 for l in ls):
        months += 1
        for l in ls:
            if l["bal"] > 0:
                i = interest(l["bal"], l["rate"]); l["bal"] += i; total_int += i
        avail = budget
        for l in [l for l in ls if l["bal"] > 0]:
            p = min(l["min"], l["bal"]); l["bal"] -= p; avail -= p
        act = [l for l in ls if l["bal"] > 0]
        act.sort(key=(lambda l: (l["bal"], l["name"])) if strategy == "snowball"
                 else (lambda l: (-l["rate"], l["bal"], l["name"])))
        for l in act:
            p = min(avail, l["bal"]); l["bal"] -= p; avail -= p
    return months, total_int
```
EMI: compute with exact decimal arithmetic, `emi = round_half_up(P·r·(1+r)^n / ((1+r)^n − 1))` with `r = annualPct/1200`; schedule interest each month `round_half_up(balance·r)`; final payment = remaining balance + interest. In TypeScript use `bigint` and exact integer arithmetic for the rate (for example scale by 10^12) or a decimal library with fixed rounding; do not use `Math.pow` on floats for the final value.

---

# Appendix C — Example baseline template (illustration only, not the user's facts)
Used in onboarding to show what to enter. These amounts happened to total ₹17,710 in the earlier draft:
| Line | Example |
|---|---|
| Rent, PG or home contribution | ₹7,500 |
| Meals, groceries, mess | ₹5,000 |
| Transport, petrol, commute | ₹2,500 |
| Mobile, internet, electricity | ₹1,500 |
| Medical, pharmacy, toiletries | ₹1,210 |
Show these as placeholders, not prefilled values.

---

# Appendix D — Glossary
Paise: 1/100 rupee. RRN/UTR: 12-digit UPI reference. P2M/P2A/P2P: UPI to merchant, account or person. Baseline: fixed monthly essentials. Cycle: salary to next salary. Creep lock: keeps Needs at baseline when income rises. Sink jar: where surplus savings go once the emergency target is met. Freedom years: invested assets divided by annual baseline. Margin of safety: a conservative return curve shown beside the expected one.
</USER_REQUEST>
<ADDITIONAL_METADATA>
The current local time is: 2026-10-25T12:16:48+05:30.

The user's current state is as follows:
Other open documents:
- c:\Projects\WealthOS\app\src\main\kotlin\app\privatemoney\cloud\SupabaseSyncService.kt (LANGUAGE_KOTLIN)
- c:\Projects\WealthOS\domain\src\main\kotlin\app\privatemoney\domain\ledger\LedgerIntegrityValidator.kt (LANGUAGE_KOTLIN)
- c:\Projects\WealthOS\domain\src\main\kotlin\app\privatemoney\domain\model\Money.kt (LANGUAGE_KOTLIN)
- c:\Projects\WealthOS\build.gradle.kts (LANGUAGE_UNSPECIFIED)
- c:\Projects\WealthOS\docs\SECURITY.md (LANGUAGE_MARKDOWN)
</ADDITIONAL_METADATA>
<USER_SETTINGS_CHANGE>
The user changed setting `Model Selection` from Gemini 3.8 Flash (High) to Claude Sonnet 4.6 (Thinking). No need to comment on this change if the user doesn't ask about it. If reporting what model you are, please use a human readable name instead of the exact string.
</USER_SETTINGS_CHANGE>