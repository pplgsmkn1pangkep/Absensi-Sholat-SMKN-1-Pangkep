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

    // Filter states for Monitoring Screen
    private val _selectedJurusanFilter = MutableStateFlow<Jurusan?>(null)
    val selectedJurusanFilter: StateFlow<Jurusan?> = _selectedJurusanFilter.asStateFlow()

    private val _selectedPrayerFilter = MutableStateFlow<PrayerType?>(null)
    val selectedPrayerFilter: StateFlow<PrayerType?> = _selectedPrayerFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Last scan feedback message & state
    private val _scanFeedback = MutableStateFlow<String?>(null)
    val scanFeedback: StateFlow<String?> = _scanFeedback.asStateFlow()

    fun login(usernameOrNisn: String, role: Role, passwordInput: String? = null): Boolean {
        val cleanInput = usernameOrNisn.trim()
        val cleanPass = passwordInput?.trim()
        if (cleanInput.isEmpty()) return false

        val found = repository.allUsers.find {
            it.role == role &&
            (it.nisnOrNip.equals(cleanInput, ignoreCase = true) || it.id.equals(cleanInput, ignoreCase = true)) &&
            (cleanPass == null || it.password == cleanPass)
        }

        return if (found != null) {
            _currentUser.value = found
            _currentScreen.value = Screen.DASHBOARD
            if (found.role == Role.ADMIN_GURU && found.jurusan != null) {
                _selectedJurusanFilter.value = found.jurusan
            } else {
                _selectedJurusanFilter.value = null
            }
            true
        } else {
            false
        }
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
            _currentUser.value = newStudent
            _currentScreen.value = Screen.DASHBOARD
            _selectedJurusanFilter.value = null
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
        initialPassword: String
    ): Result<UserProfile> {
        return repository.addTeacher(nipOrUsername, fullName, gender, jurusan, kelas, initialPassword)
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
        _currentUser.value = null
        _currentScreen.value = Screen.LOGIN
    }

    fun switchUserDirectly(user: UserProfile) {
        _currentUser.value = user
        _currentScreen.value = Screen.DASHBOARD
        if (user.role == Role.ADMIN_GURU && user.jurusan != null) {
            _selectedJurusanFilter.value = user.jurusan
        } else {
            _selectedJurusanFilter.value = null
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setJurusanFilter(jurusan: Jurusan?) {
        val user = _currentUser.value
        if (user?.role == Role.ADMIN_GURU && user.jurusan != null) {
            _selectedJurusanFilter.value = user.jurusan
        } else {
            _selectedJurusanFilter.value = jurusan
        }
    }

    fun setPrayerFilter(prayerType: PrayerType?) {
        _selectedPrayerFilter.value = prayerType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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

    // Filtered list of students for monitoring
    fun getFilteredStudents(allRecords: List<AttendanceRecord>): List<StudentAttendanceSummary> {
        val user = _currentUser.value ?: return emptyList()
        val query = _searchQuery.value.lowercase().trim()
        val prayerFilter = _selectedPrayerFilter.value
        val today = repository.getTodayDateString()

        val accessibleStudents = when (user.role) {
            Role.SISWA -> repository.allStudents.filter { it.nisnOrNip == user.nisnOrNip }
            Role.ADMIN_GURU -> {
                if (user.kelas != null) {
                    repository.allStudents.filter { it.kelas == user.kelas }
                } else if (user.jurusan != null) {
                    repository.allStudents.filter { it.jurusan == user.jurusan }
                } else {
                    val jFilter = _selectedJurusanFilter.value
                    if (jFilter != null) repository.allStudents.filter { it.jurusan == jFilter }
                    else repository.allStudents
                }
            }
            Role.ADMIN_PUSAT -> {
                val jFilter = _selectedJurusanFilter.value
                if (jFilter != null) repository.allStudents.filter { it.jurusan == jFilter }
                else repository.allStudents
            }
        }

        val searchFiltered = if (query.isEmpty()) {
            accessibleStudents
        } else {
            accessibleStudents.filter {
                it.fullName.lowercase().contains(query) ||
                it.nisnOrNip.contains(query) ||
                (it.kelas?.lowercase()?.contains(query) == true)
            }
        }

        return searchFiltered.map { student ->
            val studentRecords = allRecords.filter { it.nisn == student.nisnOrNip && it.dateString == today }
            val dhuha = studentRecords.find { it.prayerType == PrayerType.DHUHA }
            val dzuhur = studentRecords.find { it.prayerType == PrayerType.DZUHUR }
            val ashar = studentRecords.find { it.prayerType == PrayerType.ASHAR }
            val isOnPeriod = repository.isStudentCurrentlyOnPeriod(student.nisnOrNip)
            val periodRecord = repository.getActivePeriodRecord(student.nisnOrNip)

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
        }.filter { summary ->
            if (prayerFilter == null) true
            else when (prayerFilter) {
                PrayerType.DHUHA -> summary.dhuhaStatus == DhuhaStatus.HADIR
                PrayerType.DZUHUR -> summary.dzuhurRecord != null
                PrayerType.ASHAR -> summary.asharRecord != null
            }
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
