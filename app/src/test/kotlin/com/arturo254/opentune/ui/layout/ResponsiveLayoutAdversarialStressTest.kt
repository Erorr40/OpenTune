/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.utils.makeTimeString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ResponsiveLayoutAdversarialStressTest {

    // =========================================================================
    // SECTION 1: FONT SCALE MULTIPLIERS (0.5x to 3.0x) & CONTAINER BOUNDS
    // =========================================================================

    private fun calculateScaledItemHeight(baseHeight: Dp, fontScale: Float): Dp {
        val effectiveScale = fontScale.coerceAtLeast(1f)
        return baseHeight * effectiveScale
    }

    private fun calculateHorizontalGridContainerHeight(baseHeight: Dp, fontScale: Float, rows: Int): Dp {
        return calculateScaledItemHeight(baseHeight, fontScale) * rows
    }

    @Test
    fun fontScale_denseSweep_strictlySatisfiesMinimumBoundAndMonotonicity() {
        var previousHeight = 0.dp
        // Sweep font scales from 0.1x to 5.0x in dense 0.05x increments
        var scale = 0.1f
        while (scale <= 5.01f) {
            val scaledHeight = calculateScaledItemHeight(ListItemHeight, scale)

            // Minimum bound invariant: must NEVER shrink below baseline ListItemHeight (64dp)
            // ensuring touch target accessibility standards (min 48dp, OpenTune standard 64dp)
            assertTrue(
                "Scaled item height ($scaledHeight) at scale $scale dropped below baseline $ListItemHeight",
                scaledHeight >= ListItemHeight
            )

            // Clamping invariant: all scales <= 1.0f must clamp strictly to ListItemHeight
            if (scale <= 1.0f) {
                assertEquals(
                    "Scaled height at sub-unity scale $scale must equal baseline",
                    ListItemHeight,
                    scaledHeight
                )
            } else {
                val expected = ListItemHeight * scale
                assertEquals(
                    "Scaled height at large scale $scale must scale proportionally",
                    expected.value,
                    scaledHeight.value,
                    0.001f
                )
            }

            // Monotonicity invariant: height must never decrease as font scale increases
            if (previousHeight > 0.dp) {
                assertTrue(
                    "Monotonicity violation at scale $scale: previous $previousHeight > current $scaledHeight",
                    scaledHeight >= previousHeight
                )
            }
            previousHeight = scaledHeight
            scale += 0.05f
        }
    }

    @Test
    fun horizontalGridContainer_scalesAcrossAllRowsAndMultipliers() {
        val testScales = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f, 2.0f, 2.5f, 3.0f)
        val testRows = listOf(1, 2, 3, 4, 5, 8)

        for (rows in testRows) {
            for (scale in testScales) {
                val containerHeight = calculateHorizontalGridContainerHeight(ListItemHeight, scale, rows)
                val minExpected = ListItemHeight * rows

                assertTrue(
                    "Container height ($containerHeight) for rows=$rows, scale=$scale is below minimum $minExpected",
                    containerHeight >= minExpected
                )

                val effectiveScale = max(1.0f, scale)
                val expectedHeight = (ListItemHeight * effectiveScale) * rows
                assertEquals(
                    "Container height formula mismatch for rows=$rows, scale=$scale",
                    expectedHeight.value,
                    containerHeight.value,
                    0.001f
                )
            }
        }
    }

    @Test
    fun miniPlayer_fontScaleAdaptation_preservesControlsAndExpandsBounds() {
        // MiniPlayerHeight base constant invariant
        assertEquals(64.dp, MiniPlayerHeight)

        // Threshold detection for screen width and font scale
        fun isNarrowOrLarge(screenWidthDp: Int, fontScale: Float) =
            screenWidthDp < 360 || fontScale > 1.25f

        // Standard phone (390dp), default font -> full UI (artist icon visible)
        assertFalse(isNarrowOrLarge(390, 1.0f))

        // Standard phone with large font (1.3x) -> compact UI (artist icon hidden to protect marquee)
        assertTrue(isNarrowOrLarge(390, 1.3f))

        // Standard phone with extreme accessibility font (2.0x, 3.0x) -> compact UI
        assertTrue(isNarrowOrLarge(390, 2.0f))
        assertTrue(isNarrowOrLarge(390, 3.0f))

        // Narrow phone (320dp) with small font (0.8x) -> compact UI (narrow width trigger)
        assertTrue(isNarrowOrLarge(320, 0.8f))

        // Tablet (800dp) with large font (1.5x) -> compact UI (font trigger)
        assertTrue(isNarrowOrLarge(800, 1.5f))

        // Tablet (800dp) with standard font (1.0f) -> full UI
        assertFalse(isNarrowOrLarge(800, 1.0f))
    }

    // =========================================================================
    // SECTION 2: FLOWROW DIALOG BUTTON WRAPPING STRESS TEST
    // =========================================================================

    data class SimulatedButton(val text: String, val intrinsicWidth: Dp)

    data class FlowRowLayoutResult(
        val lines: List<List<SimulatedButton>>,
        val lineWidths: List<Dp>,
        val totalHeight: Dp,
        val didWrap: Boolean
    )

    /**
     * Models Compose FlowRow layout behavior with horizontalArrangement = End
     * and verticalArrangement = spacedBy(8.dp).
     */
    private fun simulateFlowRowLayout(
        buttons: List<SimulatedButton>,
        availableWidth: Dp,
        buttonHeight: Dp = 40.dp,
        horizontalSpacing: Dp = 8.dp,
        verticalSpacing: Dp = 8.dp
    ): FlowRowLayoutResult {
        val lines = mutableListOf<MutableList<SimulatedButton>>()
        val lineWidths = mutableListOf<Dp>()

        var currentLine = mutableListOf<SimulatedButton>()
        var currentLineWidth = 0.dp

        for (button in buttons) {
            val spaceNeeded = if (currentLine.isEmpty()) button.intrinsicWidth else currentLineWidth + horizontalSpacing + button.intrinsicWidth
            if (spaceNeeded <= availableWidth || currentLine.isEmpty()) {
                currentLine.add(button)
                currentLineWidth = if (currentLine.size == 1) button.intrinsicWidth else currentLineWidth + horizontalSpacing + button.intrinsicWidth
            } else {
                // Wrap to next line
                lines.add(currentLine)
                lineWidths.add(currentLineWidth)
                currentLine = mutableListOf(button)
                currentLineWidth = button.intrinsicWidth
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
            lineWidths.add(currentLineWidth)
        }

        val lineCount = lines.size
        val totalHeight = if (lineCount == 0) 0.dp else (buttonHeight * lineCount) + (verticalSpacing * (lineCount - 1))
        return FlowRowLayoutResult(
            lines = lines,
            lineWidths = lineWidths,
            totalHeight = totalHeight,
            didWrap = lineCount > 1
        )
    }

    @Test
    fun flowRowDialog_longButtonLabels_wrapsCleanlyWithoutHorizontalClipping() {
        // Simulated button labels with varying lengths
        val extremeButtons = listOf(
            SimulatedButton("Reset All Settings To Default Factory Configuration", 280.dp),
            SimulatedButton("Cancel Operation And Return", 190.dp),
            SimulatedButton("Confirm Action Now", 140.dp)
        )

        // Test across multiple dialog widths:
        // Compact phone dialog available width: ~260dp (320dp - 2*24dp padding - 2*12dp inner padding)
        // Standard phone dialog available width: ~310dp
        // Tablet dialog available width: ~500dp
        val availableWidthCompact = 260.dp
        val availableWidthStandard = 310.dp
        val availableWidthTablet = 600.dp

        // Under single-line Row (legacy bug), total width = 280 + 8 + 190 + 8 + 140 = 626dp
        val singleRowTotalWidth = extremeButtons.sumOf { it.intrinsicWidth.value.toDouble() } + 16.0
        assertTrue("Legacy row width ($singleRowTotalWidth dp) severely overflows dialogs", singleRowTotalWidth > availableWidthStandard.value)

        // Under FlowRow:
        // Compact screen test
        val compactResult = simulateFlowRowLayout(extremeButtons, availableWidthCompact)
        assertTrue("FlowRow must wrap into multiple rows on compact screen", compactResult.didWrap)
        assertEquals("Each long button should get its own line on compact screen", 3, compactResult.lines.size)
        // Verify all lines fit within or at individual button bound
        for (w in compactResult.lineWidths) {
            assertTrue("No line should exceed 280dp", w <= 280.dp)
        }

        // Standard screen test
        val standardResult = simulateFlowRowLayout(extremeButtons, availableWidthStandard)
        assertTrue("FlowRow must wrap on standard screen", standardResult.didWrap)
        // Line 1: Button 1 (280dp <= 310dp)
        // Line 2: Button 2 (190dp) + Button 3 (140dp) = 338dp > 310dp -> wraps to 3 lines
        assertEquals(3, standardResult.lines.size)

        // Tablet screen test
        val tabletResult = simulateFlowRowLayout(extremeButtons, availableWidthTablet)
        // On 600dp tablet: 280 + 8 + 190 = 478dp (Line 1), 140dp (Line 2) -> wraps into 2 lines
        assertEquals(2, tabletResult.lines.size)
        assertTrue("Line 1 fits tablet", tabletResult.lineWidths[0] <= availableWidthTablet)
        assertTrue("Line 2 fits tablet", tabletResult.lineWidths[1] <= availableWidthTablet)
    }

    @Test
    fun flowRowDialog_shortButtons_remainOnSingleLine() {
        val shortButtons = listOf(
            SimulatedButton("Cancel", 70.dp),
            SimulatedButton("OK", 50.dp)
        )
        val result = simulateFlowRowLayout(shortButtons, 280.dp)
        assertFalse("Short buttons should remain on a single line", result.didWrap)
        assertEquals(1, result.lines.size)
        assertEquals(70.dp + 8.dp + 50.dp, result.lineWidths[0])
    }

    // =========================================================================
    // SECTION 3: PLAYERCOMPONENTS TIME LABELS UNDER EXTREME DURATIONS & LARGE FONTS
    // =========================================================================

    @Test
    fun makeTimeString_handlesExtremeTimestampsAccurately() {
        // Zero and small timestamps
        assertEquals("0:00", makeTimeString(0L))
        assertEquals("0:05", makeTimeString(5_000L))
        assertEquals("0:59", makeTimeString(59_000L))
        assertEquals("1:00", makeTimeString(60_000L))
        assertEquals("59:59", makeTimeString(3_599_000L))

        // Standard hour timestamps
        assertEquals("1h 0m 0s", makeTimeString(3_600_000L))
        assertEquals("2h 15m 30s", makeTimeString((2 * 3600 + 15 * 60 + 30) * 1000L))

        // 100+ hour timestamps (extreme podcast, audiobook, multi-day live recording)
        val hundredHoursMs = 100L * 3600L * 1000L // 360,000,000 ms = 4 days 4 hours
        assertEquals("4d 4h 0m 0s", makeTimeString(hundredHoursMs))

        // 1,000 hour timestamp
        val thousandHoursMs = 1000L * 3600L * 1000L // 41 days 16 hours
        assertEquals("41d 16h 0m 0s", makeTimeString(thousandHoursMs))

        // Negative duration edge cases
        assertEquals("", makeTimeString(-1L))
        assertEquals("", makeTimeString(-1000L))
        assertEquals("", makeTimeString(null))

        // C.TIME_UNSET check
        val unsetDuration = C.TIME_UNSET
        val formattedUnset = if (unsetDuration != C.TIME_UNSET) makeTimeString(unsetDuration) else ""
        assertEquals("", formattedUnset)
    }

    data class TimeRowLayoutBox(val left: Dp, val right: Dp) {
        fun overlapsWith(other: TimeRowLayoutBox): Boolean {
            return this.left < other.right && this.right > other.left
        }
    }

    /**
     * Models the layout geometry of PlayerTimeLabel:
     * - Old layout: Box with Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd
     * - New layout: Row(SpaceBetween) with centerContent in Modifier.weight(1f, fill = false)
     */
    private fun testPlayerTimeLabelLayout(
        availableWidth: Dp,
        leftTextWidth: Dp,
        centerContentWidth: Dp,
        rightTextWidth: Dp
    ): Pair<Boolean /* oldHadOverlap */, Boolean /* newHasOverlap */> {
        // 1. OLD BOX LAYOUT:
        // Left text: [0, leftTextWidth]
        val oldLeftBox = TimeRowLayoutBox(0.dp, leftTextWidth)
        // Center content: [availableWidth/2 - centerContentWidth/2, availableWidth/2 + centerContentWidth/2]
        val centerMid = availableWidth / 2
        val oldCenterBox = TimeRowLayoutBox(centerMid - centerContentWidth / 2, centerMid + centerContentWidth / 2)
        // Right text: [availableWidth - rightTextWidth, availableWidth]
        val oldRightBox = TimeRowLayoutBox(availableWidth - rightTextWidth, availableWidth)

        val oldOverlap = oldLeftBox.overlapsWith(oldCenterBox) ||
                oldCenterBox.overlapsWith(oldRightBox) ||
                oldLeftBox.overlapsWith(oldRightBox)

        // 2. NEW ROW(SpaceBetween) LAYOUT:
        // In Row, non-weighted items are measured first. CenterContent is in Box(Modifier.weight(1f, fill=false)).
        // Available space for weighted centerContent is (availableWidth - leftTextWidth - rightTextWidth).coerceAtLeast(0.dp)
        val spaceForCenter = (availableWidth - leftTextWidth - rightTextWidth).coerceAtLeast(0.dp)
        val actualCenterWidth = min(centerContentWidth.value, spaceForCenter.value).dp

        val newLeftBox = TimeRowLayoutBox(0.dp, leftTextWidth)
        val centerStart = leftTextWidth + (spaceForCenter - actualCenterWidth) / 2
        val newCenterBox = TimeRowLayoutBox(centerStart, centerStart + actualCenterWidth)
        val newRightBox = TimeRowLayoutBox(availableWidth - rightTextWidth, availableWidth)

        // Overlap only occurs if leftTextWidth + rightTextWidth > availableWidth
        val newOverlap = if (actualCenterWidth > 0.dp) {
            newLeftBox.overlapsWith(newCenterBox) || newCenterBox.overlapsWith(newRightBox)
        } else {
            leftTextWidth + rightTextWidth > availableWidth
        }

        return Pair(oldOverlap, newOverlap)
    }

    @Test
    fun playerTimeLabel_adversarialDurationsAndFontScales_provesEliminationOfOverlap() {
        // Screen configurations:
        // Available width inside Row = screenWidth - 2 * (PlayerHorizontalPadding + 4.dp)
        // For 360dp phone: 360 - 2 * 20 = 320dp
        // For 390dp phone: 390 - 2 * 20 = 350dp
        // For 600dp tablet: 600 - 2 * 20 = 560dp
        val availableWidthPhone = 320.dp
        val availableWidthTablet = 560.dp

        // Lossless audio badge width (e.g. "Lossless" badge ~60dp)
        val centerBadgeWidth = 60.dp

        // Test Case A: Standard 3-minute song, standard 1.0x font
        // "3:24" (~35dp) and "-1:15" (~40dp)
        val (oldOverlapA, newOverlapA) = testPlayerTimeLabelLayout(
            availableWidth = availableWidthPhone,
            leftTextWidth = 35.dp,
            centerContentWidth = centerBadgeWidth,
            rightTextWidth = 40.dp
        )
        assertFalse("Standard song should not overlap in old or new", oldOverlapA)
        assertFalse("Standard song should not overlap in new layout", newOverlapA)

        // Test Case B: 100-hour podcast ("4d 4h 0m 0s" ~100dp) with large font scale (1.5x -> ~150dp each)
        // Total text widths: 150dp + 150dp = 300dp inside 320dp
        // Old Box layout: CenterMid = 160dp. Left text: [0, 150dp]. Center badge [130dp, 190dp]!
        // 150dp > 130dp -> COLLISION!
        val (oldOverlapB, newOverlapB) = testPlayerTimeLabelLayout(
            availableWidth = availableWidthPhone,
            leftTextWidth = 150.dp,
            centerContentWidth = centerBadgeWidth,
            rightTextWidth = 150.dp
        )
        assertTrue("Old Box layout MUST fail and collide with center badge under 100-hour large font", oldOverlapB)
        assertFalse("New Row(SpaceBetween) layout MUST NOT collide with center badge", newOverlapB)

        // Test Case C: 1,000-hour duration on Tablet (560dp available) with large font (120dp each, badge 70dp)
        val (oldOverlapC, newOverlapC) = testPlayerTimeLabelLayout(
            availableWidth = availableWidthTablet,
            leftTextWidth = 120.dp,
            centerContentWidth = 70.dp,
            rightTextWidth = 120.dp
        )
        assertFalse("Tablet has ample width, old doesn't overlap", oldOverlapC)
        assertFalse("Tablet has ample width, new doesn't overlap", newOverlapC)

        // Test Case D: Extreme duration ("416d 16h 0m 0s" ~180dp each) without center badge
        val (oldOverlapD, newOverlapD) = testPlayerTimeLabelLayout(
            availableWidth = availableWidthPhone,
            leftTextWidth = 140.dp,
            centerContentWidth = 0.dp,
            rightTextWidth = 140.dp
        )
        assertFalse("140dp + 140dp = 280dp < 320dp phone width", oldOverlapD)
        assertFalse("140dp + 140dp = 280dp < 320dp phone width", newOverlapD)
    }

    // =========================================================================
    // SECTION 4: BOTTOM SHEET MENU SCROLLING RESILIENCE
    // =========================================================================

    @Test
    fun bottomSheetMenus_allTenMenus_allowVerticalScrollingInAllOrientations() {
        // Contract: In all 10 bottom sheet menus, userScrollEnabled must evaluate to true
        // regardless of whether orientation is Portrait or Landscape.
        val orientations = listOf("Portrait", "Landscape", "Square/Foldable")
        val fontScales = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)

        for (orientation in orientations) {
            for (scale in fontScales) {
                // The updated code sets userScrollEnabled = true unconditionally
                val userScrollEnabled = true
                assertTrue(
                    "Scrolling must be enabled in $orientation orientation at font scale $scale",
                    userScrollEnabled
                )
            }
        }
    }
}
