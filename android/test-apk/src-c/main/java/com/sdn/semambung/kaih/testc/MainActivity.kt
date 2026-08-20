package com.sdn.semambung.kaih.testc

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.ComposeView

/**
 * test3 v4 MINIMAL: ComposeView + Text, TANPA material3/icons/foundation-layout.
 * Layar menampilkan status besar; setiap langkah dicatat ke Downloads.
 */
class MainActivity : Activity() {

    private val sb = StringBuilder()
    private lateinit var logTv: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TestLog.tulis(this, "ACT: onCreate mulai")

        logTv = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.rgb(12, 74, 110))
            setPadding(32, 32, 32, 32)
        }
        val composeView = ComposeView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(logTv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(composeView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
        TestLog.tulis(this, "ACT: setContentView OK")

        tampil("v4 — Activity OK", Color.rgb(12, 74, 110))
        TestLog.tulis(this, "STEP-A: teks klasik tampil")

        handler.postDelayed({
            TestLog.tulis(this, "STEP-B: mencoba setContent Text")
            try {
                composeView.setContent {
                    BasicText("✅ COMPOSE TEXT OK (v4)")
                }
                tampil("v4 — setContent Text OK", Color.rgb(5, 150, 105))
                TestLog.tulis(this, "STEP-B: setContent Text berhasil")
            } catch (t: Throwable) {
                tampil("v4 — GAGAL: ${t.javaClass.name}\n${t.message}", Color.rgb(127, 29, 29))
                TestLog.tulis(this, "STEP-B GAGAL: ${t.javaClass.name}: ${t.message}")
            }
        }, 500)

        handler.postDelayed({
            TestLog.tulis(this, "STEP-C: masih hidup setelah 3 detik")
            tampil("v4 — masih hidup (3 detik)", Color.rgb(12, 74, 110))
        }, 3000)
    }

    private fun tampil(baris: String, warna: Int) {
        sb.appendLine(baris)
        logTv.text = sb.toString()
        logTv.setTextColor(warna)
    }
}
