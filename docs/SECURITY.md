# Security

## Controls (target design; implementation status in PROJECT_STATUS.md)
- **Storage:** SQLCipher DB, random 256-bit key, wrapped with Keystore AES-256-GCM. Attachments encrypted, private storage only, EXIF stripped.
- **Auth:** OS `BiometricPrompt` with device-credential fallback; lock on launch/background/timeout.
- **Screen:** `FLAG_SECURE` default on; opt-out in settings with explanation.
- **Network:** HTTPS only (`usesCleartextTraffic=false`, network security config), system trust store, no custom TrustManagers.
- **Manifest:** only the launcher activity exported; no providers; `allowBackup=false`, data-extraction rules exclude everything; minimal permissions (INTERNET, USE_BIOMETRIC; POST_NOTIFICATIONS/CAMERA on demand).
- **Imports:** untrusted; magic-byte validation, size/row/cell limits, zip-ratio checks, values-only, preview-then-commit in a single DB transaction.
- **Export:** CSV formula-injection escaping; plaintext exports carry an unencrypted warning.
- **Logging:** redacted logger; never amounts, names, tokens, keys, prompts.
- **No** analytics, ads, trackers, WebView, SMS, or banking credentials.
- **AI:** read-only, consent-gated, via Edge Function; key never on device.
- **Release:** R8, non-debuggable, signed in CI from Secrets, SHA-256 + `apksigner verify`.

## Reporting
Personal project; open a private security advisory on the GitHub repo.

## Limits
No system can guarantee zero vulnerabilities. A rooted/compromised OS defeats on-device protections. See THREAT_MODEL.md.
