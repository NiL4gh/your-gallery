package com.yourgallery.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

@Composable
fun GalleryTheme(
    darkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF222222),
            onPrimaryContainer = Color.White,
            background = Color.Black,
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCCCCCC)
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color.Black,
            background = Color.White,
            surface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFFEEEEEE),
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF444444)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
        ),
        content = content
    )
}
