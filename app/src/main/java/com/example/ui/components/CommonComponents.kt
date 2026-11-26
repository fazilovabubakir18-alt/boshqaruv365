package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CityStoryHighlight
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun CivicTopAppBar(
    currentRole: UserRole,
    userName: String,
    cityHealthScore: Int,
    unreadNotificationsCount: Int,
    isOffline: Boolean,
    isDarkTheme: Boolean,
    onRoleClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Premium Apple / Fintech identity header matching Mockup 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onRoleClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Box(modifier = Modifier.size(42.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(TelegramHeaderGradient)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo_1790585476502),
                                contentDescription = "Boshqaruv 365 logotipi",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        // Live online indicator dot
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (isOffline) StatusYellow else StatusGreen)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Boshqaruv 365",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Tasdiqlangan",
                                tint = TelegramBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (isOffline) "Offline" else "Onlayn"} • ${currentRole.titleUz}",
                                color = if (isOffline) StatusYellow else StatusGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Compact Action Icons + Glass Role Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("top_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = StatusRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadNotificationsCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Bildirishnomalar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("top_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Mavzuni almashtirish",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onRoleClick() }
                            .testTag("top_role_pill_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentRole.shortLabel(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Rolni almashtirish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), thickness = 0.7.dp)
        }
    }
}

fun UserRole.shortLabel(): String = when (this) {
    UserRole.HOKIM -> "Rahbar (Hokim)"
    UserRole.SUPER_ADMIN -> "Markaziy Admin"
    UserRole.ORINBOSAR -> "O'rinbosar"
    UserRole.BOLIM_RAHBARI -> "Bo'lim Mas'uli"
    UserRole.IJROCHI -> "Ijrochi"
    UserRole.NAZORATCHI -> "Nazoratchi"
    UserRole.TAHLILCHI -> "Tahlilchi"
}

/**
 * Reusable 3-Way Task Chain pill row: Beruvchi -> Nazoratchi -> Bajaruvchi
 */
@Composable
fun TaskChainThreeWayRow(
    creatorName: String,
    reviewerName: String,
    assigneeName: String,
    modifier: Modifier = Modifier
) {
    val creatorShort = creatorName.substringBefore(" ").ifBlank { "Rahbar" }
    val reviewerShort = reviewerName.substringBefore(" ").ifBlank { "Nazorat" }
    val assigneeShort = assigneeName.substringBefore(" ").ifBlank { "Ijrochi" }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChainNodeChip(
            label = "Beruvchi: $creatorShort",
            accent = Slate400,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp)
        )
        ChainNodeChip(
            label = "Nazoratchi: $reviewerShort",
            accent = HokimGold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp)
        )
        ChainNodeChip(
            label = "Bajaruvchi: $assigneeShort",
            accent = TelegramCyan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChainNodeChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

/**
 * Sleek Fintech-style gradient progress bar + percentage label.
 */
@Composable
fun GradientProgressBar(
    progressPercent: Int,
    accentColors: List<Color> = listOf(TelegramBlue, HokimEmerald),
    modifier: Modifier = Modifier
) {
    val clamped = progressPercent.coerceIn(0, 100)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(accentColors))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$clamped%",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StoriesHighlightRow(
    stories: List<CityStoryHighlight>,
    onStoryClick: (CityStoryHighlight) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStoryClick(story) }
                    .testTag("story_${story.id}")
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (story.isUnread) {
                                    Brush.sweepGradient(story.ringColors + story.ringColors.first())
                                } else {
                                    Brush.linearGradient(listOf(Slate400, Slate500))
                                }
                            )
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = story.imageResId),
                                contentDescription = story.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Surface(
                        color = story.ringColors.first(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = story.badgeText,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = story.title,
                    fontSize = 10.sp,
                    fontWeight = if (story.isUnread) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val statusEnum = try {
        TaskStatus.valueOf(status)
    } catch (_: Exception) {
        TaskStatus.IN_PROGRESS
    }

    val (fg, icon) = when (statusEnum) {
        TaskStatus.COMPLETED -> Pair(HokimEmerald, Icons.Default.CheckCircle)
        TaskStatus.IN_PROGRESS -> Pair(HokimGold, Icons.Default.Autorenew)
        TaskStatus.REVIEW -> Pair(StatusPurple, Icons.Default.RateReview)
        TaskStatus.APPROVAL_PENDING -> Pair(Color(0xFF7C3AED), Icons.Default.HourglassTop)
        TaskStatus.REWORK -> Pair(StatusRed, Icons.Default.SyncProblem)
        TaskStatus.REJECTED -> Pair(StatusRed, Icons.Default.Cancel)
        TaskStatus.ACCEPTED -> Pair(TelegramCyan, Icons.Default.ThumbUp)
        TaskStatus.ASSIGNED -> Pair(TelegramBlue, Icons.Default.AssignmentInd)
        TaskStatus.NOTIFIED -> Pair(Color(0xFF818CF8), Icons.Default.NotificationsActive)
        TaskStatus.SUBMITTED -> Pair(Color(0xFFA78BFA), Icons.Default.Send)
        TaskStatus.ON_HOLD -> Pair(Slate400, Icons.Default.PauseCircle)
        TaskStatus.EXTENSION_REQUESTED -> Pair(Color(0xFFFB923C), Icons.Default.MoreTime)
        TaskStatus.ESCALATED -> Pair(StatusRed, Icons.Default.PriorityHigh)
        TaskStatus.CANCELLED -> Pair(Slate400, Icons.Default.Block)
        TaskStatus.DRAFT -> Pair(Slate400, Icons.Default.Drafts)
        TaskStatus.ARCHIVED -> Pair(Slate400, Icons.Default.Archive)
    }

    Surface(
        color = fg.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.38f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusEnum.titleUz,
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val pEnum = try {
        TaskPriority.valueOf(priority)
    } catch (_: Exception) {
        TaskPriority.MEDIUM
    }

    val fg = when (pEnum) {
        TaskPriority.EMERGENCY -> StatusRed
        TaskPriority.HIGH -> Color(0xFFFB923C)
        TaskPriority.MEDIUM -> TelegramCyan
        TaskPriority.LOW -> Slate400
    }

    Surface(
        color = fg.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = pEnum.titleUz,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WorkflowStepper(currentStatus: String, modifier: Modifier = Modifier) {
    val steps = listOf(
        Pair("ASSIGNED", "Yaratildi"),
        Pair("ACCEPTED", "Qabul"),
        Pair("IN_PROGRESS", "Ijro"),
        Pair("SUBMITTED", "Tekshiruv"),
        Pair("APPROVAL_PENDING", "Tasdiq ✓"),
        Pair("COMPLETED", "Yakunlandi")
    )

    val currentIdx = when (currentStatus) {
        "DRAFT" -> -1
        "ASSIGNED", "NOTIFIED" -> 0
        "ACCEPTED" -> 1
        "IN_PROGRESS", "ON_HOLD", "EXTENSION_REQUESTED", "ESCALATED" -> 2
        "SUBMITTED" -> 3
        "REVIEW", "APPROVAL_PENDING", "REWORK" -> 4
        "COMPLETED", "ARCHIVED" -> 5
        else -> 2
    }

    val isDoneAll = currentStatus == "COMPLETED" || currentStatus == "ARCHIVED"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "IJRO VA 2-BOSQICHLI TASDIQ ZANJIRI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { idx, (_, label) ->
                    val isDone = idx < currentIdx || isDoneAll
                    val isCurrent = idx == currentIdx && !isDoneAll

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> StatusGreen
                                        isCurrent -> if (currentStatus == "REWORK" || currentStatus == "ESCALATED") StatusRed else TelegramBlue
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            fontSize = 8.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isDone -> StatusGreen
                                isCurrent -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveCivicKpiCard(
    title: String,
    value: String,
    deltaText: String,
    isPositiveTrend: Boolean,
    icon: ImageVector,
    accentColor: Color,
    sparklinePoints: List<Float>? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trendColor = if (isPositiveTrend) StatusGreen else StatusRed
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = value,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = deltaText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!sparklinePoints.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                MiniTrendSparkline(
                    points = sparklinePoints,
                    color = accentColor
                )
            }
        }
    }
}

data class SidebarNavEntry(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int? = null
)

@Composable
fun ExecutiveSidebar(
    activeRouteId: String,
    tasksCount: Int,
    problemsCount: Int,
    onSelectRoute: (String) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        SidebarNavEntry("HOME", "Bosh sahifa", Icons.Filled.Home),
        SidebarNavEntry("ORG_STRUCTURE", "Tashkiliy tuzilma", Icons.Outlined.AccountTree),
        SidebarNavEntry("EMPLOYEES", "Xodimlar", Icons.Outlined.Groups),
        SidebarNavEntry("TASKS", "Topshiriqlar", Icons.Outlined.Assignment, badgeCount = tasksCount),
        SidebarNavEntry("PROJECTS", "Loyihalar va dasturlar", Icons.Outlined.BusinessCenter),
        SidebarNavEntry("PROBLEMS", "Muammolar reyestri", Icons.Outlined.ReportProblem, badgeCount = problemsCount),
        SidebarNavEntry("GIS", "GIS xarita", Icons.Outlined.Map),
        SidebarNavEntry("PERMISSIONS", "Vakolatlar matritsasi", Icons.Outlined.AdminPanelSettings),
        SidebarNavEntry("AI", "Sun'iy intellekt (AI)", Icons.Outlined.AutoAwesome),
        SidebarNavEntry("ANALYTICS", "Hisobotlar va tahlil", Icons.Outlined.BarChart),
        SidebarNavEntry("PROFILE", "Profil va sozlamalar", Icons.Outlined.Settings)
    )

    Column(
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(ExecutiveSidebarGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        // Zarafshon Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_1790585476502),
                contentDescription = "Zarafshon Gerbi",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, HokimGoldLight, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BOSHQARUV 365",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Zarafshon shahri",
                    color = HokimGoldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Items
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { entry ->
                val isSelected = activeRouteId == entry.id
                Surface(
                    color = if (isSelected) Color(0xFF1D4ED8) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectRoute(entry.id) }
                        .testTag("sidebar_${entry.id.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = entry.title,
                                tint = if (isSelected) Color.White else Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = entry.title,
                                color = if (isSelected) Color.White else Slate200,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.badgeCount != null && entry.badgeCount > 0) {
                            Surface(
                                color = if (isSelected) Color.White.copy(alpha = 0.22f) else TelegramBlue,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${entry.badgeCount}",
                                    color = Color.White,
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

        // Bottom Zarafshon City Card (Mockups 2 & 3)
        Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.zarafshon_banner_1790585492174),
                    contentDescription = "Zarafshon shahri",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Zarafshon shahri",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Yangi imkoniyatlar shahri!",
                    color = Slate300,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onLogoutClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tizimdan chiqish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
