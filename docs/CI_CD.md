# CI/CD

All third-party actions are pinned to commit SHAs (tag in a trailing comment). Workflows default to `contents: read`.

## verify.yml (pull requests and pushes to main)
1. Secret scan with gitleaks over full history.
2. Gradle wrapper validation.
3. `:domain:test` and `:app:testDebugUnitTest`.
4. `:app:lintDebug`.
5. `:app:assembleDebug`.
6. `scripts/verify-apk.sh`: fails on secret-shaped strings, `debuggable`, cleartext, `allowBackup`, more than one exported component, or any forbidden permission.
7. Test and lint reports uploaded as artifacts.

## release.yml (every push to main and tags `v*`)
Triggered automatically on every push to `main` as well as release tags `v*`.
Checks out code, scans for secrets with Gitleaks, runs domain tests and Android lint, builds the APK (signed release APK if keystore secrets exist, or debug APK), verifies the APK against credential and manifest security checks, generates SHA-256 checksums, uploads the APK to GitHub Actions artifacts, and automatically publishes/updates the latest GitHub Release using GitHub CLI (`gh release`).

### Secrets (Settings > Secrets and variables > Actions)
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

### Automatic Release
Every push to `main` builds and updates the release APK automatically. You can also manually trigger a release from GitHub Actions or cut a versioned tag:
```bash
git tag v0.2.0 && git push origin v0.2.0
```

## Not yet in CI (tracked in PROJECT_STATUS.md)
Dependency vulnerability scanning, instrumented tests on an emulator, Gradle dependency locking and verification metadata, SBOM, and Edge Function tests (Deno).

## Local checks that mirror CI
```
./gradlew :domain:test :app:lintDebug :app:assembleRelease
bash scripts/verify-apk.sh app/build/outputs/apk/release/app-release-unsigned.apk
```
