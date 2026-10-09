# Decisions

| ID | Decision | Rationale |
|---|---|---|
| D1 | Native Kotlin + Compose, Android only | Requirement; best biometric/Keystore integration |
| D2 | Money as `Long` minor units + currency code | Eliminates float drift |
| D3 | Journal (double-entry) under transaction UI | Balances cannot silently drift |
| D4 | Room + SQLCipher pinned as a tested pair | Avoid compatibility breakage from independent upgrades |
| D5 | `allowBackup=false`; in-app encrypted backup | Avoid sensitive DB in Google auto-backup |
| D6 | Gemini only via Supabase Edge Function | No key in APK |
| D7 | AI off by default, consent-gated, read-only | Privacy and safety |
| D8 | WorkManager, no exact alarms | Battery, permission minimization |
| D9 | Pure-Kotlin `:domain` | Fast, heavy property testing of finance logic |
| D10 | Few modules, split on need | Avoid ceremony |
| D11 | minSdk 31 (Android 12); compile/target = latest stable SDK (device runs Android 16) | User requirement: Android 12 through current and future versions |
| D13 | Write code first, install SDK afterwards, then compile and fix in one pass | User's chosen workflow; raises risk of compile errors, so code stays simple and conventional |
| D12 | Exact dependency versions chosen at bootstrap from official release notes | Do not rely on stale knowledge |
