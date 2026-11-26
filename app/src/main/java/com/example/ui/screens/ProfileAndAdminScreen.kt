package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AuditLogEntity
import com.example.data.BoshqaruvRepository
import com.example.data.OfflineQueueEntity
import com.example.data.TaskEntity
import com.example.model.AccentChoice
import com.example.model.AppLanguage
import com.example.model.AppSettingsState
import com.example.model.AppUser
import com.example.model.DepartmentInfo
import com.example.model.DocumentRecord
import com.example.model.IndicatorCatalogItem
import com.example.model.InvestmentProject
import com.example.model.ThemeMode
import com.example.model.UserPermissions
import com.example.model.UserRole
import com.example.ui.components.ExecutiveCivicKpiCard
import com.example.ui.components.GradientProgressBar
import com.example.ui.theme.*

@Composable
fun ProfileAndAdminScreen(
    currentUser: AppUser,
    isOffline: Boolean,
    offlineQueue: List<OfflineQueueEntity>,
    auditLogs: List<AuditLogEntity>,
    allUsers: List<AppUser>,
    allDepartments: List<DepartmentInfo>,
    documents: List<DocumentRecord>,
    indicators: List<IndicatorCatalogItem>,
    settings: AppSettingsState,
    tasks: List<TaskEntity> = emptyList(),
    investments: List<InvestmentProject> = emptyList(),
    employeesDirectoryMode: Boolean = false,
    initialAdminTab: Int? = null,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentChoice) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDocumentClick: (DocumentRecord) -> Unit,
    onOpenAiClick: () -> Unit,
    onRoleChangeClick: () -> Unit,
    onToggleOffline: () -> Unit,
    onSyncOfflineClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAddEmployeeClick: () -> Unit,
    onAddDepartmentClick: () -> Unit,
    onToggleBlockEmployee: (String) -> Unit,
    onToggleBlockDepartment: (String) -> Unit,
    onEditPermissionsClick: (AppUser) -> Unit,
    onResetPasswordClick: (AppUser) -> Unit,
    onEmployeeClick: (AppUser) -> Unit = {},
    onSaveRolePermissions: (UserRole, UserPermissions) -> Unit = { _, _ -> },
    onAddProjectClick: () -> Unit = {},
    lastGeneratedOtpHint: Pair<String, String>? = null,
    lastBackupVerification: String? = null,
    onRunBackupSnapshotTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var adminTab by remember(employeesDirectoryMode, initialAdminTab) {
        mutableStateOf(initialAdminTab ?: if (employeesDirectoryMode) 0 else 2)
    }
    var searchText by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("ALL") }
    var auditResultFilter by remember { mutableStateOf("ALL") }
    val isSuperAdmin = currentUser.role == UserRole.SUPER_ADMIN ||
        currentUser.role == UserRole.HOKIM ||
        currentUser.permissions.canManageStaff

    val filteredUsers = remember(allUsers, searchText, roleFilter) {
        allUsers.filter { user ->
            val matchesRole = when (roleFilter) {
                "ALL" -> true
                "LEADERS" -> user.role == UserRole.HOKIM || user.role == UserRole.ORINBOSAR || user.role == UserRole.BOLIM_RAHBARI || user.role == UserRole.SUPER_ADMIN
                "SUPERVISORS" -> user.role == UserRole.NAZORATCHI || user.role == UserRole.TAHLILCHI
                "EXECUTORS" -> user.role == UserRole.IJROCHI
                else -> true
            }
            val matchesQuery = if (searchText.isBlank()) true else {
                user.name.contains(searchText, true) ||
                    user.department.contains(searchText, true) ||
                    user.position.contains(searchText, true) ||
                    user.role.titleUz.contains(searchText, true)
            }
            matchesRole && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
    ) {
        if (!employeesDirectoryMode && initialAdminTab == null) {
            // User Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ExecutiveHeaderGradient, RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(HokimGold.copy(alpha = 0.2f))
                                    .border(2.dp, HokimGoldLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentUser.avatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser.avatarUri,
                                        contentDescription = currentUser.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo_1790585476502),
                                        contentDescription = currentUser.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentUser.position,
                                    fontSize = 11.sp,
                                    color = HokimGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentUser.role.titleUz} • ${currentUser.scope.territory}",
                                    fontSize = 10.sp,
                                    color = Slate200,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onRoleChangeClick,
                                modifier = Modifier.testTag("profile_role_switch_button")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Rolni almashtirish", tint = HokimGoldLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileStatBox("ERI Imzo", "Faol", Modifier.weight(1f))
                            ProfileStatBox("Bo'limlar", "${allDepartments.size} ta", Modifier.weight(1f))
                            ProfileStatBox("Xodimlar", "${allUsers.size} ta", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tel: ${currentUser.phone} • @${currentUser.login}",
                                    fontSize = 9.sp,
                                    color = Slate200,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Rahbar: ${currentUser.supervisorName}",
                                    fontSize = 9.sp,
                                    color = Slate300
                                )
                            }

                            TextButton(
                                onClick = onLogoutClick,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFCA5A5)),
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chiqish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Theme, Accent, Language Settings
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SHAXSIYLASHTIRISH VA INTERFEYS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    label = { Text(mode.labelUz, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${mode.name}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentChoice.entries.forEach { accent ->
                                val selected = settings.accentChoice == accent
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(accent.primaryColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onAccentChange(accent) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = accent.labelUz, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppLanguage.entries) { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (employeesDirectoryMode || adminTab == 0) {
            // Mockup 1#1 & 4#1 & 5: Employees Directory KPI Strip + Role Filters + Search + Add
            item {
                val activeCount = allUsers.count { !it.isBlocked }
                val blockedCount = allUsers.count { it.isBlocked }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        listOf(
                            Triple("Jami xodimlar", "${allUsers.size}", TelegramBlue),
                            Triple("Faol", "$activeCount", HokimEmerald),
                            Triple("Lavozimlar", "${UserRole.entries.size}", StatusPurple),
                            Triple("Bo'limlar", "${allDepartments.size}", HokimGold),
                            Triple("Bloklangan", "$blockedCount", StatusRed)
                        )
                    ) { (label, valStr, accent) ->
                        ExecutiveCivicKpiCard(
                            title = label,
                            value = valStr,
                            deltaText = "Zarafshon",
                            isPositiveTrend = accent != StatusRed,
                            icon = Icons.Default.Badge,
                            accentColor = accent,
                            onClick = { adminTab = 0 },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Xodim, lavozim yoki bo'lim...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TelegramBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    if (isSuperAdmin) {
                        Button(
                            onClick = onAddEmployeeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("rbac_add_employee_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Yangi xodim", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Qo'shish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        listOf(
                            "ALL" to "Barcha xodimlar",
                            "LEADERS" to "Rahbarlar",
                            "SUPERVISORS" to "Nazorat va tahlil",
                            "EXECUTORS" to "Ijrochilar"
                        )
                    ) { (key, label) ->
                        FilterChip(
                            selected = roleFilter == key,
                            onClick = { roleFilter = key },
                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0B1F44), Color(0xFF132B52), Color(0xFF281D4D))),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Tizim & Vakolatlar Nazorati (RBAC + Scope)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Faol xodimlar: ${allUsers.count { !it.isBlocked }} ta • Bloklangan: ${allUsers.count { it.isBlocked }} ta • Bo'limlar: ${allDepartments.size} ta",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                        Text(
                            text = "✓ Ruxsatlar tartibli: profil kartasiga bosib batafsil ko'ring.",
                            fontSize = 10.sp,
                            color = HokimEmeraldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // RBAC / Workspace Filter Tabs (Including Org Structure & Projects!)
        item {
            val tabItems = if (isSuperAdmin) {
                listOf(
                    0 to "Xodimlar (${allUsers.size})",
                    2 to "Vakolatlar matritsasi",
                    6 to "Tashkiliy tuzilma",
                    7 to "Loyihalar (${investments.size})",
                    1 to "Bo'limlar (${allDepartments.size})",
                    3 to "Hujjatlar (${documents.size})",
                    4 to "Ko'rsatkichlar",
                    5 to "Audit & ERI"
                )
            } else {
                listOf(
                    2 to "Mening Vakolatlarim",
                    6 to "Tashkiliy tuzilma",
                    7 to "Loyihalar (${investments.size})",
                    3 to "Hujjatlar (${documents.size})",
                    4 to "Ko'rsatkichlar"
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tabItems) { (idx, label) ->
                    val selected = adminTab == idx
                    Surface(
                        color = if (selected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { adminTab = idx }
                            .testTag("admin_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Tab 0: Employees Directory with Tap-to-Profile
        if (adminTab == 0 && isSuperAdmin) {
            if (lastGeneratedOtpHint != null) {
                item {
                    Surface(
                        color = HokimEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HokimEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = HokimEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Bir martalik kirish kodi (${lastGeneratedOtpHint.first})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HokimEmerald
                                )
                                Text(
                                    text = "Kod: ${lastGeneratedOtpHint.second} (ilk kirishda o'chiriladi)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            items(filteredUsers, key = { it.id }) { user ->
                EmployeeAdminCard(
                    user = user,
                    isCurrent = user.id == currentUser.id,
                    onClickProfile = { onEmployeeClick(user) },
                    onToggleBlock = { onToggleBlockEmployee(user.id) },
                    onEditPermissions = { onEditPermissionsClick(user) },
                    onResetPassword = { onResetPasswordClick(user) }
                )
            }
        }

        // Tab 1: Departments
        if (adminTab == 1 && isSuperAdmin) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TARKIBIY BO'LINMALAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = onAddDepartmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bo'lim Qo'shish", fontSize = 11.sp)
                    }
                }
            }
            items(allDepartments, key = { it.id }) { dept ->
                DepartmentAdminCard(dept = dept, onToggleBlock = { onToggleBlockDepartment(dept.id) })
            }
        }

        // Tab 2: Interactive Vakolatlar Matritsasi (Mockup 1#4 & 4#4)
        if (adminTab == 2 || (!isSuperAdmin && adminTab == 0)) {
            if (isSuperAdmin) {
                item {
                    PermissionsMatrixCard(
                        allUsers = allUsers,
                        onSaveRolePermissions = onSaveRolePermissions
                    )
                }

                items(filteredUsers, key = { "perm-${it.id}" }) { user ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onEmployeeClick(user) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${user.name} (${user.role.titleUz})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Scope: ${user.scope.territory}", fontSize = 10.sp, color = TelegramBlue)
                                }
                                Button(
                                    onClick = { onEditPermissionsClick(user) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Tahrirlash", fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            PermissionItemRow("Topshiriq yaratish", user.permissions.canCreateTask)
                            PermissionItemRow("Yakuniy tasdiqlash", user.permissions.canApproveReport)
                            PermissionItemRow("Qaytarish / Nazorat", user.permissions.canReworkReport)
                            PermissionItemRow("Xodim va RBAC boshqaruvi", user.permissions.canManageStaff)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SIZNING VAKOLATLARINGIZ (${currentUser.role.titleUz})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            PermissionItemRow("Topshiriq yaratish", currentUser.permissions.canCreateTask)
                            PermissionItemRow("O'z xodimlariga topshiriq berish", currentUser.permissions.canAssignToOwnStaff)
                            PermissionItemRow("Nazorat tekshiruvi", currentUser.permissions.canReviewEvidence)
                            PermissionItemRow("Yakuniy tasdiqlash", currentUser.permissions.canApproveReport)
                            PermissionItemRow("Qayta ishlashga yuborish", currentUser.permissions.canReworkReport)
                            PermissionItemRow("Topshiriqni asosli rad etish", currentUser.permissions.canRejectTask)
                        }
                    }
                }
            }
        }

        // Tab 6: Tashkiliy tuzilma (Mockup 1#5 & 4#5)
        if (adminTab == 6) {
            item {
                OrganizationalStructureCard(
                    allDepartments = allDepartments,
                    allUsers = allUsers,
                    canAddDepartment = isSuperAdmin,
                    onAddDepartmentClick = onAddDepartmentClick
                )
            }
        }

        // Tab 7: Loyihalar va dasturlar (Mockup 1#8)
        if (adminTab == 7) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Loyihalar va dasturlar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Faol loyihalar: ${investments.size} ta",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSuperAdmin || currentUser.permissions.canCreateTask) {
                        Button(
                            onClick = onAddProjectClick,
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_project_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loyiha qo'shish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(investments, key = { it.id }) { proj ->
                InvestmentProjectCard(proj = proj)
            }
        }

        // Tab 3: Documents Workspace
        if (adminTab == 3) {
            items(documents, key = { it.id }) { doc ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onDocumentClick(doc) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = TelegramBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${doc.category} • ${doc.docNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                            Text(doc.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${doc.date} • ${doc.status}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Ko'rish", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Tab 4: Indicators Catalog
        if (adminTab == 4) {
            items(indicators, key = { it.id }) { ind ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ind.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Surface(
                                color = if (ind.isHealthy) HokimEmerald.copy(alpha = 0.14f) else HokimGold.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${ind.currentValue} / ${ind.targetValue}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (ind.isHealthy) StatusGreen else HokimGold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Soha: ${ind.sector} • Mas'ul: ${ind.sourceOrg}", fontSize = 10.sp, color = TelegramBlue, fontWeight = FontWeight.SemiBold)
                        Text("Formula: ${ind.formula}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Tab 5: Security & ERI + Audit Logs + Backup Snapshot
        if (adminTab == 5) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("XAVFSIZLIK VA ERI SERTIFIKATI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TelegramBlue)
                        Text("• Parollar saqlanishi: Tuzlangan SHA-256 xesh (Ochiq parol ko'rsatilmaydi)", fontSize = 11.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                        Text("• Brute-force himoyasi: 5 xato urinishda 60s bloklash", fontSize = 11.sp)
                        Text("• ERI Kalit: ${currentUser.digitalSignatureId}", fontSize = 11.sp)
                        currentUser.activeDevices.forEach { dev ->
                            Text("• Qurilma: $dev", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (currentUser.permissions.canModifySystemSettings) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ZAXIRA NUSXA VA TIKLASH SINOVI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TelegramBlue)
                            if (!lastBackupVerification.isNullOrBlank()) {
                                Text(lastBackupVerification, fontSize = 10.sp, color = HokimEmerald, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onRunBackupSnapshotTest,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("backup_snapshot_button")
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Zaxira nusxa va tiklash sinovini bajarish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ALL" to "Barchasi", "SUCCESS" to "Muvaffaqiyatli", "DENIED" to "Rad etilgan", "FAILED" to "Xato urinish")) { (key, label) ->
                        FilterChip(
                            selected = auditResultFilter == key,
                            onClick = { auditResultFilter = key },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
            val visibleLogs = auditLogs.filter { auditResultFilter == "ALL" || it.result == auditResultFilter }
            items(visibleLogs.take(20), key = { "sec-audit-${it.id}" }) { log ->
                val resColor = when (log.result) {
                    "DENIED", "FAILED" -> StatusRed
                    else -> HokimEmerald
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue, modifier = Modifier.weight(1f))
                            Surface(
                                color = resColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = log.result,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = resColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(BoshqaruvRepository.formatDate(log.timestamp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${log.action} → ${log.entity}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${log.oldValue} ➔ ${log.newValue}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (log.correlationId.isNotBlank()) {
                            Text("ID: ${log.correlationId} • ${log.reason}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (!employeesDirectoryMode && initialAdminTab == null) {
            // Offline Sync Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isOffline) StatusYellow else StatusGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Offline Rejim", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Switch(checked = isOffline, onCheckedChange = { onToggleOffline() })
                        }
                        Text(
                            text = if (isOffline) "Offline rejim faol. Navbat: ${offlineQueue.size} ta" else "Server bilan onlayn aloqa o'rnatilgan.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (offlineQueue.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onSyncOfflineClick,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sync Qilish (${offlineQueue.size})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsMatrixCard(
    allUsers: List<AppUser>,
    onSaveRolePermissions: (UserRole, UserPermissions) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.ORINBOSAR) }
    val basePerms = remember(selectedRole, allUsers) {
        allUsers.firstOrNull { it.role == selectedRole }?.permissions ?: when (selectedRole) {
            UserRole.HOKIM -> UserPermissions.HOKIM_PERMISSIONS
            UserRole.SUPER_ADMIN -> UserPermissions.SUPER_ADMIN_PERMISSIONS
            UserRole.ORINBOSAR -> UserPermissions.ORINBOSAR_PERMISSIONS
            UserRole.BOLIM_RAHBARI -> UserPermissions.BOLIM_RAHBARI_PERMISSIONS
            UserRole.IJROCHI -> UserPermissions.IJROCHI_PERMISSIONS
            UserRole.NAZORATCHI -> UserPermissions.NAZORATCHI_PERMISSIONS
            UserRole.TAHLILCHI -> UserPermissions.TAHLILCHI_PERMISSIONS
        }
    }

    var canView by remember(basePerms) { mutableStateOf(basePerms.canViewReports) }
    var canCreate by remember(basePerms) { mutableStateOf(basePerms.canCreateTask) }
    var canEdit by remember(basePerms) { mutableStateOf(basePerms.canAssignToOwnStaff) }
    var canApprove by remember(basePerms) { mutableStateOf(basePerms.canApproveReport) }
    var canManageStaff by remember(basePerms) { mutableStateOf(basePerms.canManageStaff) }
    var canUseAi by remember(basePerms) { mutableStateOf(basePerms.canUseAiAssistant) }
    var canExport by remember(basePerms) { mutableStateOf(basePerms.canExportReports) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vakolatlar matritsasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rollar bo'yicha modul va amallar ruxsati",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        onSaveRolePermissions(
                            selectedRole,
                            basePerms.copy(
                                canViewReports = canView,
                                canCreateTask = canCreate,
                                canAssignToOwnStaff = canEdit,
                                canApproveReport = canApprove,
                                canReviewEvidence = canApprove,
                                canManageStaff = canManageStaff,
                                canUseAiAssistant = canUseAi,
                                canExportReports = canExport
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("matrix_save_button")
                ) {
                    Text("Saqlash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(UserRole.entries) { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.titleUz, fontSize = 10.sp) }
                    )
                }
            }

            // Column headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Modul nomi", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                Text("Ko'rish", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f))
                Text("Yaratish", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f))
                Text("Tahrirlash", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f))
                Text("Tasdiq", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f))
            }

            MatrixRow("Topshiriqlar", canView, canCreate, canEdit, canApprove,
                onView = { canView = it }, onCreate = { canCreate = it }, onEdit = { canEdit = it }, onApprove = { canApprove = it })
            MatrixRow("Loyihalar", canView, canCreate, canEdit, canApprove,
                onView = { canView = it }, onCreate = { canCreate = it }, onEdit = { canEdit = it }, onApprove = { canApprove = it })
            MatrixRow("Hujjatlar", canView, canCreate, canEdit, canApprove,
                onView = { canView = it }, onCreate = { canCreate = it }, onEdit = { canEdit = it }, onApprove = { canApprove = it })
            MatrixRow("GIS xarita", canView, canUseAi, canUseAi, canApprove,
                onView = { canView = it }, onCreate = { canUseAi = it }, onEdit = { canUseAi = it }, onApprove = { canApprove = it })
            MatrixRow("Hisobotlar", canView, canExport, canExport, canApprove,
                onView = { canView = it }, onCreate = { canExport = it }, onEdit = { canExport = it }, onApprove = { canApprove = it })
            MatrixRow("Xodimlar", canView, canManageStaff, canManageStaff, canManageStaff,
                onView = { canView = it }, onCreate = { canManageStaff = it }, onEdit = { canManageStaff = it }, onApprove = { canManageStaff = it })
        }
    }
}

@Composable
private fun MatrixRow(
    moduleName: String,
    v: Boolean,
    c: Boolean,
    e: Boolean,
    a: Boolean,
    onView: (Boolean) -> Unit,
    onCreate: (Boolean) -> Unit,
    onEdit: (Boolean) -> Unit,
    onApprove: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(moduleName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
        Box(modifier = Modifier.weight(0.65f)) {
            Checkbox(checked = v, onCheckedChange = onView, modifier = Modifier.size(28.dp))
        }
        Box(modifier = Modifier.weight(0.65f)) {
            Checkbox(checked = c, onCheckedChange = onCreate, modifier = Modifier.size(28.dp))
        }
        Box(modifier = Modifier.weight(0.65f)) {
            Checkbox(checked = e, onCheckedChange = onEdit, modifier = Modifier.size(28.dp))
        }
        Box(modifier = Modifier.weight(0.65f)) {
            Checkbox(checked = a, onCheckedChange = onApprove, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun OrganizationalStructureCard(
    allDepartments: List<DepartmentInfo>,
    allUsers: List<AppUser>,
    canAddDepartment: Boolean,
    onAddDepartmentClick: () -> Unit
) {
    var selectedDept by remember(allDepartments) { mutableStateOf(allDepartments.firstOrNull()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tashkiliy tuzilma", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("Zarafshon shahar hokimligi iyerarxiyasi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canAddDepartment) {
                Button(
                    onClick = onAddDepartmentClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bo'lim qo'shish", fontSize = 11.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OrgTreeRow(Icons.Default.AccountBalance, "Zarafshon shahar hokimligi", 0, false, HokimNavy) {}
                OrgTreeRow(Icons.Default.VerifiedUser, "Hokim — Shukurov A. D.", 1, false, HokimGold) {}
                OrgTreeRow(Icons.Default.SupervisorAccount, "Hokim o'rinbosarlari (${allUsers.count { it.role == UserRole.ORINBOSAR }})", 1, false, TelegramBlue) {}
                OrgTreeRow(Icons.Default.AccountTree, "Boshqarma va bo'limlar (${allDepartments.size})", 1, false, HokimEmerald) {}

                allDepartments.forEach { dept ->
                    val isSelected = selectedDept?.id == dept.id
                    OrgTreeRow(
                        icon = Icons.Default.Apartment,
                        label = dept.name,
                        indentLevel = 2,
                        selected = isSelected,
                        accent = TelegramBlue,
                        onClick = { selectedDept = dept }
                    )
                }
                OrgTreeRow(Icons.Default.LocationCity, "Mahallalar (15 ta MFY)", 1, false, StatusPurple) {}
            }
        }

        selectedDept?.let { dept ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("BO'LIM MA'LUMOTLARI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TelegramBlue)
                    Text(dept.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rahbar: ${dept.headName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Xodimlar: ${dept.staffCount} nafar • Faol topshiriq: ${dept.activeTasksCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = HokimEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = dept.code,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = HokimEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Image(
                        painter = painterResource(id = R.drawable.zarafshon_banner_1790585492174),
                        contentDescription = dept.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun OrgTreeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    indentLevel: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) TelegramBlue.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 14).dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) TelegramBlue else accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected || indentLevel == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TelegramBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InvestmentProjectCard(proj: InvestmentProject) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = proj.imageResId),
                    contentDescription = proj.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = if (proj.isDelayed) StatusRed else HokimEmerald,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = proj.statusLabel,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(proj.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = "${proj.sector} • Byudjet: $${proj.budgetUsdMillion} mln • ${proj.jobsPlanned} ish o'rni",
                fontSize = 10.sp,
                color = TelegramBlue,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Pudratchi: ${proj.contractor}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradientProgressBar(
                progressPercent = proj.progressPercent,
                accentColors = if (proj.isDelayed) listOf(HokimGold, StatusRed) else listOf(TelegramBlue, HokimEmerald)
            )
        }
    }
}

@Composable
private fun ProfileStatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = Slate200, fontSize = 9.sp)
        }
    }
}

@Composable
private fun EmployeeAdminCard(
    user: AppUser,
    isCurrent: Boolean,
    onClickProfile: () -> Unit,
    onToggleBlock: () -> Unit,
    onEditPermissions: () -> Unit,
    onResetPassword: () -> Unit
) {
    val badgeColor = Color(user.role.badgeColorHex)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (user.isBlocked) StatusRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClickProfile() }
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (user.isBlocked) StatusRed.copy(alpha = 0.14f) else badgeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatarUri,
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = user.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString(""),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (user.isBlocked) StatusRed else badgeColor,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCurrent) "${user.name} (Siz)" else user.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = if (user.isBlocked) StatusRed.copy(alpha = 0.14f) else HokimEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (user.isBlocked) "Blok" else "Faol",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isBlocked) StatusRed else HokimEmerald,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.role.titleUz.substringBefore(" "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text("${user.position} • ${user.department}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "Scope: ${user.scope.territory} • @${user.login} • SHA-256 Xeshlangan${if (user.mustChangePassword) " • Yangilash kutilmoqda" else ""}",
                        fontSize = 9.sp,
                        color = TelegramBlue
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEditPermissions,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("Vakolatlar", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onResetPassword,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("Parol", fontSize = 10.sp)
                }
                if (!isCurrent) {
                    OutlinedButton(
                        onClick = onToggleBlock,
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (user.isBlocked) StatusGreen else StatusRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text(if (user.isBlocked) "Yoqish" else "Bloklash", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentAdminCard(
    dept: DepartmentInfo,
    onToggleBlock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (dept.isBlocked) StatusRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = TelegramBlue.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(dept.code, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TelegramBlue)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dept.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Rahbar: ${dept.headName} • ${dept.staffCount} xodim", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onToggleBlock) {
                Text(if (dept.isBlocked) "Faollashtirish" else "Cheklash", fontSize = 10.sp, color = if (dept.isBlocked) StatusGreen else StatusRed)
            }
        }
    }
}

@Composable
private fun PermissionItemRow(title: String, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) StatusGreen else StatusRed.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
