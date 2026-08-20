# 🔨 Tutorial: Build Ulang APK di Android Studio
### (Generate Signed Bundle / APK — keystore `kaih-release.keystore`)

Panduan ini menjelaskan cara **membangun ulang APK** Aplikasi 7 KAIH setelah
`google-services.json` asli Anda dipasang, menggunakan **Android Studio**, dan
menandatanganinya dengan keystore **Perusahaan Wabcraft**.

---

## 📋 Prasyarat

| Kebutuhan | Keterangan |
|---|---|
| Android Studio | Versi **Hedgehog / Iguana / Ladybug** atau lebih baru (disarankan versi terbaru) |
| JDK | **JDK 17** — sudah otomatis tersedia di dalam Android Studio (JBR) |
| Internet | Diperlukan saat *Gradle Sync* pertama (mengunduh dependensi dari `google()` dan Maven Central) |
| Folder proyek | `android/` dari ZIP terbaru (tag `v1.0.0`) |
| `google-services.json` **asli** | Sudah berada di `android\app\google-services.json` (hasil langkah Firebase sebelumnya) |
| Keystore | `android\kaih-release.keystore` (sudah ada di folder proyek) |

> ⚠️ Jika `google-services.json` masih yang contoh (berisi `kaih-android-placeholder`),
> build akan tetap berhasil, tetapi aplikasi **tidak** tersambung ke Firestore.
> Pastikan file asli sudah menimpa file contoh: cek isinya mengandung
> `"project_id": "kaih-sdn-semambung"`.

---

## Langkah 1 — Buka proyek di Android Studio

1. Jalankan **Android Studio**.
2. Klik **File → Open…** (atau **Open** di layar selamat datang).
3. Pilih folder **`android`** (folder yang berisi `build.gradle.kts`, `app`, `settings.gradle.kts`)
   → klik **OK**.
4. Jika muncul dialog *"Gradle wrapper not found"* atau *"gradle-wrapper.jar is missing"*:
   klik **OK / Create wrapper** — Android Studio akan membuatnya otomatis.
5. Tunggu sampai **Gradle Sync** selesai (indikator di pojok kanan bawah).
   - Pertama kali bisa memakan waktu beberapa menit (mengunduh Gradle 8.7 + dependensi).
   - Bila diminta **trust project**: centang *Trust project* → **Trust Project**.

### Jika Sync error: SDK tidak ditemukan

**File → Project Structure → SDK Location** → isi lokasi Android SDK
(biasanya `C:\Users\<nama>\AppData\Local\Android\Sdk`) → **OK** → **Sync Now**.

### Jika Sync error: versi JDK

**File → Settings → Build, Execution, Deployment → Build Tools → Gradle →
Gradle JDK** → pilih **17** (atau *jbr-17*) → **Apply**.

---

## Langkah 2 — Generate Signed APK

1. Klik menu **Build → Generate Signed Bundle / APK…**
2. Pilih **APK** → klik **Next**.
3. Isi form **Key store path**:

| Kolom | Isi |
|---|---|
| **Key store path** | Klik *Choose existing keystore* → pilih `android\kaih-release.keystore` |
| **Key store password** | `wabcraft2026` |
| **Key alias** | `wabcraft` |
| **Key password** | `wabcraft2026` |

   ✅ Centang **Remember passwords** (biar tidak perlu mengetik ulang).
   Klik **Next**.

   > 💡 *Key store* dan *Key password* kebetulan sama (`wabcraft2026`), dan alias-nya
   > `wabcraft` — ini sengaja dibuat sederhana untuk keperluan sekolah.
   > Simpan keystore di tempat aman; **jangan hilangkan** karena untuk memperbarui
   > aplikasi di HP, tanda tangan harus berasal dari keystore yang sama.

4. Pilih **release** pada *build variant*.
5. Pastikan **kedua kotak centang** aktif:
   - ☑ **V1 (Jar Signature)** — untuk Android 7.0 ke bawah
   - ☑ **V2 (Full APK Signature)** — untuk Android 7.0+ (mayoritas HP sekarang)
6. Klik **Create** → tunggu sampai muncul notifikasi **"APK(s) generated successfully"** →
   klik **locate in Explorer** (atau lihat di panel *Build*).

---

## Langkah 3 — Ambil APK hasil build

File APK ada di:

```
android\app\build\outputs\apk\release\app-release.apk
```

- File ini sudah **ditandatangani** dengan keystore Wabcraft.
- Anda bebas mengganti namanya (misal `KAIH-1.1-release.apk`) sebelum dikirim ke HP.

### Kirim & pasang ke HP

- **Kabel USB**: aktifkan *USB debugging* → jalankan di CMD:
  ```cmd
  adb install -r app-release.apk
  ```
  (adb ada di `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`)
- **Tanpa kabel**: kirim APK via WhatsApp/Drive/Bluetooth → buka di HP → izinkan
  *"instal dari sumber tidak dikenal"*.

---

## Langkah 4 — Verifikasi tanda tangan (opsional)

Buka CMD di folder yang berisi `app-release.apk`:

```cmd
keytool -printcert -jarfile app-release.apk
```

Harus menampilkan:

```
Owner: CN=Aguk Rudianto, OU=Development, O=Perusahaan Wabcraft, L=Kraksaan, ST=Jawa Timur, C=ID
```

---

## 🖥️ Alternatif: Build dari CMD (tanpa klik-klik)

Proyek sudah dilengkapi **Gradle wrapper** (`gradlew.bat`), jadi bisa dibangun dari
Command Prompt:

```cmd
cd C:\Users\peran\Downloads\7kaih-1.0.0\android
gradlew.bat assembleRelease
```

Agar hasilnya **ditandatangani otomatis** (tanpa wizard), tambahkan konfigurasi
signing ke `app\build.gradle.kts`:

1. Buat berkas **`keystore.properties`** di folder `android\`:
   ```properties
   storeFile=kaih-release.keystore
   storePassword=wabcraft2026
   keyAlias=wabcraft
   keyPassword=wabcraft2026
   ```

2. Edit **`app\build.gradle.kts`** — tambahkan di dalam blok `android { ... }`:
   ```kotlin
   import java.util.Properties
   import java.io.FileInputStream

   // (letakkan di paling atas file, sebelum blok plugins)

   // di dalam blok android { }:
   val keystoreProps = Properties().apply {
       val f = rootProject.file("keystore.properties")
       if (f.exists()) load(FileInputStream(f))
   }

   signingConfigs {
       create("release") {
           if (keystoreProps.isNotEmpty()) {
               storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
               storePassword = keystoreProps.getProperty("storePassword")
               keyAlias = keystoreProps.getProperty("keyAlias")
               keyPassword = keystoreProps.getProperty("keyPassword")
           }
       }
   }

   buildTypes {
       release {
           isMinifyEnabled = false
           signingConfig = signingConfigs.getByName("release")
           proguardFiles(...)  // biarkan apa adanya
       }
   }
   ```

3. Jalankan lagi:
   ```cmd
   gradlew.bat assembleRelease
   ```
   Hasil: `app\build\outputs\apk\release\app-release.apk` — sudah ditandatangani.

> ⚠️ `keystore.properties` berisi kata sandi — **jangan di-commit** ke GitHub
> (tambahkan ke `.gitignore` bila perlu). Untuk keperluan sekolah, menyimpannya
> lokal saja sudah cukup.

---

## 🛠️ Troubleshooting

| Masalah | Solusi |
|---|---|
| **Gradle Sync gagal** | Pastikan internet aktif; coba **File → Sync Project with Gradle Files**. Cek juga *Gradle JDK* = 17. |
| **`Keystore was tampered with, or password was incorrect`** | Sandi/alias salah. Gunakan persis: sandi `wabcraft2026`, alias `wabcraft`. |
| **`File google-services.json is missing`** | Salin `google-services.json` asli ke `android\app\google-services.json` lalu Sync ulang. |
| **`SDK location not found`** | **File → Project Structure → SDK Location** → arahkan ke folder Android SDK. |
| **`Could not find com.android.tools.build:gradle:8.5.2`** | Koneksi ke `dl.google.com` diblokir (mis. jaringan sekolah). Gunakan jaringan lain atau VPN, atau pakai skrip offline `build-apk-offline.sh` (Linux/WSL). |
| **Hasil build `app-release-unsigned.apk`** | Build lewat menu *Generate Signed Bundle/APK*, atau tambahkan `signingConfigs` (lihat Alternatif CMD). |
| **Aplikasi terpasang tapi tidak terhubung Firestore** | `google-services.json` masih contoh → ulangi langkah Firebase lalu build ulang. |
| **APK lama tidak bisa di-update (INSTALL_FAILED_UPDATE_INCOMPATIBLE)** | Tanda tangan berbeda. Selalu pakai keystore `kaih-release.keystore` yang sama. Solusi sementara: hapus aplikasi lama lalu instal baru (data jurnal di Firestore tetap aman). |
| **Error Java version / `Unsupported class file major version`** | Pakai JDK 17 (bukan JDK 21+): *Settings → Build Tools → Gradle → Gradle JDK → 17*. |

---

## 📌 Catatan Penting

- **Keystore = identitas aplikasi.** Simpan `kaih-release.keystore` + sandi di tempat
  aman (mis. Google Drive sekolah). Kehilangan keystore = tidak bisa update aplikasi
  yang sudah terpasang (harus uninstall).
- **Data jurnal aman** di Firestore — build ulang & reinstall tidak menghapus data.
- APK hasil build mungkin berbeda ukuran dari `KAIH-1.0-release-signed.apk` (yang dibangun
  lewat skrip offline) — itu wajar karena jalur build berbeda.

---

Dibuat untuk **Aplikasi 7 KAIH** — Aguk Rudianto · Wabcraft@2026
