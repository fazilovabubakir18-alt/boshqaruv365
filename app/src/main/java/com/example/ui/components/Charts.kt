package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MahallaProfile
import com.example.ui.theme.*

data class DonutSegment(
    val label: String,
    val value: Int,
    val color: Color
)

@Composable
fun CityHealthGaugeChart(
    score: Int,
    modifier: Modifier = Modifier
) {
    val clamped = score.coerceIn(0, 100)
    val gaugeColor = when {
        clamped >= 85 -> StatusGreen
        clamped >= 70 -> StatusYellow
        else -> StatusRed
    }
    val statusText = when {
        clamped >= 85 -> "YAXSHI"
        clamped >= 70 -> "E'TIBOR"
        else -> "XAVFLI"
    }

    Box(
        modifier = modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(TelegramCyan, gaugeColor, HokimGoldLight)
                ),
                startAngle = 135f,
                sweepAngle = 270f * (clamped / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clamped%",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(
                color = gaugeColor.copy(alpha = 0.22f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun TaskDonutChart(
    segments: List<DonutSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1)
    val trackBg = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 0.18f
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = trackBg,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                segments.forEach { seg ->
                    if (seg.value > 0) {
                        val sweep = 360f * (seg.value.toFloat() / total)
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = centerLabel,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.forEach { seg ->
                val pct = (seg.value * 100f / total).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(seg.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = seg.label,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${seg.value} ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TrendMultiLineChart(
    currentSeries: List<Float>,
    previousSeries: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = TelegramBlue, label = "Joriy hafta (%)")
            LegendDot(color = Slate400, label = "O'tgan hafta (%)")
            LegendDot(color = StatusGreen, label = "Maqsad (90%)")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padTop = 12f
                val padBottom = 16f
                val chartH = h - padTop - padBottom

                // Horizontal grid lines (50%, 75%, 100%)
                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = padTop + chartH * ratio
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                // Target line at 90% (assuming scale 50..100)
                val targetY = padTop + chartH * (1f - ((90f - 50f) / 50f))
                drawLine(
                    color = StatusGreen.copy(alpha = 0.65f),
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 2.5f
                )

                fun pointsFor(series: List<Float>): List<Offset> {
                    if (series.isEmpty()) return emptyList()
                    val step = if (series.size > 1) w / (series.size - 1) else w
                    return series.mapIndexed { idx, v ->
                        val norm = ((v - 50f) / 50f).coerceIn(0f, 1f)
                        Offset(x = idx * step, y = padTop + chartH * (1f - norm))
                    }
                }

                val prevPts = pointsFor(previousSeries)
                if (prevPts.size >= 2) {
                    val prevPath = Path().apply {
                        moveTo(prevPts.first().x, prevPts.first().y)
                        for (i in 1 until prevPts.size) {
                            lineTo(prevPts[i].x, prevPts[i].y)
                        }
                    }
                    drawPath(
                        path = prevPath,
                        color = Slate400,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }

                val currPts = pointsFor(currentSeries)
                if (currPts.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(currPts.first().x, padTop + chartH)
                        currPts.forEach { lineTo(it.x, it.y) }
                        lineTo(currPts.last().x, padTop + chartH)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TelegramBlue.copy(alpha = 0.28f),
                                TelegramBlue.copy(alpha = 0.02f)
                            )
                        )
                    )

                    val linePath = Path().apply {
                        moveTo(currPts.first().x, currPts.first().y)
                        for (i in 1 until currPts.size) {
                            lineTo(currPts[i].x, currPts[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = TelegramBlue,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )

                    currPts.forEach { pt ->
                        drawCircle(color = Color.White, radius = 6.5f, center = pt)
                        drawCircle(color = TelegramBlue, radius = 4.5f, center = pt)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { lbl ->
                Text(
                    text = lbl,
                    fontSize = 9.sp,
                    color = labelColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HorizontalBarComparisonChart(
    items: List<Triple<String, Int, Color>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (name, pct, color) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$pct%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((pct.coerceIn(0, 100)) / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun MahallaHeatmapGrid(
    mahallas: List<MahallaProfile>,
    onMahallaClick: (MahallaProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = mahallas.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { m ->
                    val tileColor = when {
                        m.healthScore >= 90 -> StatusGreen
                        m.healthScore >= 82 -> StatusYellow
                        else -> StatusRed
                    }
                    Surface(
                        color = tileColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tileColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onMahallaClick(m) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${m.healthScore}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tileColor
                                )
                                Surface(
                                    color = if (m.openIssues > 0) tileColor else StatusGreen,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (m.openIssues > 0) "${m.openIssues} muammo" else "Barqaror",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = m.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Rais: ${m.coordinator}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MiniTrendSparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val minVal = points.minOrNull() ?: 0f
        val maxVal = (points.maxOrNull() ?: 1f).let { if (it == minVal) minVal + 1f else it }
        val step = w / (points.size - 1)
        val coords = points.mapIndexed { idx, v ->
            val norm = (v - minVal) / (maxVal - minVal)
            Offset(idx * step, h - (norm * (h - 6f)) - 3f)
        }

        val area = Path().apply {
            moveTo(coords.first().x, h)
            coords.forEach { lineTo(it.x, it.y) }
            lineTo(coords.last().x, h)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        val line = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val p0 = coords[i - 1]
                val p1 = coords[i]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SingleCircularRing(
    percent: Int,
    label: String,
    color: Color,
    ringSize: androidx.compose.ui.unit.Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val clamped = percent.coerceIn(0, 100)
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 0.13f
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (clamped / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$clamped%",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CityTrendsMultiSeriesChart(
    modifier: Modifier = Modifier
) {
    val months = listOf("Noy", "Dek", "Yan", "Fev", "Mar", "Apr")
    val execSeries = listOf(78f, 86f, 84f, 80f, 92f, 96f)
    val planSeries = listOf(66f, 76f, 72f, 68f, 80f, 94f)
    val effSeries = listOf(52f, 63f, 59f, 55f, 66f, 82f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = TelegramBlue, label = "Ijro darajasi")
            LegendDot(color = StatusGreen, label = "Reja bajarilishi")
            LegendDot(color = StatusPurple, label = "Samaradorlik")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padTop = 10f
                val padBottom = 10f
                val chartH = h - padTop - padBottom

                listOf(0f, 0.33f, 0.66f, 1f).forEach { r ->
                    val y = padTop + chartH * r
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                }

                fun drawSmooth(values: List<Float>, lineColor: Color, withArea: Boolean) {
                    val step = w / (values.size - 1)
                    val pts = values.mapIndexed { idx, v ->
                        val norm = ((v - 30f) / 70f).coerceIn(0f, 1f)
                        Offset(idx * step, padTop + chartH * (1f - norm))
                    }
                    if (withArea) {
                        val area = Path().apply {
                            moveTo(pts.first().x, padTop + chartH)
                            pts.forEachIndexed { idx, pt ->
                                if (idx == 0) lineTo(pt.x, pt.y) else {
                                    val prev = pts[idx - 1]
                                    val cx = (prev.x + pt.x) / 2f
                                    cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                }
                            }
                            lineTo(pts.last().x, padTop + chartH)
                            close()
                        }
                        drawPath(
                            path = area,
                            brush = Brush.verticalGradient(
                                listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
                            )
                        )
                    }

                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val pt = pts[i]
                            val cx = (prev.x + pt.x) / 2f
                            cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                        }
                    }
                    drawPath(path, color = lineColor, style = Stroke(width = 4.2f, cap = StrokeCap.Round))

                    pts.forEachIndexed { idx, pt ->
                        if (idx == 4) {
                            drawCircle(Color.White, radius = 8f, center = pt)
                            drawCircle(lineColor, radius = 5.5f, center = pt)
                        } else {
                            drawCircle(lineColor, radius = 3.5f, center = pt)
                        }
                    }
                }

                drawSmooth(effSeries, StatusPurple, false)
                drawSmooth(planSeries, StatusGreen, false)
                drawSmooth(execSeries, TelegramBlue, true)
            }

            // Tooltip pill matching Mockup 3 ("Mart • Ijro darajasi: 92%")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                    Text("Mart", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ijro darajasi: 92%", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TelegramBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { m ->
                Text(
                    text = m,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TripleKpiRingsCard(
    executionPct: Int,
    planPct: Int,
    efficiencyPct: Int,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
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
                    text = "KPI ko'rsatkichlari",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Batafsil hisobot →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDetailsClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleCircularRing(
                    percent = executionPct,
                    label = "Ijro darajasi",
                    color = TelegramCyan,
                    modifier = Modifier.weight(1f)
                )
                SingleCircularRing(
                    percent = planPct,
                    label = "Reja bajarilishi",
                    color = StatusGreen,
                    modifier = Modifier.weight(1f)
                )
                SingleCircularRing(
                    percent = efficiencyPct,
                    label = "Samaradorlik",
                    color = StatusPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
