# Puzzle Rosdie (Android)

Sliding puzzle native Android (Kotlin, Jetpack Compose, Material 3) yang membangun ulang game web
Puzzle Rosdie. Tanpa WebView, tanpa iklan, tanpa akun, tanpa server, tanpa pelacakan. Semua data lokal.

- Package ID: `id.rosdie.puzzle` · minSdk 24 · compileSdk/targetSdk **37**
- Bahasa: Indonesia (default) dan Inggris, dipilih per aplikasi di Pengaturan
- Permission: hanya `VIBRATE`. Tidak ada `INTERNET`, tidak ada permission media (Photo Picker),
  tidak ada `CAMERA` (kamera lewat intent `TakePicture` + `FileProvider`).

## Fitur

- **Permainan**: mode angka dan gambar; Mudah 3×3, Normal 4×4, Sulit 5×5; acakan random walk
  1000–2000 langkah dari posisi selesai (tanpa langkah balik, diulang bila hasilnya selesai), jadi
  selalu solvable. Ketuk (termasuk geser seluruh barisan), swipe, dan seret tile dengan animasi spring.
  Undo (50 aksi), Pause (papan disembunyikan, timer berhenti), Reset, Acak, Hint, Upload gambar,
  panel acuan, simpan otomatis + kartu Lanjutkan.
- **AI solver**: Manhattan Distance + Linear Conflict. 3×3 A* optimal; 4×4 IDA* optimal dengan
  anggaran waktu 20 detik lalu pilihan Lanjutkan / Batal / Solusi Cepat (weighted A*); 5×5 bertahap
  dengan label jujur "Solusi tidak dijamin terpendek". Panel progres ±10×/detik, pembatalan coroutine
  sungguhan, eksekusi langkah dengan slider kecepatan (50–1000 ms), jeda/hentikan, perbandingan
  "AI: 42 langkah, kamu: 87 langkah".
- **Data**: Room (hasil permainan, simpanan, pencapaian, metadata gambar) dan DataStore (pengaturan).
  Rekor per ukuran grid hanya dari kemenangan tanpa AI dan hint.
- **Gamifikasi**: Puzzle Harian (4×4 angka, seed tanggal lokal), streak dan kalender, 11 pencapaian,
  statistik + grafik 7 hari (Canvas), mode Target Langkah dan Lomba Waktu, bagikan hasil.
- **Galeri**: 5 gambar bawaan digambar lewat kode (bebas hak cipta), tambah dari Photo Picker atau
  kamera, crop persegi, simpan 1024 px JPEG di `filesDir`, batas 10 MB per file asli dan 50 MB total
  (peringatan di 80 %).
- **Rasa bermain**: bunyi disintesis sendiri (SoundPool, 3 varian klik, fanfare), getar per gerakan,
  pola menang, getar pendek untuk gerakan tidak valid; confetti di Canvas.
- **Aksesibilitas**: deskripsi per tile ("Tile 5, baris 2, kolom 3"), aksi TalkBack "Geser tile ini",
  target sentuh ≥ 48 dp, kontras AA pada palet bawaan, menghormati "hapus animasi" sistem.
- **Tampilan**: tema terang/gelap/sistem, warna dinamis Android 12+, landscape (papan kiri, panel kanan),
  adaptive icon + ikon monokrom (themed icon Android 13+), Splash Screen API + animasi tile menyusun diri.

## Sistem desain

Komponen kustom di `ui/components/DesignKit.kt` dan `ResultDialog.kt` menggantikan komponen bawaan
yang polos, semuanya diturunkan dari skema warna aktif (ikut tema gelap dan warna dinamis):

- `PillSelector` — pemilih bersegmen dengan indikator yang meluncur (kesulitan, mode, tema, bahasa).
- `HudStat` — statistik permainan dengan angka tabular (tidak "bergoyang") dan bilah progres tipis.
- `DockAction` — dock kontrol permainan; `GradientButton` — aksi utama; `AppSlider` — slider bertrack penuh.
- `SectionCard` + `IconBadge` — kartu seksi dengan ikon bernada warna; `ProgressRing`; `ResultDialog`.
- Shapes M3 lebih membulat (8/12/18/24/32 dp) agar serasi dengan tile papan.

Fitur UX tambahan: meter dan penanda "tile sudah benar", tombol **Intip** (tahan untuk menumpuk
susunan target di atas papan; untuk TalkBack ketuk dua kali), pratinjau papan di kartu Lanjutkan,
hitung mundur Puzzle Harian, sapaan sesuai waktu, pemilih tema dengan pratinjau mini.

## Build

Butuh JDK 17+ (diuji dengan JDK 21) dan Android SDK dengan platform 37 dan build-tools 37.0.0.
Buat `local.properties` berisi `sdk.dir=/path/ke/android-sdk` (atau set `ANDROID_HOME`).

```bash
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease          # R8 minify + shrinkResources
./gradlew installDebug             # pasang ke perangkat/emulator yang terhubung
```

### APK rilis yang ditandatangani

Konfigurasi signing dibaca dari `keystore.properties` di root project. File itu, dan semua `*.jks`,
ada di `.gitignore`; jangan pernah commit kredensial.

1. Buat keystore lokal (interaktif, password tidak ditulis ke repo):
   ```bash
   ./scripts/create-release-keystore.sh
   ```
   Atau manual dengan `keytool`:
   ```bash
   keytool -genkeypair -v -keystore release-keystore.jks -storetype PKCS12 \
     -alias puzzle-rosdie -keyalg RSA -keysize 4096 -validity 10000
   cp keystore.properties.example keystore.properties   # lalu isi password
   ```
2. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
3. Cek tanda tangan: `$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`

Tanpa `keystore.properties`, `assembleRelease` tetap berhasil dan menghasilkan
`app-release-unsigned.apk`.

**Ukuran APK rilis (diukur):** 2.224.281 byte (≈ 2,2 MB, bertanda tangan, satu APK universal; tidak
perlu ABI split karena aplikasi tidak memuat library native).

## Struktur

```
domain/                         Modul Kotlin JVM murni (tanpa Android)
  src/main/kotlin/id/rosdie/puzzle/domain/
    model/   PuzzleState, Move/Direction, Difficulty, GameResult, StarRating, ChallengeRules
    engine/  PuzzleEngine (gerak, menang, solvable, acak), SeededRandom (SplitMix64)
    solver/  PuzzleSolver (fasad), Heuristics, IdaStarSolver, BestFirstSearch (A*/weighted A*),
             StagedSolver (5×5), SearchSupport (heap, hash map Long, pengawas progres/pembatalan)
  src/test/kotlin/               Unit test + SolverBenchmark
app/
  src/main/java/id/rosdie/puzzle/
    MainActivity, PuzzleApplication, AppContainer (DI manual)
    ui/  theme/ navigation/ splash/ home/ game/ gallery/ stats/ howto/ settings/ components/
    data/ local/ (Room) datastore/ (pengaturan) repository/ (game, statistik, pencapaian) images/
    feedback/ SoundSynth, SoundPlayer, Haptics
  src/test/                      Robolectric: ViewModel, Room, repositori, UI end-to-end, screenshot
  src/androidTest/               Smoke test, satu permainan sampai menang, validasi migrasi Room
  schemas/                       Skema Room yang diekspor (untuk migrasi)
gradle/libs.versions.toml        Version catalog
scripts/create-release-keystore.sh
keystore.properties.example
```

Arsitektur: satu Activity, Navigation Compose (route bertipe `@Serializable`), MVVM (satu ViewModel per
layar, state berupa `StateFlow` + data class immutable), repository untuk data, komputasi solver di
`Dispatchers.Default`.

## Test

```bash
./gradlew testDebugUnitTest      # test domain (alias ke :domain:test) + test app (Robolectric)
./gradlew lint                   # lint Android
./gradlew :domain:benchmark      # benchmark solver lengkap (beberapa menit)
./gradlew connectedDebugAndroidTest   # butuh perangkat/emulator
```

Hasil aktual di lingkungan pengerjaan (container Linux 4 vCPU, JDK 21, Robolectric 4.18 dengan SDK 36):

- `:domain:test` — **26 test, 0 gagal**. Termasuk: aturan inversi diverifikasi exhaustif terhadap
  BFS (181.440 state 3×3), acakan 3×3/4×4/5×5 selalu solvable (900 seed), PRNG cocok dengan vektor
  referensi SplitMix64, heuristik admissible + konsisten untuk SEMUA state 3×3, A* 3×3 sama dengan
  jarak BFS eksak (300 instans + dua instans 31 langkah), IDA* 4×4 sama dengan BFS (40 instans) dan
  instans Korf #1 = 57 langkah, pembatalan berhenti < 500 ms dan papan tidak berubah, anggaran waktu +
  lanjutkan menghasilkan jawaban optimal yang sama, progres 3–12 event per detik.
- `:app:testDebugUnitTest` — **27 test, 0 gagal**. Termasuk ViewModel (undo menambah langkah,
  pause menghentikan timer & mengunci papan, menang mencatat hasil dan rekor, kalah di Target Langkah
  dan Lomba Waktu, AI menyelesaikan dan mencatat AI_SOLVED, batal AI membiarkan papan, hint, simpan &
  lanjutkan, Puzzle Harian, ganti kesulitan), Room (rekor tanpa AI/hint, simpanan, ABANDONED,
  pencapaian), validasi & impor gambar, serta UI end-to-end: membuka semua layar, memainkan 3×3 sampai
  menang dengan mengetuk tile (posisi dibaca dari deskripsi TalkBack), dan AI menyelesaikan Puzzle
  Harian 4×4 secara optimal.
- `lint` — **No issues found** (tanpa baseline, tanpa `tools:ignore`).
- `assembleRelease` — berhasil (R8 + shrinkResources), APK bertanda tangan diverifikasi `apksigner`.
- `assembleDebugAndroidTest` — terkompilasi; **belum dijalankan** (lihat bagian berikut).

## Benchmark solver

`./gradlew :domain:benchmark`, instans = acakan aplikasi dengan seed 1..N, JVM desktop (4 vCPU):

| Grid | Strategi | Instans | Selesai | Panjang rata-rata | Waktu median | Waktu maks | Iterasi rata-rata |
|---|---|---|---|---|---|---|---|
| 3x3 | A* optimal | 200 | 200 | 21,6 | 0 ms | 3 ms | 349 |
| 4x4 | IDA* optimal (anggaran 60 dtk) | 30 | 30 | 51,5 | 154 ms | 10.281 ms | 13.713.776 |
| 4x4 | Weighted A* (Solusi Cepat) | 100 | 100 | 55,4 | 7 ms | 496 ms | 21.174 |
| 5x5 | Bertahap (baris/kolom + A* 3×3) | 100 | 100 | 178,2 | 9 ms | 82 ms | 47.195 |

Keputusan per ukuran yang didukung angka di atas:
- **3×3**: A* optimal selalu selesai dalam milidetik.
- **4×4**: IDA* (memori O(kedalaman), linear conflict lewat tabel per baris/kolom sehingga O(1) per
  node) menyelesaikan mayoritas acakan dalam < 1 detik, tetapi ekornya panjang (maks 10 detik di
  desktop; di HP kelas menengah diperkirakan beberapa kali lebih lambat). Karena itu ada anggaran 20
  detik, lalu pemain memilih Lanjutkan (melanjutkan dari batas f terakhir, bukan dari awal), Batal,
  atau Solusi Cepat (weighted A*, w = 1,5 lalu dinaikkan bila batas 1,5 juta node tercapai). Rata-rata
  Solusi Cepat ±8 % lebih panjang dari optimal (55,4 vs 51,5; set instans berbeda).
- **5×5**: optimal tidak realistis di HP. Solver bertahap menempatkan baris atas lalu kolom kiri
  (dua tile terakhir tiap garis ditempatkan bersama) sampai tersisa 3×3, lalu A*. Selalu selesai
  < 0,1 detik, ±178 langkah, dan UI selalu memberi label "tidak dijamin terpendek".
- Klaim "optimal" hanya ditampilkan untuk A* dan IDA* yang selesai (bukan untuk Solusi Cepat/bertahap).

## Asumsi dan keputusan

- **Hitungan langkah**: setiap tile yang bergeser = 1 langkah (ketuk satu barisan berisi 3 tile = 3).
- **Undo** menambah hitungan langkah (bukan mengurangi), tersedia untuk 50 aksi terakhir, nonaktif
  saat AI berjalan; tumpukan undo dikosongkan saat AI mulai menjalankan solusi.
- **Hasil permainan**: `WON` (pemain mencapai posisi selesai; hint boleh), `AI_SOLVED` (tidak dihitung
  menang), `LOST` (tantangan gagal), `ABANDONED` (puzzle diganti setelah ada langkah). Persentase
  menang = WON / semua hasil.
- **Rekor**: hanya `WON` tanpa AI dan tanpa hint. Penanda AI/hint melekat pada puzzle tersebut, juga
  setelah Reset/Coba Lagi.
- **Timer** mulai pada langkah pertama, berhenti saat jeda dan saat AI berpikir/menjalankan solusi.
- **Mode tantangan** (ditentukan per ukuran grid): Target Langkah = batas 2 bintang (3×3: 40,
  4×4: 400, 5×5: 800; kalah saat langkah habis tanpa selesai). Lomba Waktu: 3×3 1:30, 4×4 6:00,
  5×5 15:00 (hitung mundur mulai di langkah pertama).
- **Puzzle Harian**: seed dari yyyyMMdd lokal → SplitMix64; selalu 4×4 angka. Hanya dihitung untuk
  streak bila diselesaikan pada tanggal yang sama. Streak saat ini dihitung mundur dari hari ini, atau
  dari kemarin bila hari ini belum dimainkan. Acak/ganti kesulitan/Upload dinonaktifkan di mode harian.
- **Simpan otomatis**: 1 detik setelah langkah, saat aplikasi ke latar (ON_STOP, sekaligus dijeda), dan
  saat meninggalkan layar. Permainan yang dilanjutkan muncul dalam keadaan Jeda bila disimpan saat jeda.
  Memulai puzzle baru lalu melangkah menandai simpanan lama sebagai ABANDONED.
- **Pencapaian "Gambar custom pertama"** terbuka saat gambar pertama berhasil ditambahkan.
- **Warna dinamis** default mati agar identitas biru tetap tampil; bisa dinyalakan di Android 12+.
- **Mode gambar** menampilkan nomor kecil di pojok tile (membantu dan untuk aksesibilitas).
- **Gambar dari kamera/galeri** dengan format lain (mis. HEIC) ditolak dengan pesan jelas, sesuai daftar
  format JPG/PNG/GIF/WebP.
- **Version catalog** ada di lokasi standar Gradle `gradle/libs.versions.toml`.
- **Ikon UI** ditulis sebagai `ImageVector` lokal dari path Material Icons (Apache 2.0) karena artefak
  `material-icons-*` dibekukan di 1.7.8 dan tidak lagi dirawat.
- **Haptik di Android < 8/12** memakai API `Vibrator` lama; tiga pemanggilan itu diberi
  `@Suppress("DEPRECATION")` karena berada di cabang versi API yang memang membutuhkannya.
- Versi dependensi diverifikasi terhadap repositori resmi pada 2026-11-14: AGP 9.5.1, Gradle 9.9.1,
  Kotlin 2.4.21, KSP 2.3.14, Compose BOM 2026.09.00, Room 2.8.5, DataStore 1.3.0,
  Navigation 2.10.2, Lifecycle 2.11.0, Coil 3.6.3, Robolectric 4.18.

## Yang belum bisa diverifikasi

- **Belum dijalankan di perangkat fisik atau emulator.** Lingkungan pengerjaan tidak punya KVM, jadi
  emulator tidak bisa berjalan. Semua test UI dijalankan di Robolectric (Android SDK 36 tersimulasi di
  JVM), bukan di API 34/37 sungguhan. `connectedDebugAndroidTest` sudah terkompilasi tetapi belum
  dijalankan.
- **APK hasil R8 belum dijalankan.** Build rilis berhasil dan tanda tangan valid, tetapi perilaku
  runtime setelah minify belum diuji di perangkat.
- **Performa 60 fps 5×5 dan Macrobenchmark** belum diukur di HP. Desain sudah diarahkan ke sana (posisi
  tile dibaca di fase layout lewat `Modifier.offset {}`, satu `Animatable` per tile, model domain
  ditandai stabil sehingga tick timer tidak me-recompose papan, satu bitmap untuk mode gambar), tetapi
  belum ada angka frame time.
- **Waktu solver di HP**: benchmark di atas dari JVM desktop; HP kelas menengah diperkirakan lebih
  lambat, sehingga lebih banyak acakan 4×4 akan mencapai anggaran 20 detik.
- Bunyi, getar, Photo Picker, kamera, bahasa per aplikasi, dan themed icon belum dicoba di perangkat
  nyata (logikanya diuji sejauh bisa di Robolectric).
