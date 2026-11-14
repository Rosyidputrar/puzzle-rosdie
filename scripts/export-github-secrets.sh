#!/usr/bin/env bash
# Menyiapkan nilai untuk GitHub Actions secrets dari keystore lokal.
# Jalankan setelah ./scripts/create-release-keystore.sh. Tidak ada yang dikirim ke mana pun:
# skrip hanya menulis release-keystore.jks.base64 (di-.gitignore) dan menampilkan petunjuk.
set -euo pipefail
cd "$(dirname "$0")/.."

[[ -f keystore.properties ]] || { echo "keystore.properties tidak ditemukan. Jalankan scripts/create-release-keystore.sh dulu." >&2; exit 1; }
prop() { grep -E "^$1=" keystore.properties | head -1 | cut -d= -f2-; }
STORE_FILE=$(prop storeFile)
ALIAS=$(prop keyAlias)
[[ -f "$STORE_FILE" ]] || { echo "Keystore $STORE_FILE tidak ditemukan." >&2; exit 1; }

OUT="${STORE_FILE}.base64"
umask 077
base64 < "$STORE_FILE" | tr -d '\n' > "$OUT"

cat <<MSG
Buka GitHub: repository > Settings > Secrets and variables > Actions > New repository secret,
lalu buat empat secret berikut:

  RELEASE_KEYSTORE_BASE64   = seluruh isi file $OUT (satu baris panjang)
  RELEASE_KEYSTORE_PASSWORD = nilai storePassword di keystore.properties
  RELEASE_KEY_ALIAS         = $ALIAS
  RELEASE_KEY_PASSWORD      = nilai keyPassword di keystore.properties

Setelah disalin, hapus file $OUT. Simpan cadangan $STORE_FILE dan password-nya di tempat aman:
tanpa keystore yang sama, update aplikasi berikutnya tidak bisa dipasang di atas versi lama.
MSG
