package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiExecutiveAssistant
import com.example.data.AuditLogEntity
import com.example.data.BoshqaruvRepository
import com.example.data.TaskEntity
import com.example.model.AiAnomalySignal
import com.example.model.AiControlModule
import com.example.ui.components.GradientProgressBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskChainThreeWayRow
import com.example.ui.theme.*

@Composable
fun AiAssistantScreen(
    tasks: List<TaskEntity>,
    auditLogs: List<AuditLogEntity>,
    aiModules: List<AiControlModule>,
    aiSignals: List<AiAnomalySignal>,
    messages: List<Pair<Boolean, String>>,
    onSendMessage: (String) -> Unit,
    onOpenBriefing: () -> Unit,
    onSelectTask: (Long) -> Unit,
    onNotifySupervisor: (TaskEntity) -> Unit,
    onExportClick: (String) -> Unit,
    onCloseOverlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputQuery by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    var aiSubTab by remember { mutableStateOf(0) } // 0: AI Audit, 1: Kechikish Radari, 2: Tizim Loglari, 3: Savol-Javob
    val listState = rememberLazyListState()

    val aiSummary = remember(tasks) {
        AiExecutiveAssistant.generateDailySummary(tasks)
    }

    val now = System.currentTimeMillis()
    val radarTasks = remember(tasks, searchText) {
        val base = tasks.filter {
            val matchText = if (searchText.isBlank()) true else {
                it.title.contains(searchText, true) ||
                    it.department.contains(searchText, true) ||
                    it.reviewerName.contains(searchText, true)
            }
            matchText && (it.cycleCount >= 3 ||
                it.status == "REWORK" ||
                (it.deadline < now && it.status != "COMPLETED") ||
                (it.progressPercent < 50 && it.status != "COMPLETED"))
        }
        base.ifEmpty { tasks }
    }

    LaunchedEffect(messages.size, aiSubTab) {
        if (aiSubTab == 3 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar matching Mockup 6
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("AI tahlillari va tizim loglaridan qidirish...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TelegramBlue) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-tabs matching Mockup 6
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tabs = listOf(
                0 to "AI Audit (${aiSignals.size + radarTasks.size})",
                1 to "Kechikish Radari (${radarTasks.size})",
                2 to "Tizim Loglari (${auditLogs.size})",
                3 to "Savol-Javob"
            )
            items(tabs) { (idx, label) ->
                val active = aiSubTab == idx
                FilterChip(
                    selected = active,
                    onClick = { aiSubTab = idx },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("ai_subtab_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (aiSubTab == 3) {
            // AI Savol-Javob Chat
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ExecutiveHeaderGradient, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "BUGUNGI BOSHQARUV XULOSASI",
                                color = HokimGoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiSummary.executiveRecommendation,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                items(messages) { (isUser, text) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            color = if (isUser) TelegramBlue else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)) else null,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(
                                text = text,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val suggestions = listOf(
                    "Kechikish radari holati?",
                    "3+ marta qaytgan topshiriqlar?",
                    "Qaysi bo'limda muammo ko'p?",
                    "Xodimlar KPI reytingi qanday?"
                )
                items(suggestions) { prompt ->
                    SuggestionChip(
                        onClick = { onSendMessage(prompt) },
                        label = { Text(prompt, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("AI Nazorat savoli...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputQuery.isNotBlank()) {
                            onSendMessage(inputQuery)
                            inputQuery = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue)
                        .testTag("ai_send_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Yuborish", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Hero Card matching Mockup 6
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, StatusPurple.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF131D36), Color(0xFF1F1B4D), Color(0xFF102A43))
                                ),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Nazorati va Intellektual Tahlil",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                if (onCloseOverlay != null) {
                                    IconButton(onClick = onCloseOverlay, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("94%", fontSize = 26.sp, fontWeight = FontWeight.Black, color = HokimEmeraldLight)
                                    Text("Samaradorlik Ko'rsatkichi", fontSize = 10.sp, color = Slate300)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("91%", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFFC4B5FD))
                                    Text("Nazorat va Intizom", fontSize = 10.sp, color = Slate300)
                                }
                            }

                            Surface(
                                color = StatusRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = HokimGoldLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kechikish xavfi: ${aiSummary.highRiskCount + aiSummary.multiCycleCount} ta signal • 3+ qaytarilgan: ${aiSummary.multiCycleCount} ta",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenBriefing,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("5 Daq. Hisobot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onExportClick("PDF") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Audit Eksport", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Rule 5 Advisory Notice
                item {
                    Surface(
                        color = HokimGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HokimGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = HokimGold, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Chegarasi: Tizim faqat tahlil qiladi va ogohlantiradi; mustaqil topshiriq bermaydi, xodimni bloklamaydi va intizomiy qaror chiqarmaydi.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                if (aiSubTab == 0 || aiSubTab == 1) {
                    item {
                        Text(
                            text = if (aiSubTab == 1) "KECHIKISH RADARI VA 3+ TSIKL SIGNALLARI" else "AI NAZORAT & AUDIT LOGLARI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(radarTasks, key = { "ai-task-${it.id}" }) { task ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectTask(task.id) }
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
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusBadge(status = task.status)
                                }

                                TaskChainThreeWayRow(
                                    creatorName = task.creatorName,
                                    reviewerName = task.reviewerName,
                                    assigneeName = task.assigneeName
                                )

                                Text(
                                    text = when {
                                        task.cycleCount >= 3 -> "AI Tahlil: ${task.cycleCount} marta qaytarilgan — Rahbar aralashuvi tavsiya etiladi."
                                        task.status == "APPROVAL_PENDING" -> "AI Tahlil: Nazoratchi tasdiqlagan, yakuniy tasdiq kutilmoqda."
                                        task.status == "SUBMITTED" -> "AI Tahlil: Bajaruvchi hisobot yuklagan, Nazoratchi tekshiruvi kutilmoqda."
                                        else -> "AI Prognoz: Progress ${task.progressPercent}%, muddat nazorat ostida."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                GradientProgressBar(
                                    progressPercent = task.progressPercent,
                                    accentColors = listOf(TelegramBlue, HokimGold, StatusRed)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onNotifySupervisor(task) },
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = HokimGold, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ogohlantirish", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { onSelectTask(task.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Text("Hisobotni ko'rish", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (aiSubTab == 0) {
                        item {
                            Text(
                                text = "8 TA AI TAHLILIY MODUL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TelegramBlue
                            )
                        }
                        val moduleRows = aiModules.chunked(2)
                        items(moduleRows) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { mod ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, mod.accentColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(mod.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(mod.subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (aiSubTab == 2) {
                    item {
                        Text(
                            text = "TIZIM AUDIT LOGLARI (${auditLogs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TelegramBlue
                        )
                    }
                    items(auditLogs, key = { "audit-${it.id}" }) { log ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${log.userName} (${log.userRole})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue, modifier = Modifier.weight(1f))
                                    val resColor = when (log.result) {
                                        "DENIED", "FAILED" -> StatusRed
                                        else -> HokimEmerald
                                    }
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
                                Text("${log.action} • ${log.entity}: ${log.oldValue} ➔ ${log.newValue}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${log.reason}${if (log.correlationId.isNotBlank()) " [${log.correlationId}]" else ""}",
                                    fontSize = 10.sp,
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
