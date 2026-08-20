package com.sdn.semambung.kaih.data

import org.json.JSONObject

/** Baca string dari map Firestore dengan aman (tanpa crash bila null). */
fun Map<String, Any>?.str(kunci: String): String = (this?.get(kunci) as? String) ?: ""

/**
 * Model pengguna — padanan tabel `pengguna` pada versi web (SQLite).
 * Peran: "murid" | "guru" | "kepala_sekolah"
 */
data class Pengguna(
    val id: String = "",
    val nama: String = "",
    val username: String = "",
    val kataSandi: String = "",
    val peran: String = "",
    val kelas: String = "",
    val nis: String = "",
    val dibuatPada: String = ""
) {
    val peranLabel: String
        get() = when (peran) {
            "murid" -> "Murid"
            "guru" -> "Guru"
            "kepala_sekolah" -> "Kepala Sekolah"
            else -> peran
        }

    fun keMap(): Map<String, Any> = mapOf(
        "nama" to nama,
        "username" to username,
        "kata_sandi" to kataSandi,
        "peran" to peran,
        "kelas" to kelas,
        "nis" to nis,
        "dibuat_pada" to dibuatPada
    )

    fun keJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nama", nama)
        put("username", username)
        put("kata_sandi", kataSandi)
        put("peran", peran)
        put("kelas", kelas)
        put("nis", nis)
        put("dibuat_pada", dibuatPada)
    }

    companion object {
        fun dariMap(id: String, data: Map<String, Any>?): Pengguna = Pengguna(
            id = id,
            nama = data.str("nama"),
            username = data.str("username"),
            kataSandi = data.str("kata_sandi"),
            peran = data.str("peran"),
            kelas = data.str("kelas"),
            nis = data.str("nis"),
            dibuatPada = data.str("dibuat_pada")
        )

        fun dariJson(j: JSONObject): Pengguna = Pengguna(
            id = j.optString("id"),
            nama = j.optString("nama"),
            username = j.optString("username"),
            kataSandi = j.optString("kata_sandi"),
            peran = j.optString("peran"),
            kelas = j.optString("kelas"),
            nis = j.optString("nis"),
            dibuatPada = j.optString("dibuat_pada")
        )
    }
}

/** Nama-nama kolom jurnal — sama persis dengan kolom tabel `jurnal_harian` versi web. */
object Kolom {
    const val ID_MURID = "id_murid"
    const val TANGGAL = "tanggal"

    const val BANGUN_PAGI = "bangun_pagi"
    const val JAM_BANGUN = "jam_bangun"
    const val BERIBADAH = "beribadah"
    const val IBADAH_LAINNYA = "ibadah_lainnya"
    const val MENGAJI = "mengaji_alquran"
    const val MAKAN_PAGI = "makan_pagi"
    const val MAKAN_SIANG = "makan_siang"
    const val MAKAN_MALAM = "makan_malam"
    const val BERGIZI = "bergizi"
    const val GEMAR_BELAJAR = "gemar_belajar"
    const val JAM_BELAJAR = "jam_belajar"
    const val BUKU_DIBACA = "buku_dibaca"
    const val INFORMASI_BUKU = "informasi_buku"
    const val BEROLAHRAGA = "berolahraga"
    const val JAM_OLAHRAGA = "jam_olahraga"
    const val JENIS_OLAHRAGA = "jenis_olahraga"
    const val BERMASYARAKAT = "bermasyarakat"
    const val JAM_BERMASYARAKAT = "jam_bermasyarakat"
    const val KEGIATAN_MASYARAKAT = "kegiatan_masyarakat"
    const val TIDUR_CEPAT = "tidur_cepat"
    const val JAM_TIDUR = "jam_tidur"

    const val PARAF = "paraf_orang_tua"
    const val STATUS = "status_persetujuan"
    const val CATATAN = "catatan_guru"
    const val DIPERBARUI = "diperbarui_pada"

    /** 21 kolom isian kebiasaan, urutan sesuai versi web. */
    val SEMUA = listOf(
        BANGUN_PAGI, JAM_BANGUN, BERIBADAH, IBADAH_LAINNYA, MENGAJI,
        MAKAN_PAGI, MAKAN_SIANG, MAKAN_MALAM, BERGIZI,
        GEMAR_BELAJAR, JAM_BELAJAR, BUKU_DIBACA, INFORMASI_BUKU,
        BEROLAHRAGA, JAM_OLAHRAGA, JENIS_OLAHRAGA,
        BERMASYARAKAT, JAM_BERMASYARAKAT, KEGIATAN_MASYARAKAT,
        TIDUR_CEPAT, JAM_TIDUR
    )
}

/**
 * Model jurnal harian — padanan baris tabel `jurnal_harian`.
 * ID dokumen Firestore: "{id_murid}_{tanggal}" (mis. "4_2026-08-19").
 */
data class JurnalHarian(
    val id: String = "",
    val idMurid: String = "",
    val tanggal: String = "",
    val bangunPagi: String = "",
    val jamBangun: String = "",
    val beribadah: String = "",
    val ibadahLainnya: String = "",
    val mengajiAlquran: String = "",
    val makanPagi: String = "",
    val makanSiang: String = "",
    val makanMalam: String = "",
    val bergizi: String = "",
    val gemarBelajar: String = "",
    val jamBelajar: String = "",
    val bukuDibaca: String = "",
    val informasiBuku: String = "",
    val berolahraga: String = "",
    val jamOlahraga: String = "",
    val jenisOlahraga: String = "",
    val bermasyarakat: String = "",
    val jamBermasyarakat: String = "",
    val kegiatanMasyarakat: String = "",
    val tidurCepat: String = "",
    val jamTidur: String = "",
    val parafOrangTua: String = "",
    val statusPersetujuan: String = "",
    val catatanGuru: String = "",
    val diperbaruiPada: String = ""
) {
    /** Ambil nilai kolom berdasarkan nama kolom (untuk logika rekap). */
    fun nilai(kolom: String): String = when (kolom) {
        Kolom.BANGUN_PAGI -> bangunPagi
        Kolom.JAM_BANGUN -> jamBangun
        Kolom.BERIBADAH -> beribadah
        Kolom.IBADAH_LAINNYA -> ibadahLainnya
        Kolom.MENGAJI -> mengajiAlquran
        Kolom.MAKAN_PAGI -> makanPagi
        Kolom.MAKAN_SIANG -> makanSiang
        Kolom.MAKAN_MALAM -> makanMalam
        Kolom.BERGIZI -> bergizi
        Kolom.GEMAR_BELAJAR -> gemarBelajar
        Kolom.JAM_BELAJAR -> jamBelajar
        Kolom.BUKU_DIBACA -> bukuDibaca
        Kolom.INFORMASI_BUKU -> informasiBuku
        Kolom.BEROLAHRAGA -> berolahraga
        Kolom.JAM_OLAHRAGA -> jamOlahraga
        Kolom.JENIS_OLAHRAGA -> jenisOlahraga
        Kolom.BERMASYARAKAT -> bermasyarakat
        Kolom.JAM_BERMASYARAKAT -> jamBermasyarakat
        Kolom.KEGIATAN_MASYARAKAT -> kegiatanMasyarakat
        Kolom.TIDUR_CEPAT -> tidurCepat
        Kolom.JAM_TIDUR -> jamTidur
        else -> ""
    }

    fun keMap(): Map<String, Any> = mapOf(
        Kolom.ID_MURID to idMurid,
        Kolom.TANGGAL to tanggal,
        Kolom.BANGUN_PAGI to bangunPagi,
        Kolom.JAM_BANGUN to jamBangun,
        Kolom.BERIBADAH to beribadah,
        Kolom.IBADAH_LAINNYA to ibadahLainnya,
        Kolom.MENGAJI to mengajiAlquran,
        Kolom.MAKAN_PAGI to makanPagi,
        Kolom.MAKAN_SIANG to makanSiang,
        Kolom.MAKAN_MALAM to makanMalam,
        Kolom.BERGIZI to bergizi,
        Kolom.GEMAR_BELAJAR to gemarBelajar,
        Kolom.JAM_BELAJAR to jamBelajar,
        Kolom.BUKU_DIBACA to bukuDibaca,
        Kolom.INFORMASI_BUKU to informasiBuku,
        Kolom.BEROLAHRAGA to berolahraga,
        Kolom.JAM_OLAHRAGA to jamOlahraga,
        Kolom.JENIS_OLAHRAGA to jenisOlahraga,
        Kolom.BERMASYARAKAT to bermasyarakat,
        Kolom.JAM_BERMASYARAKAT to jamBermasyarakat,
        Kolom.KEGIATAN_MASYARAKAT to kegiatanMasyarakat,
        Kolom.TIDUR_CEPAT to tidurCepat,
        Kolom.JAM_TIDUR to jamTidur,
        Kolom.PARAF to parafOrangTua,
        Kolom.STATUS to statusPersetujuan,
        Kolom.CATATAN to catatanGuru,
        Kolom.DIPERBARUI to diperbaruiPada
    )

    companion object {
        fun dariMap(id: String, data: Map<String, Any>?): JurnalHarian = JurnalHarian(
            id = id,
            idMurid = data.str(Kolom.ID_MURID),
            tanggal = data.str(Kolom.TANGGAL),
            bangunPagi = data.str(Kolom.BANGUN_PAGI),
            jamBangun = data.str(Kolom.JAM_BANGUN),
            beribadah = data.str(Kolom.BERIBADAH),
            ibadahLainnya = data.str(Kolom.IBADAH_LAINNYA),
            mengajiAlquran = data.str(Kolom.MENGAJI),
            makanPagi = data.str(Kolom.MAKAN_PAGI),
            makanSiang = data.str(Kolom.MAKAN_SIANG),
            makanMalam = data.str(Kolom.MAKAN_MALAM),
            bergizi = data.str(Kolom.BERGIZI),
            gemarBelajar = data.str(Kolom.GEMAR_BELAJAR),
            jamBelajar = data.str(Kolom.JAM_BELAJAR),
            bukuDibaca = data.str(Kolom.BUKU_DIBACA),
            informasiBuku = data.str(Kolom.INFORMASI_BUKU),
            berolahraga = data.str(Kolom.BEROLAHRAGA),
            jamOlahraga = data.str(Kolom.JAM_OLAHRAGA),
            jenisOlahraga = data.str(Kolom.JENIS_OLAHRAGA),
            bermasyarakat = data.str(Kolom.BERMASYARAKAT),
            jamBermasyarakat = data.str(Kolom.JAM_BERMASYARAKAT),
            kegiatanMasyarakat = data.str(Kolom.KEGIATAN_MASYARAKAT),
            tidurCepat = data.str(Kolom.TIDUR_CEPAT),
            jamTidur = data.str(Kolom.JAM_TIDUR),
            parafOrangTua = data.str(Kolom.PARAF),
            statusPersetujuan = data.str(Kolom.STATUS),
            catatanGuru = data.str(Kolom.CATATAN),
            diperbaruiPada = data.str(Kolom.DIPERBARUI)
        )
    }
}
