package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.AccentChoice
import com.example.model.ThemeMode

private fun buildDarkColorScheme(accent: AccentChoice) = darkColorScheme(
    primary = accent.primaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF152544),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = HokimEmeraldLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = HokimGoldLight,
    onTertiary = Color.Black,
    background = Color(0xFF080F1E),
    surface = Color(0xFF111B2E),
    surfaceVariant = Color(0xFF1A2844),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF283A5E),
    error = StatusRed
)

private fun buildLightColorScheme(accent: AccentChoice) = lightColorScheme(
    primary = accent.primaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = HokimNavy,
    secondary = HokimBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = HokimNavy,
    tertiary = HokimGold,
    onTertiary = Color.White,
    tertiaryContainer = HokimGoldBg,
    onTertiaryContainer = HokimGold,
    background = TelegramBg,
    surface = Color.White,
    surfaceVariant = Slate100,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    error = StatusRed
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentChoice: AccentChoice = AccentChoice.TELEGRAM_BLUE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> buildDarkColorScheme(accentChoice)
        else -> buildLightColorScheme(accentChoice)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
