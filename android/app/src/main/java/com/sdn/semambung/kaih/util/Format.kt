package com.sdn.semambung.kaih.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pemformat tanggal & waktu ala Indonesia (zona waktu Asia/Jakarta). */
object Format {

    val NAMA_BULAN = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    /** NAMA_HARI[0] = Senin ... NAMA_HARI[6] = Minggu (dayOfWeek.value: 1=Senin..7=Minggu). */
    val NAMA_HARI = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    private val ZONA: ZoneId = ZoneId.of("Asia/Jakarta")

    /** "2026-08-19" -> "19 Agustus 2026" */
    fun tanggalIndonesia(tanggal: String): String = try {
        val d = LocalDate.parse(tanggal)
        "${d.dayOfMonth} ${NAMA_BULAN[d.monthValue - 1]} ${d.year}"
    } catch (e: Exception) {
        tanggal
    }

    /** YearMonth(2026,8) -> "Agustus 2026" */
    fun bulanIndonesia(bulan: YearMonth): String =
        "${NAMA_BULAN[bulan.monthValue - 1]} ${bulan.year}"

    /** "2026-08-19" -> "Rabu" */
    fun hariIndonesia(tanggal: String): String = try {
        NAMA_HARI[LocalDate.parse(tanggal).dayOfWeek.value - 1]
    } catch (e: Exception) {
        ""
    }

    /** Tanggal hari ini format "YYYY-MM-DD". */
    fun hariIni(): String = LocalDate.now().toString()

    /** Waktu paraf: "19/08/2026 14:30" — sama dengan date('d/m/Y H:i') versi web. */
    fun sekarangParaf(): String =
        OffsetDateTime.now(ZONA).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

    /** Waktu ISO dengan offset — padanan date('c') versi web. */
    fun sekarangIso(): String =
        OffsetDateTime.now(ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
