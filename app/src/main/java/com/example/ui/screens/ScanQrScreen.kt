package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AttendanceRepository
import com.example.model.PrayerType
import com.example.ui.components.CameraQrScannerView
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BrownDark
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.ui.theme.TicketGreenPass
import com.example.ui.theme.TicketRedBlock
import com.example.viewmodel.AttendanceViewModel

@Composable
fun ScanQrScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var manualShortCodeInput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<AttendanceRepository.ScanResult?>(null) }
    var isScanningActive by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

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

    // Request camera permission automatically when entering the Scan screen
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Re-check camera permission when returning from App Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val grantedNow = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = grantedNow
                if (grantedNow) {
                    cameraError = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                modifier = Modifier.testTag("scan_back_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Absensi Sholat di Masjid",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Masjid Darul Ulum SMKN 1 Pangkajene dan Kepulauan",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Camera Scanner Card
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
                    text = "ARAHKAN KAMERA KE QR CODE DI MASJID",
                    color = ScanGreenBorder,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "QR Code ditampilkan oleh Guru Agama / Petugas Masjid setelah salam sholat.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real CameraX Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 270.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        hasCameraPermission && cameraError == null -> {
                            CameraQrScannerView(
                                isScanningActive = isScanningActive,
                                onQrCodeDetected = { decodedCode ->
                                    isScanningActive = false
                                    scanResult = viewModel.processAttendanceCode(decodedCode)
                                },
                                onCameraError = { errMsg ->
                                    cameraError = errMsg
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("camera_qr_preview")
                            )
                        }
                        !hasCameraPermission -> {
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
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Izin Kamera Diperlukan",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Izinkan akses kamera agar aplikasi dapat memindai barcode sholat di masjid.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("request_camera_permission_button")
                                ) {
                                    Text("Aktifkan Kamera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Buka Pengaturan Izin",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(2.dp, AmberAccent, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Kamera Tidak Tersedia",
                                    tint = AmberAccent,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = cameraError ?: "Kamera tidak dapat dibuka.",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        cameraError = null
                                        isScanningActive = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Coba Lagi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (hasCameraPermission && cameraError == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isScanningActive) {
                        Button(
                            onClick = {
                                scanResult = null
                                isScanningActive = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rescan_qr_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Lagi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Text(
                            text = "Posisikan kotak QR Code di dalam bingkai kamera secara otomatis.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Code Entry Card (Fallback if QR Camera is unreadable/broken)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = ScanGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kamera Bermasalah? Masukkan Kode Sesi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrownDark
                    )
                }

                Text(
                    text = "Ketik kode angka 4-digit yang tertera di bawah QR Code layar guru/masjid untuk validasi sesuai waktu sholat.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = manualShortCodeInput,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                manualShortCodeInput = input
                            }
                        },
                        placeholder = { Text("4 digit kode masjid", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_short_code_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )

                    Button(
                        onClick = {
                            if (manualShortCodeInput.isNotBlank()) {
                                scanResult = viewModel.processAttendanceCode(manualShortCodeInput)
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("validate_short_code_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary)
                    ) {
                        Text("Validasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan/Code Result Notification
        AnimatedVisibility(visible = scanResult != null) {
            scanResult?.let { res ->
                val (bgColor, borderColor, icon, title, desc) = when (res) {
                    is AttendanceRepository.ScanResult.Success -> {
                        val titleText = if (res.isGoHomeActivated) {
                            "ABSENSI ${res.prayerType.prayerName.uppercase()} BERHASIL & TIKET HIJAU AKTIF!"
                        } else {
                            "ABSENSI ${res.prayerType.prayerName.uppercase()} BERHASIL!"
                        }
                        val descText = when {
                            res.isGoHomeActivated ->
                                "Alhamdulillah! Sholat Dzuhur & Ashar hari ini lengkap. Tiket Izin Pulang aktif untuk ditunjukkan pada satpam di gerbang."
                            res.prayerType == PrayerType.DZUHUR && !res.isAsharCompleted ->
                                "Kehadiran Sholat Dzuhur tercatat. Lanjutkan Sholat Ashar berjamaah nanti untuk mengaktifkan Tiket Hijau Izin Pulang."
                            res.prayerType == PrayerType.ASHAR && !res.isDzuhurCompleted ->
                                "Kehadiran Sholat Ashar tercatat. Catatan: Sholat Dzuhur hari ini belum tercatat (syarat pulang wajib Dzuhur & Ashar)."
                            else ->
                                "Kehadiran Anda telah dicatat oleh sistem sekolah."
                        }
                        Tuple5(
                            Color(0xFFE8F5E9),
                            TicketGreenPass,
                            Icons.Default.CheckCircle,
                            titleText,
                            descText
                        )
                    }
                    is AttendanceRepository.ScanResult.AlreadyScanned -> {
                        Tuple5(
                            Color(0xFFFFF8E1),
                            AmberAccent,
                            Icons.Default.Info,
                            "SUDAH TERCATAT SEBELUMNYA",
                            "Anda sudah melakukan absensi untuk ${res.prayerType.prayerName} pada jam ${res.timeScanned}."
                        )
                    }
                    is AttendanceRepository.ScanResult.InvalidQr -> {
                        Tuple5(
                            Color(0xFFFFEBEE),
                            TicketRedBlock,
                            Icons.Default.Error,
                            "KODE TIDAK VALID ATAU KADALUARSA",
                            res.reason
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_result_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.5.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                fontSize = 13.sp
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
