package com.sdn.semambung.kaih.testfb

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * APK uji FIREBASE: mencoba inisialisasi FirebaseApp dan
 * menampilkan hasilnya (sukses/gagal + pesan error) di layar.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isi = try {
            val app = com.google.firebase.FirebaseApp.initializeApp(this)
            "✅ FIREBASE OK\n\nFirebaseApp berhasil diinisialisasi: ${app?.name}\n\nBerarti manifest provider, resource, dan kelas Firebase sehat.\n\n(Versi uji 1)"
        } catch (t: Throwable) {
            "❌ FIREBASE GAGAL\n\n${t.javaClass.name}: ${t.message}\n\n" +
                t.stackTrace.take(10).joinToString("\n") { "  at $it" }
        }

        val tv = TextView(this).apply {
            text = isi
            textSize = 14f
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
