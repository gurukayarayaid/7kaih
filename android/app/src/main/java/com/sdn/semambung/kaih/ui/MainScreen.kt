package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Layar utama — berisi menu TAB BAWAH layar (pengganti menu tab atas versi web).
 * Daftar tab menyesuaikan peran pengguna.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: KaihViewModel) {
    val user = vm.user ?: return
    SideEffect { com.sdn.semambung.kaih.DebugLog.log("MainScreen composed tab=${vm.tab}") }
    val tabs = tabsUntuk(user.peran)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.pesan) {
        vm.pesan?.let {
            snackbar.showSnackbar(it)
            vm.pesan = null
        }
    }
    LaunchedEffect(tabs) {
        if (vm.tab !in tabs) vm.tab = tabs.first()
    }

    Scaffold(
        containerColor = Warna.Sky50,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "✦ Aplikasi 7 KAIH",
                            fontWeight = FontWeight.Black,
                            color = Warna.Sky700,
                            fontSize = 17.sp
                        )
                        Text(
                            "${user.nama} · ${user.peranLabel}" +
                                (if (user.kelas.isNotBlank()) " · ${user.kelas}" else ""),
                            fontSize = 10.sp,
                            color = Warna.Sky600
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.keluar() }) {
                        Icon(
                            Icons.Filled.ExitToApp,
                            contentDescription = "Keluar",
                            tint = Warna.Rose600
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = vm.tab == t,
                        onClick = { vm.tab = t },
                        icon = { Icon(t.ikon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (vm.tab) {
                Tab.Jurnal -> JurnalScreen(vm)
                Tab.Kalender -> KalenderScreen(vm)
                Tab.RekapBulanan -> RekapBulananScreen(vm)
                Tab.RekapSemester -> RekapSemesterScreen(vm)
                Tab.Admin -> AdminScreen(vm)
                Tab.Data -> DataScreen(vm)
                Tab.Profil -> ProfilScreen(vm)
            }
        }
    }
}
