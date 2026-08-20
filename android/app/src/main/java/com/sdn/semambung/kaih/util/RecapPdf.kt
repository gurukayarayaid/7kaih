package com.sdn.semambung.kaih.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri

/**
 * Pembuat PDF rekap — padanan tombol "🖨 Cetak PDF" pada versi web.
 * Menghasilkan dokumen A4 potret bergaya formulir rekap resmi.
 */
object RecapPdf {

    fun buat(
        context: Context,
        uri: Uri,
        judul: String,                      // boleh berisi "\n"
        periode: String,                    // mis. "Bulan : Agustus 2026"
        info: List<Pair<String, String>>,   // mis. "Sekolah" to "SD Negeri Semambung"
        bulan: List<Pair<String, Boolean>>?, // grid bulan (rekap semester); null utk bulanan
        kebiasaan: List<Pair<String, Boolean>>,
        namaGuru: String
    ) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas

        val tJudul = catatan(13f, 0xFF252525.toInt(), true)
        val tBold = catatan(11f, 0xFF3C3C3C.toInt(), true)
        val tNormal = catatan(11f, 0xFF3C3C3C.toInt(), false)
        val tKecil = catatan(10f, 0xFF3C3C3C.toInt(), false)
        val garisTebal = catatan(13f, 0xFF3C3C3C.toInt(), true)

        var y = 48f

        // ---- Judul dalam pil kuning ----
        val barisJudul = judul.split("\n")
        val lebarTeks = barisJudul.maxOf { tJudul.measureText(it) }
        val pilW = lebarTeks + 56f
        val pilH = barisJudul.size * 20f + 22f
        val pilX = (595f - pilW) / 2f
        c.drawRoundRect(
            pilX, y, pilX + pilW, y + pilH, 24f, 24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF7D900.toInt() }
        )
        var ty = y + pilH / 2f - (barisJudul.size - 1) * 10f
        for (b in barisJudul) {
            c.drawText(b, (595f - tJudul.measureText(b)) / 2f, ty - (tJudul.ascent() + tJudul.descent()) / 2f, tJudul)
            ty += 20f
        }
        y += pilH + 20f

        // ---- Periode ----
        c.drawText(periode, (595f - tBold.measureText(periode)) / 2f, y - (tBold.ascent() + tBold.descent()) / 2f, tBold)
        y += 24f

        // ---- Info (sekolah / nama / kelas) ----
        for ((label, nilai) in info) {
            c.drawText("$label : ", 60f, y - (tBold.ascent() + tBold.descent()) / 2f, tBold)
            c.drawText(nilai, 150f, y - (tNormal.ascent() + tNormal.descent()) / 2f, tNormal)
            y += 26f
        }

        // ---- Grid bulan (khusus rekap semester) ----
        if (bulan != null) {
            var bx = 60f
            for ((i, item) in bulan.withIndex()) {
                val teks = "${item.first} : ${if (item.second) "✓" else ""}"
                c.drawText(teks, bx, y - (tNormal.ascent() + tNormal.descent()) / 2f, tNormal)
                bx += 160f
                if ((i + 1) % 3 == 0) {
                    bx = 60f
                    y += 20f
                }
            }
            y += 6f
        }

        // ---- Tabel 7 kebiasaan ----
        val xNo = 60f
        val xNama = 104f
        val xBelum = 452f
        val xSudah = 508f
        val xAkhir = 562f
        val tinggiBaris = 26f

        fun sel(ax: Float, ay: Float, bx: Float, by: Float, teks: String? = null, bold: Boolean = false, bg: Int? = null, size: Float = 11f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = bg ?: Color.WHITE
            }
            c.drawRect(ax, ay, bx, by, p)
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
                color = 0xFFDF6E99.toInt()
            }
            c.drawRect(ax, ay, bx, by, border)
            if (teks != null) {
                val tp = catatan(size, 0xFF3C3C3C.toInt(), bold)
                val barisTeks = teks.split("\n")
                val offsetAwal = (barisTeks.size - 1) * -6.5f
                barisTeks.forEachIndexed { i, b ->
                    c.drawText(
                        b,
                        ax + 8f,
                        ay + (by - ay) / 2f - (tp.ascent() + tp.descent()) / 2f + offsetAwal + i * 13f,
                        tp
                    )
                }
            }
        }

        // Header baris 1
        sel(xNo, y, xNama, y + 30f, "No.", bold = true, bg = 0xFFED7FAC.toInt())
        sel(xNama, y, xBelum, y + 30f, "Tujuh Kebiasaan Anak\nIndonesia Hebat", bold = true, bg = 0xFFED7FAC.toInt())
        sel(xBelum, y, xAkhir, y + 30f, "Penerapan 7 Kebiasaan\nAnak Indonesia Hebat", bold = true, bg = 0xFFED7FAC.toInt())
        y += 30f
        // Header baris 2
        sel(xNo, y, xNama, y + 26f, bg = 0xFFED7FAC.toInt())
        sel(xNama, y, xBelum, y + 26f, bg = 0xFFED7FAC.toInt())
        sel(xBelum, y, xSudah, y + 26f, "Belum Terbiasa", bold = true, bg = 0xFFED7FAC.toInt())
        sel(xSudah, y, xAkhir, y + 26f, "Sudah Terbiasa", bold = true, bg = 0xFFED7FAC.toInt())
        y += 26f
        // Isi 7 kebiasaan
        kebiasaan.forEachIndexed { i, (nama, sudah) ->
            sel(xNo, y, xNama, y + tinggiBaris, "${i + 1}.")
            sel(xNama, y, xBelum, y + tinggiBaris, nama)
            sel(xBelum, y, xSudah, y + tinggiBaris, if (sudah) "" else "✓", bold = true, size = 13f)
            sel(xSudah, y, xAkhir, y + tinggiBaris, if (sudah) "✓" else "", bold = true, size = 13f)
            y += tinggiBaris
        }
        y += 26f

        // ---- Tanda tangan ----
        val garis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3C3C3C.toInt()
            strokeWidth = 1.2f
        }
        fun teksTengah(x: Float, teks: String, p: Paint, yy: Float) {
            c.drawText(teks, x - p.measureText(teks) / 2f, yy, p)
        }
        c.drawText("Menyetujui:", (595f - tBold.measureText("Menyetujui:")) / 2f, y - (tBold.ascent() + tBold.descent()) / 2f, tBold)
        y += 64f
        teksTengah(170f, "Guru Kelas III", garisTebal, y)
        teksTengah(425f, "Orang Tua/Wali", garisTebal, y)
        y += 46f
        teksTengah(170f, namaGuru, garisTebal, y)
        c.drawLine(
            170f - garisTebal.measureText(namaGuru) / 2f, y + 5f,
            170f + garisTebal.measureText(namaGuru) / 2f, y + 5f,
            garis
        )
        teksTengah(425f, "_____________________", garisTebal, y)
        y += 34f

        // ---- Keterangan ----
        c.drawText("Keterangan:", 60f, y, tBold)
        y += 20f
        val keterangan = listOf(
            "Beri tanda checklist (✓) sesuai pilihan.",
            "Belum terbiasa, jika anak belum melaksanakan 7 kebiasaan setiap hari.",
            "Sudah terbiasa, jika anak sudah melaksanakan 7 kebiasaan setiap hari."
        )
        for (k in keterangan) {
            c.drawText(k, 60f, y, tKecil)
            y += 18f
        }

        doc.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            ?: throw Exception("Tidak dapat menulis berkas PDF.")
        doc.close()
    }

    private fun catatan(size: Float, warna: Int, tebal: Boolean): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = warna
            typeface = Typeface.create(Typeface.DEFAULT, if (tebal) Typeface.BOLD else Typeface.NORMAL)
        }
}
