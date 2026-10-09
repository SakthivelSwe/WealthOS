# Threat Model

Status: design-stage. Mitigations marked *(planned)* are not yet implemented; see PROJECT_STATUS.md.

**Boundary:** if the OS is fully compromised or rooted, the app cannot guarantee protection.
No claim of "100% secure" is made.

| # | Threat | Impact | Mitigation | Residual risk |
|---|---|---|---|---|
| 1 | Lost phone | Data exposure | SQLCipher DB; key wrapped by Keystore AES-GCM; app lock (BiometricPrompt + device credential); FLAG_SECURE; no sensitive notifications | Weak device PIN; attacker with lock-screen bypass exploits |
| 2 | Stolen unlocked phone | Full app access | Inactivity/background lock timeout; immediate-lock option; re-auth for delete-all, export, backup | Access within the timeout window |
| 3 | Malicious app on device | Read data, overlay | Private storage only, no exported components, no providers, FLAG_SECURE, no SMS/accessibility use | Same-UID impossible; accessibility malware may still read screen |
| 4 | Malicious imported file | Code exec / DoS / bad data | Treat as untrusted; magic-byte + MIME check; size/row/cell/zip-ratio limits; values only, no formulas/macros; preview before commit; temp file deletion | Parser library 0-days |
| 5 | Compromised network | MITM | HTTPS only, cleartext disabled, system trust only, no bypass; E2E-encrypted backups; payload minimization | Compromised user-installed CA on device |
| 6 | Leaked API key | Abuse/cost | Gemini key only as Edge Function secret; never in APK/repo; CI secret scan + APK scan | Server-side secret exposure via Supabase account compromise |
| 7 | Supabase misconfiguration | Data exposure | Migrations enforce RLS; setup guide includes unauthorized-user verification | Operator mistakes |
| 8 | RLS failure | Cross-user access | `auth.uid() = user_id` on all tables/storage paths; policy tests; E2E ciphertext means leakage ≠ plaintext | Metadata leakage |
| 9 | AI prompt injection | Misleading output | Strict delimiters; untrusted data never in system instructions; AI read-only, no tools; output schema validation | Misleading explanations; user must confirm everything |
| 10 | Malicious AI response | Bad suggestions, injection into UI | JSON schema, enum allowlist, amount/date/ID validation vs local data; rendered as plain text | Plausible but wrong advice |
| 11 | Dependency compromise | Supply chain | Minimal deps, official repos only, lockfiles, verification-metadata, pinned Actions, vuln scan *(planned)* | Zero-day in trusted vendor |
| 12 | Compromised GitHub repo | Malicious release | Branch protection, signed tags recommended, keystore only in Secrets, checksum + cert fingerprint verification | Attacker with Secrets access |
| 13 | Accidental secret commit | Credential leak | .gitignore, secret scan in CI incl. history, rotate on leak | Window before detection |
| 14 | Backup theft | Data exposure | Client-side AES-256-GCM, PBKDF2-HMAC-SHA256 with random salt, unique nonce, hash verification | Weak passphrase; offline brute force |
| 15 | Rooted device | Key/data extraction | Keystore (hardware-backed where available); documented as out of scope | Not mitigated |

## Explicit non-goals
Banking credentials, SMS scraping, ads, analytics SDKs: never present.
