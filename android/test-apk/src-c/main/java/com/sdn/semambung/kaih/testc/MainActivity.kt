package com.sdn.semambung.kaih.testc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * APK uji COMPOSE mini: hanya menampilkan teks via Jetpack Compose.
 * Jika layar ini muncul -> Compose runtime + R-class sehat.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Layar()
            }
        }
    }
}

@Composable
fun Layar() {
    Text("✅ COMPOSE OK — Jetpack Compose berjalan normal di APK ini.")
}
