# 🧯 Tutorial: Membuat Database Firebase (Cloud Firestore) via CMD / CLI

Tutorial ini memandu membuat database **Firebase Cloud Firestore** untuk **Aplikasi 7 KAIH**
menggunakan **Firebase CLI** dari **Command Prompt (CMD) Windows** — tanpa perlu klik-klik
di konsol Firebase. Semua perintah bisa disalin-tempel langsung.

> Prasyarat: komputer Windows (atau Linux/Mac), koneksi internet, dan akun Google.

---

## 📋 Daftar Isi

1. [Instal Firebase CLI](#langkah-1--instal-firebase-cli)
2. [Login ke Firebase](#langkah-2--login-ke-firebase)
3. [Buat proyek Firebase](#langkah-3--buat-proyek-firebase)
4. [Buat database Firestore](#langkah-4--buat-database-firestore)
5. [Daftarkan aplikasi Android + unduh google-services.json](#langkah-5--daftarkan-aplikasi-android--unduh-google-servicesjson)
6. [Pasang aturan keamanan (firestore.rules)](#langkah-6--pasang-aturan-keamanan-firestorerules)
7. [Build ulang APK](#langkah-7--build-ulang-apk)
8. [Ringkasan semua perintah](#8--ringkasan-semua-perintah-copy-paste)
9. [Skrip otomatis setup-firebase.bat](#9--skrip-otomatis-setup-firebasebat)
10. [Troubleshooting](#10--troubleshooting)

---

## Langkah 1 — Instal Firebase CLI

### 1a. Pastikan Node.js terpasang

Firebase CLI berjalan di atas Node.js. Cek di CMD:

```cmd
node --version
npm --version
```

Jika belum ada, unduh **Node.js LTS** dari <https://nodejs.org> lalu instal (centang
"Add to PATH"). Tutup & buka ulang CMD, lalu cek lagi.

### 1b. Instal Firebase CLI (global)

```cmd
npm install -g firebase-tools
```

Cek hasilnya:

```cmd
firebase --version
```

Contoh keluaran: `13.30.0`

---

## Langkah 2 — Login ke Firebase

```cmd
firebase login
```

- Perintah ini membuka browser → pilih akun Google → klik **Allow**.
- Di CMD akan muncul `✔  Success! Logged in as anda@gmail.com`

> **Tidak ada browser?** (mis. server): pakai
> `firebase login --no-localhost` lalu buka URL yang diberikan dan tempel kode-nya.

Cek daftar proyek yang bisa diakses:

```cmd
firebase projects:list
```

---

## Langkah 3 — Buat proyek Firebase

Pilih **ID proyek** yang unik secara global (huruf kecil, angka, tanda hubung).
Contoh: `kaih-sdn-semambung`.

```cmd
firebase projects:create kaih-sdn-semambung --display-name "Aplikasi 7 KAIH"
```

Contoh keluaran:

```
✔ Creating Google Cloud project...
✔ Adding Firebase resources to Google Cloud project...
✔  Your Firebase project kaih-sdn-semambung is ready!
```

> ID proyek tidak bisa diubah setelah dibuat. Jika ID sudah dipakai orang lain,
> gunakan ID lain (mis. `kaih-sdn-semambung-2026`).

---

## Langkah 4 — Buat database Firestore

### 4a. Lihat lokasi (region) yang tersedia

```cmd
firebase firestore:locations
```

Untuk Indonesia, gunakan **`asia-southeast2` (Jakarta)** — paling dekat & cepat.

### 4b. Buat database

```cmd
firebase firestore:databases:create "(default)" --location=asia-southeast2 --project kaih-sdn-semambung
```

> Tanda kutip di sekitar `(default)` wajib di CMD (kurung adalah karakter khusus cmd).

Contoh keluaran:

```
✔ Creating Firestore database
✔  Successfully created Firestore database: (default) at asia-southeast2
```

Periksa database yang sudah dibuat:

```cmd
firebase firestore:databases:list --project kaih-sdn-semambung
```

> 💡 **Soal paket/pembayaran:** Cloud Firestore tersedia di paket gratis **Spark**
> dengan kuota terbatas (cukup untuk sekolah). Jika muncul pesan yang meminta
> mengaktifkan penagihan (Blaze), buka <https://console.firebase.google.com> →
> proyek Anda → **Upgrade** → pilih **Blaze (bayar sesuai pemakaian)**. Tanpa dipakai,
> biayanya Rp0.

---

## Langkah 5 — Daftarkan aplikasi Android + unduh google-services.json

### 5a. Daftarkan aplikasi Android

Package aplikasi 7 KAIH: **`com.sdn.semambung.kaih`**

```cmd
firebase apps:create android com.sdn.semambung.kaih --project kaih-sdn-semambung
```

Contoh keluaran (catat **App ID**-nya!):

```
✔ Creating Firebase Android app...
✔  Created new Android app with package name com.sdn.semambung.kaih, app id 1:1234567890:android:abc123def456
```

### 5b. Unduh google-services.json

Ganti `APP_ID` dengan hasil di atas (contoh: `1:1234567890:android:abc123def456`):

```cmd
firebase apps:sdkconfig android 1:1234567890:android:abc123def456 --project kaih-sdn-semambung > google-services.json
```

Perintah di atas menyimpan konfigurasi ke berkas `google-services.json` di folder
CMD aktif. Pastikan isinya benar:

```cmd
type google-services.json
```

Harus berisi `project_id: "kaih-sdn-semambung"` dan
`package_name: "com.sdn.semambung.kaih"`.

### 5c. Salin ke folder proyek Android

Masuk ke folder `android/` proyek ini, lalu:

```cmd
cd C:\path\ke\7kaih\android
copy C:\path\google-services.json app\google-services.json
```

> Berkas ini **menimpa** `app/google-services.json` contoh yang ada sekarang.

Daftar aplikasi yang terdaftar (opsional, untuk cek):

```cmd
firebase apps:list --project kaih-sdn-semambung
```

---

## Langkah 6 — Pasang aturan keamanan (firestore.rules)

Aplikasi 7 KAIH memakai *login aplikasi* (kredensial dicocokkan di aplikasi, sama
seperti versi web), sehingga aturan demo dibuka untuk baca-tulis. Repositori ini
sudah menyediakan `firestore.rules`.

### Jalur A — Non-interaktif (disarankan)

Berkas `firebase.json` dan `firestore.indexes.json` sudah tersedia di folder
`android/`. Dari folder `android/`:

```cmd
firebase deploy --only firestore:rules --project kaih-sdn-semambung
```

Contoh keluaran:

```
✔  Deploy complete!
```

### Jalur B — Interaktif (firebase init)

```cmd
firebase init firestore --project kaih-sdn-semambung
```

Jawab prompt:
- *What file should be used for Firestore Rules?* → tekan **Enter** (`firestore.rules`)
- *What file should be used for Firestore indexes?* → tekan **Enter** (`firestore.indexes.json`)
- *Do you want to edit the rules?* → **No** (berkas dari repo sudah benar)

Lalu deploy:

```cmd
firebase deploy --only firestore:rules --project kaih-sdn-semambung
```

> ⚠️ **Isi `firestore.rules`** (sudah ada di repo):
> ```
> rules_version = '2';
> service cloud.firestore {
>   match /databases/{database}/documents {
>     match /{document=**} { allow read, write: if true; }
>   }
> }
> ```
> Untuk produksi sungguhan, sebaiknya dipersempit (mis. wajib login Firebase
> Authentication) — lihat komentar di berkas `firestore.rules`.

---

## Langkah 7 — Build ulang APK

Setelah `google-services.json` asli terpasang, APK harus dibangun ulang agar memuat
konfigurasi Firebase yang benar.

**Lewat Android Studio (disarankan):**
1. Buka folder `android/` dengan Android Studio.
2. Menu **Build → Generate Signed Bundle / APK… → APK**.
3. Pilih keystore `kaih-release.keystore` (alias `wabcraft`, sandi `wabcraft2026`).
4. Hasil: `app-release.apk` → instal ke HP.

**Catatan:** APK lama (`KAIH-1.0-release-signed.apk`) masih bisa dipasang dan dibuka,
tetapi baru tersambung ke Firestore setelah build ulang di atas.

**Uji:** buka aplikasi → layar masuk muncul → daftar murid otomatis terisi (aplikasi
membuat data awal bila koleksi `pengguna` kosong). Cek di konsol Firebase →
**Firestore Database** → koleksi `pengguna` & `jurnal_harian` akan muncul.

---

## 8 — Ringkasan semua perintah (copy-paste)

```cmd
:: 1. Instal & login
npm install -g firebase-tools
firebase login

:: 2. Buat proyek (ganti ID sesuai keinginan)
firebase projects:create kaih-sdn-semambung --display-name "Aplikasi 7 KAIH"

:: 3. Buat database Firestore (Jakarta)
firebase firestore:databases:create "(default)" --location=asia-southeast2 --project kaih-sdn-semambung

:: 4. Daftarkan aplikasi Android
firebase apps:create android com.sdn.semambung.kaih --project kaih-sdn-semambung

:: 5. Unduh google-services.json (ganti APP_ID dari hasil langkah 4)
firebase apps:sdkconfig android APP_ID --project kaih-sdn-semambung > google-services.json
copy google-services.json app\google-services.json

:: 6. Deploy aturan (jalankan dari folder android/)
firebase deploy --only firestore:rules --project kaih-sdn-semambung
```

---

## 9 — Skrip otomatis setup-firebase.bat

Repositori menyertakan **`setup-firebase.bat`** — tinggal klik dua kali, jawab
beberapa pertanyaan, dan semua langkah di atas dijalankan otomatis satu per satu.

```cmd
setup-firebase.bat
```

Yang ditanyakan skrip:
1. **ID proyek** (contoh: `kaih-sdn-semambung`)
2. **Region** (Enter = `asia-southeast2`)
3. **App ID** hasil `apps:create` (akan diminta setelah langkah itu)

---

## 10 — Troubleshooting

| Masalah | Solusi |
|---|---|
| `Not in a Firebase app directory (could not locate firebase.json)` | Berkas `firebase.json` belum ada di folder `android/`. **Cara cepat** (jalankan dari folder `android/`):<br>`echo {"firestore": {"rules": "firestore.rules", "indexes": "firestore.indexes.json"}} > firebase.json`<br>lalu ulangi `firebase deploy --only firestore:rules`. Atau unduh ulang ZIP dari tag `v1.0.0` yang sudah diperbarui (sudah berisi `firebase.json`). Alternatif lain: `firebase init firestore` lalu jawab prompt (atur *rules file* = `firestore.rules`, *indexes file* = `firestore.indexes.json`). |
| `Assertion failed: !(handle->flags & UV_HANDLE_CLOSING), file src\win\async.c, line 94` | Bug kosmetik firebase-tools di Windows saat output di-redirect dengan `>`. **Unduhan biasanya TETAP SUKSES.** Cek hasilnya:<br>`dir google-services.json` dan `findstr /C:"project_id" google-services.json`<br>Jika string muncul → file valid, abaikan pesan error. Jika file kosong → jalankan tanpa redirect lalu salin manual, atau unduh dari konsol: <https://console.firebase.google.com/project/NAMA-PROYEK/settings/general> → aplikasi Android → tombol **google-services.json**. |
|---|---|
| `firebase: not recognized...` | Node.js belum di PATH atau instal ulang: `npm install -g firebase-tools`. Coba `npx firebase --version`. |
| `project already exists` | ID proyek sudah dipakai → pilih ID lain. |
| `PERMISSION_DENIED` saat deploy | Login ulang (`firebase login --reauth`) atau pastikan `--project` benar. |
| Diminta aktifkan penagihan (Blaze) | Buka Firebase Console → Upgrade ke Blaze (gratis jika kuota tidak terpakai). |
| `API rate limit exceeded` | Tunggu sebentar, lalu ulangi. |
| `(default)` error di CMD | Pastikan pakai tanda kutip: `"(default)"`. |
| Login di server tanpa browser | `firebase login --no-localhost` |
| Aplikasi tidak tersambung | Pastikan `google-services.json` sudah diganti & APK di-build ulang. Cek juga `app/build.gradle.kts` memuat plugin `com.google.gms.google-services` (sudah ada). |
| Data tidak muncul di konsol | Buka aplikasi dulu sekali — data awal (akun demo + 22 murid) dibuat otomatis saat koleksi `pengguna` kosong. |

---

Dibuat untuk **Aplikasi 7 KAIH** — Aguk Rudianto · Wabcraft@2026
