package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DepartmentInfo
import com.example.model.DiagonalTransfer
import com.example.model.DiagonalTransferStatus
import com.example.model.DiagonalTransferType
import com.example.ui.theme.*

@Composable
fun DiagonalCommContent(
    transfers: List<DiagonalTransfer>,
    departments: List<DepartmentInfo>,
    canSendDiagonal: Boolean,
    onCreateTransfer: (
        type: DiagonalTransferType,
        receiverDept: String,
        receiverLeader: String,
        subject: String,
        basisDoc: String,
        deadlineText: String
    ) -> Unit,
    onAcceptTransfer: (String) -> Unit,
    onRespondTransfer: (String, String) -> Unit,
    onCreateTaskFromDiagonal: (DiagonalTransfer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStatusFilter by remember { mutableStateOf("BARCHASI") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var transferToRespond by remember { mutableStateOf<DiagonalTransfer?>(null) }

    val filtered = remember(transfers, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "KUTILMOQDA" -> transfers.filter {
                it.status == DiagonalTransferStatus.YUBORILDI || it.status == DiagonalTransferStatus.QABUL_QILINDI
            }
            "KECHIKMOQDA" -> transfers.filter { it.status == DiagonalTransferStatus.KECHIKMOQDA }
            "BAJARILDI" -> transfers.filter { it.status == DiagonalTransferStatus.BAJARILDI }
            else -> transfers
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Official Diagonal Communication Rule Notice
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Rasmiy diagonal aloqa",
                    tint = TelegramBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bo'limlararo Rasmiy Aloqa Reestri",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Chat emas — har bir uzatishda yuboruvchi, qabul qiluvchi, asos hujjat, muddat va nazoratchi qayd etiladi.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary Stats Row + Create Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiagonalMiniStat(
                label = "Jami",
                value = "${transfers.size}",
                color = TelegramBlue,
                modifier = Modifier.weight(1f)
            )
            DiagonalMiniStat(
                label = "Kechikmoqda",
                value = "${transfers.count { it.status == DiagonalTransferStatus.KECHIKMOQDA }}",
                color = StatusRed,
                modifier = Modifier.weight(1f)
            )
            DiagonalMiniStat(
                label = "Bajarildi",
                value = "${transfers.count { it.status == DiagonalTransferStatus.BAJARILDI }}",
                color = StatusGreen,
                modifier = Modifier.weight(1f)
            )
            if (canSendDiagonal) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = HokimBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("create_diagonal_transfer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Yangi rasmiy uzatish", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uzatish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chips = listOf(
                "BARCHASI" to "Barchasi (${transfers.size})",
                "KUTILMOQDA" to "Jarayonda",
                "KECHIKMOQDA" to "Kechikmoqda",
                "BAJARILDI" to "Javob berilgan"
            )
            items(chips) { (key, label) ->
                val selected = selectedStatusFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { selectedStatusFilter = key },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TelegramBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filtered, key = { it.id }) { item ->
                DiagonalTransferCard(
                    transfer = item,
                    onAccept = { onAcceptTransfer(item.id) },
                    onRespondClick = { transferToRespond = item },
                    onCreateTaskClick = { onCreateTaskFromDiagonal(item) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateDiagonalTransferDialog(
            departments = departments,
            onDismiss = { showCreateDialog = false },
            onSubmit = { type, dept, leader, subj, basis, deadline ->
                onCreateTransfer(type, dept, leader, subj, basis, deadline)
                showCreateDialog = false
            }
        )
    }

    transferToRespond?.let { target ->
        RespondDiagonalDialog(
            transfer = target,
            onDismiss = { transferToRespond = null },
            onSubmit = { response ->
                onRespondTransfer(target.id, response)
                transferToRespond = null
            }
        )
    }
}

@Composable
private fun DiagonalMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier.height(48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DiagonalTransferCard(
    transfer: DiagonalTransfer,
    onAccept: () -> Unit,
    onRespondClick: () -> Unit,
    onCreateTaskClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Doc Code + Type + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = transfer.docCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TelegramBlue
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = transfer.type.labelUz,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = transfer.status.color.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, transfer.status.color.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = transfer.status.labelUz,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = transfer.status.color,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Sender -> Receiver Flow Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YUBORUVCHI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transfer.senderDept, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(transfer.senderLeader, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Yo'nalish",
                        tint = TelegramBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("QABUL QILUVCHI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transfer.receiverDept, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(transfer.receiverLeader, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }

            // Subject
            Text(
                text = transfer.subject,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 17.sp
            )

            // Metadata grid: Basis Doc, Deadline, Supervisor
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Asos hujjat: ${transfer.basisDoc}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Muddat: ${transfer.deadlineText}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transfer.status == DiagonalTransferStatus.KECHIKMOQDA) StatusRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Nazorat: ${transfer.supervisorName.substringBefore("(").trim()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Response Note if answered
            transfer.responseNote?.let { note ->
                Surface(
                    color = StatusGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rasmiy javob: $note",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Monitoring Strip (Advisory only)
            Surface(
                color = TelegramBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = transfer.aiMonitoringNote,
                        fontSize = 10.sp,
                        color = TelegramBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!transfer.linkedTaskNumber.isNullOrBlank()) {
                Surface(
                    color = HokimEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = HokimEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bog'langan topshiriq: ${transfer.linkedTaskNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HokimEmerald
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (transfer.status == DiagonalTransferStatus.YUBORILDI) {
                    OutlinedButton(
                        onClick = onAccept,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("accept_diagonal_${transfer.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Qabul qilish", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (transfer.status != DiagonalTransferStatus.BAJARILDI) {
                    Button(
                        onClick = onRespondClick,
                        colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("respond_diagonal_${transfer.id}")
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rasmiy javob", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (transfer.linkedTaskNumber.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCreateTaskClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("task_from_diagonal_${transfer.id}")
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(14.dp), tint = TelegramBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Topshiriq", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDiagonalTransferDialog(
    departments: List<DepartmentInfo>,
    onDismiss: () -> Unit,
    onSubmit: (
        type: DiagonalTransferType,
        receiverDept: String,
        receiverLeader: String,
        subject: String,
        basisDoc: String,
        deadlineText: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(DiagonalTransferType.RASMIY_SOROV) }
    val firstDept = departments.firstOrNull()
    var receiverDept by remember { mutableStateOf(firstDept?.name ?: "Uy-joy kommunal xizmat (UJKX)") }
    var receiverLeader by remember { mutableStateOf(firstDept?.headName ?: "Haydarov O. T.") }
    var subject by remember { mutableStateOf("") }
    var basisDoc by remember { mutableStateOf("Xizmat zaruriyati / Hokimlik bayonnomasi") }
    var deadlineText by remember { mutableStateOf("Ertaga, 18:00 gacha") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi Rasmiy Diagonal Uzatish", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Uzatish turi:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DiagonalTransferType.entries) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.labelUz, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Qabul qiluvchi bo'lim:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(departments) { dept ->
                        FilterChip(
                            selected = receiverDept == dept.name,
                            onClick = {
                                receiverDept = dept.name
                                receiverLeader = dept.headName
                            },
                            label = { Text(dept.code, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Mazmuni / Rasmiy talab *", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diagonal_subject_input")
                )
                OutlinedTextField(
                    value = basisDoc,
                    onValueChange = { basisDoc = it },
                    label = { Text("Asos hujjat", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadlineText,
                    onValueChange = { deadlineText = it },
                    label = { Text("Javob muddati", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        onSubmit(selectedType, receiverDept, receiverLeader, subject, basisDoc, deadlineText)
                    }
                },
                enabled = subject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                modifier = Modifier.testTag("submit_diagonal_button")
            ) {
                Text("Rasmiy Yuborish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        }
    )
}

@Composable
private fun RespondDiagonalDialog(
    transfer: DiagonalTransfer,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var responseText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rasmiy Javob (${transfer.docCode})", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(transfer.subject, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("Rasmiy javob va asos hujjat raqami *", fontSize = 11.sp) },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diagonal_response_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(responseText) },
                enabled = responseText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HokimEmerald),
                modifier = Modifier.testTag("confirm_diagonal_response_button")
            ) {
                Text("Javobni Tasdiqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Yopish") }
        }
    )
}
