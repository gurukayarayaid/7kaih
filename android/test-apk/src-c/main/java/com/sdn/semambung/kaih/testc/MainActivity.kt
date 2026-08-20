package com.sdn.semambung.kaih.testc

import android.app.Activity
import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

/**
 * APK uji COMPOSE BERTAHAP (v3):
 *   A. TextView klasik (bukti Activity normal)
 *   B. ComposeView kosong
 *   C. ComposeView + Text
 *   D. ComposeView + MaterialTheme + Text
 * Setiap langkah dicatat ke LOG yang BISA DIBUKA dari folder Downloads HP.
 */
class MainActivity : Activity() {

    private val sb = StringBuilder()
    private lateinit var logTv: TextView
    private lateinit var composeView: ComposeView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Penangkap error: tulis log (termasuk ke Downloads publik) lalu tampilkan di layar
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            tulisLog("\n===== UNCAUGHT =====")
            tulisLog(trace.take(3000))
            runOnUiThread {
                try {
                    val errTv = TextView(this).apply {
                        text = "❌ CRASH:\n\n$trace"
                        textSize = 10f
                        setTextColor(Color.rgb(127, 29, 29))
                        setBackgroundColor(Color.WHITE)
                        setPadding(40, 40, 40, 40)
                    }
                    setContentView(errTv)
                } catch (_: Exception) {
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 20000)
        }

        // UI: log di atas, ComposeView di bawah
        logTv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(12, 74, 110))
            setPadding(24, 24, 24, 24)
        }
        composeView = ComposeView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(logTv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(composeView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)

        tulisLog("=== TEST3 v3 MULAI ===")
        tulisLog("A: Activity onCreate OK")
        tampil("A OK — Activity berjalan")

        // B: ComposeView kosong
        handler.postDelayed({
            tulisLog("B: mencoba setContent KOSONG")
            try {
                composeView.setContent {
                    // kosong
                }
                tulisLog("B: setContent kosong OK")
                tampil("B OK — ComposeView kosong berhasil")
            } catch (t: Throwable) {
                tulisLog("B GAGAL: ${t.javaClass.name}: ${t.message}")
                tampil("B GAGAL: ${t.javaClass.name}: ${t.message}")
            }
        }, 300)

        // C: Text (tanpa MaterialTheme)
        handler.postDelayed({
            tulisLog("C: mencoba setContent Text")
            try {
                composeView.setContent {
                    Text("✅ COMPOSE TEXT OK")
                }
                tulisLog("C: Text OK")
                tampil("C OK — Text berhasil")
            } catch (t: Throwable) {
                tulisLog("C GAGAL: ${t.javaClass.name}: ${t.message}")
                tampil("C GAGAL: ${t.javaClass.name}: ${t.message}")
            }
        }, 800)

        // D: MaterialTheme + Text
        handler.postDelayed({
            tulisLog("D: mencoba MaterialTheme")
            try {
                composeView.setContent {
                    MaterialTheme {
                        Text("✅ MATERIAL OK")
                    }
                }
                tulisLog("D: MaterialTheme OK")
                tampil("D OK — MaterialTheme berhasil")
            } catch (t: Throwable) {
                tulisLog("D GAGAL: ${t.javaClass.name}: ${t.message}")
                tampil("D GAGAL: ${t.javaClass.name}: ${t.message}")
            }
        }, 1300)

        // E: penanda hidup
        handler.postDelayed({
            tulisLog("E: aplikasi masih hidup setelah 5 detik")
            tampil("E: masih hidup")
        }, 5000)
    }

    private fun tampil(baris: String) {
        sb.appendLine(baris)
        logTv.text = sb.toString()
    }

    /** Tulis log ke: Downloads (publik, bisa dibuka dari aplikasi File), eksternal, internal. */
    private fun tulisLog(isi: String) {
        val teks = "[${System.currentTimeMillis()}] $isi\n"
        // 1) Downloads publik (Android 10+)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "kaih_test3_log.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(teks.toByteArray()) }
                }
            }
        } catch (_: Exception) {
        }
        // 2) external files
        try {
            val d = getExternalFilesDir(null)
            if (d != null) {
                FileOutputStream(File(d, "kaih_test3_log.txt"), true).use { it.write(teks.toByteArray()) }
            }
        } catch (_: Exception) {
        }
        // 3) internal
        try {
            FileOutputStream(File(filesDir, "kaih_test3_log.txt"), true).use { it.write(teks.toByteArray()) }
        } catch (_: Exception) {
        }
    }
}
