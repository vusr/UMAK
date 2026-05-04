package com.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme { Dark, Light, AmoledBlack }

val LocalAppTheme = staticCompositionLocalOf { AppTheme.Dark }

private val DarkColorScheme = darkColorScheme(
    primary = Secondary,
    onPrimary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimary,
    secondary = Tertiary,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    background = Background,
    onBackground = OnBackground,
    error = Error,
    onError = OnError,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0277BD),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
)

private val AmoledColorScheme = darkColorScheme(
    primary = Secondary,
    onPrimary = AmoledBlack,
    primaryContainer = Color(0xFF1A1A1A),
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceVariant = Color(0xFF0D0D0D),
    onSurface = OnSurface,
    onBackground = OnBackground,
    surfaceContainer = AmoledBlack,
    surfaceContainerLow = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerHigh = Color(0xFF0D0D0D),
    surfaceContainerHighest = Color(0xFF111111),
)

@Composable
fun MusicPlayerTheme(
    appTheme: AppTheme = AppTheme.AmoledBlack,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> DarkColorScheme
        AppTheme.AmoledBlack -> AmoledColorScheme
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
