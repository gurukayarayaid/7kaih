package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.sdn.semambung.kaih.util.Format
import com.sdn.semambung.kaih.util.pesanError
import java.time.LocalDate

/**
 * Layar "Lihat Pengisian Jurnal" (khusus murid) — kalender bulanan.
 * Hari hijau = sudah mengisi jurnal; hari abu-abu = tanggal masa depan.
 * Tekan tanggal untuk membuka jurnal tanggal tersebut.
 */
@Composable
fun KalenderScreen(vm: KaihViewModel) {
    val user = vm.user ?: return
    var muat by remember { mutableStateOf<Muat<Set<String>>>(Muat.Memuat) }

    LaunchedEffect(vm.kalenderBulan, user.id) {
        muat = Muat.Memuat
        muat = try {
            Muat.Sukses(
                FirestoreRepo.jurnalMurid(user.id)
                    .map { it.tanggal }
                    .filter { it.startsWith(vm.kalenderBulan.toString()) }
                    .toSet()
            )
        } catch (e: Exception) {
            Muat.Gagal(pesanError(e))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigasi bulan
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { vm.kalenderBulan = vm.kalenderBulan.minusMonths(1) }) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Bulan lalu"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Lihat Pengisian Jurnal",
                    fontWeight = FontWeight.Black,
                    color = Warna.Sky800,
                    fontSize = 16.sp
                )
                Text(
                    Format.bulanIndonesia(vm.kalenderBulan),
                    fontSize = 13.sp,
                    color = Warna.Slate500
                )
            }
            IconButton(onClick = { vm.kalenderBulan = vm.kalenderBulan.plusMonths(1) }) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Bulan berikut"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Kartu {
            Column {
                // Nama hari (mulai Senin, sama seperti versi web)
                Row(Modifier.fillMaxWidth()) {
                    Format.NAMA_HARI.forEach { hari ->
                        Text(
                            hari.take(3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Warna.Slate500,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                when (muat) {
                    is Muat.Memuat -> IndikatorMuat()
                    is Muat.Gagal -> BlokGagal((muat as Muat.Gagal).pesan)
                    is Muat.Sukses -> {
                        val terisi = (muat as Muat.Sukses<Set<String>>).data
                        val hariPertama = vm.kalenderBulan.atDay(1).dayOfWeek.value // 1=Senin
                        val jumlahHari = vm.kalenderBulan.lengthOfMonth()
                        val sel = List(hariPertama - 1) { null } + (1..jumlahHari).map { it }

                        sel.chunked(7).forEach { minggu ->
                            Row(Modifier.fillMaxWidth()) {
                                minggu.forEach { d ->
                                    Box(Modifier.weight(1f).padding(2.dp).aspectRatio(1f)) {
                                        if (d != null) {
                            SelKalender(
                                hari = d,
                                terisi = "${LocalDate.of(vm.kalenderBulan.year, vm.kalenderBulan.month, d)}" in terisi,
                                masaDepan = LocalDate.of(vm.kalenderBulan.year, vm.kalenderBulan.month, d)
                                    .toString() > LocalDate.now().toString(),
                                onClick = {
                                    vm.bukaJurnal(
                                        LocalDate.of(vm.kalenderBulan.year, vm.kalenderBulan.month, d).toString()
                                    )
                                }
                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Legenda(warna = Warna.Emerald500, teks = "Sudah mengerjakan")
                    Legenda(warna = Color.White, border = true, teks = "Belum mengerjakan")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tekan salah satu tanggal untuk membuka atau mengisi jurnal pada tanggal tersebut.",
                    fontSize = 11.sp,
                    color = Warna.Slate500
                )
            }
        }
    }
}

@Composable
private fun SelKalender(hari: Int, terisi: Boolean, masaDepan: Boolean, onClick: () -> Unit) {
    val bg = when {
        masaDepan -> Warna.Slate100
        terisi -> Warna.Emerald500
        else -> Color.White
    }
    val fg = when {
        masaDepan -> Warna.Slate400
        terisi -> Color.White
        else -> Warna.Slate700
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (masaDepan) Modifier
                else Modifier.clickable { onClick() }
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$hari", fontWeight = FontWeight.Bold, color = fg, fontSize = 13.sp)
            if (terisi) {
                Text("✓ Terisi", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun Legenda(warna: Color, teks: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .height(14.dp)
                .width(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (border) Color.White else warna)
                .then(
                    if (border) {
                        Modifier.border(1.dp, Warna.Slate200, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(teks, fontSize = 12.sp, color = Warna.Slate700)
    }
}
