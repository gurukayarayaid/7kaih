package com.sdn.semambung.kaih.util

import com.google.firebase.firestore.FirebaseFirestoreException

/** Ubah exception Firebase menjadi pesan yang mudah dipahami pengguna. */
fun pesanError(e: Exception): String = when {
    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        "Tidak dapat terhubung ke Firebase. Periksa koneksi internet dan pastikan google-services.json sudah diganti dengan berkas dari proyek Firebase Anda."

    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Akses ditolak oleh aturan Firestore. Periksa firestore.rules di Firebase Console."

    e.message?.contains("project_id") == true ||
        e.message?.contains("API key") == true ||
        e.message?.contains("api_key") == true ->
        "Firebase belum dikonfigurasi dengan benar. Ganti app/google-services.json dengan berkas dari konsol Firebase (lihat README)."

    else -> e.message ?: "Terjadi kesalahan."
}
