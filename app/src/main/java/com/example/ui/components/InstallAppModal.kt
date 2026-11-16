package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary

// Permanent Google Drive APK download link provided by school admin
const val DEFAULT_APK_DOWNLOAD_URL =
    "https://drive.google.com/drive/folders/1NQ6-cXCH1wyW394sactIJXocMW4lC9uC?usp=sharing"

private const val PREFS_INSTALL = "install_apk_prefs"
private const val KEY_CUSTOM_APK_URL = "custom_apk_url_v5"

@Composable
fun InstallAppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_INSTALL, Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scan Barcode APK, 1: Panduan Izin HP
    var savedApkUrl by remember {
        mutableStateOf(
            prefs.getString(KEY_CUSTOM_APK_URL, DEFAULT_APK_DOWNLOAD_URL)
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_APK_DOWNLOAD_URL
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 14.dp, vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScanGreenDark, ScanGreenPrimary)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PASANG DI HP ANDROID",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("close_install_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Barcode Instalasi SIM SHOLAT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "SMKN 1 Pangkajene dan Kepulauan • Masjid Darul Ulum",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // 2-Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CreamCardBg,
                    contentColor = BrownDark,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ScanGreenPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_apk_barcode"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) ScanGreenPrimary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan Barcode APK",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_safety_guide"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) ScanGreenPrimary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Panduan Izin HP",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    )
                }

                // Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    if (selectedTab == 0) {
                        PermanentBarcodeInstallView(
                            context = context,
                            savedApkUrl = savedApkUrl,
                            onSaveApkUrl = { newUrl ->
                                val clean = newUrl.trim().ifEmpty { DEFAULT_APK_DOWNLOAD_URL }
                                savedApkUrl = clean
                                prefs.edit().putString(KEY_CUSTOM_APK_URL, clean).apply()
                            },
                            onOpenSafetyTab = { selectedTab = 1 }
                        )
                    } else {
                        PhonePermissionSafetyGuideView(context = context)
                    }
                }

                // Bottom Action Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dismiss_install_dialog_button")
                    ) {
                        Text(text = "Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermanentBarcodeInstallView(
    context: Context,
    savedApkUrl: String,
    onSaveApkUrl: (String) -> Unit,
    onOpenSafetyTab: () -> Unit
) {
    var isEditingUrl by remember { mutableStateOf(false) }
    var draftApkUrl by remember(savedApkUrl) { mutableStateOf(savedApkUrl) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Permanent Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE8F5E9))
                .border(1.dp, Color(0xFF81C784), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Barcode Permanen Aktif (Google Drive)",
                color = Color(0xFF1B5E20),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Scan Barcode untuk Unduh & Pasang",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = BrownDark,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Arahkan kamera HP / Google Lens ke QR Code di bawah ini untuk langsung membuka folder unduhan aplikasi:",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Crisp Black-on-White QR Barcode ready to scan immediately
        Box(
            modifier = Modifier
                .size(232.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(2.dp, ScanGreenPrimary, RoundedCornerShape(14.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            ProceduralQrView(
                qrContent = savedApkUrl,
                modifier = Modifier
                    .size(220.dp)
                    .testTag("install_apk_qr_code"),
                matrixDimension = 512,
                darkColor = Color.Black,
                lightColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCardBg),
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tautan Unduhan APK:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownPrimary
                    )
                    Text(
                        text = if (isEditingUrl) "Tutup Edit" else "Ganti Link",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScanGreenPrimary,
                        modifier = Modifier
                            .clickable { isEditingUrl = !isEditingUrl }
                            .padding(4.dp)
                    )
                }

                if (isEditingUrl) {
                    OutlinedTextField(
                        value = draftApkUrl,
                        onValueChange = { draftApkUrl = it },
                        placeholder = { Text("https://drive.google.com/...", fontSize = 11.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = ScanGreenPrimary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("permanent_apk_url_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reset ke Default",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable {
                                    draftApkUrl = DEFAULT_APK_DOWNLOAD_URL
                                    onSaveApkUrl(DEFAULT_APK_DOWNLOAD_URL)
                                    isEditingUrl = false
                                    Toast.makeText(context, "Kembali ke link Google Drive default.", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                        Button(
                            onClick = {
                                val clean = draftApkUrl.trim()
                                if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) {
                                    onSaveApkUrl(clean)
                                    isEditingUrl = false
                                    Toast.makeText(context, "Barcode diperbarui!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Masukkan URL lengkap diawali https://", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("save_permanent_apk_url_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = savedApkUrl,
                        fontSize = 10.5.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Link APK", savedApkUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Link", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(savedApkUrl)))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tautan tidak valid: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka Link", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Unduh Aplikasi SIM SHOLAT SMKN 1 Pangkep")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Silakan unduh dan pasang aplikasi SIM SHOLAT SMKN 1 Pangkep (Masjid Darul Ulum) melalui tautan Google Drive berikut:\n$savedApkUrl"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Link APK"))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Link", tint = ScanGreenPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step-by-step installation guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScanGreenBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ScanGreenBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Langkah Instalasi di HP Setelah Scan Barcode:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = ScanGreenDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Scan barcode di atas dengan Kamera HP atau Google Lens.\n" +
                        "2. Ketuk file aplikasi (.apk) di Google Drive lalu pilih Unduh / Download.\n" +
                        "3. Buka file yang selesai diunduh dan ketuk 'Pasang / Install'.\n" +
                        "4. Jika muncul peringatan Play Protect / Sumber Tidak Dikenal, buka tab 'Panduan Izin HP'.",
                    fontSize = 10.5.sp,
                    color = ScanGreenDark.copy(alpha = 0.9f),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenSafetyTab,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrownPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Panduan Jika HP Meminta Izin Pasang",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark
            )
        }
    }
}

@Composable
private fun PhonePermissionSafetyGuideView(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF81C784))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aplikasi 100% Aman & Terverifikasi",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Khusus Presensi Ibadah SMKN 1 Pangkajene & Kepulauan (Masjid Darul Ulum).",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Bebas virus dan malware\n✓ Kamera hanya aktif saat memindai QR Code\n✓ Data aman melalui enkripsi Google Firebase",
                        fontSize = 10.5.sp,
                        color = Color(0xFF1B5E20),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CreamCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrownPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mengapa HP Meminta Izin Saat Memasang?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = BrownDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android selalu memberi konfirmasi untuk aplikasi yang dipasang langsung lewat file APK (di luar Play Store). Cukup izinkan sekali saat pemasangan.",
                    fontSize = 10.5.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Langkah Mudah Mengaktifkan Izin di HP:",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = BrownDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecurityStepCard(
            stepNumber = "1",
            title = "Jika Muncul Google Play Protect",
            description = "1. Jangan klik 'Batal'.\n" +
                "2. Klik 'Detail Selengkapnya' (More details) di bagian bawah.\n" +
                "3. Klik 'Tetap Instal' (Install anyway).",
            badgeText = "Paling Sering",
            badgeColor = AmberAccent
        )

        SecurityStepCard(
            stepNumber = "2",
            title = "Jika Diminta 'Izinkan dari Sumber Ini'",
            description = "1. Ketuk 'Setelan' (Settings) di layar.\n" +
                "2. Aktifkan 'Izinkan dari sumber ini' (Allow from this source).\n" +
                "3. Tekan Kembali, lalu ketuk 'Instal'.",
            badgeText = "Sumber Tidak Dikenal",
            badgeColor = ScanGreenPrimary
        )

        SecurityStepCard(
            stepNumber = "3",
            title = "Khusus HP Xiaomi, Redmi, POCO & Realme",
            description = "1. Centang 'Saya menyadari kemungkinan risiko...'\n" +
                "2. Tunggu hitungan 10 detik lalu ketuk 'Oke'.",
            badgeText = "MIUI / HyperOS",
            badgeColor = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val guideText = "Panduan Memasang Aplikasi SIM SHOLAT SMKN 1 Pangkep:\n\n" +
                "1. Pada peringatan Play Protect, klik 'Detail Selengkapnya' -> 'Tetap Instal'.\n" +
                "2. Jika diminta izin sumber, pilih 'Setelan' lalu aktifkan 'Izinkan dari sumber ini'.\n" +
                "3. Aplikasi 100% aman dan resmi untuk SMKN 1 Pangkep & Masjid Darul Ulum."

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Panduan Izin HP", guideText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Panduan disalin!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Salin", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Panduan Izin Instalasi Absensi SMKN 1 Pangkep")
                        putExtra(Intent.EXTRA_TEXT, guideText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Kirim Panduan Izin"))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Kirim ke WA", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SecurityStepCard(
    stepNumber: String,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ScanGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = BrownDark
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 10.5.sp,
                color = Color(0xFF333333),
                lineHeight = 15.sp
            )
        }
    }
}
