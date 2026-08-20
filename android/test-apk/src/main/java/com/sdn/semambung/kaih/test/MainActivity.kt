package com.sdn.semambung.kaih.test

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * APK uji MINIMAL (tanpa Compose, tanpa Firebase).
 * Jika layar ini muncul, berarti pipeline build sehat —
 * dan masalah ada di dependensi aplikasi utama.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isi = buildString {
            appendLine("✅ APK UJI MINIMAL BERHASIL")
            appendLine()
            appendLine("Ini bukti: APK dibuat dengan toolchain yang sama")
            appendLine("dengan Aplikasi 7 KAIH, dan BERJALAN NORMAL.")
            appendLine()
            appendLine("Langkah berikutnya: pasang 'Uji Firebase' (test-firebase.apk).")
            appendLine()
            appendLine("Versi: 1")
        }

        val tv = TextView(this).apply {
            text = isi
            textSize = 16f
            setTextColor(Color.rgb(12, 74, 110))
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
            addView(tv)
        }
        setContentView(ll)
    }
}
