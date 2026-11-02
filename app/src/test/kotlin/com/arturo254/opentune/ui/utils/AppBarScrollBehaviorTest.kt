/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.utils

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class AppBarScrollBehaviorTest {

    @Test
    fun heightOffset_cannotExceedZero_duringDownwardScrollOrOverscroll() {
        val state = TopAppBarState(
            initialHeightOffsetLimit = -200f,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        // Simulate downward scroll / overscroll (consumed.y > 0)
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 50f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        // Height offset must remain clamped at 0f and not detach downward
        assertEquals(0f, state.heightOffset, 0.001f)
    }

    @Test
    fun heightOffset_scrollsUpAndClampsAtLimit() {
        val state = TopAppBarState(
            initialHeightOffsetLimit = -150f,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        // Scroll up by 80px
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -80f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(-80f, state.heightOffset, 0.001f)

        // Scroll up further by 100px (total -180px, exceeding limit of -150px)
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -100f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(-150f, state.heightOffset, 0.001f)

        // Scroll back down by 200px (should clamp at 0f, never positive)
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 200f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(0f, state.heightOffset, 0.001f)
    }

    @Test
    fun heightOffset_canScrollDisabled_ignoresScrollEvents() {
        val state = TopAppBarState(
            initialHeightOffsetLimit = -150f,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { false },
        )

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -50f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        assertEquals(0f, state.heightOffset, 0.001f)
    }
}
