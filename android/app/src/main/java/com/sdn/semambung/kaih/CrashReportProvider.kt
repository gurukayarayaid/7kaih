package com.sdn.semambung.kaih

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Provider debug: memasang penangkap error SELURUH aplikasi sejak paling awal
 * (initOrder=1, dijalankan sebelum FirebaseInitProvider). Bila terjadi crash,
 * stack trace ditulis ke berkas crash_log.txt dan ditampilkan di layar.
 */
class CrashReportProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            try {
                val f = File(context!!.filesDir, "crash_log.txt")
                FileOutputStream(f, true).use { it.write(("=== CRASH ${System.currentTimeMillis()} ===\n$trace\n").toByteArray()) }
            } catch (ignored: Exception) {
            }
            // Salinan ke penyimpanan eksternal (bisa diambil via USB tanpa root):
            // Android/data/com.sdn.semambung.kaih/files/crash_log.txt
            try {
                val dir = context!!.getExternalFilesDir(null)
                if (dir != null) {
                    val f2 = File(dir, "crash_log.txt")
                    FileOutputStream(f2, true).use { it.write(("=== CRASH ${System.currentTimeMillis()} ===\n$trace\n").toByteArray()) }
                }
            } catch (ignored: Exception) {
            }
            try {
                val i = Intent(context, CrashActivity::class.java)
                    .putExtra("trace", if (trace.length > 4000) trace.substring(0, 4000) else trace)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context!!.startActivity(i)
            } catch (ignored: Exception) {
            }
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 6000)
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
