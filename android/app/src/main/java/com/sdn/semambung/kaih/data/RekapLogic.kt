package com.sdn.semambung.kaih.data

import com.sdn.semambung.kaih.util.Format
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Logika rekap — persis mengikuti perhitungan versi web PHP.
 */
object RekapLogic {

    /** 7 kebiasaan yang dinilai (kolom -> label). */
    val KEBIASAAN: Map<String, String> = linkedMapOf(
        "bangun_pagi" to "Bangun Pagi",
        "beribadah" to "Beribadah",
        "berolahraga" to "Berolahraga",
        "makan_pagi" to "Makan Sehat dan Bergizi",
        "gemar_belajar" to "Gemar Belajar",
        "bermasyarakat" to "Bermasyarakat",
        "tidur_cepat" to "Tidur Cepat"
    )

    /** Satu hari dianggap lengkap bila seluruh 7 kebiasaan terisi. */
    fun hariLengkap(j: JurnalHarian): Boolean =
        KEBIASAAN.keys.all { j.nilai(it).isNotBlank() }

    /** Bulan dianggap tuntas bila hari lengkap >= 80% jumlah hari dalam bulan. */
    fun bulanLengkap(rows: List<JurnalHarian>, jumlahHari: Int): Boolean =
        rows.count { hariLengkap(it) } >= ceil(jumlahHari * 0.8).toInt()

    /** Semester dianggap tuntas bila SEMUA hari dalam semester terisi lengkap. */
    fun semesterLengkap(rows: List<JurnalHarian>, jumlahHariSemester: Int): Boolean {
        if (rows.size != jumlahHariSemester) return false
        if (rows.map { it.tanggal }.distinct().size != jumlahHariSemester) return false
        return rows.all { hariLengkap(it) }
    }

    fun jumlahHariSemester(tahun: Int, bulan: List<String>): Int =
        bulan.sumOf { YearMonth.of(tahun, it.toInt()).lengthOfMonth() }

    // ---------------------------------------------------------------
    // Hasil pemuatan data untuk layar
    // ---------------------------------------------------------------

    data class RekapBulanan(
        val profil: Pengguna?,
        val rows: List<JurnalHarian>,
        val jumlahHari: Int,
        val minimalHari: Int,
        val lengkap: Boolean,
        val daftarMurid: List<Pengguna>,
        val statSelesai: List<Pengguna>,
        val statBelum: List<Pengguna>,
        val persenSelesai: Int
    )

    data class RekapSemester(
        val profil: Pengguna?,
        val rows: List<JurnalHarian>,
        val sem: String,
        val jumlahHari: Int,
        val lengkap: Boolean,
        val daftarMurid: List<Pengguna>,
        val bulanSemester: List<Pair<String, Boolean>>
    )

    data class JurnalAdmin(val jurnal: JurnalHarian, val nama: String, val kelas: String)

    data class DataAdmin(
        val semuaMurid: List<Pengguna>,
        val rows: List<JurnalAdmin>,
        val sudah: List<Pengguna>,
        val belum: List<Pengguna>,
        val nilaiHabit: Map<String, Int>
    )

    suspend fun muatRekapBulanan(idMurid: String, bulan: YearMonth): RekapBulanan {
        val daftar = FirestoreRepo.daftarMurid(true)
        val profil = FirestoreRepo.ambilPengguna(idMurid)
        val semuaJurnal = FirestoreRepo.semuaJurnal()
        val awalan = bulan.toString() // "2026-08"
        val rows = semuaJurnal
            .filter { it.idMurid == idMurid && it.tanggal.startsWith(awalan) }
            .sortedBy { it.tanggal }

        val jumlahHari = bulan.lengthOfMonth()
        val minimalHari = ceil(jumlahHari * 0.8).toInt()
        val lengkap = bulanLengkap(rows, jumlahHari)

        val selesai = mutableListOf<Pengguna>()
        val belum = mutableListOf<Pengguna>()
        for (m in daftar) {
            val r = semuaJurnal.filter { it.idMurid == m.id && it.tanggal.startsWith(awalan) }
            if (bulanLengkap(r, jumlahHari)) selesai.add(m) else belum.add(m)
        }
        val total = selesai.size + belum.size
        val persen = if (total > 0) (selesai.size * 100.0 / total).roundToInt() else 0

        return RekapBulanan(
            profil = profil,
            rows = rows,
            jumlahHari = jumlahHari,
            minimalHari = minimalHari,
            lengkap = lengkap,
            daftarMurid = daftar,
            statSelesai = selesai,
            statBelum = belum,
            persenSelesai = persen
        )
    }

    suspend fun muatRekapSemester(idMurid: String, sem: String): RekapSemester {
        val bulan = if (sem == "ganjil") {
            listOf("07", "08", "09", "10", "11", "12")
        } else {
            listOf("01", "02", "03", "04", "05", "06")
        }
        val tahun = LocalDate.now().year
        val semuaJurnal = FirestoreRepo.semuaJurnal()
        val rows = semuaJurnal
            .filter {
                it.idMurid == idMurid &&
                    it.tanggal.length >= 10 &&
                    it.tanggal.take(4).toIntOrNull() == tahun &&
                    it.tanggal.substring(5, 7) in bulan
            }
            .sortedBy { it.tanggal }

        val jumlahHari = jumlahHariSemester(tahun, bulan)
        val lengkap = semesterLengkap(rows, jumlahHari)
        val daftar = FirestoreRepo.daftarMurid(true)
        val profil = FirestoreRepo.ambilPengguna(idMurid)
        val bulanSemester = Format.NAMA_BULAN.mapIndexed { i, nama ->
            nama to ((i + 1).toString().padStart(2, '0') in bulan)
        }

        return RekapSemester(
            profil = profil,
            rows = rows,
            sem = sem,
            jumlahHari = jumlahHari,
            lengkap = lengkap,
            daftarMurid = daftar,
            bulanSemester = bulanSemester
        )
    }

    suspend fun muatAdmin(tanggal: String): DataAdmin {
        val semua = FirestoreRepo.daftarMurid(false)
        val namaMap = semua.associateBy { it.id }
        val rows = FirestoreRepo.jurnalTanggal(tanggal)
            .mapNotNull { j ->
                namaMap[j.idMurid]?.let { u -> JurnalAdmin(j, u.nama, u.kelas) }
            }
            .sortedBy { it.nama }

        val sudah = semua.filter { m -> rows.any { it.jurnal.idMurid == m.id } }
        val belum = semua.filterNot { m -> rows.any { it.jurnal.idMurid == m.id } }
        val nilaiHabit = KEBIASAAN.keys.associateWith { k ->
            rows.count { it.jurnal.nilai(k).isNotBlank() }
        }

        return DataAdmin(semua, rows, sudah, belum, nilaiHabit)
    }
}
