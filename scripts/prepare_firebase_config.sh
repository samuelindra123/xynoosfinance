#!/usr/bin/env bash
set -euo pipefail

TARGET="app/google-services.json"

if [[ -n "${GOOGLE_SERVICES_JSON_BASE64:-}" ]]; then
  # Accept a JSON value here as well so a misnamed secret does not fail with a
  # confusing base64 error before the configuration can be validated below.
  if [[ "${GOOGLE_SERVICES_JSON_BASE64}" =~ ^[[:space:]]*\{ ]]; then
    printf '%s' "${GOOGLE_SERVICES_JSON_BASE64}" > "${TARGET}"
  else
    printf '%s' "${GOOGLE_SERVICES_JSON_BASE64}" | tr -d '[:space:]' | base64 --decode > "${TARGET}.tmp"
    mv "${TARGET}.tmp" "${TARGET}"
  fi
elif [[ -n "${GOOGLE_SERVICES_JSON:-}" ]]; then
  printf '%s' "${GOOGLE_SERVICES_JSON}" > "${TARGET}"
fi

if [[ ! -f "${TARGET}" ]]; then
  echo "::error::Firebase configuration missing: add app/google-services.json or set GOOGLE_SERVICES_JSON_BASE64 / GOOGLE_SERVICES_JSON as a repository Actions secret."
  exit 1
fi

python3 - <<'PY'
import json
from pathlib import Path

path = Path("app/google-services.json")
try:
    data = json.loads(path.read_text(encoding="utf-8-sig"))
except (UnicodeError, json.JSONDecodeError):
    raise SystemExit("::error::GOOGLE_SERVICES_JSON must contain a valid Firebase Android JSON configuration.")

package_name = "com.aistudio.xynoosfinance.xfnmkz"
client = next((
    item for item in data.get("client", [])
    if item.get("client_info", {}).get("android_client_info", {}).get("package_name") == package_name
), None) if isinstance(data, dict) else None
if client is None:
    raise SystemExit(f"::error::google-services.json does not contain package {package_name}.")

web_client = next((
    item for item in client.get("oauth_client", [])
    if str(item.get("client_type")) == "3"
), {})
client_id = web_client.get("client_id")
if not isinstance(client_id, str) or not client_id.endswith(".apps.googleusercontent.com"):
    raise SystemExit(
        f"::error::Missing Web OAuth client (client_type 3) for {package_name}. "
        "Enable Google in Firebase Authentication, register the APK signing SHA-1, "
        "download a fresh google-services.json, and update the Actions secret used by this workflow."
    )
PY

echo "Firebase configuration ready: ${TARGET}"
