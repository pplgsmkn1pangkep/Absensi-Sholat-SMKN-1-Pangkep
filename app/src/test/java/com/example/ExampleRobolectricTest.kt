package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceRepository
import com.example.model.PrayerType
import com.example.model.Role
import com.example.model.UserProfile
import com.example.viewmodel.AttendanceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Absensi Sholat SMKN 1", appName)
    }

    @Test
    fun `verify viewmodel starts on login screen with null user and validates credentials`() {
        val viewModel = AttendanceViewModel()

        // Must start on LOGIN screen with no user pre-logged in
        assertNull(viewModel.currentUser.value)
        assertEquals(AttendanceViewModel.Screen.LOGIN, viewModel.currentScreen.value)

        // Wrong password must fail
        assertFalse(viewModel.login("0061234567", Role.SISWA, "salah"))
        assertNull(viewModel.currentUser.value)

        // Unknown user must fail
        assertFalse(viewModel.login("tidak.ada", Role.SISWA, "123456"))
        assertNull(viewModel.currentUser.value)

        // Valid credentials must succeed
        assertTrue(viewModel.login("0061234567", Role.SISWA, "123456"))
        assertEquals("0061234567", viewModel.currentUser.value?.nisnOrNip)
        assertEquals(AttendanceViewModel.Screen.DASHBOARD, viewModel.currentScreen.value)
    }

    @Test
    fun `verify dzuhur and ashar together activate go-home permit and green ticket`() {
        val repository = AttendanceRepository()
        val student = UserProfile(
            id = "test-s-01",
            nisnOrNip = "999888777",
            fullName = "Fadhil Test",
            role = Role.SISWA
        )

        val today = repository.getTodayDateString()
        val dzuhurQrToken = "SMKN1PKP|SHOLAT|dzuhur|$today|111222"
        val asharQrToken = "SMKN1PKP|SHOLAT|ashar|$today|123456"

        assertFalse(repository.isGoHomeEligibleToday(student.nisnOrNip))

        // 1. Scan Dzuhur only -> recorded, but go-home ticket is NOT active yet
        val dzuhurResult = repository.processAttendanceInput(dzuhurQrToken, student)
        assertTrue(dzuhurResult is AttendanceRepository.ScanResult.Success)
        val dzuhurSuccess = dzuhurResult as AttendanceRepository.ScanResult.Success
        assertEquals(PrayerType.DZUHUR, dzuhurSuccess.prayerType)
        assertTrue(dzuhurSuccess.isDzuhurCompleted)
        assertFalse(dzuhurSuccess.isAsharCompleted)
        assertFalse(dzuhurSuccess.isGoHomeActivated)
        assertFalse(repository.isGoHomeEligibleToday(student.nisnOrNip))

        // 2. Scan Ashar -> now BOTH Dzuhur & Ashar complete -> go-home ticket activated!
        val asharResult = repository.processAttendanceInput(asharQrToken, student)
        assertTrue(asharResult is AttendanceRepository.ScanResult.Success)
        val asharSuccess = asharResult as AttendanceRepository.ScanResult.Success
        assertEquals(PrayerType.ASHAR, asharSuccess.prayerType)
        assertTrue(asharSuccess.isDzuhurCompleted)
        assertTrue(asharSuccess.isAsharCompleted)
        assertTrue(asharSuccess.isGoHomeActivated)
        assertTrue(repository.isGoHomeEligibleToday(student.nisnOrNip))
    }

    @Test
    fun `verify manual short code for dzuhur verifies dzuhur even when ashar session also exists`() {
        val repository = AttendanceRepository()
        val student = UserProfile(
            id = "test-s-02",
            nisnOrNip = "555444333",
            fullName = "Short Code Test Student",
            role = Role.SISWA
        )

        val dzuhurToken = repository.generateNewDynamicQr(PrayerType.DZUHUR)
        val asharToken = repository.generateNewDynamicQr(PrayerType.ASHAR)

        // Ensure codes are distinct
        assertFalse(dzuhurToken.shortEntryCode == asharToken.shortEntryCode)

        // Student types Dzuhur manual code -> MUST verify DZUHUR (not ASHAR!)
        val dzuhurResult = repository.processAttendanceInput(dzuhurToken.shortEntryCode, student)
        assertTrue(dzuhurResult is AttendanceRepository.ScanResult.Success)
        assertEquals(PrayerType.DZUHUR, (dzuhurResult as AttendanceRepository.ScanResult.Success).prayerType)
        assertFalse(dzuhurResult.isGoHomeActivated)

        // Student types Ashar manual code -> MUST verify ASHAR and unlock go-home ticket!
        val asharResult = repository.processAttendanceInput(asharToken.shortEntryCode, student)
        assertTrue(asharResult is AttendanceRepository.ScanResult.Success)
        assertEquals(PrayerType.ASHAR, (asharResult as AttendanceRepository.ScanResult.Success).prayerType)
        assertTrue(asharResult.isGoHomeActivated)
    }

    @Test
    fun `verify expired qr or wrong date is rejected for fraud prevention`() {
        val repository = AttendanceRepository()
        val student = UserProfile(
            id = "test-s-03",
            nisnOrNip = "111222333",
            fullName = "Fraud Test Student",
            role = Role.SISWA
        )

        val yesterdayQr = "SMKN1PKP|SHOLAT|ashar|2024-01-01|999999"
        val result = repository.processAttendanceInput(yesterdayQr, student)

        assertTrue(result is AttendanceRepository.ScanResult.InvalidQr)
    }

    @Test
    fun `verify female student period report grants automatic go-home permit and triggers notification on expiration`() {
        val repository = AttendanceRepository()
        val femaleStudent = UserProfile(
            id = "test-s-female",
            nisnOrNip = "0067654399",
            fullName = "Fatimah Zahra",
            role = Role.SISWA,
            gender = com.example.model.Gender.PEREMPUAN
        )

        // Initial: Not on period
        assertFalse(repository.isStudentCurrentlyOnPeriod(femaleStudent.nisnOrNip))

        // Report period for 5 days
        repository.reportPeriodStart(femaleStudent, 5, "Mulai hari ini")
        assertTrue(repository.isStudentCurrentlyOnPeriod(femaleStudent.nisnOrNip))

        // On period: automatically eligible to go home without scanning
        assertTrue(repository.isGoHomeEligibleToday(femaleStudent.nisnOrNip))

        // End period (bersuci / mandi wajib)
        repository.reportPeriodEnd(femaleStudent.nisnOrNip)
        assertFalse(repository.isStudentCurrentlyOnPeriod(femaleStudent.nisnOrNip))
    }

    @Test
    fun `verify admin pusat can add new teacher account successfully`() {
        val repository = AttendanceRepository()
        val initialCount = repository.allUsers.count { it.role == Role.ADMIN_GURU }

        val result = repository.addTeacher(
            nipOrUsername = "guru.syarif",
            fullName = "Ust. M. Syarif, S.Pd.I",
            gender = com.example.model.Gender.LAKI_LAKI,
            jurusan = com.example.model.Jurusan.PPLG,
            kelas = "Wali Kelas XI PPLG 1",
            initialPassword = "guru"
        )

        assertTrue(result.isSuccess)
        val newTeacher = result.getOrNull()
        org.junit.Assert.assertNotNull(newTeacher)
        assertEquals("guru.syarif", newTeacher?.nisnOrNip)
        assertEquals("Ust. M. Syarif, S.Pd.I", newTeacher?.fullName)
        assertEquals("guru", newTeacher?.password)
        assertEquals(initialCount + 1, repository.allUsers.count { it.role == Role.ADMIN_GURU })

        // Check duplicate rejection
        val duplicateResult = repository.addTeacher(
            nipOrUsername = "guru.syarif",
            fullName = "Ust. Duplicate",
            gender = com.example.model.Gender.LAKI_LAKI,
            jurusan = null,
            kelas = null
        )
        assertTrue(duplicateResult.isFailure)
    }

    @Test
    fun `verify admin pusat can reset password for teacher and student`() {
        val repository = AttendanceRepository()

        // 1. Reset teacher password
        val teacher = repository.allUsers.first { it.role == Role.ADMIN_GURU }
        val resetTeacherOk = repository.resetPassword(teacher.id, "guruBaru2026")
        assertTrue(resetTeacherOk)
        val updatedTeacher = repository.allUsers.first { it.id == teacher.id }
        assertEquals("guruBaru2026", updatedTeacher.password)

        // 2. Reset student password
        val student = repository.allStudents.first()
        val resetStudentOk = repository.resetPassword(student.id, "siswaBaru123")
        assertTrue(resetStudentOk)
        val updatedStudent = repository.allStudents.first { it.id == student.id }
        assertEquals("siswaBaru123", updatedStudent.password)
    }

    @Test
    fun `verify student self-registration creates account and allows login`() {
        val repository = AttendanceRepository()
        val initialStudentCount = repository.allStudents.size

        val regResult = repository.registerStudent(
            fullName = "Anugrah Pratama",
            kelas = "X PPLG 1",
            jurusan = com.example.model.Jurusan.PPLG,
            gender = com.example.model.Gender.LAKI_LAKI,
            nisn = "0069911223",
            password = "password123"
        )

        assertTrue(regResult.isSuccess)
        val newStudent = regResult.getOrNull()
        org.junit.Assert.assertNotNull(newStudent)
        assertEquals("0069911223", newStudent?.nisnOrNip)
        assertEquals("Anugrah Pratama", newStudent?.fullName)
        assertEquals("X PPLG 1", newStudent?.kelas)
        assertEquals(com.example.model.Jurusan.PPLG, newStudent?.jurusan)
        assertEquals("password123", newStudent?.password)
        assertEquals(initialStudentCount + 1, repository.allStudents.size)

        // Duplicate rejection test
        val dupResult = repository.registerStudent(
            fullName = "Anugrah Kloning",
            kelas = "X PPLG 2",
            jurusan = com.example.model.Jurusan.PPLG,
            gender = com.example.model.Gender.LAKI_LAKI,
            nisn = "0069911223",
            password = "other"
        )
        assertTrue(dupResult.isFailure)
    }

    @Test
    fun `verify admin pusat can delete teacher and student accounts`() {
        val repository = AttendanceRepository()

        // 1. Delete a teacher
        val teacher = repository.allUsers.first { it.role == Role.ADMIN_GURU }
        val deleteTeacherOk = repository.deleteUser(teacher.id)
        assertTrue(deleteTeacherOk)
        assertFalse(repository.allUsers.any { it.id == teacher.id })

        // 2. Delete a student
        val student = repository.allStudents.first()
        val deleteStudentOk = repository.deleteUser(student.id)
        assertTrue(deleteStudentOk)
        assertFalse(repository.allUsers.any { it.id == student.id })

        // 3. Admin Pusat cannot be deleted (protection)
        val admin = repository.allUsers.first { it.role == Role.ADMIN_PUSAT }
        val deleteAdminOk = repository.deleteUser(admin.id)
        assertFalse(deleteAdminOk)
        assertTrue(repository.allUsers.any { it.id == admin.id })
    }

    @Test
    fun `verify admin pusat can bulk delete all students`() {
        val repository = AttendanceRepository()
        val initialStudentsCount = repository.allStudents.size
        assertTrue(initialStudentsCount > 0)

        val deletedCount = repository.deleteBulkUsers(Role.SISWA)
        assertEquals(initialStudentsCount, deletedCount)
        assertEquals(0, repository.allStudents.size)

        // Admin Pusat and teachers still remain
        assertTrue(repository.allUsers.any { it.role == Role.ADMIN_PUSAT })
        assertTrue(repository.allUsers.any { it.role == Role.ADMIN_GURU })
    }
}
