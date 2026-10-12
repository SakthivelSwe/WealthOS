# Project Status

Environment: JDK 21, Android SDK (platforms 36, 37), Gradle 9.5.0, AGP 9.3.3, Kotlin 2.4.20, KSP 2.3.12, Room 2.8.5, SQLCipher 4.19.0, Compose BOM 2026.08.00, Lifecycle 2.11.0, Biometric 1.1.0. These build together (verified 2026-10-07).

## VERIFIED (run on this machine)
- `:domain:test` passes (ledger, planning, import security, duplicate detection, keypad, seed data)
- `:app:lintDebug` passes; `:app:assembleDebug` and R8 `:app:assembleRelease` succeed
- `scripts/verify-apk.sh` passes on the release APK: no secret patterns, not debuggable, no cleartext, no backup, a single exported component, no forbidden permissions. (It found an exported androidx ProfileInstallReceiver and a deprecated USE_FINGERPRINT permission; both removed in the manifest.)

## WRITTEN BUT NOT VERIFIED
- Nothing has run on a device or emulator (none available): encrypted DB open, Keystore wrapping, biometric prompt, all screens.
- Supabase migration and Edge Functions (`ai-analyze`, `ai-categorize`, `health`): not deployed, not executed (no Deno installed), never called against real Gemini.
- GitHub workflows `verify.yml` and `release.yml`: not run yet. Action SHAs were resolved from the real repos.

## DONE
- Complete modern UI/UX overhaul across all screens (Home, Ledger, Plans, Insights, Accounts, Add, Import, More, Onboarding)
- Ultra-premium 3D Prismatic Vault Crest app icon (adaptive launcher vector background & foreground)
- FinTech dark/light/midnight design system with GlassCards, linear progress, category glyphs with emoji, and tabular money typography
- Privacy Shield: Global balance masking toggle (••••••) for viewing in public
- Theme Mode Switcher: Dark Obsidian, Midnight OLED, Light Apple, System Default
- Edit Transaction & Delete Transaction via detailed BottomSheet wired to SQLite DAO
- Paged transaction search and type filter chips (All, Expenses, Income, Transfers)
- Interactive 50/30/20 budget planner with segmented progress bar and setup presets
- Financial Goals, Debts, Chit Funds, and Recurring Subscriptions tracking UI
- 6-month dual-column cashflow trend chart and category expense distribution analytics
- Digital Wallet account cards with net worth portfolio breakdown
- Safe-to-spend gauge with days-remaining-in-month countdown and interactive breakdown sheet
- Statement import UI for CSV, XLSX, and PDF
- Design docs; domain engines; hardened manifest; Keystore-wrapped SQLCipher DB; app lock

## KNOWN GAPS
- Safe-to-spend commitments and buffer are zero until bills, goals and debts are linked to ledger payments.
- Circuit breaker and minute limiter are per function instance.

## TODO (in order)
1. Goals, debts, chit fund, recurring (WorkManager), notifications
2. Encrypted backup (PBKDF2 + AES-GCM), JSON export, delete-all, Supabase client and consent UI
5. Encrypted backup (PBKDF2 + AES-GCM), JSON export, delete-all, Supabase client and consent UI
6. AI client with local validation; `ai-statement-extract`
7. Integrity validator; instrumented, UI, migration and security tests; Deno tests for functions
8. Gradle dependency locking and verification metadata; vulnerability scan; SBOM
9. THREAT_MODEL/MASVS checklist updates, PRIVACY, BACKUP, TEST_PLAN, DATABASE refresh; release v0.1.0

## SECURITY RISKS
- Backup crypto, attachments, import UI, AI client, dependency locking and vulnerability scanning are not implemented.
- The Supabase password and Gemini key were pasted in chat: rotate both before use.

## KNOWN LIMITATIONS
- minSdk 31. No zero-vulnerability guarantee; a rooted OS defeats on-device protections. AI can be wrong and needs consent. Release signing, GitHub runs, Supabase deployment and physical-device tests need the user.
