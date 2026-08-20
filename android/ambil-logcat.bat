@echo off
chcp 65001 >nul
title USB Debugging - Ambil Log Crash Aplikasi 7 KAIH
color 1F
echo ============================================================
echo   USB DEBUGGING - AMBIL LOG CRASH APLIKASI 7 KAIH
echo ============================================================
echo.
echo [1/5] Memeriksa koneksi ADB...
adb devices
echo.
echo   Jika daftar perangkat KOSONG:
echo     - Pastikan USB debugging AKTIF di HP (lihat tutorial)
echo     - Pilih mode "File Transfer (MTP)" di notifikasi HP
echo     - Setujui dialog "Allow USB debugging?" di HP
echo     - Coba ganti kabel / colok ke port USB lain
echo.
pause

echo [2/5] Menghapus log lama...
adb logcat -c
echo   Log lama dibersihkan.
echo.
echo [3/5] SEKARANG buka aplikasi 7 KAIH di HP
echo   sampai muncul error / crash / layar merah.
echo.
echo   Setelah itu, tekan tombol apa saja di sini...
pause

echo [4/5] Mengambil log crash...
adb logcat -d -v time *:E > crash_log_error.txt
adb logcat -d -v time > crash_log_full.txt
echo   - crash_log_error.txt (log error saja)
echo   - crash_log_full.txt (log lengkap)
echo.
echo [5/5] Mengambil crash_log.txt internal aplikasi...
adb shell cat /data/data/com.sdn.semambung.kaih/files/crash_log.txt > crash_log_aplikasi.txt 2>nul
echo.
echo ============================================================
echo   SELESAI!
echo.
echo   Kirimkan ke pengembang:
echo     1. crash_log_error.txt  (paling penting)
echo     2. crash_log_aplikasi.txt (bila ada isinya)
echo   atau foto layar error di HP.
echo ============================================================
pause
