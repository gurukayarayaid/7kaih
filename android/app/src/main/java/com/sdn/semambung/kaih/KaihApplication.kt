package com.sdn.semambung.kaih

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Application utama 7 KAIH.
 *
 * PENTING: Firebase diinisialisasi SECARA MANUAL dengan FirebaseOptions
 * (bukan lewat FirebaseInitProvider + lookup resource). Ini menghindari
 * error "Resources$NotFoundException" yang terjadi bila ID resource
 * tidak konsisten antara R-class library dan tabel resource APK.
 */
class KaihApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Penangkap error global (redundan dengan CrashReportProvider)
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            tulisLog(trace)
            try {
                val i = Intent(this, CrashActivity::class.java)
                    .putExtra("trace", if (trace.length > 4000) trace.substring(0, 4000) else trace)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
            } catch (ignored: Exception) {
            }
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 6000)
        }

        // Inisialisasi Firebase MANUAL — nilai diambil dari google-services.json
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:546434337251:android:bcadc591cf723f211438d6")
                    .setApiKey("AIzaSyBEBK-Ac4_vq3R5WOwXoeMr0gwBkn8AMmY")
                    .setProjectId("kaih-sdn-semambung")
                    .setGcmSenderId("546434337251")
                    .setStorageBucket("kaih-sdn-semambung.firebasestorage.app")
                    .build()
                val app = FirebaseApp.initializeApp(this, options)
                tulisLog("Firebase init OK: ${app?.name}")
            }
        } catch (t: Throwable) {
            tulisLog("Firebase init GAGAL: ${t.javaClass.name}: ${t.message}")
        }
    }

    private fun tulisLog(teks: String) {
        try {
            val f = File(filesDir, "crash_log.txt")
            FileOutputStream(f, true).use { it.write(("=== $teks ===\n").toByteArray()) }
        } catch (ignored: Exception) {
        }
    }
}
