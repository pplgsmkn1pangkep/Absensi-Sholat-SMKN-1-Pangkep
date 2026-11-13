package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DhuhaStatus
import com.example.model.Jurusan
import com.example.model.Role
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel

@Composable
fun MonitoringScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUserState by viewModel.currentUser.collectAsState()
    val currentUser = currentUserState ?: return
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val selectedJurusan by viewModel.selectedJurusanFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val studentSummaries = viewModel.getFilteredStudents(allRecords)
    val eligibleCount = studentSummaries.count { it.isEligibleToGoHome }
    val totalCount = studentSummaries.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD) },
                modifier = Modifier.testTag("monitoring_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pemantauan Sholat Siswa",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Akses: ${currentUser.role.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Export / Download Report Action for Admins
            IconButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Laporan Kehadiran Sholat berhasil diunduh (Excel / PDF)",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier.testTag("export_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Unduh Laporan",
                    tint = BrownPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // RBAC Scope Info Banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrownContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = BrownPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                val scopeNotice = when (currentUser.role) {
                    Role.SISWA -> "Catatan absensi pribadi siswa."
                    Role.ADMIN_GURU -> "Akses kelas/jurusan: ${currentUser.kelas ?: currentUser.jurusan?.name ?: "Piket Masjid Darul Ulum"}."
                    Role.ADMIN_PUSAT -> "Akses Penuh: Seluruh 7 jurusan di SMKN 1 Pangkajene dan Kepulauan."
                }
                Text(
                    text = scopeNotice,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Cari nama siswa, NISN, atau kelas...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_student_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrownPrimary,
                focusedLabelColor = BrownPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Jurusan Filter Chips
        if (currentUser.role == Role.ADMIN_PUSAT || (currentUser.role == Role.ADMIN_GURU && currentUser.jurusan == null)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedJurusan == null,
                    onClick = { viewModel.setJurusanFilter(null) },
                    label = { Text("Semua (7 Jurusan)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrownPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                Jurusan.entries.forEach { j ->
                    FilterChip(
                        selected = selectedJurusan == j,
                        onClick = { viewModel.setJurusanFilter(j) },
                        label = { Text(j.code, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrownPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Statistics Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total: $totalCount Siswa",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (eligibleCount > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = "$eligibleCount/$totalCount Izin Pulang (Dzuhur & Ashar)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (eligibleCount > 0) TicketGreenPass else TicketRedBlock,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Student List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(studentSummaries, key = { it.student.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_item_${item.student.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDFB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE8DCCF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BrownContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = item.student.avatarEmoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.student.fullName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.5.sp,
                                    color = Color(0xFF1B120C),
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "NISN: ${item.student.nisnOrNip} • ${item.student.kelas ?: item.student.jurusan?.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5A4A42)
                                )
                            }

                            // Gate Clearance Badge
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.isEligibleToGoHome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isEligibleToGoHome) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (item.isEligibleToGoHome) TicketGreenPass else TicketRedBlock,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (item.isEligibleToGoHome) "Boleh Pulang" else "Tahan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (item.isEligibleToGoHome) TicketGreenPass else TicketRedBlock
                                        )
                                    }
                                }
                                if (item.isCurrentlyOnPeriod) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFAEDCD)
                                    ) {
                                        Text(
                                            text = "Berhalangan (${item.periodRecord?.estimatedDays ?: 5} hr)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrownDark,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Sholat Mini Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Dhuha Status (Sunnah)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (item.dhuhaStatus) {
                                    DhuhaStatus.HADIR -> Color(0xFFE8F5E9)
                                    DhuhaStatus.TIDAK_HADIR -> Color(0xFFF1F3F4)
                                    DhuhaStatus.BELUM_MEMILIH -> BrownContainer
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Dhuha (Sunnah)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (item.dhuhaStatus) {
                                            DhuhaStatus.HADIR -> Color(0xFF0F5132)
                                            DhuhaStatus.TIDAK_HADIR -> Color.DarkGray
                                            DhuhaStatus.BELUM_MEMILIH -> BrownDark
                                        }
                                    )
                                    Text(
                                        text = when (item.dhuhaStatus) {
                                            DhuhaStatus.HADIR -> "Hadir"
                                            DhuhaStatus.TIDAK_HADIR -> "Tidak Hadir"
                                            DhuhaStatus.BELUM_MEMILIH -> "—"
                                        },
                                        fontSize = 9.sp,
                                        color = when (item.dhuhaStatus) {
                                            DhuhaStatus.HADIR -> TicketGreenPass
                                            DhuhaStatus.TIDAK_HADIR -> Color.Gray
                                            DhuhaStatus.BELUM_MEMILIH -> Color.Gray
                                        }
                                    )
                                }
                            }

                            // Dzuhur
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.dzuhurRecord != null) Color(0xFFE8F5E9) else Color(0xFFF1F3F4),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Dzuhur ★",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.dzuhurRecord != null) Color(0xFF0F5132) else Color.DarkGray
                                    )
                                    Text(
                                        text = if (item.dzuhurRecord != null) item.dzuhurRecord.formattedTime.take(5) else "—",
                                        fontSize = 9.sp,
                                        color = if (item.dzuhurRecord != null) TicketGreenPass else Color.Gray
                                    )
                                }
                            }

                            // Ashar (Syarat Pulang)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.asharRecord != null) Color(0xFFD1E7DD) else Color(0xFFF1F3F4),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Ashar ★",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.asharRecord != null) Color(0xFF0F5132) else Color.DarkGray
                                    )
                                    Text(
                                        text = if (item.asharRecord != null) item.asharRecord.formattedTime.take(5) else "—",
                                        fontSize = 9.sp,
                                        color = if (item.asharRecord != null) TicketGreenPass else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
