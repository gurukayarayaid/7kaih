package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdn.semambung.kaih.data.RekapLogic
import com.sdn.semambung.kaih.util.Format
import com.sdn.semambung.kaih.util.pesanError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Layar Admin (guru & kepala sekolah) — dashboard harian + persetujuan jurnal.
 * Fitur sama dengan versi web: statistik, diagram batang, diagram jaring laba-laba,
 * daftar murid, dan form persetujuan (Menunggu / Disetujui / Perlu Perbaikan).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(vm: KaihViewModel) {
    var muat by remember { mutableStateOf<Muat<RekapLogic.DataAdmin>>(Muat.Memuat) }
    var pilihTanggal by remember { mutableStateOf(false) }
    val user = vm.user ?: return

    LaunchedEffect(vm.adminTanggal, vm.adminReload, user.id) {
        muat = Muat.Memuat
        muat = try {
            Muat.Sukses(RekapLogic.muatAdmin(vm.adminTanggal))
        } catch (e: Exception) {
            Muat.Gagal(pesanError(e))
        }
    }

    // Dialog pilih tanggal
    if (pilihTanggal) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(vm.adminTanggal).atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { pilihTanggal = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        vm.adminTanggal = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pilihTanggal = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { pilihTanggal = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigasi tanggal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { vm.gantiTanggalAdmin(-1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Sebelumnya")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { pilihTanggal = true }
            ) {
                Text(
                    "Dashboard Jurnal Harian Murid",
                    fontWeight = FontWeight.Black,
                    color = Warna.Sky800,
                    fontSize = 15.sp
                )
                Text(
                    "Pemantauan dan persetujuan jurnal harian.",
                    fontSize = 11.sp,
                    color = Warna.Slate500
                )
                Text(
                    Format.hariIndonesia(vm.adminTanggal) + ", " +
                        Format.tanggalIndonesia(vm.adminTanggal),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Warna.Sky700
                )
            }
            IconButton(onClick = { vm.gantiTanggalAdmin(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Berikutnya")
            }
        }

        Spacer(Modifier.height(12.dp))

        when (muat) {
            is Muat.Memuat -> IndikatorMuat()
            is Muat.Gagal -> BlokGagal((muat as Muat.Gagal).pesan)
            is Muat.Sukses -> {
                val data = (muat as Muat.Sukses).data
                val total = data.semuaMurid.size

                // Kartu statistik
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatRekapAdmin(
                        "Sudah mengerjakan hari ini",
                        data.sudah.size.toString(),
                        "dari $total murid",
                        Warna.Emerald50, Warna.Emerald700,
                        Modifier.weight(1f)
                    )
                    StatRekapAdmin(
                        "Belum mengerjakan hari ini",
                        data.belum.size.toString(),
                        "dari $total murid",
                        Warna.Rose50, Warna.Rose600,
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Diagram batang
                Kartu {
                    Column {
                        Text(
                            "Diagram batang pengerjaan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        val beratHijau = if (total > 0) data.sudah.size.toFloat() / total else 0f
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Warna.Rose400)
                        ) {
                            if (beratHijau > 0f) {
                                Box(
                                    Modifier
                                        .weight(beratHijau)
                                        .fillMaxSize()
                                        .background(Warna.Emerald500),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${data.sudah.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text("${data.belum.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("■ Sudah", fontSize = 12.sp, color = Warna.Emerald700)
                            Text("■ Belum", fontSize = 12.sp, color = Warna.Rose600)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Diagram jaring laba-laba
                Kartu {
                    Column {
                        Text(
                            "Diagram jaring laba-laba · Kebiasaan dikerjakan",
                            fontWeight = FontWeight.Bold,
                            color = Warna.Sky800,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadarChart(
                                nilai = data.nilaiHabit,
                                maks = maxOf(1, total),
                                modifier = Modifier.size(190.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                RekapLogic.KEBIASAAN.forEach { (kunci, label) ->
                                    Text(
                                        "$label: ${data.nilaiHabit[kunci] ?: 0} murid",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Daftar sudah / belum
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Kartu(Modifier.weight(1f), borderWarna = Warna.Emerald50) {
                        Column {
                            Text(
                                "✓ Sudah mengerjakan (${data.sudah.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Warna.Emerald700
                            )
                            Spacer(Modifier.height(6.dp))
                            if (data.sudah.isEmpty()) {
                                Text("Belum ada data.", fontSize = 12.sp, color = Warna.Slate400)
                            } else {
                                data.sudah.forEachIndexed { i, m -> Text("${i + 1}. ${m.nama}", fontSize = 12.sp) }
                            }
                        }
                    }
                    Kartu(Modifier.weight(1f), borderWarna = Warna.Rose50) {
                        Column {
                            Text(
                                "! Belum mengerjakan (${data.belum.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Warna.Rose600
                            )
                            Spacer(Modifier.height(6.dp))
                            data.belum.forEachIndexed { i, m -> Text("${i + 1}. ${m.nama}", fontSize = 12.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "Jurnal yang dikerjakan · ${Format.tanggalIndonesia(vm.adminTanggal)}",
                    fontWeight = FontWeight.Black,
                    color = Warna.Sky800,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))

                if (data.rows.isEmpty()) {
                    Kartu {
                        Text(
                            "Belum ada jurnal yang masuk.",
                            color = Warna.Slate500,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    data.rows.forEach { row ->
                        KartuJurnalAdmin(vm, row)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/** Kartu detail jurnal + form persetujuan guru. */
@Composable
private fun KartuJurnalAdmin(vm: KaihViewModel, row: RekapLogic.JurnalAdmin) {
    val j = row.jurnal
    var status by remember(j.id) { mutableStateOf(j.statusPersetujuan.ifEmpty { "Menunggu" }) }
    var catatan by remember(j.id) { mutableStateOf(j.catatanGuru) }

    Kartu(borderWarna = Color(0xFFE0F2FE)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${row.nama}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${row.kelas.ifBlank { "-" }} · ${j.tanggal}",
                        fontSize = 11.sp,
                        color = Warna.Slate500
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            DetailKebiasaan(1, "Bangun Pagi", Warna.Sky50,
                "Status: ${j.bangunPagi.ifBlank { "Belum diisi" }} · Jam bangun: ${j.jamBangun.ifBlank { "-" }}")
            DetailKebiasaan(2, "Beribadah", Warna.Violet50,
                "Salat/ibadah dipilih: ${j.beribadah.ifBlank { "Belum diisi" }}\n" +
                    "Ibadah lainnya: ${j.ibadahLainnya.ifBlank { "-" }}\n" +
                    "Mengaji Al-Qur'an/Iqra halaman: ${j.mengajiAlquran.ifBlank { "-" }}")
            DetailKebiasaan(3, "Berolahraga", Warna.Emerald50,
                "Status: ${j.berolahraga.ifBlank { "Belum diisi" }} · Jam: ${j.jamOlahraga.ifBlank { "-" }}\n" +
                    "Jenis olahraga: ${j.jenisOlahraga.ifBlank { "-" }}")
            DetailKebiasaan(4, "Makan Sehat dan Bergizi", Warna.Orange50,
                "Pagi: ${j.makanPagi.ifBlank { "-" }}\nSiang: ${j.makanSiang.ifBlank { "-" }}\nSore: ${j.makanMalam.ifBlank { "-" }}")
            DetailKebiasaan(5, "Gemar Belajar", Warna.Indigo50,
                "Status: ${j.gemarBelajar.ifBlank { "Belum diisi" }} · Jam: ${j.jamBelajar.ifBlank { "-" }}\n" +
                    "Buku dibaca: ${j.bukuDibaca.ifBlank { "-" }}\nInformasi buku: ${j.informasiBuku.ifBlank { "-" }}")
            DetailKebiasaan(6, "Bermasyarakat", Warna.Teal50,
                "Status: ${j.bermasyarakat.ifBlank { "Belum diisi" }} · Jam: ${j.jamBermasyarakat.ifBlank { "-" }}\n" +
                    "Kegiatan: ${j.kegiatanMasyarakat.ifBlank { "-" }}")
            DetailKebiasaan(7, "Tidur Cepat", Warna.Rose50,
                "Status: ${j.tidurCepat.ifBlank { "Belum diisi" }} · Jam tidur: ${j.jamTidur.ifBlank { "-" }}")

            Spacer(Modifier.height(10.dp))

            // Status paraf
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status Paraf Orang Tua: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (j.parafOrangTua.isNotBlank()) {
                    Text(
                        "✓ Sudah paraf · ${j.parafOrangTua}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Warna.Emerald700,
                        modifier = Modifier
                            .background(Warna.Emerald50, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                } else {
                    Text(
                        "⌛ Belum paraf",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Warna.Amber700,
                        modifier = Modifier
                            .background(Warna.Amber50, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Form persetujuan
            Isian(
                label = "Catatan guru",
                value = catatan,
                onValueChange = { catatan = it },
                placeholder = "Catatan guru"
            )
            Spacer(Modifier.height(8.dp))
            SelectField(
                label = "Status",
                options = listOf("Menunggu", "Disetujui", "Perlu Perbaikan"),
                selectedLabel = status,
                onSelect = { i -> status = listOf("Menunggu", "Disetujui", "Perlu Perbaikan")[i] }
            )
            Spacer(Modifier.height(10.dp))
            TombolAksi(
                teks = "Simpan",
                onClick = { vm.simpanPersetujuan(j.id, status, catatan) }
            )
        }
    }
}

@Composable
private fun DetailKebiasaan(no: Int, judul: String, warna: Color, isi: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(warna, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text("$no. $judul", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Warna.Slate700)
        Text(isi, fontSize = 11.sp, color = Warna.Slate700)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun StatRekapAdmin(
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

/** Diagram jaring laba-laba 7 kebiasaan (padanan SVG versi web). */
@Composable
private fun RadarChart(
    nilai: Map<String, Int>,
    maks: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f - 6.dp.toPx()
        val jari = r / 3f

        // Lingkaran grid
        for (i in 1..3) {
            drawCircle(
                color = Color(0xFFCBD5E1),
                radius = jari * i,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // Sumbu
        val sudutAwal = -90.0
        for (i in 0 until 7) {
            val sudut = Math.toRadians(sudutAwal + i * 360.0 / 7.0)
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(cx, cy),
                end = Offset(cx + r * cos(sudut).toFloat(), cy + r * sin(sudut).toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }
        // Heptagon luar
        val titikLuar = List(7) { i ->
            val sudut = Math.toRadians(sudutAwal + i * 360.0 / 7.0)
            Offset(cx + r * cos(sudut).toFloat(), cy + r * sin(sudut).toFloat())
        }
        val jalurLuar = Path().apply {
            moveTo(titikLuar[0].x, titikLuar[0].y)
            titikLuar.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(jalurLuar, color = Color(0xFF94A3B8), style = Stroke(width = 1.dp.toPx()))

        // Poligon nilai
        val kunci = RekapLogic.KEBIASAAN.keys.toList()
        val skala = if (maks > 0) 1f / maks else 1f
        val titikNilai = List(7) { i ->
            val sudut = Math.toRadians(sudutAwal + i * 360.0 / 7.0)
            val rasio = (nilai[kunci[i]] ?: 0).toFloat() * skala
            Offset(cx + r * rasio * cos(sudut).toFloat(), cy + r * rasio * sin(sudut).toFloat())
        }
        val jalurNilai = Path().apply {
            moveTo(titikNilai[0].x, titikNilai[0].y)
            titikNilai.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(jalurNilai, color = Color(0x660EA5E9))
        drawPath(jalurNilai, color = Color(0xFF0284C7), style = Stroke(width = 2.dp.toPx()))
    }
}
