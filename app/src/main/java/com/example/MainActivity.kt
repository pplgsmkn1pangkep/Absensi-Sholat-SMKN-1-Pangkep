package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Role
import com.example.ui.components.InstallAppDialog
import com.example.ui.screens.AdminUserManagementScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GateInspectionScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonitoringScreen
import com.example.ui.screens.PeriodReportScreen
import com.example.ui.screens.QrGeneratorScreen
import com.example.ui.screens.ScanQrScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamPillActive
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: AttendanceViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showUserSwitcherSheet by remember { mutableStateOf(false) }
    var showInstallAppDialog by remember { mutableStateOf(false) }

    // If not logged in, render the Login Screen
    if (currentUser == null || currentScreen == AttendanceViewModel.Screen.LOGIN) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val loggedInUser = currentUser!!

    // Back handling to return to Dashboard
    BackHandler(enabled = currentScreen != AttendanceViewModel.Screen.DASHBOARD) {
        viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            // Aesthetic Top Strip with Active Account and Quick Switch / Logout (Safe Insets)
            Surface(
                color = BrownDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile info (account switcher only allowed for Admin Pusat)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (loggedInUser.role == Role.ADMIN_PUSAT) {
                                    Modifier.clickable { showUserSwitcherSheet = true }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = 2.dp)
                            .testTag("open_role_switcher_button")
                    ) {
                        Text(text = loggedInUser.avatarEmoji, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = loggedInUser.fullName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (loggedInUser.role == Role.ADMIN_PUSAT) {
                                    "Admin Pusat • Ganti Akun ▾"
                                } else {
                                    "${loggedInUser.role.title.split(" ").first()} • ${loggedInUser.nisnOrNip}"
                                },
                                color = AmberAccent,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pasang di HP Button (Compact badge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2E7D32))
                                .clickable { showInstallAppDialog = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("btn_install_app_topbar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Pasang di HP",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Pasang HP",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Logout button
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Keluar",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CreamCardBg,
                tonalElevation = 2.dp
            ) {
                // 1. Dashboard (Jadwal Sholat)
                NavigationBarItem(
                    selected = currentScreen == AttendanceViewModel.Screen.DASHBOARD,
                    onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD) },
                    icon = {
                        Icon(
                            Icons.Default.Mosque,
                            contentDescription = "Beranda",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Jadwal",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (currentScreen == AttendanceViewModel.Screen.DASHBOARD) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrownDark,
                        selectedTextColor = BrownDark,
                        unselectedIconColor = Color(0xFF5A4A42),
                        unselectedTextColor = Color(0xFF5A4A42),
                        indicatorColor = CreamPillActive
                    ),
                    modifier = Modifier.testTag("nav_bottom_dashboard")
                )

                // 2. Scan QR / Tampil QR
                if (loggedInUser.role == Role.SISWA) {
                    NavigationBarItem(
                        selected = currentScreen == AttendanceViewModel.Screen.SCAN_QR,
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.SCAN_QR) },
                        icon = {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Scan QR",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (currentScreen == AttendanceViewModel.Screen.SCAN_QR) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2E7D32),
                            selectedTextColor = Color(0xFF2E7D32),
                            unselectedIconColor = Color(0xFF5A4A42),
                            unselectedTextColor = Color(0xFF5A4A42),
                            indicatorColor = Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.testTag("nav_bottom_scan")
                    )
                } else {
                    NavigationBarItem(
                        selected = currentScreen == AttendanceViewModel.Screen.QR_GENERATOR,
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.QR_GENERATOR) },
                        icon = {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = "Tampil QR",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "QR Masjid",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (currentScreen == AttendanceViewModel.Screen.QR_GENERATOR) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrownDark,
                            selectedTextColor = BrownDark,
                            unselectedIconColor = Color(0xFF5A4A42),
                            unselectedTextColor = Color(0xFF5A4A42),
                            indicatorColor = CreamPillActive
                        ),
                        modifier = Modifier.testTag("nav_bottom_qr_gen")
                    )
                }

                // 3. Tampilan Gerbang (Khusus Siswa: profil & kartu hijau dirinya)
                if (loggedInUser.role == Role.SISWA) {
                    NavigationBarItem(
                        selected = currentScreen == AttendanceViewModel.Screen.GATE_INSPECTION,
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.GATE_INSPECTION) },
                        icon = {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = "Kartu Gerbang",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Gerbang",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (currentScreen == AttendanceViewModel.Screen.GATE_INSPECTION) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrownDark,
                            selectedTextColor = BrownDark,
                            unselectedIconColor = Color(0xFF5A4A42),
                            unselectedTextColor = Color(0xFF5A4A42),
                            indicatorColor = CreamPillActive
                        ),
                        modifier = Modifier.testTag("nav_bottom_gate")
                    )
                }

                // 4. Monitoring (Admin Guru & Admin Pusat)
                if (loggedInUser.role != Role.SISWA) {
                    NavigationBarItem(
                        selected = currentScreen == AttendanceViewModel.Screen.MONITORING_REKAP,
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.MONITORING_REKAP) },
                        icon = {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = "Rekap",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Monitoring",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (currentScreen == AttendanceViewModel.Screen.MONITORING_REKAP) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrownDark,
                            selectedTextColor = BrownDark,
                            unselectedIconColor = Color(0xFF5A4A42),
                            unselectedTextColor = Color(0xFF5A4A42),
                            indicatorColor = CreamPillActive
                        ),
                        modifier = Modifier.testTag("nav_bottom_monitoring")
                    )
                }

                // 4.5 Kelola Akun (Khusus Admin Pusat)
                if (loggedInUser.role == Role.ADMIN_PUSAT) {
                    NavigationBarItem(
                        selected = currentScreen == AttendanceViewModel.Screen.ADMIN_USER_MANAGEMENT,
                        onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.ADMIN_USER_MANAGEMENT) },
                        icon = {
                            Icon(
                                Icons.Default.SupervisorAccount,
                                contentDescription = "Kelola Akun",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Kelola Akun",
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (currentScreen == AttendanceViewModel.Screen.ADMIN_USER_MANAGEMENT) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrownDark,
                            selectedTextColor = BrownDark,
                            unselectedIconColor = Color(0xFF5A4A42),
                            unselectedTextColor = Color(0xFF5A4A42),
                            indicatorColor = CreamPillActive
                        ),
                        modifier = Modifier.testTag("nav_bottom_admin_users")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                AttendanceViewModel.Screen.LOGIN -> LoginScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.SCAN_QR -> ScanQrScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.QR_GENERATOR -> QrGeneratorScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.MONITORING_REKAP -> MonitoringScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.GATE_INSPECTION -> GateInspectionScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.PERIOD_REPORT -> PeriodReportScreen(viewModel = viewModel)
                AttendanceViewModel.Screen.ADMIN_USER_MANAGEMENT -> AdminUserManagementScreen(viewModel = viewModel)
            }
        }

        // Quick Switcher Sheet between Admin Pusat, Admin Guru, and Siswa accounts
        if (showUserSwitcherSheet) {
            ModalBottomSheet(
                onDismissRequest = { showUserSwitcherSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "GANTI AKUN CEPAT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownPrimary
                    )
                    Text(
                        text = "Pilih kategori pengguna: Siswa, Admin Guru, atau Admin Pusat",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 1. Admin Pusat
                    Text(
                        text = "ADMIN PUSAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownDark
                    )
                    viewModel.repository.allUsers.filter { it.role == Role.ADMIN_PUSAT }.forEach { user ->
                        val isSelected = user.id == loggedInUser.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    viewModel.switchUserDirectly(user)
                                    showUserSwitcherSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrownContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrownPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = user.avatarEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownDark)
                                    Text(text = "Admin Pusat • 7 Jurusan", fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrownPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Admin Guru
                    Text(
                        text = "ADMIN GURU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownDark
                    )
                    viewModel.repository.allUsers.filter { it.role == Role.ADMIN_GURU }.take(6).forEach { user ->
                        val isSelected = user.id == loggedInUser.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    viewModel.switchUserDirectly(user)
                                    showUserSwitcherSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrownContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrownPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = user.avatarEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownDark)
                                    Text(text = "Guru • ${user.kelas ?: "Piket Masjid"}", fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrownPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Siswa
                    Text(
                        text = "SISWA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrownDark
                    )
                    viewModel.repository.allStudents.take(2).forEach { user ->
                        val isSelected = user.id == loggedInUser.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    viewModel.switchUserDirectly(user)
                                    showUserSwitcherSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrownContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrownPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = user.avatarEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownDark)
                                    Text(text = "NISN: ${user.nisnOrNip} • ${user.kelas}", fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrownPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Dialog Pasang di HP (Unduh APK & QR Link)
        if (showInstallAppDialog) {
            InstallAppDialog(onDismissRequest = { showInstallAppDialog = false })
        }
    }
}
