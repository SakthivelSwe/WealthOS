# Private Money OS

A private, local-first personal money manager for Android (Kotlin, Jetpack Compose). Your data lives encrypted on the phone. Cloud backup and AI are optional and off by default.

**Status:** early development. See [PROJECT_STATUS.md](PROJECT_STATUS.md) for what works, what is verified, and what is missing. Do not rely on it for real finances until a release is tagged and you have tested it.

## Build

Requirements: JDK 21, Android SDK (platform 37, build-tools), Windows/macOS/Linux.

```
cp local.properties.example local.properties   # set sdk.dir
./gradlew :domain:test :app:assembleDebug
```
Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Install an APK on your phone

1. Download the APK from the GitHub Release (or build the debug APK).
2. On the phone: Settings > Apps > Special access > Install unknown apps, allow your browser or file manager.
3. Open the APK and install. To update, install a newer APK signed with the same key (same signing key is required).
4. Optional, with USB debugging: `adb install -r PrivateMoneyOS-vX.Y.Z.apk`.

## Verify a release before installing

1. Compare checksums (Windows PowerShell): `Get-FileHash PrivateMoneyOS-vX.Y.Z.apk -Algorithm SHA256` against `PrivateMoneyOS-vX.Y.Z.apk.sha256`.
2. Check the signer: `apksigner verify --print-certs PrivateMoneyOS-vX.Y.Z.apk` (from Android build-tools). The SHA-256 certificate digest must equal the value you recorded when you created your key and the one in `signing-certificate-sha256.txt`. Record it once from your own keystore; a different value on a later release means do not install.

## Documentation

| Doc | Purpose |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, money and ledger model |
| [docs/DATABASE.md](docs/DATABASE.md) | Schema and encryption |
| [docs/SECURITY.md](docs/SECURITY.md) | Controls |
| [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md) | Threats and residual risk |
| [docs/AI.md](docs/AI.md) | Money Lens and data minimisation |
| [docs/SETUP_SUPABASE.md](docs/SETUP_SUPABASE.md) | Optional cloud setup |
| [docs/CI_CD.md](docs/CI_CD.md) | Pipelines and release |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why things are the way they are |

## Limits you should know

- No software is guaranteed free of vulnerabilities. A rooted or compromised phone defeats on-device protections.
- AI can be wrong. It only explains numbers the app calculated; it never changes your data.
- Free-tier cloud quotas and third-party services can change.
