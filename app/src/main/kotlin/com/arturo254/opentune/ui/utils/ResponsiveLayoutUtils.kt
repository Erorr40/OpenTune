/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ResponsiveLayoutUtils {
    fun calculateScaledItemHeight(baseHeight: Dp, fontScale: Float): Dp {
        val effectiveScale = fontScale.coerceAtLeast(1f)
        return baseHeight * effectiveScale
    }

    fun calculateAdaptiveColumns(
        availableWidth: Dp,
        minColumnWidth: Dp = 88.dp,
        minColumns: Int = 2,
        maxColumns: Int = 6,
        explicitColumns: Int? = null
    ): Int {
        if (explicitColumns != null) return explicitColumns
        return (availableWidth / minColumnWidth).toInt().coerceIn(minColumns, maxColumns)
    }

    fun isNarrowOrLargeFont(screenWidthDp: Int, fontScale: Float): Boolean {
        return screenWidthDp < 360 || fontScale > 1.25f
    }

    fun calculateControlButtonSize(
        availableWidth: Dp,
        minSize: Dp = 48.dp,
        maxSize: Dp = 80.dp
    ): Dp {
        val effectiveMin = minOf(minSize, maxSize)
        val effectiveMax = maxOf(minSize, maxSize)
        return (availableWidth / 6f).coerceIn(effectiveMin, effectiveMax)
    }
}
