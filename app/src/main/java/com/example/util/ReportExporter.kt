package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.DhuhaStatus
import com.example.model.Jurusan
import com.example.model.Role
import com.example.model.UserProfile
import com.example.viewmodel.DailyPrayerTrend
import com.example.viewmodel.PrayerPercentageStats
import com.example.viewmodel.StudentAttendanceSummary
import java.io.File
import java.io.OutputStream

object ReportExporter {

    private const val UTF8_BOM = "\uFEFF"

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\r", " ").replace("\n", " ")
        return if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains(";")) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }

    private fun csvRow(vararg cells: String): String =
        cells.joinToString(",") { escapeCsv(it) }

    fun buildScopeLabel(
        currentUser: UserProfile,
        selectedJurusanFilter: Jurusan?,
        selectedKelasFilter: String?
    ): String {
        return when (currentUser.role) {
            Role.SISWA -> "${currentUser.jurusan?.code ?: "Siswa"} - ${currentUser.kelas ?: "-"}"
            Role.ADMIN_GURU -> {
                val j = currentUser.jurusan
                val k = currentUser.effectiveKelasFilter() ?: selectedKelasFilter
                if (j == null) "Belum Memilih Jurusan"
                else "Jurusan ${j.code} (${k ?: "Semua Kelas ${j.code}"})"
            }
            Role.ADMIN_PUSAT -> {
                val j = selectedJurusanFilter
                val k = selectedKelasFilter
                if (j == null) "Semua 7 Jurusan"
                else "Jurusan ${j.code} (${k ?: "Semua Kelas ${j.code}"})"
            }
        }
    }

    fun buildSuggestedFileName(
        currentUser: UserProfile,
        selectedJurusanFilter: Jurusan?,
        selectedKelasFilter: String?,
        dateTag: String,
        isMultiDay: Boolean
    ): String {
        val rawScope = when (currentUser.role) {
            Role.ADMIN_GURU -> {
                val k = currentUser.effectiveKelasFilter() ?: selectedKelasFilter
                k ?: currentUser.jurusan?.code ?: "Umum"
            }
            Role.ADMIN_PUSAT -> selectedKelasFilter ?: selectedJurusanFilter?.code ?: "Semua_Jurusan"
            Role.SISWA -> currentUser.nisnOrNip
        }
        val safeScope = rawScope.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val modeTag = if (isMultiDay) "7Hari" else dateTag
        return "Rekap_SIM_SHOLAT_${safeScope}_$modeTag.csv"
    }

    fun buildSingleDayCsvContent(
        currentUser: UserProfile,
        scopeLabel: String,
        readableDate: String,
        stats: PrayerPercentageStats,
        filteredSummaries: List<StudentAttendanceSummary> = stats.studentSummaries
    ): String {
        val sb = StringBuilder()
        sb.appendLine(csvRow("SIM SHOLAT - REKAP KEHADIRAN SHOLAT SISWA"))
        sb.appendLine(csvRow("Sekolah", "SMKN 1 Pangkajene dan Kepulauan (Masjid Darul Ulum)"))
        sb.appendLine(csvRow("Petugas / Pemantau", "${currentUser.fullName} (${currentUser.effectiveJabatanLabel()})"))
        sb.appendLine(csvRow("Kualifikasi Data", scopeLabel))
        sb.appendLine(csvRow("Tanggal Rekap", "$readableDate (${stats.dateString})"))
        sb.appendLine()

        sb.appendLine(csvRow("RINGKASAN PERSENTASE KEHADIRAN"))
        sb.appendLine(csvRow("Kategori", "Jumlah Hadir", "Total Siswa", "Persentase"))
        sb.appendLine(csvRow("Sholat Dhuha (Sunnah)", stats.dhuhaPresentCount.toString(), stats.totalStudents.toString(), "${stats.dhuhaPercentage}%"))
        sb.appendLine(csvRow("Sholat Dzuhur (Wajib)", stats.dzuhurPresentCount.toString(), stats.totalStudents.toString(), "${stats.dzuhurPercentage}%"))
        sb.appendLine(csvRow("Sholat Ashar (Wajib)", stats.asharPresentCount.toString(), stats.totalStudents.toString(), "${stats.asharPercentage}%"))
        sb.appendLine(csvRow("Izin Pulang Lengkap (Dzuhur & Ashar / Syar'i)", stats.goHomeEligibleCount.toString(), stats.totalStudents.toString(), "${stats.goHomePercentage}%"))
        sb.appendLine(csvRow("Siswi Berhalangan Syar'i", stats.halanganCount.toString(), stats.totalStudents.toString(), "-"))
        sb.appendLine()

        if (stats.classBreakdown.isNotEmpty()) {
            sb.appendLine(csvRow("PERSENTASE PER KELAS"))
            sb.appendLine(csvRow("Kelas", "Jumlah Siswa", "Dhuha (%)", "Dzuhur (%)", "Ashar (%)", "Izin Pulang (%)"))
            stats.classBreakdown.forEach { cls ->
                sb.appendLine(
                    csvRow(
                        cls.className,
                        cls.totalStudents.toString(),
                        "${cls.dhuhaPercentage}%",
                        "${cls.dzuhurPercentage}%",
                        "${cls.asharPercentage}%",
                        "${cls.goHomePercentage}%"
                    )
                )
            }
            sb.appendLine()
        }

        sb.appendLine(csvRow("DATA RINCIAN SISWA"))
        sb.appendLine(
            csvRow(
                "No",
                "NISN",
                "Nama Siswa",
                "Jurusan",
                "Kelas",
                "Sholat Dhuha",
                "Jam Dhuha",
                "Sholat Dzuhur",
                "Jam Dzuhur",
                "Sholat Ashar",
                "Jam Ashar",
                "Berhalangan Syar'i",
                "Status Izin Pulang"
            )
        )

        filteredSummaries.forEachIndexed { idx, item ->
            val dhuhaLabel = when (item.dhuhaStatus) {
                DhuhaStatus.HADIR -> "Hadir"
                DhuhaStatus.TIDAK_HADIR -> "Tidak Hadir"
                DhuhaStatus.BELUM_MEMILIH -> "Belum Memilih"
            }
            val dzuhurLabel = if (item.dzuhurRecord != null) "Hadir" else "Belum Absen"
            val asharLabel = if (item.asharRecord != null) "Hadir" else "Belum Absen"
            val syariLabel = if (item.isCurrentlyOnPeriod) "Ya (Berhalangan)" else "Tidak"
            val gateLabel = if (item.isEligibleToGoHome) "Boleh Pulang (Tiket Hijau)" else "Tahan (Belum Lengkap)"

            sb.appendLine(
                csvRow(
                    (idx + 1).toString(),
                    item.student.nisnOrNip,
                    item.student.fullName,
                    item.student.jurusan?.code ?: "-",
                    item.student.kelas ?: "-",
                    dhuhaLabel,
                    item.dhuhaRecord?.formattedTime ?: "-",
                    dzuhurLabel,
                    item.dzuhurRecord?.formattedTime ?: "-",
                    asharLabel,
                    item.asharRecord?.formattedTime ?: "-",
                    syariLabel,
                    gateLabel
                )
            )
        }

        return sb.toString()
    }

    fun buildMultiDayCsvContent(
        currentUser: UserProfile,
        scopeLabel: String,
        dailyTrends: List<DailyPrayerTrend>,
        dailyStatsList: List<Pair<DailyPrayerTrend, PrayerPercentageStats>>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(csvRow("SIM SHOLAT - REKAP 7 HARI TERAKHIR"))
        sb.appendLine(csvRow("Sekolah", "SMKN 1 Pangkajene dan Kepulauan (Masjid Darul Ulum)"))
        sb.appendLine(csvRow("Petugas / Pemantau", "${currentUser.fullName} (${currentUser.effectiveJabatanLabel()})"))
        sb.appendLine(csvRow("Kualifikasi Data", scopeLabel))
        sb.appendLine()

        sb.appendLine(csvRow("RINGKASAN PERSENTASE HARIAN (7 HARI TERAKHIR)"))
        sb.appendLine(csvRow("Hari", "Tanggal", "Total Siswa", "Dhuha (%)", "Dzuhur (%)", "Ashar (%)", "Izin Pulang (%)"))
        dailyTrends.forEach { trend ->
            sb.appendLine(
                csvRow(
                    trend.label,
                    trend.dateString,
                    trend.totalStudents.toString(),
                    "${trend.dhuhaPercentage}%",
                    "${trend.dzuhurPercentage}%",
                    "${trend.asharPercentage}%",
                    "${trend.goHomePercentage}%"
                )
            )
        }
        sb.appendLine()

        sb.appendLine(csvRow("DATA RINCIAN SISWA PER HARI (7 HARI TERAKHIR)"))
        sb.appendLine(
            csvRow(
                "Tanggal",
                "Hari",
                "NISN",
                "Nama Siswa",
                "Jurusan",
                "Kelas",
                "Dhuha",
                "Dzuhur",
                "Ashar",
                "Berhalangan Syar'i",
                "Status Izin Pulang"
            )
        )
        dailyStatsList.forEach { (trend, stats) ->
            stats.studentSummaries.forEach { item ->
                sb.appendLine(
                    csvRow(
                        trend.dateString,
                        trend.label,
                        item.student.nisnOrNip,
                        item.student.fullName,
                        item.student.jurusan?.code ?: "-",
                        item.student.kelas ?: "-",
                        when (item.dhuhaStatus) {
                            DhuhaStatus.HADIR -> "Hadir"
                            DhuhaStatus.TIDAK_HADIR -> "Tidak Hadir"
                            DhuhaStatus.BELUM_MEMILIH -> "Belum"
                        },
                        if (item.dzuhurRecord != null) "Hadir (${item.dzuhurRecord.formattedTime})" else "Belum",
                        if (item.asharRecord != null) "Hadir (${item.asharRecord.formattedTime})" else "Belum",
                        if (item.isCurrentlyOnPeriod) "Ya" else "Tidak",
                        if (item.isEligibleToGoHome) "Boleh Pulang" else "Tahan"
                    )
                )
            }
        }

        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                writeUtf8BomCsv(stream, csvContent)
            } ?: return Result.failure(IllegalStateException("Gagal membuka lokasi penyimpanan file."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveCsvToDownloads(context: Context, fileName: String, csvContent: String): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SIM_SHOLAT")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Tidak dapat membuat file di folder Download."))

                resolver.openOutputStream(uri)?.use { stream ->
                    writeUtf8BomCsv(stream, csvContent)
                } ?: return Result.failure(IllegalStateException("Gagal menulis isi file rekap."))

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.success("Download/SIM_SHOLAT/$fileName")
            } else {
                Result.failure(UnsupportedOperationException("NEEDS_CREATE_DOCUMENT"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createAndShareCsvFile(context: Context, fileName: String, csvContent: String): Result<Unit> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }
            val file = File(exportDir, fileName)
            file.outputStream().use { stream ->
                writeUtf8BomCsv(stream, csvContent)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rekap Kehadiran SIM SHOLAT - $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Buka File Rekap (.csv)"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeUtf8BomCsv(outputStream: OutputStream, csvContent: String) {
        val bytes = (UTF8_BOM + csvContent).toByteArray(Charsets.UTF_8)
        outputStream.write(bytes)
        outputStream.flush()
    }
}
