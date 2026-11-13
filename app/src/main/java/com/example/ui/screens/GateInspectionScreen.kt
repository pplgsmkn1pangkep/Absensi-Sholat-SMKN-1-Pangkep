package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrayerType
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel

@Composable
fun GateInspectionScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val currentUserState by viewModel.currentUser.collectAsState()
    val student = currentUserState ?: return
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val today = viewModel.repository.getTodayDateString()

    val isStudentOnPeriod = viewModel.repository.isStudentCurrentlyOnPeriod(student.nisnOrNip)
    val periodRecord = viewModel.repository.getActivePeriodRecord(student.nisnOrNip)
    val dzuhurRecord = allRecords.find {
        it.nisn == student.nisnOrNip &&
        it.dateString == today &&
        it.prayerType == PrayerType.DZUHUR
    }
    val asharRecord = allRecords.find {
        it.nisn == student.nisnOrNip &&
        it.dateString == today &&
        it.prayerType == PrayerType.ASHAR
    }
    val isGoHomeReady = isStudentOnPeriod || (dzuhurRecord != null && asharRecord != null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD) },
                modifier = Modifier.testTag("gate_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Profil Tiket Gerbang",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Pemeriksaan Izin Pulang Satpam",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Student Info Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrownContainer)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🛡️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kartu Identitas Keluar Siswa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BrownDark
                    )
                    Text(
                        text = "Cukup perlihatkan layar ini kepada satpam atau guru piket di pintu gerbang sekolah saat akan pulang.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Gate Ticket Card (Dedicated to Current Student)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gate_inspection_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isGoHomeReady) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.5.dp,
                color = if (isGoHomeReady) TicketGreenPass else TicketRedBlock
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Icon with Pulse Ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGoHomeReady) TicketGreenPass.copy(alpha = 0.15f)
                            else TicketRedBlock.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGoHomeReady) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = if (isGoHomeReady) TicketGreenPass else TicketRedBlock,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isGoHomeReady) "IZIN PULANG AKTIF" else "BELUM BOLEH PULANG",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGoHomeReady) TicketGreenPass else TicketRedBlock,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isStudentOnPeriod -> "TIKET HIJAU • BERHALANGAN SYAR'I"
                        isGoHomeReady -> "TIKET HIJAU • DZUHUR & ASHAR LENGKAP"
                        else -> "GERBANG DITAHAN • WAJIB DZUHUR & ASHAR"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGoHomeReady) Color(0xFF0F5132) else Color(0xFF842029),
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Student Identity Card in Gate Screen
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = student.avatarEmoji,
                            fontSize = 38.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = student.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BrownDark
                        )
                        Text(
                            text = "NISN: ${student.nisnOrNip} • ${student.kelas ?: "Siswa SMKN 1"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Jurusan: ${student.jurusan?.fullName ?: student.jurusan?.code ?: "-"}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isStudentOnPeriod) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ Verifikasi Satpam: Siswi Berhalangan Syar'i (Izin Pulang Sah)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TicketGreenPass,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (dzuhurRecord != null) Color(0xFFE8F5E9) else Color(0xFFFFF3CD),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (dzuhurRecord != null)
                                        "✓ Sholat Dzuhur: ${dzuhurRecord.formattedTime} (Masjid)"
                                    else
                                        "⚠️ Belum melakukan scan Sholat Dzuhur di masjid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dzuhurRecord != null) TicketGreenPass else Color(0xFF664D03),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (asharRecord != null) Color(0xFFE8F5E9) else Color(0xFFFFF3CD),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (asharRecord != null)
                                        "✓ Sholat Ashar: ${asharRecord.formattedTime} (Masjid)"
                                    else
                                        "⚠️ Belum melakukan scan Sholat Ashar di masjid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (asharRecord != null) TicketGreenPass else Color(0xFF664D03),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // If not done: Quick action to Scan
                if (!isGoHomeReady) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.SCAN_QR) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Kamera Scan QR Sholat Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
