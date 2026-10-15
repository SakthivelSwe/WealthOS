# WealthOS — Repository-Grounded Audit and Improvement Prompt Pack
Repository reviewed: https://github.com/SakthivelSwe/WealthOS
Default branch: main
Review date: 2026-10-15

This is a native Android Kotlin + Jetpack Compose application (repo README), not the web deployment shown in screenshots. The repository includes :app and :domain modules, Room + SQLCipher local data, a journal-ledger design, account/transaction/goals/debt/chit/recurring models, Supabase cloud code, AI functionality, GitHub Actions, and security/architecture docs. Work from the checked-out repo, not the screenshots alone.

## Evidence-based findings to start with

1. `PROJECT_STATUS.md` says builds/domain tests had been verified locally as of 2026-10-07, but device/emulator tests, Supabase deployment/Edge Function execution, real Gemini calls, and GitHub workflows were not verified. It also lists encrypted backup/JSON export and integration/security test gaps. Treat status as a clue to verify against current main, not a substitute for running tests.
2. `app/src/main/kotlin/app/privatemoney/cloud/GeminiAIService.kt` directly constructs Google's `GenerativeModel` using `BuildConfig.GEMINI_API_KEY`; `.github/workflows/release.yml` passes `GEMINI_API_KEY` into the Android build. This conflicts with `docs/AI.md` and `docs/THREAT_MODEL.md`, which say the Gemini key must remain server-side in Supabase and must never be in the APK. Priority Critical: verify BuildConfig wiring and inspect shipped APK; remove any production client-side Gemini key and route calls through a consent-gated server Edge Function. If a real key was ever included in a public APK/repo, rotate it immediately and examine git history and releases.
3. `app/src/main/kotlin/app/privatemoney/ui/home/HomeViewModel.kt` computes Safe-to-Spend inputs from liquid balance, recurring bills, expected debt payments, planned goal contributions, negative credit-card balances, and a 5% income buffer. `PROJECT_STATUS.md` says commitments and buffer were previously zero until linkage was added. Verify correctness rather than relying on this statement: ensure recurring dates, already-posted bill payments, debt/card overlap, goals and account currencies are handled without double-counting.
4. `app/src/main/kotlin/app/privatemoney/data/local/AppDatabase.kt` uses journal-entry sums for account balance, with SQL formulas around sign convention; `LedgerRepository.record` posts transaction and journal legs atomically. Test all balance signs and whether soft deletion consistently removes every analytic effect. Editing appears limited to details; verify whether amount/type/account/date edits are supported or intentionally disallowed.
5. `app/src/main/kotlin/app/privatemoney/ui/insights/InsightsViewModel.kt` creates its analysis date ranges when the ViewModel is created and injects a `GeminiAIService` default. Verify midnight/month rollover refresh behavior and ensure AI cannot receive raw sensitive data without explicit, informed consent.
6. `app/src/main/kotlin/app/privatemoney/ui/plans/PlansViewModel.kt` uses an app-configured basis income and calls PlanReport with Needs/Wants/Savings lane totals. Validate the 50/30/20 methodology, treatment of unclassified expenses, investments and transfers, and the income basis.
7. `app/src/main/kotlin/app/privatemoney/data/local/Entities.kt` shows recurring transactions but a simple frequency/nextRunDate model. Verify WorkManager generation, idempotency, retries, timezone/month-end, and whether each generated transaction is safely linked to its rule.
8. `CloudSyncService.kt` contains encrypted backup envelope construction using PBKDF2-HMAC-SHA256/AES-GCM and is substantial. Do not assume restore is safe: test authentication, KDF parameters, envelope parsing/limits, schema versioning, checksum/authentication, import atomicity, conflict semantics, partial failure recovery, all entity coverage, and wrong-password/corrupt-file scenarios.
9. `docs/ARCHITECTURE.md`, `docs/DATABASE.md`, `docs/SECURITY.md`, and `docs/AI.md` express target architecture; compare each claim to actual shipped implementation. Treat documentation that says “planned” as not implemented.
10. Repo includes CI workflows and `scripts/verify-apk.sh`; verify current workflow results and whether the script detects real production secrets and ensures release APK is signed with a known certificate. Do not assume CI is green merely because workflows exist.

---

## MASTER PROMPT — Audit only, do not edit yet

Act as a staff Android engineer, Kotlin/Compose architect, personal-finance domain specialist, database/recovery engineer, security engineer, accessibility expert, and QA lead. Review the actual checked-out repository at https://github.com/SakthivelSwe/WealthOS.

IMPORTANT: This is a native Android application. Preserve Kotlin, Jetpack Compose, Room/SQLCipher, the domain module, local-first/offline behavior, and the current design language unless evidence justifies a change. Do not turn it into a web app. Do not rebuild everything from scratch. Do not change or delete user data. Do not deploy, merge, publish, or rotate secrets automatically.

Phase 1 is READ-ONLY. Inspect README, PROJECT_STATUS, docs, Gradle files, all modules, domain engines, database schema and migrations, repositories, ViewModels, screens, cloud/AI code, manifest, import/export, backup/restore, notifications/work scheduling, and workflows. Read actual implementation and test it where environment permits.

Produce:
A. Architecture map and actual feature inventory (implemented / partial / UI-only / planned / verified).
B. Repository file map for each feature.
C. Findings ranked Critical / High / Medium / Low with severity rationale, exact file + symbol/line if available, evidence, user impact, reproduction, fix, regression test, migration/security implications.
D. A money-flow trace for expense, income, transfer, delete, edit, recurring generation, statement import, backup/restore and cloud sync.
E. Financial calculation worksheet with explicit sign conventions and sample amounts.
F. CI and device-testing gap analysis.
G. Phased plan with smallest safe PRs and acceptance criteria.
H. List exactly which commands ran and results. Never report unrun tests as passing.

At minimum investigate the code-specific concerns at the top of this document, but do not assume the listed risks remain present until confirming current source. Also search repository and git history for credentials or accidental financial data; never paste secret values into the report. If a secret is found, name the file/commit and secret category only, and recommend revocation/rotation.

STOP after delivering the audit. Make no changes until I approve the phases.

---

## PROMPT 1 — Security-first Gemini/API-key remediation

After the audit is approved, fix the AI key architecture before other AI enhancements.

Inspect:
- app/src/main/kotlin/app/privatemoney/cloud/GeminiAIService.kt
- app/src/main/kotlin/app/privatemoney/ui/insights/InsightsViewModel.kt
- app/build.gradle.kts (actual path from tree; locate current config)
- .github/workflows/release.yml
- supabase/functions/ai-analyze and ai-categorize
- docs/AI.md and docs/THREAT_MODEL.md

Requirements:
1. Production Android artifacts must never contain GEMINI_API_KEY or other server secrets. Remove the direct Gemini Android SDK path if it embeds a secret. Use a Supabase Edge Function with server-side secret, authenticated invocation, rate limiting, input/output validation and explicit consent. A publishable/anon Supabase key may be embedded only if intended and protected by correct Row Level Security; never include service_role or database passwords.
2. Ensure local deterministic analytics work with AI disabled, no network, timeout, invalid JSON, quota exhaustion and server failure.
3. AI may only produce read-only explanations/suggestions; it must never create/update/delete records or perform arithmetic that overrides the local financial engine.
4. Send only minimized, locally computed aggregates by default. Never send account numbers, tokens, raw database/backups, attachments, sensitive notes or full transaction histories by default.
5. Validate response schema, lengths, enum IDs, amounts, dates and references locally. Render untrusted output as text, not HTML/markup.
6. Add unit/contract/security tests proving:
   - build config does not provide a Gemini secret to app code;
   - release APK scan detects known secret patterns;
   - AI off causes no request;
   - consent is required and revocable;
   - malformed/oversized/timeout responses fail safely;
   - prompts/merchant text cannot issue instructions to the AI client.
7. If historical public releases or commits contain a real credential, do not silently rewrite shared git history. Document the exposed credential category and immediate revocation steps; do not print the value.
8. Update docs only after behavior is real. Do not claim Edge Functions are deployed unless deployment was verified.

Acceptance: no production Gemini key in APK; tests pass; AI remains off by default; user can disable/revoke consent; app remains usable offline.

---

## PROMPT 2 — Financial ledger correctness and calculation engine

Review domain ledger posting and planning code, including JournalPosting, TransactionDraft, Money, SafeToSpendEngine, PlanReport, repositories, DAO SQL and every summary ViewModel.

Build one source of truth for ledger semantics.

Rules and checks:
- Money is integer minor units (Long) with explicit currency; never Float/Double for balances.
- Document account sign conventions for assets, liabilities, income, expense and transfers.
- Each posted journal transaction must balance to zero under the selected double-entry convention; validate account and category legs, transaction currency, transfer source/destination, and positive magnitude.
- Expense reduces the chosen asset (or increases selected liability) and raises expense totals once.
- Income raises asset (or reduces liability only where explicitly modelled) and raises income totals once.
- Transfer moves value between accounts and must not count as income, consumption, or net-worth creation.
- Deletion/void/reversal updates balance, trends, budget, goals and insights consistently; preserve an audit trail and do not leave orphan journal legs.
- Prevent duplicate IDs and repeated submit; use database transaction boundaries for all multi-row operations.
- Editing amount, date, account, category, currency or transaction type must either safely reverse+repost all journal legs atomically or be explicitly disabled. Do not update metadata while silently leaving journal entries stale.
- Enforce currency compatibility. Do not sum different currencies as if comparable without explicit FX rate/date/source and base currency.
- Reject zero, negative and overflow amounts; validate date/time and configured financial timezone/month start.
- Financial percentages must use explicit rounding. Handle no income, negative balances and zero denominator.

Write deterministic unit tests for:
1. One expense; one income; transfer between two assets; credit-card purchase/payment; refund; reversal; soft delete.
2. Two-account transfer balance conservation.
3. Same-account transfer rejection.
4. Month boundaries and leap day.
5. Cents/paise rounding and Long overflow edge cases.
6. Every summary equals the source-ledger recomputation after create/edit/delete/import/restore.
7. Fuzz/property-style generated transactions if test stack supports it.

Do not build UI or add features until these tests expose and resolve inconsistent domain behavior.

---

## PROMPT 3 — Safe-to-Spend, 50/30/20, debt and goal accounting

Review HomeViewModel, PlansViewModel, InsightsViewModel, PlanningRepository, planning engines, and the domain documentation.

Safe-to-Spend:
- Keep a pure deterministic domain function: available liquid cash minus upcoming mandatory bills that have not already been paid, debt payments due in the period, credit-card dues not already reflected in bills/debt payments, planned goal contributions that are real commitments, and chosen reserve/buffer.
- Define exact as-of date, month window, currency, account types counted as liquid, and debt/credit-card sign conventions.
- Deduplicate by commitment identity and linked transaction, not by fuzzy text alone.
- Avoid subtracting historical outflow a second time if current account balance is already post-transaction.
- Expose an itemized breakdown and explain negative/low values; never silently replace calculation exceptions with an optimistic liquid-only result.
- If input data is missing/stale/unknown, show “incomplete estimate” plus missing items, not a falsely precise figure.
- Make date calculations refresh after midnight, month rollover, timezone changes and app resume. Inject Clock/ZoneId for tests.

Budget:
- Define whether 50/30/20 is based on configured take-home income or actual received income. Show the basis explicitly.
- Needs/Wants should measure consumption expenses, while savings/investment contributions and debt principal payments need explicit treatment. Transfers must be excluded from income/expense totals.
- Include unclassified items in an “Unclassified — needs review” bucket; don't omit them.
- Display target %, actual amount, actual %, remaining allocation and overspend. Ensure targets add to 100 or validation reports an error.
- Never call “investment” consumption spending unless the user selected that rule knowingly.
- Support monthly budgets and a configurable period when architecture permits; define rollover clearly.

Debts/goals:
- Add debt balance history or a clear manually-updated as-of balance with audit timestamps; do not imply automatic lender sync.
- Optional payoff forecast should state assumed APR, compounding, payment date, fees and accuracy limits; implement avalanche/snowball scenarios as projections only.
- Goal contributions must be linked to actual accounts/transactions or explicitly labelled manually tracked. Avoid double-counting goal balances and assets in net worth.
- Chit fund is a region-specific risk-bearing scheme; show contribution schedule, payout status, total paid/received, counterparty notes, due reminders and explicit disclaimer; do not imply guaranteed return.
- Add tests for commitment duplication and goal/debt/card interactions.

---

## PROMPT 4 — Backup, restore, import/export, data recovery

Review CloudSyncService.kt, BackupModels.kt, CloudBackupSheet.kt, ImportScreen.kt/ImportViewModel.kt, database DAOs, settings and migrations.

Treat backup restore as a release-blocking data-safety workflow.

A. Encrypted portable backup:
- Explicitly document format version, algorithm suite, KDF (PBKDF2-HMAC-SHA256 with reviewed work factor or supported stronger alternative), unique random salt and AES-GCM nonce, authenticated envelope metadata, and maximum allowed sizes.
- Calibrate KDF parameters for mobile performance and security; never downgrade silently; include crypto-format version to permit future upgrades.
- Clear passphrase char arrays and derived key bytes where safely possible; don't log keys, passphrases, content or names.
- Bind metadata using AEAD associated data or authenticated payload; don't rely on an unauthenticated plain SHA-256 checksum alone to authenticate encrypted data.
- Validate magic, versions, lengths, counts, UTF-8/JSON schema and referential integrity before writing to DB. Prevent giant-file/zip-bomb-like resource exhaustion.
- Include every user-data table required for a full recovery: categories/custom categories, accounts, transactions including deleted/audit data as policy requires, journal legs, goals/contributions, budgets, debt, chit funds/installments, recurring rules and instances, tags, attachments metadata/files if implemented, preferences (never secrets), notification preferences and migration/version metadata. Make exclusions explicit.
- Snapshot the database consistently (no half-written backup while new transactions commit).
- Keep cloud backup optional and consent-gated; upload only ciphertext. Explain provider retention/deletion behavior accurately.

B. Restore:
- Wrong password/corrupt/truncated/unsupported-version backup must leave current data unchanged.
- Validate backup completely in staging before any live mutation.
- Offer clear modes: Merge with conflict policy OR Replace all local data with explicit double confirmation. Never silently mix both.
- Perform commit atomically in a DB transaction where possible. Keep a pre-restore safety backup for Replace mode.
- Preserve IDs and links. Restore parent tables before child rows, and reassign autogenerated journal row IDs correctly; do not rely on row IDs from a different DB.
- Define duplicate ID handling with version/conflict timestamp; don't silently IGNORE records then report full success.
- Return a report: imported counts, skipped/conflicts, rejected records, prior-backup creation, timestamp and failure reason code.
- Ensure cancellation/process death leaves either old DB intact or a verifiable recovery path.

C. CSV/XLSX/PDF statement import:
- Treat files as hostile input; validate magic bytes, MIME, size, row/cell count, archive expansion, encoding, dates and numbers.
- Parse locale/Indian amount formats and CR/DR markers carefully; show a preview with mapping and row-level errors before commit.
- Deduplicate using stable statement reference where available, otherwise deterministic normalized fingerprint with collision review. Do not assume date+amount alone uniquely identifies a transaction.
- Escape CSV formula injection on export. Never autoexecute formulas/macros.
- Import must be transactional, idempotent on repeated same file, and rollback cleanly on malformed input.
- For AI extraction, do not trust model output; validate each field/amount/date/account locally and require user approval before import.

D. Tests required:
- Backup -> clear test DB -> restore -> compare canonical snapshots and balance/report totals.
- Wrong password, flipped bit, truncated header, huge metadata, unsupported version, missing parent/foreign key, duplicate IDs, incomplete journal, duplicate restore and canceled restore.
- Repeated import same statement creates no duplicates.
- Failed restore never damages existing data.

---

## PROMPT 5 — Recurring transactions, reminders and lifecycle

Inspect recurring transaction entity, PlanningRepository, NotificationHub, NightlyReminderScheduler/Receiver/BootReceiver, AndroidManifest and any WorkManager use.

Implement robust idempotent recurring processing:
- One rule can generate at most one posted instance per scheduled occurrence. Add stable occurrence key/idempotency constraint.
- Define DAILY/WEEKLY/MONTHLY/YEARLY and intervals; month-end rules must define behavior for dates such as the 29th/30th/31st.
- Honor local timezone, DST, timezone changes, phone reboot, app update, permissions denied, battery restrictions, offline state and process death.
- Use WorkManager for deferrable jobs unless a precisely timed alarm is essential and justified. Re-evaluate exact alarm permissions because current manifest requests exact-alarm permissions; document why they are necessary or remove them.
- Do not automatically book a transaction if the user expects a reminder only. Provide per-rule setting: reminder-only vs auto-post; clearly disclose that auto-post is local bookkeeping, not a real bank payment.
- Show due date, last generated occurrence, next date, pause/resume, edit, skip once and cancel.
- Keep notification content privacy-safe (hide amount/merchant on lock screen unless user opts in).
- Ensure notification actions are idempotent and never create duplicates on double tap.
- Refresh calculations and notifications after timezone/date/config changes.
- Provide deterministic tests around month ends, leap years, reboot/restore, duplicate worker execution and notification permission denial.

---

## PROMPT 6 — UI/UX review for native Android screenshots and everyday use

Retain the existing "Quiet Financial Instrument" visual identity and design-system components; avoid arbitrary redesign, decorative 3D effects that lower clarity, and unnecessary animation.

Audit each screen and flow in Jetpack Compose: onboarding, home, ledger, account/vault list, insights, goals/debts/chit funds/recurring plans, add transaction, import, cloud backup, settings, app lock, notifications and AI.

Apply:
- Use user-centred names: Home/Dashboard, Accounts, Transactions, Insights, Goals & Plans, Settings; “Vaults” can remain brand language only if users also see “Accounts”.
- Use consistent Indian number formatting (₹95,000 for whole rupees; display paise only when relevant), explicit date format and account masking.
- Use stable component states: loading/skeleton, empty with clear next action, content, error+retry, success feedback, offline, permission denied and stale-data warning.
- Transaction forms should branch by type:
  Expense: amount, source account, expense category, merchant/payee, date, optional note, optional Need/Want classification.
  Income: amount, destination account, income category/source, date, note.
  Transfer: amount, source account, destination account, date/note; no expense category or Need/Want.
  Investment: only if model has investment asset/contribution semantics; do not model a portfolio purchase as plain consumption expense by default.
- Make amount entry fast: decimal keypad, sensible focus order, clear/reset behavior, dynamic account balance preview, category search, recent merchants and editable suggestions.
- Use concise labels; examples: “Record Financial Movement” -> “Add transaction”; “Real-Time Transaction Stream” -> “Recent transactions” unless true live sync is verified; “Debt Exposure” -> “Outstanding debt”; “Neural Financial Intelligence” -> “AI Money Coach”; Config -> Settings.
- Verify all buttons do work and have loading/disabled behavior. No fake realtime/AI/cloud status.
- Improve accessibility: meaningful semantic labels, TalkBack order, 48dp touch targets, scalable font, contrast, focus, no color-only meaning, reduced animation.
- Test compact Android phones, tablets, landscape, system large font, light/dark/OLED themes, keyboard opening, insets and edge-to-edge.
- Privacy controls: mask values with privacy-safe accessibility text; secure window behavior and screenshots should match user preference.
- Make charts legible, label axes/period, add accessible summaries, tap-to-filter, zero-data and negative values handling.
- Avoid unnecessary new dependencies; refactor only where it improves maintainability.

Deliver before/after screenshots or Compose UI test evidence. Do not change financial computation while doing a visual-only PR.

---

## PROMPT 7 — Insights and AI Money Coach

Use deterministic local calculations as the source of truth. AI explains verified figures, never invents amounts or trades.

1. Add selectable periods: month to date, previous month, last 3/6/12 months, custom range. Avoid date range becoming stale in a long-lived ViewModel; refresh by injected clock/zone.
2. Add month-over-month and year-over-year comparisons only when the prior period has comparable data. Show incomplete months clearly.
3. Provide category drill-down, merchant trends, recurring payment detection, spending pace and unusual-change flags, with transparent deterministic rules.
4. Separate essential consumption, discretionary consumption, savings/investments, internal transfers, debt principal and interest where model supports it.
5. Include data completeness indicator, missing/unclassified categories and excluded accounts/currencies.
6. For AI, offer specific prompts and answers grounded in selected, user-approved aggregates; list data sent before the first request; consent is off by default and easy to revoke.
7. Validate strict JSON schema and sanitize lengths and enumerations. No executing generated content, SQL, links, app actions, or markdown HTML.
8. Add guardrails: not a certified financial adviser, never promise returns, no automated investment trades or loan decisions; disclose assumptions and low confidence.
9. Provide offline fallback, request ID for support without sensitive data, timeout/cancellation and retry control.
10. Use evaluation fixtures with known synthetic data to assess correctness, hallucination, injection, privacy and response usefulness.

Potential later features (after reliability):
- budget pace forecasts with transparent math;
- subscription price-change detection;
- forecasted low-cash days;
- goal contribution scenario comparison;
- debt payoff timeline comparisons;
- anomaly detection explained using deterministic criteria;
- monthly PDF/CSV report generation and reminders.

---

## PROMPT 8 — Architecture, quality and release hardening

Review current code rather than blindly following architecture docs. The documented target describes domain-pure Kotlin, immutable UI state, money in minor units, a journal ledger and local-first offline operation.

Tasks:
- Check Compose files for oversized composables and duplicate calculation logic. Extract smaller components only where this improves tests and clarity.
- Check ViewModels for unhandled coroutine failures, unsafe default constructors, date ranges computed only once, force-refresh hacks, direct SharedPreferences reads, and exception swallowing.
- Add DI/injected clock, zone, repositories and AI client where useful; do not introduce a large DI framework without justification.
- Check Room migrations and exported schema versions; test upgrade across every supported version using real old schema files, populated sample data and interrupted migration assumptions.
- Add integrity validator: balanced journal, valid account/category references, currency consistency, transfer invariants, no orphan legs, derived balance reconciliation, duplicate recurring occurrence keys.
- Add dependency locking + Gradle verification metadata if compatible with current Gradle; add dependency vulnerability checks and SBOM generation in CI. Pin GitHub Actions by reviewed SHA.
- Review release workflow to ensure no automatic release from an unreviewed push to main if project policy expects tagged releases. Never publish a debug APK as the latest production release without an unmistakable warning.
- Verify release APK is not debuggable, has no cleartext networking, no accidental Gemini secret, correct signature certificate, checksums and version metadata. A passing string scan alone is insufficient.
- Add unit, Room DAO/instrumented, Compose UI, migration, backup-restore, import, notification/worker, AI contract and security tests.
- Add a release checklist covering clean install, upgrade without data loss, locked screen, offline mode, wrong PIN, wrong backup passphrase, restore failure, export/share, accessibility, app update and uninstall/reinstall consequences.
- Update PROJECT_STATUS.md with status labels: Implemented, Unit-tested, Integration-tested, Device-tested, Deployed, Release-verified. Never conflate them.
- Keep CI secrets out of app configs. Rotate exposed secrets rather than attempting to hide them with Base64/obfuscation.

---

## PROMPT 9 — Test execution and final acceptance

Before calling a phase complete:
1. Check out a clean branch and report working-tree status. Do not overwrite unrelated user changes.
2. Run the applicable command(s), e.g.:
   - `./gradlew --no-daemon :domain:test :app:testDebugUnitTest`
   - `./gradlew --no-daemon :app:lintDebug`
   - `./gradlew --no-daemon :app:assembleDebug`
   - `bash scripts/verify-apk.sh app/build/outputs/apk/debug/app-debug.apk`
   - relevant instrumented/device tests when emulator/device is available.
3. If a command cannot run due to missing SDK/emulator/network/credentials, state “NOT RUN” and explain why; do not report pass.
4. Compare all dashboard values with independent calculations from synthetic fixture records. Build a canonical expected-result table for income, expenses, transfers, account balances, net worth, savings rate, budget allocation, goal totals and Safe-to-Spend.
5. Use synthetic test records, never the real user's financial data.
6. Confirm no secret string in app artifacts, no credentials in logs, and no sensitive payload sent without consent.
7. Confirm backups restore all required records and a failed restore leaves prior data intact.
8. Summarize modified files, migration impact, test outputs, known limitations and manual device steps still required.

Acceptance means: data safety, money math, privacy, tests and release behavior are correct—not just that the UI looks polished.

## Suggested order

1. Audit only (MASTER PROMPT).
2. Critical security/key remediation.
3. Ledger invariant and calculation tests.
4. Backup/restore and import integrity.
5. Safe-to-Spend and budget semantics.
6. Recurring jobs and reminders.
7. UX/accessibility pass.
8. Insights/AI.
9. Release hardening + physical-device acceptance.

Do not bundle all phases into one giant change. Make small commits and review diffs after each phase.
