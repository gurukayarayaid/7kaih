package com.sdn.semambung.kaih.testc

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/** Pencatat log yang bisa DIBACA dari folder Downloads HP (tanpa adb). */
object TestLog {
    fun tulis(ctx: Context, isi: String) {
        val teks = "[${System.currentTimeMillis()}] $isi\n"
        // 1) Downloads publik (Android 10+)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "kaih_test3_log.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(teks.toByteArray()) }
                }
            }
        } catch (_: Exception) {
        }
        // 2) external files
        try {
            val d = ctx.getExternalFilesDir(null)
            if (d != null) {
                FileOutputStream(File(d, "kaih_test3_log.txt"), true).use { it.write(teks.toByteArray()) }
            }
        } catch (_: Exception) {
        }
        // 3) internal
        try {
            FileOutputStream(File(ctx.filesDir, "kaih_test3_log.txt"), true).use { it.write(teks.toByteArray()) }
        } catch (_: Exception) {
        }
    }
}
