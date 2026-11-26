package com.example.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.BoshqaruvRepository
import com.example.data.DisciplinaryEntity
import com.example.data.TaskEntity
import com.example.model.HealthCategoryWeight
import com.example.model.InvestmentProject
import com.example.model.MahallaProfile
import com.example.ui.components.CityHealthGaugeChart
import com.example.ui.components.DonutSegment
import com.example.ui.components.HorizontalBarComparisonChart
import com.example.ui.components.MahallaHeatmapGrid
import com.example.ui.components.TaskDonutChart
import com.example.ui.components.TrendMultiLineChart
import com.example.ui.theme.*

data class OrgRankRow(
    val orgName: String,
    val leader: String,
    val kpiScore: Double,
    val tasksDone: Int,
    val delayed: Int
)

@Composable
fun AnalyticsKpiScreen(
    tasks: List<TaskEntity>,
    disciplinaryActions: List<DisciplinaryEntity>,
    cityHealthScore: Int,
    healthWeights: List<HealthCategoryWeight>,
    mahallas: List<MahallaProfile>,
    investments: List<InvestmentProject>,
    onOpenHealthEditor: () -> Unit,
    onExportClick: (String) -> Unit,
    onIssueDisciplineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(0) } // 0: Diagrammalar, 1: Mahallalar, 2: Jadval & Eksport, 3: Investitsiya
    var tableSearch by remember { mutableStateOf("") }
    var sortByScoreDesc by remember { mutableStateOf(true) }
    var expandedChartTitle by remember { mutableStateOf<String?>(null) }
    var selectedMahalla by remember { mutableStateOf<MahallaProfile?>(null) }

    val now = System.currentTimeMillis()
    val totalTasks = tasks.size
    val completedCount = tasks.count { it.status == "COMPLETED" }
    val inProgressCount = tasks.count { it.status == "IN_PROGRESS" || it.status == "ACCEPTED" || it.status == "ASSIGNED" }
    val reviewCount = tasks.count { it.status == "REVIEW" || it.status == "SUBMITTED" }
    val overdueCount = tasks.count { it.deadline < now && it.status != "COMPLETED" }
    val executionIndex = if (totalTasks > 0) (completedCount * 100 / totalTasks) else 89

    val donutSegments = remember(completedCount, inProgressCount, reviewCount, overdueCount) {
        listOf(
            DonutSegment("Bajarilgan", completedCount.coerceAtLeast(1), StatusGreen),
            DonutSegment("Jarayonda", inProgressCount.coerceAtLeast(1), TelegramBlue),
            DonutSegment("Tekshiruv", reviewCount.coerceAtLeast(1), StatusPurple),
            DonutSegment("Kechikkan", overdueCount, StatusRed)
        )
    }

    val deptBars = remember {
        listOf(
            Triple("Obodonlashtirish boshqarmasi", 96, StatusGreen),
            Triple("Yoshlar siyosati va sport", 93, StatusGreen),
            Triple("Qurilish va arxitektura", 84, TelegramBlue),
            Triple("Zarafshon Suv Ta'minoti", 79, StatusYellow),
            Triple("Ekologiya va atrof-muhit", 74, StatusRed)
        )
    }

    val orgRows = remember(tableSearch, sortByScoreDesc) {
        val base = listOf(
            OrgRankRow("Obodonlashtirish boshqarmasi", "Karimov Jasur", 96.4, 34, 0),
            OrgRankRow("Yoshlar siyosati bo'limi", "Yusupova Madina", 93.2, 27, 1),
            OrgRankRow("Qurilish va arxitektura", "Raxmonov Otabek", 84.5, 22, 2),
            OrgRankRow("Zarafshon Suv Ta'minoti", "To'rayev Sherzod", 79.1, 18, 4),
            OrgRankRow("Ekologiya inspeksiyasi", "Boboyev Bobur", 74.0, 14, 5)
        ).filter {
            tableSearch.isBlank() ||
                it.orgName.contains(tableSearch, ignoreCase = true) ||
                it.leader.contains(tableSearch, ignoreCase = true)
        }
        if (sortByScoreDesc) base.sortedByDescending { it.kpiScore } else base.sortedBy { it.delayed }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Telegram-style Sub-navigation Pill Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sections = listOf(
                0 to "Diagrammalar",
                1 to "Mahallalar",
                2 to "Jadval & Eksport",
                3 to "Investitsiya"
            )
            items(sections) { (idx, label) ->
                val selected = selectedSection == idx
                Surface(
                    color = if (selected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedSection = idx }
                        .testTag("analytics_tab_$idx")
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp)
        ) {
            if (selectedSection == 0) {
                // Health Index + Gauge Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ExecutiveHeaderGradient, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SHAHAR SOG'LIG'I VA IJRO INDEKSI",
                                    color = HokimGoldLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ijro samaradorligi: $executionIndex%",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "O'rtacha bajarish vaqti: 1.8 kun • Qayta ishlash: 4.1%",
                                    color = Slate200,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onOpenHealthEditor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Vaznlarni Sozlash", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            CityHealthGaugeChart(score = cityHealthScore)
                        }
                    }
                }

                // Donut Chart Card
                item {
                    ChartCard(
                        title = "TOPSHIRIQLAR HOLATI (DONUT DIAGRAMMA)",
                        onExpand = { expandedChartTitle = "Topshiriqlar Holati" }
                    ) {
                        TaskDonutChart(
                            segments = donutSegments,
                            centerValue = "$totalTasks",
                            centerLabel = "Jami"
                        )
                    }
                }

                // Multi-Line Trend Chart Card
                item {
                    ChartCard(
                        title = "HAFTALIK IJRO DINAMIKASI (TREND GRAFIGI)",
                        onExpand = { expandedChartTitle = "Haftalik Ijro Dinamikasi" }
                    ) {
                        TrendMultiLineChart(
                            currentSeries = listOf(76f, 80f, 82f, 85f, 87f, 89f, 92f),
                            previousSeries = listOf(70f, 73f, 75f, 74f, 79f, 81f, 84f),
                            labels = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")
                        )
                    }
                }

                // Horizontal Bar Comparison Chart
                item {
                    ChartCard(
                        title = "BO'LIMLAR SAMARADORLIGI (TAQQOSLAMA)",
                        onExpand = { expandedChartTitle = "Bo'limlar Samaradorligi" }
                    ) {
                        HorizontalBarComparisonChart(items = deptBars)
                    }
                }

                // 9 Weighted Categories Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SHAHAR SOG'LIG'I INDEKSI TARKIBI (9 YO'NALISH)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            healthWeights.forEach { cat ->
                                val c = when {
                                    cat.scorePercent >= 90 -> StatusGreen
                                    cat.scorePercent >= 80 -> TelegramBlue
                                    else -> StatusYellow
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${cat.name} (${cat.weightPercent}% vazn)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${cat.scorePercent}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = c
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSection == 1) {
                item {
                    Text(
                        text = "MAHALLALAR SOG'LIG'I ISSIQLIK XARITASI (HEATMAP)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MahallaHeatmapGrid(
                        mahallas = mahallas,
                        onMahallaClick = { selectedMahalla = it }
                    )
                }

                items(mahallas, key = { it.id }) { m ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedMahalla = m }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Mahalla raisi: ${m.coordinator} • Aholi: ${m.population} nafar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = if (m.healthScore >= 90) HokimEmeraldBg else HokimGoldBg,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${m.healthScore}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (m.healthScore >= 90) StatusGreen else HokimGold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bajarilgan ishlar: ${m.completedTasks} ta", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Murojaat yopilishi: ${m.appealsResolvedPct}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                            }
                        }
                    }
                }
            }

            if (selectedSection == 2) {
                // Searchable / Sortable Leaderboard Table
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
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
                                Text(
                                    text = "TASHKILOTLAR ANALITIK JADVALI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilterChip(
                                    selected = true,
                                    onClick = { sortByScoreDesc = !sortByScoreDesc },
                                    label = {
                                        Text(
                                            if (sortByScoreDesc) "KPI ↓" else "Kechikish ↑",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = tableSearch,
                                onValueChange = { tableSearch = it },
                                placeholder = { Text("Tashkilot yoki rahbar qidirish...", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tashkilot / Rahbar", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                                Text("Bajar.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                                Text("Kech.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.55f))
                                Text("KPI", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f))
                            }

                            orgRows.forEachIndexed { idx, row ->
                                val scoreCol = when {
                                    row.kpiScore >= 90 -> StatusGreen
                                    row.kpiScore >= 80 -> TelegramBlue
                                    else -> StatusRed
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(
                                            text = "${idx + 1}. ${row.orgName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.leader,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text("${row.tasksDone}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.6f))
                                    Text(
                                        text = "${row.delayed}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.delayed > 2) StatusRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.55f)
                                    )
                                    Surface(
                                        color = scoreCol.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(0.65f)
                                    ) {
                                        Text(
                                            text = "${row.kpiScore}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = scoreCol,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (idx < orgRows.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }

                // Official Export Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "RASMIY HISOBOTLAR EKSPORTI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Shahar hokimligi ijro intizomi va Shahar 360° ko'rsatkichlarini Excel yoki imzolangan PDF formatida yuklab oling.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onExportClick("EXCEL") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_excel_button")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Excel (.xlsx)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onExportClick("PDF") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_pdf_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF Hisobot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Disciplinary Journal
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTIZOMIY CHORALAR JURNALI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onIssueDisciplineClick) {
                            Text("+ Chora qo'llash", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                        }
                    }
                }

                if (disciplinaryActions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Intizomiy choralar qayd etilmagan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(disciplinaryActions, key = { it.id }) { disc ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(disc.employeeName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = disc.actionType,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(disc.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Hujjat: ${disc.docNumber}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(BoshqaruvRepository.formatDate(disc.issueDate), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSection == 3) {
                item {
                    Text(
                        text = "INVESTITSIYA VA QURILISH LOYIHALARI ($25.1 MLN)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(investments, key = { it.id }) { inv ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = inv.imageResId),
                                    contentDescription = inv.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = if (inv.isDelayed) StatusRed else StatusGreen,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = inv.statusLabel,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "$${inv.budgetUsdMillion} mln",
                                        color = HokimGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(inv.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = "${inv.sector} • Pudratchi: ${inv.contractor}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reja bajarilishi: ${inv.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                    Text("${inv.jobsPlanned} ta ish o'rni", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = StatusGreen)
                                }
                                LinearProgressIndicator(
                                    progress = { inv.progressPercent / 100f },
                                    color = if (inv.isDelayed) StatusYellow else StatusGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Chart Expand Dialog
    expandedChartTitle?.let { chartTitle ->
        Dialog(
            onDismissRequest = { expandedChartTitle = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(chartTitle, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { expandedChartTitle = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    when (chartTitle) {
                        "Topshiriqlar Holati" -> TaskDonutChart(segments = donutSegments, centerValue = "$totalTasks", centerLabel = "Jami")
                        "Haftalik Ijro Dinamikasi" -> TrendMultiLineChart(
                            currentSeries = listOf(76f, 80f, 82f, 85f, 87f, 89f, 92f),
                            previousSeries = listOf(70f, 73f, 75f, 74f, 79f, 81f, 84f),
                            labels = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")
                        )
                        else -> HorizontalBarComparisonChart(items = deptBars)
                    }
                }
            }
        }
    }

    // Mahalla Detail Popup
    selectedMahalla?.let { m ->
        AlertDialog(
            onDismissRequest = { selectedMahalla = null },
            title = { Text(m.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mahalla raisi: ${m.coordinator}", fontSize = 12.sp)
                    Text("Aholi soni: ${m.population} nafar", fontSize = 12.sp)
                    Text("Sog'liq indeksi: ${m.healthScore}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                    Text("Ochiq muammolar: ${m.openIssues} ta", fontSize = 12.sp, color = if (m.openIssues > 0) StatusRed else StatusGreen)
                    Text("Bajarilgan topshiriqlar: ${m.completedTasks} ta", fontSize = 12.sp)
                    Text("Murojaatlar yopilishi: ${m.appealsResolvedPct}%", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { selectedMahalla = null }) {
                    Text("Yopish")
                }
            }
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    onExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onExpand, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Kattalashtirish", tint = TelegramBlue, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
