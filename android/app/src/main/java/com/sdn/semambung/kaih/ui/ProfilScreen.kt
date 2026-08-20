package com.sdn.semambung.kaih.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Layar Profil (murid) — info akun dan tombol keluar. */
@Composable
fun ProfilScreen(vm: KaihViewModel) {
    val user = vm.user ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Avatar inisial
        Box(
            Modifier
                .size(88.dp)
                .background(Warna.Sky600, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.nama.take(1).ifEmpty { "?" },
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(user.nama, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Warna.Sky800)
        Text(user.peranLabel, fontSize = 13.sp, color = Warna.Sky600)
        if (user.kelas.isNotBlank()) {
            Text(user.kelas, fontSize = 12.sp, color = Warna.Slate500)
        }

        Spacer(Modifier.height(20.dp))

        Kartu {
            Column {
                BarisInfo("Nama", user.nama)
                BarisInfo("Peran", user.peranLabel)
                if (user.kelas.isNotBlank()) BarisInfo("Kelas", user.kelas)
                if (user.nis.isNotBlank()) BarisInfo("NIS", user.nis)
                BarisInfo("Username", user.username.ifBlank { "-" })
            }
        }

        Spacer(Modifier.height(20.dp))

        TombolAksi(
            teks = "Keluar",
            onClick = { vm.keluar() },
            warna = Warna.Rose600
        )
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            fontSize = 13.sp,
            color = Warna.Slate500,
            modifier = Modifier.width(110.dp)
        )
        Text(
            nilai,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Warna.Slate700
        )
    }
}
