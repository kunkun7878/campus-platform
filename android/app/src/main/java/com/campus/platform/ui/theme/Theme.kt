package com.campus.platform.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 蓝白校园风 — 映射自 res/values/colors.xml
val Primary = Color(0xFF2D6BFF)            // brand
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDBE4FF)
val OnPrimaryContainer = Color(0xFF001A52)

val Secondary = Color(0xFF12B7AE)          // accent
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFC5FCF8)
val OnSecondaryContainer = Color(0xFF003330)

val Background = Color(0xFFF5F8FF)         // --bg
val OnBackground = Color(0xFF1B2850)

val Surface = Color(0xFFFFFFFF)            // --panel
val OnSurface = Color(0xFF1B2850)
val SurfaceVariant = Color(0xFFF0F4FF)
val OnSurfaceVariant = Color(0xFF7C89A6)   // --muted

val Error = Color(0xFFF45F89)              // --rose
val OnError = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = OnError,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C6FF),
    onPrimary = Color(0xFF002E7A),
    primaryContainer = Color(0xFF0045AD),
    onPrimaryContainer = Color(0xFFDBE4FF),
    secondary = Color(0xFF80DDD7),
    onSecondary = Color(0xFF003330),
    secondaryContainer = Color(0xFF008A81),
    onSecondaryContainer = Color(0xFFC5FCF8),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E8),
    surface = Color(0xFF1C1E25),
    onSurface = Color(0xFFE3E2E8),
    surfaceVariant = Color(0xFF2A2D35),
    onSurfaceVariant = Color(0xFFC4C6D0),
    error = Color(0xFFFFB3C0),
    onError = Color(0xFF68002E),
)

@Composable
fun CampusPlatformTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
