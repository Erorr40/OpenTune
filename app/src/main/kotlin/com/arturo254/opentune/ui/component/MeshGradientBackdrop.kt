/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Draws the Expressive M3 multi-blob radial mesh backdrop cached across frames.
 * Keeping shader instantiation inside [drawWithCache] and applying scroll fade via
 * [graphicsLayer] avoids per-frame Brush allocations during scrolling.
 */
fun Modifier.meshGradientBackdrop(
    gradientColors: List<Color>,
    surfaceColor: Color,
    alpha: Float = 1f,
    verticalScale: Float = 1f,
    fadeStartFraction: Float = 0.4f,
): Modifier {
    if (gradientColors.isEmpty() || alpha <= 0f) return this

    return this
        .graphicsLayer {
            this.alpha = alpha.coerceIn(0f, 1f)
        }
        .drawWithCache {
            val width = size.width
            val fullHeight = size.height
            val height = fullHeight * verticalScale

            val brushes = buildList {
                if (gradientColors.size >= 3) {
                    val c0 = gradientColors[0]
                    val c1 = gradientColors[1]
                    val c2 = gradientColors[2]
                    val c3 = gradientColors.getOrElse(3) { c0 }
                    val c4 = gradientColors.getOrElse(4) { c1 }

                    add(
                        Brush.radialGradient(
                            colors = listOf(c0.copy(alpha = 0.75f), c0.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.15f),
                            radius = (width * 0.8f).coerceAtLeast(1f),
                        ),
                    )
                    add(
                        Brush.radialGradient(
                            colors = listOf(c1.copy(alpha = 0.55f), c1.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(width * 0.1f, height * 0.4f),
                            radius = (width * 0.6f).coerceAtLeast(1f),
                        ),
                    )
                    add(
                        Brush.radialGradient(
                            colors = listOf(c2.copy(alpha = 0.5f), c2.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(width * 0.9f, height * 0.35f),
                            radius = (width * 0.55f).coerceAtLeast(1f),
                        ),
                    )
                    add(
                        Brush.radialGradient(
                            colors = listOf(c3.copy(alpha = 0.35f), c3.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(width * 0.25f, height * 0.65f),
                            radius = (width * 0.75f).coerceAtLeast(1f),
                        ),
                    )
                    add(
                        Brush.radialGradient(
                            colors = listOf(c4.copy(alpha = 0.3f), c4.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(width * 0.55f, height * 0.85f),
                            radius = (width * 0.9f).coerceAtLeast(1f),
                        ),
                    )
                } else {
                    val c0 = gradientColors[0]
                    add(
                        Brush.radialGradient(
                            colors = listOf(c0.copy(alpha = 0.7f), c0.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.25f),
                            radius = (width * 0.85f).coerceAtLeast(1f),
                        ),
                    )
                }

                add(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            surfaceColor.copy(alpha = 0.22f),
                            surfaceColor.copy(alpha = 0.55f),
                            surfaceColor,
                        ),
                        startY = fullHeight * fadeStartFraction,
                        endY = fullHeight,
                    ),
                )
            }

            onDrawBehind {
                brushes.forEach { brush ->
                    drawRect(brush = brush)
                }
            }
        }
}

/**
 * Cached M3 Expressive 5-blob theme backdrop matching HomeScreen/LibraryScreen aesthetics.
 */
fun Modifier.meshGradientThemeBackdrop(
    color1: Color,
    color2: Color,
    color3: Color,
    color4: Color,
    color5: Color,
    surfaceColor: Color,
): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val rMin = 1f

    val brushes = listOf(
        Brush.radialGradient(
            colors = listOf(
                color1.copy(alpha = 0.38f),
                color1.copy(alpha = 0.24f),
                color1.copy(alpha = 0.14f),
                color1.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(width * 0.15f, height * 0.1f),
            radius = (width * 0.55f).coerceAtLeast(rMin),
        ),
        Brush.radialGradient(
            colors = listOf(
                color2.copy(alpha = 0.34f),
                color2.copy(alpha = 0.2f),
                color2.copy(alpha = 0.11f),
                color2.copy(alpha = 0.05f),
                Color.Transparent,
            ),
            center = Offset(width * 0.85f, height * 0.2f),
            radius = (width * 0.65f).coerceAtLeast(rMin),
        ),
        Brush.radialGradient(
            colors = listOf(
                color3.copy(alpha = 0.3f),
                color3.copy(alpha = 0.17f),
                color3.copy(alpha = 0.09f),
                color3.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            center = Offset(width * 0.3f, height * 0.45f),
            radius = (width * 0.6f).coerceAtLeast(rMin),
        ),
        Brush.radialGradient(
            colors = listOf(
                color4.copy(alpha = 0.26f),
                color4.copy(alpha = 0.14f),
                color4.copy(alpha = 0.08f),
                color4.copy(alpha = 0.03f),
                Color.Transparent,
            ),
            center = Offset(width * 0.7f, height * 0.5f),
            radius = (width * 0.7f).coerceAtLeast(rMin),
        ),
        Brush.radialGradient(
            colors = listOf(
                color5.copy(alpha = 0.22f),
                color5.copy(alpha = 0.12f),
                color5.copy(alpha = 0.06f),
                color5.copy(alpha = 0.02f),
                Color.Transparent,
            ),
            center = Offset(width * 0.5f, height * 0.75f),
            radius = (width * 0.8f).coerceAtLeast(rMin),
        ),
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                surfaceColor.copy(alpha = 0.22f),
                surfaceColor.copy(alpha = 0.55f),
                surfaceColor,
            ),
            startY = height * 0.4f,
            endY = height,
        ),
    )

    onDrawBehind {
        brushes.forEach { brush -> drawRect(brush = brush) }
    }
}
