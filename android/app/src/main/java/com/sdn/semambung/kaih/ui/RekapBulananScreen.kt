package com.sdn.semambung.kaih.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdn.semambung.kaih.data.FirestoreRepo
import com.sdn.semambung.kaih.data.RekapLogic
import com.sdn.semambung.kaih.util.Format
import com.sdn.semambung.kaih.util.RecapPdf
import com.sdn.semambung.kaih.util.pesanError

/** Layar Rekap Bulanan — formulir rekap + statistik pengerjaan jurnal. */
@Composable
fun RekapBulananScreen(vm: KaihViewModel) {
    var muat by remember { mutableStateOf<Muat<RekapLogic.RekapBulanan>>(Muat.Memuat) }
    val user = vm.user ?: return
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(vm.rekapBulan, vm.rekapIdMurid, vm.rekapReload, user.id) {
        muat = Muat.Memuat
        muat = try {
            val id = vm.rekapIdMurid ?: FirestoreRepo.daftarMurid(true).firstOrNull()?.id
            if (id == null) {
                Muat.Gagal("Belum ada data murid.")
            } else {
                Muat.Sukses(RekapLogic.muatRekapBulanan(id, vm.rekapBulan))
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
                val file = "Rekap-Bulanan-${nama.replace(" ", "-")}-${vm.rekapBulan}.pdf"
                RecapPdf.buat(
                    context = context,
                    uri = it,
                    judul = "Rekapitulasi Pemantauan dan Penilaian\nTujuh Kebiasaan Anak Indonesia Hebat",
                    periode = "Bulan : ${Format.bulanIndonesia(vm.rekapBulan)}",
                    info = listOf(
                        "Sekolah" to "SD Negeri Semambung",
                        "Nama" to nama,
                        "Kelas" to (data.profil?.kelas?.ifBlank { "Kelas III" } ?: "Kelas III")
                    ),
                    bulan = null,
                    kebiasaan = RekapLogic.KEBIASAAN.values.map { namaKebiasaan ->
                        namaKebiasaan to data.lengkap
                    },
                    namaGuru = "Aguk Rudianto, S.Pd."
                )
                vm.pesan = "PDF rekap berhasil dibuat."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Kontrol: bulan + pilih murid + cetak
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.rekapBulan = vm.rekapBulan.minusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Bulan lalu")
            }
            Text(
                Format.bulanIndonesia(vm.rekapBulan),
                fontWeight = FontWeight.Black,
                color = Warna.Sky800,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { vm.rekapBulan = vm.rekapBulan.plusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Bulan berikut")
            }
        }

        if (user.peran == "guru" || user.peran == "kepala_sekolah") {
            Spacer(Modifier.height(8.dp))
            val data = (muat as? Muat.Sukses)?.data
            if (data != null) {
                val pilihan = data.daftarMurid
                if (pilihan.isNotEmpty()) {
                    val idSaatIni = vm.rekapIdMurid ?: pilihan.first().id
                    SelectField(
                        label = "Pilih Murid",
                        options = pilihan.map { "${it.nis} · ${it.nama}" },
                        selectedLabel = pilihan.firstOrNull { it.id == idSaatIni }
                            ?.let { "${it.nis} · ${it.nama}" } ?: "",
                        onSelect = { i ->
                            vm.rekapIdMurid = pilihan[i].id
                            vm.rekapReload++
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TombolAksi(
                teks = "🖨 Cetak PDF",
                onClick = {
                    launcherPdf.launch(
                        "Rekap-Bulanan-${(muat as? Muat.Sukses)?.data?.profil?.nama ?: "Murid"}.pdf"
                    )
                },
                modifier = Modifier.weight(1f),
                warna = Warna.Rose600
            )
        }

        Spacer(Modifier.height(12.dp))

        when (muat) {
            is Muat.Memuat -> IndikatorMuat()
            is Muat.Gagal -> BlokGagal((muat as Muat.Gagal).pesan)
            is Muat.Sukses -> {
                val data = (muat as Muat.Sukses).data

                // ---- Formulir rekap resmi ----
                Kartu {
                    Column {
                        PilJudulRekap(
                            "Rekapitulasi Pemantauan dan Penilaian\n" +
                                "Tujuh Kebiasaan Anak Indonesia Hebat"
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Bulan : ${Format.bulanIndonesia(vm.rekapBulan)}",
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
                        Spacer(Modifier.height(14.dp))
                        TabelRekap(lengkap = data.lengkap)
                        Spacer(Modifier.height(18.dp))
                        BarisTandaTangan(namaGuru = "Aguk Rudianto, S.Pd.")
                        Spacer(Modifier.height(20.dp))
                        KeteranganRekap()
                    }
                }

                // ---- Statistik (guru / kepala sekolah) ----
                if (user.peran == "guru" || user.peran == "kepala_sekolah") {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Statistik Pengerjaan Jurnal · ${Format.bulanIndonesia(vm.rekapBulan)}",
                        fontWeight = FontWeight.Black,
                        color = Warna.Sky800,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatRekap(
                            judul = "Sudah mengerjakan lengkap",
                            angka = data.statSelesai.size.toString(),
                            sub = "dari ${data.daftarMurid.size} siswa",
                            bg = Warna.Emerald50,
                            fg = Warna.Emerald700,
                            modifier = Modifier.weight(1f)
                        )
                        StatRekap(
                            judul = "Belum mengerjakan lengkap",
                            angka = data.statBelum.size.toString(),
                            sub = "dari ${data.daftarMurid.size} siswa",
                            bg = Warna.Rose50,
                            fg = Warna.Rose600,
                            modifier = Modifier.weight(1f)
                        )
                        StatRekap(
                            judul = "Persentase",
                            angka = "${data.persenSelesai}%",
                            sub = "lengkap",
                            bg = Color.White,
                            fg = Warna.Sky700,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Diagram batang
                    Kartu {
                        Column {
                            Text(
                                "Diagram batang pengerjaan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            val total = data.statSelesai.size + data.statBelum.size
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Warna.Rose400)
                            ) {
                                val beratHijau = if (total > 0) {
                                    (data.statSelesai.size.toFloat() / total)
                                } else 0f
                                if (beratHijau > 0f) {
                                    Box(
                                        Modifier
                                            .weight(beratHijau)
                                            .fillMaxSize()
                                            .background(Warna.Emerald500),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${data.statSelesai.size}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (beratHijau < 1f) {
                                    Box(
                                        Modifier
                                            .weight(1f - beratHijau)
                                            .fillMaxSize()
                                            .background(Warna.Rose400),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${data.statBelum.size}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("■ Sudah lengkap", fontSize = 12.sp, color = Warna.Emerald700)
                                Text("■ Belum lengkap", fontSize = 12.sp, color = Warna.Rose600)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Kartu(
                            modifier = Modifier.weight(1f),
                            borderWarna = Warna.Emerald50
                        ) {
                            Column {
                                Text(
                                    "✓ Daftar siswa sudah mengerjakan (${data.statSelesai.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Warna.Emerald700
                                )
                                Spacer(Modifier.height(8.dp))
                                if (data.statSelesai.isEmpty()) {
                                    Text("Belum ada siswa yang memenuhi jurnal lengkap.", fontSize = 12.sp, color = Warna.Slate500)
                                } else {
                                    data.statSelesai.forEachIndexed { i, m ->
                                        Text("${i + 1}. ${m.nama}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Kartu(
                            modifier = Modifier.weight(1f),
                            borderWarna = Warna.Rose50
                        ) {
                            Column {
                                Text(
                                    "! Daftar siswa belum mengerjakan lengkap (${data.statBelum.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Warna.Rose600
                                )
                                Spacer(Modifier.height(8.dp))
                                data.statBelum.forEachIndexed { i, m ->
                                    Text("${i + 1}. ${m.nama}", fontSize = 12.sp)
                                }
                            }
                        }
                    }

        Spacer(Modifier.height(10.dp))
                    Text(
                        "Kriteria lengkap: seluruh 7 kebiasaan diisi minimal 80% hari dalam bulan ini " +
                            "(minimal ${data.minimalHari} dari ${data.jumlahHari} hari).",
                        fontSize = 11.sp,
                        color = Warna.Slate500
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRekap(
    judul: String,
    angka: String,
    sub: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(judul, fontSize = 11.sp, color = fg)
        Text(angka, fontSize = 26.sp, fontWeight = FontWeight.Black, color = fg)
        Text(sub, fontSize = 10.sp, color = fg)
    }
}
