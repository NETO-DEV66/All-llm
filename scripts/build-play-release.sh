#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${ALLLM_UPLOAD_STORE_FILE:-}" ]]; then
  echo "ALLLM_UPLOAD_STORE_FILE is not set: point it at your upload keystore, e.g." >&2
  echo "  export ALLLM_UPLOAD_STORE_FILE=\"\$HOME/.alllm/alllm-upload.jks\"" >&2
  exit 1
fi
keystore_path="$ALLLM_UPLOAD_STORE_FILE"
key_alias="${ALLLM_UPLOAD_KEY_ALIAS:-alllm-upload}"
keychain_account="com.netodev66.alllm"
keychain_service="All-LLM Upload Key"
version_code="${1:-1}"
version_name="${2:-1.0.0}"

if [[ ! -f "$keystore_path" ]]; then
  echo "Upload keystore not found: $keystore_path" >&2
  exit 1
fi

upload_secret="$(security find-generic-password -w -a "$keychain_account" -s "$keychain_service")"
trap 'unset upload_secret' EXIT

cd "$project_dir"
ALLLM_UPLOAD_STORE_FILE="$keystore_path" \
ALLLM_UPLOAD_STORE_PASSWORD="$upload_secret" \
ALLLM_UPLOAD_KEY_ALIAS="$key_alias" \
ALLLM_UPLOAD_KEY_PASSWORD="$upload_secret" \
./gradlew \
  -PplayBuild=true \
  -PappVersionCode="$version_code" \
  -PappVersionName="$version_name" \
  playReadinessCheck testOnlineDebugUnitTest lintOnlineDebug assembleRelease bundleRelease

echo "Signed online bundle: $project_dir/app/build/outputs/bundle/onlineRelease/app-online-release.aab"
echo "Signed offline bundle: $project_dir/app/build/outputs/bundle/offlineRelease/app-offline-release.aab"
echo "Signed online APK: $project_dir/app/build/outputs/apk/online/release/app-online-release.apk"
echo "Signed offline APK: $project_dir/app/build/outputs/apk/offline/release/app-offline-release.apk"
