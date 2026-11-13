package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.WaterDrop
import com.example.model.Gender
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DhuhaStatus
import com.example.model.PrayerType
import com.example.model.Role
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.CreamPillActive
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.ui.components.InstallAppDialog
import com.example.viewmodel.AttendanceViewModel

@Composable
fun DashboardScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val currentUserState by viewModel.currentUser.collectAsState()
    val currentUser = currentUserState ?: return
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val today = viewModel.repository.getTodayDateString()
    var showInstallAppDialog by remember { mutableStateOf(false) }

    val studentRecords = allRecords.filter {
        it.nisn == currentUser.nisnOrNip && it.dateString == today
    }
    val isStudentOnPeriod = viewModel.repository.isStudentCurrentlyOnPeriod(currentUser.nisnOrNip)
    val periodStatus = viewModel.getPeriodStatus()
    val dzuhurCompleted = studentRecords.any { it.prayerType == PrayerType.DZUHUR }
    val asharCompleted = studentRecords.any { it.prayerType == PrayerType.ASHAR }
    val canGoHome = isStudentOnPeriod || (dzuhurCompleted && asharCompleted)
    val dhuhaRecord = studentRecords.find { it.prayerType == PrayerType.DHUHA }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card with Aesthetic Earthy Brown Gradient
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(BrownDark, BrownMedium)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = "Mosque Icon",
                                    tint = AmberAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "SMKN 1 PANGKAJENE DAN KEPULAUAN",
                                    color = AmberAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Absensi Sholat Berjamaah",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Role badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberContainer
                        ) {
                            Text(
                                text = currentUser.role.title.split(" ").first(),
                                color = BrownDark,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // User Profile Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = currentUser.avatarEmoji,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.fullName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subInfo = when (currentUser.role) {
                                Role.SISWA -> "NISN: ${currentUser.nisnOrNip} • ${currentUser.kelas} • ${currentUser.jurusan?.name}"
                                Role.ADMIN_GURU -> "Guru: ${currentUser.nisnOrNip} • ${currentUser.kelas ?: currentUser.jurusan?.name ?: "Piket Masjid"}"
                                Role.ADMIN_PUSAT -> "Akses Pusat: Seluruh 7 Jurusan SMKN 1 Pangkep"
                            }
                            Text(
                                text = subInfo,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Card Pasang di HP (Unduh APK / Scan QR)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInstallAppDialog = true }
                .testTag("dashboard_btn_install_app"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ScanGreenBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ScanGreenBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ScanGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akses Website & Pasang di HP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ScanGreenDark
                    )
                    Text(
                        text = "Buka lewat browser atau scan barcode APK permanen",
                        fontSize = 11.sp,
                        color = ScanGreenDark.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = ScanGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // KHUSUS SISWI PEREMPUAN: NOTIFIKASI & AKSES LAPORAN BERHALANGAN SYAR'I
        if (currentUser.role == Role.SISWA && currentUser.gender == Gender.PEREMPUAN) {
            if (periodStatus.hasExpired) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AttendanceViewModel.Screen.PERIOD_REPORT) }
                        .testTag("dashboard_period_expired_alert"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD4A017))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFECB3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOTIFIKASI: BATAS BERHALANGAN SELESAI!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF856404)
                            )
                            Text(
                                text = "Estimasi ${periodStatus.estimatedDays} hari Anda telah selesai (Hari ke-${periodStatus.currentDay}). Harap mandi wajib dan kembali sholat berjamaah.",
                                fontSize = 11.sp,
                                color = Color(0xFF533E2D),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else if (isStudentOnPeriod) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AttendanceViewModel.Screen.PERIOD_REPORT) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrownMedium)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = BrownPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STATUS: BERHALANGAN SYAR'I (HARI KE-${periodStatus.currentDay}/${periodStatus.estimatedDays})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrownDark
                            )
                            Text(
                                text = "Bebas kewajiban sholat dan Tiket Izin Pulang otomatis aktif.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                        Text(
                            text = "Kelola ›",
                            fontWeight = FontWeight.Bold,
                            color = BrownPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.PERIOD_REPORT) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrownPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lapor Berhalangan Syar'i (Haid)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                }
            }
        }

        // TIKET PULANG / GATE CLEARANCE (Siswa Focus Feature)
        if (currentUser.role == Role.SISWA) {
            Text(
                text = "STATUS SYARAT IZIN PULANG",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark,
                letterSpacing = 0.8.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gate_ticket_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canGoHome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = if (canGoHome) TicketGreenPass else TicketRedBlock
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (canGoHome) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                            contentDescription = "Status Icon",
                            tint = if (canGoHome) TicketGreenPass else TicketRedBlock,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (canGoHome) "IZIN PULANG AKTIF (TIKET HIJAU)" else "BELUM BOLEH PULANG",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canGoHome) TicketGreenPass else TicketRedBlock
                            )
                            Text(
                                text = if (canGoHome) {
                                    if (isStudentOnPeriod) "Terverifikasi izin pulang: Berhalangan Syar'i (Bebas kewajiban sholat)."
                                    else "Terverifikasi telah melaksanakan Sholat Dzuhur & Ashar berjamaah di masjid."
                                } else {
                                    "Wajib melaksanakan Sholat Dzuhur (${if (dzuhurCompleted) "✓" else "Belum"}) & Ashar (${if (asharCompleted) "✓" else "Belum"}) berjamaah di masjid."
                                },
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Check-in Action Button for Student (Kolom / Tombol Scan QR Hijau)
                    Button(
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.SCAN_QR) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scan_qr_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScanGreenPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan QR / Masukkan Kode Sesi",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tunjukkan kartu ini kepada Satpam di pos gerbang sekolah saat keluar",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3 WAKTU SHOLAT SCHEDULE & STATUS
        Text(
            text = "JADWAL SHOLAT BERJAMAAH HARI INI",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BrownDark,
            letterSpacing = 0.8.sp
        )

        // 1. Sholat Dhuha Card (Sunnah: Hadir / Tidak Hadir)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CreamPillActive),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌅", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sholat Dhuha",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = BrownDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberContainer
                            ) {
                                Text(
                                    text = "Sunnah",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrownDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pukul: 07:00 - 08:30 WITA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5A4A42)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status display
                    val dhuhaStatus = dhuhaRecord?.dhuhaStatus ?: DhuhaStatus.BELUM_MEMILIH
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (dhuhaStatus) {
                            DhuhaStatus.HADIR -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF3ECE7)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (dhuhaStatus) {
                                DhuhaStatus.HADIR -> TicketGreenPass.copy(alpha = 0.5f)
                                else -> Color(0xFFDDD2C9)
                            }
                        )
                    ) {
                        Text(
                            text = when (dhuhaStatus) {
                                DhuhaStatus.HADIR -> "Hadir"
                                DhuhaStatus.TIDAK_HADIR -> "Tidak Hadir"
                                DhuhaStatus.BELUM_MEMILIH -> "Belum Dipilih"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when (dhuhaStatus) {
                                DhuhaStatus.HADIR -> TicketGreenPass
                                else -> Color(0xFF7A6A60)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // If Siswa: Provide direct toggle button for Sunnah (Hadir / Tidak Hadir)
                if (currentUser.role == Role.SISWA) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isHadirSelected = dhuhaRecord?.dhuhaStatus == DhuhaStatus.HADIR
                        val isTidakSelected = dhuhaRecord?.dhuhaStatus == DhuhaStatus.TIDAK_HADIR

                        Button(
                            onClick = { viewModel.submitDhuhaChoice(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("dhuha_hadir_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHadirSelected) TicketGreenPass else BrownContainer,
                                contentColor = if (isHadirSelected) Color.White else BrownDark
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hadir Sholat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.submitDhuhaChoice(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("dhuha_tidak_hadir_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTidakSelected) Color(0xFF757575) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isTidakSelected) Color.White else Color.DarkGray
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tidak Hadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Sholat Dzuhur & 3. Sholat Ashar Cards
        listOf(PrayerType.DZUHUR, PrayerType.ASHAR).forEach { pt ->
            val isDone = studentRecords.find { it.prayerType == pt }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CreamCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDone != null) Color(0xFFD1E7DD) else CreamPillActive
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pt == PrayerType.DZUHUR) "☀️" else "🌇",
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pt.prayerName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = BrownDark
                            )
                            if (pt.isMandatoryForGoHome) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberContainer
                                ) {
                                    Text(
                                        text = "Wajib",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrownDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pukul: ${pt.typicalTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5A4A42)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Fixed Clean Status Indicator (Hadir / Belum Absen)
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (isDone != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TicketGreenPass.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TicketGreenPass,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Column {
                                        Text(
                                            text = "HADIR",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TicketGreenPass,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = isDone.formattedTime.take(5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF3ECE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD2C9))
                            ) {
                                Text(
                                    text = "Belum Absen",
                                    color = Color(0xFF7A6A60),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // QUICK ACCESS MENU FOR ADMINS
        if (currentUser.role == Role.ADMIN_GURU || currentUser.role == Role.ADMIN_PUSAT) {
            Text(
                text = "MENU KHUSUS PEMANTAUAN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark,
                letterSpacing = 0.8.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AttendanceViewModel.Screen.MONITORING_REKAP) }
                        .testTag("nav_monitoring_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrownContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "📊", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rekap Absensi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrownDark
                        )
                        Text(
                            text = if (currentUser.role == Role.ADMIN_PUSAT) "Semua 7 Jurusan" else "Kelas / Jurusan Anda",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AttendanceViewModel.Screen.QR_GENERATOR) }
                        .testTag("nav_qr_generator_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrownContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "🕌", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tampilkan QR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrownDark
                        )
                        Text(
                            text = "Kode Sesi & Layar Masjid",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // KHUSUS ADMIN PUSAT: KELOLA AKUN GURU & RESET PASSWORD
            if (currentUser.role == Role.ADMIN_PUSAT) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AttendanceViewModel.Screen.ADMIN_USER_MANAGEMENT) }
                        .testTag("nav_admin_user_mgmt_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = BrownPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kelola Akun Guru & Password",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = BrownDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberAccent.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Admin Pusat",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrownDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tambah akun guru baru & reset kata sandi guru / siswa",
                                fontSize = 11.5.sp,
                                color = Color(0xFF5A4A42)
                            )
                        }
                        Text(
                            text = "Buka ›",
                            fontWeight = FontWeight.Bold,
                            color = BrownPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Dialog Pasang di HP (Unduh APK & QR Link)
    if (showInstallAppDialog) {
        InstallAppDialog(onDismissRequest = { showInstallAppDialog = false })
    }
}
