#!/usr/bin/env bash
# Fails when an APK contains credential-looking strings or risky manifest settings.
# Usage: scripts/verify-apk.sh path/to/app.apk
set -euo pipefail

apk="${1:?usage: verify-apk.sh <apk>}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
unzip -q -o "$apk" -d "$work"

fail=0

# Known secret shapes. Matches are never printed, only the file name and the rule.
declare -A rules=(
  ["Google API key"]='AIza[0-9A-Za-z_-]{35}'
  ["JWT"]='eyJ[A-Za-z0-9_-]{15,}\.eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{10,}'
  ["Supabase secret key"]='sb_secret_[A-Za-z0-9_-]{10,}'
  ["service_role marker"]='service_role'
  ["GitHub token"]='gh[pousr]_[A-Za-z0-9]{30,}'
  ["AWS access key"]='AKIA[0-9A-Z]{16}'
  ["Private key block"]='-----BEGIN [A-Z ]*PRIVATE KEY-----'
  ["GEMINI_API_KEY marker"]='GEMINI_API_KEY'
)
for name in "${!rules[@]}"; do
  if hits=$(grep -rlaE "${rules[$name]}" "$work" 2>/dev/null); then
    echo "FAIL: ${name} pattern found in:"; echo "$hits" | sed "s|$work/||"; fail=1
  fi
done

# Manifest checks via aapt2 when available (it is on GitHub runners via the Android SDK).
aapt2="$(ls "${ANDROID_HOME:-/usr/local/lib/android/sdk}"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "$aapt2" ]; then
  manifest="$("$aapt2" dump xmltree --file AndroidManifest.xml "$apk")"
  if echo "$manifest" | grep -Eq 'debuggable.*=true'; then echo "FAIL: debuggable is true"; fail=1; fi
  if echo "$manifest" | grep -Eq 'usesCleartextTraffic.*=true'; then echo "FAIL: cleartext traffic enabled"; fail=1; fi
  if echo "$manifest" | grep -Eq 'allowBackup.*=true'; then echo "FAIL: allowBackup enabled"; fail=1; fi
  # The launcher activity, WorkManager services/receivers, and ProfileInstaller are exported.
  exported=$(echo "$manifest" | grep -c 'exported.*=true' || true)
  if [ "$exported" -gt 5 ]; then echo "FAIL: $exported exported components (expected max 5)"; fail=1; fi
  for perm in READ_SMS RECEIVE_SMS READ_CONTACTS ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION READ_CALL_LOG \
              WRITE_EXTERNAL_STORAGE MANAGE_EXTERNAL_STORAGE QUERY_ALL_PACKAGES; do
    if echo "$manifest" | grep -q "android.permission.$perm"; then echo "FAIL: forbidden permission $perm"; fail=1; fi
  done
else
  echo "WARN: aapt2 not found, skipping manifest checks"
fi

if [ "$fail" -ne 0 ]; then exit 1; fi
echo "APK checks passed"
