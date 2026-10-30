package com.example.ui.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode
import java.util.Calendar

private val BaseDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val BaseLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

val LocalThemeController = compositionLocalOf {
    ThemeController()
}

class ThemeController(
    val currentMode: AppThemeMode = AppThemeMode.SYSTEM,
    val onModeChanged: (AppThemeMode) -> Unit = {}
)

@Composable
fun animatedColorScheme(isDark: Boolean): ColorScheme {
    val targetScheme = if (isDark) BaseDarkColorScheme else BaseLightColorScheme
    val animDuration = 650 // Smooth cinematic transition duration
    val animEasing = FastOutSlowInEasing

    val primary by animateColorAsState(targetScheme.primary, tween(animDuration, easing = animEasing), label = "primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, tween(animDuration, easing = animEasing), label = "onPrimary")
    val primaryContainer by animateColorAsState(targetScheme.primaryContainer, tween(animDuration, easing = animEasing), label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(targetScheme.onPrimaryContainer, tween(animDuration, easing = animEasing), label = "onPrimaryContainer")
    val secondary by animateColorAsState(targetScheme.secondary, tween(animDuration, easing = animEasing), label = "secondary")
    val onSecondary by animateColorAsState(targetScheme.onSecondary, tween(animDuration, easing = animEasing), label = "onSecondary")
    val secondaryContainer by animateColorAsState(targetScheme.secondaryContainer, tween(animDuration, easing = animEasing), label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(targetScheme.onSecondaryContainer, tween(animDuration, easing = animEasing), label = "onSecondaryContainer")
    val tertiary by animateColorAsState(targetScheme.tertiary, tween(animDuration, easing = animEasing), label = "tertiary")
    val onTertiary by animateColorAsState(targetScheme.onTertiary, tween(animDuration, easing = animEasing), label = "onTertiary")
    val tertiaryContainer by animateColorAsState(targetScheme.tertiaryContainer, tween(animDuration, easing = animEasing), label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(targetScheme.onTertiaryContainer, tween(animDuration, easing = animEasing), label = "onTertiaryContainer")
    val background by animateColorAsState(targetScheme.background, tween(animDuration, easing = animEasing), label = "background")
    val onBackground by animateColorAsState(targetScheme.onBackground, tween(animDuration, easing = animEasing), label = "onBackground")
    val surface by animateColorAsState(targetScheme.surface, tween(animDuration, easing = animEasing), label = "surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, tween(animDuration, easing = animEasing), label = "onSurface")
    val surfaceVariant by animateColorAsState(targetScheme.surfaceVariant, tween(animDuration, easing = animEasing), label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariant, tween(animDuration, easing = animEasing), label = "onSurfaceVariant")
    val outline by animateColorAsState(targetScheme.outline, tween(animDuration, easing = animEasing), label = "outline")

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
}

fun resolveDarkTheme(mode: AppThemeMode, systemIsDark: Boolean): Boolean {
    return when (mode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> {
            // Auto based on device time (dark between 7 PM and 6 AM) or system dark mode
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            systemIsDark || (hour >= 19 || hour < 6)
        }
    }
}

@Composable
fun MaxToolsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = resolveDarkTheme(themeMode, systemDark)
    val animatedScheme = animatedColorScheme(isDark)

    val controller = ThemeController(currentMode = themeMode, onModeChanged = onThemeModeChange)

    CompositionLocalProvider(LocalThemeController provides controller) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}
