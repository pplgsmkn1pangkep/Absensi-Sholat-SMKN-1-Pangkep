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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
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
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary

// Official permanent AI Studio Shared Web App URL (accessible via browser without installing APK)
const val DEFAULT_WEB_ACCESS_URL =
    "https://ais-pre-b7rp7h5hpfe7fm7nuzsiuz-362569563212.asia-northeast1.run.app"

private const val PREFS_INSTALL = "install_apk_prefs"
private const val KEY_CUSTOM_APK_URL = "custom_apk_url"
private const val KEY_CUSTOM_WEB_URL = "custom_web_url"

@Composable
fun InstallAppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_INSTALL, Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Website & QR, 1: Barcode APK Permanen, 2: Izin HP & Aman
    var savedWebUrl by remember {
        mutableStateOf(
            prefs.getString(KEY_CUSTOM_WEB_URL, DEFAULT_WEB_ACCESS_URL)
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_WEB_ACCESS_URL
        )
    }
    var savedApkUrl by remember {
        mutableStateOf(prefs.getString(KEY_CUSTOM_APK_URL, "") ?: "")
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
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WEBSITE & INSTAL DI HP",
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
                            text = "Akses Website & Pasang APK",
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

                // Tab Switcher (3 Tabs)
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
                        modifier = Modifier.testTag("tab_web_access"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selectedTab == 0) ScanGreenPrimary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "1. Website",
                                    fontSize = 11.sp,
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
                        modifier = Modifier.testTag("tab_apk_barcode"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selectedTab == 1) ScanGreenPrimary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "2. APK Barcode",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_safety_guide"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selectedTab == 2) ScanGreenPrimary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "3. Izin HP",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedTab == 2) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    )
                }

                // Body Content based on Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            WebAccessQrView(
                                context = context,
                                webUrl = savedWebUrl,
                                onSaveWebUrl = { newUrl ->
                                    val normalized = newUrl.trim().ifEmpty { DEFAULT_WEB_ACCESS_URL }
                                    savedWebUrl = normalized
                                    prefs.edit().putString(KEY_CUSTOM_WEB_URL, normalized).apply()
                                    Toast.makeText(context, "Tautan website disimpan!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        1 -> {
                            PermanentApkBarcodeView(
                                context = context,
                                savedApkUrl = savedApkUrl,
                                onSaveApkUrl = { newUrl ->
                                    val clean = newUrl.trim()
                                    savedApkUrl = clean
                                    prefs.edit().putString(KEY_CUSTOM_APK_URL, clean).apply()
                                },
                                onSwitchToSafetyTab = { selectedTab = 2 }
                            )
                        }
                        else -> {
                            PhonePermissionSafetyGuideView(context = context)
                        }
                    }
                }

                // Bottom Action Buttons
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
                        Text(text = "Mengerti & Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebAccessQrView(
    context: Context,
    webUrl: String,
    onSaveWebUrl: (String) -> Unit
) {
    var isEditingUrl by remember { mutableStateOf(false) }
    var draftUrl by remember(webUrl) { mutableStateOf(webUrl) }

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
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Akses Browser / Website Aktif (Tanpa Expire)",
                color = Color(0xFF1B5E20),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Buka Aplikasi Lewat Website / Browser",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = BrownDark,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Scan barcode di bawah atau buka tautan ini di browser Chrome/Safari (HP, Laptop, maupun Komputer Sekolah) tanpa perlu menginstal file APK:",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Crisp Black-on-White QR for the Website Access URL
        Box(
            modifier = Modifier
                .size(196.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(2.dp, ScanGreenPrimary, RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            ProceduralQrView(
                qrContent = webUrl,
                modifier = Modifier.size(180.dp),
                matrixDimension = 512,
                darkColor = Color.Black,
                lightColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // URL Card & Actions
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
                        text = "Alamat Website Aplikasi:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownPrimary
                    )
                    Text(
                        text = if (isEditingUrl) "Tutup" else "Ubah URL",
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
                        value = draftUrl,
                        onValueChange = { draftUrl = it },
                        label = { Text("URL Website Aplikasi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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
                            text = "Reset Default",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable {
                                    draftUrl = DEFAULT_WEB_ACCESS_URL
                                    onSaveWebUrl(DEFAULT_WEB_ACCESS_URL)
                                    isEditingUrl = false
                                }
                                .padding(4.dp)
                        )
                        Button(
                            onClick = {
                                onSaveWebUrl(draftUrl)
                                isEditingUrl = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan URL", fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = webUrl,
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
                            val clip = ClipData.newPlainText("Link Website Aplikasi", webUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link website berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Salin Link", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Buka Web", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Akses Web Absensi Sholat SMKN 1 Pangkep")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Akses Aplikasi Absensi Sholat SMKN 1 Pangkep (Masjid Darul Ulum) langsung dari browser HP/Laptop melalui tautan berikut:\n$webUrl"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Link Website"))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Link Website", tint = ScanGreenPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note explaining Native Android vs Standalone HTML Web Portal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScanGreenBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ScanGreenBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Info Akses Website (Browser):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = ScanGreenDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Tautan di atas membuka aplikasi versi web streaming langsung dari browser HP atau Laptop tanpa perlu instalasi.\n" +
                        "• Data akun & absensi tetap tersinkronisasi melalui database Google Firebase yang sama.",
                    fontSize = 10.5.sp,
                    color = ScanGreenDark.copy(alpha = 0.9f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PermanentApkBarcodeView(
    context: Context,
    savedApkUrl: String,
    onSaveApkUrl: (String) -> Unit,
    onSwitchToSafetyTab: () -> Unit
) {
    var draftApkUrl by remember(savedApkUrl) { mutableStateOf(savedApkUrl) }
    val hasValidApkUrl = savedApkUrl.startsWith("http://", ignoreCase = true) ||
        savedApkUrl.startsWith("https://", ignoreCase = true)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Explanation why previous tmpfiles link expired & how permanent barcode works
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AmberAccent)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = BrownPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Solusi Agar Link APK Tidak Pernah Expire",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = BrownDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Server file sementara (seperti tmpfiles) otomatis menghapus file setelah 60 menit. Agar barcode instalasi APK berlaku selamanya untuk semua siswa, unduh file APK resmi dari menu AI Studio lalu simpan di Google Drive sekolah atau GitHub Releases, kemudian tempel tautannya di bawah.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF422C1A),
                        lineHeight = 14.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (hasValidApkUrl) {
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
                    text = "Barcode APK Permanen Aktif",
                    color = Color(0xFF1B5E20),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(196.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(2.dp, ScanGreenPrimary, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ProceduralQrView(
                    qrContent = savedApkUrl,
                    modifier = Modifier.size(180.dp),
                    matrixDimension = 512,
                    darkColor = Color.Black,
                    lightColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    Text("Buka Link APK", fontSize = 11.sp)
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Unduh APK Absensi Sholat SMKN 1 Pangkep")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Silakan unduh Aplikasi Absensi Sholat SMKN 1 Pangkep melalui tautan APK resmi berikut:\n$savedApkUrl"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Link APK"))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan Link APK", tint = ScanGreenPrimary)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CreamCardBg)
                    .border(1.5.dp, CreamCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = BrownPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Belum Ada Link APK Permanen Tersimpan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrownDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tempel tautan Google Drive / GitHub Release / Website Sekolah untuk file .apk di bawah agar QR Barcode langsung terbentuk.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input card to save permanent APK URL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCardBg),
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Tempel Link Permanen File APK (Google Drive / Server):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = draftApkUrl,
                    onValueChange = { draftApkUrl = it },
                    placeholder = { Text("https://drive.google.com/file/d/...", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = ScanGreenPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permanent_apk_url_field"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScanGreenPrimary,
                        focusedLabelColor = ScanGreenPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (savedApkUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                draftApkUrl = ""
                                onSaveApkUrl("")
                                Toast.makeText(context, "Link APK dihapus.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hapus", fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = {
                            val clean = draftApkUrl.trim()
                            if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) {
                                onSaveApkUrl(clean)
                                Toast.makeText(context, "Link APK permanen disimpan & Barcode dibuat!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Masukkan URL lengkap diawali https://", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_permanent_apk_url_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buat & Simpan Barcode APK", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step-by-step instructions to get permanent APK from AI Studio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScanGreenBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ScanGreenBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = ScanGreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cara Mendapat File APK Permanen dari AI Studio:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = ScanGreenDark
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Di halaman AI Studio (pojok kanan atas), klik ikon Settings / Titik Tiga (⋮) -> pilih 'Download APK / Build APK'.\n" +
                        "2. Unggah file .apk tersebut ke Google Drive sekolah (ubah akses menjadi 'Siapa saja yang memiliki link').\n" +
                        "3. Salin link Google Drive lalu tempel di kolom atas — Barcode APK akan aktif permanen tanpa pernah expire!",
                    fontSize = 10.5.sp,
                    color = ScanGreenDark.copy(alpha = 0.9f),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onSwitchToSafetyTab,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrownPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Lihat Panduan Izin Instalasi di HP (Play Protect)",
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
        // Assurance Banner
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
                        text = "✓ Bebas virus dan malware\n✓ Kamera hanya aktif saat memindai QR Code\n✓ Komunikasi cloud aman dengan enkripsi Google Firebase SSL",
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
                        text = "Mengapa HP Meminta Izin / Memberi Peringatan?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = BrownDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android otomatis memunculkan peringatan pada semua file APK di luar Google Play Store. Karena aplikasi ini didistribusikan mandiri untuk sekolah, sistem Play Protect meminta konfirmasi manual pengguna.",
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
            title = "Jika Muncul Google Play Protect (Layar Peringatan)",
            description = "1. Jangan klik 'Batal' atau 'Hapus'.\n" +
                "2. Klik tulisan 'Detail Selengkapnya' (More details) di bagian bawah.\n" +
                "3. Klik tombol 'Tetap Instal' / 'Install anyway'.",
            badgeText = "Paling Sering",
            badgeColor = AmberAccent
        )

        SecurityStepCard(
            stepNumber = "2",
            title = "Jika Diminta 'Izinkan dari Sumber Ini'",
            description = "1. Ketuk tombol 'Setelan' (Settings) yang muncul di layar.\n" +
                "2. Aktifkan sakelar 'Izinkan dari sumber ini' (Allow from this source).\n" +
                "3. Tekan Kembali (Back), lalu ketuk 'Instal'.",
            badgeText = "Sumber Tidak Dikenal",
            badgeColor = ScanGreenPrimary
        )

        SecurityStepCard(
            stepNumber = "3",
            title = "Khusus HP Xiaomi, Redmi, POCO & Realme",
            description = "1. Centang kotak 'Saya menyadari kemungkinan risiko...'\n" +
                "2. Tunggu hitungan mundur 10 detik lalu ketuk 'Oke'.",
            badgeText = "MIUI / HyperOS",
            badgeColor = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val guideText = "Panduan Memasang Aplikasi Absensi Sholat SMKN 1 Pangkep:\n\n" +
                "1. Pada peringatan Play Protect, klik 'Detail Selengkapnya' -> 'Tetap Instal'.\n" +
                "2. Jika diminta izin sumber, pilih 'Setelan' lalu aktifkan 'Izinkan dari sumber ini'.\n" +
                "3. Aplikasi 100% aman dan resmi untuk SMKN 1 Pangkep & Masjid Darul Ulum."

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Panduan Izin HP", guideText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Panduan izin berhasil disalin!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Salin Panduan", fontSize = 11.sp)
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
