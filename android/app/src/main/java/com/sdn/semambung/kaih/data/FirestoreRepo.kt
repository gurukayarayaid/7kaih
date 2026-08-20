package com.sdn.semambung.kaih.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sdn.semambung.kaih.util.Csv
import com.sdn.semambung.kaih.util.Format
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

/** Ubah Task Firebase menjadi fungsi suspend (tanpa dependensi -ktx). */
suspend fun <T> Task<T>.tunggu(): T = suspendCancellableCoroutine { kont ->
    addOnSuccessListener { kont.resume(it) }
    addOnFailureListener { kont.resumeWithException(it) }
}

/**
 * Lapisan database — Cloud Firestore menggantikan SQLite pada versi web.
 *
 * Koleksi:
 *  - pengguna       : dokumen = akun (murid, guru, kepala sekolah)
 *  - jurnal_harian  : dokumen = satu jurnal per murid per tanggal
 *  - pengaturan     : (dicadangkan)
 */
object FirestoreRepo {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Daftar resmi murid Kelas III (NIS = username sekaligus kata sandi)
    private val SEED_MURID = listOf(
        "3262" to "ACHMAD DHAFIN KHALIF ALGHIFARI",
        "3263" to "ALFIRA NAHDA RAFANDA",
        "3264" to "ARSYILA ROMEESA FARZANA",
        "3265" to "ASSYFA PUTRI NAURA ZASKIA",
        "3266" to "ATIQAH FATIMATUS ZAHRA",
        "3267" to "ELLVINO GAVRIEL ALVARO",
        "3268" to "FALISA AMALIA PUTRI",
        "3269" to "GIBRAN KEENANDRA ARDIANSYAH",
        "3270" to "KANIA DWI NUR MAULIDDIAH",
        "3271" to "MUHAMMAD ABDULLOH FADIL",
        "3272" to "MUHAMMAD NATHAN HAFIZ PRADIPTA",
        "3273" to "MUHAMMAD NAUFAL AL RAJABI",
        "3274" to "MUHAMMAD RAKA ISLAMUDDIN",
        "3275" to "MUHAMMAD RAKHA FATKHUL HALIM",
        "3276" to "NAFISA AZZAHRA KHUSNANDAR",
        "3277" to "NIKMATUL NISA",
        "3278" to "OKTAVIA PUTRI GANESHA",
        "3279" to "RAYSA NABILAH PUTRI",
        "3280" to "RIKA ELVINA",
        "3281" to "SAYYID MAULANA IBRAHIM",
        "3282" to "TIKA ASSYIFAH ARRUM",
        "3317" to "MUHAMAD RAHMADANI"
    )

    // ---------------------------------------------------------------
    // Data awal (padanan seed otomatis pada versi web)
    // ---------------------------------------------------------------

    /** Isi data awal bila koleksi pengguna masih kosong (dijalankan saat login). */
    suspend fun pastikanDataAwal() {
        val ada = db.collection("pengguna").limit(1).get().tunggu()
        if (!ada.isEmpty) return

        val batch = db.batch()
        var urut = 1
        val tambah = { map: Map<String, Any> ->
            batch.set(db.collection("pengguna").document(urut.toString()), map)
            urut++
        }
        tambah(
            mapOf(
                "nama" to "Siti Nurhaliza",
                "username" to "siti",
                "kata_sandi" to "123456",
                "peran" to "murid",
                "kelas" to "Kelas III",
                "nis" to "",
                "dibuat_pada" to Format.sekarangIso()
            )
        )
        tambah(
            mapOf(
                "nama" to "Ibu Rina Wati",
                "username" to "guru",
                "kata_sandi" to "alal",
                "peran" to "guru",
                "kelas" to "",
                "nis" to "",
                "dibuat_pada" to Format.sekarangIso()
            )
        )
        tambah(
            mapOf(
                "nama" to "Bapak Ahmad",
                "username" to "kepsek",
                "kata_sandi" to "123456",
                "peran" to "kepala_sekolah",
                "kelas" to "",
                "nis" to "",
                "dibuat_pada" to Format.sekarangIso()
            )
        )
        SEED_MURID.forEach { (nis, nama) ->
            tambah(
                mapOf(
                    "nama" to nama,
                    "username" to nis,
                    "kata_sandi" to nis,
                    "peran" to "murid",
                    "kelas" to "Kelas III",
                    "nis" to nis,
                    "dibuat_pada" to Format.sekarangIso()
                )
            )
        }
        batch.commit().tunggu()
    }

    // ---------------------------------------------------------------
    // Pengguna & login (logika sama dengan versi web PHP)
    // ---------------------------------------------------------------

    /** Semua murid; hanyaBerNis = true meniru daftar login versi web (hanya ber-NIS). */
    suspend fun daftarMurid(hanyaBerNis: Boolean = true): List<Pengguna> {
        val docs = db.collection("pengguna").whereEqualTo("peran", "murid").get().tunggu()
        return docs.documents
            .map { Pengguna.dariMap(it.id, it.data) }
            .filter { !hanyaBerNis || it.nis.isNotBlank() }
            .sortedBy { it.nama.lowercase() }
    }

    suspend fun ambilPengguna(id: String): Pengguna? {
        val d = db.collection("pengguna").document(id).get().tunggu()
        return if (d.exists()) Pengguna.dariMap(d.id, d.data) else null
    }

    suspend fun loginMurid(id: String, kataSandi: String): Pengguna? {
        val d = db.collection("pengguna").document(id).get().tunggu()
        if (!d.exists()) return null
        val u = Pengguna.dariMap(d.id, d.data)
        return if (u.peran == "murid" && u.kataSandi == kataSandi) u else null
    }

    suspend fun loginGuru(kataSandi: String): Pengguna? {
        val docs = db.collection("pengguna").whereEqualTo("peran", "guru").limit(1).get().tunggu()
        val u = docs.documents.firstOrNull()?.let { Pengguna.dariMap(it.id, it.data) } ?: return null
        return if (u.kataSandi == kataSandi) u else null
    }

    suspend fun loginKepalaSekolah(username: String, kataSandi: String): Pengguna? {
        val docs = db.collection("pengguna").whereEqualTo("peran", "kepala_sekolah").get().tunggu()
        val u = docs.documents
            .map { Pengguna.dariMap(it.id, it.data) }
            .firstOrNull { it.username == username }
            ?: return null
        return if (u.kataSandi == kataSandi) u else null
    }

    // ---------------------------------------------------------------
    // Jurnal harian
    // ---------------------------------------------------------------

    /** Simpan jurnal (upsert). Status persetujuan direset ke "Menunggu", seperti versi web. */
    suspend fun upsertJurnal(idMurid: String, tanggal: String, nilai: Map<String, Any>) {
        db.collection("jurnal_harian")
            .document("${idMurid}_$tanggal")
            .set(nilai, SetOptions.merge())
            .tunggu()
    }

    suspend fun ambilJurnal(idMurid: String, tanggal: String): JurnalHarian? {
        val d = db.collection("jurnal_harian").document("${idMurid}_$tanggal").get().tunggu()
        return if (d.exists()) JurnalHarian.dariMap(d.id, d.data) else null
    }

    suspend fun aturParaf(idMurid: String, tanggal: String, waktu: String) {
        db.collection("jurnal_harian")
            .document("${idMurid}_$tanggal")
            .set(
                mapOf(Kolom.PARAF to waktu, Kolom.DIPERBARUI to Format.sekarangIso()),
                SetOptions.merge()
            )
            .tunggu()
    }

    /** Semua jurnal milik seorang murid (difilter per bulan di sisi aplikasi). */
    suspend fun jurnalMurid(idMurid: String): List<JurnalHarian> {
        return db.collection("jurnal_harian")
            .whereEqualTo(Kolom.ID_MURID, idMurid)
            .get()
            .tunggu()
            .documents
            .mapNotNull { if (it.exists()) JurnalHarian.dariMap(it.id, it.data) else null }
    }

    /** Semua jurnal di seluruh murid pada tanggal tertentu (halaman Admin). */
    suspend fun jurnalTanggal(tanggal: String): List<JurnalHarian> {
        return db.collection("jurnal_harian")
            .whereEqualTo(Kolom.TANGGAL, tanggal)
            .get()
            .tunggu()
            .documents
            .mapNotNull { if (it.exists()) JurnalHarian.dariMap(it.id, it.data) else null }
    }

    /** Semua jurnal (dipakai untuk statistik rekap, satu kali query). */
    suspend fun semuaJurnal(): List<JurnalHarian> {
        return db.collection("jurnal_harian")
            .get()
            .tunggu()
            .documents
            .mapNotNull { if (it.exists()) JurnalHarian.dariMap(it.id, it.data) else null }
    }

    suspend fun updatePersetujuan(idJurnal: String, status: String, catatan: String) {
        db.collection("jurnal_harian")
            .document(idJurnal)
            .set(
                mapOf(Kolom.STATUS to status, Kolom.CATATAN to catatan),
                SetOptions.merge()
            )
            .tunggu()
    }

    // ---------------------------------------------------------------
    // Data / cadangan (menu "Data" khusus guru)
    // ---------------------------------------------------------------

    suspend fun kosongkanJurnal() {
        val dokumen = db.collection("jurnal_harian").get().tunggu().documents
        dokumen.chunked(400).forEach { potong ->
            val batch = db.batch()
            potong.forEach { batch.delete(it.reference) }
            batch.commit().tunggu()
        }
    }

    /** Unduh cadangan seluruh data dalam format JSON (pengganti berkas .sqlite). */
    suspend fun eksporBackup(): String {
        val pengguna = db.collection("pengguna").get().tunggu()
        val jurnal = db.collection("jurnal_harian").get().tunggu()

        val root = JSONObject()
        root.put("aplikasi", "7 KAIH")
        root.put("versi", 1)
        root.put("dibuat_pada", Format.sekarangIso())

        val arrP = JSONArray()
        for (d in pengguna.documents) {
            val o = JSONObject()
            o.put("id", d.id)
            d.data?.forEach { (k, v) -> o.put(k, v) }
            arrP.put(o)
        }
        root.put("pengguna", arrP)

        val arrJ = JSONArray()
        for (d in jurnal.documents) {
            val o = JSONObject()
            o.put("id", d.id)
            d.data?.forEach { (k, v) -> o.put(k, v) }
            arrJ.put(o)
        }
        root.put("jurnal_harian", arrJ)

        return root.toString()
    }

    /** Pulihkan cadangan JSON: seluruh data lama dihapus lalu diganti data cadangan. */
    suspend fun imporBackup(isi: String): Int {
        val root = JSONObject(isi)
        val arrP = root.optJSONArray("pengguna") ?: JSONArray()
        val arrJ = root.optJSONArray("jurnal_harian") ?: JSONArray()

        // Hapus semua data lama (semantik sama dengan restore berkas SQLite)
        val lama = db.collection("pengguna").get().tunggu().documents +
            db.collection("jurnal_harian").get().tunggu().documents
        lama.chunked(400).forEach { potong ->
            val batch = db.batch()
            potong.forEach { batch.delete(it.reference) }
            batch.commit().tunggu()
        }

        var total = 0
        suspend fun tulis(koleksi: String, arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                val map = mutableMapOf<String, Any>()
                val kunci = o.keys()
                while (kunci.hasNext()) {
                    val k = kunci.next()
                    if (k != "id") {
                        val v = o.get(k)
                        if (v != JSONObject.NULL) map[k] = v
                    }
                }
                db.collection(koleksi).document(id).set(map).tunggu()
                total++
            }
        }
        tulis("pengguna", arrP)
        tulis("jurnal_harian", arrJ)
        return total
    }

    /**
     * Unggah data murid dari CSV (nama, username, kata_sandi, kelas).
     * Baris dengan username duplikat dilewati — sama seperti versi web.
     */
    suspend fun unggahMuridCsv(isi: String): Int {
        val rows = Csv.parse(isi)
        val ada = db.collection("pengguna").get().tunggu()
            .documents.mapNotNull { it.data?.get("username") as? String }
            .toMutableSet()

        var tambah = 0
        for (r in rows) {
            if (r.size < 4) continue
            if (r[0].trim().lowercase() == "nama") continue // baris header
            val nama = r[0].trim()
            val username = r[1].trim()
            val kataSandi = r[2].trim()
            val kelas = r[3].trim()
            if (nama.isEmpty() || username.isEmpty()) continue
            if (username in ada) continue

            db.collection("pengguna").add(
                mapOf(
                    "nama" to nama,
                    "username" to username,
                    "kata_sandi" to kataSandi,
                    "peran" to "murid",
                    "kelas" to kelas,
                    "nis" to "",
                    "dibuat_pada" to Format.sekarangIso()
                )
            ).tunggu()
            ada.add(username)
            tambah++
        }
        return tambah
    }
}
