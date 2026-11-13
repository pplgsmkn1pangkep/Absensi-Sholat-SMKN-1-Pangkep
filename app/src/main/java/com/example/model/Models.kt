package com.example.model

enum class Role(val title: String) {
    ADMIN_PUSAT("Admin Pusat (Kepala Sekolah & Operator)"),
    ADMIN_GURU("Admin Guru (Guru Agama / Wali Kelas / Kaprodi)"),
    SISWA("Siswa")
}

enum class Gender {
    LAKI_LAKI,
    PEREMPUAN
}

enum class Jurusan(val code: String, val fullName: String, val displayLabel: String) {
    PPLG("PPLG", "Pengembangan Perangkat Lunak dan Gim", "Pengembangan Perangkat Lunak dan Gim (PPLG)"),
    AKL("AKL", "Akuntansi dan Keuangan Lembaga", "Akuntansi dan Keuangan Lembaga (AKL)"),
    PM("PM", "Bisnis Daring dan Pemasaran", "Bisnis Daring dan Pemasaran (PM)"),
    TJKT("TJKT", "Teknik Telekomunikasi dan Jaringan Komputer", "Teknik Telekomunikasi dan Jaringan Komputer (TJKT)"),
    DKV("DKV", "Desain Komunikasi Visual", "Desain Komunikasi Visual (DKV)"),
    APHP("APHP", "Agribisnis Pengolahan Hasil Pertanian", "Agribisnis Pengolahan Hasil Pertanian (APHP)"),
    MPLB("MPLB", "Manajemen Perkantoran dan Layanan Bisnis", "Manajemen Perkantoran dan Layanan Bisnis (MPLB)")
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
    val kelas: String? = null, // e.g. "XII PPLG 1"
    val avatarEmoji: String = "👤",
    val password: String = "123456"
)

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
