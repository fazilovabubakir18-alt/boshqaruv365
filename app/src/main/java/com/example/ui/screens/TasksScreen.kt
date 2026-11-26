package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BoshqaruvRepository
import com.example.data.TaskEntity
import com.example.model.AppUser
import com.example.model.ConfidentialityLevel
import com.example.model.DepartmentInfo
import com.example.model.DiagonalTransfer
import com.example.model.DiagonalTransferType
import com.example.model.TaskRole
import com.example.model.UserRole
import com.example.ui.components.DonutSegment
import com.example.ui.components.ExecutiveCivicKpiCard
import com.example.ui.components.GradientProgressBar
import com.example.ui.components.HorizontalBarComparisonChart
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskChainThreeWayRow
import com.example.ui.components.TaskDonutChart
import com.example.ui.theme.*

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    currentUser: AppUser,
    currentRole: UserRole,
    selectedFilter: String,
    searchQuery: String,
    diagonalTransfers: List<DiagonalTransfer>,
    departments: List<DepartmentInfo>,
    initialSubMode: Int = 0,
    resolveTaskRole: (AppUser, TaskEntity) -> TaskRole,
    onFilterSelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    onApproveTaskDirect: (Long) -> Unit,
    onNewTaskClick: () -> Unit,
    onCreateDiagonalTransfer: (
        type: DiagonalTransferType,
        receiverDept: String,
        receiverLeader: String,
        subject: String,
        basisDoc: String,
        deadlineText: String
    ) -> Unit,
    onAcceptDiagonalTransfer: (String) -> Unit,
    onRespondDiagonalTransfer: (String, String) -> Unit,
    onCreateTaskFromDiagonal: (DiagonalTransfer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var subMode by remember(initialSubMode) { mutableStateOf(initialSubMode) } // 0: Topshiriqlar, 1: Diagonal Aloqa
    var viewMode by remember { mutableStateOf(0) } // 0: Ro'yxat, 1: Kanban, 2: Kalendar
    val now = System.currentTimeMillis()

    // ABAC + Scope filtering
    val accessibleTasks = remember(tasks, currentUser) {
        tasks.filter { BoshqaruvRepository.canUserAccessTask(currentUser, it) }
    }

    val filteredTasks = remember(accessibleTasks, selectedFilter, searchQuery) {
        accessibleTasks.filter { task ->
            val matchesFilter = when (selectedFilter) {
                "BARCHASI" -> true
                "YANGI" -> task.status == "ASSIGNED" || task.status == "NOTIFIED" || task.status == "DRAFT"
                "JARAYONDA" -> task.status == "ACCEPTED" || task.status == "IN_PROGRESS"
                "TEKSHIRUVDA" -> task.status == "SUBMITTED" || task.status == "REVIEW" || task.status == "APPROVAL_PENDING"
                "KECHIKKAN" -> task.deadline < now && task.status != "COMPLETED" && task.status != "ARCHIVED"
                "ESKALATSIYA" -> task.status == "ESCALATED" || task.status == "EXTENSION_REQUESTED" || task.status == "ON_HOLD"
                "BAJARILGAN" -> task.status == "COMPLETED" || task.status == "ARCHIVED"
                "QAYTA_ISHLASH" -> task.status == "REWORK" || task.cycleCount >= 3
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                task.title.contains(searchQuery, ignoreCase = true) ||
                    task.taskNumber.contains(searchQuery, ignoreCase = true) ||
                    task.assigneeName.contains(searchQuery, ignoreCase = true) ||
                    task.reviewerName.contains(searchQuery, ignoreCase = true) ||
                    task.department.contains(searchQuery, ignoreCase = true) ||
                    task.objectName.contains(searchQuery, ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }
    }

    var previewTaskId by remember(filteredTasks) {
        mutableStateOf(filteredTasks.firstOrNull()?.id ?: accessibleTasks.firstOrNull()?.id)
    }
    val previewTask = remember(accessibleTasks, previewTaskId) {
        accessibleTasks.firstOrNull { it.id == previewTaskId } ?: filteredTasks.firstOrNull()
    }

    val totalCount = accessibleTasks.size
    val completedCount = accessibleTasks.count { it.status == "COMPLETED" || it.status == "ARCHIVED" }
    val delayedCount = accessibleTasks.count {
        (it.deadline < now && it.status != "COMPLETED" && it.status != "ARCHIVED") || it.cycleCount >= 3
    }
    val cancelledCount = accessibleTasks.count { it.status == "CANCELLED" || it.status == "REJECTED" }
    val inProgressCount = (totalCount - completedCount - delayedCount - cancelledCount).coerceAtLeast(0)
    val onTimePct = if (totalCount > 0) ((totalCount - delayedCount) * 100 / totalCount).coerceIn(0, 100) else 86
    val uniqueAssigneesCount = accessibleTasks.map { it.assigneeName }.distinct().size.coerceAtLeast(1)

    Scaffold(
        floatingActionButton = {
            if (subMode == 0 && currentUser.permissions.canCreateTask && currentRole != UserRole.TAHLILCHI) {
                FloatingActionButton(
                    onClick = onNewTaskClick,
                    containerColor = TelegramBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_new_task")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Yangi topshiriq")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Segmented Switcher: Topshiriqlar vs Diagonal Aloqa
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        0 to "Topshiriqlar (${accessibleTasks.size})",
                        1 to "Diagonal Aloqa (${diagonalTransfers.size})"
                    ).forEach { (idx, title) ->
                        val active = subMode == idx
                        Surface(
                            color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (active) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { subMode = idx }
                                .testTag(if (idx == 0) "subtab_tasks" else "subtab_diagonal")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (active) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (subMode == 1) {
                DiagonalCommContent(
                    transfers = diagonalTransfers,
                    departments = departments,
                    canSendDiagonal = currentUser.permissions.canSendDiagonalTransfer,
                    onCreateTransfer = onCreateDiagonalTransfer,
                    onAcceptTransfer = onAcceptDiagonalTransfer,
                    onRespondTransfer = onRespondDiagonalTransfer,
                    onCreateTaskFromDiagonal = onCreateTaskFromDiagonal,
                    modifier = Modifier.weight(1f)
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val isWide = maxWidth >= 780.dp

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main Tasks Pane
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            modifier = Modifier
                                .weight(if (isWide) 1.35f else 1f)
                                .fillMaxHeight()
                        ) {
                            // 1. Mockup 2 KPI Sparkline Strip
                            item {
                                val kpis = listOf(
                                    TasksKpiSpec(
                                        title = "Jami topshiriqlar",
                                        value = "$totalCount",
                                        delta = "↑ +12%",
                                        positive = true,
                                        icon = Icons.Default.Assignment,
                                        color = TelegramBlue,
                                        points = listOf(12f, 15f, 14f, 18f, 21f, 24f),
                                        filter = "BARCHASI"
                                    ),
                                    TasksKpiSpec(
                                        title = "Ijro etilgan",
                                        value = "$completedCount",
                                        delta = "↑ +8%",
                                        positive = true,
                                        icon = Icons.Default.CheckCircle,
                                        color = HokimEmerald,
                                        points = listOf(8f, 11f, 13f, 15f, 19f, 21f),
                                        filter = "BAJARILGAN"
                                    ),
                                    TasksKpiSpec(
                                        title = "Kechikayotgan",
                                        value = "$delayedCount",
                                        delta = "↑ +3%",
                                        positive = false,
                                        icon = Icons.Default.Schedule,
                                        color = HokimGold,
                                        points = listOf(3f, 2f, 4f, 3f, 5f, 4f),
                                        filter = "KECHIKKAN"
                                    ),
                                    TasksKpiSpec(
                                        title = "Muddatida",
                                        value = "$onTimePct%",
                                        delta = "↑ +2%",
                                        positive = true,
                                        icon = Icons.Default.HourglassBottom,
                                        color = StatusPurple,
                                        points = listOf(74f, 77f, 80f, 83f, 85f, 86f),
                                        filter = "BARCHASI"
                                    ),
                                    TasksKpiSpec(
                                        title = "Mas'ullar",
                                        value = "$uniqueAssigneesCount",
                                        delta = "Faol",
                                        positive = true,
                                        icon = Icons.Default.Groups,
                                        color = TelegramCyan,
                                        points = listOf(4f, 5f, 5f, 6f, 7f, 8f),
                                        filter = "JARAYONDA"
                                    )
                                )

                                if (isWide) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        kpis.forEach { k ->
                                            ExecutiveCivicKpiCard(
                                                title = k.title,
                                                value = k.value,
                                                deltaText = k.delta,
                                                isPositiveTrend = k.positive,
                                                icon = k.icon,
                                                accentColor = k.color,
                                                sparklinePoints = k.points,
                                                onClick = { onFilterSelected(k.filter) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(kpis) { k ->
                                            ExecutiveCivicKpiCard(
                                                title = k.title,
                                                value = k.value,
                                                deltaText = k.delta,
                                                isPositiveTrend = k.positive,
                                                icon = k.icon,
                                                accentColor = k.color,
                                                sparklinePoints = k.points,
                                                onClick = { onFilterSelected(k.filter) },
                                                modifier = Modifier.width(154.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Role + Scope Pill & View Mode Switcher (Ro'yxat / Kanban / Kalendar)
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = TelegramBlue.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Default.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = TelegramBlue,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Rol: ${currentRole.titleUz} • Ko'lam: ${currentUser.scope.territory}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (currentRole == UserRole.TAHLILCHI) "Read-Only" else "Daraja ${currentUser.scope.maxConfidentialityLevel}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TelegramBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                listOf(
                                                    Triple(0, "Ro'yxat", Icons.Outlined.ViewList),
                                                    Triple(1, "Kanban", Icons.Outlined.ViewKanban),
                                                    Triple(2, "Kalendar", Icons.Outlined.CalendarMonth)
                                                ).forEach { (idx, label, icon) ->
                                                    val active = viewMode == idx
                                                    Surface(
                                                        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                                                        shape = RoundedCornerShape(9.dp),
                                                        shadowElevation = if (active) 2.dp else 0.dp,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(34.dp)
                                                            .clip(RoundedCornerShape(9.dp))
                                                            .clickable { viewMode = idx }
                                                            .testTag("tasks_view_mode_$idx")
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = label,
                                                                tint = if (active) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = label,
                                                                fontSize = 10.sp,
                                                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                                                color = if (active) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isWide && currentUser.permissions.canCreateTask && currentRole != UserRole.TAHLILCHI) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = onNewTaskClick,
                                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Yangi topshiriq", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Search & Filter Chips
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        placeholder = { Text("Topshiriq, mas'ul, B365 raqam yoki tashkilot...", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TelegramBlue) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { onSearchQueryChange("") }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Tozalash")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("tasks_search_input")
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val filters = listOf(
                                            "BARCHASI" to "Barchasi (${accessibleTasks.size})",
                                            "YANGI" to "Yaratildi",
                                            "JARAYONDA" to "Jarayonda",
                                            "TEKSHIRUVDA" to "Tekshiruvda",
                                            "QAYTA_ISHLASH" to "Qaytarilgan / 3+ tsikl",
                                            "KECHIKKAN" to "Kechikkan",
                                            "BAJARILGAN" to "Yakunlandi"
                                        )

                                        items(filters) { (key, label) ->
                                            val isSelected = selectedFilter == key
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onFilterSelected(key) },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "3-Tomonlama Topshiriqlar Zanjiri",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${filteredTasks.size} ta",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 4. Tasks Content by View Mode
                            if (filteredTasks.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPasteOff,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Mos keluvchi topshiriqlar topilmadi",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            } else {
                                when (viewMode) {
                                    0 -> {
                                        // Ro'yxat View
                                        items(filteredTasks, key = { it.id }) { task ->
                                            val myTaskRole = resolveTaskRole(currentUser, task)
                                            val confLevel = try {
                                                ConfidentialityLevel.valueOf(task.confidentialityLevel)
                                            } catch (_: Exception) {
                                                ConfidentialityLevel.XIZMAT_UCHUN
                                            }
                                            val isSameReviewer = currentUser.name.equals(task.reviewerName, ignoreCase = true)
                                            val canQuickApprove = currentRole != UserRole.SUPER_ADMIN &&
                                                currentRole != UserRole.TAHLILCHI && (
                                                (task.status == "SUBMITTED" && currentUser.permissions.canReviewEvidence) ||
                                                    ((task.status == "APPROVAL_PENDING" || task.status == "REVIEW") && currentUser.permissions.canApproveReport && !isSameReviewer)
                                                )
                                            val isSelectedPreview = isWide && previewTask?.id == task.id

                                            Card(
                                                shape = RoundedCornerShape(18.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelectedPreview) {
                                                        TelegramBlue.copy(alpha = 0.06f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    }
                                                ),
                                                border = BorderStroke(
                                                    width = if (isSelectedPreview) 1.6.dp else 1.dp,
                                                    color = when {
                                                        task.cycleCount >= 3 -> StatusRed.copy(alpha = 0.6f)
                                                        isSelectedPreview -> TelegramBlue
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .clickable {
                                                        previewTaskId = task.id
                                                        if (!isWide) onSelectTask(task.id)
                                                    }
                                                    .testTag("task_card_${task.id}")
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
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            Text(
                                                                text = task.taskNumber,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 11.sp,
                                                                color = TelegramBlue
                                                            )
                                                            PriorityBadge(priority = task.priority)
                                                            if (task.cycleCount >= 3) {
                                                                Surface(
                                                                    color = StatusRed.copy(alpha = 0.16f),
                                                                    shape = RoundedCornerShape(5.dp),
                                                                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f))
                                                                ) {
                                                                    Text(
                                                                        text = "Ko'p qaytgan (${task.cycleCount}x)",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        color = StatusRed,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        StatusBadge(status = task.status)
                                                    }

                                                    Text(
                                                        text = task.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

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
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Rol: ${myTaskRole.titleUz}",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Surface(
                                                                color = Color(confLevel.colorHex).copy(alpha = 0.12f),
                                                                shape = RoundedCornerShape(5.dp)
                                                            ) {
                                                                Text(
                                                                    text = confLevel.titleUz,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(confLevel.colorHex),
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        val isOverdue = task.deadline < now && task.status != "COMPLETED" && task.status != "ARCHIVED"
                                                        Text(
                                                            text = BoshqaruvRepository.formatDate(task.deadline),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = { onSelectTask(task.id) },
                                                            shape = RoundedCornerShape(9.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(34.dp)
                                                        ) {
                                                            Text("Hisobot va izohlar", fontSize = 10.sp)
                                                        }
                                                        if (canQuickApprove) {
                                                            Button(
                                                                onClick = { onApproveTaskDirect(task.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                                                                shape = RoundedCornerShape(9.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(34.dp)
                                                            ) {
                                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Tasdiqlash", fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    1 -> {
                                        // Kanban View (4 horizontal status columns)
                                        item {
                                            val columns = listOf(
                                                Triple("Yangi", TelegramBlue) { t: TaskEntity ->
                                                    t.status == "ASSIGNED" || t.status == "NOTIFIED" || t.status == "DRAFT"
                                                },
                                                Triple("Jarayonda", HokimGold) { t: TaskEntity ->
                                                    t.status == "ACCEPTED" || t.status == "IN_PROGRESS" || t.status == "ESCALATED" || t.status == "EXTENSION_REQUESTED"
                                                },
                                                Triple("Tekshiruvda", StatusPurple) { t: TaskEntity ->
                                                    t.status == "SUBMITTED" || t.status == "REVIEW" || t.status == "APPROVAL_PENDING" || t.status == "REWORK"
                                                },
                                                Triple("Yakunlandi", HokimEmerald) { t: TaskEntity ->
                                                    t.status == "COMPLETED" || t.status == "ARCHIVED"
                                                }
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(columns) { (colTitle, colColor, predicate) ->
                                                    val colTasks = filteredTasks.filter(predicate)
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                        shape = RoundedCornerShape(16.dp),
                                                        border = BorderStroke(1.dp, colColor.copy(alpha = 0.3f)),
                                                        modifier = Modifier.width(248.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(8.dp)
                                                                            .clip(CircleShape)
                                                                            .background(colColor)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(colTitle, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                                                }
                                                                Surface(
                                                                    color = colColor.copy(alpha = 0.16f),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${colTasks.size}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = colColor,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }

                                                            if (colTasks.isEmpty()) {
                                                                Text(
                                                                    text = "Hozircha topshiriq yo'q",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(vertical = 12.dp)
                                                                )
                                                            } else {
                                                                colTasks.forEach { task ->
                                                                    Surface(
                                                                        color = MaterialTheme.colorScheme.surface,
                                                                        shape = RoundedCornerShape(12.dp),
                                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .clip(RoundedCornerShape(12.dp))
                                                                            .clickable {
                                                                                previewTaskId = task.id
                                                                                if (!isWide) onSelectTask(task.id)
                                                                            }
                                                                    ) {
                                                                        Column(
                                                                            modifier = Modifier.padding(10.dp),
                                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            Row(
                                                                                modifier = Modifier.fillMaxWidth(),
                                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                                            ) {
                                                                                Text(task.taskNumber, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                                                                PriorityBadge(priority = task.priority)
                                                                            }
                                                                            Text(
                                                                                text = task.title,
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                maxLines = 2,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                            GradientProgressBar(
                                                                                progressPercent = task.progressPercent,
                                                                                accentColors = listOf(colColor, TelegramCyan)
                                                                            )
                                                                            Row(
                                                                                modifier = Modifier.fillMaxWidth(),
                                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                                            ) {
                                                                                Text(
                                                                                    text = task.assigneeName.substringBefore(" "),
                                                                                    fontSize = 9.sp,
                                                                                    fontWeight = FontWeight.SemiBold,
                                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                )
                                                                                Text(
                                                                                    text = BoshqaruvRepository.formatDate(task.deadline),
                                                                                    fontSize = 9.sp,
                                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        // Kalendar View (Grouped by deadline date)
                                        val groupedByDate = filteredTasks
                                            .sortedBy { it.deadline }
                                            .groupBy { BoshqaruvRepository.formatDate(it.deadline) }

                                        groupedByDate.forEach { (dateLabel, dateTasks) ->
                                            item(key = "cal-hdr-$dateLabel") {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Event,
                                                        contentDescription = null,
                                                        tint = TelegramBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "$dateLabel (${dateTasks.size} ta muddat)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = TelegramBlue
                                                    )
                                                }
                                            }
                                            items(dateTasks, key = { "cal-${it.id}" }) { task ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            previewTaskId = task.id
                                                            if (!isWide) onSelectTask(task.id)
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(4.dp)
                                                                .height(38.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(if (task.deadline < now && task.status != "COMPLETED") StatusRed else TelegramBlue)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(task.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text("${task.assigneeName} • ${task.department}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        StatusBadge(status = task.status)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Mockup 2 Bottom Analytical Cards (Donut + Org Bars + Activity Feed)
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                TasksBottomAnalyticsSection(
                                    completedCount = completedCount,
                                    inProgressCount = inProgressCount,
                                    delayedCount = delayedCount,
                                    cancelledCount = cancelledCount,
                                    accessibleTasks = accessibleTasks,
                                    onSelectTask = onSelectTask
                                )
                            }
                        }

                        // Wide Split-Pane Right Preview ("Topshiriq tafsilotlari" - Mockup 2)
                        if (isWide && previewTask != null) {
                            val previewSameReviewer = currentUser.name.equals(previewTask.reviewerName, ignoreCase = true)
                            TaskSidePreviewCard(
                                task = previewTask,
                                now = now,
                                canApprove = currentRole != UserRole.SUPER_ADMIN &&
                                    currentRole != UserRole.TAHLILCHI && (
                                    (previewTask.status == "SUBMITTED" && currentUser.permissions.canReviewEvidence) ||
                                        ((previewTask.status == "APPROVAL_PENDING" || previewTask.status == "REVIEW") && currentUser.permissions.canApproveReport && !previewSameReviewer)
                                    ),
                                onOpenFull = { onSelectTask(previewTask.id) },
                                onApproveDirect = { onApproveTaskDirect(previewTask.id) },
                                modifier = Modifier
                                    .weight(0.85f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TasksKpiSpec(
    val title: String,
    val value: String,
    val delta: String,
    val positive: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val points: List<Float>,
    val filter: String
)

@Composable
private fun TaskSidePreviewCard(
    task: TaskEntity,
    now: Long,
    canApprove: Boolean,
    onOpenFull: () -> Unit,
    onApproveDirect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = task.deadline < now && task.status != "COMPLETED" && task.status != "ARCHIVED"
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Topshiriq tafsilotlari",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.taskNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramBlue
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(status = task.status)
                PriorityBadge(priority = task.priority)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Muddat: ${BoshqaruvRepository.formatDate(task.deadline)} ${if (isOverdue) "(muddat o'tgan)" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailPersonLine(Icons.Default.Person, "Mas'ul shaxs", task.assigneeName)
                    DetailPersonLine(Icons.Default.Apartment, "Tashkilot", task.department)
                    DetailPersonLine(Icons.Default.VerifiedUser, "Nazorat qiluvchi", task.reviewerName)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Vazifa tavsifi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 17.sp
                )
            }

            GradientProgressBar(
                progressPercent = task.progressPercent,
                accentColors = listOf(TelegramBlue, HokimEmerald)
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenFull,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("To'liq ko'rish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (canApprove) {
                    Button(
                        onClick = onApproveDirect,
                        colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tasdiqlash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPersonLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    roleLabel: String,
    name: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TelegramBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = roleLabel, tint = TelegramBlue, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(roleLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TasksBottomAnalyticsSection(
    completedCount: Int,
    inProgressCount: Int,
    delayedCount: Int,
    cancelledCount: Int,
    accessibleTasks: List<TaskEntity>,
    onSelectTask: (Long) -> Unit
) {
    val total = accessibleTasks.size.coerceAtLeast(1)
    val onTrackPct = (((completedCount + inProgressCount) * 100f) / total).toInt().coerceIn(0, 100)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Ijro holati bo'yicha statistika (Donut)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Ijro holati bo'yicha statistika",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                TaskDonutChart(
                    segments = listOf(
                        DonutSegment("Ijro etilgan", completedCount.coerceAtLeast(1), HokimEmerald),
                        DonutSegment("Jarayonda", inProgressCount.coerceAtLeast(1), TelegramBlue),
                        DonutSegment("Kechikayotgan", delayedCount, HokimGold),
                        DonutSegment("Bekor qilingan", cancelledCount, StatusRed)
                    ),
                    centerValue = "$onTrackPct%",
                    centerLabel = "Ijro"
                )
            }
        }

        // 2. Tashkilotlar kesimida (Bars)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Tashkilotlar kesimida",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalBarComparisonChart(
                    items = listOf(
                        Triple("Obodonlashtirish", 95, TelegramBlue),
                        Triple("Xalq ta'limi", 92, TelegramCyan),
                        Triple("Suv xo'jaligi", 88, HokimEmerald),
                        Triple("Yo'l xo'jaligi", 85, HokimGold),
                        Triple("Iqtisodiyot", 80, StatusPurple)
                    )
                )
            }
        }

        // 3. So'nggi faoliyatlar
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "So'nggi faoliyatlar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                accessibleTasks.take(3).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTask(task.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(TelegramBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${task.assigneeName.substringBefore(" ")} • ${task.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.department} • ${task.progressPercent}% bajarildi",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
