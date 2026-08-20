package com.sdn.semambung.kaih.testc

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

/**
 * APK uji COMPOSE (v2): dengan penangkap error yang menampilkan
 * pesan crash langsung di layar (View klasik).
 */
class MainActivity : androidx.activity.ComponentActivity() {

    private val sb = StringBuilder()
    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Penangkap error: tampilkan di layar + tulis log eksternal
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            tulisLog("UNCAUGHT: $trace")
            runOnUiThread {
                val errTv = TextView(this).apply {
                    text = "❌ COMPOSE CRASH:\n\n$trace"
                    textSize = 11f
                    setTextColor(Color.rgb(127, 29, 29))
                    setBackgroundColor(Color.WHITE)
                    setPadding(40, 40, 40, 40)
                }
                setContentView(errTv)
            }
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 15000)
        }

        tv = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(12, 74, 110))
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
            addView(tv)
        }
        val sc = ScrollView(this)
        sc.addView(ll)
        setContentView(sc)

        sb.appendLine("Mencoba setContent + MaterialTheme + Text ...")
        tv.text = sb.toString()

        try {
            setContent {
                MaterialTheme {
                    Layar()
                }
            }
            sb.appendLine("setContent OK (komposisi dipanggil)")
            tv.text = sb.toString()
            tulisLog("setContent OK")
        } catch (t: Throwable) {
            sb.appendLine("GAGAL (sync): ${t.javaClass.name}: ${t.message}")
            tv.text = sb.toString()
            tulisLog("GAGAL sync: ${t.javaClass.name}: ${t.message}\n" + t.stackTrace.take(10).joinToString("\n"))
        }
    }

    @Composable
    fun Layar() {
        Text("✅ COMPOSE OK — Jetpack Compose berjalan normal di APK ini.")
    }

    private fun tulisLog(isi: String) {
        try {
            val dir = getExternalFilesDir(null)
            if (dir != null) {
                val f = File(dir, "compose_test_log.txt")
                FileOutputStream(f, true).use { it.write(("=== TEST3 ${System.currentTimeMillis()} ===\n$isi\n").toByteArray()) }
            }
        } catch (ignored: Exception) {
        }
    }
}
