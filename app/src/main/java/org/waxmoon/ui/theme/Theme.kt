package org.waxmoon.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShadowLightColors = lightColors(
    primary = Color(0xFF2F6FED),
    primaryVariant = Color(0xFF2F6FED),
    secondary = Color(0xFF2F6FED),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFAFA),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)

@Composable
fun ShadowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = ShadowLightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

/** @deprecated Use [ShadowTheme] */
@Composable
fun GithubMultiAppTheme(content: @Composable () -> Unit) {
    ShadowTheme(content = content)
}
