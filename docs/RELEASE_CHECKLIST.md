# WealthOS Release & Quality Acceptance Checklist

This checklist is mandatory prior to tagging or publishing any production release (`v*.*.*`) of WealthOS (PrivateMoneyOS). All manual verification steps must be executed on a physical Android device or official Android emulator running API 31+.

---

## 1. Clean Installation & Initial Setup
- [ ] **Clean Install**: Install fresh APK onto device via `adb install -r PrivateMoneyOS-<TAG>.apk`.
- [ ] **Onboarding Flow**:
  - Currency selection defaults correctly (e.g. INR / ₹).
  - Primary account setup accepts initial opening balance (e.g. ₹50,000) and saves without error.
  - Initial categories seeded automatically (`CategoryDao.count() > 0`).
  - Home dashboard renders immediately with correct opening balance.
- [ ] **App Lock Setup**:
  - Configure 4-digit PIN. Verify PIN confirmation enforces exact match.
  - Biometric prompt appears on supported devices and succeeds.
  - App state securely transitions to unlocked without leaking balance previews during cold start.

---

## 2. Upgrade & Data Migration (Zero Data Loss)
- [ ] **Consecutive Schema Migrations**:
  - Install prior release APK (e.g., Schema v1, v2, or v3) with populated sample transactions, categories, goals, debts, and recurring rules.
  - Install new APK over existing install (`adb install -r`).
  - App opens successfully without crashing or triggering destructive fallback.
  - Verify all historical transactions, journal entries, account balances, and recurring rules remain intact.
  - Confirm new columns (`needWant` on transactions, `autoPost` on recurring rules) have default values and no data corruption.

---

## 3. Security, Lock Screen & Biometric Invariants
- [ ] **Background & Multitasking Masking**:
  - Minimize app to Android recent apps / task switcher.
  - Confirm window content is secured (`FLAG_SECURE`) if user enabled secure screen, preventing screenshots and task preview leaks.
- [ ] **Timeout Re-locking**:
  - Leave app in background beyond the configured lock timeout (immediate / 1 min / 5 min).
  - Re-open app: PIN/Biometric lock screen must display immediately.
- [ ] **Wrong PIN Protection**:
  - Enter incorrect PIN 3 times. Verify error vibration and visual feedback.
  - Confirm database remains locked and un-decrypted until correct PIN/biometrics provided.
- [ ] **Lock Screen Notification Privacy**:
  - Trigger recurring bill or reminder notification.
  - When device lock screen is active, confirm notification visibility is masked (`NotificationCompat.VISIBILITY_PRIVATE`) so sensitive transaction amounts and merchant names are hidden unless opted in.

---

## 4. Offline Mode & Local-First Operation
- [ ] **Zero Network Test**:
  - Enable Airplane mode (disable Wi-Fi and mobile data).
  - Launch app: all features (Home, Ledger, Accounts, Insights, Goals & Plans) must function completely without error dialogs or network timeouts.
  - Create new expense, income, and transfer transactions. Confirm immediate local journal posting and balance updates.
  - Generate AI Insights: confirm app gracefully falls back to deterministic local rule-based analytics with zero crashes.
  - Reconnect network: verify no data duplication or synchronization errors.

---

## 5. Ledger Invariants & Financial Correctness
- [ ] **Double-Entry Balance Check**:
  - Create a ₹5,000 transfer between Bank and Cash accounts.
  - Verify Bank balance decreases by ₹5,000, Cash increases by ₹5,000, and Net Worth remains strictly unchanged.
  - Confirm no income or expense category totals are modified by transfers.
- [ ] **Transaction Void & Soft-Deletion**:
  - Delete an expense transaction.
  - Verify account balance is immediately restored and monthly expense totals reduce by the exact amount.
  - Verify journal entries are retained with `deletedAt` audit timestamp for auditability.
- [ ] **Integrity Audit**:
  - Run `DatabaseIntegrityValidator.validateDatabase()`. Report must confirm `isValid == true` with 0 critical violations.

---

## 6. Backup, Restore & Disaster Recovery
- [ ] **Encrypted Backup Export**:
  - Set a strong backup passphrase. Export `.pmos` backup file.
  - Confirm generated envelope adheres to format: `PMOSBKP2` magic header, PBKDF2 salt (32 bytes), AES-GCM nonce (12 bytes), and authenticated metadata.
- [ ] **Wrong Passphrase Rejection**:
  - Attempt restore with an incorrect passphrase.
  - Confirm operation fails cleanly with `AEADBadTagException` or decryption error.
  - **CRITICAL**: Confirm existing local database is completely untouched and intact.
- [ ] **Corrupted Envelope Handling**:
  - Flip a byte in the backup payload or truncate the file.
  - Attempt restore: confirm validation detects tamper/corruption and aborts safely.
- [ ] **Complete Restore Verification**:
  - Clear app storage or use a clean device.
  - Restore with correct passphrase in Replace mode.
  - Confirm all accounts, categories, transactions, journal legs, debts, goals, and recurring rules are 100% restored with identical balances and historical totals.

---

## 7. Statement Import & Formula Injection Safety
- [ ] **CSV / PDF Import**:
  - Import bank statement CSV with Indian number formatting (e.g. `1,25,000.50` CR/DR).
  - Verify transaction mapping preview accurately identifies dates, amounts, and descriptions.
  - Confirm duplicate transactions are safely flagged.
- [ ] **CSV Export Formula Injection**:
  - Ensure any exported descriptions beginning with `=, +, -, @` are sanitized with leading single quotes (`'`) to prevent formula execution in Excel/Sheets.

---

## 8. Accessibility & Visual Design Acceptance
- [ ] **TalkBack Screen Reader**:
  - Enable TalkBack. Navigate through Home, Ledger, and Add Transaction screens.
  - Confirm all icon buttons, chips, and financial cards have meaningful content descriptions.
  - Confirm masked balances read privacy-safe accessibility text instead of raw characters.
- [ ] **Font & Display Scaling**:
  - In Android System Settings, increase font size to Largest (130%+) and display size to Large.
  - Verify no text clipping, button overlap, or broken layouts.
- [ ] **Touch Target Sizing**:
  - Ensure all clickable actions meet the minimum 48dp x 48dp touch target standard.
- [ ] **Theme Switching**:
  - Switch between Dark Obsidian, Midnight OLED, and Light Apple themes. Confirm all text elements maintain WCAG AAA contrast ratios.

---

## 9. Release Binary Verification (Automated & Manual)
- [ ] **Signing & Key Architecture**:
  - Build signed release APK using production keystore via `.github/workflows/release.yml`.
  - Verify release APK signature with `apksigner verify --verbose --print-certs`.
  - Confirm SHA-256 certificate fingerprint matches official developer certificate.
- [ ] **Secret Scan**:
  - Execute `bash scripts/verify-apk.sh PrivateMoneyOS-<TAG>.apk`.
  - Confirm zero hits for Google API keys, JWT tokens, Supabase secret keys, private keys, or AWS keys.
- [ ] **Manifest & Security Constraints**:
  - Verify `android:debuggable` is `false`.
  - Verify `android:usesCleartextTraffic` is `false`.
  - Verify `android:allowBackup` is `false`.
  - Verify forbidden permissions (`READ_SMS`, `ACCESS_FINE_LOCATION`, etc.) are absent.
  - Verify `USE_EXACT_ALARM` is absent, and only `SCHEDULE_EXACT_ALARM` is declared.
