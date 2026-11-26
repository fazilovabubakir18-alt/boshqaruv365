package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BoshqaruvRepository
import com.example.data.TaskEntity
import com.example.model.AppUser
import com.example.model.CityObjectFeedItem
import com.example.model.CityProblem
import com.example.model.CityStoryHighlight
import com.example.model.ProblemStatus
import com.example.model.UserRole
import com.example.ui.components.CityHealthGaugeChart
import com.example.ui.components.CityTrendsMultiSeriesChart
import com.example.ui.components.ExecutiveCivicKpiCard
import com.example.ui.components.GradientProgressBar
import com.example.ui.components.HorizontalBarComparisonChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.StoriesHighlightRow
import com.example.ui.components.TaskChainThreeWayRow
import com.example.ui.components.TripleKpiRingsCard
import com.example.ui.components.shortLabel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    tasks: List<TaskEntity>,
    currentUser: AppUser,
    currentRole: UserRole,
    allUsers: List<AppUser>,
    cityHealthScore: Int,
    stories: List<CityStoryHighlight>,
    problems: List<CityProblem>,
    cityFeed: List<CityObjectFeedItem>,
    diagonalTransfersCount: Int,
    aiAlertsCount: Int,
    departmentsCount: Int = 6,
    investmentsCount: Int = 3,
    onStoryClick: (CityStoryHighlight) -> Unit,
    onProblemClick: (CityProblem) -> Unit,
    onFeedVerifyClick: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    onApproveTaskDirect: (Long) -> Unit,
    onNewTaskClick: () -> Unit,
    onOpenAiClick: () -> Unit,
    onOpenBriefingClick: () -> Unit,
    onOpenAnalyticsClick: () -> Unit,
    onOpenGisClick: () -> Unit,
    onOpenDiagonalClick: () -> Unit,
    onOpenHealthEditorClick: () -> Unit,
    onOpenOrgStructureClick: () -> Unit = {},
    onOpenProjectsClick: () -> Unit = onOpenAnalyticsClick,
    onOpenEmployeesClick: () -> Unit = {},
    onOpenPermissionsClick: () -> Unit = {},
    onAddProjectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var liveMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd-MMMM, yyyy", Locale("uz")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale("uz")) }
    val currentDateStr = remember(liveMillis / 60000L) { dateFormat.format(Date(liveMillis)) }
    val currentDayStr = remember(liveMillis / 60000L) {
        dayFormat.format(Date(liveMillis)).replaceFirstChar { it.uppercase() }
    }
    val currentTimeStr = remember(liveMillis / 1000L) { timeFormat.format(Date(liveMillis)) }

    val now = liveMillis
    val accessibleTasks = remember(tasks, currentUser) {
        tasks.filter { BoshqaruvRepository.canUserAccessTask(currentUser, it) }
    }
    val totalCount = accessibleTasks.size
    val completedCount = accessibleTasks.count { it.status == "COMPLETED" }
    val inProgressCount = accessibleTasks.count { it.status == "IN_PROGRESS" || it.status == "ACCEPTED" }
    val riskCount = accessibleTasks.count {
        (it.deadline < now && it.status != "COMPLETED" && it.status != "ARCHIVED") ||
            it.cycleCount >= 3 ||
            it.riskLevel == "HIGH"
    }
    val openProblemsCount = problems.count { it.status == ProblemStatus.OCHIQ }
    val execPct = if (totalCount > 0) ((completedCount * 100 / totalCount).coerceIn(78, 98)) else 96

    var selectedChainTab by remember { mutableStateOf(0) }
    val userFirst = currentUser.name.substringBefore(" ")
    val chainFilteredTasks = remember(accessibleTasks, selectedChainTab, userFirst) {
        when (selectedChainTab) {
            0 -> accessibleTasks.filter {
                it.creatorName.contains(userFirst, ignoreCase = true) || currentRole == UserRole.HOKIM
            }
            1 -> accessibleTasks.filter {
                it.status == "SUBMITTED" || it.status == "REVIEW" || it.status == "REWORK" ||
                    it.reviewerName.contains(userFirst, ignoreCase = true)
            }
            2 -> accessibleTasks.filter {
                it.status == "IN_PROGRESS" || it.status == "ACCEPTED" || it.status == "ASSIGNED"
            }
            3 -> accessibleTasks.filter {
                it.coAssigneesRaw.isNotBlank() || it.observersRaw.isNotBlank() || it.sourceType.contains("Diagonal", true)
            }
            else -> accessibleTasks
        }
    }

    val deptRatings = remember {
        listOf(
            Triple("Shahar hokimligi apparati", 98, HokimEmerald),
            Triple("Obodonlashtirish boshqarmasi", 95, TelegramCyan),
            Triple("Yoshlar siyosati bo'limi", 92, TelegramBlue),
            Triple("Qurilish va arxitektura", 89, HokimGold),
            Triple("Zarafshon Suv Ta'minoti", 87, StatusPurple)
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
        ) {
            // 1. ZARAFSHON COMMAND CENTER PANORAMIC HERO BANNER (Mockup 3)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 128.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.zarafshon_banner_1790585492174),
                            contentDescription = "Zarafshon shahri panoramasi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xEE0B1F44),
                                            Color(0xCC132B52),
                                            Color(0xAA0B1F44)
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Boshqaruv 365",
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Zarafshon shahrini yagona raqamli platformada boshqarish tizimi",
                                    color = Slate200,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = currentRole.titleUz,
                                            color = HokimGoldLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = HokimEmerald.copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Shahar Sog'lig'i: $cityHealthScore%",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Live Clock Pill (Mockup 3)
                            Surface(
                                color = Color(0xCC091834),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = currentDateStr,
                                        color = HokimGoldLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentTimeStr,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = currentDayStr,
                                        color = Slate300,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. 5-CARD CIVIC KPI STRIP (Mockup 3)
            item {
                val kpiItems = listOf(
                    KpiStripSpec("Tashkilotlar", "$departmentsCount", "↑ +2%", true, Icons.Default.Apartment, TelegramBlue, onOpenOrgStructureClick),
                    KpiStripSpec("Xodimlar soni", "${allUsers.size}", "↑ +1%", true, Icons.Default.Groups, HokimEmerald, onOpenEmployeesClick),
                    KpiStripSpec("Topshiriqlar", "$totalCount", "↑ +5%", true, Icons.Default.AssignmentTurnedIn, StatusPurple) { onSelectTask(accessibleTasks.firstOrNull()?.id ?: 1L) },
                    KpiStripSpec("Loyihalar", "$investmentsCount", "↑ +8%", true, Icons.Default.FolderSpecial, HokimGold, onOpenProjectsClick),
                    KpiStripSpec("Muammolar", "$openProblemsCount", "↓ -3%", false, Icons.Default.ReportProblem, StatusRed, onOpenGisClick)
                )

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        kpiItems.forEach { kpi ->
                            ExecutiveCivicKpiCard(
                                title = kpi.title,
                                value = kpi.value,
                                deltaText = kpi.delta,
                                isPositiveTrend = kpi.positive,
                                icon = kpi.icon,
                                accentColor = kpi.color,
                                onClick = kpi.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(kpiItems) { kpi ->
                            ExecutiveCivicKpiCard(
                                title = kpi.title,
                                value = kpi.value,
                                deltaText = kpi.delta,
                                isPositiveTrend = kpi.positive,
                                icon = kpi.icon,
                                accentColor = kpi.color,
                                onClick = kpi.onClick,
                                modifier = Modifier.width(152.dp)
                            )
                        }
                    }
                }
            }

            // 3. FAOL FOYDALANUVCHILAR & ROLLARI
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "FAOL FOYDALANUVCHILAR & ROLLARI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(allUsers, key = { it.id }) { user ->
                            val ringColor = Color(user.role.badgeColorHex)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenEmployeesClick() }
                            ) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(ringColor.copy(alpha = 0.14f))
                                            .border(1.8.dp, ringColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.split(" ")
                                                .take(2)
                                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                                .joinToString(""),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(if (user.isBlocked) StatusRed else StatusGreen)
                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.name.substringBefore(" "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = user.role.shortLabel().substringBefore(" "),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. HOKIM BOSHQARUV MARKAZI + 2x2 SQUIRCLE QUICK ACTIONS
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        HokimHeroStatsCard(
                            currentRole = currentRole,
                            cityHealthScore = cityHealthScore,
                            totalCount = totalCount,
                            inProgressCount = inProgressCount,
                            riskCount = riskCount,
                            onOpenHealthEditorClick = onOpenHealthEditorClick,
                            onOpenBriefingClick = onOpenBriefingClick,
                            onOpenAiClick = onOpenAiClick,
                            modifier = Modifier.weight(1.1f)
                        )
                        TezkorAmallarPanel(
                            canCreateTask = currentUser.permissions.canCreateTask,
                            onNewTaskClick = onNewTaskClick,
                            onAddProjectClick = onAddProjectClick,
                            onOpenAnalyticsClick = onOpenAnalyticsClick,
                            onOpenOrgStructureClick = onOpenOrgStructureClick,
                            onOpenPermissionsClick = onOpenPermissionsClick,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        HokimHeroStatsCard(
                            currentRole = currentRole,
                            cityHealthScore = cityHealthScore,
                            totalCount = totalCount,
                            inProgressCount = inProgressCount,
                            riskCount = riskCount,
                            onOpenHealthEditorClick = onOpenHealthEditorClick,
                            onOpenBriefingClick = onOpenBriefingClick,
                            onOpenAiClick = onOpenAiClick
                        )

                        // 2x2 Squircle Tiles (Mockup 1 mobile)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SquircleActionTile(
                                title = "Yangi Topshiriq",
                                subtitle = if (currentUser.permissions.canCreateTask) {
                                    "Beruvchi, Nazoratchi, Ijrochi"
                                } else {
                                    "Faqat rahbar uchun"
                                },
                                icon = Icons.Default.NoteAdd,
                                gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                                badgeText = null,
                                enabled = currentUser.permissions.canCreateTask,
                                onClick = onNewTaskClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_new_task")
                            )

                            SquircleActionTile(
                                title = "Tahlil & Hisobotlar",
                                subtitle = "KPI va bo'limlar dinamikasi",
                                icon = Icons.Default.InsertChart,
                                gradientColors = listOf(Color(0xFF0D9488), Color(0xFF047857)),
                                badgeText = null,
                                enabled = true,
                                onClick = onOpenAnalyticsClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_analytics")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SquircleActionTile(
                                title = "Xarita 360° GIS",
                                subtitle = "Shahar obyektlari va GPS",
                                icon = Icons.Default.Map,
                                gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                                badgeText = null,
                                enabled = true,
                                onClick = onOpenGisClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_gis")
                            )

                            SquircleActionTile(
                                title = "AI Nazorat & Audit",
                                subtitle = "Kechikish radari va signallar",
                                icon = Icons.Default.Memory,
                                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                                badgeText = if (aiAlertsCount > 0) "$aiAlertsCount ta Signal" else null,
                                enabled = true,
                                onClick = onOpenAiClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_ai")
                            )
                        }

                        // Compact Mobile Shortcut Pills for Mockup Modules (Tuzilma, Loyihalar, Vakolatlar)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                ModuleQuickPill(Icons.Default.AccountTree, "Tashkiliy tuzilma", TelegramBlue, onOpenOrgStructureClick)
                            }
                            item {
                                ModuleQuickPill(Icons.Default.BusinessCenter, "Loyihalar ($investmentsCount)", HokimEmerald, onOpenProjectsClick)
                            }
                            item {
                                ModuleQuickPill(Icons.Default.AdminPanelSettings, "Vakolatlar matritsasi", StatusPurple, onOpenPermissionsClick)
                            }
                            item {
                                ModuleQuickPill(Icons.Default.Groups, "Xodimlar (${allUsers.size})", HokimGold, onOpenEmployeesClick)
                            }
                        }
                    }
                }
            }

            // 5. SHAHAR BO'YICHA ASOSIY KO'RSATKICHLAR + SHAHAR XARITASI PREVIEW (Mockup 3)
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CityTrendsCard(
                            onDetailsClick = onOpenAnalyticsClick,
                            modifier = Modifier.weight(1.2f)
                        )
                        GisPreviewCard(
                            openProblemsCount = openProblemsCount,
                            investmentsCount = investmentsCount,
                            onOpenGisClick = onOpenGisClick,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        CityTrendsCard(onDetailsClick = onOpenAnalyticsClick)
                        GisPreviewCard(
                            openProblemsCount = openProblemsCount,
                            investmentsCount = investmentsCount,
                            onOpenGisClick = onOpenGisClick
                        )
                    }
                }
            }

            // 6. KPI KO'RSATKICHLARI (3 RINGS) + TASHKILOTLAR REYTINGI (Mockup 3)
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TripleKpiRingsCard(
                            executionPct = execPct,
                            planPct = 94,
                            efficiencyPct = 92,
                            onDetailsClick = onOpenAnalyticsClick,
                            modifier = Modifier.weight(1f)
                        )
                        OrgRankingCard(
                            ratings = deptRatings,
                            onDetailsClick = onOpenAnalyticsClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        TripleKpiRingsCard(
                            executionPct = execPct,
                            planPct = 94,
                            efficiencyPct = 92,
                            onDetailsClick = onOpenAnalyticsClick
                        )
                        OrgRankingCard(
                            ratings = deptRatings,
                            onDetailsClick = onOpenAnalyticsClick
                        )
                    }
                }
            }

            // 7. TOPSHIRIQ ZANJIRI & NAZORAT Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = "TOPSHIRIQ ZANJIRI & NAZORAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tabs = listOf(
                            0 to "Berganlarim (${accessibleTasks.size})",
                            1 to "Nazoratda (${accessibleTasks.count { it.status == "SUBMITTED" || it.status == "REVIEW" || it.status == "REWORK" }})",
                            2 to "Bajarilayotgan ($inProgressCount)",
                            3 to "Diagonal ($diagonalTransfersCount)"
                        )
                        items(tabs) { (idx, label) ->
                            val active = selectedChainTab == idx
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (idx == 3 && chainFilteredTasks.isEmpty()) {
                                            onOpenDiagonalClick()
                                        } else {
                                            selectedChainTab = idx
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .testTag("chain_tab_$idx")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(if (active) 48.dp else 0.dp)
                                        .height(2.5.dp)
                                        .background(TelegramBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }

            items(chainFilteredTasks.take(3), key = { "chain-${it.id}" }) { task ->
                val canQuickApprove = (currentUser.permissions.canApproveReport || currentUser.permissions.canReviewEvidence) &&
                    (task.status == "SUBMITTED" || task.status == "REVIEW")

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (task.cycleCount >= 3) StatusRed.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectTask(task.id) }
                        .testTag("dashboard_chain_card_${task.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (task.cycleCount >= 3) {
                                Surface(
                                    color = StatusRed.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "${task.cycleCount}x qaytdi!",
                                        color = StatusRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        TaskChainThreeWayRow(
                            creatorName = task.creatorName,
                            reviewerName = task.reviewerName,
                            assigneeName = task.assigneeName
                        )

                        GradientProgressBar(
                            progressPercent = task.progressPercent,
                            accentColors = when {
                                task.cycleCount >= 3 -> listOf(StatusRed, HokimGold)
                                task.status == "COMPLETED" -> listOf(HokimEmerald, HokimEmeraldLight)
                                else -> listOf(TelegramBlue, HokimEmerald)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(status = task.status)
                            Text(
                                text = "Muddat: ${BoshqaruvRepository.formatDate(task.deadline)}",
                                fontSize = 10.sp,
                                color = if (task.deadline < now && task.status != "COMPLETED") StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChainQuickButton(
                                icon = Icons.Outlined.Visibility,
                                label = "Ko'rish",
                                accent = MaterialTheme.colorScheme.onSurface,
                                onClick = { onSelectTask(task.id) },
                                modifier = Modifier.weight(1f)
                            )
                            if (canQuickApprove) {
                                ChainQuickButton(
                                    icon = Icons.Default.Check,
                                    label = "Tasdiqlash",
                                    accent = HokimEmerald,
                                    onClick = { onApproveTaskDirect(task.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ChainQuickButton(
                                icon = Icons.Outlined.ModeComment,
                                label = "Izohlar",
                                accent = TelegramBlue,
                                onClick = { onSelectTask(task.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 8. DIAGONAL MA'LUMOT ALMASHINUVI SHORTCUT
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenDiagonalClick() }
                        .testTag("dashboard_diagonal_shortcut")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TelegramBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Diagonal ma'lumot almashinuvi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$diagonalTransfersCount ta bo'limlararo aloqa • AI muvofiqlashtirish",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ochish", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 9. SHAHAR 360° STORIES & MUAMMOLAR MARKAZI
            item {
                StoriesHighlightRow(
                    stories = stories,
                    onStoryClick = onStoryClick
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = StatusRed, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MUAMMOLAR MARKAZI ($openProblemsCount ochiq)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "1-bosishda topshiriq",
                            fontSize = 11.sp,
                            color = TelegramBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(problems, key = { it.id }) { problem ->
                            ProblemHorizontalCard(
                                problem = problem,
                                onClick = { onProblemClick(problem) }
                            )
                        }
                    }
                }
            }

            // 10. SHAHAR OBYEKTLARI LENTASI
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHAHAR OBYEKTLARI LENTASI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Foto & GPS tasdiq",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(cityFeed, key = { it.id }) { feedItem ->
                CityInstagramFeedCard(
                    item = feedItem,
                    onVerifyToggle = { onFeedVerifyClick(feedItem.id) },
                    onOpenTask = { onSelectTask(feedItem.linkedTaskId) },
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

private data class KpiStripSpec(
    val title: String,
    val value: String,
    val delta: String,
    val positive: Boolean,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun HokimHeroStatsCard(
    currentRole: UserRole,
    cityHealthScore: Int,
    totalCount: Int,
    inProgressCount: Int,
    riskCount: Int,
    onOpenHealthEditorClick: () -> Unit,
    onOpenBriefingClick: () -> Unit,
    onOpenAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_command_hero_1795671436085),
                contentDescription = "Zarafshon Boshqaruv Markazi",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xF00B1528),
                                Color(0xE013254A),
                                Color(0xCC241C4A)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentRole) {
                                UserRole.HOKIM -> "Hokim Boshqaruv Markazi"
                                UserRole.SUPER_ADMIN -> "Markaziy Administrator Markazi"
                                UserRole.ORINBOSAR -> "O'rinbosar Nazorat Markazi"
                                UserRole.BOLIM_RAHBARI -> "Bo'lim Boshqaruv Markazi"
                                UserRole.IJROCHI -> "Ijrochi Ishchi Markazi"
                                UserRole.NAZORATCHI -> "Nazorat & Audit Markazi"
                                UserRole.TAHLILCHI -> "Tahlilchi & Audit Markazi"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 21.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Boshqaruv Ko'rsatkichlari • Umumiy samaradorlik",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }

                    Surface(
                        color = HokimEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HokimEmerald.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HokimEmeraldLight)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AI Nazoratda",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onOpenHealthEditorClick() }
                            .testTag("city_health_gauge")
                    ) {
                        CityHealthGaugeChart(score = cityHealthScore)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HeroStatLine("Topshiriqlar:", "$totalCount ta", Color.White)
                        HeroStatLine("Ijroda:", "$inProgressCount ta", TelegramCyan)
                        HeroStatLine(
                            "Kechikish xavfi:",
                            "$riskCount (AI)",
                            if (riskCount > 0) Color(0xFFFCA5A5) else HokimEmeraldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenBriefingClick() }
                            .testTag("five_min_briefing_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("5 Daq. Hisobot", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = Color(0xFF7C3AED).copy(alpha = 0.42f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenAiClick() }
                            .testTag("hero_ai_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Tahlil Markazi", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TezkorAmallarPanel(
    canCreateTask: Boolean,
    onNewTaskClick: () -> Unit,
    onAddProjectClick: () -> Unit,
    onOpenAnalyticsClick: () -> Unit,
    onOpenOrgStructureClick: () -> Unit,
    onOpenPermissionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tezkor amallar",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onNewTaskClick,
                enabled = canCreateTask,
                colors = ButtonDefaults.buttonColors(containerColor = HokimNavy),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yangi topshiriq", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            TezkorOutlineRow(Icons.Default.BusinessCenter, "Yangi loyiha qo'shish", onAddProjectClick)
            TezkorOutlineRow(Icons.Default.BarChart, "Hisobot va KPI tahlil", onOpenAnalyticsClick)
            TezkorOutlineRow(Icons.Default.AccountTree, "Tashkiliy tuzilma", onOpenOrgStructureClick)
            TezkorOutlineRow(Icons.Default.AdminPanelSettings, "Vakolatlar matritsasi", onOpenPermissionsClick)
        }
    }
}

@Composable
private fun TezkorOutlineRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = TelegramBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CityTrendsCard(
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shahar bo'yicha asosiy ko'rsatkichlar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDetailsClick() }
                ) {
                    Text(
                        text = "So'nggi 6 oy",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            CityTrendsMultiSeriesChart()
        }
    }
}

@Composable
private fun GisPreviewCard(
    openProblemsCount: Int,
    investmentsCount: Int,
    onOpenGisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenGisClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shahar xaritasi (GIS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Batafsil ko'rish →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramBlue
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE5EFFA))
                    .border(1.dp, TelegramBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    for (i in 1..6) {
                        drawLine(Color(0xFFCBD5E1), Offset(0f, h * i / 7f), Offset(w, h * i / 7f), 1.2f)
                        drawLine(Color(0xFFCBD5E1), Offset(w * i / 7f, 0f), Offset(w * i / 7f, h), 1.2f)
                    }
                    val boundary = Path().apply {
                        moveTo(w * 0.25f, h * 0.35f)
                        lineTo(w * 0.62f, h * 0.16f)
                        lineTo(w * 0.86f, h * 0.45f)
                        lineTo(w * 0.65f, h * 0.82f)
                        lineTo(w * 0.28f, h * 0.72f)
                        close()
                    }
                    drawPath(boundary, color = TelegramBlue.copy(alpha = 0.14f))
                    drawPath(boundary, color = TelegramBlue.copy(alpha = 0.55f), style = Stroke(width = 3f))

                    listOf(
                        Triple(0.36f, 0.48f, StatusGreen),
                        Triple(0.64f, 0.42f, HokimGold),
                        Triple(0.45f, 0.70f, TelegramBlue),
                        Triple(0.68f, 0.24f, StatusGreen)
                    ).forEach { (xr, yr, col) ->
                        drawCircle(Color.White, radius = 14f, center = Offset(w * xr, h * yr))
                        drawCircle(col, radius = 9f, center = Offset(w * xr, h * yr))
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("$openProblemsCount ta muammo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                        Text("$investmentsCount ta loyiha", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TelegramBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgRankingCard(
    ratings: List<Triple<String, Int, Color>>,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tashkilotlar reytingi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Barchasini ko'rish →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDetailsClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalBarComparisonChart(items = ratings)
        }
    }
}

@Composable
private fun ModuleQuickPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun HeroStatLine(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = Slate300)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
private fun SquircleActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    badgeText: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "squircleScale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = if (enabled) gradientColors else listOf(Slate700, Slate800)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(12.dp)
    ) {
        if (badgeText != null) {
            Surface(
                color = StatusRed,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChainQuickButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProblemHorizontalCard(
    problem: CityProblem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, problem.severity.color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("problem_card_${problem.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                Image(
                    painter = painterResource(id = problem.imageResId),
                    contentDescription = problem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
                Surface(
                    color = problem.severity.color,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = problem.severity.labelUz,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${problem.appealsCount} murojaat",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = problem.mahalla,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            Column(modifier = Modifier.padding(11.dp)) {
                Text(
                    text = problem.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = problem.orgName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = problem.status.color.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (problem.status == ProblemStatus.OCHIQ) "Topshiriq +" else "Yuborilgan",
                            color = problem.status.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityInstagramFeedCard(
    item: CityObjectFeedItem,
    onVerifyToggle: () -> Unit,
    onOpenTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TelegramHeaderGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.channelName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        text = "${item.location} • ${item.timestampText}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = item.statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.statusTag,
                        color = item.statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onOpenTask() }
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Ijro: ${item.progressPercent}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onVerifyToggle() }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isVerifiedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Tasdiqlash",
                                tint = if (item.isVerifiedByUser) StatusRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.verifiedCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenTask() }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ModeComment,
                                contentDescription = "Topshiriqqa o'tish",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hisobot", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    TextButton(
                        onClick = onOpenTask,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Obyektni ochish", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                    }
                }

                LinearProgressIndicator(
                    progress = { item.progressPercent / 100f },
                    color = item.statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.caption,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
