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

## release.yml (tag `v*`)
Checks out the tag, scans for secrets, runs tests and release lint, decodes the keystore into `$RUNNER_TEMP` only, builds the signed R8-minified release APK (Gradle reads the keystore from environment variables; AGP signs and zip-aligns), runs `apksigner verify`, runs the APK content checks, writes the SHA-256 file and signing certificate digest, uploads the R8 mapping as a private 90-day workflow artifact, creates the release with `gh`, and always deletes the keystore.

### Secrets to add (Settings > Secrets and variables > Actions)
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Only these four; no Gemini or Supabase secret is needed by CI.

### Cut a release
```
git tag v0.1.0 && git push origin v0.1.0
```

## Not yet in CI (tracked in PROJECT_STATUS.md)
Dependency vulnerability scanning, instrumented tests on an emulator, Gradle dependency locking and verification metadata, SBOM, and Edge Function tests (Deno).

## Local checks that mirror CI
```
./gradlew :domain:test :app:lintDebug :app:assembleRelease
bash scripts/verify-apk.sh app/build/outputs/apk/release/app-release-unsigned.apk
```
