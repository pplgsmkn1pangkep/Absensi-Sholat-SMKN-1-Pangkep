package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.PrayerType
import com.example.ui.components.CameraQrScannerView
import com.example.ui.components.ProceduralQrView
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel

@Composable
fun QrGeneratorScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val activeQrByPrayer by viewModel.repository.activeQrByPrayer.collectAsState()
    var selectedPrayer by remember { mutableStateOf(PrayerType.DZUHUR) }
    var selectedModeTab by remember { mutableIntStateOf(0) } // 0: Tampilkan QR Masjid, 1: Scan / Cek Barcode Kamera

    // Ensure the displayed prayer QR is published to Firestore when Guru selects a tab
    LaunchedEffect(selectedPrayer) {
        viewModel.ensurePrayerQrPublished(selectedPrayer)
    }

    val displayedQr = activeQrByPrayer[selectedPrayer]
        ?: viewModel.repository.getActiveQrFor(selectedPrayer)

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
                modifier = Modifier.testTag("qr_gen_back_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "QR Code Masjid & Scanner Guru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Mode Tampil & Verifikasi Petugas Piket / Guru",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Switcher for Guru: Display QR vs Camera Scan
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CreamCardBg,
            border = BorderStroke(1.dp, BrownPrimary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedModeTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedModeTab]),
                        color = if (selectedModeTab == 0) BrownPrimary else ScanGreenPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedModeTab == 0,
                    onClick = { selectedModeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tampilkan QR Masjid",
                                fontSize = 12.sp,
                                fontWeight = if (selectedModeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedModeTab == 1,
                    onClick = { selectedModeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Kamera (Guru)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedModeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedModeTab == 0) {
            // --- MODE 0: DISPLAY PRAYER QR CODE & MANUAL CODE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrownContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = BrownPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Setiap waktu sholat (Dhuha, Dzuhur, Ashar) memiliki QR dan kode manual 4-digit terpisah agar verifikasi presensi tidak tertukar.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SESI WAKTU SHOLAT YANG DIBUKA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrayerType.entries.forEach { prayer ->
                    FilterChip(
                        selected = selectedPrayer == prayer,
                        onClick = { selectedPrayer = prayer },
                        label = {
                            Text(
                                text = prayer.prayerName.replace("Sholat ", ""),
                                fontSize = 12.sp,
                                fontWeight = if (selectedPrayer == prayer) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_prayer_tab_${prayer.code}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrownPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Display Card (High-contrast Black-on-White QR for instant camera detection)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(2.dp, BrownPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrownDark
                    ) {
                        Text(
                            text = "MASJID DARUL ULUM SMKN 1 PANGKAJENE DAN KEPULAUAN",
                            color = AmberAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedPrayer.prayerName.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrownDark
                    )

                    if (selectedPrayer.isMandatoryForGoHome) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "★ WAJIB & SYARAT IZIN PULANG ★",
                                color = BrownDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProceduralQrView(
                            qrContent = displayedQr.tokenCode,
                            darkColor = Color.Black,
                            lightColor = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Prominent 4-digit Short Entry Code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrownContainer,
                        border = BorderStroke(1.5.dp, BrownPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "KODE SESI MANUAL (${selectedPrayer.prayerName.uppercase()}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrownMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = displayedQr.shortEntryCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = BrownDark,
                                letterSpacing = 4.sp,
                                modifier = Modifier.testTag("qr_manual_code_display")
                            )
                            Text(
                                text = "(Masukkan kode ini di HP jika kamera tidak terbaca)",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.generateNewQr(selectedPrayer) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("refresh_qr_token_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acak & Perbarui Sesi ${selectedPrayer.prayerName}", fontWeight = FontWeight.Bold)
            }
        } else {
            // --- MODE 1: CAMERA SCANNER FOR GURU (VERIFY QR OR STUDENT NISN) ---
            TeacherCameraScannerSection(viewModel = viewModel)
        }
    }
}

@Composable
private fun TeacherCameraScannerSection(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }
    var isScanningActive by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var scannedPayload by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDeniedOnce = true
        } else {
            cameraError = null
            isScanningActive = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ScanGreenDark),
        border = BorderStroke(2.dp, ScanGreenBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KAMERA SCANNER GURU / PETUGAS",
                color = ScanGreenBorder,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Arahkan kamera ke QR Code Sesi Sholat untuk verifikasi, atau pindai QR/NISN siswa.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 270.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission && cameraError == null) {
                    CameraQrScannerView(
                        isScanningActive = isScanningActive,
                        onQrCodeDetected = { decoded ->
                            isScanningActive = false
                            scannedPayload = decoded
                        },
                        onCameraError = { err -> cameraError = err },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(2.dp, ScanGreenBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Izin Kamera",
                            tint = ScanGreenBorder,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cameraError ?: "Izinkan akses kamera untuk memindai barcode.",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                cameraError = null
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary)
                        ) {
                            Text("Aktifkan Kamera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (permissionDeniedOnce) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Text("Buka Pengaturan", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (hasCameraPermission && cameraError == null && !isScanningActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scannedPayload = null
                        isScanningActive = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Barcode Lagi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    scannedPayload?.let { payload ->
        Spacer(modifier = Modifier.height(14.dp))
        val parts = payload.trim().split("|")
        val matchedStudent = viewModel.repository.allStudents.find {
            it.nisnOrNip.equals(payload.trim(), ignoreCase = true)
        }
        val (title, detail, isPositive) = when {
            parts.size >= 5 && parts[0] == "SMKN1PKP" && parts[1] == "SHOLAT" -> {
                val pType = PrayerType.entries.find { it.code == parts[2] }
                val date = parts[3]
                val isToday = date == viewModel.repository.getTodayDateString()
                if (pType != null && isToday) {
                    Triple(
                        "QR SESI SHOLAT VALID",
                        "Barcode aktif untuk ${pType.prayerName} (Tanggal $date). Siap dipindai oleh siswa.",
                        true
                    )
                } else {
                    Triple(
                        "QR SHOLAT KADALUARSA",
                        "QR ini untuk tanggal $date dan sudah tidak berlaku hari ini.",
                        false
                    )
                }
            }
            matchedStudent != null -> {
                val eligible = viewModel.repository.isGoHomeEligibleToday(matchedStudent.nisnOrNip)
                val dzuhurDone = viewModel.repository.isPrayerCompletedToday(matchedStudent.nisnOrNip, PrayerType.DZUHUR)
                val asharDone = viewModel.repository.isPrayerCompletedToday(matchedStudent.nisnOrNip, PrayerType.ASHAR)
                val statusLabel = if (eligible) "IZIN PULANG AKTIF (TIKET HIJAU)" else "BELUM BOLEH PULANG"
                Triple(
                    "${matchedStudent.fullName} • $statusLabel",
                    "NISN: ${matchedStudent.nisnOrNip} • Dzuhur: ${if (dzuhurDone) "✓" else "Belum"} • Ashar: ${if (asharDone) "✓" else "Belum"}",
                    eligible
                )
            }
            else -> {
                Triple("HASIL PINDAI BARCODE", "Isi QR: $payload", true)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            border = BorderStroke(1.5.dp, if (isPositive) TicketGreenPass else TicketRedBlock)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isPositive) TicketGreenPass else TicketRedBlock,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isPositive) TicketGreenPass else TicketRedBlock
                    )
                    Text(
                        text = detail,
                        fontSize = 11.5.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
