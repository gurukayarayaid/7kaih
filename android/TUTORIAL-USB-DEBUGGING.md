# 🔌 Tutorial USB Debugging — Ambil Log Crash Aplikasi 7 KAIH

Panduan langkah-demi-langkah untuk mengaktifkan **USB Debugging** di HP Android,
menghubungkan ke komputer Windows, dan mengambil **log error (logcat)** saat aplikasi
crash — supaya penyebabnya bisa diketahui pasti.

> ⏱️ Total: ±10 menit. Yang dibutuhkan: HP Android + kabel USB + komputer Windows.

---

## Langkah 1 — Aktifkan Opsi Pengembang (Developer Options) di HP

1. Buka **Pengaturan (Settings)** di HP.
2. Gulir ke bawah → **Tentang Ponsel (About phone)**.
3. Cari **Nomor Build (Build number)**.
4. **Ketuk "Nomor Build" 7 kali berturut-turut** → muncul notifikasi
   *"You are now a developer!"* (Anda sekarang pengembang).
   - Jika diminta PIN/pola, masukkan.
5. Kembali ke Pengaturan → sekarang muncul menu **Opsi Pengembang (Developer options)**.

## Langkah 2 — Aktifkan USB Debugging

1. Buka **Pengaturan → Opsi Pengembang (Developer options)**.
2. Nyalakan **USB debugging**.
3. (Jika ada) nyalakan juga **"USB debugging (Security settings)"** dan setujui.
4. Bila muncul dialog *"Allow USB debugging?"* → centang **Always allow from this computer** → **OK**.

## Langkah 3 — Siapkan ADB di Windows

**Cara A — Pakai ADB dari Android Studio (jika sudah terpasang):**
Buka folder (di File Explorer):
```
C:\Users\peran\AppData\Local\Android\Sdk\platform-tools
```
Di dalamnya ada `adb.exe`. Buka CMD di folder itu (klik kanan → **Open in Terminal**).

**Cara B — Unduh ADB saja (tanpa Android Studio):**
1. Buka <https://developer.android.com/tools/releases/platform-tools> di Chrome.
2. Unduh **Windows** zip → ekstrak ke `C:\adb`.
3. Buka CMD, lalu:
   ```cmd
   cd C:\adb
   ```

## Langkah 4 — Hubungkan HP & Cek Koneksi

1. Colokkan HP ke komputer via **kabel USB**.
2. Di notifikasi HP, pilih mode **"File Transfer (MTP)"** (bukan "Charging only").
3. Di CMD, jalankan:
   ```cmd
   adb devices
   ```
4. Hasil yang benar:
   ```
   List of devices attached
   R58N1234567    device
   ```
   - ✅ `device` = siap dipakai.
   - ⚠️ `unauthorized` = setujui dialog di layar HP.
   - ⚠️ kosong = ganti kabel/port, atau aktifkan MTP.

## Langkah 5 — Ambil Log Saat Aplikasi Crash

**Cara cepat — pakai skrip otomatis `ambil-logcat.bat`** (di folder `android/`):
1. Klik dua kali `ambil-logcat.bat`.
2. Ikuti petunjuknya (akan bertanya kapan Anda membuka aplikasi).
3. Hasilnya: `crash_log_error.txt` + `crash_log_full.txt` + `crash_log_aplikasi.txt` di folder yang sama.

**Atau manual — ketik perintah ini satu per satu:**

```cmd
:: 1. Bersihkan log lama
adb logcat -c

:: 2. (Sekarang buka aplikasi 7 KAIH di HP sampai crash)

:: 3. Ambil log error saja
adb logcat -d -v time *:E > crash_log_error.txt

:: 4. Ambil log lengkap
adb logcat -d -v time > crash_log_full.txt

:: 5. Ambil file crash_log.txt internal aplikasi (bila CrashActivity sempat menulisnya)
adb shell cat /data/data/com.sdn.semambung.kaih/files/crash_log.txt > crash_log_aplikasi.txt
```

## Langkah 6 — Kirim Log ke Pengembang

Buka `crash_log_error.txt` dengan Notepad, lalu kirim:
- **Isi lengkap file** (atau minimal 30–50 baris pertama), ATAU
- **Foto layar** hasil `adb logcat` yang berisi baris:
  ```
  FATAL EXCEPTION: main
  java.lang.RuntimeException: ...
  ```

> Yang paling penting dicari: blok **`FATAL EXCEPTION`** + **`Caused by:`** — di sanalah
> nama kelas & baris yang gagal.

---

## 🆘 Alternatif Tanpa USB — Foto Layar Error

APK versi terbaru (v1.0.1 build 5) sudah punya **penampil error di layar**:
- Kalau aplikasi crash → muncul **halaman merah berisi pesan error** (bukan langsung tertutup).
- **Foto halaman itu** dan kirimkan — itu sudah cukup untuk analisis.

---

## Troubleshooting ADB

| Masalah | Solusi |
|---|---|
| `adb: command not found` | Jalankan dari folder `platform-tools` / `C:\adb`, atau tambahkan ke PATH. |
| `unauthorized` di `adb devices` | Di HP muncul dialog "Allow USB debugging?" → centang Always allow → OK. |
| Perangkat tidak terdeteksi | Ganti kabel USB (harus yang bisa data, bukan cuma charge), colok ke port USB langsung (bukan hub), pilih mode MTP. |
| Driver tidak dikenal (HP China) | Instal driver dari situs merek HP, atau coba `adb kill-server` lalu `adb start-server`. |
| `adb devices` kosong | Pastikan USB debugging benar-benar ON, dan layar HP tidak terkunci. |

---

Dibuat untuk **Aplikasi 7 KAIH** — Aguk Rudianto · Wabcraft@2026
