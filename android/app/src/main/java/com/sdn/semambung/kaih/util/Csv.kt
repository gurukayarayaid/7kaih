package com.sdn.semambung.kaih.util

/** Parser CSV sederhana (mendukung tanda kutip dan koma di dalam kutipan). */
object Csv {

    fun parse(isi: String): List<List<String>> {
        val baris = mutableListOf<List<String>>()
        var kolom = mutableListOf<String>()
        val sb = StringBuilder()
        var dalamKutip = false
        var i = 0

        while (i < isi.length) {
            val ch = isi[i]
            when {
                dalamKutip -> when {
                    ch == '"' && i + 1 < isi.length && isi[i + 1] == '"' -> {
                        sb.append('"')
                        i++
                    }
                    ch == '"' -> dalamKutip = false
                    else -> sb.append(ch)
                }
                ch == '"' -> dalamKutip = true
                ch == ',' -> {
                    kolom.add(sb.toString().trim())
                    sb.setLength(0)
                }
                ch == '\n' || ch == '\r' -> {
                    if (ch == '\r' && i + 1 < isi.length && isi[i + 1] == '\n') i++
                    kolom.add(sb.toString().trim())
                    sb.setLength(0)
                    if (kolom.isNotEmpty() && kolom.any { it.isNotEmpty() }) baris.add(kolom)
                    kolom = mutableListOf()
                }
                else -> sb.append(ch)
            }
            i++
        }

        // Baris terakhir tanpa newline
        if (sb.isNotEmpty() || kolom.any { it.isNotEmpty() }) {
            kolom.add(sb.toString().trim())
            if (kolom.any { it.isNotEmpty() }) baris.add(kolom)
        }
        return baris
    }
}
