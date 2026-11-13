package com.example.data

import android.util.Log
import com.example.R
import com.example.model.AttendanceRecord
import com.example.model.DhuhaStatus
import com.example.model.DynamicQrToken
import com.example.model.Gender
import com.example.model.Jurusan
import com.example.model.PeriodRecord
import com.example.model.PrayerType
import com.example.model.Role
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceRepository(
    private val firestore: FirebaseFirestore? = try {
        FirebaseApp.getInstance()
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        Log.w("AttendanceRepo", "Firestore not available or uninitialized, falling back to local storage: ${e.message}")
        null
    }
) {

    // Predefined accounts categorized into Admin Pusat, Admin Guru, and Siswa (with explicit Gender)
    private val initialUsers: List<UserProfile> = listOf(
        // 1. Admin Pusat
        UserProfile(
            id = "admin-pusat-01",
            nisnOrNip = "admin.pusat",
            fullName = "Indah Purnama Syahir, S.Pd., Gr",
            role = Role.ADMIN_PUSAT,
            gender = Gender.PEREMPUAN,
            avatarEmoji = "🧕",
            password = "admin"
        ),
        UserProfile(
            id = "admin-pusat-02",
            nisnOrNip = "operator.sekolah",
            fullName = "Operator Sistem SMKN 1 Pangkep",
            role = Role.ADMIN_PUSAT,
            gender = Gender.LAKI_LAKI,
            avatarEmoji = "💻",
            password = "admin"
        ),

        // 2. Admin Guru (Guru Agama, Wali Kelas, Kaprodi)
        UserProfile(
            id = "guru-01",
            nisnOrNip = "guru.agama",
            fullName = "Ust. Syamsul Bahri, S.Pd.I (Guru Agama / Piket Masjid)",
            role = Role.ADMIN_GURU,
            gender = Gender.LAKI_LAKI,
            avatarEmoji = "👳",
            password = "guru"
        ),
        UserProfile(
            id = "guru-02",
            nisnOrNip = "wali.pplg",
            fullName = "Drs. H. Ahmad Dahlan, M.Pd (Wali Kelas XII PPLG 1)",
            role = Role.ADMIN_GURU,
            gender = Gender.LAKI_LAKI,
            jurusan = Jurusan.PPLG,
            kelas = "XII PPLG 1",
            avatarEmoji = "👨‍🏫",
            password = "guru"
        ),
        UserProfile(
            id = "guru-03",
            nisnOrNip = "kaprodi.pplg",
            fullName = "Ir. St. Rahmah, S.Kom, M.T (Kaprodi PPLG)",
            role = Role.ADMIN_GURU,
            gender = Gender.PEREMPUAN,
            jurusan = Jurusan.PPLG,
            avatarEmoji = "👩‍💼",
            password = "guru"
        ),

        // 3. Siswa (All 7 Majors, including female students)
        UserProfile("s-01", "0061234567", "Muhammad Fadhil", Role.SISWA, Gender.LAKI_LAKI, Jurusan.PPLG, "XII PPLG 1", "👨‍💻", "123456"),
        UserProfile("s-02", "0067654321", "Nur Aisyah Putri", Role.SISWA, Gender.PEREMPUAN, Jurusan.AKL, "XI AKL 2", "🧕", "123456"),
        UserProfile("s-03", "0069988776", "Andi Reski Pratama", Role.SISWA, Gender.LAKI_LAKI, Jurusan.TJKT, "X TJKT 1", "🧑‍💻", "123456"),
        UserProfile("s-04", "0061234568", "Ahmad Fauzan R", Role.SISWA, Gender.LAKI_LAKI, Jurusan.PPLG, "XII PPLG 1", "👦", "123456"),
        UserProfile("s-05", "0061234569", "Siti Rahmawati", Role.SISWA, Gender.PEREMPUAN, Jurusan.PPLG, "XII PPLG 2", "🧕", "123456"),
        UserProfile("s-06", "0067654322", "Budi Darmawan", Role.SISWA, Gender.LAKI_LAKI, Jurusan.AKL, "XI AKL 1", "👦", "123456"),
        UserProfile("s-07", "0065544331", "Fitri Handayani", Role.SISWA, Gender.PEREMPUAN, Jurusan.PM, "XII PM 1", "👩", "123456"),
        UserProfile("s-08", "0069988777", "Muh. Ilham Jaya", Role.SISWA, Gender.LAKI_LAKI, Jurusan.TJKT, "XI TJKT 2", "👦", "123456"),
        UserProfile("s-09", "0061122334", "Indah Permatasari", Role.SISWA, Gender.PEREMPUAN, Jurusan.DKV, "XI DKV 1", "🎨", "123456"),
        UserProfile("s-10", "0061122335", "Rian Hidayat", Role.SISWA, Gender.LAKI_LAKI, Jurusan.DKV, "XII DKV 1", "🧑‍🎨", "123456"),
        UserProfile("s-11", "0063344556", "Dewi Sartika", Role.SISWA, Gender.PEREMPUAN, Jurusan.APHP, "X APHP 1", "🌾", "123456"),
        UserProfile("s-12", "0067788990", "Zulfa Azzahra", Role.SISWA, Gender.PEREMPUAN, Jurusan.MPLB, "XI MPLB 1", "📑", "123456")
    )

    private val _allUsers = MutableStateFlow<List<UserProfile>>(initialUsers)
    val users: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()
    val allUsers: List<UserProfile> get() = _allUsers.value
    val allStudents: List<UserProfile> get() = _allUsers.value.filter { it.role == Role.SISWA }

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    // Period / Halangan Syar'i records for female students
    private val _periodRecords = MutableStateFlow<List<PeriodRecord>>(emptyList())
    val periodRecords: StateFlow<List<PeriodRecord>> = _periodRecords.asStateFlow()

    private val _activeQrByPrayer = MutableStateFlow<Map<PrayerType, DynamicQrToken>>(emptyMap())
    val activeQrByPrayer: StateFlow<Map<PrayerType, DynamicQrToken>> = _activeQrByPrayer.asStateFlow()

    private val _currentActiveQr = MutableStateFlow<DynamicQrToken?>(null)
    val currentActiveQr: StateFlow<DynamicQrToken?> = _currentActiveQr.asStateFlow()

    init {
        val today = getTodayDateString()

        // Seed initial local fallback attendance
        val initialList = mutableListOf(
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG 1",
                prayerType = PrayerType.DHUHA,
                dateString = today,
                timestampMillis = System.currentTimeMillis() - 8 * 3600 * 1000,
                formattedTime = "07:35 WITA",
                dhuhaStatus = DhuhaStatus.HADIR
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG 1",
                prayerType = PrayerType.DZUHUR,
                dateString = today,
                timestampMillis = System.currentTimeMillis() - 3 * 3600 * 1000,
                formattedTime = "12:30 WITA"
            )
        )
        _attendanceRecords.value = initialList

        // Seed an active halangan record for student Siti Rahmawati (s-05) starting 3 days ago
        val threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 3600 * 1000)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sitiRecord = PeriodRecord(
            id = UUID.randomUUID().toString(),
            nisn = "0061234569",
            startDateString = sdf.format(Date(threeDaysAgo)),
            estimatedDays = 5,
            startTimestampMillis = threeDaysAgo,
            isActive = true,
            isManuallyEnded = false,
            notes = "Halangan syar'i hari ke-4"
        )
        _periodRecords.value = listOf(sitiRecord)

        // Seed distinct local tokens per prayer type WITHOUT overwriting Firestore on app startup
        val initialTokens = PrayerType.entries.associateWith { createTokenForPrayer(it, today) }
        _activeQrByPrayer.value = initialTokens
        _currentActiveQr.value = initialTokens[PrayerType.DZUHUR]

        // Connect to Cloud Firestore Realtime Sync
        firestore?.let { db ->
            // Keep Admin Pusat account name in sync with Cloud Firestore
            initialUsers.filter { it.role == Role.ADMIN_PUSAT }.forEach { adminUser ->
                db.collection("users").document(adminUser.id).set(userToMap(adminUser))
            }
            setupFirestoreSync(db)
        }
    }

    private fun setupFirestoreSync(db: FirebaseFirestore) {
        // 1. Synchronize Users
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AttendanceRepo", "Users listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    // Seed initial predefined accounts into Firestore
                    initialUsers.forEach { user ->
                        db.collection("users").document(user.id).set(userToMap(user))
                    }
                } else {
                    val loaded = snapshot.documents.mapNotNull { doc -> mapToUser(doc.id, doc.data) }
                    if (loaded.isNotEmpty()) {
                        _allUsers.value = loaded
                    }
                }
            }
        }

        // 2. Synchronize Attendance Records
        db.collection("attendance_records").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AttendanceRepo", "Attendance listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val records = snapshot.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                if (records.isNotEmpty()) {
                    _attendanceRecords.value = records
                }
            }
        }

        // 3. Synchronize Period Records (Halangan Syar'i)
        db.collection("period_records").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AttendanceRepo", "Period listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val pRecords = snapshot.documents.mapNotNull { doc -> mapToPeriod(doc.id, doc.data) }
                if (pRecords.isNotEmpty()) {
                    _periodRecords.value = pRecords
                }
            }
        }

        // 4. Synchronize Active Mosque Dynamic QR Sessions (per prayer type)
        db.collection("active_qr_tokens").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AttendanceRepo", "QR session listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val today = getTodayDateString()
                val updatedMap = _activeQrByPrayer.value.toMutableMap()
                var changed = false
                snapshot.documents.forEach { doc ->
                    if (doc.id.startsWith("session_")) {
                        val token = mapToQrToken(doc.data)
                        if (token != null && token.dateString == today) {
                            updatedMap[token.prayerType] = token
                            changed = true
                        }
                    }
                }
                if (changed) {
                    _activeQrByPrayer.value = updatedMap
                }
            }
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getFormattedCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm 'WITA'", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun createTokenForPrayer(prayerType: PrayerType, today: String): DynamicQrToken {
        // Distinct leading digit per prayer guarantees manual codes never collide across prayers
        val prayerPrefix = when (prayerType) {
            PrayerType.DHUHA -> "1"
            PrayerType.DZUHUR -> "2"
            PrayerType.ASHAR -> "3"
        }
        val shortEntryCode = prayerPrefix + (100..999).random().toString()
        val randomSalt = (100000..999999).random().toString()
        val tokenString = "SMKN1PKP|SHOLAT|${prayerType.code}|$today|$randomSalt|$shortEntryCode"
        return DynamicQrToken(
            dateString = today,
            prayerType = prayerType,
            tokenCode = tokenString,
            shortEntryCode = shortEntryCode,
            generatedAtMillis = System.currentTimeMillis(),
            validUntilMillis = System.currentTimeMillis() + 60 * 60 * 1000
        )
    }

    fun getActiveQrFor(prayerType: PrayerType): DynamicQrToken {
        val today = getTodayDateString()
        val existing = _activeQrByPrayer.value[prayerType]
        if (existing != null && existing.dateString == today) {
            return existing
        }
        val created = createTokenForPrayer(prayerType, today)
        _activeQrByPrayer.value = _activeQrByPrayer.value + (prayerType to created)
        return created
    }

    fun ensurePrayerQrPublished(prayerType: PrayerType): DynamicQrToken {
        val token = getActiveQrFor(prayerType)
        _currentActiveQr.value = token
        firestore?.collection("active_qr_tokens")?.document("session_${prayerType.code}")?.set(qrTokenToMap(token))
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to publish QR token to Firestore: ${it.message}") }
        return token
    }

    fun generateNewDynamicQr(prayerType: PrayerType): DynamicQrToken {
        val today = getTodayDateString()
        val token = createTokenForPrayer(prayerType, today)
        _activeQrByPrayer.value = _activeQrByPrayer.value + (prayerType to token)
        _currentActiveQr.value = token

        // Write to Cloud Firestore under prayer-specific document
        firestore?.collection("active_qr_tokens")?.document("session_${prayerType.code}")?.set(qrTokenToMap(token))
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to sync QR token to Firestore: ${it.message}") }

        return token
    }

    sealed class ScanResult {
        data class Success(
            val prayerType: PrayerType,
            val isGoHomeActivated: Boolean,
            val isDzuhurCompleted: Boolean,
            val isAsharCompleted: Boolean
        ) : ScanResult()
        data class AlreadyScanned(val prayerType: PrayerType, val timeScanned: String) : ScanResult()
        data class InvalidQr(val reason: String) : ScanResult()
    }

    fun processAttendanceInput(inputCode: String, student: UserProfile): ScanResult {
        // If female student is currently on halangan, they are excused from prayer and pass gate automatically
        if (isStudentCurrentlyOnPeriod(student.nisnOrNip)) {
            return ScanResult.InvalidQr("Anda sedang berstatus Berhalangan Syar'i (Tidak wajib sholat). Tiket izin pulang otomatis aktif.")
        }

        val cleanInput = inputCode.trim()
        val today = getTodayDateString()

        // 1. Check 4-6 digit manual code across ALL active prayer sessions for today
        if (cleanInput.length in 4..6 && cleanInput.all { it.isDigit() }) {
            val matchedToken = _activeQrByPrayer.value.values.find {
                it.dateString == today && it.shortEntryCode.equals(cleanInput, ignoreCase = true)
            } ?: _currentActiveQr.value?.takeIf {
                it.dateString == today && it.shortEntryCode.equals(cleanInput, ignoreCase = true)
            }
            if (matchedToken != null) {
                return recordStudentAttendance(student, matchedToken.prayerType, today)
            }
            return ScanResult.InvalidQr("Kode sesi '$cleanInput' tidak cocok dengan sesi sholat aktif hari ini.")
        }

        // 2. Check full QR token payload: SMKN1PKP|SHOLAT|<prayerCode>|<date>|<salt>|[shortCode]
        val parts = cleanInput.split("|")
        if (parts.size >= 5 && parts[0] == "SMKN1PKP" && parts[1] == "SHOLAT") {
            val prayerCode = parts[2]
            val qrDate = parts[3]

            if (qrDate != today) {
                return ScanResult.InvalidQr("Kode QR kadaluarsa! QR ini untuk tanggal $qrDate.")
            }

            val prayerType = PrayerType.entries.find { it.code == prayerCode }
                ?: return ScanResult.InvalidQr("Waktu Sholat tidak dikenal.")

            return recordStudentAttendance(student, prayerType, today)
        }

        return ScanResult.InvalidQr("Kode tidak cocok dengan sesi sholat aktif di masjid.")
    }

    private fun recordStudentAttendance(student: UserProfile, prayerType: PrayerType, today: String): ScanResult {
        val existing = _attendanceRecords.value.find {
            it.nisn == student.nisnOrNip &&
            it.dateString == today &&
            it.prayerType == prayerType
        }

        if (existing != null) {
            return ScanResult.AlreadyScanned(prayerType, existing.formattedTime)
        }

        val newRecord = AttendanceRecord(
            id = UUID.randomUUID().toString(),
            nisn = student.nisnOrNip,
            studentName = student.fullName,
            jurusan = student.jurusan ?: Jurusan.PPLG,
            kelas = student.kelas ?: "X PPLG 1",
            prayerType = prayerType,
            dateString = today,
            timestampMillis = System.currentTimeMillis(),
            formattedTime = getFormattedCurrentTime(),
            isVerifiedInMosque = true,
            isHalanganSyari = false,
            dhuhaStatus = DhuhaStatus.HADIR
        )

        _attendanceRecords.value = _attendanceRecords.value + newRecord

        // Push new attendance record to Firestore
        firestore?.collection("attendance_records")?.document(newRecord.id)?.set(attendanceToMap(newRecord))
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to sync attendance to Firestore: ${it.message}") }

        val todayRecords = _attendanceRecords.value.filter {
            it.nisn == student.nisnOrNip && it.dateString == today
        }
        val dzuhurDone = todayRecords.any { it.prayerType == PrayerType.DZUHUR }
        val asharDone = todayRecords.any { it.prayerType == PrayerType.ASHAR }

        return ScanResult.Success(
            prayerType = prayerType,
            isGoHomeActivated = dzuhurDone && asharDone,
            isDzuhurCompleted = dzuhurDone,
            isAsharCompleted = asharDone
        )
    }

    // Set Dhuha Sunnah
    fun setDhuhaChoice(student: UserProfile, isPresent: Boolean) {
        val today = getTodayDateString()
        val existing = _attendanceRecords.value.find {
            it.nisn == student.nisnOrNip &&
            it.dateString == today &&
            it.prayerType == PrayerType.DHUHA
        }

        val status = if (isPresent) DhuhaStatus.HADIR else DhuhaStatus.TIDAK_HADIR

        if (existing != null) {
            val updated = existing.copy(dhuhaStatus = status)
            _attendanceRecords.value = _attendanceRecords.value.map {
                if (it.id == existing.id) updated else it
            }
            firestore?.collection("attendance_records")?.document(existing.id)?.update("dhuhaStatus", status.name)
        } else {
            val newRecord = AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = student.nisnOrNip,
                studentName = student.fullName,
                jurusan = student.jurusan ?: Jurusan.PPLG,
                kelas = student.kelas ?: "X PPLG 1",
                prayerType = PrayerType.DHUHA,
                dateString = today,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = getFormattedCurrentTime(),
                isVerifiedInMosque = isPresent,
                isHalanganSyari = false,
                dhuhaStatus = status
            )
            _attendanceRecords.value = _attendanceRecords.value + newRecord
            firestore?.collection("attendance_records")?.document(newRecord.id)?.set(attendanceToMap(newRecord))
        }
    }

    // --- HALANGAN SYAR'I (PERIOD LOGIC FOR FEMALE STUDENTS) ---

    fun getActivePeriodRecord(nisn: String): PeriodRecord? {
        val now = System.currentTimeMillis()
        val record = _periodRecords.value.find { it.nisn == nisn && it.isActive && !it.isManuallyEnded }
            ?: return null

        val elapsedDays = ((now - record.startTimestampMillis) / (24L * 3600 * 1000)).toInt() + 1
        // Max limit in Fiqh is 15 days; beyond that it is Istihadah (must pray)
        if (elapsedDays > 15) {
            return null
        }
        return record
    }

    fun isStudentCurrentlyOnPeriod(nisn: String): Boolean {
        return getActivePeriodRecord(nisn) != null
    }

    // Start a new Period report with specified estimated days (e.g. 5, 6, 7 days)
    fun reportPeriodStart(student: UserProfile, estimatedDays: Int, notes: String = "") {
        val today = getTodayDateString()
        val newRecord = PeriodRecord(
            id = UUID.randomUUID().toString(),
            nisn = student.nisnOrNip,
            startDateString = today,
            estimatedDays = estimatedDays.coerceIn(3, 15),
            startTimestampMillis = System.currentTimeMillis(),
            isActive = true,
            isManuallyEnded = false,
            notes = notes
        )
        // Deactivate previous active records for this student
        val updated = _periodRecords.value.map {
            if (it.nisn == student.nisnOrNip) it.copy(isActive = false) else it
        } + newRecord
        _periodRecords.value = updated

        // Push to Cloud Firestore
        firestore?.collection("period_records")?.document(newRecord.id)?.set(periodToMap(newRecord))
    }

    // End period (Suci / Mandi Wajib done, ready to pray)
    fun reportPeriodEnd(nisn: String) {
        val target = _periodRecords.value.find { it.nisn == nisn && it.isActive }
        _periodRecords.value = _periodRecords.value.map {
            if (it.nisn == nisn && it.isActive) it.copy(isActive = false, isManuallyEnded = true) else it
        }
        target?.let {
            firestore?.collection("period_records")?.document(it.id)?.update(
                mapOf("isActive" to false, "isManuallyEnded" to true)
            )
        }
    }

    // Check if period limit has expired (notification condition)
    data class PeriodNotificationStatus(
        val hasExpired: Boolean,
        val currentDay: Int,
        val estimatedDays: Int,
        val message: String?
    )

    fun checkPeriodStatus(nisn: String): PeriodNotificationStatus {
        val record = _periodRecords.value.find { it.nisn == nisn && !it.isManuallyEnded }
            ?: return PeriodNotificationStatus(false, 0, 0, null)

        val now = System.currentTimeMillis()
        val currentDay = ((now - record.startTimestampMillis) / (24L * 3600 * 1000)).toInt() + 1

        return if (currentDay > record.estimatedDays) {
            PeriodNotificationStatus(
                hasExpired = true,
                currentDay = currentDay,
                estimatedDays = record.estimatedDays,
                message = "Perhatian: Batas estimasi masa berhalangan Anda (${record.estimatedDays} hari) telah berakhir (Hari ke-$currentDay). Silakan lakukan mandi wajib (bersuci) dan kembali mengikuti sholat berjamaah di Masjid Darul Ulum SMKN 1 Pangkajene dan Kepulauan."
            )
        } else {
            PeriodNotificationStatus(
                hasExpired = false,
                currentDay = currentDay,
                estimatedDays = record.estimatedDays,
                message = "Status Berhalangan Aktif: Hari ke-$currentDay dari estimasi ${record.estimatedDays} hari. Bebas kewajiban sholat dan izin pulang aktif."
            )
        }
    }

    fun isPrayerCompletedToday(nisn: String, prayerType: PrayerType): Boolean {
        val today = getTodayDateString()
        return _attendanceRecords.value.any {
            it.nisn == nisn &&
            it.dateString == today &&
            it.prayerType == prayerType
        }
    }

    fun isGoHomeEligibleToday(nisn: String): Boolean {
        // If student is on active halangan syar'i, they are automatically excused and allowed to go home
        if (isStudentCurrentlyOnPeriod(nisn)) {
            return true
        }

        val today = getTodayDateString()
        val todayRecords = _attendanceRecords.value.filter {
            it.nisn == nisn && it.dateString == today
        }
        val hasDzuhur = todayRecords.any { it.prayerType == PrayerType.DZUHUR }
        val hasAshar = todayRecords.any { it.prayerType == PrayerType.ASHAR }
        return hasDzuhur && hasAshar
    }

    // --- REGISTRASI SISWA MANDIRI ---

    fun registerStudent(
        fullName: String,
        kelas: String,
        jurusan: Jurusan,
        gender: Gender,
        nisn: String,
        password: String
    ): Result<UserProfile> {
        val cleanNisn = nisn.trim()
        val cleanName = fullName.trim()
        val cleanKelas = kelas.trim()
        val cleanPass = password.trim().ifEmpty { "123456" }

        if (cleanNisn.isEmpty() || cleanName.isEmpty() || cleanKelas.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nama lengkap, Kelas, dan NISN wajib diisi"))
        }

        if (_allUsers.value.any { it.nisnOrNip.equals(cleanNisn, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("NISN '$cleanNisn' sudah terdaftar! Silakan langsung login."))
        }

        val avatar = if (gender == Gender.PEREMPUAN) "🧕" else "👦"
        val newStudent = UserProfile(
            id = "s-${UUID.randomUUID().toString().take(6)}",
            nisnOrNip = cleanNisn,
            fullName = cleanName,
            role = Role.SISWA,
            gender = gender,
            jurusan = jurusan,
            kelas = cleanKelas,
            avatarEmoji = avatar,
            password = cleanPass
        )

        _allUsers.value = _allUsers.value + newStudent

        // Sync new student account to Cloud Firestore
        firestore?.collection("users")?.document(newStudent.id)?.set(userToMap(newStudent))
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to sync student to Firestore: ${it.message}") }

        return Result.success(newStudent)
    }

    // --- ADMIN PUSAT: TAMBAH AKUN GURU & RESET PASSWORD ---

    fun addTeacher(
        nipOrUsername: String,
        fullName: String,
        gender: Gender,
        jurusan: Jurusan?,
        kelas: String?,
        initialPassword: String = "guru"
    ): Result<UserProfile> {
        val cleanNip = nipOrUsername.trim()
        val cleanName = fullName.trim()

        if (cleanNip.isEmpty() || cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("NIP/Username dan Nama Lengkap wajib diisi"))
        }

        // Check duplicate
        if (_allUsers.value.any { it.nisnOrNip.equals(cleanNip, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("NIP / Username '$cleanNip' sudah terdaftar!"))
        }

        val avatar = if (gender == Gender.PEREMPUAN) "👩‍🏫" else "👨‍🏫"
        val newTeacher = UserProfile(
            id = "guru-${UUID.randomUUID().toString().take(6)}",
            nisnOrNip = cleanNip,
            fullName = cleanName,
            role = Role.ADMIN_GURU,
            gender = gender,
            jurusan = jurusan,
            kelas = kelas?.trim()?.ifEmpty { null },
            avatarEmoji = avatar,
            password = initialPassword.trim().ifEmpty { "guru" }
        )

        _allUsers.value = _allUsers.value + newTeacher

        // Sync new teacher to Cloud Firestore
        firestore?.collection("users")?.document(newTeacher.id)?.set(userToMap(newTeacher))
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to sync teacher to Firestore: ${it.message}") }

        return Result.success(newTeacher)
    }

    fun resetPassword(targetUserIdOrNisn: String, newPassword: String): Boolean {
        val cleanTarget = targetUserIdOrNisn.trim()
        val cleanPassword = newPassword.trim().ifEmpty { "123456" }

        val found = _allUsers.value.find {
            it.id == cleanTarget || it.nisnOrNip.equals(cleanTarget, ignoreCase = true)
        } ?: return false

        _allUsers.value = _allUsers.value.map {
            if (it.id == found.id) it.copy(password = cleanPassword) else it
        }

        // Sync password update to Cloud Firestore
        firestore?.collection("users")?.document(found.id)?.update("password", cleanPassword)
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Failed to update password in Firestore: ${it.message}") }

        return true
    }

    /**
     * Admin Pusat: Hapus akun guru atau siswa
     */
    fun deleteUser(targetUserId: String): Boolean {
        val userToDelete = _allUsers.value.find { it.id == targetUserId } ?: return false

        // Lindungi akun Admin Pusat dari penghapusan tidak sengaja
        if (userToDelete.role == Role.ADMIN_PUSAT) {
            Log.w("AttendanceRepo", "Akun Admin Pusat tidak dapat dihapus.")
            return false
        }

        // Hapus dari state lokal
        _allUsers.value = _allUsers.value.filter { it.id != targetUserId }

        // Jika siswa dihapus, bersihkan juga rekaman presensi dan laporan halangannya
        if (userToDelete.role == Role.SISWA) {
            _attendanceRecords.value = _attendanceRecords.value.filter { it.nisn != userToDelete.nisnOrNip }
            _periodRecords.value = _periodRecords.value.filter { it.nisn != userToDelete.nisnOrNip }
        }

        // Sinkronkan penghapusan ke Cloud Firestore
        firestore?.collection("users")?.document(targetUserId)?.delete()
            ?.addOnFailureListener { Log.w("AttendanceRepo", "Gagal menghapus user di Firestore: ${it.message}") }

        return true
    }

    /**
     * Admin Pusat: Hapus massal seluruh akun berdasarkan peran (GURU atau SISWA)
     */
    fun deleteBulkUsers(roleToDelete: Role): Int {
        if (roleToDelete == Role.ADMIN_PUSAT) return 0

        val toRemove = _allUsers.value.filter { it.role == roleToDelete }
        if (toRemove.isEmpty()) return 0

        _allUsers.value = _allUsers.value.filter { it.role != roleToDelete }

        if (roleToDelete == Role.SISWA) {
            _attendanceRecords.value = emptyList()
            _periodRecords.value = emptyList()
        }

        // Hapus dari Cloud Firestore
        firestore?.let { db ->
            toRemove.forEach { u ->
                db.collection("users").document(u.id).delete()
            }
            if (roleToDelete == Role.SISWA) {
                db.collection("attendance_records").get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { it.reference.delete() }
                }
                db.collection("period_records").get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { it.reference.delete() }
                }
            }
        }

        return toRemove.size
    }

    // --- FIRESTORE MAP SERIALIZERS & DESERIALIZERS ---

    private fun userToMap(u: UserProfile): Map<String, Any?> = mapOf(
        "id" to u.id,
        "nisnOrNip" to u.nisnOrNip,
        "fullName" to u.fullName,
        "role" to u.role.name,
        "gender" to u.gender.name,
        "jurusan" to u.jurusan?.name,
        "kelas" to u.kelas,
        "avatarEmoji" to u.avatarEmoji,
        "password" to u.password
    )

    private fun mapToUser(docId: String, data: Map<String, Any?>?): UserProfile? {
        if (data == null) return null
        val roleStr = data["role"] as? String ?: return null
        val role = try { Role.valueOf(roleStr) } catch (e: Exception) { Role.SISWA }
        val genderStr = data["gender"] as? String ?: "LAKI_LAKI"
        val gender = try { Gender.valueOf(genderStr) } catch (e: Exception) { Gender.LAKI_LAKI }
        val jurStr = data["jurusan"] as? String
        val jurusan = if (jurStr != null) try { Jurusan.valueOf(jurStr) } catch (e: Exception) { null } else null

        return UserProfile(
            id = docId,
            nisnOrNip = data["nisnOrNip"] as? String ?: docId,
            fullName = data["fullName"] as? String ?: "Pengguna",
            role = role,
            gender = gender,
            jurusan = jurusan,
            kelas = data["kelas"] as? String,
            avatarEmoji = data["avatarEmoji"] as? String ?: "👤",
            password = data["password"] as? String ?: "123456"
        )
    }

    private fun attendanceToMap(a: AttendanceRecord): Map<String, Any?> = mapOf(
        "id" to a.id,
        "nisn" to a.nisn,
        "studentName" to a.studentName,
        "jurusan" to a.jurusan.name,
        "kelas" to a.kelas,
        "prayerType" to a.prayerType.name,
        "dateString" to a.dateString,
        "timestampMillis" to a.timestampMillis,
        "formattedTime" to a.formattedTime,
        "isVerifiedInMosque" to a.isVerifiedInMosque,
        "isHalanganSyari" to a.isHalanganSyari,
        "dhuhaStatus" to a.dhuhaStatus.name
    )

    private fun mapToAttendance(docId: String, data: Map<String, Any?>?): AttendanceRecord? {
        if (data == null) return null
        val prayerStr = data["prayerType"] as? String ?: return null
        val prayerType = try { PrayerType.valueOf(prayerStr) } catch (e: Exception) { return null }
        val jurStr = data["jurusan"] as? String ?: "PPLG"
        val jurusan = try { Jurusan.valueOf(jurStr) } catch (e: Exception) { Jurusan.PPLG }
        val dhuhaStr = data["dhuhaStatus"] as? String ?: "HADIR"
        val dhuhaStatus = try { DhuhaStatus.valueOf(dhuhaStr) } catch (e: Exception) { DhuhaStatus.HADIR }

        return AttendanceRecord(
            id = docId,
            nisn = data["nisn"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            jurusan = jurusan,
            kelas = data["kelas"] as? String ?: "",
            prayerType = prayerType,
            dateString = data["dateString"] as? String ?: "",
            timestampMillis = (data["timestampMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            formattedTime = data["formattedTime"] as? String ?: "",
            isVerifiedInMosque = (data["isVerifiedInMosque"] as? Boolean) ?: true,
            isHalanganSyari = (data["isHalanganSyari"] as? Boolean) ?: false,
            dhuhaStatus = dhuhaStatus
        )
    }

    private fun periodToMap(p: PeriodRecord): Map<String, Any?> = mapOf(
        "id" to p.id,
        "nisn" to p.nisn,
        "startDateString" to p.startDateString,
        "estimatedDays" to p.estimatedDays,
        "startTimestampMillis" to p.startTimestampMillis,
        "isActive" to p.isActive,
        "isManuallyEnded" to p.isManuallyEnded,
        "notes" to p.notes
    )

    private fun mapToPeriod(docId: String, data: Map<String, Any?>?): PeriodRecord? {
        if (data == null) return null
        return PeriodRecord(
            id = docId,
            nisn = data["nisn"] as? String ?: "",
            startDateString = data["startDateString"] as? String ?: "",
            estimatedDays = (data["estimatedDays"] as? Number)?.toInt() ?: 5,
            startTimestampMillis = (data["startTimestampMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isActive = (data["isActive"] as? Boolean) ?: true,
            isManuallyEnded = (data["isManuallyEnded"] as? Boolean) ?: false,
            notes = data["notes"] as? String ?: ""
        )
    }

    private fun qrTokenToMap(q: DynamicQrToken): Map<String, Any?> = mapOf(
        "dateString" to q.dateString,
        "prayerType" to q.prayerType.name,
        "tokenCode" to q.tokenCode,
        "shortEntryCode" to q.shortEntryCode,
        "generatedAtMillis" to q.generatedAtMillis,
        "validUntilMillis" to q.validUntilMillis
    )

    private fun mapToQrToken(data: Map<String, Any?>?): DynamicQrToken? {
        if (data == null) return null
        val prayerStr = data["prayerType"] as? String ?: return null
        val prayerType = try { PrayerType.valueOf(prayerStr) } catch (e: Exception) { PrayerType.ASHAR }
        return DynamicQrToken(
            dateString = data["dateString"] as? String ?: "",
            prayerType = prayerType,
            tokenCode = data["tokenCode"] as? String ?: "",
            shortEntryCode = data["shortEntryCode"] as? String ?: "4829",
            generatedAtMillis = (data["generatedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            validUntilMillis = (data["validUntilMillis"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 3600000)
        )
    }
}
