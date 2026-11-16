package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UserProfile
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.util.ReportExporter
import com.example.viewmodel.AttendanceViewModel

@Composable
fun ExportRekapDialog(
    viewModel: AttendanceViewModel,
    currentUser: UserProfile,
    selectedDate: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val allUsers by viewModel.repository.users.collectAsState()
    val selectedJurusanFilter by viewModel.selectedJurusanFilter.collectAsState()
    val selectedKelasFilter by viewModel.selectedKelasFilter.collectAsState()

    var exportMode by remember { mutableIntStateOf(0) } // 0: Tanggal Terpilih, 1: 7 Hari Terakhir
    var savedBannerMessage by remember { mutableStateOf<String?>(null) }

    val scopeLabel = remember(currentUser, selectedJurusanFilter, selectedKelasFilter) {
        ReportExporter.buildScopeLabel(currentUser, selectedJurusanFilter, selectedKelasFilter)
    }

    val singleDayStats = remember(allRecords, allUsers, selectedDate, selectedJurusanFilter, selectedKelasFilter, currentUser) {
        viewModel.calculatePrayerStatsForDate(allRecords, selectedDate)
    }

    val readableDate = remember(selectedDate) {
        viewModel.repository.formatReadableDate(selectedDate)
    }

    fun buildCurrentCsv(): Pair<String, String> {
        val isMultiDay = exportMode == 1
        val fileName = ReportExporter.buildSuggestedFileName(
            currentUser = currentUser,
            selectedJurusanFilter = selectedJurusanFilter,
            selectedKelasFilter = selectedKelasFilter,
            dateTag = selectedDate,
            isMultiDay = isMultiDay
        )
        val csvContent = if (!isMultiDay) {
            ReportExporter.buildSingleDayCsvContent(
                currentUser = currentUser,
                scopeLabel = scopeLabel,
                readableDate = readableDate,
                stats = singleDayStats
            )
        } else {
            val trends = viewModel.calculateRecentDaysTrend(allRecords, daysCount = 7)
            val dailyStats = trends.map { t ->
                t to viewModel.calculatePrayerStatsForDate(allRecords, t.dateString)
            }
            ReportExporter.buildMultiDayCsvContent(
                currentUser = currentUser,
                scopeLabel = scopeLabel,
                dailyTrends = trends,
                dailyStatsList = dailyStats
            )
        }
        return fileName to csvContent
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val (fileName, csvContent) = buildCurrentCsv()
            val result = ReportExporter.writeCsvToUri(context, uri, csvContent)
            result.onSuccess {
                savedBannerMessage = "File rekap '$fileName' berhasil disimpan!"
                Toast.makeText(context, "File rekap berhasil disimpan.", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, "Gagal menyimpan file: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ScanGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = ScanGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Unduh File Rekap Siswa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrownDark
                            )
                            Text(
                                text = "Format Excel / CSV (.csv) • SIM SHOLAT",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preview scope card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DATA YANG AKAN DIUNDUH:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kualifikasi: $scopeLabel",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrownDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Jumlah Siswa: ${singleDayStats.totalStudents} siswa",
                            fontSize = 11.5.sp,
                            color = Color(0xFF5A4A42)
                        )
                        if (singleDayStats.totalStudents > 0) {
                            Text(
                                text = "Ringkasan ($readableDate): Dhuha ${singleDayStats.dhuhaPercentage}% • Dzuhur ${singleDayStats.dzuhurPercentage}% • Ashar ${singleDayStats.asharPercentage}% • Pulang ${singleDayStats.goHomePercentage}%",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScanGreenDark,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pilih Rentang Data Rekap:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Option 0: Single Date
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportMode = 0 }
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (exportMode == 0) ScanGreenBg else Color(0xFFF8F5F2),
                    border = BorderStroke(1.dp, if (exportMode == 0) ScanGreenPrimary else CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == 0,
                            onClick = { exportMode = 0 },
                            colors = RadioButtonDefaults.colors(selectedColor = ScanGreenPrimary)
                        )
                        Column {
                            Text(
                                text = "Tanggal Terpilih ($readableDate)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrownDark
                            )
                            Text(
                                text = "Persentase sholat + rincian siswa pada tanggal ini",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Option 1: Last 7 Days
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportMode = 1 }
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (exportMode == 1) ScanGreenBg else Color(0xFFF8F5F2),
                    border = BorderStroke(1.dp, if (exportMode == 1) ScanGreenPrimary else CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == 1,
                            onClick = { exportMode = 1 },
                            colors = RadioButtonDefaults.colors(selectedColor = ScanGreenPrimary)
                        )
                        Column {
                            Text(
                                text = "Rekap Lengkap 7 Hari Terakhir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrownDark
                            )
                            Text(
                                text = "Perbandingan persentase 7 hari + absensi harian semua siswa",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                savedBannerMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ScanGreenBg,
                        border = BorderStroke(1.dp, ScanGreenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ScanGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScanGreenDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Download Button (MediaStore Downloads on Android 10+, CreateDocument fallback)
                Button(
                    onClick = {
                        if (singleDayStats.totalStudents == 0) {
                            Toast.makeText(context, "Belum ada data siswa pada kualifikasi ini.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val (fileName, csvContent) = buildCurrentCsv()
                        val saveRes = ReportExporter.saveCsvToDownloads(context, fileName, csvContent)
                        saveRes.onSuccess { savedPath ->
                            savedBannerMessage = "Berhasil diunduh ke folder $savedPath"
                            Toast.makeText(context, "File tersimpan di $savedPath", Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            if (err is UnsupportedOperationException) {
                                createDocumentLauncher.launch(fileName)
                            } else {
                                // Fallback to document picker if MediaStore insert fails on specific OEM
                                createDocumentLauncher.launch(fileName)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_download_csv_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unduh ke Folder Download (.csv)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (singleDayStats.totalStudents == 0) {
                                Toast.makeText(context, "Belum ada data siswa pada kualifikasi ini.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val (fileName, _) = buildCurrentCsv()
                            createDocumentLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp), tint = BrownPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih Folder", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                    }

                    OutlinedButton(
                        onClick = {
                            if (singleDayStats.totalStudents == 0) {
                                Toast.makeText(context, "Belum ada data siswa pada kualifikasi ini.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val (fileName, csvContent) = buildCurrentCsv()
                            val shareRes = ReportExporter.createAndShareCsvFile(context, fileName, csvContent)
                            shareRes.onFailure {
                                Toast.makeText(context, "Gagal membagikan file: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_csv_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = ScanGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim / Excel", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ScanGreenDark)
                    }
                }
            }
        }
    }
}
