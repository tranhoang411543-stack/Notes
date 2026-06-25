package com.fini.todoapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FiniDarkText,
    onPrimary = FiniDarkBackground,
    primaryContainer = FiniDarkSurface,
    onPrimaryContainer = FiniDarkText,
    secondary = FiniDarkText,
    onSecondary = FiniDarkBackground,
    tertiary = FiniDarkText,
    onTertiary = FiniDarkBackground,
    background = FiniDarkBackground,
    onBackground = FiniDarkText,
    surface = FiniDarkSurface,
    onSurface = FiniDarkText,
    surfaceVariant = FiniDarkSurface,
    onSurfaceVariant = FiniDarkText,
    outline = FiniGray
)

private val LightColorScheme = lightColorScheme(
    primary = FiniBlack,
    onPrimary = FiniBackground,
    primaryContainer = FiniLightGray,
    onPrimaryContainer = FiniBlack,
    secondary = FiniInk,
    onSecondary = FiniBackground,
    secondaryContainer = FiniSurface,
    onSecondaryContainer = FiniBlack,
    tertiary = FiniBlack,
    onTertiary = FiniBackground,
    tertiaryContainer = FiniLightGray,
    onTertiaryContainer = FiniBlack,
    background = FiniBackground,
    onBackground = FiniBlack,
    surface = FiniBackground,
    onSurface = FiniBlack,
    surfaceVariant = FiniSurface,
    onSurfaceVariant = FiniGray,
    outline = FiniBorder,
    error = androidx.compose.ui.graphics.Color(0xFFD92D20),
    onError = FiniBackground
)

private val FiniShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiniTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    val rippleColor = if (darkTheme) Color.White else Color.Black
    val customRippleConfiguration = RippleConfiguration(
        color = rippleColor,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.35f,
            focusedAlpha = 0.25f,
            hoveredAlpha = 0.15f,
            draggedAlpha = 0.15f
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FiniShapes
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides customRippleConfiguration,
            content = content
        )
    }
}
