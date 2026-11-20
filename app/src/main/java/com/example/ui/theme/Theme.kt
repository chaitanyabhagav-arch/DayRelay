package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DaylightColorScheme = lightColorScheme(
    primary = VermilionAccent,
    onPrimary = PureIvory,
    primaryContainer = VermilionContainer,
    onPrimaryContainer = OnVermilionContainer,
    secondary = CobaltEvent,
    onSecondary = PureIvory,
    secondaryContainer = CobaltContainer,
    onSecondaryContainer = OnCobaltContainer,
    tertiary = ForestHabit,
    onTertiary = PureIvory,
    tertiaryContainer = ForestContainer,
    onTertiaryContainer = OnForestContainer,
    background = AlabasterCanvas,
    onBackground = DeepInk,
    surface = PureIvory,
    onSurface = DeepInk,
    surfaceVariant = SoftLinen,
    onSurfaceVariant = SlateSecondary,
    surfaceContainerLowest = PureIvory,
    surfaceContainerLow = AlabasterCanvas,
    surfaceContainer = SoftLinen,
    surfaceContainerHigh = WarmSand,
    outline = HairlineBorder,
    outlineVariant = WarmSand,
    error = UrgentCrimson,
    onError = PureIvory,
    errorContainer = UrgentContainer,
    onErrorContainer = OnUrgentContainer
)

private val NightFocusColorScheme = darkColorScheme(
    primary = VermilionAccent,
    onPrimary = PureIvory,
    primaryContainer = ColorTokenBridge.DarkPrimaryContainer,
    onPrimaryContainer = VermilionContainer,
    secondary = CobaltEvent,
    onSecondary = PureIvory,
    secondaryContainer = ColorTokenBridge.DarkSecondaryContainer,
    onSecondaryContainer = CobaltContainer,
    tertiary = ForestHabit,
    onTertiary = PureIvory,
    tertiaryContainer = ColorTokenBridge.DarkTertiaryContainer,
    onTertiaryContainer = ForestContainer,
    background = DarkSlateCanvas,
    onBackground = DarkInk,
    surface = DarkElevatedSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryInk,
    surfaceContainerLowest = DarkSlateCanvas,
    surfaceContainerLow = DarkElevatedSurface,
    surfaceContainer = DarkSurfaceVariant,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = UrgentCrimson,
    onError = PureIvory
)

private object ColorTokenBridge {
    val DarkPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF5A1F0F)
    val DarkSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF172E61)
    val DarkTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF0E3F20)
}

val DayRelayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DayRelayTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Crisp Daylight Planner is the primary intentional theme requested by the user.
    val colorScheme = if (darkTheme) NightFocusColorScheme else DaylightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DayRelayTypography,
        shapes = DayRelayShapes,
        content = content
    )
}
