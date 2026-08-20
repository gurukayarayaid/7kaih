package com.sdn.semambung.kaih.ui

import android.app.Application
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sdn.semambung.kaih.data.FirestoreRepo
import com.sdn.semambung.kaih.data.Kolom
import com.sdn.semambung.kaih.data.Pengguna
import com.sdn.semambung.kaih.DebugLog
import com.sdn.semambung.kaih.util.Format
import com.sdn.semambung.kaih.util.pesanError
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Tab menu bawah — menggantikan menu tab atas pada versi web. */
enum class Tab(val label: String, val ikon: ImageVector) {
    Jurnal("Jurnal", Icons.Outlined.EditCalendar),
    Kalender("Kalender", Icons.Outlined.CalendarMonth),
    RekapBulanan("Rekap Bulan", Icons.Outlined.BarChart),
    RekapSemester("Rekap Semester", Icons.Outlined.Assessment),
    Admin("Admin", Icons.Outlined.FactCheck),
    Data("Data", Icons.Outlined.Backup),
    Profil("Profil", Icons.Outlined.Person)
}

/** Daftar tab sesuai peran — sama dengan menu navigasi versi web. */
fun tabsUntuk(peran: String): List<Tab> = when (peran) {
    "murid" -> listOf(Tab.Jurnal, Tab.Kalender, Tab.Profil)
    "guru" -> listOf(Tab.Jurnal, Tab.RekapBulanan, Tab.RekapSemester, Tab.Admin, Tab.Data)
    else -> listOf(Tab.Jurnal, Tab.RekapBulanan, Tab.RekapSemester, Tab.Admin) // kepala sekolah
}

/** Isian formulir jurnal — disimpan di ViewModel agar tidak hilang saat pindah tab. */
class JurnalFormState {
    var bangunPagi by mutableStateOf(false)
    var jamBangun by mutableStateOf("")
    val ibadah = mutableStateListOf<String>()
    var ibadahLainnya by mutableStateOf("")
    var mengaji by mutableStateOf("")
    var makanPagi by mutableStateOf("")
    var makanSiang by mutableStateOf("")
    var makanMalam by mutableStateOf("")
    var gemarBelajar by mutableStateOf(false)
    var jamBelajar by mutableStateOf("")
    var bukuDibaca by mutableStateOf("")
    var informasiBuku by mutableStateOf("")
    var berolahraga by mutableStateOf(false)
    var jamOlahraga by mutableStateOf("")
    var jenisOlahraga by mutableStateOf("")
    var bermasyarakat by mutableStateOf(false)
    var jamBermasyarakat by mutableStateOf("")
    var kegiatanMasyarakat by mutableStateOf("")
    var tidurCepat by mutableStateOf(false)
    var jamTidur by mutableStateOf("")

    // Metadata jurnal (dari Firestore)
    var status by mutableStateOf("")
    var catatanGuru by mutableStateOf("")
    var paraf by mutableStateOf("")
    var masaDepan by mutableStateOf(false)
    var terkunci by mutableStateOf(false)

    fun muatDari(j: com.sdn.semambung.kaih.data.JurnalHarian?) {
        bangunPagi = j?.bangunPagi == "Ya"
        jamBangun = j?.jamBangun ?: ""
        ibadah.clear()
        j?.beribadah?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.let {
            ibadah.addAll(it)
        }
        ibadahLainnya = j?.ibadahLainnya ?: ""
        mengaji = j?.mengajiAlquran ?: ""
        makanPagi = j?.makanPagi ?: ""
        makanSiang = j?.makanSiang ?: ""
        makanMalam = j?.makanMalam ?: ""
        gemarBelajar = j?.gemarBelajar == "Ya"
        jamBelajar = j?.jamBelajar ?: ""
        bukuDibaca = j?.bukuDibaca ?: ""
        informasiBuku = j?.informasiBuku ?: ""
        berolahraga = j?.berolahraga == "Ya"
        jamOlahraga = j?.jamOlahraga ?: ""
        jenisOlahraga = j?.jenisOlahraga ?: ""
        bermasyarakat = j?.bermasyarakat == "Ya"
        jamBermasyarakat = j?.jamBermasyarakat ?: ""
        kegiatanMasyarakat = j?.kegiatanMasyarakat ?: ""
        tidurCepat = j?.tidurCepat == "Ya"
        jamTidur = j?.jamTidur ?: ""
        status = j?.statusPersetujuan ?: ""
        catatanGuru = j?.catatanGuru ?: ""
        paraf = j?.parafOrangTua ?: ""
    }

    fun reset() {
        bangunPagi = false
        jamBangun = ""
        ibadah.clear()
        ibadahLainnya = ""
        mengaji = ""
        makanPagi = ""
        makanSiang = ""
        makanMalam = ""
        gemarBelajar = false
        jamBelajar = ""
        bukuDibaca = ""
        informasiBuku = ""
        berolahraga = false
        jamOlahraga = ""
        jenisOlahraga = ""
        bermasyarakat = false
        jamBermasyarakat = ""
        kegiatanMasyarakat = ""
        tidurCepat = false
        jamTidur = ""
        status = ""
        catatanGuru = ""
        paraf = ""
        masaDepan = false
        terkunci = false
    }

    /** Bangun map dokumen jurnal untuk Firestore — kolom sama dengan versi web. */
    fun keMapJurnal(idMurid: String, tanggal: String): Map<String, Any> = mapOf(
        Kolom.ID_MURID to idMurid,
        Kolom.TANGGAL to tanggal,
        Kolom.BANGUN_PAGI to if (bangunPagi) "Ya" else "",
        Kolom.JAM_BANGUN to jamBangun,
        Kolom.BERIBADAH to ibadah.joinToString(", "),
        Kolom.IBADAH_LAINNYA to ibadahLainnya,
        Kolom.MENGAJI to mengaji,
        Kolom.MAKAN_PAGI to makanPagi,
        Kolom.MAKAN_SIANG to makanSiang,
        Kolom.MAKAN_MALAM to makanMalam,
        Kolom.BERGIZI to "Ya",
        Kolom.GEMAR_BELAJAR to if (gemarBelajar) "Ya" else "",
        Kolom.JAM_BELAJAR to jamBelajar,
        Kolom.BUKU_DIBACA to bukuDibaca,
        Kolom.INFORMASI_BUKU to informasiBuku,
        Kolom.BEROLAHRAGA to if (berolahraga) "Ya" else "",
        Kolom.JAM_OLAHRAGA to jamOlahraga,
        Kolom.JENIS_OLAHRAGA to jenisOlahraga,
        Kolom.BERMASYARAKAT to if (bermasyarakat) "Ya" else "",
        Kolom.JAM_BERMASYARAKAT to jamBermasyarakat,
        Kolom.KEGIATAN_MASYARAKAT to kegiatanMasyarakat,
        Kolom.TIDUR_CEPAT to if (tidurCepat) "Ya" else "",
        Kolom.JAM_TIDUR to jamTidur,
        Kolom.STATUS to "Menunggu",
        Kolom.DIPERBARUI to Format.sekarangIso()
    )
}

/** ViewModel utama aplikasi — menyimpan sesi, tab aktif, dan semua state antar layar. */
class KaihViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val PREFS = "kaih_sesi"
        private const val KUNCI_USER = "pengguna"
    }

    // ---- Sesi ----
    var user by mutableStateOf<Pengguna?>(null)
        private set
    var pesan by mutableStateOf<String?>(null)
    var sibuk by mutableStateOf(false)       // proses login
    var sibukData by mutableStateOf(false)   // proses backup/restore/unggah

    // ---- Data login ----
    var daftarMurid by mutableStateOf<List<Pengguna>>(emptyList())
        private set
    var daftarSiap by mutableStateOf(false)
        private set

    // ---- Tab aktif (menu bawah) ----
    var tab by mutableStateOf(Tab.Jurnal)

    // ---- Jurnal ----
    var jurnalTanggal by mutableStateOf(Format.hariIni())
    val form = JurnalFormState()
    var jurnalMemuat by mutableStateOf(false)
        private set

    // ---- Kalender ----
    var kalenderBulan by mutableStateOf(YearMonth.now())

    // ---- Rekap ----
    var rekapBulan by mutableStateOf(YearMonth.now())
    var rekapSem by mutableStateOf("ganjil")
    var rekapIdMurid by mutableStateOf<String?>(null)
    var rekapReload by mutableStateOf(0)

    // ---- Admin ----
    var adminTanggal by mutableStateOf(Format.hariIni())
    var adminReload by mutableStateOf(0)

    // ---------------------------------------------------------------
    // Sesi & login
    // ---------------------------------------------------------------

    fun restoreSession() {
        DebugLog.log("VM restoreSession")
        val prefs = getApplication<Application>().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KUNCI_USER, null) ?: return
        user = Pengguna.dariJson(JSONObject(json))
        // Muat ulang data terbaru dari Firestore (bila offline, pakai cache sesi)
        viewModelScope.launch {
            try {
                val u = FirestoreRepo.ambilPengguna(user?.id ?: return@launch)
                if (u != null) user = u else keluar()
            } catch (e: Exception) {
                // tidak ada koneksi — tetap gunakan sesi tersimpan
            }
        }
    }

    fun muatDaftarMurid() {
        DebugLog.log("VM muatDaftarMurid mulai")
        viewModelScope.launch {
            daftarSiap = false
            try {
                FirestoreRepo.pastikanDataAwal()
                daftarMurid = FirestoreRepo.daftarMurid(hanyaBerNis = true)
                DebugLog.log("VM daftarMurid: ${daftarMurid.size} murid")
            } catch (e: Exception) {
                pesan = pesanError(e)
                DebugLog.log("VM daftarMurid GAGAL: ${e.javaClass.name}: ${e.message}")
            }
            daftarSiap = true
        }
    }

    fun login(jenis: String, idMurid: String?, kataSandi: String, username: String) {
        if (sibuk) return
        sibuk = true
        viewModelScope.launch {
            try {
                val u = when (jenis) {
                    "murid" -> FirestoreRepo.loginMurid(idMurid ?: "", kataSandi)
                    "guru" -> FirestoreRepo.loginGuru(kataSandi)
                    else -> FirestoreRepo.loginKepalaSekolah(username, kataSandi)
                }
                if (u == null) {
                    pesan = "Data masuk atau kata sandi tidak sesuai."
                } else {
                    simpanSesi(u)
                    user = u
                    tab = Tab.Jurnal
                    jurnalTanggal = Format.hariIni()
                    pesan = null
                }
            } catch (e: Exception) {
                pesan = pesanError(e)
            } finally {
                sibuk = false
            }
        }
    }

    fun keluar() {
        hapusSesi()
        user = null
        tab = Tab.Jurnal
        form.reset()
        daftarMurid = emptyList()
        daftarSiap = false
        pesan = null
    }

    private fun simpanSesi(u: Pengguna) {
        getApplication<Application>()
            .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KUNCI_USER, u.keJson().toString())
            .apply()
    }

    private fun hapusSesi() {
        getApplication<Application>()
            .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .remove(KUNCI_USER)
            .apply()
    }

    // ---------------------------------------------------------------
    // Jurnal
    // ---------------------------------------------------------------

    fun muatJurnal() {
        viewModelScope.launch {
            val u = user ?: return@launch
            val tgl = jurnalTanggal
            jurnalMemuat = true
            try {
                val j = FirestoreRepo.ambilJurnal(u.id, tgl)
                if (jurnalTanggal != tgl) {
                    jurnalMemuat = false // pengguna sudah pindah tanggal
                    return@launch
                }
                form.muatDari(j)
                form.masaDepan = u.peran == "murid" && tgl > Format.hariIni()
                form.terkunci =
                    u.peran == "murid" && (form.masaDepan || form.status == "Disetujui")
            } catch (e: Exception) {
                pesan = pesanError(e)
            }
            jurnalMemuat = false
        }
    }

    fun simpanJurnal(denganParaf: Boolean) {
        viewModelScope.launch {
            val u = user ?: return@launch
            val tgl = jurnalTanggal
            try {
                if (u.peran == "murid" && tgl > Format.hariIni()) {
                    pesan = "Jurnal untuk tanggal yang belum terjadi tidak dapat diisi."
                    return@launch
                }
                if (u.peran == "murid" && form.status == "Disetujui") {
                    pesan = "Jurnal ini telah disetujui guru dan tidak dapat diubah lagi."
                    return@launch
                }
                FirestoreRepo.upsertJurnal(u.id, tgl, form.keMapJurnal(u.id, tgl))
                if (denganParaf) {
                    FirestoreRepo.aturParaf(u.id, tgl, Format.sekarangParaf())
                    pesan = "Paraf orang tua berhasil tersimpan."
                } else {
                    pesan = "Jurnal tersimpan dan tersinkron ke database."
                }
                muatJurnal()
            } catch (e: Exception) {
                pesan = pesanError(e)
            }
        }
    }

    fun gantiTanggalJurnal(geser: Long) {
        jurnalTanggal = try {
            LocalDate.parse(jurnalTanggal).plusDays(geser).toString()
        } catch (e: Exception) {
            Format.hariIni()
        }
    }

    /** Buka jurnal untuk tanggal tertentu (dari kalender). */
    fun bukaJurnal(tanggal: String) {
        jurnalTanggal = tanggal
        tab = Tab.Jurnal
    }

    // ---------------------------------------------------------------
    // Admin
    // ---------------------------------------------------------------

    fun gantiTanggalAdmin(geser: Long) {
        adminTanggal = try {
            LocalDate.parse(adminTanggal).plusDays(geser).toString()
        } catch (e: Exception) {
            Format.hariIni()
        }
    }

    fun simpanPersetujuan(idJurnal: String, status: String, catatan: String) {
        viewModelScope.launch {
            try {
                FirestoreRepo.updatePersetujuan(idJurnal, status, catatan)
                pesan = "Persetujuan tersimpan."
                adminReload++
            } catch (e: Exception) {
                pesan = pesanError(e)
            }
        }
    }

    // ---------------------------------------------------------------
    // Data (backup / restore / unggah CSV / kosongkan) — khusus guru
    // ---------------------------------------------------------------

    fun eksporBackup(uri: Uri) {
        viewModelScope.launch {
            sibukData = true
            try {
                val json = FirestoreRepo.eksporBackup()
                val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: throw Exception("Tidak dapat menulis berkas cadangan.")
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                pesan = "Backup berhasil diunduh."
            } catch (e: Exception) {
                pesan = pesanError(e)
            } finally {
                sibukData = false
            }
        }
    }

    fun pulihkanBackup(uri: Uri) {
        viewModelScope.launch {
            sibukData = true
            try {
                val isi = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw Exception("Tidak dapat membaca berkas cadangan.")
                val n = FirestoreRepo.imporBackup(isi)
                pesan = "Cadangan berhasil dipulihkan ($n data)."
            } catch (e: Exception) {
                pesan = pesanError(e)
            } finally {
                sibukData = false
            }
        }
    }

    fun unggahMurid(uri: Uri) {
        viewModelScope.launch {
            sibukData = true
            try {
                val isi = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw Exception("Tidak dapat membaca berkas CSV.")
                val n = FirestoreRepo.unggahMuridCsv(isi)
                pesan = "$n data murid berhasil diunggah."
            } catch (e: Exception) {
                pesan = pesanError(e)
            } finally {
                sibukData = false
            }
        }
    }

    fun kosongkanJurnal() {
        viewModelScope.launch {
            sibukData = true
            try {
                FirestoreRepo.kosongkanJurnal()
                pesan = "Semua data jurnal telah dikosongkan."
            } catch (e: Exception) {
                pesan = pesanError(e)
            } finally {
                sibukData = false
            }
        }
    }
}
