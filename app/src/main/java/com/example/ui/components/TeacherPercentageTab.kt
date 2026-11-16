package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DhuhaStatus
import com.example.model.Jurusan
import com.example.model.Role
import com.example.model.UserProfile
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPercentageTab(
    viewModel: AttendanceViewModel,
    currentUser: UserProfile,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val allUsers by viewModel.repository.users.collectAsState()
    val selectedDate by viewModel.selectedStatsDate.collectAsState()
    val selectedJurusanFilter by viewModel.selectedJurusanFilter.collectAsState()
    val selectedKelasFilter by viewModel.selectedKelasFilter.collectAsState()

    val today = viewModel.repository.getTodayDateString()
    val yesterday = viewModel.repository.getDateStringDaysAgo(1)
    val twoDaysAgo = viewModel.repository.getDateStringDaysAgo(2)
    val threeDaysAgo = viewModel.repository.getDateStringDaysAgo(3)

    // Recompute stats whenever records, users, date, or filters change
    val stats = remember(allRecords, allUsers, selectedDate, selectedJurusanFilter, selectedKelasFilter, currentUser) {
        viewModel.calculatePrayerStatsForDate(allRecords, selectedDate)
    }
    val recentTrend = remember(allRecords, allUsers, selectedJurusanFilter, selectedKelasFilter, currentUser) {
        viewModel.calculateRecentDaysTrend(allRecords, daysCount = 7)
    }

    var isEditingQualification by remember(currentUser.id) {
        mutableStateOf(currentUser.role == Role.ADMIN_GURU && currentUser.jurusan == null)
    }
    var draftJurusan by remember(currentUser.jurusan) {
        mutableStateOf(currentUser.jurusan ?: Jurusan.PPLG)
    }
    var draftKelas by remember(currentUser.kelas) {
        mutableStateOf(currentUser.effectiveKelasFilter() ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. KUALIFIKASI JURUSAN & KELAS GURU / KAPRODI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("teacher_qualification_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentUser.role == Role.ADMIN_GURU && currentUser.jurusan == null) {
                    Color(0xFFFFF8E1)
                } else {
                    CreamCardBg
                }
            ),
            border = BorderStroke(
                1.5.dp,
                if (currentUser.role == Role.ADMIN_GURU && currentUser.jurusan == null) AmberAccent else CreamCardBorder
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrownContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = BrownPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val roleLabel = if (currentUser.role == Role.ADMIN_PUSAT) {
                                "Filter Pemantauan Admin Pusat"
                            } else {
                                "Kualifikasi ${currentUser.effectiveJabatanLabel()}"
                            }
                            Text(
                                text = roleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrownMedium
                            )
                            val scopeTitle = if (currentUser.role == Role.ADMIN_GURU) {
                                val j = currentUser.jurusan
                                val k = currentUser.effectiveKelasFilter() ?: selectedKelasFilter
                                if (j == null) {
                                    "Belum Memilih Jurusan"
                                } else {
                                    "Jurusan ${j.code} • ${k ?: "Semua Kelas ${j.code}"}"
                                }
                            } else {
                                val j = selectedJurusanFilter
                                val k = selectedKelasFilter
                                if (j == null) "Semua 7 Jurusan"
                                else "Jurusan ${j.code} • ${k ?: "Semua Kelas ${j.code}"}"
                            }
                            Text(
                                text = scopeTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrownDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (currentUser.role == Role.ADMIN_GURU) {
                        TextButton(
                            onClick = {
                                draftJurusan = currentUser.jurusan ?: Jurusan.PPLG
                                draftKelas = currentUser.effectiveKelasFilter() ?: ""
                                isEditingQualification = !isEditingQualification
                            },
                            modifier = Modifier.testTag("toggle_qualification_edit_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BrownPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditingQualification) "Tutup" else "Pilih Jurusan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrownPrimary
                            )
                        }
                    }
                }

                if (currentUser.role == Role.ADMIN_GURU && currentUser.jurusan != null && !isEditingQualification) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Data di bawah hanya menampilkan siswa ${currentUser.effectiveKelasFilter() ?: "jurusan ${currentUser.jurusan.code}"} (${stats.totalStudents} siswa).",
                        fontSize = 11.sp,
                        color = Color(0xFF5A4A42)
                    )
                }

                // Class quick filter for Kaprodi (who oversees all classes of 1 jurusan)
                if (currentUser.role == Role.ADMIN_GURU &&
                    currentUser.jurusan != null &&
                    currentUser.effectiveKelasFilter() == null &&
                    !isEditingQualification
                ) {
                    val availableClasses = remember(currentUser.jurusan, allUsers) {
                        viewModel.repository.allStudents
                            .filter { it.jurusan == currentUser.jurusan }
                            .mapNotNull { it.kelas }
                            .distinct()
                            .sorted()
                    }
                    if (availableClasses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedKelasFilter == null,
                                onClick = { viewModel.setKelasFilter(null) },
                                label = { Text("Semua Kelas ${currentUser.jurusan.code}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrownPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            availableClasses.forEach { kelasName ->
                                FilterChip(
                                    selected = selectedKelasFilter.equals(kelasName, ignoreCase = true),
                                    onClick = { viewModel.setKelasFilter(kelasName) },
                                    label = { Text(kelasName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrownPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Admin Pusat can filter across 7 majors
                if (currentUser.role == Role.ADMIN_PUSAT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedJurusanFilter == null,
                            onClick = { viewModel.setJurusanFilter(null) },
                            label = { Text("Semua (7 Jurusan)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrownPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        Jurusan.entries.forEach { jur ->
                            FilterChip(
                                selected = selectedJurusanFilter == jur,
                                onClick = { viewModel.setJurusanFilter(jur) },
                                label = { Text(jur.code, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrownPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Inline Jurusan & Kelas Qualification Picker for Guru / Kaprodi
                AnimatedVisibility(visible = isEditingQualification && currentUser.role == Role.ADMIN_GURU) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = CreamCardBorder)
                        Text(
                            text = "Pilih Jurusan & Kelas yang Anda Pantau:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownDark
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Jurusan.entries.forEach { jur ->
                                FilterChip(
                                    selected = draftJurusan == jur,
                                    onClick = {
                                        draftJurusan = jur
                                        draftKelas = ""
                                    },
                                    label = { Text(jur.code, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ScanGreenPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("qualify_jurusan_${jur.code}")
                                )
                            }
                        }

                        val suggestedClasses = remember(draftJurusan, allUsers) {
                            viewModel.repository.getKnownClassesForJurusan(draftJurusan)
                        }
                        Text(
                            text = "Pilih Kelas (kosongkan / 'Semua Kelas' untuk Kaprodi):",
                            fontSize = 11.sp,
                            color = Color(0xFF5A4A42)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = draftKelas.isBlank(),
                                onClick = { draftKelas = "" },
                                label = { Text("Semua Kelas ${draftJurusan.code}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrownPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            suggestedClasses.forEach { cls ->
                                FilterChip(
                                    selected = draftKelas.equals(cls, ignoreCase = true),
                                    onClick = { draftKelas = cls },
                                    label = { Text(cls, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrownPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = draftKelas,
                            onValueChange = { draftKelas = it },
                            label = { Text("Atau ketik kelas spesifik (opsional)") },
                            placeholder = { Text("Contoh: XII ${draftJurusan.code} 1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScanGreenPrimary,
                                focusedLabelColor = ScanGreenPrimary
                            )
                        )

                        Button(
                            onClick = {
                                viewModel.updateTeacherQualification(
                                    jurusan = draftJurusan,
                                    kelas = draftKelas.trim().ifEmpty { null }
                                )
                                isEditingQualification = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_qualification_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Terapkan Kualifikasi ${draftJurusan.code}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. PEMILIH TANGGAL (HARI INI & HARI YANG LALU)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = ScanGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pilih Hari / Tanggal Pemantauan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownDark
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedDate == today) ScanGreenBg else AmberContainer
                    ) {
                        Text(
                            text = if (selectedDate == today) "HARI INI" else "RIWAYAT LALU",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedDate == today) ScanGreenDark else BrownDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.repository.formatReadableDate(selectedDate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickDates = listOf(
                        "Hari Ini" to today,
                        "Kemarin" to yesterday,
                        "2 Hari Lalu" to twoDaysAgo,
                        "3 Hari Lalu" to threeDaysAgo
                    )
                    quickDates.forEach { (label, dateStr) ->
                        FilterChip(
                            selected = selectedDate == dateStr,
                            onClick = { viewModel.setSelectedStatsDate(dateStr) },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ScanGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("date_chip_$dateStr")
                        )
                    }
                    FilterChip(
                        selected = selectedDate !in listOf(today, yesterday, twoDaysAgo, threeDaysAgo),
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text("Pilih Tanggal", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrownPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("date_picker_chip")
                    )
                }
            }
        }

        // 3. KARTU PERSENTASE KEHADIRAN SHOLAT (HARI TERPILIH)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer_percentage_summary_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCardBg),
            border = BorderStroke(1.5.dp, CreamCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = BrownPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Persentase Sholat Siswa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrownDark
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrownContainer
                    ) {
                        Text(
                            text = "${stats.totalStudents} Siswa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (stats.totalStudents == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser.role == Role.ADMIN_GURU && currentUser.jurusan == null) {
                                "Silakan pilih Jurusan & Kelas Anda di kartu atas untuk menampilkan persentase siswa."
                            } else {
                                "Belum ada data siswa terdaftar pada jurusan / kelas ini."
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    // Main Go-Home / Sholat Wajib KPI Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ScanGreenBg,
                        border = BorderStroke(1.dp, ScanGreenBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sholat Wajib Lengkap (Izin Pulang)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ScanGreenDark
                                )
                                Text(
                                    text = "${stats.goHomeEligibleCount} dari ${stats.totalStudents} siswa (Dzuhur + Ashar / Syar'i)",
                                    fontSize = 11.sp,
                                    color = ScanGreenDark.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ScanGreenPrimary
                            ) {
                                Text(
                                    text = "${stats.goHomePercentage}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Individual Prayer Progress Bars
                    PrayerProgressRow(
                        emoji = "🌅",
                        label = "Sholat Dhuha (Sunnah)",
                        presentCount = stats.dhuhaPresentCount,
                        totalStudents = stats.totalStudents,
                        percentage = stats.dhuhaPercentage,
                        barColor = AmberAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrayerProgressRow(
                        emoji = "☀️",
                        label = "Sholat Dzuhur (Wajib)",
                        presentCount = stats.dzuhurPresentCount,
                        totalStudents = stats.totalStudents,
                        percentage = stats.dzuhurPercentage,
                        barColor = BrownPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrayerProgressRow(
                        emoji = "🌇",
                        label = "Sholat Ashar (Wajib)",
                        presentCount = stats.asharPresentCount,
                        totalStudents = stats.totalStudents,
                        percentage = stats.asharPercentage,
                        barColor = ScanGreenPrimary
                    )

                    if (stats.halanganCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ℹ ${stats.halanganCount} siswi berstatus Berhalangan Syar'i pada tanggal ini (terhitung izin pulang aktif).",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrownMedium
                        )
                    }
                }
            }
        }

        // 4. PERBANDINGAN 7 HARI TERAKHIR (HARI INI VS HARI YANG LALU)
        if (stats.totalStudents > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("past_days_percentage_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = BrownPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Riwayat Persentase 7 Hari Terakhir",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrownDark
                        )
                    }
                    Text(
                        text = "Ketuk baris hari untuk melihat rincian tanggal tersebut",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    recentTrend.forEachIndexed { idx, dayTrend ->
                        val isSelected = dayTrend.dateString == selectedDate
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setSelectedStatsDate(dayTrend.dateString) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ScanGreenBg else CreamCardBg,
                            border = if (isSelected) BorderStroke(1.2.dp, ScanGreenPrimary) else null
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayTrend.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) ScanGreenDark else BrownDark
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MiniPercentBadge("Dhuha", dayTrend.dhuhaPercentage, AmberContainer, BrownDark)
                                        MiniPercentBadge("Dzuhur", dayTrend.dzuhurPercentage, BrownContainer, BrownDark)
                                        MiniPercentBadge("Ashar", dayTrend.asharPercentage, Color(0xFFE8F5E9), TicketGreenPass)
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { (dayTrend.goHomePercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isSelected) ScanGreenPrimary else BrownPrimary,
                                    trackColor = Color(0xFFE8E0D8),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                        if (idx < recentTrend.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }

        // 5. PERSENTASE PER KELAS (DALAM JURUSAN TERPILIH)
        if (stats.classBreakdown.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = BrownPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rincian Persentase Per Kelas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrownDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    stats.classBreakdown.forEach { classStat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDE4DC))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${classStat.className} (${classStat.totalStudents} Siswa)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BrownDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (classStat.goHomePercentage >= 50) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ) {
                                        Text(
                                            text = "Wajib ${classStat.goHomePercentage}%",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (classStat.goHomePercentage >= 50) TicketGreenPass else TicketRedBlock,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Dhuha: ${classStat.dhuhaPercentage}%", fontSize = 11.sp, color = Color(0xFF5A4A42), modifier = Modifier.weight(1f))
                                    Text("Dzuhur: ${classStat.dzuhurPercentage}%", fontSize = 11.sp, color = Color(0xFF5A4A42), modifier = Modifier.weight(1f))
                                    Text("Ashar: ${classStat.asharPercentage}%", fontSize = 11.sp, color = Color(0xFF5A4A42), modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. DAFTAR RINGKAS SISWA PADA TANGGAL TERPILIH + TOMBOL MONITORING LENGKAP
        if (stats.studentSummaries.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Status Siswa (${viewModel.repository.formatReadableDate(selectedDate)})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrownDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    stats.studentSummaries.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.student.fullName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrownDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.student.kelas ?: item.student.jurusan?.code} • " +
                                        "Dhuha: ${if (item.dhuhaStatus == DhuhaStatus.HADIR) "✓" else "—"} | " +
                                        "Dzuhur: ${if (item.dzuhurRecord != null) "✓" else "—"} | " +
                                        "Ashar: ${if (item.asharRecord != null) "✓" else "—"}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF5A4A42)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.isEligibleToGoHome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = when {
                                        item.isCurrentlyOnPeriod -> "Syar'i"
                                        item.isEligibleToGoHome -> "Lengkap"
                                        else -> "Belum"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isEligibleToGoHome) TicketGreenPass else TicketRedBlock,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF3ECE7))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_rekap_from_stats_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unduh File Rekap Siswa (.csv / Excel)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.MONITORING_REKAP) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_full_monitoring_from_stats_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Buka Rekap & Pencarian Lengkap ›",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrownPrimary
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(selectedDate) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(selectedDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            viewModel.setSelectedStatsDate(sdf.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih", fontWeight = FontWeight.Bold, color = ScanGreenPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExportDialog) {
        ExportRekapDialog(
            viewModel = viewModel,
            currentUser = currentUser,
            selectedDate = selectedDate,
            onDismissRequest = { showExportDialog = false }
        )
    }
}

@Composable
private fun PrayerProgressRow(
    emoji: String,
    label: String,
    presentCount: Int,
    totalStudents: Int,
    percentage: Int,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$emoji $label",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark
            )
            Text(
                text = "$percentage% ($presentCount/$totalStudents)",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = barColor,
            trackColor = Color(0xFFEAE0D6),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun MiniPercentBadge(
    label: String,
    percentage: Int,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = "$label $percentage%",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
