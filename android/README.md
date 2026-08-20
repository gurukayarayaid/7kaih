# 📱 Aplikasi 7 KAIH — Versi Android

Versi Android dari **Aplikasi 7 KAIH** (SD Negeri Semambung · Kelas III), dibuat dengan
**Kotlin + Jetpack Compose** dan **Firebase Cloud Firestore** sebagai pengganti SQLite.
Menu tab atas pada versi web **telah diubah menjadi menu tab bawah layar**.

Fitur dan fungsi sama dengan versi web PHP (`index.php`):

| Fitur | Murid | Guru | Kepala Sekolah |
|---|---|---|---|
| 🗓 Jurnal Harian 7 Kebiasaan | ✅ | ✅ | ✅ |
| 📅 Kalender lihat pengisian jurnal | ✅ | – | – |
| ▦ Rekap Bulanan (+ Cetak PDF) | – | ✅ | ✅ |
| ◫ Rekap Semester (+ Cetak PDF) | – | ✅ | ✅ |
| ✓ Admin: dashboard & persetujuan jurnal | – | ✅ | ✅ |
| ⚙ Data: backup, restore, unggah CSV, kosongkan | – | ✅ | – |
| 👤 Profil & Keluar | ✅ | (ikona keluar di atas) | (ikona keluar di atas) |

---

## 🚀 APK Siap Pakai

**`KAIH-1.0-release-signed.apk`** (di folder ini) sudah merupakan APK final yang
**ditandatangani** dengan sertifikat:

```
CN=Aguk Rudianto, OU=Development, O=Perusahaan Wabcraft, L=Kraksaan, ST=Jawa Timur, C=ID
```

- Package : `com.sdn.semambung.kaih`
- Versi   : 1.0 (versionCode 1)
- Android : minimal 8.0 (API 26), target 14 (API 34)
- Skema tanda tangan: v1 + v2/v3
- Keystore: `kaih-release.keystore` (alias `wabcraft`, kata sandi `wabcraft2026`)
  — simpan baik-baik; keystore inilah identitas penandatangan aplikasi.

### 📥 Link Unduhan Langsung (GitHub)

| Berkas | Link |
|---|---|
| **APK** (79,7 MB) | https://github.com/gurukayarayaid/7kaih/raw/v1.0.0/android/KAIH-1.0-release-signed.apk |
| Keystore | https://github.com/gurukayarayaid/7kaih/raw/v1.0.0/android/kaih-release.keystore |
| Halaman rilis | https://github.com/gurukayarayaid/7kaih/releases/tag/v1.0.0 |

> ⚠️ **PENTING — Konfigurasi Firebase:** APK ini dibangun dengan `google-services.json`
> contoh/placeholder, jadi aplikasi **terpasang dan berjalan**, tetapi koneksi ke
> Cloud Firestore baru aktif setelah Anda mengganti `app/google-services.json` dengan
> berkas dari proyek Firebase Anda sendiri lalu **membangun ulang** (lihat bagian 2 & 7).

Cara pasang: salin APK ke HP Android → ketuk berkasnya → izinkan "instal dari sumber
tidak dikenal" bila diminta.

---

## 1. Struktur Proyek

```
android/
├── app/
│   ├── google-services.json      ← GANTI dengan berkas dari konsol Firebase Anda!
│   └── src/main/java/com/sdn/semambung/kaih/
│       ├── MainActivity.kt            # Titik masuk aplikasi
│       ├── data/
│       │   ├── Model.kt               # Model Pengguna & JurnalHarian (padanan tabel SQLite)
│       │   ├── FirestoreRepo.kt       # Lapisan database Firestore (query, seed, backup)
│       │   └── RekapLogic.kt          # Logika rekap bulanan/semester & data admin
│       ├── ui/
│       │   ├── KaihViewModel.kt       # State aplikasi, sesi login, tab bawah
│       │   ├── LoginScreen.kt         # Layar masuk (Murid/Guru/Kepala Sekolah)
│       │   ├── MainScreen.kt          # Kerangka utama + NAVIGASI TAB BAWAH
│       │   ├── JurnalScreen.kt        # Formulir 7 kebiasaan
│       │   ├── KalenderScreen.kt      # Kalender pengisian jurnal (murid)
│       │   ├── RekapBulananScreen.kt  # Rekap bulanan + statistik + PDF
│       │   ├── RekapSemesterScreen.kt # Rekap semester + PDF
│       │   ├── AdminScreen.kt         # Dashboard + persetujuan jurnal
│       │   ├── DataScreen.kt          # Backup/restore/CSV/kosongkan (guru)
│       │   ├── ProfilScreen.kt        # Profil murid
│       │   └── Components.kt          # Komponen UI bersama
│       └── util/
│           ├── Format.kt              # Tanggal Indonesia (Asia/Jakarta)
│           ├── Csv.kt                 # Parser CSV
│           ├── Pesan.kt               # Pesan error Firebase yang ramah
│           └── RecapPdf.kt            # Pembuat PDF rekap (A4)
├── firestore.rules                    # Contoh aturan keamanan Firestore
└── build.gradle.kts, settings.gradle.kts, ...
```

---

## 2. Setup Firebase (wajib, sekali saja)

Aplikasi ini **belum tersambung ke Firebase** sampai Anda melakukan langkah berikut:

1. **Buat proyek Firebase**
   - Buka <https://console.firebase.google.com> → **Tambah proyek** (misal: `kaih`).

2. **Daftarkan aplikasi Android**
   - Klik ikon **Android** ➜ package name: `com.sdn.semambung.kaih`
   - Unduh **google-services.json** ➜ letakkan di folder `app/`
     (menimpa berkas `app/google-services.json` contoh yang ada sekarang).

3. **Aktifkan Cloud Firestore**
   - Firebase Console → **Build → Firestore Database** → **Buat database**
   - Pilih lokasi terdekat (mis. `asia-southeast2` Jakarta) → mode **Produksi** atau **Mode uji coba**
     (mode uji coba paling mudah untuk pengembangan awal).

4. **Pasang aturan keamanan** (opsional, disarankan)
   - Salin isi `firestore.rules` ke tab **Rules** di halaman Firestore Database, lalu **Publish**.
   - ⚠️ Aplikasi ini memakai *login aplikasi* (kredensial dicocokkan di aplikasi, sama seperti
     versi web PHP), sehingga aturan contoh dibuka untuk demo. Untuk produksi nyata,
     gunakan Firebase Authentication dan persempit aturan (lihat komentar di `firestore.rules`).

5. **Jalankan aplikasi**
   - Buka folder `android/` ini dengan **Android Studio** (Ladybug atau lebih baru, JDK 17).
   - Tunggu Gradle Sync selesai (butuh koneksi internet saat pertama kali mengunduh dependensi).
   - Tekan **Run ▶** di emulator atau HP Android (minimal Android 8.0 / API 26).

> **Catatan Gradle wrapper:** folder ini berisi `gradle-wrapper.properties` tetapi tanpa
> `gradlew`/`gradle-wrapper.jar` biner. Jika Android Studio menanyakan konfigurasi Gradle,
> pilih **"Use Gradle from: 'gradle-wrapper.properties' file"** atau biarkan Android Studio
> membuatkan wrapper-nya. Alternatif: jalankan `gradle wrapper --gradle-version 8.7` di folder
> `android/` bila Gradle terpasang di komputer Anda.

---

## 3. Akun Demo (dibuat otomatis saat pertama kali dibuka)

Bila koleksi `pengguna` masih kosong, aplikasi mengisi data awal persis seperti versi web:

| Peran | Cara masuk | Kata sandi |
|---|---|---|
| Murid | pilih nama dari daftar | **NIS** (mis. `3262`) |
| Guru | langsung | `alal` |
| Kepala Sekolah | username `kepsek` | `123456` |

Murid Kelas III yang tersedia: NIS 3262–3282 dan 3317 (22 murid), kata sandi = NIS masing-masing.

---

## 4. Struktur Database Firestore

| Koleksi | Dokumen | Keterangan |
|---|---|---|
| `pengguna` | `"1"`, `"2"`, … (atau ID otomatis hasil CSV) | Akun murid/guru/kepala sekolah. Kolom: `nama`, `username`, `kata_sandi`, `peran`, `kelas`, `nis`, `dibuat_pada` |
| `jurnal_harian` | `"{idMurid}_{tanggal}"` (mis. `4_2026-08-19`) | Satu dokumen per murid per tanggal. 21 kolom kebiasaan + `status_persetujuan`, `catatan_guru`, `paraf_orang_tua`, `diperbarui_pada` |
| `pengaturan` | kunci | Dicadangkan untuk pengaturan global |

Kolom jurnal sama persis dengan tabel `jurnal_harian` versi web: `bangun_pagi`, `jam_bangun`,
`beribadah`, `ibadah_lainnya`, `mengaji_alquran`, `makan_pagi`, `makan_siang`, `makan_malam`,
`bergizi`, `gemar_belajar`, `jam_belajar`, `buku_dibaca`, `informasi_buku`, `berolahraga`,
`jam_olahraga`, `jenis_olahraga`, `bermasyarakat`, `jam_bermasyarakat`, `kegiatan_masyarakat`,
`tidur_cepat`, `jam_tidur`.

---

## 5. Perbedaan Teknis vs Versi Web (PHP + SQLite)

| Versi web | Versi Android |
|---|---|
| SQLite (`data_kaih.sqlite`) | **Cloud Firestore** (Firebase) |
| Menu tab di **atas** layar | Menu tab di **bawah** layar (NavigationBar Material 3) |
| Backup = unduh berkas `.sqlite` | Backup = unduh **JSON** seluruh koleksi |
| Restore = unggah `.sqlite` | Restore = pilih berkas **JSON** (data lama diganti) |
| Unggah CSV murid | Sama, pilih berkas CSV dari penyimpanan |
| Cetak PDF via browser | Cetak PDF via pembuat PDF bawaan (A4, disimpan lewat dialog "Simpan") |

**Aturan bisnis tetap identik:**
- Murid tidak bisa mengisi jurnal tanggal masa depan.
- Jurnal berstatus **Disetujui** tidak bisa diubah murid; menyimpan ulang mengembalikan status ke **Menunggu**.
- Rekap bulanan tuntas bila 7 kebiasaan terisi **≥ 80%** hari dalam bulan.
- Rekap semester tuntas bila **semua** hari dalam semester terisi lengkap.
- Paraf orang tua menyimpan waktu `dd/MM/yyyy HH:mm` (zona Asia/Jakarta).

---

## 6. Troubleshooting

| Masalah | Solusi |
|---|---|
| `Firebase belum dikonfigurasi…` | `app/google-services.json` masih contoh. Ikuti langkah 2 di atas. |
| `Akses ditolak…` | Aturan Firestore belum dibuka / belum di-publish. Salin `firestore.rules`. |
| `Tidak dapat terhubung…` | Cek koneksi internet perangkat; pastikan Firestore Database sudah dibuat di konsol. |
| Login gagal meski akun benar | Pastikan data awal sudah dibuat (buka aplikasi sekali, lalu cek koleksi `pengguna` di konsol). |
| Gradle sync error | Pastikan Android Studio versi terbaru, JDK 17, dan koneksi internet stabil saat sync pertama. |

---

## 7. Membangun Ulang APK (Tanpa Android Studio)

Repositori ini menyertakan **`build-apk-offline.sh`** — skrip build mandiri (tanpa Gradle)
yang dipakai untuk menghasilkan APK yang sudah jadi. Skrip ini membutuhkan: JDK, Kotlin
compiler, Android SDK `android.jar` API 34, `aapt2`, `d8/r8`, `apksig`, dan jar dependensi
(lengkapnya ada di dalam skrip). Jalankan:

```bash
bash build-apk-offline.sh
```

Hasil: `KAIH-1.0-release-signed.apk` yang ditandatangani dengan
`kaih-release.keystore` (CN=Aguk Rudianto, O=Perusahaan Wabcraft).

> Untuk pengembangan normal, tetap disarankan membangun lewat Android Studio
> (lihat bagian 2–6 di atas). Skrip offline ini adalah cadangan bila tidak ada
> akses internet/Gradle.

---

Dibuat dari `index.php` (Aplikasi 7 KAIH — Aguk Rudianto, Wabcraft@2026).
