package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdn.semambung.kaih.util.Format

/**
 * Layar Jurnal Harian — formulir 7 Kebiasaan Anak Indonesia Hebat.
 * Logika kunci sama dengan versi web:
 *  - murid tidak bisa mengisi tanggal masa depan
 *  - jurnal yang sudah "Disetujui" tidak bisa diubah murid
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JurnalScreen(vm: KaihViewModel) {
    val user = vm.user ?: return
    val form = vm.form
    val tanggal = vm.jurnalTanggal
    val aktif = !form.terkunci

    LaunchedEffect(tanggal, user.id) { vm.muatJurnal() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        BarisTanggal(
            judul = Format.hariIndonesia(tanggal).ifEmpty { "—" },
            subjudul = Format.tanggalIndonesia(tanggal),
            sebelumnya = { vm.gantiTanggalJurnal(-1) },
            berikutnya = { vm.gantiTanggalJurnal(1) }
        )

        Spacer(Modifier.height(12.dp))

        // Banner status persetujuan guru
        val status = form.status.ifEmpty { "Belum dikirim" }
        val (bannerBg, bannerFg) = when (form.status) {
            "Disetujui" -> Warna.Emerald50 to Warna.Emerald700
            "Perlu Perbaikan" -> Warna.Rose50 to Warna.Rose600
            else -> Warna.Amber50 to Warna.Amber700
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(bannerBg, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                "Status Persetujuan Guru: $status",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = bannerFg
            )
            if (form.catatanGuru.isNotBlank()) {
                Text(
                    "Catatan Guru: ${form.catatanGuru}",
                    fontSize = 12.sp,
                    color = bannerFg
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Banner pengunci
        if (form.masaDepan) {
            BannerInfo(
                "🔒 Jurnal untuk tanggal yang belum terjadi tidak dapat diisi. " +
                    "Silakan isi jurnal hari ini atau hari yang telah terlewat.",
                Warna.Slate100, Warna.Slate500
            )
        } else if (form.status == "Disetujui" && user.peran == "murid") {
            BannerInfo(
                "🔒 Jurnal ini telah disetujui guru dan tidak dapat diubah lagi.",
                Warna.Emerald50, Warna.Emerald700
            )
        }

        Spacer(Modifier.height(10.dp))

        if (vm.jurnalMemuat) {
            IndikatorMuat()
        } else {
            // Judul formulir
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Warna.Sky600, Color(0xFF06B6D4))
                        ),
                        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    "Jurnal 7 Kebiasaan Anak Indonesia Hebat",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                Text(
                    "Isi aktivitasmu hari ini dengan jujur dan mandiri.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(Modifier.alpha(if (aktif) 1f else 0.55f)) {
                // 1. Bangun Pagi
                KebiasaanCard(1, "Bangun Pagi", Warna.Sky50) {
                    TandaCentang(
                        "Sudah bangun pagi",
                        form.bangunPagi,
                        { form.bangunPagi = it },
                        aktif
                    )
                    Spacer(Modifier.height(4.dp))
                    Isian(
                        "Jam bangun",
                        form.jamBangun,
                        { form.jamBangun = it },
                        placeholder = "Contoh: 05:30",
                        enabled = aktif
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 2. Beribadah
                KebiasaanCard(2, "Beribadah", Warna.Violet50) {
                    Text(
                        "Centang semua ibadah yang dilakukan hari ini:",
                        fontSize = 12.sp,
                        color = Warna.Slate500
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Subuh", "Zuhur", "Ashar", "Magrib", "Isya").forEach { nama ->
                            FilterChip(
                                selected = nama in form.ibadah,
                                onClick = {
                                    if (nama in form.ibadah) form.ibadah.remove(nama)
                                    else form.ibadah.add(nama)
                                },
                                label = { Text(nama, fontSize = 12.sp) },
                                enabled = aktif
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Isian(
                        "Ibadah lainnya",
                        form.ibadahLainnya,
                        { form.ibadahLainnya = it },
                        placeholder = "Contoh: kebaktian, sembahyang...",
                        enabled = aktif
                    )
                    Spacer(Modifier.height(8.dp))
                    Isian(
                        "Mengaji Al-Qur'an/Iqra halaman",
                        form.mengaji,
                        { form.mengaji = it },
                        placeholder = "Contoh: Iqra 3 halaman 12",
                        enabled = aktif
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 3. Berolahraga
                KebiasaanCard(3, "Berolahraga", Warna.Emerald50) {
                    TandaCentang(
                        "Sudah berolahraga",
                        form.berolahraga,
                        { form.berolahraga = it },
                        aktif
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Isian(
                            "Jam",
                            form.jamOlahraga,
                            { form.jamOlahraga = it },
                            placeholder = "16:00",
                            modifier = Modifier.width(110.dp),
                            enabled = aktif
                        )
                        Isian(
                            "Jenis olahraga",
                            form.jenisOlahraga,
                            { form.jenisOlahraga = it },
                            placeholder = "Jenis olahraga",
                            enabled = aktif
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 4. Makan Sehat dan Bergizi
                KebiasaanCard(4, "Makan Sehat dan Bergizi", Warna.Orange50) {
                    Isian(
                        "Pagi",
                        form.makanPagi,
                        { form.makanPagi = it },
                        placeholder = "Makanan/minuman pagi hari",
                        enabled = aktif
                    )
                    Spacer(Modifier.height(8.dp))
                    Isian(
                        "Siang",
                        form.makanSiang,
                        { form.makanSiang = it },
                        placeholder = "Makanan/minuman siang hari",
                        enabled = aktif
                    )
                    Spacer(Modifier.height(8.dp))
                    Isian(
                        "Sore",
                        form.makanMalam,
                        { form.makanMalam = it },
                        placeholder = "Makanan/minuman sore hari",
                        enabled = aktif
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 5. Gemar Belajar
                KebiasaanCard(5, "Gemar Belajar", Warna.Indigo50) {
                    TandaCentang(
                        "Sudah gemar belajar",
                        form.gemarBelajar,
                        { form.gemarBelajar = it },
                        aktif
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Isian(
                            "Jam",
                            form.jamBelajar,
                            { form.jamBelajar = it },
                            placeholder = "19:00",
                            modifier = Modifier.width(110.dp),
                            enabled = aktif
                        )
                        Isian(
                            "Buku yang dibaca",
                            form.bukuDibaca,
                            { form.bukuDibaca = it },
                            placeholder = "Buku yang dibaca",
                            enabled = aktif
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Isian(
                        "Informasi tentang buku",
                        form.informasiBuku,
                        { form.informasiBuku = it },
                        placeholder = "Informasi tentang buku...",
                        enabled = aktif
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 6. Bermasyarakat
                KebiasaanCard(6, "Bermasyarakat", Warna.Teal50) {
                    TandaCentang(
                        "Sudah bermasyarakat",
                        form.bermasyarakat,
                        { form.bermasyarakat = it },
                        aktif
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Isian(
                            "Jam",
                            form.jamBermasyarakat,
                            { form.jamBermasyarakat = it },
                            placeholder = "16:30",
                            modifier = Modifier.width(110.dp),
                            enabled = aktif
                        )
                        Isian(
                            "Kegiatan yang dilakukan",
                            form.kegiatanMasyarakat,
                            { form.kegiatanMasyarakat = it },
                            placeholder = "Kegiatan yang dilakukan...",
                            enabled = aktif
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 7. Tidur Cepat
                KebiasaanCard(7, "Tidur Cepat", Warna.Rose50) {
                    TandaCentang(
                        "Sudah tidur cepat",
                        form.tidurCepat,
                        { form.tidurCepat = it },
                        aktif
                    )
                    Spacer(Modifier.height(4.dp))
                    Isian(
                        "Jam tidur",
                        form.jamTidur,
                        { form.jamTidur = it },
                        placeholder = "Contoh: 20:30",
                        enabled = aktif
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TombolAksi(
                teks = "💾 Simpan & Sinkronkan",
                onClick = { vm.simpanJurnal(denganParaf = false) },
                enabled = aktif
            )
            Spacer(Modifier.height(10.dp))
            TombolAksi(
                teks = if (form.paraf.isNotBlank()) {
                    "✍ Simpan, Sinkronkan & Paraf Orang Tua ✓"
                } else {
                    "✍ Simpan, Sinkronkan & Paraf Orang Tua"
                },
                onClick = { vm.simpanJurnal(denganParaf = true) },
                warna = Warna.Amber400,
                teksWarna = Color(0xFF451A03),
                enabled = aktif
            )
            if (form.paraf.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "✓ Sudah paraf · ${form.paraf}",
                    fontSize = 12.sp,
                    color = Warna.Emerald700,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BannerInfo(teks: String, bg: Color, fg: Color) {
    Text(
        teks,
        fontSize = 13.sp,
        color = fg,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
}
