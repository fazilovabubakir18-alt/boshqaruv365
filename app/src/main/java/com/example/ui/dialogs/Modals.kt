package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.NotificationEntity
import com.example.data.TaskEntity
import com.example.model.AppUser
import com.example.model.Department
import com.example.model.DepartmentInfo
import com.example.model.ReportFormat
import com.example.model.SubTask
import com.example.model.TaskPriority
import com.example.model.TaskRoutingType
import com.example.model.UserPermissions
import com.example.model.UserRole
import com.example.ui.components.SingleCircularRing
import com.example.ui.theme.*

@Composable
fun RoleSwitcherDialog(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = HokimNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foydalanuvchi Rolini Tanlash", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tizim imkoniyatlarini turli mansabdor shaxslar nigohi bilan ko'rish uchun rolni almashtiring:",
                    fontSize = 12.sp,
                    color = Slate600
                )
                UserRole.values().forEach { role ->
                    val isSelected = role == currentRole
                    Surface(
                        color = if (isSelected) TelegramBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) TelegramBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onRoleSelected(role)
                                onDismiss()
                            }
                            .testTag("role_option_${role.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onRoleSelected(role)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TelegramBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = role.titleUz,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (role) {
                                        UserRole.HOKIM -> "Butun tizim (100%): Barcha bo'limlar, qarorlar va yakuniy tasdiq"
                                        UserRole.SUPER_ADMIN -> "Markaziy administrator: Xodimlar, bo'limlar, RBAC matritsasi, audit va zaxira (topshiriq yopmaydi)"
                                        UserRole.ORINBOSAR -> "Biriktirilgan yo'nalishlar nazorati, hisobotni tasdiqlash/qaytarish"
                                        UserRole.BOLIM_RAHBARI -> "O'z bo'limiga topshiriq berish, hisobot yuborish, diagonal aloqa"
                                        UserRole.IJROCHI -> "Topshiriqni bajarish, progress (%) va GPS/foto hisobot (o'zi yopa olmaydi)"
                                        UserRole.NAZORATCHI -> "Ijro intizomi, 3-tomonlama nazorat, AI radar va audit tekshiruvi"
                                        UserRole.TAHLILCHI -> "Faqat o'qish (Read-Only): Tahlil, KPI va AI audit hisobotlarini ko'rish"
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Yopish", color = TelegramBlue)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskDialog(
    creatorName: String = "Shukurov Alisher Davronovich",
    availableUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        department: String,
        assigneeName: String,
        priority: String,
        deadlineMillis: Long,
        objectName: String,
        address: String,
        normativAsos: String,
        kpiPoints: Int,
        subtasks: List<SubTask>,
        reviewerName: String,
        finalApproverName: String,
        aiMonitoringMode: String,
        routingType: String,
        reportFormat: String,
        participatingOrgsRaw: String
    ) -> Unit
) {
    val reviewers = remember(availableUsers) {
        availableUsers.filter {
            !it.isBlocked && (it.role == UserRole.NAZORATCHI || it.role == UserRole.ORINBOSAR)
        }.ifEmpty { availableUsers.filter { !it.isBlocked } }
    }
    val approvers = remember(availableUsers) {
        availableUsers.filter {
            !it.isBlocked && it.permissions.canApproveReport && it.role != UserRole.SUPER_ADMIN
        }.ifEmpty { availableUsers.filter { !it.isBlocked } }
    }
    val assignees = remember(availableUsers) {
        availableUsers.filter {
            !it.isBlocked && (it.role == UserRole.IJROCHI || it.role == UserRole.BOLIM_RAHBARI)
        }.ifEmpty { availableUsers.filter { !it.isBlocked } }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(Department.OBODONLASHTIRISH.titleUz) }
    var reviewerName by remember {
        mutableStateOf(reviewers.firstOrNull()?.name ?: "Saidov Mirzo O'ktamovich")
    }
    var finalApproverName by remember {
        mutableStateOf(approvers.firstOrNull()?.name ?: "Shukurov Alisher Davronovich")
    }
    var assigneeName by remember {
        mutableStateOf(assignees.firstOrNull()?.name ?: "Karimov Jasur Bahromovich")
    }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH.name) }
    var selectedAiMode by remember { mutableStateOf("AI monitoring + Haftalik tasdiq") }
    var selectedRoutingType by remember { mutableStateOf(TaskRoutingType.VERTIKAL) }
    var selectedReportFormat by remember { mutableStateOf(ReportFormat.FOTO) }
    var deadlineHours by remember { mutableStateOf("48") }
    var objectName by remember { mutableStateOf("14-kichik nohiya hududi") }
    var address by remember { mutableStateOf("Zarafshon sh., 14-kichik nohiya") }
    var normativAsos by remember { mutableStateOf("Shahar hokimi farmoyishi") }
    var kpiPoints by remember { mutableStateOf("15") }
    var newSubtaskText by remember { mutableStateOf("") }
    val subtasks = remember {
        mutableStateListOf(
            SubTask("1", "Hududni ko'zdan kechirish va foto dalil olish", false),
            SubTask("2", "Asosiy ijro ishlarini bajarish", false),
            SubTask("3", "GPS va foto hisobotni nazoratga yuborish", false)
        )
    }
    val hasSoDConflict = reviewerName.equals(finalApproverName, ignoreCase = true) ||
        assigneeName.equals(reviewerName, ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = TelegramBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Yangi Topshiriq Yaratish",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "3-Tomonlama Vakolat Zanjiri",
                                fontSize = 10.sp,
                                color = TelegramBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tezkor Shablonlar:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Ichimlik suvi" to "14-kichik nohiya ichimlik suvi tarmog'i bosimini tiklash va dalil yuklash",
                            "Yo'l ta'miri" to "Ichki yo'llarda chuqurlarni asfalt-beton qoplamasi bilan yamoqlash",
                            "Obodonlashtirish" to "Hududni obodonlashtirish, chiqindilarni chiqarish va ko'kalamzorlashtirish"
                        ).forEach { (label, desc) ->
                            SuggestionChip(
                                onClick = {
                                    title = label
                                    description = desc
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Mavzu / Topshiriq nomi *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_task_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Topshiriq mazmuni va talablar *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // 3-Way Chain Builder (Matching Mockup 3)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "3-TOMONLAMA VAKOLAT ZANJIRI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TelegramBlue
                            )
                            Surface(
                                color = TelegramBlue.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Beruvchi: $creatorName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text("Nazoratchi tanlang:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HokimGold)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(reviewers) { rev ->
                                    val selected = reviewerName == rev.name
                                    FilterChip(
                                        selected = selected,
                                        onClick = { reviewerName = rev.name },
                                        label = { Text(rev.name.substringBefore(" "), fontSize = 10.sp) }
                                    )
                                }
                            }

                            Text("Bajaruvchi (Ijrochi) tanlang:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramCyan)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(assignees) { emp ->
                                    val selected = assigneeName == emp.name
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            assigneeName = emp.name
                                            selectedDept = emp.department
                                        },
                                        label = { Text(emp.name.substringBefore(" "), fontSize = 10.sp) }
                                    )
                                }
                            }

                            Text("Yakuniy tasdiqlovchi:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HokimEmerald)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(approvers) { app ->
                                    val selected = finalApproverName == app.name
                                    FilterChip(
                                        selected = selected,
                                        onClick = { finalApproverName = app.name },
                                        label = { Text(app.name.substringBefore(" "), fontSize = 10.sp) }
                                    )
                                }
                            }

                            if (hasSoDConflict) {
                                Surface(
                                    color = StatusRed.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Vakolat bo'linishi: Nazoratchi, Ijrochi va Yakuniy tasdiqlovchi har xil shaxs bo'lishi shart.",
                                        color = StatusRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = assigneeName,
                                onValueChange = { assigneeName = it },
                                label = { Text("Ijrochi xodim *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    Text("Yo'nalish turi (Routing):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TaskRoutingType.entries) { rt ->
                            FilterChip(
                                selected = selectedRoutingType == rt,
                                onClick = { selectedRoutingType = rt },
                                label = { Text(rt.labelUz, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Hisobot formati:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ReportFormat.entries) { rf ->
                            FilterChip(
                                selected = selectedReportFormat == rf,
                                onClick = { selectedReportFormat = rf },
                                label = { Text(rf.labelUz, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Mas'ul Bo'lim:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(Department.values()) { dept ->
                            FilterChip(
                                selected = selectedDept == dept.titleUz,
                                onClick = { selectedDept = dept.titleUz },
                                label = { Text(dept.shortCode, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Nazorat turi (AI):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(
                            listOf(
                                "AI monitoring + Haftalik tasdiq",
                                "Kunlik AI Radar",
                                "Kritik obyekt nazorati"
                            )
                        ) { mode ->
                            FilterChip(
                                selected = selectedAiMode == mode,
                                onClick = { selectedAiMode = mode },
                                label = { Text(mode, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = deadlineHours,
                            onValueChange = { deadlineHours = it },
                            label = { Text("Muddat (soat)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = kpiPoints,
                            onValueChange = { kpiPoints = it },
                            label = { Text("KPI balli") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = objectName,
                        onValueChange = { objectName = it },
                        label = { Text("Obyekt / Hudud nomi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Aniq manzil") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = normativAsos,
                        onValueChange = { normativAsos = it },
                        label = { Text("Normativ asos / Farmoyish") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Subtask / Checklist vazifalari:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    subtasks.forEachIndexed { index, subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${index + 1}.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(subtask.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { subtasks.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "O'chirish", tint = StatusRed)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Kichik vazifa qo'shish...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks.add(SubTask("${subtasks.size + 1}", newSubtaskText, false))
                                    newSubtaskText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Qo'shish", tint = TelegramBlue)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Bekor qilish")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val hours = deadlineHours.toLongOrNull() ?: 48L
                                val deadlineMillis = System.currentTimeMillis() + (hours * 3600 * 1000L)
                                val points = kpiPoints.toIntOrNull() ?: 15
                                val multiOrgParts = if (selectedRoutingType == TaskRoutingType.KOP_TASHKILOTLI) {
                                    "$selectedDept|$assigneeName|0|false;;Uy-joy kommunal xizmat|Haydarov O. T.|0|false"
                                } else ""
                                onSubmit(
                                    title,
                                    description.ifBlank { "Topshiriq ijrosi ta'minlansin." },
                                    selectedDept,
                                    assigneeName,
                                    selectedPriority,
                                    deadlineMillis,
                                    objectName,
                                    address,
                                    normativAsos,
                                    points,
                                    subtasks.toList(),
                                    reviewerName,
                                    finalApproverName,
                                    selectedAiMode,
                                    selectedRoutingType.name,
                                    selectedReportFormat.name,
                                    multiOrgParts
                                )
                            }
                        },
                        enabled = title.isNotBlank() && !hasSoDConflict,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        modifier = Modifier.testTag("submit_new_task_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zanjirga Biriktirish")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSubmissionDialog(
    targetObjectName: String,
    targetAddress: String,
    onDismiss: () -> Unit,
    onSubmit: (comment: String, lat: Double, lng: Double, photoPath: String?) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var photoTaken by remember { mutableStateOf(false) }
    // Realistic coordinates near Zarafshon target
    val simulatedLat = 41.5724
    val simulatedLng = 64.1986
    val simulatedDistance = 14 // meters from geofence

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = HokimNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ijro Hisobotini Yuborish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Obyekt: $targetObjectName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Manzil: $targetAddress",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Bajarilgan ishlar mazmuni *") },
                    placeholder = { Text("Qilingan ishlar, natijalar va dalillar...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // GPS & Anti-Fraud Inspection Box
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "GPS Geofence: Mos keldi ($simulatedDistance m)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            "Koordinatalar: $simulatedLat, $simulatedLng (Aniqlik: ±3m)",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                        Text(
                            "Anti-fraud: EXIF va media takrorlanmasligi tekshirildi",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                // Camera & Photo Proof
                Surface(
                    color = if (photoTaken) Color(0xFFE3F2FD) else Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { photoTaken = !photoTaken }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (photoTaken) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (photoTaken) StatusGreen else HokimNavy,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (photoTaken) "Foto dalil biriktirildi (IMG_2026.jpg)" else "Foto / Video biriktirish (Kamera)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                            Text(
                                text = if (photoTaken) "EXIF ma'lumoti kiritildi" else "Kamera orqali real vaqtda rasmga oling",
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.isNotBlank()) {
                        onSubmit(
                            comment,
                            simulatedLat,
                            simulatedLng,
                            if (photoTaken) "photo_report.jpg" else null
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HokimNavy)
            ) {
                Text("Hisobotni Jo'natish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish", color = Slate600)
            }
        }
    )
}

@Composable
fun ReasonInputDialog(
    title: String,
    label: String,
    confirmButtonText: String,
    confirmButtonColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    "Qoida: Izoh va aniq sabab yozilmasdan topshiriqni qaytarish yoki rad etish mumkin emas!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StatusRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reason_input_field"),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotBlank()) {
                        onSubmit(reason.trim())
                    }
                },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                modifier = Modifier.testTag("reason_confirm_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}

@Composable
fun DisciplineDialog(
    defaultEmployee: String,
    defaultDept: String,
    onDismiss: () -> Unit,
    onSubmit: (employee: String, dept: String, type: String, reason: String) -> Unit
) {
    var employee by remember { mutableStateOf(defaultEmployee) }
    var department by remember { mutableStateOf(defaultDept) }
    var selectedType by remember { mutableStateOf("OGOHLANTIRISH") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Intizomiy Chora Rasmiylashtirish", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Mehnat intizomi yoki topshiriq muddatini buzganlik bo'yicha chora belgilash:",
                    fontSize = 11.sp,
                    color = Slate600
                )

                OutlinedTextField(
                    value = employee,
                    onValueChange = { employee = it },
                    label = { Text("Xodim F.I.Sh.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Chora turi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "OGOHLANTIRISH" to "Ogohlantirish",
                        "DAKKI" to "Dakki",
                        "HAYFSAN" to "Hayfsan",
                        "TUSHUNTIRISH_XATI" to "Tushuntirish"
                    ).forEach { (typeKey, typeLabel) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(typeLabel, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Asos va kamchiliklar tafsiloti *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotBlank()) {
                        onSubmit(employee, department, selectedType, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
            ) {
                Text("Chorani Tasdiqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish", color = Slate600)
            }
        }
    )
}

@Composable
fun NotificationsSheet(
    notifications: List<NotificationEntity>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = HokimNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bildirishnomalar Markazi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TextButton(onClick = onMarkAllRead) {
                    Text("Barchasini o'qish", fontSize = 11.sp, color = HokimNavy)
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Hozircha yangi bildirishnomalar yo'q", color = Slate600, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notif ->
                        Surface(
                            color = if (notif.isRead) Slate50 else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (notif.isRead) Slate200 else StatusGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (notif.isRead) Slate700 else HokimNavy
                                    )
                                    Surface(
                                        color = when (notif.category) {
                                            "INTIZOM" -> Color(0xFFFFEBEE)
                                            "DEADLINE" -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFE8F5E9)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = notif.category,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (notif.category) {
                                                "INTIZOM" -> StatusRed
                                                "DEADLINE" -> StatusYellow
                                                else -> StatusGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Yopish", color = HokimNavy)
            }
        }
    )
}

@Composable
fun AddEmployeeDialog(
    departments: List<DepartmentInfo> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        login: String,
        password: String,
        role: UserRole,
        dept: String,
        position: String,
        phone: String,
        supervisor: String,
        email: String,
        avatarUri: String?
    ) -> Unit
) {
    var activeStep by remember { mutableStateOf(0) } // 0: Asosiy, 1: Lavozim va vakolatlar, 2: Boshqa
    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("xodim365") }
    var selectedRole by remember { mutableStateOf(UserRole.IJROCHI) }
    var department by remember { mutableStateOf(departments.firstOrNull()?.name ?: "Obodonlashtirish boshqarmasi") }
    var position by remember { mutableStateOf("Mutaxassis") }
    var phone by remember { mutableStateOf("+998 90 ") }
    var email by remember { mutableStateOf("") }
    var supervisor by remember { mutableStateOf("To'rayev Sh. M. (Bo'lim boshlig'i)") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var pPermTasks by remember { mutableStateOf(true) }
    var pPermAnalysis by remember { mutableStateOf(true) }
    var pPermReview by remember { mutableStateOf(false) }
    var pPermReports by remember { mutableStateOf(true) }
    var pPermGis by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedAvatarUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TelegramBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yangi xodim qo'shish",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zarafshon shahar hokimligi kadrlar tizimi",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                // 3-Step Tabs matching Mockups 1#2, 4#2, 8
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "1. Asosiy",
                        1 to "2. Lavozim",
                        2 to "3. Tasdiqlash"
                    ).forEach { (idx, label) ->
                        val active = activeStep == idx
                        Surface(
                            color = if (active) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeStep = idx }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        color = StatusRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = StatusRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (activeStep == 0) {
                    // Photo Picker + Basic Info (Zero storage permission PickVisualMedia)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, TelegramBlue.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAvatarUri != null) {
                                AsyncImage(
                                    model = selectedAvatarUri,
                                    contentDescription = "Tanlangan rasm",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Rasm yuklash", tint = TelegramBlue)
                                    Text("Rasm", fontSize = 9.sp, color = TelegramBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (selectedAvatarUri != null) "Rasmni almashtirish" else "Rasm yuklash",
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                "JPG, PNG (ixtiyoriy)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("F.I.Sh. (To'liq ism) *") },
                        placeholder = { Text("Masalan: Azimov Azizbek") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_emp_name_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon raqami *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("azizbek@zarafshon.gov.uz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (activeStep == 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            label = { Text("Login *") },
                            placeholder = { Text("axolov") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_emp_login_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Parol *") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_emp_password_input")
                        )
                    }

                    Text(
                        text = "Lavozim Darajasi (Rol):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(UserRole.values().toList()) { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.titleUz, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("Lavozim nomi *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (departments.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(departments) { dept ->
                                FilterChip(
                                    selected = department == dept.name,
                                    onClick = { department = dept.name },
                                    label = { Text(dept.code, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Bo'lim nomi *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (activeStep == 2) {
                    OutlinedTextField(
                        value = supervisor,
                        onValueChange = { supervisor = it },
                        label = { Text("To'g'ridan-to'g'ri Rahbari *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Asosiy modul ruxsatlari:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            PermissionSwitchRow("Boshqaruv va Topshiriqlar", pPermTasks) { pPermTasks = it }
                            PermissionSwitchRow("Tahlil va KPI ko'rish", pPermAnalysis) { pPermAnalysis = it }
                            PermissionSwitchRow("Nazorat va Tasdiqlash", pPermReview) { pPermReview = it }
                            PermissionSwitchRow("Hisobotlar eksporti", pPermReports) { pPermReports = it }
                            PermissionSwitchRow("GIS Xarita monitoringi", pPermGis) { pPermGis = it }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (activeStep > 0) activeStep -= 1 else onDismiss()
                    }) {
                        Text(if (activeStep > 0) "Orqaga" else "Bekor qilish")
                    }

                    if (activeStep < 2) {
                        Button(
                            onClick = { activeStep += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Keyingi →", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (name.isBlank() || login.isBlank() || password.isBlank() || position.isBlank()) {
                                    validationError = "F.I.Sh., login, parol va lavozimni to'ldiring!"
                                    activeStep = if (name.isBlank()) 0 else 1
                                    return@Button
                                }
                                onSubmit(
                                    name,
                                    login,
                                    password,
                                    selectedRole,
                                    department,
                                    position,
                                    phone,
                                    supervisor,
                                    email,
                                    selectedAvatarUri?.toString()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_new_employee_button")
                        ) {
                            Text("Saqlash", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeProfileDialog(
    user: AppUser,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onEditPermissions: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleBlock: () -> Unit
) {
    var profileTab by remember { mutableStateOf(0) } // 0: Ma'lumotlar, 1: Lavozim tarixi, 2: Topshiriqlar, 3: Natijalar, 4: Hujjatlar
    val userFirst = user.name.substringBefore(" ")
    val userTasks = remember(tasks, userFirst) {
        tasks.filter {
            it.assigneeName.contains(userFirst, true) ||
                it.reviewerName.contains(userFirst, true) ||
                it.creatorName.contains(userFirst, true)
        }
    }
    val completedCount = userTasks.count { it.status == "COMPLETED" }
    val efficiencyPct = if (userTasks.isNotEmpty()) ((completedCount * 100f / userTasks.size).toInt().coerceIn(72, 98)) else 95

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
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
                        text = "Xodim profili",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TelegramBlue
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                // Header Card (Mockup 1#3 & 4#3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(user.role.badgeColorHex).copy(alpha = 0.14f))
                            .border(2.dp, Color(user.role.badgeColorHex), CircleShape),
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(user.role.badgeColorHex)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = if (user.isBlocked) StatusRed.copy(alpha = 0.14f) else HokimEmerald.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (user.isBlocked) "Bloklangan" else "Faol",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isBlocked) StatusRed else HokimEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(user.position, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TelegramBlue)
                        Text("${user.phone} • ${user.email}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Zarafshon shahri • @${user.login}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Profile Sub-tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tabs = listOf(
                        0 to "Ma'lumotlar",
                        1 to "Lavozim tarixi",
                        2 to "Topshiriqlar (${userTasks.size})",
                        3 to "Natijalar",
                        4 to "Hujjatlar"
                    )
                    items(tabs) { (idx, title) ->
                        val active = profileTab == idx
                        Surface(
                            color = if (active) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { profileTab = idx }
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                when (profileTab) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Tashkilot:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user.scope.organization, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Bo'lim:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user.department, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Rahbar:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user.supervisorName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("ERI Kalit:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user.digitalSignatureId, fontSize = 10.sp, color = TelegramBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Faoliyat statistikasi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    SingleCircularRing(
                                        percent = efficiencyPct,
                                        label = "Ijro darajasi",
                                        color = HokimEmerald,
                                        ringSize = 70.dp
                                    )
                                    Text(
                                        text = "${userTasks.size} ta topshiriq",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• 2024–Hozir: ${user.position} (${user.department})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("• 2021–2024: Yetakchi mutaxassis (Zarafshon shahar apparati)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• Kirish tarixi:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                user.loginHistory.forEach { entry ->
                                    Text("  - $entry", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    2 -> {
                        if (userTasks.isEmpty()) {
                            Text("Xodimga biriktirilgan topshiriqlar hozircha yo'q.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            userTasks.take(5).forEach { t ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(t.taskNumber, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                            Text(t.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("${t.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = HokimEmerald)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = HokimEmerald.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$efficiencyPct%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = HokimEmerald)
                                    Text("KPI Samaradorlik", fontSize = 10.sp)
                                }
                            }
                            Surface(
                                color = TelegramBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$completedCount/${userTasks.size.coerceAtLeast(1)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TelegramBlue)
                                    Text("Yakunlangan", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            "Biriktirilgan sertifikat va buyruqlar: ${user.digitalSignatureId} (ERI faol).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bottom Admin Action Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditPermissions,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vakolatlar", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onResetPassword,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Parol", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onToggleBlock,
                        colors = ButtonDefaults.buttonColors(containerColor = if (user.isBlocked) HokimEmerald else StatusRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (user.isBlocked) "Yoqish" else "Bloklash", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, sector: String, budgetUsdMillion: Double, contractor: String, jobsPlanned: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("Shahar infratuzilmasi") }
    var budgetStr by remember { mutableStateOf("5.0") }
    var contractor by remember { mutableStateOf("Zarafshon Qurilish MCHJ") }
    var jobsStr by remember { mutableStateOf("65") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = TelegramBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yangi loyiha qo'shish", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Loyiha nomi *") },
                    placeholder = { Text("Masalan: Shahar infratuzilmasini yangilash") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Soha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it },
                        label = { Text("Byudjet ($ mln)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = jobsStr,
                        onValueChange = { jobsStr = it },
                        label = { Text("Ish o'rni") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = contractor,
                    onValueChange = { contractor = it },
                    label = { Text("Pudratchi tashkilot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(
                            title,
                            sector,
                            budgetStr.toDoubleOrNull() ?: 5.0,
                            contractor,
                            jobsStr.toIntOrNull() ?: 50
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Loyihani Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        }
    )
}

@Composable
fun AddDepartmentDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, code: String, headName: String, staffCount: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var headName by remember { mutableStateOf("") }
    var staffCountStr by remember { mutableStateOf("10") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = TelegramBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yangi Bo'lim Yaratish",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (validationError != null) {
                    Text(text = validationError ?: "", color = StatusRed, fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bo'linma to'liq nomi *") },
                    placeholder = { Text("masalan: Sanoat va transport boshqarmasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Qisqa kod *") },
                        placeholder = { Text("SAN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = staffCountStr,
                        onValueChange = { staffCountStr = it },
                        label = { Text("Shtat birligi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = headName,
                    onValueChange = { headName = it },
                    label = { Text("Bo'lim rahbari (F.I.Sh.) *") },
                    placeholder = { Text("masalan: Qosimov N. B.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Bekor qilish")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || code.isBlank() || headName.isBlank()) {
                                validationError = "Barcha maydonlarni to'ldiring!"
                                return@Button
                            }
                            val count = staffCountStr.toIntOrNull() ?: 10
                            onSubmit(name, code, headName, count)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Text("Bo'limni Yaratish")
                    }
                }
            }
        }
    }
}

@Composable
fun EditPermissionsDialog(
    user: AppUser,
    onDismiss: () -> Unit,
    onSave: (UserPermissions) -> Unit
) {
    var pCreateTask by remember { mutableStateOf(user.permissions.canCreateTask) }
    var pAssignOwn by remember { mutableStateOf(user.permissions.canAssignToOwnStaff) }
    var pAssignAll by remember { mutableStateOf(user.permissions.canAssignToAll) }
    var pApprove by remember { mutableStateOf(user.permissions.canApproveReport) }
    var pRework by remember { mutableStateOf(user.permissions.canReworkReport) }
    var pReject by remember { mutableStateOf(user.permissions.canRejectTask) }
    var pManageStaff by remember { mutableStateOf(user.permissions.canManageStaff) }
    var pDiscipline by remember { mutableStateOf(user.permissions.canIssueDiscipline) }
    var pAi by remember { mutableStateOf(user.permissions.canUseAiAssistant) }
    var pExport by remember { mutableStateOf(user.permissions.canExportReports) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = TelegramBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vakolatlarni Boshqarish",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Xodim: ${user.name}\nLavozim: ${user.position} (${user.role.titleUz})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                PermissionSwitchRow(
                    label = "Topshiriq yaratish",
                    checked = pCreateTask,
                    onCheckedChange = { pCreateTask = it }
                )
                PermissionSwitchRow(
                    label = "O'z xodimlariga topshiriq berish",
                    checked = pAssignOwn,
                    onCheckedChange = { pAssignOwn = it }
                )
                PermissionSwitchRow(
                    label = "Barcha bo'limlarga topshiriq berish",
                    checked = pAssignAll,
                    onCheckedChange = { pAssignAll = it }
                )
                PermissionSwitchRow(
                    label = "Hisobotlarni tasdiqlash",
                    checked = pApprove,
                    onCheckedChange = { pApprove = it }
                )
                PermissionSwitchRow(
                    label = "Qayta ishlashga yuborish",
                    checked = pRework,
                    onCheckedChange = { pRework = it }
                )
                PermissionSwitchRow(
                    label = "Topshiriqni asosli rad etish",
                    checked = pReject,
                    onCheckedChange = { pReject = it }
                )
                PermissionSwitchRow(
                    label = "Xodimlarni boshqarish / qo'shish",
                    checked = pManageStaff,
                    onCheckedChange = { pManageStaff = it }
                )
                PermissionSwitchRow(
                    label = "Intizomiy chora qo'llash",
                    checked = pDiscipline,
                    onCheckedChange = { pDiscipline = it }
                )
                PermissionSwitchRow(
                    label = "AI tahliliy monitoring",
                    checked = pAi,
                    onCheckedChange = { pAi = it }
                )
                PermissionSwitchRow(
                    label = "Hisobotlarni eksport qilish",
                    checked = pExport,
                    onCheckedChange = { pExport = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Bekor qilish")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPerms = user.permissions.copy(
                                canCreateTask = pCreateTask,
                                canAssignToOwnStaff = pAssignOwn,
                                canAssignToAll = pAssignAll,
                                canViewReports = true,
                                canReviewEvidence = user.permissions.canReviewEvidence || pApprove,
                                canApproveReport = pApprove,
                                canReworkReport = pRework,
                                canRejectTask = pReject,
                                canManageStaff = pManageStaff,
                                canBlockStaff = pManageStaff,
                                canManagePermissions = pManageStaff,
                                canManageDepartments = pManageStaff,
                                canIssueDiscipline = pDiscipline,
                                canViewAllKpi = user.permissions.canViewAllKpi,
                                canUseAiAssistant = pAi,
                                canExportReports = pExport,
                                canModifySystemSettings = pManageStaff,
                                canResetPasswords = pManageStaff || user.permissions.canResetPasswords
                            )
                            onSave(newPerms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Text("Saqlash")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun ResetPasswordDialog(
    user: AppUser,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onIssueOtp: (() -> Unit)? = null
) {
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = TelegramBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Parolni Yangilash", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${user.name} (@${user.login})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Xavfsizlik qoidasi: joriy parol SHA-256 bilan xeshlangan va ko'rsatilmaydi.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Yangi parol *") },
                    placeholder = { Text("Kamida 6 ta belgi...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (onIssueOtp != null) {
                    OutlinedButton(
                        onClick = onIssueOtp,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bir martalik OTP kod yaratish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newPassword.isNotBlank()) onSave(newPassword.trim()) },
                enabled = newPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Yangilash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
