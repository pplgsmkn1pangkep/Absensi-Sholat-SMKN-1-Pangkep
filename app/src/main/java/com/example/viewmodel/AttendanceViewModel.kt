package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.AttendanceRepository
import com.example.model.AttendanceRecord
import com.example.model.DhuhaStatus
import com.example.model.Gender
import com.example.model.Jurusan
import com.example.model.PeriodRecord
import com.example.model.PrayerType
import com.example.model.Role
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AttendanceViewModel(
    val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    // Currently logged-in user (null = user starts on Login Screen for privacy)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Screen navigation state
    enum class Screen {
        LOGIN,
        DASHBOARD,
        SCAN_QR,
        QR_GENERATOR,          // For Admin Guru / Admin Pusat
        MONITORING_REKAP,      // For Admin Guru & Admin Pusat
        GATE_INSPECTION,       // Profil Gerbang / Tiket Hijau Siswa
        PERIOD_REPORT,         // Khusus Siswa Perempuan: Laporan & Notif Batas Hari Berhalangan
        ADMIN_USER_MANAGEMENT  // Khusus Admin Pusat: Tambah Akun Guru & Reset Password
    }

    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Filter states for Monitoring & Dashboard Percentage Screen
    private val _selectedJurusanFilter = MutableStateFlow<Jurusan?>(null)
    val selectedJurusanFilter: StateFlow<Jurusan?> = _selectedJurusanFilter.asStateFlow()

    private val _selectedKelasFilter = MutableStateFlow<String?>(null)
    val selectedKelasFilter: StateFlow<String?> = _selectedKelasFilter.asStateFlow()

    private val _selectedStatsDate = MutableStateFlow(repository.getTodayDateString())
    val selectedStatsDate: StateFlow<String> = _selectedStatsDate.asStateFlow()

    private val _selectedPrayerFilter = MutableStateFlow<PrayerType?>(null)
    val selectedPrayerFilter: StateFlow<PrayerType?> = _selectedPrayerFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Last scan feedback message & state
    private val _scanFeedback = MutableStateFlow<String?>(null)
    val scanFeedback: StateFlow<String?> = _scanFeedback.asStateFlow()

    // Loading state for one-shot Firestore refresh actions
    val isRefreshingData: StateFlow<Boolean> = repository.isLoadingRemote

    // Guard so 7-day recap (.get()) is only triggered once per active scope when the recap tab/screen is opened
    private var loadedSevenDayRecapScopeKey: String? = null

    fun login(
        usernameOrNisn: String,
        role: Role,
        passwordInput: String? = null,
        selectedJurusanOverride: Jurusan? = null,
        selectedKelasOverride: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null
    ): Boolean {
        val cleanInput = usernameOrNisn.trim()
        val cleanPass = passwordInput?.trim()
        if (cleanInput.isEmpty()) {
            onResult?.invoke(false, "Silakan isi NISN/Username dan Kata Sandi.")
            return false
        }

        val localMatch = repository.allUsers.find {
            it.role == role &&
                (it.nisnOrNip.equals(cleanInput, ignoreCase = true) || it.id.equals(cleanInput, ignoreCase = true)) &&
                (cleanPass == null || it.password == cleanPass)
        }

        if (localMatch != null) {
            activateUserSession(
                found = localMatch,
                selectedJurusanOverride = selectedJurusanOverride,
                selectedKelasOverride = selectedKelasOverride
            )
            onResult?.invoke(true, "Berhasil masuk! Selamat datang, ${localMatch.fullName}.")
            // Verify single user document via .whereEqualTo("nisnOrNip", username).limit(1).get()
            // so profile updates/password resets from server stay accurate with only 1 read.
            repository.fetchUserByNisnOrNip(cleanInput) { remoteUser ->
                if (remoteUser != null && remoteUser.role == role) {
                    if (cleanPass != null && remoteUser.password != cleanPass) {
                        // Password was changed remotely by Admin Pusat
                        logout()
                    } else if (_currentUser.value?.id == remoteUser.id) {
                        val syncedUser = if (remoteUser.role == Role.ADMIN_GURU && selectedJurusanOverride != null) {
                            remoteUser.copy(
                                jurusan = selectedJurusanOverride,
                                kelas = selectedKelasOverride?.trim()?.ifEmpty { null }
                            )
                        } else {
                            remoteUser
                        }
                        _currentUser.value = syncedUser
                    }
                }
            }
            return true
        }

        // Not yet in local cache (e.g., 1,500 remote students/teachers):
        // Query ONLY 1 document via .whereEqualTo("nisnOrNip", cleanInput).limit(1).get()
        repository.fetchUserByNisnOrNip(cleanInput) { remoteUser ->
            when {
                remoteUser == null -> {
                    onResult?.invoke(false, "Gagal masuk. Akun tidak ditemukan, periksa kembali NISN/Username Anda.")
                }
                remoteUser.role != role -> {
                    onResult?.invoke(false, "Gagal masuk. Akun terdaftar bukan pada jenis akun yang dipilih.")
                }
                cleanPass != null && remoteUser.password != cleanPass -> {
                    onResult?.invoke(false, "Gagal masuk. Kata sandi tidak sesuai, periksa kembali sandi Anda.")
                }
                else -> {
                    activateUserSession(
                        found = remoteUser,
                        selectedJurusanOverride = selectedJurusanOverride,
                        selectedKelasOverride = selectedKelasOverride
                    )
                    onResult?.invoke(true, "Berhasil masuk! Selamat datang, ${remoteUser.fullName}.")
                }
            }
        }
        return false
    }

    private fun activateUserSession(
        found: UserProfile,
        selectedJurusanOverride: Jurusan? = null,
        selectedKelasOverride: String? = null
    ) {
        // Clear any previous session's listeners before starting new role-scoped sync
        repository.clearListeners()
        loadedSevenDayRecapScopeKey = null

        val activeUser = if (found.role == Role.ADMIN_GURU && selectedJurusanOverride != null) {
            repository.updateTeacherQualification(
                userId = found.id,
                jurusan = selectedJurusanOverride,
                kelas = selectedKelasOverride?.trim()?.ifEmpty { null }
            ) ?: found.copy(
                jurusan = selectedJurusanOverride,
                kelas = selectedKelasOverride?.trim()?.ifEmpty { null }
            )
        } else {
            found
        }

        _currentUser.value = activeUser
        _currentScreen.value = Screen.DASHBOARD
        _selectedStatsDate.value = repository.getTodayDateString()

        if (activeUser.role == Role.ADMIN_GURU) {
            _selectedJurusanFilter.value = activeUser.jurusan
            _selectedKelasFilter.value = activeUser.effectiveKelasFilter()
        } else {
            _selectedJurusanFilter.value = null
            _selectedKelasFilter.value = null
        }

        configureFirestoreForRole(activeUser)
    }

    /**
     * Mengatur strategi koneksi Firestore hemat kuota sesuai peran pengguna:
     * - SISWA: addSnapshotListener khusus dokumen miliknya hari ini + 1 dokumen QR aktif.
     * - ADMIN_GURU / ADMIN_PUSAT: .get() sekali ambil untuk users terfilter & absensi hari ini + 1 dokumen QR aktif.
     */
    private fun configureFirestoreForRole(user: UserProfile) {
        // 4. Dengarkan HANYA 1 dokumen token QR sesi sholat yang sedang aktif
        repository.observeActiveQrSession(repository.getCurrentActivePrayerSession())

        when (user.role) {
            Role.SISWA -> {
                // 2A. Realtime listener terbatas HANYA untuk studentId milik siswa & tanggal hari ini
                repository.startStudentAttendanceListener(user)
            }
            Role.ADMIN_GURU -> {
                val targetJurusan = user.jurusan
                    ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
                val targetKelas = user.effectiveKelasFilter()
                    ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
                // Hemat kuota Spark Plan: tunda .get() sampai guru (Wali Kelas/Kaprodi/Guru BK/Guru Agama)
                // memiliki jurusan/kelas target agar tidak mengunduh data seluruh 1.500 siswa sekolah sekaligus.
                if (targetJurusan == null || (user.isWaliKelas() && targetKelas == null)) {
                    return
                }
                // 1B & 2B. Sekali ambil (.get()) khusus jurusan/kelas kualifikasi guru
                repository.fetchUsersOnce(
                    jurusan = targetJurusan,
                    kelas = targetKelas,
                    includeAllRoles = false,
                    forceRefresh = false
                )
                repository.fetchTodayAttendanceOnce(
                    jurusan = targetJurusan,
                    kelas = targetKelas,
                    forceRefresh = false
                )
                repository.fetchActivePeriodsOnce(forceRefresh = false)
            }
            Role.ADMIN_PUSAT -> {
                val targetJurusan = _selectedJurusanFilter.value
                val targetKelas = _selectedKelasFilter.value
                // 1B & 2B. Sekali ambil (.get()) untuk Admin Pusat
                repository.fetchUsersOnce(
                    jurusan = targetJurusan,
                    kelas = targetKelas,
                    includeAllRoles = true,
                    forceRefresh = false
                )
                repository.fetchTodayAttendanceOnce(
                    jurusan = targetJurusan,
                    kelas = targetKelas,
                    forceRefresh = false
                )
                repository.fetchActivePeriodsOnce(forceRefresh = false)
            }
        }
    }

    /**
     * Aksi Refresh sekali ambil (.get()) untuk Guru / Admin Pusat memantau data hari ini.
     */
    fun refreshMonitoringData(forceRefresh: Boolean = true) {
        val user = _currentUser.value ?: return
        if (user.role == Role.SISWA) return

        val targetJurusan = if (user.role == Role.ADMIN_GURU) {
            user.jurusan ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
        } else {
            _selectedJurusanFilter.value
        }
        val targetKelas = if (user.role == Role.ADMIN_GURU) {
            user.effectiveKelasFilter() ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
        } else {
            _selectedKelasFilter.value
        }

        // Untuk ADMIN_GURU: wajib terfilter minimal per jurusan (atau kelas untuk Wali Kelas)
        // agar 100 guru memantau bersamaan tidak menarik seluruh dokumen sekolah.
        if (user.role == Role.ADMIN_GURU && (targetJurusan == null || (user.isWaliKelas() && targetKelas == null))) {
            return
        }

        repository.fetchUsersOnce(
            jurusan = targetJurusan,
            kelas = targetKelas,
            includeAllRoles = (user.role == Role.ADMIN_PUSAT && targetJurusan == null && targetKelas == null),
            forceRefresh = forceRefresh
        )
        repository.fetchTodayAttendanceOnce(
            jurusan = targetJurusan,
            kelas = targetKelas,
            forceRefresh = forceRefresh
        )
        repository.fetchActivePeriodsOnce(forceRefresh = forceRefresh)
    }

    /**
     * Aksi Refresh sekali ambil (.get()) untuk daftar akun di halaman Admin Pusat / Manajemen Akun.
     */
    fun refreshUsersList(forceRefresh: Boolean = true) {
        val user = _currentUser.value ?: return
        if (user.role == Role.SISWA) return

        val includeAll = user.role == Role.ADMIN_PUSAT
        val targetJurusan = if (includeAll) null else (
            user.jurusan ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
        )
        val targetKelas = if (includeAll) null else (
            user.effectiveKelasFilter() ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
        )
        if (!includeAll && (targetJurusan == null || (user.isWaliKelas() && targetKelas == null))) {
            return
        }

        repository.fetchUsersOnce(
            jurusan = targetJurusan,
            kelas = targetKelas,
            includeAllRoles = includeAll,
            forceRefresh = forceRefresh
        )
    }

    /**
     * 3. Jalur Terpisah untuk Rekap / Laporan 7 Hari (Sekali Ambil / .get()):
     * Dipanggil saat tab/halaman rekap dibuka atau saat tombol refresh rekap ditekan.
     */
    fun loadSevenDayRecap(daysCount: Int = 7, forceRefresh: Boolean = false) {
        val user = _currentUser.value ?: return
        if (user.role == Role.SISWA) return
        val targetJurusan = if (user.role == Role.ADMIN_GURU) {
            user.jurusan ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
        } else {
            _selectedJurusanFilter.value
        }
        val targetKelas = if (user.role == Role.ADMIN_GURU) {
            user.effectiveKelasFilter() ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
        } else {
            _selectedKelasFilter.value
        }
        if (user.role == Role.ADMIN_GURU && (targetJurusan == null || (user.isWaliKelas() && targetKelas == null))) {
            return
        }
        loadedSevenDayRecapScopeKey = "$daysCount|${targetJurusan?.name ?: "ALL"}|${targetKelas ?: "ALL"}"
        repository.fetchSevenDayAttendanceRecap(
            daysCount = daysCount,
            forceRefresh = forceRefresh,
            jurusan = targetJurusan,
            kelas = targetKelas
        )
    }

    fun updateTeacherQualification(jurusan: Jurusan?, kelas: String?) {
        val user = _currentUser.value ?: return
        if (user.role != Role.ADMIN_GURU) return
        // Wali Kelas & Kaprodi yang sudah memiliki jurusan/kelas yang diampu atau dipilih di awal tidak dapat mengubah ruang lingkupnya
        if (user.isScopeLocked()) return
        val cleanKelas = kelas?.trim()?.ifEmpty { null }
        if (user.isWaliKelas() && (jurusan == null || cleanKelas == null)) return
        val updated = repository.updateTeacherQualification(user.id, jurusan, cleanKelas)
            ?: user.copy(jurusan = jurusan, kelas = cleanKelas)
        _currentUser.value = updated
        _selectedJurusanFilter.value = updated.jurusan
        _selectedKelasFilter.value = updated.effectiveKelasFilter()

        // Muat ulang data siswa & absensi hari ini sesuai kualifikasi jurusan/kelas baru via .get()
        refreshMonitoringData(forceRefresh = true)
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
        val result = repository.registerStudent(fullName, kelas, jurusan, gender, nisn, password)
        result.onSuccess { newStudent ->
            activateUserSession(newStudent)
        }
        return result
    }

    // --- ADMIN PUSAT: MANAJEMEN AKUN GURU & RESET PASSWORD ---

    fun addTeacher(
        nipOrUsername: String,
        fullName: String,
        gender: Gender,
        jurusan: Jurusan?,
        kelas: String?,
        initialPassword: String,
        jabatan: String? = null
    ): Result<UserProfile> {
        return repository.addTeacher(nipOrUsername, fullName, gender, jurusan, kelas, initialPassword, jabatan)
    }

    fun resetPassword(targetUserIdOrNisn: String, newPassword: String): Boolean {
        return repository.resetPassword(targetUserIdOrNisn, newPassword)
    }

    fun deleteUser(targetUserId: String): Boolean {
        return repository.deleteUser(targetUserId)
    }

    fun deleteBulkUsers(roleToDelete: Role): Int {
        return repository.deleteBulkUsers(roleToDelete)
    }

    fun logout() {
        // 4B. Tutup seluruh ListenerRegistration saat pengguna logout
        repository.clearListeners()
        loadedSevenDayRecapScopeKey = null
        _currentUser.value = null
        _currentScreen.value = Screen.LOGIN
        _selectedJurusanFilter.value = null
        _selectedKelasFilter.value = null
    }

    override fun onCleared() {
        // 4B. Tutup seluruh ListenerRegistration saat ViewModel dibersihkan
        repository.clearListeners()
        super.onCleared()
    }

    fun switchUserDirectly(user: UserProfile) {
        activateUserSession(user)
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        val user = _currentUser.value ?: return
        when (screen) {
            Screen.MONITORING_REKAP -> {
                if (user.role != Role.SISWA) {
                    refreshMonitoringData(forceRefresh = false)
                }
            }
            Screen.ADMIN_USER_MANAGEMENT -> {
                if (user.role != Role.SISWA) {
                    refreshUsersList(forceRefresh = false)
                }
            }
            Screen.SCAN_QR -> {
                repository.observeActiveQrSession(repository.getCurrentActivePrayerSession())
            }
            else -> Unit
        }
    }

    fun setJurusanFilter(jurusan: Jurusan?) {
        val user = _currentUser.value
        if (user?.role == Role.ADMIN_GURU && (user.jurusan != null || user.isRestrictedToAssignedScope())) {
            _selectedJurusanFilter.value = user.jurusan
        } else {
            _selectedJurusanFilter.value = jurusan
            _selectedKelasFilter.value = null
            if (user != null && user.role != Role.SISWA) {
                refreshMonitoringData(forceRefresh = false)
            }
        }
    }

    fun setKelasFilter(kelas: String?) {
        val user = _currentUser.value
        val lockedKelas = user?.effectiveKelasFilter()
        if (user?.role == Role.ADMIN_GURU && (lockedKelas != null || user.isWaliKelas())) {
            _selectedKelasFilter.value = lockedKelas
        } else {
            _selectedKelasFilter.value = kelas?.trim()?.ifEmpty { null }
            if (user != null && user.role != Role.SISWA) {
                refreshMonitoringData(forceRefresh = false)
            }
        }
    }

    fun setSelectedStatsDate(dateString: String) {
        _selectedStatsDate.value = dateString
        val user = _currentUser.value ?: return
        if (user.role == Role.SISWA) return

        val today = repository.getTodayDateString()
        val sevenDaysAgo = repository.getDateStringDaysAgo(6)
        when {
            dateString == today -> refreshMonitoringData(forceRefresh = false)
            dateString >= sevenDaysAgo -> loadSevenDayRecap(daysCount = 7, forceRefresh = false)
            else -> {
                val targetJurusan = if (user.role == Role.ADMIN_GURU) {
                    user.jurusan ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
                } else {
                    _selectedJurusanFilter.value
                }
                val targetKelas = if (user.role == Role.ADMIN_GURU) {
                    user.effectiveKelasFilter() ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
                } else {
                    _selectedKelasFilter.value
                }
                if (user.role == Role.ADMIN_GURU && (targetJurusan == null || (user.isWaliKelas() && targetKelas == null))) {
                    return
                }
                repository.fetchAttendanceForSpecificDateOnce(
                    dateString = dateString,
                    jurusan = targetJurusan,
                    kelas = targetKelas
                )
            }
        }
    }

    fun setPrayerFilter(prayerType: PrayerType?) {
        _selectedPrayerFilter.value = prayerType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun processAttendanceInput(code: String) = processAttendanceCode(code)

    fun processAttendanceCode(code: String): AttendanceRepository.ScanResult {
        val user = _currentUser.value ?: return AttendanceRepository.ScanResult.InvalidQr("Pengguna belum login.")
        val result = repository.processAttendanceInput(code, user)
        when (result) {
            is AttendanceRepository.ScanResult.Success -> {
                val statusMsg = if (result.isGoHomeActivated) {
                    " 🌟 TIKET HIJAU IZIN PULANG AKTIF (Dzuhur & Ashar Lengkap)!"
                } else if (result.prayerType == PrayerType.DZUHUR && !result.isAsharCompleted) {
                    " Tinggal Sholat Ashar untuk mengaktifkan Tiket Hijau."
                } else if (result.prayerType == PrayerType.ASHAR && !result.isDzuhurCompleted) {
                    " Sholat Dzuhur belum tercatat (wajib Dzuhur & Ashar untuk izin pulang)."
                } else {
                    ""
                }
                _scanFeedback.value = "Alhamdulillah! Absensi ${result.prayerType.prayerName} Berhasil.$statusMsg"
            }
            is AttendanceRepository.ScanResult.AlreadyScanned -> {
                _scanFeedback.value = "Anda sudah tercatat sholat ${result.prayerType.prayerName} pada ${result.timeScanned}."
            }
            is AttendanceRepository.ScanResult.InvalidQr -> {
                _scanFeedback.value = "Gagal: ${result.reason}"
            }
        }
        return result
    }

    fun submitDhuhaChoice(isPresent: Boolean) {
        val user = _currentUser.value ?: return
        repository.setDhuhaChoice(user, isPresent)
    }

    // --- HALANGAN SYAR'I ACTIONS ---
    fun reportPeriodStart(days: Int, notes: String = "") {
        val user = _currentUser.value ?: return
        repository.reportPeriodStart(user, days, notes)
    }

    fun reportPeriodEnd() {
        val user = _currentUser.value ?: return
        repository.reportPeriodEnd(user.nisnOrNip)
    }

    fun getPeriodStatus(): AttendanceRepository.PeriodNotificationStatus {
        val user = _currentUser.value ?: return AttendanceRepository.PeriodNotificationStatus(false, 0, 0, null)
        return repository.checkPeriodStatus(user.nisnOrNip)
    }

    fun isUserOnPeriod(): Boolean {
        val user = _currentUser.value ?: return false
        return repository.isStudentCurrentlyOnPeriod(user.nisnOrNip)
    }

    fun clearScanFeedback() {
        _scanFeedback.value = null
    }

    fun generateNewQr(prayerType: PrayerType) {
        repository.generateNewDynamicQr(prayerType)
    }

    fun ensurePrayerQrPublished(prayerType: PrayerType) {
        repository.ensurePrayerQrPublished(prayerType)
    }

    fun getQualifiedStudentsForCurrentUser(): List<UserProfile> {
        val user = _currentUser.value ?: return emptyList()
        return when (user.role) {
            Role.SISWA -> repository.allStudents.filter { it.nisnOrNip == user.nisnOrNip }
            Role.ADMIN_GURU -> {
                val qualifiedJurusan = user.jurusan
                    ?: if (user.isRestrictedToAssignedScope()) return emptyList()
                    else (_selectedJurusanFilter.value ?: return emptyList())
                val majorStudents = repository.allStudents.filter { it.jurusan == qualifiedJurusan }
                val lockedKelas = user.effectiveKelasFilter()
                if (user.isWaliKelas()) {
                    val waliKelas = lockedKelas ?: return emptyList()
                    majorStudents.filter { it.kelas.equals(waliKelas, ignoreCase = true) }
                } else {
                    val qualifiedKelas = lockedKelas ?: _selectedKelasFilter.value
                    if (qualifiedKelas != null) {
                        majorStudents.filter { it.kelas.equals(qualifiedKelas, ignoreCase = true) }
                    } else {
                        majorStudents
                    }
                }
            }
            Role.ADMIN_PUSAT -> {
                val jFilter = _selectedJurusanFilter.value
                val majorStudents = if (jFilter != null) {
                    repository.allStudents.filter { it.jurusan == jFilter }
                } else {
                    repository.allStudents
                }
                val kFilter = _selectedKelasFilter.value
                if (kFilter != null) {
                    majorStudents.filter { it.kelas.equals(kFilter, ignoreCase = true) }
                } else {
                    majorStudents
                }
            }
        }
    }

    private fun buildSummaries(
        students: List<UserProfile>,
        allRecords: List<AttendanceRecord>,
        dateString: String
    ): List<StudentAttendanceSummary> {
        return students.map { student ->
            val studentRecords = allRecords.filter { it.nisn == student.nisnOrNip && it.dateString == dateString }
            val dhuha = studentRecords.find { it.prayerType == PrayerType.DHUHA }
            val dzuhur = studentRecords.find { it.prayerType == PrayerType.DZUHUR }
            val ashar = studentRecords.find { it.prayerType == PrayerType.ASHAR }
            val isOnPeriod = repository.isStudentOnPeriodOnDate(student.nisnOrNip, dateString)
            val periodRecord = if (isOnPeriod) repository.getActivePeriodRecord(student.nisnOrNip) else null

            // If on halangan, they are excused; otherwise BOTH Dzuhur and Ashar are mandatory to go home
            val canGoHome = isOnPeriod || (dzuhur != null && ashar != null)

            val dhuhaStatus = when {
                dhuha == null -> DhuhaStatus.BELUM_MEMILIH
                dhuha.dhuhaStatus == DhuhaStatus.HADIR -> DhuhaStatus.HADIR
                else -> DhuhaStatus.TIDAK_HADIR
            }

            StudentAttendanceSummary(
                student = student,
                dhuhaRecord = dhuha,
                dzuhurRecord = dzuhur,
                asharRecord = ashar,
                dhuhaStatus = dhuhaStatus,
                isEligibleToGoHome = canGoHome,
                isCurrentlyOnPeriod = isOnPeriod,
                periodRecord = periodRecord
            )
        }
    }

    // Filtered list of students for monitoring (supports today & past dates)
    fun getFilteredStudents(
        allRecords: List<AttendanceRecord>,
        dateString: String = repository.getTodayDateString()
    ): List<StudentAttendanceSummary> {
        val query = _searchQuery.value.lowercase().trim()
        val prayerFilter = _selectedPrayerFilter.value
        val accessibleStudents = getQualifiedStudentsForCurrentUser()

        val searchFiltered = if (query.isEmpty()) {
            accessibleStudents
        } else {
            accessibleStudents.filter {
                it.fullName.lowercase().contains(query) ||
                    it.nisnOrNip.contains(query) ||
                    (it.kelas?.lowercase()?.contains(query) == true)
            }
        }

        return buildSummaries(searchFiltered, allRecords, dateString).filter { summary ->
            if (prayerFilter == null) true
            else when (prayerFilter) {
                PrayerType.DHUHA -> summary.dhuhaStatus == DhuhaStatus.HADIR
                PrayerType.DZUHUR -> summary.dzuhurRecord != null
                PrayerType.ASHAR -> summary.asharRecord != null
            }
        }
    }

    fun calculatePrayerStatsForDate(
        allRecords: List<AttendanceRecord>,
        dateString: String
    ): PrayerPercentageStats {
        val students = getQualifiedStudentsForCurrentUser()
        val summaries = buildSummaries(students, allRecords, dateString)
        val total = summaries.size

        if (total == 0) {
            return PrayerPercentageStats(
                dateString = dateString,
                totalStudents = 0,
                dhuhaPresentCount = 0,
                dhuhaPercentage = 0,
                dzuhurPresentCount = 0,
                dzuhurPercentage = 0,
                asharPresentCount = 0,
                asharPercentage = 0,
                goHomeEligibleCount = 0,
                goHomePercentage = 0,
                halanganCount = 0,
                classBreakdown = emptyList(),
                studentSummaries = emptyList()
            )
        }

        val dhuhaCount = summaries.count { it.dhuhaStatus == DhuhaStatus.HADIR }
        val dzuhurCount = summaries.count { it.dzuhurRecord != null }
        val asharCount = summaries.count { it.asharRecord != null }
        val eligibleCount = summaries.count { it.isEligibleToGoHome }
        val halanganCount = summaries.count { it.isCurrentlyOnPeriod }

        val classBreakdown = summaries
            .groupBy { it.student.kelas ?: (it.student.jurusan?.code ?: "Umum") }
            .entries
            .sortedBy { it.key }
            .map { (className, classStudents) ->
                val classTotal = classStudents.size
                val cDhuha = classStudents.count { it.dhuhaStatus == DhuhaStatus.HADIR }
                val cDzuhur = classStudents.count { it.dzuhurRecord != null }
                val cAshar = classStudents.count { it.asharRecord != null }
                val cEligible = classStudents.count { it.isEligibleToGoHome }
                ClassAttendanceStats(
                    className = className,
                    totalStudents = classTotal,
                    dhuhaPercentage = (cDhuha * 100) / classTotal,
                    dzuhurPercentage = (cDzuhur * 100) / classTotal,
                    asharPercentage = (cAshar * 100) / classTotal,
                    goHomePercentage = (cEligible * 100) / classTotal,
                    goHomeCount = cEligible
                )
            }

        return PrayerPercentageStats(
            dateString = dateString,
            totalStudents = total,
            dhuhaPresentCount = dhuhaCount,
            dhuhaPercentage = (dhuhaCount * 100) / total,
            dzuhurPresentCount = dzuhurCount,
            dzuhurPercentage = (dzuhurCount * 100) / total,
            asharPresentCount = asharCount,
            asharPercentage = (asharCount * 100) / total,
            goHomeEligibleCount = eligibleCount,
            goHomePercentage = (eligibleCount * 100) / total,
            halanganCount = halanganCount,
            classBreakdown = classBreakdown,
            studentSummaries = summaries
        )
    }

    fun calculateRecentDaysTrend(
        allRecords: List<AttendanceRecord>,
        daysCount: Int = 7
    ): List<DailyPrayerTrend> {
        // Trigger one-shot 7-day recap (.get()) lazily once per active scope when the recap tab is opened
        val user = _currentUser.value
        if (user != null && user.role != Role.SISWA) {
            val targetJurusan = if (user.role == Role.ADMIN_GURU) {
                user.jurusan ?: if (user.isRestrictedToAssignedScope()) null else _selectedJurusanFilter.value
            } else {
                _selectedJurusanFilter.value
            }
            val targetKelas = if (user.role == Role.ADMIN_GURU) {
                user.effectiveKelasFilter() ?: if (user.isWaliKelas()) null else _selectedKelasFilter.value
            } else {
                _selectedKelasFilter.value
            }
            val desiredScopeKey = "$daysCount|${targetJurusan?.name ?: "ALL"}|${targetKelas ?: "ALL"}"
            if (loadedSevenDayRecapScopeKey != desiredScopeKey) {
                loadSevenDayRecap(daysCount = daysCount, forceRefresh = false)
            }
        }

        return (0 until daysCount).map { daysAgo ->
            val dateStr = repository.getDateStringDaysAgo(daysAgo)
            val stats = calculatePrayerStatsForDate(allRecords, dateStr)
            val shortLabel = when (daysAgo) {
                0 -> "Hari Ini"
                1 -> "Kemarin"
                else -> "$daysAgo Hari Lalu"
            }
            DailyPrayerTrend(
                dateString = dateStr,
                label = shortLabel,
                readableDate = repository.formatReadableDate(dateStr),
                totalStudents = stats.totalStudents,
                dhuhaPercentage = stats.dhuhaPercentage,
                dzuhurPercentage = stats.dzuhurPercentage,
                asharPercentage = stats.asharPercentage,
                goHomePercentage = stats.goHomePercentage
            )
        }
    }
}

data class StudentAttendanceSummary(
    val student: UserProfile,
    val dhuhaRecord: AttendanceRecord?,
    val dzuhurRecord: AttendanceRecord?,
    val asharRecord: AttendanceRecord?,
    val dhuhaStatus: DhuhaStatus,
    val isEligibleToGoHome: Boolean,
    val isCurrentlyOnPeriod: Boolean = false,
    val periodRecord: PeriodRecord? = null
)

data class ClassAttendanceStats(
    val className: String,
    val totalStudents: Int,
    val dhuhaPercentage: Int,
    val dzuhurPercentage: Int,
    val asharPercentage: Int,
    val goHomePercentage: Int,
    val goHomeCount: Int
)

data class PrayerPercentageStats(
    val dateString: String,
    val totalStudents: Int,
    val dhuhaPresentCount: Int,
    val dhuhaPercentage: Int,
    val dzuhurPresentCount: Int,
    val dzuhurPercentage: Int,
    val asharPresentCount: Int,
    val asharPercentage: Int,
    val goHomeEligibleCount: Int,
    val goHomePercentage: Int,
    val halanganCount: Int,
    val classBreakdown: List<ClassAttendanceStats>,
    val studentSummaries: List<StudentAttendanceSummary>
)

data class DailyPrayerTrend(
    val dateString: String,
    val label: String,
    val readableDate: String,
    val totalStudents: Int,
    val dhuhaPercentage: Int,
    val dzuhurPercentage: Int,
    val asharPercentage: Int,
    val goHomePercentage: Int
)
