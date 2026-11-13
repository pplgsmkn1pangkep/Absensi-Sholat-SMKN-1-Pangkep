package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Gender
import com.example.model.Jurusan
import com.example.model.Role
import com.example.model.UserProfile
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BrownContainer
import com.example.ui.theme.BrownDark
import com.example.ui.theme.BrownMedium
import com.example.ui.theme.BrownPrimary
import com.example.ui.theme.CreamCardBg
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.CreamPillActive
import com.example.ui.theme.ScanGreenPrimary
import com.example.ui.theme.TicketRedBlock
import com.example.ui.theme.TicketRedBlockBg
import com.example.viewmodel.AttendanceViewModel

@Composable
fun AdminUserManagementScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allUsers by viewModel.repository.users.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Guru, 1: Siswa
    var searchQuery by remember { mutableStateOf("") }
    var selectedJurusanFilter by remember { mutableStateOf<Jurusan?>(null) }

    // Dialog state for adding a new teacher
    var showAddTeacherDialog by remember { mutableStateOf(false) }

    // Dialog state for resetting password
    var targetUserToReset by remember { mutableStateOf<UserProfile?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }

    // Dialog state for deleting a single user
    var targetUserToDelete by remember { mutableStateOf<UserProfile?>(null) }

    // Dialog state for bulk delete (all teachers or all students)
    var showBulkDeleteDialog by remember { mutableStateOf<Role?>(null) }

    val teachers = allUsers.filter { it.role == Role.ADMIN_GURU }
    val students = allUsers.filter { it.role == Role.SISWA }

    val filteredTeachers = teachers.filter {
        val q = searchQuery.trim().lowercase()
        q.isEmpty() || it.fullName.lowercase().contains(q) || it.nisnOrNip.lowercase().contains(q) ||
                (it.kelas?.lowercase()?.contains(q) == true) || (it.jurusan?.name?.lowercase()?.contains(q) == true)
    }

    val filteredStudents = students.filter {
        val q = searchQuery.trim().lowercase()
        val matchesSearch = q.isEmpty() || it.fullName.lowercase().contains(q) || it.nisnOrNip.lowercase().contains(q) ||
                (it.kelas?.lowercase()?.contains(q) == true)
        val matchesJurusan = selectedJurusanFilter == null || it.jurusan == selectedJurusanFilter
        matchesSearch && matchesJurusan
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AttendanceViewModel.Screen.DASHBOARD) },
                modifier = Modifier.testTag("admin_users_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Manajemen Akun & Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownDark
                )
                Text(
                    text = "Hak Akses: Admin Pusat (Kepsek & Operator)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Add Teacher Button in Top Bar
            Button(
                onClick = { showAddTeacherDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("top_add_teacher_button")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Guru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Role Tabs (Akun Guru / Akun Siswa)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CreamCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CreamCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrownPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Akun Guru (${teachers.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (selectedTab == 0) BrownDark else Color(0xFF6B584E),
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = if (selectedTab == 0) BrownPrimary else Color(0xFF6B584E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Akun Siswa (${students.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (selectedTab == 1) BrownDark else Color(0xFF6B584E),
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selectedTab == 1) BrownPrimary else Color(0xFF6B584E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (selectedTab == 0) "Cari nama guru, NIP, atau tugas..." else "Cari nama siswa, NISN, atau kelas...",
                    fontSize = 12.sp
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_user_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrownPrimary,
                focusedLabelColor = BrownPrimary
            )
        )

        // Jurusan filter chips for students
        if (selectedTab == 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedJurusanFilter == null,
                    onClick = { selectedJurusanFilter = null },
                    label = { Text("Semua Jurusan", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrownPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                Jurusan.entries.forEach { j ->
                    FilterChip(
                        selected = selectedJurusanFilter == j,
                        onClick = { selectedJurusanFilter = j },
                        label = { Text(j.code, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrownPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List Header & Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedTab == 0) "DAFTAR DEWAN GURU & PEMBINA" else "DAFTAR SISWA 7 JURUSAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrownDark,
                letterSpacing = 0.5.sp
            )
            if (selectedTab == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (teachers.isNotEmpty()) {
                        Text(
                            text = "Hapus Semua Guru",
                            color = TicketRedBlock,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { showBulkDeleteDialog = Role.ADMIN_GURU }
                                .padding(4.dp)
                                .testTag("bulk_delete_teachers_btn")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "+ Tambah Guru",
                        color = ScanGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { showAddTeacherDialog = true }
                            .padding(4.dp)
                            .testTag("add_teacher_header_btn")
                    )
                }
            } else {
                if (students.isNotEmpty()) {
                    Text(
                        text = "Hapus Semua Siswa",
                        color = TicketRedBlock,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { showBulkDeleteDialog = Role.SISWA }
                            .padding(4.dp)
                            .testTag("bulk_delete_students_btn")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Users List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedTab == 0) {
                // Guru Items
                items(filteredTeachers, key = { it.id }) { teacher ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_item_${teacher.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = teacher.avatarEmoji, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = teacher.fullName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = BrownDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subText = "NIP/ID: ${teacher.nisnOrNip} • ${teacher.kelas ?: teacher.jurusan?.name ?: "Piket Masjid"}"
                                    Text(
                                        text = subText,
                                        fontSize = 11.sp,
                                        color = Color(0xFF5A4A42),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Password display pill
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrownContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrownMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sandi: ${teacher.password}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = BrownDark
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Reset Button
                                    Button(
                                        onClick = {
                                            targetUserToReset = teacher
                                            newPasswordInput = "guru"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp).testTag("reset_pass_btn_${teacher.id}")
                                    ) {
                                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Reset", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Hapus Button
                                    OutlinedButton(
                                        onClick = { targetUserToDelete = teacher },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TicketRedBlock),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, TicketRedBlock),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp).testTag("delete_teacher_btn_${teacher.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(13.dp), tint = TicketRedBlock)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Hapus", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TicketRedBlock)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Siswa Items
                items(filteredStudents, key = { it.id }) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_pass_item_${student.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CreamCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = student.avatarEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1B120C),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "NISN: ${student.nisnOrNip} • ${student.kelas ?: student.jurusan?.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF5A4A42),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrownContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrownMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sandi: ${student.password}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = BrownDark
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            targetUserToReset = student
                                            newPasswordInput = "123456"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp).testTag("reset_student_pass_btn_${student.id}")
                                    ) {
                                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Reset", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { targetUserToDelete = student },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TicketRedBlock),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, TicketRedBlock),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp).testTag("delete_student_btn_${student.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(13.dp), tint = TicketRedBlock)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Hapus", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TicketRedBlock)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG 1: TAMBAH AKUN GURU BARU ---
    if (showAddTeacherDialog) {
        var inputFullName by remember { mutableStateOf("") }
        var inputNip by remember { mutableStateOf("") }
        var inputGender by remember { mutableStateOf(Gender.LAKI_LAKI) }
        var inputJabatanType by remember { mutableIntStateOf(0) } // 0: Guru Agama/Piket, 1: Wali Kelas, 2: Kaprodi, 3: Guru Umum
        var inputSelectedJurusan by remember { mutableStateOf<Jurusan?>(Jurusan.PPLG) }
        var inputKelas by remember { mutableStateOf("XII PPLG 1") }
        var inputInitialPassword by remember { mutableStateOf("guru") }

        AlertDialog(
            onDismissRequest = { showAddTeacherDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = BrownPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Akun Guru Baru", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Admin Pusat dapat mendaftarkan akun Guru Agama, Wali Kelas, atau Kaprodi SMKN 1 Pangkajene dan Kepulauan.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = inputFullName,
                        onValueChange = { inputFullName = it },
                        label = { Text("Nama Lengkap Guru & Gelar") },
                        placeholder = { Text("Contoh: Ust. H. Abdullah, S.Pd.I") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_teacher_name")
                    )

                    OutlinedTextField(
                        value = inputNip,
                        onValueChange = { inputNip = it },
                        label = { Text("NIP atau Username Login") },
                        placeholder = { Text("Contoh: guru.abdullah atau 198205...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_teacher_nip")
                    )

                    // Gender Selector
                    Text("Jenis Kelamin:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = inputGender == Gender.LAKI_LAKI,
                                onClick = { inputGender = Gender.LAKI_LAKI },
                                colors = RadioButtonDefaults.colors(selectedColor = BrownPrimary)
                            )
                            Text("Laki-laki", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = inputGender == Gender.PEREMPUAN,
                                onClick = { inputGender = Gender.PEREMPUAN },
                                colors = RadioButtonDefaults.colors(selectedColor = BrownPrimary)
                            )
                            Text("Perempuan", fontSize = 12.sp)
                        }
                    }

                    // Jabatan / Peran
                    Text("Peran / Tugas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                    val jabatanOptions = listOf("Guru Agama / Piket Masjid", "Wali Kelas", "Kaprodi", "Guru Pengajar")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        jabatanOptions.forEachIndexed { idx, opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inputJabatanType = idx }
                            ) {
                                RadioButton(
                                    selected = inputJabatanType == idx,
                                    onClick = { inputJabatanType = idx },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrownPrimary)
                                )
                                Text(opt, fontSize = 12.sp)
                            }
                        }
                    }

                    // Jurusan Selector
                    Text("Pilih Jurusan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = inputSelectedJurusan == null,
                            onClick = { inputSelectedJurusan = null },
                            label = { Text("Umum (Semua)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrownPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        Jurusan.entries.forEach { jur ->
                            FilterChip(
                                selected = inputSelectedJurusan == jur,
                                onClick = { inputSelectedJurusan = jur },
                                label = { Text(jur.code) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrownPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (inputJabatanType == 1) {
                        OutlinedTextField(
                            value = inputKelas,
                            onValueChange = { inputKelas = it },
                            label = { Text("Kelas yang Diwalikan") },
                            placeholder = { Text("Contoh: X PPLG 2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = inputInitialPassword,
                        onValueChange = { inputInitialPassword = it },
                        label = { Text("Kata Sandi Awal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kelasValue = when (inputJabatanType) {
                            0 -> "Piket Masjid SMKN 1"
                            1 -> inputKelas
                            2 -> "Kaprodi ${inputSelectedJurusan?.name ?: "Kejuruan"}"
                            else -> "Guru Pengajar"
                        }
                        val result = viewModel.addTeacher(
                            nipOrUsername = inputNip,
                            fullName = inputFullName,
                            gender = inputGender,
                            jurusan = inputSelectedJurusan,
                            kelas = kelasValue,
                            initialPassword = inputInitialPassword
                        )
                        result.onSuccess { teacher ->
                            Toast.makeText(context, "Berhasil! Akun Guru '${teacher.fullName}' telah ditambahkan.", Toast.LENGTH_LONG).show()
                            showAddTeacherDialog = false
                        }.onFailure { err ->
                            Toast.makeText(context, "Gagal: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenPrimary),
                    modifier = Modifier.testTag("submit_add_teacher_button")
                ) {
                    Text("Simpan Akun Guru", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTeacherDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // --- DIALOG 2: RESET PASSWORD PENGGUNA (GURU & SISWA) ---
    targetUserToReset?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { targetUserToReset = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = BrownPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Kata Sandi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrownDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Anda akan mereset kata sandi untuk:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrownContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = targetUser.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownDark)
                            Text(
                                text = "Role: ${targetUser.role.title.split(" ").first()} • ID/NISN: ${targetUser.nisnOrNip}",
                                fontSize = 11.sp,
                                color = Color(0xFF5A4A42)
                            )
                            Text(
                                text = "Sandi Saat Ini: ${targetUser.password}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = BrownMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Kata Sandi Baru") },
                        placeholder = { Text("Masukkan kata sandi baru...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reset_password_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrownPrimary,
                            focusedLabelColor = BrownPrimary
                        )
                    )

                    Text(
                        text = "Tips: Beritahukan kata sandi baru ini kepada yang bersangkutan agar dapat login kembali.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.resetPassword(targetUser.id, newPasswordInput)
                        if (success) {
                            Toast.makeText(
                                context,
                                "Kata sandi untuk '${targetUser.fullName}' berhasil direset menjadi: '$newPasswordInput'",
                                Toast.LENGTH_LONG
                            ).show()
                            targetUserToReset = null
                        } else {
                            Toast.makeText(context, "Gagal mereset kata sandi.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                    modifier = Modifier.testTag("confirm_reset_password_button")
                ) {
                    Text("Konfirmasi Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { targetUserToReset = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // --- DIALOG 3: KONFIRMASI HAPUS SATU AKUN ---
    if (targetUserToDelete != null) {
        val user = targetUserToDelete!!
        AlertDialog(
            onDismissRequest = { targetUserToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TicketRedBlock)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hapus Akun ${if (user.role == Role.SISWA) "Siswa" else "Guru"}?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrownDark
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus akun berikut dari sistem?",
                        fontSize = 13.sp,
                        color = BrownDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrownContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = user.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrownDark
                            )
                            Text(
                                text = "${if (user.role == Role.SISWA) "NISN" else "NIP/ID"}: ${user.nisnOrNip} • ${user.kelas ?: user.jurusan?.name ?: "Dewan Guru"}",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                    if (user.role == Role.SISWA) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Perhatian: Rekaman presensi dan riwayat laporan izin sholat siswa ini juga akan dibersihkan secara otomatis.",
                            fontSize = 11.sp,
                            color = TicketRedBlock,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = user.fullName
                        val success = viewModel.deleteUser(user.id)
                        targetUserToDelete = null
                        if (success) {
                            Toast.makeText(context, "Akun '$name' berhasil dihapus.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal menghapus akun.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TicketRedBlock),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_user_btn")
                ) {
                    Text("Ya, Hapus Akun", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { targetUserToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // --- DIALOG 4: KONFIRMASI HAPUS SEMUA AKUN (MASSAL) ---
    if (showBulkDeleteDialog != null) {
        val targetRole = showBulkDeleteDialog!!
        val roleLabel = if (targetRole == Role.SISWA) "Seluruh Siswa" else "Seluruh Guru"
        val count = if (targetRole == Role.SISWA) students.size else teachers.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = TicketRedBlock)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hapus $roleLabel?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TicketRedBlock
                    )
                }
            },
            text = {
                Text(
                    text = "Tindakan ini akan menghapus $count akun $roleLabel sekaligus beserta seluruh data presensi terkait. Akun Admin Pusat tetap aman.\n\nApakah Anda yakin ingin melanjutkan?",
                    fontSize = 13.sp,
                    color = BrownDark,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedCount = viewModel.deleteBulkUsers(targetRole)
                        showBulkDeleteDialog = null
                        Toast.makeText(context, "$deletedCount akun $roleLabel berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TicketRedBlock),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_bulk_delete_btn")
                ) {
                    Text("Ya, Hapus Semua ($count)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}
