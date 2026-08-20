package com.sdn.semambung.kaih.testc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/** Provider: dijalankan PALING AWAL (initOrder=1) — bukti proses hidup. */
class LogProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        TestLog.tulis(context!!, "PROVIDER: onCreate (paling awal)")
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
