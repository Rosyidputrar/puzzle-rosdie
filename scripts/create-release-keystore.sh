#!/usr/bin/env bash
# Membuat keystore rilis LOKAL dan keystore.properties (keduanya di-.gitignore).
# Pemakaian: ./scripts/create-release-keystore.sh
# Password ditanyakan secara interaktif dan tidak pernah ditulis ke repo.
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE=release-keystore.jks
ALIAS=puzzle-rosdie

if [[ -e "$KEYSTORE" || -e keystore.properties ]]; then
  echo "release-keystore.jks atau keystore.properties sudah ada. Hapus dulu bila ingin membuat ulang." >&2
  exit 1
fi

read -r -s -p "Password keystore (min 6 karakter): " STORE_PASS; echo
read -r -s -p "Ulangi password: " STORE_PASS2; echo
[[ "$STORE_PASS" == "$STORE_PASS2" ]] || { echo "Password tidak sama." >&2; exit 1; }
[[ ${#STORE_PASS} -ge 6 ]] || { echo "Password terlalu pendek." >&2; exit 1; }

keytool -genkeypair -v \
  -keystore "$KEYSTORE" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
  -dname "CN=Puzzle Rosdie, O=Rosdie, C=ID"

umask 077
cat > keystore.properties <<PROPS
storeFile=$KEYSTORE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$STORE_PASS
PROPS

echo "Selesai. Simpan cadangan $KEYSTORE dan password-nya di tempat aman (bukan di repo)."
echo "Buat APK rilis bertanda tangan: ./gradlew assembleRelease"
