package com.sdn.semambung.kaih.testfb

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream

/**
 * APK uji FIREBASE tahap-2c: inisialisasi MANUAL dengan FirebaseOptions
 * (tanpa resource lookup). Setiap langkah dalam try/catch dan hasilnya
 * DITAMPILKAN di layar + ditulis ke log eksternal.
 */
class MainActivity : Activity() {

    private val sb = StringBuilder()
    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tv = TextView(this).apply {
            textSize = 13f
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

        // STEP 1: FirebaseApp.initializeApp MANUAL (tanpa resource)
        try {
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:546434337251:android:bcadc591cf723f211438d6")
                .setApiKey("AIzaSyBEBK-Ac4_vq3R5WOwXoeMr0gwBkn8AMmY")
                .setProjectId("kaih-sdn-semambung")
                .setGcmSenderId("546434337251")
                .setStorageBucket("kaih-sdn-semambung.firebasestorage.app")
                .build()
            val app = com.google.firebase.FirebaseApp.initializeApp(this, options)
            sb.appendLine("STEP1 FirebaseApp.initializeApp(MANUAL):")
            sb.appendLine(if (app != null) "  OK -> ${app.name}" else "  null")
        } catch (t: Throwable) {
            sb.appendLine("STEP1 GAGAL: ${t.javaClass.name}: ${t.message}")
            sb.appendLine(t.stackTrace.take(8).joinToString("\n") { "  at $it" })
        }
        sb.appendLine()
        tampil()

        // STEP 2: Firestore.getInstance
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            sb.appendLine("STEP2 FirebaseFirestore.getInstance: OK")
            sb.appendLine("  ${db.javaClass.name}")
        } catch (t: Throwable) {
            sb.appendLine("STEP2 GAGAL: ${t.javaClass.name}: ${t.message}")
            sb.appendLine(t.stackTrace.take(8).joinToString("\n") { "  at $it" })
        }
        sb.appendLine()
        tampil()
        tulisLog()
    }

    private fun tampil() {
        tv.text = sb.toString()
    }

    private fun tulisLog() {
        try {
            val dir = getExternalFilesDir(null)
            if (dir != null) {
                val f = File(dir, "crash_log_test.txt")
                FileOutputStream(f, true).use { it.write(("=== TEST2c ${System.currentTimeMillis()} ===\n$sb\n").toByteArray()) }
            }
        } catch (ignored: Exception) {
        }
    }
}
