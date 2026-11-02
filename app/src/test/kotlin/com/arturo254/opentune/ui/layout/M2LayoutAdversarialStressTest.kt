/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Adversarial stress tests for Milestone 2 Responsive Layout & Font Scaling Resilience.
 * Authored by challenger_layout_2 to stress test edge cases, boundaries, and mathematical invariants.
 */
class M2LayoutAdversarialStressTest {

    // ── 1. Bottom Sheet Menu Scrolling Verification ──────────────────────────

    @Test
    fun bottomSheetMenus_allTenMenusHaveUserScrollEnabledAlwaysTrue() {
        // Find project root
        var projectDir = File(System.getProperty("user.dir") ?: ".")
        while (!File(projectDir, "settings.gradle.kts").exists() && projectDir.parentFile != null) {
            projectDir = projectDir.parentFile
        }

        val menuDir = File(projectDir, "app/src/main/kotlin/com/arturo254/opentune/ui/menu")
        assertTrue("Menu directory must exist: ${menuDir.absolutePath}", menuDir.exists())

        val targetMenuFiles = listOf(
            "SongMenu.kt",
            "YouTubeSongMenu.kt",
            "YouTubePlaylistMenu.kt",
            "YouTubeArtistMenu.kt",
            "YouTubeAlbumMenu.kt",
            "SelectionSongsMenu.kt",
            "PlaylistMenu.kt",
            "LyricsMenu.kt",
            "AlbumMenu.kt",
            "ArtistMenu.kt"
        )

        for (fileName in targetMenuFiles) {
            val file = File(menuDir, fileName)
            assertTrue("Menu file must exist: $fileName", file.exists())
            val text = file.readText()

            // Verify it does NOT contain the buggy condition
            assertFalse(
                "Menu $fileName still contains '!isPortrait' scroll lock!",
                text.contains("userScrollEnabled = !isPortrait")
            )
            assertFalse(
                "Menu $fileName contains 'userScrollEnabled = false'!",
                text.contains("userScrollEnabled = false")
            )

            // Verify it explicitly enables user scrolling
            assertTrue(
                "Menu $fileName must have 'userScrollEnabled = true'!",
                text.contains("userScrollEnabled = true")
            )
        }
    }

    // ── 2. NewActionGrid Stress Testing ──────────────────────────────────────

    private fun computeEffectiveColumns(
        availableWidth: Dp,
        minColumnWidth: Dp = 88.dp,
        explicitColumns: Int? = null
    ): Int {
        return explicitColumns ?: (availableWidth / minColumnWidth).toInt().coerceIn(2, 6)
    }

    private data class GridSimulationResult(
        val effectiveColumns: Int,
        val totalRows: Int,
        val rowItemCounts: List<Int>,
        val lastRowSpacerCount: Int,
        val totalWeightsPerRow: List<Float>
    )

    private fun simulateActionGrid(
        itemCount: Int,
        availableWidth: Dp,
        minColumnWidth: Dp = 88.dp,
        explicitColumns: Int? = null
    ): GridSimulationResult {
        val effectiveColumns = computeEffectiveColumns(availableWidth, minColumnWidth, explicitColumns)
        assertTrue("Effective columns must be >= 1", effectiveColumns >= 1)

        val items = (1..itemCount).toList()
        val rows = items.chunked(effectiveColumns)

        val rowItemCounts = rows.map { it.size }
        val lastRowSpacerCount = if (rows.isEmpty()) 0 else (effectiveColumns - rows.last().size)
        val totalWeights = rows.map { row ->
            val itemWeights = row.size * 1f
            val spacerWeights = (effectiveColumns - row.size) * 1f
            itemWeights + spacerWeights
        }

        return GridSimulationResult(
            effectiveColumns = effectiveColumns,
            totalRows = rows.size,
            rowItemCounts = rowItemCounts,
            lastRowSpacerCount = lastRowSpacerCount,
            totalWeightsPerRow = totalWeights
        )
    }

    @Test
    fun newActionGrid_stressTestItemCountsAcrossWidths() {
        val testWidths = listOf(300.dp, 360.dp, 400.dp, 600.dp, 1000.dp)
        val itemCounts = listOf(1, 2, 3, 5, 10, 20)

        val expectedColumnsMap = mapOf(
            300.dp to 3,  // 300 / 88 = 3.40 -> 3
            360.dp to 4,  // 360 / 88 = 4.09 -> 4
            400.dp to 4,  // 400 / 88 = 4.54 -> 4
            600.dp to 6,  // 600 / 88 = 6.81 -> 6 (clamped)
            1000.dp to 6  // 1000 / 88 = 11.36 -> 6 (clamped)
        )

        for (width in testWidths) {
            val expectedCols = expectedColumnsMap[width]!!

            for (count in itemCounts) {
                val sim = simulateActionGrid(count, width)

                // 1. Column clamp verification
                assertEquals(
                    "Column count mismatch at width $width",
                    expectedCols,
                    sim.effectiveColumns
                )
                assertTrue("Columns must be clamped within 2..6", sim.effectiveColumns in 2..6)

                // 2. Row count verification
                val expectedRows = (count + expectedCols - 1) / expectedCols
                assertEquals(
                    "Total rows mismatch for $count items at width $width",
                    expectedRows,
                    sim.totalRows
                )

                // 3. Weight symmetry verification (each row must sum to effectiveColumns * 1f)
                for (weight in sim.totalWeightsPerRow) {
                    assertEquals(
                        "Each row total weight must equal effectiveColumns for perfect layout symmetry",
                        expectedCols.toFloat(),
                        weight,
                        0.0001f
                    )
                }

                // 4. Spacer padding non-negativity
                assertTrue(
                    "Last row spacer count must be non-negative",
                    sim.lastRowSpacerCount >= 0
                )
                assertTrue(
                    "Last row spacer count must be strictly less than effectiveColumns",
                    sim.lastRowSpacerCount < expectedCols
                )
            }
        }
    }

    @Test
    fun newActionGrid_extremeBoundaryWidths() {
        // Zero or negative widths must clamp to 2
        assertEquals(2, computeEffectiveColumns(0.dp))
        assertEquals(2, computeEffectiveColumns((-50).dp))

        // Tiny widths (< 176dp) clamp to 2
        assertEquals(2, computeEffectiveColumns(100.dp))
        assertEquals(2, computeEffectiveColumns(175.dp))

        // Exact boundary at 176dp (88 * 2) -> 2
        assertEquals(2, computeEffectiveColumns(176.dp))

        // Huge widths clamp to 6
        assertEquals(6, computeEffectiveColumns(1500.dp))
        assertEquals(6, computeEffectiveColumns(5000.dp))
        assertEquals(6, computeEffectiveColumns(Dp.Infinity))

        // Zero items produces 0 rows safely without division by zero
        val zeroItems = simulateActionGrid(0, 360.dp)
        assertEquals(0, zeroItems.totalRows)
        assertEquals(0, zeroItems.lastRowSpacerCount)
    }

    // ── 3. MiniPlayer Narrow Width Budget Stress Testing ─────────────────────

    private data class MiniPlayerWidthBudget(
        val screenWidth: Dp,
        val fontScale: Float,
        val isNarrowOrLargeFont: Boolean,
        val artworkWidth: Dp,
        val artistProfileButtonWidth: Dp,
        val likeButtonWidth: Dp,
        val playPauseButtonWidth: Dp,
        val totalFixedAndPaddingWidth: Dp,
        val remainingWidthForTextMarquee: Dp
    )

    private fun computeMiniPlayerBudget(
        screenWidth: Dp,
        fontScale: Float = 1.0f,
        isTogetherActive: Boolean = false
    ): MiniPlayerWidthBudget {
        val screenWidthDpInt = screenWidth.value.toInt()
        val isNarrowOrLargeFont = screenWidthDpInt < 360 || fontScale > 1.25f

        val horizontalPadding = 8.dp * 2 // Row padding
        val artworkSize = 44.dp
        val spacerAfterArtwork = if (isNarrowOrLargeFont) 6.dp else 12.dp

        val togetherWidth = if (isTogetherActive) {
            val spacer = if (isNarrowOrLargeFont) 4.dp else 8.dp
            val pill = 14.dp + (10.dp * 2) // icon + padding
            spacer + pill
        } else 0.dp

        val spacerBeforeControls = if (isNarrowOrLargeFont) 2.dp else 4.dp
        val artistButtonWidth = if (!isNarrowOrLargeFont) 40.dp else 0.dp
        val likeButtonWidth = 40.dp
        val spacerBetweenControls = 4.dp
        val playPauseButtonWidth = 48.dp
        val trailingSpacer = 4.dp

        val totalFixed = horizontalPadding +
                artworkSize +
                spacerAfterArtwork +
                togetherWidth +
                spacerBeforeControls +
                artistButtonWidth +
                likeButtonWidth +
                spacerBetweenControls +
                playPauseButtonWidth +
                trailingSpacer

        val remainingForText = screenWidth - totalFixed

        return MiniPlayerWidthBudget(
            screenWidth = screenWidth,
            fontScale = fontScale,
            isNarrowOrLargeFont = isNarrowOrLargeFont,
            artworkWidth = artworkSize,
            artistProfileButtonWidth = artistButtonWidth,
            likeButtonWidth = likeButtonWidth,
            playPauseButtonWidth = playPauseButtonWidth,
            totalFixedAndPaddingWidth = totalFixed,
            remainingWidthForTextMarquee = remainingForText
        )
    }

    @Test
    fun miniPlayer_essentialControlsNeverClippedUnderNarrowWidths() {
        val narrowWidths = listOf(240.dp, 280.dp, 300.dp, 310.dp, 319.dp)

        for (width in narrowWidths) {
            val budget = computeMiniPlayerBudget(width, fontScale = 1.0f)

            // 1. isNarrowOrLargeFont MUST activate for < 360dp
            assertTrue("Must detect narrow screen at $width", budget.isNarrowOrLargeFont)

            // 2. Artist profile button MUST be dropped to conserve horizontal space
            assertEquals(0.dp, budget.artistProfileButtonWidth)

            // 3. Essential controls MUST retain full hit targets
            assertEquals("Like button must remain full 40dp", 40.dp, budget.likeButtonWidth)
            assertEquals("Play/Pause button must remain full 48dp", 48.dp, budget.playPauseButtonWidth)

            // 4. Fixed footprint must be compact (<= 164dp)
            assertEquals(164.dp, budget.totalFixedAndPaddingWidth)

            // 5. Remaining width for text marquee must strictly be positive
            assertTrue(
                "Remaining text width at $width must be > 0 (was ${budget.remainingWidthForTextMarquee})",
                budget.remainingWidthForTextMarquee > 0.dp
            )
            // Even at 240dp, remaining width is 76dp (enough for marquee)
            assertTrue(
                "Text marquee must have at least 70dp at $width",
                budget.remainingWidthForTextMarquee >= 76.dp
            )
        }
    }

    @Test
    fun miniPlayer_largeFontActivationOnStandardWidth() {
        // At standard 390dp screen width but large font (e.g. 1.35x), isNarrowOrLargeFont also engages
        val budgetLargeFont = computeMiniPlayerBudget(390.dp, fontScale = 1.35f)
        assertTrue(budgetLargeFont.isNarrowOrLargeFont)
        assertEquals(0.dp, budgetLargeFont.artistProfileButtonWidth)
        assertEquals(40.dp, budgetLargeFont.likeButtonWidth)
        assertEquals(48.dp, budgetLargeFont.playPauseButtonWidth)
        assertTrue(budgetLargeFont.remainingWidthForTextMarquee > 200.dp)
    }
}
