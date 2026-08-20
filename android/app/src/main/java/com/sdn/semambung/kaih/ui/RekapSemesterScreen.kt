package com.sdn.semambung.kaih.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdn.semambung.kaih.data.FirestoreRepo
import com.sdn.semambung.kaih.data.RekapLogic
import com.sdn.semambung.kaih.util.Format
import com.sdn.semambung.kaih.util.RecapPdf
import com.sdn.semambung.kaih.util.pesanError

/** Layar Rekap Semester — Ganjil (Jul–Des) / Genap (Jan–Jun), ala versi web. */
@Composable
fun RekapSemesterScreen(vm: KaihViewModel) {
    var muat by remember { mutableStateOf<Muat<RekapLogic.RekapSemester>>(Muat.Memuat) }
    val user = vm.user ?: return
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(vm.rekapSem, vm.rekapIdMurid, vm.rekapReload, user.id) {
        muat = Muat.Memuat
        muat = try {
            val id = vm.rekapIdMurid ?: FirestoreRepo.daftarMurid(true).firstOrNull()?.id
            if (id == null) {
                Muat.Gagal("Belum ada data murid.")
            } else {
                Muat.Sukses(RekapLogic.muatRekapSemester(id, vm.rekapSem))
            }
        } catch (e: Exception) {
            Muat.Gagal(pesanError(e))
        }
    }

    val launcherPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            val data = (muat as? Muat.Sukses)?.data
            if (data != null) {
                val nama = data.profil?.nama ?: "Murid"
                RecapPdf.buat(
                    context = context,
                    uri = it,
                    judul = "Rekapitulasi Pemantauan dan Penilaian Tujuh\n" +
                        "Kebiasaan Anak Indonesia Hebat Semester",
                    periode = "Semester : ${data.sem.replaceFirstChar { it.uppercase() }}",
                    info = listOf(
                        "Sekolah" to "SD Negeri Semambung",
                        "Nama" to nama,
                        "Kelas" to (data.profil?.kelas?.ifBlank { "Kelas III" } ?: "Kelas III")
                    ),
                    bulan = data.bulanSemester,
                    kebiasaan = RekapLogic.KEBIASAAN.values.map { namaKebiasaan ->
                        namaKebiasaan to data.lengkap
                    },
                    namaGuru = "Aguk Rudianto, S.Pd."
                )
                vm.pesan = "PDF rekap semester berhasil dibuat."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Pilih semester
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = vm.rekapSem == "ganjil",
                onClick = { vm.rekapSem = "ganjil" },
                label = { Text("Semester Ganjil", fontSize = 12.sp) }
            )
            FilterChip(
                selected = vm.rekapSem == "genap",
                onClick = { vm.rekapSem = "genap" },
                label = { Text("Semester Genap", fontSize = 12.sp) }
            )
        }

        if (user.peran == "guru" || user.peran == "kepala_sekolah") {
            Spacer(Modifier.height(10.dp))
            val data = (muat as? Muat.Sukses)?.data
            if (data != null && data.daftarMurid.isNotEmpty()) {
                val idSaatIni = vm.rekapIdMurid ?: data.daftarMurid.first().id
                SelectField(
                    label = "Pilih Murid",
                    options = data.daftarMurid.map { "${it.nis} · ${it.nama}" },
                    selectedLabel = data.daftarMurid.firstOrNull { it.id == idSaatIni }
                        ?.let { "${it.nis} · ${it.nama}" } ?: "",
                    onSelect = { i ->
                        vm.rekapIdMurid = data.daftarMurid[i].id
                        vm.rekapReload++
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        TombolAksi(
            teks = "🖨 Cetak PDF",
            onClick = {
                launcherPdf.launch(
                    "Rekap-Semester-${(muat as? Muat.Sukses)?.data?.profil?.nama ?: "Murid"}-" +
                        "${vm.rekapSem}.pdf"
                )
            },
            warna = Warna.Rose600
        )

        Spacer(Modifier.height(12.dp))

        when (muat) {
            is Muat.Memuat -> IndikatorMuat()
            is Muat.Gagal -> BlokGagal((muat as Muat.Gagal).pesan)
            is Muat.Sukses -> {
                val data = (muat as Muat.Sukses).data

                Kartu {
                    Column {
                        PilJudulRekap(
                            "Rekapitulasi Pemantauan dan Penilaian Tujuh\n" +
                                "Kebiasaan Anak Indonesia Hebat Semester"
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Semester : ${data.sem.replaceFirstChar { it.uppercase() }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TeksRekap,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Sekolah : SD Negeri Semambung", fontSize = 13.sp, color = TeksRekap)
                        Text(
                            "Nama : ${data.profil?.nama ?: "-"}",
                            fontSize = 13.sp,
                            color = TeksRekap
                        )
                        Text(
                            "Kelas : ${data.profil?.kelas?.ifBlank { "Kelas III" } ?: "Kelas III"}",
                            fontSize = 13.sp,
                            color = TeksRekap
                        )
                        Spacer(Modifier.height(10.dp))

                        // Grid 12 bulan dengan tanda ✓ untuk bulan dalam semester
                        data.bulanSemester.chunked(3).forEach { barisBulan ->
                            Row(Modifier.fillMaxWidth()) {
                                barisBulan.forEach { (nama, aktifBulan) ->
                                    Text(
                                        "$nama : ${if (aktifBulan) "✓" else ""}",
                                        fontSize = 12.sp,
                                        color = TeksRekap,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - barisBulan.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        TabelRekap(lengkap = data.lengkap)
                        Spacer(Modifier.height(18.dp))
                        BarisTandaTangan(namaGuru = "Aguk Rudianto, S.Pd.")
                        Spacer(Modifier.height(20.dp))
                        KeteranganRekap()
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Kriteria tuntas: seluruh ${data.jumlahHari} hari dalam semester terisi " +
                        "lengkap 7 kebiasaan.",
                    fontSize = 11.sp,
                    color = Warna.Slate500
                )
            }
        }
    }
}
