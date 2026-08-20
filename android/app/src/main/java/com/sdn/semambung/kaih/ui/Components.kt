package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Warna bantu — palet Tailwind yang dipakai versi web. */
object Warna {
    val Sky900 = Color(0xFF0C4A6E)
    val Sky800 = Color(0xFF075985)
    val Sky700 = Color(0xFF0369A1)
    val Sky600 = Color(0xFF0284C7)
    val Sky50 = Color(0xFFF0F9FF)
    val Sky100 = Color(0xFFE0F2FE)
    val Emerald50 = Color(0xFFECFDF5)
    val Emerald700 = Color(0xFF047857)
    val Emerald500 = Color(0xFF10B981)
    val Rose50 = Color(0xFFFFF1F2)
    val Rose600 = Color(0xFFE11D48)
    val Rose400 = Color(0xFFFB7185)
    val Amber50 = Color(0xFFFFFBEB)
    val Amber400 = Color(0xFFFBBF24)
    val Amber700 = Color(0xFFB45309)
    val Violet50 = Color(0xFFF5F3FF)
    val Orange50 = Color(0xFFFFF7ED)
    val Indigo50 = Color(0xFFEEF2FF)
    val Teal50 = Color(0xFFF0FDFA)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate700 = Color(0xFF334155)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
}

/** Kartu putih standar ala versi web. */
@Composable
fun Kartu(
    modifier: Modifier = Modifier,
    borderWarna: Color = Color(0xFFE0F2FE),
    isi: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderWarna)
    ) {
        Box(Modifier.padding(16.dp)) { isi() }
    }
}

/** Status pemuatan data dari Firestore. */
sealed interface Muat<out T> {
    data object Memuat : Muat<Nothing>
    data class Sukses<T>(val data: T) : Muat<T>
    data class Gagal(val pesan: String) : Muat<Nothing>
}

/** Bidang isian teks standar. */
@Composable
fun Isian(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}

/** Baris centang (checkbox) dengan label. */
@Composable
fun TandaCentang(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(label, fontSize = 14.sp)
    }
}

/** Pilihan dropdown sederhana (pengganti <select>). */
@Composable
fun SelectField(
    label: String,
    options: List<String>,
    selectedLabel: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(
                    text = { Text(o, maxLines = 1, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        onSelect(i)
                    }
                )
            }
        }
    }
}

/** Kartu judul kebiasaan (1..7) dengan latar berwarna, ala versi web. */
@Composable
fun KebiasaanCard(
    no: Int,
    judul: String,
    warnaHeader: Color,
    isi: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(warnaHeader)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(22.dp)
                    .background(Warna.Sky600, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("$no", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text(judul, fontWeight = FontWeight.Bold, color = Warna.Sky900, fontSize = 14.sp)
        }
        Column(Modifier.padding(12.dp)) { isi() }
    }
}

/** Bar pemuatan penuh. */
@Composable
fun IndikatorMuat() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Memuat data…", fontSize = 12.sp, color = Warna.Slate500)
        }
    }
}

/** Blok pesan galat. */
@Composable
fun BlokGagal(pesan: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Warna.Rose50, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(pesan, color = Warna.Rose600, fontSize = 13.sp)
    }
}

/** Tombol aksi umum (lebar penuh). */
@Composable
fun TombolAksi(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    warna: Color = Warna.Sky600,
    teksWarna: Color = Color.White,
    enabled: Boolean = true
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = warna,
            contentColor = teksWarna
        )
    ) {
        Text(teks, fontWeight = FontWeight.Bold)
    }
}

/** Baris navigasi tanggal: ‹ Sebelumnya | judul | Berikutnya › */
@Composable
fun BarisTanggal(
    judul: String,
    subjudul: String,
    sebelumnya: () -> Unit,
    berikutnya: () -> Unit,
    onJudulClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.OutlinedButton(
            onClick = sebelumnya,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("← Sebelumnya", fontSize = 12.sp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .then(if (onJudulClick != null) Modifier.clickable { onJudulClick() } else Modifier)
        ) {
            Text(judul, fontWeight = FontWeight.Black, color = Warna.Sky800, fontSize = 16.sp)
            Text(subjudul, fontSize = 12.sp, color = Warna.Slate500)
        }
        androidx.compose.material3.OutlinedButton(
            onClick = berikutnya,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Berikutnya →", fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------------
// Komponen formulir rekap (tabel 7 kebiasaan ala versi web)
// ---------------------------------------------------------------------

val PinkRekap = Color(0xFFDF6E99)
val PinkRekapBg = Color(0xFFED7FAC)
val TeksRekap = Color(0xFF3C3C3C)

/** Tabel rekap: header 2 baris + 7 kebiasaan dengan kolom Belum/Sudah Terbiasa. */
@Composable
fun TabelRekap(lengkap: Boolean) {
    val kebiasaan = com.sdn.semambung.kaih.data.RekapLogic.KEBIASAAN.values.toList()
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, PinkRekap)
    ) {
        // Baris header 1
        Row(Modifier.fillMaxWidth()) {
            SelTabel(0.1f, "No.", tebal = true, baris = 2, bg = PinkRekapBg)
            SelTabel(0.5f, "Tujuh Kebiasaan Anak\nIndonesia Hebat", tebal = true, baris = 2, bg = PinkRekapBg)
            SelTabel(0.4f, "Penerapan 7 Kebiasaan\nAnak Indonesia Hebat", tebal = true, baris = 2, bg = PinkRekapBg)
        }
        // Baris header 2
        Row(Modifier.fillMaxWidth()) {
            SelTabel(0.1f, "", bg = PinkRekapBg)
            SelTabel(0.5f, "", bg = PinkRekapBg)
            SelTabel(0.2f, "Belum Terbiasa", tebal = true, bg = PinkRekapBg)
            SelTabel(0.2f, "Sudah Terbiasa", tebal = true, bg = PinkRekapBg)
        }
        // 7 baris kebiasaan
        kebiasaan.forEachIndexed { i, nama ->
            Row(Modifier.fillMaxWidth()) {
                SelTabel(0.1f, "${i + 1}.")
                SelTabel(0.5f, nama)
                SelTabel(0.2f, if (lengkap) "" else "✓", centang = true)
                SelTabel(0.2f, if (lengkap) "✓" else "", centang = true)
            }
        }
    }
}

@Composable
private fun RowScope.SelTabel(
    berat: Float,
    teks: String,
    tebal: Boolean = false,
    baris: Int = 1,
    bg: Color = Color.White,
    centang: Boolean = false
) {
    Column(
        Modifier
            .weight(berat)
            .border(1.dp, PinkRekap)
            .background(bg)
            .padding(vertical = if (baris > 1) 10.dp else 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        teks.split("\n").forEach { b ->
            Text(
                b,
                fontSize = if (centang) 16.sp else 12.sp,
                fontWeight = if (tebal) FontWeight.Bold else FontWeight.Normal,
                color = TeksRekap,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Judul rekap dalam pil kuning, ala versi web. */
@Composable
fun PilJudulRekap(teks: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7D900), RoundedCornerShape(45.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            teks,
            fontWeight = FontWeight.Black,
            color = Color(0xFF252525),
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Baris tanda tangan rekap (Guru Kelas III / Orang Tua-Wali). */
@Composable
fun BarisTandaTangan(namaGuru: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Menyetujui:",
            fontSize = 13.sp,
            color = TeksRekap,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Guru Kelas III", fontSize = 14.sp, color = TeksRekap)
                Spacer(Modifier.height(18.dp))
                Text(
                    namaGuru,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeksRekap,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Orang Tua/Wali", fontSize = 14.sp, color = TeksRekap)
                Spacer(Modifier.height(18.dp))
                Text("_____________________", fontSize = 14.sp, color = TeksRekap)
            }
        }
    }
}

/** Keterangan di bawah formulir rekap. */
@Composable
fun KeteranganRekap() {
    Text(
        "Keterangan:\n" +
            "Beri tanda checklist (✓) sesuai pilihan.\n" +
            "Belum terbiasa, jika anak belum melaksanakan 7 kebiasaan setiap hari.\n" +
            "Sudah terbiasa, jika anak sudah melaksanakan 7 kebiasaan setiap hari.",
        fontSize = 11.sp,
        color = TeksRekap
    )
}
