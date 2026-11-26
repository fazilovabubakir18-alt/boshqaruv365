package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppUser
import com.example.model.PresetUsers
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    availableUsers: List<AppUser>,
    onAttemptCredentialsLogin: (String, String) -> Unit,
    onQuickUserLogin: (AppUser) -> Unit,
    errorMessage: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Visual Hero Banner with Zarafshon Civic Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zarafshon_banner_1790585492174),
                    contentDescription = "Zarafshon Shahar Hokimligi",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xC4080F1E),
                                    Color(0xF5080F1E)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, HokimGoldLight, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo_1790585476502),
                            contentDescription = "Boshqaruv 365 Logotipi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BOSHQARUV 365",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = TelegramBlue, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "RBAC + AI",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Zarafshon shahar hokimligi raqamli boshqaruv platformasi",
                        fontSize = 11.sp,
                        color = Slate200,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .padding(horizontal = 16.dp)
                    .offset(y = (-18).dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Strict No-Public-Signup Login Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TIZIMGA KIRISH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TelegramBlue,
                                letterSpacing = 0.5.sp
                            )
                            Surface(color = HokimEmerald.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "RBAC • Faqat ruxsatli",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HokimEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = HokimGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ochiq ro'yxatdan o'tish yopiq. Yangi xodim login/parolini faqat Markaziy administrator yaratadi; 5 marta xato urinishda akkaunt bloklanadi.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = StatusRed.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(errorMessage, color = StatusRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = loginInput,
                            onValueChange = {
                                loginInput = it
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Login yoki telefon") },
                            placeholder = { Text("hokim, orinbosar, ijrochi, tahlilchi...") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TelegramBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_input")
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Maxfiy parol") },
                            placeholder = { Text("Rahbar bergan parolni kiriting") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TelegramBlue) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Parolni ko'rsatish"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        Button(
                            onClick = { onAttemptCredentialsLogin(loginInput, passwordInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_button")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tizimga Kirish", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }

                Text(
                    text = "DEMO ROLLAR BO'YICHA TEZKOR KIRISH:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )

                availableUsers.forEach { user ->
                    RoleLoginCard(
                        user = user,
                        onClick = {
                            loginInput = user.login
                            passwordInput = PresetUsers.demoPasswordHintForLoginScreen(user.login)
                            onQuickUserLogin(user)
                        }
                    )
                }

                Spacer(modifier = Modifier.navigationBarsPadding().height(20.dp))
            }
        }
    }
}

@Composable
private fun RoleLoginCard(
    user: AppUser,
    onClick: () -> Unit
) {
    val isTempLocked = user.lockedUntilMillis > System.currentTimeMillis()
    val (icon, accentCol) = when (user.role) {
        UserRole.HOKIM -> Pair(Icons.Default.AccountBalance, HokimNavyLight)
        UserRole.SUPER_ADMIN -> Pair(Icons.Default.AdminPanelSettings, Color(0xFF0F766E))
        UserRole.ORINBOSAR -> Pair(Icons.Default.Domain, StatusGreen)
        UserRole.BOLIM_RAHBARI -> Pair(Icons.Default.Groups, StatusPurple)
        UserRole.IJROCHI -> Pair(Icons.Default.Engineering, HokimGold)
        UserRole.NAZORATCHI -> Pair(Icons.Default.Security, StatusRed)
        UserRole.TAHLILCHI -> Pair(Icons.Default.Analytics, Color(0xFFA78BFA))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (user.isBlocked) StatusRed else accentCol.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("quick_login_${user.role.name}")
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentCol.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = if (user.isBlocked) StatusRed else accentCol, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.role.titleUz,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (user.isBlocked) StatusRed else accentCol
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            user.isBlocked -> "BLOKLANGAN"
                            isTempLocked -> "VAQTINCHA BLOKDA"
                            else -> "@${user.login}"
                        },
                        fontSize = 10.sp,
                        color = if (user.isBlocked || isTempLocked) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${user.position} • ${user.department}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accentCol)
        }
    }
}
