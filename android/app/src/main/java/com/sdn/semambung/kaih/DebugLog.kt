package com.sdn.semambung.kaih

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pencatat langkah debug untuk build uji.
 * Menulis ke file internal + eksternal (bisa diambil via USB tanpa root)
 * dan menyimpan status terakhir untuk ditampilkan di overlay layar.
 */
object DebugLog {
    @Volatile var current: String = "mulai"
    private var ctx: Context? = null

    fun init(c: Context) {
        ctx = c.applicationContext
    }

    fun log(step: String) {
        current = step
        val c = ctx ?: return
        val line = "[" + SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()) + "] " + step + "\n"
        try {
            FileOutputStream(File(c.filesDir, "debug_log.txt"), true).use { it.write(line.toByteArray()) }
        } catch (_: Exception) {
        }
        try {
            val dir = c.getExternalFilesDir(null)
            if (dir != null) {
                FileOutputStream(File(dir, "debug_log.txt"), true).use { it.write(line.toByteArray()) }
            }
        } catch (_: Exception) {
        }
    }
}
