# 7 KAIH — Aplikasi 7 Kebiasaan Anak Indonesia Hebat

Repositori ini berisi dua versi aplikasi:

| Folder | Versi | Teknologi |
|---|---|---|
| `index.php` + `data_kaih.sqlite` | Versi **web** (asli) | PHP 8 + SQLite |
| [`android/`](android/README.md) | Versi **Android** (baru) | Kotlin + Jetpack Compose + **Firebase Firestore** |

## Versi Android

- Fitur dan fungsi **sama** dengan versi web: login 3 peran (murid/guru/kepala sekolah),
  jurnal harian 7 kebiasaan, kalender pengisian, rekap bulanan & semester (+ Cetak PDF),
  admin persetujuan jurnal, serta backup/restore/unggah CSV khusus guru.
- Database memakai **Firebase Cloud Firestore** (bukan SQLite).
- Menu tab **atas** pada versi web diubah menjadi **tab bawah layar**.

➡️ Petunjuk lengkap setup Firebase & cara menjalankan: **[android/README.md](android/README.md)**
