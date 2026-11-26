package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TaskEntity
import com.example.ui.theme.*

data class GisPin(
    val id: Long,
    val name: String,
    val category: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val statusLabel: String,
    val statusColor: Color,
    val xRatio: Float,
    val yRatio: Float,
    val taskId: Long,
    val imageResId: Int
)

@Composable
fun GisMapScreen(
    tasks: List<TaskEntity>,
    onSelectTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allPins = remember(tasks) {
        listOf(
            GisPin(
                id = 1,
                name = "12-kichik nohiya Suv Stansiyasi",
                category = "Muammo",
                address = "Zarafshon sh., 12-kichik nohiya, 24-42 uylar",
                lat = 41.5725,
                lng = 64.1985,
                radiusMeters = 150,
                statusLabel = "Kritik • 23 murojaat",
                statusColor = StatusRed,
                xRatio = 0.34f,
                yRatio = 0.44f,
                taskId = 1,
                imageResId = R.drawable.img_water_infra_1795671435228
            ),
            GisPin(
                id = 2,
                name = "Yangi Zarafshon 14-bino",
                category = "Qurilish",
                address = "Yangi Zarafshon massivi, 14-bino",
                lat = 41.5640,
                lng = 64.2150,
                radiusMeters = 200,
                statusLabel = "Qurilish 78%",
                statusColor = StatusYellow,
                xRatio = 0.68f,
                yRatio = 0.66f,
                taskId = 2,
                imageResId = R.drawable.img_city_construction_1795671434776
            ),
            GisPin(
                id = 3,
                name = "Markaziy Xiyobon va Favvoralar",
                category = "Obodonlashtirish",
                address = "Zarafshon sh., Navoiy shoh ko'chasi",
                lat = 41.5768,
                lng = 64.2045,
                radiusMeters = 120,
                statusLabel = "Barqaror • 96%",
                statusColor = StatusGreen,
                xRatio = 0.49f,
                yRatio = 0.28f,
                taskId = 3,
                imageResId = R.drawable.zarafshon_banner_1790585492174
            ),
            GisPin(
                id = 4,
                name = "Sanoat Zonasi 7-sektor",
                category = "Muammo",
                address = "Zarafshon sanoat xabi, Janubiy yo'l",
                lat = 41.5890,
                lng = 64.2310,
                radiusMeters = 300,
                statusLabel = "Ekologiya nazorati",
                statusColor = StatusRed,
                xRatio = 0.81f,
                yRatio = 0.20f,
                taskId = 4,
                imageResId = R.drawable.img_command_hero_1795671436085
            ),
            GisPin(
                id = 5,
                name = "IT-Park va Yoshlar Saroyi",
                category = "Investitsiya",
                address = "Zarafshon sh., Yoshlar ko'chasi 5-uy",
                lat = 41.5710,
                lng = 64.2090,
                radiusMeters = 100,
                statusLabel = "Investitsiya • 94%",
                statusColor = TelegramBlue,
                xRatio = 0.47f,
                yRatio = 0.56f,
                taskId = 5,
                imageResId = R.drawable.img_command_hero_1795671436085
            )
        )
    }

    val categories = listOf("Barchasi", "Muammo", "Qurilish", "Investitsiya", "Obodonlashtirish")
    var selectedCategory by remember { mutableStateOf("Barchasi") }

    val visiblePins = remember(allPins, selectedCategory) {
        if (selectedCategory == "Barchasi") allPins
        else allPins.filter { it.category == selectedCategory }
    }

    var selectedPin by remember(visiblePins) { mutableStateOf(visiblePins.firstOrNull()) }

    val mapBg = if (MaterialTheme.colorScheme.background == Color(0xFF0B1424)) {
        Color(0xFF101C33)
    } else {
        Color(0xFFE5ECF4)
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Map Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(TelegramHeaderGradient, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SHAHAR 360° • GIS XARITA",
                        color = HokimGoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Zarafshon (41.5714° N, 64.2008° E)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("${visiblePins.size} obyekt", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("gis_filter_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive Map Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(mapBg)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(visiblePins) {
                        detectTapGestures { offset ->
                            val tapped = visiblePins.minByOrNull { pin ->
                                val px = pin.xRatio * size.width
                                val py = pin.yRatio * size.height
                                val dx = offset.x - px
                                val dy = offset.y - py
                                dx * dx + dy * dy
                            }
                            if (tapped != null) {
                                selectedPin = tapped
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                for (i in 1..8) {
                    val y = h * (i / 9f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }
                for (i in 1..8) {
                    val x = w * (i / 9f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
                }

                val mainRoad = Path().apply {
                    moveTo(0f, h * 0.45f)
                    lineTo(w * 0.45f, h * 0.40f)
                    lineTo(w, h * 0.30f)
                }
                drawPath(mainRoad, color = Color(0xFF94A3B8).copy(alpha = 0.7f), style = Stroke(width = 8f))

                val branchRoad = Path().apply {
                    moveTo(w * 0.45f, 0f)
                    lineTo(w * 0.45f, h)
                }
                drawPath(branchRoad, color = Color(0xFF94A3B8).copy(alpha = 0.7f), style = Stroke(width = 6f))

                drawCircle(
                    color = StatusGreen.copy(alpha = 0.16f),
                    radius = 74f,
                    center = Offset(w * 0.49f, h * 0.28f)
                )

                drawRect(
                    color = StatusRed.copy(alpha = 0.10f),
                    topLeft = Offset(w * 0.70f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.34f)
                )

                visiblePins.forEach { pin ->
                    val px = pin.xRatio * w
                    val py = pin.yRatio * h
                    val isSelected = selectedPin?.id == pin.id

                    val geofenceRadiusPx = pin.radiusMeters * 0.30f
                    drawCircle(
                        color = pin.statusColor.copy(alpha = if (isSelected) 0.26f else 0.12f),
                        radius = geofenceRadiusPx,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pin.statusColor.copy(alpha = 0.65f),
                        radius = geofenceRadiusPx,
                        center = Offset(px, py),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = if (isSelected) HokimGoldLight else Color.White,
                        radius = if (isSelected) 16f else 12f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pin.statusColor,
                        radius = if (isSelected) 11f else 8f,
                        center = Offset(px, py)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Zarafshon Bosh Rejasi • Nuqtani bosing",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Pin Detail Sheet Card with Object Photo
        selectedPin?.let { pin ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = pin.imageResId),
                            contentDescription = pin.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = pin.statusColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${pin.category} • ${pin.statusLabel}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pin.statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pin.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pin.address,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "GPS: ${pin.lat}, ${pin.lng} • Geofence ${pin.radiusMeters}m",
                                fontSize = 9.sp,
                                color = TelegramBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onSelectTask(pin.taskId) },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("gis_open_task_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Obyekt Topshirig'ini Ko'rish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
