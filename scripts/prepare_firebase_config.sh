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
data = json.loads(path.read_text(encoding="utf-8"))
clients = data.get("client", [])
package_names = {
    android.get("package_name")
    for client in clients
    for android in [client.get("client_info", {}).get("android_client_info", {})]
    if android.get("package_name")
}

if "com.aistudio.xynoosfinance.xfnmkz" not in package_names:
    raise SystemExit(
        "::error::google-services.json does not contain package "
        "com.aistudio.xynoosfinance.xfnmkz"
    )
PY

echo "Firebase configuration ready: ${TARGET}"
