package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CityProblem
import com.example.model.CityStoryHighlight
import com.example.model.DocumentRecord
import com.example.model.HealthCategoryWeight
import com.example.model.ProblemStatus
import com.example.ui.theme.*

@Composable
fun StoryViewerDialog(
    story: CityStoryHighlight,
    onNavigateTarget: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    Image(
                        painter = painterResource(id = story.imageResId),
                        contentDescription = story.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )

                    // Top story bar indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = story.ringColors.first(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = story.metricValue,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "SHAHAR 360° • JONLI XULOSA",
                            color = HokimGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = story.headline,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 22.sp
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = story.summary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yopish", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onNavigateTarget(story.targetTab)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("story_open_target_button")
                        ) {
                            Text("Batafsil Ko'rish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemDetailTaskDialog(
    problem: CityProblem,
    canCreateTask: Boolean,
    onCreateTask: (String, Int) -> Unit,
    onOpenLinkedTask: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var taskTitle by remember(problem.id) { mutableStateOf(problem.aiSuggestedTaskTitle) }
    var selectedDays by remember { mutableStateOf(1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
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
                                    listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.72f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = problem.severity.color,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${problem.severity.labelUz} • ${problem.sector}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = problem.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Key metadata grid
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MetaLine("Hudud / Mahalla:", problem.mahalla)
                            MetaLine("Mas'ul tashkilot:", "${problem.orgName} (${problem.leaderName})")
                            MetaLine("Boshlanish vaqti:", problem.startDate)
                            MetaLine("Aholi murojaatlari:", "${problem.appealsCount} ta tasdiqlangan")
                            MetaLine("Ko'rsatkich ta'siri:", problem.indicatorDropNote)
                        }
                    }

                    Text(
                        text = problem.description,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (problem.status == ProblemStatus.TOPSHIRIQ_BERILDI && problem.linkedTaskId != null) {
                        Button(
                            onClick = {
                                onOpenLinkedTask(problem.linkedTaskId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Biriktirilgan Topshiriqni Ochish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else if (canCreateTask) {
                        HorizontalDivider()
                        Text(
                            text = "BIR BOSISHDA TOPSHIRIQ YARATISH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TelegramBlue,
                            letterSpacing = 0.5.sp
                        )

                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Topshiriq sarlavhasi (AI taklifi)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "24 soat", 3 to "3 kun", 7 to "7 kun").forEach { (days, label) ->
                                val selected = selectedDays == days
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedDays = days },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = { onCreateTask(taskTitle, selectedDays) },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("problem_one_click_task_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TOPSHIRIQ BERISH", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun FiveMinuteBriefingDialog(
    healthScore: Int,
    onOpenAiCenter: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HokimGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Hokimga 5 Daqiqalik Hisobot", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Shahar holati: $healthScore% • Avtomatik tahlil", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                HorizontalDivider()

                BriefingPointCard(
                    number = "1",
                    question = "Bugun eng kritik muammo nima?",
                    answer = "12-kichik nohiyada kechki suv bosimi tushishi (23 ta murojaat). Nasos stansiyasiga zudlik bilan ta'mir brigadasi zarur.",
                    accent = StatusRed
                )
                BriefingPointCard(
                    number = "2",
                    question = "Qaysi ko'rsatkich eng tez pasaymoqda?",
                    answer = "Murojaatlarni muddatida yopish tezligi (-8% hafta davomida) va Sanoat zonasi changlanish ko'rsatkichi.",
                    accent = HokimGold
                )
                BriefingPointCard(
                    number = "3",
                    question = "Qaysi tashkilot va mahalla zaiflashmoqda?",
                    answer = "Zarafshon Suv Ta'minoti (79%) hamda 12-kichik nohiya MFY (76% sog'liq indeksi).",
                    accent = Color(0xFFF97316)
                )
                BriefingPointCard(
                    number = "4",
                    question = "Qayerda ijobiy natija bor?",
                    answer = "Alisher Navoiy MFY (95%), Obodonlashtirish boshqarmasi (94%) va IT-Park qurilishi (94% tayyor).",
                    accent = StatusGreen
                )
                BriefingPointCard(
                    number = "5",
                    question = "Bugun kimga tezkor topshiriq berish kerak?",
                    answer = "To'rayev Sherzodga (Suv Ta'minoti) — 24 soatlik favqulodda nazorat topshirig'i; Raxmonov Otabekka — 14-bino fasad grafikini tiklash.",
                    accent = TelegramBlue
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yopish", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onOpenAiCenter()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("AI Savol-Javob", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingPointCard(
    number: String,
    question: String,
    answer: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(question, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(answer, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HealthWeightsEditorDialog(
    weights: List<HealthCategoryWeight>,
    computedScore: Int,
    onUpdateWeight: (String, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalWeight = weights.sumOf { it.weightPercent }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Shahar Sog'lig'i Indeksi Formulasi", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Joriy indeks: $computedScore% • Jami vazn: $totalWeight%", fontSize = 11.sp, color = TelegramBlue, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                HorizontalDivider()

                weights.forEach { cat ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${cat.name} (Natija: ${cat.scorePercent}%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Vazn: ${cat.weightPercent}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TelegramBlue)
                        }
                        Slider(
                            value = cat.weightPercent.toFloat(),
                            onValueChange = { onUpdateWeight(cat.id, it.toInt()) },
                            valueRange = 0f..40f
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Standart (100%)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Saqlash", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentPreviewDialog(
    doc: DocumentRecord,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = TelegramBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${doc.category} • ${doc.docNumber}",
                            color = TelegramBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                Text(doc.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Sana: ${doc.date} • Mas'ul: ${doc.signedBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text(doc.previewSummary, fontSize = 13.sp, lineHeight = 19.sp)

                Surface(
                    color = HokimEmeraldBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Holat: ${doc.status} • QR/ERI bilan himoyalangan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yopish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
