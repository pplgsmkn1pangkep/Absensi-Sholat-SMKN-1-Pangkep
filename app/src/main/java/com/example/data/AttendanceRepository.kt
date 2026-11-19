package com.example.data

import android.util.Log
import com.example.model.AttendanceRecord
import com.example.model.DhuhaStatus
import com.example.model.DynamicQrToken
import com.example.model.Gender
import com.example.model.Jurusan
import com.example.model.PeriodRecord
import com.example.model.PrayerType
import com.example.model.Role
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceRepository(
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        try {
            FirebaseConfig.getFirestore()
        } catch (fallbackError: Throwable) {
            Log.w(
                "AttendanceRepo",
                "Firestore not available or uninitialized, falling back to local storage: ${fallbackError.message}"
            )
            null
        }
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
            password = "guru",
            jabatan = "Guru Agama / Piket Masjid"
        ),
        UserProfile(
            id = "guru-02",
            nisnOrNip = "wali.pplg",
            fullName = "Drs. H. Ahmad Dahlan, M.Pd (Wali Kelas XII PPLG)",
            role = Role.ADMIN_GURU,
            gender = Gender.LAKI_LAKI,
            jurusan = Jurusan.PPLG,
            kelas = "XII PPLG",
            avatarEmoji = "👨‍🏫",
            password = "guru",
            jabatan = "Wali Kelas"
        ),
        UserProfile(
            id = "guru-03",
            nisnOrNip = "kaprodi.pplg",
            fullName = "Ir. St. Rahmah, S.Kom, M.T (Kaprodi PPLG)",
            role = Role.ADMIN_GURU,
            gender = Gender.PEREMPUAN,
            jurusan = Jurusan.PPLG,
            avatarEmoji = "👩‍💼",
            password = "guru",
            jabatan = "Kaprodi"
        ),

        // 3. Siswa (All 7 Majors, including female students)
        UserProfile("s-01", "0061234567", "Muhammad Fadhil", Role.SISWA, Gender.LAKI_LAKI, Jurusan.PPLG, "XII PPLG", "👨‍💻", "123456"),
        UserProfile("s-02", "0067654321", "Nur Aisyah Putri", Role.SISWA, Gender.PEREMPUAN, Jurusan.AKL, "XI AKL 2", "🧕", "123456"),
        UserProfile("s-03", "0069988776", "Andi Reski Pratama", Role.SISWA, Gender.LAKI_LAKI, Jurusan.TJKT, "X TJKT 1", "🧑‍💻", "123456"),
        UserProfile("s-04", "0061234568", "Ahmad Fauzan R", Role.SISWA, Gender.LAKI_LAKI, Jurusan.PPLG, "XII PPLG", "👦", "123456"),
        UserProfile("s-05", "0061234569", "Siti Rahmawati", Role.SISWA, Gender.PEREMPUAN, Jurusan.PPLG, "XI PPLG", "🧕", "123456"),
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

    // Dedicated state for one-shot 7-day recap results
    private val _sevenDayRecapRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val sevenDayRecapRecords: StateFlow<List<AttendanceRecord>> = _sevenDayRecapRecords.asStateFlow()

    // Period / Halangan Syar'i records for female students
    private val _periodRecords = MutableStateFlow<List<PeriodRecord>>(emptyList())
    val periodRecords: StateFlow<List<PeriodRecord>> = _periodRecords.asStateFlow()

    private val _activeQrByPrayer = MutableStateFlow<Map<PrayerType, DynamicQrToken>>(emptyMap())
    val activeQrByPrayer: StateFlow<Map<PrayerType, DynamicQrToken>> = _activeQrByPrayer.asStateFlow()

    private val _currentActiveQr = MutableStateFlow<DynamicQrToken?>(null)
    val currentActiveQr: StateFlow<DynamicQrToken?> = _currentActiveQr.asStateFlow()

    private val _isLoadingRemote = MutableStateFlow(false)
    val isLoadingRemote: StateFlow<Boolean> = _isLoadingRemote.asStateFlow()

    // Active scoped listeners (MUST be declared above init and removed on logout / onCleared)
    private var studentAttendanceListener: ListenerRegistration? = null
    private var studentPeriodListener: ListenerRegistration? = null
    private var activeQrListener: ListenerRegistration? = null
    private var listeningQrPrayer: PrayerType? = null

    // Cooldown cache timestamps to prevent redundant .get() reads on rapid tab/filter switches
    private var lastUsersFetchKey: String? = null
    private var lastUsersFetchMillis: Long = 0L
    private var lastTodayAttendanceFetchKey: String? = null
    private var lastTodayAttendanceFetchMillis: Long = 0L
    private var lastSevenDayRecapKey: String? = null
    private var lastSevenDayRecapMillis: Long = 0L
    private var lastSevenDayRecapCoversAllScopes: Boolean = false
    private var lastActivePeriodsFetchMillis: Long = 0L
    private val syncedRemoteQrDates = mutableMapOf<PrayerType, String>()

    companion object {
        private const val TAG = "AttendanceRepo"
        private const val CACHE_COOLDOWN_MS = 30_000L
        private const val USERS_CACHE_COOLDOWN_MS = 300_000L
        private const val RECAP_CACHE_COOLDOWN_MS = 300_000L
    }

    init {
        val today = getTodayDateString()
        val yesterday = getDateStringDaysAgo(1)
        val twoDaysAgo = getDateStringDaysAgo(2)

        // Seed initial local fallback attendance (used offline / before remote sync returns)
        val initialList = mutableListOf(
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
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
                kelas = "XII PPLG",
                prayerType = PrayerType.DZUHUR,
                dateString = today,
                timestampMillis = System.currentTimeMillis() - 3 * 3600 * 1000,
                formattedTime = "12:30 WITA"
            ),
            // Yesterday records
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.DHUHA,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 28L * 3600 * 1000,
                formattedTime = "07:25 WITA",
                dhuhaStatus = DhuhaStatus.HADIR
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.DZUHUR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 27L * 3600 * 1000,
                formattedTime = "12:20 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.ASHAR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 24L * 3600 * 1000,
                formattedTime = "15:35 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234568",
                studentName = "Ahmad Fauzan R",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.DZUHUR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 27L * 3600 * 1000,
                formattedTime = "12:24 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234568",
                studentName = "Ahmad Fauzan R",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.ASHAR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 24L * 3600 * 1000,
                formattedTime = "15:40 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0067654321",
                studentName = "Nur Aisyah Putri",
                jurusan = Jurusan.AKL,
                kelas = "XI AKL 2",
                prayerType = PrayerType.DZUHUR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 27L * 3600 * 1000,
                formattedTime = "12:22 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0067654321",
                studentName = "Nur Aisyah Putri",
                jurusan = Jurusan.AKL,
                kelas = "XI AKL 2",
                prayerType = PrayerType.ASHAR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 24L * 3600 * 1000,
                formattedTime = "15:38 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0069988776",
                studentName = "Andi Reski Pratama",
                jurusan = Jurusan.TJKT,
                kelas = "X TJKT 1",
                prayerType = PrayerType.DZUHUR,
                dateString = yesterday,
                timestampMillis = System.currentTimeMillis() - 27L * 3600 * 1000,
                formattedTime = "12:28 WITA"
            ),
            // 2 days ago records
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234567",
                studentName = "Muhammad Fadhil",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.DZUHUR,
                dateString = twoDaysAgo,
                timestampMillis = System.currentTimeMillis() - 51L * 3600 * 1000,
                formattedTime = "12:25 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0061234568",
                studentName = "Ahmad Fauzan R",
                jurusan = Jurusan.PPLG,
                kelas = "XII PPLG",
                prayerType = PrayerType.DHUHA,
                dateString = twoDaysAgo,
                timestampMillis = System.currentTimeMillis() - 55L * 3600 * 1000,
                formattedTime = "07:40 WITA",
                dhuhaStatus = DhuhaStatus.HADIR
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0067654322",
                studentName = "Budi Darmawan",
                jurusan = Jurusan.AKL,
                kelas = "XI AKL 1",
                prayerType = PrayerType.DZUHUR,
                dateString = twoDaysAgo,
                timestampMillis = System.currentTimeMillis() - 51L * 3600 * 1000,
                formattedTime = "12:29 WITA"
            ),
            AttendanceRecord(
                id = UUID.randomUUID().toString(),
                nisn = "0067654322",
                studentName = "Budi Darmawan",
                jurusan = Jurusan.AKL,
                kelas = "XI AKL 1",
                prayerType = PrayerType.ASHAR,
                dateString = twoDaysAgo,
                timestampMillis = System.currentTimeMillis() - 48L * 3600 * 1000,
                formattedTime = "15:32 WITA"
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

        // Seed distinct local tokens per prayer type WITHOUT querying/overwriting Firestore on cold start
        val initialTokens = PrayerType.entries.associateWith { createTokenForPrayer(it, today) }
        _activeQrByPrayer.value = initialTokens
        _currentActiveQr.value = initialTokens[getCurrentActivePrayerSession()]

        // Hemat kuota Spark Plan: TIDAK memasang listener koleksi global saat startup sebelum login.
        // Semua query/listener dipasang sesuai peran (SISWA / ADMIN_GURU / ADMIN_PUSAT) setelah login.
    }

    // =========================================================================
    // SPARK-PLAN QUOTA-OPTIMIZED FIRESTORE QUERIES & SCOPED LISTENERS
    // =========================================================================

    /**
     * 1A. Efisiensi Login:
     * Mengecek akun pengguna yang login menggunakan query spesifik:
     * .whereEqualTo("nisnOrNip", username).limit(1).get()
     * (Maksimal 1 Read per login, TIDAK mengunduh seluruh koleksi users).
     */
    fun fetchUserByNisnOrNip(
        username: String,
        onResult: (UserProfile?) -> Unit
    ) {
        val cleanUsername = username.trim()
        if (cleanUsername.isEmpty()) {
            onResult(null)
            return
        }

        val db = firestore
        if (db == null) {
            val local = _allUsers.value.find {
                it.nisnOrNip.equals(cleanUsername, ignoreCase = true) ||
                    it.id.equals(cleanUsername, ignoreCase = true)
            }
            onResult(local)
            return
        }

        _isLoadingRemote.value = true
        db.collection("users")
            .whereEqualTo("nisnOrNip", cleanUsername)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                _isLoadingRemote.value = false
                val doc = snapshot.documents.firstOrNull()
                val remoteUser = doc?.let { mapToUser(it.id, it.data) }
                if (remoteUser != null) {
                    upsertUsersLocally(listOf(remoteUser))
                }
                onResult(remoteUser)
            }
            .addOnFailureListener { error ->
                _isLoadingRemote.value = false
                Log.w(TAG, "Login query error for '$cleanUsername': ${error.message}")
                onResult(null)
            }
    }

    /**
     * 1B. Efisiensi Daftar Users untuk Admin / Guru (Sekali Ambil / .get(), BUKAN addSnapshotListener):
     * - Jika Wali Kelas (kelas != null): hanya mengambil siswa pada kelas tersebut.
     * - Jika Kaprodi / Guru Jurusan (jurusan != null): hanya mengambil siswa pada jurusan tersebut.
     * - Jika Admin Pusat (includeAllRoles = true / tanpa filter): mengambil daftar users sekali jalan.
     */
    fun fetchUsersOnce(
        jurusan: Jurusan? = null,
        kelas: String? = null,
        includeAllRoles: Boolean = false,
        forceRefresh: Boolean = false,
        onComplete: ((List<UserProfile>) -> Unit)? = null
    ) {
        val db = firestore ?: run {
            onComplete?.invoke(_allUsers.value)
            return
        }

        val cleanKelas = kelas?.trim()?.takeIf { it.isNotEmpty() }
        val fetchKey = "${jurusan?.name ?: "ALL"}|${cleanKelas ?: "ALL"}|$includeAllRoles"
        val now = System.currentTimeMillis()
        val isCoveredByFullCache = lastUsersFetchKey == "ALL|ALL|true" &&
            (now - lastUsersFetchMillis) < USERS_CACHE_COOLDOWN_MS
        if (!forceRefresh && (isCoveredByFullCache ||
                (lastUsersFetchKey == fetchKey && (now - lastUsersFetchMillis) < USERS_CACHE_COOLDOWN_MS))
        ) {
            onComplete?.invoke(_allUsers.value)
            return
        }

        val query: Query = when {
            includeAllRoles -> db.collection("users")
            cleanKelas != null -> db.collection("users")
                .whereEqualTo("role", Role.SISWA.name)
                .whereEqualTo("kelas", cleanKelas)
            jurusan != null -> db.collection("users")
                .whereEqualTo("role", Role.SISWA.name)
                .whereEqualTo("jurusan", jurusan.name)
            else -> db.collection("users")
                .whereEqualTo("role", Role.SISWA.name)
        }

        _isLoadingRemote.value = true
        query.get()
            .addOnSuccessListener { snapshot ->
                _isLoadingRemote.value = false
                lastUsersFetchKey = fetchKey
                lastUsersFetchMillis = System.currentTimeMillis()
                val loaded = snapshot.documents.mapNotNull { doc -> mapToUser(doc.id, doc.data) }
                if (loaded.isNotEmpty()) {
                    if (includeAllRoles || (jurusan == null && cleanKelas == null)) {
                        // Full fetch for Admin Pusat: merge while preserving protected initial admins if absent
                        val merged = loaded.toMutableList()
                        initialUsers.filter { it.role == Role.ADMIN_PUSAT }.forEach { adminUser ->
                            if (merged.none { it.id == adminUser.id }) {
                                merged.add(adminUser)
                            }
                        }
                        _allUsers.value = merged
                    } else {
                        // Scoped fetch for Guru/Kaprodi: replace only students inside this scope,
                        // keeping the logged-in teacher and other cached accounts intact.
                        val remaining = _allUsers.value.filterNot { existing ->
                            existing.role == Role.SISWA &&
                                (jurusan == null || existing.jurusan == jurusan) &&
                                (cleanKelas == null || existing.kelas.equals(cleanKelas, ignoreCase = true))
                        }
                        _allUsers.value = mergeUsersById(remaining, loaded)
                    }
                }
                onComplete?.invoke(_allUsers.value)
            }
            .addOnFailureListener { error ->
                _isLoadingRemote.value = false
                Log.w(TAG, "fetchUsersOnce failed ($fetchKey): ${error.message}")
                onComplete?.invoke(_allUsers.value)
            }
    }

    /**
     * 2A. Filter Absensi Role SISWA (Realtime Listener Terbatas):
     * Memasang addSnapshotListener pada attendance_records HANYA untuk dokumen milik siswa ini
     * (whereEqualTo("studentId", student.id)) dan tanggal hari ini (whereEqualTo("dateString", today)).
     * Tidak pernah memuat data absensi siswa lain pada akun siswa.
     */
    fun startStudentAttendanceListener(student: UserProfile) {
        studentAttendanceListener?.remove()
        studentAttendanceListener = null
        studentPeriodListener?.remove()
        studentPeriodListener = null

        val today = getTodayDateString()
        // Isolasi data lokal: akun siswa hanya menyimpan data absensinya sendiri untuk hari ini
        _attendanceRecords.value = _attendanceRecords.value.filter {
            it.nisn == student.nisnOrNip && it.dateString == today
        }

        val db = firestore ?: return

        studentAttendanceListener = db.collection("attendance_records")
            .whereEqualTo("studentId", student.id)
            .whereEqualTo("dateString", today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Student attendance listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remoteRecords = snapshot.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                    // Strictly isolate to this student's own today records
                    _attendanceRecords.value = remoteRecords
                }
            }

        // Khusus siswa perempuan: dengarkan hanya status halangan aktif milik siswa sendiri (0-1 dokumen)
        if (student.gender == Gender.PEREMPUAN) {
            studentPeriodListener = db.collection("period_records")
                .whereEqualTo("nisn", student.nisnOrNip)
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Student period listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remotePeriods = snapshot.documents.mapNotNull { doc -> mapToPeriod(doc.id, doc.data) }
                        val others = _periodRecords.value.filterNot { it.nisn == student.nisnOrNip }
                        _periodRecords.value = others + remotePeriods
                    }
                }
        }
    }

    /**
     * 2B. Filter Absensi Role ADMIN_GURU / ADMIN_PUSAT (Sekali Ambil / .get() Hari Ini):
     * Mengambil data attendance_records khusus tanggal hari ini (whereEqualTo("dateString", today))
     * disertai filter kelas / jurusan jika tersedia agar 100 guru memantau bersamaan tetap sangat hemat kuota.
     */
    fun fetchTodayAttendanceOnce(
        jurusan: Jurusan? = null,
        kelas: String? = null,
        forceRefresh: Boolean = false,
        onComplete: ((List<AttendanceRecord>) -> Unit)? = null
    ) {
        val db = firestore ?: run {
            onComplete?.invoke(_attendanceRecords.value)
            return
        }

        val today = getTodayDateString()
        val cleanKelas = kelas?.trim()?.takeIf { it.isNotEmpty() }
        val fetchKey = "$today|${jurusan?.name ?: "ALL"}|${cleanKelas ?: "ALL"}"
        val now = System.currentTimeMillis()
        if (!forceRefresh && lastTodayAttendanceFetchKey == fetchKey && (now - lastTodayAttendanceFetchMillis) < CACHE_COOLDOWN_MS) {
            onComplete?.invoke(_attendanceRecords.value)
            return
        }

        var query: Query = db.collection("attendance_records")
            .whereEqualTo("dateString", today)
        if (cleanKelas != null) {
            query = query.whereEqualTo("kelas", cleanKelas)
        } else if (jurusan != null) {
            query = query.whereEqualTo("jurusan", jurusan.name)
        }

        _isLoadingRemote.value = true
        query.get()
            .addOnSuccessListener { snapshot ->
                _isLoadingRemote.value = false
                lastTodayAttendanceFetchKey = fetchKey
                lastTodayAttendanceFetchMillis = System.currentTimeMillis()
                val loaded = snapshot.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                replaceAttendanceScopeLocally(
                    targetDate = today,
                    jurusan = jurusan,
                    kelas = cleanKelas,
                    newRecords = loaded
                )
                onComplete?.invoke(_attendanceRecords.value)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Scoped today attendance query failed ($fetchKey), falling back to date-only query: ${error.message}")
                // Safe fallback if a scoped query ever fails: query by dateString only
                if (cleanKelas != null || jurusan != null) {
                    db.collection("attendance_records")
                        .whereEqualTo("dateString", today)
                        .get()
                        .addOnSuccessListener { fallbackSnap ->
                            _isLoadingRemote.value = false
                            lastTodayAttendanceFetchKey = fetchKey
                            lastTodayAttendanceFetchMillis = System.currentTimeMillis()
                            val loaded = fallbackSnap.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                            replaceAttendanceScopeLocally(
                                targetDate = today,
                                jurusan = null,
                                kelas = null,
                                newRecords = loaded
                            )
                            onComplete?.invoke(_attendanceRecords.value)
                        }
                        .addOnFailureListener { innerErr ->
                            _isLoadingRemote.value = false
                            Log.w(TAG, "Fallback today attendance query failed: ${innerErr.message}")
                            onComplete?.invoke(_attendanceRecords.value)
                        }
                } else {
                    _isLoadingRemote.value = false
                    onComplete?.invoke(_attendanceRecords.value)
                }
            }
    }

    /**
     * 3. Jalur Terpisah untuk Rekap / Laporan 7 Hari (Sekali Ambil / .get(), BUKAN addSnapshotListener):
     * Menggunakan filter whereGreaterThanOrEqualTo("dateString", sevenDaysAgoDateString)
     * disertai filter kelas / jurusan bila tersedia (dengan fallback otomatis ke filter tanggal)
     * dan hanya dipanggil saat halaman/tab rekap dibuka atau tombol refresh ditekan.
     */
    fun fetchSevenDayAttendanceRecap(
        daysCount: Int = 7,
        forceRefresh: Boolean = false,
        jurusan: Jurusan? = null,
        kelas: String? = null,
        onComplete: ((List<AttendanceRecord>) -> Unit)? = null
    ) {
        val db = firestore ?: run {
            onComplete?.invoke(_attendanceRecords.value)
            return
        }

        val cleanKelas = kelas?.trim()?.takeIf { it.isNotEmpty() }
        val sevenDaysAgoDateString = getDateStringDaysAgo((daysCount - 1).coerceAtLeast(1))
        val fetchKey = "$sevenDaysAgoDateString|$daysCount|${jurusan?.name ?: "ALL"}|${cleanKelas ?: "ALL"}"
        val dateRangePrefix = "$sevenDaysAgoDateString|$daysCount|"
        val now = System.currentTimeMillis()
        val isCacheFresh = (now - lastSevenDayRecapMillis) < RECAP_CACHE_COOLDOWN_MS
        val isCoveredByCache = isCacheFresh && (
            lastSevenDayRecapKey == fetchKey ||
                (lastSevenDayRecapCoversAllScopes && lastSevenDayRecapKey?.startsWith(dateRangePrefix) == true)
            )
        if (!forceRefresh && isCoveredByCache) {
            onComplete?.invoke(_sevenDayRecapRecords.value.ifEmpty { _attendanceRecords.value })
            return
        }

        fun applyRecapResults(loaded: List<AttendanceRecord>, isFromServer: Boolean, scopedByFilter: Boolean) {
            _isLoadingRemote.value = false
            lastSevenDayRecapKey = if (scopedByFilter) fetchKey else "${dateRangePrefix}ALL|ALL"
            lastSevenDayRecapCoversAllScopes = !scopedByFilter
            lastSevenDayRecapMillis = System.currentTimeMillis()
            _sevenDayRecapRecords.value = loaded
            if (loaded.isNotEmpty() || isFromServer) {
                val preserved = _attendanceRecords.value.filterNot { rec ->
                    rec.dateString >= sevenDaysAgoDateString &&
                        (!scopedByFilter || (jurusan == null || rec.jurusan == jurusan)) &&
                        (!scopedByFilter || (cleanKelas == null || rec.kelas.equals(cleanKelas, ignoreCase = true)))
                }
                _attendanceRecords.value = mergeAttendanceById(preserved, loaded)
            }
            onComplete?.invoke(loaded)
        }

        var query: Query = db.collection("attendance_records")
            .whereGreaterThanOrEqualTo("dateString", sevenDaysAgoDateString)
        val isScoped = cleanKelas != null || jurusan != null
        if (cleanKelas != null) {
            query = query.whereEqualTo("kelas", cleanKelas)
        } else if (jurusan != null) {
            query = query.whereEqualTo("jurusan", jurusan.name)
        }

        _isLoadingRemote.value = true
        query.get()
            .addOnSuccessListener { snapshot ->
                val loaded = snapshot.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                applyRecapResults(loaded, isFromServer = !snapshot.metadata.isFromCache, scopedByFilter = isScoped)
            }
            .addOnFailureListener { error ->
                if (isScoped) {
                    Log.w(TAG, "Scoped 7-day recap failed ($fetchKey), falling back to date range query: ${error.message}")
                    db.collection("attendance_records")
                        .whereGreaterThanOrEqualTo("dateString", sevenDaysAgoDateString)
                        .get()
                        .addOnSuccessListener { fallbackSnap ->
                            val loaded = fallbackSnap.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                            applyRecapResults(loaded, isFromServer = !fallbackSnap.metadata.isFromCache, scopedByFilter = false)
                        }
                        .addOnFailureListener { innerErr ->
                            _isLoadingRemote.value = false
                            Log.w(TAG, "Fallback 7-day recap failed: ${innerErr.message}")
                            onComplete?.invoke(_attendanceRecords.value)
                        }
                } else {
                    _isLoadingRemote.value = false
                    Log.w(TAG, "fetchSevenDayAttendanceRecap failed: ${error.message}")
                    onComplete?.invoke(_attendanceRecords.value)
                }
            }
    }

    /**
     * Pengambilan sekali jalan (.get()) untuk tanggal tertentu di luar rentang 7 hari (misal dari DatePicker).
     */
    fun fetchAttendanceForSpecificDateOnce(
        dateString: String,
        jurusan: Jurusan? = null,
        kelas: String? = null,
        onComplete: ((List<AttendanceRecord>) -> Unit)? = null
    ) {
        val db = firestore ?: run {
            onComplete?.invoke(_attendanceRecords.value)
            return
        }

        val cleanKelas = kelas?.trim()?.takeIf { it.isNotEmpty() }
        var query: Query = db.collection("attendance_records")
            .whereEqualTo("dateString", dateString)
        if (cleanKelas != null) {
            query = query.whereEqualTo("kelas", cleanKelas)
        } else if (jurusan != null) {
            query = query.whereEqualTo("jurusan", jurusan.name)
        }

        _isLoadingRemote.value = true
        query.get()
            .addOnSuccessListener { snapshot ->
                _isLoadingRemote.value = false
                val loaded = snapshot.documents.mapNotNull { doc -> mapToAttendance(doc.id, doc.data) }
                replaceAttendanceScopeLocally(
                    targetDate = dateString,
                    jurusan = jurusan,
                    kelas = cleanKelas,
                    newRecords = loaded
                )
                onComplete?.invoke(loaded)
            }
            .addOnFailureListener { error ->
                _isLoadingRemote.value = false
                Log.w(TAG, "fetchAttendanceForSpecificDateOnce ($dateString) failed: ${error.message}")
                onComplete?.invoke(_attendanceRecords.value)
            }
    }

    /**
     * Mengambil data halangan syar'i yang sedang aktif sekali jalan (.get()) untuk monitoring Guru/Admin.
     */
    fun fetchActivePeriodsOnce(forceRefresh: Boolean = false) {
        val db = firestore ?: return
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastActivePeriodsFetchMillis) < CACHE_COOLDOWN_MS) {
            return
        }

        db.collection("period_records")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                lastActivePeriodsFetchMillis = System.currentTimeMillis()
                val loaded = snapshot.documents.mapNotNull { doc -> mapToPeriod(doc.id, doc.data) }
                if (loaded.isNotEmpty() || snapshot.metadata.isFromCache.not()) {
                    val inactiveCached = _periodRecords.value.filterNot { it.isActive }
                    _periodRecords.value = inactiveCached + loaded
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "fetchActivePeriodsOnce failed: ${error.message}")
            }
    }

    /**
     * 4A. Efisiensi Token QR (active_qr_tokens):
     * Hanya mendengarkan SATU dokumen sesi sholat yang sedang aktif saja
     * (active_qr_tokens/session_<prayerCode>), BUKAN seluruh koleksi.
     */
    fun observeActiveQrSession(prayerType: PrayerType = getCurrentActivePrayerSession()) {
        val db = firestore ?: return
        if (activeQrListener != null && listeningQrPrayer == prayerType) {
            return
        }

        activeQrListener?.remove()
        activeQrListener = null
        listeningQrPrayer = prayerType

        activeQrListener = db.collection("active_qr_tokens")
            .document("session_${prayerType.code}")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Active QR session listener error (${prayerType.code}): ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val today = getTodayDateString()
                    val token = mapToQrToken(snapshot.data)
                    if (token != null && token.dateString == today) {
                        syncedRemoteQrDates[token.prayerType] = today
                        _activeQrByPrayer.value = _activeQrByPrayer.value + (token.prayerType to token)
                        _currentActiveQr.value = token
                    }
                }
            }
    }

    /**
     * 4B. Pembersihan Listener:
     * Menutup (remove()) seluruh ListenerRegistration saat pengguna logout atau ViewModel dibersihkan (onCleared()).
     */
    fun clearListeners() {
        studentAttendanceListener?.remove()
        studentAttendanceListener = null

        studentPeriodListener?.remove()
        studentPeriodListener = null

        activeQrListener?.remove()
        activeQrListener = null
        listeningQrPrayer = null

        // Reset cache markers so next login performs a clean scoped fetch
        lastUsersFetchKey = null
        lastUsersFetchMillis = 0L
        lastTodayAttendanceFetchKey = null
        lastTodayAttendanceFetchMillis = 0L
        lastSevenDayRecapKey = null
        lastSevenDayRecapMillis = 0L
        lastSevenDayRecapCoversAllScopes = false
        lastActivePeriodsFetchMillis = 0L
        syncedRemoteQrDates.clear()
    }

    /**
     * Menentukan sesi sholat aktif saat ini berdasarkan jam perangkat (WITA):
     * - < 10:30 : DHUHA
     * - 10:30 - 14:29 : DZUHUR
     * - >= 14:30 : ASHAR
     */
    fun getCurrentActivePrayerSession(): PrayerType {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute
        return when {
            totalMinutes < 10 * 60 + 30 -> PrayerType.DHUHA
            totalMinutes < 14 * 60 + 30 -> PrayerType.DZUHUR
            else -> PrayerType.ASHAR
        }
    }

    private fun upsertUsersLocally(incoming: List<UserProfile>) {
        if (incoming.isEmpty()) return
        _allUsers.value = mergeUsersById(_allUsers.value, incoming)
    }

    private fun mergeUsersById(base: List<UserProfile>, incoming: List<UserProfile>): List<UserProfile> {
        val byId = LinkedHashMap<String, UserProfile>()
        base.forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    private fun mergeAttendanceById(
        base: List<AttendanceRecord>,
        incoming: List<AttendanceRecord>
    ): List<AttendanceRecord> {
        val byId = LinkedHashMap<String, AttendanceRecord>()
        base.forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    private fun replaceAttendanceScopeLocally(
        targetDate: String,
        jurusan: Jurusan?,
        kelas: String?,
        newRecords: List<AttendanceRecord>
    ) {
        val preserved = _attendanceRecords.value.filterNot { rec ->
            rec.dateString == targetDate &&
                (jurusan == null || rec.jurusan == jurusan) &&
                (kelas == null || rec.kelas.equals(kelas, ignoreCase = true))
        }
        _attendanceRecords.value = mergeAttendanceById(preserved, newRecords)
    }

    // =========================================================================
    // DATE & TIME UTILITIES
    // =========================================================================

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getDateStringDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun formatReadableDate(dateString: String): String {
        val today = getTodayDateString()
        val yesterday = getDateStringDaysAgo(1)
        val parsed = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
        val formatted = if (parsed != null) {
            SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(parsed)
        } else {
            dateString
        }
        return when (dateString) {
            today -> "Hari Ini • $formatted"
            yesterday -> "Kemarin • $formatted"
            else -> formatted
        }
    }

    fun getFormattedCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm 'WITA'", Locale.getDefault())
        return sdf.format(Date())
    }

    // =========================================================================
    // QR CODE MANAGEMENT (SINGLE ACTIVE SESSION DOCUMENT)
    // =========================================================================

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
        // Switch single-document QR listener to this active prayer session
        observeActiveQrSession(prayerType)
        val today = getTodayDateString()
        val localToken = getActiveQrFor(prayerType)
        _currentActiveQr.value = localToken

        // Hemat kuota Spark Plan: jangan menimpa dokumen QR yang sudah aktif hari ini di Firestore
        // agar tidak memicu fan-out read ke 1.500 HP siswa setiap kali guru membuka layar QR.
        if (syncedRemoteQrDates[prayerType] == today) {
            return localToken
        }

        val docRef = firestore?.collection("active_qr_tokens")?.document("session_${prayerType.code}")
        docRef?.get()
            ?.addOnSuccessListener { snapshot ->
                val remoteToken = if (snapshot.exists()) mapToQrToken(snapshot.data) else null
                if (remoteToken != null && remoteToken.dateString == today) {
                    syncedRemoteQrDates[prayerType] = today
                    _activeQrByPrayer.value = _activeQrByPrayer.value + (prayerType to remoteToken)
                    _currentActiveQr.value = remoteToken
                } else {
                    syncedRemoteQrDates[prayerType] = today
                    docRef.set(qrTokenToMap(localToken))
                        .addOnFailureListener { Log.w(TAG, "Failed to publish QR token to Firestore: ${it.message}") }
                }
            }
            ?.addOnFailureListener { Log.w(TAG, "Failed to check active QR token in Firestore: ${it.message}") }
        return localToken
    }

    fun generateNewDynamicQr(prayerType: PrayerType): DynamicQrToken {
        // Switch single-document QR listener to this active prayer session
        observeActiveQrSession(prayerType)
        val today = getTodayDateString()
        val token = createTokenForPrayer(prayerType, today)
        syncedRemoteQrDates[prayerType] = today
        _activeQrByPrayer.value = _activeQrByPrayer.value + (prayerType to token)
        _currentActiveQr.value = token

        // Write to Cloud Firestore under prayer-specific document
        firestore?.collection("active_qr_tokens")?.document("session_${prayerType.code}")?.set(qrTokenToMap(token))
            ?.addOnFailureListener { Log.w(TAG, "Failed to sync QR token to Firestore: ${it.message}") }

        return token
    }

    // =========================================================================
    // ATTENDANCE SCAN & WRITE OPERATIONS (INCLUDES studentId FOR SCOPED QUERIES)
    // =========================================================================

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

        // 1. Check 4-6 digit manual code against active prayer session token
        if (cleanInput.length in 4..6 && cleanInput.all { it.isDigit() }) {
            val matchedToken = _activeQrByPrayer.value.values.find {
                it.dateString == today && it.shortEntryCode.equals(cleanInput, ignoreCase = true)
            } ?: _currentActiveQr.value?.takeIf {
                it.dateString == today && it.shortEntryCode.equals(cleanInput, ignoreCase = true)
            }
            if (matchedToken != null) {
                return recordStudentAttendance(student, matchedToken.prayerType, today)
            }
            // If prefix points to a different prayer session, switch single-doc QR listener to fetch latest code
            val targetPrayer = when (cleanInput.firstOrNull()) {
                '1' -> PrayerType.DHUHA
                '2' -> PrayerType.DZUHUR
                '3' -> PrayerType.ASHAR
                else -> null
            }
            if (targetPrayer != null && targetPrayer != listeningQrPrayer) {
                observeActiveQrSession(targetPrayer)
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

        val deterministicDocId = "${student.id}_${today}_${prayerType.code}"
        val newRecord = AttendanceRecord(
            id = deterministicDocId,
            nisn = student.nisnOrNip,
            studentName = student.fullName,
            jurusan = student.jurusan ?: Jurusan.PPLG,
            kelas = student.kelas ?: (student.jurusan?.officialClasses?.firstOrNull() ?: "X PPLG"),
            prayerType = prayerType,
            dateString = today,
            timestampMillis = System.currentTimeMillis(),
            formattedTime = getFormattedCurrentTime(),
            isVerifiedInMosque = true,
            isHalanganSyari = false,
            dhuhaStatus = DhuhaStatus.HADIR
        )

        _attendanceRecords.value = mergeAttendanceById(_attendanceRecords.value, listOf(newRecord))

        // Push new attendance record to Firestore (includes studentId for SISWA scoped listener)
        firestore?.collection("attendance_records")
            ?.document(newRecord.id)
            ?.set(attendanceToMap(newRecord, studentId = student.id))
            ?.addOnFailureListener { Log.w(TAG, "Failed to sync attendance to Firestore: ${it.message}") }

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
            val updated = existing.copy(
                dhuhaStatus = status,
                isVerifiedInMosque = isPresent
            )
            _attendanceRecords.value = _attendanceRecords.value.map {
                if (it.id == existing.id) updated else it
            }
            firestore?.collection("attendance_records")?.document(existing.id)
                ?.update(
                    mapOf(
                        "studentId" to student.id,
                        "dhuhaStatus" to status.name,
                        "isVerifiedInMosque" to isPresent
                    )
                )
                ?.addOnFailureListener { Log.w(TAG, "Failed to update Dhuha status in Firestore: ${it.message}") }
        } else {
            val deterministicDocId = "${student.id}_${today}_${PrayerType.DHUHA.code}"
            val newRecord = AttendanceRecord(
                id = deterministicDocId,
                nisn = student.nisnOrNip,
                studentName = student.fullName,
                jurusan = student.jurusan ?: Jurusan.PPLG,
                kelas = student.kelas ?: (student.jurusan?.officialClasses?.firstOrNull() ?: "X PPLG"),
                prayerType = PrayerType.DHUHA,
                dateString = today,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = getFormattedCurrentTime(),
                isVerifiedInMosque = isPresent,
                isHalanganSyari = false,
                dhuhaStatus = status
            )
            _attendanceRecords.value = mergeAttendanceById(_attendanceRecords.value, listOf(newRecord))
            firestore?.collection("attendance_records")
                ?.document(newRecord.id)
                ?.set(attendanceToMap(newRecord, studentId = student.id))
                ?.addOnFailureListener { Log.w(TAG, "Failed to save Dhuha choice in Firestore: ${it.message}") }
        }
    }

    // =========================================================================
    // HALANGAN SYAR'I (PERIOD LOGIC FOR FEMALE STUDENTS)
    // =========================================================================

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

    fun isStudentOnPeriodOnDate(nisn: String, dateString: String): Boolean {
        if (dateString == getTodayDateString()) {
            return isStudentCurrentlyOnPeriod(nisn)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = try { sdf.parse(dateString) } catch (e: Exception) { null } ?: return false
        return _periodRecords.value.any { record ->
            if (record.nisn != nisn || record.isManuallyEnded) return@any false
            val startDate = try { sdf.parse(record.startDateString) } catch (e: Exception) { null } ?: return@any false
            val diffDays = ((targetDate.time - startDate.time) / (24L * 3600 * 1000)).toInt()
            diffDays in 0 until record.estimatedDays.coerceIn(1, 15)
        }
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
        // Deactivate previous active records for this student locally and in Firestore
        val previouslyActive = _periodRecords.value.filter { it.nisn == student.nisnOrNip && it.isActive }
        val updated = _periodRecords.value.map {
            if (it.nisn == student.nisnOrNip) it.copy(isActive = false) else it
        } + newRecord
        _periodRecords.value = updated

        // Push to Cloud Firestore
        firestore?.let { db ->
            previouslyActive.forEach { oldRec ->
                db.collection("period_records").document(oldRec.id)
                    .update("isActive", false)
                    .addOnFailureListener { Log.w(TAG, "Failed to deactivate old period in Firestore: ${it.message}") }
            }
            db.collection("period_records").document(newRecord.id).set(periodToMap(newRecord))
                .addOnFailureListener { Log.w(TAG, "Failed to save period report in Firestore: ${it.message}") }
        }
    }

    // End period (Suci / Mandi Wajib done, ready to pray)
    fun reportPeriodEnd(nisn: String) {
        val activeTargets = _periodRecords.value.filter { it.nisn == nisn && it.isActive }
        _periodRecords.value = _periodRecords.value.map {
            if (it.nisn == nisn && it.isActive) it.copy(isActive = false, isManuallyEnded = true) else it
        }
        firestore?.let { db ->
            activeTargets.forEach { target ->
                db.collection("period_records").document(target.id)
                    .update(mapOf("isActive" to false, "isManuallyEnded" to true))
                    .addOnFailureListener { Log.w(TAG, "Failed to end period in Firestore: ${it.message}") }
            }
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

    // =========================================================================
    // USER MANAGEMENT (REGISTER, ADD TEACHER, QUALIFICATION, PASSWORD, DELETE)
    // =========================================================================

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
            ?.addOnFailureListener { Log.w(TAG, "Failed to sync student to Firestore: ${it.message}") }

        return Result.success(newStudent)
    }

    fun addTeacher(
        nipOrUsername: String,
        fullName: String,
        gender: Gender,
        jurusan: Jurusan?,
        kelas: String?,
        initialPassword: String = "guru",
        jabatan: String? = null
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
        val cleanKelas = kelas?.trim()?.ifEmpty { null }
        val newTeacher = UserProfile(
            id = "guru-${UUID.randomUUID().toString().take(6)}",
            nisnOrNip = cleanNip,
            fullName = cleanName,
            role = Role.ADMIN_GURU,
            gender = gender,
            jurusan = jurusan,
            kelas = cleanKelas,
            avatarEmoji = avatar,
            password = initialPassword.trim().ifEmpty { "guru" },
            jabatan = jabatan?.trim()?.ifEmpty { null }
        )

        _allUsers.value = _allUsers.value + newTeacher

        // Sync new teacher to Cloud Firestore
        firestore?.collection("users")?.document(newTeacher.id)?.set(userToMap(newTeacher))
            ?.addOnFailureListener { Log.w(TAG, "Failed to sync teacher to Firestore: ${it.message}") }

        return Result.success(newTeacher)
    }

    fun updateTeacherQualification(
        userId: String,
        jurusan: Jurusan?,
        kelas: String?
    ): UserProfile? {
        val existing = _allUsers.value.find { it.id == userId } ?: return null
        val cleanKelas = kelas?.trim()?.ifEmpty { null }
        if (existing.jurusan == jurusan && existing.kelas == cleanKelas) {
            return existing
        }
        val updated = existing.copy(
            jurusan = jurusan,
            kelas = cleanKelas
        )
        _allUsers.value = _allUsers.value.map {
            if (it.id == userId) updated else it
        }
        firestore?.collection("users")?.document(userId)?.update(
            mapOf(
                "jurusan" to jurusan?.name,
                "kelas" to cleanKelas
            )
        )?.addOnFailureListener {
            Log.w(TAG, "Failed to update teacher qualification in Firestore: ${it.message}")
        }
        return updated
    }

    fun getKnownClassesForJurusan(jurusan: Jurusan?): List<String> {
        if (jurusan == null) return emptyList()
        val official = jurusan.officialClasses
        val extraFromStudents = allStudents
            .filter { it.jurusan == jurusan }
            .mapNotNull { it.kelas?.trim()?.takeIf { k -> k.isNotEmpty() && k !in official } }
            .distinct()
            .sorted()
        return official + extraFromStudents
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
            ?.addOnFailureListener { Log.w(TAG, "Failed to update password in Firestore: ${it.message}") }

        return true
    }

    /**
     * Admin Pusat: Hapus akun guru atau siswa
     */
    fun deleteUser(targetUserId: String): Boolean {
        val userToDelete = _allUsers.value.find { it.id == targetUserId } ?: return false

        // Lindungi akun Admin Pusat dari penghapusan tidak sengaja
        if (userToDelete.role == Role.ADMIN_PUSAT) {
            Log.w(TAG, "Akun Admin Pusat tidak dapat dihapus.")
            return false
        }

        val attendanceToDelete = if (userToDelete.role == Role.SISWA) {
            _attendanceRecords.value.filter { it.nisn == userToDelete.nisnOrNip }
        } else {
            emptyList()
        }
        val periodToDelete = if (userToDelete.role == Role.SISWA) {
            _periodRecords.value.filter { it.nisn == userToDelete.nisnOrNip }
        } else {
            emptyList()
        }

        // Hapus dari state lokal
        _allUsers.value = _allUsers.value.filter { it.id != targetUserId }

        // Jika siswa dihapus, bersihkan juga rekaman presensi dan laporan halangannya
        if (userToDelete.role == Role.SISWA) {
            _attendanceRecords.value = _attendanceRecords.value.filter { it.nisn != userToDelete.nisnOrNip }
            _periodRecords.value = _periodRecords.value.filter { it.nisn != userToDelete.nisnOrNip }
        }

        // Sinkronkan penghapusan ke Cloud Firestore (user + riwayat absensi & halangan terkait)
        firestore?.let { db ->
            db.collection("users").document(targetUserId).delete()
                .addOnFailureListener { Log.w(TAG, "Gagal menghapus user di Firestore: ${it.message}") }
            attendanceToDelete.forEach { rec ->
                db.collection("attendance_records").document(rec.id).delete()
                    .addOnFailureListener { Log.w(TAG, "Gagal menghapus absensi siswa di Firestore: ${it.message}") }
            }
            periodToDelete.forEach { rec ->
                db.collection("period_records").document(rec.id).delete()
                    .addOnFailureListener { Log.w(TAG, "Gagal menghapus data halangan siswa di Firestore: ${it.message}") }
            }
        }

        return true
    }

    /**
     * Admin Pusat: Hapus massal seluruh akun berdasarkan Role (Hanya SISWA atau ADMIN_GURU)
     */
    fun deleteBulkUsers(roleToDelete: Role): Int {
        if (roleToDelete == Role.ADMIN_PUSAT) {
            Log.w(TAG, "Penghapusan massal Admin Pusat tidak diizinkan.")
            return 0
        }

        val targets = _allUsers.value.filter { it.role == roleToDelete }
        if (targets.isEmpty()) return 0

        val targetIds = targets.map { it.id }.toSet()
        val targetNisns = targets.map { it.nisnOrNip }.toSet()
        val attendanceToDelete = if (roleToDelete == Role.SISWA) {
            _attendanceRecords.value.filter { it.nisn in targetNisns }
        } else {
            emptyList()
        }
        val periodToDelete = if (roleToDelete == Role.SISWA) {
            _periodRecords.value.filter { it.nisn in targetNisns }
        } else {
            emptyList()
        }

        // Hapus dari state lokal
        _allUsers.value = _allUsers.value.filter { it.id !in targetIds }

        // Jika menghapus seluruh siswa, bersihkan juga data presensi dan halangan syar'i terkait
        if (roleToDelete == Role.SISWA) {
            _attendanceRecords.value = _attendanceRecords.value.filter { it.nisn !in targetNisns }
            _periodRecords.value = _periodRecords.value.filter { it.nisn !in targetNisns }
        }

        // Hapus massal di Firestore menggunakan WriteBatch (maksimal 450 dokumen per batch)
        firestore?.let { db ->
            val refsToDelete = buildList {
                targets.forEach { user -> add(db.collection("users").document(user.id)) }
                attendanceToDelete.forEach { rec -> add(db.collection("attendance_records").document(rec.id)) }
                periodToDelete.forEach { rec -> add(db.collection("period_records").document(rec.id)) }
            }
            refsToDelete.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit()
                    .addOnFailureListener { Log.w(TAG, "Gagal hapus massal batch di Firestore: ${it.message}") }
            }
        }

        return targets.size
    }

    // =========================================================================
    // FIRESTORE MAPPING HELPERS
    // =========================================================================

    private fun userToMap(u: UserProfile): Map<String, Any?> = mapOf(
        "id" to u.id,
        "nisnOrNip" to u.nisnOrNip,
        "fullName" to u.fullName,
        "role" to u.role.name,
        "gender" to u.gender.name,
        "jurusan" to u.jurusan?.name,
        "kelas" to u.kelas,
        "avatarEmoji" to u.avatarEmoji,
        "password" to u.password,
        "jabatan" to u.jabatan
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
            password = data["password"] as? String ?: "123456",
            jabatan = data["jabatan"] as? String
        )
    }

    private fun attendanceToMap(a: AttendanceRecord, studentId: String? = null): Map<String, Any?> {
        val resolvedStudentId = studentId
            ?: _allUsers.value.find { it.nisnOrNip == a.nisn }?.id
            ?: a.nisn
        return mapOf(
            "id" to a.id,
            "studentId" to resolvedStudentId,
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
    }

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
