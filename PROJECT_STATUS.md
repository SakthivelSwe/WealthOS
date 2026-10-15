# Project Status

Environment: JDK 21, Android SDK (platforms 36, 37), Gradle 9.5.0, AGP 9.3.3, Kotlin 2.4.20, KSP 2.3.12, Room 2.8.5, SQLCipher 4.19.0, Compose BOM 2026.08.00, Lifecycle 2.11.0, Biometric 1.1.0. These build together (verified 2026-10-07).

## VERIFIED (run on this machine)
- `:domain:test` passes with 100% success (ledger invariants, double-entry balancing, credit-card liabilities/payments, transfers, refunds, reversals, soft-delete, month/leap-day boundaries, Money parsing/overflow, percentage math, property fuzz tests)
- `:app:testDebugUnitTest` passes (FinancialIntegrityTest, BackupEnvelopeTest, NotificationPolicyTest)
- `:app:assembleDebug` builds cleanly
- `scripts/verify-apk.sh` hardened and passing: zero hardcoded secrets, release APK verified against missing signing keys, cleartext traffic disabled

## DONE & AUDIT REMEDIATED (2026-10-15)
- Double-entry ledger semantics & sign conventions formalized across Assets (+), Liabilities (-), Categories (+/-), and Transfers
- Atomic transaction replacement (`LedgerRepository.replace`) via Room transaction boundary
- Reversal entry generation (`JournalPosting.reverse`) with zero-sum validation
- Multi-currency segregation added to all ledger aggregate queries (`observeLiquidBalance`, `observeTotal`, `observeLaneTotals`, `observeMonthlyTotals`, `observeTopCategories`)
- Release pipeline hardened (`.github/workflows/release.yml` fails build on missing production keystore; removed fallback to debug APK)
- SQLCipher downgrade safety: removed `.fallbackToDestructiveMigrationOnDowngrade` from `DatabaseFactory.kt`
- AI architecture aligned: removed direct Google Gemini client SDK and `BuildConfig.GEMINI_API_KEY` embedding; added deterministic local fallback in `GeminiAIService.kt`
- Backup & restore deduplication: filters journal entries on restore to prevent duplicate balance inflation; backs up and restores `categories`; handles `LOCAL_ONLY` status on network failures
- Safe-to-Spend engine wired in `HomeViewModel` with active recurring bills, debts, goals, and credit card liabilities, filtered by user currency and safe fallback
- Full-month expense totals wired in `InsightsViewModel` via `observeTotal(EXPENSE)`
- Unclassified expense tracking rendered in `PlansScreen` and calculated in `PlansViewModel`
- Deterministic recurring transaction IDs (`txn_recur_${recurring.id}_${nextRunDate}`) with atomic schedule advancement in `RecurringWorker.kt`
- Complete modern UI/UX overhaul across all screens (Home, Ledger, Plans, Insights, Accounts, Add, Import, More, Onboarding)
- FinTech dark/light/midnight design system with GlassCards, linear progress, category glyphs with emoji, and tabular money typography
- Privacy Shield: Global balance masking toggle (••••••) for viewing in public
- Theme Mode Switcher: Dark Obsidian, Midnight OLED, Light Apple, System Default
- Paged transaction search and type filter chips (All, Expenses, Income, Transfers)
- Interactive 50/30/20 budget planner with segmented progress bar and setup presets
- Digital Wallet account cards with net worth portfolio breakdown
- Statement import UI for CSV, XLSX, and PDF
- Design docs; domain engines; hardened manifest; Keystore-wrapped SQLCipher DB; app lock

## REMAINING PHASES
1. Safe-to-Spend & Budget Period / Rollover Refinements (Prompt 3)
2. Cloud/local backup file validation, import error reporting, and export formula injection prevention (Prompt 4)
3. WorkManager recurring transaction scheduling & notification privacy toggles (Prompt 5)
4. UI/UX TalkBack accessibility, large font scaling, and edge-to-edge audit (Prompt 6)
5. Insights date range dynamic refresh on midnight/month rollover (Prompt 7)
6. Final release readiness, dependency locking, and physical device checklist (Prompt 8 & 9)

## KNOWN LIMITATIONS
- minSdk 31. No zero-vulnerability guarantee; a rooted OS defeats on-device protections. AI can be wrong and needs consent. Release signing, GitHub runs, Supabase deployment and physical-device tests need the user.
