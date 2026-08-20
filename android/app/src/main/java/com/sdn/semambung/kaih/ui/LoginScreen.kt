package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layar masuk — sama dengan halaman "Masuk" versi web:
 *  - Murid  : pilih nama dari daftar, kata sandi = NIS
 *  - Guru   : kata sandi guru
 *  - Kepala Sekolah : username + kata sandi
 */
@Composable
fun LoginScreen(vm: KaihViewModel) {
    var jenis by remember { mutableStateOf("murid") }
    var idMurid by remember { mutableStateOf<String?>(null) }
    var kataSandi by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("kepsek") }
    val snackbar = remember { SnackbarHostState() }

    SideEffect { com.sdn.semambung.kaih.DebugLog.log("LoginScreen composed (daftarSiap=${vm.daftarSiap})") }
    LaunchedEffect(Unit) { vm.muatDaftarMurid() }
    LaunchedEffect(vm.pesan) {
        vm.pesan?.let {
            snackbar.showSnackbar(it)
            vm.pesan = null
        }
    }

    val labelMurid = vm.daftarMurid.firstOrNull { it.id == idMurid }
        ?.let { "NIS ${it.nis} · ${it.nama}" }
        ?: ""

    Scaffold(
        containerColor = Warna.Sky50,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("🌟", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Aplikasi 7 KAIH",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Warna.Sky800
            )
            Text(
                "SD Negeri Semambung · Kelas III",
                fontSize = 13.sp,
                color = Warna.Slate500
            )
            Spacer(Modifier.height(20.dp))

            Kartu {
                Column {
                    Text(
                        "Masuk sebagai",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = jenis == "murid",
                            onClick = { jenis = "murid"; idMurid = null },
                            label = { Text("Murid", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = jenis == "guru",
                            onClick = { jenis = "guru" },
                            label = { Text("Guru", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = jenis == "kepala_sekolah",
                            onClick = { jenis = "kepala_sekolah" },
                            label = { Text("Kepala Sekolah", fontSize = 12.sp) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (jenis == "murid") {
                        if (!vm.daftarSiap) {
                            Text("Memuat daftar murid…", fontSize = 12.sp, color = Warna.Slate500)
                        } else {
                            SelectField(
                                label = "Nama Murid",
                                options = vm.daftarMurid.map { "NIS ${it.nis} · ${it.nama}" },
                                selectedLabel = labelMurid,
                                onSelect = { i -> idMurid = vm.daftarMurid[i].id }
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Pilih nama Murid, lalu masukkan NIS sebagai kata sandi.",
                                fontSize = 11.sp,
                                color = Warna.Sky700
                            )
                        }
                    }

                    if (jenis == "kepala_sekolah") {
                        Isian(
                            label = "Username",
                            value = username,
                            onValueChange = { username = it },
                            placeholder = "Username kepala sekolah"
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(10.dp))
                    Isian(
                        label = "Kata sandi",
                        value = kataSandi,
                        onValueChange = { kataSandi = it },
                        placeholder = if (jenis == "murid") "Masukkan NIS"
                        else if (jenis == "guru") "Kata sandi guru"
                        else "Masukkan kata sandi",
                        enabled = !vm.sibuk,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(16.dp))

                    TombolAksi(
                        teks = "Masuk ke Jurnal",
                        onClick = {
                            vm.login(jenis, idMurid, kataSandi, username)
                        },
                        enabled = !vm.sibuk
                    )
                    if (vm.sibuk) {
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Akun demo: Murid (pilih nama, kata sandi = NIS) · " +
                    "Guru (kata sandi: alal) · Kepala Sekolah (kepsek / 123456)",
                fontSize = 11.sp,
                color = Warna.Slate500,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Warna.Sky50, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            )
        }
    }
}
