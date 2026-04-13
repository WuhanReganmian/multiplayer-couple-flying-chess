package com.couplechess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = PureBlack,
    primaryContainer = MutedPurple,
    onPrimaryContainer = SoftWhite,
    secondary = WarmRed,
    onSecondary = SoftWhite,
    secondaryContainer = DarkRed,
    onSecondaryContainer = SoftWhite,
    tertiary = GoldLight,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = SoftWhite,
    surface = BackgroundCard,
    onSurface = SoftWhite,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = Color(0xFFCF6679),
    onError = PureBlack,
)

@Composable
fun CoupleChessTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CoupleChessTypography,
        content = content
    )
}
