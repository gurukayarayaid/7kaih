package com.sdn.semambung.kaih.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdn.semambung.kaih.util.Format

/**
 * Layar Data (khusus guru) — padanan halaman "Backup & Restore" versi web:
 *  - Unduh backup (JSON, pengganti berkas .sqlite)
 *  - Pulihkan backup
 *  - Unggah data murid (CSV)
 *  - Kosongkan semua jurnal
 */
@Composable
fun DataScreen(vm: KaihViewModel) {
    var konfirmasiKosong by remember { mutableStateOf(false) }

    val launcherUnduh = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { vm.eksporBackup(it) }
    }

    val launcherPulihkan = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.pulihkanBackup(it) }
    }

    val launcherUnggah = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.unggahMurid(it) }
    }

    if (konfirmasiKosong) {
        AlertDialog(
            onDismissRequest = { konfirmasiKosong = false },
            title = { Text("Kosongkan Semua Jurnal") },
            text = {
                Text(
                    "Menghapus semua jurnal, tetapi data akun murid tetap ada. " +
                        "Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    konfirmasiKosong = false
                    vm.kosongkanJurnal()
                }) { Text("Ya, Kosongkan", color = Warna.Rose600) }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiKosong = false }) { Text("Batal") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (vm.sibukData) {
            IndikatorMuat()
            Spacer(Modifier.height(8.dp))
        }

        // Backup
        Kartu {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, tint = Warna.Sky600)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup Data", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Warna.Sky800)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unduh seluruh data murid dan jurnal dalam berkas JSON (pengganti berkas SQLite).",
                    fontSize = 13.sp,
                    color = Warna.Slate500
                )
                Spacer(Modifier.height(12.dp))
                TombolAksi(
                    teks = "⬇ Unduh Backup",
                    onClick = {
                        launcherUnduh.launch("Cadangan-7-KAIH-${Format.hariIni()}.json")
                    },
                    enabled = !vm.sibukData
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Restore
        Kartu {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = Warna.Amber700)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Data", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Warna.Sky800)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pulihkan data dari berkas cadangan JSON. Seluruh data saat ini akan diganti.",
                    fontSize = 13.sp,
                    color = Warna.Slate500
                )
                Spacer(Modifier.height(12.dp))
                TombolAksi(
                    teks = "↻ Pulihkan Backup",
                    onClick = { launcherPulihkan.launch(arrayOf("application/json", "text/plain")) },
                    warna = Warna.Amber400,
                    teksWarna = Warna.Sky900,
                    enabled = !vm.sibukData
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Unggah data murid
        Kartu {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = Warna.Emerald700)
                    Spacer(Modifier.width(8.dp))
                    Text("Unggah Data Semua Murid", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Warna.Sky800)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unggah CSV dengan kolom: nama, username, kata_sandi, kelas.",
                    fontSize = 13.sp,
                    color = Warna.Slate500
                )
                Spacer(Modifier.height(12.dp))
                TombolAksi(
                    teks = "↑ Unggah Data Murid",
                    onClick = {
                        launcherUnggah.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
                    },
                    warna = Warna.Emerald700,
                    enabled = !vm.sibukData
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Kosongkan jurnal
        Column(
            Modifier
                .fillMaxWidth()
                .background(Warna.Rose50, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Warna.Rose600)
                Spacer(Modifier.width(8.dp))
                Text("Kosongkan Data Jurnal", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Warna.Rose600)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Menghapus semua jurnal, tetapi data akun murid tetap ada. Tindakan ini tidak dapat dibatalkan.",
                fontSize = 13.sp,
                color = Warna.Slate700
            )
            Spacer(Modifier.height(12.dp))
            TombolAksi(
                teks = "Kosongkan Semua Jurnal",
                onClick = { konfirmasiKosong = true },
                warna = Warna.Rose600,
                enabled = !vm.sibukData
            )
        }
    }
}
