/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.ui.utils.ResponsiveLayoutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * Empirical Adversarial Challenger Test Suite for Milestone 2 Responsive Layout & Font Scaling Resilience.
 *
 * Authored by challenger_m2_rem_2 to empirically verify:
 * 1. Font scaling calculations across edge cases and dense sweeps (sub-unity, negative, standard, large, extreme).
 * 2. Adaptive grid columns calculations across narrow and wide screens, monotonicity, and layout symmetry.
 * 3. Bottom sheet menus scrolling contract across all 10 bottom sheet menu files (AST/regex verification).
 * 4. Screen width and font scale threshold boundaries.
 */
class EmpiricalChallengerM2StressTest {

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

    private val allTenBottomSheetMenuFiles = listOf(
        "SongMenu.kt",
        "YouTubeSongMenu.kt",
        "YouTubePlaylistMenu.kt",
        "YouTubeArtistMenu.kt",
        "YouTubeAlbumMenu.kt",
        "SelectionSongsMenu.kt",
        "PlaylistMenu.kt",
        "LyricsMenu.kt",
        "AlbumMenu.kt",
        "ArtistMenu.kt",
    )

    // =========================================================================
    // 1. ADVERSARIAL STRESS TEST: FONT SCALING CALCULATIONS ACROSS EDGE CASES
    // =========================================================================

    @Test
    fun fontScaling_subUnityDenseSweep_strictlyClampsToUnity() {
        val baseHeights = listOf(48.dp, 64.dp, 80.dp, 100.dp)
        for (baseHeight in baseHeights) {
            // Sweep sub-unity scales from -10.0f up to 1.0f
            val testScales = listOf(-10.0f, -1.0f, 0.0f, 0.001f, 0.1f, 0.33f, 0.5f, 0.75f, 0.85f, 0.999f, 1.0f)
            for (scale in testScales) {
                val scaled = ResponsiveLayoutUtils.calculateScaledItemHeight(baseHeight, scale)
                assertEquals(
                    "Font scale $scale with baseHeight $baseHeight must clamp strictly to baseHeight",
                    baseHeight.value,
                    scaled.value,
                    0.0001f
                )
            }
        }
    }

    @Test
    fun fontScaling_largeAndExtremeScales_scalesLinearlyWithoutDistortion() {
        val baseHeight = 64.dp
        // Test Android 14+ large font scales (1.15 to 2.0) and extreme accessibility scales up to 10.0
        val testScales = listOf(1.05f, 1.15f, 1.25f, 1.30f, 1.50f, 1.75f, 2.0f, 2.5f, 3.0f, 5.0f, 10.0f)
        for (scale in testScales) {
            val scaled = ResponsiveLayoutUtils.calculateScaledItemHeight(baseHeight, scale)
            val expected = baseHeight * scale
            assertEquals(
                "Font scale $scale must scale linearly to $expected",
                expected.value,
                scaled.value,
                0.0001f
            )
            assertTrue(
                "Scaled height at scale $scale must be strictly greater than baseHeight",
                scaled > baseHeight
            )
        }
    }

    @Test
    fun fontScaling_monotonicityInvariantAcrossEntireSpectrum() {
        val baseHeight = ListItemHeight // 64.dp
        var previousHeight = 0.dp
        var scale = -2.0f
        val step = 0.05f

        while (scale <= 6.0f) {
            val currentHeight = ResponsiveLayoutUtils.calculateScaledItemHeight(baseHeight, scale)
            if (previousHeight > 0.dp) {
                assertTrue(
                    "Monotonicity violated: at scale $scale, current ($currentHeight) < previous ($previousHeight)",
                    currentHeight >= previousHeight
                )
            }
            previousHeight = currentHeight
            scale += step
        }
    }

    @Test
    fun fontScaling_horizontalGridContainerHeightInvariant() {
        val testRows = listOf(1, 2, 3, 4, 6)
        val testScales = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 2.0f)

        for (rows in testRows) {
            for (scale in testScales) {
                val scaledHeight = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, scale)
                val totalContainerHeight = scaledHeight * rows
                val baselineContainerHeight = ListItemHeight * rows

                assertTrue(
                    "Total container height ($totalContainerHeight) must be >= baseline ($baselineContainerHeight)",
                    totalContainerHeight >= baselineContainerHeight
                )

                if (scale > 1.0f) {
                    assertTrue(
                        "Total container height ($totalContainerHeight) under large font ($scale) must exceed baseline",
                        totalContainerHeight > baselineContainerHeight
                    )
                }
            }
        }
    }

    // =========================================================================
    // 2. ADVERSARIAL STRESS TEST: ADAPTIVE GRID COLUMNS ACROSS NARROW & WIDE
    // =========================================================================

    @Test
    fun adaptiveGridColumns_narrowWidthBoundaries_neverDropBelowMinColumns() {
        // Any narrow width from negative up to 175.9dp must evaluate to minColumns (default 2)
        val narrowWidths = listOf(
            (-500).dp, (-1).dp, 0.dp, 0.1.dp, 10.dp, 50.dp, 87.9.dp, 88.dp, 120.dp, 175.dp, 175.9.dp
        )
        for (w in narrowWidths) {
            val cols = ResponsiveLayoutUtils.calculateAdaptiveColumns(w)
            assertEquals(
                "Width $w must evaluate to minColumns (2)",
                2,
                cols
            )
        }
    }

    @Test
    fun adaptiveGridColumns_standardPhoneWidths() {
        // Exact mathematical boundary intervals for minColumnWidth = 88.dp:
        // [0.dp, 263.9dp] -> 2 columns
        // [264.dp, 351.9dp] -> 3 columns
        // [352.dp, 439.9dp] -> 4 columns
        // [440.dp, 527.9dp] -> 5 columns
        // [528.dp, ...] -> 6 columns (clamped to maxColumns = 6)

        // Test boundary at 264.dp (88 * 3)
        assertEquals(2, ResponsiveLayoutUtils.calculateAdaptiveColumns(263.9.dp))
        assertEquals(3, ResponsiveLayoutUtils.calculateAdaptiveColumns(264.dp))

        // Compact phone ~320dp
        assertEquals(3, ResponsiveLayoutUtils.calculateAdaptiveColumns(320.dp))

        // Test boundary at 352.dp (88 * 4)
        assertEquals(3, ResponsiveLayoutUtils.calculateAdaptiveColumns(351.9.dp))
        assertEquals(4, ResponsiveLayoutUtils.calculateAdaptiveColumns(352.dp))

        // Standard phone 360dp, 390dp, 412dp
        assertEquals(4, ResponsiveLayoutUtils.calculateAdaptiveColumns(360.dp))
        assertEquals(4, ResponsiveLayoutUtils.calculateAdaptiveColumns(390.dp))
        assertEquals(4, ResponsiveLayoutUtils.calculateAdaptiveColumns(412.dp))

        // Large phablet 480dp
        assertEquals(5, ResponsiveLayoutUtils.calculateAdaptiveColumns(480.dp))
    }

    @Test
    fun adaptiveGridColumns_tabletsAndDesktops_neverExceedMaxColumns() {
        // Any wide screen >= 528dp (88 * 6) must evaluate to maxColumns (default 6)
        val wideWidths = listOf(
            528.dp, 600.dp, 720.dp, 800.dp, 840.dp, 1024.dp, 1280.dp, 1920.dp, 3840.dp, Dp.Infinity
        )
        for (w in wideWidths) {
            val cols = ResponsiveLayoutUtils.calculateAdaptiveColumns(w)
            assertEquals(
                "Wide screen width $w must clamp to maxColumns (6)",
                6,
                cols
            )
        }
    }

    @Test
    fun adaptiveGridColumns_monotonicityInvariantAcrossWidthSweep() {
        var previousCols = 2
        var w = 0.dp
        val step = 10.dp

        while (w <= 1200.dp) {
            val cols = ResponsiveLayoutUtils.calculateAdaptiveColumns(w)
            assertTrue(
                "Columns must be within [2, 6] at width $w (was $cols)",
                cols in 2..6
            )
            assertTrue(
                "Monotonicity violated: at width $w, cols ($cols) < previousCols ($previousCols)",
                cols >= previousCols
            )
            previousCols = cols
            w += step
        }
    }

    @Test
    fun adaptiveGridColumns_explicitOverrideStrictlyEnforced() {
        val testWidths = listOf(50.dp, 320.dp, 360.dp, 600.dp, 1200.dp)
        val testOverrides = listOf(1, 2, 3, 4, 5, 7, 8)

        for (width in testWidths) {
            for (explicit in testOverrides) {
                val result = ResponsiveLayoutUtils.calculateAdaptiveColumns(
                    availableWidth = width,
                    explicitColumns = explicit
                )
                assertEquals(
                    "Explicit override $explicit must be returned for width $width",
                    explicit,
                    result
                )
            }
        }
    }

    @Test
    fun newActionGrid_chunkingAndSpacerLayoutSymmetryStressTest() {
        // Stress-test the exact chunking and spacer math used in NewMenuComponents.kt:167-185
        val testWidths = listOf(320.dp, 360.dp, 400.dp, 600.dp, 1024.dp)
        val itemCounts = (0..25).toList()

        for (width in testWidths) {
            val cols = ResponsiveLayoutUtils.calculateAdaptiveColumns(width)
            for (count in itemCounts) {
                val dummyItems = (1..count).toList()
                val rows = dummyItems.chunked(cols)

                val expectedRowCount = if (count == 0) 0 else (count + cols - 1) / cols
                assertEquals(expectedRowCount, rows.size)

                for ((rowIndex, row) in rows.withIndex()) {
                    val isLastRow = (rowIndex == rows.size - 1)
                    val spacerCount = cols - row.size
                    assertTrue("Spacer count must never be negative", spacerCount >= 0)
                    if (!isLastRow) {
                        assertEquals("Non-last rows must be completely filled", cols, row.size)
                        assertEquals(0, spacerCount)
                    } else {
                        val expectedSpacers = (cols - (count % cols)) % cols
                        assertEquals(expectedSpacers, spacerCount)
                    }
                    val totalWeight = row.size * 1f + spacerCount * 1f
                    assertEquals("Row total weight must sum to columns ($cols)", cols.toFloat(), totalWeight, 0.0001f)
                }
            }
        }
    }

    // =========================================================================
    // 3. ADVERSARIAL STRESS TEST: BOTTOM SHEET MENUS SCROLLING CONTRACT (ALL 10)
    // =========================================================================

    @Test
    fun bottomSheetMenus_allTenMenus_haveExplicitUserScrollEnabledTrue() {
        for (fileName in allTenBottomSheetMenuFiles) {
            val file = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/menu/$fileName")
            assertTrue("Menu file $fileName must exist", file.exists())
            val content = file.readText()

            // 1. Must contain userScrollEnabled = true
            assertTrue(
                "Menu $fileName must contain 'userScrollEnabled = true'",
                content.contains("userScrollEnabled = true")
            )

            // 2. Must NOT contain userScrollEnabled = false
            assertFalse(
                "Menu $fileName must NOT contain 'userScrollEnabled = false'",
                content.contains("userScrollEnabled = false")
            )

            // 3. Must NOT condition userScrollEnabled on !isPortrait
            assertFalse(
                "Menu $fileName must NOT contain 'userScrollEnabled = !isPortrait'",
                content.contains("userScrollEnabled = !isPortrait")
            )

            // 4. Must NOT contain any negated orientation check in userScrollEnabled
            val conditionalScrollPattern = Pattern.compile("userScrollEnabled\\s*=\\s*(!?isPortrait|false)")
            assertFalse(
                "Menu $fileName matches prohibited scroll pattern: $conditionalScrollPattern",
                conditionalScrollPattern.matcher(content).find()
            )

            // 5. Verify userScrollEnabled is passed inside LazyColumn
            val lazyColumnIdx = content.indexOf("LazyColumn(")
            assertTrue("Menu $fileName must have a LazyColumn", lazyColumnIdx >= 0)
            val scrollEnabledIdx = content.indexOf("userScrollEnabled = true")
            assertTrue(
                "In menu $fileName, 'userScrollEnabled = true' must follow LazyColumn declaration",
                scrollEnabledIdx > lazyColumnIdx
            )
        }
    }

    // =========================================================================
    // 4. ADVERSARIAL STRESS TEST: isNarrowOrLargeFont BOUNDARIES
    // =========================================================================

    @Test
    fun isNarrowOrLargeFont_boundaryAnalysis() {
        // Width boundary: screenWidthDp < 360
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(359, 1.0f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.0f))

        // Font scale boundary: fontScale > 1.25f
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.25f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.2501f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.3f))

        // Sub-unity font scales on narrow vs standard screens
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(320, 0.85f)) // narrow triggers
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 0.85f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(412, 0.85f))

        // Large tablets
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.0f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.25f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.26f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(1200, 2.0f))
    }
}
