package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.ui.components.InstallAppDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Gender
import com.example.model.Jurusan
import com.example.model.Role
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.ScanGreenBg
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenDark
import com.example.ui.theme.ScanGreenPrimary
import com.example.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("absensi_auth_prefs", Context.MODE_PRIVATE) }

    fun getRoleKey(roleIdx: Int): String = when (roleIdx) {
        0 -> "siswa"
        1 -> "kaprodi"
        2 -> "wali_kelas"
        3 -> "guru_bk"
        4 -> "guru_agama"
        else -> "admin"
    }

    // Restore last selected account type (0: Siswa, 1: Kaprodi, 2: Wali Kelas, 3: Guru BK, 4: Guru Agama, 5: Admin Pusat)
    val savedRoleIndex = remember { authPrefs.getInt("last_role_index", 0).coerceIn(0, 5) }
    var selectedRoleIndex by remember { mutableIntStateOf(savedRoleIndex) }

    val targetRole = when (selectedRoleIndex) {
        0 -> Role.SISWA
        5 -> Role.ADMIN_PUSAT
        else -> Role.ADMIN_GURU
    }

    // Sub-mode for Siswa: 0 = Masuk (Login), 1 = Daftar Akun Baru (Register)
    var isRegisterMode by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }

    // Remember Me State
    var rememberMe by remember {
        mutableStateOf(authPrefs.getBoolean("remember_${getRoleKey(selectedRoleIndex)}", true))
    }

    // Login Form State (pre-filled from saved credentials)
    val initialUser = remember(selectedRoleIndex) {
        if (authPrefs.getBoolean("remember_${getRoleKey(selectedRoleIndex)}", true)) {
            authPrefs.getString("user_${getRoleKey(selectedRoleIndex)}", "") ?: ""
        } else ""
    }
    val initialPass = remember(selectedRoleIndex) {
        if (authPrefs.getBoolean("remember_${getRoleKey(selectedRoleIndex)}", true)) {
            authPrefs.getString("pass_${getRoleKey(selectedRoleIndex)}", "") ?: ""
        } else ""
    }

    var usernameOrNisn by remember { mutableStateOf(initialUser) }
    var password by remember { mutableStateOf(initialPass) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Guru / Kaprodi qualification override on login
    var guruSelectedJurusan by remember { mutableStateOf<Jurusan?>(null) }
    var guruSelectedKelas by remember { mutableStateOf<String?>(null) }

    // Student Registration Form State
    var regFullName by remember { mutableStateOf("") }
    var regKelas by remember { mutableStateOf("") }
    var regSelectedJurusan by remember { mutableStateOf(Jurusan.PPLG) }
    var regGender by remember { mutableStateOf(Gender.LAKI_LAKI) }
    var regNisn by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var isJurusanDropdownExpanded by remember { mutableStateOf(false) }

    // Helper when role tab changes: loads stored credentials for the clicked role
    val onTabChanged: (Int) -> Unit = { index ->
        selectedRoleIndex = index
        isRegisterMode = false
        val roleKey = getRoleKey(index)
        val isRemembered = authPrefs.getBoolean("remember_$roleKey", true)
        rememberMe = isRemembered
        if (isRemembered) {
            usernameOrNisn = authPrefs.getString("user_$roleKey", "") ?: ""
            password = authPrefs.getString("pass_$roleKey", "") ?: ""
        } else {
            usernameOrNisn = ""
            password = ""
        }
        authPrefs.edit().putInt("last_role_index", index).apply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Aesthetic Header with Mosque Icon in Green (Requested)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ScanGreenPrimary, ScanGreenDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mosque,
                contentDescription = "Logo Masjid Darul Ulum",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nama Sekolah (Sesuai Permintaan)
        Text(
            text = "SMKN 1 PANGKAJENE DAN KEPULAUAN",
            color = ScanGreenPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.5.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center
        )

        // Sub Judul Aplikasi
        Text(
            text = "SIM SHOLAT",
            color = BrownDark,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        // Nama Mesjid (Sesuai Permintaan)
        Text(
            text = "MASJID DARUL ULUM",
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = "Sistem Informasi Monitoring Sholat & Izin Pulang Siswa",
            color = Color.Gray,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Banner Pasang di HP (Unduh APK / Scan QR) - Responsive
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .clickable { showInstallDialog = true }
                .testTag("btn_install_app_login_banner"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ScanGreenBg),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, ScanGreenBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ScanGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pasang Aplikasi di HP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = ScanGreenDark
                        )
                        Text(
                            text = "Scan barcode Google Drive untuk unduh & pasang APK di HP",
                            fontSize = 9.5.sp,
                            color = ScanGreenDark.copy(alpha = 0.85f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = ScanGreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Account Type Selector (Siswa, Kaprodi, Wali Kelas, Guru BK, Guru Agama, Admin Pusat)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CreamCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CreamCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            val accountOptions = listOf(
                Triple(0, "Siswa", Icons.Default.School),
                Triple(1, "Kaprodi", Icons.Default.AccountCircle),
                Triple(2, "Wali Kelas", Icons.Default.Badge),
                Triple(3, "Guru BK", Icons.Default.Person),
                Triple(4, "Guru Agama", Icons.Default.Mosque),
                Triple(5, "Admin Pusat", Icons.Default.SupervisorAccount)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Siswa, Kaprodi, Wali Kelas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountOptions.take(3).forEach { (idx, label, icon) ->
                        val isSelected = selectedRoleIndex == idx
                        val activeColor = if (idx == 0) ScanGreenPrimary else BrownPrimary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) activeColor else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeColor else CreamCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTabChanged(idx) }
                                .testTag("login_account_tab_$idx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else Color(0xFF6B584E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF6B584E),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                // Row 2: Guru BK, Guru Agama, Admin Pusat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountOptions.drop(3).forEach { (idx, label, icon) ->
                        val isSelected = selectedRoleIndex == idx
                        val activeColor = if (idx == 5) BrownDark else BrownPrimary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) activeColor else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeColor else CreamCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTabChanged(idx) }
                                .testTag("login_account_tab_$idx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else Color(0xFF6B584E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF6B584E),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Card (Login or Registration) - Responsive Max Width
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // If Siswa & in Register Mode
                if (selectedRoleIndex == 0 && isRegisterMode) {
                    // --- FORM PENDAFTARAN SISWA MANDIRI ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = ScanGreenPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pendaftaran Akun Siswa Baru",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ScanGreenDark
                        )
                    }

                    Text(
                        text = "Silakan lengkapi identitas Anda untuk membuat akun absensi sholat di Masjid Darul Ulum.",
                        fontSize = 11.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // 1. Nama Lengkap
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Nama Lengkap Siswa") },
                        placeholder = { Text("Contoh: Muhammad Fadhil") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ScanGreenPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_student_name_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Jurusan (Dropdown dengan 7 Pilihan Lengkap)
                    ExposedDropdownMenuBox(
                        expanded = isJurusanDropdownExpanded,
                        onExpandedChange = { isJurusanDropdownExpanded = !isJurusanDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = regSelectedJurusan.displayLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jurusan Siswa") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJurusanDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("reg_student_jurusan_dropdown"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScanGreenPrimary,
                                focusedLabelColor = ScanGreenPrimary
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isJurusanDropdownExpanded,
                            onDismissRequest = { isJurusanDropdownExpanded = false }
                        ) {
                            Jurusan.entries.forEach { jur ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = jur.displayLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                                        }
                                    },
                                    onClick = {
                                        regSelectedJurusan = jur
                                        if (regKelas !in jur.officialClasses) {
                                            regKelas = jur.officialClasses.firstOrNull() ?: ""
                                        }
                                        isJurusanDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Kelas (pilihan resmi sesuai jurusan + dapat diketik manual)
                    OutlinedTextField(
                        value = regKelas,
                        onValueChange = { regKelas = it },
                        label = { Text("Kelas") },
                        placeholder = { Text("Contoh: ${regSelectedJurusan.officialClasses.firstOrNull() ?: "X PPLG"}") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = ScanGreenPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_student_class_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        regSelectedJurusan.officialClasses.forEach { cls ->
                            FilterChip(
                                selected = regKelas.equals(cls, ignoreCase = true),
                                onClick = { regKelas = cls },
                                label = { Text(cls, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScanGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Jenis Kelamin (Laki-laki / Perempuan)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Jenis Kelamin:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { regGender = Gender.LAKI_LAKI }
                            ) {
                                RadioButton(
                                    selected = regGender == Gender.LAKI_LAKI,
                                    onClick = { regGender = Gender.LAKI_LAKI },
                                    colors = RadioButtonDefaults.colors(selectedColor = ScanGreenPrimary)
                                )
                                Text("Laki-laki 👦", fontSize = 13.sp, color = BrownDark)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { regGender = Gender.PEREMPUAN }
                            ) {
                                RadioButton(
                                    selected = regGender == Gender.PEREMPUAN,
                                    onClick = { regGender = Gender.PEREMPUAN },
                                    colors = RadioButtonDefaults.colors(selectedColor = ScanGreenPrimary)
                                )
                                Text("Perempuan 🧕", fontSize = 13.sp, color = BrownDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 5. NISN
                    OutlinedTextField(
                        value = regNisn,
                        onValueChange = { regNisn = it },
                        label = { Text("NISN (Nomor Induk Siswa Nasional)") },
                        placeholder = { Text("Contoh: 0061234567") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ScanGreenPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_student_nisn_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. Password
                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Kata Sandi Akun") },
                        placeholder = { Text("Minimal 4 karakter") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ScanGreenPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_student_password_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScanGreenPrimary,
                            focusedLabelColor = ScanGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tombol Submit Pendaftaran
                    Button(
                        onClick = {
                            if (regFullName.isBlank() || regKelas.isBlank() || regNisn.isBlank() || regPassword.isBlank()) {
                                Toast.makeText(context, "Harap lengkapi seluruh kolom formulir!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val result = viewModel.registerStudent(
                                fullName = regFullName,
                                kelas = regKelas,
                                jurusan = regSelectedJurusan,
                                gender = regGender,
                                nisn = regNisn,
                                password = regPassword
                            )
                            result.onSuccess { student ->
                                // Auto-save newly registered student credentials
                                authPrefs.edit()
                                    .putBoolean("remember_siswa", true)
                                    .putString("user_siswa", regNisn.trim())
                                    .putString("pass_siswa", regPassword.trim())
                                    .putInt("last_role_index", 0)
                                    .apply()
                                Toast.makeText(context, "Pendaftaran berhasil! Akun & sandi tersimpan otomatis. Selamat datang, ${student.fullName}", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Pendaftaran gagal: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_student_register_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daftar Akun Siswa Sekarang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch back to Login
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Sudah memiliki akun? ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "Masuk di sini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScanGreenPrimary,
                            modifier = Modifier
                                .clickable { isRegisterMode = false }
                                .padding(4.dp)
                        )
                    }
                } else {
                    // --- FORM LOGIN STANDAR ---
                    Text(
                        text = when (selectedRoleIndex) {
                            0 -> "Masuk Sebagai Siswa"
                            1 -> "Masuk Sebagai Kaprodi"
                            2 -> "Masuk Sebagai Wali Kelas"
                            3 -> "Masuk Sebagai Guru BK"
                            4 -> "Masuk Sebagai Guru Agama"
                            else -> "Masuk Sebagai Admin Pusat"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrownDark
                    )

                    Text(
                        text = when (selectedRoleIndex) {
                            0 -> "Gunakan NISN dan kata sandi Anda yang terdaftar"
                            1 -> "Pemantauan sholat siswa seluruh kelas pada jurusan yang diampu"
                            2 -> "Pemantauan sholat siswa pada jurusan & kelas binaan Anda"
                            3 -> "Pemantauan pembinaan & kedisiplinan sholat siswa"
                            4 -> "Pemantauan sholat berjamaah & verifikasi ibadah siswa"
                            else -> "Kontrol penuh seluruh akun, reset sandi & pemantauan 7 jurusan"
                        },
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = usernameOrNisn,
                        onValueChange = { usernameOrNisn = it },
                        label = { Text(if (selectedRoleIndex == 0) "NISN Siswa" else "NIP / Username") },
                        placeholder = { Text(if (selectedRoleIndex == 0) "Masukkan NISN..." else "Masukkan NIP/Username...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                            )
                        },
                        keyboardOptions = if (selectedRoleIndex == 0) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary,
                            focusedLabelColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        placeholder = { Text("Masukkan kata sandi...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                            )
                        },
                        trailingIcon = {
                            val icon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (isPasswordVisible) "Sembunyikan Sandi" else "Tampilkan Sandi",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary,
                            focusedLabelColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                        )
                    )

                    // Qualification Selector for Kaprodi, Wali Kelas, Guru BK, and Guru Agama
                    if (selectedRoleIndex in 1..4) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CreamCardBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CreamCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Kualifikasi Jurusan & Kelas Pemantauan:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrownDark
                                )
                                Text(
                                    text = when (selectedRoleIndex) {
                                        1 -> "Pilih jurusan/kelas yang Anda ampu sebagai Kaprodi (dikunci setelah masuk)"
                                        2 -> "Pilih jurusan & 1 kelas binaan Anda sebagai Wali Kelas (dikunci setelah masuk)"
                                        3 -> "Pilih jurusan/kelas siswa binaan Guru BK (opsional)"
                                        else -> "Pilih jurusan/kelas siswa yang Anda ampu sebagai Guru Agama (opsional)"
                                    },
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = guruSelectedJurusan == null,
                                        onClick = {
                                            guruSelectedJurusan = null
                                            guruSelectedKelas = null
                                        },
                                        label = { Text("Sesuai Akun", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrownPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    Jurusan.entries.forEach { jur ->
                                        FilterChip(
                                            selected = guruSelectedJurusan == jur,
                                            onClick = {
                                                guruSelectedJurusan = jur
                                                val classes = viewModel.repository.getKnownClassesForJurusan(jur)
                                                guruSelectedKelas = if (selectedRoleIndex == 2) classes.firstOrNull() else null
                                            },
                                            label = { Text(jur.code, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ScanGreenPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("login_guru_jurusan_${jur.code}")
                                        )
                                    }
                                }

                                if (guruSelectedJurusan != null) {
                                    val classesForJurusan = remember(guruSelectedJurusan) {
                                        viewModel.repository.getKnownClassesForJurusan(guruSelectedJurusan)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when (selectedRoleIndex) {
                                            2 -> "Pilih Kelas Binaan Wali Kelas (wajib pilih 1 kelas):"
                                            1 -> "Pilih Kelas Spesifik atau Semua Kelas Jurusan ${guruSelectedJurusan?.code}:"
                                            else -> "Pilih Kelas (opsional):"
                                        },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrownMedium
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (selectedRoleIndex != 2) {
                                            FilterChip(
                                                selected = guruSelectedKelas == null,
                                                onClick = { guruSelectedKelas = null },
                                                label = { Text("Semua Kelas ${guruSelectedJurusan?.code}", fontSize = 10.5.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrownPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                        classesForJurusan.forEach { cls ->
                                            FilterChip(
                                                selected = guruSelectedKelas == cls,
                                                onClick = { guruSelectedKelas = cls },
                                                label = { Text(cls, fontSize = 10.5.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrownPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Ingat Saya" Checkbox & Bersihkan Data Tersimpan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                                ),
                                modifier = Modifier.testTag("remember_me_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Simpan Akun & Kata Sandi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrownDark
                                )
                                Text(
                                    text = "Tidak perlu mengetik lagi saat login",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Tombol bersihkan data yang tersimpan
                        if (usernameOrNisn.isNotBlank() && password.isNotBlank()) {
                            Text(
                                text = "Bersihkan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier
                                    .clickable {
                                        usernameOrNisn = ""
                                        password = ""
                                        val roleKey = getRoleKey(selectedRoleIndex)
                                        authPrefs.edit()
                                            .remove("user_$roleKey")
                                            .remove("pass_$roleKey")
                                            .apply()
                                        Toast.makeText(context, "Data tersimpan dibersihkan.", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("clear_saved_login_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isLoggingIn) return@Button
                            if (usernameOrNisn.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Silakan isi NISN/Username dan Kata Sandi.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedRoleIndex == 2 && guruSelectedJurusan != null && guruSelectedKelas.isNullOrBlank()) {
                                Toast.makeText(context, "Wali Kelas wajib memilih 1 kelas binaan terlebih dahulu.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isStaffRole = selectedRoleIndex in 1..4
                            val jurusanOverride = if (isStaffRole) guruSelectedJurusan else null
                            val kelasOverride = if (isStaffRole && guruSelectedJurusan != null) guruSelectedKelas else null
                            val currentRoleIdx = selectedRoleIndex
                            val currentUserInput = usernameOrNisn.trim()
                            val currentPassInput = password.trim()
                            val shouldRemember = rememberMe
                            isLoggingIn = true

                            viewModel.login(
                                usernameOrNisn = currentUserInput,
                                role = targetRole,
                                passwordInput = currentPassInput,
                                selectedJurusanOverride = jurusanOverride,
                                selectedKelasOverride = kelasOverride
                            ) { isSuccess, message ->
                                isLoggingIn = false
                                if (isSuccess) {
                                    val roleKey = getRoleKey(currentRoleIdx)
                                    if (shouldRemember) {
                                        authPrefs.edit()
                                            .putBoolean("remember_$roleKey", true)
                                            .putString("user_$roleKey", currentUserInput)
                                            .putString("pass_$roleKey", currentPassInput)
                                            .putInt("last_role_index", currentRoleIdx)
                                            .apply()
                                    } else {
                                        authPrefs.edit()
                                            .putBoolean("remember_$roleKey", false)
                                            .remove("user_$roleKey")
                                            .remove("pass_$roleKey")
                                            .putInt("last_role_index", currentRoleIdx)
                                            .apply()
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isLoggingIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRoleIndex == 0) ScanGreenPrimary else BrownPrimary
                        )
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Memeriksa Akun...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Masuk Sekarang",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // For Siswa: Show Registration prompt
                    if (selectedRoleIndex == 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Belum punya akun siswa? ", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = "Daftar Akun Baru",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScanGreenPrimary,
                                modifier = Modifier
                                    .clickable { isRegisterMode = true }
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "(Akun Kaprodi, Wali Kelas, Guru BK & Guru Agama dikelola langsung oleh Admin Pusat)",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog Pasang di HP (Unduh APK & QR Link)
    if (showInstallDialog) {
        InstallAppDialog(onDismissRequest = { showInstallDialog = false })
    }
}
