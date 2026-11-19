package com.example.model

enum class Role(val title: String) {
    ADMIN_PUSAT("Admin Pusat (Kepala Sekolah & Operator)"),
    ADMIN_GURU("Kaprodi / Wali Kelas / Guru BK / Guru Agama"),
    SISWA("Siswa")
}

enum class Gender {
    LAKI_LAKI,
    PEREMPUAN
}

enum class Jurusan(
    val code: String,
    val fullName: String,
    val displayLabel: String,
    val officialClasses: List<String>
) {
    PPLG(
        "PPLG",
        "Pengembangan Perangkat Lunak dan Gim",
        "Pengembangan Perangkat Lunak dan Gim (PPLG)",
        listOf("X PPLG", "XI PPLG", "XII PPLG")
    ),
    AKL(
        "AKL",
        "Akuntansi dan Keuangan Lembaga",
        "Akuntansi dan Keuangan Lembaga (AKL)",
        listOf(
            "X AKL 1", "X AKL 2", "X AKL 3",
            "XI AKL 1", "XI AKL 2", "XI AKL 3",
            "XII AKL 1", "XII AKL 2", "XII AKL 3"
        )
    ),
    PM(
        "PM",
        "Bisnis Daring dan Pemasaran",
        "Bisnis Daring dan Pemasaran (PM)",
        listOf(
            "X PM 1", "X PM 2", "X PM 3",
            "XI PM 1", "XI PM 2", "XI PM 3",
            "XII PM 1", "XII PM 2", "XII PM 3"
        )
    ),
    TJKT(
        "TJKT",
        "Teknik Telekomunikasi dan Jaringan Komputer",
        "Teknik Telekomunikasi dan Jaringan Komputer (TJKT)",
        listOf(
            "X TJKT 1", "X TJKT 2", "X TJKT 3",
            "XI TJKT 1", "XI TJKT 2", "XI TJKT 3",
            "XII TJKT 1", "XII TJKT 2", "XII TJKT 3"
        )
    ),
    DKV(
        "DKV",
        "Desain Komunikasi Visual",
        "Desain Komunikasi Visual (DKV)",
        listOf(
            "X DKV 1", "X DKV 2",
            "XI DKV 1", "XI DKV 2",
            "XII DKV 1", "XII DKV 2"
        )
    ),
    APHP(
        "APHP",
        "Agribisnis Pengolahan Hasil Pertanian",
        "Agribisnis Pengolahan Hasil Pertanian (APHP)",
        listOf(
            "X APHP 1", "X APHP 2",
            "XI APHP 1", "XI APHP 2",
            "XII APHP 1", "XII APHP 2"
        )
    ),
    MPLB(
        "MPLB",
        "Manajemen Perkantoran dan Layanan Bisnis",
        "Manajemen Perkantoran dan Layanan Bisnis (MPLB)",
        listOf(
            "X MPLB 1", "X MPLB 2", "X MPLB 3",
            "XI MPLB 1", "XI MPLB 2", "XI MPLB 3",
            "XII MPLB 1", "XII MPLB 2", "XII MPLB 3"
        )
    )
}

enum class PrayerType(
    val code: String,
    val prayerName: String,
    val category: String,
    val isMandatoryForGoHome: Boolean,
    val typicalTime: String
) {
    DHUHA("dhuha", "Sholat Dhuha", "Sunnah (Bisa Hadir / Tidak)", false, "07:00 - 08:30"),
    DZUHUR("dzuhur", "Sholat Dzuhur", "Wajib & Syarat Pulang", true, "12:00 - 13:15"),
    ASHAR("ashar", "Sholat Ashar", "Wajib & Syarat Pulang", true, "15:20 - 16:30")
}

enum class DhuhaStatus {
    HADIR,
    TIDAK_HADIR,
    BELUM_MEMILIH
}

data class PeriodRecord(
    val id: String,
    val nisn: String,
    val startDateString: String,      // Format YYYY-MM-DD
    val estimatedDays: Int,           // Standard fiqh 3 - 7 days (max 15 days)
    val startTimestampMillis: Long,
    val isActive: Boolean = true,
    val isManuallyEnded: Boolean = false,
    val notes: String = ""
)

data class UserProfile(
    val id: String,
    val nisnOrNip: String,
    val fullName: String,
    val role: Role,
    val gender: Gender = Gender.LAKI_LAKI,
    val jurusan: Jurusan? = null,
    val kelas: String? = null, // e.g. "XII PPLG 1" (null = semua kelas di jurusan)
    val avatarEmoji: String = "👤",
    val password: String = "123456",
    val jabatan: String? = null // e.g. "Kaprodi", "Wali Kelas", "Guru Agama / Piket Masjid"
) {
    fun effectiveKelasFilter(): String? {
        val raw = kelas?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase()
        if (
            lower.startsWith("kaprodi") ||
            lower.startsWith("piket") ||
            lower.startsWith("guru") ||
            lower.startsWith("bk") ||
            lower.startsWith("semua")
        ) {
            return null
        }
        return raw
    }

    fun effectiveJabatanLabel(): String {
        jabatan?.takeIf { it.isNotBlank() }?.let { rawJabatan ->
            val lower = rawJabatan.lowercase()
            return when {
                lower.contains("kaprodi") -> "Kaprodi"
                lower.contains("wali") -> "Wali Kelas"
                lower.contains("bk") -> "Guru BK"
                lower.contains("agama") || lower.contains("piket") -> "Guru Agama"
                else -> rawJabatan
            }
        }
        val lowerName = fullName.lowercase()
        val lowerKelas = kelas?.lowercase().orEmpty()
        return when {
            lowerName.contains("kaprodi") || lowerKelas.startsWith("kaprodi") -> "Kaprodi"
            lowerName.contains("bk") || lowerKelas.startsWith("bk") || lowerKelas.contains("guru bk") -> "Guru BK"
            effectiveKelasFilter() != null -> "Wali Kelas"
            lowerName.contains("agama") || lowerKelas.startsWith("piket") -> "Guru Agama"
            else -> "Guru Agama"
        }
    }

    fun isKaprodi(): Boolean =
        role == Role.ADMIN_GURU && effectiveJabatanLabel().equals("Kaprodi", ignoreCase = true)

    fun isWaliKelas(): Boolean =
        role == Role.ADMIN_GURU && effectiveJabatanLabel().equals("Wali Kelas", ignoreCase = true)

    fun isRestrictedToAssignedScope(): Boolean = isKaprodi() || isWaliKelas()

    fun isScopeLocked(): Boolean = when {
        isWaliKelas() -> jurusan != null && effectiveKelasFilter() != null
        isKaprodi() -> jurusan != null
        else -> false
    }
}

data class AttendanceRecord(
    val id: String,
    val nisn: String,
    val studentName: String,
    val jurusan: Jurusan,
    val kelas: String,
    val prayerType: PrayerType,
    val dateString: String, // YYYY-MM-DD
    val timestampMillis: Long,
    val formattedTime: String,
    val isVerifiedInMosque: Boolean = true,
    val isHalanganSyari: Boolean = false,
    val dhuhaStatus: DhuhaStatus = DhuhaStatus.HADIR
)

data class DynamicQrToken(
    val dateString: String,
    val prayerType: PrayerType,
    val tokenCode: String,
    val shortEntryCode: String,
    val generatedAtMillis: Long,
    val validUntilMillis: Long
)
