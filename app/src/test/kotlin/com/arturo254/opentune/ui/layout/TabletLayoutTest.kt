/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.layout

import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.component.GridPosition
import com.arturo254.opentune.ui.utils.ResponsiveLayoutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TabletLayoutTest {

    private fun resolveSourceFile(relativePath: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("../app", relativePath),
            File(userDir, relativePath),
            File(userDir, "app/$relativePath"),
            File("D:/MyWork/OpenTune", relativePath),
            File("D:/MyWork/OpenTune/app", relativePath),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Source file not found: $relativePath (searched candidates: $candidates)")
    }

    // =========================================================================
    // 1. Player Control Sizing & Clamping Tests
    // =========================================================================

    @Test
    fun controlButtonSize_phoneWidthsScaleProportionallyWithinBounds() {
        // Phone width 360dp: 360 / 6 = 60dp
        val size360 = ResponsiveLayoutUtils.calculateControlButtonSize(360.dp)
        assertEquals(60.dp, size360)

        // Phone width 390dp: 390 / 6 = 65dp
        val size390 = ResponsiveLayoutUtils.calculateControlButtonSize(390.dp)
        assertEquals(65.dp, size390)

        // Phone width 412dp: 412 / 6 = 68.666664dp
        val size412 = ResponsiveLayoutUtils.calculateControlButtonSize(412.dp)
        assertEquals(412.dp / 6f, size412)
        assertTrue(size412 >= 48.dp && size412 <= 80.dp)
    }

    @Test
    fun controlButtonSize_tabletWidthsAreStrictlyClampedToMaxSize() {
        // Small tablet / Foldable: 600dp -> 600 / 6 = 100dp -> clamped to 80dp
        val size600 = ResponsiveLayoutUtils.calculateControlButtonSize(600.dp)
        assertEquals(80.dp, size600)

        // Tablet 800dp: 800 / 6 = 133.33dp -> clamped to 80dp
        val size800 = ResponsiveLayoutUtils.calculateControlButtonSize(800.dp)
        assertEquals(80.dp, size800)

        // Tablet 1200dp: 1200 / 6 = 200dp -> clamped to 80dp
        val size1200 = ResponsiveLayoutUtils.calculateControlButtonSize(1200.dp)
        assertEquals(80.dp, size1200)

        // Tablet 1600dp: 1600 / 6 = 266.67dp -> clamped to 80dp
        val size1600 = ResponsiveLayoutUtils.calculateControlButtonSize(1600.dp)
        assertEquals(80.dp, size1600)
    }

    @Test
    fun controlButtonSize_narrowAndNegativeWidthsAreStrictlyClampedToMinSize() {
        // Extremely narrow width: 240dp -> 240 / 6 = 40dp -> clamped to 48dp
        val size240 = ResponsiveLayoutUtils.calculateControlButtonSize(240.dp)
        assertEquals(48.dp, size240)

        // Zero width -> clamped to 48dp
        val size0 = ResponsiveLayoutUtils.calculateControlButtonSize(0.dp)
        assertEquals(48.dp, size0)

        // Negative width -> clamped to 48dp
        val sizeNegative = ResponsiveLayoutUtils.calculateControlButtonSize((-100).dp)
        assertEquals(48.dp, sizeNegative)
    }

    @Test
    fun controlButtonSize_exactBoundaryValues() {
        // Exact lower bound transition point: 288dp -> 288 / 6 = 48dp
        val size288 = ResponsiveLayoutUtils.calculateControlButtonSize(288.dp)
        assertEquals(48.dp, size288)

        // Exact upper bound transition point: 480dp -> 480 / 6 = 80dp
        val size480 = ResponsiveLayoutUtils.calculateControlButtonSize(480.dp)
        assertEquals(80.dp, size480)

        // Just below lower bound: 282dp -> 47dp -> clamped to 48dp
        assertEquals(48.dp, ResponsiveLayoutUtils.calculateControlButtonSize(282.dp))

        // Just above upper bound: 486dp -> 81dp -> clamped to 80dp
        assertEquals(80.dp, ResponsiveLayoutUtils.calculateControlButtonSize(486.dp))
    }

    @Test
    fun controlButtonSize_customThresholdsAndGracefulInversion() {
        // Custom min/max sizes
        val custom = ResponsiveLayoutUtils.calculateControlButtonSize(
            availableWidth = 500.dp,
            minSize = 40.dp,
            maxSize = 120.dp
        )
        // 500 / 6 = 83.33dp (between 40 and 120)
        assertEquals(500.dp / 6f, custom)

        // Inverted bounds (min > max) handled gracefully via minOf/maxOf
        val inverted = ResponsiveLayoutUtils.calculateControlButtonSize(
            availableWidth = 1000.dp,
            minSize = 80.dp,
            maxSize = 48.dp
        )
        assertEquals(80.dp, inverted)
    }

    // =========================================================================
    // 2. Tablet Adaptive Playlist Grid Column Tests
    // =========================================================================

    @Test
    fun adaptivePlaylistColumns_phonesMaintainCompactTwoColumns() {
        val minColumnWidth = 160.dp
        val minColumns = 2
        val maxColumns = 6

        // Compact phone 320dp: 320 / 160 = 2
        val cols320 = ResponsiveLayoutUtils.calculateAdaptiveColumns(320.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(2, cols320)

        // Standard phone 360dp: 360 / 160 = 2.25 -> 2
        val cols360 = ResponsiveLayoutUtils.calculateAdaptiveColumns(360.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(2, cols360)

        // Modern phone 390dp: 390 / 160 = 2.4375 -> 2
        val cols390 = ResponsiveLayoutUtils.calculateAdaptiveColumns(390.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(2, cols390)

        // Large phone 412dp: 412 / 160 = 2.575 -> 2
        val cols412 = ResponsiveLayoutUtils.calculateAdaptiveColumns(412.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(2, cols412)
    }

    @Test
    fun adaptivePlaylistColumns_tabletsScaleToThreeFourOrMoreColumns() {
        val minColumnWidth = 160.dp
        val minColumns = 2
        val maxColumns = 6

        // Foldable / Small tablet 600dp: 600 / 160 = 3.75 -> 3 columns
        val cols600 = ResponsiveLayoutUtils.calculateAdaptiveColumns(600.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(3, cols600)

        // Medium tablet 720dp: 720 / 160 = 4.5 -> 4 columns
        val cols720 = ResponsiveLayoutUtils.calculateAdaptiveColumns(720.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(4, cols720)

        // Large tablet 800dp: 800 / 160 = 5.0 -> 5 columns
        val cols800 = ResponsiveLayoutUtils.calculateAdaptiveColumns(800.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(5, cols800)

        // Expanded tablet 1000dp: 1000 / 160 = 6.25 -> clamped to 6 columns
        val cols1000 = ResponsiveLayoutUtils.calculateAdaptiveColumns(1000.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(6, cols1000)

        // High-res tablet 1200dp: 1200 / 160 = 7.5 -> clamped to maxColumns 6
        val cols1200 = ResponsiveLayoutUtils.calculateAdaptiveColumns(1200.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(6, cols1200)

        // Ultra-wide tablet 1600dp: 1600 / 160 = 10 -> clamped to maxColumns 6
        val cols1600 = ResponsiveLayoutUtils.calculateAdaptiveColumns(1600.dp, minColumnWidth, minColumns, maxColumns)
        assertEquals(6, cols1600)
    }

    @Test
    fun adaptivePlaylistColumns_strictBoundsAdherence() {
        val minColumnWidth = 160.dp
        val minColumns = 2
        val maxColumns = 6

        // Below minimum width: 100dp / 160 = 0 -> clamped to minColumns = 2
        assertEquals(2, ResponsiveLayoutUtils.calculateAdaptiveColumns(100.dp, minColumnWidth, minColumns, maxColumns))

        // Negative width -> clamped to minColumns = 2
        assertEquals(2, ResponsiveLayoutUtils.calculateAdaptiveColumns((-50).dp, minColumnWidth, minColumns, maxColumns))

        // Huge width: 5000dp / 160 = 31 -> clamped to maxColumns = 6
        assertEquals(6, ResponsiveLayoutUtils.calculateAdaptiveColumns(5000.dp, minColumnWidth, minColumns, maxColumns))

        // Explicit override overrides clamping
        val explicitOverride = ResponsiveLayoutUtils.calculateAdaptiveColumns(
            availableWidth = 5000.dp,
            minColumnWidth = minColumnWidth,
            minColumns = minColumns,
            maxColumns = maxColumns,
            explicitColumns = 3
        )
        assertEquals(3, explicitOverride)
    }

    @Test
    fun gridPosition_supportsMiddleForMultiColumnLayouts() {
        // Verify GridPosition enum contains LEFT, RIGHT, MIDDLE, SINGLE
        val values = GridPosition.values().map { it.name }
        assertTrue("GridPosition must contain LEFT", values.contains("LEFT"))
        assertTrue("GridPosition must contain RIGHT", values.contains("RIGHT"))
        assertTrue("GridPosition must contain MIDDLE", values.contains("MIDDLE"))
        assertTrue("GridPosition must contain SINGLE", values.contains("SINGLE"))
    }

    // =========================================================================
    // 3. Source Code Integration & Architectural Guardrails
    // =========================================================================

    @Test
    fun playerComponents_usesClampedControlButtonSize() {
        val file = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/player/PlayerComponents.kt")
        assertTrue("PlayerComponents.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "PlayerComponents.kt must call ResponsiveLayoutUtils.calculateControlButtonSize",
            content.contains("ResponsiveLayoutUtils.calculateControlButtonSize(maxW)") ||
                    content.contains("ResponsiveLayoutUtils.calculateControlButtonSize(maxWidth)")
        )
        assertFalse(
            "PlayerComponents.kt must NOT use unconstrained maxW / 6f directly without clamping",
            content.contains("val playButtonHeight = maxW / 6f")
        )
        assertTrue(
            "PlayerComponents.kt must clamp Row width with widthIn(max = 540.dp)",
            content.contains("widthIn(max = 540.dp)")
        )
    }

    @Test
    fun libraryPlaylistsScreen_usesAdaptiveColumnsInBoxWithConstraints() {
        val file = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryPlaylistsScreen.kt")
        assertTrue("LibraryPlaylistsScreen.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "LibraryPlaylistsScreen.kt must wrap shortcuts in BoxWithConstraints",
            content.contains("BoxWithConstraints(modifier = modifier)")
        )
        assertTrue(
            "LibraryPlaylistsScreen.kt must calculate adaptive columns using calculateAdaptiveColumns",
            content.contains("ResponsiveLayoutUtils.calculateAdaptiveColumns")
        )
        assertTrue(
            "LibraryPlaylistsScreen.kt must chunk using dynamic columns",
            content.contains("remainingEntries.chunked(columns)")
        )
        assertFalse(
            "LibraryPlaylistsScreen.kt must NOT hardcode remainingEntries.chunked(2)",
            content.contains("remainingEntries.chunked(2)")
        )
        assertTrue(
            "LibraryPlaylistsScreen.kt must handle GridPosition.MIDDLE",
            content.contains("GridPosition.MIDDLE")
        )
    }
}
