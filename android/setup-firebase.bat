@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Setup Firebase - Aplikasi 7 KAIH
color 1F

echo ============================================================
echo   SETUP FIREBASE CLOUD FIRESTORE - APLIKASI 7 KAIH
echo   (Aguk Rudianto / Perusahaan Wabcraft)
echo ============================================================
echo.

:: ---------- Cek prasyarat ----------
echo [1/7] Memeriksa prasyarat...
where node >nul 2>nul
if errorlevel 1 (
  echo   [GAGAL] Node.js tidak ditemukan.
  echo   Install Node.js LTS dari https://nodejs.org lalu ulangi.
  pause
  exit /b 1
)
where npm >nul 2>nul
if errorlevel 1 (
  echo   [GAGAL] npm tidak ditemukan.
  pause
  exit /b 1
)
where firebase >nul 2>nul
if errorlevel 1 (
  echo   firebase CLI belum terpasang, menginstal...
  call npm install -g firebase-tools
  if errorlevel 1 (
    echo   [GAGAL] Gagal menginstal firebase-tools.
    pause
    exit /b 1
  )
)
for /f "delims=" %%v in ('firebase --version 2^>nul') do set FIREBASE_VER=%%v
echo   OK - Node %NODE_VER% - Firebase CLI %FIREBASE_VER%
echo.

:: ---------- Login ----------
echo [2/7] Login ke Firebase (browser akan terbuka)...
call firebase login
if errorlevel 1 (
  echo   [GAGAL] Login gagal. Ulangi: firebase login
  pause
  exit /b 1
)
echo.

:: ---------- ID proyek ----------
echo [3/7] Buat proyek Firebase
set "PROJECT_ID="
set /p "PROJECT_ID=   ID proyek (huruf kecil/angka/tanda hubung, contoh: kaih-sdn-semambung): "
if "%PROJECT_ID%"=="" (
  echo   [GAGAL] ID proyek wajib diisi.
  pause
  exit /b 1
)
call firebase projects:create %PROJECT_ID% --display-name "Aplikasi 7 KAIH"
if errorlevel 1 (
  echo.
  echo   [CATATAN] Bila proyek sudah ada, lanjutkan saja ke langkah berikutnya.
)
echo.

:: ---------- Database Firestore ----------
echo [4/7] Buat database Cloud Firestore
echo   Lokasi tersedia:
call firebase firestore:locations
set "REGION="
set /p "REGION=   Pilih region (Enter = asia-southeast2 / Jakarta): "
if "%REGION%"=="" set "REGION=asia-southeast2"
call firebase firestore:databases:create "(default)" --location=%REGION% --project %PROJECT_ID%
if errorlevel 1 (
  echo   [GAGAL] Gagal membuat database. Pastikan paket Blaze/Spark aktif.
  pause
  exit /b 1
)
echo.

:: ---------- Daftarkan aplikasi Android ----------
echo [5/7] Daftarkan aplikasi Android
call firebase apps:create android com.sdn.semambung.kaih --project %PROJECT_ID%
echo.
echo   ^>^>^> Salin APP ID dari keluaran di atas (format: 1:xxxx:android:yyyy) ^<^<^<
set "APP_ID="
set /p "APP_ID=   Masukkan APP ID: "
if "%APP_ID%"=="" (
  echo   [GAGAL] APP ID wajib diisi.
  pause
  exit /b 1
)
echo.

:: ---------- Unduh google-services.json ----------
echo [6/7] Unduh google-services.json
call firebase apps:sdkconfig android %APP_ID% --project %PROJECT_ID% > google-services.json
echo   [INFO] Bila muncul "Assertion failed ... async.c" itu bug kosmetik Windows -
echo          unduhan tetap dianggap berhasil selama berkasnya terisi.
set "SZ=0"
if exist google-services.json for %%A in (google-services.json) do set "SZ=%%~zA"
if %SZ% LSS 100 (
  echo   [PERINGATAN] google-services.json kosong atau tidak terbentuk.
  echo   Solusi: unduh manual dari Firebase Console:
  echo     https://console.firebase.google.com/project/%PROJECT_ID%/settings/general
  echo   klik aplikasi Android -^> tombol "google-services.json"
  echo   lalu simpan ke folder: app\google-services.json
  pause
  exit /b 1
)
findstr /C:"project_id" google-services.json >nul
if errorlevel 1 (
  echo   [PERINGATAN] Isi google-services.json tidak valid. Unduh manual dari Firebase Console.
  pause
  exit /b 1
)
echo   google-services.json berhasil diunduh (%SZ% byte).
echo.
echo   Menyalin ke folder app\ ... (ganti jalur bila folder berbeda)
if exist "app\" (
  copy /Y google-services.json "app\google-services.json"
) else (
  echo   [INFO] Folder "app" tidak ditemukan di sini. Salin manual:
  echo          copy google-services.json ^<folder-android^>\app\google-services.json
)
echo.

:: ---------- Deploy aturan ----------
echo [7/7] Deploy aturan keamanan Firestore
if not exist "firebase.json" (
  echo   firebase.json tidak ditemukan, membuat...
  echo {"firestore": {"rules": "firestore.rules", "indexes": "firestore.indexes.json"}} > firebase.json
)
if not exist "firestore.rules" (
  echo   firestore.rules tidak ditemukan, membuat aturan demo...
  (
    echo rules_version = '2';
    echo service cloud.firestore {
    echo   match /databases/{database}/documents {
    echo     match /{document=**} { allow read, write: if true; }
    echo   }
    echo }
  ) > firestore.rules
)
if not exist "firestore.indexes.json" (
  echo {"indexes": [], "fieldOverrides": []} > firestore.indexes.json
)
call firebase deploy --only firestore:rules --project %PROJECT_ID%
if errorlevel 1 (
  echo   [GAGAL] Gagal deploy aturan.
  pause
  exit /b 1
)
echo.

echo ============================================================
echo   ✅ SELESAI!
echo   Database Firestore aktif untuk proyek: %PROJECT_ID%
echo.
echo   Langkah berikutnya:
echo     1. Buka folder android/ di Android Studio
echo     2. Build ^> Generate Signed Bundle/APK (keystore: kaih-release.keystore,
echo        alias wabcraft, sandi wabcraft2026)
echo     3. Instal APK baru dan coba login
echo ============================================================
pause
