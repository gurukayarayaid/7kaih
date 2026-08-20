package com.sdn.semambung.kaih

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Aktivitas bantuan debug: menampilkan pesan error crash di layar.
 * Sengaja memakai View klasik (bukan Compose) agar tetap tampil
 * meskipun error terjadi di dalam Compose/Firebase.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = intent.getStringExtra("trace") ?: "Error tidak diketahui."

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        layout.addView(
            TextView(this).apply {
                text = "⚠️ Aplikasi 7 KAIH mengalami error:\n\n$trace"
                textSize = 13f
                setTextColor(Color.rgb(127, 29, 29))
                setBackgroundColor(Color.rgb(255, 241, 242))
                setPadding(32, 32, 32, 32)
            }
        )
        layout.addView(
            TextView(this).apply {
                text = "\n\n📌 Salin / foto pesan di atas, lalu kirimkan ke pengembang " +
                    "beserta keterangan langkah sebelum error muncul."
                textSize = 13f
                setTextColor(Color.rgb(71, 85, 105))
            }
        )
        layout.addView(
            TextView(this).apply {
                text = "\nTekan tombol Kembali (Back) untuk menutup aplikasi."
                textSize = 12f
                setTextColor(Color.rgb(148, 163, 184))
            }
        )

        scroll.addView(layout)
        setContentView(scroll)
    }
}
