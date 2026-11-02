/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.layout

import androidx.compose.ui.unit.dp
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.ui.utils.ResponsiveLayoutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResponsiveLayoutScalingTest {

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

    private val bottomSheetMenuFiles = listOf(
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

    @Test
    fun bottomSheetMenus_strictlyEnforceUnconstrainedScrollingInPortrait() {
        for (fileName in bottomSheetMenuFiles) {
            val file = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/menu/$fileName")
            assertTrue("Menu source file $fileName must exist on disk", file.exists())
            val content = file.readText()

            assertTrue(
                "Menu $fileName must explicitly set userScrollEnabled = true for unconstrained scrolling",
                content.contains("userScrollEnabled = true")
            )
            assertFalse(
                "Menu $fileName must NOT restrict scrolling based on orientation (!isPortrait)",
                content.contains("!isPortrait")
            )
        }
    }

    @Test
    fun scaledItemHeight_clampsFontScaleBelowOneToUnity() {
        val smallFontScale = 0.5f
        val scaledHeight = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, smallFontScale)
        assertEquals(ListItemHeight, scaledHeight)

        val point85FontScale = 0.85f
        val scaledHeight85 = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, point85FontScale)
        assertEquals(ListItemHeight, scaledHeight85)
    }

    @Test
    fun scaledItemHeight_equalsBaseHeightAtNormalFont() {
        val defaultFontScale = 1.0f
        val scaledHeight = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, defaultFontScale)
        assertEquals(64.dp, scaledHeight)
    }

    @Test
    fun scaledItemHeight_scalesProportionallyUnderLargeFonts() {
        val scale15 = 1.5f
        val scaledHeight15 = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, scale15)
        assertEquals(96.dp, scaledHeight15)

        val scale20 = 2.0f
        val scaledHeight20 = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, scale20)
        assertEquals(128.dp, scaledHeight20)

        val scale30 = 3.0f
        val scaledHeight30 = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, scale30)
        assertEquals(192.dp, scaledHeight30)
    }

    @Test
    fun horizontalGridContainerHeight_scalesWithRowsAndFontScale() {
        val rows = 4
        val fontScale = 1.3f
        val scaledItemHeight = ResponsiveLayoutUtils.calculateScaledItemHeight(ListItemHeight, fontScale)
        val containerHeight = scaledItemHeight * rows

        val expectedSingleHeight = 64.dp * 1.3f
        val expectedContainerHeight = expectedSingleHeight * 4
        assertEquals(expectedContainerHeight.value, containerHeight.value, 0.001f)
        assertTrue(containerHeight > ListItemHeight * rows)
    }

    @Test
    fun miniPlayerHeight_satisfiesMinimumBound() {
        assertEquals(64.dp, MiniPlayerHeight)
    }

    @Test
    fun adaptiveGridColumns_calculatesCorrectColumnCountAcrossWidths() {
        // Narrow screen (< 176dp, e.g. 100dp) -> clamped to minColumns = 2
        val narrowColumns = ResponsiveLayoutUtils.calculateAdaptiveColumns(100.dp, 88.dp)
        assertEquals(2, narrowColumns)

        // Width 200dp with 88dp min width -> 200 / 88 = 2 -> 2 columns
        val columns200 = ResponsiveLayoutUtils.calculateAdaptiveColumns(200.dp, 88.dp)
        assertEquals(2, columns200)

        // Compact phone screen (~320dp) with 88dp min width -> 320 / 88 = 3 columns
        val compactColumns = ResponsiveLayoutUtils.calculateAdaptiveColumns(320.dp, 88.dp)
        assertEquals(3, compactColumns)

        // Standard phone screen (360dp) with 88dp min width -> 360 / 88 = 4 columns
        val columns360 = ResponsiveLayoutUtils.calculateAdaptiveColumns(360.dp, 88.dp)
        assertEquals(4, columns360)

        // Wide phone / small tablet (600dp) with 88dp min width -> 600 / 88 = 6 columns (maxColumns)
        val columns600 = ResponsiveLayoutUtils.calculateAdaptiveColumns(600.dp, 88.dp)
        assertEquals(6, columns600)

        // Large tablet screen (1000dp) with 88dp min width -> 1000 / 88 = 11 -> clamped to maxColumns = 6
        val columns1000 = ResponsiveLayoutUtils.calculateAdaptiveColumns(1000.dp, 88.dp)
        assertEquals(6, columns1000)
    }

    @Test
    fun adaptiveGridColumns_respectsExplicitOverride() {
        val override3 = ResponsiveLayoutUtils.calculateAdaptiveColumns(
            availableWidth = 1000.dp,
            minColumnWidth = 88.dp,
            explicitColumns = 3
        )
        assertEquals(3, override3)

        val override1 = ResponsiveLayoutUtils.calculateAdaptiveColumns(
            availableWidth = 100.dp,
            minColumnWidth = 88.dp,
            explicitColumns = 1
        )
        assertEquals(1, override1)

        val override5 = ResponsiveLayoutUtils.calculateAdaptiveColumns(
            availableWidth = 360.dp,
            minColumnWidth = 88.dp,
            explicitColumns = 5
        )
        assertEquals(5, override5)
    }

    @Test
    fun adaptiveGridColumns_customThresholds() {
        // Custom minColumnWidth = 100dp, minColumns = 1, maxColumns = 8
        assertEquals(1, ResponsiveLayoutUtils.calculateAdaptiveColumns(50.dp, 100.dp, minColumns = 1, maxColumns = 8))
        assertEquals(5, ResponsiveLayoutUtils.calculateAdaptiveColumns(550.dp, 100.dp, minColumns = 1, maxColumns = 8))
        assertEquals(8, ResponsiveLayoutUtils.calculateAdaptiveColumns(950.dp, 100.dp, minColumns = 1, maxColumns = 8))
    }

    @Test
    fun isNarrowOrLargeFont_detectsBoundaryDimensionsAndScales() {
        // Normal phone, normal font (screenWidth >= 360 and fontScale <= 1.25f)
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(390, 1.0f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.0f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.25f))

        // Boundary width: screenWidthDp < 360
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(359, 1.0f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(320, 1.0f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(280, 0.85f))

        // Boundary font scale: fontScale > 1.25f
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(360, 1.26f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(390, 1.3f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(390, 2.0f))

        // Tablet with normal font
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.0f))
        assertFalse(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.25f))

        // Tablet with large / extra large font
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 1.5f))
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(800, 3.0f))

        // Narrow screen with large font (both conditions true)
        assertTrue(ResponsiveLayoutUtils.isNarrowOrLargeFont(320, 1.5f))
    }
}
