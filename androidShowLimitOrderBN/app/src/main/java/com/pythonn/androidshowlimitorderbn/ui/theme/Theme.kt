package com.pythonn.androidshowlimitorderbn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryGoldDark,
    onPrimaryContainer = TextOnPrimary,
    secondary = SecondaryGray,
    onSecondary = TextOnDark,
    secondaryContainer = SecondaryLightGray,
    onSecondaryContainer = TextOnDark,
    tertiary = InfoBlue,
    onTertiary = TextOnPrimary,
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = SecondaryDark,
    onSurface = TextOnDark,
    surfaceVariant = SecondaryGray,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = ErrorRed,
    onError = TextOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGold,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryGoldLight,
    onPrimaryContainer = TextPrimary,
    secondary = SecondaryGray,
    onSecondary = TextOnPrimary,
    secondaryContainer = BackgroundSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = InfoBlue,
    onTertiary = TextOnPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundSecondary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    error = ErrorRed,
    onError = TextOnPrimary
)

@Composable
fun AndroidShowLimitOrderBNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to use our custom Binance-inspired theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}