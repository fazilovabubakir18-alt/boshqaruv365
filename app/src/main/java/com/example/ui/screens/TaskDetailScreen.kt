package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiExecutiveAssistant
import com.example.data.AuditLogEntity
import com.example.data.BoshqaruvRepository
import com.example.data.ReportEntity
import com.example.data.TaskCommentEntity
import com.example.data.TaskEntity
import com.example.model.AppUser
import com.example.model.ConfidentialityLevel
import com.example.model.TaskCommentType
import com.example.model.TaskRole
import com.example.model.TaskStatus
import com.example.model.UserRole
import com.example.ui.components.GradientProgressBar
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskChainThreeWayRow
import com.example.ui.components.WorkflowStepper
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TaskDetailScreen(
    task: TaskEntity,
    reports: List<ReportEntity>,
    comments: List<TaskCommentEntity>,
    auditLogs: List<AuditLogEntity>,
    allUsers: List<AppUser>,
    currentUser: AppUser,
    currentRole: UserRole,
    taskRole: TaskRole,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onRejectClick: () -> Unit,
    onSubmitReportClick: () -> Unit,
    onVerifyReviewClick: () -> Unit,
    onApproveClick: () -> Unit,
    onReworkClick: () -> Unit,
    onDisciplineClick: () -> Unit,
    onTransitionStatus: (TaskStatus, String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddComment: (TaskCommentType, String) -> Unit,
    onUpdateProgress: (Int) -> Unit,
    onInterveneTask: (Long, String?, String?, String, Boolean) -> Unit,
    onRequestClarification: (String) -> Unit = {},
    onRequestExtension: (Long, String) -> Unit = { _, _ -> },
    onResolveExtension: (Boolean, String) -> Unit = { _, _ -> },
    onToggleMultiOrgPart: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val subtasks = remember(task.subtasksRaw) {
        BoshqaruvRepository.parseSubtasks(task.subtasksRaw)
    }
    val taskAudits = remember(auditLogs, task.taskNumber) {
        auditLogs.filter { it.entity == task.taskNumber }
    }
    val taskComments = remember(comments, task.id) {
        comments.filter { it.taskId == task.id }
    }
    val confLevel = remember(task.confidentialityLevel) {
        try {
            ConfidentialityLevel.valueOf(task.confidentialityLevel)
        } catch (_: Exception) {
            ConfidentialityLevel.XIZMAT_UCHUN
        }
    }
    val aiThreadSummary = remember(task, taskComments) {
        AiExecutiveAssistant.summarizeTaskThread(task, taskComments)
    }

    val isHokimDecree = task.creatorRole.contains("Hokim") || task.creatorName.contains("Shukurov")
    val perms = currentUser.permissions
    val isReadOnly = currentRole == UserRole.TAHLILCHI || currentRole == UserRole.SUPER_ADMIN
    val multiOrgParts = remember(task.participatingOrgsRaw) {
        BoshqaruvRepository.parseMultiOrgParts(task.participatingOrgsRaw)
    }
    val isSameAsReviewer = currentUser.name.equals(task.reviewerName, ignoreCase = true)

    var sliderProgress by remember(task.progressPercent) { mutableFloatStateOf(task.progressPercent.toFloat()) }
    var newCommentText by remember { mutableStateOf("") }
    var showClarificationDialog by remember { mutableStateOf(false) }
    var clarificationText by remember { mutableStateOf("") }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var extensionHoursText by remember { mutableStateOf("24") }
    var extensionReasonText by remember { mutableStateOf("") }
    var selectedCommentType by remember {
        mutableStateOf(
            when (currentRole) {
                UserRole.HOKIM, UserRole.ORINBOSAR, UserRole.NAZORATCHI -> TaskCommentType.INSTRUCTION
                UserRole.IJROCHI, UserRole.BOLIM_RAHBARI -> TaskCommentType.PROGRESS_NOTE
                UserRole.TAHLILCHI, UserRole.SUPER_ADMIN -> TaskCommentType.PROGRESS_NOTE
            }
        )
    }

    // Super Admin intervention UI state
    var showInterventionBox by remember { mutableStateOf(false) }
    var interventionNote by remember { mutableStateOf("") }
    var selectedNewReviewer by remember(task.reviewerName) { mutableStateOf(task.reviewerName) }
    var selectedNewAssignee by remember(task.assigneeName) { mutableStateOf(task.assigneeName) }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ExecutiveHeaderGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("task_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${task.taskNumber} • ${task.aiMonitoringMode}",
                            color = HokimGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Topshiriq & Izohlar Zanjiri",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    StatusBadge(status = task.status)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isReadOnly -> {
                            Surface(
                                color = StatusPurple.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (currentRole == UserRole.SUPER_ADMIN) {
                                            "Markaziy Administrator: Topshiriqni yakuniy yopish huquqi yo'q"
                                        } else {
                                            "Tahlilchi rejimi (Faqat ko'rish va audit — Read-Only)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPurple
                                    )
                                }
                            }
                        }
                        // Ijrochi actions — NEVER allowed to self-close to COMPLETED
                        currentRole == UserRole.IJROCHI && (task.status == "ASSIGNED" || task.status == "NOTIFIED") -> {
                            OutlinedButton(
                                onClick = onRejectClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("reject_task_button")
                            ) {
                                Text("Rad etish", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showClarificationDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("clarify_task_button")
                            ) {
                                Text("Aniqlashtirish", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onAccept,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("accept_task_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Qabul", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        currentRole == UserRole.IJROCHI && (task.status == "ACCEPTED" || task.status == "IN_PROGRESS" || task.status == "REWORK" || task.status == "EXTENSION_REQUESTED") -> {
                            OutlinedButton(
                                onClick = { showExtensionDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("request_extension_button")
                            ) {
                                Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Muddat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSubmitReportClick,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("submit_report_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tekshiruvga Yuborish", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        // Stage 1: Nazoratchi tekshiruvi (SUBMITTED -> APPROVAL_PENDING)
                        task.status == "SUBMITTED" && perms.canReviewEvidence -> {
                            OutlinedButton(
                                onClick = onReworkClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HokimGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rework_task_button")
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Qaytarish", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onVerifyReviewClick,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("verify_review_button")
                            ) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nazorat Tasdig'i", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Stage 2: Final Approval (APPROVAL_PENDING / REVIEW -> COMPLETED) with Separation of Duties
                        (task.status == "APPROVAL_PENDING" || task.status == "REVIEW") && perms.canApproveReport -> {
                            OutlinedButton(
                                onClick = onReworkClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HokimGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rework_task_button")
                            ) {
                                Text("Qaytarish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onApproveClick,
                                enabled = !isSameAsReviewer,
                                colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("final_approve_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isSameAsReviewer) "Nazoratchi yopa olmaydi" else "Yakuniy Tasdiq",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        task.status == "COMPLETED" && perms.canArchiveTask -> {
                            Button(
                                onClick = { onTransitionStatus(TaskStatus.ARCHIVED, "Yakuniy tasdiqdan keyin arxivga olindi") },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("archive_task_button")
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Arxivlash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            if (perms.canIssueDiscipline) {
                                OutlinedButton(
                                    onClick = onDisciplineClick,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("discipline_task_button")
                                ) {
                                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Intizomiy Chora Taklifi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            // 1. 3+ Cycle Alert Banner
            if (task.cycleCount >= 3) {
                item {
                    Surface(
                        color = StatusRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ko'p Qaytgan / Diqqat Talab (${task.cycleCount}-tsikl)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusRed
                                )
                                Text(
                                    text = "Topshiriq 3+ marta qaytarildi. Rahbar va AI Nazoratga signal yuborilgan.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. 3-Tomonlama Zanjir + Progress (%) Slider Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "3-TOMONLAMA ZANJIR & BAJARILISH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TelegramBlue
                            )
                            Surface(
                                color = Color(confLevel.colorHex).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${currentRole.titleUz} • ${taskRole.titleUz}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(confLevel.colorHex),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        TaskChainThreeWayRow(
                            creatorName = task.creatorName,
                            reviewerName = task.reviewerName,
                            assigneeName = task.assigneeName
                        )

                        Text(
                            text = "Yakuniy tasdiqlovchi: ${task.finalApproverName} • Yo'nalish: ${task.routingType} • Format: ${task.reportFormat}${if (!task.sourceCommunicationId.isNullOrBlank()) " • Manba: ${task.sourceCommunicationId}" else ""}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        GradientProgressBar(progressPercent = task.progressPercent)

                        if (!isReadOnly && task.status != "COMPLETED" && task.status != "ARCHIVED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = sliderProgress,
                                    onValueChange = { sliderProgress = it },
                                    valueRange = 0f..100f,
                                    steps = 9,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { onUpdateProgress(sliderProgress.roundToInt()) },
                                    modifier = Modifier.testTag("save_progress_button")
                                ) {
                                    Text("${sliderProgress.roundToInt()}% Saqlash", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2b. Extension Request Resolution Card
            if (task.status == "EXTENSION_REQUESTED") {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HokimGold.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, HokimGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "MUDDAT UZAYTIRISH SO'ROVI (+${task.pendingExtensionHours.coerceAtLeast(24L)} soat)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = HokimGold
                            )
                            Text(
                                text = "Sabab: ${task.pendingExtensionReason ?: "Izoh ko'rsatilmagan"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if ((perms.canApproveReport || perms.canEditDeadline) && !isReadOnly) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onResolveExtension(false, "Asos yetarli emas") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                                        modifier = Modifier.weight(1f).testTag("reject_extension_button")
                                    ) {
                                        Text("Rad etish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onResolveExtension(true, "Rahbariyat tomonidan tasdiqlandi") },
                                        colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                                        modifier = Modifier.weight(1f).testTag("approve_extension_button")
                                    ) {
                                        Text("Tasdiqlash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2c. Multi-Org Execution Parts Checklist Card
            if (task.routingType == "KOP_TASHKILOTLI" && multiOrgParts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, TelegramCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KO'P TASHKILOTLI IJRO QISMLARI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TelegramBlue
                                )
                                Text(
                                    text = "${multiOrgParts.count { it.isCompleted }}/${multiOrgParts.size} yakunlandi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HokimEmerald
                                )
                            }
                            multiOrgParts.forEach { part ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isReadOnly) { onToggleMultiOrgPart(part.orgName) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = part.isCompleted,
                                        enabled = !isReadOnly,
                                        onCheckedChange = { if (!isReadOnly) onToggleMultiOrgPart(part.orgName) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(part.orgName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Mas'ul: ${part.assigneeName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = if (part.isCompleted) "100%" else "${part.progressPercent}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (part.isCompleted) HokimEmerald else HokimGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. AI Qisqa Nazorat Xulosasi Card (Rule 5 Advisory Summary)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StatusPurple.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI QISQA NAZORAT XULOSASI (TAVSIYA)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFA78BFA)
                            )
                        }
                        Text(
                            text = aiThreadSummary,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 4. Rahbar (Super Admin) Direct Intervention Panel
            if (currentRole == UserRole.HOKIM) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, HokimGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showInterventionBox = !showInterventionBox },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = HokimGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rahbar Aralashuvi (Super Admin)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HokimGold
                                    )
                                }
                                Icon(
                                    imageVector = if (showInterventionBox) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Ochish",
                                    tint = HokimGold
                                )
                            }

                            if (showInterventionBox) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = interventionNote,
                                    onValueChange = { interventionNote = it },
                                    label = { Text("Rahbar ko'rsatmasi (ixtiyoriy)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nazoratchini almashtirish:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(allUsers.filter { it.role == UserRole.NAZORATCHI || it.role == UserRole.ORINBOSAR }) { u ->
                                        FilterChip(
                                            selected = selectedNewReviewer == u.name,
                                            onClick = { selectedNewReviewer = u.name },
                                            label = { Text(u.name.substringBefore(" "), fontSize = 10.sp) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Ijrochini almashtirish:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(allUsers.filter { it.role == UserRole.IJROCHI || it.role == UserRole.BOLIM_RAHBARI }) { u ->
                                        FilterChip(
                                            selected = selectedNewAssignee == u.name,
                                            onClick = { selectedNewAssignee = u.name },
                                            label = { Text(u.name.substringBefore(" "), fontSize = 10.sp) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onInterveneTask(24L, selectedNewReviewer, selectedNewAssignee, interventionNote, false)
                                            interventionNote = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+24s & Saqlash", fontSize = 10.sp)
                                    }
                                    if (task.status == "COMPLETED" || task.status == "ARCHIVED") {
                                        Button(
                                            onClick = {
                                                onInterveneTask(48L, selectedNewReviewer, selectedNewAssignee, interventionNote.ifBlank { "Rahbar tomonidan qayta ochildi" }, true)
                                                interventionNote = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HokimGold),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Qayta Ochish", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Workflow Stepper
            item {
                WorkflowStepper(currentStatus = task.status)
            }

            // 6. Hokim decree non-rejection notice
            if (isHokimDecree && currentRole == UserRole.BOLIM_RAHBARI) {
                item {
                    Surface(
                        color = HokimGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HokimGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = HokimGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ushbu topshiriq to'g'ridan-to'g'ri Rahbar tomonidan berilgan. Rad etish mumkin emas!",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // 7. Main Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TelegramBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = task.department,
                                    color = TelegramBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            PriorityBadge(priority = task.priority)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = task.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Normativ asos:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = task.normativAsos.ifBlank { "Hokimiyat rejali topshirig'i" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = HokimGold.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = HokimGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${task.kpiPoints} KPI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HokimGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8. Comment Loop & Instructions (Izohlar va Harakatlar Lentasi)
            item {
                Text(
                    text = "IZOHLAR VA KO'RSATMALAR LENTASI (${taskComments.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TelegramBlue
                )
            }

            if (taskComments.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hali izohlar kiritilmagan.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(taskComments, key = { it.id }) { comment ->
                    val typeEnum = try {
                        TaskCommentType.valueOf(comment.commentType)
                    } catch (_: Exception) {
                        TaskCommentType.PROGRESS_NOTE
                    }
                    val accent = Color(typeEnum.colorHex)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accent.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = typeEnum.labelUz,
                                        color = accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = BoshqaruvRepository.formatDate(comment.timestamp),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${comment.authorName} • ${comment.authorRole}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = comment.text,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                            if (!comment.attachmentName.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(comment.attachmentName, fontSize = 10.sp, color = TelegramBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inline Comment / Instruction Composer
            if (!isReadOnly) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "YANGI IZOH / KO'RSATMA QO'SHISH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramBlue
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        TaskCommentType.INSTRUCTION,
                                        TaskCommentType.PROGRESS_NOTE,
                                        TaskCommentType.DIAGONAL_SYNC
                                    )
                                ) { t ->
                                    FilterChip(
                                        selected = selectedCommentType == t,
                                        onClick = { selectedCommentType = t },
                                        label = { Text(t.labelUz, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCommentText,
                                    onValueChange = { newCommentText = it },
                                    placeholder = { Text("Izoh yoki ko'rsatma yozing...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("task_comment_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newCommentText.isNotBlank()) {
                                            onAddComment(selectedCommentType, newCommentText.trim())
                                            newCommentText = ""
                                        }
                                    },
                                    enabled = newCommentText.isNotBlank(),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (newCommentText.isNotBlank()) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .testTag("task_comment_send_button")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Yuborish", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 9. Subtasks (Checklist)
            if (subtasks.isNotEmpty()) {
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
                                Text(
                                    text = "BOSQICHMA-BOSQICH CHECKLIST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${subtasks.count { it.isCompleted }}/${subtasks.size} bajarildi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            subtasks.forEach { sub ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isReadOnly) { onToggleSubtask(sub.id) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = sub.isCompleted,
                                        enabled = !isReadOnly,
                                        onCheckedChange = { if (!isReadOnly) onToggleSubtask(sub.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = StatusGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sub.title,
                                        fontSize = 12.sp,
                                        color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 10. Reports Timeline
            item {
                Text(
                    text = "IJRO HISOBOTLARI VA DALILLAR (${reports.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (reports.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hali bu topshiriq bo'yicha foto/GPS dalil yuklanmagan.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(reports) { rep ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rep.reporterName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                Text(BoshqaruvRepository.formatDate(rep.timestamp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(rep.comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = HokimEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "GPS moslik: ${rep.distanceToTargetMeters}m • EXIF tasdiqlangan",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HokimEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 11. Task Audit Log History
            if (taskAudits.isNotEmpty()) {
                item {
                    Text(
                        text = "AUDIT VA HARAKATLAR TARIXI (${taskAudits.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(taskAudits.take(6)) { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.action, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                Text(BoshqaruvRepository.formatDate(log.timestamp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${log.userName} (${log.userRole}): ${log.oldValue} -> ${log.newValue}", fontSize = 10.sp)
                            Text(log.reason, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showClarificationDialog) {
        AlertDialog(
            onDismissRequest = { showClarificationDialog = false },
            title = { Text("Aniqlashtirish So'rovi", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = clarificationText,
                    onValueChange = { clarificationText = it },
                    label = { Text("Savol yoki aniqlik kiritiladigan band *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clarificationText.isNotBlank()) {
                            onRequestClarification(clarificationText.trim())
                            clarificationText = ""
                            showClarificationDialog = false
                        }
                    },
                    enabled = clarificationText.isNotBlank()
                ) { Text("Yuborish") }
            },
            dismissButton = {
                TextButton(onClick = { showClarificationDialog = false }) { Text("Bekor qilish") }
            }
        )
    }

    if (showExtensionDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionDialog = false },
            title = { Text("Muddat Uzaytirish So'rovi", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eslatma: joriy muddat faqat rahbar tasdiqlagandan so'ng o'zgaradi.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = extensionHoursText,
                        onValueChange = { extensionHoursText = it },
                        label = { Text("Qo'shimcha soat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = extensionReasonText,
                        onValueChange = { extensionReasonText = it },
                        label = { Text("Asosli sabab *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = extensionHoursText.toLongOrNull() ?: 24L
                        if (extensionReasonText.isNotBlank() && hours > 0L) {
                            onRequestExtension(hours, extensionReasonText.trim())
                            extensionReasonText = ""
                            showExtensionDialog = false
                        }
                    },
                    enabled = extensionReasonText.isNotBlank()
                ) { Text("So'rov Yuborish") }
            },
            dismissButton = {
                TextButton(onClick = { showExtensionDialog = false }) { Text("Bekor qilish") }
            }
        )
    }
}

