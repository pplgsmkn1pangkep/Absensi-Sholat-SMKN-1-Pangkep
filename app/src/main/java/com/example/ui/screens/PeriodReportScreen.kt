package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel

@Composable
fun PeriodReportScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUserState by viewModel.currentUser.collectAsState()
    val student = currentUserState ?: return
    val periodRecords by viewModel.repository.periodRecords.collectAsState()

    val currentRecord = viewModel.repository.getActivePeriodRecord(student.nisnOrNip)
    val periodStatus = viewModel.getPeriodStatus()

    var selectedDays by remember { mutableIntStateOf(currentRecord?.estimatedDays ?: 5) }
    var notesText by remember { mutableStateOf(currentRecord?.notes ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD) },
                modifier = Modifier.testTag("period_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Laporan Berhalangan Syar'i",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Khusus Siswa Perempuan • Batasan Hari & Pengingat Sholat",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // NOTIFIKASI BATAS SELESAI BERHALANGAN (JIKA SUDAH MELEBIHI BATAS)
        if (periodStatus.hasExpired) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("period_expired_alert_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD4A017))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFECB3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PENGINGAT: BATAS BERHALANGAN SELESAI!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = periodStatus.message ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF533E2D),
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.reportPeriodEnd()
                                Toast.makeText(context, "Alhamdulillah! Status telah diubah menjadi Suci. Silakan ikuti sholat berjamaah.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("end_period_now_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saya Sudah Suci (Mandi Wajib Selesai)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current Active Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentRecord != null) Color(0xFFFDF7F4) else Color(0xFFF4FBF6)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (currentRecord != null) BrownMedium else TicketGreenPass
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentRecord != null) Icons.Default.WaterDrop else Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = if (currentRecord != null) BrownPrimary else TicketGreenPass,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (currentRecord != null) "STATUS: SEDANG BERHALANGAN SYAR'I" else "STATUS: SUCI (WAJIB SHOLAT BERJAMAAH)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (currentRecord != null) BrownDark else TicketGreenPass
                        )
                        Text(
                            text = if (currentRecord != null)
                                "Hari ke-${periodStatus.currentDay} dari batasan ${currentRecord.estimatedDays} hari (Mulai: ${currentRecord.startDateString})"
                            else
                                "Wajib mengikuti sholat Dhuha (sunnah), Dzuhur, dan Ashar.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                if (currentRecord != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrownContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Selama status ini aktif, Anda bebas kewajiban sholat dan sistem secara otomatis mengaktifkan Tiket Hijau Izin Pulang di gerbang sekolah.",
                            fontSize = 11.sp,
                            color = BrownDark,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.reportPeriodEnd()
                            Toast.makeText(context, "Status berhalangan telah diselesaikan. Selamat beribadah kembali!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TicketGreenPass),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Konfirmasi Sudah Selesai / Suci Hari Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Form Laporan Baru / Perbarui Batasan Hari
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "PILIH ESTIMASI BATAS HARI BERHALANGAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Menurut fiqih Islam, masa haid umumnya berlangsung 3 hingga 7 hari (maksimal 15 hari). Notifikasi akan muncul saat batas hari ini tercapai.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Days Selection (3, 4, 5, 6, 7, 8, 9, 10 Hari)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 5, 6, 7, 8).forEach { d ->
                        FilterChip(
                            selected = selectedDays == d,
                            onClick = { selectedDays = d },
                            label = { Text("$d Hari", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrownPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Keterangan Tambahan (Opsional)") },
                    placeholder = { Text("Contoh: Hari pertama mulai pagi ini") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_notes_field"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrownPrimary,
                        focusedLabelColor = BrownPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.reportPeriodStart(selectedDays, notesText)
                        Toast.makeText(context, "Laporan berhalangan $selectedDays hari berhasil disimpan.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_period_report_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentRecord == null) "Kirim Laporan Berhalangan" else "Perbarui Batasan Hari",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fiqh Guidelines Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BrownContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Panduan Fiqih Ibadah Siswi SMKN 1 Pangkajene dan Kepulauan:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BrownDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Waktu minimal haid adalah 24 jam (1 hari 1 malam), umumnya 6 - 7 hari, dan maksimal 15 hari.\n• Jika darah keluar melebihi 15 hari, maka itu darah istihadah (penyakit), dan siswi diwajibkan mandi wajib serta sholat berjamaah seperti biasa.\n• Ketika darah sudah berhenti sebelum batas hari yang ditentukan, segera mandi wajib dan nonaktifkan status berhalangan.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
